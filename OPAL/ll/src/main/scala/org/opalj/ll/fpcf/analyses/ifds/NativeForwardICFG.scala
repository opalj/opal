/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ll.fpcf.analyses.ifds

import org.opalj.ll.llvm.{BasicBlock, Function, Instruction, Terminator}
import org.opalj.tac.fpcf.analyses.ifds.{AbstractIFDSFact}

class NativeForwardICFG[IFDSFact <: AbstractIFDSFact] extends ICFG[IFDSFact, Function, LLVMStatement, BasicBlock] {
    /**
     * Determines the basic blocks, at which the analysis starts.
     *
     * @param sourceFact The source fact of the analysis.
     * @param callable   The analyzed callable.
     * @return The basic blocks, at which the analysis starts.
     */
    override def startNodes(sourceFact: IFDSFact, callable: Function): Set[BasicBlock] = Set(callable.entryBlock())

    /**
     * Determines the nodes, that will be analyzed after some `basicBlock`.
     *
     * @param node The basic block, that was analyzed before.
     * @return The nodes, that will be analyzed after `basicBlock`.
     */
    override def nextNodes(node: BasicBlock): Set[BasicBlock] = node.terminator match {
        case Some(terminator) ⇒ terminator.successors().map(_.parent()).toSet
        case None             ⇒ Set.empty
    }

    /**
     * Checks, if some `node` is the last node.
     *
     * @return True, if `node` is the last node, i.e. there is no next node.
     */
    override def isLastNode(node: BasicBlock): Boolean = !node.hasSuccessors

    /**
     * Determines the first index of some `basic block`, that will be analyzed.
     *
     * @param basicBlock The basic block.
     * @return The first index of some `basic block`, that will be analyzed.
     */
    override def firstStatement(basicBlock: BasicBlock): LLVMStatement = LLVMStatement(basicBlock.firstInstruction())

    /**
     * Determines the last index of some `basic block`, that will be analzyed.
     *
     * @param basicBlock The basic block.
     * @return The last index of some `basic block`, that will be analzyed.
     */
    override def lastStatement(basicBlock: BasicBlock): LLVMStatement = LLVMStatement(basicBlock.lastInstruction())

    /**
     * Determines the statement that will be analyzed after some other statement.
     *
     * @param statement The current statement.
     * @return The statement that will be analyzed after `statement`.
     */
    override def nextStatement(statement: LLVMStatement): LLVMStatement = LLVMStatement(statement.instruction.next().get)

    /**
     * Determines the statement, that will be analyzed after some other `statement`.
     *
     * @param statement The source statement.
     * @return The successor statements
     */
    override def nextStatements(statement: LLVMStatement): Set[LLVMStatement] = {
        if (!statement.instruction.isTerminator) return Set(nextStatement(statement))
        statement.instruction.asInstanceOf[Instruction with Terminator].successors().map(LLVMStatement(_)).toSet
    }

    /**
     * Gets the set of all methods possibly called at some statement.
     *
     * @param statement The statement.
     * @return All callables possibly called at the statement or None, if the statement does not
     *         contain a call.
     */
    override def getCalleesIfCallStatement(statement: LLVMStatement): Option[collection.Set[Function]] = ???

    /**
     * Collects the output facts at the predecessors of the normal and abnormal return node.
     */
    override protected def collectResult(implicit state: State): Map[LLVMStatement, Set[IFDSFact]] = {
        Map.empty //state.outgoingFacts
    }

    /**
     * Calls callFlow.
     */
    override protected def callToStartFacts(
        call:   LLVMStatement,
        callee: Function,
        in:     Set[IFDSFact]
    )(implicit
        state: State,
      ifdsProblem: Problem,
      statistics:  Statistics
    ): Set[IFDSFact] = {
        statistics.numberOfCalls.callFlow += 1
        statistics.sumOfInputfactsForCallbacks += in.size
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
    )(implicit state: State, ifdsProblem: Problem): Map[LLVMStatement, Set[IFDSFact]] = {
        // First process for normal returns, then abnormal returns.
        var result = summaryEdges
        if (ifdsProblem.OPTIMIZE_CROSS_PRODUCT_IN_RETURN_FLOW) {
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
                               allNewExitFacts: Map[LLVMStatement, Set[IFDSFact]])(
        implicit
        statistics:  Statistics,
        ifdsProblem: Problem
    ): Map[LLVMStatement, Set[IFDSFact]] = {
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
