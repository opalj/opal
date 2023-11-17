/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.analyses

import org.opalj.br.DefinedMethod
import org.opalj.br.analyses.{BasicReport, Project, ProjectAnalysisApplication}
import org.opalj.br.fpcf.properties.pointsto.{AllocationSitePointsToSet, PointsToSetLike}
import org.opalj.br.fpcf.{FPCFAnalysesManagerKey, FPCFAnalysisScheduler, PropertyStoreKey}
import org.opalj.fpcf.seq.PKESequentialPropertyStore
import org.opalj.fpcf.{PropertyStore, PropertyStoreContext, SomeEPS}
import org.opalj.log.LogContext
import org.opalj.tac.cg.AllocationSiteBasedPointsToCallGraphKey
import org.opalj.tac.common.DefinitionSite
import org.opalj.tac.fpcf.analyses.LazyTACAIProvider
import org.opalj.tac.fpcf.analyses.cg.xta.{TypePropagationAnalysisScheduler, XTASetEntitySelector}
import org.opalj.tac.fpcf.analyses.pointsto.AllocationSiteBasedPointsToAnalysisScheduler
import org.opalj.xl.AllocationSiteBasedTriggeredTajsConnectorScheduler
import org.opalj.xl.javaanalyses.detector.scriptengine.AllocationSiteBasedScriptEngineDetectorScheduler

import java.net.URL
import scala.collection.Iterable


object ComparePTS {
    def main(args: Array[String]): Unit = {
        val withoutTAJS = new PointsToAnalysisRunner()
        val withTAJS = new PointsToAnalysisRunner()
        withoutTAJS.main(args)
        withTAJS.main(args ++ Iterable("RunTAJS"))
        println(s"Points to set without TAJS: ${withoutTAJS.pts.size}")
        println(s"Points to set with TAJS: ${withTAJS.pts.size}")
        println(s"diff: ${withTAJS.pts.size - withoutTAJS.pts.size}")

    }
}
class PointsToAnalysisRunner extends ProjectAnalysisApplication {
    var pts = Set[(DefinedMethod, DefinitionSite, Option[AllocationSitePointsToSet])]()
    override def checkAnalysisSpecificParameters(parameters: Seq[String]): Iterable[String] = {
         Iterable()
    }


    override def doAnalyze(project: Project[URL], parameters: Seq[String], isInterrupted: () => Boolean): BasicReport = {
        val TAJSAnalyses = Iterable(
            AllocationSiteBasedScriptEngineDetectorScheduler,
            AllocationSiteBasedTriggeredTajsConnectorScheduler,
        )
        var analyses: List[FPCFAnalysisScheduler] = List(LazyTACAIProvider)

        analyses ++= AllocationSiteBasedPointsToCallGraphKey.allCallGraphAnalyses(project)
        analyses ::= new TypePropagationAnalysisScheduler(XTASetEntitySelector)
        analyses ::= AllocationSiteBasedPointsToAnalysisScheduler
        val runTAJS = parameters.contains("RunTAJS")
        if (runTAJS) analyses ++= TAJSAnalyses

        PropertyStore.updateDebug(true)
        implicit val logContext: LogContext = project.logContext
        project.getOrCreateProjectInformationKeyInitializationData(
            PropertyStoreKey,
            (context: List[PropertyStoreContext[AnyRef]]) => {
                val ps = PKESequentialPropertyStore(context: _*)
                ps
            }
        )


        val (ps1, _) = project.get(FPCFAnalysesManagerKey).runAll(analyses)

        val filter: SomeEPS => Boolean = _ => true
        val allEntities = ps1.entities(propertyFilter = filter).toList

        val defSites = allEntities.filter(_.isInstanceOf[DefinitionSite]).map(_.asInstanceOf[DefinitionSite])
        for (ds <- defSites) {
            val epss = ps1.properties(ds).toIndexedSeq

            val properties = epss.map(_.toFinalEP.p)
            val contextP = properties.find(_.isInstanceOf[PointsToSetLike[_, _, _]]).
                map(_.asInstanceOf[PointsToSetLike[_, _, _]])

            println(contextP)
        }
        val definedMethods = allEntities.filter(_.isInstanceOf[DefinedMethod]).map(_.asInstanceOf[DefinedMethod])

        var results = ""
        for (m <- definedMethods) {
            val dm = m.definedMethod
            val defsitesInMethod = ps1.entities(propertyFilter = _.e.isInstanceOf[DefinitionSite]).map(_.asInstanceOf[DefinitionSite]).filter(_.method == dm).toSet
            println(defsitesInMethod)
            val ptsInMethod = defsitesInMethod.toArray.map(defsite => (m, defsite, ps1.properties(defsite).map(_.toFinalEP.p).find(_.isInstanceOf[AllocationSitePointsToSet]).map(_.asInstanceOf[AllocationSitePointsToSet])))
            pts ++= ptsInMethod
            results += m.name+"\n"
            results += "points to sets: "+pts
        }

        BasicReport(
            results
        )
    }
}
