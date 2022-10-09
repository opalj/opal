/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ll.llvm.value

import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM._
import org.opalj.ll.llvm.value.constant.ConstantIntValue
import org.opalj.ll.llvm.{FunctionType, Type}

object OptionalInstruction {
    def apply(ref: LLVMValueRef): Option[Instruction] = {
        if (ref.isNull) return None
        Some(Instruction(ref))
    }
}

object Instruction {
    def apply(ref: LLVMValueRef): Instruction = {
        assert(ref != null && !ref.isNull(), "ref may not be null")
        assert(LLVMGetValueKind(ref) == LLVMInstructionValueKind, "ref has to be an instruction")
        LLVMGetInstructionOpcode(ref) match {
            case LLVMRet            => Ret(ref)
            case LLVMBr             => Br(ref)
            case LLVMSwitch         => Switch(ref)
            case LLVMIndirectBr     => IndirectBr(ref)
            case LLVMInvoke         => Invoke(ref)
            case LLVMUnreachable    => Unreachable(ref)
            case LLVMCallBr         => CallBr(ref)
            case LLVMFNeg           => FNeg(ref)
            case LLVMAdd            => Add(ref)
            case LLVMFAdd           => FAdd(ref)
            case LLVMSub            => Sub(ref)
            case LLVMFSub           => FSub(ref)
            case LLVMMul            => Mul(ref)
            case LLVMFMul           => FMul(ref)
            case LLVMUDiv           => UDiv(ref)
            case LLVMSDiv           => SDiv(ref)
            case LLVMFDiv           => FDiv(ref)
            case LLVMURem           => URem(ref)
            case LLVMSRem           => SRem(ref)
            case LLVMFRem           => FRem(ref)
            case LLVMShl            => Shl(ref)
            case LLVMLShr           => LShr(ref)
            case LLVMAShr           => AShr(ref)
            case LLVMAnd            => And(ref)
            case LLVMOr             => Or(ref)
            case LLVMXor            => Xor(ref)
            case LLVMAlloca         => Alloca(ref)
            case LLVMLoad           => Load(ref)
            case LLVMStore          => Store(ref)
            case LLVMGetElementPtr  => GetElementPtr(ref)
            case LLVMTrunc          => Trunc(ref)
            case LLVMZExt           => ZExt(ref)
            case LLVMSExt           => SExt(ref)
            case LLVMFPToUI         => FPToUI(ref)
            case LLVMFPToSI         => FPToSI(ref)
            case LLVMUIToFP         => UIToFP(ref)
            case LLVMSIToFP         => SIToFP(ref)
            case LLVMFPTrunc        => FPTrunc(ref)
            case LLVMFPExt          => FPExt(ref)
            case LLVMPtrToInt       => PtrToInt(ref)
            case LLVMIntToPtr       => IntToPtr(ref)
            case LLVMBitCast        => BitCast(ref)
            case LLVMAddrSpaceCast  => AddrSpaceCast(ref)
            case LLVMICmp           => ICmp(ref)
            case LLVMFCmp           => FCmp(ref)
            case LLVMPHI            => PHI(ref)
            case LLVMCall           => Call(ref)
            case LLVMSelect         => Select(ref)
            case LLVMUserOp1        => UserOp1(ref)
            case LLVMUserOp2        => UserOp2(ref)
            case LLVMVAArg          => VAArg(ref)
            case LLVMExtractElement => ExtractElement(ref)
            case LLVMInsertElement  => InsertElement(ref)
            case LLVMShuffleVector  => ShuffleVector(ref)
            case LLVMExtractValue   => ExtractValue(ref)
            case LLVMInsertValue    => InsertValue(ref)
            case LLVMFreeze         => Freeze(ref)
            case LLVMFence          => Fence(ref)
            case LLVMAtomicCmpXchg  => AtomicCmpXchg(ref)
            case LLVMAtomicRMW      => AtomicRMW(ref)
            case LLVMResume         => Resume(ref)
            case LLVMLandingPad     => LandingPad(ref)
            case LLVMCleanupRet     => CleanupRet(ref)
            case LLVMCatchRet       => CatchRet(ref)
            case LLVMCatchPad       => CatchPad(ref)
            case LLVMCleanupPad     => CleanupPad(ref)
            case LLVMCatchSwitch    => CatchSwitch(ref)
            case opCode             => throw new IllegalArgumentException("unknown instruction opcode: "+opCode)
        }
    }
}

