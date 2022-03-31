/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package ifds

import com.typesafe.config.ConfigValueFactory
import org.opalj.ai.domain.l0.PrimitiveTACAIDomain
import org.opalj.ai.domain.l2
import org.opalj.ai.fpcf.properties.AIDomainFactoryKey
import org.opalj.br.{DeclaredMethod, DefinedMethod, Method, ObjectType}
import org.opalj.br.analyses.{DeclaredMethods, DeclaredMethodsKey, Project, ProjectInformationKeys, SomeProject}
import org.opalj.br.cfg.{BasicBlock, CFG, CFGNode}
import org.opalj.br.fpcf.{FPCFAnalysesManagerKey, FPCFAnalysis, FPCFLazyAnalysisScheduler, PropertyStoreKey}
import org.opalj.fpcf.seq.PKESequentialPropertyStore
import org.opalj.fpcf._
import org.opalj.ifds.old.{IFDSProblem, NumberOfCalls, Subsumable}
import org.opalj.ifds.{AbstractIFDSFact, IFDSProperty, IFDSPropertyMetaInformation, Statement}
import org.opalj.tac.cg.{RTACallGraphKey, TypeProviderKey}
import org.opalj.tac.fpcf.analyses.cg.TypeProvider
import org.opalj.tac.fpcf.analyses.ifds.AbstractIFDSAnalysis.V
import org.opalj.tac.fpcf.properties.{TACAI, TheTACAI}
import org.opalj.tac.fpcf.properties.cg.{Callees, Callers}
import org.opalj.util.Milliseconds
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.value.ValueInformation

import java.io.{File, PrintWriter}
import javax.swing.JOptionPane
import scala.collection.{mutable, Set ⇒ SomeSet}

/**
 *
 * @param ifdsProblem
 * @param propertyKey Provides the concrete property key that must be unique for every distinct concrete analysis and the lower bound for the IFDSProperty.
 * @tparam IFDSFact
 */
