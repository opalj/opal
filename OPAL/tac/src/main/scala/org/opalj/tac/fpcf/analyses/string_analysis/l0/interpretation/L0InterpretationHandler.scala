/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package l0
package interpretation

import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.fpcf.ProperPropertyComputationResult
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

    override protected def processNewDefSite(defSite: Int)(implicit state: State): ProperPropertyComputationResult = {
        state.tac.stmts(defSite) match {
            case Assignment(_, _, expr: StringConst)   => StringConstInterpreter.interpret(expr, defSite)
            case Assignment(_, _, expr: IntConst)      => IntegerValueInterpreter.interpret(expr, defSite)
            case Assignment(_, _, expr: FloatConst)    => FloatValueInterpreter.interpret(expr, defSite)
            case Assignment(_, _, expr: DoubleConst)   => DoubleValueInterpreter.interpret(expr, defSite)
            case Assignment(_, _, expr: BinaryExpr[V]) => BinaryExprInterpreter.interpret(expr, defSite)

            case Assignment(_, _, expr: ArrayLoad[V]) => L0ArrayAccessInterpreter(ps).interpret(expr, defSite)
            case Assignment(_, _, expr: NewArray[V])  => new L0NewArrayInterpreter(ps).interpret(expr, defSite)
            case Assignment(_, _, _: New) =>
                StringInterpreter.computeFinalResult(defSite, StringConstancyInformation.getNeutralElement)

            // Currently unsupported
            case Assignment(_, _, _: GetField[V]) =>
                StringInterpreter.computeFinalResult(defSite, StringConstancyInformation.lb)

            case Assignment(_, _, expr: VirtualFunctionCall[V]) =>
                L0VirtualFunctionCallInterpreter(ps).interpret(expr, defSite)
            case ExprStmt(_, expr: VirtualFunctionCall[V]) =>
                L0VirtualFunctionCallInterpreter(ps).interpret(expr, defSite)

            case Assignment(_, _, expr: NonVirtualFunctionCall[V]) =>
                L0NonVirtualFunctionCallInterpreter().interpret(expr, defSite)
            case ExprStmt(_, expr: NonVirtualFunctionCall[V]) =>
                L0NonVirtualFunctionCallInterpreter().interpret(expr, defSite)

            case Assignment(_, _, expr: StaticFunctionCall[V]) =>
                L0StaticFunctionCallInterpreter().interpret(expr, defSite)
            case ExprStmt(_, expr: StaticFunctionCall[V]) =>
                L0StaticFunctionCallInterpreter().interpret(expr, defSite)

            case vmc: VirtualMethodCall[V]     => L0VirtualMethodCallInterpreter().interpret(vmc, defSite)
            case nvmc: NonVirtualMethodCall[V] => L0NonVirtualMethodCallInterpreter(ps).interpret(nvmc, defSite)

            case _ =>
                StringInterpreter.computeFinalResult(defSite, StringConstancyInformation.getNeutralElement)
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
