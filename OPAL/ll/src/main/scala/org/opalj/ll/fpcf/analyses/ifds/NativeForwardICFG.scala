/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ll.fpcf.analyses.ifds

import org.opalj.ifds.{AbstractIFDSFact, ICFG}
import org.opalj.ll.llvm.value.{Call, Function, Instruction, Ret, Terminator}

class NativeForwardICFG[IFDSFact <: AbstractIFDSFact] extends ICFG[IFDSFact, Function, LLVMStatement] {
    /**
     * Determines the statements at which the analysis starts.
     *
     * @param callable The analyzed callable.
     * @return The statements at which the analysis starts.
     */
    override def startStatements(callable: Function): Set[LLVMStatement] = {
        if (callable.basicBlockCount == 0)
            throw new IllegalArgumentException(s"${callable} does not contain any basic blocks and likely should not be in scope of the analysis")
        Set(LLVMStatement(callable.entryBlock.firstInstruction))
    }

    /**
     * Determines the statement, that will be analyzed after some other `statement`.
     *
     * @param statement The source statement.
     * @return The successor statements
     */
    override def nextStatements(statement: LLVMStatement): Set[LLVMStatement] = {
        if (!statement.instruction.isTerminator) return Set(LLVMStatement(statement.instruction.next.get))
        statement.instruction.asInstanceOf[Instruction with Terminator].successors.map(LLVMStatement(_)).toSet
    }

    /**
     * Gets the set of all methods possibly called at some statement.
     *
     * @param statement The statement.
     * @return All callables possibly called at the statement or None, if the statement does not
     *         contain a call.
     */
    override def getCalleesIfCallStatement(statement: LLVMStatement): Option[collection.Set[Function]] = {
        statement.instruction match {
            case call: Call ⇒ call.calledValue match {
                case function: Function ⇒ Some(Set(function))
                case _                  ⇒ Some(Set()) // TODO
            }
            case _ ⇒ None
        }
    }

    override def isExitStatement(statement: LLVMStatement): Boolean = statement.instruction match {
        case Ret(_) ⇒ true
        case _      ⇒ false
    }
}
