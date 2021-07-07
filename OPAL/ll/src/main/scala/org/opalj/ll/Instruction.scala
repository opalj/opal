/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ll

import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM.{LLVMGetInstructionOpcode, LLVMGetValueKind, LLVMInstructionValueKind, LLVMAlloca, LLVMStore, LLVMLoad, LLVMAdd, LLVMRet}

object Instruction {
    def fromValue(ref: LLVMValueRef): Instruction = {
        assert(LLVMGetValueKind(ref) == LLVMInstructionValueKind, "ref has to be an instruction")
        LLVMGetInstructionOpcode(ref) match {
            case LLVMAlloca => Alloca(ref)
            case LLVMStore => Store(ref)
            case LLVMLoad => Load(ref)
            case LLVMAdd => Add(ref)
            case LLVMRet => Ret(ref)
            case opCode => throw new IllegalArgumentException("unknown instruction opcode: " + opCode)
        }
    }
}

sealed abstract class Instruction(ref: LLVMValueRef) extends Value(ref)
case class Alloca(ref: LLVMValueRef) extends Instruction(ref)
case class Store(ref: LLVMValueRef) extends Instruction(ref)
case class Load(ref: LLVMValueRef) extends Instruction(ref)
case class Add(ref: LLVMValueRef) extends Instruction(ref)
case class Ret(ref: LLVMValueRef) extends Instruction(ref)


