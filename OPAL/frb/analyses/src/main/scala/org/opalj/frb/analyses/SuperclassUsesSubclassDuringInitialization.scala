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
 * This analysis reports static initializers (`<clinit>`) that access `static` fields
 * of a subclass. This is bad because the subclass is not initialized at that point.
 * Superclasses are initialized first, under all circumstances.
 *
 * TODO (ideas for future improvement):
 * - Should not report fields that were written with a PUTSTATIC before being accessed
 *   through a GETSTATIC, because then they're not uninitialized.
 *
 * @author Florian Brandherm
 */
class SuperclassUsesSubclassDuringInitialization[Source]
        extends MultipleResultsAnalysis[Source, ClassBasedReport[Source]] {

    def description: String = "Reports classes which use a static field of a Subclass "+
        "during (static) initialization."

    /**
     * Runs this analysis on the given project. Reports superclasses calling static
     * constructors with access to subclasses.
     *
     * @param project The project to analyze.
     * @param parameters Options for the analysis. Currently unused.
     * @return A list of reports, or an empty list.
     */
    def analyze(
        project: Project[Source],
        parameters: Seq[String] = List.empty): Iterable[ClassBasedReport[Source]] = {

        /*
         * Checks whether a given class is derived from another class.
         *
         * @param subclass class for which is checked if it's a true subclass of thisClass
         * @param thisClass class that is supposed to be a true supertype of subclass
         * @return true, if the subclass is a true subtype of thisClass
         */
        def derivedFrom(subclass: ObjectType, thisClass: ObjectType): Boolean = {
            subclass != thisClass &&
                project.classHierarchy.isSubtypeOf(subclass, thisClass).isYes
        }

        /*
         * For a given method body, collects a list of all subclass methods that are
         * called through INVOKESTATIC instructions.
         *
         * @param toplevelClass class for which all called subclass methods should be
         * determined
         * @param body instructions of the method of toplevelClass that should be
         * searched for subclass accesses
         * @return pairs of the `ObjectType` and corresponding `Method` that are called
         */
        def getCalls(
            toplevelClass: ObjectType,
            body: Code): Array[(ObjectType, Method)] = {

            body.instructions.collect {
                case INVOKESTATIC(subclass, name, desc) if derivedFrom(subclass,
                    toplevelClass) && !project.classHierarchy.isInterface(subclass) ⇒

                    (subclass, project.classHierarchy.
                        resolveMethodReference(subclass, name, desc, project))
            }.
                filter(_._2.isDefined).
                map(call ⇒ (call._1, call._2.get))
        }

        /*
         * For a given method body, recursively collects a list of the names of accessed
         * static fields, including accesses from the given method body, and accesses from
         * methods called in the given method body.
         *
         * @param toplevelClass class for which should be determined which fields it
         * accesses
         * @param thisClass class that should be examined for static field accesses of
         * subclasses of topLevelClass
         * @param body corresponding method that should be examined for static field
         * accesses of subclasses of topLevelClass
         * @return pairs of subclasses and corresponding field names that are accessed
         * (transitively) by the method that corresponds to thisClass and body
         */
        def getAccesses(
            toplevelClass: ObjectType,
            thisClass: ObjectType,
            body: Code): Array[(ObjectType, String)] = {

            var results: Array[(ObjectType, String)] =
                body.instructions.collect {
                    case GETSTATIC(subclass, fieldName, _) if derivedFrom(subclass,
                        toplevelClass) ⇒ (subclass, fieldName)
                }

            for ((subclass, MethodWithBody(calledBody)) ← getCalls(toplevelClass, body)) {
                results ++= getAccesses(toplevelClass, subclass, calledBody)
            }

            results
        }

        var reports: List[ClassBasedReport[Source]] = List.empty

        for {
            classFile ← project.classFiles
            if !project.isLibraryType(classFile)
            clinit @ MethodWithBody(body) ← classFile.staticInitializer.toSeq
        } {
            val toplevelClass = classFile.thisType
            for (
                (subclass, fieldName) ← getAccesses(toplevelClass, toplevelClass, body)
            ) {
                reports = ClassBasedReport(
                    project.source(toplevelClass),
                    Severity.Error,
                    toplevelClass,
                    "Class uses uninitialized field "+fieldName+" of subclass "+
                        subclass.fqn+" during initialization.") :: reports
            }
        }

        reports
    }
}
