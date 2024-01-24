/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ll
package fpcf
package analyses
package ifds

import org.opalj.ifds.Statement
import org.opalj.ll.llvm.value.BasicBlock
import org.opalj.ll.llvm.value.Instruction

/**
 * A statement that is passed to the concrete analysis.
 *
 * @param instruction The LLVM instruction.
 */
case class LLVMStatement(instruction: Instruction) extends Statement[LLVMFunction, BasicBlock] {
    lazy val function: LLVMFunction = LLVMFunction(instruction.function)
    override def basicBlock: BasicBlock = instruction.parent
    override def callable: LLVMFunction = function
    override def toString: String = s"${function.name}\n\t${instruction}\n\t${function}"
}
