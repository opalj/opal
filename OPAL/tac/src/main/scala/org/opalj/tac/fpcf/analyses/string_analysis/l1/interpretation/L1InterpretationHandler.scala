/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package l1
package interpretation

import org.opalj.br.analyses.DeclaredFields
import org.opalj.br.analyses.DeclaredFieldsKey
import org.opalj.br.analyses.FieldAccessInformation
import org.opalj.br.analyses.FieldAccessInformationKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.ContextProviderKey
import org.opalj.br.fpcf.analyses.ContextProvider
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyStore
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.BinaryExprInterpreter
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.InterpretationHandler
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.SimpleValueConstExprInterpreter
import org.opalj.tac.fpcf.analyses.string_analysis.l0.interpretation.L0ArrayAccessInterpreter
import org.opalj.tac.fpcf.analyses.string_analysis.l0.interpretation.L0NewArrayInterpreter
import org.opalj.tac.fpcf.analyses.string_analysis.l0.interpretation.L0NonVirtualFunctionCallInterpreter
import org.opalj.tac.fpcf.analyses.string_analysis.l0.interpretation.L0NonVirtualMethodCallInterpreter
import org.opalj.tac.fpcf.analyses.string_analysis.l0.interpretation.L0StaticFunctionCallInterpreter
import org.opalj.tac.fpcf.analyses.string_analysis.l0.interpretation.L0VirtualMethodCallInterpreter

/**
 * @inheritdoc
 *
 * @author Maximilian Rüsch
 */
class L1InterpretationHandler[State <: L1ComputationState](
    implicit val p:  SomeProject,
    implicit val ps: PropertyStore
) extends InterpretationHandler[State] {

    val declaredFields: DeclaredFields = p.get(DeclaredFieldsKey)
    val fieldAccessInformation: FieldAccessInformation = p.get(FieldAccessInformationKey)
    implicit val contextProvider: ContextProvider = p.get(ContextProviderKey)

    override protected def processNewDefSite(defSite: Int)(implicit state: State): ProperPropertyComputationResult = {
        state.tac.stmts(defSite) match {
            case Assignment(_, _, expr: SimpleValueConst) => SimpleValueConstExprInterpreter.interpret(expr, defSite)

            case Assignment(_, _, expr: ArrayLoad[V]) => L0ArrayAccessInterpreter(ps).interpret(expr, defSite)
            case Assignment(_, _, expr: NewArray[V])  => new L0NewArrayInterpreter(ps).interpret(expr, defSite)
            case Assignment(_, _, _: New) =>
                StringInterpreter.computeFinalResult(defSite, StringConstancyInformation.getNeutralElement)

            case Assignment(_, _, expr: FieldRead[V]) =>
                L1FieldReadInterpreter(ps, fieldAccessInformation, p, declaredFields, contextProvider).interpret(
                    expr,
                    defSite
                )
            case ExprStmt(_, expr: FieldRead[V]) =>
                L1FieldReadInterpreter(ps, fieldAccessInformation, p, declaredFields, contextProvider).interpret(
                    expr,
                    defSite
                )

            case Assignment(_, _, expr: VirtualFunctionCall[V]) =>
                new L1VirtualFunctionCallInterpreter().interpret(expr, defSite)
            case ExprStmt(_, expr: VirtualFunctionCall[V]) =>
                new L1VirtualFunctionCallInterpreter().interpret(expr, defSite)

            case Assignment(_, _, expr: NonVirtualFunctionCall[V]) =>
                L0NonVirtualFunctionCallInterpreter().interpret(expr, defSite)
            case ExprStmt(_, expr: NonVirtualFunctionCall[V]) =>
                L0NonVirtualFunctionCallInterpreter().interpret(expr, defSite)

            case Assignment(_, _, expr: StaticFunctionCall[V]) =>
                L0StaticFunctionCallInterpreter().interpret(expr, defSite)
            case ExprStmt(_, expr: StaticFunctionCall[V]) =>
                L0StaticFunctionCallInterpreter().interpret(expr, defSite)

            // TODO: For binary expressions, use the underlying domain to retrieve the result of such expressions
            case Assignment(_, _, expr: BinaryExpr[V]) => BinaryExprInterpreter.interpret(expr, defSite)

            case vmc: VirtualMethodCall[V] =>
                L0VirtualMethodCallInterpreter().interpret(vmc, defSite)
            case nvmc: NonVirtualMethodCall[V] =>
                L0NonVirtualMethodCallInterpreter(ps).interpret(nvmc, defSite)

            case _ =>
                StringInterpreter.computeFinalResult(defSite, StringConstancyInformation.getNeutralElement)
        }
    }
}

object L1InterpretationHandler {

    def requiredProjectInformation: ProjectInformationKeys =
        Seq(DeclaredFieldsKey, FieldAccessInformationKey, ContextProviderKey)

    def apply[State <: L1ComputationState](project: SomeProject, ps: PropertyStore): L1InterpretationHandler[State] =
        new L1InterpretationHandler[State]()(project, ps)
}
