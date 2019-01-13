/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package support
package info

import java.net.URL

import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.DefaultOneStepAnalysis
import org.opalj.br.analyses.Project
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.br.fpcf.analyses.LazyUnsoundPrematurelyReadFieldsAnalysis
import org.opalj.br.fpcf.cg.properties.ReflectionRelatedCallees
import org.opalj.br.fpcf.cg.properties.SerializationRelatedCallees
import org.opalj.br.fpcf.cg.properties.StandardInvokeCallees
import org.opalj.br.fpcf.cg.properties.ThreadRelatedIncompleteCallSites
import org.opalj.br.fpcf.properties.DeclaredFinalField
import org.opalj.br.fpcf.properties.EffectivelyFinalField
import org.opalj.br.fpcf.properties.LazyInitializedField
import org.opalj.br.fpcf.properties.NonFinalFieldByAnalysis
import org.opalj.br.fpcf.properties.NonFinalFieldByLackOfInformation
import org.opalj.ai.fpcf.analyses.LazyL0BaseAIAnalysis
import org.opalj.tac.fpcf.analyses.purity.LazyL2PurityAnalysis
import org.opalj.tac.fpcf.analyses.TACAITransformer
import org.opalj.tac.fpcf.analyses.TriggeredSystemPropertiesAnalysis
import org.opalj.tac.fpcf.analyses.cg.LazyCalleesAnalysis
import org.opalj.tac.fpcf.analyses.cg.TriggeredConfiguredNativeMethodsAnalysis
import org.opalj.tac.fpcf.analyses.cg.TriggeredInstantiatedTypesAnalysis
import org.opalj.tac.fpcf.analyses.cg.reflection.TriggeredReflectionRelatedCallsAnalysis
import org.opalj.tac.fpcf.analyses.cg.RTACallGraphAnalysisScheduler
import org.opalj.tac.fpcf.analyses.cg.TriggeredFinalizerAnalysisScheduler
import org.opalj.tac.fpcf.analyses.cg.TriggeredLoadedClassesAnalysis
import org.opalj.tac.fpcf.analyses.cg.TriggeredSerializationRelatedCallsAnalysis
import org.opalj.tac.fpcf.analyses.cg.TriggeredStaticInitializerAnalysis
import org.opalj.tac.fpcf.analyses.cg.TriggeredThreadRelatedCallsAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazyInterProceduralEscapeAnalysis
import org.opalj.tac.fpcf.analyses.EagerL1FieldMutabilityAnalysis

/**
 * Computes the field mutability; see [[org.opalj.br.fpcf.properties.FieldMutability]] for details.
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
            /* Call Graph Analyses */
            RTACallGraphAnalysisScheduler,
            TriggeredStaticInitializerAnalysis,
            TriggeredLoadedClassesAnalysis,
            TriggeredFinalizerAnalysisScheduler,
            TriggeredThreadRelatedCallsAnalysis,
            TriggeredSerializationRelatedCallsAnalysis,
            TriggeredReflectionRelatedCallsAnalysis,
            TriggeredInstantiatedTypesAnalysis,
            TriggeredConfiguredNativeMethodsAnalysis,
            TriggeredSystemPropertiesAnalysis,
            LazyCalleesAnalysis(
                Set(
                    StandardInvokeCallees,
                    SerializationRelatedCallees,
                    ReflectionRelatedCallees,
                    ThreadRelatedIncompleteCallSites
                )
            ),
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
