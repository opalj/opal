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

import analyses._
import br._
import br.analyses._
import br.reader._
import java.net.URL
import java.io.File

/**
 * FindRealBugs is a QA-tool using the OPAL(AI) framework to perform static code analysis
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
    type AnalysisCreator = () ⇒ Analysis
    type AnalysesMap = Map[String, AnalysisCreator]

    /**
     * FindRealBugs' built-in analyses.
     */
    val builtInAnalyses: AnalysesMap = Map(
        ("AnonymousInnerClassShouldBeStatic" ->
            (() ⇒ new AnonymousInnerClassShouldBeStatic[URL])),
        ("BadlyOverriddenAdapter" ->
            (() ⇒ new BadlyOverriddenAdapter[URL])),
        ("BitNops" ->
            (() ⇒ new BitNops[URL])),
        ("BoxingImmediatelyUnboxedToPerformCoercion" ->
            (() ⇒ new BoxingImmediatelyUnboxedToPerformCoercion[URL])),
        ("CatchesIllegalMonitorStateException" ->
            (() ⇒ new CatchesIllegalMonitorStateException[URL])),
        ("CloneDoesNotCallSuperClone" ->
            (() ⇒ new CloneDoesNotCallSuperClone[URL])),
        ("CnImplementsCloneButNotCloneable" ->
            (() ⇒ new CnImplementsCloneButNotCloneable[URL])),
        ("CovariantCompareTo" ->
            (() ⇒ new CovariantCompareTo[URL])),
        ("CovariantEquals" ->
            (() ⇒ new CovariantEquals[URL])),
        ("DmRunFinalizersOnExit" ->
            (() ⇒ new DmRunFinalizersOnExit[URL])),
        ("DoInsideDoPrivileged" ->
            (() ⇒ new DoInsideDoPrivileged[URL])),
        ("EqualsHashCodeContract" ->
            (() ⇒ new EqualsHashCodeContract[URL])),
        ("FieldIsntImmutableInImmutableClass" ->
            (() ⇒ new FieldIsntImmutableInImmutableClass[URL])),
        ("FieldShouldBeFinal" ->
            (() ⇒ new FieldShouldBeFinal[URL])),
        ("FieldShouldBePackageProtected" ->
            (() ⇒ new FieldShouldBePackageProtected[URL])),
        ("FinalizeUseless" ->
            (() ⇒ new FinalizeUseless[URL])),
        ("ImmutableClassInheritsMutableClass" ->
            (() ⇒ new ImmutableClassInheritsMutableClass[URL])),
        ("ImplementsCloneableButNotClone" ->
            (() ⇒ new ImplementsCloneableButNotClone[URL])),
        ("InefficientToArray" ->
            (() ⇒ new InefficientToArray[URL])),
        ("LongBitsToDoubleInvokedOnInt" ->
            (() ⇒ new LongBitsToDoubleInvokedOnInt[URL])),
        ("NativeMethodInImmutableClass" ->
            (() ⇒ new NativeMethodInImmutableClass[URL])),
        ("NonSerializableClassHasASerializableInnerClass" ->
            (() ⇒ new NonSerializableClassHasASerializableInnerClass[URL])),
        ("ManualGarbageCollection" ->
            (() ⇒ new ManualGarbageCollection[URL])),
        ("ProtectedFieldInFinalClass" ->
            (() ⇒ new ProtectedFieldInFinalClass[URL])),
        ("PublicFinalizeMethodShouldBeProtected" ->
            (() ⇒ new PublicFinalizeMethodShouldBeProtected[URL])),
        ("SerializableNoSuitableConstructor" ->
            (() ⇒ new SerializableNoSuitableConstructor[URL])),
        ("SwingMethodInvokedInSwingThread" ->
            (() ⇒ new SwingMethodInvokedInSwingThread[URL])),
        ("SyncSetUnsyncGet" ->
            (() ⇒ new SyncSetUnsyncGet[URL])),
        ("UninitializedFieldAccessDuringStaticInitialization" ->
            (() ⇒ new UninitializedFieldAccessDuringStaticInitialization[URL])),
        ("UnusedPrivateFields" ->
            (() ⇒ new UnusedPrivateFields[URL])),
        ("UrUninitReadCalledFromSuperConstructor" ->
            (() ⇒ new UrUninitReadCalledFromSuperConstructor[URL])),
        ("UselessIncrementInReturn" ->
            (() ⇒ new UselessIncrementInReturn[URL]))
    )

    /**
     * Analyzes a project using the currently enabled analyses.
     *
     * Thread safety: This method runs all analyses in parallel, but ensures that calls
     * it makes to the given `progressListener` are synchronized.
     *
     * @param project The project to analyze.
     * TODO [Refactor] This is counter-intuitive - I can't see a good reason why we need both parameters: analysesToRun and analyses. The latter should you contain that analyses that are required.
     * @param analysesToRun Iterable of names of the analyses that should be run.
     * @param progressListener ProgressListener object that will get notified about the
     *      analysis progress.
     * @param analyses Map of names and analyses; this contains all possible analyses
     *      (filtering is done with the `analysesToRun` parameter).
     * @return List of analyses' reports: each analysis' name associated with its reports.
     */
    final def analyze(
        project: Project[URL],
        analysesToRun: Iterable[String],
        progressListener: Option[ProgressListener] = None,
        progressController: Option[ProgressController] = None,
        analyses: AnalysesMap = builtInAnalyses): Iterable[AnalysisResult] = {

        val lock = new Object
        var allResults: Set[AnalysisResult] = Set.empty
        var startedCount: Int = 0
        var cancelled: Boolean = false

        for (name ← analysesToRun.par) {

            // Check whether this analysis should be run at all, and if so, determine it's
            // start position
            var position: Int = 0
            lock.synchronized {
                // Only start new analysis if the process wasn't cancelled yet
                if (!cancelled) {
                    // Check whether we should cancel now
                    if (progressController.isDefined) {
                        cancelled = progressController.get.isCancelled
                    }
                    if (!cancelled) {
                        startedCount += 1
                        position = startedCount
                        if (progressListener.isDefined) {
                            progressListener.get.beginAnalysis(name, position)
                        }
                    }
                }
            }

            // Should we run this analysis?
            if (position > 0) {
                // Invoke the analysis and immediately turn the `Iterable` result into a
                // `Set`, to enforce immediate execution instead of delayed (on-demand)
                // execution.
                import util.PerformanceEvaluation.{ run, ns2sec }

                run {
                    analyses(name)().analyze(project, Seq.empty).toSet
                } { (time, reports) ⇒
                    // TODO [Refactor] Move lock.synchronized into the if statement as soon as the progress listener is thread safe
                    lock.synchronized {
                        if (reports.nonEmpty) {
                            allResults += ((name, reports))
                        }
                        progressListener.map(
                            _.endAnalysis(name, position, ns2sec(time), reports))
                    }
                }
            }
        }

        allResults
    }

    // TODO [Refactor] (Re)use the same code as AnalysisExcecutor (after the refactoring of that code). 
    /**
     * Load the given file names as `ClassFile`s.
     *
     * @param inputFileNames The class files to load.
     * @param loadAsLibrary Whether to use `Java8LibraryFramework` or `Java8Framework`.
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
                Java8LibraryFramework.ClassFiles(file)
            } else {
                Java8Framework.ClassFiles(file)
            }
        }).flatten
    }
}
