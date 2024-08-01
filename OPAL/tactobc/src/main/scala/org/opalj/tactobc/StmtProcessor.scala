/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tactobc

import org.opalj.RelationalOperator
import org.opalj.RelationalOperators._
import org.opalj.br.{BootstrapMethod, ComputationalTypeDouble, ComputationalTypeFloat, ComputationalTypeInt, ComputationalTypeLong, ComputationalTypeReference, FieldType, MethodDescriptor, ObjectType, PCs, ReferenceType}
import org.opalj.br.instructions.{AASTORE, ARETURN, ATHROW, DASTORE, DRETURN, FASTORE, FRETURN, GOTO, IASTORE, IFNONNULL, IFNULL, IF_ICMPEQ, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE, IF_ICMPLT, IF_ICMPNE, INVOKESPECIAL, INVOKESTATIC, INVOKEVIRTUAL, IRETURN, Instruction, LASTORE, LOOKUPSWITCH, LRETURN, MONITORENTER, MONITOREXIT, NOP, PUTFIELD, PUTSTATIC, RET, RETURN, TABLESWITCH}
import org.opalj.collection.immutable.{IntIntPair, IntTrieSet}
import org.opalj.tac.{Expr, UVar, Var}

import scala.collection.immutable.ArraySeq
import scala.collection.mutable.ArrayBuffer

object StmtProcessor {

  //Assignment
  def processAssignment(targetVar: Var[_], expr: Expr[_], instructionsWithPCs: ArrayBuffer[(Int, Instruction)], currentPC: Int): Int = {
    // Evaluate the RHS and update the PC accordingly
    val afterExprPC = ExprProcessor.processExpression(expr, instructionsWithPCs, currentPC)
    // Store the result into the target variable and update the PC
    val finalPC = ExprProcessor.storeVariable(targetVar, instructionsWithPCs, afterExprPC)
    // Return the updated PC
    finalPC
  }

  def processSwitch(defaultOffset: Int, index: Expr[_], npairs: ArraySeq[IntIntPair /*(Case Value, Jump Target)*/], instructionsWithPCs: ArrayBuffer[(Int, Instruction)], currentPC: Int): Int = {
    // Translate the index expression first
    val afterExprPC = ExprProcessor.processExpression(index, instructionsWithPCs, currentPC)

    // Prepare the bytecode pairs with placeholders for targets
    val bCnpairs = prepareBCnpairs(npairs)

    if (isLookupSwitch(index)) {
      // Add LOOKUPSWITCH instruction with placeholders for targets
      val lookupswitchInstruction = LOOKUPSWITCH(defaultOffset, bCnpairs)
      instructionsWithPCs += ((afterExprPC, lookupswitchInstruction))
      afterExprPC + lookupSwitchLength(bCnpairs.size, afterExprPC)
    } else {
      // Add TABLESWITCH instruction with placeholders for targets
      val minValue = bCnpairs.minBy(_._1)._1
      val maxValue = bCnpairs.maxBy(_._1)._1
      val jumpTable = ArrayBuffer.fill(maxValue - minValue + 1)(-1)

      // Set the case values in the jump table
      bCnpairs.foreach { case IntIntPair(caseValue, _) =>
        jumpTable(caseValue - minValue) = -1
      }

      val tableswitchInstruction = TABLESWITCH(defaultOffset, minValue, maxValue, jumpTable.to(ArraySeq))
      instructionsWithPCs += ((afterExprPC, tableswitchInstruction))
      afterExprPC + tableSwitchLength(minValue, maxValue, afterExprPC)
    }
  }

  def lookupSwitchLength(numPairs: Int, currentPC: Int): Int = {
    // Opcode (1 byte) + padding (0-3 bytes) + default offset (4 bytes) + number of pairs (4 bytes) + pairs (8 bytes each)
    val padding = (4 - (currentPC % 4)) % 4
    1 + padding + 4 + 4 + (numPairs * 8)
  }

  def tableSwitchLength(low: Int, high: Int, currentPC: Int): Int = {
    // Opcode (1 byte) + padding (0-3 bytes) + default offset (4 bytes) + low value (4 bytes) + high value (4 bytes) + jump offsets (4 bytes each)
    val padding = (4 - (currentPC % 4)) % 4
    val numOffsets = high - low + 1
    1 + padding + 4 + 4 + 4 + (numOffsets * 4)
  }

