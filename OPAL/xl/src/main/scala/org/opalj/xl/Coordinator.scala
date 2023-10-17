/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.xl

import java.net.URL
import org.opalj.xl.javaanalyses.detector.scriptengine.AllocationSiteBasedApiScriptEngineDetectorScheduler

import org.opalj.log.GlobalLogContext
import org.opalj.log.LogContext
import org.opalj.value.ValueInformation
import org.opalj.br.analyses.AnalysisApplication
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.OneStepAnalysis
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ReportableAnalysisResult
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.tac.DUVar
import org.opalj.tac.cg.AllocationSiteBasedPointsToCallGraphKey
import org.opalj.tac.cg.TypeIteratorKey
import org.opalj.tac.fpcf.analyses.LazyTACAIProvider
import org.opalj.tac.fpcf.analyses.cg.AllocationSitesPointsToTypeIterator

object Coordinator extends AnalysisApplication with OneStepAnalysis[URL, ReportableAnalysisResult] {

    type V = DUVar[ValueInformation]

    case class ScriptEngineInstance[T](element: T)

    implicit val logContext: LogContext = GlobalLogContext

    final override val analysis = this

    override def doAnalyze(project: Project[URL], parameters: Seq[String], isInterrupted: () => Boolean): BasicReport = {

        var analyses: List[FPCFAnalysisScheduler] = List(LazyTACAIProvider)

        project.updateProjectInformationKeyInitializationData(TypeIteratorKey) {
            case _ => () => new AllocationSitesPointsToTypeIterator(project)
        }

        analyses ++= AllocationSiteBasedPointsToCallGraphKey.allCallGraphAnalyses(project)
        analyses ++= Iterable(
            AllocationSiteBasedApiScriptEngineDetectorScheduler,
            AllocationSiteBasedTriggeredTajsConnectorScheduler
        )

        val (_ /*propertyStore*/ , _) = project.get(FPCFAnalysesManagerKey).runAll(analyses)
        /*
        val defSites = project.get(DefinitionSitesKey).definitionSites.keySet()

        project.allProjectClassFiles.flatMap(_.methods).foreach(method => {
            propertyStore(method, AnalysisResult.key) match {
                case FinalP(FinalAnalysisResult(s)) =>
                    println(s"TAJS result: $s")
                case _ =>
            }
        }) */

        /*defSites.forEach(defSite => {
            propertyStore(defSite, AllocationSitePointsToSet.key) match {
                case FinalEP(DefinitionSite(method, pc), pointsToSet) =>
                    if (method.name.startsWith("main") && pointsToSet.elements.size > 1)
                        println(s"""
                        | ===================================
                        | OPAL results:
                        | method ${defSite.method} pc ${defSite.pc}
                        | ${
                            pointsToSet.elements.iterator
                                .map(long => ReferenceType.lookup(pointsto.allocationSiteLongToTypeId(long)))
                                .mkString("\n")
                        }
                        | ===================================
                        |""".stripMargin)
                case _ =>
            }
        }) */
        BasicReport("")
    }
}
