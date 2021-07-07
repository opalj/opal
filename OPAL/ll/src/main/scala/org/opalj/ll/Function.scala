/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ll

import org.bytedeco.llvm.LLVM.{LLVMBasicBlockRef, LLVMValueRef}
import org.bytedeco.llvm.global.LLVM.{LLVMFunctionValueKind, LLVMGetFirstBasicBlock, LLVMGetNextBasicBlock, LLVMGetValueKind}

case class Function(ref: LLVMValueRef) extends Value(ref) {
    assert(LLVMGetValueKind(ref) == LLVMFunctionValueKind, "ref has to be a function")

    def basicBlocks(): BasicBlockIterator = {
        new BasicBlockIterator(LLVMGetFirstBasicBlock(ref))
    }
}

class BasicBlockIterator(var ref: LLVMBasicBlockRef) extends Iterator[BasicBlock] {
    override def hasNext: Boolean = ref != null

    override def next(): BasicBlock = {
        val basicBlock = BasicBlock(ref)
        this.ref = LLVMGetNextBasicBlock(ref)
        basicBlock
    }
}