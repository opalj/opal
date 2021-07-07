/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ll

import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM.{LLVMDisposeMessage, LLVMGetValueName, LLVMPrintValueToString}

abstract class Value (ref: LLVMValueRef) {
    def repr(): String = {
        val bytePointer = LLVMPrintValueToString(ref)
        val string = bytePointer.getString
        LLVMDisposeMessage(bytePointer)
        string
    }

    def name(): String = {
        LLVMGetValueName(ref).getString
    }
}
