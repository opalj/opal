/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tactobc

import org.opalj.BinaryArithmeticOperators.{Add, And, Divide, Modulo, Multiply, Or, ShiftLeft, ShiftRight, Subtract, UnsignedShiftRight, XOr}
import org.opalj.RelationalOperators.{CMPG, CMPL}
import org.opalj.br.{ArrayType, BooleanType, ByteType, CharType, ComputationalTypeDouble, ComputationalTypeFloat, ComputationalTypeInt, ComputationalTypeLong, ComputationalTypeReference, DoubleType, FloatType, IntegerType, LongType, ObjectType, ReferenceType, ShortType, Type}
import org.opalj.br.instructions.{AALOAD, ACONST_NULL, ALOAD, ALOAD_0, ALOAD_1, ALOAD_2, ALOAD_3, ANEWARRAY, ARRAYLENGTH, ASTORE, ASTORE_0, ASTORE_1, ASTORE_2, ASTORE_3, BALOAD, BIPUSH, CALOAD, D2F, D2I, D2L, DADD, DALOAD, DCMPG, DCMPL, DCONST_0, DCONST_1, DDIV, DEFAULT_INVOKEDYNAMIC, DLOAD, DLOAD_0, DLOAD_1, DLOAD_2, DLOAD_3, DMUL, DREM, DSTORE, DSTORE_0, DSTORE_1, DSTORE_2, DSTORE_3, DSUB, F2D, F2I, F2L, FADD, FALOAD, FCMPG, FCMPL, FCONST_0, FCONST_1, FCONST_2, FDIV, FLOAD, FLOAD_0, FLOAD_1, FLOAD_2, FLOAD_3, FMUL, FREM, FSTORE, FSTORE_0, FSTORE_1, FSTORE_2, FSTORE_3, FSUB, GETFIELD, GETSTATIC, I2B, I2C, I2D, I2F, I2L, I2S, IADD, IALOAD, IAND, ICONST_0, ICONST_1, ICONST_2, ICONST_3, ICONST_4, ICONST_5, ICONST_M1, IDIV, ILOAD, ILOAD_0, ILOAD_1, ILOAD_2, ILOAD_3, IMUL, INVOKEINTERFACE, INVOKESTATIC, INVOKEVIRTUAL, IOR, IREM, ISHL, ISHR, ISTORE, ISTORE_0, ISTORE_1, ISTORE_2, ISTORE_3, ISUB, IUSHR, IXOR, Instruction, L2D, L2F, L2I, LADD, LALOAD, LAND, LCMP, LCONST_0, LCONST_1, LDIV, LLOAD, LLOAD_0, LLOAD_1, LLOAD_2, LLOAD_3, LMUL, LOR, LREM, LSHL, LSHR, LSTORE, LSTORE_0, LSTORE_1, LSTORE_2, LSTORE_3, LSUB, LUSHR, LXOR, LoadClass, LoadDouble, LoadFloat, LoadInt, LoadLong, LoadMethodHandle, LoadMethodType, LoadString, MULTIANEWARRAY, NEW, NEWARRAY, SALOAD, SIPUSH}
import org.opalj.bytecode.BytecodeProcessingFailedException
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.tac.{ArrayLength, ArrayLoad, BinaryExpr, ClassConst, Compare, Const, DVar, DoubleConst, Expr, FloatConst, GetField, GetStatic, IntConst, InvokedynamicFunctionCall, LongConst, MethodHandleConst, MethodTypeConst, New, NewArray, PrimitiveTypecastExpr, StaticFunctionCall, StringConst, UVar, Var, VirtualFunctionCall}
import org.opalj.value.IsSReferenceValue

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

object ExprProcessor {


