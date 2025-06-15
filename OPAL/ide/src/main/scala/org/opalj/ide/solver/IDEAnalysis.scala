/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ide
package solver

import scala.collection.mutable.{Map => MutableMap}
import scala.collection.mutable.{Queue => MutableQueue}
import scala.collection.mutable.{Set => MutableSet}

import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EPS
import org.opalj.fpcf.InterimEUBP
import org.opalj.fpcf.InterimPartialResult
import org.opalj.fpcf.PartialResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.Results
import org.opalj.fpcf.SomeEOptionP
import org.opalj.fpcf.SomeEPK
import org.opalj.fpcf.SomeEPS
import org.opalj.ide.integration.IDEPropertyMetaInformation
import org.opalj.ide.integration.IDERawProperty
import org.opalj.ide.integration.IDETargetCallablesProperty
import org.opalj.ide.problem.AllTopEdgeFunction
import org.opalj.ide.problem.EdgeFunction
import org.opalj.ide.problem.EdgeFunctionResult
import org.opalj.ide.problem.FinalEdgeFunction
import org.opalj.ide.problem.FlowFunction
import org.opalj.ide.problem.IDEFact
import org.opalj.ide.problem.IdentityEdgeFunction
import org.opalj.ide.problem.IDEProblem
import org.opalj.ide.problem.IDEValue
import org.opalj.ide.problem.InterimEdgeFunction

/**
 * Basic solver for IDE problems. Uses the exhaustive algorithm that was presented in the original IDE paper from 1996
 * as base. For an example problem have a look at `LinearConstantPropagationProblem` in the TAC module. For an example
 * of interacting IDE problems have a look at `LCPOnFieldsProblem` and `LinearConstantPropagationProblemExtended`.
 *
 * @author Robin Körkemeier
 */
