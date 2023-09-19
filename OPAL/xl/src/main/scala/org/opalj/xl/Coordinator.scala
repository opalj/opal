/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.xl

import scala.reflect.runtime.universe.runtimeMirror

import java.net.URL

import scala.jdk.CollectionConverters.CollectionHasAsScala

import org.opalj.xl.connector.TriggeredTajsConnectorScheduler
import org.opalj.xl.javaanalyses.detector.AllocationSiteBasedApiScriptEngineDetectorScheduler

import org.opalj.log.GlobalLogContext
import org.opalj.log.LogContext
import org.opalj.log.OPALLogger.error
import org.opalj.value.ValueInformation
import org.opalj.br.analyses.AnalysisApplication
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.OneStepAnalysis
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ReportableAnalysisResult
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.tac.DUVar
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.cg.TypeIteratorKey
import org.opalj.tac.fpcf.analyses.LazyTACAIProvider
import org.opalj.tac.fpcf.analyses.cg.CallGraphAnalysisScheduler
import org.opalj.tac.fpcf.analyses.cg.RTATypeIterator
import org.opalj.tac.fpcf.analyses.pointsto.AllocationSiteBasedPointsToAnalysisScheduler

object Coordinator extends AnalysisApplication with OneStepAnalysis[URL, ReportableAnalysisResult] {

    type V = DUVar[ValueInformation]

    case class ScriptEngineInstance[T](element: T)

    implicit val logContext: LogContext = GlobalLogContext

    final override val analysis = this

    override def doAnalyze(project: Project[URL], parameters: Seq[String], isInterrupted: () => Boolean): BasicReport = {

        var analyses: List[FPCFAnalysisScheduler] = List(LazyTACAIProvider)

        def resolveAnalysisRunner(className: String)(implicit logContext: LogContext): Option[FPCFAnalysisScheduler] = {
            val mirror = runtimeMirror(getClass.getClassLoader)
            try {
                val module = mirror.staticModule(className)
                import mirror.reflectModule
                Some(reflectModule(module).instance.asInstanceOf[FPCFAnalysisScheduler])
            } catch {
                case sre: ScalaReflectionException =>
                    error("call graph", s"cannot find analysis scheduler $className", sre)
                    None
                case cce: ClassCastException =>
                    error("call graph", "analysis scheduler class is invalid", cce)
                    None
            }
        }

        def registeredAnalyses(project: SomeProject): scala.collection.Seq[FPCFAnalysisScheduler] = {
            implicit val logContext: LogContext = project.logContext
            val config = project.config
            // TODO use FPCFAnaylsesRegistry here
            config.getStringList("org.opalj.tac.cg.CallGraphKey.modules").asScala.flatMap(resolveAnalysisRunner(_)).toSeq
        }

        project.updateProjectInformationKeyInitializationData(TypeIteratorKey) {
            case _ => () => new RTATypeIterator(project)
        }

        analyses ::= CallGraphAnalysisScheduler
        //analyses ++= AllocationSiteBasedPointsToCallGraphKey. RTACallGraphKey.callGraphSchedulers(project)
        analyses ++= RTACallGraphKey.callGraphSchedulers(project)
        analyses ++= registeredAnalyses(project)
        analyses ++= Iterable(
            /*  AllocationSiteBasedPointsToAnalysisScheduler,
            AllocationSiteBasedConfiguredMethodsPointsToAnalysisScheduler,
            AllocationSiteBasedTamiFlexPointsToAnalysisScheduler,
            AllocationSiteBasedArraycopyPointsToAnalysisScheduler,
            AllocationSiteBasedUnsafePointsToAnalysisScheduler,
            ReflectionAllocationsAnalysisScheduler,
            AllocationSiteBasedNewInstanceAnalysisScheduler, */
            AllocationSiteBasedPointsToAnalysisScheduler,
            AllocationSiteBasedApiScriptEngineDetectorScheduler,
            TriggeredTajsConnectorScheduler
        //  TriggeredTajsCallerScheduler
        //   AllocationSiteOpalConnectorScheduler
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