  def processExpression(expr: Expr[_], instructionsWithPCs: ArrayBuffer[(Int, Instruction)], currentPC: Int): Int = {
    expr match {
      case const: Const => loadConstant(const, instructionsWithPCs, currentPC)
      case variable: Var[_] => loadVariable(variable, instructionsWithPCs, currentPC)
      case getField: GetField[_] => handleGetField(getField, instructionsWithPCs, currentPC)
      case getStatic: GetStatic => handleGetStatic(getStatic, instructionsWithPCs, currentPC)
      case binaryExpr: BinaryExpr[_] => handleBinaryExpr(binaryExpr, instructionsWithPCs, currentPC)
      case virtualFunctionCallExpr: VirtualFunctionCall[_] => handleVirtualFunctionCall(virtualFunctionCallExpr, instructionsWithPCs, currentPC)
      case staticFunctionCallExpr: StaticFunctionCall[_] => handleStaticFunctionCall(staticFunctionCallExpr, instructionsWithPCs, currentPC)
      case newExpr: New => handleNewExpr(newExpr.tpe, instructionsWithPCs, currentPC)
      case primitiveTypecaseExpr: PrimitiveTypecastExpr[_] => handlePrimitiveTypeCastExpr(primitiveTypecaseExpr, instructionsWithPCs, currentPC)
      case arrayLength: ArrayLength[_] => handleArrayLength(arrayLength, instructionsWithPCs, currentPC)
      case arrayLoadExpr: ArrayLoad[_] => handleArrayLoad(arrayLoadExpr, instructionsWithPCs, currentPC)
      case newArrayExpr: NewArray[_] => handleNewArray(newArrayExpr, instructionsWithPCs, currentPC)
      case invokedynamicFunctionCall: InvokedynamicFunctionCall[_] => handleInvokedynamicFunctionCall(invokedynamicFunctionCall, instructionsWithPCs, currentPC)
      case compare: Compare[_] => handleCompare(compare, instructionsWithPCs, currentPC)
      case _ =>
        throw new UnsupportedOperationException("Unsupported expression type" + expr)
    }
  }

  def handleCompare(compare: Compare[_], instructionsWithPCs: ArrayBuffer[(Int, Instruction)], currentPC: Int): Int = {
    // Process the left expression and update the PC
    val pcAfterLeft = processExpression(compare.left, instructionsWithPCs, currentPC)

    // Process the right expression and update the PC
    val pcAfterRight = processExpression(compare.right, instructionsWithPCs, pcAfterLeft)

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

    // Add the comparison instruction to the list
    instructionsWithPCs += ((pcAfterRight, instruction))

    // Update the PC and return it
    pcAfterRight + instruction.length
  }

  def handleInvokedynamicFunctionCall(invokedynamicFunctionCall: InvokedynamicFunctionCall[_], instructionsWithPCs: ArrayBuffer[(Int, Instruction)], currentPC: Int): Int = {
    // Initialize the PC after processing the receiver
    var currentAfterParamsPC = currentPC

    // Process each parameter and update the PC accordingly
    for (param <- invokedynamicFunctionCall.params) {
      currentAfterParamsPC = ExprProcessor.processExpression(param, instructionsWithPCs, currentAfterParamsPC)
    }
    val instruction = DEFAULT_INVOKEDYNAMIC(invokedynamicFunctionCall.bootstrapMethod, invokedynamicFunctionCall.name, invokedynamicFunctionCall.descriptor)
    instructionsWithPCs += ((currentAfterParamsPC, instruction))
    currentAfterParamsPC + instruction.length
  }

