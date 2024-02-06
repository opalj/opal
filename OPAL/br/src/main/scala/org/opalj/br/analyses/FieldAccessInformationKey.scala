/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses

import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.br.fpcf.analyses.EagerSimpleFieldAccessInformationAnalysis
import org.opalj.log.OPALLogger

/**
 * The ''key'' object to get global field access information. Runs the [[EagerSimpleFieldAccessInformationAnalysis]] if
 * no analyses were given in the initialization data.
 *
 * @example To get the index use the [[Project]]'s `get` method and pass in `this` object.
 *
 * @author Maximilian Rüsch
 */
object FieldAccessInformationKey extends ProjectInformationKey[FieldAccessInformation, Seq[FPCFAnalysisScheduler]] {

    override def requirements(project: SomeProject): ProjectInformationKeys = {
        val schedulers = project.getOrCreateProjectInformationKeyInitializationData(
            this, {
                OPALLogger.warn(
                    "analysis configuration",
                    s"no field access information analysis configured, using SimpleFieldAccessInformationAnalysis as a fallback"
                )(project.logContext)
                Seq(EagerSimpleFieldAccessInformationAnalysis)
            }
        )

        schedulers.flatMap(_.requiredProjectInformation)
    }

    /**
     * Computes the field access information.
     */
    override def compute(project: SomeProject): FieldAccessInformation = {
        val schedulers = project.getProjectInformationKeyInitializationData(this) match {
            case Some(s) => s
            case None =>
                OPALLogger.error(
                    "analysis configuration",
                    s"no field access information analysis configured even though requirements were run"
                )(project.logContext)
                throw new IllegalStateException()
        }

        project.get(FPCFAnalysesManagerKey).runAll(schedulers)._1.waitOnPhaseCompletion()

        FieldAccessInformation(project)
    }
}
