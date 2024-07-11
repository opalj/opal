/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string
package l1
package interpretation

import org.opalj.br.analyses.DeclaredFields
import org.opalj.br.analyses.DeclaredFieldsKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.ContextProviderKey
import org.opalj.br.fpcf.analyses.ContextProvider
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.tac.fpcf.analyses.string.interpretation.BinaryExprInterpreter
import org.opalj.tac.fpcf.analyses.string.interpretation.InterpretationHandler
import org.opalj.tac.fpcf.analyses.string.interpretation.SimpleValueConstExprInterpreter
import org.opalj.tac.fpcf.analyses.string.l0.interpretation.L0NonVirtualFunctionCallInterpreter
import org.opalj.tac.fpcf.analyses.string.l0.interpretation.L0NonVirtualMethodCallInterpreter
import org.opalj.tac.fpcf.analyses.string.l0.interpretation.L0StaticFunctionCallInterpreter
import org.opalj.tac.fpcf.analyses.string.l0.interpretation.L0VirtualMethodCallInterpreter
import org.opalj.tac.fpcf.properties.string.StringFlowFunctionProperty

/**
 * @inheritdoc
 *
 * @author Maximilian Rüsch
 */
class L1InterpretationHandler(implicit override val project: SomeProject) extends InterpretationHandler {

    implicit val declaredFields: DeclaredFields = p.get(DeclaredFieldsKey)
    implicit val contextProvider: ContextProvider = p.get(ContextProviderKey)

    override protected def processStatement(implicit
        state: InterpretationState
    ): PartialFunction[Stmt[V], ProperPropertyComputationResult] = {
        case stmt @ Assignment(_, _, expr: SimpleValueConst) =>
            SimpleValueConstExprInterpreter.interpretExpr(stmt, expr)

        // Currently unsupported
        case Assignment(_, target, _: ArrayExpr[V]) => StringInterpreter.failure(target)

        case stmt @ Assignment(_, _, expr: FieldRead[V]) =>
            new L1FieldReadInterpreter().interpretExpr(stmt, expr)
        // Field reads without result usage are irrelevant
        case ExprStmt(_, _: FieldRead[V]) =>
            StringInterpreter.computeFinalResult(StringFlowFunctionProperty.identity)

        case stmt: FieldWriteAccessStmt[V] =>
            StringInterpreter.computeFinalResult(StringFlowFunctionProperty.identityForVariableAt(
                stmt.pc,
                stmt.value.asVar.toPersistentForm(state.tac.stmts)
            ))

        case stmt @ Assignment(_, _, expr: VirtualFunctionCall[V]) =>
            new L1VirtualFunctionCallInterpreter().interpretExpr(stmt, expr)
        case stmt @ ExprStmt(_, expr: VirtualFunctionCall[V]) =>
            new L1VirtualFunctionCallInterpreter().interpretExpr(stmt, expr)

        case stmt @ Assignment(_, _, expr: NonVirtualFunctionCall[V]) =>
            L0NonVirtualFunctionCallInterpreter().interpretExpr(stmt, expr)
        case stmt @ ExprStmt(_, expr: NonVirtualFunctionCall[V]) =>
            L0NonVirtualFunctionCallInterpreter().interpretExpr(stmt, expr)

        case stmt @ Assignment(_, _, expr: StaticFunctionCall[V]) =>
            L0StaticFunctionCallInterpreter().interpretExpr(stmt, expr)
        // Static function calls without return value usage are irrelevant
        case ExprStmt(_, _: StaticFunctionCall[V]) =>
            StringInterpreter.computeFinalResult(StringFlowFunctionProperty.identity)

        // TODO: For binary expressions, use the underlying domain to retrieve the result of such expressions
        case stmt @ Assignment(_, _, expr: BinaryExpr[V]) => BinaryExprInterpreter.interpretExpr(stmt, expr)

        case vmc: VirtualMethodCall[V] =>
            L0VirtualMethodCallInterpreter.interpret(vmc)
        case nvmc: NonVirtualMethodCall[V] =>
            L0NonVirtualMethodCallInterpreter.interpret(nvmc)

        case Assignment(_, _, _: New) =>
            StringInterpreter.computeFinalResult(StringFlowFunctionProperty.identity)

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

    def requiredProjectInformation: ProjectInformationKeys = Seq(DeclaredFieldsKey, ContextProviderKey)

    def apply(project: SomeProject): L1InterpretationHandler = new L1InterpretationHandler()(project)
}
