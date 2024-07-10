/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package xl

import java.net.URL

import org.opalj.fpcf.PropertyStoreContext
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.tac.fpcf.analyses.pointsto.AllocationSiteBasedLibraryPointsToAnalysisScheduler

//import com.typesafe.config.Config
//import com.typesafe.config.ConfigFactory
import org.opalj.xl.connector.svf.AllocationSiteBasedSVFConnectorDetectorScheduler

import org.opalj.fpcf.FinalEP
import org.opalj.br.fpcf.properties.pointsto.AllocationSitePointsToSet
import org.opalj.br.ReferenceType
import org.opalj.br.fpcf.properties.pointsto
import org.opalj.tac.common.DefinitionSite
import org.opalj.tac.common.DefinitionSitesKey
import org.opalj.tac.ComputeTACAIKey
//import org.opalj.xl.utility.AnalysisResult
//import org.opalj.xl.utility.InterimAnalysisResult

//import org.opalj.fpcf.FinalEP
//import org.opalj.fpcf.FinalP
//import org.opalj.br.fpcf.properties.pointsto.AllocationSitePointsToSet
//import org.opalj.br.ReferenceType
//import org.opalj.br.fpcf.properties.pointsto
//import org.opalj.tac.common.DefinitionSite
//import org.opalj.tac.common.DefinitionSitesKey
//import org.opalj.tac.ComputeTACAIKey
//import org.opalj.xl.utility.AnalysisResult
//import org.opalj.xl.utility.InterimAnalysisResult

import org.opalj.log.GlobalLogContext
import org.opalj.log.LogContext
//import org.opalj.fpcf.FinalEP
//import org.opalj.fpcf.FinalP
import org.opalj.value.ValueInformation
import org.opalj.br.analyses.AnalysisApplication
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.OneStepAnalysis
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ReportableAnalysisResult
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.br.fpcf.FPCFAnalysisScheduler
//import org.opalj.br.fpcf.properties.pointsto.AllocationSitePointsToSet
//import org.opalj.br.ReferenceType
//import org.opalj.br.fpcf.properties.pointsto
import org.opalj.tac.DUVar
import org.opalj.tac.cg.AllocationSiteBasedPointsToCallGraphKey
//import org.opalj.tac.cg.TypeIteratorKey
//import org.opalj.tac.common.DefinitionSite
//import org.opalj.tac.common.DefinitionSitesKey
import org.opalj.tac.fpcf.analyses.LazyTACAIProvider
import org.opalj.tac.fpcf.analyses.cg.AllocationSitesPointsToTypeIterator
//import org.opalj.tac.ComputeTACAIKey
import org.opalj.br.fpcf.ContextProviderKey

object Coordinator extends AnalysisApplication with OneStepAnalysis[URL, ReportableAnalysisResult] {

    type V = DUVar[ValueInformation]

    case class ScriptEngineInstance[T](element: T)

    implicit val logContext: LogContext = GlobalLogContext

    final override val analysis = this

    override def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () => Boolean
    ): BasicReport = {

        var analyses: List[FPCFAnalysisScheduler] = List(LazyTACAIProvider)

        project.updateProjectInformationKeyInitializationData(ContextProviderKey) {
            case _ => new AllocationSitesPointsToTypeIterator(project)
        }
        implicit val logContext: LogContext = project.logContext
        project.getOrCreateProjectInformationKeyInitializationData(
            PropertyStoreKey,
            (context: List[PropertyStoreContext[AnyRef]]) => {
                    org.opalj.fpcf.seq.PKESequentialPropertyStore(context: _*)
            }
        )

        analyses ++= AllocationSiteBasedPointsToCallGraphKey.allCallGraphAnalyses(project)
        analyses ++= Iterable(
            AllocationSiteBasedLibraryPointsToAnalysisScheduler,
            AllocationSiteBasedSVFConnectorDetectorScheduler
        )

        val (propertyStore, _) = project.get(FPCFAnalysesManagerKey).runAll(analyses)

        val tacaiKey = project.get(ComputeTACAIKey)

        val defSites = project.get(DefinitionSitesKey).definitionSites.keySet()
        println()
        //println("TAJS results:")
        /*  project.allProjectClassFiles
            .flatMap(_.methods)
            .foreach(method => {
                propertyStore(method, AnalysisResult.key) match {
                    case FinalP(InterimAnalysisResult(tajsStore)) =>
                        println(s"TAJS result: $tajsStore")
                    case x => println(s"other case: $x")
                }
            }) */
        println()
        println("OPAL results:")
        defSites.forEach(defSite => {
            propertyStore(defSite, AllocationSitePointsToSet.key) match {
                case FinalEP(DefinitionSite(method, pc), pointsToSet) =>
                    println(method)
                    try {
                        val taCode = tacaiKey(method)
                        val stmts = taCode.stmts
                        if (pointsToSet.elements.size > 0)
                            println(
                                s"""
                   | method ${defSite.method} pc ${defSite.pc}
                   | DefSite: ${stmts(taCode.pcToIndex(defSite.pc)).asAssignment.targetVar}
                   | PointsToSet (${pointsToSet.numElements}): ${
                                    pointsToSet.elements.iterator
                                        .map(long => ReferenceType.lookup(pointsto.allocationSiteLongToTypeId(long)))
                                        .mkString("\n")
                                }
                   | =================================== """.stripMargin
                            )
                    } catch {
                        case _: Throwable =>
                    }
                case x => println(x)
            }
        })
        BasicReport("")
    }
}