trait Terminator {
    val ref: LLVMValueRef
    def numSuccessors: Int = LLVMGetNumSuccessors(ref)
    def hasSuccessors: Boolean = numSuccessors > 0
    def getSuccessor(i: Int) = BasicBlock(LLVMGetSuccessor(ref, i))
    def foreachSuccessor(f: BasicBlock => Unit): Unit =
        (0 to numSuccessors - 1).foreach(i => f(getSuccessor(i)))
    def successors: Seq[Instruction] =
        (0 to numSuccessors - 1).map(i => getSuccessor(i).firstInstruction)
}

sealed abstract class Instruction(ref: LLVMValueRef) extends User(ref) {
    def isTerminator: Boolean = LLVMIsATerminatorInst(ref) != null
    def parent: BasicBlock = BasicBlock(LLVMGetInstructionParent(ref))
    def function: Function = parent.parent
    def next: Option[Instruction] = OptionalInstruction(LLVMGetNextInstruction(ref))
    def previous: Option[Instruction] = OptionalInstruction(LLVMGetPreviousInstruction(ref))

    override def toString: String = {
        s"${this.getClass.getSimpleName}(${repr})"
    }
}

case class Ret(ref: LLVMValueRef) extends Instruction(ref) with Terminator {
    def value: Value = operand(0)
}
case class Br(ref: LLVMValueRef) extends Instruction(ref) with Terminator
case class Switch(ref: LLVMValueRef) extends Instruction(ref) with Terminator
case class IndirectBr(ref: LLVMValueRef) extends Instruction(ref) with Terminator
case class Invoke(ref: LLVMValueRef) extends Instruction(ref) with Terminator
case class Unreachable(ref: LLVMValueRef) extends Instruction(ref) with Terminator
case class CallBr(ref: LLVMValueRef) extends Instruction(ref) with Terminator
case class FNeg(ref: LLVMValueRef) extends Instruction(ref)
case class Add(ref: LLVMValueRef) extends Instruction(ref) {
    def op1: Value = operand(0)
    def op2: Value = operand(1)
}
case class FAdd(ref: LLVMValueRef) extends Instruction(ref)
case class Sub(ref: LLVMValueRef) extends Instruction(ref) {
    def op1: Value = operand(0)
    def op2: Value = operand(1)
}
case class FSub(ref: LLVMValueRef) extends Instruction(ref)
case class Mul(ref: LLVMValueRef) extends Instruction(ref)
case class FMul(ref: LLVMValueRef) extends Instruction(ref)
case class UDiv(ref: LLVMValueRef) extends Instruction(ref)
case class SDiv(ref: LLVMValueRef) extends Instruction(ref)
case class FDiv(ref: LLVMValueRef) extends Instruction(ref)
case class URem(ref: LLVMValueRef) extends Instruction(ref)
case class SRem(ref: LLVMValueRef) extends Instruction(ref)
case class FRem(ref: LLVMValueRef) extends Instruction(ref)
case class Shl(ref: LLVMValueRef) extends Instruction(ref)
case class LShr(ref: LLVMValueRef) extends Instruction(ref)
case class AShr(ref: LLVMValueRef) extends Instruction(ref)
case class And(ref: LLVMValueRef) extends Instruction(ref)
case class Or(ref: LLVMValueRef) extends Instruction(ref)
case class Xor(ref: LLVMValueRef) extends Instruction(ref)
case class Alloca(ref: LLVMValueRef) extends Instruction(ref) {
    def allocatedType: Type = Type(LLVMGetAllocatedType(ref))
}
case class Load(ref: LLVMValueRef) extends Instruction(ref) {
    def src: Value = operand(0)
}
case class Store(ref: LLVMValueRef) extends Instruction(ref) {
    def src: Value = operand(0)
    def dst: Value = operand(1)
}
case class GetElementPtr(ref: LLVMValueRef) extends Instruction(ref) {
    def base: Value = operand(0)
    def isConstant = (1 until numOperands).forall(operand(_).isInstanceOf[ConstantIntValue])
    def constants = (1 until numOperands).map(operand(_).asInstanceOf[ConstantIntValue].signExtendedValue)
    def isZero = isConstant && constants.forall(_ == 0)

    def numIndices: Int = LLVMGetNumIndices(ref)
    def indices: Iterable[Int] = LLVMGetIndices(ref).asBuffer().array()
}
case class Trunc(ref: LLVMValueRef) extends Instruction(ref)
case class ZExt(ref: LLVMValueRef) extends Instruction(ref)
case class SExt(ref: LLVMValueRef) extends Instruction(ref)
case class FPToUI(ref: LLVMValueRef) extends Instruction(ref)
case class FPToSI(ref: LLVMValueRef) extends Instruction(ref)
case class UIToFP(ref: LLVMValueRef) extends Instruction(ref)
case class SIToFP(ref: LLVMValueRef) extends Instruction(ref)
case class FPTrunc(ref: LLVMValueRef) extends Instruction(ref)
case class FPExt(ref: LLVMValueRef) extends Instruction(ref)
case class PtrToInt(ref: LLVMValueRef) extends Instruction(ref)
case class IntToPtr(ref: LLVMValueRef) extends Instruction(ref)
case class BitCast(ref: LLVMValueRef) extends Instruction(ref)
case class AddrSpaceCast(ref: LLVMValueRef) extends Instruction(ref)
case class ICmp(ref: LLVMValueRef) extends Instruction(ref)
case class FCmp(ref: LLVMValueRef) extends Instruction(ref)
case class PHI(ref: LLVMValueRef) extends Instruction(ref)
case class Call(ref: LLVMValueRef) extends Instruction(ref) {
    def calledValue: Value = Value(LLVMGetCalledValue(ref)).get // corresponds to last operand
    def calledFunctionType: FunctionType = Type(LLVMGetCalledFunctionType(ref)).asInstanceOf[FunctionType]
    def indexOfArgument(argument: Value): Option[Int] = {
        for (i <- 0 to numOperands)
            if (operand(i) == argument) return Some(i)
        None
    }
    def numArgOperands: Int = LLVMGetNumArgOperands(ref)
}
case class Select(ref: LLVMValueRef) extends Instruction(ref)
case class UserOp1(ref: LLVMValueRef) extends Instruction(ref)
case class UserOp2(ref: LLVMValueRef) extends Instruction(ref)
case class VAArg(ref: LLVMValueRef) extends Instruction(ref)
case class ExtractElement(ref: LLVMValueRef) extends Instruction(ref)
case class InsertElement(ref: LLVMValueRef) extends Instruction(ref)
case class ShuffleVector(ref: LLVMValueRef) extends Instruction(ref)
case class ExtractValue(ref: LLVMValueRef) extends Instruction(ref)
case class InsertValue(ref: LLVMValueRef) extends Instruction(ref)
case class Freeze(ref: LLVMValueRef) extends Instruction(ref)
case class Fence(ref: LLVMValueRef) extends Instruction(ref)
case class AtomicCmpXchg(ref: LLVMValueRef) extends Instruction(ref)
case class AtomicRMW(ref: LLVMValueRef) extends Instruction(ref)
case class Resume(ref: LLVMValueRef) extends Instruction(ref) with Terminator
case class LandingPad(ref: LLVMValueRef) extends Instruction(ref)
case class CleanupRet(ref: LLVMValueRef) extends Instruction(ref) with Terminator
case class CatchRet(ref: LLVMValueRef) extends Instruction(ref) with Terminator
case class CatchPad(ref: LLVMValueRef) extends Instruction(ref)
case class CleanupPad(ref: LLVMValueRef) extends Instruction(ref)
case class CatchSwitch(ref: LLVMValueRef) extends Instruction(ref) with Terminator
