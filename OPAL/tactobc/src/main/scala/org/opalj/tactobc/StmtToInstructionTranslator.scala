package org.opalj.tactobc

import scala.collection.mutable

import org.opalj.ba.CodeElement
import org.opalj.ba.LabelElement
import org.opalj.br.instructions.POP
import org.opalj.br.instructions.POP2
import org.opalj.br.instructions.RewriteLabel
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.tac._
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
        uVarToLVIndex: mutable.Map[IntTrieSet, Int]
    ): Seq[CodeElement[Nothing]] = {

        // generate Label for each TAC-Stmt -> index in TAC-Array = corresponding label
        // e.g. labelMap(2) = RewriteLabel of TAC-Statement at index 2
        val labelMap = tacStmts.map(_ => RewriteLabel())

        // list of all CodeElements including bytecode instructions as well as pseudo instructions
        val listedCodeElements = mutable.ListBuffer[CodeElement[Nothing]]()

        // zipWithIndex -> every Tac-Stmt gets its own index -> allows finding the right Label in the labelMap
        tacStmts.zipWithIndex.foreach { case ((stmt, _), tacIndex) =>
            // add label to the list
            listedCodeElements += LabelElement(labelMap(tacIndex))
            stmt match {
                case Assignment(_, targetVar, expr) =>
                    StmtProcessor.processAssignment(targetVar, expr, uVarToLVIndex, listedCodeElements)
                case ArrayStore(_, arrayRef, index, value) =>
                    StmtProcessor.processArrayStore(arrayRef, index, value, uVarToLVIndex, listedCodeElements)
                case CaughtException(_, exceptionType, throwingStmts) =>
                    // TODO: handle CaughtExceptions
                    StmtProcessor.processCaughtException(
                        exceptionType,
                        throwingStmts,
                        listedCodeElements,
                        tacStmts,
                        labelMap,
                        tacIndex
                    )
                case ExprStmt(_, expr) =>
                    ExprProcessor.processExpression(expr, uVarToLVIndex, listedCodeElements)
                    listedCodeElements += (if (expr.cTpe.isCategory2) POP2 else POP)
                case If(_, left, condition, right, target) =>
                    StmtProcessor.processIf(left, condition, right, labelMap(target), uVarToLVIndex, listedCodeElements)
                case Goto(_, target) =>
                    StmtProcessor.processGoto(labelMap(target), listedCodeElements)
                case Switch(_, defaultTarget, index, npairs) =>
                    StmtProcessor.processSwitch(
                        labelMap(defaultTarget),
                        index,
                        npairs,
                        uVarToLVIndex,
                        listedCodeElements,
                        labelMap
                    )
                case JSR(_, target) =>
                    StmtProcessor.processJSR(labelMap(target), listedCodeElements)
                case VirtualMethodCall(_, declaringClass, isInterface, name, descriptor, receiver, params) =>
                    StmtProcessor.processVirtualMethodCall(
                        declaringClass,
                        isInterface,
                        name,
                        descriptor,
                        receiver,
                        params,
                        uVarToLVIndex,
                        listedCodeElements
                    )
                case NonVirtualMethodCall(_, declaringClass, isInterface, name, descriptor, receiver, params) =>
                    StmtProcessor.processNonVirtualMethodCall(
                        declaringClass,
                        isInterface,
                        name,
                        descriptor,
                        receiver,
                        params,
                        uVarToLVIndex,
                        listedCodeElements
                    )
                case StaticMethodCall(_, declaringClass, isInterface, name, descriptor, params) =>
                    StmtProcessor.processStaticMethodCall(
                        declaringClass,
                        isInterface,
                        name,
                        descriptor,
                        params,
                        uVarToLVIndex,
                        listedCodeElements
                    )
                case InvokedynamicMethodCall(_, bootstrapMethod, name, descriptor, params) =>
                    StmtProcessor.processInvokeDynamicMethodCall(
                        bootstrapMethod,
                        name,
                        descriptor,
                        params,
                        uVarToLVIndex,
                        listedCodeElements
                    )
                case MonitorEnter(_, objRef) =>
                    StmtProcessor.processMonitorEnter(objRef, uVarToLVIndex, listedCodeElements)
                case MonitorExit(_, objRef) =>
                    StmtProcessor.processMonitorExit(objRef, uVarToLVIndex, listedCodeElements)
                case PutField(_, declaringClass, name, declaredFieldType, objRef, value) =>
                    StmtProcessor.processPutField(
                        declaringClass,
                        name,
                        declaredFieldType,
                        objRef,
                        value,
                        uVarToLVIndex,
                        listedCodeElements
                    )
                case PutStatic(_, declaringClass, name, declaredFieldType, value) =>
                    StmtProcessor.processPutStatic(
                        declaringClass,
                        name,
                        declaredFieldType,
                        value,
                        uVarToLVIndex,
                        listedCodeElements
                    )
                case Checkcast(_, value, cmpTpe) =>
                    StmtProcessor.processCheckCast(value, cmpTpe, uVarToLVIndex, listedCodeElements)
                case Ret(_, returnAddresses) =>
                    StmtProcessor.processRet(returnAddresses, listedCodeElements)
                case ReturnValue(_, expr) =>
                    StmtProcessor.processReturnValue(expr, uVarToLVIndex, listedCodeElements)
                case Return(_) =>
                    StmtProcessor.processReturn(listedCodeElements)
                case Throw(_, exception) =>
                    StmtProcessor.processThrow(exception, uVarToLVIndex, listedCodeElements)
                case Nop(_) =>
                    StmtProcessor.processNop(listedCodeElements)
                case _ => throw new UnsupportedOperationException(s"Unsupported TAC-Stmt: $stmt")
            }
        }
        listedCodeElements.toSeq
    }
}
