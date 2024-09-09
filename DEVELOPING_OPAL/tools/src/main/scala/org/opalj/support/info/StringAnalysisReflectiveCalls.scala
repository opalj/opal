/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package support
package info

import scala.annotation.switch

import java.net.URL
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.ReferenceType
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectAnalysisApplication
import org.opalj.br.analyses.ReportableAnalysisResult
import org.opalj.br.fpcf.ContextProviderKey
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.br.fpcf.FPCFLazyAnalysisScheduler
import org.opalj.br.fpcf.analyses.ContextProvider
import org.opalj.br.fpcf.properties.string.StringConstancyLevel
import org.opalj.br.fpcf.properties.string.StringConstancyProperty
import org.opalj.br.fpcf.properties.string.StringTreeNode
import org.opalj.br.instructions.Instruction
import org.opalj.br.instructions.INVOKESTATIC
import org.opalj.br.instructions.INVOKEVIRTUAL
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.InterimEUBP
import org.opalj.fpcf.InterimLUBP
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Result
import org.opalj.fpcf.SomeEPS
import org.opalj.tac.Assignment
import org.opalj.tac.Call
import org.opalj.tac.ExprStmt
import org.opalj.tac.StaticFunctionCall
import org.opalj.tac.Stmt
import org.opalj.tac.TACMethodParameter
import org.opalj.tac.TACode
import org.opalj.tac.V
import org.opalj.tac.VirtualFunctionCall
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.fpcf.analyses.string.VariableContext
import org.opalj.tac.fpcf.analyses.string.l0.LazyL0StringAnalysis
import org.opalj.tac.fpcf.analyses.string.l2.LazyL2StringAnalysis
import org.opalj.tac.fpcf.properties.TACAI
import org.opalj.util.PerformanceEvaluation.time

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
 * @author Maximilian Rüsch
 */
object StringAnalysisReflectiveCalls extends ProjectAnalysisApplication {

    private case class Configuration(
        includeCrypto:             Boolean,
        private val runL0Analysis: Boolean
    ) {

        def analyses: Seq[FPCFLazyAnalysisScheduler] = {
            if (runL0Analysis) LazyL0StringAnalysis.allRequiredAnalyses
            else LazyL2StringAnalysis.allRequiredAnalyses
        }
    }

    private val relevantCryptoMethodNames = List(
        "javax.crypto.Cipher#getInstance",
        "javax.crypto.Cipher#getMaxAllowedKeyLength",
        "javax.crypto.Cipher#getMaxAllowedParameterSpec",
        "javax.crypto.Cipher#unwrap",
        "javax.crypto.CipherSpi#engineSetMode",
        "javax.crypto.CipherSpi#engineSetPadding",
        "javax.crypto.CipherSpi#engineUnwrap",
        "javax.crypto.EncryptedPrivateKeyInfo#getKeySpec",
        "javax.crypto.ExemptionMechanism#getInstance",
        "javax.crypto.KeyAgreement#getInstance",
        "javax.crypto.KeyGenerator#getInstance",
        "javax.crypto.Mac#getInstance",
        "javax.crypto.SealedObject#getObject",
        "javax.crypto.SecretKeyFactory#getInstance"
    )

    private val relevantReflectionMethodNames = List(
        "java.lang.Class#forName",
        "java.lang.ClassLoader#loadClass",
        "java.lang.Class#getField",
        "java.lang.Class#getDeclaredField",
        "java.lang.Class#getMethod",
        "java.lang.Class#getDeclaredMethod"
    )

    /**
     * Stores a list of pairs where the first element corresponds to the entities passed to the analysis and the second
     * element corresponds to the method name in which the entity occurred, i.e. a value in [[relevantMethodNames]].
     */
    private val entityContext = ListBuffer[(VariableContext, String)]()

    /**
     * A list of fully-qualified method names that are to be skipped, e.g., because they make an
     * analysis crash (e.g., com/sun/jmx/mbeanserver/MBeanInstantiator#deserialize)
     */
    private val ignoreMethods = List()

    override def title: String = "String Analysis for Reflective Calls"

    override def description: String = {
        "Finds calls to methods provided by the Java Reflection API and tries to resolve passed string values"
    }

