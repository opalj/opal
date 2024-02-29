/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package l0
package interpretation

import org.opalj.br.analyses.SomeProject
import org.opalj.fpcf.PropertyStore
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.BinaryExprInterpreter
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.DoubleValueInterpreter
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.FloatValueInterpreter
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.IntegerValueInterpreter
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.InterpretationHandler
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.StringConstInterpreter

/**
 * @inheritdoc
 *
 * @author Maximilian Rüsch
 */
class L0InterpretationHandler[State <: L0ComputationState]()(
    implicit
    p:  SomeProject,
    ps: PropertyStore
) extends InterpretationHandler[State] {

    override protected def processNewDefSite(defSite: Int)(implicit state: State): IPResult = {
        val defSitePC = pcOfDefSite(defSite)(state.tac.stmts)

        state.tac.stmts(defSite) match {
            case Assignment(_, _, expr: StringConst)   => StringConstInterpreter.interpret(expr, defSite)(state)
            case Assignment(_, _, expr: IntConst)      => IntegerValueInterpreter.interpret(expr, defSite)(state)
            case Assignment(_, _, expr: FloatConst)    => FloatValueInterpreter.interpret(expr, defSite)(state)
            case Assignment(_, _, expr: DoubleConst)   => DoubleValueInterpreter.interpret(expr, defSite)(state)
            case Assignment(_, _, expr: BinaryExpr[V]) => BinaryExprInterpreter.interpret(expr, defSite)(state)

            case Assignment(_, _, expr: ArrayLoad[V]) => L0ArrayAccessInterpreter(this).interpret(expr, defSite)(state)
            case Assignment(_, _, _: New)             => NoIPResult(state.dm, defSitePC)

            // Currently unsupported
            case Assignment(_, _, _: GetField[V])               => FinalIPResult.lb(state.dm, defSitePC)
            case Assignment(_, _, _: NonVirtualFunctionCall[V]) => FinalIPResult.lb(state.dm, defSitePC)

            case Assignment(_, _, expr: VirtualFunctionCall[V]) =>
                L0VirtualFunctionCallInterpreter(this).interpret(expr, defSite)
            case ExprStmt(_, expr: VirtualFunctionCall[V]) =>
                L0VirtualFunctionCallInterpreter(this).interpret(expr, defSite)

            case Assignment(_, _, expr: StaticFunctionCall[V]) =>
                L0StaticFunctionCallInterpreter(this).interpret(expr, defSite)
            case ExprStmt(_, expr: StaticFunctionCall[V]) =>
                L0StaticFunctionCallInterpreter(this).interpret(expr, defSite)

            case vmc: VirtualMethodCall[V]     => L0VirtualMethodCallInterpreter().interpret(vmc, defSite)(state)
            case nvmc: NonVirtualMethodCall[V] => L0NonVirtualMethodCallInterpreter(this).interpret(nvmc, defSite)

            case _ => NoIPResult(state.dm, defSitePC)
        }
    }
}

object L0InterpretationHandler {

    def apply[State <: L0ComputationState]()(
        implicit
        p:  SomeProject,
        ps: PropertyStore
    ): L0InterpretationHandler[State] = new L0InterpretationHandler[State]
}
