/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ll.fpcf.analyses.ifds

import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.{FPCFAnalysis, FPCFLazyAnalysisScheduler}
import org.opalj.fpcf._
import org.opalj.tac.fpcf.analyses.ifds.{AbstractIFDSFact, IFDSProblem, NumberOfCalls, Subsumable}
import org.opalj.tac.fpcf.properties.cg.Callees
import org.opalj.tac.fpcf.properties.{IFDSProperty, IFDSPropertyMetaInformation}

import scala.collection.{mutable, Set ⇒ SomeSet}

abstract class Statement[Node] {
    def node(): Node
}

/**
 * The state of the analysis. For each method and source fact, there is a separate state.
 *
 * @param source The input fact, for which the `method` is analyzed.
 * @param pendingIfdsCallSites Maps callees of the analyzed `method` together with their input
 *                             facts to the instruction, at which they may be called.
 * @param pendingIfdsDependees Maps callees of the analyzed `method` together with their input
 *                             facts to the intermediate result of their IFDS analysis.
 *                             Only contains method-fact-pairs, for which this analysis is
 *                             waiting for a final result.
 * @param pendingCgCallSites The basic blocks containing call sites, for which the analysis is
 *                           waiting for the final call graph result.
 * @param incomingFacts Maps each basic block to the data flow facts valid at its first
 *                      statement.
 * @param outgoingFacts Maps each basic block and successor node to the data flow facts valid at
 *                      the beginning of the successor. For exit statements the successor is None
 */
protected class IFDSState[IFDSFact <: AbstractIFDSFact, C <: AnyRef, S <: Statement[Node], Node](
        val source:               (C, IFDSFact),
        var pendingIfdsCallSites: Map[(C, IFDSFact), Set[S]]                                             = Map.empty[(C, IFDSFact), Set[S]],
        var pendingIfdsDependees: Map[(C, IFDSFact), EOptionP[(C, IFDSFact), IFDSProperty[S, IFDSFact]]] = Map.empty[(C, IFDSFact), EOptionP[(C, IFDSFact), IFDSProperty[S, IFDSFact]]],
        var pendingCgCallSites:   Set[Node]                                                              = Set.empty[Node],
        var incomingFacts:        Map[Node, Set[IFDSFact]]                                               = Map.empty[Node, Set[IFDSFact]],
        var outgoingFacts:        Map[Node, Map[Option[Node], Set[IFDSFact]]]                            = Map.empty[Node, Map[Option[Node], Set[IFDSFact]]]
)

protected class Statistics {

    /**
     * Counts, how many times the abstract methods were called.
     */
    var numberOfCalls = new NumberOfCalls()

    /**
     * Counts, how many input facts are passed to callbacks.
     */
    var sumOfInputfactsForCallbacks = 0L
}

/**
 *
 * @param ifdsProblem
 * @param propertyKey Provides the concrete property key that must be unique for every distinct concrete analysis and the lower bound for the IFDSProperty.
 * @tparam IFDSFact
 */
