/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.string_analysis

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

import org.opalj.fpcf.Entity
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.InterimLUBP
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Result
import org.opalj.fpcf.SomeEPS
import org.opalj.value.ValueInformation
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.br.fpcf.FPCFLazyAnalysisScheduler
import org.opalj.br.fpcf.cg.properties.Callees
import org.opalj.br.fpcf.cg.properties.CallersProperty
import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.br.Method
import org.opalj.tac.Stmt
import org.opalj.tac.fpcf.analyses.string_analysis.preprocessing.AbstractPathFinder
import org.opalj.tac.fpcf.analyses.string_analysis.preprocessing.NestedPathElement
import org.opalj.tac.fpcf.analyses.string_analysis.preprocessing.Path
import org.opalj.tac.fpcf.analyses.string_analysis.preprocessing.PathTransformer
import org.opalj.tac.fpcf.analyses.string_analysis.preprocessing.WindowPathFinder
import org.opalj.tac.fpcf.properties.TACAI
import org.opalj.tac.ExprStmt
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.InterpretationHandler
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.interprocedural.InterproceduralInterpretationHandler
import org.opalj.tac.fpcf.analyses.string_analysis.preprocessing.FlatPathElement
import org.opalj.tac.fpcf.analyses.string_analysis.preprocessing.NestedPathType
import org.opalj.tac.fpcf.analyses.string_analysis.preprocessing.SubPath
import org.opalj.tac.Assignment
import org.opalj.tac.DUVar
import org.opalj.tac.FunctionCall
import org.opalj.tac.MethodCall
import org.opalj.tac.SimpleTACAIKey
import org.opalj.tac.TACMethodParameter
import org.opalj.tac.TACode

/**
 * InterproceduralStringAnalysis processes a read operation of a string variable at a program
 * position, ''pp'', in a way that it finds the set of possible strings that can be read at ''pp''.
 * <p>
 * In comparison to [[IntraproceduralStringAnalysis]], this version tries to resolve method calls
 * that are involved in a string construction as far as possible.
 * <p>
 * The main difference in the intra- and interprocedural implementation is the following (see the
 * description of [[IntraproceduralStringAnalysis]] for a general overview): This analysis can only
 * start to transform the computed lean paths into a string tree (again using a [[PathTransformer]])
 * after all relevant string values (determined by the [[InterproceduralInterpretationHandler]])
 * have been figured out. As the [[PropertyStore]] is used for recursively starting this analysis
 * to determine possible strings of called method and functions, the path transformation can take
 * place after all results for sub-expressions are available. Thus, the interprocedural
 * interpretation handler cannot determine final results, e.g., for the array interpreter or static
 * function call interpreter. This analysis handles this circumstance by first collecting all
 * information for all definition sites. Only when these are available, further information, e.g.,
 * for the final results of arrays or static function calls, are derived. Finally, after all
 * these information are ready as well, the path transformation takes place by only looking up what
 * string expression corresponds to which definition sites (remember, at this point, for all
 * definition sites all possible string values are known, thus look-ups are enough and no further
 * interpretation is required).
 *
 * @author Patrick Mell
 */
