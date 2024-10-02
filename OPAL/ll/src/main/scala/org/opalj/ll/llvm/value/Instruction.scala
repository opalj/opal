/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ll
package llvm
package value

import org.opalj.ll.llvm.FunctionType
import org.opalj.ll.llvm.Type
import org.opalj.ll.llvm.value.constant.ConstantIntValue

import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM._

object OptionalInstruction {
    def apply(ref: LLVMValueRef): Option[Instruction] = {
        if (ref == null || ref.isNull) return None
        Some(Instruction(ref))
    }
}

/**
 * This object returns the correct instruction type for a given instruction reference
 *
 * @author Marc Clement
 */
object Instruction {
    def apply(ref: LLVMValueRef): Instruction = {
        assert((ref ne null) && !ref.isNull, "ref may not be null")
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
            case opCode             => throw new IllegalArgumentException("unknown instruction opcode: " + opCode)
        }
    }
}

trait Terminator {

    val ref: LLVMValueRef

    def numSuccessors: Int = LLVMGetNumSuccessors(ref)

    def hasSuccessors: Boolean = numSuccessors > 0

    def getSuccessor(i: Int): BasicBlock = BasicBlock(LLVMGetSuccessor(ref, i))

    def foreachSuccessor(f: BasicBlock => Unit): Unit =
        (0 until numSuccessors).foreach(i => f(getSuccessor(i)))

    def successors: Seq[Instruction] =
        (0 until numSuccessors).map(i => getSuccessor(i).firstInstruction)
}

sealed abstract class Instruction(ref: LLVMValueRef) extends User(ref) {
    lazy val isTerminator: Boolean = LLVMIsATerminatorInst(ref) != null
    lazy val parent: BasicBlock = BasicBlock(LLVMGetInstructionParent(ref))
    lazy val function: Function = parent.parent
    lazy val next: Option[Instruction] = OptionalInstruction(LLVMGetNextInstruction(ref))
    lazy val previous: Option[Instruction] = OptionalInstruction(LLVMGetPreviousInstruction(ref))

    override def toString: String = {
        s"${this.getClass.getSimpleName}($repr)"
    }
}

sealed abstract class BinaryOperation(ref: LLVMValueRef) extends Instruction(ref) {
    def op1: Value = operand(0)
    def op2: Value = operand(1)
}

sealed abstract class ConversionOperation(ref: LLVMValueRef) extends Instruction(ref) {
    def value: Value = operand(0) // value
    def newType: Value = operand(1) // type
}

