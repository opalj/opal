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
package resolved
package ai
package project

import bat.resolved.analyses.{ Project, ReportableAnalysisResult }

/**
 * Template class for analyzing complete Java projects that use the abstract interpreter.
 *
 * This trait is intended to be used in combination with the `Analysis` and the
 * `AnalysisExecutor` traits to easily create a readily executable analysis (see
 * the ''Demos'' project for examples).
 *
 * @author Michael Eichberg
 */
trait AIProject[Source, D <: Domain with Report] {

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
     * [[analyzeInParallel]] is `false.
     */
    def domain(
        project: Project[Source],
        classFile: ClassFile,
        method: Method): D

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
    def entryPoints(project: Project[Source]): Iterable[(ClassFile, Method)]

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
    def analyze(
        project: Project[Source],
        parameters: Seq[String]): ReportableAnalysisResult = {

        def analyze(cf_m: (ClassFile, Method)) = {
            val (classFile: ClassFile, method: Method) = cf_m
            val theDomain = domain(project, classFile, method)
            ai(classFile, method, theDomain)
            theDomain.report
        }

        val reports =
            if (analyzeInParallel)
                entryPoints(project).par map { analyze(_) }
            else
                entryPoints(project) map { analyze(_) }

        val theReports = reports.filter(_.isDefined).map(_.get)
        bat.resolved.analyses.BasicReport(
            "Number of reports: "+theReports.size+"\n"+theReports.mkString("\n"))
    }
}

