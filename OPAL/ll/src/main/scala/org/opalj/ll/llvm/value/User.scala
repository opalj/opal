/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ll
package llvm
package value

import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM.LLVMGetNumOperands
import org.bytedeco.llvm.global.LLVM.LLVMGetOperand
import org.bytedeco.llvm.global.LLVM.LLVMGetOperandUse

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
