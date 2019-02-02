/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.string_analysis

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.InterimLUBP
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Result
import org.opalj.fpcf.SomeEPS
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.SomeProject
import org.opalj.br.cfg.CFG
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.br.fpcf.FPCFLazyAnalysisScheduler
import org.opalj.br.fpcf.cg.properties.Callees
import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.br.DeclaredMethod
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.tac.Stmt
import org.opalj.tac.TACStmts
import org.opalj.tac.fpcf.analyses.string_analysis.preprocessing.Path
import org.opalj.tac.fpcf.analyses.string_analysis.preprocessing.PathTransformer
import org.opalj.tac.fpcf.properties.TACAI
import org.opalj.tac.SimpleTACAIKey
import org.opalj.tac.fpcf.analyses.string_analysis.preprocessing.AbstractPathFinder
import org.opalj.tac.fpcf.analyses.string_analysis.preprocessing.NestedPathElement
import org.opalj.tac.fpcf.analyses.string_analysis.preprocessing.WindowPathFinder
import org.opalj.tac.ExprStmt
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.InterpretationHandler
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.InterproceduralInterpretationHandler
import org.opalj.tac.fpcf.analyses.string_analysis.preprocessing.FlatPathElement
import org.opalj.tac.fpcf.analyses.string_analysis.preprocessing.SubPath

/**
 * This class is to be used to store state information that are required at a later point in
 * time during the analysis, e.g., due to the fact that another analysis had to be triggered to
 * have all required information ready for a final result.
 */
case class ComputationState(
        // The lean path that was computed
        var computedLeanPath: Option[Path],
        // The control flow graph on which the computedLeanPath is based
        cfg: CFG[Stmt[V], TACStmts[V]],
        //
        var callees: Option[Callees] = None
) {
    // If not empty, this very routine can only produce an intermediate result
    // TODO: The value must be a list as one entity can have multiple dependees!
    val dependees: mutable.Map[Entity, ListBuffer[EOptionP[Entity, Property]]] = mutable.Map()
    // A mapping from DUVar elements to the corresponding indices of the FlatPathElements
    val var2IndexMapping: mutable.Map[V, Int] = mutable.Map()
    // A mapping from values of FlatPathElements to StringConstancyInformation
    val fpe2sci: mutable.Map[Int, StringConstancyInformation] = mutable.Map()

    var params: List[StringConstancyInformation] = List()
}

/**
 * InterproceduralStringAnalysis processes a read operation of a string variable at a program
 * position, ''pp'', in a way that it finds the set of possible strings that can be read at ''pp''.
 *
 * In comparison to [[IntraproceduralStringAnalysis]], this version tries to resolve method calls
 * that are involved in a string construction as far as possible.
 *
 * @author Patrick Mell
 */
