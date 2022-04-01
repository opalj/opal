/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ll.llvm.value

import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM._
import org.opalj.ll.llvm.Type

class Value(ref: LLVMValueRef) {
    def repr: String = {
        val bytePointer = LLVMPrintValueToString(ref)
        val string = bytePointer.getString
        LLVMDisposeMessage(bytePointer)
        string
    }

    def name: String = {
        LLVMGetValueName(ref).getString
    }

    def typ: Type = Type(LLVMTypeOf(ref))

    val address = ref.address
    override def equals(other: Any): Boolean =
        other.isInstanceOf[Value] && address == other.asInstanceOf[Value].address

    override def toString: String = {
        s"${getClass.getSimpleName}(${repr})"
    }
}

object Value {
    def apply(ref: LLVMValueRef): Option[Value] = {
        if (ref == null) return None
        if (ref.isNull) return None
        Some(LLVMGetValueKind(ref) match {
            case LLVMArgumentValueKind       ⇒ Argument(ref)
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
            case LLVMConstantIntValueKind    ⇒ ConstantIntValue(ref)
            //LLVMConstantFPValueKind
            //LLVMConstantPointerNullValueKind
            //LLVMConstantTokenNoneValueKind
            //LLVMMetadataAsValueValueKind
            //LLVMInlineAsmValueKind
            case LLVMInstructionValueKind    ⇒ Instruction(ref)
            //LLVMPoisonValueValueKind
            case valueKind                   ⇒ throw new IllegalArgumentException("unknown valueKind: "+valueKind)
        })
    }
}
