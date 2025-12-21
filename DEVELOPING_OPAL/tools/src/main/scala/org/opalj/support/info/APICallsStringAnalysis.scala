/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package support
package info

import scala.annotation.switch
import scala.language.postfixOps

import java.io.File
import java.net.URL
import scala.collection.mutable.ListBuffer

import org.rogach.scallop.flagConverter

import org.opalj.br.ClassType
import org.opalj.br.DeclaredMethod
import org.opalj.br.ReferenceType
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectsAnalysisApplication
import org.opalj.br.analyses.ReportableAnalysisResult
import org.opalj.br.fpcf.analyses.ContextProvider
import org.opalj.br.fpcf.cli.MultiProjectAnalysisConfig
import org.opalj.br.fpcf.cli.StringAnalysisArg
import org.opalj.br.fpcf.properties.string.StringConstancyLevel
import org.opalj.br.fpcf.properties.string.StringConstancyProperty
import org.opalj.br.instructions.Instruction
import org.opalj.br.instructions.INVOKESTATIC
import org.opalj.br.instructions.INVOKEVIRTUAL
import org.opalj.cli.AnalysisLevelArg
import org.opalj.cli.PlainArg
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.FPCFAnalysesManagerKey
import org.opalj.fpcf.FPCFAnalysisScheduler
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
import org.opalj.tac.cg.CallGraphArg
import org.opalj.tac.cg.CGBasedCommandLineConfig
import org.opalj.tac.fpcf.analyses.fieldaccess.EagerFieldAccessInformationAnalysis
import org.opalj.tac.fpcf.analyses.fieldaccess.reflection.ReflectionRelatedFieldAccessesAnalysisScheduler
import org.opalj.tac.fpcf.analyses.string.VariableContext
import org.opalj.tac.fpcf.analyses.systemproperties.TriggeredSystemPropertiesAnalysisScheduler
import org.opalj.util.PerformanceEvaluation.time

/**
 * Determines which String values can be passed to calls of critical APIs such as the Java Reflection and MethodHandle
 * API and JavaX Crypto API.
 *
 * @author Maximilian Rüsch
 */
object APICallsStringAnalysis extends ProjectsAnalysisApplication {

    protected class APICallsConfig(args: Array[String]) extends MultiProjectAnalysisConfig(args)
        with CGBasedCommandLineConfig {

        val description =
            "Finds calls to methods provided by the Java Reflection and Crypto APIs and tries to resolve passed string values"

        private val analysisLevelArg =
            new AnalysisLevelArg(StringAnalysisArg.description, StringAnalysisArg.levels*) {
                override val defaultValue: Option[String] = Some("L2")
                override val withNone = false
            }

        private val onlyReflectionArg = new PlainArg[Boolean] {
            override val name: String = "onlyReflection"
            override val description: String = "Only identify reflection API calls"
            override val defaultValue: Option[Boolean] = Some(false)
        }

        private val onlyTradReflectionArg = new PlainArg[Boolean] {
            override val name: String = "onlyTraditionalReflection"
            override val description: String = "Only identify traditional Java Reflection API calls"
            override val defaultValue: Option[Boolean] = Some(false)
        }

        private val onlyMethodHandleArg = new PlainArg[Boolean] {
            override val name: String = "onlyMethodHandle"
            override val description: String = "Only identify MethdodHandle reflection API calls"
            override val defaultValue: Option[Boolean] = Some(false)
        }

        private val onlyCryptoArg = new PlainArg[Boolean] {
            override val name: String = "onlyCrypto"
            override val description: String = "Only identify Java Crypto API calls"
            override val defaultValue: Option[Boolean] = Some(false)
        }

        args(
            analysisLevelArg !,
            onlyReflectionArg ^ onlyTradReflectionArg ^ onlyMethodHandleArg ^ onlyCryptoArg
        )
        init()

        val analyses: Seq[FPCFAnalysisScheduler[?]] = {
            StringAnalysisArg.getAnalyses(apply(analysisLevelArg)).map(getScheduler(_, eager = false))
        }

        /**
         * Relevant method names, i.e., those methods from the APIs that have at least one string argument and shall be
         * considered by this analysis. The string are supposed to have the format as produced by [[buildFQMethodName]].
         */

        private val relevantReflectionMethodNames = List(
            "java.lang.Class#forName",
            "java.lang.ClassLoader#loadClass",
            "java.lang.Class#getField",
            "java.lang.Class#getDeclaredField",
            "java.lang.Class#getMethod",
            "java.lang.Class#getDeclaredMethod"
        )

        private val relevantMethodHandleMethodNames = List(
            "java.lang.invoke.MethodHandles$Lookup#findGetter",
            "java.lang.invoke.MethodHandles$Lookup#findStaticGetter",
            "java.lang.invoke.MethodHandles$Lookup#findSetter",
            "java.lang.invoke.MethodHandles$Lookup#findStaticSetter",
            "java.lang.invoke.MethodHandles$Lookup#findVirtual",
            "java.lang.invoke.MethodHandles$Lookup#findStatic",
            "java.lang.invoke.MethodHandles$Lookup#findSpecial",
            "java.lang.invoke.MethodHandles$Lookup#findConstructor"
        )

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

        private def getRelevantMethods: Set[String] = {
            var result = Set.empty[String]

            if (!(apply(onlyCryptoArg) || apply(onlyMethodHandleArg)))
                result ++= relevantReflectionMethodNames
            if (!(apply(onlyCryptoArg) || apply(onlyTradReflectionArg)))
                result ++= relevantMethodHandleMethodNames
            if (!(apply(onlyReflectionArg) || apply(onlyTradReflectionArg) || apply(onlyMethodHandleArg)))
                result ++= relevantCryptoMethodNames

            result
        }

        val relevantMethodNames = getRelevantMethods
    }