class IFDSAnalysis[IFDSFact <: AbstractIFDSFact, C <: AnyRef, S <: Statement[Node], Node](
        implicit
        val ifdsProblem: IFDSProblem[IFDSFact, C, S],
        val icfg:        ICFG[IFDSFact, C, S, Node],
        val propertyKey: IFDSPropertyMetaInformation[S, IFDSFact]
) extends FPCFAnalysis
    with Subsumable[S, IFDSFact] {
    type State = IFDSState[IFDSFact, C, S, Node]
    override val project: SomeProject = ifdsProblem.project

    implicit var statistics = new Statistics

    type QueueEntry = (Node, Set[IFDSFact], Option[S], Option[C], Option[IFDSFact])

    /**
     * Creates an IFDSProperty containing the result of this analysis.
     *
     * @param result Maps each exit statement to the facts, which hold after the exit statement.
     * @return An IFDSProperty containing the `result`.
     */
    protected def createPropertyValue(result: Map[S, Set[IFDSFact]]): IFDSProperty[S, IFDSFact] =
        propertyKey.create(result)

    /**
     * Performs an IFDS analysis for a method-fact-pair.
     *
     * @param entity The method-fact-pair, that will be analyzed.
     * @return An IFDS property mapping from exit statements to the facts valid after these exit
     *         statements. Returns an interim result, if the TAC or call graph of this method or the
     *         IFDS analysis for a callee is still pending.
     */
    def performAnalysis(entity: (C, IFDSFact)): ProperPropertyComputationResult = {
        val (function, sourceFact) = entity

        // Start processing at the start of the cfg with the given source fact
        implicit val state: State =
            new State(entity, Map(entity -> Set.empty))
        val queue = mutable.Queue
            .empty[QueueEntry]
        icfg.startNodes(sourceFact, function).foreach { start ⇒
            state.incomingFacts += start -> Set(sourceFact)
            queue.enqueue((start, Set(sourceFact), None, None, None))
        }
        process(queue)
        createResult()
    }

    /**
     * Creates the current (intermediate) result for the analysis.
     *
     * @return A result containing a map, which maps each exit statement to the facts valid after
     *         the statement, based on the current results. If the analysis is still waiting for its
     *         method's TAC or call graph or the result of another method-fact-pair, an interim
     *         result will be returned.
     *
     */
    protected def createResult()(implicit state: State): ProperPropertyComputationResult = {
        val propertyValue = createPropertyValue(collectResult)
        val dependees = state.pendingIfdsDependees.values
        if (dependees.isEmpty) Result(state.source, propertyValue)
        else InterimResult.forUB(state.source, propertyValue, dependees.toSet, propertyUpdate)
    }

    /**
     * Called, when there is an updated result for a tac, call graph or another method-fact-pair,
     * for which the current analysis is waiting. Re-analyzes the relevant parts of this method and
     * returns the new analysis result.
     *
     * @param eps The new property value.
     * @return The new (interim) result of this analysis.
     */
    protected def propertyUpdate(
        eps: SomeEPS
    )(implicit state: State): ProperPropertyComputationResult = {
        (eps: @unchecked) match {
            case FinalE(e: (C, IFDSFact) @unchecked) ⇒
                reAnalyzeCalls(state.pendingIfdsCallSites(e), e._1, Some(e._2))

            case interimEUBP @ InterimEUBP(
                e: (C, IFDSFact) @unchecked,
                ub: IFDSProperty[S, IFDSFact] @unchecked
                ) ⇒
                if (ub.flows.values
                    .forall(facts ⇒ facts.size == 1 && facts.forall(_ == ifdsProblem.nullFact))) {
                    // Do not re-analyze the caller if we only get the null fact.
                    // Update the pendingIfdsDependee entry to the new interim result.
                    state.pendingIfdsDependees +=
                        e -> interimEUBP
                        .asInstanceOf[EOptionP[(C, IFDSFact), IFDSProperty[S, IFDSFact]]]
                } else
                    reAnalyzeCalls(state.pendingIfdsCallSites(e), e._1, Some(e._2))

            case FinalEP(_: C @unchecked, _: Callees) ⇒
                reAnalyzeBasicBlocks(state.pendingCgCallSites)

            case InterimEUBP(_: C @unchecked, _: Callees) ⇒
                reAnalyzeBasicBlocks(state.pendingCgCallSites)
        }

        createResult()
    }

    /**
     * Processes a statement with a call.
     *
     * @param basicBlock The basic block, which contains the `call`.
     * @param call The call statement.
     * @param callees All possible callees.
     * @param in The facts, which hold before the call statement.
     * @param calleeWithUpdateFact If present, the `callees` will only be analyzed with this fact
     *                             instead of the facts returned by callToStartFacts.
     * @return A map, mapping from each successor statement of the `call` to the facts, which hold
     *         at their start.
     */
    protected def handleCall(
        basicBlock:           Node,
        call:                 S,
        callees:              SomeSet[C],
        in:                   Set[IFDSFact],
        calleeWithUpdateFact: Option[IFDSFact]
    )(
        implicit
        state: State
    ): Map[S, Set[IFDSFact]] = {
        val successors = icfg.nextStatements(call)
        // Facts valid at the start of each successor
        var summaryEdges: Map[S, Set[IFDSFact]] = Map.empty

        /*
     * If calleeWithUpdateFact is present, this means that the basic block already has been
     * analyzed with the `inputFacts`.
     */
        if (calleeWithUpdateFact.isEmpty)
            for (successor ← successors) {
                statistics.numberOfCalls.callToReturnFlow += 1
                statistics.sumOfInputfactsForCallbacks += in.size
                summaryEdges += successor ->
                    propagateNullFact(
                        in,
                        ifdsProblem.callToReturnFlow(call, successor, in, state.source)
                    )
            }

        for (callee ← callees) {
            if (!ifdsProblem.insideAnalysisContext(callee)) {
                // Let the concrete analysis decide what to do.
                for {
                    successor ← successors
                } summaryEdges +=
                    successor -> (summaryEdges(successor) ++
                        ifdsProblem.callOutsideOfAnalysisContext(call, callee, successor, in))
            } else {
                val callToStart =
                    if (calleeWithUpdateFact.isDefined) Set(calleeWithUpdateFact.get)
                    else {
                        propagateNullFact(in, callToStartFacts(call, callee, in))
                    }
                var allNewExitFacts: Map[S, Set[IFDSFact]] = Map.empty
                // Collect exit facts for each input fact separately
                for (fact ← callToStart) {
                    /*
           * If this is a recursive call with the same input facts, we assume that the
           * call only produces the facts that are already known. The call site is added to
           * `pendingIfdsCallSites`, so that it will be re-evaluated if new output facts
           * become known for the input fact.
           */
                    if ((callee eq state.source._1) && fact == state.source._2) {
                        val newDependee =
                            if (state.pendingIfdsCallSites.contains(state.source))
                                state.pendingIfdsCallSites(state.source) + call
                            else Set(call)
                        state.pendingIfdsCallSites =
                            state.pendingIfdsCallSites.updated(state.source, newDependee)
                        allNewExitFacts = IFDS.mergeMaps(allNewExitFacts, collectResult)
                    } else {
                        val e = (callee, fact)
                        val callFlows = propertyStore(e, propertyKey.key)
                            .asInstanceOf[EOptionP[(C, IFDSFact), IFDSProperty[S, IFDSFact]]]
                        val oldValue = state.pendingIfdsDependees.get(e)
                        val oldExitFacts: Map[S, Set[IFDSFact]] = oldValue match {
                            case Some(ep: InterimEUBP[_, IFDSProperty[S, IFDSFact]]) ⇒ ep.ub.flows
                            case _                                                   ⇒ Map.empty
                        }
                        val exitFacts: Map[S, Set[IFDSFact]] = callFlows match {
                            case ep: FinalEP[_, IFDSProperty[S, IFDSFact]] ⇒
                                if (state.pendingIfdsCallSites.contains(e)
                                    && state.pendingIfdsCallSites(e).nonEmpty) {
                                    val newDependee =
                                        state.pendingIfdsCallSites(e) - call
                                    state.pendingIfdsCallSites = state.pendingIfdsCallSites.updated(e, newDependee)
                                }
                                state.pendingIfdsDependees -= e
                                ep.p.flows
                            case ep: InterimEUBP[_, IFDSProperty[S, IFDSFact]] ⇒
                                /*
                 * Add the call site to `pendingIfdsCallSites` and
                 * `pendingIfdsDependees` and continue with the facts in the interim
                 * result for now. When the analysis for the callee finishes, the
                 * analysis for this call site will be triggered again.
                 */
                                addIfdsDependee(e, callFlows, basicBlock, call)
                                ep.ub.flows
                            case _ ⇒
                                addIfdsDependee(e, callFlows, basicBlock, call)
                                Map.empty
                        }
                        // Only process new facts that are not in `oldExitFacts`
                        allNewExitFacts = IFDS.mergeMaps(
                            allNewExitFacts,
                            filterNewInformation(exitFacts, oldExitFacts, project)
                        )
                        /*
             * If new exit facts were discovered for the callee-fact-pair, all call
             * sites depending on this pair have to be re-evaluated. oldValue is
             * undefined if the callee-fact pair has not been queried before or returned
             *  a FinalEP.
             */
                        if (oldValue.isDefined && oldExitFacts != exitFacts) {
                            reAnalyzeCalls(
                                state.pendingIfdsCallSites(e),
                                e._1,
                                Some(e._2)
                            )
                        }
                    }
                }
                summaryEdges = addExitToReturnFacts(summaryEdges, successors, call, callee, allNewExitFacts)
            }
        }
        summaryEdges
    }

    /**
     * Collects the facts valid at all exit nodes based on the current results.
     *
     * @return A map, mapping from each predecessor of all exit nodes to the facts, which hold at
     *         the exit node under the assumption that the predecessor was executed before.
     */
    protected def collectResult(implicit state: State): Map[S, Set[IFDSFact]] = {
        var result = Map.empty[S, Set[IFDSFact]]
        state.outgoingFacts.foreach(
            blockFacts ⇒
                blockFacts._2.get(None) match {
                    case Some(facts) ⇒ result += icfg.lastStatement(blockFacts._1) -> facts
                }
        )
        result
    }

    /**
     * Calls callFlow.
     */
    protected def callToStartFacts(call: S, callee: C, in: Set[IFDSFact])(
        implicit
        state: State
    ): Set[IFDSFact] = {
        statistics.numberOfCalls.callFlow += 1
        statistics.sumOfInputfactsForCallbacks += in.size
        ifdsProblem.callFlow(call, callee, in, state.source)
    }

    /**
     * Combines each normal exit node with each normal successor and each abnormal exit statement
     * with each catch node. Calls returnFlow for those pairs and adds them to the summary edges.
     */
    protected def addExitToReturnFacts(
        summaryEdges: Map[S, Set[IFDSFact]],
        successors:   Set[S],
        call:         S,
        callee:       C,
        exitFacts:    Map[S, Set[IFDSFact]]
    ): Map[S, Set[IFDSFact]] = {
        // First process for normal returns, then abnormal returns.
        var result = summaryEdges
        for {
            successor ← successors
            exitStatement ← exitFacts.keys
        } result = addSummaryEdge(result, call, exitStatement, successor, callee, exitFacts)
        result
    }

    /**
     * Analyzes a queue of BasicBlocks.
     *
     * @param worklist A queue of the following elements:
     *        bb The basic block that will be analyzed.
     *        in New data flow facts found to hold at the beginning of the basic block.
     *        calleeWithUpdateIndex If the basic block is analyzed because there is new information
     *        for a callee, this is the call site's index.
     *        calleeWithUpdate If the basic block is analyzed because there is new information for a
     *        callee, this is the callee.
     *        calleeWithUpdateFact If the basic block is analyzed because there is new information
     *        for a callee with a specific input fact, this is the input fact.
     */
    private def process(
        worklist: mutable.Queue[QueueEntry]
    )(implicit state: State): Unit = {
        while (worklist.nonEmpty) {
            val (basicBlock, in, calleeWithUpdateSite, calleeWithUpdate, calleeWithUpdateFact) =
                worklist.dequeue()
            val oldOut = state.outgoingFacts.getOrElse(basicBlock, Map.empty)
            val nextOut = analyzeBasicBlock(
                basicBlock,
                in,
                calleeWithUpdateSite,
                calleeWithUpdate,
                calleeWithUpdateFact
            )
            val allOut = IFDS.mergeMaps(oldOut, nextOut).mapValues(facts ⇒ subsume(facts, project))
            state.outgoingFacts = state.outgoingFacts.updated(basicBlock, allOut)

            for (successor ← icfg.nextNodes(basicBlock)) {
                if (icfg.isLastNode(successor)) {
                    // Re-analyze recursive call sites with the same input fact.
                    val nextOutSuccessors = nextOut.get(Some(successor))
                    if (nextOutSuccessors.isDefined && nextOutSuccessors.get.nonEmpty) {
                        val oldOutSuccessors = oldOut.get(Some(successor))
                        if (oldOutSuccessors.isEmpty || containsNewInformation(
                            nextOutSuccessors.get,
                            oldOutSuccessors.get,
                            project
                        )) {
                            val source = state.source
                            reAnalyzeCalls(
                                state.pendingIfdsCallSites(source),
                                source._1,
                                Some(source._2)
                            )
                        }
                    }
                } else {
                    val successorBlock = successor
                    val nextIn = nextOut.getOrElse(Some(successorBlock), Set.empty)
                    val oldIn = state.incomingFacts.getOrElse(successorBlock, Set.empty)
                    val newIn = notSubsumedBy(nextIn, oldIn, project)
                    val mergedIn =
                        if (nextIn.size > oldIn.size) nextIn ++ oldIn
                        else oldIn ++ nextIn
                    state.incomingFacts =
                        state.incomingFacts.updated(successorBlock, subsume(mergedIn, project))
                    /*
           * Only process the successor with new facts.
           * It is analyzed at least one time because of the null fact.
           */
                    if (newIn.nonEmpty) worklist.enqueue((successorBlock, newIn, None, None, None))
                }
            }
        }
    }

    /**
     * Computes for one basic block the facts valid on each CFG edge leaving the block if `sources`
     * held before the block.
     *
     * @param basicBlock The basic block, that will be analyzed.
     * @param in The facts, that hold before the block.
     * @param calleeWithUpdateSite If the basic block is analyzed because there is new information
     *                              for a callee, this is the call site.
     * @param calleeWithUpdate If the basic block is analyzed because there is new information for
     *                         a callee, this is the callee.
     * @param calleeWithUpdateFact If the basic block is analyzed because there is new information
     *                             for a callee with a specific input fact, this is the input fact.
     * @return A map, mapping each successor node to its input facts. Instead of catch nodes, this
     *         map contains their handler nodes.
     */
    private def analyzeBasicBlock(
        basicBlock:           Node,
        in:                   Set[IFDSFact],
        calleeWithUpdateSite: Option[S],
        calleeWithUpdate:     Option[C],
        calleeWithUpdateFact: Option[IFDSFact]
    )(
        implicit
        state: State
    ): Map[Option[Node], Set[IFDSFact]] = {

        /*
     * Collects information about a statement.
     *
     * @param index The statement's index.
     * @return A tuple of the following elements:
     *         statement: The statement at `index`.
     *         callees: The methods possibly called at this statement, if it contains a call.
     *                 If `index` equals `calleeWithUpdateIndex`, only `calleeWithUpdate` will
     *                 be returned.
     *         calleeFact: If `index` equals `calleeWithUpdateIndex`, only
     *         `calleeWithUpdateFact` will be returned, None otherwise.
     */
        def collectInformation(statement: S): (Option[SomeSet[C]], Option[IFDSFact]) = {
            val calleesO =
                if (calleeWithUpdateSite.contains(statement)) calleeWithUpdate.map(Set(_))
                else icfg.getCalleesIfCallStatement(statement)
            val calleeFact =
                if (calleeWithUpdateSite.contains(statement)) calleeWithUpdateFact
                else None
            (calleesO, calleeFact)
        }

        val last = icfg.lastStatement(basicBlock)
        var flows: Set[IFDSFact] = in
        var statement: S = icfg.firstStatement(basicBlock)

        // Iterate over all statements but the last one, only keeping the resulting DataFlowFacts.
        while (statement != last) {
            val (calleesO, calleeFact) = collectInformation(statement)
            val successor = icfg.nextStatement(statement)
            flows = if (calleesO.isEmpty) {
                statistics.numberOfCalls.normalFlow += 1
                statistics.sumOfInputfactsForCallbacks += in.size
                ifdsProblem.normalFlow(statement, Some(successor), flows)
            } else
                // Inside a basic block, we only have one successor --> Take the head
                handleCall(basicBlock, statement, calleesO.get, flows, calleeFact).values.head
            statement = successor
        }

        // Analyze the last statement for each possible successor statement.
        val (calleesO, callFact) = collectInformation(last)
        var result: Map[Option[Node], Set[IFDSFact]] =
            if (calleesO.isEmpty) {
                var result: Map[Option[Node], Set[IFDSFact]] = Map.empty
                for (node ← icfg.nextNodes(basicBlock)) {
                    statistics.numberOfCalls.normalFlow += 1
                    statistics.sumOfInputfactsForCallbacks += in.size
                    result += Some(node) -> ifdsProblem.normalFlow(
                        statement,
                        Some(icfg.firstStatement(node)),
                        flows
                    )
                }
                if (icfg.isExitStatement(last)) {
                    statistics.numberOfCalls.normalFlow += 1
                    statistics.sumOfInputfactsForCallbacks += in.size
                    result += None -> ifdsProblem.normalFlow(statement, None, flows)
                }
                result
            } else
                handleCall(basicBlock, statement, calleesO.get, flows, callFact)
                    .map(entry ⇒ Some(entry._1.node) -> entry._2)

        // Propagate the null fact.
        result = result.map(result ⇒ result._1 -> propagateNullFact(in, result._2))
        result
    }

    /**
     * Re-analyzes some basic blocks.
     *
     * @param basicBlocks The basic blocks, that will be re-analyzed.
     */
    private def reAnalyzeBasicBlocks(basicBlocks: Set[Node])(implicit state: State): Unit = {
        val queue: mutable.Queue[QueueEntry] = mutable.Queue.empty
        for (bb ← basicBlocks) queue.enqueue((bb, state.incomingFacts(bb), None, None, None))
        process(queue)
    }

    /**
     * Re-analyzes some call sites with respect to one specific callee.
     *
     * @param callSites The call sites, which are analyzed.
     * @param callee The callee, which will be considered at the `callSites`.
     * @param fact If defined, the `callee` will only be analyzed for this fact.
     */
    private def reAnalyzeCalls(
        callSites: Set[S],
        callee:    C,
        fact:      Option[IFDSFact]
    )(implicit state: State): Unit = {
        val queue: mutable.Queue[QueueEntry] = mutable.Queue.empty
        for (callSite ← callSites) {
            val node = callSite.node
            queue.enqueue((node, state.incomingFacts(node), Some(callSite), Some(callee), fact))
        }
        process(queue)
    }

    /**
     * If `from` contains a null fact, it will be added to `to`.
     *
     * @param from The set, which may contain the null fact initially.
     * @param to The set, to which the null fact may be added.
     * @return `to` with the null fact added, if it is contained in `from`.
     */
    private def propagateNullFact(from: Set[IFDSFact], to: Set[IFDSFact]): Set[IFDSFact] = {
        if (from.contains(ifdsProblem.nullFact)) to + ifdsProblem.nullFact
        else to
    }

    /**
     * Adds a method-fact-pair as to the IFDS call sites and dependees.
     *
     * @param entity The method-fact-pair.
     * @param calleeProperty The property, that was returned for `entity`.
     * @param callBB The basic block of the call site.
     * @param call The call site.
     */
    private def addIfdsDependee(
        entity:         (C, IFDSFact),
        calleeProperty: EOptionP[(C, IFDSFact), IFDSProperty[S, IFDSFact]],
        callBB:         Node,
        call:           S
    )(implicit state: State): Unit = {
        val callSites = state.pendingIfdsCallSites
        state.pendingIfdsCallSites = callSites.updated(
            entity,
            callSites.getOrElse(entity, Set.empty) + call
        )
        state.pendingIfdsDependees += entity -> calleeProperty
    }

    /**
     * Adds a summary edge for a call to a map representing summary edges.
     *
     * @param summaryEdges The current map representing the summary edges.
     *                     Maps from successor statements to facts, which hold at their beginning.
     * @param call The call, calling the `callee`.
     * @param exitStatement The exit statement for the new summary edge.
     * @param successor The successor statement of the call for the new summary edge.
     * @param callee The callee, called by `call`.
     * @param allNewExitFacts A map, mapping from the exit statements of `callee` to their newly
     *                        found exit facts.
     * @return `summaryEdges` with an additional or updated summary edge from `call` to `successor`.
     */
    private def addSummaryEdge(
        summaryEdges:    Map[S, Set[IFDSFact]],
        call:            S,
        exitStatement:   S,
        successor:       S,
        callee:          C,
        allNewExitFacts: Map[S, Set[IFDSFact]]
    ): Map[S, Set[IFDSFact]] = {
        val in = allNewExitFacts.getOrElse(exitStatement, Set.empty)
        statistics.numberOfCalls.returnFlow += 1
        statistics.sumOfInputfactsForCallbacks += in.size
        val returned = ifdsProblem.returnFlow(call, callee, exitStatement, successor, in)
        val newFacts =
            if (summaryEdges.contains(successor) && summaryEdges(successor).nonEmpty) {
                val summaryForSuccessor = summaryEdges(successor)
                if (summaryForSuccessor.size >= returned.size) summaryForSuccessor ++ returned
                else returned ++ summaryForSuccessor
            } else returned
        summaryEdges.updated(successor, newFacts)
    }
}

