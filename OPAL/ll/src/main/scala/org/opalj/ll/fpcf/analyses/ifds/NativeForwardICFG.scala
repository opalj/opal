/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ll.fpcf.analyses.ifds

import org.opalj.br.analyses.SomeProject
import org.opalj.ll.cg.PhasarCallGraphParser.PhasarCallGraph
import org.opalj.ll.llvm.value.{Instruction, Ret, Terminator}

class NativeForwardICFG(project: SomeProject) extends NativeICFG(project) {
    /**
     * Determines the statements at which the analysis starts.
     *
     * @param callable The analyzed callable.
     * @return The statements at which the analysis starts.
     */
    override def startStatements(callable: NativeFunction): Set[LLVMStatement] = callable match {
        case LLVMFunction(function) => {
            if (function.basicBlockCount == 0)
                throw new IllegalArgumentException(s"${callable} does not contain any basic blocks and likely should not be in scope of the analysis")
            Set(LLVMStatement(function.entryBlock.firstInstruction))
        }
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
     * Determines whether the statement is an exit statement.
     *
     * @param statement The source statement.
     * @return Whether the statement flow may exit its callable (function/method)
     */
    override def isExitStatement(statement: LLVMStatement): Boolean = statement.instruction match {
        case Ret(_) => true
        case _      => false
    }
}
