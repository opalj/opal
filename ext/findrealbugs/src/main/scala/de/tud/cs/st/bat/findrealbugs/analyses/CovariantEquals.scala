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
 * This analysis reports classes that have some `equals()` method(s), but not
 * `equals(Object)`. This is bad practice and can lead to unexpected behaviour, because
 * without an `equals(Object)` method, `Object.equals(Object)` is not properly overridden.
 *
 * @author Daniel Klauer
 */
class CovariantEquals[Source]
        extends MultipleResultsAnalysis[Source, ClassBasedReport[Source]] {

    def description: String = "Reports classes with one (or more) equals() methods,"+
        " but without equals(Object)."

    /**
     * Checks whether a class has `equals()` methods but not `equals(Object)`.
     *
     * @param classFile The class to check.
     * @return Whether the class has `equals()` methods but not `equals(Object)`.
     */
    private def hasEqualsButNotEqualsObject(classFile: ClassFile): Boolean = {
        val paramTypes = classFile.methods.collect(_ match {
            case Method(_, "equals", MethodDescriptor(Seq(paramType), BooleanType)) ⇒
                paramType
        })

        paramTypes.size > 0 && !paramTypes.exists(_ == ObjectType.Object)
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
        parameters: Seq[String] = List.empty): Iterable[ClassBasedReport[Source]] = {
        for (
            classFile ← project.classFiles.filter(hasEqualsButNotEqualsObject(_))
            if !project.isLibraryType(classFile)
        ) yield {
            ClassBasedReport(
                project.source(classFile.thisType),
                Severity.Warning,
                classFile.thisType,
                "Missing equals(Object) to override Object.equals(Object)")
        }
    }
}
