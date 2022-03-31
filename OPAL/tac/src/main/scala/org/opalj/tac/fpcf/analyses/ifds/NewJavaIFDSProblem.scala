/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.ifds

import org.opalj.br.Method
import org.opalj.br.analyses.SomeProject
import org.opalj.br.cfg.{CFG, CFGNode}
import org.opalj.ifds.{AbstractIFDSFact, IFDSProblem, Statement}
import org.opalj.tac.fpcf.analyses.ifds.AbstractIFDSAnalysis.V
import org.opalj.tac.{Assignment, Call, ExprStmt, Stmt, TACStmts}

/**
 * A statement that is passed to the concrete analysis.
 *
 * @param method The method containing the statement.
 * @param node The basic block containing the statement.
 * @param stmt The TAC statement.
 * @param index The index of the Statement in the code.
 * @param code The method's TAC code.
 * @param cfg The method's CFG.
 */
case class NewJavaStatement(
        method: Method,
        index:  Int,
        code:   Array[Stmt[V]],
        cfg:    CFG[Stmt[V], TACStmts[V]]
) extends Statement[Method, CFGNode] {

    override def hashCode(): Int = method.hashCode() * 31 + index

    override def equals(o: Any): Boolean = o match {
        case s: NewJavaStatement ⇒ s.index == index && s.method == method
        case _                   ⇒ false
    }

    override def toString: String = s"${method.signatureToJava(false)}[${index}]\n\t${stmt}\n\t${method.toJava}"
    override def callable(): Method = method
    override def node(): CFGNode = cfg.bb(index)
    def stmt: Stmt[V] = code(index)
}

object NewJavaStatement {
    def apply(oldStatement: NewJavaStatement, newIndex: Int): NewJavaStatement =
        NewJavaStatement(oldStatement.method, newIndex, oldStatement.code, oldStatement.cfg)
}

abstract class NewJavaIFDSProblem[Fact <: AbstractIFDSFact](project: SomeProject)
    extends IFDSProblem[Fact, Method, NewJavaStatement](new NewForwardICFG[Fact]()(project)) {
    /**
     * Gets the call object for a statement that contains a call.
     *
     * @param call The call statement.
     * @return The call object for `call`.
     */
    protected def asCall(call: Stmt[V]): Call[V] = call.astID match {
        case Assignment.ASTID ⇒ call.asAssignment.expr.asFunctionCall
        case ExprStmt.ASTID   ⇒ call.asExprStmt.expr.asFunctionCall
        case _                ⇒ call.asMethodCall
    }

    override def outsideAnalysisContext(callee: Method): Option[(NewJavaStatement, NewJavaStatement, Fact) ⇒ Set[Fact]] = callee.body.isDefined match {
        case true  ⇒ None
        case false ⇒ Some((_call: NewJavaStatement, _successor: NewJavaStatement, in: Fact) ⇒ Set(in))
    }
}

