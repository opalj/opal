/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.analyses

import java.net.URL

import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectAnalysisApplication
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.br.fpcf.analyses.EagerL0FieldImmutabilityAnalysis
import org.opalj.br.fpcf.analyses.LazyUnsoundPrematurelyReadFieldsAnalysis
import org.opalj.fpcf.PropertyStore
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.fpcf.analyses.LazyL0ReferenceImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.LazyLxClassImmutabilityAnalysis_new
import org.opalj.tac.fpcf.analyses.LazyLxTypeImmutabilityAnalysis_new
import org.opalj.tac.fpcf.analyses.purity.LazyL2PurityAnalysis
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.util.Seconds

/**
 * Runs the EagerL0FieldImmutabilityAnalysis including analysis needed for improving the result.
 *
 * @author Tobias Peter Roth
 */
object FieldImmutabilityAnalysisDemo_performanceMeasurements extends ProjectAnalysisApplication {

    override def title: String = "runs the EagerL0FieldImmutabilityAnalysis"

    override def description: String =
        "runs the EagerL0FieldImmutabilityAnalysis"

    override def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () ⇒ Boolean
    ): BasicReport = {
        val result = analyze(project)
        BasicReport(result)
    }

    def analyze(theProject: Project[URL]): String = {
        var times: List[Seconds] = Nil: List[Seconds]
        for (i ← 0 until 10) {
            val project = Project.recreate(theProject)
            val analysesManager = project.get(FPCFAnalysesManagerKey)
            analysesManager.project.get(RTACallGraphKey)
            var propertyStore: PropertyStore = null
            var analysisTime: Seconds = Seconds.None
            time {
                propertyStore = analysesManager
                    .runAll(
                        LazyLxClassImmutabilityAnalysis_new,
                        LazyUnsoundPrematurelyReadFieldsAnalysis,
                        LazyL2PurityAnalysis,
                        LazyL0ReferenceImmutabilityAnalysis,
                        EagerL0FieldImmutabilityAnalysis,
                        LazyLxTypeImmutabilityAnalysis_new
                    )
                    ._1
                propertyStore.waitOnPhaseCompletion();
            } { t ⇒
                analysisTime = t.toSeconds
            }
            times = analysisTime :: times
        }

        /**
         * "Mutable Fields: "+propertyStore
         * .finalEntities(MutableField)
         * .toList
         * .toString()+"\n"+
         * "Shallow Immutable Fields: "+propertyStore
         * .finalEntities(ShallowImmutableField)
         * .toList
         * .toString()+"\n"+
         * "Dependet Immutable Fields:"+propertyStore
         * .finalEntities(DependentImmutableField(None))
         * .toList
         * .toString()+"\n"+
         * "Deep Immutable Fields: "+propertyStore
         * .finalEntities(DeepImmutableField)
         * .toList
         * .toString()+"\n"+
         * propertyStore
         * .entities(FieldImmutability.key)
         * .toList
         * .toString*
         */
        times.foreach(s ⇒ println(s+" seconds"))
        val aver = times.fold(new Seconds(0))((x: Seconds, y: Seconds) ⇒ x + y).timeSpan / times.size
        f"took: $aver seconds on average"
    }
}
