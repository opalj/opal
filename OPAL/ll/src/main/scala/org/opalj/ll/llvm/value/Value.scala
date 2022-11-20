/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ll.llvm
package value

import org.bytedeco.llvm.LLVM.{LLVMUseRef, LLVMValueRef}
import org.bytedeco.llvm.global.LLVM._
import org.opalj.ll.llvm.value.constant.{ConstantDataArray, ConstantDataVector, ConstantExpression, ConstantIntValue}

class Value(val ref: LLVMValueRef) {
    def repr: String = {
        val bytePointer = LLVMPrintValueToString(ref)
        val string = bytePointer.getString
        LLVMDisposeMessage(bytePointer)
        string
    }

    def name: String = {
        LLVMGetValueName(ref).getString
    }

    def typ: Type = Type(LLVMTypeOf(ref)) // because type is a keyword

    val address = ref.address
    override def equals(other: Any): Boolean =
        other.isInstanceOf[Value] && address == other.asInstanceOf[Value].address

    override def toString: String = {
        s"${getClass.getSimpleName}(${repr})"
    }

    def uses: UsesIterator = new UsesIterator(LLVMGetFirstUse(ref))
    def users: Iterator[Value] = uses.map(_.user)
}

object Value {
    def apply(ref: LLVMValueRef): Option[Value] = {
        if (ref == null) return None
        if (ref.isNull) return None
        Some(LLVMGetValueKind(ref) match {
            case LLVMArgumentValueKind           => Argument(ref)
            case LLVMBasicBlockValueKind         => BasicBlock(LLVMValueAsBasicBlock(ref))
            //LLVMMemoryUseValueKind
            //LLVMMemoryDefValueKind
            //LLVMMemoryPhiValueKind
            case LLVMFunctionValueKind           => Function(ref)
            //LLVMGlobalAliasValueKind
            //LLVMGlobalIFuncValueKind
            case LLVMGlobalVariableValueKind     => GlobalVariable(ref)
            //LLVMBlockAddressValueKind
            case LLVMConstantExprValueKind       => ConstantExpression(ref)
            //LLVMConstantArrayValueKind
            //LLVMConstantStructValueKind
            //LLVMConstantVectorValueKind
            //LLVMUndefValueValueKind
            //LLVMConstantAggregateZeroValueKind
            case LLVMConstantDataArrayValueKind  => ConstantDataArray(ref)
            case LLVMConstantDataVectorValueKind => ConstantDataVector(ref)
            case LLVMConstantIntValueKind        => ConstantIntValue(ref)
            //LLVMConstantFPValueKind
            //LLVMConstantPointerNullValueKind
            //LLVMConstantTokenNoneValueKind
            //LLVMMetadataAsValueValueKind
            //LLVMInlineAsmValueKind
            case LLVMInstructionValueKind        => Instruction(ref)
            //LLVMPoisonValueValueKind
            case valueKind                       => throw new IllegalArgumentException("unknown valueKind: "+valueKind)
        })
    }
}

class UsesIterator(var ref: LLVMUseRef) extends Iterator[Use] {
    override def hasNext: Boolean = ref != null

    override def next(): Use = {
        val use = value.Use(ref)
        this.ref = LLVMGetNextUse(ref)
        use
    }
}
