/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses

import org.opalj.value.ValueInformation
import org.opalj.br.Method
import org.opalj.br.analyses.SomeProject
import org.opalj.ai.AIResult
import org.opalj.ai.Domain
import org.opalj.ai.domain.RecordDefUse
import org.opalj.tac.{TACAI => TACAIFactory}
import org.opalj.tac.fpcf.properties.TheTACAI

object TACAIAnalysis {

    def computeTheTACAI(
        m:                  Method,
        aiResult:           AIResult,
        detachFromAIResult: Boolean
    )(
        implicit
        p: SomeProject
    ): TheTACAI = {
        val typedAIResult = aiResult.asInstanceOf[AIResult { val domain: Domain with RecordDefUse }]
        val taCode = TACAIFactory(p, m, typedAIResult)
        val theTACode = if (detachFromAIResult) taCode.detach() else taCode
        val tacaiProperty = TheTACAI(
            // the following cast is safe - see TACode for details
            theTACode.asInstanceOf[TACode[TACMethodParameter, DUVar[ValueInformation]]]
        )
        tacaiProperty
    }

}
