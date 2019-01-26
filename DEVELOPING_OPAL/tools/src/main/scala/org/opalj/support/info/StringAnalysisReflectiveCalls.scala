/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.support.info

import scala.annotation.switch

import java.net.URL

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

import org.opalj.fpcf.FinalP
import org.opalj.fpcf.PropertyStore
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.DefaultOneStepAnalysis
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ReportableAnalysisResult
import org.opalj.br.instructions.Instruction
import org.opalj.br.instructions.INVOKESTATIC
import org.opalj.br.ReferenceType
import org.opalj.br.instructions.INVOKEVIRTUAL
import org.opalj.br.Method
import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.br.fpcf.properties.string_definition.StringConstancyLevel
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.tac.Assignment
import org.opalj.tac.Call
import org.opalj.tac.ExprStmt
import org.opalj.tac.SimpleTACAIKey
import org.opalj.tac.StaticFunctionCall
import org.opalj.tac.VirtualFunctionCall
import org.opalj.tac.fpcf.analyses.string_analysis.LazyLocalStringAnalysis
import org.opalj.tac.fpcf.analyses.string_analysis.P
import org.opalj.tac.fpcf.analyses.string_analysis.V

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
     * Stores a list of pairs where the first element corresponds to the entities passed to the
     * analysis and the second element corresponds to the method name in which the entity occurred,
     * i.e., a value in [[relevantMethodNames]].
     */
    private val entityContext = ListBuffer[(P, String)]()

    /**
     * Stores all relevant method names of the Java Reflection API, i.e., those methods from the
     * Reflection API that have at least one string argument and shall be considered by this
     * analysis. The string are supposed to have the format as produced by [[buildFQMethodName]].
     */
    private val relevantMethodNames = List(
        // The following is for the Java Reflection API
        "java.lang.Class#forName", "java.lang.ClassLoader#loadClass",
        "java.lang.Class#getField", "java.lang.Class#getDeclaredField",
        "java.lang.Class#getMethod", "java.lang.Class#getDeclaredMethod"
        // The following is for the javax.crypto API
        //"javax.crypto.Cipher#getInstance", "javax.crypto.Cipher#getMaxAllowedKeyLength",
        //"javax.crypto.Cipher#getMaxAllowedParameterSpec", "javax.crypto.Cipher#unwrap",
        //"javax.crypto.CipherSpi#engineSetMode", "javax.crypto.CipherSpi#engineSetPadding",
        //"javax.crypto.CipherSpi#engineUnwrap", "javax.crypto.EncryptedPrivateKeyInfo#getKeySpec",
        //"javax.crypto.ExemptionMechanism#getInstance", "javax.crypto.KeyAgreement#getInstance",
        //"javax.crypto.KeyGenerator#getInstance", "javax.crypto.Mac#getInstance",
        //"javax.crypto.SealedObject#getObject", "javax.crypto.SecretKeyFactory#getInstance"
    )

    /**
     * A list of fully-qualified method names that are to be skipped, e.g., because they make the
     * analysis crash.
     */
    private val ignoreMethods = List(
        // For the next one, there should be a \w inside the second string
        // "com/sun/glass/ui/monocle/NativePlatformFactory#getNativePlatform",
        // Check this result:
        //"com/sun/jmx/mbeanserver/MBeanInstantiator#deserialize"
    )

    override def title: String = "String Analysis for Reflective Calls"

    override def description: String = {
        "Finds calls to methods provided by the Java Reflection API and tries to resolve passed "+
            "string values"
    }

    /**
     * Using a `declaringClass` and a `methodName`, this function returns a formatted version of the
     * fully-qualified method name, in the format [fully-qualified class name]#[method name]
     * where the separator for the fq class names is a dot, e.g., "java.lang.Class#forName".
     */
    private def buildFQMethodName(declaringClass: ReferenceType, methodName: String): String =
        s"${declaringClass.toJava}#$methodName"

    /**
     * Taking the `declaringClass` and the `methodName` into consideration, this function checks
     * whether a method is relevant for this analysis.
     *
     * @note Internally, this method makes use of [[relevantMethodNames]]. A method can only be
     *       relevant if it occurs in [[relevantMethodNames]].
     */
    private def isRelevantCall(declaringClass: ReferenceType, methodName: String): Boolean =
        relevantMethodNames.contains(buildFQMethodName(declaringClass, methodName))

    /**
     * Helper function that checks whether an array of [[Instruction]]s contains at least one
     * relevant method that is to be processed by `doAnalyze`.
     */
    private def instructionsContainRelevantMethod(instructions: Array[Instruction]): Boolean = {
        instructions.filter(_ != null).foldLeft(false) { (previous, nextInstr) ⇒
            previous || ((nextInstr.opcode: @switch) match {
                case INVOKESTATIC.opcode ⇒
                    val INVOKESTATIC(declClass, _, methodName, _) = nextInstr
                    isRelevantCall(declClass, methodName)
                case INVOKEVIRTUAL.opcode ⇒
                    val INVOKEVIRTUAL(declClass, methodName, _) = nextInstr
                    isRelevantCall(declClass, methodName)
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
        if (isRelevantCall(call.declaringClass, call.name)) {
            val fqnMethodName = s"${method.classFile.thisType.fqn}#${method.name}"
            if (!ignoreMethods.contains(fqnMethodName)) {
                //println(
                //    s"Processing ${call.name} in ${method.classFile.thisType.fqn}#${method.name}"
                //)
                // Loop through all parameters and start the analysis for those that take a string
                call.descriptor.parameterTypes.zipWithIndex.foreach {
                    case (ft, index) ⇒
                        if (ft.toJava == "java.lang.String") {
                            val duvar = call.params(index).asVar
                            val e = (duvar, method)

                            ps.force(e, StringConstancyProperty.key)
                            entityContext.append(
                                (e, buildFQMethodName(call.declaringClass, call.name))
                            )
                        }
                }
            }
        }
    }

    /**
     * Takes a `resultMap` and transforms the information contained in that map into a
     * [[BasicReport]] which will serve as the final result of the analysis.
     */
    private def resultMapToReport(resultMap: ResultMapType): BasicReport = {
        val report = ListBuffer[String]("Results of the Reflection Analysis:")
        for ((reflectiveCall, entries) ← resultMap) {
            var constantCount, partConstantCount, dynamicCount = 0
            entries.foreach {
                _.constancyLevel match {
                    case StringConstancyLevel.CONSTANT           ⇒ constantCount += 1
                    case StringConstancyLevel.PARTIALLY_CONSTANT ⇒ partConstantCount += 1
                    case StringConstancyLevel.DYNAMIC            ⇒ dynamicCount += 1
                }
            }

            report.append(s"$reflectiveCall: ${entries.length}x")
            report.append(s" -> Constant:           ${constantCount}x")
            report.append(s" -> Partially Constant: ${partConstantCount}x")
            report.append(s" -> Dynamic:            ${dynamicCount}x")
        }
        BasicReport(report)
    }

    override def doAnalyze(
        project: Project[URL], parameters: Seq[String], isInterrupted: () ⇒ Boolean
    ): ReportableAnalysisResult = {
        val t0 = System.currentTimeMillis()

        implicit val propertyStore: PropertyStore = project.get(PropertyStoreKey)
        project.get(FPCFAnalysesManagerKey).runAll(LazyLocalStringAnalysis)
        val tacProvider = project.get(SimpleTACAIKey)

        // Stores the obtained results for each supported reflective operation
        val resultMap: ResultMapType = mutable.Map[String, ListBuffer[StringConstancyInformation]]()
        relevantMethodNames.foreach { resultMap(_) = ListBuffer() }

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

        // TODO: The call to waitOnPhaseCompletion is not 100 % correct, however, without it
        //       resultMap does not get filled at all
        propertyStore.waitOnPhaseCompletion()
        entityContext.foreach {
            case (e, callName) ⇒
                propertyStore.properties(e).toIndexedSeq.foreach {
                    case FinalP(p) ⇒
                        resultMap(callName).append(
                            p.asInstanceOf[StringConstancyProperty].stringConstancyInformation
                        )
                    case _ ⇒
                }
        }

        val t1 = System.currentTimeMillis()
        println(s"Elapsed Time: ${t1 - t0} ms")
        resultMapToReport(resultMap)
    }

}
