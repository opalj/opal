/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ll.llvm.value

import org.bytedeco.llvm.LLVM.LLVMValueRef

case class GlobalVariable(ref: LLVMValueRef) extends Value(ref: LLVMValueRef)
