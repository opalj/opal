/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tactobc

import scala.language.existentials

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
import org.opalj.ba.InstructionElement
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
import org.opalj.br.instructions.AALOAD
import org.opalj.br.instructions.ACONST_NULL
import org.opalj.br.instructions.ALOAD
import org.opalj.br.instructions.ALOAD_0
import org.opalj.br.instructions.ALOAD_1
import org.opalj.br.instructions.ALOAD_2
import org.opalj.br.instructions.ALOAD_3
import org.opalj.br.instructions.ANEWARRAY
import org.opalj.br.instructions.ARRAYLENGTH
import org.opalj.br.instructions.ASTORE
import org.opalj.br.instructions.BALOAD
import org.opalj.br.instructions.CALOAD
import org.opalj.br.instructions.D2F
import org.opalj.br.instructions.D2I
import org.opalj.br.instructions.D2L
import org.opalj.br.instructions.DADD
import org.opalj.br.instructions.DALOAD
import org.opalj.br.instructions.DCMPG
import org.opalj.br.instructions.DCMPL
import org.opalj.br.instructions.DCONST_0
import org.opalj.br.instructions.DCONST_1
import org.opalj.br.instructions.DDIV
import org.opalj.br.instructions.DEFAULT_INVOKEDYNAMIC
import org.opalj.br.instructions.DLOAD
import org.opalj.br.instructions.DLOAD_0
import org.opalj.br.instructions.DLOAD_1
import org.opalj.br.instructions.DLOAD_2
import org.opalj.br.instructions.DLOAD_3
import org.opalj.br.instructions.DMUL
import org.opalj.br.instructions.DNEG
import org.opalj.br.instructions.DREM
import org.opalj.br.instructions.DSTORE
import org.opalj.br.instructions.DSUB
import org.opalj.br.instructions.F2D
import org.opalj.br.instructions.F2I
import org.opalj.br.instructions.F2L
import org.opalj.br.instructions.FADD
import org.opalj.br.instructions.FALOAD
import org.opalj.br.instructions.FCMPG
import org.opalj.br.instructions.FCMPL
import org.opalj.br.instructions.FCONST_0
import org.opalj.br.instructions.FCONST_1
import org.opalj.br.instructions.FCONST_2
import org.opalj.br.instructions.FDIV
import org.opalj.br.instructions.FLOAD
import org.opalj.br.instructions.FLOAD_0
import org.opalj.br.instructions.FLOAD_1
import org.opalj.br.instructions.FLOAD_2
import org.opalj.br.instructions.FLOAD_3
import org.opalj.br.instructions.FMUL
import org.opalj.br.instructions.FNEG
import org.opalj.br.instructions.FREM
import org.opalj.br.instructions.FSTORE
import org.opalj.br.instructions.FSUB
import org.opalj.br.instructions.GETFIELD
import org.opalj.br.instructions.GETSTATIC
import org.opalj.br.instructions.I2B
import org.opalj.br.instructions.I2C
import org.opalj.br.instructions.I2D
import org.opalj.br.instructions.I2F
import org.opalj.br.instructions.I2L
import org.opalj.br.instructions.I2S
import org.opalj.br.instructions.IADD
import org.opalj.br.instructions.IALOAD
import org.opalj.br.instructions.IAND
import org.opalj.br.instructions.IDIV
import org.opalj.br.instructions.ILOAD
import org.opalj.br.instructions.IMUL
import org.opalj.br.instructions.INEG
import org.opalj.br.instructions.INSTANCEOF
import org.opalj.br.instructions.INVOKEINTERFACE
import org.opalj.br.instructions.INVOKESTATIC
import org.opalj.br.instructions.INVOKEVIRTUAL
import org.opalj.br.instructions.IOR
import org.opalj.br.instructions.IREM
import org.opalj.br.instructions.ISHL
import org.opalj.br.instructions.ISHR
import org.opalj.br.instructions.ISTORE
import org.opalj.br.instructions.ISUB
import org.opalj.br.instructions.IUSHR
import org.opalj.br.instructions.IXOR
import org.opalj.br.instructions.L2D
import org.opalj.br.instructions.L2F
import org.opalj.br.instructions.L2I
import org.opalj.br.instructions.LADD
import org.opalj.br.instructions.LALOAD
import org.opalj.br.instructions.LAND
import org.opalj.br.instructions.LCMP
import org.opalj.br.instructions.LCONST_0
import org.opalj.br.instructions.LCONST_1
import org.opalj.br.instructions.LDIV
import org.opalj.br.instructions.LLOAD
import org.opalj.br.instructions.LLOAD_0
import org.opalj.br.instructions.LLOAD_1
import org.opalj.br.instructions.LLOAD_2
import org.opalj.br.instructions.LLOAD_3
import org.opalj.br.instructions.LMUL
import org.opalj.br.instructions.LNEG
import org.opalj.br.instructions.LoadClass
import org.opalj.br.instructions.LoadConstantInstruction
import org.opalj.br.instructions.LoadDouble
import org.opalj.br.instructions.LoadDynamic
import org.opalj.br.instructions.LoadFloat
import org.opalj.br.instructions.LoadLong
import org.opalj.br.instructions.LoadMethodHandle
import org.opalj.br.instructions.LoadMethodType
import org.opalj.br.instructions.LoadString
import org.opalj.br.instructions.LOR
import org.opalj.br.instructions.LREM
import org.opalj.br.instructions.LSHL
import org.opalj.br.instructions.LSHR
import org.opalj.br.instructions.LSTORE
import org.opalj.br.instructions.LSUB
import org.opalj.br.instructions.LUSHR
import org.opalj.br.instructions.LXOR
import org.opalj.br.instructions.MULTIANEWARRAY
import org.opalj.br.instructions.NEW
import org.opalj.br.instructions.NEWARRAY
import org.opalj.br.instructions.SALOAD
import org.opalj.bytecode.BytecodeProcessingFailedException
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.tac.ArrayLength
import org.opalj.tac.ArrayLoad
import org.opalj.tac.BinaryExpr
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
import org.opalj.tac.PrefixExpr
import org.opalj.tac.PrimitiveTypecastExpr
import org.opalj.tac.StaticFunctionCall
import org.opalj.tac.StringConst
import org.opalj.tac.UVar
import org.opalj.tac.Var
import org.opalj.tac.VirtualFunctionCall
import org.opalj.value.IsSReferenceValue

