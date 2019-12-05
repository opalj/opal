/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.analyses

import java.net.URL

import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectAnalysisApplication
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.br.fpcf.analyses.LazyClassImmutabilityAnalysis
import org.opalj.br.fpcf.analyses.LazyL0FieldImmutabilityAnalysis
import org.opalj.br.fpcf.analyses.LazyUnsoundPrematurelyReadFieldsAnalysis
import org.opalj.br.fpcf.properties.DeepImmutableType
import org.opalj.br.fpcf.properties.DependentImmutableType
import org.opalj.br.fpcf.properties.MutableType_new
import org.opalj.br.fpcf.properties.ShallowImmutableType
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.fpcf.analyses.EagerLxTypeImmutabilityAnalysis_new
import org.opalj.tac.fpcf.analyses.LazyLxClassImmutabilityAnalysis_new
import org.opalj.tac.fpcf.analyses.purity.LazyL2PurityAnalysis
//import org.opalj.br.fpcf.properties.ShallowImmutableType
import org.opalj.tac.fpcf.analyses.LazyL0ReferenceImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.LazyL2FieldMutabilityAnalysis

/**
 * Runs the EagerLxClassImmutabilityAnalysis_new as well as analysis needed for improving the result
 *
 * @author Tobias Peter Roth
 */
object TypeImmutabilityAnalysisDemo extends ProjectAnalysisApplication {

    override def title: String = "run EagerLxTypeImmutabilityAnalysis_new"

    override def description: String = "run EagerLxTypeImmutabilityAnalysis_new"

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
            /**
             * LazyTypeImmutabilityAnalysis,
             * LazyUnsoundPrematurelyReadFieldsAnalysis,
             * LazyL0ReferenceImmutabilityAnalysis,
             * LazyL0PurityAnalysis,
             * LazyL2FieldMutabilityAnalysis,
             * LazyL0FieldImmutabilityAnalysis,
             * EagerLxClassImmutabilityAnalysis_new,
             * EagerLxTypeImmutabilityAnalysis_new*
             */
            //LazyTypeImmutabilityAnalysis,
            LazyUnsoundPrematurelyReadFieldsAnalysis,
            LazyL2PurityAnalysis,
            LazyL2FieldMutabilityAnalysis,
            LazyClassImmutabilityAnalysis,
            LazyL0ReferenceImmutabilityAnalysis,
            LazyL0FieldImmutabilityAnalysis,
            EagerLxTypeImmutabilityAnalysis_new,
            LazyLxClassImmutabilityAnalysis_new
        )
        "Mutable Type: "+propertyStore
            .finalEntities(MutableType_new)
            .toList
            .toString()+"\n"+
            "Shallow Immutable Type: "+propertyStore
            .finalEntities(ShallowImmutableType)
            .toList
            .toString()+"\n"+
            "Dependent Immutable Type: "+propertyStore
            .finalEntities(DependentImmutableType)
            .toList
            .toString()+"\n"+
            "Deep Immutable Type: "+propertyStore
            .finalEntities(DeepImmutableType)
            .toList
            .toString()+"\n"
    }
}
