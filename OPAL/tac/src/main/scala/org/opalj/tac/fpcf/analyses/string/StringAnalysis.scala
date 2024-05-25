/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string

import org.opalj.br.Method
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.properties.string.StringConstancyInformation
import org.opalj.br.fpcf.properties.string.StringConstancyProperty
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EPK
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.Result
import org.opalj.fpcf.SomeEPS
import org.opalj.fpcf.UBP
import org.opalj.tac.fpcf.properties.string.MethodStringFlow

/**
 * @author Maximilian Rüsch
 */
class StringAnalysis(override val project: SomeProject) extends FPCFAnalysis {

    def analyze(data: SContext): ProperPropertyComputationResult = {
        computeResults(StringAnalysisState(data, ps(data._3, MethodStringFlow.key)))
    }

    private def continuation(state: StringAnalysisState)(eps: SomeEPS): ProperPropertyComputationResult = {
        eps match {
            case _ if eps.pk.equals(MethodStringFlow.key) =>
                state.stringFlowDependee = eps.asInstanceOf[EOptionP[Method, MethodStringFlow]]
                computeResults(state)

            case _ =>
                throw new IllegalArgumentException(s"Unexpected eps in continuation: $eps")
        }
    }

    private def computeResults(implicit state: StringAnalysisState): ProperPropertyComputationResult = {
        if (state.hasDependees) {
            InterimResult(
                state.entity,
                StringConstancyProperty.lb,
                computeNewUpperBound(state),
                state.dependees.toSet,
                continuation(state)
            )
        } else {
            Result(state.entity, computeNewUpperBound(state))
        }
    }

    private def computeNewUpperBound(state: StringAnalysisState): StringConstancyProperty = {
        StringConstancyProperty(state.stringFlowDependee match {
            case UBP(methodStringFlow)       => StringConstancyInformation(methodStringFlow(state.entity._1, state.entity._2))
            case _: EPK[_, MethodStringFlow] => StringConstancyInformation.ub
        })
    }
}
