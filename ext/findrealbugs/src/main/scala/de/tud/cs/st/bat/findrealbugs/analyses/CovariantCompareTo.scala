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
 * This analysis reports classes that implement `java.lang.Comparable` and have some
 * `compareTo()` method(s), but not `compareTo(Object)`.
 *
 * `compareTo(Object)` is needed to properly override `java.lang.Comparable`'s
 * `compareTo(Object)`. It is bad practice to omit `compareTo(Object)`, as it can lead to
 * bugs where objects are not being compared as expected.
 *
 * When a normal class implements `Comparable` or `Comparable<T>`, then the Java compiler
 * will require it to implement `compareTo(Object)` or `compareTo(T)` (in the latter case,
 * the compiler will automatically generate a `compareTo(Object)` bridge method).
 *
 * When extending a normal class that implements `Comparable`, the compiler does not
 * require the `compareTo(Object)` to be implemented in the subclass, as the base class
 * already does that. This is the case where programmers can forget to implement
 * `compareTo(Object)` even though the subclass is `Comparable`.
 *
 * Here we do not complain about `compareTo()` missing altogether, because in some cases
 * it could be unnecessary for a subclass to implement compareTo().
 *
 * We do not need to check interfaces, because for interfaces it doesn't really matter
 * whether they define some additional compareTo() methods or not. What does matter is the
 * class that ultimately implements that interface.
 *
 * @author Ralf Mitschke
 * @author Daniel Klauer
 */
class CovariantCompareTo[Source]
        extends MultipleResultsAnalysis[Source, ClassBasedReport[Source]] {

    def description: String = "Reports classes implementing java.lang.Comparable "+
        "and one (or more) compareTo() methods, but without overriding compareTo(Object)."

    private val Comparable = ObjectType("java/lang/Comparable")

    /**
     * Returns a list of all classes implementing java.lang.Comparable.
     *
     * @param project The project to analyze.
     * @return List of classes implementing `Comparable`.
     */
    private def findComparables(project: Project[Source]): Iterable[ClassFile] = {
        // If it's unknown, it's neither possible nor necessary to collect subtypes
        if (project.classHierarchy.isUnknown(Comparable)) {
            return Iterable.empty
        }

        for {
            comparable ← project.classHierarchy.allSubtypes(Comparable, false)
            classFile ← project.classFile(comparable)
            if !project.isLibraryType(classFile)
            if !classFile.isInterfaceDeclaration
        } yield {
            classFile
        }
    }

    /**
     * Checks whether a class has `compareTo()` methods but not `compareTo(Object)`.
     *
     * @param classFile The class to check.
     * @return Whether the given class has `compareTo()` methods but not
     * `compareTo(Object)`.
     */
    private def hasCompareToButNotCompareToObject(classFile: ClassFile): Boolean = {
        val paramTypes = classFile.methods.collect(_ match {
            case Method(_, "compareTo", MethodDescriptor(Seq(paramType), IntegerType)) ⇒
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
            classFile ← findComparables(project).
                filter(hasCompareToButNotCompareToObject(_))
        ) yield {
            ClassBasedReport(
                project.source(classFile.thisType),
                Severity.Warning,
                classFile.thisType,
                "Missing compareTo(Object) to override Comparable.compareTo(Object)")
        }
    }
}
