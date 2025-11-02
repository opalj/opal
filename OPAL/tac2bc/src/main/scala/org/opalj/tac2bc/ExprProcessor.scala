/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac2bc

import scala.collection.mutable

import org.opalj.BinaryArithmeticOperators.Add
import org.opalj.BinaryArithmeticOperators.And
import org.opalj.BinaryArithmeticOperators.Divide
import org.opalj.BinaryArithmeticOperators.Modulo
import org.opalj.BinaryArithmeticOperators.Multiply
import org.opalj.BinaryArithmeticOperators.Or
import org.opalj.BinaryArithmeticOperators.ShiftLeft
import org.opalj.BinaryArithmeticOperators.ShiftRight
import org.opalj.BinaryArithmeticOperators.Subtract
import org.opalj.BinaryArithmeticOperators.UnsignedShiftRight
import org.opalj.BinaryArithmeticOperators.XOr
import org.opalj.RelationalOperators.CMP
import org.opalj.RelationalOperators.CMPG
import org.opalj.RelationalOperators.CMPL
import org.opalj.ba.CodeElement
import org.opalj.br.ArrayType
import org.opalj.br.BooleanType
import org.opalj.br.ByteType
import org.opalj.br.CharType
import org.opalj.br.ComputationalTypeDouble
import org.opalj.br.ComputationalTypeFloat
import org.opalj.br.ComputationalTypeInt
import org.opalj.br.ComputationalTypeLong
import org.opalj.br.ComputationalTypeReference
import org.opalj.br.DoubleType
import org.opalj.br.FieldType
import org.opalj.br.FloatType
import org.opalj.br.IntegerType
import org.opalj.br.LongType
import org.opalj.br.MethodDescriptor
import org.opalj.br.ClassType
import org.opalj.br.ReferenceType
import org.opalj.br.ShortType
import org.opalj.br.instructions._
import org.opalj.tac.ArrayLength
import org.opalj.tac.ArrayLoad
import org.opalj.tac.BinaryExpr
import org.opalj.tac.Call
import org.opalj.tac.ClassConst
import org.opalj.tac.Compare
import org.opalj.tac.Const
import org.opalj.tac.DoubleConst
import org.opalj.tac.DVar
import org.opalj.tac.DynamicConst
import org.opalj.tac.Expr
import org.opalj.tac.FloatConst
import org.opalj.tac.GetField
import org.opalj.tac.GetStatic
import org.opalj.tac.InstanceOf
import org.opalj.tac.IntConst
import org.opalj.tac.InvokedynamicFunctionCall
import org.opalj.tac.LongConst
import org.opalj.tac.MethodHandleConst
import org.opalj.tac.MethodTypeConst
import org.opalj.tac.New
import org.opalj.tac.NewArray
import org.opalj.tac.NonVirtualFunctionCall
import org.opalj.tac.NonVirtualMethodCall
import org.opalj.tac.NullExpr
import org.opalj.tac.PrefixExpr
import org.opalj.tac.PrimitiveTypecastExpr
import org.opalj.tac.StaticFunctionCall
import org.opalj.tac.StaticMethodCall
import org.opalj.tac.StringConst
import org.opalj.tac.UVar
import org.opalj.tac.V
import org.opalj.tac.Var
import org.opalj.tac.VirtualFunctionCall
import org.opalj.tac.VirtualMethodCall
import org.opalj.value.IsSReferenceValue
import org.opalj.value.ValueInformation

object ExprProcessor {

