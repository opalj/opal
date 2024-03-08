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
import org.opalj.tac.fpcf.properties.TACAI

/**
 * Processes [[NonVirtualFunctionCall]]s without a call graph.
 *
 * @author Maximilian Rüsch
 */
case class L0NonVirtualFunctionCallInterpreter[State <: L0ComputationState]()(
    implicit val p:  SomeProject,
    implicit val ps: PropertyStore
) extends L0StringInterpreter[State]
    with L0FunctionCallInterpreter[State] {

    override type T = NonVirtualFunctionCall[V]

    override def interpret(instr: T, defSite: Int)(implicit state: State): ProperPropertyComputationResult = {
        val calleeMethod = instr.resolveCallTarget(state.entity._2.classFile.thisType)
        if (calleeMethod.isEmpty) {
            return computeFinalResult(defSite, StringConstancyInformation.lb)
        }

        val m = calleeMethod.value
        val callState = FunctionCallState(defSite, Seq(m), Map((m, ps(m, TACAI.key))))

        val params = evaluateParameters(getParametersForPC(pcOfDefSite(defSite)(state.tac.stmts)))
        callState.setParamDependees(params)

        interpretArbitraryCallToMethods(state, callState)
    }
}
