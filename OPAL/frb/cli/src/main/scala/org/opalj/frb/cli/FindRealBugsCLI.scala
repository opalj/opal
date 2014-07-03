/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
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
package frb
package cli

import br._
import br.analyses._
import br.reader._
import util.PerformanceEvaluation
import java.net.URL
import java.text.DecimalFormat

/**
 * The FindRealBugs command line interface.
 *
 * @author Florian Brandherm
 * @author Peter Spieler
 * @author Daniel Klauer
 */
object FindRealBugsCLI extends ProgressListener {
    import FindRealBugs._

    // TODO(future): Read the Wiki URL from a config file
    val wikiUrlPrefix = "https://bitbucket.org/delors/opal/wiki/FindREALBugs/"

    def getAnalysisWikiUrl(analysis: Analysis): String = {
        wikiUrlPrefix + analysis.title
    }

    /**
     * Displays help output and aborts the program. Optionally shows an error message.
     *
     * @param errorMessage Error message to show, if non-empty string (default: empty
     * string).
     */
    private def printUsageAndExit(errorMessage: String = "") {
        if (errorMessage.length > 0) {
            println(Console.RED+"error: "+Console.RESET + errorMessage)
        }
        println("usage: java "+this.getClass().getName()+" <options> <input files...>")
        println("input files: *.class, *.jar, or directories containing either")
        println("options:")
        println("  -l=<input file>")
        println("      Add library input file (won't be analyzed for bugs, but may help")
        println("      making the analysis more accurate)")
        println("  --config=<file>")
        println("      Run only the analysis listed as enabled in that file.")
        println("      Format: Java properties file:")
        println("      keys = full names of FindRealBugs.Analysis-compatible classes")
        println("      values = 'yes' (enabled) or 'no' (disabled)")
        println("  --write-default-config=<file>")
        println("      Write the default list of built-in analysis into a file and exit.")
        sys.exit(1)
    }

    var inputFiles = Set[String]()
    var libraryInputFiles = Set[String]()
    var analysisClassNames = Set[String]()

    private def parseArgs(args: Array[String]) {
        var i = 0
        while (i < args.size) {
            val arg = args(i)
            if (arg.startsWith("-")) {
                val option = arg.split("=", 2)
                option(0) match {
                    case "-l" ⇒
                        libraryInputFiles += option(1)
                    case "--config" ⇒
                        analysisClassNames ++=
                            loadRegistry(new java.io.File(option(1))).
                            filter {
                                case (className, enabled) ⇒ enabled
                            }.map {
                                case (className, enabled) ⇒ className
                            }
                    case "--write-default-config" ⇒
                        saveRegistry(new java.io.File(option(1)),
                            builtInAnalysisClassNames.map(name ⇒ name -> true).toMap)
                        sys.exit(0)
                    case _ ⇒
                        printUsageAndExit("unknown command line option: "+arg)
                }
            } else {
                inputFiles += arg
            }
            i += 1
        }
    }

    /**
     * Entry point: Handles console input, runs analyses on the input files,
     * and prints the resulting reports to the console.
     *
     * @param args List of command line arguments.
     */
    def main(args: Array[String]) {
        parseArgs(args)

        if (inputFiles.size == 0) {
            printUsageAndExit("no input files")
        }

        // If the user didn't specify any analyses, use the default built-in ones
        if (analysisClassNames.size == 0) {
            analysisClassNames ++= builtInAnalysisClassNames
        }

        val analysisCreators = analysisClassNames.map { name ⇒
            (() ⇒ loadAnalysis(name))
        }

        //
        // Load .class files into a `Project`
        //
        println("Reading class files:")

        def loadClassFilesForCLI(
            fileNames: Iterable[String],
            loadAsLibrary: Boolean): Iterable[(ClassFile, URL)] = {
            loadClassFiles(
                fileNames,
                loadAsLibrary,
                error ⇒ printUsageAndExit(error),
                file ⇒ println("\t"+file.toString())
            )
        }

        val classFiles = loadClassFilesForCLI(inputFiles, false)
        val libraryClassFiles = loadClassFilesForCLI(libraryInputFiles, true)

        // Create the project
        val project = Project(classFiles, libraryClassFiles)
        println("\tClass files loaded: "+project.classFilesCount)

        //
        // Execute analyses on the `Project`
        //
        val timer = new PerformanceEvaluation
        val allResults = timer.time('analysis) {
            analyze(project, analysisCreators, Some(this))
        }
        val realSeconds = PerformanceEvaluation.ns2sec(timer.getTime('analysis))

        println("sum: "+secondsToString(analysesTotalSeconds)+", "+
            "real time: "+secondsToString(realSeconds))

        allResults.foreach {
            case (analysis, reports) ⇒
                // Display report's console messages, separated by newlines, with the
                // analysis description and wiki URL at the bottom of each analysis' list
                // of reports.
                println(reports.map(_.consoleReport(urlToLocationIdentifier)).
                    mkString("\n", "\n",
                        "\n"+Console.BLUE+"description: "+Console.RESET
                            + getAnalysisWikiUrl(analysis)))
        }

        // Display how many reports came from every analysis.
        println("\nNumber of reports per analysis:")
        allResults.foreach {
            case (analysis, reports) ⇒
                println(analysis.title+" "+reports.size)
        }
    }

    var analysesTotalSeconds: Double = 0
    val progressLock = new Object

    /**
     * Builds a nice string to display time in the console.
     */
    private def secondsToString(seconds: Double): String = {
        val formatter = new DecimalFormat("#.###")
        Console.YELLOW + formatter.format(seconds)+" seconds"+Console.RESET
    }

    /**
     * Prints a progress message.
     */
    private def printProgress(
        color: String,
        position: Int,
        total: Int,
        status: String,
        message: String) {
        println(color+"["+position+"/"+total+"]"+Console.RESET+
            " "+status+" "+message)
    }

    /**
     * Called at the beginning of each analysis.
     */
    override def analysisStarted(analysis: Analysis, position: Int, total: Int) {
        printProgress(Console.GREEN, position, total, "running",
            "               \t"+analysis.title)
    }

    /**
     * Called at the end of each analysis.
     */
    override def analysisCompleted(
        analysis: Analysis,
        position: Int,
        total: Int,
        seconds: Double,
        reports: AnalysisReports) {
        progressLock.synchronized {
            analysesTotalSeconds += seconds
        }
        printProgress(Console.RED, position, total, "finished",
            secondsToString(seconds)+"\t"+analysis.title+", "+
                reports.size+" reports.")
    }
}
