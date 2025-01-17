///* BSD 2-Clause License - see OPAL/LICENSE for details. */
//package org.opalj.support.test
//
//import java.io.File
//import java.io.PrintWriter
//
//import org.opalj.support.info.Immutability
//import org.opalj.util.PerformanceEvaluation.time
//import org.opalj.util.Seconds
//
//object QualityXCorpusTest {
//    def findBinDirectory(systemDir: File): Option[File] = {
//        val subDirs = systemDir.listFiles().filter(_.isDirectory)
//
//        subDirs.flatMap { subDir =>
//            val binDir = new File(subDir, "bin")
//            if (binDir.exists() && binDir.isDirectory) {
//                Some(binDir)
//            } else {
//                None
//            }
//        }.headOption
//    }
//
//    def main(args: Array[String]): Unit = {
//        val systemsRootPath = "C:/Users/vwysl/Desktop/Bachelorarbeit/Evaluation/QualitasCorpus/Systems"
//        val resultsFolderPath = "C:/Users/vwysl/Desktop/Bachelorarbeit/Evaluation/results"
//
//        val resultsDir = new File(resultsFolderPath)
//        if (!resultsDir.exists()) {
//            resultsDir.mkdir()
//        }
//
//        val systemsRoot = new File(systemsRootPath)
//        val systemDirs = systemsRoot.listFiles().filter(_.isDirectory)
//
//        println(s"Found ${systemDirs.length} systems to analyze")
//
//        var times: Map[String, Seconds] = Map.empty
//
//        var i = 1
//        time {
//            systemDirs.foreach { systemDir =>
//                try {
//                    if (i < 0) {
//                        val systemName = systemDir.getName
//                        println(s"\nAnalyzing system: $systemName")
//                        println("SKIP")
//                        i += 1
//                    } else {
//                        val systemName = systemDir.getName
//                        println(s"\nAnalyzing system: $systemName")
//
//                        findBinDirectory(systemDir) match {
//                            case Some(binPath) => {
//                                println(s"Found bin directory at: ${binPath.getAbsolutePath}")
//
//                                val analysisParams = Array(
//                                    "-cp",
//                                    binPath.getAbsolutePath,
//                                    "-resultFolder",
//                                    resultsDir.getAbsolutePath,
//                                    "-threads",
//                                    "12",
//                                    "-analysisName",
//                                    systemName
//                                )
//
//                                println(s"Running analysis on ${binPath.getAbsolutePath}")
//                                time {
//                                    Immutability.main(analysisParams)
//                                } { t => times = times + (systemName -> t.toSeconds) }
//                                println(s"Completed analysis for $systemName")
//                            }
//                            case None =>
//                                println(s"Warning: No bin directory found for system $systemName")
//                        }
//                    }
//                } catch {
//                    case e: Exception =>
//                        println(s"Error analyzing system ${systemDir.getName}")
//                        e.printStackTrace()
//                }
//            }
//            System.gc()
//        } { t => times = times + ("All_Analysis" -> t.toSeconds) }
//
//        println("\nAnalysis complete for all systems")
//        val writer = new PrintWriter("C:/Users/vwysl/Desktop/Bachelorarbeit/Evaluation/results/times.txt")
//        for ((name, time) <- times) {
//            val line = s"$name | $time"
//            println(line)
//            writer.println(line)
//        }
//        writer.close()
//    }
//}