class InterproceduralStringAnalysis(
        val project: SomeProject
) extends FPCFAnalysis {

    private var declaredMethods: DeclaredMethods = _

    // state will be set to a non-null value in "determinePossibleStrings"
    var state: ComputationState = _

    def analyze(data: P): ProperPropertyComputationResult = {
        declaredMethods = project.get(DeclaredMethodsKey)
        // TODO: Is there a way to get the declared method in constant time?
        val dm = declaredMethods.declaredMethods.find(dm ⇒ dm.name == data._2.name).get

        val calleesEOptP = ps(dm, Callees.key)
        if (calleesEOptP.hasUBP) {
            determinePossibleStrings(data, calleesEOptP.ub)
        } else {
            val dependees = Iterable(calleesEOptP)
            InterimResult(
                calleesEOptP,
                StringConstancyProperty.lb,
                StringConstancyProperty.ub,
                dependees,
                calleesContinuation(calleesEOptP, dependees, data)
            )
        }
    }

    private def determinePossibleStrings(
        data: P, callees: Callees
    ): ProperPropertyComputationResult = {
        // sci stores the final StringConstancyInformation (if it can be determined now at all)
        var sci = StringConstancyProperty.lb.stringConstancyInformation
        val tacProvider = p.get(SimpleTACAIKey)
        val cfg = tacProvider(data._2).cfg
        val stmts = cfg.code.instructions
        state = ComputationState(None, cfg, Some(callees))
        state.params = InterproceduralStringAnalysis.getParams(data)

        val uvar = data._1
        val defSites = uvar.definedBy.toArray.sorted
        // Function parameters are currently regarded as dynamic value; the following if finds read
        // operations of strings (not String{Builder, Buffer}s, they will be handles further down
        if (defSites.head < 0) {
            return Result(data, StringConstancyProperty.lb)
        }
        val pathFinder: AbstractPathFinder = new WindowPathFinder(cfg)

        val call = stmts(defSites.head).asAssignment.expr
        if (InterpretationHandler.isStringBuilderBufferToStringCall(call)) {
            val initDefSites = InterpretationHandler.findDefSiteOfInit(uvar, stmts)
            // initDefSites empty => String{Builder,Buffer} from method parameter is to be evaluated
            if (initDefSites.isEmpty) {
                return Result(data, StringConstancyProperty.lb)
            }

            val paths = pathFinder.findPaths(initDefSites, uvar.definedBy.head)
            state.computedLeanPath = Some(paths.makeLeanPath(uvar, stmts))

            // Find DUVars, that the analysis of the current entity depends on
            val dependentVars = findDependentVars(state.computedLeanPath.get, stmts, uvar)
            if (dependentVars.nonEmpty) {
                dependentVars.keys.foreach { nextVar ⇒
                    val toAnalyze = (nextVar, data._2)
                    dependentVars.foreach { case (k, v) ⇒ state.var2IndexMapping(k) = v }
                    val ep = propertyStore(toAnalyze, StringConstancyProperty.key)
                    ep match {
                        case FinalP(p) ⇒
                            return processFinalP(data, callees, state, ep.e, p)
                        case _ ⇒
                            if (!state.dependees.contains(toAnalyze)) {
                                state.dependees(toAnalyze) = ListBuffer()
                            }
                            state.dependees(toAnalyze).append(ep)
                    }
                }
            } else {
                val iHandler = InterproceduralInterpretationHandler(
                    cfg, ps, declaredMethods, state, continuation(data, callees, List(), state)
                )
                if (computeResultsForPath(state.computedLeanPath.get, iHandler, state)) {
                    sci = new PathTransformer(iHandler).pathToStringTree(
                        state.computedLeanPath.get, state.fpe2sci.toMap
                    ).reduce(true)
                }
            }
        } // If not a call to String{Builder, Buffer}.toString, then we deal with pure strings
        else {
            val interHandler = InterproceduralInterpretationHandler(
                cfg, ps, declaredMethods, state, continuation(data, callees, List(), state)
            )
            val results = uvar.definedBy.toArray.sorted.map { ds ⇒
                (ds, interHandler.processDefSite(ds, state.params))
            }
            val interimResults = results.filter(!_._2.isInstanceOf[Result]).map { r ⇒
                (r._1, r._2.asInstanceOf[InterimResult[StringConstancyProperty]])
            }
            if (interimResults.isEmpty) {
                // All results are available => Prepare the final result
                sci = StringConstancyInformation.reduceMultiple(
                    results.map {
                        case (_, r) ⇒
                            val p = r.asInstanceOf[Result].finalEP.p
                            p.asInstanceOf[StringConstancyProperty].stringConstancyInformation
                    }.toList
                )
            }
            // No need to cover the else branch: interimResults.nonEmpty => dependees were added to
            // state.dependees, i.e., the if that checks whether state.dependees is non-empty will
            // always be true (thus, the value of "sci" does not matter)
        }

        if (state.dependees.nonEmpty) {
            InterimResult(
                data,
                StringConstancyProperty.ub,
                StringConstancyProperty.lb,
                state.dependees.values.flatten,
                continuation(data, callees, state.dependees.values.flatten, state)
            )
        } else {
            Result(data, StringConstancyProperty(sci))
        }
    }

    private def calleesContinuation(
        e:         Entity,
        dependees: Iterable[EOptionP[DeclaredMethod, Callees]],
        inputData: P
    )(eps: SomeEPS): ProperPropertyComputationResult = eps match {
        case FinalP(callees: Callees) ⇒
            determinePossibleStrings(inputData, callees)
        case InterimLUBP(lb, ub) ⇒
            InterimResult(e, lb, ub, dependees, calleesContinuation(e, dependees, inputData))
        case _ ⇒ throw new IllegalStateException("can occur?")
    }

    /**
     * `processFinalP` is responsible for handling the case that the `propertyStore` outputs a
     * [[org.opalj.fpcf.FinalP]].
     */
    private def processFinalP(
        data:    P,
        callees: Callees,
        state:   ComputationState,
        e:       Entity,
        p:       Property
    ): ProperPropertyComputationResult = {
        // Add mapping information (which will be used for computing the final result)
        val retrievedProperty = p.asInstanceOf[StringConstancyProperty]
        val currentSci = retrievedProperty.stringConstancyInformation
        state.fpe2sci.put(state.var2IndexMapping(e.asInstanceOf[P]._1), currentSci)

        // No more dependees => Return the result for this analysis run
        state.dependees.foreach { case (k, v) ⇒ state.dependees(k) = v.filter(_.e != e) }
        val remDependees = state.dependees.values.flatten
        if (remDependees.isEmpty) {
            // This is the case if the string information stems from a String{Builder, Buffer}
            val finalSci = if (state.computedLeanPath.isDefined) {
                val interpretationHandler = InterproceduralInterpretationHandler(
                    state.cfg, ps, declaredMethods, state,
                    continuation(data, callees, List(), state)
                )
                new PathTransformer(interpretationHandler).pathToStringTree(
                    state.computedLeanPath.get, state.fpe2sci.toMap
                ).reduce(true)
            } else {
                // This is the case if the string information stems from a String variable
                currentSci
            }
            Result(data, StringConstancyProperty(finalSci))
        } else {
            InterimResult(
                data,
                StringConstancyProperty.ub,
                StringConstancyProperty.lb,
                remDependees,
                continuation(data, callees, remDependees, state)
            )
        }
    }

    /**
     * Continuation function.
     *
     * @param data The data that was passed to the `analyze` function.
     * @param dependees A list of dependencies that this analysis run depends on.
     * @param state The computation state (which was originally captured by `analyze` and possibly
     *              extended / updated by other methods involved in computing the final result.
     * @return This function can either produce a final result or another intermediate result.
     */
    private def continuation(
        data:      P,
        callees:   Callees,
        dependees: Iterable[EOptionP[Entity, Property]],
        state:     ComputationState
    )(eps: SomeEPS): ProperPropertyComputationResult = eps match {
        case FinalP(p) ⇒ processFinalP(data, callees, state, eps.e, p)
        case InterimLUBP(lb, ub) ⇒ InterimResult(
            data, lb, ub, dependees, continuation(data, callees, dependees, state)
        )
        case _ ⇒ throw new IllegalStateException("Could not process the continuation successfully.")
    }

    /**
     * This function traversed the given path, computes all string values along the path and stores
     * these information in the given state.
     *
     * @param p The path to traverse.
     * @param iHandler The handler for interpreting string related sites.
     * @param state The current state of the computation. This function will extend
     *              [[ComputationState.fpe2sci]].
     * @return Returns `true` if all values computed for the path are final results.
     */
    private def computeResultsForPath(
        p:        Path,
        iHandler: InterproceduralInterpretationHandler,
        state:    ComputationState
    ): Boolean = {
        var hasFinalResult = true

        p.elements.foreach {
            case FlatPathElement(index) ⇒
                if (!state.fpe2sci.contains(index)) {
                    iHandler.processDefSite(index, state.params) match {
                        case Result(r) ⇒
                            val p = r.p.asInstanceOf[StringConstancyProperty]
                            state.fpe2sci(index) = p.stringConstancyInformation
                        case _ ⇒ hasFinalResult = false
                    }
                }
            case npe: NestedPathElement ⇒
                val subFinalResult = computeResultsForPath(
                    Path(npe.element.toList), iHandler, state
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

    private val paramInfos = mutable.Map[Entity, List[StringConstancyInformation]]()

    def registerParams(e: Entity, scis: List[StringConstancyInformation]): Unit = {
        if (!paramInfos.contains(e)) {
            paramInfos(e) = List(scis: _*)
        }
        // Per entity and method, a StringConstancyInformation list sshoud be present only once,
        // thus no else branch
    }

    def getParams(e: Entity): List[StringConstancyInformation] =
        if (paramInfos.contains(e)) {
            paramInfos(e)
        } else {
            List()
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