    /**
     * Generates Java bytecode instructions for Expr.
     *
     * @param expr the Expression to be converted into InstructionElements
     * @param tacToLVIndex map that holds information for Local Variable Indices
     * @param code list where bytecode instructions should be added
     * @param tacContext is responsible for translating TAC to bytecode in reverse order.
     * @param delayStmtVisit if true, delays emitting the label and marking the statement as visited until later.
     * @param nestedStmt indicates nested expression and preventing unnecessary variable stores during translation.
     */
    def processExpression(
        expr:         Expr[V],
        tacToLVIndex: Map[Int, Int],
        code:         mutable.ListBuffer[CodeElement[Nothing]],
        tacContext:   Tac2BcContext,
        stmtIndex:      Int,
        delayStmtVisit: Boolean = false,
        nestedStmt:     Boolean = false
    ): Unit = {
        expr match {
            case getField: GetField[V]     => processGetField(getField, tacToLVIndex, code, tacContext, stmtIndex)
            case getStatic: GetStatic      => processGetStatic(getStatic, code)
            case binaryExpr: BinaryExpr[V] => processBinaryExpr(binaryExpr, tacToLVIndex, code, tacContext, nestedStmt, stmtIndex)
            case callExpr: Call[V @unchecked] =>
                val call @ Call(declaringClass, isInterface, name, descriptor) = callExpr
                processCall(
                    call,
                    declaringClass,
                    isInterface,
                    name,
                    descriptor,
                    tacToLVIndex,
                    code,
                    tacContext,
                    stmtIndex
                )
            case newExpr: New => processNewExpr(newExpr.tpe, code)
            case primitiveTypecastExpr: PrimitiveTypecastExpr[V] =>
                processPrimitiveTypeCastExpr(primitiveTypecastExpr, tacToLVIndex, code, tacContext, stmtIndex)
            case arrayLength: ArrayLength[V] => processArrayLength(arrayLength, tacToLVIndex, code, tacContext, delayStmtVisit)
            case arrayLoadExpr: ArrayLoad[V] => processArrayLoad(arrayLoadExpr, tacToLVIndex, code, tacContext, delayStmtVisit, stmtIndex)
            case newArrayExpr: NewArray[V]   => processNewArray(newArrayExpr, tacToLVIndex, code, tacContext)
            case invokedynamicFunctionCall: InvokedynamicFunctionCall[V] =>
                processInvokedynamicFunctionCall(invokedynamicFunctionCall, tacToLVIndex, code, tacContext, stmtIndex)
            case compare: Compare[V]       => processCompare(compare, tacToLVIndex, code, tacContext)
            case prefixExpr: PrefixExpr[V] => processPrefixExpr(prefixExpr, tacToLVIndex, code, tacContext)
            case instanceOf: InstanceOf[V] => processInstanceOf(instanceOf, tacToLVIndex, code, stmtIndex, tacContext)
            case _ =>
                throw new UnsupportedOperationException("Unsupported expression type" + expr)
        }
    }

    def processInstanceOf(
        instanceOf:   InstanceOf[V],
        tacToLVIndex: Map[Int, Int],
        code:         mutable.ListBuffer[CodeElement[Nothing]],
        stmtIdx:      Int,
        tacContext:   Tac2BcContext
    ): Unit = {
        code += INSTANCEOF(instanceOf.cmpTpe)
        tacContext.emitStmt(instanceOf.value.asVar.definedBy.head, parentIdx = stmtIdx)
    }

    def processPrefixExpr(
        prefixExpr:   PrefixExpr[V],
        tacToLVIndex: Map[Int, Int],
        code:         mutable.ListBuffer[CodeElement[Nothing]],
        tacContext:   Tac2BcContext
    ): Unit = {
        // Determine the appropriate negation instruction based on the operand type
        code += {
            prefixExpr.operand.cTpe match {
                case ComputationalTypeInt    => INEG
                case ComputationalTypeLong   => LNEG
                case ComputationalTypeFloat  => FNEG
                case ComputationalTypeDouble => DNEG
                case _ =>
                    throw new UnsupportedOperationException(s"Unsupported type for negation: ${prefixExpr.operand.cTpe}")
            }
        }
        // Note that [[UnaryArithmeticOperators.Negate]] is the only UnaryArithmeticOperator used
        assert(prefixExpr.op eq UnaryArithmeticOperators.Negate)
        // Process the operand (the expression being negated)
        if (prefixExpr.operand.asVar.definedBy.head < 0)
            ExprProcessor.loadVariable(prefixExpr.operand.asVar, tacToLVIndex, code)
        else
            tacContext.emitStmt(prefixExpr.operand.asVar.definedBy.head)
    }

    def processCompare(
        compare:      Compare[V],
        tacToLVIndex: Map[Int, Int],
        code:         mutable.ListBuffer[CodeElement[Nothing]],
        tacContext:   Tac2BcContext
    ): Unit = {
        // Determine the appropriate comparison instruction
        code += {
            (compare.left.cTpe, compare.condition) match {
                case (ComputationalTypeLong, CMP)    => LCMP
                case (ComputationalTypeFloat, CMPG)  => FCMPG
                case (ComputationalTypeFloat, CMPL)  => FCMPL
                case (ComputationalTypeDouble, CMPG) => DCMPG
                case (ComputationalTypeDouble, CMPL) => DCMPL
                case _                               => throw new IllegalArgumentException("Unsupported comparison type")
            }
        }

        // Process the left expression
        tacContext.emitStmt(compare.left.asVar.definedBy.head)
        // Process the right expression
        tacContext.emitStmt(compare.right.asVar.definedBy.head)
    }

