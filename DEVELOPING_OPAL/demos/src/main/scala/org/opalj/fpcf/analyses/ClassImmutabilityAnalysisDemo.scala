/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.analyses

import java.net.URL

import org.opalj.tac.fpcf.analyses.EagerLxClassImmutabilityAnalysis_new
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectAnalysisApplication
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.br.fpcf.analyses.EagerClassImmutabilityAnalysis
import org.opalj.br.fpcf.analyses.EagerL0FieldImmutabilityAnalysis
import org.opalj.br.fpcf.analyses.EagerL0PurityAnalysis
import org.opalj.br.fpcf.analyses.EagerTypeImmutabilityAnalysis
import org.opalj.br.fpcf.analyses.EagerUnsoundPrematurelyReadFieldsAnalysis
import org.opalj.br.fpcf.properties.DeepImmutableClass
import org.opalj.br.fpcf.properties.DependentImmutableClass
import org.opalj.br.fpcf.properties.MutableClass
import org.opalj.br.fpcf.properties.ShallowImmutableClass
import org.opalj.tac.fpcf.analyses.EagerL0ReferenceImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.EagerL1FieldMutabilityAnalysis

/**
 * Runs the EagerLxClassImmutabilityAnalysis_new as well as analysis needed for improving the result
 *
 * @author Tobias Peter Roth
 */
object ClassImmutabilityAnalysisDemo extends ProjectAnalysisApplication {

    override def title: String = "run EagerLxClassImmutabilityAnalysis_new"

    override def description: String = "run EagerLxClassImmutabilityAnalysis_new"

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
            EagerClassImmutabilityAnalysis,
            EagerTypeImmutabilityAnalysis,
            EagerUnsoundPrematurelyReadFieldsAnalysis,
            EagerL0ReferenceImmutabilityAnalysis,
            EagerL0PurityAnalysis,
            EagerL1FieldMutabilityAnalysis,
            EagerL0FieldImmutabilityAnalysis,
            EagerLxClassImmutabilityAnalysis_new
        )
        "Mutable Class: "+propertyStore
            .finalEntities(MutableClass)
            .toList
            .toString()+"\n"+
            "Dependent Immutable Class: "+propertyStore
            .finalEntities(DependentImmutableClass)
            .toList
            .toString()+"\n"+
            "Shallow Immutable Class: "+propertyStore
            .finalEntities(ShallowImmutableClass)
            .toList
            .toString()+"\n"+
            "Deep Immutable Class: "+propertyStore
            .finalEntities(DeepImmutableClass)
            .toList
            .toString()+"\n"
    }
}
