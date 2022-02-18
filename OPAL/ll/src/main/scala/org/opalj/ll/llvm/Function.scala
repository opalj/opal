/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ll.llvm

import org.bytedeco.llvm.LLVM.{LLVMBasicBlockRef, LLVMValueRef}
import org.bytedeco.llvm.global.LLVM.{
    LLVMFunctionValueKind,
    LLVMGetEntryBasicBlock,
    LLVMGetFirstBasicBlock,
    LLVMGetNextBasicBlock,
    LLVMGetValueKind,
    LLVMViewFunctionCFG,
    LLVMViewFunctionCFGOnly
}
import org.opalj.io.writeAndOpen

case class Function(ref: LLVMValueRef) extends Value(ref) {
    assert(LLVMGetValueKind(ref) == LLVMFunctionValueKind, "ref has to be a function")

    def basicBlocks(): BasicBlockIterator = {
        new BasicBlockIterator(LLVMGetFirstBasicBlock(ref))
    }

    def entryBlock(): BasicBlock = {
        BasicBlock(LLVMGetEntryBasicBlock(ref))
    }

    def viewCFG(): Unit = {
        val entryNode = entryBlock()
        val cfg_dot = org.opalj.graphs.toDot(Set(entryNode))
        writeAndOpen(cfg_dot, name+"-CFG", ".gv")
    }

    def viewLLVMCFG(include_content: Boolean = true): Unit = {
        if (include_content) {
            LLVMViewFunctionCFG(ref)
        } else {
            LLVMViewFunctionCFGOnly(ref)
        }
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