    def processInvokedynamicFunctionCall(
        invokedynamicFunctionCall: InvokedynamicFunctionCall[V],
        tacToLVIndex:              Map[Int, Int],
        code:                      mutable.ListBuffer[CodeElement[Nothing]],
        tacContext:                Tac2BcContext,
        stmtIdx:                   Int
    ): Unit = {
        code += DEFAULT_INVOKEDYNAMIC(
            invokedynamicFunctionCall.bootstrapMethod,
            invokedynamicFunctionCall.name,
            invokedynamicFunctionCall.descriptor
        )

        // Process each parameter
        for (param <- invokedynamicFunctionCall.params.reverse) {
            if(param.asVar.definedBy.head < 0) {
                ExprProcessor.loadVariable(param.asVar, tacToLVIndex, code)
            } else {
                tacContext.emitStmt(param.asVar.definedBy.iterator.min, parentIdx = stmtIdx)
            }
        }
    }

    def processNewArray(
        newArrayExpr: NewArray[V],
        tacToLVIndex: Map[Int, Int],
        code:         mutable.ListBuffer[CodeElement[Nothing]],
        tacContext:   Tac2BcContext
    ): Unit = {
        code += {
            if (newArrayExpr.counts.size > 1) {
                MULTIANEWARRAY(newArrayExpr.tpe, newArrayExpr.counts.size)
            } else if (newArrayExpr.tpe.componentType.isReferenceType) {
                ANEWARRAY(newArrayExpr.tpe.componentType.asReferenceType)
            } else {
                NEWARRAY(newArrayExpr.tpe.componentType.asBaseType.atype)
            }
        }

        // Process each parameter
        for (count <- newArrayExpr.counts)
            tacContext.emitStmt(count.asVar.definedBy.head)
    }

    def processArrayLoad(
        arrayLoadExpr:  ArrayLoad[V],
        tacToLVIndex:   Map[Int, Int],
        code:           mutable.ListBuffer[CodeElement[Nothing]],
        tacContext:     Tac2BcContext,
        delayStmtVisit: Boolean = false,
        stmtIdx:        Int
    ): Unit = {
        // Infer the element type from the array reference expression
        val elementType = inferElementType(arrayLoadExpr.arrayRef)
        code += {
            elementType match {
                case IntegerType      => IALOAD
                case LongType         => LALOAD
                case FloatType        => FALOAD
                case DoubleType       => DALOAD
                case ByteType         => BALOAD
                case BooleanType      => BALOAD // Boolean arrays are also accessed with BALOAD (see JVM Spec. newarray / baload)
                case CharType         => CALOAD
                case ShortType        => SALOAD
                case _: ReferenceType => AALOAD
            }
        }

        // Load the index onto the stack
        if (arrayLoadExpr.index.asVar.definedBy.head < 0) {
            ExprProcessor.loadVariable(arrayLoadExpr.index.asVar, tacToLVIndex, code)
        } else {
            tacContext.emitStmt(arrayLoadExpr.index.asVar.definedBy.head, delayStmtVisit = true, parentIdx = stmtIdx)
        }

        // Load the array reference onto the stack
        if (arrayLoadExpr.arrayRef.asVar.definedBy.head < 0)
            ExprProcessor.loadVariable(arrayLoadExpr.arrayRef.asVar, tacToLVIndex, code)
        else
            tacContext.emitStmt(arrayLoadExpr.arrayRef.asVar.definedBy.head, delayStmtVisit = true, parentIdx = stmtIdx)
    }

    // Helper function to infer the element type from the array reference expression
    private[tac2bc] def inferElementType(expr: Expr[V]): FieldType = {
        expr.asVar.value.asInstanceOf[IsSReferenceValue[_]].theUpperTypeBound match {
            case ArrayType(componentType) => componentType
            case _                        => throw new IllegalArgumentException(s"Expected an array type but found: ${expr.cTpe}")
        }
    }

