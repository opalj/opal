/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package support
package info

import scala.annotation.switch

import java.net.URL
import scala.collection.mutable.ListBuffer
import scala.util.Try

import org.opalj.br.DeclaredMethod
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
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.br.fpcf.analyses.ContextProvider
import org.opalj.br.fpcf.properties.string.StringConstancyLevel
import org.opalj.br.fpcf.properties.string.StringConstancyProperty
import org.opalj.br.instructions.Instruction
import org.opalj.br.instructions.INVOKESTATIC
import org.opalj.br.instructions.INVOKEVIRTUAL
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.PropertyStore
import org.opalj.tac.Assignment
import org.opalj.tac.Call
import org.opalj.tac.ComputeTACAIKey
import org.opalj.tac.ExprStmt
import org.opalj.tac.StaticFunctionCall
import org.opalj.tac.Stmt
import org.opalj.tac.TACMethodParameter
import org.opalj.tac.TACode
import org.opalj.tac.V
import org.opalj.tac.VirtualFunctionCall
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.fpcf.analyses.fieldaccess.EagerFieldAccessInformationAnalysis
import org.opalj.tac.fpcf.analyses.fieldaccess.reflection.ReflectionRelatedFieldAccessesAnalysisScheduler
import org.opalj.tac.fpcf.analyses.string.LazyMethodStringFlowAnalysis
import org.opalj.tac.fpcf.analyses.string.LazyStringAnalysis
import org.opalj.tac.fpcf.analyses.string.VariableContext
import org.opalj.tac.fpcf.analyses.string.l0.LazyL0StringFlowAnalysis
import org.opalj.tac.fpcf.analyses.string.l1.LazyL1StringFlowAnalysis
import org.opalj.tac.fpcf.analyses.string.l2.LazyL2StringFlowAnalysis
import org.opalj.tac.fpcf.analyses.string.l3.LazyL3StringFlowAnalysis
import org.opalj.tac.fpcf.analyses.string.trivial.LazyTrivialStringAnalysis
import org.opalj.tac.fpcf.analyses.systemproperties.TriggeredSystemPropertiesAnalysisScheduler
import org.opalj.util.PerformanceEvaluation.time

/**
 * Analyzes a project for calls provided by the Java Reflection API and tries to determine which string values are /
 * could be passed to these calls. Includes an option to also analyze relevant JavaX Crypto API calls.
 *
 * @author Maximilian Rüsch
 */
object StringAnalysisReflectiveCalls extends ProjectAnalysisApplication {

    /**
     * @param detectedValues Stores a list of pairs where the first element corresponds to the entities passed to the analysis and the second
     * element corresponds to the method name in which the entity occurred, i.e. a value in [[relevantMethodNames]].
     */
    private case class State(detectedValues: ListBuffer[(VariableContext, String)])

    private case class Configuration(
        includeCrypto:              Boolean,
        private val analysisConfig: Configuration.AnalysisConfig
    ) {

        def analyses: Seq[FPCFLazyAnalysisScheduler] = {
            analysisConfig match {
                case Configuration.TrivialAnalysis => Seq(LazyTrivialStringAnalysis)
                case Configuration.LevelAnalysis(level) =>
                    Seq(
                        LazyStringAnalysis,
                        LazyMethodStringFlowAnalysis,
                        Configuration.LevelToSchedulerMapping(level)
                    )
            }
        }
    }

    private object Configuration {

        private[Configuration] trait AnalysisConfig
        private[Configuration] case object TrivialAnalysis extends AnalysisConfig
        private[Configuration] case class LevelAnalysis(level: Int) extends AnalysisConfig

        final val LevelToSchedulerMapping = Map(
            0 -> LazyL0StringFlowAnalysis,
            1 -> LazyL1StringFlowAnalysis,
            2 -> LazyL2StringFlowAnalysis,
            3 -> LazyL3StringFlowAnalysis
        )

