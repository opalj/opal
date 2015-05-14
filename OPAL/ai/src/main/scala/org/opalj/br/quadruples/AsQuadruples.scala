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
package org.opalj.br
package quadruples

import org.opalj.UShort
import org.opalj.collection.mutable.Locals
import scala.collection.mutable.BitSet
import org.opalj.bytecode.BytecodeProcessingFailedException
import org.opalj.br.instructions._
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.ArrayBuffer
import com.sun.org.apache.bcel.internal.generic.ICONST
import org.opalj.ai.AIResult

/**
 * Converts the bytecode of a method into a three address/quadruples representation.
 *
 * @author Michael Eichberg
 */
object AsQuadruples {

    /**
     * Converts the bytecode of a method into a quadruples representation.
     *
     * @param method A method with a body. I.e., a non-native, non-abstract method.
     */
    def apply(method: Method, aiResult: Option[AIResult]): Array[Stmt] = {

        import ArithmeticOperators._

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
            worklist ::= ((exceptionHandler.handlerPC, List(OperandVar.HandledException)))
        )

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

            def arithmeticOperation(operator: ArithmeticOperator): Unit = {
                val value2 :: value1 :: _ = stack
                val cTpe = value2 /*or value 1*/ .cTpe
                statements(pc) = List(
                    Assignment(pc, value1, ArithExpr(pc, cTpe, operator, value1, value2))
                )
                schedule(pcOfNextInstruction(pc), stack.tail)
            }

            def unaryArithmeticOperation(operator: ArithmeticOperator): Unit = {
                val value :: _ = stack
                val cTpe = value.cTpe
                statements(pc) = List(
                    Assignment(pc, value, UnaryExpr(pc, cTpe, operator, value))
                )
                schedule(pcOfNextInstruction(pc), stack.tail)
            }

            def as[T <: Instruction](i: Instruction): T = i.asInstanceOf[T]

