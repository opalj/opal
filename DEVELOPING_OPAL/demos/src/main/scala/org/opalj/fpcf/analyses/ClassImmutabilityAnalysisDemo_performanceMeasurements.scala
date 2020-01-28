/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.analyses

import java.net.URL

import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectAnalysisApplication
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.br.fpcf.analyses.LazyClassImmutabilityAnalysis
import org.opalj.br.fpcf.analyses.LazyL0FieldImmutabilityAnalysis
import org.opalj.br.fpcf.analyses.LazyStaticDataUsageAnalysis
import org.opalj.br.fpcf.analyses.LazyTypeImmutabilityAnalysis
import org.opalj.br.fpcf.analyses.LazyUnsoundPrematurelyReadFieldsAnalysis
import org.opalj.fpcf.PropertyStore
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.fpcf.analyses.EagerLxClassImmutabilityAnalysis_new
import org.opalj.tac.fpcf.analyses.LazyFieldLocalityAnalysis
import org.opalj.tac.fpcf.analyses.LazyL0ReferenceImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.LazyL2FieldMutabilityAnalysis
import org.opalj.tac.fpcf.analyses.LazyLxTypeImmutabilityAnalysis_new
import org.opalj.tac.fpcf.analyses.escape.LazyReturnValueFreshnessAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazySimpleEscapeAnalysis
import org.opalj.tac.fpcf.analyses.purity.LazyL2PurityAnalysis
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.util.Seconds;

/**
 * Runs the EagerLxClassImmutabilityAnalysis_new as well as analysis needed for improving the result
 *
 * @author Tobias Peter Roth
 */
object ClassImmutabilityAnalysisDemo_performanceMeasurements extends ProjectAnalysisApplication {

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

  def analyze(theProject: Project[URL]): String = {
    var times: List[Seconds] = Nil: List[Seconds]
    for (i <- 0 until 19) {
      val project = Project.recreate(theProject)
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
      times = analysisTime :: times
    }

    /**
     * "Mutable Class: "+propertyStore
     * .finalEntities(MutableClass)
     * .toList
     * .toString()+"\n"+
     * "Dependent Immutable Class: "+propertyStore
     * .entities(ClassImmutability_new.key)
     * .toList
     * .toString()+"\n"+
     * "Shallow Immutable Class: "+propertyStore
     * .finalEntities(ShallowImmutableClass)
     * .toList
     * .toString()+"\n"+
     * "Deep Immutable Class: "+propertyStore
     * .finalEntities(DeepImmutableClass)
     * .toList
     * .toString()+"\n"
     */
    times.foreach(s => println(s + " seconds"))
    val aver = times.fold(new Seconds(0))((x: Seconds, y: Seconds) => x + y).timeSpan / times.size
    f"took: $aver seconds on average"
  }
}
