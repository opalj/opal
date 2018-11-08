/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

import java.io.File

import org.opalj.br.analyses.Project
import org.opalj.br.Annotation
import org.opalj.collection.immutable.RefArray
import org.opalj.fpcf.analyses.cg.V
import org.opalj.fpcf.analyses.string_definition.LazyStringTrackingAnalysis
import org.opalj.fpcf.properties.StringConstancyProperty
import org.opalj.tac.DefaultTACAIKey
import org.opalj.tac.DUVar
import org.opalj.tac.ExprStmt
import org.opalj.tac.StaticFunctionCall
import org.opalj.tac.Stmt
import org.opalj.value.KnownTypedValue

/**
 * Tests whether the StringTrackingAnalysis works correctly.
 *
 * @author Patrick Mell
 */
class LocalStringDefinitionTest extends PropertiesTest {

    val stringUsageAnnotationName = "org.opalj.fpcf.properties.string_tracking.StringDefinitions"

    /**
     * Extracts a [[org.opalj.tac.UVar]] from a set of statements. The location of the UVar is
     * identified by a program counter.
     *
     * @note If the desired statement contains more than one UVar, only the very first is returned.
     *
     * @param stmts The statements from which to extract the UVar. The statement is to be expected
     *              to be an [[ExprStmt]].
     * @return Returns the element from the statement that is identified by `pc`. In case the
     *         statement identified by pc is not present or the statement does not contain a UVar,
     *         `None` is returned.
     */
    private def extractUVar(stmts: Array[Stmt[V]], pc: UShort): Option[V] = {
        val stmt = stmts.filter(_.pc == pc)
        if (stmt.isEmpty) {
            return None
        }

        // TODO: What is a more generic / better way than nesting so deep?
        stmt.head match {
            case ExprStmt(_, expr) ⇒
                expr match {
                    case StaticFunctionCall(_, _, _, _, _, params) ⇒
                        val vars = params.filter(_.isInstanceOf[DUVar[KnownTypedValue]])
                        if (vars.isEmpty) {
                            None
                        }
                        Option(vars.head.asVar)
                    case _ ⇒ None
                }
            case _ ⇒ None
        }
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
        a.annotationType.toJavaClass.getName == stringUsageAnnotationName

    /**
     * Extracts the program counter from a
     * [[org.opalj.fpcf.properties.string_definition.StringDefinitions]] if present.
     *
     * @param annotations A set of annotations which is to be scanned. The only annotation that is
     *                    processed is
     *                    [[org.opalj.fpcf.properties.string_definition.StringDefinitions]].
     * @return Returns the `pc` value from the StringDefinitions if present. Otherwise `None` is
     *         returned.
     */
    private def pcFromAnnotations(annotations: RefArray[Annotation]): Option[UShort] = {
        val annotation = annotations.filter(isStringUsageAnnotation)
        if (annotation.isEmpty) {
            None
        }

        Option(annotation.head.elementValuePairs.filter {
            _.name == "pc"
        }.head.value.asIntValue.value)
    }

    describe("the org.opalj.fpcf.StringTrackingAnalysis is executed") {
        val as = executeAnalyses(Set(LazyStringTrackingAnalysis))
        as.propertyStore.shutdown()
        validateProperties(as, fieldsWithAnnotations(as.project), Set("StringDefinitions"))
    }

    describe("the org.opalj.fpcf.StringTrackingAnalysis is started") {
        val cutPath = System.getProperty("user.dir")+
            "/DEVELOPING_OPAL/validate/target/scala-2.12/test-classes/org/opalj/fpcf/fixtures/string_tracking/TestMethods.class"
        val p = Project(new File(cutPath))
        val ps = p.get(org.opalj.fpcf.PropertyStoreKey)
        ps.setupPhase(Set(StringConstancyProperty))

        LazyStringTrackingAnalysis.init(p, ps)
        LazyStringTrackingAnalysis.schedule(ps, null)
        val tacProvider = p.get(DefaultTACAIKey)

        // Call the analysis for all methods annotated with @StringDefinitions
        p.allMethodsWithBody.filter {
            _.runtimeInvisibleAnnotations.foldLeft(false)(
                (exists, a) ⇒ exists || isStringUsageAnnotation(a)
            )
        }.foreach { m ⇒
            pcFromAnnotations(m.runtimeInvisibleAnnotations) match {
                case Some(counter) ⇒ extractUVar(tacProvider(m).stmts, counter) match {
                    case Some(uvar) ⇒
                        ps.force(Tuple2(uvar, m), StringConstancyProperty.key)
                        ps.waitOnPhaseCompletion()
                    case _ ⇒
                }
                case _ ⇒
            }
        }
    }

}
