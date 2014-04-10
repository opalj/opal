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
import java.net.URL

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

    /**
     * A list of available analyses: names and instances.
     */
    val analyses: Map[String, Analysis] = Map(
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

    /**
     * Analyzes the project using the given analyses.
     *
     * @param project The project to analyze.
     * @param analysesNames List of analysis names representing the analyses to run: all
     * or a subset of `[[analyses]]`.
     * @return List of each run analysis' name coupled with that analysis' reports.
     */
    def analyze(
        project: Project[URL],
        analysesNames: Iterable[String]): Iterable[(String, AnalysisReports)] = {
        (for (name ← analysesNames.par) yield {
            (name, analyses(name).analyze(project, Seq.empty))
        }
        ).seq.filter(result ⇒ !result._2.isEmpty)
    }
}

/**
 * The FindRealBugs command line interface.
 *
 * @author Florian Brandherm
 * @author Peter Spieler
 * @author Daniel Klauer
 */
object FindRealBugsCLI {
    import FindRealBugs._

    // TODO(future): Read the Wiki URL from a config file
    private val wikiUrlPrefix = "https://bitbucket.org/delors/bat/wiki/FindREALBugs/"

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
        sys.exit(1)
    }

    /**
     * Entry point: Handles console input, runs analyses on the input files,
     * and prints the resulting reports to the console.
     *
     * @param args List of command line arguments.
     */
    def main(args: Array[String]) {
        val (options, inputFiles) = args.partition(_.startsWith("-"))

        //
        // Check for -i command line options:
        // -i=<analysis-name>
        //
        val analysesToDisable = for (arg ← options) yield {
            if (!arg.matches("(-i=)(.*)")) {
                printUsageAndExit("unknown option \""+arg+"\"")
            }
            val name = arg.substring(3)
            if (!analyses.contains(name)) {
                printUsageAndExit("unknown analysis \""+name+"\"")
            }
            name
        }

        //
        // Do some basic validation of the input files
        //
        if (inputFiles.size == 0) {
            printUsageAndExit("no input files")
        }
        val existingFiles = for (arg ← inputFiles) yield {
            val file = new File(arg)
            if (!file.exists ||
                !file.canRead ||
                !(arg.endsWith(".jar") ||
                    arg.endsWith(".class") ||
                    file.isDirectory())) {
                printUsageAndExit("cannot read file: \""+file+"\"")
            }
            file
        }

        //
        // Load .class files into a Project
        //
        println("Reading class files:")
        val classFiles = (
            for (file ← existingFiles) yield {
                println("\t"+file.toString())
                Java7Framework.ClassFiles(file)
            }
        ).flatten
        val project = Project(classFiles)
        println("\tClass files loaded: "+project.classFilesCount)

        //
        // Execute analyses on that Project
        //
        val analysesToRun = analyses.keys.filter(!analysesToDisable.contains(_))
        println("\nExecuting the following analyses:")
        analysesToRun.foreach(println(_))
        println("Executing analyses.")
        analyze(project, analysesToRun).foreach(
            results ⇒
                // Display report's console messages, separated by newlines, with the
                // analysis description and wiki URL at the bottom of each analysis' list
                // of reports.
                println(results._2.map(_.consoleReport(urlToLocationIdentifier)).
                    mkString("\n", "\n",
                        "\n"+Console.BLUE+"description: "+Console.RESET
                            + wikiUrlPrefix + results._1))
        )
    }
}