class InterproceduralStringAnalysis(
        val project: SomeProject
) extends FPCFAnalysis {

    private var declaredMethods: DeclaredMethods = _

    def analyze(data: P): ProperPropertyComputationResult = {
        val state = InterproceduralComputationState(data)
        declaredMethods = project.get(DeclaredMethodsKey)
        // TODO: Is there a way to get the declared method in constant time?
        val dm = declaredMethods.declaredMethods.find { dm ⇒
            dm.name == data._2.name &&
                dm.declaringClassType.toJava == data._2.classFile.thisType.toJava
        }.get

        val tacaiEOptP = ps(data._2, TACAI.key)
        if (tacaiEOptP.hasUBP) {
            if (tacaiEOptP.ub.tac.isEmpty) {
                // No TAC available, e.g., because the method has no body
                return Result(state.entity, StringConstancyProperty.lb)
            } else {
                state.tac = tacaiEOptP.ub.tac.get
            }
        } else {
            state.dependees = tacaiEOptP :: state.dependees
            InterimResult(
                data,
                StringConstancyProperty.lb,
                StringConstancyProperty.ub,
                state.dependees,
                continuation(state)
            )
        }

        val calleesEOptP = ps(dm, Callees.key)
        if (calleesEOptP.hasUBP) {
            state.callees = calleesEOptP.ub
            determinePossibleStrings(state)
        } else {
            state.dependees = calleesEOptP :: state.dependees
            InterimResult(
                data,
                StringConstancyProperty.lb,
                StringConstancyProperty.ub,
                state.dependees,
                continuation(state)
            )
        }
    }

    /**
     * Takes the `data` an analysis was started with as well as a computation `state` and determines
     * the possible string values. This method returns either a final [[Result]] or an
     * [[InterimResult]] depending on whether other information needs to be computed first.
     */
    private def determinePossibleStrings(
        state: InterproceduralComputationState
    ): ProperPropertyComputationResult = {
        val uvar = state.entity._1
        val defSites = uvar.definedBy.toArray.sorted

        val tacProvider = p.get(SimpleTACAIKey)
        if (state.cfg == null) {
            state.cfg = tacProvider(state.entity._2).cfg
        }
        if (state.iHandler == null) {
            state.iHandler = InterproceduralInterpretationHandler(
                state.cfg, ps, declaredMethods, state, continuation(state)
            )
        }

        // In case a parameter is required for approximating a string, retrieve callers information
        // (but only once)
        if (state.params.isEmpty) {
            state.params = InterproceduralStringAnalysis.getParams(state.entity)
        }
        if (state.entity._2.parameterTypes.length > 0 &&
            state.callers == null && state.params.isEmpty) {
            val declaredMethods = project.get(DeclaredMethodsKey).declaredMethods
            val dm = declaredMethods.find { dm ⇒
                // TODO: What is a better / more robust way to compare methods (a check with == did
                //  not produce the expected behavior)?
                dm.name == state.entity._2.name &&
                    dm.declaringClassType.toJava == state.entity._2.classFile.thisType.toJava &&
                    dm.definedMethod.descriptor.parameterTypes.length ==
                        state.entity._2.descriptor.parameterTypes.length
            }.get
            val callersEOptP = ps(dm, CallersProperty.key)
            if (callersEOptP.hasUBP) {
                state.callers = callersEOptP.ub
                if (!registerParams(state, tacProvider)) {
                    return InterimResult(
                        state.entity,
                        StringConstancyProperty.lb,
                        StringConstancyProperty.ub,
                        state.dependees,
                        continuation(state)
                    )
                }
            } else {
                state.dependees = callersEOptP :: state.dependees
                return InterimResult(
                    state.entity,
                    StringConstancyProperty.lb,
                    StringConstancyProperty.ub,
                    state.dependees,
                    continuation(state)
                )
            }
        }

        if (state.parameterDependeesCount > 0) {
            return InterimResult(
                state.entity,
                StringConstancyProperty.lb,
                StringConstancyProperty.ub,
                state.dependees,
                continuation(state)
            )
        } else {
            state.isSetupCompleted = true
        }

        // sci stores the final StringConstancyInformation (if it can be determined now at all)
        var sci = StringConstancyProperty.lb.stringConstancyInformation
        val stmts = state.cfg.code.instructions

        // Interpret a function / method parameter using the parameter information in state
        if (defSites.head < 0) {
            state.computedLeanPath = Path(List(FlatPathElement(defSites.head)))
            val r = state.iHandler.processDefSite(defSites.head, state.params.toList)
            return Result(state.entity, StringConstancyProperty.extractFromPPCR(r))
        }

        val pathFinder: AbstractPathFinder = new WindowPathFinder(state.cfg)
        val call = stmts(defSites.head).asAssignment.expr
        if (InterpretationHandler.isStringBuilderBufferToStringCall(call)) {
            val initDefSites = InterpretationHandler.findDefSiteOfInit(uvar, stmts)
            // initDefSites empty => String{Builder,Buffer} from method parameter is to be evaluated
            if (initDefSites.isEmpty) {
                return Result(state.entity, StringConstancyProperty.lb)
            }

            val paths = pathFinder.findPaths(initDefSites, uvar.definedBy.head)
            state.computedLeanPath = paths.makeLeanPath(uvar, stmts)

            // Find DUVars, that the analysis of the current entity depends on
            val dependentVars = findDependentVars(state.computedLeanPath, stmts, uvar)
            if (dependentVars.nonEmpty) {
                dependentVars.keys.foreach { nextVar ⇒
                    val toAnalyze = (nextVar, state.entity._2)
                    dependentVars.foreach { case (k, v) ⇒ state.var2IndexMapping(k) = v }
                    val ep = propertyStore(toAnalyze, StringConstancyProperty.key)
                    ep match {
                        case FinalP(p) ⇒ return processFinalP(state, ep.e, p)
                        case _         ⇒ state.dependees = ep :: state.dependees
                    }
                }
            } else {
                if (computeResultsForPath(state.computedLeanPath, state)) {
                    sci = new PathTransformer(state.iHandler).pathToStringTree(
                        state.computedLeanPath, state.fpe2sci.toMap
                    ).reduce(true)
                }
            }
        } // If not a call to String{Builder, Buffer}.toString, then we deal with pure strings
        else {
            state.computedLeanPath = if (defSites.length == 1) {
                // Trivial case for just one element
                Path(List(FlatPathElement(defSites.head)))
            } else {
                // For > 1 definition sites, create a nest path element with |defSites| many
                // children where each child is a NestPathElement(FlatPathElement)
                val children = ListBuffer[SubPath]()
                defSites.foreach { ds ⇒
                    children.append(NestedPathElement(ListBuffer(FlatPathElement(ds)), None))
                }
                Path(List(NestedPathElement(children, Some(NestedPathType.CondWithAlternative))))
            }

            if (computeResultsForPath(state.computedLeanPath, state)) {
                sci = new PathTransformer(state.iHandler).pathToStringTree(
                    state.computedLeanPath, state.fpe2sci.toMap
                ).reduce(true)
            }
            // No need to cover the else branch: interimResults.nonEmpty => dependees were added to
            // state.dependees, i.e., the if that checks whether state.dependees is non-empty will
            // always be true (thus, the value of "sci" does not matter)
        }

        if (state.dependees.nonEmpty) {
            InterimResult(
                state.entity,
                StringConstancyProperty.ub,
                StringConstancyProperty.lb,
                state.dependees,
                continuation(state)
            )
        } else {
            InterproceduralStringAnalysis.unregisterParams(state.entity)
            Result(state.entity, StringConstancyProperty(sci))
        }
    }

    /**
     * Continuation function for this analysis.
     *
     * @param state The current computation state. Within this continuation, dependees of the state
     *              might be updated. Furthermore, methods processing this continuation might alter
     *              the state.
     * @return Returns a final result if (already) available. Otherwise, an intermediate result will
     *         be returned.
     */
    private def continuation(
        state: InterproceduralComputationState
    )(eps: SomeEPS): ProperPropertyComputationResult = {
        val inputData = state.entity
        eps.pk match {
            case TACAI.key ⇒ eps match {
                case FinalP(tac: TACAI) ⇒
                    state.tac = tac.tac.get
                    state.dependees = state.dependees.filter(_.e != eps.e)
                    if (state.dependees.isEmpty) {
                        determinePossibleStrings(state)
                    } else {
                        InterimResult(
                            inputData,
                            StringConstancyProperty.lb,
                            StringConstancyProperty.ub,
                            state.dependees,
                            continuation(state)
                        )
                    }
                case InterimLUBP(lb, ub) ⇒ InterimResult(
                    inputData, lb, ub, state.dependees, continuation(state)
                )
                case _ ⇒ throw new IllegalStateException("Neither FinalP nor InterimResult")
            }
            case Callees.key ⇒ eps match {
                case FinalP(callees: Callees) ⇒
                    state.callees = callees
                    state.dependees = state.dependees.filter(_.e != eps.e)
                    if (state.dependees.isEmpty) {
                        determinePossibleStrings(state)
                    } else {
                        InterimResult(
                            inputData,
                            StringConstancyProperty.lb,
                            StringConstancyProperty.ub,
                            state.dependees,
                            continuation(state)
                        )
                    }
                case InterimLUBP(lb, ub) ⇒ InterimResult(
                    inputData, lb, ub, state.dependees, continuation(state)
                )
                case _ ⇒ throw new IllegalStateException("Neither FinalP nor InterimResult")
            }
            case CallersProperty.key ⇒ eps match {
                case FinalP(callers: CallersProperty) ⇒
                    state.callers = callers
                    state.dependees = state.dependees.filter(_.e != eps.e)
                    if (state.dependees.isEmpty) {
                        registerParams(state, p.get(SimpleTACAIKey))
                        determinePossibleStrings(state)
                    } else {
                        InterimResult(
                            inputData,
                            StringConstancyProperty.lb,
                            StringConstancyProperty.ub,
                            state.dependees,
                            continuation(state)
                        )
                    }
                case InterimLUBP(lb, ub) ⇒ InterimResult(
                    inputData, lb, ub, state.dependees, continuation(state)
                )
                case _ ⇒ throw new IllegalStateException("Neither FinalP nor InterimResult")
            }
            case StringConstancyProperty.key ⇒
                eps match {
                    case FinalP(p: StringConstancyProperty) ⇒
                        val resultEntity = eps.e.asInstanceOf[P]
                        // If necessary, update the parameter information with which the
                        // surrounding function / method of the entity was called with
                        if (state.paramResultPositions.contains(resultEntity)) {
                            val pos = state.paramResultPositions(resultEntity)
                            state.params(pos._1)(pos._2) = p.stringConstancyInformation
                            state.paramResultPositions.remove(resultEntity)
                            state.parameterDependeesCount -= 1
                            state.dependees = state.dependees.filter(_.e != eps.e)
                        }

                        // If necessary, update parameter information of function calls
                        if (state.entity2Function.contains(resultEntity)) {
                            state.appendToFpe2Sci(
                                state.var2IndexMapping(resultEntity._1),
                                p.stringConstancyInformation
                            )
                            val func = state.entity2Function(resultEntity)
                            val pos = state.nonFinalFunctionArgsPos(func)(resultEntity)
                            val result = Result(resultEntity, p)
                            state.nonFinalFunctionArgs(func)(pos._1)(pos._2)(pos._3) = result
                            state.entity2Function.remove(resultEntity)
                            // Continue only after all necessary function parameters are evaluated
                            if (state.entity2Function.nonEmpty) {
                                return determinePossibleStrings(state)
                            } else {
                                state.entity2Function.clear()
                                if (!computeResultsForPath(state.computedLeanPath, state)) {
                                    determinePossibleStrings(state)
                                }
                            }
                        }

                        if (state.isSetupCompleted && state.parameterDependeesCount == 0) {
                            processFinalP(state, eps.e, p)
                        } else {
                            determinePossibleStrings(state)
                        }
                    case InterimLUBP(lb, ub) ⇒
                        state.dependees = state.dependees.filter(_.e != eps.e)
                        state.dependees = eps :: state.dependees
                        InterimResult(
                            inputData, lb, ub, state.dependees, continuation(state)
                        )
                    case _ ⇒ throw new IllegalStateException("Neither FinalP nor InterimResult")
                }
        }
    }

    private def finalizePreparations(
        path:     Path,
        state:    InterproceduralComputationState,
        iHandler: InterproceduralInterpretationHandler
    ): Unit = path.elements.foreach {
        case FlatPathElement(index) ⇒
            if (!state.fpe2sci.contains(index)) {
                iHandler.finalizeDefSite(index, state)
            }
        case npe: NestedPathElement ⇒
            finalizePreparations(Path(npe.element.toList), state, iHandler)
        case _ ⇒
    }

    /**
     * computeFinalResult computes the final result of an analysis. This includes the computation
     * of instruction that could only be prepared (e.g., if an array load included a method call,
     * its final result is not yet ready, however, this function finalizes, e.g., that load).
     *
     * @param state The final computation state. For this state the following criteria must apply:
     *              For each [[FlatPathElement]], there must be a corresponding entry in
     *              `state.fpe2sci`. If this criteria is not met, a [[NullPointerException]] will
     *              be thrown (in this case there was some work to do left and this method should
     *              not have been called)!
     * @return Returns the final result.
     */
    private def computeFinalResult(
        state:    InterproceduralComputationState,
    ): Result = {
        finalizePreparations(state.computedLeanPath, state, state.iHandler)
        val finalSci = new PathTransformer(null).pathToStringTree(
            state.computedLeanPath, state.fpe2sci.toMap, resetExprHandler = false
        ).reduce(true)
        InterproceduralStringAnalysis.unregisterParams(state.entity)
        Result(state.entity, StringConstancyProperty(finalSci))
    }

    /**
     * `processFinalP` is responsible for handling the case that the `propertyStore` outputs a
     * [[org.opalj.fpcf.FinalP]].
     */
    private def processFinalP(
        state: InterproceduralComputationState,
        e:     Entity,
        p:     Property
    ): ProperPropertyComputationResult = {
        // Add mapping information (which will be used for computing the final result)
        val retrievedProperty = p.asInstanceOf[StringConstancyProperty]
        val currentSci = retrievedProperty.stringConstancyInformation
        state.appendToFpe2Sci(state.var2IndexMapping(e.asInstanceOf[P]._1), currentSci)

        // No more dependees => Return the result for this analysis run
        state.dependees = state.dependees.filter(_.e != e)
        if (state.dependees.isEmpty) {
            computeFinalResult(state)
        } else {
            InterimResult(
                state.entity,
                StringConstancyProperty.ub,
                StringConstancyProperty.lb,
                state.dependees,
                continuation(state)
            )
        }
    }

    /**
     * This method takes a computation state, `state` as well as a TAC provider, `tacProvider`, and
     * determines the interpretations of all parameters of the method under analysis. These
     * interpretations are registered using [[InterproceduralStringAnalysis.registerParams]].
     */
    private def registerParams(
        state: InterproceduralComputationState,
        tacProvider: Method ⇒ TACode[TACMethodParameter, DUVar[ValueInformation]]
    ): Boolean = {
        var hasIntermediateResult = false
        state.callers.callers(declaredMethods).toSeq.zipWithIndex.foreach {
            case ((m, pc), methodIndex) ⇒
                val tac = propertyStore(m.definedMethod, TACAI.key).ub.tac.get
                val params = tac.stmts(tac.pcToIndex(pc)) match {
                    case Assignment(_, _, fc: FunctionCall[V]) ⇒ fc.params
                    case Assignment(_, _, mc: MethodCall[V]) ⇒ mc.params
                    case ExprStmt(_, fc: FunctionCall[V]) ⇒ fc.params
                    case ExprStmt(_, fc: MethodCall[V]) ⇒ fc.params
                    case mc: MethodCall[V] ⇒ mc.params
                    case _ ⇒ List()
                }
                params.zipWithIndex.foreach { case (p, paramIndex) ⇒
                    // Add an element to the params list (we do it here because we know how many
                    // parameters there are)
                    if (state.params.length <= methodIndex) {
                        state.params.append(params.indices.map(_ ⇒
                            StringConstancyInformation.getNeutralElement
                        ).to[ListBuffer])
                    }
                    // Recursively analyze supported types
                    if (InterproceduralStringAnalysis.isSupportedType(p.asVar)) {
                        val paramEntity = (p.asVar, m.definedMethod)
                        val eps = propertyStore(paramEntity, StringConstancyProperty.key)
                        state.var2IndexMapping(paramEntity._1) = paramIndex
                        eps match {
                            case FinalP(r) ⇒
                                state.params(methodIndex)(paramIndex) = r.stringConstancyInformation
                            case _ ⇒
                                state.dependees = eps :: state.dependees
                                hasIntermediateResult = true
                                state.paramResultPositions(paramEntity) = (methodIndex, paramIndex)
                                state.parameterDependeesCount += 1
                        }
                    } else {
                        state.params(methodIndex)(paramIndex) =
                            StringConstancyProperty.lb.stringConstancyInformation
                    }

                }
        }
        // If all parameters could already be determined, register them
        if (!hasIntermediateResult) {
            InterproceduralStringAnalysis.registerParams(state.entity, state.params)
        }
        !hasIntermediateResult
    }

    /**
     * This function traverses the given path, computes all string values along the path and stores
     * these information in the given state.
     *
     * @param p The path to traverse.
     * @param state The current state of the computation. This function will alter
     *              [[InterproceduralComputationState.fpe2sci]].
     * @return Returns `true` if all values computed for the path are final results.
     */
    private def computeResultsForPath(
        p:        Path,
        state:    InterproceduralComputationState
    ): Boolean = {
        var hasFinalResult = true

        p.elements.foreach {
            case FlatPathElement(index) ⇒
                if (!state.fpe2sci.contains(index)) {
                    state.iHandler.processDefSite(index, state.params.toList) match {
                        case r: Result ⇒ state.appendResultToFpe2Sci(index, r, reset = true)
                        case _         ⇒ hasFinalResult = false
                    }
                }
            case npe: NestedPathElement ⇒
                val subFinalResult = computeResultsForPath(
                    Path(npe.element.toList), state
                )
                if (hasFinalResult) {
                    hasFinalResult = subFinalResult
                }
            case _ ⇒
        }

        hasFinalResult
    }

    /**
     * Helper / accumulator function for finding dependees. For how dependees are detected, see
     * findDependentVars. Returns a list of pairs of DUVar and the index of the
     * FlatPathElement.element in which it occurs.
     */
    private def findDependeesAcc(
        subpath:           SubPath,
        stmts:             Array[Stmt[V]],
        target:            V,
        foundDependees:    ListBuffer[(V, Int)],
        hasTargetBeenSeen: Boolean
    ): (ListBuffer[(V, Int)], Boolean) = {
        var encounteredTarget = false
        subpath match {
            case fpe: FlatPathElement ⇒
                if (target.definedBy.contains(fpe.element)) {
                    encounteredTarget = true
                }
                // For FlatPathElements, search for DUVars on which the toString method is called
                // and where these toString calls are the parameter of an append call
                stmts(fpe.element) match {
                    case ExprStmt(_, outerExpr) ⇒
                        if (InterpretationHandler.isStringBuilderBufferAppendCall(outerExpr)) {
                            val param = outerExpr.asVirtualFunctionCall.params.head.asVar
                            param.definedBy.filter(_ >= 0).foreach { ds ⇒
                                val expr = stmts(ds).asAssignment.expr
                                if (InterpretationHandler.isStringBuilderBufferToStringCall(expr)) {
                                    foundDependees.append((
                                        outerExpr.asVirtualFunctionCall.params.head.asVar,
                                        fpe.element
                                    ))
                                }
                            }
                        }
                    case _ ⇒
                }
                (foundDependees, encounteredTarget)
            case npe: NestedPathElement ⇒
                npe.element.foreach { nextSubpath ⇒
                    if (!encounteredTarget) {
                        val (_, seen) = findDependeesAcc(
                            nextSubpath, stmts, target, foundDependees, encounteredTarget
                        )
                        encounteredTarget = seen
                    }
                }
                (foundDependees, encounteredTarget)
            case _ ⇒ (foundDependees, encounteredTarget)
        }
    }

    /**
     * Takes a `path`, this should be the lean path of a [[Path]], as well as a context in the form
     * of statements, `stmts`, and detects all dependees within `path`. Dependees are found by
     * looking at all elements in the path, and check whether the argument of an `append` call is a
     * value that stems from a `toString` call of a [[StringBuilder]] or [[StringBuffer]]. This
     * function then returns the found UVars along with the indices of those append statements.
     *
     * @note In order to make sure that a [[org.opalj.tac.DUVar]] does not depend on itself, pass
     *       this variable as `ignore`.
     */
    private def findDependentVars(
        path: Path, stmts: Array[Stmt[V]], ignore: V
    ): mutable.LinkedHashMap[V, Int] = {
        val dependees = mutable.LinkedHashMap[V, Int]()
        val ignoreNews = InterpretationHandler.findNewOfVar(ignore, stmts)
        var wasTargetSeen = false

        path.elements.foreach { nextSubpath ⇒
            if (!wasTargetSeen) {
                val (currentDeps, encounteredTarget) = findDependeesAcc(
                    nextSubpath, stmts, ignore, ListBuffer(), hasTargetBeenSeen = false
                )
                wasTargetSeen = encounteredTarget
                currentDeps.foreach { nextPair ⇒
                    val newExpressions = InterpretationHandler.findNewOfVar(nextPair._1, stmts)
                    if (ignore != nextPair._1 && ignoreNews != newExpressions) {
                        dependees.put(nextPair._1, nextPair._2)
                    }
                }
            }
        }
        dependees
    }

}

