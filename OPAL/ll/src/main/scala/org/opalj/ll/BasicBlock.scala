/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ll

import org.bytedeco.llvm.LLVM.{LLVMBasicBlockRef, LLVMValueRef}
import org.bytedeco.llvm.global.LLVM.{LLVMGetFirstInstruction, LLVMGetNextInstruction, LLVMBasicBlockAsValue}

case class BasicBlock(block_ref: LLVMBasicBlockRef) extends Value(LLVMBasicBlockAsValue(block_ref)) {
    def instructions(): InstructionIterator = {
        new InstructionIterator(LLVMGetFirstInstruction(block_ref))
    }
}

class InstructionIterator(var ref: LLVMValueRef) extends Iterator[Instruction] {
    override def hasNext: Boolean = LLVMGetNextInstruction(ref) != null

    override def next(): Instruction = {
        val instruction = Instruction.fromValue(ref)
        this.ref = LLVMGetNextInstruction(ref)
        instruction
    }
}
