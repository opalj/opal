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
import resolved.instructions._

/**
 * This analysis reports violations of the contract defined in `java.lang.Object` w.r.t.
 * the methods `equals` and `hashcode`.
 *
 * @author Michael Eichberg
 */
class EqualsHashCodeContract[Source]
        extends MultipleResultsAnalysis[Source, ClassBasedReport[Source]] {

    /**
     * Returns a description text for this analysis.
     * @return analysis description
     */
    def description: String = "Finds violations of the equals-hashCode contract."

    /**
     * Runs this analysis on the given project.
     *
     * @param project The project to analyze.
     * @param parameters Options for the analysis. Currently unused.
     * @return A list of reports, or an empty list.
     */
    def analyze(
        project: Project[Source],
        parameters: Seq[String] = List.empty): Iterable[ClassBasedReport[Source]] = {

        val mutex = new Object
        var reports = List[ClassBasedReport[Source]]()

        for (classFile ← project.classFiles if !project.isLibraryType(classFile)) {
            var definesEqualsMethod = false
            var definesHashCodeMethod = false
            for (method ← classFile.methods) method match {
                case Method(_, "equals", MethodDescriptor(Seq(ObjectType.Object),
                    BooleanType)) ⇒
                    definesEqualsMethod = true
                case Method(_, "hashCode", MethodDescriptor(Seq(),
                    IntegerType)) ⇒
                    definesHashCodeMethod = true
                case _ ⇒
            }

            if (definesEqualsMethod != definesHashCodeMethod) {
                mutex.synchronized {
                    reports = ClassBasedReport(
                        project.source(classFile.thisType),
                        Severity.Error,
                        classFile.thisType,
                        "Does not satisfy java.lang.Object's equals-hashCode "+
                            "contract.") :: reports
                }
            }
        }
        reports
    }
}