object ExprProcessor {

    def processExpression(expr: Expr[_], uVarToLVIndex: mutable.Map[IntTrieSet, Int]): List[InstructionElement] = {
        expr match {
            case const: Const              => loadConstant(const)
            case variable: Var[_]          => loadVariable(variable, uVarToLVIndex)
            case getField: GetField[_]     => handleGetField(getField, uVarToLVIndex)
            case getStatic: GetStatic      => handleGetStatic(getStatic)
            case binaryExpr: BinaryExpr[_] => handleBinaryExpr(binaryExpr, uVarToLVIndex)
            case virtualFunctionCallExpr: VirtualFunctionCall[_] =>
                handleVirtualFunctionCall(virtualFunctionCallExpr, uVarToLVIndex)
            case staticFunctionCallExpr: StaticFunctionCall[_] =>
                handleStaticFunctionCall(staticFunctionCallExpr, uVarToLVIndex)
            case newExpr: New => handleNewExpr(newExpr.tpe)
            case primitiveTypecaseExpr: PrimitiveTypecastExpr[_] =>
                handlePrimitiveTypeCastExpr(primitiveTypecaseExpr, uVarToLVIndex)
            case arrayLength: ArrayLength[_] => handleArrayLength(arrayLength, uVarToLVIndex)
            case arrayLoadExpr: ArrayLoad[_] => handleArrayLoad(arrayLoadExpr, uVarToLVIndex)
            case newArrayExpr: NewArray[_]   => handleNewArray(newArrayExpr, uVarToLVIndex)
            case invokedynamicFunctionCall: InvokedynamicFunctionCall[_] =>
                handleInvokedynamicFunctionCall(invokedynamicFunctionCall, uVarToLVIndex)
            case compare: Compare[_]       => handleCompare(compare, uVarToLVIndex)
            case prefixExpr: PrefixExpr[_] => handlePrefixExpr(prefixExpr, uVarToLVIndex)
            case instanceOf: InstanceOf[_] => handleInstanceOf(instanceOf, uVarToLVIndex)
            case _ =>
                throw new UnsupportedOperationException("Unsupported expression type" + expr)
        }
    }

