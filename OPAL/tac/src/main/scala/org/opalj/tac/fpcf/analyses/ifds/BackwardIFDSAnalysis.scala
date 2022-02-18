/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.ifds

import scala.annotation.tailrec

import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.FinalE
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.InterimEUBP
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.SomeEPS
import org.opalj.value.ValueInformation
import org.opalj.br.cfg.BasicBlock
import org.opalj.br.cfg.CFGNode
import org.opalj.br.DeclaredMethod
import org.opalj.br.cfg.CatchNode
import org.opalj.br.cfg.CFG
import org.opalj.br.fpcf.properties.cg.Callers
import org.opalj.tac.fpcf.analyses.ifds.AbstractIFDSAnalysis.V
import org.opalj.tac.fpcf.properties.IFDSProperty
import org.opalj.tac.Stmt
import org.opalj.tac.TACStmts
import org.opalj.tac.fpcf.properties.TACAI
import org.opalj.tac.fpcf.properties.TheTACAI
import org.opalj.tac.DUVar
import org.opalj.tac.Return
import org.opalj.tac.ReturnValue
import org.opalj.tac.TACMethodParameter
import org.opalj.tac.TACode

/**
 * A data flow fact, that was created by an unbalanced return.
 *
 * @tparam FactType The type of flow facts, which are tracked by the concrete analysis.
 */
trait UnbalancedReturnFact[FactType] extends AbstractIFDSFact {

    /**
     * The index of the call, which creates the inner fact.
     */
    val index: Int

    /**
     * The fact, which will hold after `index`.
     */
    val innerFact: FactType
}

/**
 * An IFDS analysis, which analyzes the code against the control flow direction.
 *
 * @tparam UnbalancedIFDSFact The type of unbalanced return facts facts, which are tracked by the
 *                            concrete analysis.
 * @author Mario Trageser
 */
