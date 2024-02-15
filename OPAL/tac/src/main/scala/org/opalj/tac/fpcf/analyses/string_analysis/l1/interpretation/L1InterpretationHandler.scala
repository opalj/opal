/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package l1
package interpretation

import org.opalj.br.analyses.DeclaredFields
import org.opalj.br.analyses.FieldAccessInformation
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.analyses.ContextProvider
import org.opalj.fpcf.PropertyStore
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.BinaryExprInterpreter
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.DoubleValueInterpreter
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.FloatValueInterpreter
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.IntegerValueInterpreter
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.InterpretationHandler
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.StringConstInterpreter
import org.opalj.tac.fpcf.analyses.string_analysis.l0.interpretation.L0ArrayAccessInterpreter
import org.opalj.tac.fpcf.analyses.string_analysis.l0.interpretation.L0NonVirtualMethodCallInterpreter
import org.opalj.tac.fpcf.analyses.string_analysis.l0.interpretation.L0StaticFunctionCallInterpreter
import org.opalj.tac.fpcf.analyses.string_analysis.l0.interpretation.L0VirtualMethodCallInterpreter

/**
 * @inheritdoc
 *
 * @author Maximilian Rüsch
 */
class L1InterpretationHandler[State <: L1ComputationState[State]](
    declaredFields:               DeclaredFields,
    fieldAccessInformation:       FieldAccessInformation,
    implicit val p:               SomeProject,
    implicit val ps:              PropertyStore,
    implicit val contextProvider: ContextProvider
) extends InterpretationHandler[State] {

    override protected def processNewDefSite(defSite: Int)(implicit state: State): IPResult = {
        val defSitePC = pcOfDefSite(defSite)(state.tac.stmts)

        state.tac.stmts(defSite) match {
            case Assignment(_, _, expr: StringConst) => StringConstInterpreter.interpret(expr, defSite)(state)
            case Assignment(_, _, expr: IntConst)    => IntegerValueInterpreter.interpret(expr, defSite)(state)
            case Assignment(_, _, expr: FloatConst)  => FloatValueInterpreter.interpret(expr, defSite)(state)
            case Assignment(_, _, expr: DoubleConst) => DoubleValueInterpreter.interpret(expr, defSite)(state) // TODO what about long consts

            case Assignment(_, _, expr: ArrayLoad[V]) =>
                new L0ArrayAccessInterpreter(this).interpret(expr, defSite)
            case Assignment(_, _, expr: NewArray[V]) =>
                new L1NewArrayInterpreter(this).interpret(expr, defSite)

            case Assignment(_, _, _: New) => NoIPResult(state.dm, defSitePC)

            case Assignment(_, _, expr: GetStatic) =>
                L1FieldReadInterpreter(ps, fieldAccessInformation, p, declaredFields, contextProvider).interpret(
                    expr,
                    defSite
                )(state)
            case Assignment(_, _, expr: GetField[V]) =>
                L1FieldReadInterpreter(ps, fieldAccessInformation, p, declaredFields, contextProvider).interpret(
                    expr,
                    defSite
                )(state)
            case ExprStmt(_, expr: GetStatic) =>
                L1FieldReadInterpreter(ps, fieldAccessInformation, p, declaredFields, contextProvider).interpret(
                    expr,
                    defSite
                )(state)
            case ExprStmt(_, expr: GetField[V]) =>
                L1FieldReadInterpreter(ps, fieldAccessInformation, p, declaredFields, contextProvider).interpret(
                    expr,
                    defSite
                )(state)

            case Assignment(_, _, expr: VirtualFunctionCall[V]) =>
                new L1VirtualFunctionCallInterpreter(this, ps, contextProvider).interpret(expr, defSite)
            case ExprStmt(_, expr: VirtualFunctionCall[V]) =>
                new L1VirtualFunctionCallInterpreter(this, ps, contextProvider).interpret(expr, defSite)

            case Assignment(_, _, expr: NonVirtualFunctionCall[V]) =>
                L1NonVirtualFunctionCallInterpreter().interpret(expr, defSite)(state)
            case ExprStmt(_, expr: NonVirtualFunctionCall[V]) =>
                L1NonVirtualFunctionCallInterpreter().interpret(expr, defSite)(state)

            case Assignment(_, _, expr: StaticFunctionCall[V]) =>
                L0StaticFunctionCallInterpreter(this).interpret(expr, defSite)
            case ExprStmt(_, expr: StaticFunctionCall[V]) =>
                L0StaticFunctionCallInterpreter(this).interpret(expr, defSite)

            // TODO: For binary expressions, use the underlying domain to retrieve the result of such expressions
            case Assignment(_, _, expr: BinaryExpr[V]) => BinaryExprInterpreter.interpret(expr, defSite)(state)

            case vmc: VirtualMethodCall[V] =>
                L0VirtualMethodCallInterpreter().interpret(vmc, defSite)(state)
            case nvmc: NonVirtualMethodCall[V] =>
                L0NonVirtualMethodCallInterpreter(this).interpret(nvmc, defSite)

            case _ => NoIPResult(state.dm, defSitePC)
        }
    }
}

object L1InterpretationHandler {

    def apply[State <: L1ComputationState[State]](
        declaredFields:         DeclaredFields,
        fieldAccessInformation: FieldAccessInformation,
        project:                SomeProject,
        ps:                     PropertyStore,
        contextProvider:        ContextProvider
    ): L1InterpretationHandler[State] = new L1InterpretationHandler[State](
        declaredFields,
        fieldAccessInformation,
        project,
        ps,
        contextProvider
    )
}
