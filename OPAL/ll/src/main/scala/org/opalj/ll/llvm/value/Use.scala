/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ll
package llvm
package value

import org.bytedeco.llvm.LLVM.LLVMUseRef
import org.bytedeco.llvm.global.LLVM.LLVMGetUsedValue
import org.bytedeco.llvm.global.LLVM.LLVMGetUser

case class Use(ref: LLVMUseRef) {
    def value: Value = Value(LLVMGetUsedValue(ref)).get
    def user: Value = Value(LLVMGetUser(ref)).get
}
