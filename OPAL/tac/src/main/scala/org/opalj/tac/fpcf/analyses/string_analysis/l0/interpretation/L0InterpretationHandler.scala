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
import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.PropertyStore
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.BinaryExprInterpreter
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.DoubleValueInterpreter
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.FloatValueInterpreter
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.IntegerValueInterpreter
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.InterpretationHandler
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.NewInterpreter
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
class L0InterpretationHandler(
        tac: TAC
)(
        implicit
        p:  SomeProject,
        ps: PropertyStore
) extends InterpretationHandler[L0ComputationState](tac) {

    /**
     * Processed the given definition site in an intraprocedural fashion.
     * <p>
     * @inheritdoc
     */
    override def processDefSite(defSite: Int)(implicit
        state: L0ComputationState
    ): EOptionP[Entity, StringConstancyProperty] = {
        // Without doing the following conversion, the following compile error will occur: "the
        // result type of an implicit conversion must be more specific than org.opalj.fpcf.Entity"
        val e: Integer = defSite
        val defSitePC = pcOfDefSite(defSite)(state.tac.stmts)

        if (defSite < 0) {
            val params = state.params.toList.map(_.toList)
            if (params.isEmpty || defSite == -1 || defSite <= ImmediateVMExceptionsOriginOffset) {
                state.appendToInterimFpe2Sci(defSitePC, StringConstancyInformation.lb)
                return FinalEP(e, StringConstancyProperty.lb)
            } else {
                val sci = getParam(params, defSite)
                state.appendToInterimFpe2Sci(defSitePC, sci)
                return FinalEP(e, StringConstancyProperty(sci))
            }
        } else if (processedDefSites.contains(defSite)) {
            state.appendToInterimFpe2Sci(defSitePC, StringConstancyInformation.getNeutralElement)
            return FinalEP(e, StringConstancyProperty.getNeutralElement)
        }

        processedDefSites(defSite) = ()

        stmts(defSite) match {
            case Assignment(_, _, expr: StringConst) =>
                StringConstInterpreter(cfg, this).interpret(expr)
            case Assignment(_, _, expr: IntConst) =>
                IntegerValueInterpreter(cfg, this).interpret(expr)
            case Assignment(_, _, expr: FloatConst) =>
                FloatValueInterpreter(cfg, this).interpret(expr)
            case Assignment(_, _, expr: DoubleConst) =>
                DoubleValueInterpreter(cfg, this).interpret(expr)
            case Assignment(_, _, expr: BinaryExpr[V]) =>
                BinaryExprInterpreter(cfg, this).interpret(expr)
            case Assignment(_, _, expr: ArrayLoad[V]) =>
                L0ArrayInterpreter(cfg, this).interpret(expr, defSite)
            case Assignment(_, _, expr: New) =>
                NewInterpreter(cfg, this).interpret(expr)
            case Assignment(_, _, expr: GetField[V]) =>
                L0GetFieldInterpreter(cfg, this).interpret(expr, defSite)
            case Assignment(_, _, expr: VirtualFunctionCall[V]) =>
                L0VirtualFunctionCallInterpreter(cfg, this).interpret(expr, defSite)
            case Assignment(_, _, expr: StaticFunctionCall[V]) =>
                L0StaticFunctionCallInterpreter(cfg, this).interpret(expr, defSite)
            case Assignment(_, _, expr: NonVirtualFunctionCall[V]) =>
                // Currently unsupported
                FinalEP(expr, StringConstancyProperty.lb)
            case ExprStmt(_, expr: VirtualFunctionCall[V]) =>
                L0VirtualFunctionCallInterpreter(cfg, this).interpret(expr, defSite)
            case ExprStmt(_, expr: StaticFunctionCall[V]) =>
                L0StaticFunctionCallInterpreter(cfg, this).interpret(expr, defSite)
            case vmc: VirtualMethodCall[V] =>
                L0VirtualMethodCallInterpreter(cfg, this).interpret(vmc, defSite)
            case nvmc: NonVirtualMethodCall[V] =>
                L0NonVirtualMethodCallInterpreter(cfg, this).interpret(nvmc, defSite)
            case _ =>
                state.appendToInterimFpe2Sci(defSitePC, StringConstancyInformation.getNeutralElement)
                FinalEP(e, StringConstancyProperty.getNeutralElement)
        }
    }

    override def finalizeDefSite(defSite: Int, state: L0ComputationState): Unit = {}
}

object L0InterpretationHandler {

    def apply(tac: TAC)(
        implicit
        p:  SomeProject,
        ps: PropertyStore
    ): L0InterpretationHandler = new L0InterpretationHandler(tac)
}
