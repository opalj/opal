/* BSD 2-Clause License - see OPAL/LICENSE for details. */

package org.opalj.ll.fpcf.analyses.ifds

import org.opalj.br.analyses.{ProjectInformationKeys, SomeProject}
import org.opalj.br.fpcf.{FPCFAnalysis, FPCFLazyAnalysisScheduler}
import org.opalj.fpcf._
import org.opalj.ll.LLVMProjectKey
import org.opalj.ll.llvm.{BasicBlock, Function, Instruction}
import org.opalj.tac.fpcf.analyses.ifds.{AbstractIFDSFact, IFDSProblem, NumberOfCalls, Subsumable}
import org.opalj.tac.fpcf.properties.{IFDSProperty, IFDSPropertyMetaInformation}
import org.opalj.tac.fpcf.properties.cg.Callees

import scala.collection.{mutable, Set ⇒ SomeSet}

/**
 *
 * @param ifdsProblem
 * @param propertyKey Provides the concrete property key that must be unique for every distinct concrete analysis and the lower bound for the IFDSProperty.
 * @tparam IFDSFact
 */

abstract class AbstractNativeIFDSAnalysis[IFDSFact <: AbstractIFDSFact](
        val ifdsProblem: IFDSProblem[IFDSFact, Function, LLVMStatement],
        val icfg:        ICFG[IFDSFact, Function, LLVMStatement, BasicBlock],
        val propertyKey: IFDSPropertyMetaInformation[LLVMStatement, IFDSFact]
)
    extends FPCFAnalysis
    with Subsumable[LLVMStatement, IFDSFact] {

    /**
     * Counts, how many times the abstract methods were called.
     */
    var numberOfCalls = new NumberOfCalls()

    /**
     * Counts, how many input facts are passed to callbacks.
     */
    var sumOfInputfactsForCallbacks = 0L

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
     *                      the beginning of the successor.
     */
    protected class State(
            val source:               (Function, IFDSFact),
            var pendingIfdsCallSites: Map[(Function, IFDSFact), Set[LLVMStatement]]                                                    = Map.empty,
            var pendingIfdsDependees: Map[(Function, IFDSFact), EOptionP[(Function, IFDSFact), IFDSProperty[LLVMStatement, IFDSFact]]] = Map.empty,
            var pendingCgCallSites:   Set[BasicBlock]                                                                                  = Set.empty,
            var incomingFacts:        Map[BasicBlock, Set[IFDSFact]]                                                                   = Map.empty,
            var outgoingFacts:        Map[BasicBlock, Map[BasicBlock, Set[IFDSFact]]]                                                  = Map.empty
    )

    type QueueEntry = (BasicBlock, Set[IFDSFact], Option[LLVMStatement], Option[Function], Option[IFDSFact])

    /**
     * Collects the facts valid at all exit nodes based on the current results.
     *
     * @return A map, mapping from each predecessor of all exit nodes to the facts, which hold at
     *         the exit node under the assumption that the predecessor was executed before.
     */
    protected def collectResult(implicit state: State): Map[LLVMStatement, Set[IFDSFact]]

    /**
     * Creates an IFDSProperty containing the result of this analysis.
     *
     * @param result Maps each exit statement to the facts, which hold after the exit statement.
     * @return An IFDSProperty containing the `result`.
     */
    protected def createPropertyValue(result: Map[LLVMStatement, Set[IFDSFact]]): IFDSProperty[LLVMStatement, IFDSFact] = propertyKey.create(result)

    /**
     * Determines the facts, for which a `callee` is analyzed.
     *
     * @param call The call, which calls `callee`.
     * @param callee The method, which is called by `call`.
     * @param in The facts, which hold before the `call`.
     * @return The facts, for which `callee` will be analyzed.
     */
    protected def callToStartFacts(call: LLVMStatement, callee: Function, in: Set[IFDSFact])(
        implicit
        state: State
    ): Set[IFDSFact]

    /**
     * Collects the exit facts of a `callee` and adds them to the `summaryEdges`.
     *
     * @param summaryEdges The current summary edges. They map successor statements of the `call`
     *                     to facts, which hold before they are executed.
     * @param successors The successor of `call`, which is considered.
     * @param call The statement, which calls `callee`.
     * @param callee The method, called by `call`.
     * @param exitFacts Maps exit statements of the `callee` to the facts, which hold after them.
     * @return The summary edges plus the exit to return facts for `callee` and `successor`.
     */
    protected def addExitToReturnFacts(
        summaryEdges: Map[LLVMStatement, Set[IFDSFact]],
        successors:   Set[LLVMStatement],
        call:         LLVMStatement,
        callee:       Function,
        exitFacts:    Map[LLVMStatement, Set[IFDSFact]]
    )(implicit state: State): Map[LLVMStatement, Set[IFDSFact]]

    /**
     * Performs an IFDS analysis for a method-fact-pair.
     *
     * @param entity The method-fact-pair, that will be analyzed.
     * @return An IFDS property mapping from exit statements to the facts valid after these exit
     *         statements. Returns an interim result, if the TAC or call graph of this method or the
     *         IFDS analysis for a callee is still pending.
     */
    def performAnalysis(entity: (Function, IFDSFact)): ProperPropertyComputationResult = {
        val (function, sourceFact) = entity

        // Start processing at the start of the cfg with the given source fact
        implicit val state: State =
            new State(entity, Map(entity -> Set.empty), Map())
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
            case FinalE(e: (Function, IFDSFact) @unchecked) ⇒
                reAnalyzeCalls(state.pendingIfdsCallSites(e), e._1, Some(e._2))

            case interimEUBP @ InterimEUBP(
                e: (Function, IFDSFact) @unchecked,
                ub: IFDSProperty[LLVMStatement, IFDSFact]@unchecked
                ) ⇒
                if (ub.flows.values
                    .forall(facts ⇒ facts.size == 1 && facts.forall(_ == ifdsProblem.nullFact))) {
                    // Do not re-analyze the caller if we only get the null fact.
                    // Update the pendingIfdsDependee entry to the new interim result.
                    state.pendingIfdsDependees +=
                        e -> interimEUBP
                        .asInstanceOf[EOptionP[(Function, IFDSFact), IFDSProperty[LLVMStatement, IFDSFact]]]
                } else
                    reAnalyzeCalls(state.pendingIfdsCallSites(e), e._1, Some(e._2))

            case FinalEP(_: Function, _: Callees) ⇒
                reAnalyzebasicBlocks(state.pendingCgCallSites)

            case InterimEUBP(_: Function, _: Callees) ⇒
                reAnalyzebasicBlocks(state.pendingCgCallSites)
        }

        createResult()
    }

    /**
     * Called, when some new information is found at the last node of the method.
     * This method can be overwritten by a subclass to perform additional actions.
     *
     * @param nextIn The input facts, which were found.
     * @param oldIn The input facts, which were already known, if present.
     */
    protected def foundNewInformationForLastNode(
        nextIn: Set[IFDSFact],
        oldIn:  Option[Set[IFDSFact]],
        state:  State
    ): Unit = {}

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
        basicBlock:           BasicBlock,
        call:                 LLVMStatement,
        callees:              SomeSet[Function],
        in:                   Set[IFDSFact],
        calleeWithUpdateFact: Option[IFDSFact]
    )(
        implicit
        state: State
    ): Map[LLVMStatement, Set[IFDSFact]] = {
        val successors = icfg.nextStatements(call)
        val inputFacts = beforeHandleCall(call, in)
        // Facts valid at the start of each successor
        var summaryEdges: Map[LLVMStatement, Set[IFDSFact]] = Map.empty

        /*
 * If calleeWithUpdateFact is present, this means that the basic block already has been
 * analyzed with the `inputFacts`.
 */
        if (calleeWithUpdateFact.isEmpty)
            for (successor ← successors) {
                numberOfCalls.callToReturnFlow += 1
                sumOfInputfactsForCallbacks += in.size
                summaryEdges += successor ->
                    propagateNullFact(
                        inputFacts,
                        ifdsProblem.callToReturnFlow(call, successor, inputFacts, state.source)
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
                        propagateNullFact(inputFacts, callToStartFacts(call, callee, inputFacts))
                    }
                var allNewExitFacts: Map[LLVMStatement, Set[IFDSFact]] = Map.empty
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
                                state.pendingIfdsCallSites(state.source) +
                                    ((basicBlock, call))
                            else Set((basicBlock, call))
                        state.pendingIfdsCallSites =
                            state.pendingIfdsCallSites.updated(state.source, newDependee)
                        allNewExitFacts = mergeMaps(allNewExitFacts, collectResult)
                    } else {
                        val e = (callee, fact)
                        val callFlows = propertyStore(e, propertyKey.key)
                            .asInstanceOf[EOptionP[(Function, IFDSFact), IFDSProperty[LLVMStatement, IFDSFact]]]
                        val oldValue = state.pendingIfdsDependees.get(e)
                        val oldExitFacts: Map[LLVMStatement, Set[IFDSFact]] = oldValue match {
                            case Some(ep: InterimEUBP[_, IFDSProperty[LLVMStatement, IFDSFact]]) ⇒ ep.ub.flows
                            case _ ⇒ Map.empty
                        }
                        val exitFacts: Map[LLVMStatement, Set[IFDSFact]] = callFlows match {
                            case ep: FinalEP[_, IFDSProperty[LLVMStatement, IFDSFact]] ⇒
                                if (state.pendingIfdsCallSites.contains(e)
                                    && state.pendingIfdsCallSites(e).nonEmpty) {
                                    val newDependee =
                                        state.pendingIfdsCallSites(e) - ((basicBlock, call))
                                    state.pendingIfdsCallSites = state.pendingIfdsCallSites.updated(e, newDependee)
                                }
                                state.pendingIfdsDependees -= e
                                ep.p.flows
                            case ep: InterimEUBP[_, IFDSProperty[LLVMStatement, IFDSFact]] ⇒
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
                        allNewExitFacts = mergeMaps(
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
     * This method is called at the beginning of handleCall.
     * A subclass can overwrite this method, to change the input facts of the call.
     *
     * @param call The call statement.
     * @param in The input facts, which hold before the `call`.
     * @return The changed set of input facts.
     */
    protected def beforeHandleCall(call: LLVMStatement, in: Set[IFDSFact]): Set[IFDSFact] = in

    /**
     * Merges two maps that have sets as values.
     *
     * @param map1 The first map.
     * @param map2 The second map.
     * @return A map containing the keys of both maps. Each key is mapped to the union of both maps'
     *         values.
     */
    protected def mergeMaps[S, T](map1: Map[S, Set[T]], map2: Map[S, Set[T]]): Map[S, Set[T]] = {
        var result = map1
        for ((key, values) ← map2) {
            result.get(key) match {
                case Some(resultValues) ⇒
                    if (resultValues.size > values.size)
                        result = result.updated(key, resultValues ++ values)
                    else
                        result = result.updated(key, values ++ resultValues)
                case None ⇒
                    result = result.updated(key, values)
            }
        }
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
            val allOut = mergeMaps(oldOut, nextOut).mapValues(facts ⇒ subsume(facts, project))
            state.outgoingFacts = state.outgoingFacts.updated(basicBlock, allOut)

            for (successor ← icfg.nextNodes(basicBlock)) {
                if (icfg.isLastNode(successor)) {
                    // Re-analyze recursive call sites with the same input fact.
                    val nextOutSuccessors = nextOut.get(successor)
                    if (nextOutSuccessors.isDefined && nextOutSuccessors.get.nonEmpty) {
                        val oldOutSuccessors = oldOut.get(successor)
                        if (oldOutSuccessors.isEmpty || containsNewInformation(
                            nextOutSuccessors.get,
                            oldOutSuccessors.get,
                            project
                        )) {
                            val source = state.source
                            foundNewInformationForLastNode(
                                nextOutSuccessors.get,
                                oldOutSuccessors,
                                state
                            )
                            reAnalyzeCalls(
                                state.pendingIfdsCallSites(source),
                                source._1,
                                Some(source._2)
                            )
                        }
                    }
                } else {
                    val successorBlock = successor
                    val nextIn = nextOut.getOrElse(successorBlock, Set.empty)
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
        basicBlock:           BasicBlock,
        in:                   Set[IFDSFact],
        calleeWithUpdateSite: Option[LLVMStatement],
        calleeWithUpdate:     Option[Function],
        calleeWithUpdateFact: Option[IFDSFact]
    )(
        implicit
        state: State
    ): Map[BasicBlock, Set[IFDSFact]] = {

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
        def collectInformation(statement: LLVMStatement): (Option[SomeSet[Function]], Option[IFDSFact]) = {
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
        var statement: LLVMStatement = icfg.firstStatement(basicBlock)

        // Iterate over all statements but the last one, only keeping the resulting DataFlowFacts.
        while (statement != last) {
            val (calleesO, calleeFact) = collectInformation(statement)
            val successor = icfg.nextStatement(statement)
            flows = if (calleesO.isEmpty) {
                numberOfCalls.normalFlow += 1
                sumOfInputfactsForCallbacks += in.size
                ifdsProblem.normalFlow(statement, successor, flows)
            } else
                // Inside a basic block, we only have one successor --> Take the head
                handleCall(basicBlock, statement, calleesO.get, flows, calleeFact).values.head
            statement = successor
        }

        // Analyze the last statement for each possible successor statement.
        val (calleesO, callFact) = collectInformation(last)
        var result: Map[BasicBlock, Set[IFDSFact]] =
            if (calleesO.isEmpty) {
                var result: Map[BasicBlock, Set[IFDSFact]] = Map.empty
                for (node ← icfg.nextNodes(basicBlock)) {
                    numberOfCalls.normalFlow += 1
                    sumOfInputfactsForCallbacks += in.size
                    result += node -> ifdsProblem.normalFlow(statement, icfg.firstStatement(node), flows)
                }
                result
            } else
                handleCall(basicBlock, statement, calleesO.get, flows, callFact)
                    .map(entry ⇒ entry._1.basicBlock -> entry._2)

        // Propagate the null fact.
        result = result.map(result ⇒ result._1 -> propagateNullFact(in, result._2))
        result
    }

    /**
     * Re-analyzes some basic blocks.
     *
     * @param basicBlocks The basic blocks, that will be re-analyzed.
     */
    private def reAnalyzebasicBlocks(basicBlocks: Set[BasicBlock])(implicit state: State): Unit = {
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
        callSites: Set[LLVMStatement],
        callee:    Function,
        fact:      Option[IFDSFact]
    )(implicit state: State): Unit = {
        val queue: mutable.Queue[QueueEntry] = mutable.Queue.empty
        for (callSite ← callSites) {
            val block = callSite.basicBlock
            queue.enqueue((block, state.incomingFacts(block), Some(callSite), Some(callee), fact))
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
     * @param callIndex The index of the call site.
     */
    private def addIfdsDependee(
        entity:         (Function, IFDSFact),
        calleeProperty: EOptionP[(Function, IFDSFact), IFDSProperty[LLVMStatement, IFDSFact]],
        callBB:         BasicBlock,
        call:           LLVMStatement
    )(implicit state: State): Unit = {
        val callSites = state.pendingIfdsCallSites
        state.pendingIfdsCallSites = callSites.updated(
            entity,
            callSites.getOrElse(entity, Set.empty) + ((callBB, call))
        )
        state.pendingIfdsDependees += entity -> calleeProperty
    }
}

object AbstractNativeIFDSAnalysis {
    /**
     * When true, the cross product of exit and successor in returnFLow will be optimized.
     */
    var OPTIMIZE_CROSS_PRODUCT_IN_RETURN_FLOW: Boolean = true
}

/**
 * A statement that is passed to the concrete analysis.
 *
 * @param instruction The LLVM instruction.
 */
case class LLVMStatement(instruction: Instruction) {
    def function(): Function = instruction.function
    def basicBlock(): BasicBlock = instruction.parent
}

abstract class NativeIFDSAnalysisScheduler[IFDSFact <: AbstractIFDSFact] extends FPCFLazyAnalysisScheduler {

    final override type InitializationData = AbstractNativeIFDSAnalysis[IFDSFact]

    def property: IFDSPropertyMetaInformation[LLVMStatement, IFDSFact]

    final override def derivesLazily: Some[PropertyBounds] = Some(PropertyBounds.ub(property))

    override def requiredProjectInformation: ProjectInformationKeys = Seq(LLVMProjectKey)

    override def beforeSchedule(p: SomeProject, ps: PropertyStore): Unit = {}

    /**
     * Registers the analysis as a lazy computation, that is, the method
     * will call `ProperytStore.scheduleLazyComputation`.
     */
    override def register(
        p:        SomeProject,
        ps:       PropertyStore,
        analysis: AbstractNativeIFDSAnalysis[IFDSFact]
    ): FPCFAnalysis = {
        ps.registerLazyPropertyComputation(property.key, analysis.performAnalysis)
        analysis
    }

    override def afterPhaseScheduling(ps: PropertyStore, analysis: FPCFAnalysis): Unit = {
        val ifdsAnalysis = analysis.asInstanceOf[AbstractNativeIFDSAnalysis[IFDSFact]]
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
