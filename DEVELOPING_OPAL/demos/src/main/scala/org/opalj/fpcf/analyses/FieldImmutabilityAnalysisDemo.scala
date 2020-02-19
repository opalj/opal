/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.analyses

import java.io.BufferedWriter
import java.io.FileWriter
import java.io.File
import java.net.URL
import java.util.Calendar

import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectAnalysisApplication
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.br.fpcf.analyses.EagerL0FieldImmutabilityAnalysis
import org.opalj.br.fpcf.analyses.LazyClassImmutabilityAnalysis
import org.opalj.br.fpcf.analyses.LazyTypeImmutabilityAnalysis
import org.opalj.br.fpcf.analyses.LazyUnsoundPrematurelyReadFieldsAnalysis
import org.opalj.br.fpcf.properties.DeepImmutableField
import org.opalj.br.fpcf.properties.DependentImmutableField
import org.opalj.br.fpcf.properties.MutableField
import org.opalj.br.fpcf.properties.ShallowImmutableField
import org.opalj.fpcf.PropertyStore
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.fpcf.analyses.LazyL0ReferenceImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.LazyL2FieldMutabilityAnalysis
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
object FieldImmutabilityAnalysisDemo extends ProjectAnalysisApplication {

  override def title: String = "runs the EagerL0FieldImmutabilityAnalysis"

  override def description: String =
    "runs the EagerL0FieldImmutabilityAnalysis"

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
          LazyLxClassImmutabilityAnalysis_new,
          LazyTypeImmutabilityAnalysis,
          LazyUnsoundPrematurelyReadFieldsAnalysis,
          LazyL2PurityAnalysis,
          LazyL2FieldMutabilityAnalysis,
          LazyClassImmutabilityAnalysis,
          LazyL0ReferenceImmutabilityAnalysis,
          EagerL0FieldImmutabilityAnalysis,
          LazyLxTypeImmutabilityAnalysis_new
        )
        ._1
      propertyStore.waitOnPhaseCompletion();
    } { t =>
      analysisTime = t.toSeconds
    }

    val sb: StringBuilder = new StringBuilder
    sb.append("Mutable Fields: \n")
    sb.append(
      propertyStore
        .finalEntities(MutableField)
        .toList
        .map(x => x.toString + "\n")
        .toString()
    )
    sb.append("\nShallow Immutable Fields: \n")
    sb.append(
      propertyStore
        .finalEntities(ShallowImmutableField)
        .toList
        .map(x => x.toString + "\n")
        .toString()
    )
    sb.append("\nDependet Immutable Fields: \n")
    sb.append(
      propertyStore
        .finalEntities(DependentImmutableField)
        .toList
        .map(x => x.toString + "\n")
        .toString()
    )
    sb.append("Deep Immutable Fields: ")
    sb.append(
      propertyStore
        .finalEntities(DeepImmutableField)
        .toList
        .map(x => x.toString + "\n")
        .toString
    )

    val dateString: String = Calendar.getInstance().get(Calendar.MILLISECOND).toString
    val file = new File("C:/MA/results/fieldImm" + dateString + ".txt")
    val bw = new BufferedWriter(new FileWriter(file))
    bw.write(sb.toString())
    bw.close()

    " took : " + analysisTime + " seconds"
  }
}
