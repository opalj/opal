/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.opalj.support.debug

import java.io.File
import java.net.URL
import java.util.Locale

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigValueFactory
import org.opalj.ai.common.SimpleAIKey
import org.opalj.ai.domain.l2.DefaultPerformInvocationsDomainWithCFGAndDefUse
import org.opalj.br.Method
import org.opalj.br.analyses.Project
import org.opalj.bytecode.RTJar
import org.opalj.concurrent.NumberOfThreadsForCPUBoundTasks
import org.opalj.fpcf.seq.EPKSequentialPropertyStore
import org.opalj.fpcf.seq.EagerDependeeUpdateHandling
import org.opalj.fpcf.seq.LazyDependeeUpdateHandling
import org.opalj.fpcf.seq.PKESequentialPropertyStore
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.PropertyStoreKey
import org.opalj.fpcf.PropertyStoreKey.ConfigKeyPrefix
import org.opalj.fpcf.analyses._
import org.opalj.fpcf.analyses.escape.LazyInterProceduralEscapeAnalysis
import org.opalj.fpcf.analyses.purity.EagerL1PurityAnalysis
import org.opalj.fpcf.analyses.purity.EagerL2PurityAnalysis
import org.opalj.fpcf.par.ReactiveAsyncPropertyStore
import org.opalj.fpcf.properties.ClassImmutability
import org.opalj.fpcf.properties.EscapeProperty
import org.opalj.fpcf.properties.FieldLocality
import org.opalj.fpcf.properties.FieldMutability
import org.opalj.fpcf.properties.Purity
import org.opalj.fpcf.properties.ReturnValueFreshness
import org.opalj.fpcf.properties.ThrownExceptions
import org.opalj.fpcf.properties.ThrownExceptionsByOverridingMethods
import org.opalj.fpcf.properties.TypeImmutability
import org.opalj.fpcf.properties.VirtualMethodEscapeProperty
import org.opalj.fpcf.properties.VirtualMethodPurity
import org.opalj.fpcf.properties.VirtualMethodReturnValueFreshness
import org.opalj.log.ConsoleOPALLogger
import org.opalj.log.GlobalLogContext
import org.opalj.log.LogContext
import org.opalj.log.OPALLogger
import org.opalj.log.{Error ⇒ ErrorLogLevel}
import org.opalj.tac.DefaultTACAIKey
import org.opalj.util.Nanoseconds
import org.opalj.util.PerformanceEvaluation

/**
 * Evaluates the performance of the various property store implementations
 *
 * @author Andreas Muttscheller
 */
object PropertyStorePerformanceEvaluation {
    private val defaultConfigurationDescription = "default config"
    case class PropertyStoreEvaluation(
            className:                String,
            isParallel:               Boolean,
            configurationDescription: String               = defaultConfigurationDescription,
            setup:                    PropertyStore ⇒ Unit = { _ ⇒ }
    )

