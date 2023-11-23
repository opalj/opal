/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package xltest

import java.net.URL
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import org.opalj.xl.AllocationSiteBasedTriggeredTajsConnectorScheduler
import org.opalj.xl.javaanalyses.detector.scriptengine.AllocationSiteBasedScriptEngineDetectorScheduler
import org.opalj.fpcf.PropertiesTest
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.SomeEPS
import org.opalj.br.DefinedMethod
import org.opalj.br.analyses.Project
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.br.fpcf.properties.SimpleContext
import org.opalj.br.fpcf.properties.pointsto.{AllocationSitePointsToSet, PointsToSetLike}
import org.opalj.tac.cg.AllocationSiteBasedPointsToCallGraphKey
import org.opalj.tac.cg.TypeIteratorKey
import org.opalj.tac.common.DefinitionSite
import org.opalj.tac.fpcf.analyses.LazyTACAIProvider
import org.opalj.tac.fpcf.analyses.cg.AllocationSitesPointsToTypeIterator

/**
 * Tests XL interaction by validating Points-to-sets
 *
 * @author Julius Naeumann
 */
class XLJavaScriptTests extends PropertiesTest {

    override def withRT = false

    override def fixtureProjectPackage: List[String] = {
        List("org/opalj/fpcf/fixtures/xl/js/")
    }

    override def createConfig(): Config = ConfigFactory.load("reference.conf")

    override def init(p: Project[URL]): Unit = {

        p.updateProjectInformationKeyInitializationData(TypeIteratorKey) {
            case _ => () => new AllocationSitesPointsToTypeIterator(p)
        }
    }
    def addAnalyses(): Iterable[FPCFAnalysisScheduler] = {
        Iterable(
            AllocationSiteBasedScriptEngineDetectorScheduler,
            AllocationSiteBasedTriggeredTajsConnectorScheduler
        )
    }

    describe("test JavaScript XL points-to-sets") {
        val statistics =
            FixtureProject
                .statistics.map(kv => "- "+kv._1+": "+kv._2)
                .toList.sorted.reverse
                .mkString("project statistics:\n\t", "\n\t", "\n")
        info(statistics)
        var analyses: List[FPCFAnalysisScheduler] = List(LazyTACAIProvider)
        analyses ++= AllocationSiteBasedPointsToCallGraphKey.allCallGraphAnalyses(FixtureProject)
        analyses ++= addAnalyses()
        val as = executeAnalyses(
            analyses
        )

        as.propertyStore.shutdown()

        val filter: SomeEPS => Boolean = _ => true
        val allEntities = as.propertyStore.entities(propertyFilter = filter).toList

        val defSites = allEntities.filter(_.isInstanceOf[DefinitionSite]).map(_.asInstanceOf[DefinitionSite])
        for (ds <- defSites) {
            val epss = as.propertyStore.properties(ds).toIndexedSeq

            val properties = epss.map(_.toFinalEP.p)
            val contextP = properties.find(_.isInstanceOf[PointsToSetLike[_, _, _]]).
                map(_.asInstanceOf[PointsToSetLike[_, _, _]])

            println(contextP)
        }
        val definedMethods = allEntities.filter(_.isInstanceOf[DefinedMethod]).map(_.asInstanceOf[DefinedMethod])

        val allContexts = allEntities.filter(_.isInstanceOf[SimpleContext]).map(_.asInstanceOf[SimpleContext])
        println((allEntities, definedMethods, allContexts))

        for (c <- allContexts) {
            val epss = as.propertyStore.properties(c).toIndexedSeq

            val properties = epss.map(_.toFinalEP.p)
            val contextP = properties.find(_.isInstanceOf[PointsToSetLike[_, _, _]]).
                map(_.asInstanceOf[PointsToSetLike[_, _, _]])

            println(contextP)
        }
        val _ = as.project.allProjectClassFiles.flatMap(_.methods)
        implicit val ps: PropertyStore = as.propertyStore

        for (m <- definedMethods) {
            val dm = m.definedMethod
            val defsitesInMethod = ps.entities(propertyFilter = _.e.isInstanceOf[DefinitionSite]).map(_.asInstanceOf[DefinitionSite]).filter(_.method == dm).toSet
            println(defsitesInMethod)
            val pts = defsitesInMethod.map(defsite => (defsite, ps.properties(defsite).map(_.toFinalEP.p).find(_.isInstanceOf[AllocationSitePointsToSet]).map(_.asInstanceOf[AllocationSitePointsToSet])))
            println("points to sets: ")
            println(pts)
        }
        validateProperties(as, methodsWithAnnotations(as.project), Set("PointsToSetIncludes"))

    }
}
