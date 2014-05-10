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

import resolved._
import resolved.analyses._
import resolved.instructions._

/**
 * This analysis reports code such as this:
 * {{{
 * new Integer(1).doubleValue()
 * }}}
 * where a literal value is boxed into an object and then immediately unboxed. This means
 * that the object creation was useless.
 *
 * TODO [Improvement] Improve to also detect code like this:
 * {{{
 * Integer.valueOf(1).doubleValue()
 * }}}
 *
 * @author Ralf Mitschke
 * @author Daniel Klauer
 */
class BoxingImmediatelyUnboxedToPerformCoercion[S]
        extends MultipleResultsAnalysis[S, LineAndColumnBasedReport[S]] {

    def description: String =
        "Reports sections of code that box a value but then immediately unbox it."

    /**
     * Runs this analysis on the given project.
     *
     * @param project The project to analyze.
     * @param parameters Options for the analysis. Currently unused.
     * @return A list of reports, or an empty list.
     */
    def analyze(
        project: Project[S],
        parameters: Seq[String] = List.empty): Iterable[LineAndColumnBasedReport[S]] = {

        // For each method doing INVOKESPECIAL followed by INVOKEVIRTUAL on the same
        // java.lang class, where the called method's name ends in "Value"...
        for {
            classFile ← project.classFiles if classFile.majorVersion >= 49
            if !project.isLibraryType(classFile)
            method @ MethodWithBody(body) ← classFile.methods
            pc ← body.slidingCollect(2)({
                case (pc,
                    Seq(INVOKESPECIAL(receiver1, _, MethodDescriptor(Seq(paramType), _)),
                        INVOKEVIRTUAL(receiver2, name, MethodDescriptor(Seq(), returnType)))) if (
                    !paramType.isReferenceType &&
                    receiver1.asObjectType.fqn.startsWith("java/lang") &&
                    receiver1 == receiver2 &&
                    name.endsWith("Value") &&
                    returnType != paramType // coercion to another type performed
                ) ⇒ pc
            })
        } yield {
            LineAndColumnBasedReport(
                project.source(classFile.thisType),
                Severity.Info,
                classFile.thisType,
                method.descriptor,
                method.name,
                body.lineNumber(pc),
                None,
                "Value boxed and immediately unboxed")
        }
    }
}
