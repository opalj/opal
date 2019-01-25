/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

import java.io.File

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

import org.opalj.br.analyses.Project
import org.opalj.br.Annotation
import org.opalj.br.Method
import org.opalj.br.cfg.CFG
import org.opalj.br.Annotations
import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.tac.DefaultTACAIKey
import org.opalj.tac.Stmt
import org.opalj.tac.TACStmts
import org.opalj.tac.VirtualMethodCall
import org.opalj.tac.fpcf.analyses.string_analysis.LazyStringDefinitionAnalysis
import org.opalj.tac.fpcf.analyses.string_analysis.LocalStringDefinitionAnalysis
import org.opalj.tac.fpcf.analyses.string_analysis.V

/**
 * Tests whether the StringTrackingAnalysis works correctly.
 *
 * @author Patrick Mell
 */
class LocalStringDefinitionTest extends PropertiesTest {

    /**
     * @return Returns all relevant project files (NOT including library files) to run the tests.
     */
    private def getRelevantProjectFiles: Array[File] = {
        val necessaryFiles = Array(
            "fixtures/string_definition/TestMethods.class",
            "properties/string_definition/StringDefinitions.class"
        )
        val basePath = System.getProperty("user.dir")+
            "/DEVELOPING_OPAL/validate/target/scala-2.12/test-classes/org/opalj/fpcf/"

        necessaryFiles.map { filePath ⇒ new File(basePath + filePath) }
    }

    /**
     * Extracts [[org.opalj.tac.UVar]]s from a set of statements. The locations of the UVars are
     * identified by the argument to the very first call to TestMethods#analyzeString.
     *
     * @param cfg The control flow graph from which to extract the UVar, usually derived from the
     *            method that contains the call(s) to TestMethods#analyzeString.
     * @return Returns the arguments of the TestMethods#analyzeString as a DUVars list in the order
     *         in which they occurred in the given statements.
     */
    private def extractUVars(cfg: CFG[Stmt[V], TACStmts[V]]): List[V] = {
        cfg.code.instructions.filter {
            case VirtualMethodCall(_, declClass, _, name, _, _, _) ⇒
                declClass.toJavaClass.getName == LocalStringDefinitionTest.fqTestMethodsClass &&
                    name == LocalStringDefinitionTest.nameTestMethod
            case _ ⇒ false
        }.map(_.asVirtualMethodCall.params.head.asVar).toList
    }

    /**
     * Takes an annotation and checks if it is a
     * [[org.opalj.fpcf.properties.string_definition.StringDefinitions]] annotation.
     *
     * @param a The annotation to check.
     * @return True if the `a` is of type StringDefinitions and false otherwise.
     */
    private def isStringUsageAnnotation(a: Annotation): Boolean =
        a.annotationType.toJavaClass.getName == LocalStringDefinitionTest.fqStringDefAnnotation

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
    private def getStringDefinitionsFromCollection(a: Annotations, index: Int): Annotation =
        a.head.elementValuePairs(1).value.asArrayValue.values(index).asAnnotationValue.annotation

    describe("the org.opalj.fpcf.StringTrackingAnalysis is started") {
        val p = Project(getRelevantProjectFiles, Array[File]())

        val manager = p.get(FPCFAnalysesManagerKey)
        val (ps, _) = manager.runAll(LazyStringDefinitionAnalysis)
        val testContext = TestContext(p, ps, List(new LocalStringDefinitionAnalysis(p)))

        LazyStringDefinitionAnalysis.init(p, ps)
        LazyStringDefinitionAnalysis.schedule(ps, null)
        val tacProvider = p.get(DefaultTACAIKey)

        // We need a "method to entity" matching for the evaluation (see further below)
        val m2e = mutable.HashMap[Method, Entity]()

        p.allMethodsWithBody.filter {
            _.runtimeInvisibleAnnotations.foldLeft(false)(
                (exists, a) ⇒ exists || isStringUsageAnnotation(a)
            )
        } foreach { m ⇒
            extractUVars(tacProvider(m).cfg).foreach { uvar ⇒
                if (!m2e.contains(m)) {
                    m2e += m → ListBuffer(uvar)
                } else {
                    m2e(m).asInstanceOf[ListBuffer[V]].append(uvar)
                }
                ps.force((uvar, m), StringConstancyProperty.key)
            }
        }

        // As entity, we need not the method but a tuple (DUVar, Method), thus this transformation
        val eas = methodsWithAnnotations(p).filter(am ⇒ m2e.contains(am._1)).flatMap { am ⇒
            m2e(am._1).asInstanceOf[ListBuffer[V]].zipWithIndex.map {
                case (duvar, index) ⇒
                    Tuple3(
                        (duvar, am._1),
                        { s: String ⇒ s"${am._2(s)} (#$index)" },
                        List(getStringDefinitionsFromCollection(am._3, index))
                    )
            }
        }
        testContext.propertyStore.shutdown()
        validateProperties(testContext, eas, Set("StringConstancy"))
        ps.waitOnPhaseCompletion()
    }

}

object LocalStringDefinitionTest {

    val fqStringDefAnnotation =
        "org.opalj.fpcf.properties.string_definition.StringDefinitionsCollection"
    val fqTestMethodsClass = "org.opalj.fpcf.fixtures.string_definition.TestMethods"
    // The name of the method from which to extract DUVars to analyze
    val nameTestMethod = "analyzeString"

}
