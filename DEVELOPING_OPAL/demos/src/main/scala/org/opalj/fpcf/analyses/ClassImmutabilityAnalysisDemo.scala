/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.analyses

import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.net.URL
import java.util.Calendar

import org.opalj.tac.fpcf.analyses.EagerLxClassImmutabilityAnalysis_new
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectAnalysisApplication
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.br.fpcf.analyses.LazyClassImmutabilityAnalysis
import org.opalj.br.fpcf.analyses.LazyL0FieldImmutabilityAnalysis
import org.opalj.br.fpcf.analyses.LazyStaticDataUsageAnalysis
import org.opalj.br.fpcf.analyses.LazyTypeImmutabilityAnalysis
import org.opalj.br.fpcf.analyses.LazyUnsoundPrematurelyReadFieldsAnalysis
import org.opalj.br.fpcf.properties.ClassImmutability_new
import org.opalj.br.fpcf.properties.DeepImmutableClass
import org.opalj.br.fpcf.properties.DependentImmutableClass
import org.opalj.br.fpcf.properties.MutableClass
import org.opalj.br.fpcf.properties.ShallowImmutableClass
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.PropertyStore
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.fpcf.analyses.LazyFieldLocalityAnalysis
import org.opalj.tac.fpcf.analyses.LazyL0ReferenceImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.LazyL2FieldMutabilityAnalysis
import org.opalj.tac.fpcf.analyses.LazyLxTypeImmutabilityAnalysis_new
import org.opalj.tac.fpcf.analyses.escape.LazyReturnValueFreshnessAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazySimpleEscapeAnalysis
import org.opalj.tac.fpcf.analyses.purity.LazyL2PurityAnalysis
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.util.Seconds

/**
 * Runs the EagerLxClassImmutabilityAnalysis_new as well as analysis needed for improving the result
 *
 * @author Tobias Peter Roth
 */
object ClassImmutabilityAnalysisDemo extends ProjectAnalysisApplication {

  override def title: String = "run EagerLxClassImmutabilityAnalysis_new"

  override def description: String = "run EagerLxClassImmutabilityAnalysis_new"

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
          LazyUnsoundPrematurelyReadFieldsAnalysis,
          LazyL2PurityAnalysis,
          LazyL2FieldMutabilityAnalysis,
          LazyClassImmutabilityAnalysis,
          LazyL0ReferenceImmutabilityAnalysis,
          LazyL0FieldImmutabilityAnalysis,
          LazyLxTypeImmutabilityAnalysis_new,
          EagerLxClassImmutabilityAnalysis_new,
          LazyTypeImmutabilityAnalysis,
          LazyReturnValueFreshnessAnalysis,
          LazyFieldLocalityAnalysis,
          LazyStaticDataUsageAnalysis,
          LazySimpleEscapeAnalysis
        )
        ._1
      propertyStore.waitOnPhaseCompletion();
    } { t =>
      analysisTime = t.toSeconds
    }

    val sb = new StringBuilder
    sb.append("Mutable Class: \n")
    sb.append(
      propertyStore
        .finalEntities(MutableClass)
        .toList
        .map(x => x.toString + "\n")
    )
    sb.append("\nDependent Immutable Class: \n")
    sb.append(
      propertyStore
        .entities(ClassImmutability_new.key)
        .toList
        .collect({ case x @ FinalEP(_, DependentImmutableClass) => x })
        .map(x => x.toString + "\n")
    )
    sb.append("\nShallow Immutable Class: \n")
    sb.append(
      propertyStore
        .finalEntities(ShallowImmutableClass)
        .toList
        .map(x => x.toString + "\n")
    )
    sb.append("\nDeep Immutable Class: \n")
    sb.append(
      propertyStore
        .finalEntities(DeepImmutableClass)
        .toList
        .map(x => x.toString + "\n")
    )

    val dateString: String = Calendar.getInstance().get(Calendar.MILLISECOND).toString
    val file = new File("C:/MA/results/classImm" + dateString + ".txt")
    val bw = new BufferedWriter(new FileWriter(file))
    bw.write(sb.toString())
    bw.close()

    " took : " + analysisTime + " seconds"
  }
}