abstract class IFDSAnalysisScheduler[IFDSFact <: AbstractIFDSFact, C <: AnyRef, S <: Statement[Node], Node]
    extends FPCFLazyAnalysisScheduler {
    final override type InitializationData = IFDSAnalysis[IFDSFact, C, S, Node]
    def property: IFDSPropertyMetaInformation[S, IFDSFact]
    final override def derivesLazily: Some[PropertyBounds] = Some(PropertyBounds.ub(property))
    override def beforeSchedule(p: SomeProject, ps: PropertyStore): Unit = {}

    /**
     * Registers the analysis as a lazy computation, that is, the method
     * will call `ProperytStore.scheduleLazyComputation`.
     */
    override def register(
        p:        SomeProject,
        ps:       PropertyStore,
        analysis: IFDSAnalysis[IFDSFact, C, S, Node]
    ): FPCFAnalysis = {
        ps.registerLazyPropertyComputation(property.key, analysis.performAnalysis)
        analysis
    }

    override def afterPhaseScheduling(ps: PropertyStore, analysis: FPCFAnalysis): Unit = {
        val ifdsAnalysis = analysis.asInstanceOf[IFDSAnalysis[IFDSFact, C, S, Node]]
        for (e ← ifdsAnalysis.ifdsProblem.entryPoints) {
            ps.force(e, ifdsAnalysis.propertyKey.key)
        }
    }

    override def afterPhaseCompletion(
        p:        SomeProject,
        ps:       PropertyStore,
        analysis: FPCFAnalysis
    ): Unit = {}
}