class IDEAnalysis[Fact <: IDEFact, Value <: IDEValue, Statement, Callable <: Entity](
    val project:                 SomeProject,
    val problem:                 IDEProblem[Fact, Value, Statement, Callable],
    val icfg:                    ICFG[Statement, Callable],
    val propertyMetaInformation: IDEPropertyMetaInformation[Fact, Value, Statement, Callable]
) extends FPCFAnalysis {
    private type Node = (Statement, Fact)
    /**
     * A 'path' in the graph, denoted by it's start and end node as done in the IDE algorithm
     */
    private type Path = (Node, Node)

    private type PathWorkList = MutableQueue[Path]

    private type JumpFunction = EdgeFunction[Value]
    private type JumpFunctions = MutableMap[(Statement, Statement), MutableMap[(Fact, Fact), JumpFunction]]
    private type SummaryFunction = EdgeFunction[Value]
    private type SummaryFunctions = MutableMap[Path, SummaryFunction]

    private type NodeWorkList = MutableQueue[Node]

    private type Values = MutableMap[Node, Value]

    private val allTopEdgeFunction = new AllTopEdgeFunction[Value](problem.lattice.top) {
        override def composeWith[V >: Value <: IDEValue](secondEdgeFunction: EdgeFunction[V]): EdgeFunction[V] = {
            /* This method cannot be implemented correctly without knowledge about the other possible edge functions.
             * It will never be called on this instance anyway. However, we throw an exception here to be safe. */
            throw new UnsupportedOperationException(s"Composing $this with $secondEdgeFunction is not implemented!")
        }

        override def meet[V >: Value <: IDEValue](otherEdgeFunction: EdgeFunction[V]): EdgeFunction[V] =
            otherEdgeFunction

        override def equals[V >: Value <: IDEValue](otherEdgeFunction: EdgeFunction[V]): Boolean =
            otherEdgeFunction eq this
    }

    /**
     * Container class for simpler interaction and passing of the shared data
     */
    private class State(
        initialTargetCallablesEOptionP: EOptionP[
            IDEPropertyMetaInformation[Fact, Value, Statement, Callable],
            IDETargetCallablesProperty[Callable]
        ]
    ) {
        /**
         * Collection of callables to compute results for. Used to optimize solver computation.
         */
        private val targetCallables = MutableSet.empty[Callable]

        private var targetCallablesEOptionP: EOptionP[
            IDEPropertyMetaInformation[Fact, Value, Statement, Callable],
            IDETargetCallablesProperty[Callable]
        ] = initialTargetCallablesEOptionP

        processTargetCallablesEOptionP(initialTargetCallablesEOptionP)

        /**
         * Collection of callables that received changes (compared to the last update) which may affect their final
         * value, e.g. a changed jump function. Especially used to reduce computation overhead in phase 2 and to reduce
         * amount of created results.
         */
        private val callablesWithChanges = MutableSet.empty[Callable]

        /**
         * The work list for paths used in P1
         */
        private val pathWorkList: PathWorkList = MutableQueue.empty

        /**
         * The jump functions (incrementally calculated) in P1
         */
        private val jumpFunctions: JumpFunctions = MutableMap.empty

        /**
         * The summary functions (incrementally calculated) in P1
         */
        private val summaryFunctions: SummaryFunctions = MutableMap.empty

        /**
         * Collection of seen end nodes with corresponding jump function (needed for endSummaries extension)
         */
        private val endSummaries = MutableMap.empty[Node, MutableSet[(Node, JumpFunction)]]

        /**
         * Map call targets to all seen call sources (similar to a call graph but reversed; needed for endSummaries
         * extension)
         */
        private val callTargetsToSources = MutableMap.empty[Node, MutableSet[Node]]

        /**
         * The work list for nodes used in P2
         */
        private val nodeWorkList: NodeWorkList = MutableQueue.empty

        /**
         * Store all calculated (intermediate) values. Associated by callable for performant access.
         */
        private val values: MutableMap[Callable, Values] = MutableMap.empty

        /**
         * Map outstanding EPKs to the last processed property result and the continuations to be executed when a new
         * result is available
         */
        private val dependees = MutableMap.empty[SomeEPK, (SomeEOptionP, MutableSet[() => Unit])]

        def getTargetCallables: scala.collection.Set[Callable] = {
            targetCallables
        }

        def getTargetCallablesEOptionP: EOptionP[
            IDEPropertyMetaInformation[Fact, Value, Statement, Callable],
            IDETargetCallablesProperty[Callable]
        ] = {
            targetCallablesEOptionP
        }

        def processTargetCallablesEOptionP(newTargetCallablesEOptionP: EOptionP[
            IDEPropertyMetaInformation[Fact, Value, Statement, Callable],
            IDETargetCallablesProperty[Callable]
        ]): Unit = {
            targetCallablesEOptionP = newTargetCallablesEOptionP
            if (targetCallablesEOptionP.hasUBP) {
                val addedTargetCallables = targetCallablesEOptionP.ub.targetCallables.diff(targetCallables)
                targetCallables.addAll(addedTargetCallables)
                // Mark new target callables as changed to trigger result creation
                callablesWithChanges.addAll(addedTargetCallables)
            }
        }

        def clearCallablesWithChanges(): Unit = {
            callablesWithChanges.clear()
        }

        def rememberCallableWithChanges(callable: Callable): Unit = {
            callablesWithChanges.add(callable)
        }

        def getCallablesWithChanges: scala.collection.Set[Callable] = {
            callablesWithChanges
        }

        def enqueuePath(path: Path): Unit = {
            pathWorkList.enqueue(path)
        }

        def dequeuePath(): Path = {
            pathWorkList.dequeue()
        }

        def isPathWorkListEmpty: Boolean = {
            pathWorkList.isEmpty
        }

        def setJumpFunction(path: Path, jumpFunction: JumpFunction): Unit = {
            val ((source, sourceFact), (target, targetFact)) = path
            jumpFunctions
                .getOrElseUpdate((source, target), { MutableMap.empty })
                .put((sourceFact, targetFact), jumpFunction)
        }

        def getJumpFunction(path: Path): JumpFunction = {
            val ((source, sourceFact), (target, targetFact)) = path
            jumpFunctions
                .getOrElse((source, target), Map.empty[(Fact, Fact), JumpFunction])
                .getOrElse((sourceFact, targetFact), allTopEdgeFunction) // else part handles IDE lines 1 - 2
        }

        def lookupJumpFunctions(
            source:           Statement,
            sourceFactOption: Option[Fact] = None,
            target:           Statement,
            targetFactOption: Option[Fact] = None
        ): scala.collection.Map[(Fact, Fact), JumpFunction] = {
            val subMap = jumpFunctions.getOrElse((source, target), Map.empty[(Fact, Fact), JumpFunction])

            (sourceFactOption, targetFactOption) match {
                case (Some(sourceFact), Some(targetFact)) =>
                    subMap.filter { case ((sF, tF), _) => sF == sourceFact && tF == targetFact }
                case (Some(sourceFact), None) =>
                    subMap.filter { case ((sF, _), _) => sF == sourceFact }
                case (None, Some(targetFact)) =>
                    subMap.filter { case ((_, tF), _) => tF == targetFact }
                case _ =>
                    subMap
            }
        }

        def setSummaryFunction(path: Path, summaryFunction: SummaryFunction): Unit = {
            summaryFunctions.put(path, summaryFunction)
        }

        def getSummaryFunction(path: Path): SummaryFunction = {
            summaryFunctions.getOrElse(path, allTopEdgeFunction) // else part handels IDE lines 3 - 4
        }

        def addEndSummary(path: Path, jumpFunction: JumpFunction): Unit = {
            val (start, end) = path
            val set = endSummaries.getOrElseUpdate(start, MutableSet.empty)
            set.add((end, jumpFunction))
        }

        def getEndSummaries(start: Node): scala.collection.Set[(Node, JumpFunction)] = {
            endSummaries.getOrElse(start, Set.empty)
        }

        def rememberCallEdge(path: Path): Unit = {
            val (source, target) = path

            val set = callTargetsToSources.getOrElseUpdate(target, MutableSet.empty)
            set.add(source)
        }

        def lookupCallSourcesForTarget(target: Statement, targetFact: Fact): scala.collection.Set[Node] = {
            callTargetsToSources.getOrElse((target, targetFact), Set.empty)
        }

        def enqueueNode(node: Node): Unit = {
            nodeWorkList.enqueue(node)
        }

        def dequeueNode(): Node = {
            nodeWorkList.dequeue()
        }

        def isNodeWorkListEmpty: Boolean = {
            nodeWorkList.isEmpty
        }

        def getValue(node: Node, callable: Callable): Value = {
            values.getOrElse(callable, MutableMap.empty).getOrElse(node, problem.lattice.top) // else part handles IDE line 1
        }

        def getValue(node: Node): Value = {
            getValue(node, icfg.getCallable(node._1))
        }

        def setValue(node: Node, newValue: Value, callable: Callable): Unit = {
            values.getOrElseUpdate(callable, { MutableMap.empty }).put(node, newValue)
        }

        def clearValues(): Unit = {
            values.clear()
        }

        /**
         * @return a map from statements to results and the results for the callable in total (both per requested
         *         callable)
         */
        def collectResults(callables: scala.collection.Set[Callable]): Map[
            Callable,
            (scala.collection.Map[Statement, scala.collection.Set[(Fact, Value)]], scala.collection.Set[(Fact, Value)])
        ] = {
            val valuesByCallable = callables
                .map { callable => callable -> values.getOrElse(callable, Map.empty[Node, Value]) }
                .toMap

            valuesByCallable.map { case (callable, values) =>
                val resultsByStatement = values
                    .view
                    .filterKeys { case (_, d) => d != problem.nullFact }
                    .groupMap(_._1._1) { case ((_, d), value) => (d, value) }
                    .map { case (n, dValuePairs) =>
                        (
                            n,
                            dValuePairs.groupMapReduce(_._1)(_._2) { (value1, value2) =>
                                problem.lattice.meet(value1, value2)
                            }.toSet
                        )
                    }

                val resultsForExit = resultsByStatement
                    .collect { case (n, values) if icfg.isNormalExitStatement(n) => values.toList }
                    .flatten
                    .groupMapReduce(_._1)(_._2) {
                        (value1, value2) => problem.lattice.meet(value1, value2)
                    }
                    .toSet

                callable -> (resultsByStatement, resultsForExit)
            }
        }

        def addDependee(eOptionP: SomeEOptionP, c: () => Unit): Unit = {
            // The eOptionP is only inserted the first time the corresponding EPK occurs. Consequently, it is the most
            // precise property result that is seen by all dependents.
            val (_, set) = dependees.getOrElseUpdate(eOptionP.toEPK, (eOptionP, MutableSet.empty))
            set.add(c)
        }

        def hasDependees: Boolean = {
            dependees.nonEmpty
        }

        def getDependees: scala.collection.Set[SomeEOptionP] = {
            dependees.values.map(_._1).toSet
        }

        def getAndRemoveDependeeContinuations(eOptionP: SomeEOptionP): scala.collection.Set[() => Unit] = {
            dependees.remove(eOptionP.toEPK).map(_._2).getOrElse(Set.empty).toSet
        }
    }

    /**
     * Run the IDE solver and calculate (and return) the result. This method should only be triggered in combination
     * with the IDE proxy!
     *
     * @param entity Expected to be `None`. Other values do not cause errors but will only return empty (temporary)
     *               results.
     * @return a result for each statement of the target callables plus one result for each target callable itself
     *         (combining the results of all exit statements)
     */
    def performAnalysis(entity: Entity): ProperPropertyComputationResult = {
        /* If an actual entity reaches here, there was a concrete request to the property store that was faster than
         * this analysis could answer when being called with `entity == None`. Returning an 'empty' result in this case,
         * which of course will be updated if the right analysis request completes. */
        if (entity != None) {
            return PartialResult(
                entity,
                propertyMetaInformation.backingPropertyMetaInformation.key,
                { (_: SomeEOptionP) => None }
            )
        }

        val targetCallablesEOptionP =
            propertyStore(propertyMetaInformation, propertyMetaInformation.targetCallablesPropertyMetaInformation.key)
        implicit val state: State = new State(targetCallablesEOptionP)

        performPhase1()
        performPhase2()

        createResult()
    }

    /**
     * @return whether the phase is finished or has to be continued once the dependees are resolved
     */
    private def performPhase1()(implicit s: State): Boolean = {
        seedPhase1()
        processPathWorkList()

        !s.hasDependees
    }

    /**
     * @return whether the phase is finished or has to be continued once the dependees are resolved
     */
    private def continuePhase1()(implicit s: State): Boolean = {
        processPathWorkList()

        !s.hasDependees
    }

    /**
     * Perform phase 2 from scratch
     */
    private def performPhase2()(implicit s: State): Unit = {
        s.clearValues()

        seedPhase2()
        computeValues()
    }

    /**
     * Continue phase 2 from based on previous result
     */
    private def continuePhase2()(implicit s: State): Unit = {
        seedPhase2()
        computeValues()
    }

    private def createResult()(
        implicit s: State
    ): ProperPropertyComputationResult = {
        // Only create results for target callables whose values could have changed
        val callables = s.getCallablesWithChanges.intersect(s.getTargetCallables)
        val collectedResults = s.collectResults(callables)
        val callableResults = callables.map { callable =>
            val (resultsByStatement, resultsForExit) =
                collectedResults.getOrElse(
                    callable,
                    { (Map.empty[Statement, scala.collection.Set[(Fact, Value)]], Set.empty[(Fact, Value)]) }
                )
            val ideRawProperty = new IDERawProperty(
                propertyMetaInformation.backingPropertyMetaInformation.key,
                resultsByStatement,
                resultsForExit
            )

            PartialResult(
                callable,
                propertyMetaInformation.backingPropertyMetaInformation.key,
                { (eOptionP: SomeEOptionP) =>
                    if (eOptionP.hasUBP && eOptionP.ub == ideRawProperty) {
                        None
                    } else {
                        Some(InterimEUBP(callable, ideRawProperty))
                    }
                }
            )
        }

        Results(
            callableResults ++ Seq(
                InterimPartialResult(
                    None,
                    s.getDependees.toSet ++ Set(s.getTargetCallablesEOptionP),
                    { (eps: SomeEPS) =>
                        if (eps.toEPK == s.getTargetCallablesEOptionP.toEPK) {
                            onTargetCallablesUpdateContinuation(eps.asInstanceOf[EPS[
                                IDEPropertyMetaInformation[Fact, Value, Statement, Callable],
                                IDETargetCallablesProperty[Callable]
                            ]])
                        } else {
                            onDependeeUpdateContinuation(eps)
                        }
                    }
                )
            )
        )
    }

    private def onTargetCallablesUpdateContinuation(eps: EPS[
        IDEPropertyMetaInformation[Fact, Value, Statement, Callable],
        IDETargetCallablesProperty[Callable]
    ])(implicit s: State): ProperPropertyComputationResult = {
        s.clearCallablesWithChanges()

        s.processTargetCallablesEOptionP(eps)

        seedPhase1()
        continuePhase1()
        continuePhase2()

        createResult()
    }

    private def onDependeeUpdateContinuation(eps: SomeEPS)(
        implicit s: State
    ): ProperPropertyComputationResult = {
        s.clearCallablesWithChanges()

        // Get and remove all continuations that are remembered for the EPS
        val cs = s.getAndRemoveDependeeContinuations(eps)

        // Call continuations
        cs.foreach(c => c())

        // The continuations can have enqueued paths to the path work list
        continuePhase1()
        continuePhase2()

        createResult()
    }

    private def seedPhase1()(implicit s: State): Unit = {
        def propagateSeed(e: Path, callable: Callable, f: EdgeFunction[Value]): Unit = {
            val oldJumpFunction = s.getJumpFunction(e)
            val fPrime = f.meet(oldJumpFunction)

            if (!fPrime.equals(oldJumpFunction)) {
                s.setJumpFunction(e, fPrime)
                s.enqueuePath(e)
                s.rememberCallableWithChanges(callable)
            }
        }

        val callables = s.getTargetCallables
        callables.foreach { callable =>
            // IDE P1 lines 5 - 6
            icfg.getStartStatements(callable).foreach { stmt =>
                val path = ((stmt, problem.nullFact), (stmt, problem.nullFact))
                propagateSeed(path, callable, IdentityEdgeFunction)

                problem.getAdditionalSeeds(stmt, callable).foreach { fact =>
                    val path = ((stmt, problem.nullFact), (stmt, fact))
                    propagateSeed(path, callable, IdentityEdgeFunction)

                    def processAdditionalSeed(): Unit = {
                        val edgeFunction =
                            handleEdgeFunctionResult(
                                problem.getAdditionalSeedsEdgeFunction(stmt, fact, callable),
                                processAdditionalSeed _
                            )
                        propagateSeed(path, callable, edgeFunction)
                    }

                    processAdditionalSeed()
                }
            }
        }
    }

    private def processPathWorkList()(implicit s: State): Unit = {
        while (!s.isPathWorkListEmpty) { // IDE P1 line 7
            val path = s.dequeuePath() // IDE P1 line 8
            val ((_, _), (n, _)) = path
            val f = s.getJumpFunction(path) // IDE P1 line 9

            if (icfg.isCallStatement(n)) { // IDE P1 line 11
                processCallFlow(path, f, icfg.getCallees(n))
            } else if (icfg.isNormalExitStatement(n)) { // IDE P1 line 19
                processExitFlow(path, f)
            } else { // IDE P1 line 30
                processNormalFlow(path, f)
            }
        }
    }

    private def processCallFlow(path: Path, f: JumpFunction, qs: scala.collection.Set[Callable])(
        implicit s: State
    ): Unit = {
        val ((sp, d1), (n, d2)) = path

        val rs = icfg.getNextStatements(n) // IDE P1 line 14

        if (qs.isEmpty) {
            rs.foreach { r =>
                val d5s = handleFlowFunctionResult(problem.getPrecomputedFlowFunction(n, d2, r).compute(), path)

                d5s.foreach { d5 =>
                    val summaryFunction =
                        handleEdgeFunctionResult(problem.getPrecomputedSummaryFunction(n, d2, r, d5), path)
                    val callToReturnPath = ((n, d2), (r, d5))
                    val oldSummaryFunction = s.getSummaryFunction(callToReturnPath)
                    val fPrime = summaryFunction.meet(oldSummaryFunction)

                    if (!fPrime.equals(oldSummaryFunction)) {
                        s.setSummaryFunction(callToReturnPath, fPrime)
                    }

                    propagate(((sp, d1), (r, d5)), f.composeWith(fPrime))
                }
            }
        } else {

            qs.foreach { q =>
                if (problem.hasPrecomputedFlowAndSummaryFunction(n, d2, q)) {
                    /* Handling for precomputed summaries */
                    rs.foreach { r =>
                        val d5s =
                            handleFlowFunctionResult(problem.getPrecomputedFlowFunction(n, d2, q, r).compute(), path)

                        d5s.foreach { d5 =>
                            val summaryFunction =
                                handleEdgeFunctionResult(problem.getPrecomputedSummaryFunction(n, d2, q, r, d5), path)
                            val callToReturnPath = ((n, d2), (r, d5))
                            val oldSummaryFunction = s.getSummaryFunction(callToReturnPath)
                            val fPrime = summaryFunction.meet(oldSummaryFunction)

                            if (!fPrime.equals(oldSummaryFunction)) {
                                s.setSummaryFunction(callToReturnPath, fPrime)
                            }

                            propagate(((sp, d1), (r, d5)), f.composeWith(fPrime))
                        }
                    }
                } else {
                    val sqs = icfg.getStartStatements(q)
                    sqs.foreach { sq =>
                        // IDE P1 lines 12 - 13
                        val d3s = handleFlowFunctionResult(problem.getCallFlowFunction(n, d2, sq, q).compute(), path)

                        d3s.foreach { d3 =>
                            s.rememberCallEdge(((n, d2), (sq, d3)))

                            val endSummaries = s.getEndSummaries((sq, d3))
                            // Handling for end summaries extension
                            if (endSummaries.nonEmpty) {
                                endSummaries.foreach { case ((eq, d4), fEndSummary) =>
                                    val f4 =
                                        handleEdgeFunctionResult(problem.getCallEdgeFunction(n, d2, sq, d3, q), path)
                                    rs.foreach { r =>
                                        val d5s = handleFlowFunctionResult(
                                            problem.getReturnFlowFunction(eq, d4, q, r, n, d2).compute(),
                                            path
                                        )
                                        d5s.foreach { d5 =>
                                            val f5 = handleEdgeFunctionResult(
                                                problem.getReturnEdgeFunction(eq, d4, q, r, d5, n, d2),
                                                path
                                            )
                                            val callToReturnPath = ((n, d2), (r, d5))
                                            val oldSummaryFunction = s.getSummaryFunction(callToReturnPath)
                                            val fPrime =
                                                f4.composeWith(fEndSummary).composeWith(f5).meet(oldSummaryFunction)

                                            if (!fPrime.equals(oldSummaryFunction)) {
                                                s.setSummaryFunction(callToReturnPath, fPrime)
                                            }

                                            propagate(((sp, d1), (r, d5)), f.composeWith(fPrime))
                                        }
                                    }
                                }
                            } else {
                                // Default algorithm behavior
                                propagate(((sq, d3), (sq, d3)), IdentityEdgeFunction)
                            }
                        }
                    }
                }

                rs.foreach { r =>
                    val d3s = handleFlowFunctionResult(problem.getCallToReturnFlowFunction(n, d2, q, r).compute(), path)

                    // IDE P1 lines 15 - 16
                    d3s.foreach { d3 =>
                        propagate(
                            ((sp, d1), (r, d3)),
                            f.composeWith(handleEdgeFunctionResult(
                                problem.getCallToReturnEdgeFunction(n, d2, q, r, d3),
                                path
                            ))
                        )
                    }

                    // IDE P1 lines 17 - 18
                    d3s.foreach { d3 =>
                        val f3 = s.getSummaryFunction(((n, d2), (r, d3)))
                        if (!f3.equals(allTopEdgeFunction)) {
                            propagate(((sp, d1), (r, d3)), f.composeWith(f3))
                        }
                    }
                }
            }
        }
    }

    private def processExitFlow(path: Path, f: JumpFunction)(implicit s: State): Unit = {
        val ((sp, d1), (n, d2)) = path
        val p = icfg.getCallable(n)

        // Handling for end summaries extension
        s.addEndSummary(path, f)

        // IDE P1 line 20
        val callSources = s.lookupCallSourcesForTarget(sp, d1)
        callSources.foreach {
            case (c, d4) =>
                val rs = icfg.getNextStatements(c)
                rs.foreach { r =>
                    // IDE P1 line 21
                    val d5s = handleFlowFunctionResult(problem.getReturnFlowFunction(n, d2, p, r, c, d4).compute(), path)

                    d5s.foreach { d5 =>
                        // IDE P1 lines 22 - 23
                        val f4 = handleEdgeFunctionResult(problem.getCallEdgeFunction(c, d4, sp, d1, p), path)
                        val f5 = handleEdgeFunctionResult(problem.getReturnEdgeFunction(n, d2, p, r, d5, c, d4), path)

                        // IDE P1 line 24
                        val callToReturnPath = ((c, d4), (r, d5))
                        val oldSummaryFunction = s.getSummaryFunction(callToReturnPath)
                        val fPrime = f4.composeWith(f).composeWith(f5).meet(oldSummaryFunction)

                        // IDE P1 lines 25 - 29
                        if (!fPrime.equals(oldSummaryFunction)) {
                            s.setSummaryFunction(callToReturnPath, fPrime)

                            val sqs = icfg.getStartStatements(icfg.getCallable(c))
                            sqs.foreach { sq =>
                                val jumpFunctionsMatchingTarget =
                                    s.lookupJumpFunctions(source = sq, target = c, targetFactOption = Some(d4))
                                jumpFunctionsMatchingTarget.foreach {
                                    case ((d3, _), f3) if !f3.equals(allTopEdgeFunction) =>
                                        propagate(((sq, d3), (r, d5)), f3.composeWith(fPrime))
                                    case _ =>
                                }
                            }
                        }
                    }
                }
        }
    }

    private def processNormalFlow(path: Path, f: JumpFunction)(implicit s: State): Unit = {
        val ((sp, d1), (n, d2)) = path

        // IDE P1 lines 31 - 32
        icfg.getNextStatements(n).foreach { m =>
            val d3s = handleFlowFunctionResult(problem.getNormalFlowFunction(n, d2, m).compute(), path)

            d3s.foreach { d3 =>
                propagate(
                    ((sp, d1), (m, d3)),
                    f.composeWith(handleEdgeFunctionResult(problem.getNormalEdgeFunction(n, d2, m, d3), path))
                )
            }
        }
    }

    private def propagate(e: Path, f: EdgeFunction[Value])(implicit s: State): Unit = {
        // IDE P1 lines 34 - 37
        val oldJumpFunction = s.getJumpFunction(e)
        val fPrime = f.meet(oldJumpFunction)

        if (!fPrime.equals(oldJumpFunction)) {
            s.setJumpFunction(e, fPrime)
            s.enqueuePath(e)
            s.rememberCallableWithChanges(icfg.getCallable(e._2._1))
        }
    }

    /**
     * @param path the path to re-enqueue when encountering an interim flow function
     * @return the (interim) generated flow facts
     */
    private def handleFlowFunctionResult(
        factsAndDependees: FlowFunction.FactsAndDependees[Fact],
        path:              Path
    )(implicit s: State): scala.collection.Set[Fact] = {
        val (facts, dependees) = factsAndDependees
        if (dependees.nonEmpty) {
            dependees.foreach { dependee =>
                s.addDependee(
                    dependee,
                    () => s.enqueuePath(path)
                )
            }
        }
        facts
    }

    /**
     * @param path the path to re-enqueue when getting an interim edge function
     * @return the (interim) edge function from the result
     */
    private def handleEdgeFunctionResult(
        edgeFunctionResult: EdgeFunctionResult[Value],
        path:               Path
    )(implicit s: State): EdgeFunction[Value] = {
        handleEdgeFunctionResult(edgeFunctionResult, () => s.enqueuePath(path))
    }

    /**
     * @param continuation the continuation to execute when the dependee changes (in case of an interim edge function)
     * @return the (interim) edge function from the result
     */
    private def handleEdgeFunctionResult(
        edgeFunctionResult: EdgeFunctionResult[Value],
        continuation:       () => Unit
    )(implicit s: State): EdgeFunction[Value] = {
        edgeFunctionResult match {
            case FinalEdgeFunction(edgeFunction) =>
                edgeFunction
            case InterimEdgeFunction(intermediateEdgeFunction, dependees) =>
                dependees.foreach { dependee =>
                    s.addDependee(
                        dependee,
                        continuation
                    )
                }
                intermediateEdgeFunction
        }
    }

    private def seedPhase2()(implicit s: State): Unit = {
        val callables = s.getCallablesWithChanges
        callables.foreach { callable =>
            // IDE P2 lines 2 - 3
            icfg.getStartStatements(callable).foreach { stmt =>
                val node = (stmt, problem.nullFact)
                s.enqueueNode(node)
                s.setValue(node, problem.lattice.bottom, callable)
            }
        }
    }

    private def computeValues()(implicit s: State): Unit = {
        // IDE P2 part (i)
        while (!s.isNodeWorkListEmpty) { // IDE P2 line 4
            val node = s.dequeueNode() // IDE P2 line 5

            val (n, _) = node

            if (icfg.isCallStatement(n)) { // IDE P2 line 11
                processCallNode(node, icfg.getCallees(n))
            } else { // IDE P2 line 7
                processStartNode(node)
            }
        }

        // IDE P2 part (ii)
        // IDE P2 lines 15 - 17
        // Reduced to the callables whose values could have changed
        val ps = s.getCallablesWithChanges.intersect(s.getTargetCallables)
        ps.foreach { p =>
            val sps = icfg.getStartStatements(p)
            val ns = collectReachableStmts(sps, stmt => !icfg.isCallStatement(stmt))

            // IDE P2 line 16 - 17
            ns.foreach { n =>
                sps.foreach { sp =>
                    val jumpFunctionsMatchingTarget = s.lookupJumpFunctions(source = sp, target = n)
                    jumpFunctionsMatchingTarget.foreach {
                        case ((dPrime, d), fPrime) if !fPrime.equals(allTopEdgeFunction) =>
                            val nSharp = (n, d)
                            val vPrime = problem.lattice.meet(
                                s.getValue(nSharp, p),
                                fPrime.compute(s.getValue((sp, dPrime), p))
                            )

                            s.setValue(nSharp, vPrime, p)

                        case _ =>
                    }
                }
            }
        }
    }

    /**
     * Collect all statements that are reachable from a certain start set of statements.
     *
     * @param originStmts the statements to start searching from
     * @param filterPredicate an additional predicate the collected statements have to fulfill
     */
    private def collectReachableStmts(
        originStmts:     scala.collection.Set[Statement],
        filterPredicate: Statement => Boolean
    ): Iterator[Statement] = {
        new Iterator[Statement]() {
            private val collectedStmts = MutableSet.empty[Statement]
            private val seenStmts = MutableSet.empty[Statement]

            collectedStmts.addAll(originStmts.filter(filterPredicate))
            seenStmts.addAll(originStmts)
            originStmts.filterNot(filterPredicate).foreach { stmt => processStatement(stmt) }

            private def processStatement(stmt: Statement): Unit = {
                val workingStmts = MutableQueue(stmt)

                while (workingStmts.nonEmpty) {
                    icfg.getNextStatements(workingStmts.dequeue())
                        .foreach { followingStmt =>
                            if (!seenStmts.contains(followingStmt)) {
                                seenStmts.add(followingStmt)

                                if (filterPredicate(followingStmt)) {
                                    collectedStmts.add(followingStmt)
                                } else {
                                    workingStmts.enqueue(followingStmt)
                                }
                            }
                        }
                }
            }

            override def hasNext: Boolean = {
                collectedStmts.nonEmpty
            }

            override def next(): Statement = {
                val stmt = collectedStmts.head
                collectedStmts.remove(stmt)

                processStatement(stmt)

                stmt
            }
        }
    }

    private def processStartNode(node: Node)(implicit s: State): Unit = {
        val (n, d) = node

        // IDE P2 line 8
        val cs = collectReachableStmts(Set(n), stmt => icfg.isCallStatement(stmt))

        // IDE P2 lines 9 - 10
        cs.foreach { c =>
            val jumpFunctionsMatchingTarget =
                s.lookupJumpFunctions(source = n, sourceFactOption = Some(d), target = c)
            jumpFunctionsMatchingTarget.foreach {
                case ((_, dPrime), fPrime) if !fPrime.equals(allTopEdgeFunction) =>
                    propagateValue((c, dPrime), fPrime.compute(s.getValue((n, d))))
                case _ =>
            }
        }
    }

    private def processCallNode(node: Node, qs: scala.collection.Set[Callable])(implicit s: State): Unit = {
        val (n, d) = node

        // IDE P2 lines 12 - 13
        qs.foreach { q =>
            if (!problem.hasPrecomputedFlowAndSummaryFunction(n, d, q)) {
                val sqs = icfg.getStartStatements(q)
                sqs.foreach { sq =>
                    val dPrimes = extractFlowFunctionFromResult(problem.getCallFlowFunction(n, d, sq, q).compute())
                    dPrimes.foreach { dPrime =>
                        propagateValue(
                            (sq, dPrime),
                            extractEdgeFunctionFromResult(problem.getCallEdgeFunction(n, d, sq, dPrime, q))
                                .compute(s.getValue(node))
                        )
                    }
                }
            }
        }
    }

    private def propagateValue(nSharp: Node, v: Value)(implicit s: State): Unit = {
        val callable = icfg.getCallable(nSharp._1)

        // IDE P2 lines 18 - 21
        val oldValue = s.getValue(nSharp, callable)
        val vPrime = problem.lattice.meet(v, oldValue)

        if (vPrime != oldValue) {
            s.setValue(nSharp, vPrime, callable)
            s.enqueueNode(nSharp)
            s.rememberCallableWithChanges(callable)
        }
    }

    /**
     * Extract facts from flow function result while ignoring the dependees
     */
    private def extractFlowFunctionFromResult(
        factsAndDependees: FlowFunction.FactsAndDependees[Fact]
    ): scala.collection.Set[Fact] = {
        val (facts, _) = factsAndDependees
        facts
    }

    /**
     * Extract edge function from result ignoring the dependees
     */
    private def extractEdgeFunctionFromResult(edgeFunctionResult: EdgeFunctionResult[Value]): EdgeFunction[Value] = {
        edgeFunctionResult match {
            case FinalEdgeFunction(edgeFunction) =>
                edgeFunction
            case InterimEdgeFunction(interimEdgeFunction, _) =>
                interimEdgeFunction
        }
    }
}