abstract class BackwardIFDSAnalysis[IFDSFact <: AbstractIFDSFact, UnbalancedIFDSFact <: IFDSFact with UnbalancedReturnFact[IFDSFact]]
    extends AbstractIFDSAnalysis[IFDSFact] {

    /**
     * Called, when the entry point of the analyzed method is reached.
     * Creates unbalanced return facts for the callers.
     *
     * @param facts The facts, which hold at the entry point of the analyzed method.
     * @param call A call, which calls the analyzed method.
     * @param caller The method, invoking the `call`.
     * @param source The entity, which is currently analyzed.
     * @return Unbalanced return facts, that hold after `call` under the assumption, that `facts`
     *         held at the entry point of the analyzed method.
     */
    protected def unbalancedReturnFlow(facts: Set[IFDSFact], call: Statement, caller: DeclaredMethod,
                                       source: (DeclaredMethod, IFDSFact)): Set[UnbalancedIFDSFact]

    /**
     * If this method is analyzed for an unbalanced return fact, the single star block is the block,
     * which contains the call.
     * Otherwise, the start blocks are the return nodes of the method.
     */
    override protected def startBlocks(
        sourceFact: IFDSFact,
        cfg:        CFG[Stmt[V], TACStmts[V]]
    ): Set[BasicBlock] =
        sourceFact match {
            case fact: UnbalancedReturnFact[IFDSFact] ⇒ Set(cfg.bb(fact.index))
            case _ ⇒ Set(cfg.normalReturnNode, cfg.abnormalReturnNode).flatMap(_.predecessors)
                .foldLeft(Set.empty[BasicBlock])((c, n) ⇒ c + n.asBasicBlock)
        }

    /**
     * Collects the output facts of the entry point of the analyzed method.
     */
    override protected def collectResult(implicit state: State): Map[Statement, Set[IFDSFact]] = {
        val startBlock = state.cfg.startBlock
        val startPC = startBlock.startPC
        val statement = Statement(state.method, startBlock, state.code(startPC),
            startPC, state.code, state.cfg)
        val exitFacts = state.outgoingFacts.get(startBlock).flatMap(_.get(SyntheticStartNode))
        if (exitFacts.isDefined) Map(statement → exitFacts.get)
        else Map.empty
    }

    /**
     * If the update is for an IFDS entity with an unbalanced return fact, the IFDS dependency is
     * updated if it is an interim result or removed if it is a final result.
     * If the update is not for an unbalanced return fact, the update will be handled by
     * AbstractIFDSAnalysis.
     */
    override protected def propertyUpdate(eps: SomeEPS)(implicit state: State): ProperPropertyComputationResult = {
        (eps: @unchecked) match {
            /*
             * If the analysis for the unbalanced return finished, remove the entity from the
             * dependencies.
             */
            case FinalE(e: (DeclaredMethod, IFDSFact) @unchecked) ⇒
                if (e._2.isInstanceOf[UnbalancedReturnFact[IFDSFact]]) {
                    state.pendingIfdsDependees -= e
                    createResult
                } else super.propertyUpdate(eps)
            /*
             * If there is an interim result for an unbalanced return fact, ignore it and just create the result.
             */
            case interimEUBP @ InterimEUBP(e: (DeclaredMethod, IFDSFact) @unchecked,
                _: IFDSProperty[IFDSFact]) ⇒
                if (e._2.isInstanceOf[UnbalancedReturnFact[IFDSFact]]) {
                    state.pendingIfdsDependees +=
                        e → interimEUBP.asInstanceOf[EOptionP[(DeclaredMethod, IFDSFact), IFDSProperty[IFDSFact]]]
                    createResult()
                } else super.propertyUpdate(eps)

            // If this is not an update for an unbalanced return fact, handle it as usual.
            case _ ⇒ super.propertyUpdate(eps)
        }
    }

    /**
     * If `basicBlock` is the method's entry point, the synthetic start node will be returned.
     * Otherwise, its predecessors will be returned.
     */
    override protected def nextNodes(basicBlock: BasicBlock): Set[CFGNode] = {
        if (basicBlock.startPC == 0) Set(SyntheticStartNode)
        else {
            val predecessors = scala.collection.mutable.Set.empty[CFGNode]
            basicBlock.predecessors.foreach {
                case basicBlock: BasicBlock ⇒ predecessors += basicBlock
                case catchNode: CatchNode ⇒
                    predecessors ++= catchNode.predecessors.iterator.map(_.asBasicBlock)
            }
            predecessors.toSet
        }
    }

    /**
     * The synthetic start node is the last node.
     */
    override protected def isLastNode(node: CFGNode): Boolean = node == SyntheticStartNode

    /**
     * When new output facts for the method's entry point are found, a concrete analysis may add
     * additional facts.
     * Then, all direct callers of the method are determined. For each caller, new unbalanced return
     * facts are created and the caller entity will be added to the pending IFDS dependencies, so
     * that the caller entity will be analyzed.
     */
    override protected def foundNewInformationForLastNode(
        nextIn: Set[IFDSFact],
        oldIn:  Option[Set[IFDSFact]],
        state:  State
    ): Unit = {
        var newIn = if (oldIn.isDefined) notSubsumedBy(nextIn, oldIn.get, project) else nextIn
        val created = createFactsAtStartNode(newIn, state.source)
        if (created.nonEmpty) {
            // Add the created facts to newIn and update the outgoing facts in the state.
            newIn = newIn ++ created
            val startBlock = state.cfg.startBlock
            val oldOut = state.outgoingFacts(startBlock)
            val newOut = oldOut.updated(SyntheticStartNode, oldOut(SyntheticStartNode) ++ created)
            state.outgoingFacts = state.outgoingFacts.updated(startBlock, newOut)
        }
        // Only create unbalanced returns, if we are at an entry point or in a (indirect) caller of it.
        if (shouldPerformUnbalancedReturn(state.source)) {
            // Get all callers of this method
            propertyStore(state.source._1, Callers.key) match {
                case FinalEP(_, p: Callers) ⇒ p.callers.foreach { callersProperty ⇒
                    val (caller, callPc, directCall) = callersProperty
                    // We do not handle indirect calls.
                    if (directCall) {
                        val definedCaller = caller.definedMethod
                        // Get the caller's tac to create the unbalanced return facts
                        val callerTac = propertyStore(definedCaller, TACAI.key)
                        callerTac match {
                            case FinalP(TheTACAI(tac)) ⇒
                                addDependencyForUnbalancedReturn(caller, tac.pcToIndex(callPc),
                                    newIn, tac)(state)
                            case _ ⇒
                                val pendingTacCallSites = state.pendingTacCallSites
                                state.pendingTacDependees += definedCaller → callerTac
                                state.pendingTacCallSites = pendingTacCallSites.updated(
                                    caller,
                                    pendingTacCallSites.getOrElse(caller, Set.empty) +
                                        state.cfg.startBlock
                                )
                        }
                    }

                }
                case _ ⇒
                    throw new IllegalStateException(
                        "call graph mut be computed before the analysis starts"
                    )
            }
        }
    }

    /**
     * Called, when the analysis found new output facts at a method's start block.
     * A concrete analysis may overwrite this method to create additional facts, which will be added
     * to the analysis' result.
     *
     * @param in The new output facts at the start node.
     * @param source The entity, which is analyzed.
     * @return Nothing by default.
     */
    protected def createFactsAtStartNode(
        in:     Set[IFDSFact],
        source: (DeclaredMethod, IFDSFact)
    ): Set[IFDSFact] =
        Set.empty

    /**
     * If the node is a basic block, it will be returned.
     * Otherwise, all predecessors of the node will be returned.
     */
    override protected def skipCatchNode(node: CFGNode): Set[BasicBlock] =
        if (node.isBasicBlock) Set(node.asBasicBlock)
        else node.predecessors.map(_.asBasicBlock)

    /**
     * The first index of a basic block is its end index.
     */
    override protected def firstIndex(basicBlock: BasicBlock): Int = basicBlock.endPC

    /**
     * The last index of a basic block ist its start index.
     */
    override protected def lastIndex(basicBlock: BasicBlock): Int = basicBlock.startPC

    /**
     * The next index against the control flow direction.
     */
    override protected def nextIndex(index: Int): Int = index - 1

    /**
     * If the `node` is a basic block, its end statement will be returned.
     * If it is a catch node, the first statement of its throwing block will be returned.
     * If it is a synthetic start node, an artificial statement without code will be returned.
     */
    override protected def firstStatement(node: CFGNode)(implicit state: State): Statement = {
        if (node.isBasicBlock) {
            val index = node.asBasicBlock.endPC
            Statement(state.method, node, state.code(index), index, state.code, state.cfg)
        } else if (node.isCatchNode) firstStatement(node.successors.head)
        else if (node == SyntheticStartNode) Statement(state.method, node, null, 0, state.code, state.cfg)
        else throw new IllegalArgumentException(s"Unknown node type: $node")
    }

    /**
     * The successor statements against the control flow direction.
     */
    override protected def nextStatements(statement: Statement)(implicit state: State): Set[Statement] = {
        val index = statement.index
        val basicBlock = statement.node.asBasicBlock
        if (index == 0) {
            Set(firstStatement(SyntheticStartNode))
        } else if (index == basicBlock.startPC)
            basicBlock.predecessors.map(firstStatement(_))
        else {
            val nextIndex = index - 1
            Set(Statement(statement.method, basicBlock, statement.code(nextIndex), nextIndex,
                statement.code, statement.cfg))
        }
    }

    /**
     * Determines the callees' exit statements and all successor statements in the control flow
     * direction, which my be executed after them. Calls returnFlow on those pairs.
     */
    override protected def callToStartFacts(call: Statement, callee: DeclaredMethod,
                                            in: Set[IFDSFact])(implicit state: State): Set[IFDSFact] = {
        val definedCallee = callee.definedMethod
        val ep = propertyStore(definedCallee, TACAI.key)
        ep match {
            case FinalP(TheTACAI(tac)) ⇒
                val cfg = tac.cfg
                val successors = predecessorStatementsWithNode(call)
                val flow = scala.collection.mutable.Set.empty[IFDSFact]
                (cfg.normalReturnNode.predecessors ++ cfg.abnormalReturnNode.predecessors)
                    .foreach { bb ⇒
                        val exitPc = bb.asBasicBlock.endPC
                        val calleeStmts = tac.stmts
                        val exitStmt = calleeStmts(exitPc)
                        val exitStatement = Statement(definedCallee, cfg.bb(exitPc), exitStmt,
                            exitPc, calleeStmts, cfg)
                        for {
                            successor ← successors
                            if !AbstractIFDSAnalysis.OPTIMIZE_CROSS_PRODUCT_IN_RETURN_FLOW ||
                                (successor._2.isBasicBlock || successor._2.isNormalReturnExitNode) &&
                                (exitStatement.stmt.astID == Return.ASTID || exitStatement.stmt.astID == ReturnValue.ASTID) ||
                                (successor._2.isCatchNode || successor._2.isAbnormalReturnExitNode) &&
                                (exitStatement.stmt.astID != Return.ASTID && exitStatement.stmt.astID != ReturnValue.ASTID)
                        } {
                            numberOfCalls.returnFlow += 1
                            sumOfInputfactsForCallbacks += in.size
                            flow ++= returnFlow(call, callee, exitStatement, successor._1, in)
                        }
                    }
                flow.toSet
            case _ ⇒
                val pendingTacCallSites = state.pendingTacCallSites
                val index = call.index
                state.pendingTacDependees += definedCallee → ep
                state.pendingTacCallSites = pendingTacCallSites.updated(
                    callee,
                    pendingTacCallSites.getOrElse(callee, Set.empty) + call.cfg.bb(index)
                )
                Set.empty
        }
    }

    /**
     * Calls callFlow for the facts in exitFacts and adds them for each successor to the summary
     * edges. exitFacts should at most contain the callee's entry point.
     */
    override protected def addExitToReturnFacts(
        summaryEdges: Map[Statement, Set[IFDSFact]],
        successors:   Set[Statement], call: Statement,
        callee:    DeclaredMethod,
        exitFacts: Map[Statement, Set[IFDSFact]]
    )(implicit state: State): Map[Statement, Set[IFDSFact]] = {
        var result = summaryEdges
        if (exitFacts.nonEmpty) {
            val in = exitFacts.head._2
            numberOfCalls.callFlow += 1
            sumOfInputfactsForCallbacks += in.size
            val exitToReturnFacts = callFlow(call, callee, in, state.source)
            successors.foreach { successor ⇒
                val updatedValue = result.get(successor) match {
                    case Some(facts) ⇒
                        if (facts.size >= exitToReturnFacts.size) facts ++ exitToReturnFacts
                        else exitToReturnFacts ++ facts
                    case None ⇒ exitToReturnFacts
                }
                result = result.updated(successor, updatedValue)
            }
        }
        result
    }

    /**
     * If there is an unbalanced return fact for this call, it will be replaced by its inner fact.
     */
    override protected def beforeHandleCall(call: Statement, in: Set[IFDSFact]): Set[IFDSFact] =
        in.map {
            case unbalancedFact: UnbalancedReturnFact[IFDSFact] if unbalancedFact.index == call.index ⇒
                unbalancedFact.innerFact
            case fact ⇒ fact
        }

    /**
     * Checks for the analyzed entity, if an unbalanced return should be performed.
     *
     * @param source The analyzed entity.
     * @return False, if no unbalanced return should be performed.
     */
    protected def shouldPerformUnbalancedReturn(source: (DeclaredMethod, IFDSFact)): Boolean =
        source._2.isInstanceOf[UnbalancedReturnFact[IFDSFact]] || entryPoints.contains(source)

    /**
     * Determines the predecessor statements, i.e. the successor statements in the control flow
     * direction. They are mapped to the corresponding predecessor node. When determining the node,
     * catch nodes are not skipped.
     *
     * @param statement The statement, for which the predecessor statements will be determined.
     *
     * @return A map, mapping from a predecessor statement to the corresponding node.
     */
    private def predecessorStatementsWithNode(statement: Statement)(implicit state: State): Map[Statement, CFGNode] = {
        val index = statement.index
        val basicBlock = statement.node.asBasicBlock
        if (index == basicBlock.endPC)
            basicBlock.successors.iterator.map(successorNode ⇒ lastStatement(successorNode) → successorNode).toMap
        else {
            val nextIndex = index + 1
            Map(Statement(statement.method, basicBlock, statement.code(nextIndex), nextIndex,
                statement.code, statement.cfg) → basicBlock)
        }
    }

    /**
     * Determines the last statement of a `node`.
     * If it is a basic block, its entry point will be returned.
     * If it is a catch node, the last statement of its successor will be returned.
     * If it is an exit node, an artificial statement without code will be returned.
     *
     * @param node The node, for which the last statement will be determined.
     *
     * @return The  last statement of `node`.
     */
    @tailrec private def lastStatement(node: CFGNode)(implicit state: State): Statement = {
        if (node.isBasicBlock) {
            val index = node.asBasicBlock.startPC
            Statement(state.method, node, state.code(index), index, state.code, state.cfg)
        } else if (node.isCatchNode) lastStatement(node.successors.head)
        else if (node.isExitNode) Statement(state.method, node, null, 0, state.code, state.cfg)
        else throw new IllegalArgumentException(s"Unknown node type: $node")
    }

    /**
     * Adds a dependency to an unbalanced return fact to the pending IFDS dependencies.
     *
     * @param caller The caller of the analyzed method.
     * @param callIndex The index in the `caller`'s context, at which the analyzed method is called.
     * @param in The facts, which hold at the entry point of the analyzed method.
     * @param tac The `caller`'s tac.
     */
    private def addDependencyForUnbalancedReturn(caller: DeclaredMethod, callIndex: Int,
                                                 in:  Set[IFDSFact],
                                                 tac: TACode[TACMethodParameter, DUVar[ValueInformation]])(implicit state: State): Unit = {
        val callerStmts = tac.stmts
        val callerCfg = tac.cfg
        val call = Statement(caller.definedMethod, callerCfg.bb(callIndex),
            callerStmts(callIndex), callIndex, callerStmts, callerCfg)
        unbalancedReturnFlow(in, call, caller, state.source).foreach { in ⇒
            val callerEntity = (caller, in)
            /*
             * Add the caller with the unbalanced return fact as a dependency to
             * start its analysis.
             */
            val callerAnalysisResult = propertyStore(callerEntity, propertyKey.key)
            callerAnalysisResult match {
                case FinalEP(_, _) ⇒ // Caller was already analyzed with the fact
                case _ ⇒
                    val pendingIfdsCallSites = state.pendingIfdsCallSites
                    state.pendingIfdsDependees += callerEntity →
                        callerAnalysisResult.asInstanceOf[EOptionP[(DeclaredMethod, IFDSFact), IFDSProperty[IFDSFact]]]
                    state.pendingIfdsCallSites += callerEntity →
                        (pendingIfdsCallSites.getOrElse(callerEntity, Set.empty) +
                            ((state.cfg.startBlock, 0)))
            }
        }
    }
}

/**
 * A synthetic node, that is the predecessor of a method's entry point.
 * This node is necessary, because we need to store the output facts of the method's entry point in
 * a map, which maps from successor statements of a node to its output facts.
 * The synthetic start node represents the successor of the entry point in this map.
 */
object SyntheticStartNode extends CFGNode {

    override def isBasicBlock: Boolean = false

    override def isStartOfSubroutine: Boolean = false

    override def isAbnormalReturnExitNode: Boolean = false

    override def isExitNode: Boolean = false

    override def isNormalReturnExitNode: Boolean = false

    override def isCatchNode: Boolean = false

    override def toHRR: Option[String] = Some("Synthetic Start Node")

    override def nodeId: Int = -1
}