/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac

import scala.collection.mutable.ArrayBuffer

import org.opalj.collection.mutable.FixedSizeBitSet
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.collection.immutable.IntIntPair
import org.opalj.bytecode.BytecodeProcessingFailedException
import org.opalj.br._
import org.opalj.br.instructions._
import org.opalj.br.cfg.CFGFactory
import org.opalj.br.ClassHierarchy
import org.opalj.br.analyses.AnalysisException
import org.opalj.br.cfg.CatchNode
import org.opalj.br.cfg.BasicBlock
import org.opalj.tac.JSR

/**
 * Converts the bytecode of a method into a three address representation using a very naive
 * approach where each each operand stack value is stored in a local variable based on the
 * position of the value on the stack and where each local variable is stored in a local variable
 * named based on the register's index (In general, you should use the three-address code create
 * using [[TACAI]]).
 *
 * The converted method has an isomorophic CFG when compared to the original method,
 * but may contain more instructions due to the way how the stack manipulation instructions are
 * transformed. In general - unless JSR/RET instructions are found - no CFG is created and used.
 * This approach relies on the invariant that the stack has to have the same layout on all paths.
 * This makes the transformation very fast, but also makes it impossible to trivially compute the
 * type information.
 *
 * @author Michael Eichberg
 * @author Roberts Kolosovs
 */
object TACNaive {

    final type Stack = List[IdBasedVar]

