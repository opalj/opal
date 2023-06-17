/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses

import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.br.fpcf.analyses.EagerFieldAccessInformationAnalysis

/**
 * The ''key'' object to get global field access information. Runs the [[EagerFieldAccessInformationAnalysis]].
 *
 * @example To get the index use the [[Project]]'s `get` method and pass in `this` object.
 *
 * @author Maximilian Rüsch
 */
object FieldAccessInformationKey extends ProjectInformationKey[FieldAccessInformation, Nothing] {

    override def requirements(project: SomeProject): ProjectInformationKeys =
        EagerFieldAccessInformationAnalysis.requiredProjectInformation

    /**
     * Computes the field access information.
     */
    override def compute(project: SomeProject): FieldAccessInformation = {
        val propertyStore = project.get(FPCFAnalysesManagerKey).runAll(EagerFieldAccessInformationAnalysis)._1

        propertyStore.waitOnPhaseCompletion()

        FieldAccessInformation(project)
    }
}