abstract class AbstractIFDSAnalysis[IFDSFact <: AbstractIFDSFact](
        val ifdsProblem: IFDSProblem[IFDSFact, DeclaredMethod, JavaStatement, CFGNode],
        val propertyKey: IFDSPropertyMetaInformation[JavaStatement, IFDSFact]
) extends FPCFAnalysis
    with Subsumable[JavaStatement, IFDSFact] {

    /**
     * All declared methods in the project.
     */
    implicit final protected val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)

    implicit final protected val typeProvider: TypeProvider = project.get(TypeProviderKey)

    /**
     * Counts, how many times the abstract methods were called.
     */
    var numberOfCalls = new NumberOfCalls()

    /**
     * Counts, how many input facts are passed to callbacks.
     */
    var sumOfInputFactsForCallbacks = 0L

    /**
     * The state of the analysis. For each method and source fact, there is a separate state.
     *
     * @param declaringClass The class defining the analyzed `method`.
     * @param method The analyzed method.
     * @param source The input fact, for which the `method` is analyzed.
     * @param code The code of the `method`.
     * @param cfg The control flow graph of the `method`.
     * @param pendingIfdsCallSites Maps callees of the analyzed `method` together with their input
     *                             facts to the basic block and statement index, at which they may
     *                             be called.
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
            val declaringClass:       ObjectType,
            val method:               Method,
            val source:               (DeclaredMethod, IFDSFact),
            val code:                 Array[Stmt[V]],
            val cfg:                  CFG[Stmt[V], TACStmts[V]],
            var pendingIfdsCallSites: Map[(DeclaredMethod, IFDSFact), Set[(BasicBlock, Int)]],
            var pendingTacCallSites:  Map[DeclaredMethod, Set[BasicBlock]]                                                                         = Map.empty,
            var pendingIfdsDependees: Map[(DeclaredMethod, IFDSFact), EOptionP[(DeclaredMethod, IFDSFact), IFDSProperty[JavaStatement, IFDSFact]]] = Map.empty,
            var pendingTacDependees:  Map[Method, EOptionP[Method, TACAI]]                                                                         = Map.empty,
            var pendingCgCallSites:   Set[BasicBlock]                                                                                              = Set.empty,
            var incomingFacts:        Map[BasicBlock, Set[IFDSFact]]                                                                               = Map.empty,
            var outgoingFacts:        Map[BasicBlock, Map[CFGNode, Set[IFDSFact]]]                                                                 = Map.empty
    )

    /**
     * Determines the basic blocks, at which the analysis starts.
     *
     * @param sourceFact The source fact of the analysis.
     * @param cfg The control flow graph of the analyzed method.
     * @return The basic blocks, at which the analysis starts.
     */
    protected def startBlocks(sourceFact: IFDSFact, cfg: CFG[Stmt[V], TACStmts[V]]): Set[BasicBlock]

    /**
     * Collects the facts valid at all exit nodes based on the current results.
     *
     * @return A map, mapping from each predecessor of all exit nodes to the facts, which hold at
     *         the exit node under the assumption that the predecessor was executed before.
     */
    protected def collectResult(implicit state: State): Map[JavaStatement, Set[IFDSFact]]

    /**
     * Creates an IFDSProperty containing the result of this analysis.
     *
     * @param result Maps each exit statement to the facts, which hold after the exit statement.
     * @return An IFDSProperty containing the `result`.
     */
    protected def createPropertyValue(
        result: Map[JavaStatement, Set[IFDSFact]]
    ): IFDSProperty[JavaStatement, IFDSFact] = propertyKey.create(result)

    /**
     * Determines the nodes, that will be analyzed after some `basicBlock`.
     *
     * @param basicBlock The basic block, that was analyzed before.
     * @return The nodes, that will be analyzed after `basicBlock`.
     */
    protected def nextNodes(basicBlock: BasicBlock): Set[CFGNode]

    /**
     * Checks, if some `node` is the last node.
     *
     * @return True, if `node` is the last node, i.e. there is no next node.
     */
    protected def isLastNode(node: CFGNode): Boolean

    /**
     * If the passed `node` is a catch node, all successors of this catch node are determined.
     *
     * @param node The node.
     * @return If the node is a catch node, all its successors will be returned.
     *         Otherwise, the node itself will be returned.
     */
    protected def skipCatchNode(node: CFGNode): Set[BasicBlock]

    /**
     * Determines the first index of some `basic block`, that will be analyzed.
     *
     * @param basicBlock The basic block.
     * @return The first index of some `basic block`, that will be analyzed.
     */
    protected def firstIndex(basicBlock: BasicBlock): Int

    /**
     * Determines the last index of some `basic block`, that will be analzyed.
     *
     * @param basicBlock The basic block.
     * @return The last index of some `basic block`, that will be analzyed.
     */
    protected def lastIndex(basicBlock: BasicBlock): Int

    /**
     * Determines the index, that will be analyzed after some other `index`.
     *
     * @param index The source index.
     * @return The index, that will be analyzed after `index`.
     */
    protected def nextIndex(index: Int): Int

    /**
     * Gets the first statement of a node, that will be analyzed.
     *
     * @param node The node.
     * @return The first statement of a node, that will be analyzed.
     */
    protected def firstStatement(node: CFGNode)(implicit state: State): JavaStatement

    /**
     * Determines the statement, that will be analyzed after some other `statement`.
     *
     * @param statement The source statement.
     * @return The successor statements
     */
    protected def nextStatements(statement: JavaStatement)(implicit state: State): Set[JavaStatement]

    /**
     * Determines the facts, for which a `callee` is analyzed.
     *
     * @param call The call, which calls `callee`.
     * @param callee The method, which is called by `call`.
     * @param in The facts, which hold before the `call`.
     * @return The facts, for which `callee` will be analyzed.
     */
    protected def callToStartFacts(call: JavaStatement, callee: DeclaredMethod, in: Set[IFDSFact])(
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
        summaryEdges: Map[JavaStatement, Set[IFDSFact]],
        successors:   Set[JavaStatement],
        call:         JavaStatement,
        callee:       DeclaredMethod,
        exitFacts:    Map[JavaStatement, Set[IFDSFact]]
    )(implicit state: State): Map[JavaStatement, Set[IFDSFact]]

    /**
     * Performs an IFDS analysis for a method-fact-pair.
     *
     * @param entity The method-fact-pair, that will be analyzed.
     * @return An IFDS property mapping from exit statements to the facts valid after these exit
     *         statements. Returns an interim result, if the TAC or call graph of this method or the
     *         IFDS analysis for a callee is still pending.
     */
    def performAnalysis(entity: (DeclaredMethod, IFDSFact)): ProperPropertyComputationResult = {
        val (declaredMethod, sourceFact) = entity

        /*
     * The analysis can only handle single defined methods.
     * If a method is not single defined, this analysis assumes that it does not create any
     * facts.
     */
        if (!declaredMethod.hasSingleDefinedMethod)
            return Result(entity, createPropertyValue(Map.empty));

        ifdsProblem.specialCase(entity, propertyKey) match {
            case Some(result) ⇒ return result;
            case _            ⇒
        }

        val method = declaredMethod.definedMethod
        val declaringClass: ObjectType = method.classFile.thisType

        /*
     * Fetch the method's three address code. If it is not present, return an empty interim
     * result.
     */
        val (code, cfg) = propertyStore(method, TACAI.key) match {
            case FinalP(TheTACAI(tac)) ⇒ (tac.stmts, tac.cfg)

            case epk: EPK[Method, TACAI] ⇒
                return InterimResult.forUB(
                    entity,
                    createPropertyValue(Map.empty),
                    Set(epk),
                    _ ⇒ performAnalysis(entity)
                );

            case tac ⇒
                throw new UnknownError(s"can't handle intermediate TACs ($tac)");
        }

        // Start processing at the start of the cfg with the given source fact
        implicit val state: State =
            new State(declaringClass, method, entity, code, cfg, Map(entity -> Set.empty), Map())
        val queue = mutable.Queue
            .empty[(BasicBlock, Set[IFDSFact], Option[Int], Option[Method], Option[IFDSFact])]
        startBlocks(sourceFact, cfg).foreach { start ⇒
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
        val dependees = state.pendingIfdsDependees.values ++ state.pendingTacDependees.values
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
            case FinalE(e: (DeclaredMethod, IFDSFact) @unchecked) ⇒
                reAnalyzeCalls(state.pendingIfdsCallSites(e), e._1.definedMethod, Some(e._2))

            case interimEUBP @ InterimEUBP(
                e: (DeclaredMethod, IFDSFact) @unchecked,
                ub: IFDSProperty[JavaStatement, IFDSFact] @unchecked
                ) ⇒
                if (ub.flows.values
                    .forall(facts ⇒ facts.size == 1 && facts.forall(_ == ifdsProblem.nullFact))) {
                    // Do not re-analyze the caller if we only get the null fact.
                    // Update the pendingIfdsDependee entry to the new interim result.
                    state.pendingIfdsDependees +=
                        e -> interimEUBP
                        .asInstanceOf[EOptionP[(DeclaredMethod, IFDSFact), IFDSProperty[JavaStatement, IFDSFact]]]
                } else
                    reAnalyzeCalls(state.pendingIfdsCallSites(e), e._1.definedMethod, Some(e._2))

            case FinalEP(_: DefinedMethod, _: Callees) ⇒
                reAnalyzebasicBlocks(state.pendingCgCallSites)

            case InterimEUBP(_: DefinedMethod, _: Callees) ⇒
                reAnalyzebasicBlocks(state.pendingCgCallSites)

            case FinalEP(method: Method, _: TACAI) ⇒
                state.pendingTacDependees -= method
                reAnalyzebasicBlocks(state.pendingTacCallSites(declaredMethods(method)))
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
        call:                 JavaStatement,
        callees:              SomeSet[Method],
        in:                   Set[IFDSFact],
        calleeWithUpdateFact: Option[IFDSFact]
    )(
        implicit
        state: State
    ): Map[JavaStatement, Set[IFDSFact]] = {
        val successors = nextStatements(call)
        val inputFacts = beforeHandleCall(call, in)
        // Facts valid at the start of each successor
        var summaryEdges: Map[JavaStatement, Set[IFDSFact]] = Map.empty

        /*
     * If calleeWithUpdateFact is present, this means that the basic block already has been
     * analyzed with the `inputFacts`.
     */
        if (calleeWithUpdateFact.isEmpty)
            for (successor ← successors) {
                numberOfCalls.callToReturnFlow += 1
                sumOfInputFactsForCallbacks += in.size
                summaryEdges += successor ->
                    propagateNullFact(
                        inputFacts,
                        ifdsProblem.callToReturnFlow(call, successor, inputFacts, state.source)
                    )
            }

        for (calledMethod ← callees) {
            val callee = declaredMethods(calledMethod)
            ifdsProblem.outsideAnalysisContext(callee) match {
                case Some(handler) ⇒
                    // Let the concrete analysis decide what to do.
                    for {
                        successor ← successors
                    } summaryEdges +=
                        successor -> (summaryEdges(successor) ++
                            handler(call, successor, in))
                case None ⇒
                    val callToStart =
                        if (calleeWithUpdateFact.isDefined) Set(calleeWithUpdateFact.get)
                        else {
                            propagateNullFact(inputFacts, callToStartFacts(call, callee, inputFacts))
                        }
                    var allNewExitFacts: Map[JavaStatement, Set[IFDSFact]] = Map.empty
                    // Collect exit facts for each input fact separately
                    for (fact ← callToStart) {
                        /*
             * If this is a recursive call with the same input facts, we assume that the
             * call only produces the facts that are already known. The call site is added to
             * `pendingIfdsCallSites`, so that it will be re-evaluated if new output facts
             * become known for the input fact.
             */
                        if ((calledMethod eq state.method) && fact == state.source._2) {
                            val newDependee =
                                if (state.pendingIfdsCallSites.contains(state.source))
                                    state.pendingIfdsCallSites(state.source) +
                                        ((basicBlock, call.index))
                                else Set((basicBlock, call.index))
                            state.pendingIfdsCallSites =
                                state.pendingIfdsCallSites.updated(state.source, newDependee)
                            allNewExitFacts = mergeMaps(allNewExitFacts, collectResult)
                        } else {
                            val e = (callee, fact)
                            val callFlows = propertyStore(e, propertyKey.key)
                                .asInstanceOf[EOptionP[(DeclaredMethod, IFDSFact), IFDSProperty[JavaStatement, IFDSFact]]]
                            val oldValue = state.pendingIfdsDependees.get(e)
                            val oldExitFacts: Map[JavaStatement, Set[IFDSFact]] = oldValue match {
                                case Some(ep: InterimEUBP[_, IFDSProperty[JavaStatement, IFDSFact]]) ⇒ ep.ub.flows
                                case _ ⇒ Map.empty
                            }
                            val exitFacts: Map[JavaStatement, Set[IFDSFact]] = callFlows match {
                                case ep: FinalEP[_, IFDSProperty[JavaStatement, IFDSFact]] ⇒
                                    if (state.pendingIfdsCallSites.contains(e)
                                        && state.pendingIfdsCallSites(e).nonEmpty) {
                                        val newDependee =
                                            state.pendingIfdsCallSites(e) - ((basicBlock, call.index))
                                        state.pendingIfdsCallSites = state.pendingIfdsCallSites.updated(e, newDependee)
                                    }
                                    state.pendingIfdsDependees -= e
                                    ep.p.flows
                                case ep: InterimEUBP[_, IFDSProperty[JavaStatement, IFDSFact]] ⇒
                                    /*
                   * Add the call site to `pendingIfdsCallSites` and
                   * `pendingIfdsDependees` and continue with the facts in the interim
                   * result for now. When the analysis for the callee finishes, the
                   * analysis for this call site will be triggered again.
                   */
                                    addIfdsDependee(e, callFlows, basicBlock, call.index)
                                    ep.ub.flows
                                case _ ⇒
                                    addIfdsDependee(e, callFlows, basicBlock, call.index)
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
                                    e._1.definedMethod,
                                    Some(e._2)
                                )
                            }
                        }
                    }
                    summaryEdges =
                        addExitToReturnFacts(summaryEdges, successors, call, callee, allNewExitFacts)
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
    protected def beforeHandleCall(call: JavaStatement, in: Set[IFDSFact]): Set[IFDSFact] = in

    /**
     * Gets the set of all methods directly callable at some call statement.
     *
     * @param basicBlock The basic block containing the call.
     * @param pc The call's program counter.
     * @param caller The caller, performing the call.
     * @return All methods directly callable at the statement index.
     */
    protected def getCallees(
        statement: JavaStatement,
        caller:    DeclaredMethod
    ): Iterator[DeclaredMethod] = {
        val pc = statement.code(statement.index).pc
        val ep = propertyStore(caller, Callees.key)
        ep match {
            case FinalEP(_, p) ⇒ p.directCallees(typeProvider.newContext(caller), pc).map(_.method)
            case _ ⇒
                throw new IllegalStateException(
                    "call graph mut be computed before the analysis starts"
                )
        }
    }

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
        worklist: mutable.Queue[(BasicBlock, Set[IFDSFact], Option[Int], Option[Method], Option[IFDSFact])]
    )(implicit state: State): Unit = {
        while (worklist.nonEmpty) {
            val (basicBlock, in, calleeWithUpdateIndex, calleeWithUpdate, calleeWithUpdateFact) =
                worklist.dequeue()
            val oldOut = state.outgoingFacts.getOrElse(basicBlock, Map.empty)
            val nextOut = analyzeBasicBlock(
                basicBlock,
                in,
                calleeWithUpdateIndex,
                calleeWithUpdate,
                calleeWithUpdateFact
            )
            val allOut = mergeMaps(oldOut, nextOut).mapValues(facts ⇒ subsume(facts, project))
            state.outgoingFacts = state.outgoingFacts.updated(basicBlock, allOut)

            for (successor ← nextNodes(basicBlock)) {
                if (isLastNode(successor)) {
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
                                source._1.definedMethod,
                                Some(source._2)
                            )
                        }
                    }
                } else {
                    val successorBlock = successor.asBasicBlock
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
     * @param calleeWithUpdateIndex If the basic block is analyzed because there is new information
     *                              for a callee, this is the call site's index.
     * @param calleeWithUpdate If the basic block is analyzed because there is new information for
     *                         a callee, this is the callee.
     * @param calleeWithUpdateFact If the basic block is analyzed because there is new information
     *                             for a callee with a specific input fact, this is the input fact.
     * @return A map, mapping each successor node to its input facts. Instead of catch nodes, this
     *         map contains their handler nodes.
     */
    private def analyzeBasicBlock(
        basicBlock:            BasicBlock,
        in:                    Set[IFDSFact],
        calleeWithUpdateIndex: Option[Int],
        calleeWithUpdate:      Option[Method],
        calleeWithUpdateFact:  Option[IFDSFact]
    )(
        implicit
        state: State
    ): Map[CFGNode, Set[IFDSFact]] = {

        /*
     * Collects information about a statement.
     *
     * @param index The statement's index.
     * @return A tuple of the following elements:
     *         statement: The statement at `index`.
     *         calees: The methods possibly called at this statement, if it contains a call.
     *                 If `index` equals `calleeWithUpdateIndex`, only `calleeWithUpdate` will
     *                 be returned.
     *         calleeFact: If `index` equals `calleeWithUpdateIndex`, only
     *         `calleeWithUpdateFact` will be returned, None otherwise.
     */
        def collectInformation(
            index: Int
        ): (JavaStatement, Option[SomeSet[Method]], Option[IFDSFact]) = {
            val stmt = state.code(index)
            val statement = JavaStatement(state.method, basicBlock, stmt, index, state.code, state.cfg, state.source._1)
            val calleesO =
                if (calleeWithUpdateIndex.contains(index)) calleeWithUpdate.map(Set(_))
                else getCalleesIfCallStatement(statement)
            val calleeFact =
                if (calleeWithUpdateIndex.contains(index)) calleeWithUpdateFact
                else None
            (statement, calleesO, calleeFact)
        }

        val last = lastIndex(basicBlock)
        var flows: Set[IFDSFact] = in
        var index = firstIndex(basicBlock)

        // Iterate over all statements but the last one, only keeping the resulting DataFlowFacts.
        while (index != last) {
            val (statement, calleesO, calleeFact) = collectInformation(index)
            val next = nextIndex(index)
            flows = if (calleesO.isEmpty) {
                val successor =
                    JavaStatement(state.method, basicBlock, state.code(next), next, state.code, state.cfg, state.source._1)
                numberOfCalls.normalFlow += 1
                sumOfInputFactsForCallbacks += in.size
                ifdsProblem.normalFlow(statement, Some(successor), flows)
            } else
                // Inside a basic block, we only have one successor --> Take the head
                handleCall(basicBlock, statement, calleesO.get, flows, calleeFact).values.head
            index = next
        }

        // Analyze the last statement for each possible successor statement.
        val (statement, calleesO, callFact) = collectInformation(last)
        var result: Map[CFGNode, Set[IFDSFact]] =
            if (calleesO.isEmpty) {
                var result: Map[CFGNode, Set[IFDSFact]] = Map.empty
                for (node ← nextNodes(basicBlock)) {
                    numberOfCalls.normalFlow += 1
                    sumOfInputFactsForCallbacks += in.size
                    result += node -> ifdsProblem.normalFlow(statement, Some(firstStatement(node)), flows)
                }
                result
            } else
                handleCall(basicBlock, statement, calleesO.get, flows, callFact)
                    .map(entry ⇒ entry._1.node -> entry._2)

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
        val queue: mutable.Queue[(BasicBlock, Set[IFDSFact], Option[Int], Option[Method], Option[IFDSFact])] = mutable.Queue.empty
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
        callSites: Set[(BasicBlock, Int)],
        callee:    Method,
        fact:      Option[IFDSFact]
    )(implicit state: State): Unit = {
        val queue: mutable.Queue[(BasicBlock, Set[IFDSFact], Option[Int], Option[Method], Option[IFDSFact])] = mutable.Queue.empty
        for ((block, index) ← callSites)
            queue.enqueue((block, state.incomingFacts(block), Some(index), Some(callee), fact))
        process(queue)
    }

    /**
     * Gets the set of all methods possibly called at some statement.
     *
     * @param statement The statement.
     * @return All methods possibly called at the statement or None, if the statement does not
     *         contain a call.
     */
    private def getCalleesIfCallStatement(statement: JavaStatement)(
        implicit
        state: State
    ): Option[SomeSet[Method]] = {
        statement.stmt.astID match {
            case StaticMethodCall.ASTID | NonVirtualMethodCall.ASTID | VirtualMethodCall.ASTID ⇒
                Some(definedMethods(getCallees(statement, state.source._1)))
            case Assignment.ASTID | ExprStmt.ASTID ⇒
                getExpression(statement.stmt).astID match {
                    case StaticFunctionCall.ASTID | NonVirtualFunctionCall.ASTID | VirtualFunctionCall.ASTID ⇒
                        Some(definedMethods(getCallees(statement, state.source._1)))
                    case _ ⇒ None
                }
            case _ ⇒ None
        }
    }

    /**
     * Maps some declared methods to their defined methods.
     *
     * @param declaredMethods Some declared methods.
     * @return All defined methods of `declaredMethods`.
     */
    private def definedMethods(declaredMethods: Iterator[DeclaredMethod]): SomeSet[Method] = {
        val result = scala.collection.mutable.Set.empty[Method]
        declaredMethods
            .filter(
                declaredMethod ⇒
                    declaredMethod.hasSingleDefinedMethod ||
                        declaredMethod.hasMultipleDefinedMethods
            )
            .foreach(
                declaredMethod ⇒
                    declaredMethod
                        .foreachDefinedMethod(defineMethod ⇒ result.add(defineMethod))
            )
        result
    }

    /**
     * Retrieves the expression of an assignment or expression statement.
     *
     * @param statement The statement. Must be an Assignment or ExprStmt.
     * @return The statement's expression.
     */
    private def getExpression(statement: Stmt[V]): Expr[V] = statement.astID match {
        case Assignment.ASTID ⇒ statement.asAssignment.expr
        case ExprStmt.ASTID   ⇒ statement.asExprStmt.expr
        case _                ⇒ throw new UnknownError("Unexpected statement")
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
        entity:         (DeclaredMethod, IFDSFact),
        calleeProperty: EOptionP[(DeclaredMethod, IFDSFact), IFDSProperty[JavaStatement, IFDSFact]],
        callBB:         BasicBlock,
        callIndex:      Int
    )(implicit state: State): Unit = {
        val callSites = state.pendingIfdsCallSites
        state.pendingIfdsCallSites = callSites.updated(
            entity,
            callSites.getOrElse(entity, Set.empty) + ((callBB, callIndex))
        )
        state.pendingIfdsDependees += entity -> calleeProperty
    }
}

object AbstractIFDSAnalysis {

    /**
     * The type of the TAC domain.
     */
    type V = DUVar[ValueInformation]

    /**
     * When true, the cross product of exit and successor in returnFLow will be optimized.
     */
    var OPTIMIZE_CROSS_PRODUCT_IN_RETURN_FLOW: Boolean = true

    /**
     * Converts the index of a method's formal parameter to its tac index in the method's scope and
     * vice versa.
     *
     * @param index The index of a formal parameter in the parameter list or of a variable.
     * @param isStaticMethod States, whether the method is static.
     * @return A tac index if a parameter index was passed or a parameter index if a tac index was
     *         passed.
     */
    def switchParamAndVariableIndex(index: Int, isStaticMethod: Boolean): Int =
        (if (isStaticMethod) -2 else -1) - index

}

/**
 * A statement that is passed to the concrete analysis.
 *
 * @param method The method containing the statement.
 * @param node The basic block containing the statement.
 * @param stmt The TAC statement.
 * @param index The index of the Statement in the code.
 * @param code The method's TAC code.
 * @param cfg The method's CFG.
 */
case class JavaStatement(
        method:         Method,
        node:           CFGNode,
        stmt:           Stmt[V],
        index:          Int,
        code:           Array[Stmt[V]],
        cfg:            CFG[Stmt[V], TACStmts[V]],
        declaredMethod: DeclaredMethod
) extends Statement[DeclaredMethod, CFGNode] {

    override def hashCode(): Int = method.hashCode() * 31 + index

    override def equals(o: Any): Boolean = o match {
        case s: JavaStatement ⇒ s.index == index && s.method == method
        case _                ⇒ false
    }

    override def toString: String = s"${method.toJava}"
    override def callable(): DeclaredMethod = declaredMethod

    def asNewJavaStatement: NewJavaStatement = NewJavaStatement(method, index, code, cfg)
}

abstract class IFDSAnalysisScheduler[IFDSFact <: AbstractIFDSFact]
    extends FPCFLazyAnalysisScheduler {

    final override type InitializationData = AbstractIFDSAnalysis[IFDSFact]

    def property: IFDSPropertyMetaInformation[JavaStatement, IFDSFact]

    final override def derivesLazily: Some[PropertyBounds] = Some(PropertyBounds.ub(property))

    override def requiredProjectInformation: ProjectInformationKeys = Seq(DeclaredMethodsKey)

    override val uses: Set[PropertyBounds] = Set(PropertyBounds.finalP(TACAI))

    override def beforeSchedule(p: SomeProject, ps: PropertyStore): Unit = {}

    /**
     * Registers the analysis as a lazy computation, that is, the method
     * will call `ProperytStore.scheduleLazyComputation`.
     */
    override def register(
        p:        SomeProject,
        ps:       PropertyStore,
        analysis: AbstractIFDSAnalysis[IFDSFact]
    ): FPCFAnalysis = {
        ps.registerLazyPropertyComputation(property.key, analysis.performAnalysis)
        analysis
    }

    override def afterPhaseScheduling(ps: PropertyStore, analysis: FPCFAnalysis): Unit = {
        val ifdsAnalysis = analysis.asInstanceOf[AbstractIFDSAnalysis[IFDSFact]]
        for (e ← ifdsAnalysis.ifdsProblem.entryPoints) { ps.force(e, ifdsAnalysis.propertyKey.key) }
    }

    override def afterPhaseCompletion(
        p:        SomeProject,
        ps:       PropertyStore,
        analysis: FPCFAnalysis
    ): Unit = {}

}

abstract class AbsractIFDSAnalysisRunner {

    protected def analysisClass: IFDSAnalysisScheduler[_]

    protected def printAnalysisResults(analysis: AbstractIFDSAnalysis[_], ps: PropertyStore): Unit

    protected def run(
        debug:                    Boolean,
        useL2:                    Boolean,
        delay:                    Boolean,
        evalSchedulingStrategies: Boolean,
        evaluationFile:           Option[File]
    ): Unit = {

        if (debug) {
            PropertyStore.updateDebug(true)
        }

        def evalProject(p: SomeProject): (Milliseconds, NumberOfCalls, Option[Object], Long) = {
            if (useL2) {
                p.updateProjectInformationKeyInitializationData(AIDomainFactoryKey) {
                    case None ⇒ Set(classOf[l2.DefaultPerformInvocationsDomainWithCFGAndDefUse[_]])
                    case Some(requirements) ⇒
                        requirements + classOf[l2.DefaultPerformInvocationsDomainWithCFGAndDefUse[_]]
                }
            } else {
                p.updateProjectInformationKeyInitializationData(AIDomainFactoryKey) {
                    case None               ⇒ Set(classOf[PrimitiveTACAIDomain])
                    case Some(requirements) ⇒ requirements + classOf[PrimitiveTACAIDomain]
                }
            }

            val ps = p.get(PropertyStoreKey)
            var analysisTime: Milliseconds = Milliseconds.None
            p.get(RTACallGraphKey)
            println("Start: "+new java.util.Date)
            org.opalj.util.gc()
            if (AbstractIFDSAnalysisRunner.MEASURE_MEMORY)
                JOptionPane.showMessageDialog(null, "Call Graph finished")
            val analysis =
                time {
                    p.get(FPCFAnalysesManagerKey).runAll(analysisClass)._2
                }(t ⇒ analysisTime = t.toMilliseconds).collect {
                    case (_, a: AbstractIFDSAnalysis[_]) ⇒ a
                }.head
            if (AbstractIFDSAnalysisRunner.MEASURE_MEMORY)
                JOptionPane.showMessageDialog(null, "Analysis finished")

            printAnalysisResults(analysis, ps)
            println(s"The analysis took $analysisTime.")
            println(
                ps.statistics.iterator
                    .map(_.toString())
                    .toList
                    .sorted
                    .mkString("PropertyStore Statistics:\n\t", "\n\t", "\n")
            )
            (
                analysisTime,
                analysis.numberOfCalls,
                additionalEvaluationResult(analysis),
                analysis.sumOfInputFactsForCallbacks
            )
        }

        val p = Project(bytecode.RTJar)

        if (delay) {
            println("Sleeping for three seconds.")
            Thread.sleep(3000)
        }

        if (evalSchedulingStrategies) {
            val results = for {
                i ← 1 to AbstractIFDSAnalysisRunner.NUM_EXECUTIONS_EVAL_SCHEDULING_STRATEGIES
                strategy ← PKESequentialPropertyStore.Strategies
            } yield {
                println(s"Round: $i - $strategy")
                val strategyValue = ConfigValueFactory.fromAnyRef(strategy)
                val newConfig =
                    p.config.withValue(PKESequentialPropertyStore.TasksManagerKey, strategyValue)
                val evaluationResult = evalProject(Project.recreate(p, newConfig))
                org.opalj.util.gc()
                (i, strategy, evaluationResult._1, evaluationResult._2)
            }
            println(results.mkString("AllResults:\n\t", "\n\t", "\n"))
            if (evaluationFile.nonEmpty) {
                val pw = new PrintWriter(evaluationFile.get)
                PKESequentialPropertyStore.Strategies.foreach { strategy ⇒
                    val strategyResults = results.filter(_._2 == strategy)
                    val averageTime = strategyResults.map(_._3.timeSpan).sum / strategyResults.size
                    val (normalFlow, callToStart, exitToReturn, callToReturn) =
                        computeAverageNumberCalls(strategyResults.map(_._4))
                    pw.println(s"Strategy $strategy:")
                    pw.println(s"Average time: ${averageTime}ms")
                    pw.println(s"Average calls of normalFlow: $normalFlow")
                    pw.println(s"Average calls of callToStart: $callToStart")
                    pw.println(s"Average calls of exitToReturn: $exitToReturn")
                    pw.println(s"Average calls of callToReturn: $callToReturn")
                    pw.println()
                }
                pw.close()
            }
        } else {
            var times = Seq.empty[Milliseconds]
            var numberOfCalls = Seq.empty[NumberOfCalls]
            var sumsOfInputFactsForCallbacks = Seq.empty[Long]
            var additionalEvaluationResults = Seq.empty[Object]
            for {
                _ ← 1 to AbstractIFDSAnalysisRunner.NUM_EXECUTIONS
            } {
                val evaluationResult = evalProject(Project.recreate(p))
                val additionalEvaluationResult = evaluationResult._3
                times :+= evaluationResult._1
                numberOfCalls :+= evaluationResult._2
                sumsOfInputFactsForCallbacks :+= evaluationResult._4
                if (additionalEvaluationResult.isDefined)
                    additionalEvaluationResults :+= additionalEvaluationResult.get
            }
            if (evaluationFile.nonEmpty) {
                val (normalFlow, callFlow, returnFlow, callToReturnFlow) = computeAverageNumberCalls(
                    numberOfCalls
                )
                val time = times.map(_.timeSpan).sum / times.size
                val sumOfInputFactsForCallbacks = sumsOfInputFactsForCallbacks.sum / sumsOfInputFactsForCallbacks.size
                val pw = new PrintWriter(evaluationFile.get)
                pw.println(s"Average time: ${time}ms")
                pw.println(s"Average calls of normalFlow: $normalFlow")
                pw.println(s"Average calls of callFlow: $callFlow")
                pw.println(s"Average calls of returnFlow: $returnFlow")
                pw.println(s"Average calls of callToReturnFlow: $callToReturnFlow")
                pw.println(s"Sum of input facts for callbacks: $sumOfInputFactsForCallbacks")
                if (additionalEvaluationResults.nonEmpty)
                    writeAdditionalEvaluationResultsToFile(pw, additionalEvaluationResults)
                pw.close()
            }
        }
    }

    protected def additionalEvaluationResult(analysis: AbstractIFDSAnalysis[_]): Option[Object] = None

    protected def writeAdditionalEvaluationResultsToFile(
        writer:                      PrintWriter,
        additionalEvaluationResults: Seq[Object]
    ): Unit = {}

    protected def canBeCalledFromOutside(
        method:        DeclaredMethod,
        propertyStore: PropertyStore
    ): Boolean =
        propertyStore(method, Callers.key) match {
            // This is the case, if the method may be called from outside the library.
            case FinalEP(_, p: Callers) ⇒ p.hasCallersWithUnknownContext
            case _ ⇒
                throw new IllegalStateException(
                    "call graph mut be computed before the analysis starts"
                )
        }

    private def computeAverageNumberCalls(numberOfCalls: Seq[NumberOfCalls]): (Int, Int, Int, Int) = {
        val length = numberOfCalls.length
        val normalFlow = numberOfCalls.map(_.normalFlow).sum / length
        val callFlow = numberOfCalls.map(_.callFlow).sum / length
        val returnFlow = numberOfCalls.map(_.returnFlow).sum / length
        val callToReturnFlow = numberOfCalls.map(_.callToReturnFlow).sum / length
        (normalFlow, callFlow, returnFlow, callToReturnFlow)
    }
}

object AbstractIFDSAnalysisRunner {
    var NUM_EXECUTIONS = 10
    var NUM_EXECUTIONS_EVAL_SCHEDULING_STRATEGIES = 2
    var MEASURE_MEMORY = false
}
