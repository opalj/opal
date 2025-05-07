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
import org.opalj.RelationalOperators.CMPG
import org.opalj.RelationalOperators.CMPL
import org.opalj.ba.CodeElement
import org.opalj.br.ArrayType
import org.opalj.br.ByteType
import org.opalj.br.CharType
import org.opalj.br.ComputationalTypeDouble
import org.opalj.br.ComputationalTypeFloat
import org.opalj.br.ComputationalTypeInt
import org.opalj.br.ComputationalTypeLong
import org.opalj.br.ComputationalTypeReference
import org.opalj.br.DoubleType
import org.opalj.br.FloatType
import org.opalj.br.IntegerType
import org.opalj.br.LongType
import org.opalj.br.ObjectType
import org.opalj.br.ReferenceType
import org.opalj.br.ShortType
import org.opalj.br.Type
import org.opalj.br.instructions._
import org.opalj.bytecode.BytecodeProcessingFailedException
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.tac.ArrayLength
import org.opalj.tac.ArrayLoad
import org.opalj.tac.BinaryExpr
import org.opalj.tac.ClassConst
import org.opalj.tac.Compare
import org.opalj.tac.Const
import org.opalj.tac.DUVar
import org.opalj.tac.DVar
import org.opalj.tac.DoubleConst
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
import org.opalj.tac.PrefixExpr
import org.opalj.tac.PrimitiveTypecastExpr
import org.opalj.tac.StaticFunctionCall
import org.opalj.tac.StringConst
import org.opalj.tac.UVar
import org.opalj.tac.Var
import org.opalj.tac.VirtualFunctionCall
import org.opalj.value.IsSReferenceValue

object ExprProcessor {

    /**
     * Generates Java bytecode instructions for Expr
     *
     * @param expr the Expression to be converted into InstructionElements
     * @param uVarToLVIndex map that holds information for Local Variable Indices
     * @param listedCodeElements list where bytecode instructions should be added
     */
    def processExpression(
        expr:               Expr[_],
        uVarToLVIndex:      mutable.Map[IntTrieSet, Int],
        listedCodeElements: mutable.ListBuffer[CodeElement[Nothing]]
    ): Unit = {
        expr match {
            case const: Const              => loadConstant(const, listedCodeElements)
            case variable: Var[_]          => loadVariable(variable, uVarToLVIndex, listedCodeElements)
            case getField: GetField[_]     => handleGetField(getField, uVarToLVIndex, listedCodeElements)
            case getStatic: GetStatic      => handleGetStatic(getStatic, listedCodeElements)
            case binaryExpr: BinaryExpr[_] => handleBinaryExpr(binaryExpr, uVarToLVIndex, listedCodeElements)
            case virtualFunctionCallExpr: VirtualFunctionCall[_] =>
                handleVirtualFunctionCall(virtualFunctionCallExpr, uVarToLVIndex, listedCodeElements)
            case staticFunctionCallExpr: StaticFunctionCall[_] =>
                handleStaticFunctionCall(staticFunctionCallExpr, uVarToLVIndex, listedCodeElements)
            case newExpr: New => handleNewExpr(newExpr.tpe, listedCodeElements)
            case primitiveTypecastExpr: PrimitiveTypecastExpr[_] =>
                handlePrimitiveTypeCastExpr(primitiveTypecastExpr, uVarToLVIndex, listedCodeElements)
            case arrayLength: ArrayLength[_] => handleArrayLength(arrayLength, uVarToLVIndex, listedCodeElements)
            case arrayLoadExpr: ArrayLoad[_] => handleArrayLoad(arrayLoadExpr, uVarToLVIndex, listedCodeElements)
            case newArrayExpr: NewArray[_]   => handleNewArray(newArrayExpr, uVarToLVIndex, listedCodeElements)
            case invokedynamicFunctionCall: InvokedynamicFunctionCall[_] =>
                handleInvokedynamicFunctionCall(invokedynamicFunctionCall, uVarToLVIndex, listedCodeElements)
            case compare: Compare[_]       => handleCompare(compare, uVarToLVIndex, listedCodeElements)
            case prefixExpr: PrefixExpr[_] => handlePrefixExpr(prefixExpr, uVarToLVIndex, listedCodeElements)
            case instanceOf: InstanceOf[_] => handleInstanceOf(instanceOf, uVarToLVIndex, listedCodeElements)
            case _ =>
                throw new UnsupportedOperationException("Unsupported expression type" + expr)
        }
    }

