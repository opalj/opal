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
package org.opalj
package hermes

import java.io.File
import java.util.concurrent.CountDownLatch

import scala.io.Source
import org.opalj.io.processSource

/**
 * Executes all analyses to determine the representativeness of the given projects
 * ([[https://bitbucket.org/delors/opal/src/HEAD/DEVELOPING_OPAL/tools/src/main/resources/org/opalj/hermes/HermesCLI.txt?at=develop see HermesCLI.txt for further details]]).
 *
 * @author Michael Eichberg
 */
object HermesCLI {

    final val usage = {
        val hermesCLIInputStream = this.getClass.getResourceAsStream("HermesCLI.txt")
        processSource(Source.fromInputStream(hermesCLIInputStream)) { s ⇒
            s.getLines().mkString("\n")
        }
    }

    private def showUsage(): Unit = println(usage)

    def main(args: Array[String]): Unit = {
        var configFile: String = null
        var statisticsFile: String = null
        var mappingFile: Option[String] = None
        var noProjectStatistics: Boolean = false

        var i = 0
        while (i < args.length) {
            args(i) match {
                case "-config" ⇒
                    i += 1
                    configFile = args(i)

                case "-statistics" ⇒
                    i += 1
                    statisticsFile = args(i)

                case "-mapping" ⇒
                    i += 1
                    mappingFile = Some(args(i))

                case "-noProjectStatistics" ⇒
                    noProjectStatistics = true

                case arg ⇒
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

        object Hermes extends HermesCore {
            override def updateProjectData(f: ⇒ Unit): Unit = Hermes.synchronized { f }
            override def reportProgress(f: ⇒ Double): Unit = Hermes.synchronized { f }
        }
        val waitOnFinished = new CountDownLatch(1)
        Hermes.analysesFinished onChange { (_, _, isFinished) ⇒
            if (isFinished) {
                val theStatisticsFile = new File(statisticsFile).getAbsoluteFile()
                Hermes.exportStatistics(theStatisticsFile, !noProjectStatistics)
                println("Wrote statistics: "+theStatisticsFile)

                mappingFile.foreach { mappingFile ⇒
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
        println("Analysis finished.")
    }

}