case class Ret(ref: LLVMValueRef) extends Instruction(ref) with Terminator {
    def value: Option[Value] = if (numOperands == 0 /* return void */ ) None else Some(operand(0))
}
case class Br(ref: LLVMValueRef) extends Instruction(ref) with Terminator
case class Switch(ref: LLVMValueRef) extends Instruction(ref) with Terminator
case class IndirectBr(ref: LLVMValueRef) extends Instruction(ref) with Terminator
case class Invoke(ref: LLVMValueRef) extends Instruction(ref) with Terminator
case class Unreachable(ref: LLVMValueRef) extends Instruction(ref) with Terminator
case class CallBr(ref: LLVMValueRef) extends Instruction(ref) with Terminator
case class FNeg(ref: LLVMValueRef) extends Instruction(ref)
case class Add(ref: LLVMValueRef) extends BinaryOperation(ref)
case class FAdd(ref: LLVMValueRef) extends BinaryOperation(ref)
case class Sub(ref: LLVMValueRef) extends BinaryOperation(ref)
case class FSub(ref: LLVMValueRef) extends BinaryOperation(ref)
case class Mul(ref: LLVMValueRef) extends BinaryOperation(ref)
case class FMul(ref: LLVMValueRef) extends BinaryOperation(ref)
case class UDiv(ref: LLVMValueRef) extends BinaryOperation(ref)
case class SDiv(ref: LLVMValueRef) extends BinaryOperation(ref)
case class FDiv(ref: LLVMValueRef) extends BinaryOperation(ref)
case class URem(ref: LLVMValueRef) extends BinaryOperation(ref)
case class SRem(ref: LLVMValueRef) extends BinaryOperation(ref)
case class FRem(ref: LLVMValueRef) extends BinaryOperation(ref)
case class Shl(ref: LLVMValueRef) extends BinaryOperation(ref)
case class LShr(ref: LLVMValueRef) extends BinaryOperation(ref)
case class AShr(ref: LLVMValueRef) extends BinaryOperation(ref)
case class And(ref: LLVMValueRef) extends BinaryOperation(ref)
case class Or(ref: LLVMValueRef) extends BinaryOperation(ref)
case class Xor(ref: LLVMValueRef) extends BinaryOperation(ref)
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

    def isConstant: Boolean = (1 until numOperands).forall(operand(_).isInstanceOf[ConstantIntValue])

    /**
     * This will throw an exception if isConstant returns false.
     */
    def constants: Seq[Long] = (1 until numOperands).map(operand(_).asInstanceOf[ConstantIntValue].signExtendedValue)

    def isZero: Boolean = isConstant && constants.forall(_ == 0)

    def numIndices: Int = LLVMGetNumIndices(ref)

    def indices: Iterable[Int] = LLVMGetIndices(ref).asBuffer().array()

    def sourceElementType: Type = Type(LLVMGetGEPSourceElementType(ref))

}
case class Trunc(ref: LLVMValueRef) extends ConversionOperation(ref)
case class ZExt(ref: LLVMValueRef) extends ConversionOperation(ref)
case class SExt(ref: LLVMValueRef) extends ConversionOperation(ref)
case class FPToUI(ref: LLVMValueRef) extends ConversionOperation(ref)
case class FPToSI(ref: LLVMValueRef) extends ConversionOperation(ref)
case class UIToFP(ref: LLVMValueRef) extends ConversionOperation(ref)
case class SIToFP(ref: LLVMValueRef) extends ConversionOperation(ref)
case class FPTrunc(ref: LLVMValueRef) extends ConversionOperation(ref)
case class FPExt(ref: LLVMValueRef) extends ConversionOperation(ref)
case class PtrToInt(ref: LLVMValueRef) extends ConversionOperation(ref)
case class IntToPtr(ref: LLVMValueRef) extends ConversionOperation(ref)
case class BitCast(ref: LLVMValueRef) extends ConversionOperation(ref)
case class AddrSpaceCast(ref: LLVMValueRef) extends ConversionOperation(ref)
case class ICmp(ref: LLVMValueRef) extends Instruction(ref)
case class FCmp(ref: LLVMValueRef) extends Instruction(ref)
case class PHI(ref: LLVMValueRef) extends Instruction(ref)
case class Call(ref: LLVMValueRef) extends Instruction(ref) {
    def calledValue: Value = Value(LLVMGetCalledValue(ref)).get // corresponds to last operand
    def calledFunctionType: FunctionType = Type(LLVMGetCalledFunctionType(ref)).asInstanceOf[FunctionType]
    def indexOfArgument(argument: Value): Option[Int] = {
        for (i <- 0 until numOperands)
            if (operand(i) == argument) return Some(i)
        None
    }
    def argument(index: Int): Option[Value] = {
        if (index >= numOperands) None
        else Some(operand(index))
    }
    def numArgOperands: Int = LLVMGetNumArgOperands(ref)
}
case class Select(ref: LLVMValueRef) extends Instruction(ref)
case class UserOp1(ref: LLVMValueRef) extends Instruction(ref)
case class UserOp2(ref: LLVMValueRef) extends Instruction(ref)
case class VAArg(ref: LLVMValueRef) extends Instruction(ref)
case class ExtractElement(ref: LLVMValueRef) extends Instruction(ref) {

    def vec: Value = operand(0)

    def index: Value = operand(1)

    def isConstant: Boolean = index.isInstanceOf[ConstantIntValue]

    def constant: Long = index.asInstanceOf[ConstantIntValue].zeroExtendedValue

}
case class InsertElement(ref: LLVMValueRef) extends Instruction(ref) {

    def vec: Value = operand(0)

    def value: Value = operand(1)

    def index: Value = operand(2)

    def isConstant: Boolean = index.isInstanceOf[ConstantIntValue]

    def constant: Long = index.asInstanceOf[ConstantIntValue].zeroExtendedValue

}
case class ShuffleVector(ref: LLVMValueRef) extends Instruction(ref) {

    def vec1: Value = operand(0)

    def vec2: Value = operand(1)

}
case class ExtractValue(ref: LLVMValueRef) extends Instruction(ref) {

    def aggregVal: Value = operand(0)

    // always constant values
    def constants: Seq[Long] = (1 until numOperands)
        .map(operand(_).asInstanceOf[ConstantIntValue].signExtendedValue)

}
case class InsertValue(ref: LLVMValueRef) extends Instruction(ref) {
    def aggregVal: Value = operand(0)
    def value: Value = operand(1)

    // always constant values
    def constants: Seq[Long] = (2 until numOperands)
        .map(operand(_).asInstanceOf[ConstantIntValue].signExtendedValue)
}
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