    def processArrayLength(
        arrayLength:  ArrayLength[V],
        tacToLVIndex: Map[Int, Int],
        code:         mutable.ListBuffer[CodeElement[Nothing]],
        tacContext:   Tac2BcContext,
        delayStmtVisit: Boolean = false
    ): Unit = {
        code += ARRAYLENGTH
        // Process the receiver object
        if (arrayLength.arrayRef.asVar.definedBy.head < 0)
            ExprProcessor.loadVariable(arrayLength.arrayRef.asVar, tacToLVIndex, code)
        else
            tacContext.emitStmt(arrayLength.arrayRef.asVar.definedBy.head, delayStmtVisit)
    }

    def processNewExpr(
        tpe:  ClassType,
        code: mutable.ListBuffer[CodeElement[Nothing]]
    ): Unit = {
        code += NEW(tpe)
    }

    def processCall(
        call:             Call[V],
        declaringClass:   ReferenceType,
        isInterface:      Boolean,
        methodName:       String,
        methodDescriptor: MethodDescriptor,
        tacToLVIndex:     Map[Int, Int],
        code:             mutable.ListBuffer[CodeElement[Nothing]],
        tacContext:       Tac2BcContext,
        stmtIndex:        Int
    ): Unit = {
        code += {
            call match {
                case _: VirtualMethodCall[V] | _: VirtualFunctionCall[V] =>
                    if (isInterface) INVOKEINTERFACE(declaringClass.asClassType, methodName, methodDescriptor)
                    else INVOKEVIRTUAL(declaringClass, methodName, methodDescriptor)
                case _: NonVirtualMethodCall[V] | _: NonVirtualFunctionCall[V] =>
                    INVOKESPECIAL(declaringClass.asClassType, isInterface, methodName, methodDescriptor)
                case _: StaticMethodCall[V] | _: StaticFunctionCall[V] =>
                    INVOKESTATIC(declaringClass.asClassType, isInterface, methodName, methodDescriptor)
            }
        }

        // 1. Process each parameter without receiver
        for (param <- call.params.reverse) {
            val definedByIdx = param.asVar.definedBy.head

            if(param.asVar.definedBy.head < 0)
                ExprProcessor.loadVariable(param.asVar, tacToLVIndex, code)
            else
                tacContext.emitStmt(definedByIdx, parentIdx = stmtIndex)
        }

        // 2. With receiver
        call.receiverOption.foreach { receiver =>
            val definedByIdx = receiver.asVar.definedBy.head

            if(receiver.asVar.definedBy.head < 0)
                ExprProcessor.loadVariable(receiver.asVar, tacToLVIndex, code)
            else
                tacContext.emitStmt(definedByIdx, parentIdx = stmtIndex)
        }
    }

    def loadConstant(
        constExpr: Const,
        code:      mutable.ListBuffer[CodeElement[Nothing]]
    ): Unit = {
        code += {
            constExpr match {
                case _: NullExpr                 => ACONST_NULL
                case IntConst(_, value)          => LoadConstantInstruction(value)
                case FloatConst(_, value)        => LoadFloat(value)
                case ClassConst(_, value)        => LoadClass(value)
                case StringConst(_, value)       => LoadString(value)
                case MethodHandleConst(_, value) => LoadMethodHandle(value)
                case MethodTypeConst(_, value)   => LoadMethodType(value)
                case DoubleConst(_, value)       => LoadDouble(value)
                case LongConst(_, value)         => LoadLong(value)
                case DynamicConst(_, bootstrapMethod, name, descriptor) =>
                    LoadDynamic(bootstrapMethod, name, descriptor)
            }
        }
    }

    // Method to get LVIndex for a variable
    private def getVariableLvIndex(variable: Var[V], tacToLVIndex: Map[Int, Int]): Int = {
        val tacIndex = variable match {
            case dVar: DVar[ValueInformation] => dVar.originatedAt
            case uVar: UVar[ValueInformation] => uVar.definedBy.head
        }
        tacToLVIndex.getOrElse(tacIndex, throw new RuntimeException(s"no index found for variable $variable"))
    }