    def handleInstanceOf(
        instanceOf:    InstanceOf[_],
        uVarToLVIndex: mutable.Map[IntTrieSet, Int]
    ): List[InstructionElement] = {
        val instructions = mutable.ListBuffer[InstructionElement]()
        instructions ++= ExprProcessor.processExpression(instanceOf.value, uVarToLVIndex)
        instructions += INSTANCEOF(instanceOf.cmpTpe)
        instructions.toList
    }

    def handlePrefixExpr(
        prefixExpr:    PrefixExpr[_],
        uVarToLVIndex: mutable.Map[IntTrieSet, Int]
    ): List[InstructionElement] = {
        val instructions = mutable.ListBuffer[InstructionElement]()

        // Process the operand (the expression being negated)
        instructions ++= ExprProcessor.processExpression(prefixExpr.operand, uVarToLVIndex)

        // Determine the appropriate negation instruction based on the operand type
        val instruction = prefixExpr.operand.cTpe match {
            case ComputationalTypeInt    => INEG
            case ComputationalTypeLong   => LNEG
            case ComputationalTypeFloat  => FNEG
            case ComputationalTypeDouble => DNEG
            case _ =>
                throw new UnsupportedOperationException(s"Unsupported type for negation: ${prefixExpr.operand.cTpe}")
        }
        instructions += instruction
        instructions.toList
    }