    protected type ConfigType = APICallsConfig

    protected def createConfig(args: Array[String]): APICallsConfig = new APICallsConfig(args)

    /**
     * Using a `declaringClass` and a `methodName`, this function returns a formatted version of the
     * fully-qualified method name, in the format [fully-qualified class name]#[method name]
     * where the separator for the fq class names is a dot, e.g., "java.lang.Class#forName".
     */
    @inline private final def buildFQMethodName(declaringClass: ReferenceType, methodName: String): String =
        s"${declaringClass.toJava}#$methodName"

    override protected def analyze(
        cp:             Iterable[File],
        analysisConfig: APICallsConfig,
        execution:      Int
    ): (Project[URL], ReportableAnalysisResult) = {
        // Stores a list of pairs where the first element corresponds to the entities passed to the analysis and the
        // second corresponds to the method name in which the entity occurred, i.e. a value in [[relevantMethodNames]].
        val detectedValues = ListBuffer.empty[(VariableContext, String)]

        val (project, _) = analysisConfig.setupProject(cp)
        implicit val (ps: PropertyStore, _) = analysisConfig.setupPropertyStore(project)

        val callGraphKey = analysisConfig(CallGraphArg).get

        callGraphKey.requirements(project)

        implicit val contextProvider: ContextProvider = callGraphKey.getTypeIterator(project)

        val manager = project.get(FPCFAnalysesManagerKey)
        val computeTac = project.get(ComputeTACAIKey)
        val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)

        @inline def isRelevant(methodName: String): Boolean = analysisConfig.relevantMethodNames.contains(methodName)

        /**
         * Taking the `declaringClass` and the `methodName` into consideration, this function checks
         * whether a method is relevant for this analysis.
         */
        @inline def isRelevantCall(declaringClass: ReferenceType, methodName: String): Boolean =
            isRelevant(buildFQMethodName(declaringClass, methodName))

        /**
         * Helper function that checks whether an array of [[Instruction]]s contains at least one
         * relevant method that is to be processed by `doAnalyze`.
         */
        def instructionsContainRelevantMethod(instructions: Array[Instruction]): Boolean = {
            instructions
                .exists {
                    case INVOKESTATIC(declClass, _, methodName, _) => isRelevantCall(declClass, methodName)
                    case INVOKEVIRTUAL(declClass, methodName, _)   => isRelevantCall(declClass, methodName)
                    case _                                         => false
                }
        }

        /**
         * This function is a wrapper function for processing a method. It checks whether the given
         * `method`, is relevant at all, and if so uses the given function `call` to call the
         * analysis using the property store, `ps`, to finally store it in the given `resultMap`.
         */
        def processFunctionCall(pc: Int, dm: DeclaredMethod, call: Call[V])(
            implicit stmts: Array[Stmt[V]]
        ): Unit = {
            if (isRelevantCall(call.declaringClass, call.name)) {
                // Loop through all parameters and start the analysis for those that take a string
                call.descriptor.parameterTypes.zipWithIndex.foreach {
                    case (ft, index) if ft == ClassType.String =>
                        val e = VariableContext(
                            pc,
                            call.params(index).asVar.toPersistentForm,
                            contextProvider.newContext(dm)
                        )
                        ps.force(e, StringConstancyProperty.key)
                        detectedValues.append((e, buildFQMethodName(call.declaringClass, call.name)))
                    case _ =>
                }
            }
        }

        def processStatements(tac: TACode[TACMethodParameter, V], dm: DeclaredMethod): Unit = {
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

        time {
            manager.runAll(
                callGraphKey.allCallGraphAnalyses(project) ++
                    analysisConfig.analyses ++
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

        val resultMap =
            Map.from(analysisConfig.relevantMethodNames.map((_, ListBuffer.empty[FinalEP[?, StringConstancyProperty]])))
        detectedValues.foreach {
            case (e, callName) =>
                resultMap(callName).append(ps(e, StringConstancyProperty.key).asFinal)
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

        (project, BasicReport(report))
    }

}
