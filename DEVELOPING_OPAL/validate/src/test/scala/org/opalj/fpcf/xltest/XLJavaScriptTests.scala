/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.xltest

import java.net.URL

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import org.opalj.xl.AllocationSiteBasedTriggeredTajsConnectorScheduler
import org.opalj.xl.javaanalyses.detector.scriptengine.AllocationSiteBasedScriptEngineDetectorScheduler

import org.opalj.fpcf.PropertiesTest
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.SomeEPS
import org.opalj.br.DefinedMethod
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.Project
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.br.fpcf.properties.SimpleContext
import org.opalj.br.fpcf.properties.SimpleContextsKey
import org.opalj.br.fpcf.properties.pointsto.PointsToSetLike
import org.opalj.tac.cg.AllocationSiteBasedPointsToCallGraphKey
import org.opalj.tac.cg.TypeIteratorKey
import org.opalj.tac.common.DefinitionSite
import org.opalj.tac.common.DefinitionSitesKey
import org.opalj.tac.fpcf.analyses.LazyTACAIProvider
import org.opalj.tac.fpcf.analyses.cg.AllocationSitesPointsToTypeIterator
import org.opalj.tac.fpcf.analyses.cg.TypeIterator
import org.opalj.tac.fpcf.analyses.cg.xta.TypePropagationAnalysisScheduler
import org.opalj.tac.fpcf.analyses.cg.xta.XTASetEntitySelector
import org.opalj.tac.fpcf.properties.TACAI
import org.opalj.tac.fpcf.properties.cg.Callees

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

    describe("test JavaScript XL points-to-sets") {
        val statistics =
            FixtureProject
                .statistics.map(kv => "- "+kv._1+": "+kv._2)
                .toList.sorted.reverse
                .mkString("project statistics:\n\t", "\n\t", "\n")
        info(statistics)
        var analyses: List[FPCFAnalysisScheduler] = List(LazyTACAIProvider)
        analyses ++= AllocationSiteBasedPointsToCallGraphKey.allCallGraphAnalyses(FixtureProject)
        analyses ++= Iterable(
            AllocationSiteBasedScriptEngineDetectorScheduler,
            AllocationSiteBasedTriggeredTajsConnectorScheduler,
            new TypePropagationAnalysisScheduler(XTASetEntitySelector)
        )
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
        val allocSites = as.propertyStore.properties(as.project.get(DefinitionSitesKey).getAllocationSites).toList
        val contexts = as.propertyStore.properties(as.project.get(SimpleContextsKey)).toList
        val methods = as.propertyStore.properties(as.project.allMethods).toList
        val fields = as.propertyStore.properties(as.project.allFields).toList
        val pts = allocSites.find(_.isInstanceOf[PointsToSetLike[_, _, _]])

        for (c <- allContexts) {
            val epss = as.propertyStore.properties(c).toIndexedSeq

            val properties = epss.map(_.toFinalEP.p)
            val contextP = properties.find(_.isInstanceOf[PointsToSetLike[_, _, _]]).
                map(_.asInstanceOf[PointsToSetLike[_, _, _]])

            println(contextP)
        }
        val _ = as.project.allProjectClassFiles.flatMap(_.methods)
        implicit val typeIterator: TypeIterator = as.project.get(TypeIteratorKey)
        implicit val ps: PropertyStore = as.propertyStore

        for (m <- definedMethods) {
            val epss = as.propertyStore.properties(m).toIndexedSeq

            val properties = epss.map(_.toFinalEP.p)
            val calleesP = properties.find(_.isInstanceOf[Callees]).map(_.asInstanceOf[Callees])
            val aitac = properties.find(_.isInstanceOf[TACAI])
            if (aitac.nonEmpty) {
                val tacs = aitac.get.asInstanceOf[TACAI]
                val props = tacs.tac.get.stmts.map(as.propertyStore.properties).map(_.toList)
                for (stmt <- tacs.tac.get.stmts) {
                    val p = as.propertyStore.properties(stmt)
                    println(p.toList)
                }
                println(props)
            }

            if (calleesP.nonEmpty) {
                val callees = calleesP.get

                val call = for {
                    callerContext <- callees.callerContexts
                    pc <- callees.callSitePCs(callerContext)
                    calleeContext <- callees.callees(callerContext, pc)
                } yield {
                    val callee = calleeContext.method
                    (
                        callee.declaringClassType.toJVMTypeName,
                        callee.name,
                        callee.descriptor
                    )
                }
                val resolvedCalls = call.toList
                println(resolvedCalls)
            }

            print(calleesP)
            println(aitac)

            val codeEps = m.definedMethod.body.map(as.propertyStore.properties).map(_.toIndexedSeq)
            println(codeEps)
        }
        as.project.get(DeclaredMethodsKey)
        println((pts, contexts, methods, fields))
        validateProperties(as, methodsWithAnnotations(as.project), Set("PointsToSetIncludes"))

    }
}
