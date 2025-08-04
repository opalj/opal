/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package support
package info

import scala.language.postfixOps

import java.io.File
import java.net.URL

import org.opalj.br.DeclaredMethod
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectsAnalysisApplication
import org.opalj.br.collection.TypesSet
import org.opalj.br.fpcf.ContextProviderKey
import org.opalj.br.fpcf.cli.MultiProjectAnalysisConfig
import org.opalj.br.fpcf.properties.{ThrownExceptions => ThrownExceptionsProperty}
import org.opalj.br.fpcf.properties.Context
import org.opalj.cli.AnalysisLevelArg
import org.opalj.cli.IndividualArg
import org.opalj.fpcf.FPCFAnalysesManagerKey
import org.opalj.fpcf.PropertyKind
import org.opalj.fpcf.PropertyStoreBasedCommandLineConfig
import org.opalj.fpcf.SomeEPS
import org.opalj.tac.fpcf.analyses.EagerL1ThrownExceptionsAnalysis
import org.opalj.util.Nanoseconds
import org.opalj.util.PerformanceEvaluation.time

/**
 * Prints out the information about the exceptions thrown by methods.
 *
 * @author Michael Eichberg
 * @author Andreas Muttschelller
 */
object ThrownExceptions extends ProjectsAnalysisApplication {

    protected class ThrownExceptionsConfig(args: Array[String]) extends MultiProjectAnalysisConfig(args)
        with PropertyStoreBasedCommandLineConfig {
        val description = "Computes the set of the exceptions (in)directly thrown by methods"

        private val analysisLevelArg =
            new AnalysisLevelArg("Thrown-exceptions analysis level", Seq("L0" -> "L0", "L1" -> "L1"): _*) {
                override val defaultValue: Option[String] = Some("L1")
                override val withNone = false
            }

        args(
            analysisLevelArg !,
            IndividualArg
        )
        init()

        val analysisLevel: String = apply(analysisLevelArg)
    }

    protected type ConfigType = ThrownExceptionsConfig

    protected def createConfig(args: Array[String]): ThrownExceptionsConfig = new ThrownExceptionsConfig(args)

    override protected def analyze(
        cp:             Iterable[File],
        analysisConfig: ThrownExceptionsConfig,
        execution:      Int
    ): (Project[URL], BasicReport) = {
        val (project, _) = analysisConfig.setupProject(cp)
        val (ps, _) = analysisConfig.setupPropertyStore(project)
        var executionTime: Nanoseconds = Nanoseconds.None
        time {
            if (analysisConfig.analysisLevel == "L0") {
                // We are relying on/using the "FallbackAnalysis":
                ps.setupPhase(Set.empty[PropertyKind]) // <= ALWAYS REQUIRED.
                // We have to query the properties...
                val declaredMethods = project.get(DeclaredMethodsKey)
                val contextProvider = project.get(ContextProviderKey)
                project.allMethods foreach { m =>
                    ps.force(contextProvider.newContext(declaredMethods(m)), ThrownExceptionsProperty.key)
                }
                ps.waitOnPhaseCompletion()
                ps
            } else /* if no analysis level is specified or L1 */ {
                project.get(FPCFAnalysesManagerKey).runAll(
                    EagerL1ThrownExceptionsAnalysis
                )
            }
        } { t => executionTime = t }

        val allMethods = ps.entities(ThrownExceptionsProperty.key).iterator.to(Iterable)
        val (epsNotThrowingExceptions, otherEPS) =
            allMethods.partition(_.ub.throwsNoExceptions)
        val epsThrowingExceptions = otherEPS.filter(eps => eps.lb.types != TypesSet.SomeException)

        val methodsThrowingExceptions = epsThrowingExceptions.map(_.e.asInstanceOf[Context])
        val privateMethodsThrowingExceptionsCount = methodsThrowingExceptions.count(_.method.definedMethod.isPrivate)

        val privateMethodsNotThrowingExceptions =
            epsNotThrowingExceptions.map(_.e.asInstanceOf[Context]).filter(_.method.definedMethod.isPrivate)

        val perMethodsReport =
            if (!analysisConfig.get(IndividualArg, false))
                ""
            else {
                val epsThrowingExceptionsByClassFile =
                    epsThrowingExceptions groupBy (_.e.asInstanceOf[Context].method.definedMethod.classFile)
                epsThrowingExceptionsByClassFile.map { e =>
                    val (cf, epsThrowingExceptionsPerMethod) = e
                    cf.thisType.toJava + "{" +
                        epsThrowingExceptionsPerMethod.map { (eps: SomeEPS) =>
                            val m: DeclaredMethod = eps.e.asInstanceOf[Context].method
                            val ThrownExceptionsProperty(types) = eps.ub
                            m.descriptor.toJava(m.name) + " throws " + types.toString
                        }.toList.sorted.mkString("\n\t\t", "\n\t\t", "\n") +
                        "}"

                }.mkString("\n", "\n", "\n")
            }

        val psStatistics =
            ps.statistics.map(e => e._1 + ": " + e._2).mkString("Property Store Statistics:\n\t", "\n\t", "\n")

        val analysisStatistics: String =
            "\nStatistics:\n" +
                "#methods with a thrown exceptions property: " +
                s"${allMethods.size} (${project.methodsCount})\n" +
                "#methods with exceptions information more precise than _ <: Throwable: " +
                s"${methodsThrowingExceptions.size + epsNotThrowingExceptions.size}\n" +
                s" ... #exceptions == 0: ${epsNotThrowingExceptions.size}\n" +
                s" ... #exceptions == 0 and private: ${privateMethodsNotThrowingExceptions.size}\n" +
                s" ... #exceptions >  0 and private: $privateMethodsThrowingExceptionsCount\n" +
                s"execution time: ${executionTime.toSeconds}\n"

        (
            project,
            BasicReport(
                psStatistics +
                    "\nThrown Exceptions Information:\n" +
                    perMethodsReport + "\n" +
                    ps.toString(printProperties = false) +
                    analysisStatistics
            )
        )
    }
}