    def loadVariable(
        variable:     Var[V],
        tacToLVIndex: Map[Int, Int],
        code:         mutable.ListBuffer[CodeElement[Nothing]]
    ): Unit = {
        val index = getVariableLvIndex(variable, tacToLVIndex)
        code += {
            variable.cTpe match {
                case ComputationalTypeInt       => ILOAD.canonicalRepresentation(index)
                case ComputationalTypeFloat     => FLOAD.canonicalRepresentation(index)
                case ComputationalTypeDouble    => DLOAD.canonicalRepresentation(index)
                case ComputationalTypeLong      => LLOAD.canonicalRepresentation(index)
                case ComputationalTypeReference => ALOAD.canonicalRepresentation(index)
                case _ =>
                    throw new UnsupportedOperationException(
                        "Unsupported computational type for loading variable" + variable
                    )
            }
        }
    }

    def storeVariable(
        variable:     Var[V],
        tacToLVIndex: Map[Int, Int],
        code:         mutable.ListBuffer[CodeElement[Nothing]]
    ): Unit = {
        val index: Int = getVariableLvIndex(variable, tacToLVIndex)
        code += {
            variable.cTpe match {
                case ComputationalTypeInt       => ISTORE.canonicalRepresentation(index)
                case ComputationalTypeFloat     => FSTORE.canonicalRepresentation(index)
                case ComputationalTypeDouble    => DSTORE.canonicalRepresentation(index)
                case ComputationalTypeLong      => LSTORE.canonicalRepresentation(index)
                case ComputationalTypeReference => ASTORE.canonicalRepresentation(index)
                case _ =>
                    throw new UnsupportedOperationException(
                        "Unsupported computational type for storing variable" + variable
                    )
            }
        }
    }

    def processGetField(
        getField:     GetField[V],
        tacToLVIndex: Map[Int, Int],
        code:         mutable.ListBuffer[CodeElement[Nothing]],
        tacContext:   Tac2BcContext,
        stmtIndex:    Int
    ): Unit = {
        // Generate the GETFIELD instruction
        code += GETFIELD(getField.declaringClass, getField.name, getField.declaredFieldType)
        // Load the object reference onto the stack
        if (getField.objRef.asVar.definedBy.head < 0)
            ExprProcessor.loadVariable(getField.objRef.asVar, tacToLVIndex, code)
        else
            tacContext.emitStmt(getField.objRef.asVar.definedBy.head, parentIdx = stmtIndex)
    }

    def processGetStatic(
        getStatic: GetStatic,
        code:      mutable.ListBuffer[CodeElement[Nothing]]
    ): Unit = {
        code += GETSTATIC(getStatic.declaringClass, getStatic.name, getStatic.declaredFieldType)
    }