  def prepareBCnpairs(npairs: ArraySeq[IntIntPair]): ArraySeq[IntIntPair] = {
    npairs.map { case IntIntPair(caseValue, _) => IntIntPair(caseValue, -1) }
  }

  def isLookupSwitch(index: Expr[_]): Boolean = {
    index match {
      case variable: UVar[_] => variable.defSites.size == 1
      case _ => false
    }
  }

  def processReturn(instructionsWithPCs: ArrayBuffer[(Int, Instruction)], currentPC: Int): Int = {
    val instruction = RETURN
    instructionsWithPCs += ((currentPC, instruction))
    currentPC + instruction.length
  }

  def processReturnValue(expr: Expr[_], instructionsWithPCs: ArrayBuffer[(Int, Instruction)], currentPC: Int): Int = {
    val afterExprPC = ExprProcessor.processExpression(expr, instructionsWithPCs, currentPC)
    val instruction = expr.cTpe match {
      case ComputationalTypeInt => IRETURN
      case ComputationalTypeLong => LRETURN
      case ComputationalTypeFloat => FRETURN
      case ComputationalTypeDouble => DRETURN
      case ComputationalTypeReference => ARETURN
      case _ => throw new UnsupportedOperationException("Unsupported computational type:" + expr.cTpe)
    }
    val offsetPC = currentPC + (afterExprPC - currentPC)
    instructionsWithPCs += ((currentPC, instruction))
    currentPC + offsetPC
  }

  def processVirtualMethodCall(declaringClass: ReferenceType, isInterface: Boolean, methodName: String, methodDescriptor: MethodDescriptor, receiver: Expr[_], params: Seq[Expr[_]], instructionsWithPCs: ArrayBuffer[(Int, Instruction)], currentPC: Int): Int = {
    // Process the receiver object (e.g., aload_0 for `this`)
    val afterReceiverPC = ExprProcessor.processExpression(receiver, instructionsWithPCs, currentPC)

    // Initialize the PC after processing the receiver
    var currentAfterParamsPC = afterReceiverPC

    // Process each parameter and update the PC accordingly
    for (param <- params) {
      currentAfterParamsPC = ExprProcessor.processExpression(param, instructionsWithPCs, currentAfterParamsPC)
    }

    val instruction = { /*if (isInterface) {
      INVOKEINTERFACE
    }else*/
    INVOKEVIRTUAL(declaringClass, methodName, methodDescriptor)
    }
    //val finalPC = currentPC + pcAfterLoadVariable
    instructionsWithPCs += ((currentAfterParamsPC, instruction))
    currentAfterParamsPC + instruction.length
  }

  def processArrayStore(arrayRef: Expr[_], index: Expr[_], value: Expr[_], instructionsWithPCs: ArrayBuffer[(Int, Instruction)], currentPC: Int): Int = {
    //todo: handle this correctly
    val pcAfterArrayRefLoad = ExprProcessor.processExpression(arrayRef, instructionsWithPCs, currentPC)

    // Load the index onto the stack
    val pcAfterIndexLoad = ExprProcessor.processExpression(index, instructionsWithPCs, pcAfterArrayRefLoad)

    // Load the value to be stored onto the stack
    val pcAfterValueLoad = ExprProcessor.processExpression(value, instructionsWithPCs, pcAfterIndexLoad)

    // Determine the type of the value to be stored and select the appropriate store instruction
    val instruction = value.cTpe match {
      case ComputationalTypeInt => IASTORE
      case ComputationalTypeLong => LASTORE
      case ComputationalTypeFloat => FASTORE
      case ComputationalTypeDouble => DASTORE
      //case UVar(_, _: Byte) => BASTORE
      //case UVar(_, _: Char) => CASTORE
      //case UVar(_, _: Short) => SASTORE
      case ComputationalTypeReference => AASTORE
      case _ => throw new IllegalArgumentException("Unsupported array store type")
    }

    // Add the store instruction
    instructionsWithPCs += ((pcAfterValueLoad, instruction))
    pcAfterValueLoad + instruction.length
  }

