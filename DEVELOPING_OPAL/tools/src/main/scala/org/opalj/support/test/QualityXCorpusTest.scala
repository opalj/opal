/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package support
package test

import java.io.File

import org.opalj.support.info.Immutability

object QualityXCorpusTest {
    def findBinDirectory(systemDir: File): Option[File] = {
        val subDirs = systemDir.listFiles().filter(_.isDirectory)

        subDirs.flatMap { subDir =>
            val binDir = new File(subDir, "bin")
            if (binDir.exists() && binDir.isDirectory) {
                Some(binDir)
            } else {
                None
            }
        }.headOption
    }

    def main(args: Array[String]): Unit = {
        val systemsRootPath = "C:/Users/vwysl/Desktop/Bachelorarbeit/Evaluation/QualitasCorpus/Systems" // Path to the Systems directory
        val resultsFolderPath = "C:/Users/vwysl/Desktop/Bachelorarbeit/Evaluation/results" // Where to store results

        val resultsDir = new File(resultsFolderPath)
        if (!resultsDir.exists()) {
            resultsDir.mkdir()
        }

        val systemsRoot = new File(systemsRootPath)
        val systemDirs = systemsRoot.listFiles().filter(_.isDirectory)

        println(s"Found ${systemDirs.length} systems to analyze")

        systemDirs.foreach { systemDir =>
            try {
                val systemName = systemDir.getName
                println(s"\nAnalyzing system: $systemName")

                findBinDirectory(systemDir) match {
                    case Some(binPath) => {
                        println(s"Found bin directory at: ${binPath.getAbsolutePath}")

                        val analysisParams = Array(
                            "-cp",
                            binPath.getAbsolutePath,
                            "-resultFolder",
                            resultsDir.getAbsolutePath,
                            "-analysis",
                            "All",
                            "-threads",
                            "12",
                            "-level",
                            "2",
                            "-analysisName",
                            systemName
                        )

                        println(s"Running analysis on ${binPath.getAbsolutePath}")
                        Immutability.main(analysisParams)

                        println(s"Completed analysis for $systemName")
                    }
                    case None =>
                        println(s"Warning: No bin directory found for system $systemName")
                }
            } catch {
                case e: Exception =>
                    println(s"Error analyzing system ${systemDir.getName}")
                    e.printStackTrace()
            }
        }

        println("\nAnalysis complete for all systems")
    }
}
