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
import scala.collection.mutable.ArrayBuffer
import org.opalj.bytecode.BytecodeProcessingFailedException
import org.opalj.br._
import org.opalj.br.instructions._
import org.opalj.br.ClassHierarchy
import org.opalj.br.analyses.AnalysisException
import org.opalj.br.Method
import org.opalj.br.ClassHierarchy
import org.opalj.br.cfg.CFG
import org.opalj.ai.{AIResult, IsAReferenceValue, IsPrimitiveValue}
import org.opalj.collection.immutable.IntSet
/*import org.opalj.ai.IsAReferenceValue
import org.opalj.ai.IsPrimitiveValue
import org.opalj.ai.TypeUnknown
*/
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

        type ValueType = aiResult.domain.DomainValue
        import BinaryArithmeticOperators._
        import RelationalOperators._
        import UnaryArithmeticOperators._
        import classHierarchy.{joinReferenceTypesUntilSingleUpperBound ⇒ computeLeastCommonSuperType}

        val code = method.body.get
        import code.pcOfNextInstruction
        val instructions : Array[Instruction] = code.instructions
        val codeSize :Int = instructions.length
        val domain : aiResult.domain.type = aiResult.domain
        val wasExecuted : BitSet= new BitSet(codeSize) ++= aiResult.evaluated
        val cfg: CFG = domain.bbCFG
        val operandsArray : aiResult.domain.OperandsArray = aiResult.operandsArray

        // We already have the def-use information directly available, hence, for
        // instructions such as swap and dup, which do not create "relevant"
        // uses, we do not have to create multiple instructions, therefore, we
        // can directly create the "final list" of statements (which will include nops
        // for all useless instructions).

        val statements = new Array[Stmt](codeSize)

        var pc : PC = 0

        def addStmt(stmt: Stmt): Unit = {
            // TODO if the previous statement belongs to the same basic block as this one and is a NOP statement, we replace it by this one
            statements(pc) = stmt
        }

        do {
            val nextPC = pcOfNextInstruction(pc)
            val instruction = instructions(pc)
            val opcode = instruction.opcode
            val operands = operandsArray(pc)

            def operandUse(index: Int): UVar[ValueType] = {
                // 1. get the definition site
                // Recall: if the defSite is negative, we are using a parameter
                val defSites = domain.operandOrigin(pc, index)
                new UVar[ValueType](operands(index), defSites)
            }

            def registerUse(index: Int): DVar[ValueType] = {
                // 1. get the definition site
                // Recall: if the defSite is negative, we are using a parameter
                val defSite = domain.localOrigin(pc, index)

                // 2. get more precise information about the type etc.
                ???
            }

            /*
            def VarUse(vos: ValueOrigins, v: aiResult.domain.DomainValue): VarUse = {
                aiResult.domain.typeOfValue(v) match {
                    case refVal: IsAReferenceValue ⇒
                        val tpe = computeLeastCommonSuperType(refVal.upperTypeBound)
                        SSARefVar(pc, tpe, Some(refVal))

                    case primVal @ IsPrimitiveValue(tpe) ⇒
                        SSAPrimVar(pc, tpe, Some(primVal))

                    case TypeUnknown ⇒
                        throw new BytecodeProcessingFailedException(s"the type of $v is unknown")
                }
            }*/

            /**
             * Creates a local var using the current pc and the type
             * information from the domain value.
             */
            def domainValueBasedLocalVar(
                                            v: aiResult.domain.DomainValue
                                        ): DVar[ValueType] = {
                aiResult.domain.typeOfValue(v) match {
                    case refVal: IsAReferenceValue ⇒
                        val tpe = computeLeastCommonSuperType(refVal.upperTypeBound)
                        DUVar(pc, tpe, Some(refVal))

                    case primVal @ IsPrimitiveValue(tpe) ⇒
                        SSAPrimVar(pc, tpe, Some(primVal))

                    case TypeUnknown ⇒
                        throw new BytecodeProcessingFailedException(s"the type of $v is unknown")
                }
            }

            def arrayLoad(): Unit = {
                val index = operandUse(0)
                val arrayRef = operandUse(1)
                // to get the precise type we take a look at the next instruction's
                // top operand value
                val localVar = domainValueBasedLocalVar(operandsArray(nextPC).head)
                val source = ArrayLoad(pc, index, arrayRef)
                addStmt(Assignment(pc, localVar, source))
            }

            def binaryArithmeticOperation(operator: BinaryArithmeticOperator): Unit = {
                val value2 = operandUse(0)
                val value1 = operandUse(1)
                val localVar = domainValueBasedLocalVar(operandsArray(nextPC).head)
                val expr = BinaryExpr(pc, localVar.tpe.computationalType, operator, value1, value2)
                addStmt(Assignment(pc, localVar, expr))
            }

            def prefixArithmeticOperation(operator: UnaryArithmeticOperator): Unit = {
                val value = operandUse(0)
                val localVar = domainValueBasedLocalVar(operandsArray(nextPC).head)
                val expr = PrefixExpr(pc, localVar.tpe.computationalType, operator, value)
                addStmt(Assignment(pc, localVar, expr))
            }

            def primitiveCastOperation(): Unit = {
                val value = operandUse(0)
                val localVar = domainValueBasedLocalVar(operandsArray(nextPC).head)
                val castExpr = PrimitiveTypecastExpr(pc, localVar.tpe.asBaseType, value)
                addStmt(Assignment(pc, localVar, castExpr))
            }

            def newArray(arrayType: ArrayType): Unit = {
                val count = operandUse(0)
                val newArray = NewArray(pc, List(count), arrayType)
                val newVar = domainValueBasedLocalVar(operandsArray(nextPC).head)
                addStmt(Assignment(pc, newVar, newArray))
            }

            def loadConstant(instr: LoadConstantInstruction[_]): Unit = {
                instr match {
                    case LDCInt(value) ⇒
                        val newVar = SSAPrimVar(pc, IntegerType)
                        addStmt(Assignment(pc, newVar, IntConst(pc, value)))

                    case LDCFloat(value) ⇒
                        val newVar = SSAPrimVar(pc, FloatType)
                        val floatConst = FloatConst(pc, value)
                        addStmt(Assignment(pc, newVar, floatConst))

                    case LDCClass(value) ⇒
                        val newVar = SSARefVar(pc, ObjectType.Class)
                        addStmt(Assignment(pc, newVar, ClassConst(pc, value)))

                    case LDCString(value) ⇒
                        val newVar = SSARefVar(pc, ObjectType.String)
                        addStmt(Assignment(pc, newVar, StringConst(pc, value)))

                    case LDCMethodHandle(value) ⇒
                        val newVar = SSARefVar(pc, ObjectType.MethodHandle)
                        addStmt(Assignment(pc, newVar, MethodHandleConst(pc, value)))

                    case LDCMethodType(value) ⇒
                        val newVar = SSARefVar(pc, ObjectType.MethodType)
                        val methodTypeConst = MethodTypeConst(pc, value)
                        addStmt(Assignment(pc, newVar, methodTypeConst))

                    case LoadDouble(value) ⇒
                        val newVar = SSAPrimVar(pc, DoubleType)
                        addStmt(Assignment(pc, newVar, DoubleConst(pc, value)))

                    case LoadLong(value) ⇒
                        val newVar = SSAPrimVar(pc, LongType)
                        addStmt(Assignment(pc, newVar, LongConst(pc, value)))

                    case _ ⇒
                        val message = s"unexpected constant $instr"
                        throw BytecodeProcessingFailedException(message)
                }
            }

            def compareValues(op: RelationalOperator): Unit = {
                val value2 = operandUse(0)
                val value1 = operandUse(1)
                val result = domainValueBasedLocalVar(operandsArray(nextPC).head)
                val compare = Compare(pc, value1, op, value2)
                addStmt(Assignment(pc, result, compare))
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
                    val lengthVar = domainValueBasedLocalVar(operandsArray(nextPC).head)
                    val lengthExpr = ArrayLength(pc, arrayRef)
                    addStmt(Assignment(pc, lengthVar, lengthExpr))

                case BIPUSH.opcode | SIPUSH.opcode ⇒
                    val value = as[LoadConstantInstruction[Int]](instruction).value
                    val targetVar = SSAPrimVar(pc, IntegerType)
                    addStmt(Assignment(pc, targetVar, IntConst(pc, value)))

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
                    val ifInstr = as[IF0Instruction](instruction)
                    val value = operandUse(0)
                    // let's calculate the final address
                    val targetPC = pc + ifInstr.branchoffset
                    addStmt(If(pc, value, ifInstr.condition, IntConst(-pc, 0), targetPC))

                case IF_ACMPEQ.opcode | IF_ACMPNE.opcode ⇒
                    val ifInstr = as[IFACMPInstruction](instruction)
                    val value2 = operandUse(0)
                    val value1 = operandUse(1)
                    // let's calculate the final address
                    val targetPC = pc + ifInstr.branchoffset
                    addStmt(If(pc, value1, ifInstr.condition, value2, targetPC))

                case IFNONNULL.opcode | IFNULL.opcode ⇒
                    val ifInstr = as[IFXNullInstruction](instruction)
                    val value = operandUse(0)
                    val targetPC = pc + ifInstr.branchoffset
                    addStmt(If(pc, value, ifInstr.condition, NullExpr(-pc), targetPC))

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
                    val localVar = domainValueBasedLocalVar(operandsArray(nextPC).head)
                    addStmt(Assignment(pc, localVar, iinc))

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
                    val targetVar = SSAPrimVar(pc, IntegerType)
                    addStmt(Assignment(pc, targetVar, IntConst(pc, value)))

                case ACONST_NULL.opcode ⇒
                    val targetVar = SSARefVar(pc, ObjectType.Object /* TODO java.null ...*/ )
                    addStmt(Assignment(pc, targetVar, NullExpr(pc)))

                case DCONST_0.opcode | DCONST_1.opcode ⇒
                    val value = as[LoadConstantInstruction[Double]](instruction).value
                    val targetVar = SSAPrimVar(pc, DoubleType)
                    addStmt(Assignment(pc, targetVar, DoubleConst(pc, value)))

                case FCONST_0.opcode | FCONST_1.opcode | FCONST_2.opcode ⇒
                    val value = as[LoadConstantInstruction[Float]](instruction).value
                    val targetVar = SSAPrimVar(pc, FloatType)
                    addStmt(Assignment(pc, targetVar, FloatConst(pc, value)))

                case LCONST_0.opcode | LCONST_1.opcode ⇒
                    val value = as[LoadConstantInstruction[Long]](instruction).value
                    val targetVar = SSAPrimVar(pc, LongType)
                    addStmt(Assignment(pc, targetVar, LongConst(pc, value)))

                case LDC.opcode | LDC_W.opcode | LDC2_W.opcode ⇒
                    loadConstant(as[LoadConstantInstruction[_]](instruction))

                case INVOKEINTERFACE.opcode |
                    INVOKESPECIAL.opcode |
                    INVOKEVIRTUAL.opcode ⇒
                    val invoke = as[MethodInvocationInstruction](instruction)
                    val parametersCount = invoke.methodDescriptor.parametersCount
                    val params = (0 until parametersCount).map(i ⇒ operandUse(i))(Seq.canBuildFrom)
                    val receiver = operandUse(parametersCount) // this is the self reference
                    import invoke.{methodDescriptor, declaringClass, name}
                    val returnType = methodDescriptor.returnType
                    if (returnType.isVoidType) {
                        val stmtFactory =
                            if (invoke.isVirtualMethodCall)
                                VirtualMethodCall.apply _
                            else
                                NonVirtualMethodCall.apply _
                        addStmt(stmtFactory(
                            pc,
                            declaringClass, name, methodDescriptor,
                            receiver,
                            params
                        ))
                    } else {
                        val localVar = domainValueBasedLocalVar(operandsArray(nextPC).head)
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
                        addStmt(Assignment(pc, localVar, expr))
                    }

                case INVOKESTATIC.opcode ⇒
                    val invoke = as[INVOKESTATIC](instruction)
                    val parametersCount = invoke.methodDescriptor.parametersCount
                    val params = (0 until parametersCount).map(i ⇒ operandUse(i))(Seq.canBuildFrom)
                    import invoke.{declaringClass, methodDescriptor, name}
                    val returnType = methodDescriptor.returnType
                    if (returnType.isVoidType) {
                        addStmt(
                            StaticMethodCall(
                                pc,
                                declaringClass, name, methodDescriptor,
                                params
                            )
                        )
                    } else {
                        val newVar = domainValueBasedLocalVar(operandsArray(nextPC).head)
                        val expr =
                            StaticFunctionCall(
                                pc,
                                declaringClass, name, methodDescriptor,
                                params
                            )
                        addStmt(Assignment(pc, newVar, expr))
                    }

                case INVOKEDYNAMIC.opcode ⇒
                    val invoke = as[INVOKEDYNAMIC](instruction)
                    val parametersCount = invoke.methodDescriptor.parametersCount
                    val params = (0 until parametersCount).map(i ⇒ operandUse(i))(Seq.canBuildFrom)
                    val returnType = invoke.methodDescriptor.returnType
                    val bootstrapMethod = invoke.bootstrapMethod
                    val name = invoke.name
                    val methodDescriptor = invoke.methodDescriptor
                    val expr = Invokedynamic(pc, bootstrapMethod, name, methodDescriptor, params)
                    val newVar = {
                        if (returnType.isBaseType) SSAPrimVar(pc, returnType.asBaseType)
                        else SSARefVar(pc, returnType.asReferenceType)
                    }
                    addStmt(Assignment(pc, newVar, expr))

                case PUTSTATIC.opcode ⇒
                    val value = operandUse(0)
                    val PUTSTATIC = as[PUTSTATIC](instruction)
                    val putStatic = PutStatic(pc, PUTSTATIC.declaringClass, PUTSTATIC.name, value)
                    addStmt(putStatic)

                case PUTFIELD.opcode ⇒
                    val value = operandUse(0)
                    val objRef = operandUse(1)
                    val PUTFIELD = as[PUTFIELD](instruction)
                    val putField = PutField(pc, PUTFIELD.declaringClass, PUTFIELD.name, objRef, value)
                    addStmt(putField)

                case GETSTATIC.opcode ⇒
                    val GETSTATIC = as[GETSTATIC](instruction)
                    val getStatic = GetStatic(pc, GETSTATIC.declaringClass, GETSTATIC.name)
                    val newVar = domainValueBasedLocalVar(operandsArray(nextPC).head)
                    addStmt(Assignment(pc, newVar, getStatic))

                case GETFIELD.opcode ⇒
                    val objRef = operandUse(0)
                    val GETFIELD = as[GETFIELD](instruction)
                    val getField = GetField(pc, GETFIELD.declaringClass, GETFIELD.name, objRef)
                    val newVar = domainValueBasedLocalVar(operandsArray(nextPC).head)
                    addStmt(Assignment(pc, newVar, getField))

                case NEW.opcode ⇒
                    val instr = as[NEW](instruction)
                    val newVal = SSARefVar(pc, instr.objectType)
                    addStmt(Assignment(pc, newVal, New(pc, instr.objectType)))

                case NEWARRAY.opcode ⇒
                    newArray(ArrayType(as[NEWARRAY](instruction).elementType))

                case ANEWARRAY.opcode ⇒
                    newArray(ArrayType(as[ANEWARRAY](instruction).componentType))

                case MULTIANEWARRAY.opcode ⇒
                    val instr = as[MULTIANEWARRAY](instruction)
                    // TODO Do we need to reverse the list "counts"
                    val counts = (0 until instr.dimensions).map(d ⇒ operandUse(d))(Seq.canBuildFrom)
                    val newArray = NewArray(pc, counts, instr.componentType)
                    val newVal = domainValueBasedLocalVar(operandsArray(nextPC).head)
                    addStmt(Assignment(pc, newVal, newArray))

                case GOTO.opcode | GOTO_W.opcode ⇒
                    val targetPC = pc + as[GotoInstruction](instruction).branchoffset
                    addStmt(Goto(pc, targetPC))

                case JSR.opcode | JSR_W.opcode ⇒
                    val JSRInstruction(branchoffset) = instruction
                    addStmt(f
                case RET.opcode ⇒
                    val RET(lvIndex) = instruction
                    // the def sites are actually the JSR instructions ...
                    // However, in this case we explicitly encode the targets
                    val domain.TheReturnAddressValues() = registerUse(lvIndex)
                    var returnAddresses =  IntSet.empty // TODO extract the values
                    addStmt( Ret(pc, returnAddresses)

                case NOP.opcode               ⇒ addNOP()
                case POP.opcode | POP2.opcode ⇒ addNOP()

                case INSTANCEOF.opcode ⇒
                    val value1 = operandUse(0)
                    val resultVar = domainValueBasedLocalVar(operandsArray(nextPC).head)
                    val INSTANCEOF(tpe) = instruction
                    val instanceOf = InstanceOf(pc, value1, tpe)
                    addStmt(Assignment(pc, resultVar, instanceOf))

                case CHECKCAST.opcode ⇒
                    val value1 = operandUse(0)
                    val CHECKCAST(targetType) = instruction
                    val checkcast = Checkcast(pc, value1, targetType)
                    if (wasExecuted(nextPC)) {
                        val resultVar = domainValueBasedLocalVar(operandsArray(nextPC).head)
                        addStmt(Assignment(pc, resultVar, checkcast))
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

                case D2F.opcode | I2F.opcode | L2F.opcode ⇒ primitiveCastOperation()
                case D2I.opcode | F2I.opcode | L2I.opcode ⇒ primitiveCastOperation()
                case D2L.opcode | I2L.opcode | F2L.opcode ⇒ primitiveCastOperation()
                case F2D.opcode | I2D.opcode | L2D.opcode ⇒ primitiveCastOperation()
                case I2C.opcode                           ⇒ primitiveCastOperation()
                case I2B.opcode                           ⇒ primitiveCastOperation()
                case I2S.opcode                           ⇒ primitiveCastOperation()

                case ATHROW.opcode                        ⇒ addStmt(Throw(pc, operandUse(0)))

                case WIDE.opcode                          ⇒ addNOP()

                case opcode ⇒
                    throw BytecodeProcessingFailedException(s"unknown opcode: $opcode")
            }

            pc = nextPC
            while (pc < codeSize && !wasExecuted(pc)) pc = pcOfNextInstruction(pc)
        } while (pc < codeSize)

        var tacCFG = cfg
        var tacCode = statements
        if (optimizations.nonEmpty) {
            val baseTAC = TACOptimizationResult(tacCode, tacCFG, false)
            val result = optimizations.foldLeft(baseTAC) { (tac, optimization) ⇒ optimization(tac) }
            tacCode = result.code
            tacCFG = result.cfg
        }
        (tacCode, tacCFG)

    }

}
