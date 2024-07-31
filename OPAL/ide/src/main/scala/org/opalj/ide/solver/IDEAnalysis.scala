/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ide.solver

import scala.annotation.unused

import scala.collection.immutable
import scala.collection.mutable

import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.fpcf.Entity
import org.opalj.fpcf.InterimPartialResult
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.Result
import org.opalj.fpcf.Results
import org.opalj.fpcf.SomeEOptionP
import org.opalj.fpcf.SomeEPK
import org.opalj.fpcf.SomeEPS
import org.opalj.ide.ConfigKeyDebugLog
import org.opalj.ide.ConfigKeyTraceLog
import org.opalj.ide.FrameworkName
import org.opalj.ide.integration.IDEPropertyMetaInformation
import org.opalj.ide.problem.AllTopEdgeFunction
import org.opalj.ide.problem.EdgeFunction
import org.opalj.ide.problem.EdgeFunctionResult
import org.opalj.ide.problem.FinalEdgeFunction
import org.opalj.ide.problem.IDEFact
import org.opalj.ide.problem.IdentityEdgeFunction
import org.opalj.ide.problem.IDEProblem
import org.opalj.ide.problem.IDEValue
import org.opalj.ide.problem.InterimEdgeFunction
import org.opalj.log.OPALLogger

/**
 * Basic solver for IDE problems. Uses the exhaustive/forward algorithm that was presented in the original IDE paper
 * from 1996 as base.
 */
