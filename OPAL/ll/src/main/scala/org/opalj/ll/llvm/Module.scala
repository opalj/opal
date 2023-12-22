/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ll
package llvm

import org.opalj.ll.llvm.value.Function
import org.opalj.ll.llvm.value.Value

import org.bytedeco.llvm.LLVM.LLVMModuleRef
import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM.LLVMDisposeMessage
import org.bytedeco.llvm.global.LLVM.LLVMGetFirstFunction
import org.bytedeco.llvm.global.LLVM.LLVMGetNamedFunction
import org.bytedeco.llvm.global.LLVM.LLVMGetNextFunction
import org.bytedeco.llvm.global.LLVM.LLVMPrintModuleToString

/**
 * Wrapper representing a LLVM module
 *
 * @param ref the reference to the module
 *
 * @author Marc Clement
 */
case class Module(ref: LLVMModuleRef) {
    def functions: FunctionIterator = {
        new FunctionIterator(LLVMGetFirstFunction(ref))
    }

    def repr: String = {
        val bytePointer = LLVMPrintModuleToString(ref)
        val string = bytePointer.getString
        LLVMDisposeMessage(bytePointer)
        string
    }

    def function(name: String): Function =
        Value(LLVMGetNamedFunction(ref, name)) match {
            case None =>
                throw new IllegalArgumentException("Unknown function '" + name + "'")
            case Some(function: Function) => function
            case Some(_) => throw new IllegalStateException(
                "Expected LLVMGetNamedFunction to return a Function ref"
            )
        }
}

/**
 * Iterates over all functions in a value
 *
 * @author Marc Clement
 */
class FunctionIterator(var ref: LLVMValueRef) extends Iterator[Function] {
    override def hasNext: Boolean = ref != null

    override def next(): Function = {
        val function = Function(ref)
        this.ref = LLVMGetNextFunction(ref)
        function
    }
}
