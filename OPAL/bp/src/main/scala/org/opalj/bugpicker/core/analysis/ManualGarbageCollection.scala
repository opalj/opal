/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
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
package bugpicker
package core
package analysis

import org.opalj.issues.Issue
import org.opalj.br.MethodWithBody
import org.opalj.br.analyses.SomeProject
import org.opalj.br.MethodDescriptor
import org.opalj.br.ClassFile
import org.opalj.br.ObjectType
import org.opalj.br.instructions.INVOKESTATIC
import org.opalj.br.instructions.INVOKEVIRTUAL
import org.opalj.issues.IssueCategory
import org.opalj.issues.IssueKind
import org.opalj.issues.Relevance
import org.opalj.issues.InstructionLocation

/**
 * This analysis reports calls to `java.lang.System/Runtime.gc()` that seem to be made
 * manually in code outside the core of the JRE.
 *
 * Manual invocations of garbage collection are usually unnecessary and can lead to
 * performance problems. This heuristic tries to detect such cases.
 *
 * @author Ralf Mitschke
 * @author Peter Spieler
 * @author Michael Eichberg
 */
object ManualGarbageCollection {

    final val Runtime = ObjectType("java/lang/Runtime")

    def description: String =
        "Reports methods outside of java.lang that explicitly invoke the garbage collector."

    /**
     * Runs this analysis on the given project.
     *
     * @param project The project to analyze.
     * @param parameters Options for the analysis. Currently unused.
     * @return A list of reports, or an empty list.
     */
    def apply(theProject: SomeProject, classFile: ClassFile): Iterable[Issue] = {

        import MethodDescriptor.NoArgsAndReturnVoid

        if (classFile.thisType.fqn.startsWith("java/lang"))
            return Seq.empty;

        for {
            method @ MethodWithBody(body) ← classFile.methods
            (pc, gcCall) ← body.collectWithIndex {
                case (pc, INVOKESTATIC(ObjectType.System, false, "gc", NoArgsAndReturnVoid)) ⇒
                    (pc, "System.gc()")
                case (pc, INVOKEVIRTUAL(Runtime, "gc", NoArgsAndReturnVoid)) ⇒
                    (pc, "Runtime.gc()")
            }
        } yield Issue(
            "ManualGarbageCollection",
            Relevance.Low,
            s"contains dubious call to $gcCall",
            Set(IssueCategory.Performance),
            Set(IssueKind.DubiousMethodCall),
            List(new InstructionLocation(None, theProject, classFile, method, pc))
        )
    }
}