  def handleNewArray(newArrayExpr: NewArray[_], instructionsWithPCs: ArrayBuffer[(Int, Instruction)], currentPC: Int): Int = {
    // Initialize the PC after processing the counts
    var currentAfterCountsPC = currentPC
    // Process each parameter and update the PC accordingly
    for (count <- newArrayExpr.counts.reverse) {
      currentAfterCountsPC = ExprProcessor.processExpression(count, instructionsWithPCs, currentAfterCountsPC)
    }
    if(newArrayExpr.counts.size > 1) {
      // Construct the array type string for the multi-dimensional array
      val arrayTypeString = newArrayExpr.tpe match {
        case arrayType: ArrayType => constructArrayTypeString(arrayType)
        case _ => throw new IllegalArgumentException("Expected an array type for MULTIANEWARRAY")
      }
      val instruction = MULTIANEWARRAY(arrayTypeString, newArrayExpr.counts.size)
      instructionsWithPCs += ((currentAfterCountsPC, instruction))
      return currentAfterCountsPC + instruction.length
    }
    val instruction = newArrayExpr.tpe.componentType match {
      case _: ReferenceType => ANEWARRAY(newArrayExpr.tpe.componentType.toJava.replace(".", "/"))
      case _: BooleanType => NEWARRAY(BooleanType.atype)
      case _: CharType => NEWARRAY(CharType.atype)
      case _: FloatType => NEWARRAY(FloatType.atype)
      case _: DoubleType => NEWARRAY(DoubleType.atype)
      case _: ByteType => NEWARRAY(ShortType.atype)
      case _: ShortType => NEWARRAY(ShortType.atype)
      case _: IntegerType => NEWARRAY(IntegerType.atype)
      case _: LongType => NEWARRAY(LongType.atype)
      case _ => throw new IllegalArgumentException("Unsupported array load type")
    }
    instructionsWithPCs += ((currentAfterCountsPC, instruction))
    currentAfterCountsPC + instruction.length
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

  def handleArrayLoad(arrayLoadExpr: ArrayLoad[_], instructionsWithPCs: ArrayBuffer[(Int, Instruction)], currentPC: Int): Int = {
    // Load the array reference onto the stack
    val pcAfterArrayRefLoad = processExpression(arrayLoadExpr.arrayRef, instructionsWithPCs, currentPC)
    // Load the index onto the stack
    val pcAfterIndexLoad = processExpression(arrayLoadExpr.index, instructionsWithPCs, pcAfterArrayRefLoad)

    // Infer the element type from the array reference expression
    val elementType = inferElementType(arrayLoadExpr.arrayRef)

    val instruction = elementType match {
      case IntegerType => IALOAD
      case LongType => LALOAD
      case FloatType => FALOAD
      case DoubleType => DALOAD
      case ByteType => BALOAD
      case CharType => CALOAD
      case ShortType => SALOAD
      case _: ReferenceType => AALOAD
      case _ => throw new IllegalArgumentException("Unsupported array load type" + elementType)
    }
    instructionsWithPCs += ((pcAfterIndexLoad, instruction))
    pcAfterIndexLoad + instruction.length
  }


  // Helper function to infer the element type from the array reference expression
  def inferElementType(expr: Expr[_]): Type = {
    expr.asInstanceOf[UVar[_]].value.asInstanceOf[IsSReferenceValue[_]].theUpperTypeBound.asInstanceOf[ArrayType] match {
      case ArrayType(componentType) => componentType
      case _ => throw new IllegalArgumentException(s"Expected an array type but found: ${expr.cTpe}")
    }
  }
  def handleArrayLength(arrayLength: ArrayLength[_], instructionsWithPCs: ArrayBuffer[(Int, Instruction)], currentPC: Int): Int = {
    // Process the receiver object (e.g., aload_0 for `this`)
    val afterReceiverPC = ExprProcessor.processExpression(arrayLength.arrayRef, instructionsWithPCs, currentPC)
    val instruction = ARRAYLENGTH
    instructionsWithPCs += ((afterReceiverPC, instruction))
    afterReceiverPC + instruction.length
  }

  def handleNewExpr(tpe: ObjectType, instructionsWithPCs: ArrayBuffer[(Int, Instruction)], currentPC: Int): Int = {
    val instruction = NEW(tpe)
    instructionsWithPCs += ((currentPC, instruction))
    currentPC + instruction.length
  }

  def handleStaticFunctionCall(expr: StaticFunctionCall[_], instructionsWithPCs: ArrayBuffer[(Int, Instruction)], currentPC: Int): Int = {
    // Initialize the PC after processing the receiver
    var currentAfterParamsPC = currentPC

    // Process each parameter and update the PC accordingly
    for (param <- expr.params) {
      currentAfterParamsPC = ExprProcessor.processExpression(param, instructionsWithPCs, currentAfterParamsPC)
    }
    val instruction = if (expr.isInterface) {
      INVOKEINTERFACE(expr.declaringClass, expr.name, expr.descriptor)
      throw new UnsupportedOperationException("Unsupported expression type" + expr)
    } else {
      INVOKESTATIC(expr.declaringClass, expr.isInterface, expr.name, expr.descriptor)
    }

    instructionsWithPCs += ((currentAfterParamsPC, instruction))
    currentAfterParamsPC + instruction.length
  }

  def handleVirtualFunctionCall(expr: VirtualFunctionCall[_], instructionsWithPCs: ArrayBuffer[(Int, Instruction)], currentPC: Int): Int = {
    // Process the receiver object (e.g., aload_0 for `this`)
    val afterReceiverPC = ExprProcessor.processExpression(expr.receiver, instructionsWithPCs, currentPC)

    // Initialize the PC after processing the receiver
    var currentAfterParamsPC = afterReceiverPC

    // Process each parameter and update the PC accordingly
    for (param <- expr.params) {
      currentAfterParamsPC = ExprProcessor.processExpression(param, instructionsWithPCs, currentAfterParamsPC)
    }
    val instruction = if (expr.isInterface) {
      //INVOKEINTERFACE(expr.declaringClass, expr.name, expr.descriptor)
      throw new UnsupportedOperationException("Unsupported expression type" + expr)
    } else {
      INVOKEVIRTUAL(expr.declaringClass, expr.name, expr.descriptor)
    }

    instructionsWithPCs += ((currentAfterParamsPC, instruction))
    currentAfterParamsPC + instruction.length
  }

  private def loadConstant(constExpr: Const, instructionsWithPCs: ArrayBuffer[(Int, Instruction)], currentPC: Int): Int = {
    val instruction = {
      if(constExpr.isNullExpr){
        ACONST_NULL
      }else
      constExpr match {
      case IntConst(_, value) => value match {
        case -1 => ICONST_M1
        case 0 => ICONST_0
        case 1 => ICONST_1
        case 2 => ICONST_2
        case 3 => ICONST_3
        case 4 => ICONST_4
        case 5 => ICONST_5
        case _ if value >= Byte.MinValue && value <= Byte.MaxValue => BIPUSH(value)
        case _ if value >= Short.MinValue && value <= Short.MaxValue => SIPUSH(value)
        case _ => LoadInt(value)
      }
      case FloatConst(_, value) => value match {
        case 0 => FCONST_0
        case 1 => FCONST_1
        case 2 => FCONST_2
        case _ => LoadFloat(value)
      }
      case ClassConst(_, value) => LoadClass(value)
      case StringConst(_, value) => LoadString(value)
      case MethodHandleConst(_, value) => LoadMethodHandle(value)
      case MethodTypeConst(_, value) => LoadMethodType(value)
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
      //todo: figure out how and what LoadDynamic is
      //I think LDCDynamic is not an actual Instruction.
      /*case Assignment(_, _, DynamicConst(_, bootstrapMethod, name, descriptor)) =>
        val instruction = LoadDynamic(-1, bootstrapMethod, name, descriptor)
        instructionsWithPCs += ((currentPC, instruction))
        currentPC += instruction.length*/
      case _ =>
        //todo: check that this is the right exception to throw
        throw BytecodeProcessingFailedException(
          "unsupported constant value: " + constExpr
        )
    }
    }
    instructionsWithPCs += ((currentPC, instruction))
    currentPC + instruction.length // Update and return the new program counter
  }

  var uVarToLVIndex = mutable.Map[IntTrieSet, Int]()
  var nextLVIndex = 1

  // Method to get LVIndex for a variable
  def getVariableLvlIndex(variable: Var[_]): Int = {
    variable match {
      case dVar: DVar[_] =>
        val uVarDefSites = uVarToLVIndex.find { case (defSites, _) => defSites.contains(dVar.origin) }
        uVarDefSites match {
          case Some((_, index)) => index
          case None => throw new NoSuchElementException(s"No LVIndex found for DVar with origin ${dVar.origin}")
        }
      case uVar: UVar[_] =>
        val defSiteMatch = uVarToLVIndex.find { case (defSites, _) => defSites.exists(uVar.defSites.contains) }
        defSiteMatch match {
          case Some((_, index)) => index
          case None => throw new NoSuchElementException(s"No LVIndex found for UVar with defSites ${uVar.defSites}")
        }
      case _ => throw new UnsupportedOperationException("Unsupported variable type")
    }
  }

  def loadVariable(variable: Var[_], instructionsWithPCs: ArrayBuffer[(Int, Instruction)], currentPC: Int): Int = {
    val index = getVariableLvlIndex(variable)
      val instruction = variable.cTpe match {
        case ComputationalTypeInt => index match {
          case 0 => ILOAD_0
          case 1 => ILOAD_1
          case 2 => ILOAD_2
          case 3 => ILOAD_3
          case _ => ILOAD(index)
        }
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
        case _ => throw new UnsupportedOperationException("Unsupported computational type for loading variable" + variable)
      }
      instructionsWithPCs += ((currentPC, instruction))
      currentPC + (if (index < 4) 1 else 2)
  }

  def storeVariable(variable: Var[_], instructionsWithPCs: ArrayBuffer[(Int, Instruction)], currentPC: Int): Int = {
    //val variableName = handleDVarName(variable)
    val index = getVariableLvlIndex(variable)
      val storeInstruction = variable.cTpe match {
        case ComputationalTypeInt => index match {
          //The <n> must be an index into the local variable array of the current frame (§2.6).
          // The value on the top of the operand stack must be of type int. It is popped from the operand stack, and the value of the local variable at <n> is set to value.
          case 0 => ISTORE_0
          case 1 => ISTORE_1
          case 2 => ISTORE_2
          case 3 => ISTORE_3
          case _ => ISTORE(index)
        }
        case ComputationalTypeFloat => index match {
          case 0 => FSTORE_0
          case 1 => FSTORE_1
          case 2 => FSTORE_2
          case 3 => FSTORE_3
          case _ => FSTORE(index)
        }
        case ComputationalTypeDouble => index match {
          case 0 => DSTORE_0
          case 1 => DSTORE_1
          case 2 => DSTORE_2
          case 3 => DSTORE_3
          case _ => DSTORE(index)
        }
        case ComputationalTypeLong => index match {
          case 0 => LSTORE_0
          case 1 => LSTORE_1
          case 2 => LSTORE_2
          case 3 => LSTORE_3
          case _ => LSTORE(index)
        }
        case ComputationalTypeReference => index match {
          case 0 => ASTORE_0
          case 1 => ASTORE_1
          case 2 => ASTORE_2
          case 3 => ASTORE_3
          case _ => ASTORE(index)
        }
        case _ => throw new UnsupportedOperationException("Unsupported computational type for storing variable" + variable)
      }
    instructionsWithPCs += ((currentPC, storeInstruction))
    currentPC + (if (index < 4) 1 else 2)
  }

  def handleGetField(getField: GetField[_], instructionsWithPCs: ArrayBuffer[(Int, Instruction)], currentPC: Int): Int = {
    // Load the object reference onto the stack
    val pcAfterObjectRefLoad = processExpression(getField.objRef, instructionsWithPCs, currentPC)
    // Generate the GETFIELD instruction
    val instruction = GETFIELD(getField.declaringClass, getField.name, getField.declaredFieldType)
    instructionsWithPCs += ((pcAfterObjectRefLoad, instruction))
    pcAfterObjectRefLoad + instruction.length // Update and return the new program counter
  }

  def handleGetStatic(getStatic: GetStatic, instructionsWithPCs: ArrayBuffer[(Int, Instruction)], currentPC: Int): Int = {
    val instruction = GETSTATIC(getStatic.declaringClass, getStatic.name, getStatic.declaredFieldType)
    instructionsWithPCs += ((currentPC, instruction))
    currentPC + instruction.length // Update and return the new program counter
  }

  def handleBinaryExpr(binaryExpr: BinaryExpr[_], instructionsWithPCs: ArrayBuffer[(Int, Instruction)], currentPC: Int): Int = {
    // process the left expr and save the pc to give in the right expr processing
    val leftPC = processExpression(binaryExpr.left, instructionsWithPCs, currentPC)
    // process the right Expr
    val rightPC = processExpression(binaryExpr.right, instructionsWithPCs, leftPC)
    val (instruction, instructionLength) = (binaryExpr.cTpe, binaryExpr.op) match {
      //Double
      case (ComputationalTypeDouble, Add) => (DADD, DADD.length)
      case (ComputationalTypeDouble, Subtract) => (DSUB, DSUB.length)
      case (ComputationalTypeDouble, Multiply) => (DMUL, DMUL.length)
      case (ComputationalTypeDouble, Divide) => (DDIV, DDIV.length)
      case (ComputationalTypeDouble, Modulo) => (DREM, DREM.length)
      //Todo figure out where and how to do with Negate
      //Float
      case (ComputationalTypeFloat, Add) => (FADD, FADD.length)
      case (ComputationalTypeFloat, Subtract) => (FSUB, FSUB.length)
      case (ComputationalTypeFloat, Multiply) => (FMUL, FMUL.length)
      case (ComputationalTypeFloat, Divide) => (FDIV, FDIV.length)
      case (ComputationalTypeFloat, Modulo) => (FREM, FREM.length)
      //Int
      case (ComputationalTypeInt, Add) => (IADD, IADD.length)
      case (ComputationalTypeInt, Subtract) => (ISUB, ISUB.length)
      case (ComputationalTypeInt, Multiply) => (IMUL, IMUL.length)
      case (ComputationalTypeInt, Divide) => (IDIV, IDIV.length)
      case (ComputationalTypeInt, Modulo) => (IREM, IREM.length)
      case (ComputationalTypeInt, And) => (IAND, IAND.length)
      case (ComputationalTypeInt, Or) => (IOR, IOR.length)
      case (ComputationalTypeInt, ShiftLeft) => (ISHL, ISHL.length)
      case (ComputationalTypeInt, ShiftRight) => (ISHR, ISHR.length)
      case (ComputationalTypeInt, UnsignedShiftRight) => (IUSHR, IUSHR.length)
      case (ComputationalTypeInt, XOr) => (IXOR, IXOR.length)
      //Long
      case (ComputationalTypeLong, Add) => (LADD, LADD.length)
      case (ComputationalTypeLong, Subtract) => (LSUB, LSUB.length)
      case (ComputationalTypeLong, Multiply) => (LMUL, LMUL.length)
      case (ComputationalTypeLong, Divide) => (LDIV, LDIV.length)
      case (ComputationalTypeLong, Modulo) => (LREM, LREM.length)
      case (ComputationalTypeLong, And) => (LAND, LAND.length)
      case (ComputationalTypeLong, Or) => (LOR, LOR.length)
      case (ComputationalTypeLong, ShiftLeft) => (LSHL, LSHL.length)
      case (ComputationalTypeLong, ShiftRight) => (LSHR, LSHR.length)
      case (ComputationalTypeLong, UnsignedShiftRight) => (LUSHR, LUSHR.length)
      case (ComputationalTypeLong, XOr) => (LXOR, LXOR.length)
      //Unsupported
      case _ => throw new UnsupportedOperationException("Unsupported operation or computational type in BinaryExpr" + binaryExpr)
    }
    val offsetPC = currentPC + (rightPC - currentPC)
    instructionsWithPCs += ((offsetPC, instruction))
    offsetPC + instructionLength
  }
  def handlePrimitiveTypeCastExpr(primitiveTypecastExpr: PrimitiveTypecastExpr[_], instructionsWithPCs: ArrayBuffer[(Int, Instruction)], currentPC: Int): Int = {
    // First, process the operand expression and add its instructions to the buffer
    val operandPC = processExpression(primitiveTypecastExpr.operand, instructionsWithPCs, currentPC)

    val instruction = (primitiveTypecastExpr.operand.cTpe, primitiveTypecastExpr.targetTpe) match {
      // -> to Float
      case (ComputationalTypeDouble, FloatType) => D2F
      case (ComputationalTypeInt, FloatType) => I2F
      case (ComputationalTypeLong, FloatType) => L2F
      // -> to Int
      case (ComputationalTypeDouble, IntegerType) => D2I
      case (ComputationalTypeFloat, IntegerType) => F2I
      case (ComputationalTypeLong, IntegerType) => L2I
      // -> to Long
      case (ComputationalTypeDouble, LongType) => D2L
      case (ComputationalTypeInt, LongType) => I2L
      case (ComputationalTypeFloat, LongType) => F2L
      // -> to Double
      case (ComputationalTypeFloat, DoubleType) => F2D
      case (ComputationalTypeInt, DoubleType) => I2D
      case (ComputationalTypeLong, DoubleType) => L2D
      // -> to Char
      case (ComputationalTypeInt, CharType) => I2C
      // -> to Byte
      case (ComputationalTypeInt, ByteType) => I2B
      // -> to Short
      case (ComputationalTypeInt, ShortType) => I2S
      // -> other cases are not supported
      case _ => throw new UnsupportedOperationException("Unsupported operation or computational type in PrimitiveTypecastExpr" + primitiveTypecastExpr)
    }
    instructionsWithPCs += ((operandPC, instruction))
    operandPC + instruction.length
  }
}
