/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ll.llvm.value

import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM.{LLVMConstIntGetSExtValue, LLVMConstIntGetZExtValue}

case class ConstantIntValue(ref: LLVMValueRef) extends Value(ref) {
    def zeroExtendedValue(): Long = LLVMConstIntGetZExtValue(ref)
    def signExtendedValue(): Long = LLVMConstIntGetSExtValue(ref)
}
