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
import org.opalj.br.cfg.CFGFactory
import org.opalj.br.cfg.CatchNode
import org.opalj.br.cfg.BasicBlock
import org.opalj.br.analyses.ClassHierarchy
import org.opalj.br.analyses.AnalysisException

/**
 * Converts the bytecode of a method into a three address/quadruples representation.
 *
 * @author Michael Eichberg
 * @author Roberts Kolosovs
 */
object AsQuadruples {

    /**
     * Converts the bytecode of a method into a three address/quadruples representation using
     * the result of a bytecode based abstract interpretation of the method.
     *
     * @param method A method with a body. I.e., a non-native, non-abstract method.
     * @param aiResult The result of an abstract interpretation of the respective method.
     *
     * @return The array with the generated statements.
     */
    def apply(
        method:         Method,
        classHierarchy: ClassHierarchy   = Code.preDefinedClassHierarchy,
        aiResult:       Option[AIResult]
    ): Array[Stmt] = {

        import BinaryArithmeticOperators._
        import RelationalOperators._
        import UnaryArithmeticOperators._

        val code = method.body.get
        import code.pcOfNextInstruction
        val instructions = code.instructions
        val codeSize = instructions.size

        // only needed if we find jsr/ret instructions
        lazy val cfg = CFGFactory.apply(method, classHierarchy)

        // Used to determine if we have already transformed the respective instruction.
        val processed = new BitSet(codeSize)

        // In a few cases, such as swap and dup instructions, we have to
        // create multiple three-address instructions. In this case
        // we have to make sure that jump targets are still pointing to the right
        // instructions. Hence, we have to make sure that all statements created
        // for one instruction appear to have the same pc.
        //
        // However, no transformation creates new control structures.
        val statements = new Array[List[Stmt]](codeSize)

        processed(0) = true
        var worklist: List[(PC, Stack)] = List((0, Nil))
        for (exceptionHandler ← code.exceptionHandlers) {
            worklist ::= ((exceptionHandler.handlerPC, List(OperandVar.HandledException)))
        }

        while (worklist.nonEmpty) {
            val (pc, stack) = worklist.head
            val instruction = instructions(pc)
            val opcode = instruction.opcode
            worklist = worklist.tail

            // Schedules the execution of the instruction using the given stack.
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
                schedule(pcOfNextInstruction(pc), operandVar :: stack)
            }

            def storeInstruction(targetRegister: UShort): Unit = {
                val operandVar = stack.head
                val cTpe = operandVar.cTpe
                val registerVar = RegisterVar(cTpe, targetRegister)
                statements(pc) = List(Assignment(pc, registerVar, operandVar))
                val newStack = stack.tail
                val nextPC = pcOfNextInstruction(pc)
                schedule(nextPC, newStack)
            }

            def arrayLoad(cTpe: ComputationalType): Unit = {
                val index :: arrayRef :: rest = stack
                val operandVar = OperandVar(cTpe, rest)
                val source = ArrayLoad(pc, index, arrayRef)
                statements(pc) = List(Assignment(pc, operandVar, source))
                schedule(pcOfNextInstruction(pc), operandVar :: rest)
            }

            // Note:
            // The computational type of the binary expression is determined using the
            // first (left) value of the expression. This makes it possible to use
            // this function for bit shift operations where value1 and
            // value2 may have different computational types, but the resulting type
            // is always determined by the type of value1.
            def binaryArithmeticOperation(operator: BinaryArithmeticOperator): Unit = {
                val value2 :: value1 :: _ = stack
                val cTpe = value1.cTpe
                val expr = BinaryExpr(pc, cTpe, operator, value1, value2)
                statements(pc) = List(Assignment(pc, value1, expr))
                schedule(pcOfNextInstruction(pc), stack.tail)
            }

            def prefixArithmeticOperation(operator: UnaryArithmeticOperator): Unit = {
                val value :: _ = stack
                val cTpe = value.cTpe
                val expr = PrefixExpr(pc, cTpe, operator, value)
                statements(pc) = List(Assignment(pc, value, expr))
                schedule(pcOfNextInstruction(pc), stack)
            }

            def primitiveCastOperation(trgtTpe: BaseType): Unit = {
                val value :: rest = stack
                val result = OperandVar(trgtTpe.computationalType, stack)
                val castExpr = PrimitiveTypecastExpr(pc, trgtTpe, value)
                statements(pc) = List(Assignment(pc, result, castExpr))
                schedule(pcOfNextInstruction(pc), result :: rest)
            }

            def returnInstruction(fallback: SimpleVar): Unit = {
                val returnedValue =
                    aiResult.flatMap { r ⇒
                        // We have to be able to handle the case that the operands
                        // array is empty (i.e., the instruction is dead).
                        Option(r.operandsArray(pc)).map(ops ⇒ DomainValueBasedVar(0, ops.head))
                    }.getOrElse(fallback)
                statements(pc) = List(ReturnValue(pc, returnedValue))
            }

            def newArray(arrayType: ArrayType): Unit = {
                val count :: rest = stack
                val newArray = NewArray(pc, List(count), arrayType)
                val newVal = OperandVar(ComputationalTypeReference, rest)
                statements(pc) = List(Assignment(pc, newVal, newArray))
                schedule(pcOfNextInstruction(pc), newVal :: rest)
            }

            def loadConstant(instr: LoadConstantInstruction[_]): Unit = {
                instr match {
                    case LDCInt(value) ⇒ {
                        val newVar = OperandVar(ComputationalTypeInt, stack)
                        statements(pc) = List(Assignment(pc, newVar, IntConst(pc, value)))
                        schedule(pcOfNextInstruction(pc), newVar :: stack)
                    }
                    case LDCFloat(value) ⇒ {
                        val newVar = OperandVar(ComputationalTypeFloat, stack)
                        val floatConst = FloatConst(pc, value)
                        statements(pc) = List(Assignment(pc, newVar, floatConst))
                        schedule(pcOfNextInstruction(pc), newVar :: stack)
                    }
                    case LDCClass(value) ⇒ {
                        val newVar = OperandVar(ComputationalTypeReference, stack)
                        statements(pc) = List(Assignment(pc, newVar, ClassConst(pc, value)))
                        schedule(pcOfNextInstruction(pc), newVar :: stack)
                    }
                    case LDCString(value) ⇒ {
                        val newVar = OperandVar(ComputationalTypeReference, stack)
                        statements(pc) = List(Assignment(pc, newVar, StringConst(pc, value)))
                        schedule(pcOfNextInstruction(pc), newVar :: stack)
                    }
                    case LDCMethodHandle(value) ⇒ {
                        val newVar = OperandVar(ComputationalTypeReference, stack)
                        statements(pc) = List(Assignment(pc, newVar, MethodHandleConst(pc, value)))
                        schedule(pcOfNextInstruction(pc), newVar :: stack)
                    }
                    case LDCMethodType(value) ⇒ {
                        val newVar = OperandVar(ComputationalTypeReference, stack)
                        val methodTypeConst = MethodTypeConst(pc, value)
                        statements(pc) = List(Assignment(pc, newVar, methodTypeConst))
                        schedule(pcOfNextInstruction(pc), newVar :: stack)
                    }

                    case LoadDouble(value) ⇒ {
                        val newVar = OperandVar(ComputationalTypeDouble, stack)
                        statements(pc) = List(Assignment(pc, newVar, DoubleConst(pc, value)))
                        schedule(pcOfNextInstruction(pc), newVar :: stack)
                    }
                    case LoadLong(value) ⇒ {
                        val newVar = OperandVar(ComputationalTypeLong, stack)
                        statements(pc) = List(Assignment(pc, newVar, LongConst(pc, value)))
                        schedule(pcOfNextInstruction(pc), newVar :: stack)
                    }

                    case _ ⇒
                        val message = s"unexpected constant $instr"
                        throw BytecodeProcessingFailedException(message)
                }
            }

            def compareValues(op: RelationalOperator): Unit = {
                val value2 :: value1 :: rest = stack
                val result = OperandVar(ComputationalTypeInt, rest)
                val compare = Compare(pc, value1, op, value2)
                statements(pc) = List(Assignment(pc, result, compare))
                schedule(pcOfNextInstruction(pc), result :: rest)
            }

            def as[T <: Instruction](i: Instruction): T = i.asInstanceOf[T]

            (opcode: @scala.annotation.switch) match {
                case ALOAD_0.opcode ⇒ loadInstruction(0, ComputationalTypeReference)
                case ALOAD_1.opcode ⇒ loadInstruction(1, ComputationalTypeReference)
                case ALOAD_2.opcode ⇒ loadInstruction(2, ComputationalTypeReference)
                case ALOAD_3.opcode ⇒ loadInstruction(3, ComputationalTypeReference)
                case ALOAD.opcode ⇒
                    val lvIndex = as[ALOAD](instruction).lvIndex
                    loadInstruction(lvIndex, ComputationalTypeReference)

                case ASTORE_0.opcode ⇒ storeInstruction(0)
                case ASTORE_1.opcode ⇒ storeInstruction(1)
                case ASTORE_2.opcode ⇒ storeInstruction(2)
                case ASTORE_3.opcode ⇒ storeInstruction(3)
                case ASTORE.opcode   ⇒ storeInstruction(as[ASTORE](instruction).lvIndex)

                case ILOAD_0.opcode  ⇒ loadInstruction(0, ComputationalTypeInt)
                case ILOAD_1.opcode  ⇒ loadInstruction(1, ComputationalTypeInt)
                case ILOAD_2.opcode  ⇒ loadInstruction(2, ComputationalTypeInt)
                case ILOAD_3.opcode  ⇒ loadInstruction(3, ComputationalTypeInt)
                case ILOAD.opcode ⇒
                    loadInstruction(as[ILOAD](instruction).lvIndex, ComputationalTypeInt)

                case ISTORE_0.opcode ⇒ storeInstruction(0)
                case ISTORE_1.opcode ⇒ storeInstruction(1)
                case ISTORE_2.opcode ⇒ storeInstruction(2)
                case ISTORE_3.opcode ⇒ storeInstruction(3)
                case ISTORE.opcode   ⇒ storeInstruction(as[ISTORE](instruction).lvIndex)

                case DLOAD_0.opcode  ⇒ loadInstruction(0, ComputationalTypeDouble)
                case DLOAD_1.opcode  ⇒ loadInstruction(1, ComputationalTypeDouble)
                case DLOAD_2.opcode  ⇒ loadInstruction(2, ComputationalTypeDouble)
                case DLOAD_3.opcode  ⇒ loadInstruction(3, ComputationalTypeDouble)
                case DLOAD.opcode ⇒
                    loadInstruction(as[DLOAD](instruction).lvIndex, ComputationalTypeDouble)

                case DSTORE_0.opcode ⇒ storeInstruction(0)
                case DSTORE_1.opcode ⇒ storeInstruction(1)
                case DSTORE_2.opcode ⇒ storeInstruction(2)
                case DSTORE_3.opcode ⇒ storeInstruction(3)
                case DSTORE.opcode   ⇒ storeInstruction(as[DSTORE](instruction).lvIndex)

                case FLOAD_0.opcode  ⇒ loadInstruction(0, ComputationalTypeFloat)
                case FLOAD_1.opcode  ⇒ loadInstruction(1, ComputationalTypeFloat)
                case FLOAD_2.opcode  ⇒ loadInstruction(2, ComputationalTypeFloat)
                case FLOAD_3.opcode  ⇒ loadInstruction(3, ComputationalTypeFloat)
                case FLOAD.opcode ⇒
                    loadInstruction(as[FLOAD](instruction).lvIndex, ComputationalTypeFloat)

                case FSTORE_0.opcode ⇒ storeInstruction(0)
                case FSTORE_1.opcode ⇒ storeInstruction(1)
                case FSTORE_2.opcode ⇒ storeInstruction(2)
                case FSTORE_3.opcode ⇒ storeInstruction(3)
                case FSTORE.opcode   ⇒ storeInstruction(as[FSTORE](instruction).lvIndex)

                case LLOAD_0.opcode  ⇒ loadInstruction(0, ComputationalTypeLong)
                case LLOAD_1.opcode  ⇒ loadInstruction(1, ComputationalTypeLong)
                case LLOAD_2.opcode  ⇒ loadInstruction(2, ComputationalTypeLong)
                case LLOAD_3.opcode  ⇒ loadInstruction(3, ComputationalTypeLong)
                case LLOAD.opcode ⇒
                    loadInstruction(as[LLOAD](instruction).lvIndex, ComputationalTypeLong)

                case LSTORE_0.opcode ⇒ storeInstruction(0)
                case LSTORE_1.opcode ⇒ storeInstruction(1)
                case LSTORE_2.opcode ⇒ storeInstruction(2)
                case LSTORE_3.opcode ⇒ storeInstruction(3)
                case LSTORE.opcode   ⇒ storeInstruction(as[LSTORE](instruction).lvIndex)

                case IRETURN.opcode  ⇒ returnInstruction(OperandVar.IntReturnValue)
                case LRETURN.opcode  ⇒ returnInstruction(OperandVar.LongReturnValue)
                case FRETURN.opcode  ⇒ returnInstruction(OperandVar.FloatReturnValue)
                case DRETURN.opcode  ⇒ returnInstruction(OperandVar.DoubleReturnValue)
                case ARETURN.opcode  ⇒ returnInstruction(OperandVar.ReferenceReturnValue)
                case RETURN.opcode   ⇒ statements(pc) = List(Return(pc))

                case AALOAD.opcode   ⇒ arrayLoad(ComputationalTypeReference)
                case DALOAD.opcode   ⇒ arrayLoad(ComputationalTypeDouble)
                case FALOAD.opcode   ⇒ arrayLoad(ComputationalTypeFloat)
                case IALOAD.opcode   ⇒ arrayLoad(ComputationalTypeInt)
                case LALOAD.opcode   ⇒ arrayLoad(ComputationalTypeLong)
                case SALOAD.opcode   ⇒ arrayLoad(ComputationalTypeInt)
                case BALOAD.opcode   ⇒ arrayLoad(ComputationalTypeInt)
                case CALOAD.opcode   ⇒ arrayLoad(ComputationalTypeInt)

                case AASTORE.opcode | DASTORE.opcode |
                    FASTORE.opcode | IASTORE.opcode |
                    LASTORE.opcode | SASTORE.opcode |
                    BASTORE.opcode | CASTORE.opcode ⇒
                    val operandVar :: index :: arrayRef :: rest = stack
                    statements(pc) = List(ArrayStore(pc, arrayRef, index, operandVar))
                    schedule(pcOfNextInstruction(pc), rest)

                case ARRAYLENGTH.opcode ⇒
                    val arrayRef :: rest = stack
                    val lengthVar = OperandVar(ComputationalTypeInt, rest)
                    val lengthExpr = ArrayLength(pc, arrayRef)
                    statements(pc) = List(Assignment(pc, lengthVar, lengthExpr))
                    schedule(pcOfNextInstruction(pc), lengthVar :: rest)

                case BIPUSH.opcode | SIPUSH.opcode ⇒
                    val value = as[LoadConstantInstruction[Int]](instruction).value
                    val targetVar = OperandVar(ComputationalTypeInt, stack)
                    statements(pc) = List(Assignment(pc, targetVar, IntConst(pc, value)))
                    schedule(pcOfNextInstruction(pc), targetVar :: stack)

                case IF_ICMPEQ.opcode | IF_ICMPNE.opcode |
                    IF_ICMPLT.opcode | IF_ICMPLE.opcode |
                    IF_ICMPGT.opcode | IF_ICMPGE.opcode ⇒
                    val ifInstr = as[IFICMPInstruction](instruction)
                    val value2 :: value1 :: rest = stack
                    // let's calculate the final address
                    val targetPC = pc + ifInstr.branchoffset
                    val stmt = If(pc, value1, ifInstr.condition, value2, targetPC)
                    schedule(pcOfNextInstruction(pc), rest)
                    schedule(targetPC, rest)
                    statements(pc) = List(stmt)

                case IFEQ.opcode | IFNE.opcode |
                    IFLT.opcode | IFLE.opcode |
                    IFGT.opcode | IFGE.opcode ⇒
                    val ifInstr = as[IF0Instruction](instruction)
                    val value :: rest = stack
                    // let's calculate the final address
                    val targetPC = pc + ifInstr.branchoffset
                    val stmt = If(pc, value, ifInstr.condition, IntConst(-pc, 0), targetPC)
                    schedule(pcOfNextInstruction(pc), rest)
                    schedule(targetPC, rest)
                    statements(pc) = List(stmt)

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

                case DCMPG.opcode | FCMPG.opcode ⇒ compareValues(CMPG)
                case DCMPL.opcode | FCMPL.opcode ⇒ compareValues(CMPL)
                case LCMP.opcode                 ⇒ compareValues(CMP)

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

                case DADD.opcode | FADD.opcode | IADD.opcode | LADD.opcode ⇒
                    binaryArithmeticOperation(Add)
                case DDIV.opcode | FDIV.opcode | IDIV.opcode | LDIV.opcode ⇒
                    binaryArithmeticOperation(Divide)

                case DNEG.opcode | FNEG.opcode | INEG.opcode | LNEG.opcode ⇒
                    prefixArithmeticOperation(Negate)
                case DMUL.opcode | FMUL.opcode | IMUL.opcode | LMUL.opcode ⇒
                    binaryArithmeticOperation(Multiply)
                case DREM.opcode | FREM.opcode | IREM.opcode | LREM.opcode ⇒
                    binaryArithmeticOperation(Modulo)
                case DSUB.opcode | FSUB.opcode | ISUB.opcode | LSUB.opcode ⇒
                    binaryArithmeticOperation(Subtract)

                case IINC.opcode ⇒
                    val IINC(index, const) = instruction
                    val indexReg = RegisterVar(ComputationalTypeInt, index)
                    val incVal = IntConst(pc, const)
                    val iinc = BinaryExpr(pc, ComputationalTypeInt, Add, indexReg, incVal)
                    statements(pc) = List(Assignment(pc, indexReg, iinc))
                    schedule(pcOfNextInstruction(pc), stack)

                case IAND.opcode | LAND.opcode   ⇒ binaryArithmeticOperation(And)
                case IOR.opcode | LOR.opcode     ⇒ binaryArithmeticOperation(Or)
                case ISHL.opcode | LSHL.opcode   ⇒ binaryArithmeticOperation(ShiftLeft)
                case ISHR.opcode | LSHR.opcode   ⇒ binaryArithmeticOperation(ShiftRight)
                case IUSHR.opcode | LUSHR.opcode ⇒ binaryArithmeticOperation(UnsignedShiftRight)
                case IXOR.opcode | LXOR.opcode   ⇒ binaryArithmeticOperation(XOr)

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

                case FCONST_0.opcode | FCONST_1.opcode | FCONST_2.opcode ⇒
                    val value = as[LoadConstantInstruction[Float]](instruction).value
                    val targetVar = OperandVar(ComputationalTypeFloat, stack)
                    statements(pc) = List(Assignment(pc, targetVar, FloatConst(pc, value)))
                    schedule(pcOfNextInstruction(pc), targetVar :: stack)

                case LCONST_0.opcode | LCONST_1.opcode ⇒
                    val value = as[LoadConstantInstruction[Long]](instruction).value
                    val targetVar = OperandVar(ComputationalTypeLong, stack)
                    statements(pc) = List(Assignment(pc, targetVar, LongConst(pc, value)))
                    schedule(pcOfNextInstruction(pc), targetVar :: stack)

                case LDC.opcode | LDC_W.opcode | LDC2_W.opcode ⇒
                    loadConstant(as[LoadConstantInstruction[_]](instruction))

                case INVOKEINTERFACE.opcode |
                    INVOKESPECIAL.opcode |
                    INVOKEVIRTUAL.opcode ⇒
                    val invoke = as[MethodInvocationInstruction](instruction)
                    val numOps = invoke.numberOfPoppedOperands { x ⇒ stack.drop(x).head.cTpe.computationalTypeCategory }
                    val (operands, rest) = stack.splitAt(numOps)
                    val (params, List(receiver)) = operands.splitAt(numOps - 1)
                    import invoke.{methodDescriptor, declaringClass, name}
                    val returnType = methodDescriptor.returnType
                    if (returnType.isVoidType) {
                        val stmtFactory =
                            if (invoke.isVirtualMethodCall)
                                VirtualMethodCall.apply _
                            else
                                NonVirtualMethodCall.apply _
                        val stmt =
                            stmtFactory(
                                pc,
                                declaringClass, name, methodDescriptor,
                                receiver,
                                params
                            )
                        statements(pc) = List(stmt)
                        schedule(pcOfNextInstruction(pc), rest)
                    } else {
                        val newVal = OperandVar(returnType.computationalType, rest)
                        val exprFactory =
                            if (invoke.isVirtualMethodCall)
                                VirtualFunctionCall.apply _
                            else
                                NonVirtualFunctionCall.apply _
                        val expr =
                            exprFactory(
                                pc,
                                declaringClass, name, methodDescriptor,
                                receiver,
                                params
                            )
                        statements(pc) = List(Assignment(pc, newVal, expr))
                        schedule(pcOfNextInstruction(pc), newVal :: rest)
                    }

                case INVOKESTATIC.opcode ⇒
                    val invoke = as[INVOKESTATIC](instruction)
                    val numOps = invoke.numberOfPoppedOperands { x ⇒ stack.drop(x).head.cTpe.computationalTypeCategory }
                    val (params, rest) = stack.splitAt(numOps)
                    import invoke.{declaringClass, methodDescriptor, name}
                    val returnType = methodDescriptor.returnType
                    if (returnType.isVoidType) {
                        val stmt =
                            StaticMethodCall(
                                pc,
                                declaringClass, name, methodDescriptor,
                                params
                            )
                        statements(pc) = List(stmt)
                        schedule(pcOfNextInstruction(pc), rest)
                    } else {
                        val newVal = OperandVar(returnType.computationalType, rest)
                        val expr =
                            StaticFunctionCall(
                                pc,
                                declaringClass, name, methodDescriptor,
                                params
                            )
                        statements(pc) = List(Assignment(pc, newVal, expr))
                        schedule(pcOfNextInstruction(pc), newVal :: rest)
                    }

                case INVOKEDYNAMIC.opcode ⇒
                    ???
                //                    val invoke = as[INVOKEDYNAMIC](instruction)
                //                    val numOps = invoke.numberOfPoppedOperands { x ⇒ stack.drop(x).head.cTpe.computationalTypeCategory }
                //                    val (operands, rest) = stack.splitAt(numOps)
                //                    val target: Option[Var] =
                //                        if (invoke.methodDescriptor.returnType.isVoidType) None
                //                        else Some(OperandVar(invoke.methodDescriptor.returnType.computationalType, rest))
                //                    statements(pc) = List(
                //                        MethodCall(pc, invoke.declaringClass, invoke.name, invoke.methodDescriptor,
                //                            None, operands, target))
                //                    val newStack = if (target.nonEmpty) { target.get :: rest } else { rest }
                //                    schedule(pcOfNextInstruction(pc), newStack)

                case PUTSTATIC.opcode ⇒
                    val value :: rest = stack
                    val PUTSTATIC = as[PUTSTATIC](instruction)
                    val putStatic = PutStatic(pc, PUTSTATIC.declaringClass, PUTSTATIC.name, value)
                    statements(pc) = List(putStatic)
                    schedule(pcOfNextInstruction(pc), rest)

                case PUTFIELD.opcode ⇒
                    val value :: objRef :: rest = stack
                    val PUTFIELD = as[PUTFIELD](instruction)
                    val putField = PutField(pc, PUTFIELD.declaringClass, PUTFIELD.name, objRef, value)
                    statements(pc) = List(putField)
                    schedule(pcOfNextInstruction(pc), rest)

                case GETSTATIC.opcode ⇒
                    val GETSTATIC = as[GETSTATIC](instruction)
                    val getStatic = GetStatic(pc, GETSTATIC.declaringClass, GETSTATIC.name)
                    val newVal = OperandVar(ComputationalTypeReference, stack)
                    statements(pc) = List(Assignment(pc, newVal, getStatic))
                    schedule(pcOfNextInstruction(pc), newVal :: stack)

                case GETFIELD.opcode ⇒
                    val objRef :: rest = stack
                    val GETFIELD = as[GETFIELD](instruction)
                    val getField = GetField(pc, GETFIELD.declaringClass, GETFIELD.name, objRef)
                    val newVal = OperandVar(ComputationalTypeReference, rest)
                    statements(pc) = List(Assignment(pc, newVal, getField))
                    schedule(pcOfNextInstruction(pc), newVal :: rest)

                case NEW.opcode ⇒
                    val instr = as[NEW](instruction)
                    val newVal = OperandVar(ComputationalTypeReference, stack)
                    statements(pc) = List(Assignment(pc, newVal, New(pc, instr.objectType)))
                    schedule(pcOfNextInstruction(pc), newVal :: stack)

                case NEWARRAY.opcode ⇒
                    newArray(ArrayType(as[NEWARRAY](instruction).elementType))

                case ANEWARRAY.opcode ⇒
                    newArray(ArrayType(as[ANEWARRAY](instruction).componentType))

                case MULTIANEWARRAY.opcode ⇒
                    val instr = as[MULTIANEWARRAY](instruction)
                    val (counts, rest) = stack.splitAt(instr.dimensions)
                    val newArray = NewArray(pc, counts, instr.componentType)
                    val newVal = OperandVar(ComputationalTypeReference, rest)
                    statements(pc) = List(Assignment(pc, newVal, newArray))
                    schedule(pcOfNextInstruction(pc), newVal :: rest)

                case GOTO.opcode | GOTO_W.opcode ⇒
                    val targetPC = pc + as[GotoInstruction](instruction).branchoffset
                    statements(pc) = List(Goto(pc, targetPC))
                    schedule(targetPC, stack)

                case JSR.opcode | JSR_W.opcode ⇒
                    val targetPC = pc + as[JSRInstruction](instruction).branchoffset
                    val retVar = OperandVar(ComputationalTypeReturnAddress, stack)
                    statements(pc) = List(JumpToSubroutine(pc, targetPC))
                    schedule(targetPC, retVar :: stack)

                case RET.opcode ⇒
                    val ret = as[RET](instruction)
                    val returnAddressVar = SimpleVar(ret.lvIndex, ComputationalTypeReturnAddress)
                    statements(pc) = List(Ret(pc, returnAddressVar))
                    cfg.bb(pc).successors.foreach { cfgNode ⇒
                        cfgNode match {
                            case cn: CatchNode  ⇒ schedule(cn.handlerPC, stack)
                            case bb: BasicBlock ⇒ schedule(bb.startPC, stack)
                            case _ ⇒
                                // in these cases something went terribly wrong...
                                val message = "the cfg has an unexpected shape: "+cfgNode
                                throw new AnalysisException(message)
                        }
                    }

                case NOP.opcode ⇒
                    statements(pc) = List(Nop(pc))
                    schedule(pcOfNextInstruction(pc), stack)

                case POP.opcode ⇒
                    val _ :: rest = stack
                    statements(pc) = List(Nop(pc))
                    schedule(pcOfNextInstruction(pc), rest)

                case POP2.opcode ⇒
                    statements(pc) = List(Nop(pc))
                    stack match {
                        case (val1 @ CTC1()) :: val2 :: rest ⇒
                            schedule(pcOfNextInstruction(pc), rest)
                        case _ :: rest ⇒
                            schedule(pcOfNextInstruction(pc), rest)
                    }

                case INSTANCEOF.opcode ⇒
                    val value1 :: rest = stack
                    val resultVar = OperandVar(ComputationalTypeInt, stack)
                    val tpe = as[INSTANCEOF](instruction).referenceType
                    val instanceOf = InstanceOf(pc, value1, tpe)
                    statements(pc) = List(Assignment(pc, resultVar, instanceOf))
                    schedule(pcOfNextInstruction(pc), resultVar :: rest)

                case CHECKCAST.opcode ⇒
                    val value1 :: rest = stack
                    val resultVar = OperandVar(ComputationalTypeReference, rest)
                    val targetType = as[CHECKCAST](instruction).referenceType
                    val checkcast = Checkcast(pc, value1, targetType)
                    statements(pc) = List(Assignment(pc, resultVar, checkcast))
                    schedule(pcOfNextInstruction(pc), resultVar :: rest)

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
                    val tableSwitch = as[TABLESWITCH](instruction)
                    val defaultTarget = pc + tableSwitch.defaultOffset
                    var caseValue = tableSwitch.low
                    val npairs = tableSwitch.jumpOffsets map { jo ⇒
                        val caseTarget = pc + jo
                        val npair = (caseValue, caseTarget)
                        schedule(caseTarget, rest)
                        caseValue += 1
                        npair
                    }
                    schedule(defaultTarget, rest)
                    statements(pc) = List(Switch(pc, defaultTarget, index, npairs))

                case LOOKUPSWITCH.opcode ⇒
                    val index :: rest = stack
                    val lookupSwitch = as[LOOKUPSWITCH](instruction)
                    val defaultTarget = pc + lookupSwitch.defaultOffset
                    val npairs = lookupSwitch.npairs.map { npair ⇒
                        val (caseValue, branchOffset) = npair
                        val caseTarget = pc + branchOffset
                        schedule(caseTarget, rest)
                        (caseValue, caseTarget)
                    }
                    schedule(defaultTarget, rest)
                    statements(pc) = List(Switch(pc, defaultTarget, index, npairs))

                case DUP.opcode ⇒
                    statements(pc) = List(Nop(pc))
                    schedule(pcOfNextInstruction(pc), stack.head :: stack)

                case DUP_X1.opcode ⇒
                    val val1 :: val2 :: rest = stack
                    statements(pc) = List(Nop(pc))
                    schedule(pcOfNextInstruction(pc), val1 :: val2 :: val1 :: rest)

                case DUP_X2.opcode ⇒
                    statements(pc) = List(Nop(pc))
                    stack match {
                        case v1 :: (v2 @ CTC1()) :: v3 :: rest ⇒
                            schedule(pcOfNextInstruction(pc), v1 :: v2 :: v3 :: v1 :: rest)
                        case v1 :: v2 :: rest ⇒
                            schedule(pcOfNextInstruction(pc), v1 :: v2 :: v1 :: rest)
                    }

                case DUP2.opcode ⇒
                    statements(pc) = List(Nop(pc))
                    stack match {
                        case (v1 @ CTC1()) :: v2 :: rest ⇒
                            schedule(pcOfNextInstruction(pc), v1 :: v2 :: v1 :: v2 :: rest)
                        case v1 :: rest ⇒
                            schedule(pcOfNextInstruction(pc), v1 :: v1 :: rest)
                    }

                case DUP2_X1.opcode ⇒
                    statements(pc) = List(Nop(pc))
                    stack match {
                        case (v1 @ CTC1()) :: v2 :: v3 :: rest ⇒
                            schedule(pcOfNextInstruction(pc), v1 :: v2 :: v3 :: v1 :: v2 :: rest)
                        case v1 :: v2 :: rest ⇒
                            schedule(pcOfNextInstruction(pc), v1 :: v2 :: v1 :: rest)
                    }

                case DUP2_X2.opcode ⇒
                    statements(pc) = List(Nop(pc))
                    stack match {
                        case (v1 @ CTC1()) :: (v2 @ CTC1()) :: (v3 @ CTC1()) :: (v4 /*@ CTC1()*/ ) :: rest ⇒
                            schedule(pcOfNextInstruction(pc), v1 :: v2 :: v3 :: v4 :: v1 :: v2 :: rest)
                        case (v1 @ CTC2()) :: (v2 @ CTC1()) :: (v3 @ CTC1()) :: rest ⇒
                            schedule(pcOfNextInstruction(pc), v1 :: v2 :: v3 :: v1 :: rest)
                        case (v1 @ CTC1()) :: (v2 @ CTC1()) :: (v3 @ CTC2()) :: rest ⇒
                            schedule(pcOfNextInstruction(pc), v1 :: v2 :: v3 :: v1 :: v2 :: rest)
                        case (v1 /*@ CTC2()*/ ) :: (v2 /*@ CTC1()*/ ) :: rest ⇒
                            schedule(pcOfNextInstruction(pc), v1 :: v2 :: v1 :: rest)
                    }

                case D2F.opcode | I2F.opcode | L2F.opcode ⇒ primitiveCastOperation(FloatType)
                case D2I.opcode | F2I.opcode | L2I.opcode ⇒ primitiveCastOperation(IntegerType)
                case D2L.opcode | I2L.opcode | F2L.opcode ⇒ primitiveCastOperation(LongType)
                case F2D.opcode | I2D.opcode | L2D.opcode ⇒ primitiveCastOperation(DoubleType)
                case I2C.opcode                           ⇒ primitiveCastOperation(CharType)
                case I2B.opcode                           ⇒ primitiveCastOperation(ByteType)
                case I2S.opcode                           ⇒ primitiveCastOperation(ShortType)

                case ATHROW.opcode ⇒
                    statements(pc) = List(Throw(pc, stack.head))

                case WIDE.opcode ⇒
                    statements(pc) = List(Nop(pc))
                    schedule(pcOfNextInstruction(pc), stack)

                case opcode ⇒
                    throw BytecodeProcessingFailedException(s"unknown opcode: $opcode")
            }
        }

        // Now we have to remap the target pcs to create the final statements array.
        // However, before we can do that we first add the register initialization
        // statements.
        var index = 0
        val finalStatements = new ArrayBuffer[Stmt](codeSize)
        var registerIndex = 0
        if (!method.isStatic) {
            val targetVar = RegisterVar(ComputationalTypeReference, 0)
            val sourceParam = Param(ComputationalTypeReference, "this")
            finalStatements += Assignment(-1, targetVar, sourceParam)
            index += 1
            registerIndex += 1
        }
        method.descriptor.parameterTypes foreach { parameterType ⇒
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
 * Facilitates matching against values of computational type category 1.
 *
 * @example
 * {{{
 * case v @ CTC1() => ...
 * }}}
 */
private[tac] object CTC1 { def unapply(value: Var): Boolean = value.cTpe.category == 1 }

/**
 * Facilitates matching against values of computational type category 2.
 *
 * @example
 * {{{
 * case v @ CTC2() => ...
 * }}}
 */
private[tac] object CTC2 { def unapply(value: Var): Boolean = value.cTpe.category == 2 }

