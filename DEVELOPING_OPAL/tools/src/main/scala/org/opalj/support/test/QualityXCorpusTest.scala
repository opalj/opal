/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package support
package test

import java.io.File

import org.opalj.support.info.Immutability

object QualityXCorpusTest {
    def findBinDirectory(systemDir: File): Option[File] = {
        // First level - get all subdirectories
        val subDirs = systemDir.listFiles().filter(_.isDirectory)

        // For each subdirectory, look for a bin folder
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
        // Configuration
         val systemsRootPath = "C:/Users/vwysl/Desktop/Bachelorarbeit/Evaluation/QualitasCorpus/Systems"  // Path to the Systems directory
         val resultsFolderPath = "C:/Users/vwysl/Desktop/Bachelorarbeit/Evaluation/results" // Where to store results

        // Create results directory if it doesn't exist
        val resultsDir = new File(resultsFolderPath)
        if (!resultsDir.exists()) {
            resultsDir.mkdir()
        }

        // Get all system directories
        val systemsRoot = new File(systemsRootPath)
        val systemDirs = systemsRoot.listFiles().filter(_.isDirectory)

        println(s"Found ${systemDirs.length} systems to analyze")

        // Process each system
        systemDirs.foreach { systemDir =>
            try {
                val systemName = systemDir.getName
                println(s"\nAnalyzing system: $systemName")

                // Find the bin directory
                findBinDirectory(systemDir) match {
                    case Some(binPath) => {
                        println(s"Found bin directory at: ${binPath.getAbsolutePath}")

                        // Configure analysis parameters
                        val analysisParams = Array(
                            "-cp",
                            binPath.getAbsolutePath,
                            "-resultFolder",
                            resultsDir.getAbsolutePath,
                            "-analysis",
                            "All", // Run all analyses
                            "-threads",
                            "12", // Use 12 threads
                            "-level",
                            "2", // Use level 2 analysis
                            "-analysisName",
                            systemName // Use system name for output files
                        )

                        // Run the analysis
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
