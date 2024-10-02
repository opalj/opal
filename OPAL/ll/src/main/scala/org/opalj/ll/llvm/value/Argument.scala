/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ll
package llvm
package value

import org.opalj.ll.llvm.value

import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM.LLVMArgumentValueKind
import org.bytedeco.llvm.global.LLVM.LLVMGetParamParent
import org.bytedeco.llvm.global.LLVM.LLVMGetValueKind

/**
 * Represents an argument to a LLVM function.
 *
 * @param ref reference to an argument
 * @param index the index of the argument
 *
 * @author Marc Clement
 */
case class Argument(ref: LLVMValueRef, index: Int) extends Value(ref) {
    assert(LLVMGetValueKind(ref) == LLVMArgumentValueKind, "ref has to be an argument")

    def parent(): value.Function = value.Function(LLVMGetParamParent(ref))
}

object Argument {
    def apply(ref: LLVMValueRef): Argument =
        value.Function(LLVMGetParamParent(ref)).arguments.find(_.ref == ref).get
}