object InterproceduralStringAnalysis {

    /**
     * Maps entities to a list of lists of parameters. As currently this analysis works context-
     * insensitive, we have a list of lists to capture all parameters of all potential method /
     * function calls.
     */
    private val paramInfos = mutable.Map[Entity, ListBuffer[ListBuffer[StringConstancyInformation]]]()

    def registerParams(e: Entity, scis: ListBuffer[ListBuffer[StringConstancyInformation]]): Unit = {
        if (!paramInfos.contains(e)) {
            paramInfos(e) = ListBuffer(scis: _*)
        } else {
            paramInfos(e).appendAll(scis)
        }
    }

    def unregisterParams(e: Entity): Unit = paramInfos.remove(e)

    def getParams(e: Entity): ListBuffer[ListBuffer[StringConstancyInformation]] =
        if (paramInfos.contains(e)) {
            paramInfos(e)
        } else {
            ListBuffer()
        }

    /**
     * Checks whether a value of some type is supported by the [[InterproceduralStringAnalysis]].
     * Currently supported types are, java.lang.String and the primitive types short, int, float,
     * and double.
     *
     * @param v The value to check if it is supported.
     * @return Returns `true` if `v` is a supported type and `false` otherwise.
     */
    def isSupportedType(v: V): Boolean =
        if (v.value.isPrimitiveValue) {
            val primTypeName = v.value.asPrimitiveValue.primitiveType.toJava
            primTypeName == "short" || primTypeName == "int" || primTypeName == "float" ||
                primTypeName == "double"
        } else {
            try {
                v.value.verificationTypeInfo.asObjectVariableInfo.clazz.toJava == "java.lang.String"
            } catch {
                case _: Exception ⇒ false
            }
        }

}

