/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ll.llvm.value

import org.bytedeco.llvm.LLVM.LLVMUseRef
import org.bytedeco.llvm.global.LLVM.{LLVMGetUsedValue, LLVMGetUser}

case class Use(ref: LLVMUseRef) {
    def value: Value = Value(LLVMGetUsedValue(ref)).get
    def user: Value = Value(LLVMGetUser(ref)).get
}
