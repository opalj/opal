package org.opalj.tactobc

import org.opalj.br.instructions.Instruction
import org.opalj.tac.{ArrayStore, Assignment, CaughtException, Checkcast, DUVar, ExprStmt, Goto, If, InvokedynamicMethodCall, JSR, MonitorEnter, MonitorExit, NonVirtualMethodCall, PutField, PutStatic, Ret, Return, ReturnValue, StaticMethodCall, Stmt, Switch, Throw, VirtualMethodCall}
import org.opalj.value.ValueInformation

import scala.collection.mutable.ArrayBuffer

object SecondPass {

  def translateStmtsToInstructions(tacStmts: Array[(Stmt[DUVar[ValueInformation]], Int)], generatedByteCodeWithPC: ArrayBuffer[(Int, Instruction)], tacTargetToByteCodePcs: ArrayBuffer[(Int, Int)], switchCases: ArrayBuffer[(Int, Int)]): Unit = {
    var currentPC = 0
    tacStmts.foreach { case (stmt, _) =>
      stmt match {
        case Assignment(_, targetVar, expr) =>
          tacTargetToByteCodePcs += ((-1, currentPC))
          currentPC = StmtProcessor.processAssignment(targetVar, expr, generatedByteCodeWithPC, currentPC)
        case ArrayStore(_, arrayRef, index, value) =>
          tacTargetToByteCodePcs += ((-1, currentPC))
          currentPC = StmtProcessor.processArrayStore(arrayRef, index, value, generatedByteCodeWithPC, currentPC)
        case CaughtException(_, exceptionType, throwingStmts) =>
          tacTargetToByteCodePcs += ((-1, currentPC))
          currentPC = StmtProcessor.processCaughtException(exceptionType, throwingStmts, generatedByteCodeWithPC, currentPC)
        case ExprStmt(_, expr) =>
          tacTargetToByteCodePcs += ((-1, currentPC))
          currentPC = ExprProcessor.processExpression(expr, generatedByteCodeWithPC, currentPC)
        case If(_, left, condition, right, target) =>
          tacTargetToByteCodePcs += ((target, currentPC))
          currentPC = StmtProcessor.processIf(left, condition, right, target, generatedByteCodeWithPC, currentPC)
        case Goto(_, target) =>
          tacTargetToByteCodePcs += ((target, currentPC))
          currentPC = StmtProcessor.processGoto(generatedByteCodeWithPC, currentPC)
        case Switch(_, defaultTarget, index, npairs) =>
          npairs.foreach { pair =>
            switchCases += ((pair._1, pair._2)) //case values to jump target
          }
          tacTargetToByteCodePcs += ((defaultTarget, currentPC))
          currentPC = StmtProcessor.processSwitch(defaultTarget, index, npairs, generatedByteCodeWithPC, currentPC)
        case JSR(_, target) =>
          tacTargetToByteCodePcs += ((-1, currentPC))
          currentPC = StmtProcessor.processJSR(target, generatedByteCodeWithPC, currentPC)
        case VirtualMethodCall(_, declaringClass, isInterface, name, descriptor, receiver, params) =>
          tacTargetToByteCodePcs += ((-1, currentPC))
          currentPC = StmtProcessor.processVirtualMethodCall(declaringClass, isInterface, name, descriptor, receiver, params, generatedByteCodeWithPC, currentPC)
        case NonVirtualMethodCall(_, declaringClass, isInterface, name, descriptor, receiver, params) =>
          tacTargetToByteCodePcs += ((-1, currentPC))
          currentPC = StmtProcessor.processNonVirtualMethodCall(declaringClass, isInterface, name, descriptor, receiver, params, generatedByteCodeWithPC, currentPC)
        case StaticMethodCall(_, declaringClass, isInterface, name, descriptor, params) =>
          tacTargetToByteCodePcs += ((-1, currentPC))
          currentPC = StmtProcessor.processStaticMethodCall(declaringClass, isInterface, name, descriptor, params, generatedByteCodeWithPC, currentPC)
        case InvokedynamicMethodCall(_, bootstrapMethod, name, descriptor, params) =>
          tacTargetToByteCodePcs += ((-1, currentPC))
          currentPC = StmtProcessor.processInvokeDynamicMethodCall(bootstrapMethod, name, descriptor, params)
        case MonitorEnter(_, objRef) =>
          tacTargetToByteCodePcs += ((-1, currentPC))
          currentPC = StmtProcessor.processMonitorEnter(objRef, generatedByteCodeWithPC, currentPC)
        case MonitorExit(_, objRef) =>
          tacTargetToByteCodePcs += ((-1, currentPC))
          currentPC = StmtProcessor.processMonitorExit(objRef, generatedByteCodeWithPC, currentPC)
        case PutField(_, declaringClass, name, declaredFieldType, objRef, value) =>
          tacTargetToByteCodePcs += ((-1, currentPC))
          currentPC = StmtProcessor.processPutField(declaringClass, name, declaredFieldType, objRef, value, generatedByteCodeWithPC, currentPC)
        case PutStatic(_, declaringClass, name, declaredFieldType, value) =>
          tacTargetToByteCodePcs += ((-1, currentPC))
          currentPC = StmtProcessor.processPutStatic(declaringClass, name, declaredFieldType, value, generatedByteCodeWithPC, currentPC)
        case Checkcast(_, value, cmpTpe) =>
          tacTargetToByteCodePcs += ((-1, currentPC))
          currentPC = StmtProcessor.processCheckCast(value, cmpTpe, generatedByteCodeWithPC, currentPC)
        case Ret(_, returnAddresses) =>
          tacTargetToByteCodePcs += ((-1, currentPC))
          currentPC = StmtProcessor.processRet(returnAddresses, generatedByteCodeWithPC, currentPC)
        case ReturnValue(_, expr) =>
          tacTargetToByteCodePcs += ((-1, currentPC))
          currentPC = StmtProcessor.processReturnValue(expr, generatedByteCodeWithPC, currentPC)
        case Return(_) =>
          tacTargetToByteCodePcs += ((-1, currentPC))
          currentPC = StmtProcessor.processReturn(generatedByteCodeWithPC, currentPC)
        case Throw(_, exception) =>
          tacTargetToByteCodePcs += ((-1, currentPC))
          currentPC = StmtProcessor.processThrow(exception, generatedByteCodeWithPC, currentPC)
        case _ =>
      }
    }
  }

}
