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
package de.tud.cs.st
package bat
package findrealbugs

import resolved._
import resolved.analyses._
import resolved.reader._
import java.io.File
import java.io.IOException
import java.net.URL
import java.lang.management.ManagementFactory
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
        println("  -i=<analysis>    Ignore and do not run the specified analysis")
        println("  -l=<input file>  Load an input file as Library (This file will not "+
            "be searched for bugs)")
        println("  -c=<configuration file> read the list of disabled anlyses from a "+
            "configuration file")
        println("                          if this is used, no -i= arguments are allowed")
        sys.exit(1)
    }

    /**
     * List of analyses to run. By default, all analyses are enabled.
     */
    var analysesToRun: Set[String] = builtInAnalyses.keySet

    /**
     * Disable a certain analysis. Used to implement the -i command line option.
     */
    private def disableAnalysis(name: String) {
        analysesToRun = analysesToRun.filter(_ != name)
    }

    /**
     * Entry point: Handles console input, runs analyses on the input files,
     * and prints the resulting reports to the console.
     *
     * @param args List of command line arguments.
     */
    def main(args: Array[String]) {
        val (options, inputFiles) = args.partition(_.startsWith("-"))

        val ignoreParams = options.filter(_.startsWith("-i="))
        val libraryInputFileParams = options.filter(_.startsWith("-l="))
        val configurationFileParams = options.filter(_.startsWith("-c="))
        val unknownParams = options.diff(
            ignoreParams ++ libraryInputFileParams ++ configurationFileParams)

        // Check for unknown parameters:
        if (unknownParams.size > 0) {
            if (unknownParams.size == 1) {
                printUsageAndExit("unknown parameter: "+unknownParams(0))
            } else {
                printUsageAndExit("unknown parameters: "+unknownParams.mkString(", "))
            }
        }

        //
        // Check for -c command line options:
        // -c=<filename>
        //
        if (configurationFileParams.size > 1) {
            printUsageAndExit("only one configuration file allowed")
        }
        if (configurationFileParams.size == 1) {
            if (!ignoreParams.isEmpty) {
                printUsageAndExit("if a configuration file is used, no -i parameters "+
                    "are allowed")
            }

            // Load configuration file
            val filename = configurationFileParams(0).substring(3)
            try {
                ConfigurationFile.getDisabledAnalysesNamesFromFile(filename).
                    foreach(disableAnalysis(_))
            } catch {
                case e: IOException ⇒
                    printUsageAndExit("could not load configuration file \""+
                        filename+"\" ("+e+")")
            }
        }

        //
        // Check for -i command line options:
        // -i=<analysis-name>
        //
        for (ignoreParam ← ignoreParams) {
            val name = ignoreParam.substring(3)
            if (!builtInAnalyses.contains(name)) {
                printUsageAndExit("unknown analysis \""+name+"\"")
            }
            disableAnalysis(name)
        }

        //
        // Check for -l command line options:
        // -l=<input-file-name>
        //
        val libraryInputFiles =
            for (libraryInputFileParam ← libraryInputFileParams) yield {
                val fileName = libraryInputFileParam.substring(3)
                if (fileName == "") {
                    printUsageAndExit("empty library file name")
                }
                fileName
            }

        //
        // Do some basic validation of the input files
        //
        if (inputFiles.size == 0) {
            printUsageAndExit("no input files")
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
        // Execute enabled analyses on the `Project`
        //
        val startTime = System.nanoTime()

        val allResults = analyze(project, analysesToRun, Some(this))

        val realTime = System.nanoTime() - startTime
        println("sum: "+timeToString(analysesTotalTime)+", "+
            "real time (including synchronization overhead etc.): "+
            timeToString(realTime))

        allResults.foreach(
            results ⇒
                // Display report's console messages, separated by newlines, with the
                // analysis description and wiki URL at the bottom of each analysis' list
                // of reports.
                println(results._2.map(_.consoleReport(urlToLocationIdentifier)).
                    mkString("\n", "\n",
                        "\n"+Console.BLUE+"description: "+Console.RESET
                            + wikiUrlPrefix + results._1))
        )

        // Display how many reports came from every analysis.
        println("\nNumber of reports per analysis:")
        allResults.foreach(results ⇒ println(results._1+" "+results._2.size))
    }

    var analysesStartTimes: Map[Int, Long] = Map.empty
    var analysesTotalTime: Long = 0
    val threadmxbean = ManagementFactory.getThreadMXBean()
    val currentThreadCpuTimeSupported = threadmxbean.isCurrentThreadCpuTimeSupported()

    /**
     * Returns a time in ns.
     */
    private def getTime(): Long = {
        if (currentThreadCpuTimeSupported) {
            threadmxbean.getCurrentThreadCpuTime()
        } else {
            0
        }
    }

    /**
     * Builds a nice string to display time in the console.
     */
    private def timeToString(nanosecs: Long): String = {
        val formatter = new DecimalFormat("#.###")
        Console.YELLOW +
            formatter.format(nanosecs.toFloat / 1e9f)+" seconds"+
            Console.RESET
    }

    /**
     * Prints a progress message.
     */
    private def printProgress(
        color: String,
        position: Int,
        status: String,
        message: String) {
        println(color+"["+position+"/"+analysesToRun.size+"]"+Console.RESET+
            " "+status+" "+message)
    }

    /**
     * Called at the beginning of each analysis.
     */
    override def beginAnalysis(name: String, position: Int) {
        analysesStartTimes += (position -> getTime())
        printProgress(Console.GREEN, position, "running", "               \t"+name)
    }

    /**
     * Called at the end of each analysis.
     */
    override def endAnalysis(name: String, position: Int, reports: AnalysisReports) {
        val time = getTime() - analysesStartTimes(position)
        analysesTotalTime += time
        printProgress(Console.RED, position, "finished",
            timeToString(time)+"\t"+name+", "+reports.size+" reports.")
    }
}