    private def handleInstanceOf(
        instanceOf:         InstanceOf[_],
        uVarToLVIndex:      mutable.Map[IntTrieSet, Int],
        listedCodeElements: mutable.ListBuffer[CodeElement[Nothing]]
    ): Unit = {
        ExprProcessor.processExpression(instanceOf.value, uVarToLVIndex, listedCodeElements)
        listedCodeElements += INSTANCEOF(instanceOf.cmpTpe)
    }

    private def handlePrefixExpr(
        prefixExpr:         PrefixExpr[_],
        uVarToLVIndex:      mutable.Map[IntTrieSet, Int],
        listedCodeElements: mutable.ListBuffer[CodeElement[Nothing]]
    ): Unit = {
        // Process the operand (the expression being negated)
        ExprProcessor.processExpression(prefixExpr.operand, uVarToLVIndex, listedCodeElements)
        // Determine the appropriate negation instruction based on the operand type
        listedCodeElements += {
            prefixExpr.operand.cTpe match {
                case ComputationalTypeInt    => INEG
                case ComputationalTypeLong   => LNEG
                case ComputationalTypeFloat  => FNEG
                case ComputationalTypeDouble => DNEG
                case _ =>
                    throw new UnsupportedOperationException(s"Unsupported type for negation: ${prefixExpr.operand.cTpe}")
            }
        }
    }

    private def handleCompare(
        compare:            Compare[_],
        uVarToLVIndex:      mutable.Map[IntTrieSet, Int],
        listedCodeElements: mutable.ListBuffer[CodeElement[Nothing]]
    ): Unit = {
        // Process the left expression
        processExpression(compare.left, uVarToLVIndex, listedCodeElements)
        // Process the right expression
        processExpression(compare.right, uVarToLVIndex, listedCodeElements)
        // Determine the appropriate comparison instruction
        listedCodeElements += {
            compare.left.cTpe match {
                case ComputationalTypeFloat =>
                    compare.condition match {
                        case CMPG => FCMPG
                        case CMPL => FCMPL
                        case _    => throw new IllegalArgumentException("Unsupported comparison operator for float type")
                    }
                case ComputationalTypeDouble =>
                    compare.condition match {
                        case CMPG => DCMPG
                        case CMPL => DCMPL
                        case _    => throw new IllegalArgumentException("Unsupported comparison operator for double type")
                    }
                case ComputationalTypeLong =>
                    LCMP
                case _ => throw new IllegalArgumentException("Unsupported comparison type")
            }
        }
    }

    private def handleInvokedynamicFunctionCall(
        invokedynamicFunctionCall: InvokedynamicFunctionCall[_],
        uVarToLVIndex:             mutable.Map[IntTrieSet, Int],
        listedCodeElements:        mutable.ListBuffer[CodeElement[Nothing]]
    ): Unit = {
        // Process each parameter
        for (param <- invokedynamicFunctionCall.params)
            ExprProcessor.processExpression(param, uVarToLVIndex, listedCodeElements)
        listedCodeElements += DEFAULT_INVOKEDYNAMIC(
            invokedynamicFunctionCall.bootstrapMethod,
            invokedynamicFunctionCall.name,
            invokedynamicFunctionCall.descriptor
        )
    }

