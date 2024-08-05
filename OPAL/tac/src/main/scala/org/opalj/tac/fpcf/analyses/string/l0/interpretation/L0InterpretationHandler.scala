/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string
package l0
package interpretation

import org.opalj.br.analyses.SomeProject
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.tac.fpcf.analyses.string.interpretation.InterpretationHandler
import org.opalj.tac.fpcf.properties.string.StringFlowFunctionProperty

/**
 * @inheritdoc
 *
 * @author Maximilian Rüsch
 */
class L0InterpretationHandler(implicit override val project: SomeProject) extends InterpretationHandler {

    override protected def processStatement(implicit
        state: InterpretationState
    ): PartialFunction[Stmt[V], ProperPropertyComputationResult] = {
        case stmt @ Assignment(_, _, expr: SimpleValueConst) =>
            SimpleValueConstExprInterpreter.interpretExpr(stmt, expr)
        case stmt @ Assignment(_, _, expr: BinaryExpr[V]) => BinaryExprInterpreter.interpretExpr(stmt, expr)

        case ExprStmt(_, expr: VirtualFunctionCall[V])    => StringInterpreter.failure(expr.receiver.asVar)
        case ExprStmt(_, expr: NonVirtualFunctionCall[V]) => StringInterpreter.failure(expr.receiver.asVar)

        // Static function calls without return value usage are irrelevant
        case ExprStmt(_, _: StaticFunctionCall[V]) =>
            StringInterpreter.computeFinalResult(StringFlowFunctionProperty.identity)

        case vmc: VirtualMethodCall[V]     => StringInterpreter.failure(vmc.receiver.asVar)
        case nvmc: NonVirtualMethodCall[V] => StringInterpreter.failure(nvmc.receiver.asVar)

        case Assignment(_, target, _) => StringInterpreter.failure(target)

        case ReturnValue(pc, expr) =>
            StringInterpreter.computeFinalResult(StringFlowFunctionProperty.identityForVariableAt(
                pc,
                expr.asVar.toPersistentForm(state.tac.stmts)
            ))

        case _ =>
            StringInterpreter.computeFinalResult(StringFlowFunctionProperty.identity)
    }
}

object L0InterpretationHandler {

    def apply(project: SomeProject): L0InterpretationHandler = new L0InterpretationHandler()(project)
}
