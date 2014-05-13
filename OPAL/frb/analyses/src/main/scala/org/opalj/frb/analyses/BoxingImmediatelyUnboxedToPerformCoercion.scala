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
package analyses

import br._
import br.analyses._
import br.instructions._

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
        val theTypes = scala.collection.mutable.HashSet(
            ObjectType("java/lang/Boolean"),
            ObjectType("java/lang/Byte"),
            ObjectType("java/lang/Character"),
            ObjectType("java/lang/Short"),
            ObjectType("java/lang/Integer"),
            ObjectType("java/lang/Long"),
            ObjectType("java/lang/Float"),
            ObjectType("java/lang/Double"))
        val theMethods = scala.collection.mutable.HashSet(
            "booleanValue",
            "byteValue",
            "charValue",
            "shortValue",
            "intValue",
            "longValue",
            "floatValue",
            "doubleValue")

        var result: List[LineAndColumnBasedReport[S]] = List.empty
        for {
            classFile ← project.classFiles.par if classFile.majorVersion >= 49
            if !project.isLibraryType(classFile)
            method @ MethodWithBody(body) ← classFile.methods
        } {
            val instructions = body.instructions
            val max_pc = body.instructions.length

            var pc = 0
            var next_pc = body.pcOfNextInstruction(pc)

            while (next_pc < max_pc) {
                if (pc + 3 == next_pc) {
                    instructions(pc) match {
                        case INVOKESPECIAL(receiver1, _, TheArgument(parameterType: BaseType)) ⇒
                            instructions(next_pc) match {
                                case INVOKEVIRTUAL(
                                    `receiver1`,
                                    name,
                                    NoArgumentMethodDescriptor(returnType: BaseType)
                                    ) if ((returnType ne parameterType) && (theTypes.contains(receiver1) && theMethods.contains(name))) ⇒
                                    {
                                        result =
                                            LineAndColumnBasedReport(
                                                project.source(classFile.thisType),
                                                Severity.Info,
                                                classFile.thisType,
                                                method.descriptor,
                                                method.name,
                                                body.lineNumber(pc),
                                                None,
                                                "Value boxed and immediately unboxed") :: result
                                        // we have matched the sequence
                                        pc = body.pcOfNextInstruction(next_pc)
                                    }
                                case _ ⇒
                                    pc = next_pc
                                    next_pc = body.pcOfNextInstruction(pc)

                            }
                        case _ ⇒
                            pc = next_pc
                            next_pc = body.pcOfNextInstruction(pc)
                    }
                } else {
                    pc = next_pc
                    next_pc = body.pcOfNextInstruction(pc)
                }
            }
        }
        result
    }
}
