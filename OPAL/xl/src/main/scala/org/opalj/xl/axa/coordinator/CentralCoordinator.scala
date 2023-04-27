/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package xl
package axa
package coordinator

import dk.brics.tajs.analysis.nativeobjects.ECMAScriptObjects
import dk.brics.tajs.lattice.ObjectLabel
import org.opalj.br.ReferenceType
import org.opalj.br.analyses.AnalysisApplication
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.OneStepAnalysis
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ReportableAnalysisResult
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.br.fpcf.properties.pointsto
import org.opalj.br.fpcf.properties.pointsto.AllocationSitePointsToSet
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.FinalP
import org.opalj.log.GlobalLogContext
import org.opalj.log.LogContext
import org.opalj.log.OPALLogger.error
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.cg.TypeIteratorKey
import org.opalj.tac.common.DefinitionSite
import org.opalj.tac.common.DefinitionSitesKey
import org.opalj.tac.fpcf.analyses.LazyTACAIProvider
import org.opalj.tac.fpcf.analyses.cg.CallGraphAnalysisScheduler
import org.opalj.tac.fpcf.analyses.cg.RTATypeIterator
import org.opalj.tac.fpcf.analyses.pointsto.AllocationSiteBasedPointsToAnalysisScheduler
import org.opalj.xl.analyses.javaAnalysis.detector.EagerJavaScriptEngineDetector
import org.opalj.xl.axa.common.AnalysisResults
import org.opalj.xl.axa.common.FinalAnalysisResult
import org.opalj.xl.axa.connector.tajs.TriggeredOpalTajsConnectorScheduler
import org.opalj.xl.axa.translator.tajs.TriggeredTajsTranslatorScheduler

import java.net.URL
import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.reflect.runtime.universe.runtimeMirror

object CentralCoordinator extends AnalysisApplication with OneStepAnalysis[URL, ReportableAnalysisResult] {

    implicit val logContext: LogContext = GlobalLogContext

    final override val analysis = this


    override def doAnalyze(project: Project[URL], parameters: Seq[String], isInterrupted: () => Boolean): BasicReport = {

        var analyses: List[FPCFAnalysisScheduler] = List(LazyTACAIProvider)

        def resolveAnalysisRunner(
            className: String
        )(implicit logContext: LogContext): Option[FPCFAnalysisScheduler] = {
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
            config.getStringList(
                "org.opalj.tac.cg.CallGraphKey.modules"
            ).asScala.flatMap(resolveAnalysisRunner(_)).toSeq
        }

        project.updateProjectInformationKeyInitializationData(TypeIteratorKey) {
            case _ =>
                () => new RTATypeIterator(project)
        }

        analyses ::= CallGraphAnalysisScheduler
        analyses ++= RTACallGraphKey.callGraphSchedulers(project)
        analyses ++= registeredAnalyses(project)
        analyses ++= Iterable(
          EagerJavaScriptEngineDetector,
          TriggeredTajsTranslatorScheduler,
          AllocationSiteBasedPointsToAnalysisScheduler,
          TriggeredOpalTajsConnectorScheduler
        )

        val (propertyStore, _) = project.get(FPCFAnalysesManagerKey).runAll(analyses)

        val defSites = project.get(DefinitionSitesKey).definitionSites.keySet()

        project.allProjectClassFiles.flatMap(_.methods).foreach(method => {
           /* propertyStore(method, DetectorLattice.key) match {

              case clc:CrossLanguageCall => println(clc)
              case x => println(x)
            }
            propertyStore(method, TajsInquiries.key) match {
              case jsc:JavaScriptCall => println(jsc)
              case x => println(x)
            } */

            propertyStore(method, AnalysisResults.key) match {
                case FinalP(FinalAnalysisResult(s)) =>
                    val solver = s.asInstanceOf[dk.brics.tajs.analysis.Solver]
                    val mainFunction = solver.getFlowGraph.getMain
                    val exitBB = mainFunction.getOrdinaryExit
                    val states = solver.getAnalysisLatticeElement.getStates(exitBB)

                    val globalObject = ObjectLabel.make(ECMAScriptObjects.GLOBAL, ObjectLabel.Kind.OBJECT)

                    states.values().forEach(state =>
                      state.getStore.get(globalObject).getModified.keySet().forEach(key => {println(key+": "+state.getStore.get(globalObject).getModified.get(key))}))
                case _ =>
            }
        })

      defSites.forEach(defSite=>{
        propertyStore(defSite, AllocationSitePointsToSet.key) match {
        case FinalEP(DefinitionSite(method, pc), pointsToSet) =>
          if(method.name.startsWith("main") && pointsToSet.elements.size>1)
          println(s""" | ===================================
                       | method ${defSite.method} pc ${defSite.pc}
                       | ${pointsToSet.elements.iterator
                       .map(long => ReferenceType.lookup(pointsto.allocationSiteLongToTypeId(long)))
                       .mkString("\n")}
                       | ===================================
                       |""".stripMargin)
        case _ =>
        }
      }
      )

   //   javaResults.foreach(println(_))
        BasicReport("")
    }
}
