/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.support.info

import scala.annotation.switch

import java.net.URL

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

import org.opalj.fpcf.FinalP
import org.opalj.fpcf.InterimELUBP
import org.opalj.fpcf.InterimLUBP
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Result
import org.opalj.fpcf.SomeEPS
import org.opalj.value.ValueInformation
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ReportableAnalysisResult
import org.opalj.br.instructions.Instruction
import org.opalj.br.instructions.INVOKESTATIC
import org.opalj.br.ReferenceType
import org.opalj.br.instructions.INVOKEVIRTUAL
import org.opalj.br.Method
import org.opalj.br.analyses.ProjectAnalysisApplication
import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.br.fpcf.properties.string_definition.StringConstancyLevel
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.tac.Assignment
import org.opalj.tac.Call
import org.opalj.tac.ExprStmt
import org.opalj.tac.StaticFunctionCall
import org.opalj.tac.VirtualFunctionCall
import org.opalj.tac.fpcf.analyses.string_analysis.P
import org.opalj.tac.fpcf.analyses.string_analysis.V
import org.opalj.tac.fpcf.properties.TACAI
import org.opalj.tac.DUVar
import org.opalj.tac.Stmt
import org.opalj.tac.TACMethodParameter
import org.opalj.tac.TACode
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.fpcf.analyses.string_analysis.LazyInterproceduralStringAnalysis

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
object StringAnalysisReflectiveCalls extends ProjectAnalysisApplication {

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
        // The following is for the javax.crypto API
        //"javax.crypto.Cipher#getInstance", "javax.crypto.Cipher#getMaxAllowedKeyLength",
        //"javax.crypto.Cipher#getMaxAllowedParameterSpec", "javax.crypto.Cipher#unwrap",
        //"javax.crypto.CipherSpi#engineSetMode", "javax.crypto.CipherSpi#engineSetPadding",
        //"javax.crypto.CipherSpi#engineUnwrap", "javax.crypto.EncryptedPrivateKeyInfo#getKeySpec",
        //"javax.crypto.ExemptionMechanism#getInstance", "javax.crypto.KeyAgreement#getInstance",
        //"javax.crypto.KeyGenerator#getInstance", "javax.crypto.Mac#getInstance",
        //"javax.crypto.SealedObject#getObject", "javax.crypto.SecretKeyFactory#getInstance"
        // The following is for the Java Reflection API
        "java.lang.Class#forName", "java.lang.ClassLoader#loadClass",
        "java.lang.Class#getField", "java.lang.Class#getDeclaredField",
        "java.lang.Class#getMethod", "java.lang.Class#getDeclaredMethod"
    )

    /**
     * A list of fully-qualified method names that are to be skipped, e.g., because they make an
     * analysis crash (e.g., com/sun/jmx/mbeanserver/MBeanInstantiator#deserialize)
     */
    private val ignoreMethods = List()

    // executeFrom specifies the index / counter when to start feeding entities to the property
    // store. executeTo specifies the index / counter when to stop feeding entities to the property
    // store. These values are basically to help debugging. executionCounter is a helper variable
    // for that purpose
    private val executeFrom = 0
    private val executeTo = 10000
    private var executionCounter = 0

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
                if (executionCounter >= executeFrom && executionCounter <= executeTo) {
                    println(
                        s"Starting ${call.name} in ${method.classFile.thisType.fqn}#${method.name}"
                    )
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
                executionCounter += 1
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

    private def processStatements(
        ps:        PropertyStore,
        stmts:     Array[Stmt[V]],
        m:         Method,
        resultMap: ResultMapType
    ): Unit = {
        stmts.foreach { stmt ⇒
            // Using the following switch speeds up the whole process
            (stmt.astID: @switch) match {
                case Assignment.ASTID ⇒ stmt match {
                    case Assignment(_, _, c: StaticFunctionCall[V]) ⇒
                        processFunctionCall(ps, m, c, resultMap)
                    case Assignment(_, _, c: VirtualFunctionCall[V]) ⇒
                        processFunctionCall(ps, m, c, resultMap)
                    case _ ⇒
                }
                case ExprStmt.ASTID ⇒ stmt match {
                    case ExprStmt(_, c: StaticFunctionCall[V]) ⇒
                        processFunctionCall(ps, m, c, resultMap)
                    case ExprStmt(_, c: VirtualFunctionCall[V]) ⇒
                        processFunctionCall(ps, m, c, resultMap)
                    case _ ⇒
                }
                case _ ⇒
            }
        }
    }

    private def continuation(
        ps: PropertyStore, m: Method, resultMap: ResultMapType
    )(eps: SomeEPS): ProperPropertyComputationResult = {
        eps match {
            case FinalP(tac: TACAI) ⇒
                processStatements(ps, tac.tac.get.stmts, m, resultMap)
                Result(m, tac)
            case InterimLUBP(lb, ub) ⇒
                InterimResult(
                    m, lb, ub, List(eps), continuation(ps, m, resultMap)
                )
            case _ ⇒ throw new IllegalStateException("should never happen!")
        }
    }

    override def doAnalyze(
        project: Project[URL], parameters: Seq[String], isInterrupted: () ⇒ Boolean
    ): ReportableAnalysisResult = {
        val manager = project.get(FPCFAnalysesManagerKey)
        project.get(RTACallGraphKey)
        implicit val (propertyStore, analyses) = manager.runAll(
            LazyInterproceduralStringAnalysis
        // LazyIntraproceduralStringAnalysis
        )

        // Stores the obtained results for each supported reflective operation
        val resultMap: ResultMapType = mutable.Map[String, ListBuffer[StringConstancyInformation]]()
        relevantMethodNames.foreach { resultMap(_) = ListBuffer() }

        project.allMethodsWithBody.foreach { m ⇒
            // To dramatically reduce work, quickly check if a method is relevant at all
            if (instructionsContainRelevantMethod(m.body.get.instructions)) {
                var tac: TACode[TACMethodParameter, DUVar[ValueInformation]] = null
                val tacaiEOptP = propertyStore(m, TACAI.key)
                if (tacaiEOptP.hasUBP) {
                    if (tacaiEOptP.ub.tac.isEmpty) {
                        // No TAC available, e.g., because the method has no body
                        println(s"No body for method: ${m.classFile.fqn}#${m.name}")
                    } else {
                        tac = tacaiEOptP.ub.tac.get
                        processStatements(propertyStore, tac.stmts, m, resultMap)
                    }
                } else {
                    InterimResult(
                        m,
                        StringConstancyProperty.ub,
                        StringConstancyProperty.lb,
                        List(tacaiEOptP),
                        continuation(propertyStore, m, resultMap)
                    )
                }
            }
        }

        val t0 = System.currentTimeMillis()
        propertyStore.waitOnPhaseCompletion()
        entityContext.foreach {
            case (e, callName) ⇒
                propertyStore.properties(e).toIndexedSeq.foreach {
                    case FinalP(p: StringConstancyProperty) ⇒
                        resultMap(callName).append(p.stringConstancyInformation)
                    case InterimELUBP(_, _, ub: StringConstancyProperty) ⇒
                        resultMap(callName).append(ub.stringConstancyInformation)
                    case _ ⇒
                        println(s"Neither a final nor an interim result for $e in $callName; "+
                            "this should never be the case!")
                }
        }

        val t1 = System.currentTimeMillis()
        println(s"Elapsed Time: ${t1 - t0} ms")
        resultMapToReport(resultMap)
    }

}