    val propertyStoreImplementationList: List[PropertyStoreEvaluation] = List(
        // *** Default Sequential Schedulings ***
        new PropertyStoreEvaluation(
            "org.opalj.fpcf.seq.PKESequentialPropertyStore",
            false
        ),
        new PropertyStoreEvaluation(
            "org.opalj.fpcf.seq.EPKSequentialPropertyStore",
            false
        ),

        // *** PKESequentialPropertyStore ***
        new PropertyStoreEvaluation(
            "org.opalj.fpcf.seq.PKESequentialPropertyStore",
            false,
            "EagerTrue",
            { ps ⇒
                val s = ps.asInstanceOf[PKESequentialPropertyStore]
                s.dependeeUpdateHandling = EagerDependeeUpdateHandling
                s.delayHandlingOfDependerNotification = true
            }
        ),
        new PropertyStoreEvaluation(
            "org.opalj.fpcf.seq.PKESequentialPropertyStore",
            false,
            "EagerFalse",
            { ps ⇒
                val s = ps.asInstanceOf[PKESequentialPropertyStore]
                s.dependeeUpdateHandling = EagerDependeeUpdateHandling
                s.delayHandlingOfDependerNotification = false
            }
        ),
        new PropertyStoreEvaluation(
            "org.opalj.fpcf.seq.PKESequentialPropertyStore",
            false,
            "LazyTrueTrueTrue",
            { ps ⇒
                val s = ps.asInstanceOf[PKESequentialPropertyStore]
                s.dependeeUpdateHandling = LazyDependeeUpdateHandling(
                    delayHandlingOfFinalDependeeUpdates = true,
                    delayHandlingOfNonFinalDependeeUpdates = true
                )
                s.delayHandlingOfDependerNotification = true
            }
        ),
        new PropertyStoreEvaluation(
            "org.opalj.fpcf.seq.PKESequentialPropertyStore",
            false,
            "LazyTrueTrueFalse",
            { ps ⇒
                val s = ps.asInstanceOf[PKESequentialPropertyStore]
                s.dependeeUpdateHandling = LazyDependeeUpdateHandling(
                    delayHandlingOfFinalDependeeUpdates = true,
                    delayHandlingOfNonFinalDependeeUpdates = true
                )
                s.delayHandlingOfDependerNotification = false
            }
        ),
        new PropertyStoreEvaluation(
            "org.opalj.fpcf.seq.PKESequentialPropertyStore",
            false,
            "LazyTrueFalseTrue",
            { ps ⇒
                val s = ps.asInstanceOf[PKESequentialPropertyStore]
                s.dependeeUpdateHandling = LazyDependeeUpdateHandling(
                    delayHandlingOfFinalDependeeUpdates = true,
                    delayHandlingOfNonFinalDependeeUpdates = false
                )
                s.delayHandlingOfDependerNotification = true
            }
        ),
        new PropertyStoreEvaluation(
            "org.opalj.fpcf.seq.PKESequentialPropertyStore",
            false,
            "LazyTrueFalseFalse",
            { ps ⇒
                val s = ps.asInstanceOf[PKESequentialPropertyStore]
                s.dependeeUpdateHandling = LazyDependeeUpdateHandling(
                    delayHandlingOfFinalDependeeUpdates = true,
                    delayHandlingOfNonFinalDependeeUpdates = false
                )
                s.delayHandlingOfDependerNotification = false
            }
        ),
        new PropertyStoreEvaluation(
            "org.opalj.fpcf.seq.PKESequentialPropertyStore",
            false,
            "LazyFalseTrueTrue",
            { ps ⇒
                val s = ps.asInstanceOf[PKESequentialPropertyStore]
                s.dependeeUpdateHandling = LazyDependeeUpdateHandling(
                    delayHandlingOfFinalDependeeUpdates = false,
                    delayHandlingOfNonFinalDependeeUpdates = true
                )
                s.delayHandlingOfDependerNotification = true
            }
        ),
        new PropertyStoreEvaluation(
            "org.opalj.fpcf.seq.PKESequentialPropertyStore",
            false,
            "LazyFalseTrueFalse",
            { ps ⇒
                val s = ps.asInstanceOf[PKESequentialPropertyStore]
                s.dependeeUpdateHandling = LazyDependeeUpdateHandling(
                    delayHandlingOfFinalDependeeUpdates = false,
                    delayHandlingOfNonFinalDependeeUpdates = true
                )
                s.delayHandlingOfDependerNotification = false
            }
        ),
        new PropertyStoreEvaluation(
            "org.opalj.fpcf.seq.PKESequentialPropertyStore",
            false,
            "LazyFalseFalseTrue",
            { ps ⇒
                val s = ps.asInstanceOf[PKESequentialPropertyStore]
                s.dependeeUpdateHandling = LazyDependeeUpdateHandling(
                    delayHandlingOfFinalDependeeUpdates = false,
                    delayHandlingOfNonFinalDependeeUpdates = false
                )
                s.delayHandlingOfDependerNotification = true
            }
        ),
        new PropertyStoreEvaluation(
            "org.opalj.fpcf.seq.PKESequentialPropertyStore",
            false,
            "LazyFalseFalseFalse",
            { ps ⇒
                val s = ps.asInstanceOf[PKESequentialPropertyStore]
                s.dependeeUpdateHandling = LazyDependeeUpdateHandling(
                    delayHandlingOfFinalDependeeUpdates = false,
                    delayHandlingOfNonFinalDependeeUpdates = false
                )
                s.delayHandlingOfDependerNotification = false
            }
        ),

        // *** EPKSequentialPropertyStore ***
        new PropertyStoreEvaluation(
            "org.opalj.fpcf.seq.EPKSequentialPropertyStore",
            false,
            "EagerTrue",
            { ps ⇒
                val s = ps.asInstanceOf[EPKSequentialPropertyStore]
                s.dependeeUpdateHandling = EagerDependeeUpdateHandling
                s.delayHandlingOfDependerNotification = true
            }
        ),
        new PropertyStoreEvaluation(
            "org.opalj.fpcf.seq.EPKSequentialPropertyStore",
            false,
            "EagerFalse",
            { ps ⇒
                val s = ps.asInstanceOf[EPKSequentialPropertyStore]
                s.dependeeUpdateHandling = EagerDependeeUpdateHandling
                s.delayHandlingOfDependerNotification = false
            }
        ),
        new PropertyStoreEvaluation(
            "org.opalj.fpcf.seq.EPKSequentialPropertyStore",
            false,
            "LazyTrueTrueTrue",
            { ps ⇒
                val s = ps.asInstanceOf[EPKSequentialPropertyStore]
                s.dependeeUpdateHandling = LazyDependeeUpdateHandling(
                    delayHandlingOfFinalDependeeUpdates = true,
                    delayHandlingOfNonFinalDependeeUpdates = true
                )
                s.delayHandlingOfDependerNotification = true
            }
        ),
        new PropertyStoreEvaluation(
            "org.opalj.fpcf.seq.EPKSequentialPropertyStore",
            false,
            "LazyTrueTrueFalse",
            { ps ⇒
                val s = ps.asInstanceOf[EPKSequentialPropertyStore]
                s.dependeeUpdateHandling = LazyDependeeUpdateHandling(
                    delayHandlingOfFinalDependeeUpdates = true,
                    delayHandlingOfNonFinalDependeeUpdates = true
                )
                s.delayHandlingOfDependerNotification = false
            }
        ),
        new PropertyStoreEvaluation(
            "org.opalj.fpcf.seq.EPKSequentialPropertyStore",
            false,
            "LazyTrueFalseTrue",
            { ps ⇒
                val s = ps.asInstanceOf[EPKSequentialPropertyStore]
                s.dependeeUpdateHandling = LazyDependeeUpdateHandling(
                    delayHandlingOfFinalDependeeUpdates = true,
                    delayHandlingOfNonFinalDependeeUpdates = false
                )
                s.delayHandlingOfDependerNotification = true
            }
        ),
        new PropertyStoreEvaluation(
            "org.opalj.fpcf.seq.EPKSequentialPropertyStore",
            false,
            "LazyTrueFalseFalse",
            { ps ⇒
                val s = ps.asInstanceOf[EPKSequentialPropertyStore]
                s.dependeeUpdateHandling = LazyDependeeUpdateHandling(
                    delayHandlingOfFinalDependeeUpdates = true,
                    delayHandlingOfNonFinalDependeeUpdates = false
                )
                s.delayHandlingOfDependerNotification = false
            }
        ),
        new PropertyStoreEvaluation(
            "org.opalj.fpcf.seq.EPKSequentialPropertyStore",
            false,
            "LazyFalseTrueTrue",
            { ps ⇒
                val s = ps.asInstanceOf[EPKSequentialPropertyStore]
                s.dependeeUpdateHandling = LazyDependeeUpdateHandling(
                    delayHandlingOfFinalDependeeUpdates = false,
                    delayHandlingOfNonFinalDependeeUpdates = true
                )
                s.delayHandlingOfDependerNotification = true
            }
        ),
        new PropertyStoreEvaluation(
            "org.opalj.fpcf.seq.EPKSequentialPropertyStore",
            false,
            "LazyFalseTrueFalse",
            { ps ⇒
                val s = ps.asInstanceOf[EPKSequentialPropertyStore]
                s.dependeeUpdateHandling = LazyDependeeUpdateHandling(
                    delayHandlingOfFinalDependeeUpdates = false,
                    delayHandlingOfNonFinalDependeeUpdates = true
                )
                s.delayHandlingOfDependerNotification = false
            }
        ),
        new PropertyStoreEvaluation(
            "org.opalj.fpcf.seq.EPKSequentialPropertyStore",
            false,
            "LazyFalseFalseTrue",
            { ps ⇒
                val s = ps.asInstanceOf[EPKSequentialPropertyStore]
                s.dependeeUpdateHandling = LazyDependeeUpdateHandling(
                    delayHandlingOfFinalDependeeUpdates = false,
                    delayHandlingOfNonFinalDependeeUpdates = false
                )
                s.delayHandlingOfDependerNotification = true
            }
        ),
        new PropertyStoreEvaluation(
            "org.opalj.fpcf.seq.EPKSequentialPropertyStore",
            false,
            "LazyFalseFalseFalse",
            { ps ⇒
                val s = ps.asInstanceOf[EPKSequentialPropertyStore]
                s.dependeeUpdateHandling = LazyDependeeUpdateHandling(
                    delayHandlingOfFinalDependeeUpdates = false,
                    delayHandlingOfNonFinalDependeeUpdates = false
                )
                s.delayHandlingOfDependerNotification = false
            }
        ),

        // *** ReactiveAsyncPropertyStore ***
        new PropertyStoreEvaluation(
            "org.opalj.fpcf.par.ReactiveAsyncPropertyStore",
            true
        )
    )

