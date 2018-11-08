/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

import java.io.File

import org.opalj.br.analyses.Project
import org.opalj.br.Annotation
import org.opalj.fpcf.analyses.cg.V
import org.opalj.fpcf.analyses.string_definition.LazyStringTrackingAnalysis
import org.opalj.fpcf.properties.StringConstancyProperty
import org.opalj.tac.DefaultTACAIKey
import org.opalj.tac.Stmt
import org.opalj.tac.VirtualMethodCall

/**
 * Tests whether the StringTrackingAnalysis works correctly.
 *
 * @author Patrick Mell
 */
class LocalStringDefinitionTest extends PropertiesTest {

    val fqStringDefAnnotation = "org.opalj.fpcf.properties.string_definition.StringDefinitions"
    val fqTestMethodsClass = "org.opalj.fpcf.fixtures.string_definition.TestMethods"
    // The name of the method from which to extract DUVars to analyze
    val nameTestMethod = "analyzeString"

    /**
     * Extracts a [[org.opalj.tac.UVar]] from a set of statements. The location of the UVar is
     * identified the argument to the very first call to TestMethods#analyzeString.
     *
     * @param stmts The statements from which to extract the UVar, usually the method that contains
     *              the call to TestMethods#analyzeString.
     * @return Returns the argument of the TestMethods#analyzeString as a DUVar. In case the
     *         expected analyze method is not present, None is returned.
     */
    private def extractUVar(stmts: Array[Stmt[V]]): Option[V] = {
        val relMethodCalls = stmts.filter {
            case VirtualMethodCall(_, declClass, _, name, _, _, _) ⇒
                declClass.toJavaClass.getName == fqTestMethodsClass && name == nameTestMethod
            case _ ⇒ false
        }

        if (relMethodCalls.isEmpty) {
            return None
        }

        Some(relMethodCalls.head.asVirtualMethodCall.params.head.asVar)
    }

    /**
     * Takes an annotation and checks if it is a
     * [[org.opalj.fpcf.properties.string_definition.StringDefinitions]] annotation.
     *
     * @param a The annotation to check.
     * @return True if the `a` is of type StringDefinitions and false otherwise.
     */
    private def isStringUsageAnnotation(a: Annotation): Boolean =
        // TODO: Is there a better way than string comparison?
        a.annotationType.toJavaClass.getName == fqStringDefAnnotation

    describe("the org.opalj.fpcf.StringTrackingAnalysis is executed") {
        val as = executeAnalyses(Set(LazyStringTrackingAnalysis))
        as.propertyStore.shutdown()
        validateProperties(as, fieldsWithAnnotations(as.project), Set("StringDefinitions"))
    }

    describe("the org.opalj.fpcf.StringTrackingAnalysis is started") {
        val cutPath = System.getProperty("user.dir")+
            "/DEVELOPING_OPAL/validate/target/scala-2.12/test-classes/org/opalj/fpcf/fixtures/string_definition/TestMethods.class"
        val p = Project(new File(cutPath))
        val ps = p.get(org.opalj.fpcf.PropertyStoreKey)
        ps.setupPhase(Set(StringConstancyProperty))

        LazyStringTrackingAnalysis.init(p, ps)
        LazyStringTrackingAnalysis.schedule(ps, null)
        val tacProvider = p.get(DefaultTACAIKey)

        p.allMethodsWithBody.filter {
            _.runtimeInvisibleAnnotations.foldLeft(false)(
                (exists, a) ⇒ exists || isStringUsageAnnotation(a)
            )
        } foreach { m ⇒
            extractUVar(tacProvider(m).stmts) match {
                case Some(uvar) ⇒
                    ps.force(Tuple2(uvar, m), StringConstancyProperty.key)
                    ps.waitOnPhaseCompletion()
                case _ ⇒
            }
        }
    }

}
