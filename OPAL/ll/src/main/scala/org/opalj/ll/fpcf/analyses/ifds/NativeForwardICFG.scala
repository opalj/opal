/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ll.fpcf.analyses.ifds

import org.opalj.ll.llvm.{BasicBlock, Function, Instruction, Ret, Terminator}
import org.opalj.tac.fpcf.analyses.ifds.AbstractIFDSFact

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
    override def getCalleesIfCallStatement(statement: LLVMStatement): Option[collection.Set[Function]] = None //TODO

    override def isExitStatement(statement: LLVMStatement): Boolean = statement.instruction match {
        case Ret(_) ⇒ true
        case _      ⇒ false
    }
}
