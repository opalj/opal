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
import org.opalj.tac.fpcf.analyses.string.interpretation.BinaryExprInterpreter
import org.opalj.tac.fpcf.analyses.string.interpretation.InterpretationHandler
import org.opalj.tac.fpcf.analyses.string.interpretation.SimpleValueConstExprInterpreter
import org.opalj.tac.fpcf.properties.string.IdentityFlow

/**
 * @inheritdoc
 *
 * @author Maximilian Rüsch
 */
class L0InterpretationHandler(implicit override val project: SomeProject) extends InterpretationHandler {

    override protected def processNew(implicit
        state: InterpretationState
    ): ProperPropertyComputationResult = {
        val duSiteOpt = valueOriginOfPC(state.pc, state.tac.pcToIndex);
        if (duSiteOpt.isEmpty) {
            throw new IllegalArgumentException(s"Obtained a pc that does not represent a definition site: ${state.pc}")
        }

        state.tac.stmts(duSiteOpt.get) match {
            case stmt @ Assignment(_, _, expr: SimpleValueConst) =>
                SimpleValueConstExprInterpreter.interpretExpr(stmt, expr)
            case stmt @ Assignment(_, _, expr: BinaryExpr[V]) => BinaryExprInterpreter.interpretExpr(stmt, expr)

            // Currently unsupported
            case Assignment(_, target, _: ArrayExpr[V]) => StringInterpreter.computeFinalLBFor(target)
            case Assignment(_, target, _: FieldRead[V]) => StringInterpreter.computeFinalLBFor(target)

            case Assignment(_, _, _: New) =>
                StringInterpreter.computeFinalResult(IdentityFlow)

            case stmt @ Assignment(_, _, expr: VirtualFunctionCall[V]) =>
                L0VirtualFunctionCallInterpreter().interpretExpr(stmt, expr)
            case stmt @ ExprStmt(_, expr: VirtualFunctionCall[V]) =>
                L0VirtualFunctionCallInterpreter().interpretExpr(stmt, expr)

            case stmt @ Assignment(_, _, expr: NonVirtualFunctionCall[V]) =>
                L0NonVirtualFunctionCallInterpreter().interpretExpr(stmt, expr)
            case stmt @ ExprStmt(_, expr: NonVirtualFunctionCall[V]) =>
                L0NonVirtualFunctionCallInterpreter().interpretExpr(stmt, expr)

            case stmt @ Assignment(_, target, expr: StaticFunctionCall[V]) =>
                L0StaticFunctionCallInterpreter().interpretExpr(stmt, expr)
            // Static function calls without return value usage are irrelevant
            case ExprStmt(_, _: StaticFunctionCall[V]) => StringInterpreter.computeFinalResult(IdentityFlow)

            case vmc: VirtualMethodCall[V]     => L0VirtualMethodCallInterpreter.interpret(vmc)
            case nvmc: NonVirtualMethodCall[V] => L0NonVirtualMethodCallInterpreter.interpret(nvmc)

            case _ => StringInterpreter.computeFinalResult(IdentityFlow)
        }
    }
}

object L0InterpretationHandler {

    def apply(project: SomeProject): L0InterpretationHandler = new L0InterpretationHandler()(project)
}
