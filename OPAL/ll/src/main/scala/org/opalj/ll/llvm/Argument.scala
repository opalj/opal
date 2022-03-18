/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ll.llvm

import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM.{LLVMArgumentValueKind, LLVMGetParamParent, LLVMGetValueKind}

case class Argument(ref: LLVMValueRef) extends Value(ref) {
    assert(LLVMGetValueKind(ref) == LLVMArgumentValueKind, "ref has to be an argument")

    def parent(): Function = Function(LLVMGetParamParent(ref))
}
