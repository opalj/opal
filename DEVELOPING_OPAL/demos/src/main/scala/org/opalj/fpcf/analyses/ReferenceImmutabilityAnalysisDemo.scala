/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.analyses

import java.net.URL

import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectAnalysisApplication
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.br.fpcf.analyses.LazyUnsoundPrematurelyReadFieldsAnalysis
import org.opalj.br.fpcf.properties.ImmutableReference
import org.opalj.br.fpcf.properties.LazyInitializedReference
import org.opalj.br.fpcf.properties.MutableReference
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.fpcf.analyses.EagerL0ReferenceImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.LazyL2FieldMutabilityAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazyInterProceduralEscapeAnalysis
import org.opalj.tac.fpcf.analyses.purity.LazyL2PurityAnalysis

/**
 * Runs the EagerL0ReferenceImmutabilityAnalysis including all analysis needed for improving the result.
 *
 * @author Tobias Peter Roth
 */
object ReferenceImmutabilityAnalysisDemo extends ProjectAnalysisApplication {

    override def title: String = "runs the EagerL0ReferenceImmutabilityAnalysis"

    override def description: String =
        "runs the EagerL0ReferenceImmutabilityAnalysis"

    override def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () ⇒ Boolean
    ): BasicReport = {
        val result = analyze(project)
        BasicReport(result)
    }

    def analyze(project: Project[URL]): String = {
        val analysesManager = project.get(FPCFAnalysesManagerKey)
        analysesManager.project.get(RTACallGraphKey)
        val (propertyStore, _) = analysesManager.runAll(
            //EagerUnsoundPrematurelyReadFieldsAnalysis,
            //EagerL2PurityAnalysis,
            //EagerL2FieldMutabilityAnalysis,
            EagerL0ReferenceImmutabilityAnalysis,
            LazyL2FieldMutabilityAnalysis,
            LazyUnsoundPrematurelyReadFieldsAnalysis,
            LazyL2PurityAnalysis,
            LazyInterProceduralEscapeAnalysis
        );

        "Mutable References: "+propertyStore
            .finalEntities(MutableReference)
            .toList
            .toString()+"\n"+
            "Lazy Initialized Reference: "+propertyStore
            .finalEntities(LazyInitializedReference)
            .toList
            .toString()+"\n"+
            "Immutable References: "+propertyStore
            .finalEntities(ImmutableReference)
            .toList
            .toString()
    }

}
