/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ll
package llvm
package value

import org.opalj.io.writeAndOpen

import org.bytedeco.llvm.LLVM.LLVMBasicBlockRef
import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM.LLVMCountBasicBlocks
import org.bytedeco.llvm.global.LLVM.LLVMCountParams
import org.bytedeco.llvm.global.LLVM.LLVMDisposeMessage
import org.bytedeco.llvm.global.LLVM.LLVMFunctionValueKind
import org.bytedeco.llvm.global.LLVM.LLVMGetEntryBasicBlock
import org.bytedeco.llvm.global.LLVM.LLVMGetFirstBasicBlock
import org.bytedeco.llvm.global.LLVM.LLVMGetFirstParam
import org.bytedeco.llvm.global.LLVM.LLVMGetNextBasicBlock
import org.bytedeco.llvm.global.LLVM.LLVMGetNextParam
import org.bytedeco.llvm.global.LLVM.LLVMGetParam
import org.bytedeco.llvm.global.LLVM.LLVMGetReturnType
import org.bytedeco.llvm.global.LLVM.LLVMGetValueKind
import org.bytedeco.llvm.global.LLVM.LLVMPrintTypeToString
import org.bytedeco.llvm.global.LLVM.LLVMTypeOf
import org.bytedeco.llvm.global.LLVM.LLVMViewFunctionCFG
import org.bytedeco.llvm.global.LLVM.LLVMViewFunctionCFGOnly

/**
 * This represents a LLVM function
 *
 * @param ref reference to a LLVM function.
 *
 * @author Marc Clement
 */
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

    lazy val returnType: String = {
        val t = LLVMPrintTypeToString(LLVMGetReturnType(LLVMTypeOf(ref)))
        val s = t.toString
        LLVMDisposeMessage(t)
        s
    }

    def entryBlock: BasicBlock = {
        if (basicBlockCount == 0) throw new IllegalStateException(
            "this function does not contain any basic block and may not be defined"
        )
        BasicBlock(LLVMGetEntryBasicBlock(ref))
    }

    def exitBlocks: Iterator[BasicBlock] = {
        if (basicBlockCount == 0) throw new IllegalStateException(
            "this function does not contain any basic block and may not be defined"
        )
        basicBlocks.filter(bb => bb.lastInstruction match {
            case Ret(_) => true
            case _      => false
        })
    }

    def viewCFG(): Unit = {
        val cfg_dot = org.opalj.graphs.toDot(Set(entryBlock))
        writeAndOpen(cfg_dot, name + "-CFG", ".gv")
    }

    def viewLLVMCFG(include_content: Boolean = true): Unit = {
        if (include_content) {
            LLVMViewFunctionCFG(ref)
        } else {
            LLVMViewFunctionCFGOnly(ref)
        }
    }

    def getSignature: String = {
        s"$returnType $name(${arguments.map(_.tpe.repr).mkString(", ")})"
    }

    override def toString: String = {
        s"Function($name(${arguments.map(_.tpe.repr).mkString(", ")}))"
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