    /**
     * Retrieves all relevant method names, i.e., those methods from the Reflection API that have at least one string
     * argument and shall be considered by this analysis. The string are supposed to have the format as produced
     * by [[buildFQMethodName]]. If the 'crypto' parameter is set, relevant methods of the javax.crypto API are
     * included, too.
     */
    private def relevantMethodNames(implicit configuration: Configuration) = {
        if (configuration.includeCrypto)
            relevantReflectionMethodNames ++ relevantCryptoMethodNames
        else
            relevantReflectionMethodNames
    }

    /**
     * Using a `declaringClass` and a `methodName`, this function returns a formatted version of the
     * fully-qualified method name, in the format [fully-qualified class name]#[method name]
     * where the separator for the fq class names is a dot, e.g., "java.lang.Class#forName".
     */
    @inline private final def buildFQMethodName(declaringClass: ReferenceType, methodName: String): String =
        s"${declaringClass.toJava}#$methodName"

    /**
     * Taking the `declaringClass` and the `methodName` into consideration, this function checks
     * whether a method is relevant for this analysis.
     *
     * @note Internally, this method makes use of [[relevantMethodNames]]. A method can only be
     *       relevant if it occurs in [[relevantMethodNames]].
     */
    @inline private final def isRelevantCall(declaringClass: ReferenceType, methodName: String)(
        implicit configuration: Configuration
    ): Boolean = relevantMethodNames.contains(buildFQMethodName(declaringClass, methodName))

    /**
     * Helper function that checks whether an array of [[Instruction]]s contains at least one
     * relevant method that is to be processed by `doAnalyze`.
     */
    private def instructionsContainRelevantMethod(instructions: Array[Instruction])(
        implicit configuration: Configuration
    ): Boolean = {
        instructions
            .filter(_ != null)
            .exists {
                case INVOKESTATIC(declClass, _, methodName, _) if isRelevantCall(declClass, methodName) => true
                case INVOKEVIRTUAL(declClass, methodName, _) if isRelevantCall(declClass, methodName)   => true
                case _                                                                                  => false
            }
    }

    /**
     * This function is a wrapper function for processing a method. It checks whether the given
     * `method`, is relevant at all, and if so uses the given function `call` to call the
     * analysis using the property store, `ps`, to finally store it in the given `resultMap`.
     */
    private def processFunctionCall(pc: Int, method: Method, call: Call[V])(
        implicit
        stmts:           Array[Stmt[V]],
        ps:              PropertyStore,
        contextProvider: ContextProvider,
        declaredMethods: DeclaredMethods,
        configuration:   Configuration
    ): Unit = {
        if (isRelevantCall(call.declaringClass, call.name)) {
            // Loop through all parameters and start the analysis for those that take a string
            call.descriptor.parameterTypes.zipWithIndex.foreach {
                case (ft, index) if ft == ObjectType.String =>
                    val context = contextProvider.newContext(declaredMethods(method))
                    val e = VariableContext(pc, call.params(index).asVar.toPersistentForm, context)
                    ps.force(e, StringConstancyProperty.key)
                    entityContext.append((e, buildFQMethodName(call.declaringClass, call.name)))
                case _ =>
            }
        }
    }

    private def processStatements(tac: TACode[TACMethodParameter, V], m: Method)(
        implicit
        contextProvider: ContextProvider,
        ps:              PropertyStore,
        declaredMethods: DeclaredMethods,
        configuration:   Configuration
    ): Unit = {
        implicit val stmts: Array[Stmt[V]] = tac.stmts
        stmts.foreach { stmt =>
            // Using the following switch speeds up the whole process
            (stmt.astID: @switch) match {
                case Assignment.ASTID => stmt match {
                        case Assignment(pc, _, c: StaticFunctionCall[V]) =>
                            processFunctionCall(pc, m, c)
                        case Assignment(pc, _, c: VirtualFunctionCall[V]) =>
                            processFunctionCall(pc, m, c)
                        case _ =>
                    }
                case ExprStmt.ASTID => stmt match {
                        case ExprStmt(pc, c: StaticFunctionCall[V]) =>
                            processFunctionCall(pc, m, c)
                        case ExprStmt(pc, c: VirtualFunctionCall[V]) =>
                            processFunctionCall(pc, m, c)
                        case _ =>
                    }
                case _ =>
            }
        }
    }

