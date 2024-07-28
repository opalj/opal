/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tactobc

import org.opalj.RelationalOperator
import org.opalj.RelationalOperators._
import org.opalj.br.{BootstrapMethod, ComputationalTypeDouble, ComputationalTypeFloat, ComputationalTypeInt, ComputationalTypeLong, ComputationalTypeReference, FieldType, MethodDescriptor, ObjectType, PCs, ReferenceType}
import org.opalj.br.instructions.{ARETURN, ATHROW, DRETURN, FRETURN, GOTO, IFNONNULL, IFNULL, IF_ICMPEQ, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE, IF_ICMPLT, IF_ICMPNE, INVOKESPECIAL, INVOKESTATIC, INVOKEVIRTUAL, IRETURN, Instruction, LOOKUPSWITCH, LRETURN, MONITORENTER, MONITOREXIT, PUTFIELD, PUTSTATIC, RET, RETURN, TABLESWITCH}
import org.opalj.collection.immutable.{IntIntPair, IntTrieSet}
import org.opalj.tac.{Expr, UVar, Var}

import scala.collection.immutable.ArraySeq
import scala.collection.mutable.ArrayBuffer

object StmtProcessor {

  //Assignment
  def processAssignment(targetVar: Var[_], expr: Expr[_], instructionsWithPCs: ArrayBuffer[(Int, Instruction)], currentPC: Int): Int = {
    // Evaluate the RHS and update the PC accordingly
    val afterExprPC = ExprUtils.processExpression(expr, instructionsWithPCs, currentPC)
    // Store the result into the target variable and update the PC
    val finalPC = ExprUtils.storeVariable(targetVar, instructionsWithPCs, afterExprPC)
    // Return the updated PC
    finalPC
  }

  def processSwitch(defaultOffset: Int, index: Expr[_], npairs: ArraySeq[IntIntPair /*(Case Value, Jump Target)*/], instructionsWithPCs: ArrayBuffer[(Int, Instruction)], currentPC: Int): Int = {
    // Translate the index expression first
    val afterExprPC = ExprUtils.processExpression(index, instructionsWithPCs, currentPC)

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
    val afterExprPC = ExprUtils.processExpression(expr, instructionsWithPCs, currentPC)
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
    val afterReceiverPC = ExprUtils.processExpression(receiver, instructionsWithPCs, currentPC)

    // Initialize the PC after processing the receiver
    var currentAfterParamsPC = afterReceiverPC

    // Process each parameter and update the PC accordingly
    for (param <- params) {
      currentAfterParamsPC = ExprUtils.processExpression(param, instructionsWithPCs, currentAfterParamsPC)
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
    1
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
    //todo: look what to do with the value :)
    val instruction = PUTSTATIC(declaringClass, name, declaredFieldType)
    instructionsWithPCs += ((currentPC, instruction))
    currentPC + instruction.length
  }

  def processPutField(declaringClass: ObjectType, name: String, declaredFieldType: FieldType, objRef: Expr[_], value: Expr[_], instructionsWithPCs: ArrayBuffer[(Int, Instruction)], currentPC: Int): Int = {
    //todo: look what to do with the value AND the objRef :)
    val instruction = PUTFIELD(declaringClass, name, declaredFieldType)
    instructionsWithPCs += ((currentPC, instruction))
    currentPC + instruction.length
  }

  def processMonitorEnter(objRef: Expr[_], instructionsWithPCs: ArrayBuffer[(Int, Instruction)], currentPC: Int): Int = {
    //todo: look what to do with the objRef :)
    val instruction = MONITORENTER
    instructionsWithPCs += ((currentPC, instruction))
    currentPC + instruction.length
  }
  def processMonitorExit(objRef: Expr[_], instructionsWithPCs: ArrayBuffer[(Int, Instruction)], currentPC: Int): Int = {
    //todo: look what to do with the objRef :)
    val instruction = MONITOREXIT
    instructionsWithPCs += ((currentPC, instruction))
    currentPC + instruction.length
  }

  def processJSR(target: Int, instructionsWithPCs: ArrayBuffer[(Int, Instruction)], currentPC: Int): Int = {
    //todo: look what to do with the target, how to get the length and if it is a jump instruction
    //val instruction = JSR
    //instructionsWithPCs += ((currentPC, instruction))
    currentPC + 1
  }

  def processNonVirtualMethodCall(declaringClass: ObjectType, isInterface: Boolean, methodName: String, methodDescriptor: MethodDescriptor, receiver: Expr[_], params: Seq[Expr[_]], instructionsWithPCs: ArrayBuffer[(Int, Instruction)], currentPC: Int): Int = {
    val afterReceiverPC = ExprUtils.processExpression(receiver, instructionsWithPCs, currentPC)

    // Initialize the PC after processing the receiver
    var currentAfterParamsPC = afterReceiverPC

    // Process each parameter and update the PC accordingly
    for (param <- params) {
      currentAfterParamsPC = ExprUtils.processExpression(param, instructionsWithPCs, currentAfterParamsPC)
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
      currentAfterParamsPC = ExprUtils.processExpression(param, instructionsWithPCs, currentAfterParamsPC)
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
    val leftPC = ExprUtils.processExpression(left, instructionsWithPCs, currentPC)
    // process the right expr
    val rightPC = ExprUtils.processExpression(right, instructionsWithPCs, leftPC)
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
