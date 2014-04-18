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

import analyses._
import resolved._
import resolved.analyses._
import resolved.reader._
import java.io.File
import java.io.IOException
import java.net.URL
import java.lang.management.ManagementFactory
import java.text.DecimalFormat

/**
 * FindRealBugs is a QA-tool using the BAT(AI) framework to perform static code analysis
 * on Java-bytecode and detect bugs.
 *
 * This object keeps a list of all available analyses and provides a way to run them on
 * a given `bat.resolved.analyses.ProjectLike`.
 *
 * @author Florian Brandherm
 * @author Peter Spieler
 * @author Daniel Klauer
 */
object FindRealBugs {
    type Analysis = MultipleResultsAnalysis[URL, SourceLocationBasedReport[URL]]
    type AnalysisReports = Iterable[SourceLocationBasedReport[URL]]
    type AnalysisResult = (String, AnalysisReports)
    type AnalysesMap = Map[String, Analysis]

    /**
     * A list of available analyses: names and instances.
     */
    val builtInAnalyses: AnalysesMap = Map(
        ("AnonymousInnerClassShouldBeStatic" ->
            new AnonymousInnerClassShouldBeStatic[URL]),
        ("BadlyOverriddenAdapter" ->
            new BadlyOverriddenAdapter[URL]),
        ("BitNops" ->
            new BitNops[URL]),
        ("BoxingImmediatelyUnboxedToPerformCoercion" ->
            new BoxingImmediatelyUnboxedToPerformCoercion[URL]),
        ("CatchesIllegalMonitorStateException" ->
            new CatchesIllegalMonitorStateException[URL]),
        ("CloneDoesNotCallSuperClone" ->
            new CloneDoesNotCallSuperClone[URL]),
        ("CnImplementsCloneButNotCloneable" ->
            new CnImplementsCloneButNotCloneable[URL]),
        ("CovariantCompareTo" ->
            new CovariantCompareTo[URL]),
        ("CovariantEquals" ->
            new CovariantEquals[URL]),
        ("DmRunFinalizersOnExit" ->
            new DmRunFinalizersOnExit[URL]),
        ("DoInsideDoPrivileged" ->
            new DoInsideDoPrivileged[URL]),
        ("EqualsHashCodeContract" ->
            new EqualsHashCodeContract[URL]),
        ("FieldIsntImmutableInImmutableClass" ->
            new FieldIsntImmutableInImmutableClass[URL]),
        ("FieldShouldBeFinal" ->
            new FieldShouldBeFinal[URL]),
        ("FieldShouldBePackageProtected" ->
            new FieldShouldBePackageProtected[URL]),
        ("FinalizeUseless" ->
            new FinalizeUseless[URL]),
        ("ImmutableClassInheritsMutableClass" ->
            new ImmutableClassInheritsMutableClass[URL]),
        ("ImplementsCloneableButNotClone" ->
            new ImplementsCloneableButNotClone[URL]),
        ("InefficientToArray" ->
            new InefficientToArray[URL]),
        ("LongBitsToDoubleInvokedOnInt" ->
            new LongBitsToDoubleInvokedOnInt[URL]),
        ("NativeMethodInImmutableClass" ->
            new NativeMethodInImmutableClass[URL]),
        ("NonSerializableClassHasASerializableInnerClass" ->
            new NonSerializableClassHasASerializableInnerClass[URL]),
        ("ManualGarbageCollection" ->
            new ManualGarbageCollection[URL]),
        ("ProtectedFieldInFinalClass" ->
            new ProtectedFieldInFinalClass[URL]),
        ("PublicFinalizeMethodShouldBeProtected" ->
            new PublicFinalizeMethodShouldBeProtected[URL]),
        ("SerializableNoSuitableConstructor" ->
            new SerializableNoSuitableConstructor[URL]),
        ("SuperclassUsesSubclassDuringinitialization" ->
            new SuperclassUsesSubclassDuringInitialization[URL]),
        ("SwingMethodInvokedInSwingThread" ->
            new SwingMethodInvokedInSwingThread[URL]),
        ("SyncSetUnsyncGet" ->
            new SyncSetUnsyncGet[URL]),
        ("UnusedPrivateFields" ->
            new UnusedPrivateFields[URL]),
        ("UrUninitReadCalledFromSuperConstructor" ->
            new UrUninitReadCalledFromSuperConstructor[URL]),
        ("UselessIncrementInReturn" ->
            new UselessIncrementInReturn[URL])
    )
}

