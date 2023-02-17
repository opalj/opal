/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ll
package llvm
package value

import org.bytedeco.llvm.LLVM.LLVMBasicBlockRef
import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM.LLVMCountBasicBlocks
import org.bytedeco.llvm.global.LLVM.LLVMCountParams
import org.bytedeco.llvm.global.LLVM.LLVMGetEntryBasicBlock
import org.bytedeco.llvm.global.LLVM.LLVMGetFirstBasicBlock
import org.bytedeco.llvm.global.LLVM.LLVMGetFirstParam
import org.bytedeco.llvm.global.LLVM.LLVMGetNextBasicBlock
import org.bytedeco.llvm.global.LLVM.LLVMGetNextParam
import org.bytedeco.llvm.global.LLVM.LLVMGetParam
import org.bytedeco.llvm.global.LLVM.LLVMGetValueKind
import org.bytedeco.llvm.global.LLVM.LLVMViewFunctionCFG
import org.bytedeco.llvm.global.LLVM.LLVMViewFunctionCFGOnly
import org.bytedeco.llvm.global.LLVM.LLVMFunctionValueKind
import org.opalj.io.writeAndOpen

case class Function(ref: LLVMValueRef) extends Value(ref) {
    assert(LLVMGetValueKind(ref) == LLVMFunctionValueKind, "ref has to be a function")

    def basicBlocks: BasicBlockIterator = {
        new BasicBlockIterator(LLVMGetFirstBasicBlock(ref))
    }
    def basicBlockCount: Int = LLVMCountBasicBlocks(ref)

    def arguments: ArgumentIterator = {
        new ArgumentIterator(LLVMGetFirstParam(ref))
    }
    def argumentCount: Int = LLVMCountParams(ref)
    def argument(index: Int): Argument = {
        assert(index < argumentCount)
        Argument(LLVMGetParam(ref, index), index)
    }

    def entryBlock: BasicBlock = {
        if (basicBlockCount == 0) throw new IllegalStateException("this function does not contain any basic block and may not be defined")
        BasicBlock(LLVMGetEntryBasicBlock(ref))
    }

    def viewCFG(): Unit = {
        val cfg_dot = org.opalj.graphs.toDot(Set(entryBlock))
        writeAndOpen(cfg_dot, name+"-CFG", ".gv")
    }

    def viewLLVMCFG(include_content: Boolean = true): Unit = {
        if (include_content) {
            LLVMViewFunctionCFG(ref)
        } else {
            LLVMViewFunctionCFGOnly(ref)
        }
    }

    override def toString: String = {
        s"Function(${name}(${arguments.map(_.typ.repr).mkString(", ")}))"
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

class ArgumentIterator(var ref: LLVMValueRef) extends Iterator[Argument] {
    override def hasNext: Boolean = ref != null
    var index = 0

    override def next(): Argument = {
        val argument = Argument(ref, index)
        this.ref = LLVMGetNextParam(ref)
        index += 1
        argument
    }
}