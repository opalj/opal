/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package ide
package solver

import org.opalj.br.Method
import org.opalj.br.cfg.BasicBlock
import org.opalj.br.cfg.CFG
import org.opalj.value.ValueInformation

/**
 * Class to model statements used with IDE analyses.
 *
 * @param tacIndex the index of the statement in the TAC
 * @param isReturnNode whether the statement models the return node of a call
 *
 * @author Robin Körkemeier
 */
case class JavaStatement(
    method:       Method,
    tacIndex:     Int,
    isReturnNode: Boolean = false,
    stmts:        Array[Stmt[JavaStatement.V]],
    cfg:          CFG[Stmt[JavaStatement.V], TACStmts[JavaStatement.V]]
) {
    def stmt: Stmt[JavaStatement.V] = stmts(tacIndex)

    def basicBlock: BasicBlock = cfg.bb(tacIndex)

    override def hashCode(): Int = {
        (method.hashCode() * 31 + tacIndex) * 31 + (
            if (isReturnNode) { 1 }
            else { 0 }
        )
    }

    override def equals(obj: Any): Boolean = obj match {
        case JavaStatement(method2, tacIndex2, isReturnNode2, _, _) =>
            method == method2 && tacIndex == tacIndex2 && isReturnNode == isReturnNode2
        case _ => false
    }

    override def toString: String = {
        val returnOptional =
            if (isReturnNode) { "(return)" }
            else { "" }
        s"${method.classFile.thisType.simpleName}:${method.name}[$tacIndex]$returnOptional{$stmt}"
    }
}

object JavaStatement {
    type V = DUVar[ValueInformation]

    implicit class StmtAsCall(stmt: Stmt[JavaStatement.V]) {
        def asCall(): Call[V] = stmt.astID match {
            case Assignment.ASTID => stmt.asAssignment.expr.asFunctionCall
            case ExprStmt.ASTID   => stmt.asExprStmt.expr.asFunctionCall
            case _                => stmt.asMethodCall
        }
    }
}
