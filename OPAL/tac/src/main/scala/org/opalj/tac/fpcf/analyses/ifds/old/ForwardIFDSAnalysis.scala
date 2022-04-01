/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.ifds.old

import org.opalj.br.DeclaredMethod
import org.opalj.br.cfg.{BasicBlock, CFG, CFGNode}
import org.opalj.ifds.old.IFDSProblem
import org.opalj.ifds.{AbstractIFDSFact, IFDSPropertyMetaInformation}
import org.opalj.tac.fpcf.analyses.ifds.old
import org.opalj.tac.fpcf.analyses.ifds.JavaIFDSProblem.V
import org.opalj.tac.{Return, ReturnValue, Stmt, TACStmts}

/**
 * An IFDS analysis, which analyzes the code in the control flow direction.
 *
 * @author Mario Trageser
 */
abstract class ForwardIFDSAnalysis[IFDSFact <: AbstractIFDSFact](ifdsProblem: IFDSProblem[IFDSFact, DeclaredMethod, DeclaredMethodJavaStatement, CFGNode], propertyKey: IFDSPropertyMetaInformation[DeclaredMethodJavaStatement, IFDSFact]) extends AbstractIFDSAnalysis[IFDSFact](ifdsProblem, propertyKey) {

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
    override protected def collectResult(implicit state: State): Map[DeclaredMethodJavaStatement, Set[IFDSFact]] = {
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
    override protected def firstStatement(node: CFGNode)(implicit state: State): DeclaredMethodJavaStatement = {
        if (node.isBasicBlock) {
            val index = node.asBasicBlock.startPC
            old.DeclaredMethodJavaStatement(state.method, node, state.code(index), index, state.code, state.cfg, state.source._1)
        } else if (node.isCatchNode) firstStatement(node.successors.head)
        else if (node.isExitNode) old.DeclaredMethodJavaStatement(state.method, node, null, 0, state.code, state.cfg, state.source._1)
        else throw new IllegalArgumentException(s"Unknown node type: $node")
    }

    /**
     * The successor statements in the direction of the control flow.
     */
    override protected def nextStatements(statement: DeclaredMethodJavaStatement)(implicit state: State): Set[DeclaredMethodJavaStatement] = {
        val index = statement.index
        val basicBlock = statement.node.asBasicBlock
        if (index == basicBlock.endPC)
            basicBlock.successors.map(firstStatement(_))
        else {
            val nextIndex = index + 1
            Set(old.DeclaredMethodJavaStatement(statement.method, basicBlock, statement.code(nextIndex), nextIndex,
                statement.code, statement.cfg, statement.declaredMethod))
        }
    }

    /**
     * Calls callFlow.
     */
    override protected def callToStartFacts(call: DeclaredMethodJavaStatement, callee: DeclaredMethod,
                                            in: Set[IFDSFact])(implicit state: State): Set[IFDSFact] = {
        numberOfCalls.callFlow += 1
        sumOfInputFactsForCallbacks += in.size
        ifdsProblem.callFlow(call, callee, in, state.source)
    }

    /**
     * Combines each normal exit node with each normal successor and each abnormal exit statement
     * with each catch node. Calls returnFlow for those pairs and adds them to the summary edges.
     */
    override protected def addExitToReturnFacts(
        summaryEdges: Map[DeclaredMethodJavaStatement, Set[IFDSFact]],
        successors:   Set[DeclaredMethodJavaStatement], call: DeclaredMethodJavaStatement,
        callee:    DeclaredMethod,
        exitFacts: Map[DeclaredMethodJavaStatement, Set[IFDSFact]]
    )(implicit state: State): Map[DeclaredMethodJavaStatement, Set[IFDSFact]] = {
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
    private def nextStatementsWithNode(statement: DeclaredMethodJavaStatement)(implicit state: State): Map[DeclaredMethodJavaStatement, CFGNode] = {
        val index = statement.index
        val basicBlock = statement.node.asBasicBlock
        if (index == basicBlock.endPC)
            basicBlock.successors.iterator
                .map(successorNode ⇒ firstStatement(successorNode) → successorNode).toMap
        else {
            val nextIndex = index + 1
            Map(old.DeclaredMethodJavaStatement(statement.method, basicBlock, statement.code(nextIndex), nextIndex,
                statement.code, statement.cfg, statement.declaredMethod) → basicBlock)
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
    ): Map[DeclaredMethodJavaStatement, Set[IFDSFact]] = {
        var result = Map.empty[DeclaredMethodJavaStatement, Set[IFDSFact]]
        exit.predecessors foreach { predecessor ⇒
            if (predecessor.isBasicBlock) {
                val basicBlock = predecessor.asBasicBlock
                val exitFacts = state.outgoingFacts.get(basicBlock).flatMap(_.get(exit))
                if (exitFacts.isDefined) {
                    val lastIndex = basicBlock.endPC
                    val stmt = old.DeclaredMethodJavaStatement(state.method, basicBlock, state.code(lastIndex),
                        lastIndex, state.code, state.cfg, state.source._1)
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
    private def addSummaryEdge(summaryEdges: Map[DeclaredMethodJavaStatement, Set[IFDSFact]], call: DeclaredMethodJavaStatement,
                               exitStatement: DeclaredMethodJavaStatement, successor: DeclaredMethodJavaStatement,
                               callee:          DeclaredMethod,
                               allNewExitFacts: Map[DeclaredMethodJavaStatement, Set[IFDSFact]]): Map[DeclaredMethodJavaStatement, Set[IFDSFact]] = {
        val in = allNewExitFacts.getOrElse(exitStatement, Set.empty)
        numberOfCalls.returnFlow += 1
        sumOfInputFactsForCallbacks += in.size
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
