/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ll.llvm

import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM._

class Value(ref: LLVMValueRef) {
    def repr(): String = {
        val bytePointer = LLVMPrintValueToString(ref)
        val string = bytePointer.getString
        LLVMDisposeMessage(bytePointer)
        string
    }

    def name(): String = {
        LLVMGetValueName(ref).getString
    }
}

object Value {
    def apply(ref: LLVMValueRef): Value = {
        LLVMGetValueKind(ref) match {
            //LLVMArgumentValueKind
            case LLVMBasicBlockValueKind     ⇒ BasicBlock(LLVMValueAsBasicBlock(ref))
            //LLVMMemoryUseValueKind
            //LLVMMemoryDefValueKind
            //LLVMMemoryPhiValueKind
            case LLVMFunctionValueKind       ⇒ Function(ref)
            //LLVMGlobalAliasValueKind
            //LLVMGlobalIFuncValueKind
            case LLVMGlobalVariableValueKind ⇒ GlobalVariable(ref)
            //LLVMBlockAddressValueKind
            //LLVMConstantExprValueKind
            //LLVMConstantArrayValueKind
            //LLVMConstantStructValueKind
            //LLVMConstantVectorValueKind
            //LLVMUndefValueValueKind
            //LLVMConstantAggregateZeroValueKind
            //LLVMConstantDataArrayValueKind
            //LLVMConstantDataVectorValueKind
            //LLVMConstantIntValueKind
            //LLVMConstantFPValueKind
            //LLVMConstantPointerNullValueKind
            //LLVMConstantTokenNoneValueKind
            //LLVMMetadataAsValueValueKind
            //LLVMInlineAsmValueKind
            case LLVMInstructionValueKind    ⇒ Instruction(ref)
            //LLVMPoisonValueValueKind
            case valueKind                   ⇒ throw new IllegalArgumentException("unknown valueKind: "+valueKind)
        }
    }
}
