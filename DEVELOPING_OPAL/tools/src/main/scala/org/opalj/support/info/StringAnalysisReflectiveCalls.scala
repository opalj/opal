/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.support.info

import java.net.URL

import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.DefaultOneStepAnalysis
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ReportableAnalysisResult
import org.opalj.fpcf.analyses.string_definition.V
import org.opalj.fpcf.FPCFAnalysesManagerKey
import org.opalj.fpcf.analyses.string_definition.LazyStringDefinitionAnalysis
import org.opalj.fpcf.string_definition.properties.StringConstancyInformation
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.properties.StringConstancyProperty
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.PropertyStoreKey
import org.opalj.tac.Assignment
import org.opalj.tac.ExprStmt
import org.opalj.tac.SimpleTACAIKey
import org.opalj.tac.StaticFunctionCall
import org.opalj.tac.VirtualFunctionCall

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
 * Analyzes a project for calls provided by the Java Reflection API and tries to determine which
 * string values are / could be passed to these calls.
 * <p>
 * Currently, this runner supports / handles the following reflective calls:
 * <ul>
 *     <li>`Class.forName(string)`</li>
 *     <li>`Class.forName(string, boolean, classLoader)`</li>
 *     <li>`Class.getField(string)`</li>
 *     <li>`Class.getDeclaredField(string)`</li>
 *     <li>`Class.getMethod(String, Class[])`</li>
 *     <li>`Class.getDeclaredMethod(String, Class[])`</li>
 * </ul>
 *
 * @author Patrick Mell
 */
object StringAnalysisReflectiveCalls extends DefaultOneStepAnalysis {

    override def title: String = "String Analysis for Reflective Calls"

    override def description: String = {
        "Finds calls to methods provided by the Java Reflection API and tries to resolve passed "+
            "string values"
    }

    override def doAnalyze(
        project: Project[URL], parameters: Seq[String], isInterrupted: () ⇒ Boolean
    ): ReportableAnalysisResult = {
        implicit val propertyStore: PropertyStore = project.get(PropertyStoreKey)
        project.get(FPCFAnalysesManagerKey).runAll(LazyStringDefinitionAnalysis)

        // Stores the obtained results for each supported reflective operation
        val countMap = mutable.Map[String, ListBuffer[StringConstancyInformation]](
            "forName" → ListBuffer(),
            "getField" → ListBuffer(),
            "getDeclaredField" → ListBuffer(),
            "getMethod" → ListBuffer(),
            "getDeclaredMethod" → ListBuffer()
        )

        val tacProvider = project.get(SimpleTACAIKey)
        project.allMethodsWithBody.foreach { m ⇒
            val stmts = tacProvider(m).stmts
            stmts.foreach {
                // Capture the Class.forName calls
                case Assignment(_, _, expr) if expr.isInstanceOf[StaticFunctionCall[V]] ⇒
                    val sfc = expr.asInstanceOf[StaticFunctionCall[V]]
                    val fqClassName = sfc.declaringClass.toJava
                    if (countMap.contains(sfc.name) && fqClassName == "java.lang.Class") {
                        val duvar = sfc.params.head.asVar
                        propertyStore((List(duvar), m), StringConstancyProperty.key) match {
                            case FinalEP(_, prop) ⇒
                                countMap(sfc.name).appendAll(prop.stringConstancyInformation)
                            case _ ⇒
                        }
                    }
                // Capture all other reflective calls
                case ExprStmt(_, expr) ⇒
                    expr match {
                        case vfc: VirtualFunctionCall[V] ⇒
                            // Make sure we really deal with a call from the reflection API
                            if (countMap.contains(vfc.name) &&
                                vfc.descriptor.returnType.toJava.contains("java.lang.reflect.")) {
                                // String argument is always the first one
                                val duvar = vfc.params.head.asVar
                                propertyStore((List(duvar), m), StringConstancyProperty.key) match {
                                    case FinalEP(_, prop) ⇒
                                        countMap(vfc.name).appendAll(
                                            prop.stringConstancyInformation
                                        )
                                    case _ ⇒
                                }
                            }
                        case _ ⇒
                    }
                case _ ⇒
            }
        }

        val report = ListBuffer[String]("Results:")
        // TODO: Define what the report shall look like
        BasicReport(report)
    }

}
