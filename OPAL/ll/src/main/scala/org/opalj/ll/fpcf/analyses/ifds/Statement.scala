/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ll.fpcf.analyses.ifds

import org.opalj.fpcf.ifds.Statement
import org.opalj.ll.llvm.value.{BasicBlock, Instruction}

/**
 * A statement that is passed to the concrete analysis.
 *
 * @param instruction The LLVM instruction.
 */
case class LLVMStatement(instruction: Instruction) extends Statement[LLVMFunction, BasicBlock] {
    def function: LLVMFunction = LLVMFunction(instruction.function)
    def basicBlock: BasicBlock = instruction.parent
    override def node: BasicBlock = basicBlock
    override def callable: LLVMFunction = function
    override def toString: String = s"${function.name}\n\t${instruction}\n\t${function}"
}