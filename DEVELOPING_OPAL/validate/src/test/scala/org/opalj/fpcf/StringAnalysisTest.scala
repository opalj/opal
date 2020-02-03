/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

import java.io.File
import java.net.URL

import scala.collection.mutable.ListBuffer

import org.opalj.collection.immutable.Chain
import org.opalj.br.analyses.Project
import org.opalj.br.Annotation
import org.opalj.br.Method
import org.opalj.br.cfg.CFG
import org.opalj.br.Annotations
import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.tac.Stmt
import org.opalj.tac.TACStmts
import org.opalj.tac.VirtualMethodCall
import org.opalj.tac.fpcf.analyses.string_analysis.LazyInterproceduralStringAnalysis
import org.opalj.tac.fpcf.analyses.string_analysis.LazyIntraproceduralStringAnalysis
import org.opalj.tac.EagerDetachedTACAIKey
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.fpcf.analyses.string_analysis.V

/**
 * @param fqTestMethodsClass The fully-qualified name of the class that contains the test methods.
 * @param nameTestMethod The name of the method from which to extract DUVars to analyze.
 * @param filesToLoad Necessary (test) files / classes to load. Note that this list should not
 *                    include "StringDefinitions.class" as this class is loaded by default.
 */
sealed class StringAnalysisTestRunner(
        val fqTestMethodsClass: String,
        val nameTestMethod:     String,
        val filesToLoad:        List[String]
) extends PropertiesTest {

    /**
     * @return Returns all relevant project files (NOT including library files) to run the tests.
     */
    def getRelevantProjectFiles: Array[File] = {
        val necessaryFiles = Array(
            "properties/string_analysis/StringDefinitions.class"
        ) ++ filesToLoad
        val basePath = System.getProperty("user.dir")+
            "/DEVELOPING_OPAL/validate/target/scala-2.12/test-classes/org/opalj/fpcf/"

        necessaryFiles.map { filePath ⇒ new File(basePath + filePath) }
    }

    /**
     * Extracts a `StringDefinitions` annotation from a `StringDefinitionsCollection` annotation.
     * Make sure that you pass an instance of `StringDefinitionsCollection` and that the element at
     * the given index really exists. Otherwise an exception will be thrown.
     *
     * @param a The `StringDefinitionsCollection` to extract a `StringDefinitions` from.
     * @param index The index of the element from the `StringDefinitionsCollection` annotation to
     *              get.
     * @return Returns the desired `StringDefinitions` annotation.
     */
    def getStringDefinitionsFromCollection(a: Annotations, index: Int): Annotation =
        a.head.elementValuePairs(1).value.asArrayValue.values(index).asAnnotationValue.annotation

    def determineEntitiesToAnalyze(
        project: Project[URL]
    ): Iterable[(V, Method)] = {
        val entitiesToAnalyze = ListBuffer[(V, Method)]()
        val tacProvider = project.get(EagerDetachedTACAIKey)
        project.allMethodsWithBody.filter {
            _.runtimeInvisibleAnnotations.foldLeft(false)(
                (exists, a) ⇒ exists || StringAnalysisTestRunner.isStringUsageAnnotation(a)
            )
        } foreach { m ⇒
            StringAnalysisTestRunner.extractUVars(
                tacProvider(m).cfg, fqTestMethodsClass, nameTestMethod
            ).foreach { uvar ⇒
                entitiesToAnalyze.append((uvar, m))
            }
        }
        entitiesToAnalyze
    }

    def determineEAS(
        entities: Iterable[(V, Method)],
        project:  Project[URL]
    ): Traversable[((V, Method), String ⇒ String, List[Annotation])] = {
        val m2e = entities.groupBy(_._2).iterator.map(e ⇒ e._1 → e._2.map(k ⇒ k._1)).toMap
        // As entity, we need not the method but a tuple (DUVar, Method), thus this transformation

        val eas = methodsWithAnnotations(project).filter(am ⇒ m2e.contains(am._1)).flatMap { am ⇒
            m2e(am._1).zipWithIndex.map {
                case (duvar, index) ⇒
                    Tuple3(
                        (duvar, am._1),
                        { s: String ⇒ s"${am._2(s)} (#$index)" },
                        List(getStringDefinitionsFromCollection(am._3, index))
                    )
            }
        }

        eas
    }
}

object StringAnalysisTestRunner {

    private val fqStringDefAnnotation =
        "org.opalj.fpcf.properties.string_analysis.StringDefinitionsCollection"

    /**
     * Takes an annotation and checks if it is a
     * [[org.opalj.fpcf.properties.string_analysis.StringDefinitions]] annotation.
     *
     * @param a The annotation to check.
     * @return True if the `a` is of type StringDefinitions and false otherwise.
     */
    def isStringUsageAnnotation(a: Annotation): Boolean =
        a.annotationType.toJavaClass.getName == fqStringDefAnnotation