    private def buildProject(cp: String, testConfig: Config): Project[URL] = {
        implicit val logContext: LogContext = GlobalLogContext
        OPALLogger.updateLogger(GlobalLogContext, new ConsoleOPALLogger(true, ErrorLogLevel))

        val classFileReader = Project.JavaClassFileReader()
        import classFileReader.ClassFiles
        val fixtureFiles = new File(cp)
        val fixtureClassFiles = ClassFiles(fixtureFiles)

        Project(
            fixtureClassFiles,
            List.empty,
            libraryClassFilesAreInterfacesOnly = false,
            Traversable.empty,
            Project.defaultHandlerForInconsistentProjects,
            testConfig,
            logContext
        )

    }

    private def showUsage(error: Option[String]): Unit = {
        println("OPAL - PropertyStore evaluation")
        println("This program evaluates the performance of the different PropertyStore implementations.")
        error.foreach { e ⇒ println(); Console.err.println(e); println() }
        println("Parameters:")
        println("   -a <analysis> the name of the analysis to evaluate. Default: L1ThrownExceptionsAnalysis")
        println("   -minThreads <minThreads> The minimum threads the analysis should run. Default: 1")
        println("   -t <threads> specify a fixed number of threads to evaluate")
        println("   -cp <classpath> A path to a file or folder containing classfiles or a jar file. Default: rt.jar")
        println("   -allScheduling Test all scheduling variants of the PropertyStore. Without specifying --allScheduling, only the default schedulings are evaluated")
        println("   -ps <PropertyStore> Run the Evaluation for a specific PropertyStore Implementation. Can be used with --allSchedulings")
        println()
        println("Example:")
        println("   java org.opalj.support.debug.PropertyStorePerformanceEvaluation")
    }

