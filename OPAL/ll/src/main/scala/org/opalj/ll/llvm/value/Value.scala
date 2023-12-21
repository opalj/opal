/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ll
package llvm
package value

import org.opalj.ll.llvm.value.constant.ConstantDataArray
import org.opalj.ll.llvm.value.constant.ConstantDataVector
import org.opalj.ll.llvm.value.constant.ConstantExpression
import org.opalj.ll.llvm.value.constant.ConstantIntValue

import org.bytedeco.llvm.LLVM.LLVMUseRef
import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM.LLVMArgumentValueKind
import org.bytedeco.llvm.global.LLVM.LLVMBasicBlockValueKind
import org.bytedeco.llvm.global.LLVM.LLVMConstantDataArrayValueKind
import org.bytedeco.llvm.global.LLVM.LLVMConstantDataVectorValueKind
import org.bytedeco.llvm.global.LLVM.LLVMConstantExprValueKind
import org.bytedeco.llvm.global.LLVM.LLVMConstantIntValueKind
import org.bytedeco.llvm.global.LLVM.LLVMDisposeMessage
import org.bytedeco.llvm.global.LLVM.LLVMFunctionValueKind
import org.bytedeco.llvm.global.LLVM.LLVMGetFirstUse
import org.bytedeco.llvm.global.LLVM.LLVMGetNextUse
import org.bytedeco.llvm.global.LLVM.LLVMGetValueKind
import org.bytedeco.llvm.global.LLVM.LLVMGetValueName
import org.bytedeco.llvm.global.LLVM.LLVMGlobalVariableValueKind
import org.bytedeco.llvm.global.LLVM.LLVMInstructionValueKind
import org.bytedeco.llvm.global.LLVM.LLVMPrintValueToString
import org.bytedeco.llvm.global.LLVM.LLVMTypeOf
import org.bytedeco.llvm.global.LLVM.LLVMValueAsBasicBlock

class Value(ref: LLVMValueRef) {
    val address: Long = ref.address

    def repr: String = {
        val bytePointer = LLVMPrintValueToString(ref)
        val string = bytePointer.getString
        LLVMDisposeMessage(bytePointer)
        string
    }

    def name: String = {
        LLVMGetValueName(ref).getString
    }

    def tpe: Type = Type(LLVMTypeOf(ref)) // because type is a keyword

    override def equals(other: Any): Boolean =
        other.isInstanceOf[Value] && address == other.asInstanceOf[Value].address

    override def toString: String = {
        s"${getClass.getSimpleName}(${repr})"
    }

    def uses: UsesIterator = new UsesIterator(LLVMGetFirstUse(ref))
    def users: Iterator[Value] = uses.map(_.user)
}

/**
 * This returns the corresponding sub type of a value.
 * This will throw an IllegalArgumentException if we do not handle the value kind at the moment.
 *
 * @author Marc Clement
 */
object Value {
    def apply(ref: LLVMValueRef): Option[Value] = {
        if (ref == null) return None
        if (ref.isNull) return None
        Some(LLVMGetValueKind(ref) match {
            case LLVMArgumentValueKind           => Argument(ref)
            case LLVMBasicBlockValueKind         => BasicBlock(LLVMValueAsBasicBlock(ref))
            // LLVMMemoryUseValueKind
            // LLVMMemoryDefValueKind
            // LLVMMemoryPhiValueKind
            case LLVMFunctionValueKind           => Function(ref)
            // LLVMGlobalAliasValueKind
            // LLVMGlobalIFuncValueKind
            case LLVMGlobalVariableValueKind     => GlobalVariable(ref)
            // LLVMBlockAddressValueKind
            case LLVMConstantExprValueKind       => ConstantExpression(ref)
            // LLVMConstantArrayValueKind
            // LLVMConstantStructValueKind
            // LLVMConstantVectorValueKind
            // LLVMUndefValueValueKind
            // LLVMConstantAggregateZeroValueKind
            case LLVMConstantDataArrayValueKind  => ConstantDataArray(ref)
            case LLVMConstantDataVectorValueKind => ConstantDataVector(ref)
            case LLVMConstantIntValueKind        => ConstantIntValue(ref)
            // LLVMConstantFPValueKind
            // LLVMConstantPointerNullValueKind
            // LLVMConstantTokenNoneValueKind
            // LLVMMetadataAsValueValueKind
            // LLVMInlineAsmValueKind
            case LLVMInstructionValueKind        => Instruction(ref)
            // LLVMPoisonValueValueKind
            case valueKind                       => throw new IllegalArgumentException("unknown valueKind: " + valueKind)
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