/**
 * This can be used to obtain information about the progress about an analysis.
 * @author Florian Brandherm
 */
trait ProgressListener {
    import FindRealBugs._

    /**
     * Override this callback to be notified when a certain analysis is started.
     *
     * Note: Since the analyses are executed in parallel, begin/end events may not
     * necessarily be in order. Calls to this method may come from multiple threads.
     * However, all calls to this method are synchronized.
     *
     * @param name The analysis' name.
     * @param position The analysis' start number. 1st analysis = 1, 2nd analysis = 2,
     * etc.
     */
    def beginAnalysis(name: String, position: Integer)

    /**
     * Override this callback to be notified when a certain analysis ends.
     *
     * Note: see also beginAnalysis()
     *
     * @param name The analysis' name.
     * @param reports The reports produced by the analysis, if any.
     * @param position The analysis' start number.
     */
    def endAnalysis(name: String, reports: AnalysisReports, position: Integer)

    /**
     * Override this callback to be able to prevent the beginning of any more analyses.
     * Important: Once this returns true, it must always return true afterwards!
     *
     * @return Returns `true`, if the analysis should be cancelled, `false` otherwise.
     * Returning `true` prevents further analyses from being started, while allowing
     * currently running ones to finish.
     */
    def isCancelled: Boolean = false
}

trait FindRealBugs {
    import FindRealBugs._

    /**
     * Analyzes a project using the currently enabled analyses.
     *
     * @param project The project to analyze.
     * @param analysesToRun Iterable of names of the analyses that should be run
     * @param progressListener ProgressListener object that will get notified about the
     * analysis progress
     * @param analyses Map of names and analyses; this contains all possible analyses
     * (filtering is done with the analysesToRun parameter)
     * @return List of analyses' reports: each analysis' name associated with its reports.
     */
    final def analyze(
        project: Project[URL],
        analysesToRun: Iterable[String],
        progressListener: ProgressListener = null,
        analyses: AnalysesMap = builtInAnalyses): Iterable[AnalysisResult] = {

        var startedCount: Integer = 0
        var allResults: Set[AnalysisResult] = Set.empty

        for (name ← analysesToRun.par) {
            // If the analysis was cancelled, don't begin new analyses
            if (progressListener == null ||
                !this.synchronized(progressListener.isCancelled)) {

                var position: Integer = 0

                this.synchronized {
                    startedCount += 1
                    position = startedCount
                    if (progressListener != null) {
                        progressListener.beginAnalysis(name, position)
                    }
                }

                // Invoke the analysis and immediately turn the `Iterable` result into a
                // `Set`, to enforce immediate execution instead of delayed (on-demand)
                // execution.
                val results = analyses(name).analyze(project, Seq.empty).toSet

                this.synchronized {
                    if (results.nonEmpty) {
                        allResults += ((name, results))
                    }
                    if (progressListener != null) {
                        progressListener.endAnalysis(name, results, position)
                    }
                }
            }
        }

        allResults
    }

    /**
     * Exception that will be thrown if analyze() encounters an error.
     *
     * @param message message that will be passed by the exception.
     */
    class FindRealBugsException(message: String) extends Exception(message)

    /**
     * Load the given file names as `ClassFile`s.
     *
     * @param inputFileNames The class files to load.
     * @param loadAsLibrary Whether to use `Java7LibraryFramework` or `Java7Framework`.
     * @return The loaded `ClassFile`s, ready to be passed to a `Project`.
     */
    def loadClassFiles(
        inputFileNames: Iterable[String],
        loadAsLibrary: Boolean,
        errorHandler: (String) ⇒ Unit,
        inputFileHandler: (File) ⇒ Unit): Iterable[(ClassFile, URL)] = {

        // Read in files
        val existingFiles = for (arg ← inputFileNames) yield {
            val file = new File(arg)
            if (!file.exists ||
                !file.canRead ||
                (!arg.endsWith(".jar") &&
                    !arg.endsWith(".class") &&
                    !file.isDirectory())) {
                errorHandler("cannot read file: \""+file+"\"")
            }
            file
        }

        // Load class files from the given input files
        (for (file ← existingFiles) yield {
            inputFileHandler(file)
            if (loadAsLibrary) {
                Java7LibraryFramework.ClassFiles(file)
            } else {
                Java7Framework.ClassFiles(file)
            }
        }).flatten
    }
}

