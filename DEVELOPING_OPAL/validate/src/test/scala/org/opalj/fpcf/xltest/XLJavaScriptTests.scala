/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package xltest

import java.net.URL

import scala.util.matching.Regex

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

import org.opalj.br.DefinedMethod
import org.opalj.br.analyses.Project
import org.opalj.br.fpcf.properties.SimpleContext
import org.opalj.br.fpcf.properties.pointsto.{AllocationSitePointsToSet, PointsToSetLike}
import org.opalj.br.fpcf.{ContextProviderKey, FPCFAnalysisScheduler}
import org.opalj.fpcf.{PropertiesTest, PropertyStore, SomeEPS}
import org.opalj.tac.cg.AllocationSiteBasedPointsToCallGraphKey
import org.opalj.tac.common.DefinitionSite
import org.opalj.tac.fpcf.analyses.LazyTACAIProvider
import org.opalj.tac.fpcf.analyses.cg.AllocationSitesPointsToTypeIterator
import org.opalj.xl.connector.AllocationSiteBasedTriggeredTajsConnectorScheduler
import org.opalj.xl.javaanalyses.detector.scriptengine.AllocationSiteBasedScriptEngineDetectorScheduler
import org.scalatest.Reporter
import org.scalatest.events.Event
import org.scalatest.events.SuiteCompleted
import org.scalatest.events.TestFailed
import org.scalatest.events.TestSucceeded
import org.scalatest.tools.Runner
import org.opalj.log.LogContext
import org.opalj.br.fpcf.PropertyStoreKey

object RunXLTests {
    def main(args: Array[String]): Unit = {
        //val test = new XLJavaScriptTests()
        Runner.run(Array("-C", "org.opalj.fpcf.xltest.MyCustomReporter", "-s", "org.opalj.fpcf.xltest.XLJavaScriptTests"))
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
        val regex = new Regex("test .* points-to-sets ([a-zA-Z0-9\\._]*)\\{.+")
        regex.findFirstMatchIn(testname).map(_.group(1))
    }
    def extractCategory(testcase: String): Option[String] = {
        val parts = testcase.split('.')
        if (parts.length > 5) {
            Some(s"${parts(2)}${parts(4)}")
        } else {
            None
        }
    }

    var failedClassnames = Set[String]()
    var succeededClassnames = Set[String]()

    def printTestcaseSummary(): Unit = {

        val allTestcases = failedClassnames ++ succeededClassnames
        val failedGrouped = failedClassnames.filter(extractCategory(_).isDefined).groupBy(cl => extractCategory(cl).get)
        val succeededGrouped = succeededClassnames.filter(extractCategory(_).isDefined).groupBy(cl => extractCategory(cl).get)
        val sucdfaild = allTestcases.flatMap(extractCategory).map(cat => (cat -> (
            succeededGrouped.getOrElse(cat, Set.empty[String]), failedGrouped.getOrElse(cat, Set.empty[String])
        ))).toSeq
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
        var total = 0
        var totalsucceeded = 0
        sucdfaild.foreach {
            case (category, (succeededTests, failedTests)) =>
                val failedCount = failedTests.size
                val succeededCount = succeededTests.size
                total += succeededCount + failedCount
                totalsucceeded += succeededCount
                println(s"\\newcommand{\\$category}{\\tnum{$succeededCount / ${succeededCount + failedCount}}}")
                for (succeeded <- succeededTests) {
                    println(s"%succeeded: $succeeded")
                }
                for (failed <- failedTests) {
                    println(s"%failed: $failed")
                }
        }
        println(s"\\newcommand{\\overalltests}{\\tnum{$totalsucceeded}}")
        println(s"\\newcommand{\\testcasecount}{\\tnum{$total}}")
        // Mapping of original labels to new labels
        val labelMap = Map(
            "controlflowunidirectional" -> "Unidirectional Execution",
            "controlflowinterleaved" -> "Interleaved Execution",
            "controlflowcyclic" -> "Mutual Recursion",
            "stateaccessunidirectional" -> "Unidirectional State Access",
            "stateaccessbidirectional" -> "Bidirectional State Access"
        )

        // Desired order using original labels
        val desiredOrder = List(
            "controlflowunidirectional",
            "controlflowinterleaved",
            "controlflowcyclic",
            "stateaccessunidirectional",
            "stateaccessbidirectional"
        )

        // Prepare table data with new labels and order
        val categoryData = desiredOrder.flatMap { originalLabel =>
            sucdfaild.find(_._1 == originalLabel).map {
                case (_, (succeededTests, failedTests)) =>
                    val succeededCount = succeededTests.size
                    val totalCount = succeededCount + failedTests.size
                    val newLabel = labelMap.getOrElse(originalLabel, originalLabel)
                    List(newLabel, s"$succeededCount / $totalCount")
            }
        }

        val tableData = List(
            List("Category", "Passed / Total"),
            List.empty[String]  // Empty line for special header separator
        ) ++ categoryData ++ List(
            List.empty[String],  // Empty line for special overall separator
            List("Overall", s"$totalsucceeded / $total")
        )

        printAsciiTable(tableData)
    }

    def printAsciiTable(data: List[List[String]]): Unit = {
        if (data.isEmpty || data.head.isEmpty) return

        val colWidths = data.filter(_.nonEmpty).transpose.map(col => col.map(_.length).max)

        def horizontalLine(char: Char) =
            "+" + colWidths.map(w => char.toString * (w + 2)).mkString("+") + "+"

        def createRow(row: List[String]): String =
            if (row.isEmpty) horizontalLine('-')
            else "|" + row.zip(colWidths).map { case (cell, width) =>
                s" ${cell.padTo(width, ' ')} "
            }.mkString("|") + "|"

        println(horizontalLine('-'))
        data.foreach(row => println(createRow(row)))
        println(horizontalLine('-'))
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
        //List("org/opalj/fpcf/fixtures/xl/js/stateaccess/intraprocedural/unidirectional/JSAccessJava/")
    }

    override def createConfig(): Config = ConfigFactory.load("reference.conf")

    override def init(p: Project[URL]): Unit = {
        p.updateProjectInformationKeyInitializationData(ContextProviderKey)(_ => new AllocationSitesPointsToTypeIterator(p))

        implicit val logContext: LogContext = p.logContext
        p.getOrCreateProjectInformationKeyInitializationData(
            PropertyStoreKey,
            (context: List[PropertyStoreContext[AnyRef]]) => {
                // Some integrated analyses cannot cope with multiple threads
                org.opalj.fpcf.par.PKECPropertyStore.MaxThreads = 1
                org.opalj.fpcf.par.PKECPropertyStore(context: _*)
            }

        )
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
        validateProperties(as, methodsWithAnnotations(as.project), Set("PointsToSetIncludes", "TAJSEnvironment"))
        // tally.
        //succeededTestcases.foreach(println)
        //failedTestcases.foreach(println)
        //printTestcaseSummary(failedTestcases, succeededTestcases)
    }

}
