/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses

import java.net.URL

import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.ProjectAnalysisApplication
import org.opalj.br.analyses.Project
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.br.fpcf.analyses.{EagerClassImmutabilityAnalysis, EagerL3FieldMutabilityAnalysis_new, EagerTypeImmutabilityAnalysis}
import org.opalj.br.fpcf.properties.{DeepImmutableField, MutableField, ShallowImmutableField}
import org.opalj.tac.fpcf.analyses.EagerL1FieldMutabilityAnalysis

/**
 * Runs the new field mutability implementation
 *
 * @author Tobias Peter Roth
 */
object RunFieldMutabilityAnalysisNew extends ProjectAnalysisApplication {

    override def title: String = "run new field immutability analysis"

    override def description: String = "run new field immutability analysis"

    override def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () ⇒ Boolean
    ): BasicReport = {
        val result = analyze(project)
        BasicReport(result)
    }

    def analyze(project: Project[URL]): String = {
        //project.
        val analysesManager = project.get(FPCFAnalysesManagerKey)

        val (propertyStore, _) = analysesManager.runAll(EagerTypeImmutabilityAnalysis, EagerL3FieldMutabilityAnalysis_new, EagerL1FieldMutabilityAnalysis, EagerClassImmutabilityAnalysis);
        return "Mutable Fields: "+propertyStore.finalEntities(MutableField).toList.toString()+"\n"+
            "Shallow Immutable Fields: "+propertyStore.finalEntities(ShallowImmutableField).toList.toString()+"\n"+
            "Deep Immutable Fields: "+propertyStore.finalEntities(DeepImmutableField).toList.toString()
    }
}
