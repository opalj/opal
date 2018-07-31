/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package analyses

import org.opalj.ai.Domain
import org.opalj.ai.InterruptableAI
import org.opalj.br.Method
import org.opalj.br.analyses.ProjectInformationKey
import org.opalj.br.analyses.SomeProject
import org.opalj.log.OPALLogger

/**
 * The ''key'' object to get information about the "more precise" return values.
 *
 * @author Michael Eichberg
 */
object MethodReturnValuesKey extends ProjectInformationKey[MethodReturnValueInformation, Nothing] {

    override protected def requirements = Seq(FieldValuesKey)

    /**
     * Computes the return value information.
     */
    override protected def compute(project: SomeProject): MethodReturnValueInformation = {

        implicit val logContext = project.logContext

        val fieldValueInformation = project.get(FieldValuesKey)

        OPALLogger.info("progress", "computing method return value information")
        val result = MethodReturnValuesAnalysis.doAnalyze(
            project,
            () ⇒ false, // make it configurable
            (ai: InterruptableAI[Domain], method: Method) ⇒
                // TODO Make the used domain configurable... delegate to the TACAIKey???
                new BaseMethodReturnValuesAnalysisDomain(
                    project, fieldValueInformation, ai, method
                )
        )

        OPALLogger.info("progress", s"refined the return type of ${result.size} methods")
        result
    }
}