    private def continuation(m: Method)(eps: SomeEPS)(
        implicit
        contextProvider: ContextProvider,
        declaredMethods: DeclaredMethods,
        ps:              PropertyStore,
        configuration:   Configuration
    ): ProperPropertyComputationResult = {
        eps match {
            case FinalP(tac: TACAI) =>
                processStatements(tac.tac.get, m)
                Result(m, tac)
            case InterimLUBP(lb, ub) =>
                InterimResult(
                    m,
                    lb,
                    ub,
                    Set(eps),
                    continuation(m)
                )
            case _ => throw new IllegalStateException("should never happen!")
        }
    }

    override def checkAnalysisSpecificParameters(parameters: Seq[String]): Seq[String] = {
        parameters.flatMap { p =>
            p.toLowerCase match {
                case "-includecryptoapi" => Nil
                case "-l0"               => Nil
                case _                   => List(s"Unknown parameter: $p")
            }
        }
    }

    override def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () => Boolean
    ): ReportableAnalysisResult = {
        implicit val configuration: Configuration = Configuration(
            // Check whether string-consuming methods of the javax.crypto API should be considered. Default is false.
            includeCrypto = parameters.exists(p => p.equalsIgnoreCase("-includeCryptoApi")),
            // Check whether the L0 analysis should be run. By default, L1 is selected.
            runL0Analysis = parameters.exists(p => p.equalsIgnoreCase("-l0"))
        )

        val manager = project.get(FPCFAnalysesManagerKey)
        project.get(RTACallGraphKey)
        implicit val contextProvider: ContextProvider = project.get(ContextProviderKey)
        implicit val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)
        implicit val (propertyStore, _) = manager.runAll(configuration.analyses)

        // Stores the obtained results for each supported reflective operation
        val resultMap = mutable.Map[String, ListBuffer[StringTreeNode]]()
        relevantMethodNames.foreach { resultMap(_) = ListBuffer() }

        project.allMethodsWithBody.foreach { m =>
            val fqnMethodName = buildFQMethodName(m.classFile.thisType, m.name)
            // To dramatically reduce work, quickly check if a method is ignored or not relevant at all
            if (!ignoreMethods.contains(fqnMethodName) && instructionsContainRelevantMethod(m.body.get.instructions)) {
                val tacaiEOptP = propertyStore(m, TACAI.key)
                if (tacaiEOptP.hasUBP) {
                    if (tacaiEOptP.ub.tac.isEmpty) {
                        // No TAC available, e.g., because the method has no body
                        println(s"No body for method: ${m.classFile.fqn}#${m.name}")
                    } else {
                        processStatements(tacaiEOptP.ub.tac.get, m)
                    }
                } else {
                    InterimResult(
                        m,
                        StringConstancyProperty.ub,
                        StringConstancyProperty.lb,
                        Set(tacaiEOptP),
                        continuation(m)
                    )
                }
            }
        }

        time {
            propertyStore.waitOnPhaseCompletion()
            entityContext.foreach {
                case (e, callName) =>
                    propertyStore.properties(e).toIndexedSeq.foreach {
                        case FinalP(p: StringConstancyProperty) =>
                            resultMap(callName).append(p.tree)
                        case InterimEUBP(_, ub: StringConstancyProperty) =>
                            resultMap(callName).append(ub.tree)
                        case _ =>
                            println(s"No result for $e in $callName found!")
                    }
            }
        } { t => println(s"Elapsed Time: ${t.toMilliseconds} ms") }

        resultMapToReport(resultMap)
    }

    private def resultMapToReport(resultMap: mutable.Map[String, ListBuffer[StringTreeNode]]): BasicReport = {
        val report = ListBuffer[String]("Results of the Reflection Analysis:")
        for ((reflectiveCall, entries) <- resultMap) {
            var constantCount, partConstantCount, dynamicCount = 0
            entries.foreach {
                _.constancyLevel match {
                    case StringConstancyLevel.Constant          => constantCount += 1
                    case StringConstancyLevel.PartiallyConstant => partConstantCount += 1
                    case StringConstancyLevel.Dynamic           => dynamicCount += 1
                }
            }

            report.append(s"$reflectiveCall: ${entries.length}x")
            report.append(s" -> Constant:           ${constantCount}x")
            report.append(s" -> Partially Constant: ${partConstantCount}x")
            report.append(s" -> Dynamic:            ${dynamicCount}x")
        }
        BasicReport(report)
    }
}
