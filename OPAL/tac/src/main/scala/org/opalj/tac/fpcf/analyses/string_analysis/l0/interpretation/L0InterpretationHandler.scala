/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package l0
package interpretation

import org.opalj.ai.ImmediateVMExceptionsOriginOffset
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.fpcf.PropertyStore
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.BinaryExprInterpreter
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.DoubleValueInterpreter
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.FloatValueInterpreter
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.IntegerValueInterpreter
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.InterpretationHandler
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.StringConstInterpreter

/**
 * `IntraproceduralInterpretationHandler` is responsible for processing expressions that are
 * relevant in order to determine which value(s) a string read operation might have. These
 * expressions usually come from the definitions sites of the variable of interest.
 * <p>
 * This handler may use [[L0StringInterpreter]]s and general
 * [[org.opalj.tac.fpcf.analyses.string_analysis.interpretation.StringInterpreter]]s.
 *
 * @author Maximilian Rüsch
 */
class L0InterpretationHandler[State <: L0ComputationState[State]]()(
    implicit
    p:  SomeProject,
    ps: PropertyStore
) extends InterpretationHandler[State] {

    /**
     * Processed the given definition site in an intraprocedural fashion.
     * <p>
     * @inheritdoc
     */
    override def processDefSite(defSite: Int)(implicit state: State): IPResult = {
        val defSitePC = pcOfDefSite(defSite)(state.tac.stmts)

        if (defSite < 0) {
            val params = state.params.toList.map(_.toList)
            if (params.isEmpty || defSite == -1 || defSite <= ImmediateVMExceptionsOriginOffset) {
                state.appendToInterimFpe2Sci(defSitePC, StringConstancyInformation.lb)
                return FinalIPResult.lb
            } else {
                val sci = getParam(params, defSite)
                state.appendToInterimFpe2Sci(defSitePC, sci)
                return FinalIPResult(sci)
            }
        } else if (processedDefSites.contains(defSite)) {
            state.appendToInterimFpe2Sci(defSitePC, StringConstancyInformation.getNeutralElement)
            return FinalIPResult(StringConstancyInformation.getNeutralElement)
        }

        processedDefSites(defSite) = ()

        state.tac.stmts(defSite) match {
            case Assignment(_, _, expr: StringConst) =>
                StringConstInterpreter.interpret(expr, defSite)(state)
            case Assignment(_, _, expr: IntConst) =>
                IntegerValueInterpreter.interpret(expr, defSite)(state)
            case Assignment(_, _, expr: FloatConst) =>
                FloatValueInterpreter.interpret(expr, defSite)(state)
            case Assignment(_, _, expr: DoubleConst) =>
                DoubleValueInterpreter.interpret(expr, defSite)(state)
            case Assignment(_, _, expr: BinaryExpr[V]) =>
                BinaryExprInterpreter.interpret(expr, defSite)(state)
            case Assignment(_, _, expr: ArrayLoad[V]) =>
                L0ArrayAccessInterpreter(this).interpret(expr, defSite)(state)
            case Assignment(_, _, _: New) =>
                state.appendToInterimFpe2Sci(defSitePC, StringConstancyInformation.getNeutralElement)
                NoIPResult
            case Assignment(_, _, _: GetField[V]) =>
                // Currently unsupported
                FinalIPResult.lb
            case Assignment(_, _, expr: VirtualFunctionCall[V]) =>
                L0VirtualFunctionCallInterpreter(this).interpret(expr, defSite)
            case Assignment(_, _, expr: StaticFunctionCall[V]) =>
                L0StaticFunctionCallInterpreter(this).interpret(expr, defSite)
            case Assignment(_, _, _: NonVirtualFunctionCall[V]) =>
                // Currently unsupported
                FinalIPResult.lb
            case ExprStmt(_, expr: VirtualFunctionCall[V]) =>
                L0VirtualFunctionCallInterpreter(this).interpret(expr, defSite)
            case ExprStmt(_, expr: StaticFunctionCall[V]) =>
                L0StaticFunctionCallInterpreter(this).interpret(expr, defSite)
            case vmc: VirtualMethodCall[V] =>
                L0VirtualMethodCallInterpreter().interpret(vmc, defSite)(state)
            case nvmc: NonVirtualMethodCall[V] =>
                L0NonVirtualMethodCallInterpreter(this).interpret(nvmc, defSite)
            case _ =>
                state.appendToInterimFpe2Sci(defSitePC, StringConstancyInformation.getNeutralElement)
                NoIPResult
        }
    }
}

object L0InterpretationHandler {

    def apply[State <: L0ComputationState[State]]()(
        implicit
        p:  SomeProject,
        ps: PropertyStore
    ): L0InterpretationHandler[State] = new L0InterpretationHandler[State]
}
