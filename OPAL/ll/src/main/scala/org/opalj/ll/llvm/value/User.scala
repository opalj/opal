/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ll.llvm
package value

import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM._

class User(ref: LLVMValueRef) extends Value(ref) {
    def numOperands: Int = LLVMGetNumOperands(ref)
    def operand(index: Int): Value = {
        assert(index < numOperands)
        Value(LLVMGetOperand(ref, index)).get
    }
    def operandUse(index: Int): UsesIterator = {
        assert(index < numOperands)
        new UsesIterator(LLVMGetOperandUse(ref, index))
    }
}
