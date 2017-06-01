/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
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

import scala.annotation.switch
import scala.collection.mutable.BitSet
import org.opalj.bytecode.BytecodeProcessingFailedException
import org.opalj.br._
import org.opalj.br.instructions._
import org.opalj.br.Method
import org.opalj.br.ClassHierarchy
import org.opalj.br.cfg.CFG
import org.opalj.ai.AIResult
import org.opalj.ai.domain.RecordDefUse

/**
 * Factory to convert the bytecode of a method into a three address representation using the
 * results of an abstract interpretation of the method.
 *
 * @author Michael Eichberg
 */
object TACAI {

    /**
     * Converts the bytecode of a method into a three address representation using
     * the result of an abstract interpretation.
     *
     * @param   method A method with a body. I.e., a non-native, non-abstract method.
     * @param   aiResult The result of the abstract interpretation of the respective method.
     * @param   optimizations The transformations that should be executed (NoOptimizations
     *          is always possible).
     * @return  The array with the generated statements.
     */
    def apply(
        method:         Method,
        classHierarchy: ClassHierarchy,
        aiResult:       AIResult { val domain: RecordDefUse },
        optimizations:  List[TACOptimization]
    ): (Array[Stmt], CFG) = {

        import BinaryArithmeticOperators._
        import RelationalOperators._
        import UnaryArithmeticOperators._

        val code = method.body.get
        import code.pcOfNextInstruction
        val instructions: Array[Instruction] = code.instructions
        val codeSize: Int = instructions.length
        val domain: aiResult.domain.type = aiResult.domain
        val wasExecuted: BitSet = new BitSet(codeSize) ++= aiResult.evaluated
        val cfg: CFG = domain.bbCFG
        val operandsArray: aiResult.domain.OperandsArray = aiResult.operandsArray
        val localsArray: aiResult.domain.LocalsArray = aiResult.localsArray

        // We already have the def-use information directly available, hence, for
        // instructions such as swap and dup, which do not create "relevant"
        // uses, we do not have to create multiple instructions, therefore, we
        // can directly create the "final list" of statements (which will include nops
        // for all useless instructions).

        val statements = new Array[Stmt](codeSize)
        val pcToIndex = new Array[Int](codeSize)

        var pc: PC = 0
        var index: Int = 0

        do {
            val nextPC = pcOfNextInstruction(pc)
            val instruction = instructions(pc)
            val opcode = instruction.opcode

            def addStmt(stmt: Stmt): Unit = {
                statements(index) = stmt
                pcToIndex(pc) = index
                index += 1
            }

            def operandUse(index: Int): UVar[aiResult.domain.DomainValue] = {
                val operands = operandsArray(pc)
                // get the definition site; recall: negative pcs refer to parameters
                val defSites = domain.operandOrigin(pc, index)
                UVar(aiResult.domain)(operands(index), defSites)
            }

            def registerUse(index: Int): UVar[aiResult.domain.DomainValue] = {
                val locals = localsArray(pc)
                // get the definition site; recall: negative pcs refer to parameters
                val defSites = domain.localOrigin(pc, index)
                UVar(aiResult.domain)(locals(index), defSites)
            }

            /**
             * Creates a local var using the current pc and the type
             * information from the domain value.
             */
            def initLocalVal(
                pc:   PC,
                v:    aiResult.domain.DomainValue,
                expr: Expr
            ): Unit = {
                val uses = domain.usedBy(pc)
                if (uses ne null) {
                    val localVal = DVar(aiResult.domain)(pc, v, uses)
                    addStmt(Assignment(pc, localVal, expr))
                } else {
                    // TODO Check is expr has any potential side effect... if not filter!
                    addStmt(ExprStmt(pc, expr))
                }
            }

            def arrayLoad(): Unit = {
                val index = operandUse(0)
                val arrayRef = operandUse(1)
                // to get the precise type we take a look at the next instruction's
                // top operand value
                val source = ArrayLoad(pc, index, arrayRef)
                initLocalVal(pc, operandsArray(nextPC).head, source)
            }

            def binaryArithmeticOperation(operator: BinaryArithmeticOperator): Unit = {
                val value2 = operandUse(0)
                val value1 = operandUse(1)
                val cTpe = operandsArray(nextPC).head.computationalType
                val binExpr = BinaryExpr(pc, cTpe, operator, value1, value2)
                initLocalVal(pc, operandsArray(nextPC).head, binExpr)
            }

            def prefixArithmeticOperation(operator: UnaryArithmeticOperator): Unit = {
                val value = operandUse(0)
                val cTpe = operandsArray(nextPC).head.computationalType
                val preExpr = PrefixExpr(pc, cTpe, operator, value)
                initLocalVal(pc, operandsArray(nextPC).head, preExpr)
            }

            def primitiveCastOperation(targetTpe: BaseType): Unit = {
                val value = operandUse(0)
                val castExpr = PrimitiveTypecastExpr(pc, targetTpe, value)
                initLocalVal(pc, operandsArray(nextPC).head, castExpr)
            }

            def newArray(arrayType: ArrayType): Unit = {
                val count = operandUse(0)
                val newArray = NewArray(pc, List(count), arrayType)
                initLocalVal(pc, operandsArray(nextPC).head, newArray)
            }

            def loadConstant(instr: LoadConstantInstruction[_]): Unit = {
                instr match {
                    case LDCInt(value) ⇒
                        initLocalVal(pc, operandsArray(nextPC).head, IntConst(pc, value))

                    case LDCFloat(value) ⇒
                        initLocalVal(pc, operandsArray(nextPC).head, FloatConst(pc, value))

                    case LDCClass(value) ⇒
                        initLocalVal(pc, operandsArray(nextPC).head, ClassConst(pc, value))

                    case LDCString(value) ⇒
                        initLocalVal(pc, operandsArray(nextPC).head, StringConst(pc, value))

                    case LDCMethodHandle(value) ⇒
                        initLocalVal(pc, operandsArray(nextPC).head, MethodHandleConst(pc, value))

                    case LDCMethodType(value) ⇒
                        initLocalVal(pc, operandsArray(nextPC).head, MethodTypeConst(pc, value))

                    case LoadDouble(value) ⇒
                        initLocalVal(pc, operandsArray(nextPC).head, DoubleConst(pc, value))

                    case LoadLong(value) ⇒
                        initLocalVal(pc, operandsArray(nextPC).head, LongConst(pc, value))

                    case _ ⇒
                        val message = s"unexpected constant $instr"
                        throw BytecodeProcessingFailedException(message)
                }
            }

            def compareValues(op: RelationalOperator): Unit = {
                val value2 = operandUse(0)
                val value1 = operandUse(1)
                val compare = Compare(pc, value1, op, value2)
                initLocalVal(pc, operandsArray(nextPC).head, compare)
            }

            def addNOP(): Unit = {
                // TODO Don't add if we don't have to (per basic block, we currently need at least one instruction, because we keep the existing CFG.)
                addStmt(Nop(pc))
            }

            def as[T <: Instruction](i: Instruction): T = i.asInstanceOf[T]

            (opcode: @switch) match {
                case ALOAD_0.opcode | ALOAD_1.opcode | ALOAD_2.opcode | ALOAD_3.opcode |
                    ALOAD.opcode |
                    ASTORE_0.opcode | ASTORE_1.opcode | ASTORE_2.opcode | ASTORE_3.opcode |
                    ASTORE.opcode |
                    ILOAD_0.opcode | ILOAD_1.opcode | ILOAD_2.opcode | ILOAD_3.opcode |
                    ILOAD.opcode |
                    ISTORE_0.opcode | ISTORE_1.opcode | ISTORE_2.opcode | ISTORE_3.opcode |
                    ISTORE.opcode |
                    DLOAD_0.opcode | DLOAD_1.opcode | DLOAD_2.opcode | DLOAD_3.opcode |
                    DLOAD.opcode |
                    DSTORE_0.opcode | DSTORE_1.opcode | DSTORE_2.opcode | DSTORE_3.opcode |
                    DSTORE.opcode |
                    FLOAD_0.opcode | FLOAD_1.opcode | FLOAD_2.opcode | FLOAD_3.opcode |
                    FLOAD.opcode |
                    FSTORE_0.opcode | FSTORE_1.opcode | FSTORE_2.opcode | FSTORE_3.opcode |
                    FSTORE.opcode |
                    LLOAD_0.opcode | LLOAD_1.opcode | LLOAD_2.opcode | LLOAD_3.opcode |
                    LLOAD.opcode |
                    LSTORE_0.opcode | LSTORE_1.opcode | LSTORE_2.opcode | LSTORE_3.opcode |
                    LSTORE.opcode ⇒
                    addNOP()

                case IRETURN.opcode | LRETURN.opcode | FRETURN.opcode | DRETURN.opcode |
                    ARETURN.opcode ⇒
                    addStmt(ReturnValue(pc, operandUse(0)))

                case RETURN.opcode ⇒ addStmt(Return(pc))

                case AALOAD.opcode |
                    DALOAD.opcode | FALOAD.opcode | LALOAD.opcode |
                    IALOAD.opcode | SALOAD.opcode | CALOAD.opcode |
                    BALOAD.opcode ⇒ arrayLoad()

                case AASTORE.opcode | DASTORE.opcode |
                    FASTORE.opcode | IASTORE.opcode |
                    LASTORE.opcode | SASTORE.opcode |
                    BASTORE.opcode | CASTORE.opcode ⇒
                    val operandVar = operandUse(0)
                    val index = operandUse(1)
                    val arrayRef = operandUse(2)
                    addStmt(ArrayStore(pc, arrayRef, index, operandVar))

                case ARRAYLENGTH.opcode ⇒
                    val arrayRef = operandUse(0)
                    val lengthExpr = ArrayLength(pc, arrayRef)
                    initLocalVal(pc, operandsArray(nextPC).head, lengthExpr)

                case BIPUSH.opcode | SIPUSH.opcode ⇒
                    val value = as[LoadConstantInstruction[Int]](instruction).value
                    initLocalVal(pc, operandsArray(nextPC).head, IntConst(pc, value))

                case IF_ICMPEQ.opcode | IF_ICMPNE.opcode |
                    IF_ICMPLT.opcode | IF_ICMPLE.opcode |
                    IF_ICMPGT.opcode | IF_ICMPGE.opcode ⇒
                    val ifInstr = as[IFICMPInstruction](instruction)
                    val value2 = operandUse(0)
                    val value1 = operandUse(1)
                    val targetPC = pc + ifInstr.branchoffset
                    addStmt(If(pc, value1, ifInstr.condition, value2, targetPC))

                case IFEQ.opcode | IFNE.opcode |
                    IFLT.opcode | IFLE.opcode |
                    IFGT.opcode | IFGE.opcode ⇒
                    val IF0Instruction(condition, branchoffset) = instruction
                    val value = operandUse(0)
                    // let's calculate the final address
                    val targetPC = pc + branchoffset
                    val cmpVal = IntConst(ai.ValueOriginForVMLevelValue(pc), 0)
                    addStmt(If(pc, value, condition, cmpVal, targetPC))

                case IF_ACMPEQ.opcode | IF_ACMPNE.opcode ⇒
                    val IFACMPInstruction(condition, branchoffset) = instruction
                    val value2 = operandUse(0)
                    val value1 = operandUse(1)
                    // let's calculate the final address
                    val targetPC = pc + branchoffset
                    addStmt(If(pc, value1, condition, value2, targetPC))

                case IFNONNULL.opcode | IFNULL.opcode ⇒
                    val IFXNullInstruction(condition, branchoffset) = instruction
                    val value = operandUse(0)
                    val targetPC = pc + branchoffset
                    val cmpVal = NullExpr(ai.ValueOriginForVMLevelValue(-pc))
                    addStmt(If(pc, value, condition, cmpVal, targetPC))

                case DCMPG.opcode | FCMPG.opcode ⇒ compareValues(CMPG)
                case DCMPL.opcode | FCMPL.opcode ⇒ compareValues(CMPL)
                case LCMP.opcode                 ⇒ compareValues(CMP)

                case SWAP.opcode                 ⇒ addNOP()

                case DADD.opcode | FADD.opcode | IADD.opcode | LADD.opcode ⇒
                    binaryArithmeticOperation(Add)
                case DDIV.opcode | FDIV.opcode | IDIV.opcode | LDIV.opcode ⇒
                    binaryArithmeticOperation(Divide)
                case DMUL.opcode | FMUL.opcode | IMUL.opcode | LMUL.opcode ⇒
                    binaryArithmeticOperation(Multiply)
                case DREM.opcode | FREM.opcode | IREM.opcode | LREM.opcode ⇒
                    binaryArithmeticOperation(Modulo)
                case DSUB.opcode | FSUB.opcode | ISUB.opcode | LSUB.opcode ⇒
                    binaryArithmeticOperation(Subtract)

                case DNEG.opcode | FNEG.opcode | INEG.opcode | LNEG.opcode ⇒
                    prefixArithmeticOperation(Negate)

                case IINC.opcode ⇒
                    val IINC(index, const) = instruction
                    val indexReg = registerUse(index)
                    val incVal = IntConst(pc, const)
                    val iinc = BinaryExpr(pc, ComputationalTypeInt, Add, indexReg, incVal)
                    initLocalVal(pc, operandsArray(nextPC).head, iinc)

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
                    val IConstInstruction(value) = instruction
                    initLocalVal(pc, operandsArray(nextPC).head, IntConst(pc, value))

                case ACONST_NULL.opcode ⇒
                    initLocalVal(pc, operandsArray(nextPC).head, NullExpr(pc))

                case DCONST_0.opcode | DCONST_1.opcode ⇒
                    val value = as[LoadConstantInstruction[Double]](instruction).value
                    initLocalVal(pc, operandsArray(nextPC).head, DoubleConst(pc, value))

                case FCONST_0.opcode | FCONST_1.opcode | FCONST_2.opcode ⇒
                    val value = as[LoadConstantInstruction[Float]](instruction).value
                    initLocalVal(pc, operandsArray(nextPC).head, FloatConst(pc, value))

                case LCONST_0.opcode | LCONST_1.opcode ⇒
                    val value = as[LoadConstantInstruction[Long]](instruction).value
                    initLocalVal(pc, operandsArray(nextPC).head, LongConst(pc, value))

                case LDC.opcode | LDC_W.opcode | LDC2_W.opcode ⇒
                    loadConstant(as[LoadConstantInstruction[_]](instruction))

                case INVOKEINTERFACE.opcode |
                    INVOKESPECIAL.opcode |
                    INVOKEVIRTUAL.opcode ⇒
                    val call @ MethodInvocationInstruction(declaringClass, isInterface, name, methodDescriptor) = instruction
                    val parametersCount = methodDescriptor.parametersCount
                    val params = (0 until parametersCount).map(i ⇒ operandUse(i))(Seq.canBuildFrom)
                    val receiver = operandUse(parametersCount) // this is the self reference
                    val returnType = methodDescriptor.returnType
                    if (returnType.isVoidType) {
                        val stmtFactory =
                            if (call.isVirtualMethodCall)
                                VirtualMethodCall.apply _
                            else
                                NonVirtualMethodCall.apply _
                        addStmt(stmtFactory(
                            pc,
                            declaringClass, isInterface, name, methodDescriptor,
                            receiver,
                            params
                        ))
                    } else {
                        val exprFactory =
                            if (call.isVirtualMethodCall)
                                VirtualFunctionCall.apply _
                            else
                                NonVirtualFunctionCall.apply _
                        val expr =
                            exprFactory(
                                pc,
                                declaringClass, isInterface, name, methodDescriptor,
                                receiver,
                                params
                            )
                        initLocalVal(pc, operandsArray(nextPC).head, expr)
                    }

                case INVOKESTATIC.opcode ⇒
                    val INVOKESTATIC(declaringClass, isInterface, name, methodDescriptor) = instruction
                    val parametersCount = methodDescriptor.parametersCount
                    val params = (0 until parametersCount).map(i ⇒ operandUse(i))(Seq.canBuildFrom)
                    val returnType = methodDescriptor.returnType
                    if (returnType.isVoidType) {
                        addStmt(
                            StaticMethodCall(
                                pc,
                                declaringClass, isInterface, name, methodDescriptor,
                                params
                            )
                        )
                    } else {
                        val expr =
                            StaticFunctionCall(
                                pc,
                                declaringClass, isInterface, name, methodDescriptor,
                                params
                            )
                        initLocalVal(pc, operandsArray(nextPC).head, expr)
                    }

                case INVOKEDYNAMIC.opcode ⇒
                    /*
                    val INVOKEDYNAMIC(bootstrapMethod, name, methodDescriptor) = instruction
                    val parametersCount = methodDescriptor.parametersCount
                    val params = (0 until parametersCount).map(i ⇒ operandUse(i))(Seq.canBuildFrom)
                    val returnType = methodDescriptor.returnType
                    val expr = Invokedynamic(pc, bootstrapMethod, name, methodDescriptor, params)
                    returnType match {
                        case VoidType => addStmt()
                    }
                    val newVar = {
                        if (returnType.isBaseType) SSAPrimVar(pc, returnType.asBaseType)
                        else SSARefVar(pc, returnType.asReferenceType)
                    }
                    addStmt(Assignment(pc, newVar, expr))
                    */
                    ???

                case PUTSTATIC.opcode ⇒
                    val PUTSTATIC(declaringClass, name, _ /*fieldType*/ ) = instruction
                    val value = operandUse(0)
                    val putStatic = PutStatic(pc, declaringClass, name, value)
                    addStmt(putStatic)

                case PUTFIELD.opcode ⇒
                    val PUTFIELD(declaringClass, name, _ /*fieldType*/ ) = instruction
                    val value = operandUse(0)
                    val objRef = operandUse(1)
                    val putField = PutField(pc, declaringClass, name, objRef, value)
                    if (wasExecuted(nextPC)) {
                        addStmt(putField)
                    } else {
                        // IMPROVE Encode information about the failing exception!
                        addStmt(FailingStatement(pc, putField))
                    }

                case GETSTATIC.opcode ⇒
                    val GETSTATIC(declaringClass, name, _ /*fieldType*/ ) = instruction
                    val getStatic = GetStatic(pc, declaringClass, name)
                    initLocalVal(pc, operandsArray(nextPC).head, getStatic)

                case GETFIELD.opcode ⇒
                    val GETFIELD(declaringClass, name, _ /*fieldType*/ ) = instruction
                    val getField = GetField(pc, declaringClass, name, operandUse(0))
                    if (wasExecuted(nextPC)) {
                        initLocalVal(pc, operandsArray(nextPC).head, getField)
                    } else {
                        // IMPROVE Encode information about the failing exception!
                        addStmt(FailingExpression(pc, getField))
                    }

                case NEW.opcode ⇒
                    val NEW(objectType) = instruction
                    val newObject = New(pc, objectType)
                    if (wasExecuted(nextPC)) {
                        initLocalVal(pc, operandsArray(nextPC).head, newObject)
                    } else {
                        // IMPROVE Encode information about the failing exception!
                        addStmt(FailingExpression(pc, newObject))
                    }

                case NEWARRAY.opcode ⇒
                    newArray(ArrayType(as[NEWARRAY](instruction).elementType))

                case ANEWARRAY.opcode ⇒
                    newArray(ArrayType(as[ANEWARRAY](instruction).componentType))

                case MULTIANEWARRAY.opcode ⇒
                    val MULTIANEWARRAY(arrayType, dimensions) = instruction
                    val counts = (0 until dimensions).map(d ⇒ operandUse(d))(Seq.canBuildFrom)
                    val newArray = NewArray(pc, counts, arrayType)
                    initLocalVal(pc, operandsArray(nextPC).head, newArray)

                case GOTO.opcode | GOTO_W.opcode ⇒
                    val GotoInstruction(branchoffset) = instruction
                    addStmt(Goto(pc, pc + branchoffset))

                case JSR.opcode | JSR_W.opcode ⇒
                    val JSRInstruction(branchoffset) = instruction
                    addStmt(JumpToSubroutine(pc, pc + branchoffset))
                case RET.opcode ⇒
                    addStmt(Ret(pc, cfg.successors(pc)))

                case NOP.opcode               ⇒ addNOP()
                case POP.opcode | POP2.opcode ⇒ addNOP()

                case INSTANCEOF.opcode ⇒
                    val value1 = operandUse(0)
                    val INSTANCEOF(tpe) = instruction
                    val instanceOf = InstanceOf(pc, value1, tpe)
                    initLocalVal(pc, operandsArray(nextPC).head, instanceOf)

                case CHECKCAST.opcode ⇒
                    val value1 = operandUse(0)
                    val CHECKCAST(targetType) = instruction
                    val checkcast = Checkcast(pc, value1, targetType)
                    if (wasExecuted(nextPC)) {
                        initLocalVal(pc, operandsArray(nextPC).head, checkcast)
                    } else {
                        addStmt(FailingExpression(pc, checkcast))
                    }

                case MONITORENTER.opcode ⇒ addStmt(MonitorEnter(pc, operandUse(0)))
                case MONITOREXIT.opcode  ⇒ addStmt(MonitorExit(pc, operandUse(0)))

                case TABLESWITCH.opcode ⇒
                    val index = operandUse(0)
                    val tableSwitch = as[TABLESWITCH](instruction)
                    val defaultTarget = pc + tableSwitch.defaultOffset
                    var caseValue = tableSwitch.low
                    val npairs = tableSwitch.jumpOffsets map { jo ⇒
                        val caseTarget = pc + jo
                        val npair = (caseValue, caseTarget)
                        caseValue += 1
                        npair
                    }
                    addStmt(Switch(pc, defaultTarget, index, npairs))

                case LOOKUPSWITCH.opcode ⇒
                    val index = operandUse(0)
                    val lookupSwitch = as[LOOKUPSWITCH](instruction)
                    val defaultTarget = pc + lookupSwitch.defaultOffset
                    val npairs = lookupSwitch.npairs.map { npair ⇒
                        val (caseValue, branchOffset) = npair
                        val caseTarget = pc + branchOffset
                        (caseValue, caseTarget)
                    }
                    addStmt(Switch(pc, defaultTarget, index, npairs))

                case DUP.opcode | DUP_X1.opcode | DUP_X2.opcode
                    | DUP2.opcode | DUP2_X1.opcode | DUP2_X2.opcode ⇒ addNOP()

                case D2F.opcode | I2F.opcode | L2F.opcode ⇒ primitiveCastOperation(FloatType)
                case D2I.opcode | F2I.opcode | L2I.opcode ⇒ primitiveCastOperation(IntegerType)
                case D2L.opcode | I2L.opcode | F2L.opcode ⇒ primitiveCastOperation(LongType)
                case F2D.opcode | I2D.opcode | L2D.opcode ⇒ primitiveCastOperation(DoubleType)
                case I2C.opcode                           ⇒ primitiveCastOperation(CharType)
                case I2B.opcode                           ⇒ primitiveCastOperation(ByteType)
                case I2S.opcode                           ⇒ primitiveCastOperation(ShortType)

                case ATHROW.opcode                        ⇒ addStmt(Throw(pc, operandUse(0)))

                case WIDE.opcode                          ⇒ addNOP()

                case opcode ⇒
                    throw BytecodeProcessingFailedException(s"unknown opcode: $opcode")
            }

            pc = nextPC
            while (pc < codeSize && !wasExecuted(pc)) pc = pcOfNextInstruction(pc)
        } while (pc < codeSize)

        var tacCode = {
            val tacCode = new Array[Stmt](index)
            var s = 0
            while (s < index) {
                val stmt = statements(s)
                stmt.remapIndexes(pcToIndex)
                tacCode(s) = stmt
                s += 1
            }
            tacCode
        }
        var tacCFG = cfg.mapPCsToIndexes(pcToIndex, lastIndex = index - 1)

        if (optimizations.nonEmpty) {
            val baseTAC = TACOptimizationResult(tacCode, tacCFG, wasTransformed = false)
            val result = optimizations.foldLeft(baseTAC) { (tac, optimization) ⇒ optimization(tac) }
            tacCode = result.code
            tacCFG = result.cfg
        }
        (tacCode, tacCFG)

    }

}