    def processBinaryExpr(
        binaryExpr:   BinaryExpr[V],
        tacToLVIndex: Map[Int, Int],
        code:         mutable.ListBuffer[CodeElement[Nothing]],
        tacContext:   Tac2BcContext,
        nestedStmt:   Boolean = false,
        stmtIdx:    Int
    ): Unit = {
        // Loop handling: If a variable is used more than once and isn’t an end node, spill it to a local.
        if(tacContext.getVarFromId(stmtIdx) != null) {
            if(tacContext.getVarFromId(stmtIdx).asVar.usedBy.size > 1 && !tacContext.isStmtMarkedAsEndNode(stmtIdx)) {
                ExprProcessor.storeVariable(binaryExpr.left.asVar, tacToLVIndex, code)
            }
        }

        code += {
            (binaryExpr.cTpe, binaryExpr.op) match {
                // Double
                case (ComputationalTypeDouble, Add)      => DADD
                case (ComputationalTypeDouble, Subtract) => DSUB
                case (ComputationalTypeDouble, Multiply) => DMUL
                case (ComputationalTypeDouble, Divide)   => DDIV
                case (ComputationalTypeDouble, Modulo)   => DREM
                // Float
                case (ComputationalTypeFloat, Add)      => FADD
                case (ComputationalTypeFloat, Subtract) => FSUB
                case (ComputationalTypeFloat, Multiply) => FMUL
                case (ComputationalTypeFloat, Divide)   => FDIV
                case (ComputationalTypeFloat, Modulo)   => FREM
                // Int
                case (ComputationalTypeInt, Add)                => IADD
                case (ComputationalTypeInt, Subtract)           => ISUB
                case (ComputationalTypeInt, Multiply)           => IMUL
                case (ComputationalTypeInt, Divide)             => IDIV
                case (ComputationalTypeInt, Modulo)             => IREM
                case (ComputationalTypeInt, And)                => IAND
                case (ComputationalTypeInt, Or)                 => IOR
                case (ComputationalTypeInt, ShiftLeft)          => ISHL
                case (ComputationalTypeInt, ShiftRight)         => ISHR
                case (ComputationalTypeInt, UnsignedShiftRight) => IUSHR
                case (ComputationalTypeInt, XOr)                => IXOR
                // Long
                case (ComputationalTypeLong, Add)                => LADD
                case (ComputationalTypeLong, Subtract)           => LSUB
                case (ComputationalTypeLong, Multiply)           => LMUL
                case (ComputationalTypeLong, Divide)             => LDIV
                case (ComputationalTypeLong, Modulo)             => LREM
                case (ComputationalTypeLong, And)                => LAND
                case (ComputationalTypeLong, Or)                 => LOR
                case (ComputationalTypeLong, ShiftLeft)          => LSHL
                case (ComputationalTypeLong, ShiftRight)         => LSHR
                case (ComputationalTypeLong, UnsignedShiftRight) => LUSHR
                case (ComputationalTypeLong, XOr)                => LXOR
                // Unsupported
                case _ => throw new UnsupportedOperationException(
                        "Unsupported operation or computational type in BinaryExpr" + binaryExpr
                    )
            }
        }

        binaryExpr.right match {
            case const: Const => ExprProcessor.loadConstant(const, code)
            case uvar: UVar[_] =>
                if (uvar.definedBy.head < 0)
                    ExprProcessor.loadVariable(uvar, tacToLVIndex, code)
                else
                    tacContext.emitStmt(uvar.definedBy.iterator.min, delayStmtVisit = true, nestedStmt = true, parentIdx = stmtIdx)
        }

        binaryExpr.left match {
            case const: Const => ExprProcessor.loadConstant(const, code)
            case uvar: UVar[_] =>
                if (uvar.definedBy.head < 0)
                    ExprProcessor.loadVariable(uvar, tacToLVIndex, code)
                else
                    tacContext.emitStmt(uvar.definedBy.iterator.min, nestedStmt = true, parentIdx = stmtIdx)
        }
    }

    def processPrimitiveTypeCastExpr(
        primitiveTypecastExpr: PrimitiveTypecastExpr[V],
        tacToLVIndex:          Map[Int, Int],
        code:                  mutable.ListBuffer[CodeElement[Nothing]],
        tacContext:            Tac2BcContext,
        stmtIndex:             Int
    ): Unit = {
        code += {
            (primitiveTypecastExpr.operand.cTpe, primitiveTypecastExpr.targetTpe) match {
                // -> to Float
                case (ComputationalTypeDouble, FloatType) => D2F
                case (ComputationalTypeInt, FloatType)    => I2F
                case (ComputationalTypeLong, FloatType)   => L2F
                // -> to Int
                case (ComputationalTypeDouble, IntegerType) => D2I
                case (ComputationalTypeFloat, IntegerType)  => F2I
                case (ComputationalTypeLong, IntegerType)   => L2I
                // -> to Long
                case (ComputationalTypeDouble, LongType) => D2L
                case (ComputationalTypeInt, LongType)    => I2L
                case (ComputationalTypeFloat, LongType)  => F2L
                // -> to Double
                case (ComputationalTypeFloat, DoubleType) => F2D
                case (ComputationalTypeInt, DoubleType)   => I2D
                case (ComputationalTypeLong, DoubleType)  => L2D
                // -> to Char
                case (ComputationalTypeInt, CharType) => I2C
                // -> to Byte
                case (ComputationalTypeInt, ByteType) => I2B
                // -> to Short
                case (ComputationalTypeInt, ShortType) => I2S
                // -> other cases are not supported
                case _ => throw new UnsupportedOperationException(
                        "Unsupported operation or computational type in PrimitiveTypecastExpr" + primitiveTypecastExpr
                    )
            }
        }

        // Process the operand expression and add its instructions to the buffer
        if (primitiveTypecastExpr.operand.asVar.definedBy.head < 0)
            ExprProcessor.loadVariable(primitiveTypecastExpr.operand.asVar, tacToLVIndex, code)
        else
            tacContext.emitStmt(primitiveTypecastExpr.operand.asVar.definedBy.head, parentIdx = stmtIndex)

    }
}
