/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ll.llvm.value

import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM.LLVMGetInitializer

case class GlobalVariable(ref: LLVMValueRef) extends Value(ref: LLVMValueRef) {
    def initializer: Value = Value(LLVMGetInitializer(ref)).get
}
