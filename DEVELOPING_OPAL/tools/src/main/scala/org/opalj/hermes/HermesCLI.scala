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

/**
 * Executes all analyses to determine the representativeness of the given projects.
 *
 * @author Michael Eichberg
 */
object HermesCLI {

    object Hermes extends HermesCore {

        override def updateProjectData(f: ⇒ Unit): Unit = Hermes.synchronized { f }

        override def reportProgress(f: ⇒ Double): Unit = Hermes.synchronized { f }
    }

    private def showUsage(): Unit = {
        println("OPAL - Hermes")
        println("Parameters:")
        println("   -config <FileName> the configuration which lists a corpus' projects")
        println("   -statistics <FileName> the csv file to which the results should be exported")
        println("   -mapping <FileName> the properties file with the mapping between the feature")
        println("                       queries and the extracted features; format:")
        println("                       <FeatureQueryClass>=<FeatureID>(,<FeatureID>)*")
        println("                       where in FeatureIDs every \\ is replaced by \\\\")
        println("                                             ... new line ('\\n') is replaced by \\n")
        println("                                             ... , is replaced by \\,")
        println("   [-noProjectStatistics project statistics are not exported]")
        println()
        println("java org.opalj.hermes.HermesCLI -config <ConfigFile.json> -statistics <FileName>")
    }

    def main(args: Array[String]): Unit = {
        var configFile: String = null
        var statisticsFile: String = null
        var mappingFile: Option[String] = None
        var noProjectStatistics: Boolean = false

        var i = 0
        while (i < args.length) {
            args(i) match {
                case "-config" ⇒
                    i += 1; configFile = args(i)

                case "-statistics" ⇒
                    i += 1; statisticsFile = args(i)

                case "-mapping" ⇒
                    i += 1; mappingFile = Some(args(i))

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

                System.exit(0)
            }
        }
        Hermes.initialize(new File(configFile))
        Hermes.analyzeCorpus(runAsDaemons = false)
    }

}
