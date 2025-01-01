/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package support
package test

import org.opalj.support.info.Immutability

object OneFileRunner {
    case class AnalysisConfig(
        level:     Int,
        callGraph: String
    )

    private val levels = Set(0, 1, 2)
    private val callGraphs = Set("CHA", "RTA", "XTA", "PointsTo")

    private val configs = for {
        level <- levels
        callGraph <- callGraphs
    } yield AnalysisConfig(level, callGraph)

    def main(args: Array[String]): Unit = {
        configs.foreach { config =>
            try {
                println(s"\nRunning analysis with Level ${config.level}, CallGraph: ${config.callGraph}")

                if (false) {
                    println("SKIP")
                } else {
                    // Configure analysis parameters
                    val analysisParams = Array(
                        "-cp",
                        "C:/Users/vwysl/Documents/opal/OPAL/bi/target/scala-2.13/resource_managed/test/immutable.jar",
                        "-threads",
                        "16",
//                        "-level",
//                        config.level.toString,
                        "-callGraph",
                        config.callGraph,
                    )

                    println(s"Running Immutability analysis...")
                    Immutability.main(analysisParams)
                }
                println(s"Completed analysis for configuration: Level ${config.level}, CallGraph ${config.callGraph}")
            } catch {
                case e: Exception =>
                    println(s"Error analyzing with Level ${config.level}, CallGraph ${config.callGraph}")
                    e.printStackTrace()
            }
        }
        println("\nAnalysis complete for all configurations")
        println(s"Results are stored in: C:/Users/vwysl/Desktop/Bachelorarbeit/Evaluation/results/ImmutabilityTestResults")
    }
}