    private def handleNewArray(
        newArrayExpr:       NewArray[_],
        uVarToLVIndex:      mutable.Map[IntTrieSet, Int],
        listedCodeElements: mutable.ListBuffer[CodeElement[Nothing]]
    ): Unit = {
        // Process each parameter
        for (count <- newArrayExpr.counts.reverse)
            ExprProcessor.processExpression(count, uVarToLVIndex, listedCodeElements)
        listedCodeElements += {
            if (newArrayExpr.counts.size > 1) {
                MULTIANEWARRAY(newArrayExpr.tpe, newArrayExpr.counts.size)
            } else if (newArrayExpr.tpe.componentType.isReferenceType) {
                ANEWARRAY(newArrayExpr.tpe.componentType.asReferenceType)
            } else {
                NEWARRAY(newArrayExpr.tpe.componentType.asBaseType.atype)
            }
        }
    }

    private def handleArrayLoad(
        arrayLoadExpr:      ArrayLoad[_],
        uVarToLVIndex:      mutable.Map[IntTrieSet, Int],
        listedCodeElements: mutable.ListBuffer[CodeElement[Nothing]]
    ): Unit = {
        // Load the array reference onto the stack
        processExpression(arrayLoadExpr.arrayRef, uVarToLVIndex, listedCodeElements)
        // Load the index onto the stack
        processExpression(arrayLoadExpr.index, uVarToLVIndex, listedCodeElements)
        // Infer the element type from the array reference expression
        val elementType = inferElementType(arrayLoadExpr.arrayRef)
        listedCodeElements += {
            elementType match {
                case IntegerType      => IALOAD
                case LongType         => LALOAD
                case FloatType        => FALOAD
                case DoubleType       => DALOAD
                case ByteType         => BALOAD
                case CharType         => CALOAD
                case ShortType        => SALOAD
                case _: ReferenceType => AALOAD
                case _                => throw new IllegalArgumentException("Unsupported array load type" + elementType)
            }
        }
    }

    // Helper function to infer the element type from the array reference expression
    def inferElementType(expr: Expr[_]): Type = {
        expr.asInstanceOf[UVar[_]].value.asInstanceOf[IsSReferenceValue[_]].theUpperTypeBound.asInstanceOf[ArrayType] match {
            case ArrayType(componentType) => componentType
            case _                        => throw new IllegalArgumentException(s"Expected an array type but found: ${expr.cTpe}")
        }
    }
    private def handleArrayLength(
        arrayLength:        ArrayLength[_],
        uVarToLVIndex:      mutable.Map[IntTrieSet, Int],
        listedCodeElements: mutable.ListBuffer[CodeElement[Nothing]]
    ): Unit = {
        // Process the receiver object (e.g., aload_0 for `this`)
        ExprProcessor.processExpression(arrayLength.arrayRef, uVarToLVIndex, listedCodeElements)
        listedCodeElements += ARRAYLENGTH
    }

    private def handleNewExpr(
        tpe:                ObjectType,
        listedCodeElements: mutable.ListBuffer[CodeElement[Nothing]]
    ): Unit = {
        listedCodeElements += NEW(tpe)
    }

    private def handleStaticFunctionCall(
        expr:               StaticFunctionCall[_],
        uVarToLVIndex:      mutable.Map[IntTrieSet, Int],
        listedCodeElements: mutable.ListBuffer[CodeElement[Nothing]]
    ): Unit = {
        // Process each parameter
        for (param <- expr.params) ExprProcessor.processExpression(param, uVarToLVIndex, listedCodeElements)
        listedCodeElements += {
            if (expr.isInterface) {
                INVOKEINTERFACE(expr.declaringClass, expr.name, expr.descriptor)
            } else {
                INVOKESTATIC(expr.declaringClass, expr.isInterface, expr.name, expr.descriptor)
            }
        }
    }

    private def handleVirtualFunctionCall(
        expr:               VirtualFunctionCall[_],
        uVarToLVIndex:      mutable.Map[IntTrieSet, Int],
        listedCodeElements: mutable.ListBuffer[CodeElement[Nothing]]
    ): Unit = {
        // Process the receiver object (e.g., ALOAD_0 for `this`)
        ExprProcessor.processExpression(expr.receiver, uVarToLVIndex, listedCodeElements)
        // Process each parameter
        for (param <- expr.params) ExprProcessor.processExpression(param, uVarToLVIndex, listedCodeElements)
        listedCodeElements += {
            if (expr.isInterface) {
                INVOKEINTERFACE(expr.declaringClass.asObjectType, expr.name, expr.descriptor)
            } else {
                INVOKEVIRTUAL(expr.declaringClass, expr.name, expr.descriptor)
            }
        }
    }

