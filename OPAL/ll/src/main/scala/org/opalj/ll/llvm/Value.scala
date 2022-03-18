/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ll.llvm

import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM._

class OptionalValue

class Value(ref: LLVMValueRef) extends OptionalValue {
    def repr(): String = {
        val bytePointer = LLVMPrintValueToString(ref)
        val string = bytePointer.getString
        LLVMDisposeMessage(bytePointer)
        string
    }

    def name(): String = {
        LLVMGetValueName(ref).getString
    }

    def typ(): Type = Type(LLVMTypeOf(ref))
}

case class NullValue() extends OptionalValue

object Value {
    def apply(ref: LLVMValueRef): OptionalValue = {
        if (ref == null) return NullValue()
        if (ref.isNull) return NullValue()
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
