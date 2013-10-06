/* License (BSD Style License):
 * Copyright (c) 2009 - 2013
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
 *  - Neither the name of the Software Technology Group or Technische
 *    Universität Darmstadt nor the names of its contributors may be used to
 *    endorse or promote products derived from this software without specific
 *    prior written permission.
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

import analyses._

/**
 * @author Michael Eichberg
 */
trait AIProject[Source] {

    def analyze(project: analyses.Project[Source]): ReportableAnalysisResult = {
        val reports = for ((classFile, method) ← entryPoints(project)) yield {
            val theDomain = domain(project, classFile, method)
            AI(classFile, method, theDomain)
            theDomain.report
        }
        val theReports: Iterable[String] = reports.filter(_.isDefined).map(_.get)
        BasicReport("Number of reports: "+theReports.size+"\n"+theReports.mkString("\n"))
    }

    def domain(
        project: analyses.Project[Source],
        classFile: ClassFile,
        method: Method): Domain[_] with Report

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

}

/**
 * @author Michael Eichberg
 */
trait Report {
    def report: Option[String]
}