    /**
     * Extracts [[org.opalj.tac.UVar]]s from a set of statements. The locations of the UVars are
     * identified by the argument to the very first call to LocalTestMethods#analyzeString.
     *
     * @param cfg The control flow graph from which to extract the UVar, usually derived from the
     *            method that contains the call(s) to LocalTestMethods#analyzeString.
     * @return Returns the arguments of the LocalTestMethods#analyzeString as a DUVars list in the
     *         order in which they occurred in the given statements.
     */
    def extractUVars(
        cfg:                CFG[Stmt[V], TACStmts[V]],
        fqTestMethodsClass: String,
        nameTestMethod:     String
    ): List[V] = {
        cfg.code.instructions.filter {
            case VirtualMethodCall(_, declClass, _, name, _, _, _) ⇒
                declClass.toJavaClass.getName == fqTestMethodsClass && name == nameTestMethod
            case _ ⇒ false
        }.map(_.asVirtualMethodCall.params.head.asVar).toList
    }

}

/**
 * Tests whether the [[IntraproceduralStringAnalysis]] works correctly with respect to some
 * well-defined tests.
 *
 * @author Patrick Mell
 */
class IntraproceduralStringAnalysisTest extends PropertiesTest {

    describe("the org.opalj.fpcf.LocalStringAnalysis is started") {
        val runner = new StringAnalysisTestRunner(
            IntraproceduralStringAnalysisTest.fqTestMethodsClass,
            IntraproceduralStringAnalysisTest.nameTestMethod,
            IntraproceduralStringAnalysisTest.filesToLoad
        )
        val p = Project(runner.getRelevantProjectFiles, Array[File]())

        val manager = p.get(FPCFAnalysesManagerKey)
        val ps = p.get(PropertyStoreKey)
        val entities = runner.determineEntitiesToAnalyze(p)
        val (_, analyses) = manager.runAll(
            List(LazyIntraproceduralStringAnalysis),
            { _: Chain[ComputationSpecification[FPCFAnalysis]] ⇒
                entities.foreach(ps.force(_, StringConstancyProperty.key))
            }
        )

        val testContext = TestContext(p, ps, analyses.map(_._2))

        val eas = runner.determineEAS(entities, p)

        ps.shutdown()
        validateProperties(testContext, eas, Set("StringConstancy"))
        ps.waitOnPhaseCompletion()
    }

}

object IntraproceduralStringAnalysisTest {

    val fqTestMethodsClass = "org.opalj.fpcf.fixtures.string_analysis.LocalTestMethods"
    // The name of the method from which to extract DUVars to analyze
    val nameTestMethod = "analyzeString"
    // Files to load for the runner
    val filesToLoad = List(
        "fixtures/string_analysis/LocalTestMethods.class"
    )

}

/**
 * Tests whether the InterproceduralStringAnalysis works correctly with respect to some
 * well-defined tests.
 *
 * @author Patrick Mell
 */
class InterproceduralStringAnalysisTest extends PropertiesTest {

    describe("the org.opalj.fpcf.InterproceduralStringAnalysis is started") {
        val runner = new StringAnalysisTestRunner(
            InterproceduralStringAnalysisTest.fqTestMethodsClass,
            InterproceduralStringAnalysisTest.nameTestMethod,
            InterproceduralStringAnalysisTest.filesToLoad
        )
        val p = Project(runner.getRelevantProjectFiles, Array[File]())

        val entities = runner.determineEntitiesToAnalyze(p)

        p.get(RTACallGraphKey)
        val ps = p.get(PropertyStoreKey)
        val manager = p.get(FPCFAnalysesManagerKey)
        val analysesToRun = Set(
            LazyInterproceduralStringAnalysis
        )

        val (_, analyses) = manager.runAll(
            analysesToRun,
            { _: Chain[ComputationSpecification[FPCFAnalysis]] ⇒
                entities.foreach(ps.force(_, StringConstancyProperty.key))
            }
        )

        val testContext = TestContext(p, ps, analyses.map(_._2))
        val eas = runner.determineEAS(entities, p)

        ps.waitOnPhaseCompletion()
        ps.shutdown()

        validateProperties(testContext, eas, Set("StringConstancy"))
    }

}

object InterproceduralStringAnalysisTest {

    val fqTestMethodsClass = "org.opalj.fpcf.fixtures.string_analysis.InterproceduralTestMethods"
    // The name of the method from which to extract DUVars to analyze
    val nameTestMethod = "analyzeString"
    // Files to load for the runner
    val filesToLoad = List(
        "fixtures/string_analysis/InterproceduralTestMethods.class",
        "fixtures/string_analysis/StringProvider.class",
        "fixtures/string_analysis/hierarchies/GreetingService.class",
        "fixtures/string_analysis/hierarchies/HelloGreeting.class",
        "fixtures/string_analysis/hierarchies/SimpleHelloGreeting.class"
    )

}