  def processNop(instructionsWithPCs: ArrayBuffer[(Int, Instruction)], currentPC: Int): Int = {
    val instruction = NOP
    instructionsWithPCs += ((currentPC, instruction))
    currentPC + instruction.length
  }

  def processInvokeDynamicMethodCall(bootstrapMethod: BootstrapMethod, name: String, descriptor: MethodDescriptor, params: Seq[Expr[_]]): Int = {
    //todo: handle this correctly
    1
  }

  def processCheckCast(value: Expr[_], cmpTpe: ReferenceType, instructionsWithPCs: ArrayBuffer[(Int, Instruction)], currentPC: Int): Int = {
    //todo: handle this correctly
    1
  }

  def processRet(returnAdresses: PCs, instructionsWithPCs: ArrayBuffer[(Int, Instruction)], currentPC: Int): Int = {
    //todo: handle returnAdresses this correctly
    val instruction = RET(returnAdresses.size)
    instructionsWithPCs += ((currentPC, instruction))
    currentPC + instruction.length
  }

  def processCaughtException(exceptionType: Option[ObjectType], throwingStmts: IntTrieSet, instructionsWithPCs: ArrayBuffer[(Int, Instruction)], currentPC: Int): Int = {
    //todo: handle this correctly
    1
  }

  def processThrow(exception: Expr[_], instructionsWithPCs: ArrayBuffer[(Int, Instruction)], currentPC: Int): Int = {
    //todo: handle this correctly
    val instruction = ATHROW
    instructionsWithPCs += ((currentPC, instruction))
    currentPC + 1
  }

  def processPutStatic(declaringClass: ObjectType, name: String, declaredFieldType: FieldType, value: Expr[_], instructionsWithPCs: ArrayBuffer[(Int, Instruction)], currentPC: Int): Int = {
    val pcAfterValueExpr = ExprProcessor.processExpression(value, instructionsWithPCs, currentPC)
    val instruction = PUTSTATIC(declaringClass, name, declaredFieldType)
    instructionsWithPCs += ((pcAfterValueExpr, instruction))
    pcAfterValueExpr + instruction.length
  }

  def processPutField(declaringClass: ObjectType, name: String, declaredFieldType: FieldType, objRef: Expr[_], value: Expr[_], instructionsWithPCs: ArrayBuffer[(Int, Instruction)], currentPC: Int): Int = {
    // Load the object reference onto the stack
    val pcAfterObjRefLoad = ExprProcessor.processExpression(objRef, instructionsWithPCs, currentPC)
    // Load the value to be stored onto the stack
    val pcAfterValueLoad = ExprProcessor.processExpression(value, instructionsWithPCs, pcAfterObjRefLoad)
    val instruction = PUTFIELD(declaringClass, name, declaredFieldType)
    instructionsWithPCs += ((pcAfterValueLoad, instruction))
    pcAfterValueLoad + instruction.length
  }

  def processMonitorEnter(objRef: Expr[_], instructionsWithPCs: ArrayBuffer[(Int, Instruction)], currentPC: Int): Int = {
    // Load the object reference onto the stack
    val pcAfterObjRefLoad = ExprProcessor.processExpression(objRef, instructionsWithPCs, currentPC)
    val instruction = MONITORENTER
    instructionsWithPCs += ((pcAfterObjRefLoad, instruction))
    pcAfterObjRefLoad + instruction.length
  }
  def processMonitorExit(objRef: Expr[_], instructionsWithPCs: ArrayBuffer[(Int, Instruction)], currentPC: Int): Int = {
    // Load the object reference onto the stack
    val pcAfterObjRefLoad = ExprProcessor.processExpression(objRef, instructionsWithPCs, currentPC)
    val instruction = MONITOREXIT
    instructionsWithPCs += ((pcAfterObjRefLoad, instruction))
    pcAfterObjRefLoad + instruction.length
  }

  def processJSR(target: Int, instructionsWithPCs: ArrayBuffer[(Int, Instruction)], currentPC: Int): Int = {
    //todo: look what to do with the target, how to get the length and if it is a jump instruction
    //val instruction = JSR
    //instructionsWithPCs += ((currentPC, instruction))
    currentPC + 1
  }