    def handleCompare(compare: Compare[_], uVarToLVIndex: mutable.Map[IntTrieSet, Int]): List[InstructionElement] = {
        val instructions = mutable.ListBuffer[InstructionElement]()
        // Process the left expression and update the PC
        instructions ++= processExpression(compare.left, uVarToLVIndex)

        // Process the right expression and update the PC
        instructions ++= processExpression(compare.right, uVarToLVIndex)

        // Determine the appropriate comparison instruction
        val instruction = compare.left.cTpe match {
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
        instructions += instruction
        instructions.toList
    }

    def handleInvokedynamicFunctionCall(
        invokedynamicFunctionCall: InvokedynamicFunctionCall[_],
        uVarToLVIndex:             mutable.Map[IntTrieSet, Int]
    ): List[InstructionElement] = {
        val instructions = mutable.ListBuffer[InstructionElement]()

        // Process each parameter and update the PC accordingly
        for (param <- invokedynamicFunctionCall.params) {
            instructions ++= ExprProcessor.processExpression(param, uVarToLVIndex)
        }
        val instruction = DEFAULT_INVOKEDYNAMIC(
            invokedynamicFunctionCall.bootstrapMethod,
            invokedynamicFunctionCall.name,
            invokedynamicFunctionCall.descriptor
        )
        instructions += instruction
        instructions.toList
    }

    def handleNewArray(newArrayExpr: NewArray[_], uVarToLVIndex: mutable.Map[IntTrieSet, Int]): List[InstructionElement] = {
        val instructions = mutable.ListBuffer[InstructionElement]()
        // Process each parameter and update the PC accordingly
        for (count <- newArrayExpr.counts.reverse) {
            instructions ++= ExprProcessor.processExpression(count, uVarToLVIndex)
        }
        if (newArrayExpr.counts.size > 1) {
            // Construct the array type string for the multi-dimensional array
            val arrayTypeString = newArrayExpr.tpe match {
                case arrayType: ArrayType =>
                    constructArrayTypeString(arrayType)
                case _ => throw new IllegalArgumentException("Expected an array type for MULTIANEWARRAY")
            }
            instructions += MULTIANEWARRAY(arrayTypeString, newArrayExpr.counts.size)
        } else {
            val instruction =
                if (newArrayExpr.tpe.componentType.isReferenceType) {
                    ANEWARRAY(newArrayExpr.tpe.componentType.asReferenceType)
                } else {
                    NEWARRAY(newArrayExpr.tpe.componentType.asBaseType.atype)
                }
            instructions += instruction
        }
        instructions.toList
    }

    def constructArrayTypeString(arrayType: ArrayType): String = {
        def loop(tpe: Type, depth: Int): (String, Int) = tpe match {
            case ArrayType(componentType) =>
                loop(componentType, depth + 1)
            case baseType =>
                (baseType.toString, depth)
        }

        val (baseTypeString, depth) = loop(arrayType, 0)
        "[" * depth + baseTypeString
    }

    def handleArrayLoad(
        arrayLoadExpr: ArrayLoad[_],
        uVarToLVIndex: mutable.Map[IntTrieSet, Int]
    ): List[InstructionElement] = {
        val instructions = mutable.ListBuffer[InstructionElement]()
        // Load the array reference onto the stack
        instructions ++= processExpression(arrayLoadExpr.arrayRef, uVarToLVIndex)
        // Load the index onto the stack
        instructions ++= processExpression(arrayLoadExpr.index, uVarToLVIndex)

        // Infer the element type from the array reference expression
        val elementType = inferElementType(arrayLoadExpr.arrayRef)

        val instruction = elementType match {
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
        instructions += instruction
        instructions.toList
    }

    // Helper function to infer the element type from the array reference expression
    def inferElementType(expr: Expr[_]): Type = {
        expr.asInstanceOf[UVar[_]].value.asInstanceOf[IsSReferenceValue[_]].theUpperTypeBound.asInstanceOf[ArrayType] match {
            case ArrayType(componentType) =>
                println(s"ArrayType($componentType)")
                componentType
            case _ => throw new IllegalArgumentException(s"Expected an array type but found: ${expr.cTpe}")
        }
    }
    def handleArrayLength(
        arrayLength:   ArrayLength[_],
        uVarToLVIndex: mutable.Map[IntTrieSet, Int]
    ): List[InstructionElement] = {
        val instructions = mutable.ListBuffer[InstructionElement]()
        // Process the receiver object (e.g., aload_0 for `this`)
        instructions ++= ExprProcessor.processExpression(arrayLength.arrayRef, uVarToLVIndex)
        instructions += ARRAYLENGTH
        instructions.toList
    }

    def handleNewExpr(tpe: ObjectType): List[InstructionElement] = {
        println(s"hier wird NEW($tpe) erstellt")
        List(
            NEW(tpe)
        )
    }

    def handleStaticFunctionCall(
        expr:          StaticFunctionCall[_],
        uVarToLVIndex: mutable.Map[IntTrieSet, Int]
    ): List[InstructionElement] = {
        val instructions = mutable.ListBuffer[InstructionElement]()
        // Process each parameter and update the PC accordingly
        for (param <- expr.params) {
            instructions ++= ExprProcessor.processExpression(param, uVarToLVIndex)
        }
        val instruction = if (expr.isInterface) {
            INVOKEINTERFACE(expr.declaringClass, expr.name, expr.descriptor)
        } else {
            INVOKESTATIC(expr.declaringClass, expr.isInterface, expr.name, expr.descriptor)
        }
        instructions += instruction
        instructions.toList
    }

    def handleVirtualFunctionCall(
        expr:          VirtualFunctionCall[_],
        uVarToLVIndex: mutable.Map[IntTrieSet, Int]
    ): List[InstructionElement] = {
        val instructions = mutable.ListBuffer[InstructionElement]()

        // Process the receiver object (e.g., aload_0 for `this`)
        instructions ++= ExprProcessor.processExpression(expr.receiver, uVarToLVIndex)

        // Process each parameter and update the PC accordingly
        for (param <- expr.params) {
            instructions ++= ExprProcessor.processExpression(param, uVarToLVIndex)
        }
        val instruction = if (expr.isInterface) {
            INVOKEINTERFACE(expr.declaringClass.asObjectType, expr.name, expr.descriptor)
        } else {
            INVOKEVIRTUAL(expr.declaringClass, expr.name, expr.descriptor)
        }
        instructions += instruction
        instructions.toList
    }

    private def loadConstant(constExpr: Const): List[InstructionElement] = {
        val instructions = mutable.ListBuffer[InstructionElement]()
        val instruction = {
            if (constExpr.isNullExpr) {
                ACONST_NULL
            } else
                constExpr match {
                    case IntConst(_, value) => LoadConstantInstruction(value)
                    case FloatConst(_, value) => value match {
                            case 0 => FCONST_0
                            case 1 => FCONST_1
                            case 2 => FCONST_2
                            case _ => LoadFloat(value)
                        }
                    case ClassConst(_, value)        => LoadClass(value)
                    case StringConst(_, value)       => LoadString(value)
                    case MethodHandleConst(_, value) => LoadMethodHandle(value)
                    case MethodTypeConst(_, value)   => LoadMethodType(value)
                    case DoubleConst(_, value) => value match {
                            case 0 => DCONST_0
                            case 1 => DCONST_1
                            case _ => LoadDouble(value)
                        }
                    case LongConst(_, value) => value match {
                            case 0 => LCONST_0
                            case 1 => LCONST_1
                            case _ => LoadLong(value)
                        }
                    case DynamicConst(_, bootstrapMethod, name, descriptor) =>
                        LoadDynamic(bootstrapMethod, name, descriptor)

                    case _ => throw BytecodeProcessingFailedException("unsupported constant value: " + constExpr)
                }
        }
        instructions += instruction
        instructions.toList
    }

    // Method to get LVIndex for a variable
    def getVariableLvIndex(variable: Var[_], uVarToLVIndex: mutable.Map[IntTrieSet, Int]): Int = {
        variable match {
            case dVar: DVar[_] =>
                val uVarDefSites = uVarToLVIndex.find { case (defSites, _) =>
                    defSites.contains(dVar.origin)
                }
                uVarDefSites match {
                    case Some((_, index)) => index
                    case None =>
                        throw new NoSuchElementException(s"No LVIndex found for DVar with origin ${dVar.origin}")
                }
            case uVar: UVar[_] =>
                val defSiteMatch = uVarToLVIndex.find { case (defSites, index) =>
                    defSites.exists(uVar.defSites.contains)
                }
                defSiteMatch match {
                    case Some((_, index)) => index
                    case None =>
                        throw new NoSuchElementException(s"No LVIndex found for UVar with defSites ${uVar.defSites}")
                }
            case _ => throw new UnsupportedOperationException("Unsupported variable type")
        }
    }

    def loadVariable(variable: Var[_], uVarToLVIndex: mutable.Map[IntTrieSet, Int]): List[InstructionElement] = {
        val instructions = mutable.ListBuffer[InstructionElement]()

        val index = getVariableLvIndex(variable, uVarToLVIndex)
        val instruction = variable.cTpe match {
            case ComputationalTypeInt => ILOAD.canonicalRepresentation(index)
            case ComputationalTypeFloat => index match {
                    case 0 => FLOAD_0
                    case 1 => FLOAD_1
                    case 2 => FLOAD_2
                    case 3 => FLOAD_3
                    case _ => FLOAD(index)
                }
            case ComputationalTypeDouble => index match {
                    case 0 => DLOAD_0
                    case 1 => DLOAD_1
                    case 2 => DLOAD_2
                    case 3 => DLOAD_3
                    case _ => DLOAD(index)
                }
            case ComputationalTypeLong => index match {
                    case 0 => LLOAD_0
                    case 1 => LLOAD_1
                    case 2 => LLOAD_2
                    case 3 => LLOAD_3
                    case _ => LLOAD(index)
                }
            case ComputationalTypeReference => index match {
                    case 0 => ALOAD_0
                    case 1 => ALOAD_1
                    case 2 => ALOAD_2
                    case 3 => ALOAD_3
                    case _ => ALOAD(index)
                }
            case _ =>
                throw new UnsupportedOperationException("Unsupported computational type for loading variable" + variable)
        }
        instructions += instruction
        instructions.toList
    }

    def storeVariable(
        variable:      Var[_],
        uVarToLVIndex: mutable.Map[IntTrieSet, Int]
    ): InstructionElement = {
//         val variableName = handleDVarName(variable)
        val index = getVariableLvIndex(variable, uVarToLVIndex)
        variable.cTpe match {
            // The <n> must be an index into the local variable array of the current frame (§2.6).
            // The value on the top of the operand stack must be of type int. It is popped from the operand stack,
            // and the value of the local variable at <n> is set to value.
            case ComputationalTypeInt       => ISTORE.canonicalRepresentation(index)
            case ComputationalTypeFloat     => FSTORE.canonicalRepresentation(index)
            case ComputationalTypeDouble    => DSTORE.canonicalRepresentation(index)
            case ComputationalTypeLong      => LSTORE.canonicalRepresentation(index)
            case ComputationalTypeReference => ASTORE.canonicalRepresentation(index)
            case _ =>
                throw new UnsupportedOperationException("Unsupported computational type for storing variable" + variable)
        }
    }

    def handleGetField(getField: GetField[_], uVarToLVIndex: mutable.Map[IntTrieSet, Int]): List[InstructionElement] = {
        val instructions = mutable.ListBuffer[InstructionElement]()
        // Load the object reference onto the stack
        instructions ++= processExpression(getField.objRef, uVarToLVIndex)
        // Generate the GETFIELD instruction
        instructions += GETFIELD(getField.declaringClass, getField.name, getField.declaredFieldType)
        instructions.toList
    }

    def handleGetStatic(getStatic: GetStatic): List[InstructionElement] = {
        List(
            GETSTATIC(getStatic.declaringClass, getStatic.name, getStatic.declaredFieldType)
        )
    }

    def handleBinaryExpr(
        binaryExpr:    BinaryExpr[_],
        uVarToLVIndex: mutable.Map[IntTrieSet, Int]
    ): List[InstructionElement] = {

        val instructions = mutable.ListBuffer[InstructionElement]()

        // process the left expr and save the pc to give in the right expr processing
        instructions ++= processExpression(binaryExpr.left, uVarToLVIndex)
        // process the right Expr
        instructions ++= processExpression(binaryExpr.right, uVarToLVIndex)

        val instruction = (binaryExpr.cTpe, binaryExpr.op) match {
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
        instructions += instruction
        instructions.toList
    }
    def handlePrimitiveTypeCastExpr(
        primitiveTypecastExpr: PrimitiveTypecastExpr[_],
        uVarToLVIndex:         mutable.Map[IntTrieSet, Int]
    ): List[InstructionElement] = {
        val instructions = mutable.ListBuffer[InstructionElement]()
        // First, process the operand expression and add its instructions to the buffer
        instructions ++= processExpression(primitiveTypecastExpr.operand, uVarToLVIndex)

        val instruction = (primitiveTypecastExpr.operand.cTpe, primitiveTypecastExpr.targetTpe) match {
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
        instructions += instruction
        instructions.toList
    }
}
