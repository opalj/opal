/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ll.llvm

import org.bytedeco.llvm.LLVM.{LLVMBasicBlockRef, LLVMValueRef}
import org.bytedeco.llvm.global.LLVM.{
    LLVMBasicBlockAsValue,
    LLVMGetBasicBlockTerminator,
    LLVMGetFirstInstruction,
    LLVMGetNextInstruction
}

case class BasicBlock(block_ref: LLVMBasicBlockRef)
    extends Value(LLVMBasicBlockAsValue(block_ref)) {
    def instructions(): InstructionIterator = {
        new InstructionIterator(LLVMGetFirstInstruction(block_ref))
    }

    def terminator(): Instruction = {
        val instruction = Instruction(LLVMGetBasicBlockTerminator(block_ref))
        assert(instruction.is_terminator())
        instruction
    }
}

class InstructionIterator(var ref: LLVMValueRef) extends Iterator[Instruction] {
    override def hasNext: Boolean = LLVMGetNextInstruction(ref) != null

    override def next(): Instruction = {
        val instruction = Instruction(ref)
        this.ref = LLVMGetNextInstruction(ref)
        instruction
    }
}
