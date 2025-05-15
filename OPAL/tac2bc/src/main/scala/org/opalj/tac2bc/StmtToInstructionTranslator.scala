package org.opalj
package tac2bc

import scala.collection.mutable

import org.opalj.ba.CodeElement
import org.opalj.ba.LabelElement
import org.opalj.br.instructions.RewriteLabel
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.tac.ArrayStore
import org.opalj.tac.Assignment
import org.opalj.tac.Call
import org.opalj.tac.CaughtException
import org.opalj.tac.Checkcast
import org.opalj.tac.DUVar
import org.opalj.tac.ExprStmt
import org.opalj.tac.Goto
import org.opalj.tac.If
import org.opalj.tac.InvokedynamicMethodCall
import org.opalj.tac.JSR
import org.opalj.tac.MonitorEnter
import org.opalj.tac.MonitorExit
import org.opalj.tac.Nop
import org.opalj.tac.PutField
import org.opalj.tac.PutStatic
import org.opalj.tac.Ret
import org.opalj.tac.Return
import org.opalj.tac.ReturnValue
import org.opalj.tac.Stmt
import org.opalj.tac.Switch
import org.opalj.tac.Throw
import org.opalj.tac.V
import org.opalj.value.ValueInformation

/**
 * Handles the translation of three-address code (TAC) statements
 * into Java bytecode instructions.
 *
 * This object processes each TAC statement and generates corresponding bytecode instructions
 */
object StmtToInstructionTranslator {

    /**
     * Translates TAC statements to bytecode instructions.
     *
     * This method iterates over the given TAC statements, processes each statement according to its type,
     * generates the corresponding bytecode instructions.
     *
     * @param tacStmts Array of tuples where each tuple contains a TAC statement and its index.
     * @param uVarToLVIndex Map that holds information about what variable belongs to which register.
     */
    def translateStmtsToInstructions(
        tacStmts:      Array[(Stmt[DUVar[ValueInformation]], Int)],
        uVarToLVIndex: Map[IntTrieSet, Int]
    ): Seq[CodeElement[Nothing]] = {

        // generate Label for each TAC-Stmt -> index in TAC-Array = corresponding label
        // e.g. labelMap(2) = RewriteLabel of TAC-Statement at index 2
        val labelMap = tacStmts.map(_ => RewriteLabel())

        // list of all CodeElements including bytecode instructions as well as pseudo instructions
        val code = mutable.ListBuffer[CodeElement[Nothing]]()

        tacStmts.foreach { case (stmt, tacIndex) =>
            // add label to the list
            code += LabelElement(labelMap(tacIndex))
            stmt match {
                case Assignment(_, targetVar, expr) =>
                    StmtProcessor.processAssignment(targetVar, expr, uVarToLVIndex, code)
                case ArrayStore(_, arrayRef, index, value) =>
                    StmtProcessor.processArrayStore(arrayRef, index, value, uVarToLVIndex, code)
                case CaughtException(_, exceptionType, throwingStmts) =>
                    // TODO: handle CaughtExceptions
                    StmtProcessor.processCaughtException(
                        exceptionType,
                        throwingStmts,
                        code,
                        tacStmts,
                        labelMap,
                        tacIndex
                    )
                case ExprStmt(_, expr) =>
                    StmtProcessor.processExprStmt(expr, uVarToLVIndex, code)
                case If(_, left, condition, right, target) =>
                    StmtProcessor.processIf(left, condition, right, labelMap(target), uVarToLVIndex, code)
                case Goto(_, target) =>
                    StmtProcessor.processGoto(labelMap(target), code)
                case Switch(_, defaultTarget, index, npairs) =>
                    StmtProcessor.processSwitch(
                        labelMap(defaultTarget),
                        index,
                        npairs,
                        uVarToLVIndex,
                        code,
                        labelMap
                    )
                case JSR(_, target) =>
                    StmtProcessor.processJSR(labelMap(target), code)
                case callStmt: Call[V @unchecked] =>
                    val call @ Call(declaringClass, isInterface, name, descriptor) = callStmt
                    ExprProcessor.processCall(
                        call,
                        declaringClass,
                        isInterface,
                        name,
                        descriptor,
                        uVarToLVIndex,
                        code
                    )
                case InvokedynamicMethodCall(_, bootstrapMethod, name, descriptor, params) =>
                    StmtProcessor.processInvokeDynamicMethodCall(
                        bootstrapMethod,
                        name,
                        descriptor,
                        params,
                        uVarToLVIndex,
                        code
                    )
                case MonitorEnter(_, objRef) =>
                    StmtProcessor.processMonitorEnter(objRef, uVarToLVIndex, code)
                case MonitorExit(_, objRef) =>
                    StmtProcessor.processMonitorExit(objRef, uVarToLVIndex, code)
                case PutField(_, declaringClass, name, declaredFieldType, objRef, value) =>
                    StmtProcessor.processPutField(
                        declaringClass,
                        name,
                        declaredFieldType,
                        objRef,
                        value,
                        uVarToLVIndex,
                        code
                    )
                case PutStatic(_, declaringClass, name, declaredFieldType, value) =>
                    StmtProcessor.processPutStatic(
                        declaringClass,
                        name,
                        declaredFieldType,
                        value,
                        uVarToLVIndex,
                        code
                    )
                case Checkcast(_, value, cmpTpe) =>
                    StmtProcessor.processCheckCast(value, cmpTpe, uVarToLVIndex, code)
                case Ret(_, returnAddresses) =>
                    StmtProcessor.processRet(returnAddresses, code)
                case ReturnValue(_, expr) =>
                    StmtProcessor.processReturnValue(expr, uVarToLVIndex, code)
                case Return(_) =>
                    StmtProcessor.processReturn(code)
                case Throw(_, exception) =>
                    StmtProcessor.processThrow(exception, uVarToLVIndex, code)
                case Nop(_) =>
                    StmtProcessor.processNop(code)
                case _ => throw new UnsupportedOperationException(s"Unsupported TAC-Stmt: $stmt")
            }
        }
        code.toSeq
    }
}