    /**
     * Converts the plain bytecode of a method into a quadruples based three address
     * representation. Compared to the previous method, no data- and control-flow information is
     * used and a very general transformation mechanism is used to do the transformation.
     * Therefore the representation is very verbose. However, both leverage the same
     * AST nodes.
     *
     * @param  method A method with a body. I.e., a non-native, non-abstract method.
     * @param  classHierarchy The class hierarchy of the project defining the given method.
     *         If the class hierarchy is not available, you can use:
     *         `ClassHierarchy.PreInitializedClassHierarchy`.
     * @return The array with the generated statements.
     */
    // IMPROVE Make it explicit that op0 is the thrown exception in case of an exception handler! (Currently, op0 just exists and only by checking the exception handler table it becomes obvious where op0 is coming from.)
    def apply(
        method:         Method,
        classHierarchy: ClassHierarchy,
        optimizations:  List[TACOptimization[Param, IdBasedVar, NaiveTACode[Param]]] = List.empty
    ): NaiveTACode[Param] = {

        import BinaryArithmeticOperators._
        import RelationalOperators._
        import UnaryArithmeticOperators._

        val code = method.body.get
        import code.pcOfNextInstruction
        val instructions = code.instructions
        val codeSize = instructions.length

        // (only) used if the code contains jsr/ret instructions or if an optimization requires it
        val cfg = CFGFactory(code, classHierarchy)

        // Used to determine if we have already transformed the respective instruction.
        val processed = FixedSizeBitSet.create(codeSize)

        // In a few cases, such as swap and dup instructions, we have to
        // create multiple three-address instructions. In this case
        // we have to make sure that jump targets are still pointing to the right
        // instructions. Hence, we have to make sure that all statements created
        // for one instruction appear to have the same pc.
        //
        // However, no transformation creates new control structures.
        val statements = new Array[List[Stmt[IdBasedVar]]](codeSize)

        processed += 0
        var worklist: List[(PC, Stack)] = List((0, Nil))
        for (exceptionHandler <- code.exceptionHandlers) {
            worklist ::= ((exceptionHandler.handlerPC, List(OperandVar.HandledException)))
        }

        while (worklist.nonEmpty) {
            val (pc, stack) = worklist.head
            val instruction = instructions(pc)
            val opcode = instruction.opcode
            worklist = worklist.tail

            // Schedules the execution of the instruction using the given stack.
            def schedule(nextPC: PC, newStack: Stack): Unit = {
                if (processed add nextPC) {
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
                // the value may have computational type category 1 or 2 !
                val value :: rest = stack
                val result = OperandVar(trgtTpe.computationalType, rest)
                val castExpr = PrimitiveTypecastExpr(pc, trgtTpe, value)
                statements(pc) = List(Assignment(pc, result, castExpr))
                schedule(pcOfNextInstruction(pc), result :: rest)
            }

            def returnInstruction(returnedValue: SimpleVar): Unit = {
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
                    case LDCInt(value) =>
                        val newVar = OperandVar(ComputationalTypeInt, stack)
                        val stmt = Assignment[IdBasedVar](pc, newVar, IntConst(pc, value))
                        statements(pc) = List(stmt)
                        schedule(pcOfNextInstruction(pc), newVar :: stack)

                    case LDCFloat(value) =>
                        val newVar = OperandVar(ComputationalTypeFloat, stack)
                        val floatConst = FloatConst(pc, value)
                        statements(pc) = List(Assignment[IdBasedVar](pc, newVar, floatConst))
                        schedule(pcOfNextInstruction(pc), newVar :: stack)

                    case LDCClass(value) =>
                        val newVar = OperandVar(ComputationalTypeReference, stack)
                        val stmt = Assignment[IdBasedVar](pc, newVar, ClassConst(pc, value))
                        statements(pc) = List(stmt)
                        schedule(pcOfNextInstruction(pc), newVar :: stack)

                    case LDCString(value) =>
                        val newVar = OperandVar(ComputationalTypeReference, stack)
                        val stmt = Assignment[IdBasedVar](pc, newVar, StringConst(pc, value))
                        statements(pc) = List(stmt)
                        schedule(pcOfNextInstruction(pc), newVar :: stack)

                    case LDCMethodHandle(value) =>
                        val newVar = OperandVar(ComputationalTypeReference, stack)
                        statements(pc) =
                            List(Assignment[IdBasedVar](pc, newVar, MethodHandleConst(pc, value)))
                        schedule(pcOfNextInstruction(pc), newVar :: stack)

                    case LDCMethodType(value) =>
                        val newVar = OperandVar(ComputationalTypeReference, stack)
                        val methodTypeConst = MethodTypeConst(pc, value)
                        statements(pc) = List(Assignment[IdBasedVar](pc, newVar, methodTypeConst))
                        schedule(pcOfNextInstruction(pc), newVar :: stack)

                    case LoadDouble(value) =>
                        val newVar = OperandVar(ComputationalTypeDouble, stack)
                        val stmt = Assignment[IdBasedVar](pc, newVar, DoubleConst(pc, value))
                        statements(pc) = List(stmt)
                        schedule(pcOfNextInstruction(pc), newVar :: stack)

                    case LoadLong(value) =>
                        val newVar = OperandVar(ComputationalTypeLong, stack)
                        val stmt = Assignment[IdBasedVar](pc, newVar, LongConst(pc, value))
                        statements(pc) = List(stmt)
                        schedule(pcOfNextInstruction(pc), newVar :: stack)

                    case _ =>
                        val message = s"unexpected constant $instr"
                        throw BytecodeProcessingFailedException(message)
                }
            }

            def compareValues(op: RelationalOperator): Unit = {
                val value2 :: value1 :: rest = stack
                val result = OperandVar(ComputationalTypeInt, rest)
                val compare = Compare(pc, value1, op, value2)
                statements(pc) = List(Assignment[IdBasedVar](pc, result, compare))
                schedule(pcOfNextInstruction(pc), result :: rest)
            }

            def as[T <: Instruction](i: Instruction): T = i.asInstanceOf[T]

            (opcode: @scala.annotation.switch) match {
                case ALOAD_0.opcode => loadInstruction(0, ComputationalTypeReference)
                case ALOAD_1.opcode => loadInstruction(1, ComputationalTypeReference)
                case ALOAD_2.opcode => loadInstruction(2, ComputationalTypeReference)
                case ALOAD_3.opcode => loadInstruction(3, ComputationalTypeReference)
                case ALOAD.opcode =>
                    val lvIndex = as[ALOAD](instruction).lvIndex
                    loadInstruction(lvIndex, ComputationalTypeReference)

                case ASTORE_0.opcode => storeInstruction(0)
                case ASTORE_1.opcode => storeInstruction(1)
                case ASTORE_2.opcode => storeInstruction(2)
                case ASTORE_3.opcode => storeInstruction(3)
                case ASTORE.opcode   => storeInstruction(as[ASTORE](instruction).lvIndex)

                case ILOAD_0.opcode  => loadInstruction(0, ComputationalTypeInt)
                case ILOAD_1.opcode  => loadInstruction(1, ComputationalTypeInt)
                case ILOAD_2.opcode  => loadInstruction(2, ComputationalTypeInt)
                case ILOAD_3.opcode  => loadInstruction(3, ComputationalTypeInt)
                case ILOAD.opcode =>
                    loadInstruction(as[ILOAD](instruction).lvIndex, ComputationalTypeInt)

                case ISTORE_0.opcode => storeInstruction(0)
                case ISTORE_1.opcode => storeInstruction(1)
                case ISTORE_2.opcode => storeInstruction(2)
                case ISTORE_3.opcode => storeInstruction(3)
                case ISTORE.opcode   => storeInstruction(as[ISTORE](instruction).lvIndex)

                case DLOAD_0.opcode  => loadInstruction(0, ComputationalTypeDouble)
                case DLOAD_1.opcode  => loadInstruction(1, ComputationalTypeDouble)
                case DLOAD_2.opcode  => loadInstruction(2, ComputationalTypeDouble)
                case DLOAD_3.opcode  => loadInstruction(3, ComputationalTypeDouble)
                case DLOAD.opcode =>
                    loadInstruction(as[DLOAD](instruction).lvIndex, ComputationalTypeDouble)

                case DSTORE_0.opcode => storeInstruction(0)
                case DSTORE_1.opcode => storeInstruction(1)
                case DSTORE_2.opcode => storeInstruction(2)
                case DSTORE_3.opcode => storeInstruction(3)
                case DSTORE.opcode   => storeInstruction(as[DSTORE](instruction).lvIndex)

                case FLOAD_0.opcode  => loadInstruction(0, ComputationalTypeFloat)
                case FLOAD_1.opcode  => loadInstruction(1, ComputationalTypeFloat)
                case FLOAD_2.opcode  => loadInstruction(2, ComputationalTypeFloat)
                case FLOAD_3.opcode  => loadInstruction(3, ComputationalTypeFloat)
                case FLOAD.opcode =>
                    loadInstruction(as[FLOAD](instruction).lvIndex, ComputationalTypeFloat)

                case FSTORE_0.opcode => storeInstruction(0)
                case FSTORE_1.opcode => storeInstruction(1)
                case FSTORE_2.opcode => storeInstruction(2)
                case FSTORE_3.opcode => storeInstruction(3)
                case FSTORE.opcode   => storeInstruction(as[FSTORE](instruction).lvIndex)

                case LLOAD_0.opcode  => loadInstruction(0, ComputationalTypeLong)
                case LLOAD_1.opcode  => loadInstruction(1, ComputationalTypeLong)
                case LLOAD_2.opcode  => loadInstruction(2, ComputationalTypeLong)
                case LLOAD_3.opcode  => loadInstruction(3, ComputationalTypeLong)
                case LLOAD.opcode =>
                    loadInstruction(as[LLOAD](instruction).lvIndex, ComputationalTypeLong)

                case LSTORE_0.opcode => storeInstruction(0)
                case LSTORE_1.opcode => storeInstruction(1)
                case LSTORE_2.opcode => storeInstruction(2)
                case LSTORE_3.opcode => storeInstruction(3)
                case LSTORE.opcode   => storeInstruction(as[LSTORE](instruction).lvIndex)

                case IRETURN.opcode  => returnInstruction(OperandVar.IntReturnValue)
                case LRETURN.opcode  => returnInstruction(OperandVar.LongReturnValue)
                case FRETURN.opcode  => returnInstruction(OperandVar.FloatReturnValue)
                case DRETURN.opcode  => returnInstruction(OperandVar.DoubleReturnValue)
                case ARETURN.opcode  => returnInstruction(OperandVar.ReferenceReturnValue)
                case RETURN.opcode   => statements(pc) = List(Return(pc))

                case AALOAD.opcode   => arrayLoad(ComputationalTypeReference)
                case DALOAD.opcode   => arrayLoad(ComputationalTypeDouble)
                case FALOAD.opcode   => arrayLoad(ComputationalTypeFloat)
                case IALOAD.opcode   => arrayLoad(ComputationalTypeInt)
                case LALOAD.opcode   => arrayLoad(ComputationalTypeLong)
                case SALOAD.opcode   => arrayLoad(ComputationalTypeInt)
                case BALOAD.opcode   => arrayLoad(ComputationalTypeInt)
                case CALOAD.opcode   => arrayLoad(ComputationalTypeInt)

                case AASTORE.opcode | DASTORE.opcode |
                    FASTORE.opcode | IASTORE.opcode |
                    LASTORE.opcode | SASTORE.opcode |
                    BASTORE.opcode | CASTORE.opcode =>
                    val operandVar :: index :: arrayRef :: rest = stack
                    statements(pc) = List(ArrayStore(pc, arrayRef, index, operandVar))
                    schedule(pcOfNextInstruction(pc), rest)

                case ARRAYLENGTH.opcode =>
                    val arrayRef :: rest = stack
                    val lengthVar = OperandVar(ComputationalTypeInt, rest)
                    val lengthExpr = ArrayLength(pc, arrayRef)
                    statements(pc) = List(Assignment[IdBasedVar](pc, lengthVar, lengthExpr))
                    schedule(pcOfNextInstruction(pc), lengthVar :: rest)

                case BIPUSH.opcode | SIPUSH.opcode =>
                    val value = as[LoadConstantInstruction[Int]](instruction).value
                    val targetVar = OperandVar(ComputationalTypeInt, stack)
                    val stmt = Assignment[IdBasedVar](pc, targetVar, IntConst(pc, value))
                    statements(pc) = List(stmt)
                    schedule(pcOfNextInstruction(pc), targetVar :: stack)

                case IF_ICMPEQ.opcode | IF_ICMPNE.opcode |
                    IF_ICMPLT.opcode | IF_ICMPLE.opcode |
                    IF_ICMPGT.opcode | IF_ICMPGE.opcode =>
                    val ifInstr = instruction.asIFICMPInstruction
                    val value2 :: value1 :: rest = stack
                    // let's calculate the final address
                    val targetPC = pc + ifInstr.branchoffset
                    val stmt = If(pc, value1, ifInstr.condition, value2, targetPC)
                    schedule(pcOfNextInstruction(pc), rest)
                    schedule(targetPC, rest)
                    statements(pc) = List(stmt)

                case IFEQ.opcode | IFNE.opcode |
                    IFLT.opcode | IFLE.opcode |
                    IFGT.opcode | IFGE.opcode =>
                    val ifInstr = instruction.asIF0Instruction
                    val value :: rest = stack
                    // let's calculate the final address
                    val targetPC = pc + ifInstr.branchoffset
                    val stmt = If(pc, value, ifInstr.condition, IntConst(-pc, 0), targetPC)
                    schedule(pcOfNextInstruction(pc), rest)
                    schedule(targetPC, rest)
                    statements(pc) = List(stmt)

                case IF_ACMPEQ.opcode | IF_ACMPNE.opcode =>
                    val ifInstr = instruction.asIFACMPInstruction
                    val value2 :: value1 :: rest = stack
                    // let's calculate the final address
                    val targetPC = pc + ifInstr.branchoffset
                    val stmt = If(pc, value1, ifInstr.condition, value2, targetPC)
                    statements(pc) = List(stmt)
                    schedule(pcOfNextInstruction(pc), rest)
                    schedule(targetPC, rest)

                case IFNONNULL.opcode | IFNULL.opcode =>
                    val ifInstr = instruction.asIFXNullInstruction
                    val value :: rest = stack
                    // let's calculate the final address
                    val targetPC = pc + ifInstr.branchoffset
                    val stmt = If(pc, value, ifInstr.condition, NullExpr(-pc), targetPC)
                    statements(pc) = List(stmt)
                    schedule(pcOfNextInstruction(pc), rest)
                    schedule(targetPC, rest)

                case DCMPG.opcode | FCMPG.opcode => compareValues(CMPG)
                case DCMPL.opcode | FCMPL.opcode => compareValues(CMPL)
                case LCMP.opcode                 => compareValues(CMP)

                case SWAP.opcode =>
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

                case DADD.opcode | FADD.opcode | IADD.opcode | LADD.opcode =>
                    binaryArithmeticOperation(Add)
                case DDIV.opcode | FDIV.opcode | IDIV.opcode | LDIV.opcode =>
                    binaryArithmeticOperation(Divide)

                case DNEG.opcode | FNEG.opcode | INEG.opcode | LNEG.opcode =>
                    prefixArithmeticOperation(Negate)
                case DMUL.opcode | FMUL.opcode | IMUL.opcode | LMUL.opcode =>
                    binaryArithmeticOperation(Multiply)
                case DREM.opcode | FREM.opcode | IREM.opcode | LREM.opcode =>
                    binaryArithmeticOperation(Modulo)
                case DSUB.opcode | FSUB.opcode | ISUB.opcode | LSUB.opcode =>
                    binaryArithmeticOperation(Subtract)

                case IINC.opcode =>
                    val IINC(index, const) = instruction
                    val indexReg = RegisterVar(ComputationalTypeInt, index)
                    val incVal = IntConst(pc, const)
                    val iinc = BinaryExpr(pc, ComputationalTypeInt, Add, indexReg, incVal)
                    statements(pc) = List(Assignment(pc, indexReg, iinc))
                    schedule(pcOfNextInstruction(pc), stack)

                case IAND.opcode | LAND.opcode   => binaryArithmeticOperation(And)
                case IOR.opcode | LOR.opcode     => binaryArithmeticOperation(Or)
                case ISHL.opcode | LSHL.opcode   => binaryArithmeticOperation(ShiftLeft)
                case ISHR.opcode | LSHR.opcode   => binaryArithmeticOperation(ShiftRight)
                case IUSHR.opcode | LUSHR.opcode => binaryArithmeticOperation(UnsignedShiftRight)
                case IXOR.opcode | LXOR.opcode   => binaryArithmeticOperation(XOr)

                case ICONST_0.opcode | ICONST_1.opcode |
                    ICONST_2.opcode | ICONST_3.opcode |
                    ICONST_4.opcode | ICONST_5.opcode |
                    ICONST_M1.opcode =>
                    val value = as[LoadConstantInstruction[Int]](instruction).value
                    val targetVar = OperandVar(ComputationalTypeInt, stack)
                    val stmt = Assignment[IdBasedVar](pc, targetVar, IntConst(pc, value))
                    statements(pc) = List(stmt)
                    schedule(pcOfNextInstruction(pc), targetVar :: stack)

                case ACONST_NULL.opcode =>
                    val targetVar = OperandVar(ComputationalTypeReference, stack)
                    statements(pc) = List(Assignment[IdBasedVar](pc, targetVar, NullExpr(pc)))
                    schedule(pcOfNextInstruction(pc), targetVar :: stack)

                case DCONST_0.opcode | DCONST_1.opcode =>
                    val value = as[LoadConstantInstruction[Double]](instruction).value
                    val targetVar = OperandVar(ComputationalTypeDouble, stack)
                    val stmt = Assignment[IdBasedVar](pc, targetVar, DoubleConst(pc, value))
                    statements(pc) = List(stmt)
                    schedule(pcOfNextInstruction(pc), targetVar :: stack)

                case FCONST_0.opcode | FCONST_1.opcode | FCONST_2.opcode =>
                    val value = as[LoadConstantInstruction[Float]](instruction).value
                    val targetVar = OperandVar(ComputationalTypeFloat, stack)
                    val stmt = Assignment[IdBasedVar](pc, targetVar, FloatConst(pc, value))
                    statements(pc) = List(stmt)
                    schedule(pcOfNextInstruction(pc), targetVar :: stack)

                case LCONST_0.opcode | LCONST_1.opcode =>
                    val value = as[LoadConstantInstruction[Long]](instruction).value
                    val targetVar = OperandVar(ComputationalTypeLong, stack)
                    val stmt = Assignment[IdBasedVar](pc, targetVar, LongConst(pc, value))
                    statements(pc) = List(stmt)
                    schedule(pcOfNextInstruction(pc), targetVar :: stack)

                case LDC.opcode | LDC_W.opcode | LDC2_W.opcode =>
                    loadConstant(as[LoadConstantInstruction[_]](instruction))

                case INVOKESPECIAL.opcode =>
                    val invoke = as[MethodInvocationInstruction](instruction)
                    val numOps = invoke.numberOfPoppedOperands { x => stack(x).cTpe.category }
                    val (operands, rest) = stack.splitAt(numOps)
                    val (paramsInOperandsOrder, List(receiver)) = operands.splitAt(numOps - 1)
                    val params = paramsInOperandsOrder.reverse
                    import invoke.{methodDescriptor, declaringClass, isInterfaceCall, name}
                    val returnType = methodDescriptor.returnType
                    if (returnType.isVoidType) {
                        val stmt =
                            NonVirtualMethodCall(
                                pc,
                                declaringClass.asObjectType, isInterfaceCall,
                                name, methodDescriptor,
                                receiver,
                                params
                            )
                        statements(pc) = List(stmt)
                        schedule(pcOfNextInstruction(pc), rest)
                    } else {
                        val newVar = OperandVar(returnType.computationalType, rest)
                        val expr =
                            NonVirtualFunctionCall(
                                pc,
                                declaringClass.asObjectType, isInterfaceCall,
                                name, methodDescriptor,
                                receiver,
                                params
                            )
                        statements(pc) = List(Assignment[IdBasedVar](pc, newVar, expr))
                        schedule(pcOfNextInstruction(pc), newVar :: rest)
                    }

                case INVOKEINTERFACE.opcode | INVOKEVIRTUAL.opcode =>
                    val invoke = as[MethodInvocationInstruction](instruction)
                    val numOps = invoke.numberOfPoppedOperands { x => stack(x).cTpe.category }
                    val (operands, rest) = stack.splitAt(numOps)
                    val (paramsInOperandsOrder, List(receiver)) = operands.splitAt(numOps - 1)
                    val params = paramsInOperandsOrder.reverse
                    import invoke.{methodDescriptor, declaringClass, isInterfaceCall, name}
                    val returnType = methodDescriptor.returnType
                    if (returnType.isVoidType) {
                        val stmt =
                            VirtualMethodCall(
                                pc,
                                declaringClass, isInterfaceCall,
                                name, methodDescriptor,
                                receiver,
                                params
                            )
                        statements(pc) = List(stmt)
                        schedule(pcOfNextInstruction(pc), rest)
                    } else {
                        val newVar = OperandVar(returnType.computationalType, rest)
                        val expr =
                            VirtualFunctionCall(
                                pc,
                                declaringClass, isInterfaceCall,
                                name, methodDescriptor,
                                receiver,
                                params
                            )
                        statements(pc) = List(Assignment[IdBasedVar](pc, newVar, expr))
                        schedule(pcOfNextInstruction(pc), newVar :: rest)
                    }

                case INVOKESTATIC.opcode =>
                    val invoke = as[INVOKESTATIC](instruction)
                    val numOps = invoke.numberOfPoppedOperands { x => stack(x).cTpe.category }
                    val (paramsInOperandsOrder, rest) = stack.splitAt(numOps)
                    val params = paramsInOperandsOrder.reverse
                    import invoke.{declaringClass, methodDescriptor, name, isInterface}
                    val returnType = methodDescriptor.returnType
                    if (returnType.isVoidType) {
                        val stmt =
                            StaticMethodCall(
                                pc,
                                declaringClass, isInterface, name, methodDescriptor,
                                params
                            )
                        statements(pc) = List(stmt)
                        schedule(pcOfNextInstruction(pc), rest)
                    } else {
                        val newVar = OperandVar(returnType.computationalType, rest)
                        val expr =
                            StaticFunctionCall(
                                pc,
                                declaringClass, isInterface, name, methodDescriptor,
                                params
                            )
                        statements(pc) = List(Assignment[IdBasedVar](pc, newVar, expr))
                        schedule(pcOfNextInstruction(pc), newVar :: rest)
                    }

                case INVOKEDYNAMIC.opcode =>
                    val call @ INVOKEDYNAMIC(bootstrapMethod, name, descriptor) = instruction
                    val numOps = call.numberOfPoppedOperands(x => stack.drop(x).head.cTpe.category)
                    val (paramsInOperandsOrder, rest) = stack.splitAt(numOps)
                    val params = paramsInOperandsOrder.reverse
                    val returnType = descriptor.returnType
                    if (returnType.isVoidType) {
                        val indyMethodCall =
                            InvokedynamicMethodCall(pc, bootstrapMethod, name, descriptor, params)
                        statements(pc) = List(indyMethodCall)
                        schedule(pcOfNextInstruction(pc), rest)
                    } else {
                        val indyFunctionCall =
                            InvokedynamicFunctionCall(pc, bootstrapMethod, name, descriptor, params)
                        val newVar = OperandVar(returnType.computationalType, rest)
                        statements(pc) = List(Assignment(pc, newVar, indyFunctionCall))
                        schedule(pcOfNextInstruction(pc), newVar :: rest)
                    }

                case PUTSTATIC.opcode =>
                    val value :: rest = stack
                    val PUTSTATIC(declaringClass, name, fieldType) = instruction
                    val putStatic = PutStatic(pc, declaringClass, name, fieldType, value)
                    statements(pc) = List(putStatic)
                    schedule(pcOfNextInstruction(pc), rest)

                case PUTFIELD.opcode =>
                    val value :: objRef :: rest = stack
                    val PUTFIELD(declaringClass, name, fieldType) = instruction
                    val stmt = PutField(pc, declaringClass, name, fieldType, objRef, value)
                    statements(pc) = List(stmt)
                    schedule(pcOfNextInstruction(pc), rest)

                case GETSTATIC.opcode =>
                    val GETSTATIC(declaringClass, name, fieldType) = instruction
                    val getStatic = GetStatic(pc, declaringClass, name, fieldType)
                    val newVal = OperandVar(ComputationalTypeReference, stack)
                    statements(pc) = List(Assignment[IdBasedVar](pc, newVal, getStatic))
                    schedule(pcOfNextInstruction(pc), newVal :: stack)

                case GETFIELD.opcode =>
                    val objRef :: rest = stack
                    val GETFIELD(declaringClass, name, fieldType) = instruction
                    val getField = GetField(pc, declaringClass, name, fieldType, objRef)
                    val newVal = OperandVar(ComputationalTypeReference, rest)
                    statements(pc) = List(Assignment(pc, newVal, getField))
                    schedule(pcOfNextInstruction(pc), newVal :: rest)

                case NEW.opcode =>
                    val instr = as[NEW](instruction)
                    val newVal = OperandVar(ComputationalTypeReference, stack)
                    val stmt = Assignment[IdBasedVar](pc, newVal, New(pc, instr.objectType))
                    statements(pc) = List(stmt)
                    schedule(pcOfNextInstruction(pc), newVal :: stack)

                case NEWARRAY.opcode =>
                    newArray(ArrayType(as[NEWARRAY](instruction).elementType))

                case ANEWARRAY.opcode =>
                    newArray(ArrayType(as[ANEWARRAY](instruction).componentType))

                case MULTIANEWARRAY.opcode =>
                    val instr = as[MULTIANEWARRAY](instruction)
                    val (counts, rest) = stack.splitAt(instr.dimensions)
                    val newArray = NewArray(pc, counts, instr.arrayType)
                    val newVal = OperandVar(ComputationalTypeReference, rest)
                    statements(pc) = List(Assignment(pc, newVal, newArray))
                    schedule(pcOfNextInstruction(pc), newVal :: rest)

                case GOTO.opcode | GOTO_W.opcode =>
                    val targetPC = pc + as[GotoInstruction](instruction).branchoffset
                    statements(pc) = List(Goto(pc, targetPC))
                    schedule(targetPC, stack)

                case br.instructions.JSR.opcode | JSR_W.opcode =>
                    val targetPC = pc + as[JSRInstruction](instruction).branchoffset
                    val retVar = OperandVar(ComputationalTypeReturnAddress, stack)
                    statements(pc) = List(JSR(pc, targetPC))
                    schedule(targetPC, retVar :: stack)

                case RET.opcode =>
                    var successors = IntTrieSet.empty
                    cfg.bb(pc).successors foreach { successorNode =>
                        val successor = successorNode match {
                            case cn: CatchNode  => cn.handlerPC
                            case bb: BasicBlock => bb.startPC
                            case cfgNode =>
                                // in these cases something went terribly wrong...
                                val message = "the cfg has an unexpected shape: "+cfgNode
                                throw new AnalysisException(message)
                        }
                        successors += successor
                        schedule(successor, stack)
                    }

                    statements(pc) = List(Ret(pc, successors))

                case NOP.opcode =>
                    statements(pc) = List(Nop(pc))
                    schedule(pcOfNextInstruction(pc), stack)

                case POP.opcode =>
                    val _ :: rest = stack
                    statements(pc) = List(Nop(pc))
                    schedule(pcOfNextInstruction(pc), rest)

                case POP2.opcode =>
                    statements(pc) = List(Nop(pc))
                    stack match {
                        case CTC1() :: _ :: rest =>
                            schedule(pcOfNextInstruction(pc), rest)
                        case _ :: rest =>
                            schedule(pcOfNextInstruction(pc), rest)
                        case _ => throw new MatchError(stack)
                    }

                case INSTANCEOF.opcode =>
                    val value1 :: rest = stack
                    val resultVar = OperandVar(ComputationalTypeInt, rest)
                    val tpe = as[INSTANCEOF](instruction).referenceType
                    val instanceOf = InstanceOf(pc, value1, tpe)
                    statements(pc) = List(Assignment(pc, resultVar, instanceOf))
                    schedule(pcOfNextInstruction(pc), resultVar :: rest)

                case CHECKCAST.opcode =>
                    val value1 :: rest = stack
                    val resultVar = OperandVar(ComputationalTypeReference, rest)
                    val targetType = as[CHECKCAST](instruction).referenceType
                    val checkcast = Checkcast(pc, value1, targetType)
                    statements(pc) = List(checkcast)
                    schedule(pcOfNextInstruction(pc), resultVar :: rest)

                case MONITORENTER.opcode =>
                    val objRef :: rest = stack
                    statements(pc) = List(MonitorEnter(pc, objRef))
                    schedule(pcOfNextInstruction(pc), rest)

                case MONITOREXIT.opcode =>
                    val objRef :: rest = stack
                    statements(pc) = List(MonitorExit(pc, objRef))
                    schedule(pcOfNextInstruction(pc), rest)

                case TABLESWITCH.opcode =>
                    val index :: rest = stack
                    val tableSwitch = as[TABLESWITCH](instruction)
                    val defaultTarget = pc + tableSwitch.defaultOffset
                    var caseValue = tableSwitch.low
                    val npairs = tableSwitch.jumpOffsets.map[IntIntPair /*(Int, PC)*/ ] { jo =>
                        val caseTarget = pc + jo
                        val npair = IntIntPair(caseValue, caseTarget)
                        schedule(caseTarget, rest)
                        caseValue += 1
                        npair
                    }
                    schedule(defaultTarget, rest)
                    statements(pc) = List(Switch(pc, defaultTarget, index, npairs))

                case LOOKUPSWITCH.opcode =>
                    val index :: rest = stack
                    val lookupSwitch = as[LOOKUPSWITCH](instruction)
                    val defaultTarget = pc + lookupSwitch.defaultOffset
                    val npairs = lookupSwitch.npairs.map[IntIntPair /*(Int, PC)*/ ] { npair =>
                        val IntIntPair(caseValue, branchOffset) = npair
                        val caseTarget = pc + branchOffset
                        schedule(caseTarget, rest)
                        IntIntPair(caseValue, caseTarget)
                    }
                    schedule(defaultTarget, rest)
                    statements(pc) = List(Switch(pc, defaultTarget, index, npairs))

                case DUP.opcode =>
                    statements(pc) = List(Nop(pc))
                    schedule(pcOfNextInstruction(pc), stack.head :: stack)

                case DUP_X1.opcode =>
                    val val1 :: val2 :: rest = stack
                    statements(pc) = List(Nop(pc))
                    schedule(pcOfNextInstruction(pc), val1 :: val2 :: val1 :: rest)

                case DUP_X2.opcode =>
                    statements(pc) = List(Nop(pc))
                    stack match {
                        case v1 :: (v2 @ CTC1()) :: v3 :: rest =>
                            schedule(pcOfNextInstruction(pc), v1 :: v2 :: v3 :: v1 :: rest)
                        case v1 :: v2 :: rest =>
                            schedule(pcOfNextInstruction(pc), v1 :: v2 :: v1 :: rest)
                        case _ => throw new MatchError(stack)
                    }

                case DUP2.opcode =>
                    statements(pc) = List(Nop(pc))
                    stack match {
                        case (v1 @ CTC1()) :: v2 :: rest =>
                            schedule(pcOfNextInstruction(pc), v1 :: v2 :: v1 :: v2 :: rest)
                        case v1 :: rest =>
                            schedule(pcOfNextInstruction(pc), v1 :: v1 :: rest)
                        case _ => throw new MatchError(stack)
                    }

                case DUP2_X1.opcode =>
                    statements(pc) = List(Nop(pc))
                    stack match {
                        case (v1 @ CTC1()) :: v2 :: v3 :: rest =>
                            schedule(pcOfNextInstruction(pc), v1 :: v2 :: v3 :: v1 :: v2 :: rest)
                        case v1 :: v2 :: rest =>
                            schedule(pcOfNextInstruction(pc), v1 :: v2 :: v1 :: rest)
                        case _ => throw new MatchError(stack)
                    }

                case DUP2_X2.opcode =>
                    statements(pc) = List(Nop(pc))
                    stack match {
                        case (v1 @ CTC1()) :: (v2 @ CTC1()) :: (v3 @ CTC1()) :: v4 :: rest =>
                            val newStack = v1 :: v2 :: v3 :: v4 :: v1 :: v2 :: rest
                            schedule(pcOfNextInstruction(pc), newStack)
                        case (v1 @ CTC2()) :: (v2 @ CTC1()) :: (v3 @ CTC1()) :: rest =>
                            schedule(pcOfNextInstruction(pc), v1 :: v2 :: v3 :: v1 :: rest)
                        case (v1 @ CTC1()) :: (v2 @ CTC1()) :: (v3 @ CTC2()) :: rest =>
                            schedule(pcOfNextInstruction(pc), v1 :: v2 :: v3 :: v1 :: v2 :: rest)
                        case (v1 /*@ CTC2()*/ ) :: (v2 /*@ CTC1()*/ ) :: rest =>
                            schedule(pcOfNextInstruction(pc), v1 :: v2 :: v1 :: rest)
                        case _ => throw new MatchError(stack)
                    }

                case D2F.opcode | I2F.opcode | L2F.opcode => primitiveCastOperation(FloatType)
                case D2I.opcode | F2I.opcode | L2I.opcode => primitiveCastOperation(IntegerType)
                case D2L.opcode | I2L.opcode | F2L.opcode => primitiveCastOperation(LongType)
                case F2D.opcode | I2D.opcode | L2D.opcode => primitiveCastOperation(DoubleType)
                case I2C.opcode                           => primitiveCastOperation(CharType)
                case I2B.opcode                           => primitiveCastOperation(ByteType)
                case I2S.opcode                           => primitiveCastOperation(ShortType)

                case ATHROW.opcode =>
                    statements(pc) = List(Throw(pc, stack.head))

                case WIDE.opcode =>
                    statements(pc) = List(Nop(pc))
                    schedule(pcOfNextInstruction(pc), stack)

                case opcode => throw BytecodeProcessingFailedException(s"unknown opcode: $opcode")
            }
        }

        // Now we have to remap the target pcs to create the final statements array.
        // However, before we can do that we first add the register initialization
        // statements.
        var index = 0
        var registerIndex = 0
        val finalStatements = new ArrayBuffer[Stmt[IdBasedVar]](codeSize)
        val descriptor = method.descriptor
        val isStatic = method.isStatic
        val parameterTypes = descriptor.parameterTypes
        val paramsCount = descriptor.parametersCount + 1
        val tacParams = new Array[Param](paramsCount)

        if (!isStatic) {
            val targetVar = RegisterVar(ComputationalTypeReference, 0)
            val sourceParam = Param(ComputationalTypeReference, "this")
            tacParams(0) = sourceParam
            finalStatements += Assignment[IdBasedVar](-1, targetVar, sourceParam)
            index += 1
            registerIndex += 1
        }
        var tacIndex = 1
        parameterTypes foreach { parameterType =>
            val varName = code.localVariable(0, registerIndex).map(_.name).getOrElse("p_"+tacIndex)
            val cTpe = parameterType.computationalType
            val targetVar = RegisterVar(cTpe, registerIndex)
            val sourceParam = Param(cTpe, varName)
            tacParams(tacIndex) = sourceParam
            finalStatements += Assignment[IdBasedVar](-1, targetVar, sourceParam)
            index += 1
            tacIndex += 1
            registerIndex += cTpe.operandSize
        }

        var currentPC = 0
        val pcToIndex = new Array[Int](codeSize + 1 /* +1 for try blocks which include the last inst. */ )
        while (currentPC < codeSize) {
            val currentStatements = statements(currentPC)
            if (currentStatements ne null) {
                for (stmt <- currentStatements) {
                    finalStatements += stmt
                    if (pcToIndex(currentPC) == 0 /* <=> no mapping so far; we don't care about the remapping of 0 to 0... */ )
                        pcToIndex(currentPC) = index
                    index += 1
                }
            } else {
                // Required by subsequent transformations to identify that some pcs
                // are related to dead code!
                pcToIndex(currentPC) = Int.MinValue
            }
            currentPC += 1
        }

        // add the artificial lastPC + 1 instruction to enable the mapping of exception handlers
        pcToIndex(currentPC /* == codeSize +1 */ ) = index

        // HERE, we accept that the boundaries of the exception handlers may not cover
        // all instructions which conceptually belong in the try block if the additional
        // instruction cannot throw exceptions. I.e., if the try block just encompassed a
        // single instruction and that instruction was transformed such that we have multiple
        // instructions now. In such a case (e.g., the rewriting of swap...) the additional
        // instructions will never cause any exceptions.
        finalStatements foreach { stmt =>
            stmt.remapIndexes(pcToIndex, _ => false /*CaughtException is not used by TACNaive*/ )
        }
        val tacCode = finalStatements.toArray
        val tacCFG = cfg.mapPCsToIndexes[Stmt[IdBasedVar], TACStmts[IdBasedVar]](
            TACStmts(tacCode),
            pcToIndex,
            i => i,
            lastIndex = index - 1
        )
        def getStartAndEndIndex(
            oldEH:      ExceptionHandler,
            newIndexes: Array[Int]
        ): (Int, Int) = {
            val oldStartPC = oldEH.startPC
            val newStartIndex = newIndexes(oldStartPC)
            var newEndIndex = newIndexes(oldEH.endPC)
            // In some code (in particular groovy related code), we have found code
            // where the end of the try block is unreachable. I.e., no control flow path
            // exists that will reach the instruction... and – after removing the dead
            // instructions the exception handler collapses
            if (newEndIndex == Int.MinValue) {
                var lastPC = oldEH.endPC
                while (newEndIndex <= 0 && lastPC >= oldStartPC) {
                    lastPC -= 1
                    newEndIndex = newIndexes(lastPC)
                }
                if (newEndIndex == newStartIndex) {
                    newEndIndex += 1
                }
            }
            assert(newStartIndex < newEndIndex, s"old: $oldEH => [$newStartIndex,$newEndIndex]")
            (newStartIndex, newEndIndex)
        }

        /*
          * Updates the exception handlers by adjusting the start, end and handler index (pc).
          *
          * This method can only be used in simple cases where the order of instructions remains
          * the same and the start and end still map to valid exception handlers -
          * deleting/adding instructions is supported.
          *
          * @param exceptionHandlers The code's exception handlers.
          * @param newIndexes A map that contains for each previous index the new index
          *                   that should be used.
          * @return The new exception handler.
          */
        def updateExceptionHandlers(
            exceptionHandlers: ExceptionHandlers,
            newIndexes:        Array[Int]
        ): ExceptionHandlers = {
            exceptionHandlers map { old =>
                // Recall, that the endPC is not inclusive and - therefore - if the last instruction is
                // included in the handler block, the endPC is equal to `(pc of last instruction) +
                // instruction.size`; however, this is already handled by the caller!
                val (newStartIndex, newEndIndex) = getStartAndEndIndex(old, newIndexes)

                val newEH = old.copy(
                    startPC = newStartIndex,
                    endPC = newEndIndex,
                    handlerPC = newIndexes(old.handlerPC)
                )
                assert(
                    newEH.startPC <= newEH.endPC,
                    s"startPC=${old.startPC} => ${newEH.startPC};endPC=${old.endPC} => ${newEH.endPC}"
                )
                newEH
            }
        }

        val tacEHs = updateExceptionHandlers(code.exceptionHandlers, pcToIndex)

        val taCodeParams = new Parameters(tacParams)
        if (optimizations.nonEmpty) {
            val initialTAC = new NaiveTACode(taCodeParams, tacCode, pcToIndex, tacCFG, tacEHs)
            val base = TACOptimizationResult[Param, IdBasedVar, NaiveTACode[Param]](initialTAC, wasTransformed = false)
            optimizations.foldLeft(base)((tac, optimization) => optimization(tac)).code
        } else {
            new NaiveTACode(taCodeParams, tacCode, pcToIndex, tacCFG, tacEHs)
        }
    }

}
