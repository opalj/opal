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
 * This analysis reports code that passes a 32bit `int` to `Double.longBitsToDouble()`,
 * which takes a 64bit `long`. Such code is broken, because an `int` is too small to hold
 * a `double`'s bit pattern.
 *
 * The conversion from `int` to `long` may change the bit pattern (sign extension), and
 * even if that doesn't happen the outcome won't necessarily be intended/expected. Perhaps
 * `float` should have been used, instead of `double` - or `long` instead of `int`.
 *
 * @author Ralf Mitschke
 * @author Daniel Klauer
 */
class LongBitsToDoubleInvokedOnInt[Source] extends FindRealBugsAnalysis[Source] {

    /**
     * Returns a description text for this analysis.
     * @return analysis description
     */
    def description: String = "Reports code passing ints to Double.longBitsToDouble(long)"

    private val doubleType = ObjectType("java/lang/Double")
    private val longBitsToDoubleDescriptor =
        MethodDescriptor(IndexedSeq(LongType), DoubleType)

    /**
     * Runs this analysis on the given project.
     *
     * @param project The project to analyze.
     * @param parameters Options for the analysis. Currently unused.
     * @return A list of reports, or an empty list.
     */
    def analyze(
        project: Project[Source],
        parameters: Seq[String] = List.empty): Iterable[LineAndColumnBasedReport[Source]] = {

        // In all method bodies, look for occurrences of (I2L, INVOKESTATIC) instruction
        // sequences, where the INVOKESTATIC is a call to 
        // java.lang.Double.longBitsToDouble().
        for {
            classFile ← project.classFiles
            if !project.isLibraryType(classFile)
            method @ MethodWithBody(body) ← classFile.methods
            pc ← body.matchPair {
                case (
                    I2L,
                    INVOKESTATIC(`doubleType`, "longBitsToDouble", `longBitsToDoubleDescriptor`)
                    ) ⇒ true
                case _ ⇒ false
            }
        } yield {
            LineAndColumnBasedReport(
                project.source(classFile.thisType),
                Severity.Error,
                classFile.thisType,
                method.descriptor,
                method.name,
                body.lineNumber(pc),
                None,
                "Passing int to Double.longBitsToDouble(long)")
        }
    }
}
