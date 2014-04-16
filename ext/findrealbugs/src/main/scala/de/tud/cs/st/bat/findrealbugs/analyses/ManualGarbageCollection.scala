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
package analyses

import AnalysesHelpers._
import resolved._
import resolved.analyses._
import resolved.instructions._

/**
 * This analysis reports calls to `java.lang.System/Runtime.gc()` that seem to be made
 * manually in code outside the JRE.
 *
 * Manual invocations of garbage collection are usually unnecessary and can lead to
 * performance problems. This heuristic tries to detect such cases.
 *
 * @author Ralf Mitschke
 * @author Peter Spieler
 */
class ManualGarbageCollection[Source]
        extends MultipleResultsAnalysis[Source, MethodBasedReport[Source]] {

    def description: String =
        "Reports Methods outside of java.lang, that explicitly invoke the Garbage Collection."

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

        import MethodDescriptor.NoArgsAndReturnVoid

        // For all methods outside java.lang with "gc" in their name calling gc()...
        for {
            classFile ← project.classFiles
            if !project.isLibraryType(classFile)
            if !classFile.thisType.fqn.startsWith("java/lang")
            method @ MethodWithBody(body) ← classFile.methods
            if !"(^gc)|(gc$)".r.findFirstIn(method.name).isDefined
            instruction ← body.instructions
            if (instruction match {
                case INVOKESTATIC(SystemType, "gc", NoArgsAndReturnVoid) ⇒ true
                case INVOKEVIRTUAL(RuntimeType, "gc", NoArgsAndReturnVoid) ⇒ true
                case _ ⇒ false
            })
        } yield MethodBasedReport(
            project.source(classFile.thisType),
            Severity.Info,
            method,
            "Contains unnecessary call to gc()")
    }
}
