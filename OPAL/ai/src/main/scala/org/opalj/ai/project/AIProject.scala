/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package project

import scala.collection.parallel.CollectionConverters.IterableIsParallelizable

import org.opalj.br.analyses.BasicReport
import org.opalj.br.Method
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ReportableAnalysisResult

/**
 * Template class for analyzing complete Java projects that use the abstract interpreter.
 *
 * This trait is intended to be used in combination with the `Analysis` and the
 * `AnalysisApplication` traits to easily create a readily executable analysis (see
 * the ''Demos'' project for examples).
 *
 * @author Michael Eichberg
 */
trait AIProject[Source, D <: Domain with OptionalReport] {

    /**
     * Returns the abstract interpreter that should be used for performing the abstract
     * interpretations.
     */
    def ai: AI[D]

    /**
     * If `true` (default) all entry points will be analyzed in parallel.
     *
     * Needs to be overridden by subclasses if the entry points should be
     * analyzed sequentially.
     */
    protected val analyzeInParallel: Boolean = true

    /**
     * Returns the (initial) domain object that will be used to analyze an entry point.
     *
     * The analysis of all entry points may happen concurrently unless
     * [[analyzeInParallel]] is `false`.
     */
    def domain(project: Project[Source], method: Method): D

    /**
     * A project's entry points.
     *
     * In case of a very simple command-line application, the set of entry
     * points may just consist of the application's `main` method.
     *
     * If, for example, a desktop application with a GUI is
     * to be analyzed, the entry points are usually the application's main method and
     * all those listeners that react on system-level events (mouse move
     * events, keyboard events etc.)
     *
     * In case of a framework, the set of entry points typically encompasses
     * all non-private constructors, all non-private static methods and all
     * static initializers.
     * Additionally, listeners for system-level events are also entry points.
     *
     * @return All methods that are potential entry points.
     */
    def entryPoints(project: Project[Source]): Iterable[Method]

    /**
     * Analyzes the given project by first determining the entry points of the analysis
     * and then starting an independent analysis for each entry point using its own
     * domain.
     *
     * @note This method is intended to be overridden by subtraits that need to get
     *      hold on the specified analysis parameters. In this case (in the subtrait)
     *      it is recommended to first analyze the parameters and afterwards to call
     *      this method using `super.analyze(...)`.
     */
    def analyze(project: Project[Source], parameters: Seq[String]): ReportableAnalysisResult = {

        val analyze: Method => Option[String] = { m: Method =>
            val theDomain = domain(project, m)
            ai(m, theDomain)
            theDomain.report
        }
        val entryPoints = this.entryPoints(project)
        if (entryPoints.isEmpty) {
            throw new IllegalArgumentException("no entry points found")
        }
        val reports =
            if (analyzeInParallel)
                entryPoints.par.map(analyze).seq
            else
                entryPoints.map(analyze)

        val theReports = reports.collect { case Some(report) => report }
        BasicReport("Number of reports: "+theReports.size+"\n"+theReports.mkString("\n"))
    }
}
