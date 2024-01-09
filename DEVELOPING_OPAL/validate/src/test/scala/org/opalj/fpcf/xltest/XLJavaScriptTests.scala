/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package xltest

import java.net.URL
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
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
import org.scalatest.Reporter
import org.scalatest.events.{Event, SuiteCompleted, TestFailed, TestSucceeded}
import org.scalatest.tools.Runner

import scala.util.matching.Regex
import org.opalj.xl.connector.AllocationSiteBasedTriggeredTajsConnectorScheduler

object RunXLTests {
    def main(args: Array[String]): Unit = {
        //val test = new XLJavaScriptTests()
        println(Runner.run(Array("-C", "org.opalj.fpcf.xltest.MyCustomReporter", "-s", "org.opalj.fpcf.xltest.XLJavaScriptTests")))
    }
}

class MyCustomReporter extends Reporter {
    override def apply(event: Event): Unit = {
        // testName: "test JavaScript XL points-to-sets xl.js.controlflow.intraprocedural.unidirectional.arithmetic.Div{ public static void main(java.lang.String[]){ @PointsToSet } }"
        event match {
            case TestSucceeded(ordinal, suiteName, suiteId, suiteClassName, testName, testText, recordedEvents, duration, formatter, location, rerunner, payload, threadName, timeStamp) => {
                succeededClassnames += extractClassname(testName).get
            }
            case TestFailed(ordinal, message, suiteName, suiteId, suiteClassName, testName, testText, recordedEvents, analysis, throwable, duration, formatter, location, rerunner, payload, threadName, timeStamp) => {
                failedClassnames += extractClassname(testName).get
            }
            case SuiteCompleted(ordinal, suiteName, suiteId, suiteClassName, duration, formatter, location, rerunner, payload, threadName, timeStamp) => {
                printTestcaseSummary()
            }
            case _ =>
        }
    }
    def extractClassname(testname: String): Option[String] = {
        val regex = new Regex("test JavaScript XL points-to-sets ([a-zA-Z0-9\\._]*)\\{.+")
        regex.findFirstMatchIn(testname).map(_.group(1))
    }
    def extractCategory(testcase: String): Option[String] = {
        val parts = testcase.split('.')
        if (parts.length > 5) {
            Some(s"${parts(2)}: ${parts(4)}")
        } else {
            None
        }
    }

    var failedClassnames = Set[String]()
    var succeededClassnames = Set[String]()

    def printTestcaseSummary(): Unit = {
        val categoriesOrder = Map(
            "controlflow: unidirectional" -> 1,
            "controlflow: bidirectional" -> 2,
            "controlflow: cyclic" -> 3,
            "stateaccess: unidirectional" -> 4,
            "stateaccess: bidirectional" -> 5
        )
        val allTestcases = failedClassnames ++ succeededClassnames
        val failedGrouped = failedClassnames.filter(extractCategory(_).isDefined).groupBy(cl => extractCategory(cl).get)
        val succeededGrouped = succeededClassnames.filter(extractCategory(_).isDefined).groupBy(cl => extractCategory(cl).get)
        val sucdfaild = allTestcases.flatMap(extractCategory).map(cat => (cat -> (
            succeededGrouped.getOrElse(cat, Set.empty[String]), failedGrouped.getOrElse(cat, Set.empty[String])
        ))).toSeq
            .sortBy { case (category, _) => categoriesOrder.getOrElse(category, Int.MaxValue) }

        sucdfaild.foreach {
            case (category, (succeededTests, failedTests)) =>
                val failedCount = failedTests.size
                val succeededCount = succeededTests.size

                println(s"$category: $failedCount failed, $succeededCount succeeded")

                val failedString = if (failedTests.isEmpty) "-" else failedTests
                val succeededString = if (succeededTests.isEmpty) "-" else succeededTests

                println(s"failed:$failedString")
                println(s"succeeded:$succeededString")
        }

        println("\\begin{tabular}{lccc}")
        println("\\toprule")
        println("\\textbf{Pattern} & \\textbf{Succeeded Testcases}\\\\")
        println("\\midrule")
        sucdfaild.foreach {
            case (category, (succeededTests, failedTests)) =>
                val failedCount = failedTests.size
                val succeededCount = succeededTests.size
                // Assuming extractCategory returns strings like "controlflow: unidirectional"
                // we use this string as a label in the table. Adjust if necessary.
                println(s"$category & \\tnum{$succeededCount / ${succeededCount + failedCount}} \\\\")
        }
        println("\\end{tabular}")

    }

}

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
        // tally.
        //succeededTestcases.foreach(println)
        //failedTestcases.foreach(println)
        //printTestcaseSummary(failedTestcases, succeededTestcases)
    }

}
