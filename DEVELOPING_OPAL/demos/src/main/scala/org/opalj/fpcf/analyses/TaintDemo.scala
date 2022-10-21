/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.analyses

import org.opalj.ai.domain
import org.opalj.ai.fpcf.properties.AIDomainFactoryKey
import org.opalj.br.analyses.{BasicReport, Project, ProjectAnalysisApplication}
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.fpcf.PropertyStore
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.fpcf.properties.Taint
import org.opalj.js.IFDSAnalysisJSFixtureScheduler

import java.net.URL

object TaintDemo extends ProjectAnalysisApplication {

  override def title: String = "..."

  override def description: String = "..."

  override def doAnalyze(
                          project: Project[URL],
                          parameters: Seq[String],
                          isInterrupted: () => Boolean
                        ): BasicReport = {
    val result = analyze(project)
    BasicReport(result)
  }

  def analyze(project: Project[URL]): String = {

    var propertyStore: PropertyStore = null
    val analysesManager = project.get(FPCFAnalysesManagerKey)
    project.get(RTACallGraphKey)

    project.updateProjectInformationKeyInitializationData(AIDomainFactoryKey) { _ =>
      Set[Class[_ <: AnyRef]](classOf[domain.l2.DefaultPerformInvocationsDomainWithCFG[URL]])
    }

      propertyStore = analysesManager.runAll(
        IFDSAnalysisJSFixtureScheduler
      )._1
      propertyStore.waitOnPhaseCompletion();
    //Result:
    propertyStore.entities(Taint.key).mkString("\n")
  }
}