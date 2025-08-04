/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package ifds

import java.util.concurrent.Callable

import org.opalj.br.Method
import org.opalj.br.analyses.SomeProject
import org.opalj.ifds.AbstractIFDSFact
import org.opalj.ifds.Dependees.Getter
import org.opalj.ifds.IFDSProblem
import org.opalj.tac.Assignment
import org.opalj.tac.Call
import org.opalj.tac.DUVar
import org.opalj.tac.ExprStmt
import org.opalj.tac.Stmt
import org.opalj.tac.fpcf.analyses.ide.solver.JavaBackwardICFG
import org.opalj.tac.fpcf.analyses.ide.solver.JavaForwardICFG
import org.opalj.tac.fpcf.analyses.ide.solver.JavaICFG
import org.opalj.tac.fpcf.analyses.ide.solver.JavaStatement
import org.opalj.value.ValueInformation

abstract class JavaForwardIFDSProblem[Fact <: AbstractIFDSFact](project: SomeProject)
    extends JavaIFDSProblem[Fact](new JavaForwardICFG(project))

abstract class JavaBackwardIFDSProblem[Fact <: AbstractIFDSFact](project: SomeProject)
    extends JavaIFDSProblem[Fact](new JavaBackwardICFG(project))

abstract class JavaIFDSProblem[Fact <: AbstractIFDSFact](icfg: JavaICFG)
    extends IFDSProblem[Fact, Method, JavaStatement, JavaICFG](icfg) {

    override def needsPredecessor(statement: JavaStatement): Boolean = false

    override def outsideAnalysisContextCall(callee: Method): Option[OutsideAnalysisContextCallHandler] =
        if (callee.body.isDefined) None
        else Some((_: JavaStatement, _: Option[JavaStatement], in: Fact, unbCallChain: Seq[Method], _: Getter) =>
            Set(in)
        )

    override def outsideAnalysisContextUnbReturn(callee: Method): Option[OutsideAnalysisContextUnbReturnHandler] = None
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
    def remapParamAndVariableIndex(index: Int, isStaticMethod: Boolean): Int =
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
                remapParamAndVariableIndex(index, callee.isStatic)
                    - parameterOffset
            ).isReferenceType
        }
}
