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
import java.io._
import java.util.Properties
import scala.collection.JavaConversions._

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
    type AnalysisReports = Set[SourceLocationBasedReport[URL]]
    type AnalysisCreator = () ⇒ Analysis
    type AnalysisRegistry = Map[String, Boolean]

    /**
     * FindRealBugs' built-in analyses.
     */
    val builtInAnalysisNames: Set[String] = Set(
        "AnonymousInnerClassShouldBeStatic",
        "BadlyOverriddenAdapter",
        "BitNops",
        "BoxingImmediatelyUnboxedToPerformCoercion",
        "CatchesIllegalMonitorStateException",
        "CloneDoesNotCallSuperClone",
        "CnImplementsCloneButNotCloneable",
        "CovariantCompareTo",
        "CovariantEquals",
        "DmRunFinalizersOnExit",
        "DoInsideDoPrivileged",
        "EqualsHashCodeContract",
        "FieldIsntImmutableInImmutableClass",
        "FieldShouldBeFinal",
        "FieldShouldBePackageProtected",
        "FinalizeUseless",
        "ImmutableClassInheritsMutableClass",
        "ImplementsCloneableButNotClone",
        "InefficientToArray",
        "LongBitsToDoubleInvokedOnInt",
        "NativeMethodInImmutableClass",
        "NonSerializableClassHasASerializableInnerClass",
        "ManualGarbageCollection",
        "ProtectedFieldInFinalClass",
        "PublicFinalizeMethodShouldBeProtected",
        "SerializableNoSuitableConstructor",
        "SwingMethodInvokedInSwingThread",
        "SyncSetUnsyncGet",
        "UninitializedFieldAccessDuringStaticInitialization",
        "UnusedPrivateFields",
        "UrUninitReadCalledFromSuperConstructor",
        "UselessIncrementInReturn"
    )

    /**
     * Full class names of FindRealBugs' built-in analyses.
     */
    val builtInAnalysisClassNames: Set[String] =
        builtInAnalysisNames.map("org.opalj.frb.analyses."+_)

    val defaultRegistry: AnalysisRegistry =
        builtInAnalysisClassNames.map(name ⇒ name -> true).toMap

    /**
     * Analyzes a project using the currently enabled analyses.
     *
     * Thread safety: This method runs all analyses in parallel, but ensures that calls
     * it makes to the given `progressListener` are synchronized.
     *
     * @param project The project to analyze.
     * @param analysisCreators The analyses to run.
     * @param progressListener `ProgressListener` object that will get notified about the
     *      analysis progress.
     * @param progressController `ProgressController` object that can abort the analyses.
     * @return List of reports per analysis.
     */
    final def analyze(
        project: Project[URL],
        analysisCreators: Set[AnalysisCreator],
        progressListener: Option[ProgressListener] = None,
        progressController: Option[ProgressController] = None): Map[Analysis, AnalysisReports] = {

        import scala.collection.JavaConversions._

        val results: scala.collection.concurrent.Map[Analysis, AnalysisReports] =
            new java.util.concurrent.ConcurrentHashMap[Analysis, AnalysisReports]()
        val startedCount = new java.util.concurrent.atomic.AtomicInteger(0)
        val total = analysisCreators.size

        for (analysisCreator ← analysisCreators.par) {
            val analysis = analysisCreator()

            // Only start new analysis if the process wasn't cancelled yet
            if (!progressController.map(pc ⇒ pc.isCancelled).getOrElse(false)) {
                val position = startedCount.incrementAndGet()
                progressListener.map(_.analysisStarted(analysis, position, total))

                // Invoke the analysis and immediately turn the `Iterable` result into a
                // `Set`, to enforce immediate execution instead of delayed (on-demand)
                // execution.
                import util.PerformanceEvaluation.{ run, ns2sec }

                run {
                    val reports = analysis.analyze(project, Seq.empty).toSet
                    if (reports.nonEmpty) {
                        results += analysis -> reports
                    }
                    reports
                } { (time, reports) ⇒
                    progressListener.map(
                        _.analysisCompleted(analysis, position, total, ns2sec(time), reports))
                }
            }
        }

        results.toMap
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

    /**
     * Load an `AnalysisRegistry` from a Java properties file.
     *
     * Properties file syntax:
     * Keys are interpreted as analysis class names; values can be "yes" or "no" to indicate
     * whether or not the analysis is enabled.
     *
     * @author Daniel Klauer
     */
    def loadRegistry(file: File): AnalysisRegistry = {
        process(new BufferedInputStream(new FileInputStream(file))) { stream ⇒
            val properties = new Properties()
            properties.load(stream)
            (properties.stringPropertyNames().map { name ⇒
                val value = properties.getProperty(name, "")
                val enabled = value match {
                    case "yes" ⇒ true
                    case "no"  ⇒ false
                    case _ ⇒
                        throw new IllegalArgumentException(
                            file.getName()+": "+name+": invalid value '"+value+"'"+
                                ", expected 'yes' or 'no'")
                }

                name -> enabled
            }).toMap
        }
    }

    /**
     * Save an `AnalysisRegistry` to a Java properties file. See also `load`.
     */
    def saveRegistry(file: File, registry: AnalysisRegistry): Unit = {
        process(new BufferedOutputStream(new FileOutputStream(file))) { stream ⇒
            val properties = new Properties()
            for ((name, enabled) ← registry) {
                properties.setProperty(name, if (enabled) "yes" else "no")
            }
            properties.store(stream, null)
        }
    }

    /**
     * Load (and instantiate) a class by name and cast to a certain type.
     */
    def newInstance[T](className: String): T = {
        Class.forName(className).newInstance.asInstanceOf[T]
    }

    /**
     * Load (and instantiate) classes implementing `Analysis` by full class name.
     */
    def loadAnalysis(className: String): Analysis = newInstance[Analysis](className)
}
