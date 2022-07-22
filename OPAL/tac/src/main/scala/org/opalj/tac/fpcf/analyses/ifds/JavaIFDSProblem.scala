/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.ifds

import org.opalj.br.Method
import org.opalj.br.analyses.SomeProject
import org.opalj.br.cfg.{CFG, CFGNode}
import org.opalj.ifds.Dependees.Getter
import org.opalj.ifds.{AbstractIFDSFact, IFDSProblem, Statement}
import org.opalj.tac.{Assignment, Call, DUVar, ExprStmt, Return, ReturnValue, Stmt, TACStmts}
import org.opalj.tac.fpcf.analyses.ifds.JavaIFDSProblem.V
import org.opalj.value.ValueInformation

/**
 * A statement that is passed to the concrete analysis.
 *
 * @param method The method containing the statement.
 * @param index The index of the Statement in the code.
 * @param code The method's TAC code.
 * @param cfg The method's CFG.
 */
case class JavaStatement(
        method: Method,
        index:  Int,
        code:   Array[Stmt[V]],
        cfg:    CFG[Stmt[V], TACStmts[V]]
) extends Statement[Method, CFGNode] {

    override def hashCode(): Int = method.hashCode() * 31 + index

    override def equals(o: Any): Boolean = o match {
        case s: JavaStatement => s.index == index && s.method == method
        case _                => false
    }

    override def toString: String = s"${method.signatureToJava(false)}[${index}]\n\t${stmt}\n\t${method.toJava}"
    override def callable: Method = method
    override def node: CFGNode = cfg.bb(index)
    def stmt: Stmt[V] = code(index)
}

object JavaStatement {
    def apply(referenceStatement: JavaStatement, newIndex: Int): JavaStatement =
        JavaStatement(referenceStatement.method, newIndex, referenceStatement.code, referenceStatement.cfg)
}

abstract class JavaIFDSProblem[Fact <: AbstractIFDSFact](project: SomeProject)
    extends IFDSProblem[Fact, Method, JavaStatement](new ForwardICFG()(project)) {

    override def needsPredecessor(statement: JavaStatement): Boolean = false

    /**
     * Checks if the return flow is actually possible from the given exit statement to the given successor.
     * This is used to filter flows of exceptions into normal code without being caught
     *
     * @param exit the exit statement of the returning method
     * @param successor the successor statement of the call within the callee function
     * @return whether successor might actually be the next statement after the exit statement
     */
    protected def isPossibleReturnFlow(exit: JavaStatement, successor: JavaStatement): Boolean = {
        (successor.node.isBasicBlock || successor.node.isNormalReturnExitNode) &&
            (exit.stmt.astID == Return.ASTID || exit.stmt.astID == ReturnValue.ASTID) ||
            (successor.node.isCatchNode || successor.node.isAbnormalReturnExitNode) &&
            (exit.stmt.astID != Return.ASTID && exit.stmt.astID != ReturnValue.ASTID)
    }

    override def outsideAnalysisContext(callee: Method): Option[(JavaStatement, JavaStatement, Fact, Getter) => Set[Fact]] = callee.body.isDefined match {
        case true  => None
        case false => Some((_: JavaStatement, _: JavaStatement, in: Fact, _: Getter) => Set(in))
    }
}

object JavaIFDSProblem {
    /**
     * The type of the TAC domain.
     */
    type V = DUVar[ValueInformation]

    /**
     * Converts the index of a method's formal parameter to its tac index in the method's scope and
     * vice versa.
     *
     * @param index The index of a formal parameter in the parameter list or of a variable.
     * @param isStaticMethod States, whether the method is static.
     * @return A tac index if a parameter index was passed or a parameter index if a tac index was
     *         passed.
     */
    def switchParamAndVariableIndex(index: Int, isStaticMethod: Boolean): Int =
        (if (isStaticMethod) -2 else -1) - index

    /**
     * Gets the call object for a statement that contains a call.
     *
     * @param call The call statement.
     * @return The call object for `call`.
     */
    def asCall(call: Stmt[V]): Call[V] = call.astID match {
        case Assignment.ASTID => call.asAssignment.expr.asFunctionCall
        case ExprStmt.ASTID   => call.asExprStmt.expr.asFunctionCall
        case _                => call.asMethodCall
    }

    /**
     * Checks whether the callee's formal parameter is of a reference type.
     */
    def isRefTypeParam(callee: Method, index: Int): Boolean =
        if (index == -1) true
        else {
            val parameterOffset = if (callee.isStatic) 0 else 1
            callee.descriptor.parameterType(
                switchParamAndVariableIndex(index, callee.isStatic)
                    - parameterOffset
            ).isReferenceType
        }

}
