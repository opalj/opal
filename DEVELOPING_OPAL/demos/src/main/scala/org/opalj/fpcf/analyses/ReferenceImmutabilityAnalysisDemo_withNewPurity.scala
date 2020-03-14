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
import org.opalj.fpcf.PropertyStore
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.fpcf.analyses.EagerL0ReferenceImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazyInterProceduralEscapeAnalysis
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.util.Seconds
import org.opalj.util.PerformanceEvaluation.memory
import java.io._
import java.util.Calendar
import org.opalj.br.fpcf.analyses.LazyL0CompileTimeConstancyAnalysis
import org.opalj.br.fpcf.analyses.LazyStaticDataUsageAnalysis
import org.opalj.tac.fpcf.analyses.LazyFieldLocalityAnalysis
import org.opalj.tac.fpcf.analyses.LazyLxClassImmutabilityAnalysis_new
import org.opalj.tac.fpcf.analyses.LazyLxTypeImmutabilityAnalysis_new
import org.opalj.tac.fpcf.analyses.escape.LazyReturnValueFreshnessAnalysis
import org.opalj.tac.fpcf.analyses.purity.LazyL2PurityAnalysis_new

/**
 * Runs the EagerL0ReferenceImmutabilityAnalysis including all analysis needed for improving the result.
 *
 * @author Tobias Peter Roth
 */
object ReferenceImmutabilityAnalysisDemo_withNewPurity extends ProjectAnalysisApplication {

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
        var memoryConsumption: Long = 0
        var propertyStore: PropertyStore = null
        var analysisTime: Seconds = Seconds.None
        memory {
            val analysesManager = project.get(FPCFAnalysesManagerKey)
            analysesManager.project.get(RTACallGraphKey)

            time {
                propertyStore = analysesManager
                    .runAll(
                        EagerL0ReferenceImmutabilityAnalysis,
                        LazyUnsoundPrematurelyReadFieldsAnalysis,
                        LazyL2PurityAnalysis_new,
                        LazyInterProceduralEscapeAnalysis,
                        LazyReturnValueFreshnessAnalysis,
                        LazyStaticDataUsageAnalysis,
                        LazyFieldLocalityAnalysis,
                        LazyL0CompileTimeConstancyAnalysis,
                        LazyLxTypeImmutabilityAnalysis_new,
                        LazyLxClassImmutabilityAnalysis_new
                    )
                    ._1
                propertyStore.waitOnPhaseCompletion()

            } { t ⇒
                analysisTime = t.toSeconds
            }
        } { mu ⇒
            memoryConsumption = mu
        }
        var sb: StringBuilder = new StringBuilder()
        sb = sb.append("Mutable References: \n")
        val mutableReferences = propertyStore.finalEntities(MutableReference).toList
        sb = sb.append(
            mutableReferences.map(x ⇒ x.toString+"\n").toString()
        )

        sb = sb.append("\n Lazy Initialized Reference: \n")
        val lazyInitializedReferences = propertyStore
            .finalEntities(LazyInitializedReference)
            .toList
        sb = sb.append(
            lazyInitializedReferences
                .map(x ⇒ x.toString+"\n")
                .toString()
        )

        sb = sb.append("\nImmutable References: \n")
        val immutableReferences = propertyStore.finalEntities(ImmutableReference).toList
        sb = sb.append(
            immutableReferences.map(x ⇒ x.toString+"\n").toString()
        )
        sb.append(
            s""" 
         | mutable References: ${mutableReferences.size}
         | lazy initialized References: ${lazyInitializedReferences.size}
         | immutable References: ${immutableReferences.size}
         | 
         | took : $analysisTime seconds
         | needed: ${memoryConsumption / 1024 / 1024} MBytes        
         |     
         |""".stripMargin
        )

        val calendar = Calendar.getInstance()
        val file = new File(
            s"C:/MA/results/refImm_withNewPurity_${calendar.get(Calendar.YEAR)}_"+
                s"${calendar.get(Calendar.MONTH)}_${calendar.get(Calendar.DAY_OF_MONTH)}_"+
                s"${calendar.get(Calendar.HOUR_OF_DAY)}_${calendar.get(Calendar.MINUTE)}_"+
                s"${calendar.get(Calendar.MILLISECOND)}.txt"
        )
        val bw = new BufferedWriter(new FileWriter(file))
        bw.write(sb.toString())
        bw.close()
        s"""
       | took : $analysisTime seconds
       | needs : ${memoryConsumption / 1024 / 1024} MBytes
       |""".stripMargin

    }
}