/**
 * The FindRealBugs command line interface.
 *
 * @author Florian Brandherm
 * @author Peter Spieler
 * @author Daniel Klauer
 */
object FindRealBugsCLI extends FindRealBugs with ProgressListener {
    import FindRealBugs._

    // TODO(future): Read the Wiki URL from a config file
    val wikiUrlPrefix = "https://bitbucket.org/delors/bat/wiki/FindREALBugs/"

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
        val project = IndexBasedProject(classFiles, libraryClassFiles)
        println("\tClass files loaded: "+project.classFilesCount)

        //
        // Execute enabled analyses on the `Project`
        //
        val startTime = System.nanoTime()

        val allResults = analyze(project, analysesToRun, this)

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

    var analysesStartTimes: Map[Integer, Long] = Map.empty
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
        position: Integer,
        status: String,
        message: String) {
        println(color+"["+position+"/"+analysesToRun.size+"]"+Console.RESET+
            " "+status+" "+message)
    }

    /**
     * Called at the beginning of each analysis.
     */
    override def beginAnalysis(name: String, position: Integer) {
        analysesStartTimes += (position -> getTime())
        printProgress(Console.GREEN, position, "running", "               \t"+name)
    }

    /**
     * Called at the end of each analysis.
     */
    override def endAnalysis(name: String, reports: AnalysisReports, position: Integer) {
        val time = getTime() - analysesStartTimes(position)
        analysesTotalTime += time
        printProgress(Console.RED, position, "finished",
            timeToString(time)+"\t"+name+", "+reports.size+" reports.")
    }
}

/**
 * This object provides an interface for the Eclipse plugin.
 *
 * @author Florian Brandherm
 */
object FindRealBugsPluginInterface extends FindRealBugs {
    import FindRealBugs._

    /**
     * Analyzes a project consisting of the given files. Throws a
     * `FindRealBugsPluginInterface.FindRealBugsException` if an error occurs.
     *
     * @param inputFileNames The .class/.jar files that should be analyzed together.
     * @param inputLibraryFileNames The .class/.jar files that should be included in the
     * analysis as library class files.
     * @param disabledAnalyses Names of analyses that should not be run (default: empty).
     * @param progressListener A `ProgressListener` object that will be notified about the
     * analysis progress.
     * @param additionalAnalyses External analyses that should be added to the list of
     * analyses to run.
     * @return The analyses' reports.
     */
    def runAnalysis(
        inputFileNames: Iterable[String],
        inputLibraryFileNames: Iterable[String],
        disabledAnalyses: Iterable[String] = Nil,
        progressListener: ProgressListener,
        additionalAnalyses: Map[String, Analysis]): Array[(String, AnalysisReports)] = {

        if (inputFileNames.size == 0) {
            throw new FindRealBugsException("No input files!")
        }

        def loadClassFilesForPlugin(
            fileNames: Iterable[String],
            loadAsLibrary: Boolean): Iterable[(ClassFile, URL)] = {
            loadClassFiles(
                fileNames,
                loadAsLibrary,
                error ⇒ throw new FindRealBugsException(error),
                file ⇒ {}
            )
        }

        val classFiles = loadClassFilesForPlugin(inputFileNames, false)
        val libraryClassFiles = loadClassFilesForPlugin(inputLibraryFileNames, true)

        // Create project
        val project = IndexBasedProject(classFiles, libraryClassFiles)

        // Determine analyses that should be run
        val allAnalyses = builtInAnalyses ++ additionalAnalyses
        val analysesToRun =
            for {
                analysisName ← allAnalyses.keys
                if (!disabledAnalyses.exists(_ == analysisName))
            } yield {
                analysisName
            }

        // Analyze
        analyze(project,
            analysesToRun,
            progressListener,
            allAnalyses).toArray
    }
}