    private def loadConstant(
        constExpr:          Const,
        listedCodeElements: mutable.ListBuffer[CodeElement[Nothing]]
    ): Unit = {
        listedCodeElements += {
            if (constExpr.isNullExpr) {
                ACONST_NULL
            } else {
                constExpr match {
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
                    case _ => throw BytecodeProcessingFailedException("unsupported constant value: " + constExpr)
                }
            }
        }
    }

    // Method to get LVIndex for a variable
    private def getVariableLvIndex(variable: Var[_], uVarToLVIndex: mutable.Map[IntTrieSet, Int]): Int = {
        variable match {
            case duVar: DUVar[_] =>
                val uVarDefSites = uVarToLVIndex.find { case (defSites, _) =>
                    duVar match {
                        case dVar: DVar[_] => defSites.contains(dVar.origin)
                        case uVar: UVar[_] => defSites.exists(uVar.defSites.contains)
                    }
                }
                uVarDefSites match {
                    case Some((_, index)) => index
                    case None             => throw new RuntimeException(s"no index found for variable $variable")
                }
            case _ => throw new UnsupportedOperationException("Unsupported variable type")
        }
    }

    private def loadVariable(
        variable:           Var[_],
        uVarToLVIndex:      mutable.Map[IntTrieSet, Int],
        listedCodeElements: mutable.ListBuffer[CodeElement[Nothing]]
    ): Unit = {
        val index = getVariableLvIndex(variable, uVarToLVIndex)
        listedCodeElements += {
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
        variable:           Var[_],
        uVarToLVIndex:      mutable.Map[IntTrieSet, Int],
        listedCodeElements: mutable.ListBuffer[CodeElement[Nothing]]
    ): Unit = {
        val index: Int = getVariableLvIndex(variable, uVarToLVIndex)
        listedCodeElements += {
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

    private def handleGetField(
        getField:           GetField[_],
        uVarToLVIndex:      mutable.Map[IntTrieSet, Int],
        listedCodeElements: mutable.ListBuffer[CodeElement[Nothing]]
    ): Unit = {
        // Load the object reference onto the stack
        processExpression(getField.objRef, uVarToLVIndex, listedCodeElements)
        // Generate the GETFIELD instruction
        listedCodeElements += GETFIELD(getField.declaringClass, getField.name, getField.declaredFieldType)
    }

    private def handleGetStatic(
        getStatic:          GetStatic,
        listedCodeElements: mutable.ListBuffer[CodeElement[Nothing]]
    ): Unit = {
        listedCodeElements += GETSTATIC(getStatic.declaringClass, getStatic.name, getStatic.declaredFieldType)
    }

    private def handleBinaryExpr(
        binaryExpr:         BinaryExpr[_],
        uVarToLVIndex:      mutable.Map[IntTrieSet, Int],
        listedCodeElements: mutable.ListBuffer[CodeElement[Nothing]]
    ): Unit = {
        // process the left expr and save the pc to give in the right expr processing
        processExpression(binaryExpr.left, uVarToLVIndex, listedCodeElements)
        // process the right Expr
        processExpression(binaryExpr.right, uVarToLVIndex, listedCodeElements)

        listedCodeElements += {
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
    }
    private def handlePrimitiveTypeCastExpr(
        primitiveTypecastExpr: PrimitiveTypecastExpr[_],
        uVarToLVIndex:         mutable.Map[IntTrieSet, Int],
        listedCodeElements:    mutable.ListBuffer[CodeElement[Nothing]]
    ): Unit = {
        // First, process the operand expression and add its instructions to the buffer
        processExpression(primitiveTypecastExpr.operand, uVarToLVIndex, listedCodeElements)

        listedCodeElements += {
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
    }
}
