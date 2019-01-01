/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.support.info

import java.net.URL

import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.DefaultOneStepAnalysis
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ReportableAnalysisResult
import org.opalj.br.instructions.Instruction
import org.opalj.br.instructions.INVOKESTATIC
import org.opalj.br.MethodDescriptor
import org.opalj.br.ReferenceType
import org.opalj.br.instructions.INVOKEVIRTUAL
import org.opalj.br.Method
import org.opalj.fpcf.FPCFAnalysesManagerKey
import org.opalj.fpcf.analyses.string_definition.LazyStringDefinitionAnalysis
import org.opalj.fpcf.string_definition.properties.StringConstancyInformation
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.PropertyStoreKey
import org.opalj.fpcf.analyses.string_definition.V
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.properties.StringConstancyProperty
import org.opalj.tac.Assignment
import org.opalj.tac.Call
import org.opalj.tac.ExprStmt
import org.opalj.tac.SimpleTACAIKey
import org.opalj.tac.StaticFunctionCall
import org.opalj.tac.VirtualFunctionCall

import scala.annotation.switch
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
 * Analyzes a project for calls provided by the Java Reflection API and tries to determine which
 * string values are / could be passed to these calls.
 * <p>
 * Currently, this runner supports / handles the following reflective calls:
 * <ul>
 * <li>`Class.forName(string)`</li>
 * <li>`Class.forName(string, boolean, classLoader)`</li>
 * <li>`Class.getField(string)`</li>
 * <li>`Class.getDeclaredField(string)`</li>
 * <li>`Class.getMethod(String, Class[])`</li>
 * <li>`Class.getDeclaredMethod(String, Class[])`</li>
 * </ul>
 *
 * @author Patrick Mell
 */
object StringAnalysisReflectiveCalls extends DefaultOneStepAnalysis {

    private type ResultMapType = mutable.Map[String, ListBuffer[StringConstancyInformation]]

    /**
     * Stores all relevant method names of the Java Reflection API, i.e., those methods from the
     * Reflection API that have at least one string argument and shall be considered by this
     * analysis.
     */
    private val relevantMethodNames = List(
        "forName", "getField", "getDeclaredField", "getMethod", "getDeclaredMethod"
    )

    override def title: String = "String Analysis for Reflective Calls"

    override def description: String = {
        "Finds calls to methods provided by the Java Reflection API and tries to resolve passed "+
            "string values"
    }

    /**
     * Taking the `declaringClass`, the `methodName` as well as the `methodDescriptor` into
     * consideration, this function checks whether a method is relevant for this analysis.
     *
     * @note Internally, this method makes use of [[relevantMethodNames]]. A method can only be
     *       relevant if its name occurs in [[relevantMethodNames]].
     */
    private def isRelevantMethod(
        declaringClass: ReferenceType, methodName: String, methodDescriptor: MethodDescriptor
    ): Boolean =
        relevantMethodNames.contains(methodName) && (declaringClass.toJava == "java.lang.Class" &&
            (methodDescriptor.returnType.toJava.contains("java.lang.reflect.") ||
                methodDescriptor.returnType.toJava.contains("java.lang.Class")))

    /**
     * Helper function that checks whether an array of [[Instruction]]s contains at least one
     * relevant method that is to be processed by `doAnalyze`.
     */
    private def instructionsContainRelevantMethod(instructions: Array[Instruction]): Boolean = {
        instructions.filter(_ != null).foldLeft(false) { (previous, nextInstr) ⇒
            previous || ((nextInstr.opcode: @switch) match {
                case INVOKESTATIC.opcode ⇒
                    val INVOKESTATIC(declClass, _, methodName, methodDescr) = nextInstr
                    isRelevantMethod(declClass, methodName, methodDescr)
                case INVOKEVIRTUAL.opcode ⇒
                    val INVOKEVIRTUAL(declClass, methodName, methodDescr) = nextInstr
                    isRelevantMethod(declClass, methodName, methodDescr)
                case _ ⇒ false
            })
        }
    }

    /**
     * This function is a wrapper function for processing a method. It checks whether the given
     * `method`, is relevant at all, and if so uses the given function `call` to call the
     * analysis using the property store, `ps`, to finally store it in the given `resultMap`.
     */
    private def processFunctionCall(
        ps: PropertyStore, method: Method, call: Call[V], resultMap: ResultMapType
    ): Unit = {
        if (isRelevantMethod(call.declaringClass, call.name, call.descriptor)) {
            val duvar = call.params.head.asVar
            ps((duvar, method), StringConstancyProperty.key) match {
                case FinalEP(_, prop) ⇒
                    resultMap(call.name).append(prop.stringConstancyInformation)
                case _ ⇒
            }
        }
    }

    override def doAnalyze(
        project: Project[URL], parameters: Seq[String], isInterrupted: () ⇒ Boolean
    ): ReportableAnalysisResult = {
        implicit val propertyStore: PropertyStore = project.get(PropertyStoreKey)
        project.get(FPCFAnalysesManagerKey).runAll(LazyStringDefinitionAnalysis)
        val tacProvider = project.get(SimpleTACAIKey)

        // Stores the obtained results for each supported reflective operation
        val resultMap: ResultMapType = mutable.Map[String, ListBuffer[StringConstancyInformation]]()
        relevantMethodNames.foreach { resultMap(_) = ListBuffer() }

        identity(propertyStore)

        project.allMethodsWithBody.foreach { m ⇒
            // To dramatically reduce the work of the tacProvider, quickly check if a method is
            // relevant at all
            if (instructionsContainRelevantMethod(m.body.get.instructions)) {
                val stmts = tacProvider(m).stmts
                stmts.foreach { stmt ⇒
                    // Use the following switch to speed-up the whole process
                    (stmt.astID: @switch) match {
                        case Assignment.ASTID ⇒ stmt match {
                            case Assignment(_, _, c: StaticFunctionCall[V]) ⇒
                                processFunctionCall(propertyStore, m, c, resultMap)
                            case Assignment(_, _, c: VirtualFunctionCall[V]) ⇒
                                processFunctionCall(propertyStore, m, c, resultMap)
                            case _ ⇒
                        }
                        case ExprStmt.ASTID ⇒ stmt match {
                            case ExprStmt(_, c: StaticFunctionCall[V]) ⇒
                                processFunctionCall(propertyStore, m, c, resultMap)
                            case ExprStmt(_, c: VirtualFunctionCall[V]) ⇒
                                processFunctionCall(propertyStore, m, c, resultMap)
                            case _ ⇒
                        }
                        case _ ⇒
                    }
                }
            }
        }
        val report = ListBuffer[String]("Results:")
        // TODO: Define what the report shall look like
        BasicReport(report)
    }

}
