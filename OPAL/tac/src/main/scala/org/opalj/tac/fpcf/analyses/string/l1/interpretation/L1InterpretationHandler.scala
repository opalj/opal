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
import org.opalj.tac.fpcf.analyses.string.interpretation.InterpretationHandler
import org.opalj.tac.fpcf.analyses.string.interpretation.InterpretationState
import org.opalj.tac.fpcf.analyses.string.l0.interpretation.BinaryExprInterpreter
import org.opalj.tac.fpcf.analyses.string.l0.interpretation.SimpleValueConstExprInterpreter
import org.opalj.tac.fpcf.properties.string.StringFlowFunctionProperty

/**
 * @inheritdoc
 *
 * Interprets statements similar to the [[org.opalj.tac.fpcf.analyses.string.l0.interpretation.L0InterpretationHandler]]
 * but handles all sorts of function calls on top.
 *
 * @author Maximilian Rüsch
 */
class L1InterpretationHandler(implicit override val project: SomeProject) extends InterpretationHandler {

    override protected def processStatement(implicit
        state: InterpretationState
    ): Stmt[V] => ProperPropertyComputationResult = {
        case stmt @ Assignment(_, _, expr: SimpleValueConst) =>
            SimpleValueConstExprInterpreter.interpretExpr(stmt, expr)
        case stmt @ Assignment(_, _, expr: BinaryExpr[V]) => BinaryExprInterpreter().interpretExpr(stmt, expr)

        // Currently unsupported
        case Assignment(_, target, _: ArrayExpr[V]) => StringInterpreter.failure(target)
        case Assignment(_, target, _: FieldRead[V]) => StringInterpreter.failure(target)

        case Assignment(_, _, _: New) =>
            StringInterpreter.computeFinalResult(StringFlowFunctionProperty.identity)

        case stmt @ Assignment(_, _, expr: VirtualFunctionCall[V]) =>
            new L1VirtualFunctionCallInterpreter().interpretExpr(stmt, expr)
        case stmt @ ExprStmt(_, expr: VirtualFunctionCall[V]) =>
            new L1VirtualFunctionCallInterpreter().interpretExpr(stmt, expr)

        case stmt @ Assignment(_, _, expr: NonVirtualFunctionCall[V]) =>
            L1NonVirtualFunctionCallInterpreter().interpretExpr(stmt, expr)
        case stmt @ ExprStmt(_, expr: NonVirtualFunctionCall[V]) =>
            L1NonVirtualFunctionCallInterpreter().interpretExpr(stmt, expr)

        case stmt @ Assignment(_, _, expr: StaticFunctionCall[V]) =>
            L1StaticFunctionCallInterpreter().interpretExpr(stmt, expr)
        // Static function calls without return value usage are irrelevant
        case ExprStmt(_, _: StaticFunctionCall[V]) =>
            StringInterpreter.computeFinalResult(StringFlowFunctionProperty.identity)

        case vmc: VirtualMethodCall[V]     => L1VirtualMethodCallInterpreter().interpret(vmc)
        case nvmc: NonVirtualMethodCall[V] => L1NonVirtualMethodCallInterpreter().interpret(nvmc)

        case Assignment(_, target, _) =>
            StringInterpreter.failure(target)

        case ReturnValue(pc, expr) =>
            StringInterpreter.computeFinalResult(StringFlowFunctionProperty.identityForVariableAt(
                pc,
                expr.asVar.toPersistentForm(state.tac.stmts)
            ))

        case _ =>
            StringInterpreter.computeFinalResult(StringFlowFunctionProperty.identity)
    }
}

object L1InterpretationHandler {

    def apply(project: SomeProject): L1InterpretationHandler = new L1InterpretationHandler()(project)
}
