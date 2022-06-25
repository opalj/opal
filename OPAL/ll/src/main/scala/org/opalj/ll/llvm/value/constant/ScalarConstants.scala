/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ll.llvm.value
package constant

import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM.{LLVMConstIntGetSExtValue, LLVMConstIntGetZExtValue}
import org.opalj.ll.llvm.value.User

case class ConstantIntValue(ref: LLVMValueRef) extends User(ref) {
    def zeroExtendedValue: Long = LLVMConstIntGetZExtValue(ref)
    def signExtendedValue: Long = LLVMConstIntGetSExtValue(ref)
}
