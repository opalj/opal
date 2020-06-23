/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.analyses

import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.net.URL
import java.util.Calendar

import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectAnalysisApplication
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.br.fpcf.analyses.LazyL0CompileTimeConstancyAnalysis
import org.opalj.br.fpcf.analyses.LazyStaticDataUsageAnalysis
import org.opalj.br.fpcf.analyses.LazyUnsoundPrematurelyReadFieldsAnalysis
import org.opalj.fpcf.PropertyStore
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.fpcf.analyses.LazyFieldLocalityAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazyInterProceduralEscapeAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazyReturnValueFreshnessAnalysis
import org.opalj.tac.fpcf.analyses.immutability.EagerL0FieldImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.immutability.LazyLxClassImmutabilityAnalysis_new
import org.opalj.tac.fpcf.analyses.immutability.LazyLxTypeImmutabilityAnalysis_new
import org.opalj.tac.fpcf.analyses.immutability.reference.LazyL0ReferenceImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.purity.LazyL2PurityAnalysis_new
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
        var tmpProject = theProject
        var times: List[Seconds] = Nil: List[Seconds]
        println("before")
        for (i ← 0 to 10) {
            val project = tmpProject.recreate()
            val analysesManager = project.get(FPCFAnalysesManagerKey)
            analysesManager.project.get(RTACallGraphKey)
            var propertyStore: PropertyStore = null
            var analysisTime: Seconds = Seconds.None
            time {
                propertyStore = analysesManager
                    .runAll(
                        LazyLxClassImmutabilityAnalysis_new,
                        LazyUnsoundPrematurelyReadFieldsAnalysis,
                        LazyL2PurityAnalysis_new,
                        LazyL0ReferenceImmutabilityAnalysis,
                        EagerL0FieldImmutabilityAnalysis,
                        LazyLxTypeImmutabilityAnalysis_new,
                        LazyStaticDataUsageAnalysis,
                        LazyL0CompileTimeConstancyAnalysis,
                        LazyInterProceduralEscapeAnalysis,
                        LazyReturnValueFreshnessAnalysis,
                        LazyFieldLocalityAnalysis
                    )
                    ._1
                propertyStore.waitOnPhaseCompletion();
            } { t ⇒
                analysisTime = t.toSeconds
            }
            times = analysisTime :: times
            tmpProject = project
        }

        val sortedList = times.sortWith(_.timeSpan < _.timeSpan)
        val median = sortedList(5)

        val output =
            s"""
           |
           |${sortedList.mkString("\n")}
           |   
           |Median: $median
           |lowest: ${sortedList(0)}
           |highest: ${sortedList(sortedList.size - 1)}
           |""".stripMargin

        val calendar = Calendar.getInstance()
        val file = new File(
            s"C:/MA/results_time/fieldImm_${calendar.get(Calendar.YEAR)}_"+
                s"${calendar.get(Calendar.MONTH)}_${calendar.get(Calendar.DAY_OF_MONTH)}_"+
                s"${calendar.get(Calendar.HOUR_OF_DAY)}_${calendar.get(Calendar.MINUTE)}_"+
                s"${calendar.get(Calendar.MILLISECOND)}.txt"
        )
        val bw = new BufferedWriter(new FileWriter(file))
        bw.write(output)
        bw.close()

        s"""
       | $output
       | took: $median seconds as median
       |""".stripMargin
    }
}
