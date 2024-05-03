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
case class L0NonVirtualFunctionCallInterpreter()(
    implicit val p:  SomeProject,
    implicit val ps: PropertyStore
) extends StringInterpreter
    with L0FunctionCallInterpreter {

    override type T = NonVirtualFunctionCall[V]

    override def interpret(instr: T, pc: Int)(implicit state: DUSiteState): ProperPropertyComputationResult = {
        val calleeMethod = instr.resolveCallTarget(state.dm.definedMethod.classFile.thisType)
        if (calleeMethod.isEmpty) {
            return computeFinalResult(pc, StringConstancyInformation.lb)
        }

        val m = calleeMethod.value
        val callState = FunctionCallState(state, Seq(m), Map((m, ps(m, TACAI.key))))
        callState.setParamDependees(evaluateParameters(getParametersForPC(pc)))

        interpretArbitraryCallToMethods(callState)
    }
}
