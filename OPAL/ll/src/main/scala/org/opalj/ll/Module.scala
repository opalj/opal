/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ll

import org.bytedeco.llvm.LLVM.{LLVMModuleRef, LLVMValueRef}
import org.bytedeco.llvm.global.LLVM.{LLVMDisposeMessage, LLVMGetFirstFunction, LLVMGetNextFunction, LLVMPrintModuleToString}

case class Module(ref: LLVMModuleRef) {
    def functions(): FunctionIterator = {
        new FunctionIterator(LLVMGetFirstFunction(ref))
    }

    def repr(): String = {
      val bytePointer = LLVMPrintModuleToString(ref)
      val string = bytePointer.getString
      LLVMDisposeMessage(bytePointer)
      string
    }
}

class FunctionIterator(var ref: LLVMValueRef) extends Iterator[Function] {
    override def hasNext: Boolean = LLVMGetNextFunction(ref) != null

    override def next(): Function = {
        this.ref = LLVMGetNextFunction(ref)
        Function(ref)
    }
}
