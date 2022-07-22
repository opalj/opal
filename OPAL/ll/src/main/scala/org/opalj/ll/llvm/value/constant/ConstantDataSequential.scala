/* BSD 2-Clause License - see OPAL/LICENSE for details. */

package org.opalj.ll.llvm
package value
package constant

import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM.LLVMGetAsString
import org.bytedeco.javacpp.SizeTPointer

abstract class ConstantDataSequential(ref: LLVMValueRef) extends User(ref) {
    def asString: String = LLVMGetAsString(ref, new SizeTPointer(1)).getString
}

case class ConstantDataArray(ref: LLVMValueRef) extends ConstantDataSequential(ref)
case class ConstantDataVector(ref: LLVMValueRef) extends ConstantDataSequential(ref)