class IDEAnalysis[Fact <: IDEFact, Value <: IDEValue, Statement, Callable <: Entity](
        val project:                 SomeProject,
        val problem:                 IDEProblem[Fact, Value, Statement, Callable],
        val propertyMetaInformation: IDEPropertyMetaInformation[Fact, Value]
) extends FPCFAnalysis {
    private type Node = (Statement, Fact)
    /**
     * A 'path' in the graph, denoted by it's start and end node as done in the IDE algorithm
     */
    private type Path = (Node, Node)

    private type PathWorkList = mutable.Queue[Path]

    private type JumpFunction = EdgeFunction[Value]
    private type JumpFunctions = mutable.Map[Path, JumpFunction]
    private type SummaryFunction = EdgeFunction[Value]
    private type SummaryFunctions = mutable.Map[Path, SummaryFunction]

    private type NodeWorkList = mutable.Queue[Node]

    private type Values = mutable.Map[Node, Value]

    private val identityEdgeFunction = new IdentityEdgeFunction[Value]
    private val allTopEdgeFunction = new AllTopEdgeFunction[Value](problem.lattice.top) {
        override def composeWith(secondEdgeFunction: EdgeFunction[Value]): EdgeFunction[Value] = {
            /* This method cannot be implemented correctly without knowledge about the other possible edge functions.
             * It will never be called on this instance anyway. However, we throw an exception here to be safe. */
            throw new UnsupportedOperationException(s"Composing $this with $secondEdgeFunction is not implemented!")
        }
    }

    /**
     * Container class for simpler interaction and passing of the 'shared' data
     */
    private class State {
        /**
         * The work list for paths used in P1
         */
        private val pathWorkList: PathWorkList = mutable.Queue.empty

        /**
         * The jump functions (incrementally calculated) in P1
         */
        private val jumpFunctions: JumpFunctions = mutable.Map.empty

        /**
         * The summary functions (incrementally calculated) in P1
         */
        private val summaryFunctions: SummaryFunctions = mutable.Map.empty

        /**
         * Collection of seen end nodes with corresponding jump function (needed for endSummaries extension)
         */
        private val endSummaries = mutable.Map.empty[Node, mutable.Set[(Node, JumpFunction)]]

        /**
         * Collection of all callables that were visited in P1
         */
        private val seenCallables = mutable.Set.empty[Callable]

        /**
         * Map call targets to all seen call sources (similar to a call graph but reversed; needed for endSummaries
         * extension)
         */
        private val callTargetsToSources = mutable.Map.empty[Node, mutable.Set[Node]]

        /**
         * The work list for nodes used in P2
         */
        private val nodeWorkList: NodeWorkList = mutable.Queue.empty

        /**
         * Store all calculated (intermediate) values
         */
        private val values: Values = mutable.Map.empty

        /**
         * Map outstanding EPKs to the last processed property result and the continuations to be executed when a new
         * result is available
         */
        private val dependees = mutable.Map.empty[SomeEPK, (SomeEOptionP, mutable.Set[() => Unit])]

        def enqueuePath(path: Path): Unit = {
            pathWorkList.enqueue(path)
        }

        def dequeuePath(): Path = {
            pathWorkList.dequeue()
        }

        def isPathWorkListEmpty: Boolean = {
            pathWorkList.isEmpty
        }

        def getPathWorkListSize: Int = {
            pathWorkList.size
        }

        def setJumpFunction(path: Path, jumpFunction: JumpFunction): Unit = {
            jumpFunctions.put(path, jumpFunction)
        }

        def getJumpFunction(path: Path): JumpFunction = {
            jumpFunctions.getOrElse(path, allTopEdgeFunction) // else part handles IDE lines 1 - 2
        }

        def lookupJumpFunctions(
            source:     Option[Statement] = None,
            sourceFact: Option[Fact]      = None,
            target:     Option[Statement] = None,
            targetFact: Option[Fact]      = None
        ): collection.Map[Path, JumpFunction] = {
            // TODO (IDE) THIS COULD BE OPTIMIZED TO SPEEDUP THE ANALYSIS
            jumpFunctions.filter {
                case (((s, sf), (t, tf)), _) =>
                    source.forall { source => s == source } &&
                        sourceFact.forall { sourceFact => sf == sourceFact } &&
                        target.forall { target => t == target } &&
                        targetFact.forall { targetFact => tf == targetFact }
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
            val set = endSummaries.getOrElseUpdate(start, mutable.Set.empty)
            set.add((end, jumpFunction))
        }

        def getEndSummaries(start: Node): collection.Set[(Node, JumpFunction)] = {
            endSummaries.getOrElse(start, Set.empty)
        }

        def rememberCallable(callable: Callable): Unit = {
            seenCallables.add(callable)
        }

        def getAllSeenCallables: collection.Set[Callable] = {
            seenCallables
        }

        def rememberCallEdge(path: Path): Unit = {
            val (source, target) = path

            val set = callTargetsToSources.getOrElseUpdate(target, mutable.Set.empty)
            set.add(source)
        }

        def lookupCallSourcesForTarget(target: Statement, targetFact: Fact): collection.Set[Node] = {
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

        def getNodeWorkListSize: Int = {
            nodeWorkList.size
        }

        def getValue(node: Node): Value = {
            values.getOrElse(node, problem.lattice.top) // else part handles IDE line 1
        }

        def setValue(node: Node, newValue: Value): Unit = {
            values.put(node, newValue)
        }

        def clearValues(): Unit = {
            values.clear()
        }

        /**
         * @return a map from statements to results and the results for the callable in total
         */
        def collectResults(callable: Callable): (
            collection.Map[Statement, collection.Set[(Fact, Value)]],
            collection.Set[(Fact, Value)]
        ) = {
            val relevantValues = values
                .filter { case ((n, d), _) =>
                    icfg.getCallable(n) == callable && d != problem.nullFact
                }

            val resultsByStatement = relevantValues
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
                .filter { case (n, _) => icfg.isNormalExitStatement(n) }
                .flatMap(_._2.toList)
                .groupMapReduce(_._1)(_._2) {
                    (value1, value2) => problem.lattice.meet(value1, value2)
                }
                .toSet

            (resultsByStatement, resultsForExit)
        }

        def addDependee(eOptionP: SomeEOptionP, c: () => Unit): Unit = {
            // The eOptionP is only inserted the first time the corresponding EPK occurs. Consequently, it is the most
            // precise property result that is seen by all dependents.
            val (_, set) = dependees.getOrElseUpdate(eOptionP.toEPK, (eOptionP, mutable.Set.empty))
            set.add(c)
        }

        def areDependeesEmpty: Boolean = {
            dependees.isEmpty
        }

        def getDependeesSize: Int = {
            dependees.size
        }

        def getDependees: collection.Set[SomeEOptionP] = {
            dependees.values.map(_._1).toSet
        }

        def getAndRemoveDependeeContinuations(eOptionP: SomeEOptionP): Set[() => Unit] = {
            dependees.remove(eOptionP.toEPK).map(_._2).getOrElse(Set.empty).toSet
        }
    }

    private val icfg: ICFG[Statement, Callable] = problem.icfg

    private val isDebug: Boolean = project.config.getBoolean(ConfigKeyDebugLog)
    private val isTrace: Boolean = project.config.getBoolean(ConfigKeyTraceLog)

    private def nodeToString(node: Node, indent: String = ""): String = {
        s"Node(\n$indent\t${icfg.stringifyStatement(node._1, s"$indent\t")},\n$indent\t${node._2}\n$indent)"
    }

    private def pathToString(path: Path, indent: String = ""): String = {
        s"Path(\n$indent\t${nodeToString(path._1, s"$indent\t")} ->\n$indent\t${nodeToString(path._2, s"$indent\t")}\n$indent)"
    }

    @unused
    protected def logWarn(message: => String): Unit = {
        OPALLogger.warn(FrameworkName, message)
    }

    protected def logDebug(message: => String): Unit = {
        OPALLogger.debug({ isDebug }, s"$FrameworkName - debug", message)
    }

    protected def logTrace(message: => String): Unit = {
        OPALLogger.debug({ isTrace }, s"$FrameworkName - trace", message)
    }

    /**
     * Run the IDE solver and calculate (and return) the result
     * @param callable the callable that should be analyzed
     * @return a result for each statement of the callable plus a result for the callable itself (combining the results
     *         of all exit statements)
     */
    // TODO (IDE) WHAT HAPPENS WHEN ANALYZING MULTIPLE CALLABLES? CAN WE CACHE E.G. JUMP/SUMMARY FUNCTIONS?
    def performAnalysis(callable: Callable): ProperPropertyComputationResult = {
        logDebug(s"performing ${getClass.getSimpleName} for $callable")

        implicit val state: State = new State

        performPhase1(callable)
        performPhase2(callable)

        createResult(callable)
    }

    /**
     * @return whether the phase is finished or has to be continued once the dependees are resolved
     */
    private def performPhase1(callable: Callable)(implicit s: State): Boolean = {
        logDebug("starting phase 1")

        seedPhase1(callable)
        processPathWorkList()

        if (s.areDependeesEmpty) {
            logDebug("finished phase 1")
            true
        } else {
            logDebug(s"there are ${s.getDependeesSize} outstanding dependees")
            logDebug("pausing phase 1")
            false
        }
    }

    /**
     * @return whether the phase is finished or has to be continued once the dependees are resolved
     */
    private def continuePhase1()(implicit s: State): Boolean = {
        logDebug("continuing phase 1")

        processPathWorkList()

        if (s.areDependeesEmpty) {
            logDebug("all outstanding dependees have been processed")
            logDebug("finished phase 1")
            true
        } else {
            logDebug(s"there are ${s.getDependeesSize} outstanding dependees left")
            logDebug("pausing phase 1 again")
            false
        }
    }

    /**
     * Perform phase 2 from scratch
     */
    private def performPhase2(callable: Callable)(implicit s: State): Unit = {
        logDebug("starting phase 2")

        // TODO (IDE) PHASE 2 IS PERFORMED ALSO FOR INTERIM RESULTS TO MAKE CYCLIC ANALYSES POSSIBLE
        //  - PHASE 2 IS PERFORMED FROM SCRATCH ON EACH UPDATE AT THE MOMENT (THIS SHOULD ALWAYS PROCUDE AN UPPER BOUND
        //      OF THE FINAL VALUE)
        //  - DO WE NEED TO RERUN IT FROM SCRATCH EACH TIME OR IS THERE AN INCREMENTAL SOLUTION?
        s.clearValues()

        seedPhase2(callable)
        computeValues()

        logDebug("finished phase 2")
    }

    private def createResult(callable: Callable)(
        implicit s: State
    ): ProperPropertyComputationResult = {
        logDebug("starting creation of properties")

        val (resultsByStatement, resultsForExit) = s.collectResults(callable)
        val propertiesByStatement = resultsByStatement.map { case (stmt, results) =>
            (stmt, propertyMetaInformation.createProperty(results))
        }
        val propertyForExit = propertyMetaInformation.createProperty(resultsForExit)

        logDebug("finished creation of properties")

        if (s.areDependeesEmpty) {
            logDebug("creating final results")
            Results(
                Iterable(
                    Result(callable, propertyForExit)
                ) ++ propertiesByStatement.map { case (stmt, property) =>
                    Result((callable, stmt), property)
                }
            )
        } else {
            logDebug("creating interim results")
            Results(
                Iterable(
                    InterimPartialResult(
                        s.getDependees.toSet,
                        onDependeeUpdateContinuation(callable)
                    ),
                    InterimResult.forUB(
                        callable,
                        propertyForExit,
                        Set.empty,
                        _ => { throw new IllegalStateException() }
                    )
                ) ++ propertiesByStatement.map { case (stmt, property) =>
                    InterimResult.forUB(
                        (callable, stmt),
                        property,
                        Set.empty,
                        _ => { throw new IllegalStateException() }
                    )
                }
            )
        }
    }

    private def onDependeeUpdateContinuation(callable: Callable)(eps: SomeEPS)(
        implicit s: State
    ): ProperPropertyComputationResult = {
        // Get and remove all continuations that are remembered for the EPS
        val cs = s.getAndRemoveDependeeContinuations(eps)

        // Call continuations
        cs.foreach(c => c())

        // The continuations can have enqueued paths to the path work list
        continuePhase1()
        performPhase2(callable)

        createResult(callable)
    }

    private def seedPhase1(callable: Callable)(implicit s: State): Unit = {
        s.rememberCallable(callable)

        // IDE P1 lines 5 - 6
        icfg.getStartStatements(callable).foreach { stmt =>
            val path = ((stmt, problem.nullFact), (stmt, problem.nullFact))
            s.enqueuePath(path)
            s.setJumpFunction(path, identityEdgeFunction)
        }

        logDebug(s"seeded with ${s.getPathWorkListSize} path(s)")
    }

    private def processPathWorkList()(implicit s: State): Unit = {
        while (!s.isPathWorkListEmpty) { // IDE P1 line 7
            val path = s.dequeuePath() // IDE P1 line 8
            val ((_, _), (n, _)) = path
            val f = s.getJumpFunction(path) // IDE P1 line 9

            logDebug("\nprocessing next path")
            logTrace(s"path=${pathToString(path)}")
            logTrace(s"current jumpFunction=$f")

            icfg.getCalleesIfCallStatement(n) match { // IDE P1 line 11
                case Some(qs) =>
                    processCallFlow(path, f, qs)
                case None if icfg.isNormalExitStatement(n) => // IDE P1 line 19
                    processExitFlow(path, f)
                case None => // IDE P1 line 30
                    processNormalFlow(path, f)
            }

            logDebug(s"${s.getPathWorkListSize} path(s) remaining after processing last path")
        }
    }

    private def processCallFlow(path: Path, f: JumpFunction, qs: collection.Set[? <: Callable])(
        implicit s: State
    ): Unit = {
        logDebug("processing as call flow")

        val ((sp, d1), (n, d2)) = path

        val rs = icfg.getNextStatements(n) // IDE P1 line 14

        qs.foreach { q =>
            logDebug(s"handling call target q=$q")

            // TODO (IDE) ALSO COLLECTS JRE METHODS -> P2 part (ii) MAY GET VERY SLOW
            s.rememberCallable(q)

            val sqs = icfg.getStartStatements(q)
            sqs.foreach { sq =>
                // IDE P1 lines 12 - 13
                val d3s = problem.getCallFlowFunction(n, sq, q).compute(d2)

                logTrace(s"generated the following d3s=$d3s for start statement sq=${icfg.stringifyStatement(sq)}")

                d3s.foreach { d3 =>
                    s.rememberCallEdge(((n, d2), (sq, d3)))

                    val endSummaries = s.getEndSummaries((sq, d3))
                    // Handling for end summaries extension
                    if (endSummaries.nonEmpty) {
                        endSummaries.foreach { case ((eq, d4), fEndSummary) =>
                            val f4 = handleEdgeFunctionResult(problem.getCallEdgeFunction(n, d2, sq, d3, q), path)
                            rs.foreach { r =>
                                val d5s = problem.getReturnFlowFunction(eq, q, r).compute(d4)
                                d5s.foreach { d5 =>
                                    val f5 = handleEdgeFunctionResult(
                                        problem.getReturnEdgeFunction(eq, d4, q, r, d5),
                                        path
                                    )
                                    val callToReturnPath = ((n, d2), (r, d5))
                                    val oldSummaryFunction = s.getSummaryFunction(callToReturnPath)
                                    val fPrime =
                                        f4.composeWith(fEndSummary).composeWith(f5).meetWith(oldSummaryFunction)

                                    if (!fPrime.equalTo(oldSummaryFunction)) {
                                        s.setSummaryFunction(callToReturnPath, fPrime)

                                        propagate(((sp, d1), (r, d5)), f.composeWith(fPrime))
                                    }
                                }
                            }
                        }
                    } else {
                        // Default algorithm behavior
                        propagate(((sq, d3), (sq, d3)), identityEdgeFunction)
                    }
                }
            }

            rs.foreach { r =>
                val d3s = problem.getCallToReturnFlowFunction(n, q, r).compute(d2)

                logTrace(s"generated the following d3s=$d3s for return-site statement r=${icfg.stringifyStatement(r)}")

                // IDE P1 lines 15 - 16
                d3s.foreach { d3 =>
                    propagate(
                        ((sp, d1), (r, d3)),
                        f.composeWith(handleEdgeFunctionResult(problem.getCallToReturnEdgeFunction(n, d2, r, d3), path))
                    )
                }

                // IDE P1 lines 17 - 18
                d3s.foreach { d3 =>
                    val f3 = s.getSummaryFunction(((n, d2), (r, d3)))
                    if (!f3.equalTo(allTopEdgeFunction)) {
                        propagate(((sp, d1), (r, d3)), f.composeWith(f3))
                    }
                }
            }
        }
    }

    private def processExitFlow(path: Path, f: JumpFunction)(implicit s: State): Unit = {
        logDebug("processing as exit flow")

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
                    logDebug(s"handling calling statement c=${icfg.stringifyStatement(c)}, d4=$d4 and return-site statement r=${icfg.stringifyStatement(r)}")

                    // IDE P1 line 21
                    val d5s = problem.getReturnFlowFunction(n, p, r).compute(d2)

                    logDebug(s"generated the following d5s=$d5s")

                    d5s.foreach { d5 =>
                        // IDE P1 lines 22 - 23
                        val f4 = handleEdgeFunctionResult(problem.getCallEdgeFunction(c, d4, sp, d1, p), path)
                        val f5 = handleEdgeFunctionResult(problem.getReturnEdgeFunction(n, d2, p, r, d5), path)

                        // IDE P1 line 24
                        val callToReturnPath = ((c, d4), (r, d5))
                        val oldSummaryFunction = s.getSummaryFunction(callToReturnPath)
                        val fPrime = f4.composeWith(f).composeWith(f5).meetWith(oldSummaryFunction)

                        // IDE P1 lines 25 - 29
                        if (!fPrime.equalTo(oldSummaryFunction)) {
                            s.setSummaryFunction(callToReturnPath, fPrime)

                            val sqs = icfg.getStartStatements(icfg.getCallable(c))
                            sqs.foreach { sq =>
                                val jumpFunctionsMatchingTarget =
                                    s.lookupJumpFunctions(source = Some(sq), target = Some(c), targetFact = Some(d4))
                                jumpFunctionsMatchingTarget.foreach {
                                    case (((_, d3), (_, _)), f3) if !f3.equalTo(allTopEdgeFunction) =>
                                        propagate(((sq, d3), (r, d5)), f3.composeWith(fPrime))
                                }
                            }
                        }
                    }
                }
        }
    }

    private def processNormalFlow(path: Path, f: JumpFunction)(implicit s: State): Unit = {
        logDebug("processing as normal flow")

        val ((sp, d1), (n, d2)) = path

        // IDE P1 lines 31 - 32
        icfg.getNextStatements(n).foreach { m =>
            val d3s = problem.getNormalFlowFunction(n, m).compute(d2)

            logTrace(s"generated the following d3s=$d3s for next statement m=${icfg.stringifyStatement(m)}")

            d3s.foreach { d3 =>
                propagate(
                    ((sp, d1), (m, d3)),
                    f.composeWith(handleEdgeFunctionResult(problem.getNormalEdgeFunction(n, d2, m, d3), path))
                )
            }
        }
    }

    private def propagate(e: Path, f: EdgeFunction[Value])(implicit s: State): Unit = {
        logTrace(s"handling propagation for path=${pathToString(e)} and f=$f")

        // IDE P1 lines 34 - 37
        val oldJumpFunction = s.getJumpFunction(e)
        val fPrime = f.meetWith(oldJumpFunction)

        if (!fPrime.equalTo(oldJumpFunction)) {
            logTrace(s"updating and re-enqueuing path as oldJumpFunction=$oldJumpFunction != fPrime=$fPrime")

            s.setJumpFunction(e, fPrime)
            s.enqueuePath(e)
        } else {
            logTrace(s"nothing to do as oldJumpFunction=$oldJumpFunction == fPrime=$fPrime")
        }
    }

    /**
     * @param path the path to re-enqueue when getting an interim edge function
     * @return the (interim) edge function from the result
     */
    private def handleEdgeFunctionResult(
        edgeFunctionResult: EdgeFunctionResult[Value],
        path:               Path
    )(
        implicit s: State
    ): EdgeFunction[Value] = {
        edgeFunctionResult match {
            case FinalEdgeFunction(edgeFunction) =>
                edgeFunction
            case InterimEdgeFunction(intermediateEdgeFunction, dependees) =>
                dependees.foreach { dependee =>
                    s.addDependee(
                        dependee,
                        () => s.enqueuePath(path)
                    )
                }
                intermediateEdgeFunction
        }
    }

    private def seedPhase2(callable: Callable)(implicit s: State): Unit = {
        // IDE P2 lines 2 - 3
        icfg.getStartStatements(callable).foreach { stmt =>
            val node = (stmt, problem.nullFact)
            s.enqueueNode(node)
            s.setValue(node, problem.lattice.bottom)
        }

        logDebug(s"seeded with ${s.getNodeWorkListSize} node(s)")
    }

    // TODO (IDE) TO SPEEDUP THE ANALYSIS WE SHOULD ONLY CALCULATE THE VALUES, WE ARE INTERESTED IN/THE USER REQUESTED.
    //  ESPECIALLY THE VALUES CALCULATED BY P2 part (ii) ARE NEVER USED CURRENTLY
    private def computeValues()(implicit s: State): Unit = {
        logDebug("starting phase 2 (i)")

        // IDE P2 part (i)
        while (!s.isNodeWorkListEmpty) { // IDE P2 line 4
            val node = s.dequeueNode() // IDE P2 line 5

            logDebug("processing next node")
            logTrace(s"node=${nodeToString(node)}")

            val (n, _) = node

            icfg.getCalleesIfCallStatement(n) match { // IDE P2 line 11
                case Some(qs) =>
                    processCallNode(node, qs)
                case None => // IDE P2 line 7
                    processStartNode(node)
            }

            logDebug(s"${s.getNodeWorkListSize} node(s) remaining after processing last node")
        }

        logDebug("finished phase 2 (i)")
        logDebug("starting phase 2 (ii)")

        // IDE P2 part (ii)
        // IDE P2 lines 15 - 17
        s.getAllSeenCallables.foreach { p =>
            val sps = icfg.getStartStatements(p)
            val ns = collectReachableStmts(sps, stmt => !icfg.isCallStatement(stmt))

            // IDE P2 line 16 - 17
            sps.foreach { sp =>
                ns.foreach { n =>
                    val jumpFunctionsMatchingTarget = s.lookupJumpFunctions(source = Some(sp), target = Some(n))
                    jumpFunctionsMatchingTarget.foreach {
                        case (((_, dPrime), (_, d)), fPrime) if !fPrime.equalTo(allTopEdgeFunction) =>
                            val nSharp = (n, d)
                            val vPrime = problem.lattice.meet(
                                s.getValue((n, d)),
                                fPrime.compute(s.getValue((sp, dPrime)))
                            )

                            logTrace(s"setting value of nSharp=${nodeToString(nSharp)} to vPrime=$vPrime")

                            s.setValue(nSharp, vPrime)
                    }
                }
            }
        }

        logDebug("finished phase 2 (ii)")
    }

    /**
     * Collect all statements that are reachable from a certain start set of statements
     * @param originStmts the statements to start searchgin from
     * @param filterPredicate an additional predicate the collected statements have to fulfill
     */
    private def collectReachableStmts(
        originStmts:     collection.Set[Statement],
        filterPredicate: Statement => Boolean
    ): collection.Set[Statement] = {
        val collectedStmts = mutable.Set.empty[Statement]
        var workingStmts = originStmts
        val seenStmts = mutable.Set.empty[Statement]

        while (workingStmts.nonEmpty) {
            collectedStmts.addAll(workingStmts.filter(filterPredicate))
            seenStmts.addAll(workingStmts)
            workingStmts = workingStmts.foldLeft(mutable.Set.empty[Statement]) { (nextStmts, stmt) =>
                nextStmts.addAll(icfg.getNextStatements(stmt))
            }.diff(seenStmts)
        }

        collectedStmts
    }

    private def processStartNode(node: Node)(implicit s: State): Unit = {
        logDebug("processing as start node")

        val (n, d) = node

        // IDE P2 line 8
        val cs = collectReachableStmts(collection.Set(n), stmt => icfg.isCallStatement(stmt))

        // IDE P2 lines 9 - 10
        cs.foreach { c =>
            val jumpFunctionsMatchingTarget =
                s.lookupJumpFunctions(source = Some(n), sourceFact = Some(d), target = Some(c))
            jumpFunctionsMatchingTarget.foreach {
                case (((_, _), (_, dPrime)), fPrime) if !fPrime.equalTo(allTopEdgeFunction) =>
                    propagateValue((c, dPrime), fPrime.compute(s.getValue((n, d))))
            }
        }
    }

    private def processCallNode(node: Node, qs: collection.Set[? <: Callable])(implicit s: State): Unit = {
        logDebug("processing as call node")

        val (n, d) = node

        // IDE P2 lines 12 - 13
        qs.foreach { q =>
            val sqs = icfg.getStartStatements(q)
            sqs.foreach { sq =>
                val dPrimes = problem.getCallFlowFunction(n, sq, q).compute(d)
                dPrimes.foreach { dPrime =>
                    propagateValue(
                        (sq, dPrime),
                        enforceFinalEdgeFunction(problem.getCallEdgeFunction(n, d, sq, dPrime, q))
                            .compute(s.getValue(node))
                    )
                }
            }
        }
    }

    private def propagateValue(nSharp: Node, v: Value)(implicit s: State): Unit = {
        logTrace(s"handling propagation for nSharp=${nodeToString(nSharp)} and v=$v")

        // IDE P2 lines 18 - 21
        val oldValue = s.getValue(nSharp)
        val vPrime = problem.lattice.meet(v, oldValue)

        if (vPrime != oldValue) {
            logTrace(s"updating and re-enqueuing node as oldValue=$oldValue != vPrime=$vPrime")

            s.setValue(nSharp, vPrime)
            s.enqueueNode(nSharp)
        } else {
            logTrace(s"nothing to do as oldValue=$oldValue == vPrime=$vPrime")
        }
    }

    // TODO (IDE) THIS WILL NOT BE POSSIBLE ANY LONGER IF RETURNING AN INTERIM RESULT INVOLVES EXECUTING PHASE 2
    private def enforceFinalEdgeFunction(edgeFunctionResult: EdgeFunctionResult[Value]): EdgeFunction[Value] = {
        edgeFunctionResult match {
            case FinalEdgeFunction(edgeFunction) =>
                edgeFunction
            case _ =>
                throw new IllegalStateException(
                    s"All edge functions should be final in phase 2 but got $edgeFunctionResult!"
                )
        }
    }
}
