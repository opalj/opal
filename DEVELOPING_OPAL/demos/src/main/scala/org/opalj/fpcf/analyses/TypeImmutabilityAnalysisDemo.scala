/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.analyses

import java.net.URL

import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectAnalysisApplication
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.br.fpcf.analyses.LazyL0FieldImmutabilityAnalysis
import org.opalj.br.fpcf.analyses.LazyL0PurityAnalysis
import org.opalj.br.fpcf.analyses.LazyTypeImmutabilityAnalysis
import org.opalj.br.fpcf.analyses.LazyUnsoundPrematurelyReadFieldsAnalysis
import org.opalj.br.fpcf.properties.DeepImmutableType
import org.opalj.br.fpcf.properties.MutableType_new
import org.opalj.br.fpcf.properties.ShallowImmutableType
//import org.opalj.br.fpcf.properties.ShallowImmutableType
import org.opalj.tac.fpcf.analyses.EagerLxTypeImmutabilityAnalysis_new
import org.opalj.tac.fpcf.analyses.LazyL0ReferenceImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.LazyL2FieldMutabilityAnalysis
import org.opalj.tac.fpcf.analyses.EagerLxClassImmutabilityAnalysis_new

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

        val (propertyStore, _) = analysesManager.runAll(
            LazyTypeImmutabilityAnalysis,
            LazyUnsoundPrematurelyReadFieldsAnalysis,
            LazyL0ReferenceImmutabilityAnalysis,
            LazyL0PurityAnalysis,
            LazyL2FieldMutabilityAnalysis,
            LazyL0FieldImmutabilityAnalysis,
            EagerLxClassImmutabilityAnalysis_new,
            EagerLxTypeImmutabilityAnalysis_new
        )
        "Mutable Type: "+propertyStore
            .finalEntities(MutableType_new)
            .toList
            .toString()+"\n"+
            "Shallow Immutable Type: "+propertyStore
            .finalEntities(ShallowImmutableType)
            .toList
            .toString()+"\n"+
            "Deep Immutable Type: "+propertyStore
            .finalEntities(DeepImmutableType)
            .toList
            .toString()+"\n"
    }
}