  def processNonVirtualMethodCall(declaringClass: ObjectType, isInterface: Boolean, methodName: String, methodDescriptor: MethodDescriptor, receiver: Expr[_], params: Seq[Expr[_]], instructionsWithPCs: ArrayBuffer[(Int, Instruction)], currentPC: Int): Int = {
    val afterReceiverPC = ExprProcessor.processExpression(receiver, instructionsWithPCs, currentPC)

    // Initialize the PC after processing the receiver
    var currentAfterParamsPC = afterReceiverPC

    // Process each parameter and update the PC accordingly
    for (param <- params) {
      currentAfterParamsPC = ExprProcessor.processExpression(param, instructionsWithPCs, currentAfterParamsPC)
    }
    val instruction = INVOKESPECIAL(declaringClass, isInterface, methodName, methodDescriptor)
    val finalPC = currentPC + currentAfterParamsPC
    instructionsWithPCs += ((finalPC, instruction))
    finalPC + instruction.length
  }

  def processStaticMethodCall(declaringClass: ObjectType, isInterface: Boolean, methodName: String, methodDescriptor: MethodDescriptor, params: Seq[Expr[_]], instructionsWithPCs: ArrayBuffer[(Int, Instruction)], currentPC: Int): Int = {
    // Initialize the PC after processing the receiver
    var currentAfterParamsPC = currentPC

    // Process each parameter and update the PC accordingly
    for (param <- params) {
      currentAfterParamsPC = ExprProcessor.processExpression(param, instructionsWithPCs, currentAfterParamsPC)
    }
    val instruction = INVOKESTATIC(declaringClass, isInterface, methodName, methodDescriptor)
    instructionsWithPCs += ((currentAfterParamsPC, instruction))
    currentAfterParamsPC + instruction.length
  }

  def processGoto(instructionsWithPCs: ArrayBuffer[(Int, Instruction)], currentPC: Int): Int = {
    val instruction = GOTO(-1)
    instructionsWithPCs += ((currentPC, instruction))
    val length = instruction.length
    currentPC + length
  }

  def processIf(left: Expr[_], condition: RelationalOperator, right: Expr[_], gotoLabel: Int, instructionsWithPCs: ArrayBuffer[(Int, Instruction)], currentPC: Int): Int = {
    // process the left expr and save the pc to give in the right expr processing
    val leftPC = ExprProcessor.processExpression(left, instructionsWithPCs, currentPC)
    // process the right expr
    val rightPC = ExprProcessor.processExpression(right, instructionsWithPCs, leftPC)
    generateIfInstruction(left, condition, right, instructionsWithPCs, currentPC, rightPC)
  }

  def generateIfInstruction(left: Expr[_], condition: RelationalOperator, right: Expr[_], instructionsWithPCs: ArrayBuffer[(Int, Instruction)], currentPC: Int, rightPC: Int): Int = {
    val instruction = (left, right) match {
      case _ if right.isNullExpr || left.isNullExpr =>
        condition match {
          case EQ  => IFNULL(-1)
          case NE  => IFNONNULL(-1)
          case _ => throw new UnsupportedOperationException(s"Unsupported condition: $condition")
        }
      case _ =>
        condition match {
          case EQ  => IF_ICMPEQ(-1)
          case NE  => IF_ICMPNE(-1)
          case LT  => IF_ICMPLT(-1)
          case LE  => IF_ICMPLE(-1)
          case GT  => IF_ICMPGT(-1)
          case GE  => IF_ICMPGE(-1)
          case _ => throw new UnsupportedOperationException(s"Unsupported condition: $condition")
        }
          /*case EQ => IF_ACMPEQ(-1)
          case NE  => IF_ACMPNE(-1)
          //Unsupported
          case _ => IFNE(-1)//throw new UnsupportedOperationException(s"Unsupported condition: $condition")*/
    }
    val offsetPC = {
      if(rightPC > 0 && rightPC > currentPC){
        currentPC + (rightPC - currentPC)
      } else {
        currentPC
      }
    }
    instructionsWithPCs += ((offsetPC, instruction))
    offsetPC + instruction.length
  }
}
