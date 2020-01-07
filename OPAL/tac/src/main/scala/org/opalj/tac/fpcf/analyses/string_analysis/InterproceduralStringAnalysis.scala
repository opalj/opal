/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.string_analysis

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

import org.opalj.fpcf.Entity
import org.opalj.fpcf.FinalEP
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
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.br.fpcf.FPCFLazyAnalysisScheduler
import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.br.FieldType
import org.opalj.br.analyses.FieldAccessInformationKey
import org.opalj.br.fpcf.properties.cg.Callees
import org.opalj.br.fpcf.properties.cg.Callers
import org.opalj.br.fpcf.properties.string_definition.StringConstancyLevel
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
import org.opalj.tac.fpcf.analyses.string_analysis.preprocessing.SubPath
import org.opalj.tac.Assignment
import org.opalj.tac.DUVar
import org.opalj.tac.FunctionCall
import org.opalj.tac.MethodCall
import org.opalj.tac.TACMethodParameter
import org.opalj.tac.TACode
import org.opalj.tac.fpcf.analyses.string_analysis.preprocessing.NestedPathType
import org.opalj.tac.ArrayLoad
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.interprocedural.ArrayLoadPreparer
import org.opalj.tac.BinaryExpr
import org.opalj.tac.Expr

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

    // TODO: Is it possible to make the following two parameters configurable from the outside?

    /**
     * To analyze an expression within a method ''m'', callers information might be necessary, e.g.,
     * to know with which arguments ''m'' is called. [[callersThreshold]] determines the threshold
     * up to which number of callers parameter information are gathered. For "number of callers
     * greater than [[callersThreshold]]", parameters are approximated with the lower bound.
     */
    private val callersThreshold = 10

    /**
     * To analyze a read operation of field, ''f'', all write accesses, ''wa_f'', to ''f'' have to
     * be analyzed. ''fieldWriteThreshold'' determines the threshold of ''|wa_f|'' when ''f'' is to
     * be approximated as the lower bound, i.e., ''|wa_f|'' is greater than ''fieldWriteThreshold''
     * then the read operation of ''f'' is approximated as the lower bound. Otherwise, if ''|wa_f|''
     * is less or equal than ''fieldWriteThreshold'', analyze all ''wa_f'' to approximate the read
     * of ''f''.
     */
    private val fieldWriteThreshold = 100
    private val declaredMethods = project.get(DeclaredMethodsKey)
    private final val fieldAccessInformation = project.get(FieldAccessInformationKey)

    /**
     * Returns the current interim result for the given state. If required, custom lower and upper
     * bounds can be used for the interim result.
     */
    private def getInterimResult(
        state: InterproceduralComputationState
    ): InterimResult[StringConstancyProperty] = InterimResult(
        state.entity,
        computeNewLowerBound(state),
        computeNewUpperBound(state),
        state.dependees,
        continuation(state)
    )

    private def computeNewUpperBound(
        state: InterproceduralComputationState
    ): StringConstancyProperty = {
        if (state.computedLeanPath != null) {
            StringConstancyProperty(new PathTransformer(state.interimIHandler).pathToStringTree(
                state.computedLeanPath, state.interimFpe2sci
            ).reduce(true))
        } else {
            StringConstancyProperty.lb
        }
    }

    private def computeNewLowerBound(
        state: InterproceduralComputationState
    ): StringConstancyProperty = StringConstancyProperty.lb

    def analyze(data: P): ProperPropertyComputationResult = {
        val state = InterproceduralComputationState(data, fieldWriteThreshold)
        val dm = declaredMethods(data._2)

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
        }

        val calleesEOptP = ps(dm, Callees.key)
        if (calleesEOptP.hasUBP) {
            state.callees = calleesEOptP.ub
            determinePossibleStrings(state)
        } else {
            state.dependees = calleesEOptP :: state.dependees
            getInterimResult(state)
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
        val stmts = state.tac.stmts

        if (state.tac == null || state.callees == null) {
            return getInterimResult(state)
        }

        if (state.computedLeanPath == null) {
            state.computedLeanPath = computeLeanPath(uvar, state.tac)
        }

        if (state.iHandler == null) {
            state.iHandler = InterproceduralInterpretationHandler(
                state.tac, ps, declaredMethods, fieldAccessInformation, state
            )
            val interimState = state.copy()
            interimState.tac = state.tac
            interimState.computedLeanPath = state.computedLeanPath
            interimState.callees = state.callees
            interimState.callers = state.callers
            interimState.params = state.params
            state.interimIHandler = InterproceduralInterpretationHandler(
                state.tac, ps, declaredMethods, fieldAccessInformation, interimState
            )
        }

        var requiresCallersInfo = false
        if (state.params.isEmpty) {
            state.params = InterproceduralStringAnalysis.getParams(state.entity)
        }
        if (state.params.isEmpty) {
            // In case a parameter is required for approximating a string, retrieve callers information
            // (but only once and only if the expressions is not a local string)
            val hasCallersOrParamInfo = state.callers == null && state.params.isEmpty
            requiresCallersInfo = if (defSites.exists(_ < 0)) {
                if (InterpretationHandler.isStringConstExpression(uvar)) {
                    hasCallersOrParamInfo
                } else if (InterproceduralStringAnalysis.isSupportedPrimitiveNumberType(uvar)) {
                    val numType = uvar.value.asPrimitiveValue.primitiveType.toJava
                    val sci = InterproceduralStringAnalysis.
                        getDynamicStringInformationForNumberType(numType)
                    return Result(state.entity, StringConstancyProperty(sci))
                } else {
                    // StringBuilders as parameters are currently not evaluated
                    return Result(state.entity, StringConstancyProperty.lb)
                }
            } else {
                val call = stmts(defSites.head).asAssignment.expr
                if (InterpretationHandler.isStringBuilderBufferToStringCall(call)) {
                    val (_, hasInitDefSites) = computeLeanPathForStringBuilder(uvar, state.tac)
                    if (!hasInitDefSites) {
                        return Result(state.entity, StringConstancyProperty.lb)
                    }
                    val hasSupportedParamType = state.entity._2.parameterTypes.exists {
                        InterproceduralStringAnalysis.isSupportedType
                    }
                    if (hasSupportedParamType) {
                        hasParamUsageAlongPath(state.computedLeanPath, state.tac.stmts)
                    } else {
                        !hasCallersOrParamInfo
                    }
                } else {
                    !hasCallersOrParamInfo
                }
            }
        }

        if (requiresCallersInfo) {
            val dm = declaredMethods(state.entity._2)
            val callersEOptP = ps(dm, Callers.key)
            if (callersEOptP.hasUBP) {
                state.callers = callersEOptP.ub
                if (!registerParams(state)) {
                    return getInterimResult(state)
                }
            } else {
                state.dependees = callersEOptP :: state.dependees
                return getInterimResult(state)
            }
        }

        if (state.parameterDependeesCount > 0) {
            return getInterimResult(state)
        } else {
            state.isSetupCompleted = true
        }

        // sci stores the final StringConstancyInformation (if it can be determined now at all)
        var sci = StringConstancyProperty.lb.stringConstancyInformation
        // Interpret a function / method parameter using the parameter information in state
        if (defSites.head < 0) {
            val r = state.iHandler.processDefSite(defSites.head, state.params.toList)
            val sci = r.asFinal.p.asInstanceOf[StringConstancyProperty].stringConstancyInformation
            return Result(state.entity, StringConstancyProperty(sci))
        }

        val call = stmts(defSites.head).asAssignment.expr
        var attemptFinalResultComputation = false
        if (InterpretationHandler.isStringBuilderBufferToStringCall(call)) {
            // Find DUVars, that the analysis of the current entity depends on
            val dependentVars = findDependentVars(state.computedLeanPath, stmts, uvar)
            if (dependentVars.nonEmpty) {
                dependentVars.keys.foreach { nextVar ⇒
                    val toAnalyze = (nextVar, state.entity._2)
                    dependentVars.foreach { case (k, v) ⇒ state.appendToVar2IndexMapping(k, v) }
                    val ep = propertyStore(toAnalyze, StringConstancyProperty.key)
                    ep match {
                        case FinalP(p) ⇒ return processFinalP(state, ep.e, p)
                        case _         ⇒ state.dependees = ep :: state.dependees
                    }
                }
            } else {
                attemptFinalResultComputation = true
            }
        } // If not a call to String{Builder, Buffer}.toString, then we deal with pure strings
        else {
            attemptFinalResultComputation = true
        }

        if (attemptFinalResultComputation) {
            if (state.dependees.isEmpty && computeResultsForPath(state.computedLeanPath, state)) {
                // Check whether we deal with the empty string; it requires special treatment as the
                // PathTransformer#pathToStringTree would not handle it correctly (as
                // PathTransformer#pathToStringTree is involved in a mutual recursion)
                val isEmptyString = if (state.computedLeanPath.elements.length == 1) {
                    state.computedLeanPath.elements.head match {
                        case FlatPathElement(i) ⇒
                            state.fpe2sci.contains(i) && state.fpe2sci(i).length == 1 &&
                                state.fpe2sci(i).head == StringConstancyInformation.getNeutralElement
                        case _ ⇒ false
                    }
                } else false

                sci = if (isEmptyString) {
                    StringConstancyInformation.getNeutralElement
                } else {
                    new PathTransformer(state.iHandler).pathToStringTree(
                        state.computedLeanPath, state.fpe2sci
                    ).reduce(true)
                }
            }
        }

        if (state.dependees.nonEmpty) {
            getInterimResult(state)
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
        state.dependees = state.dependees.filter(_.e != eps.e)
        eps.pk match {
            case TACAI.key ⇒ eps match {
                case FinalP(tac: TACAI) ⇒
                    // Set the TAC only once (the TAC might be requested for other methods, so this
                    // makes sure we do not overwrite the state's TAC)
                    if (state.tac == null) {
                        state.tac = tac.tac.get
                    }
                    determinePossibleStrings(state)
                case _ ⇒
                    state.dependees = eps :: state.dependees
                    getInterimResult(state)
            }
            case Callees.key ⇒ eps match {
                case FinalP(callees: Callees) ⇒
                    state.callees = callees
                    if (state.dependees.isEmpty) {
                        determinePossibleStrings(state)
                    } else {
                        getInterimResult(state)
                    }
                case _ ⇒
                    state.dependees = eps :: state.dependees
                    getInterimResult(state)
            }
            case Callers.key ⇒ eps match {
                case FinalP(callers: Callers) ⇒
                    state.callers = callers
                    if (state.dependees.isEmpty) {
                        registerParams(state)
                        determinePossibleStrings(state)
                    } else {
                        getInterimResult(state)
                    }
                case _ ⇒
                    state.dependees = eps :: state.dependees
                    getInterimResult(state)
            }
            case StringConstancyProperty.key ⇒
                eps match {
                    case FinalEP(entity, p: StringConstancyProperty) ⇒
                        val e = entity.asInstanceOf[P]
                        // For updating the interim state
                        state.var2IndexMapping(eps.e.asInstanceOf[P]._1).foreach { i ⇒
                            state.appendToInterimFpe2Sci(i, p.stringConstancyInformation)
                        }
                        // If necessary, update the parameter information with which the
                        // surrounding function / method of the entity was called with
                        if (state.paramResultPositions.contains(e)) {
                            val pos = state.paramResultPositions(e)
                            state.params(pos._1)(pos._2) = p.stringConstancyInformation
                            state.paramResultPositions.remove(e)
                            state.parameterDependeesCount -= 1
                        }

                        // If necessary, update parameter information of function calls
                        if (state.entity2Function.contains(e)) {
                            state.var2IndexMapping(e._1).foreach(state.appendToFpe2Sci(
                                _, p.stringConstancyInformation
                            ))
                            // Update the state
                            state.entity2Function(e).foreach { f ⇒
                                val pos = state.nonFinalFunctionArgsPos(f)(e)
                                val finalEp = FinalEP(e, p)
                                state.nonFinalFunctionArgs(f)(pos._1)(pos._2)(pos._3) = finalEp
                                // Housekeeping
                                val index = state.entity2Function(e).indexOf(f)
                                state.entity2Function(e).remove(index)
                                if (state.entity2Function(e).isEmpty) {
                                    state.entity2Function.remove(e)
                                }
                            }
                            // Continue only after all necessary function parameters are evaluated
                            if (state.entity2Function.nonEmpty) {
                                return getInterimResult(state)
                            } else {
                                // We could try to determine a final result before all function
                                // parameter information are available, however, this will
                                // definitely result in finding some intermediate result. Thus,
                                // defer this computations when we know that all necessary
                                // information are available
                                state.entity2Function.clear()
                                if (!computeResultsForPath(state.computedLeanPath, state)) {
                                    return determinePossibleStrings(state)
                                }
                            }
                        }

                        if (state.isSetupCompleted && state.parameterDependeesCount == 0) {
                            processFinalP(state, eps.e, p)
                        } else {
                            determinePossibleStrings(state)
                        }
                    case InterimLUBP(_: StringConstancyProperty, ub: StringConstancyProperty) ⇒
                        state.dependees = eps :: state.dependees
                        val uvar = eps.e.asInstanceOf[P]._1
                        state.var2IndexMapping(uvar).foreach { i ⇒
                            state.appendToInterimFpe2Sci(
                                i, ub.stringConstancyInformation, Some(uvar)
                            )
                        }
                        getInterimResult(state)
                    case _ ⇒
                        state.dependees = eps :: state.dependees
                        getInterimResult(state)
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
    private def computeFinalResult(state: InterproceduralComputationState): Result = {
        finalizePreparations(state.computedLeanPath, state, state.iHandler)
        val finalSci = new PathTransformer(state.iHandler).pathToStringTree(
            state.computedLeanPath, state.fpe2sci, resetExprHandler = false
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
        state.var2IndexMapping(e.asInstanceOf[P]._1).foreach {
            state.appendToFpe2Sci(_, currentSci)
        }

        state.dependees = state.dependees.filter(_.e != e)
        // No more dependees => Return the result for this analysis run
        if (state.dependees.isEmpty) {
            computeFinalResult(state)
        } else {
            getInterimResult(state)
        }
    }

    /**
     * This method takes a computation state, `state` as well as a TAC provider, `tacProvider`, and
     * determines the interpretations of all parameters of the method under analysis. These
     * interpretations are registered using [[InterproceduralStringAnalysis.registerParams]].
     * The return value of this function indicates whether a the parameter evaluation is done
     * (`true`) or not yet (`false`).
     */
    private def registerParams(
        state: InterproceduralComputationState
    ): Boolean = {
        val callers = state.callers.callers(declaredMethods).toSeq
        if (callers.length > callersThreshold) {
            state.params.append(
                state.entity._2.parameterTypes.map {
                    _: FieldType ⇒ StringConstancyInformation.lb
                }.to[ListBuffer]
            )
            return false
        }

        var hasIntermediateResult = false
        callers.zipWithIndex.foreach {
            case ((m, pc, _), methodIndex) ⇒
                val tac = propertyStore(m.definedMethod, TACAI.key).ub.tac.get
                val params = tac.stmts(tac.pcToIndex(pc)) match {
                    case Assignment(_, _, fc: FunctionCall[V]) ⇒ fc.params
                    case Assignment(_, _, mc: MethodCall[V])   ⇒ mc.params
                    case ExprStmt(_, fc: FunctionCall[V])      ⇒ fc.params
                    case ExprStmt(_, fc: MethodCall[V])        ⇒ fc.params
                    case mc: MethodCall[V]                     ⇒ mc.params
                    case _                                     ⇒ List()
                }
                params.zipWithIndex.foreach {
                    case (p, paramIndex) ⇒
                        // Add an element to the params list (we do it here because we know how many
                        // parameters there are)
                        if (state.params.length <= methodIndex) {
                            state.params.append(params.indices.map(_ ⇒
                                StringConstancyInformation.getNeutralElement).to[ListBuffer])
                        }
                        // Recursively analyze supported types
                        if (InterproceduralStringAnalysis.isSupportedType(p.asVar)) {
                            val paramEntity = (p.asVar, m.definedMethod)
                            val eps = propertyStore(paramEntity, StringConstancyProperty.key)
                            state.appendToVar2IndexMapping(paramEntity._1, paramIndex)
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
     * @param p     The path to traverse.
     * @param state The current state of the computation. This function will alter
     *              [[InterproceduralComputationState.fpe2sci]].
     * @return Returns `true` if all values computed for the path are final results.
     */
    private def computeResultsForPath(
        p:     Path,
        state: InterproceduralComputationState
    ): Boolean = {
        var hasFinalResult = true

        p.elements.foreach {
            case FlatPathElement(index) ⇒
                if (!state.fpe2sci.contains(index)) {
                    val eOptP = state.iHandler.processDefSite(index, state.params.toList)
                    if (eOptP.isFinal) {
                        val p = eOptP.asFinal.p.asInstanceOf[StringConstancyProperty]
                        state.appendToFpe2Sci(index, p.stringConstancyInformation, reset = true)
                    } else {
                        hasFinalResult = false
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
     * This function is a wrapper function for [[computeLeanPathForStringConst]] and
     * [[computeLeanPathForStringBuilder]].
     */
    private def computeLeanPath(
        duvar: V, tac: TACode[TACMethodParameter, DUVar[ValueInformation]]
    ): Path = {
        val defSites = duvar.definedBy.toArray.sorted
        if (defSites.head < 0) {
            computeLeanPathForStringConst(duvar)
        } else {
            val call = tac.stmts(defSites.head).asAssignment.expr
            if (InterpretationHandler.isStringBuilderBufferToStringCall(call)) {
                val (leanPath, _) = computeLeanPathForStringBuilder(duvar, tac)
                leanPath
            } else {
                computeLeanPathForStringConst(duvar)
            }
        }
    }

    /**
     * This function computes the lean path for a [[DUVar]] which is required to be a string
     * expressions.
     */
    private def computeLeanPathForStringConst(duvar: V): Path = {
        val defSites = duvar.definedBy.toArray.sorted
        if (defSites.length == 1) {
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
    }

    /**
     * This function computes the lean path for a [[DUVar]] which is required to stem from a
     * `String{Builder, Buffer}#toString()` call. For this, the `tac` of the method, in which
     * `duvar` resides, is required.
     * This function then returns a pair of values: The first value is the computed lean path and
     * the second value indicates whether the String{Builder, Buffer} has initialization sites
     * within the method stored in `tac`. If it has no initialization sites, it returns
     * `(null, false)` and otherwise `(computed lean path, true)`.
     */
    private def computeLeanPathForStringBuilder(
        duvar: V, tac: TACode[TACMethodParameter, DUVar[ValueInformation]]
    ): (Path, Boolean) = {
        val pathFinder: AbstractPathFinder = new WindowPathFinder(tac.cfg)
        val initDefSites = InterpretationHandler.findDefSiteOfInit(duvar, tac.stmts)
        if (initDefSites.isEmpty) {
            (null, false)
        } else {
            val paths = pathFinder.findPaths(initDefSites, duvar.definedBy.toArray.max)
            (paths.makeLeanPath(duvar, tac.stmts), true)
        }
    }

    private def hasParamUsageAlongPath(path: Path, stmts: Array[Stmt[V]]): Boolean = {
        def hasExprParamUsage(expr: Expr[V]): Boolean = expr match {
            case al: ArrayLoad[V] ⇒
                ArrayLoadPreparer.getStoreAndLoadDefSites(al, stmts).exists(_ < 0)
            case duvar: V            ⇒ duvar.definedBy.exists(_ < 0)
            case fc: FunctionCall[V] ⇒ fc.params.exists(hasExprParamUsage)
            case mc: MethodCall[V]   ⇒ mc.params.exists(hasExprParamUsage)
            case be: BinaryExpr[V]   ⇒ hasExprParamUsage(be.left) || hasExprParamUsage(be.right)
            case _                   ⇒ false
        }

        path.elements.exists {
            case FlatPathElement(index) ⇒ stmts(index) match {
                case Assignment(_, _, expr) ⇒ hasExprParamUsage(expr)
                case ExprStmt(_, expr)      ⇒ hasExprParamUsage(expr)
                case _                      ⇒ false
            }
            case NestedPathElement(subPath, _) ⇒ hasParamUsageAlongPath(Path(subPath.toList), stmts)
            case _                             ⇒ false
        }
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
     * This function checks whether a given type is a supported primitive type. Supported currently
     * means short, int, float, or double.
     */
    def isSupportedPrimitiveNumberType(v: V): Boolean = {
        val value = v.value
        if (value.isPrimitiveValue) {
            isSupportedPrimitiveNumberType(value.asPrimitiveValue.primitiveType.toJava)
        } else {
            false
        }
    }

    /**
     * This function checks whether a given type is a supported primitive type. Supported currently
     * means short, int, float, or double.
     */
    def isSupportedPrimitiveNumberType(typeName: String): Boolean =
        typeName == "short" || typeName == "int" || typeName == "float" || typeName == "double"

    /**
     * Checks whether a given type, identified by its string representation, is supported by the
     * string analysis. That means, if this function returns `true`, a value, which is of type
     * `typeName` may be approximated by the string analysis better than just the lower bound.
     *
     * @param typeName The name of the type to check. May either be the name of a primitive type or
     *                 a fully-qualified class name (dot-separated).
     * @return Returns `true`, if `typeName` is an element in [char, short, int, float, double,
     *         java.lang.String] and `false` otherwise.
     */
    def isSupportedType(typeName: String): Boolean =
        typeName == "char" || isSupportedPrimitiveNumberType(typeName) ||
            typeName == "java.lang.String" || typeName == "java.lang.String[]"

    /**
     * Determines whether a given [[V]] element ([[DUVar]]) is supported by the string analysis.
     *
     * @param v The element to check.
     * @return Returns true if the given [[FieldType]] is of a supported type. For supported types,
     *         see [[InterproceduralStringAnalysis.isSupportedType(String)]].
     */
    def isSupportedType(v: V): Boolean =
        if (v.value.isPrimitiveValue) {
            isSupportedType(v.value.asPrimitiveValue.primitiveType.toJava)
        } else {
            try {
                isSupportedType(v.value.verificationTypeInfo.asObjectVariableInfo.clazz.toJava)
            } catch {
                case _: Exception ⇒ false
            }
        }

    /**
     * Determines whether a given [[FieldType]] element is supported by the string analysis.
     *
     * @param fieldType The element to check.
     * @return Returns true if the given [[FieldType]] is of a supported type. For supported types,
     *         see [[InterproceduralStringAnalysis.isSupportedType(String)]].
     */
    def isSupportedType(fieldType: FieldType): Boolean = isSupportedType(fieldType.toJava)

    /**
     * Takes the name of a primitive number type - supported types are short, int, float, double -
     * and returns the dynamic [[StringConstancyInformation]] for that type. In case an unsupported
     * type is given [[StringConstancyInformation.UnknownWordSymbol]] is returned as possible
     * strings.
     */
    def getDynamicStringInformationForNumberType(
        numberType: String
    ): StringConstancyInformation = {
        val possibleStrings = numberType match {
            case "short" | "int"    ⇒ StringConstancyInformation.IntValue
            case "float" | "double" ⇒ StringConstancyInformation.FloatValue
            case _                  ⇒ StringConstancyInformation.UnknownWordSymbol
        }
        StringConstancyInformation(StringConstancyLevel.DYNAMIC, possibleStrings = possibleStrings)
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
