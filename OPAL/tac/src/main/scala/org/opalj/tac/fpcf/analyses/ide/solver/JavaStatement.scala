/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.ide.solver

import org.opalj.br.Method
import org.opalj.br.cfg.BasicBlock
import org.opalj.br.cfg.CFG
import org.opalj.tac.Assignment
import org.opalj.tac.Call
import org.opalj.tac.DUVar
import org.opalj.tac.ExprStmt
import org.opalj.tac.Stmt
import org.opalj.tac.TACStmts
import org.opalj.value.ValueInformation

/**
 * Class to model statements used with IDE analyses
 * @param index the index of the statement in the code
 * @param isReturnNode whether the statement models the return node of a call
 */
case class JavaStatement(
        method:       Method,
        index:        Int,
        isReturnNode: Boolean = false,
        code:         Array[Stmt[JavaStatement.V]],
        cfg:          CFG[Stmt[JavaStatement.V], TACStmts[JavaStatement.V]]
) {
    // TODO (IDE) DOES THIS GET REEVALUATED EACH CALL? DO WE ALWAYS CALL IT AT LEAST ONCE? MAYBE MAKE IT A (LAZY)
    //  PROPERTY
    def stmt: Stmt[JavaStatement.V] = code(index)

    // TODO (IDE) DOES THIS GET REEVALUATED EACH CALL? DO WE ALWAYS CALL IT AT LEAST ONCE? MAYBE MAKE IT A (LAZY)
    //  PROPERTY
    def basicBlock: BasicBlock = cfg.bb(index)

    override def hashCode(): Int = method.hashCode() * 31 + index

    override def equals(obj: Any): Boolean = obj match {
        case JavaStatement(method2, index2, isReturnNode2, _, _) =>
            method == method2 && index == index2 && isReturnNode == isReturnNode2
        case _ => false
    }

    override def toString: String = {
        val returnOptional =
            if (isReturnNode) { "(return)" }
            else { "" }
        s"${method.classFile.thisType.simpleName}:${method.name}[$index]$returnOptional{$stmt}"
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
