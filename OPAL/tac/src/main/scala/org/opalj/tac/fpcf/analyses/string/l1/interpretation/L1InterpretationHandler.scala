/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string
package l1
package interpretation

import org.opalj.br.analyses.SomeProject
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.tac.fpcf.analyses.string.interpretation.InterpretationState
import org.opalj.tac.fpcf.analyses.string.l0.interpretation.L0InterpretationHandler
import org.opalj.tac.fpcf.properties.string.StringFlowFunctionProperty

/**
 * @inheritdoc
 *
 * Interprets statements similar to the [[org.opalj.tac.fpcf.analyses.string.l0.interpretation.L0InterpretationHandler]]
 * but handles all sorts of function calls on top.
 *
 * @author Maximilian Rüsch
 */
class L1InterpretationHandler(implicit override val project: SomeProject) extends L0InterpretationHandler {

    override protected def processStatement(implicit
        state: InterpretationState
    ): Stmt[V] => ProperPropertyComputationResult = {
        // Currently unsupported
        case Assignment(_, target, _: ArrayExpr[V]) => StringInterpreter.failure(target)
        case Assignment(_, target, _: FieldRead[V]) => StringInterpreter.failure(target)

        case Assignment(_, _, _: New) =>
            StringInterpreter.computeFinalResult(StringFlowFunctionProperty.identity)

        // We only treat calls that either produce a (potential string) result (function calls) or could be invocations
        // of special methods, e.g., on StringBuilder, that we care about.
        case stmt @ AssignmentLikeStmt(_, expr: VirtualFunctionCall[V]) =>
            new L1VirtualFunctionCallInterpreter().interpretExpr(stmt.asAssignmentLike, expr)

        case stmt @ AssignmentLikeStmt(_, expr: NonVirtualFunctionCall[V]) =>
            L1NonVirtualFunctionCallInterpreter().interpretExpr(stmt.asAssignmentLike, expr)

        case stmt @ Assignment(_, _, expr: StaticFunctionCall[V]) =>
            L1StaticFunctionCallInterpreter().interpretExpr(stmt, expr)

        case vmc: VirtualMethodCall[V]     => new L1VirtualMethodCallInterpreter().interpret(vmc)
        case nvmc: NonVirtualMethodCall[V] => L1NonVirtualMethodCallInterpreter().interpret(nvmc)

        case stmt => super.processStatement(state)(stmt)
    }
}

object L1InterpretationHandler {

    def apply(project: SomeProject): L1InterpretationHandler = new L1InterpretationHandler()(project)
}