            (opcode: @scala.annotation.switch) match {
                case ALOAD_0.opcode ⇒ loadInstruction(0, ComputationalTypeReference)
                case ALOAD_1.opcode ⇒ loadInstruction(1, ComputationalTypeReference)
                case ALOAD_2.opcode ⇒ loadInstruction(2, ComputationalTypeReference)
                case ALOAD_3.opcode ⇒ loadInstruction(3, ComputationalTypeReference)
                case ALOAD.opcode ⇒
                    loadInstruction(as[ALOAD](instruction).lvIndex, ComputationalTypeReference)

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

                case IRETURN.opcode ⇒
                    val returnedValue =
                        aiResult.flatMap { r ⇒
                            // We have to be able to handle the case that the operands
                            // array is empty (i.e., the instruction is dead)
                            Option(r.operandsArray(pc)).map(ops ⇒ DomainValueBasedVar(0, ops.head))
                        }.getOrElse(OperandVar.IntReturnValue)
                    statements(pc) = List(ReturnValue(pc, returnedValue))
                case LRETURN.opcode ⇒
                    statements(pc) = List(ReturnValue(pc, OperandVar.LongReturnValue))
                case FRETURN.opcode ⇒
                    statements(pc) = List(ReturnValue(pc, OperandVar.FloatReturnValue))
                case DRETURN.opcode ⇒
                    statements(pc) = List(ReturnValue(pc, OperandVar.DoubleReturnValue))
                case ARETURN.opcode ⇒
                    statements(pc) = List(ReturnValue(pc, OperandVar.ReferenceReturnValue))
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

                case SWAP.opcode ⇒
                    val value2 :: value1 :: rest = stack
                    val tempVar = TempVar(value2.cTpe)
                    val newValue2 = value2.updated(value1.cTpe)
                    val newValue1 = value1.updated(value2.cTpe)
                    statements(pc) = List(
                        Assignment(pc, tempVar, value2),
                        Assignment(pc, newValue2, value1),
                        Assignment(pc, newValue1, tempVar)
                    )
                    schedule(pcOfNextInstruction(pc), newValue2 :: newValue1 :: rest)

                case DADD.opcode  ⇒ arithmeticOperation(Add)
                case DDIV.opcode  ⇒ arithmeticOperation(Divide)
                case DCMPG.opcode ⇒ arithmeticOperation(Greater)
                case DCMPL.opcode ⇒ arithmeticOperation(Greater)
                case DNEG.opcode  ⇒ unaryArithmeticOperation(Subtract)
                case DMUL.opcode  ⇒ arithmeticOperation(Multiply)
                case DREM.opcode  ⇒ arithmeticOperation(Modulo)
                case DSUB.opcode  ⇒ arithmeticOperation(Subtract)

                case FADD.opcode  ⇒ arithmeticOperation(Add)
                case FDIV.opcode  ⇒ arithmeticOperation(Divide)
                case FCMPG.opcode ⇒ arithmeticOperation(Greater)
                case FCMPL.opcode ⇒ arithmeticOperation(Greater)
                case FNEG.opcode  ⇒ unaryArithmeticOperation(Subtract)
                case FMUL.opcode  ⇒ arithmeticOperation(Multiply)
                case FREM.opcode  ⇒ arithmeticOperation(Modulo)
                case FSUB.opcode  ⇒ arithmeticOperation(Subtract)

                case IADD.opcode  ⇒ arithmeticOperation(Add)
                case IAND.opcode  ⇒ arithmeticOperation(And)
                case IDIV.opcode  ⇒ arithmeticOperation(Divide)
                //                case IINC.opcode ⇒ arithmeticOperation(Increment) /*unary, doesn't use stack*/
                case INEG.opcode  ⇒ unaryArithmeticOperation(Subtract)
                case IMUL.opcode  ⇒ arithmeticOperation(Multiply)
                case IOR.opcode   ⇒ arithmeticOperation(Or)
                case IREM.opcode  ⇒ arithmeticOperation(Modulo)
                case ISHL.opcode  ⇒ arithmeticOperation(LShift)
                case ISHR.opcode  ⇒ arithmeticOperation(RShift)
                case ISUB.opcode  ⇒ arithmeticOperation(Subtract)
                case IUSHR.opcode ⇒ arithmeticOperation(RShiftLog)
                case IXOR.opcode  ⇒ arithmeticOperation(XOr)

                case LADD.opcode  ⇒ arithmeticOperation(Add)
                case LAND.opcode  ⇒ arithmeticOperation(And)
                case LDIV.opcode  ⇒ arithmeticOperation(Divide)
                case LCMP.opcode  ⇒ arithmeticOperation(Greater)
                case LNEG.opcode  ⇒ unaryArithmeticOperation(Subtract)
                case LMUL.opcode  ⇒ arithmeticOperation(Multiply)
                case LOR.opcode   ⇒ arithmeticOperation(Or)
                case LREM.opcode  ⇒ arithmeticOperation(Modulo)
                case LSHL.opcode  ⇒ arithmeticOperation(LShift)
                case LSHR.opcode  ⇒ arithmeticOperation(RShift)
                case LSUB.opcode  ⇒ arithmeticOperation(Subtract)
                case LUSHR.opcode ⇒ arithmeticOperation(RShiftLog)
                case LXOR.opcode  ⇒ arithmeticOperation(XOr)

                case ICONST_0.opcode | ICONST_1.opcode |
                    ICONST_2.opcode | ICONST_3.opcode |
                    ICONST_4.opcode ⇒
                    val value = as[LoadConstantInstruction[Int]](instruction).value
                    val targetVar = OperandVar(ComputationalTypeInt, stack)
                    statements(pc) = List(Assignment(pc, targetVar, IntConst(pc, value)))
                    schedule(pcOfNextInstruction(pc), targetVar :: stack)

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
            val sourceParam = Param(cTpe, "p_"+index)
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
 * All arithmetic operators that always operate on two stack values of the same
 * computational type.
 *
 * (The shift operators related to shifting long values operate on two different values!)
 */
object ArithmeticOperators extends Enumeration {
    final val Add = Value("+")
    final val Subtract = Value("-")
    final val Multiply = Value("*")
    final val Divide = Value("/")
    final val Modulo = Value("%")
    final val And = Value("&")
    final val Or = Value("|")
    final val LShift = Value("<<")
    final val RShift = Value(">>")
    final val RShiftLog = Value(">>>")
    final val XOr = Value("^")
    final val Greater = Value(">")
}

