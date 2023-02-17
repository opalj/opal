/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ll
package llvm
package value
package constant

import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM.LLVMConstIntGetSExtValue
import org.bytedeco.llvm.global.LLVM.LLVMConstIntGetZExtValue
import org.opalj.ll.llvm.value.User

case class ConstantIntValue(ref: LLVMValueRef) extends User(ref) {
    def zeroExtendedValue: Long = LLVMConstIntGetZExtValue(ref)
    def signExtendedValue: Long = LLVMConstIntGetSExtValue(ref)
}