sealed trait InterproceduralStringAnalysisScheduler extends FPCFAnalysisScheduler {

    final def derivedProperty: PropertyBounds = PropertyBounds.lub(StringConstancyProperty)

    final override def uses: Set[PropertyBounds] = Set(
        PropertyBounds.ub(TACAI),
        PropertyBounds.ub(Callees),
        PropertyBounds.lub(StringConstancyProperty)
    )

    final override type InitializationData = InterproceduralStringAnalysis
    final override def init(p: SomeProject, ps: PropertyStore): InitializationData = {
        new InterproceduralStringAnalysis(p)
    }

    override def beforeSchedule(p: SomeProject, ps: PropertyStore): Unit = {}

    override def afterPhaseScheduling(ps: PropertyStore, analysis: FPCFAnalysis): Unit = {}

    override def afterPhaseCompletion(
        p:        SomeProject,
        ps:       PropertyStore,
        analysis: FPCFAnalysis
    ): Unit = {}

}

/**
 * Executor for the lazy analysis.
 */
object LazyInterproceduralStringAnalysis
        extends InterproceduralStringAnalysisScheduler with FPCFLazyAnalysisScheduler {

    override def register(
        p: SomeProject, ps: PropertyStore, analysis: InitializationData
    ): FPCFAnalysis = {
        val analysis = new InterproceduralStringAnalysis(p)
        ps.registerLazyPropertyComputation(StringConstancyProperty.key, analysis.analyze)
        analysis
    }

    override def derivesLazily: Some[PropertyBounds] = Some(derivedProperty)

}
