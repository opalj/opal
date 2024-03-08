/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package l1
package interpretation

import org.opalj.br.fpcf.analyses.ContextProvider
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyStore
import org.opalj.tac.fpcf.analyses.string_analysis.l0.interpretation.L0FunctionCallInterpreter
import org.opalj.tac.fpcf.analyses.string_analysis.l0.interpretation.L0VirtualFunctionCallInterpreter
import org.opalj.tac.fpcf.properties.TACAI

/**
 * Processes [[VirtualFunctionCall]]s similar to the [[L0VirtualFunctionCallInterpreter]] but handles arbitrary calls
 * with a call graph.
 *
 * @author Maximilian Rüsch
 */
class L1VirtualFunctionCallInterpreter[State <: L1ComputationState](
    override implicit val ps:     PropertyStore,
    implicit val contextProvider: ContextProvider
) extends L0VirtualFunctionCallInterpreter[State](ps)
    with L1StringInterpreter[State]
    with L0FunctionCallInterpreter[State] {

    override type T = VirtualFunctionCall[V]

    override protected def interpretArbitraryCall(instr: T, defSite: Int)(
        implicit state: State
    ): ProperPropertyComputationResult = {
        val defSitePC = pcOfDefSite(defSite)(state.tac.stmts)

        // IMPROVE add some uncertainty element if methods with unknown body exist
        val (methods, _) = getMethodsForPC(instr.pc)
        if (methods.isEmpty) {
            return computeFinalResult(defSite, StringConstancyInformation.lb)
        }

        val params = evaluateParameters(getParametersForPC(defSitePC))
        val tacDependees = methods.map(m => (m, ps(m, TACAI.key))).toMap

        val callState = FunctionCallState(defSitePC, tacDependees.keys.toSeq, tacDependees)
        callState.setParamDependees(params)

        interpretArbitraryCallToMethods(state, callState)
    }
}
