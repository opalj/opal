/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ll.fpcf.analyses.ifds

import org.opalj.br.cfg.CFGNode
import org.opalj.ll.llvm.Function
import org.opalj.tac.fpcf.analyses.ifds.{AbstractIFDSAnalysis, AbstractIFDSFact}
import org.opalj.tac.fpcf.properties.IFDSPropertyMetaInformation
import org.opalj.tac.{Return, ReturnValue}

/**
 * An IFDS analysis, which analyzes the code in the control flow direction.
 *
 * @author Mario Trageser
 */

abstract class ForwardNativeIFDSAnalysis[IFDSFact <: AbstractIFDSFact](ifdsProblem: NativeIFDSProblem[IFDSFact], propertyKey: IFDSPropertyMetaInformation[LLVMStatement, IFDSFact]) extends AbstractNativeIFDSAnalysis[IFDSFact](ifdsProblem, new NativeForwardICFG[IFDSFact], propertyKey) {
    /**
     * Collects the output facts at the predecessors of the normal and abnormal return node.
     */
    override protected def collectResult(implicit state: State): Map[LLVMStatement, Set[IFDSFact]] = {
        mergeMaps(
            resultOfExitNode(state.cfg.normalReturnNode),
            resultOfExitNode(state.cfg.abnormalReturnNode)
        )
    }

    /**
     * Calls callFlow.
     */
    override protected def callToStartFacts(call: LLVMStatement, callee: Function,
                                            in: Set[IFDSFact])(implicit state: State): Set[IFDSFact] = {
        numberOfCalls.callFlow += 1
        sumOfInputfactsForCallbacks += in.size
        ifdsProblem.callFlow(call, callee, in, state.source)
    }

    /**
     * Combines each normal exit node with each normal successor and each abnormal exit statement
     * with each catch node. Calls returnFlow for those pairs and adds them to the summary edges.
     */
    override protected def addExitToReturnFacts(
        summaryEdges: Map[LLVMStatement, Set[IFDSFact]],
        successors:   Set[LLVMStatement], call: LLVMStatement,
        callee:    Function,
        exitFacts: Map[LLVMStatement, Set[IFDSFact]]
    )(implicit state: State): Map[LLVMStatement, Set[IFDSFact]] = {
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
    private def nextStatementsWithNode(statement: LLVMStatement)(implicit state: State): Map[LLVMStatement, CFGNode] = {
        val index = statement.index
        val basicBlock = statement.node.asBasicBlock
        if (index == basicBlock.endPC)
            basicBlock.successors.iterator
                .map(successorNode ⇒ firstStatement(successorNode) → successorNode).toMap
        else {
            val nextIndex = index + 1
            Map(LLVMStatement(statement.method, basicBlock, statement.code(nextIndex), nextIndex,
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
    ): Map[LLVMStatement, Set[IFDSFact]] = {
        var result = Map.empty[LLVMStatement, Set[IFDSFact]]
        exit.predecessors foreach { predecessor ⇒
            if (predecessor.isBasicBlock) {
                val basicBlock = predecessor.asBasicBlock
                val exitFacts = state.outgoingFacts.get(basicBlock).flatMap(_.get(exit))
                if (exitFacts.isDefined) {
                    val lastIndex = basicBlock.endPC
                    val stmt = LLVMStatement(state.method, basicBlock, state.code(lastIndex),
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
    private def addSummaryEdge(summaryEdges: Map[LLVMStatement, Set[IFDSFact]], call: LLVMStatement,
                               exitStatement: LLVMStatement, successor: LLVMStatement,
                               callee:          Function,
                               allNewExitFacts: Map[LLVMStatement, Set[IFDSFact]]): Map[LLVMStatement, Set[IFDSFact]] = {
        val in = allNewExitFacts.getOrElse(exitStatement, Set.empty)
        numberOfCalls.returnFlow += 1
        sumOfInputfactsForCallbacks += in.size
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
