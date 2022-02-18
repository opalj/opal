/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.ifds

import org.opalj.br.cfg.BasicBlock
import org.opalj.br.cfg.CFG
import org.opalj.br.cfg.CFGNode
import org.opalj.br.DeclaredMethod
import org.opalj.tac.fpcf.analyses.ifds.AbstractIFDSAnalysis.V
import org.opalj.tac.Return
import org.opalj.tac.ReturnValue
import org.opalj.tac.Stmt
import org.opalj.tac.TACStmts

/**
 * An IFDS analysis, which analyzes the code in the control flow direction.
 *
 * @author Mario Trageser
 */
abstract class ForwardIFDSAnalysis[IFDSFact <: AbstractIFDSFact] extends AbstractIFDSAnalysis[IFDSFact] {

    /**
     * The analysis starts at the entry block.
     */
    override protected def startBlocks(
        sourceFact: IFDSFact,
        cfg:        CFG[Stmt[V], TACStmts[V]]
    ): Set[BasicBlock] =
        Set(cfg.startBlock)

    /**
     * Collects the output facts at the predecessors of the normal and abnormal return node.
     */
    override protected def collectResult(implicit state: State): Map[Statement, Set[IFDSFact]] = {
        mergeMaps(
            resultOfExitNode(state.cfg.normalReturnNode),
            resultOfExitNode(state.cfg.abnormalReturnNode)
        )
    }

    /**
     * Returns the next successor nodes of `basicBlock`.
     * Catch nodes are skipped to their successor.
     */
    override protected def nextNodes(basicBlock: BasicBlock): Set[CFGNode] =
        basicBlock.successors.map { successor ⇒
            if (successor.isCatchNode) successor.successors.head
            else successor
        }

    /**
     * The exit nodes are the last nodes.
     */
    override protected def isLastNode(node: CFGNode): Boolean = node.isExitNode

    /**
     * If the node is a basic block, it will be returned.
     * Otherwise, its first successor will be returned.
     */
    override protected def skipCatchNode(node: CFGNode): Set[BasicBlock] =
        Set((if (node.isBasicBlock) node else node.successors.head).asBasicBlock)

    /**
     * The first index of a basic block is its start index.
     */
    override protected def firstIndex(basicBlock: BasicBlock): Int = basicBlock.startPC

    /**
     * The last index of a basic block is its end index.
     */
    override protected def lastIndex(basicBlock: BasicBlock): Int = basicBlock.endPC

    /**
     * The next index in the direction of the control flow.
     */
    override protected def nextIndex(index: Int): Int = index + 1

    /**
     * If the `node` is a basic block, its first statement will be returned.
     * If it is a catch node, the first statement of its handler will be returned.
     * If it is an exit node, an artificial statement without code will be returned.
     */
    override protected def firstStatement(node: CFGNode)(implicit state: State): Statement = {
        if (node.isBasicBlock) {
            val index = node.asBasicBlock.startPC
            Statement(state.method, node, state.code(index), index, state.code, state.cfg)
        } else if (node.isCatchNode) firstStatement(node.successors.head)
        else if (node.isExitNode) Statement(state.method, node, null, 0, state.code, state.cfg)
        else throw new IllegalArgumentException(s"Unknown node type: $node")
    }

    /**
     * The successor statements in the direction of the control flow.
     */
    override protected def nextStatements(statement: Statement)(implicit state: State): Set[Statement] = {
        val index = statement.index
        val basicBlock = statement.node.asBasicBlock
        if (index == basicBlock.endPC)
            basicBlock.successors.map(firstStatement(_))
        else {
            val nextIndex = index + 1
            Set(Statement(statement.method, basicBlock, statement.code(nextIndex), nextIndex,
                statement.code, statement.cfg))
        }
    }

    /**
     * Calls callFlow.
     */
    override protected def callToStartFacts(call: Statement, callee: DeclaredMethod,
                                            in: Set[IFDSFact])(implicit state: State): Set[IFDSFact] = {
        numberOfCalls.callFlow += 1
        sumOfInputfactsForCallbacks += in.size
        callFlow(call, callee, in, state.source)
    }

