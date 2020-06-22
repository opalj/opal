/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.analyses

import java.net.URL

import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectAnalysisApplication
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.br.fpcf.analyses.LazyClassImmutabilityAnalysis
import org.opalj.br.fpcf.analyses.LazyStaticDataUsageAnalysis
import org.opalj.br.fpcf.analyses.LazyTypeImmutabilityAnalysis
import org.opalj.br.fpcf.analyses.LazyUnsoundPrematurelyReadFieldsAnalysis
import org.opalj.fpcf.PropertyStore
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.fpcf.analyses.EagerL0ReferenceImmutabilityLazyInitializationAnalysis
import org.opalj.tac.fpcf.analyses.LazyFieldLocalityAnalysis
import org.opalj.tac.fpcf.analyses.LazyL2FieldMutabilityAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazyInterProceduralEscapeAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazyReturnValueFreshnessAnalysis
import org.opalj.tac.fpcf.analyses.immutability.reference.LazyL0ReferenceImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.purity.LazyL2PurityAnalysis
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.util.Seconds

/**
 * Runs the EagerL0ReferenceImmutabilityAnalysis including all analysis needed for improving the result.
 *
 * @author Tobias Peter Roth
 */
object ReferenceImmutabilityLazyInitializationAnalysisDemo_performanceMeasurements
    extends ProjectAnalysisApplication {

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
                        EagerL0ReferenceImmutabilityLazyInitializationAnalysis,
                        LazyL0ReferenceImmutabilityAnalysis,
                        LazyL2FieldMutabilityAnalysis,
                        LazyUnsoundPrematurelyReadFieldsAnalysis,
                        LazyL2PurityAnalysis,
                        LazyInterProceduralEscapeAnalysis,
                        LazyStaticDataUsageAnalysis,
                        LazyTypeImmutabilityAnalysis,
                        LazyClassImmutabilityAnalysis,
                        LazyFieldLocalityAnalysis,
                        LazyReturnValueFreshnessAnalysis
                    )
                    ._1
                propertyStore.waitOnPhaseCompletion();
            } { t ⇒
                analysisTime = t.toSeconds
            }
            times = analysisTime :: times
        }
        /*def printResult(string: String, property: Property)(
            implicit
            propertyStore: PropertyStore
        ): Unit = {} **/
        /**
         * val notInitializedReferencesString = propertyStore
         * .finalEntities(NoLazyInitialization)
         * .toList
         * .toString()
         * val notThreadSafeLazyInitializationString = propertyStore
         * .finalEntities(NotThreadSafeLazyInitialization)
         * .toList
         * .toString()
         * val lazyInitializationString = propertyStore
         * .finalEntities(LazyInitialization)
         * .toList
         * .toString()
         * println(s"Not initialized References $notInitializedReferencesString")
         * println(s"Not threadsafe Lazy Initialization: $notThreadSafeLazyInitializationString")
         * println(s"Lazy Initialization String: $lazyInitializationString")
         */
        /**
         * "Not lazy initialized References: " + propertyStore
         * .finalEntities(NoLazyInitialization)
         * .toList
         * .toString() + "\n" +
         * "Not thread safe lazy initialization: " + propertyStore
         * .finalEntities(NotThreadSafeLazyInitialization)
         * .toList
         * .toString() + "\n" +
         * "Lazy Initialization: " + propertyStore
         * .finalEntities(LazyInitialization)
         * .toList
         * .toString()*
         */
        times.foreach(s ⇒ println(s+" seconds"))
        val aver = times.fold(new Seconds(0))((x: Seconds, y: Seconds) ⇒ x + y).timeSpan / times.size
        f"took: $aver seconds on average"
    }
}