    def main(args: Array[String]): Unit = {
        // Make sure decimals are separated by a point
        Locale.setDefault(new Locale("en", "US"))

        var cp: String = RTJar.getAbsolutePath
        var threads: Int = 0
        var analysis: String = "L1ThrownExceptionsAnalysis"
        var minThreads: Int = 1
        var allSchedulings: Boolean = false
        var specificPropertyStore: Option[String] = None
        var exportHistogram = false
        var i = 0
        while (i < args.length) {
            args(i) match {
                case "-a"               ⇒ { i += 1; analysis = args(i) }
                case "-cp"              ⇒ { i += 1; cp = args(i) }
                case "-t"               ⇒ { i += 1; threads = args(i).toInt }
                case "-minThreads"      ⇒ { i += 1; minThreads = args(i).toInt }
                case "-allSchedulings"  ⇒ { allSchedulings = true }
                case "-exportHistogram" ⇒ { exportHistogram = true }
                case "-ps"              ⇒ { i += 1; specificPropertyStore = Some(args(i)) }
                case "-h" | "--help"    ⇒ { showUsage(error = None); System.exit(0) }
                case arg                ⇒ { showUsage(Some(s"Unsupported: $arg")); System.exit(2) }
            }
            i += 1
        }

        val parallelismRuns: List[Int] = if (threads != 0) {
            List(threads)
        } else {
            (minThreads to (NumberOfThreadsForCPUBoundTasks * 2 + 1)).toList
        }

        val baseConfig: Config = ConfigFactory.load()
        val propertyStoreImplementation = ConfigKeyPrefix+"PropertyStoreImplementation"

        val psResults = propertyStoreImplementationList
            .filter(psI ⇒ allSchedulings || psI.configurationDescription == defaultConfigurationDescription)
            .filter(psI ⇒ specificPropertyStore.isEmpty || psI.className == specificPropertyStore.get)
            .map { psI ⇒
                var nanos: Nanoseconds = Nanoseconds.None

                val localParallelismRuns = if (psI.isParallel)
                    parallelismRuns
                else
                    List(1)

                val timings = localParallelismRuns.map { threads ⇒
                    println(s"Running analysis $analysis with $threads threads with PropertyStore ${psI.className} (${psI.configurationDescription})")
                    var project: Project[URL] = null
                    var run = 0

                    val result = PerformanceEvaluation.time(10, 15, 3, {
                        // *** Setup - NOT TIMED ***
                        val testConfig = baseConfig.
                            withValue(propertyStoreImplementation, ConfigValueFactory.fromAnyRef(psI.className))
                        project = buildProject(cp, testConfig)

                        project.getOrCreateProjectInformationKeyInitializationData(
                            SimpleAIKey,
                            (m: Method) ⇒ {
                                new DefaultPerformInvocationsDomainWithCFGAndDefUse(project, m)
                            }
                        )

                        // Pre analysis computations
                        analysis match {
                            case "L2PurityAnalysisEagerTAC" ⇒
                                val tac = project.get(DefaultTACAIKey)
                                project.parForeachMethodWithBody() { m ⇒ tac(m.method) }
                            case _ ⇒
                        }
                    }, {
                        // *** Analysis - TIMED ***

                        // Set the number of threads
                        PropertyStoreKey.parallelismLevel = threads
                        val propertyStore = project.get(PropertyStoreKey)
                        psI.setup(propertyStore)

                        // Run the setup phase and make all PropertyKeys used in this evaluation known
                        propertyStore.setupPhase(Set(
                            ClassImmutability.key,
                            TypeImmutability.key,
                            FieldMutability.key,
                            VirtualMethodPurity.key,
                            ReturnValueFreshness.key,
                            VirtualMethodReturnValueFreshness.key,
                            EscapeProperty.key,
                            VirtualMethodEscapeProperty.key,
                            FieldLocality.key,
                            Purity.key,
                            ThrownExceptions.key,
                            ThrownExceptionsByOverridingMethods.key
                        ))

                        analysis match {
                            case "TypeImmutabilityAnalysis" ⇒
                                EagerTypeImmutabilityAnalysis.start(project, propertyStore)
                            case "L0FieldMutabilityAnalysis" ⇒
                                EagerL0FieldMutabilityAnalysis.start(project, propertyStore)
                            case "L1FieldMutabilityAnalysis" ⇒
                                EagerL1FieldMutabilityAnalysis.start(project, propertyStore)
                            case "L0PurityAnalysis" ⇒
                                LazyL0FieldMutabilityAnalysis.startLazily(project, propertyStore)
                                EagerL0PurityAnalysis.start(project, propertyStore)
                            case "L1PurityAnalysis" ⇒
                                LazyL1FieldMutabilityAnalysis.startLazily(project, propertyStore)
                                LazyVirtualMethodPurityAnalysis.startLazily(project, propertyStore)
                                EagerL1PurityAnalysis.start(project, propertyStore)
                            case "L2PurityAnalysis" | "L2PurityAnalysisEagerTAC" ⇒
                                LazyL1FieldMutabilityAnalysis.startLazily(project, propertyStore)
                                LazyVirtualMethodPurityAnalysis.startLazily(project, propertyStore)
                                LazyReturnValueFreshnessAnalysis.startLazily(project, propertyStore)
                                LazyVirtualReturnValueFreshnessAnalysis.startLazily(project, propertyStore)
                                LazyInterProceduralEscapeAnalysis.startLazily(project, propertyStore)
                                LazyVirtualCallAggregatingEscapeAnalysis.startLazily(project, propertyStore)
                                LazyFieldLocalityAnalysis.startLazily(project, propertyStore)
                                EagerL2PurityAnalysis.start(project, propertyStore)
                            case "L1ThrownExceptionsAnalysis" ⇒
                                LazyVirtualMethodThrownExceptionsAnalysis.startLazily(project, propertyStore)
                                EagerL1ThrownExceptionsAnalysis.start(project, propertyStore)
                        }

                        propertyStore.waitOnPhaseCompletion()
                        propertyStore
                    }, true) {
                        case (c, s) ⇒
                            nanos = Nanoseconds(s.map(_.timeSpan).sum / s.size)
                            run += 1
                            println(f"Run ${run}%2s: ${c.toSeconds} (${nanos.toSeconds} avg) - Times considered: [${s.map(_.toSeconds).mkString(", ")}]")
                    }

                    if (exportHistogram) {
                        result match {
                            case raPs: ReactiveAsyncPropertyStore ⇒
                                val p = org.opalj.io.write(
                                    "numberDependencies,total\n"+
                                        raPs.dependencyCounter.map {
                                            case (k, v) ⇒ s"$k,${v.get}"
                                        }.mkString("\n"),
                                    s"hist_t$threads",
                                    ".csv"
                                )
                                println(s"Histogram written to: ${p.toFile.getAbsolutePath}")
                            case _ ⇒
                        }
                    }

                    println(result)

                    println(s"Running analysis $analysis with $threads threads with PropertyStore ${psI.className} (${psI.configurationDescription})...done. Took average ${nanos.toSeconds.toString}")
                    threads -> nanos
                }

                psI -> timings
            }

        psResults.foreach { r ⇒
            println(s"Using ${r._1.className} (${r._1.configurationDescription})")
            r._2.foreach { timings ⇒
                println(f"\t\tThreads: ${timings._1}%-3s ${timings._2.toString}%20s ${timings._2.toSeconds.toString}%20s")
            }
        }

        val p = org.opalj.io.write(
            "PropertyStore;Config;Threads;avgTimeInNanos;avgTimeInSeconds\n"+
                psResults.flatMap { r ⇒
                    println(s"Using ${r._1.className} (${r._1.configurationDescription})")
                    r._2.map { timings ⇒
                        s"${r._1.className};${r._1.configurationDescription};${timings._1};${timings._2.toString(false)};${timings._2.toSeconds.toString(false)}"
                    }
                }.mkString("\n"),
            s"PropertyStoreEvaluationResults_${analysis}_",
            ".csv"
        )
        println(s"Results written to: ${p.toFile.getAbsolutePath}")

    }
}
