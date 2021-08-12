/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package hermes

import java.io.File
import java.util.concurrent.CountDownLatch

import scala.io.Source
import scala.reflect.io.Directory

import org.opalj.io.processSource

/**
 * Executes all analyses to determine the representativeness of the given projects
 * ([[https://github.com/stg-tud/opal/blob/develop/TOOLS/hermes/src/main/resources/org/opalj/hermes/HermesCLI.txt see HermesCLI.txt for further details]]).
 *
 * @author Michael Eichberg
 */
object HermesCLI {

    object Hermes extends HermesCore {
        override def updateProjectData(f: => Unit): Unit = Hermes.synchronized { f }
        override def reportProgress(f: => Double): Unit = Hermes.synchronized { f }
    }

    final val usage = {
        val hermesCLIInputStream = this.getClass.getResourceAsStream("HermesCLI.txt")
        processSource(Source.fromInputStream(hermesCLIInputStream)) { s =>
            s.getLines().mkString("\n")
        }
    }

    private def showUsage(): Unit = println(usage)

    def main(args: Array[String]): Unit = {
        var configFile: String = null
        var statisticsFile: String = null
        var mappingFile: Option[String] = None
        var noProjectStatistics: Boolean = false
        var locationsDir: String = null

        var i = 0
        while (i < args.length) {
            args(i) match {
                case "-config" =>
                    i += 1
                    configFile = args(i)

                case "-statistics" =>
                    i += 1
                    statisticsFile = args(i)

                case "-mapping" =>
                    i += 1
                    mappingFile = Some(args(i))

                case "-noProjectStatistics" =>
                    i += 1
                    noProjectStatistics = true

                case "-writeLocations" =>
                    i += 1
                    locationsDir = args(i)

                case arg =>
                    Console.err.println(s"Unknown parameter $arg.")
                    showUsage()
                    System.exit(2)
            }
            i += 1
        }
        if (configFile == null || statisticsFile == null) {
            Console.err.println("Missing config file and/or statistics file.")
            showUsage()
            System.exit(1)
        }

        val waitOnFinished = new CountDownLatch(1)
        Hermes.analysesFinished.addListener { (_, _, isFinished) =>
            if (isFinished) {
                val theStatisticsFile = new File(statisticsFile).getAbsoluteFile()
                Hermes.exportStatistics(theStatisticsFile, !noProjectStatistics)
                println("Wrote statistics: "+theStatisticsFile)

                if (locationsDir ne null) {
                    val theLocationsDir = Directory(new File(locationsDir))
                    Hermes.exportLocations(theLocationsDir)
                    println("Wrote locations: "+theLocationsDir)
                } else {
                    println("Skip writing locations, not specified.")
                }

                mappingFile.foreach { mappingFile =>
                    val theMappingFile = new File(mappingFile).getAbsoluteFile()
                    Hermes.exportMapping(theMappingFile)
                    println("Wrote mapping: "+theMappingFile)
                }

                waitOnFinished.countDown()
            }
        }
        Hermes.initialize(new File(configFile))
        Hermes.analyzeCorpus(runAsDaemons = true)
        waitOnFinished.await() // we will not return until we have finished the analysis
        println("Hermes finished.")
    }

}
