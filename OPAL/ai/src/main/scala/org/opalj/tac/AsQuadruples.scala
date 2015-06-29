/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.opalj
package tac

import scala.collection.mutable.BitSet
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.ArrayBuffer

import org.opalj.collection.mutable.Locals
import org.opalj.bytecode.BytecodeProcessingFailedException
import org.opalj.br._
import org.opalj.br.instructions._
import org.opalj.ai.AIResult

/**
 * Converts the bytecode of a method into a three address/quadruples representation.
 *
 * TODO Complete implementation of "AsQuadruples".
 *
 * @author Michael Eichberg
 * @author Roberts Kolosovs
 */
object AsQuadruples {

  /**
   * Converts the bytecode of a method into a quadruples representation.
   *
   * @param method A method with a body. I.e., a non-native, non-abstract method.
   */
  def apply(method: Method, aiResult: Option[AIResult]): Array[Stmt] = {

    import BinaryArithmeticOperators._
    import UnaryArithmeticOperators._

    val code = method.body.get
    import code.pcOfNextInstruction
    val instructions = code.instructions
    val codeSize = instructions.size
    val processed = new BitSet(codeSize)

    // In a few cases, such as swap and dup instructions, we have to
    // create multiple three-address instructions. However, in this case
    // we have to make sure that jump targets are still pointing to the right
    // instructions. Hence, we have to make sure that all statements created
    // for one instruction are mapped to the same pc.
    val statements = new Array[List[Stmt]](codeSize)

    // Calculating the basic block boundaries on demand makes it possible to
    // use some very simple pattern matchers afterwards to perform common
    // code beautifications/optimizations without performing "heavy weight" analyses.
    // TODO val basicBlockBoundaries = new BitSet(codeSize)

    processed(0) = true
    var worklist: List[(PC, Stack)] = List((0, Nil))
    for { exceptionHandler ← code.exceptionHandlers } (
      worklist ::= ((exceptionHandler.handlerPC, List(OperandVar.HandledException))))

    while (worklist.nonEmpty) {
      val (pc, stack) = worklist.head
      val instruction = instructions(pc)
      val opcode = instruction.opcode
      worklist = worklist.tail

      def schedule(nextPC: PC, newStack: Stack): Unit = {
        if (!processed(nextPC)) {
          processed.add(nextPC)
          worklist ::= ((nextPC, newStack))
        }
      }

      def loadInstruction(sourceRegister: UShort, cTpe: ComputationalType): Unit = {
        val operandVar = OperandVar(cTpe, stack)
        val registerVar = RegisterVar(cTpe, sourceRegister)
        statements(pc) = List(Assignment(pc, operandVar, registerVar))
        val newStack = operandVar :: stack
        val nextPC = pcOfNextInstruction(pc)
        schedule(nextPC, newStack)
      }

      def storeInstruction(targetRegister: UShort, cTpe: ComputationalType): Unit = {
        val operandVar = stack.head
        val registerVar = RegisterVar(cTpe, targetRegister)
        statements(pc) = List(Assignment(pc, registerVar, operandVar))
        val newStack = stack.tail
        val nextPC = pcOfNextInstruction(pc)
        schedule(nextPC, newStack)
      }

      // Note:
      // The computational type of the Binary Expression is determined using the
      // first (left) value of the expression. This makes it possible to use
      // this function for bit shift operations where value1 and
      // value2 may have different computational types, but the resulting type
      // is always determined by the type of value1.
      def binaryArithmeticOperation(operator: BinaryArithmeticOperator): Unit = {
        val value2 :: value1 :: _ = stack
        val cTpe = value1.cTpe
        statements(pc) = List(
          Assignment(pc, value1, BinaryExpr(pc, cTpe, operator, value1, value2)))
        schedule(pcOfNextInstruction(pc), stack.tail)
      }

      def prefixArithmeticOperation(operator: UnaryArithmeticOperator): Unit = {
        val value :: _ = stack
        val cTpe = value.cTpe
        statements(pc) = List(
          Assignment(pc, value, PrefixExpr(pc, cTpe, operator, value)))
        schedule(pcOfNextInstruction(pc), stack)
      }

      def castOperation(trgtTpe: BaseType): Unit = {
        val value :: rest = stack
        val result = OperandVar(trgtTpe.computationalType, stack)
        statements(pc) = List(
          Assignment(pc, result, PrimitiveTypecastExpr(pc, trgtTpe, value)))
        schedule(pcOfNextInstruction(pc), result :: rest)
      }

      def returnInstruction(fallback: SimpleVar): Unit = {
        val returnedValue =
          aiResult.flatMap { r ⇒
            // We have to be able to handle the case that the operands
            // array is empty (i.e., the instruction is dead)
            Option(r.operandsArray(pc)).map(ops ⇒ DomainValueBasedVar(0, ops.head))
          }.getOrElse(fallback)
        statements(pc) = List(ReturnValue(pc, returnedValue))
      }

      def as[T <: Instruction](i: Instruction): T = i.asInstanceOf[T]

      (opcode: @scala.annotation.switch) match {
        case ALOAD_0.opcode ⇒ loadInstruction(0, ComputationalTypeReference)
        case ALOAD_1.opcode ⇒ loadInstruction(1, ComputationalTypeReference)
        case ALOAD_2.opcode ⇒ loadInstruction(2, ComputationalTypeReference)
        case ALOAD_3.opcode ⇒ loadInstruction(3, ComputationalTypeReference)
        case ALOAD.opcode ⇒
          loadInstruction(as[ALOAD](instruction).lvIndex, ComputationalTypeReference)

        case ASTORE_0.opcode ⇒ storeInstruction(0, ComputationalTypeReference)
        case ASTORE_1.opcode ⇒ storeInstruction(1, ComputationalTypeReference)
        case ASTORE_2.opcode ⇒ storeInstruction(2, ComputationalTypeReference)
        case ASTORE_3.opcode ⇒ storeInstruction(3, ComputationalTypeReference)
        case ASTORE.opcode ⇒
          storeInstruction(as[ASTORE](instruction).lvIndex, ComputationalTypeReference)

        case ILOAD_0.opcode ⇒ loadInstruction(0, ComputationalTypeInt)
        case ILOAD_1.opcode ⇒ loadInstruction(1, ComputationalTypeInt)
        case ILOAD_2.opcode ⇒ loadInstruction(2, ComputationalTypeInt)
        case ILOAD_3.opcode ⇒ loadInstruction(3, ComputationalTypeInt)
        case ILOAD.opcode ⇒
          loadInstruction(as[ILOAD](instruction).lvIndex, ComputationalTypeInt)

        case ISTORE_0.opcode ⇒ storeInstruction(0, ComputationalTypeInt)
        case ISTORE_1.opcode ⇒ storeInstruction(1, ComputationalTypeInt)
        case ISTORE_2.opcode ⇒ storeInstruction(2, ComputationalTypeInt)
        case ISTORE_3.opcode ⇒ storeInstruction(3, ComputationalTypeInt)
        case ISTORE.opcode ⇒
          storeInstruction(as[ISTORE](instruction).lvIndex, ComputationalTypeInt)

        case DLOAD_0.opcode ⇒ loadInstruction(0, ComputationalTypeDouble)
        case DLOAD_1.opcode ⇒ loadInstruction(1, ComputationalTypeDouble)
        case DLOAD_2.opcode ⇒ loadInstruction(2, ComputationalTypeDouble)
        case DLOAD_3.opcode ⇒ loadInstruction(3, ComputationalTypeDouble)
        case DLOAD.opcode ⇒
          loadInstruction(as[DLOAD](instruction).lvIndex, ComputationalTypeDouble)

        case DSTORE_0.opcode ⇒ storeInstruction(0, ComputationalTypeDouble)
        case DSTORE_1.opcode ⇒ storeInstruction(1, ComputationalTypeDouble)
        case DSTORE_2.opcode ⇒ storeInstruction(2, ComputationalTypeDouble)
        case DSTORE_3.opcode ⇒ storeInstruction(3, ComputationalTypeDouble)
        case DSTORE.opcode ⇒
          storeInstruction(as[DSTORE](instruction).lvIndex, ComputationalTypeDouble)

        case FLOAD_0.opcode ⇒ loadInstruction(0, ComputationalTypeFloat)
        case FLOAD_1.opcode ⇒ loadInstruction(1, ComputationalTypeFloat)
        case FLOAD_2.opcode ⇒ loadInstruction(2, ComputationalTypeFloat)
        case FLOAD_3.opcode ⇒ loadInstruction(3, ComputationalTypeFloat)
        case FLOAD.opcode ⇒
          loadInstruction(as[FLOAD](instruction).lvIndex, ComputationalTypeFloat)

        case FSTORE_0.opcode ⇒ storeInstruction(0, ComputationalTypeFloat)
        case FSTORE_1.opcode ⇒ storeInstruction(1, ComputationalTypeFloat)
        case FSTORE_2.opcode ⇒ storeInstruction(2, ComputationalTypeFloat)
        case FSTORE_3.opcode ⇒ storeInstruction(3, ComputationalTypeFloat)
        case FSTORE.opcode ⇒
          storeInstruction(as[FSTORE](instruction).lvIndex, ComputationalTypeFloat)

        case LLOAD_0.opcode ⇒ loadInstruction(0, ComputationalTypeLong)
        case LLOAD_1.opcode ⇒ loadInstruction(1, ComputationalTypeLong)
        case LLOAD_2.opcode ⇒ loadInstruction(2, ComputationalTypeLong)
        case LLOAD_3.opcode ⇒ loadInstruction(3, ComputationalTypeLong)
        case LLOAD.opcode ⇒
          loadInstruction(as[LLOAD](instruction).lvIndex, ComputationalTypeLong)

        case LSTORE_0.opcode ⇒ storeInstruction(0, ComputationalTypeLong)
        case LSTORE_1.opcode ⇒ storeInstruction(1, ComputationalTypeLong)
        case LSTORE_2.opcode ⇒ storeInstruction(2, ComputationalTypeLong)
        case LSTORE_3.opcode ⇒ storeInstruction(3, ComputationalTypeLong)
        case LSTORE.opcode ⇒
          storeInstruction(as[LSTORE](instruction).lvIndex, ComputationalTypeLong)

        case IRETURN.opcode ⇒ returnInstruction(OperandVar.IntReturnValue)
        case LRETURN.opcode ⇒ returnInstruction(OperandVar.LongReturnValue)
        case FRETURN.opcode ⇒ returnInstruction(OperandVar.FloatReturnValue)
        case DRETURN.opcode ⇒ returnInstruction(OperandVar.DoubleReturnValue)
        case ARETURN.opcode ⇒ returnInstruction(OperandVar.ReferenceReturnValue)
        case RETURN.opcode ⇒
          statements(pc) = List(Return(pc))

        case IF_ICMPEQ.opcode | IF_ICMPNE.opcode |
          IF_ICMPLT.opcode | IF_ICMPLE.opcode |
          IF_ICMPGT.opcode | IF_ICMPGE.opcode ⇒
          val ifInstr = as[IFICMPInstruction](instruction)
          val value2 :: value1 :: rest = stack
          // let's calculate the final address
          val targetPC = pc + ifInstr.branchoffset
          val stmt = If(pc, value1, ifInstr.condition, value2, targetPC)
          statements(pc) = List(stmt)
          schedule(pcOfNextInstruction(pc), rest)
          schedule(targetPC, rest)

        case IFEQ.opcode | IFNE.opcode |
          IFLT.opcode | IFLE.opcode |
          IFGT.opcode | IFGE.opcode ⇒
          val ifInstr = as[IF0Instruction](instruction)
          val value :: rest = stack
          // let's calculate the final address
          val targetPC = pc + ifInstr.branchoffset
          val stmt = If(pc, value, ifInstr.condition, IntConst(-pc, 0), targetPC)
          statements(pc) = List(stmt)
          schedule(pcOfNextInstruction(pc), rest)
          schedule(targetPC, rest)

        case IF_ACMPEQ.opcode | IF_ACMPNE.opcode ⇒
          val ifInstr = as[IFACMPInstruction](instruction)
          val value2 :: value1 :: rest = stack
          // let's calculate the final address
          val targetPC = pc + ifInstr.branchoffset
          val stmt = If(pc, value1, ifInstr.condition, value2, targetPC)
          statements(pc) = List(stmt)
          schedule(pcOfNextInstruction(pc), rest)
          schedule(targetPC, rest)

        case IFNONNULL.opcode | IFNULL.opcode ⇒
          val ifInstr = as[IFXNullInstruction](instruction)
          val value :: rest = stack
          // let's calculate the final address
          val targetPC = pc + ifInstr.branchoffset
          val stmt = If(pc, value, ifInstr.condition, NullExpr(-pc), targetPC)
          statements(pc) = List(stmt)
          schedule(pcOfNextInstruction(pc), rest)
          schedule(targetPC, rest)

        case SWAP.opcode ⇒
          val value2 :: value1 :: rest = stack
          val tempVar = TempVar(value2.cTpe)
          val newValue2 = value2.updated(value1.cTpe)
          val newValue1 = value1.updated(value2.cTpe)
          statements(pc) = List(
            Assignment(pc, tempVar, value2),
            Assignment(pc, newValue2, value1),
            Assignment(pc, newValue1, tempVar))
          schedule(pcOfNextInstruction(pc), newValue2 :: newValue1 :: rest)

        case DADD.opcode | FADD.opcode |
          IADD.opcode | LADD.opcode ⇒ binaryArithmeticOperation(Add)
        case DDIV.opcode | FDIV.opcode |
          IDIV.opcode | LDIV.opcode ⇒ binaryArithmeticOperation(Divide)

        //                case DCMPG.opcode | DCMPL.opcode |
        //                    FCMPG.opcode | FCMPL.opcode ⇒
        //                    val value2 :: value1 :: rest = stack
        //                    val result = OperandVar(ComputationalTypeInt, stack)
        //                    val nanCompRes = {
        //                        if (instruction.opcode == DCMPG.opcode | instruction.opcode == FCMPG.opcode) IntConst(pc, 1)
        //                        else IntConst(pc, -1)
        //                    }
        //                        //TODO sort out the program counters
        //                    statements(pc) = List(
        //                        If(pc, value1, NE, DoubleConst(pc, Double.NaN), pc),
        //                        Assignment(pc, result, nanCompRes),
        //                        Goto(pc, pc),
        //                        If(pc, value2, NE, DoubleConst(pc, Double.NaN), pc),
        //                        Assignment(pc, result, nanCompRes),
        //                        Goto(pc, pc),
        //                        If(pc, value1, LE, value2, pc),
        //                        Assignment(pc, result, IntConst(pc, 1)),
        //                        Goto(pc, pc),
        //                        If(pc, value1, NE, value2, pc),
        //                        Assignment(pc, result, IntConst(pc, 0)),
        //                        Goto(pc, pc),
        //                        Assignment(pc, result, IntConst(pc, -1))
        //                    )
        //                    schedule(pcOfNextInstruction(pc), result :: rest)

        case DNEG.opcode | FNEG.opcode |
          INEG.opcode | LNEG.opcode ⇒ prefixArithmeticOperation(Negate)
        case DMUL.opcode | FMUL.opcode |
          IMUL.opcode | LMUL.opcode ⇒ binaryArithmeticOperation(Multiply)
        case DREM.opcode | FREM.opcode |
          IREM.opcode | LREM.opcode ⇒ binaryArithmeticOperation(Modulo)
        case DSUB.opcode | FSUB.opcode |
          ISUB.opcode | LSUB.opcode ⇒ binaryArithmeticOperation(Subtract)

        case IINC.opcode ⇒
          val IINC(index, const) = instruction
          val indexReg = RegisterVar(ComputationalTypeInt, index)
          statements(pc) = List(
            Assignment(pc, indexReg,
              BinaryExpr(pc, ComputationalTypeInt, Add, indexReg, IntConst(pc, const))))
          schedule(pcOfNextInstruction(pc), stack)

        case IAND.opcode | LAND.opcode ⇒ binaryArithmeticOperation(And)
        case IOR.opcode | LOR.opcode ⇒ binaryArithmeticOperation(Or)
        case ISHL.opcode | LSHL.opcode ⇒ binaryArithmeticOperation(ShiftLeft)
        case ISHR.opcode | LSHR.opcode ⇒ binaryArithmeticOperation(ShiftRight)
        case IUSHR.opcode | LUSHR.opcode ⇒ binaryArithmeticOperation(UnsignedShiftRight)
        case IXOR.opcode | LXOR.opcode ⇒ binaryArithmeticOperation(XOr)

        case ICONST_0.opcode | ICONST_1.opcode |
          ICONST_2.opcode | ICONST_3.opcode |
          ICONST_4.opcode | ICONST_5.opcode |
          ICONST_M1.opcode ⇒
          val value = as[LoadConstantInstruction[Int]](instruction).value
          val targetVar = OperandVar(ComputationalTypeInt, stack)
          statements(pc) = List(Assignment(pc, targetVar, IntConst(pc, value)))
          schedule(pcOfNextInstruction(pc), targetVar :: stack)

        case ACONST_NULL.opcode ⇒
          val targetVar = OperandVar(ComputationalTypeReference, stack)
          statements(pc) = List(Assignment(pc, targetVar, NullExpr(pc)))
          schedule(pcOfNextInstruction(pc), targetVar :: stack)

        case DCONST_0.opcode | DCONST_1.opcode ⇒
          val value = as[LoadConstantInstruction[Double]](instruction).value
          val targetVar = OperandVar(ComputationalTypeDouble, stack)
          statements(pc) = List(Assignment(pc, targetVar, DoubleConst(pc, value)))
          schedule(pcOfNextInstruction(pc), targetVar :: stack)

        case FCONST_0.opcode | FCONST_1.opcode |
          FCONST_2.opcode ⇒
          val value = as[LoadConstantInstruction[Float]](instruction).value
          val targetVar = OperandVar(ComputationalTypeFloat, stack)
          statements(pc) = List(Assignment(pc, targetVar, FloatConst(pc, value)))
          schedule(pcOfNextInstruction(pc), targetVar :: stack)

        case LCONST_0.opcode | LCONST_1.opcode ⇒
          val value = as[LoadConstantInstruction[Long]](instruction).value
          val targetVar = OperandVar(ComputationalTypeLong, stack)
          statements(pc) = List(Assignment(pc, targetVar, LongConst(pc, value)))
          schedule(pcOfNextInstruction(pc), targetVar :: stack)

        case GOTO.opcode | GOTO_W.opcode ⇒
          statements(pc) = List(Goto(pc, pc + as[GOTO](instruction).branchoffset))
          schedule(pcOfNextInstruction(pc), stack)

        case NOP.opcode ⇒
          statements(pc) = List(Nop(pc))
          schedule(pcOfNextInstruction(pc), stack)

        case POP.opcode ⇒
          val _ :: rest = stack
          statements(pc) = List(EmptyStmt(pc))
          schedule(pcOfNextInstruction(pc), rest)

        case POP2.opcode ⇒
          stack match {
            case (val1 @ CTC1()) :: val2 :: rest ⇒
              statements(pc) = List(EmptyStmt(pc))
              schedule(pcOfNextInstruction(pc), rest)
            case _ :: rest ⇒
              statements(pc) = List(EmptyStmt(pc))
              schedule(pcOfNextInstruction(pc), rest)
          }

        case INSTANCEOF.opcode ⇒
          val value1 :: rest = stack
          val resultVar = OperandVar(ComputationalTypeInt, stack)
          statements(pc) = List(
            Assignment(pc, resultVar, InstanceOf(value1, as[INSTANCEOF](instruction).referenceType)))
          schedule(pcOfNextInstruction(pc), resultVar :: rest)

        case CHECKCAST.opcode ⇒
          val value1 :: _ = stack
          statements(pc) = List(
            Checkcast(pc, value1, as[CHECKCAST](instruction).referenceType))
          schedule(pcOfNextInstruction(pc), stack)

        case MONITORENTER.opcode ⇒
          val objRef :: rest = stack
          statements(pc) = List(MonitorEnter(pc, objRef))
          schedule(pcOfNextInstruction(pc), rest)

        case MONITOREXIT.opcode ⇒
          val objRef :: rest = stack
          statements(pc) = List(MonitorExit(pc, objRef))
          schedule(pcOfNextInstruction(pc), rest)

        case TABLESWITCH.opcode ⇒
          val index :: rest = stack
          val tsInst = as[TABLESWITCH](instruction)
          val defaultTarget = pc + tsInst.defaultOffset
          val jumpOffsets = tsInst.jumpOffsets.seq
          val npairs = 
            for{i <- tsInst.low to tsInst.high+1} yield (i, jumpOffsets(i) + pc)
          statements(pc) = List(
            Switch(pc, defaultTarget, index, npairs))
          schedule(pcOfNextInstruction(pc), rest)
          for(target <- npairs){
            schedule(target._2, rest)
          }

        case LOOKUPSWITCH.opcode ⇒
          val index :: rest = stack
          val tsInst = as[LOOKUPSWITCH](instruction)
          val defaultTarget = pc + tsInst.defaultOffset
          val npairs = tsInst.npairs.map{ x ⇒ (x._1, x._2 + pc) }
          statements(pc) = List(
            Switch(pc, defaultTarget, index, npairs))
          schedule(pcOfNextInstruction(pc), rest)
          for(target <- npairs){
            schedule(target._2, rest)
          }

        case DUP.opcode ⇒
          val head :: _ = stack
          statements(pc) = List(EmptyStmt(pc))
          schedule(pcOfNextInstruction(pc), head :: stack)

        case DUP_X1.opcode ⇒
          val val1 :: val2 :: rest = stack
          statements(pc) = List(EmptyStmt(pc))
          schedule(pcOfNextInstruction(pc), val1 :: val2 :: val1 :: rest)

        case DUP_X2.opcode ⇒
          stack match {
            case v1 :: (v2 @ CTC1()) :: v3 :: rest ⇒
              statements(pc) = List(EmptyStmt(pc))
              schedule(pcOfNextInstruction(pc), v1 :: v2 :: v3 :: v1 :: rest)
            case v1 :: v2 :: rest ⇒
              statements(pc) = List(EmptyStmt(pc))
              schedule(pcOfNextInstruction(pc), v1 :: v2 :: v1 :: rest)
          }

        case DUP2.opcode ⇒
          stack match {
            case (v1 @ CTC1()) :: v2 :: rest ⇒
              statements(pc) = List(EmptyStmt(pc))
              schedule(pcOfNextInstruction(pc), v1 :: v2 :: v1 :: v2 :: rest)
            case v1 :: rest ⇒
              statements(pc) = List(EmptyStmt(pc))
              schedule(pcOfNextInstruction(pc), v1 :: v1 :: rest)
          }

        case DUP2_X1.opcode ⇒
          stack match {
            case (v1 @ CTC1()) :: v2 :: v3 :: rest ⇒
              statements(pc) = List(EmptyStmt(pc))
              schedule(pcOfNextInstruction(pc), v1 :: v2 :: v3 :: v1 :: v2 :: rest)
            case v1 :: v2 :: rest ⇒
              statements(pc) = List(EmptyStmt(pc))
              schedule(pcOfNextInstruction(pc), v1 :: v2 :: v1 :: rest)
          }

        case DUP2_X2.opcode ⇒
          stack match {
            case (v1 @ CTC1()) :: (v2 @ CTC1()) :: (v3 @ CTC1()) :: (v4 /*@ CTC1()*/ ) :: rest ⇒
              statements(pc) = List(EmptyStmt(pc))
              schedule(pcOfNextInstruction(pc), v1 :: v2 :: v3 :: v4 :: v1 :: v2 :: rest)
            case (v1 @ CTC2()) :: (v2 @ CTC1()) :: (v3 @ CTC1()) :: rest ⇒
              statements(pc) = List(EmptyStmt(pc))
              schedule(pcOfNextInstruction(pc), v1 :: v2 :: v3 :: v1 :: rest)
            case (v1 @ CTC1()) :: (v2 @ CTC1()) :: (v3 @ CTC2()) :: rest ⇒
              statements(pc) = List(EmptyStmt(pc))
              schedule(pcOfNextInstruction(pc), v1 :: v2 :: v3 :: v1 :: v2 :: rest)
            case (v1 /*@ CTC2()*/ ) :: (v2 /*@ CTC1()*/ ) :: rest ⇒
              statements(pc) = List(EmptyStmt(pc))
              schedule(pcOfNextInstruction(pc), v1 :: v2 :: v1 :: rest)
          }

        case D2F.opcode | I2F.opcode | L2F.opcode ⇒ castOperation(FloatType)
        case D2I.opcode | F2I.opcode | L2I.opcode ⇒ castOperation(IntegerType)
        case D2L.opcode | I2L.opcode | F2L.opcode ⇒ castOperation(LongType)
        case F2D.opcode | I2D.opcode | L2D.opcode ⇒ castOperation(DoubleType)
        case I2C.opcode ⇒ castOperation(CharType)
        case I2B.opcode ⇒ castOperation(ByteType)
        case I2S.opcode ⇒ castOperation(ShortType)

        case WIDE.opcode ⇒
          statements(pc) = List(EmptyStmt(pc))
          schedule(pcOfNextInstruction(pc), stack)

        // TODO Add support for all the other instructions!

        case opcode ⇒
          throw BytecodeProcessingFailedException(s"unknown opcode: $opcode")
      }
    }

    var index = 0
    val finalStatements = new ArrayBuffer[Stmt](codeSize)
    // Now we have to remap the target pcs to create the final statements array
    // however, before we can do that we first add the register initialization
    // statements

    var registerIndex = 0
    if (!method.isStatic) {
      val targetVar = RegisterVar(ComputationalTypeReference, 0)
      val sourceParam = Param(ComputationalTypeReference, "this")
      finalStatements += Assignment(-1, targetVar, sourceParam)
      index += 1
      registerIndex += 1
    }
    method.descriptor.parameterTypes.foreach { parameterType ⇒
      // TODO use debug information to get better names...
      val cTpe = parameterType.computationalType
      val targetVar = RegisterVar(cTpe, registerIndex)
      val sourceParam = Param(cTpe, "p_" + index)
      finalStatements += Assignment(-1, targetVar, sourceParam)
      index += 1
      registerIndex += cTpe.category
    }

    var currentPC = 0
    val pcToIndex = new Array[Int](codeSize)
    while (currentPC < codeSize) {
      val currentStatements = statements(currentPC)
      if (currentStatements ne null) {
        for (stmt ← currentStatements) {
          finalStatements += stmt
          if (pcToIndex(currentPC) == 0 /*the mapping is not yet set; we don't care about the remapping of 0 to 0...*/ )
            pcToIndex(currentPC) = index
          index += 1
        }
      }
      currentPC += 1
    }
    finalStatements.foreach(_.remapIndexes(pcToIndex))
    finalStatements.toArray
  }

}

/**
 * Facilitates matching against values of computational type category 1.
 *
 * @example
 * {{{
 * case v @ CTC1() => ...
 * }}}
 */
object CTC1 {
  def unapply(value: Var): Boolean =
    value.cTpe.category == 1
}

/**
 * Facilitates matching against values of computational type category 2.
 *
 * @example
 * {{{
 * case v @ CTC2() => ...
 * }}}
 */
object CTC2 {
  def unapply(value: Var): Boolean =
    value.cTpe.category == 2
}