    /**
     * Combines each normal exit node with each normal successor and each abnormal exit statement
     * with each catch node. Calls returnFlow for those pairs and adds them to the summary edges.
     */
    override protected def addExitToReturnFacts(
        summaryEdges: Map[Statement, Set[IFDSFact]],
        successors:   Set[Statement], call: Statement,
        callee:    DeclaredMethod,
        exitFacts: Map[Statement, Set[IFDSFact]]
    )(implicit state: State): Map[Statement, Set[IFDSFact]] = {
        // First process for normal returns, then abnormal returns.
        var result = summaryEdges
        if (AbstractIFDSAnalysis.OPTIMIZE_CROSS_PRODUCT_IN_RETURN_FLOW) {
            val successors = nextStatementsWithNode(call)
            for {
                successor ← successors
                exitStatement ← exitFacts.keys
                if (successor._2.isBasicBlock || successor._2.isNormalReturnExitNode) &&
                    (exitStatement.stmt.astID == Return.ASTID || exitStatement.stmt.astID == ReturnValue.ASTID) ||
                    (successor._2.isCatchNode || successor._2.isAbnormalReturnExitNode) &&
                    (exitStatement.stmt.astID != Return.ASTID && exitStatement.stmt.astID != ReturnValue.ASTID)
            } result = addSummaryEdge(result, call, exitStatement, successor._1, callee, exitFacts)
        } else {
            val successors = nextStatements(call)
            for {
                successor ← successors
                exitStatement ← exitFacts.keys
            } result = addSummaryEdge(result, call, exitStatement, successor, callee, exitFacts)
        }
        result
    }

    /**
     * Like nextStatements, but maps each successor statement to the corresponding successor node.
     * When determining the successor node, catch nodes are not skipped.
     */
    private def nextStatementsWithNode(statement: Statement)(implicit state: State): Map[Statement, CFGNode] = {
        val index = statement.index
        val basicBlock = statement.node.asBasicBlock
        if (index == basicBlock.endPC)
            basicBlock.successors.iterator
                .map(successorNode ⇒ firstStatement(successorNode) → successorNode).toMap
        else {
            val nextIndex = index + 1
            Map(Statement(statement.method, basicBlock, statement.code(nextIndex), nextIndex,
                statement.code, statement.cfg) → basicBlock)
        }
    }

    /**
     * Collects the facts valid at an exit node based on the current results.
     *
     * @param exit The exit node.
     * @return A map, mapping from each predecessor of the `exit` node to the facts valid at the
     *         `exit` node under the assumption that the predecessor was executed before.
     */
    private def resultOfExitNode(exit: CFGNode)(
        implicit
        state: State
    ): Map[Statement, Set[IFDSFact]] = {
        var result = Map.empty[Statement, Set[IFDSFact]]
        exit.predecessors foreach { predecessor ⇒
            if (predecessor.isBasicBlock) {
                val basicBlock = predecessor.asBasicBlock
                val exitFacts = state.outgoingFacts.get(basicBlock).flatMap(_.get(exit))
                if (exitFacts.isDefined) {
                    val lastIndex = basicBlock.endPC
                    val stmt = Statement(state.method, basicBlock, state.code(lastIndex),
                        lastIndex, state.code, state.cfg)
                    result += stmt → exitFacts.get
                }
            }
        }
        result
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
    private def addSummaryEdge(summaryEdges: Map[Statement, Set[IFDSFact]], call: Statement,
                               exitStatement: Statement, successor: Statement,
                               callee:          DeclaredMethod,
                               allNewExitFacts: Map[Statement, Set[IFDSFact]]): Map[Statement, Set[IFDSFact]] = {
        val in = allNewExitFacts.getOrElse(exitStatement, Set.empty)
        numberOfCalls.returnFlow += 1
        sumOfInputfactsForCallbacks += in.size
        val returned = returnFlow(call, callee, exitStatement, successor, in)
        val newFacts =
            if (summaryEdges.contains(successor) && summaryEdges(successor).nonEmpty) {
                val summaryForSuccessor = summaryEdges(successor)
                if (summaryForSuccessor.size >= returned.size) summaryForSuccessor ++ returned
                else returned ++ summaryForSuccessor
            } else returned
        summaryEdges.updated(successor, newFacts)
    }

}
