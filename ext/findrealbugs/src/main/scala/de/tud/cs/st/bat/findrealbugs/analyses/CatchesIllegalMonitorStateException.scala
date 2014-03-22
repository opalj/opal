/* License (BSD Style License):
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
package findrealbugs
package analyses

import resolved._
import resolved.analyses._

/**
 * This analysis reports methods that catch `java.lang.IllegalMonitorStateException`
 * exceptions, which should never be necessary in well-written code, and only hides bugs.
 *
 * @author Ralf Mitschke
 * @author Daniel Klauer
 */
class CatchesIllegalMonitorStateException[Source]
        extends MultipleResultsAnalysis[Source, MethodBasedReport[Source]] {

    def description: String = "Reports methods that catch IllegalMonitorStateException."

    /**
     * Determines whether the given method has a catch block for the given exception.
     *
     * @param method The method to check.
     * @param exception The exception type to check for.
     * @return Whether the method has an exception handler for the given exception.
     */
    private def catchesException(method: Method, exception: ObjectType): Boolean = {
        method match {
            case MethodWithBody(body) ⇒
                body.exceptionHandlers.exists {
                    case ExceptionHandler(_, _, _, Some(`exception`)) ⇒ true
                    case _ ⇒ false
                }
            case _ ⇒ false
        }
    }

    /**
     * Runs this analysis on the given project.
     *
     * @param project The project to analyze.
     * @param parameters Options for the analysis. Currently unused.
     * @return A list of reports, or an empty list.
     */
    def analyze(
        project: Project[Source],
        parameters: Seq[String] = List.empty): Iterable[MethodBasedReport[Source]] = {

        // Look for methods that have an exception handler for
        // IllegalMonitorStateException.
        for {
            classFile ← project.classFiles
            if classFile.isClassDeclaration
            method ← classFile.methods
            if catchesException(method, ObjectType.IllegalMonitorStateException)
        } yield {
            MethodBasedReport(
                project.source(classFile.thisType),
                Severity.Info,
                method,
                "Handles IllegalMonitorStateException")
        }
    }
}