        def apply(parameters: Seq[String]): Configuration = {
            val includeCrypto = parameters.contains("-includeCryptoApi")
            val levelParameter = parameters.find(_.startsWith("-level=")).getOrElse("-level=trivial")
            val analysisConfig = levelParameter.replace("-level=", "") match {
                case "trivial" => TrivialAnalysis
                case string    => LevelAnalysis(string.toInt)
            }

            new Configuration(includeCrypto, analysisConfig)
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

    override def title: String = "String Analysis for Reflective Calls"

    override def description: String = {
        "Finds calls to methods provided by the Java Reflection API and tries to resolve passed string values"
    }

    override def analysisSpecificParametersDescription: String =
        s"""
          | [-includeCryptoApi]
          | [-level=trivial|${Configuration.LevelToSchedulerMapping.keys.toSeq.sorted.mkString("|")}]
          |""".stripMargin

    override def checkAnalysisSpecificParameters(parameters: Seq[String]): Iterable[String] = {
        parameters.flatMap {
            case "-includeCryptoApi" => None
            case levelParameter if levelParameter.startsWith("-level=") =>
                levelParameter.replace("-level=", "") match {
                    case "trivial" =>
                        None
                    case string
                        if Try(string.toInt).isSuccess
                            && Configuration.LevelToSchedulerMapping.keySet.contains(string.toInt) =>
                        None
                    case value =>
                        Some(s"Unknown level parameter value: $value")
                }

            case param => Some(s"unknown parameter: $param")
        }
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
    private def processFunctionCall(pc: Int, dm: DeclaredMethod, call: Call[V])(
        implicit
        stmts:           Array[Stmt[V]],
        ps:              PropertyStore,
        contextProvider: ContextProvider,
        state:           State,
        configuration:   Configuration
    ): Unit = {
        if (isRelevantCall(call.declaringClass, call.name)) {
            // Loop through all parameters and start the analysis for those that take a string
            call.descriptor.parameterTypes.zipWithIndex.foreach {
                case (ft, index) if ft == ObjectType.String =>
                    val e = VariableContext(
                        pc,
                        call.params(index).asVar.toPersistentForm,
                        contextProvider.newContext(dm)
                    )
                    ps.force(e, StringConstancyProperty.key)
                    state.detectedValues.append((e, buildFQMethodName(call.declaringClass, call.name)))
                case _ =>
            }
        }
    }

    private def processStatements(tac: TACode[TACMethodParameter, V], dm: DeclaredMethod)(
        implicit
        contextProvider: ContextProvider,
        ps:              PropertyStore,
        state:           State,
        configuration:   Configuration
    ): Unit = {
        implicit val stmts: Array[Stmt[V]] = tac.stmts
        stmts.foreach { stmt =>
            // Using the following switch speeds up the whole process
            (stmt.astID: @switch) match {
                case Assignment.ASTID => stmt match {
                        case Assignment(pc, _, c: StaticFunctionCall[V]) =>
                            processFunctionCall(pc, dm, c)
                        case Assignment(pc, _, c: VirtualFunctionCall[V]) =>
                            processFunctionCall(pc, dm, c)
                        case _ =>
                    }
                case ExprStmt.ASTID => stmt match {
                        case ExprStmt(pc, c: StaticFunctionCall[V]) =>
                            processFunctionCall(pc, dm, c)
                        case ExprStmt(pc, c: VirtualFunctionCall[V]) =>
                            processFunctionCall(pc, dm, c)
                        case _ =>
                    }
                case _ =>
            }
        }
    }

    override def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () => Boolean
    ): ReportableAnalysisResult = {
        implicit val state: State = State(detectedValues = ListBuffer.empty)
        implicit val configuration: Configuration = Configuration(parameters)

        val cgKey = RTACallGraphKey
        val typeIterator = cgKey.getTypeIterator(project)
        project.updateProjectInformationKeyInitializationData(ContextProviderKey) { _ => typeIterator }

        val manager = project.get(FPCFAnalysesManagerKey)
        val computeTac = project.get(ComputeTACAIKey)
        val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)
        implicit val propertyStore: PropertyStore = project.get(PropertyStoreKey)
        implicit val contextProvider: ContextProvider = project.get(ContextProviderKey)

        time {
            manager.runAll(
                cgKey.allCallGraphAnalyses(project) ++
                    configuration.analyses ++
                    Seq(
                        EagerFieldAccessInformationAnalysis,
                        ReflectionRelatedFieldAccessesAnalysisScheduler,
                        TriggeredSystemPropertiesAnalysisScheduler
                    ),
                afterPhaseScheduling = _ => {
                    project.allMethodsWithBody.foreach { m =>
                        // To dramatically reduce work, quickly check if a method is relevant at all
                        if (instructionsContainRelevantMethod(m.body.get.instructions)) {
                            processStatements(computeTac(m), declaredMethods(m))
                        }
                    }
                }
            )
        } { t => println(s"Elapsed Time: ${t.toMilliseconds}") }

        val resultMap = Map.from(relevantMethodNames.map((_, ListBuffer.empty[FinalEP[_, StringConstancyProperty]])))
        state.detectedValues.foreach {
            case (e, callName) =>
                resultMap(callName).append(propertyStore(e, StringConstancyProperty.key).asFinal)
        }

        val report = ListBuffer[String]("Results of the Reflection Analysis:")
        for ((reflectiveCall, stringTrees) <- resultMap.toSeq.sortBy(_._1)) {
            val invalidCount = stringTrees.count(_.p.tree.constancyLevel == StringConstancyLevel.Invalid)
            val constantCount = stringTrees.count(_.p.tree.constancyLevel == StringConstancyLevel.Constant)
            val partiallyConstantCount =
                stringTrees.count(_.p.tree.constancyLevel == StringConstancyLevel.PartiallyConstant)
            val dynamicCount = stringTrees.count(_.p.tree.constancyLevel == StringConstancyLevel.Dynamic)

            report.append(s"$reflectiveCall: ${stringTrees.length}x")
            report.append(s" -> Invalid:            ${invalidCount}x")
            report.append(s" -> Constant:           ${constantCount}x")
            report.append(s" -> Partially Constant: ${partiallyConstantCount}x")
            report.append(s" -> Dynamic:            ${dynamicCount}x")
        }
        BasicReport(report)
    }
}
