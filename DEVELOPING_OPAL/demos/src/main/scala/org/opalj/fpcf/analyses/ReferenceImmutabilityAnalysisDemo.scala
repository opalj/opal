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
import org.opalj.tac.fpcf.analyses.LazyL2FieldMutabilityAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazyInterProceduralEscapeAnalysis
import org.opalj.tac.fpcf.analyses.purity.LazyL2PurityAnalysis
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.util.Seconds
import java.io._
import java.util.Calendar

import org.opalj.br.fpcf.analyses.LazyClassImmutabilityAnalysis
import org.opalj.br.fpcf.analyses.LazyStaticDataUsageAnalysis
import org.opalj.br.fpcf.analyses.LazyTypeImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.LazyFieldLocalityAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazyReturnValueFreshnessAnalysis

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
      project: Project[URL],
      parameters: Seq[String],
      isInterrupted: () => Boolean
  ): BasicReport = {
    val result = analyze(project)
    BasicReport(result)
  }

  def analyze(project: Project[URL]): String = {
    val analysesManager = project.get(FPCFAnalysesManagerKey)
    analysesManager.project.get(RTACallGraphKey)
    var propertyStore: PropertyStore = null
    var analysisTime: Seconds = Seconds.None
    time {
      propertyStore = analysesManager
        .runAll(
          EagerL0ReferenceImmutabilityAnalysis,
          LazyL2FieldMutabilityAnalysis,
          LazyUnsoundPrematurelyReadFieldsAnalysis,
          LazyL2PurityAnalysis,
          LazyInterProceduralEscapeAnalysis,
          LazyReturnValueFreshnessAnalysis,
          LazyStaticDataUsageAnalysis,
          LazyTypeImmutabilityAnalysis,
          LazyClassImmutabilityAnalysis,
          LazyFieldLocalityAnalysis
        )
        ._1
      propertyStore.waitOnPhaseCompletion();
    } { t =>
      analysisTime = t.toSeconds
    }
    var sb: StringBuilder = new StringBuilder()
    sb = sb.append("Mutable References: \n")
    sb = sb.append(
      propertyStore.finalEntities(MutableReference).toList.map(x => x.toString + "\n").toString()
    )

    sb = sb.append("\n Lazy Initialized Reference: \n")
    sb = sb.append(
      propertyStore
        .finalEntities(LazyInitializedReference)
        .toList
        .map(x => x.toString + "\n")
        .toString()
    )

    /**
     * .toList
     * .toString() + "\n" +*
     */
    sb = sb.append("\nImmutable References: \n")
    sb = sb.append(
      propertyStore.finalEntities(ImmutableReference).toList.map(x => x.toString + "\n").toString()
    )
    val dateString: String = Calendar.getInstance().get(Calendar.MILLISECOND).toString
    val file = new File("C:/MA/results/refLazyImm" + dateString + ".txt")
    val bw = new BufferedWriter(new FileWriter(file))
    bw.write(sb.toString())
    bw.close()

    " took : " + analysisTime + " seconds"
  }
}
