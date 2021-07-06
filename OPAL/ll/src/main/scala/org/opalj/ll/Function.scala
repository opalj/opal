/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ll

import org.bytedeco.llvm.LLVM.{LLVMBasicBlockRef, LLVMValueRef}
import org.bytedeco.llvm.global.LLVM.{LLVMDisposeMessage, LLVMGetFirstBasicBlock, LLVMGetNextBasicBlock, LLVMGetValueName, LLVMPrintValueToString}

case class Function(ref: LLVMValueRef) {
    def repr(): String = {
        val bytePointer = LLVMPrintValueToString(ref)
        val string = bytePointer.getString
        LLVMDisposeMessage(bytePointer)
        string
    }

    def name(): String = {
        LLVMGetValueName(ref).getString
    }

    def basicBlocks(): BasicBlockIterator = {
        new BasicBlockIterator(LLVMGetFirstBasicBlock(ref))
    }
}

class BasicBlockIterator(var ref: LLVMBasicBlockRef) extends Iterator[BasicBlock] {
    override def hasNext: Boolean = LLVMGetNextBasicBlock(ref) != null

    override def next(): BasicBlock = {
        this.ref = LLVMGetNextBasicBlock(ref)
        BasicBlock(ref)
    }
}