/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package support
package info

import java.net.URL

import org.opalj.ai.fpcf.analyses.LazyL0BaseAIAnalysis
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.DefaultOneStepAnalysis
import org.opalj.br.analyses.Project
import org.opalj.fpcf.analyses.EagerL1FieldMutabilityAnalysis
import org.opalj.fpcf.analyses.purity.LazyL2PurityAnalysis
import org.opalj.fpcf.analyses.LazyUnsoundPrematurelyReadFieldsAnalysis
import org.opalj.fpcf.FPCFAnalysesManagerKey
import org.opalj.fpcf.analyses.escape.LazyInterProceduralEscapeAnalysis
import org.opalj.fpcf.properties.DeclaredFinalField
import org.opalj.fpcf.properties.LazyInitializedField
import org.opalj.fpcf.properties.EffectivelyFinalField
import org.opalj.fpcf.properties.NonFinalFieldByAnalysis
import org.opalj.fpcf.properties.NonFinalFieldByLackOfInformation
import org.opalj.tac.fpcf.analyses.TACAITransformer

/**
 * Computes the field mutability; see [[org.opalj.fpcf.properties.FieldMutability]] for details.
 *
 * @author Dominik Helm
 */
object FieldMutability extends DefaultOneStepAnalysis {

    override def title: String = "Field mutability"

    override def description: String = {
        "Provides information about the mutability of fields."
    }

    override def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () ⇒ Boolean
    ): BasicReport = {
        val (ps, _) = project.get(FPCFAnalysesManagerKey).runAll(
            LazyL0BaseAIAnalysis,
            TACAITransformer,
            LazyUnsoundPrematurelyReadFieldsAnalysis,
            LazyInterProceduralEscapeAnalysis,
            LazyL2PurityAnalysis,
            EagerL1FieldMutabilityAnalysis
        )

        val declaredFinal = ps.finalEntities(DeclaredFinalField).toSeq
        val effectivelyFinal = ps.finalEntities(EffectivelyFinalField).toSeq
        val lazyInitialized = ps.finalEntities(LazyInitializedField).toSeq
        val nonFinalByAnalysis = ps.finalEntities(NonFinalFieldByAnalysis).toSeq
        val nonFinalByLackOfInformation = ps.finalEntities(NonFinalFieldByLackOfInformation).toSeq

        val message =
            s"""|# of declared final fields: ${declaredFinal.size}
                |# of effectively final fields: ${effectivelyFinal.size}
                |# of lazy initialized fields: ${lazyInitialized.size}
                |# of non final fields (by analysis): ${nonFinalByAnalysis.size}
                |# of non final fields (by lack of information): ${nonFinalByLackOfInformation.size}
                |"""

        BasicReport(message.stripMargin('|'))
    }
}
