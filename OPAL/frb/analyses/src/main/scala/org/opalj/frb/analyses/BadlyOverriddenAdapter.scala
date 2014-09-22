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

/**
 * This analysis reports methods that have the same name but different signatures when
 * compared to the methods in an AWT/Swing super class whose name ends in "Adapter".
 *
 * Such methods do not override those from the super class, but, since the developer has
 * given them the same name, we can assume that it was the developer's intention to do
 * just that, and that the developer simply used the wrong parameters and forgot using
 * the `@Override` annotation.
 *
 * @author Florian Brandherm
 */
class BadlyOverriddenAdapter[Source] extends FindRealBugsAnalysis[Source] {

    override def description: String =
        "Reports methods with the same name but different signatures when compared to "+
            "those from an AWT/Swing Adapter super class."

    /**
     * Runs this analysis on the given project.
     *
     * @param project The project to analyze.
     * @param parameters Options for the analysis. Currently unused.
     * @return An `Iterable` of reports that can be empty.
     */
    def doAnalyze(
        project: Project[Source],
        parameters: Seq[String] = List.empty,
        isInterrupted: () ⇒ Boolean): Iterable[SourceLocationBasedReport[Source]] = {

        /*
         * Heuristic to check whether a type is an Adapter.
         *
         * @param classType The type to check.
         * @return `true`, if the given type seems to be an adapter, `false` otherwise.
         */
        def isAdapter(classType: ObjectType): Boolean = {
            (classType.packageName == "java/awt/event" ||
                classType.packageName == "javax/swing/event") &&
                classType.fqn.endsWith("Adapter")
        }

        /*
         * Determines whether a method overrides a method in a super class.
         *
         * @param method The method to check.
         * @param superclass A super class of the class containing the method.
         * @return `true`, if the given super class contains a method with the same name
         * and signature as the given method. `false` otherwise.
         */
        def overridesTheAdapter(method: Method, superclass: ClassFile): Boolean = {
            superclass.methods.exists(supermethod ⇒
                supermethod.name == method.name &&
                    supermethod.descriptor == method.descriptor
            )
        }

        // For every class implementing an Adapter...
        (for {
            classFile ← project.classFiles
            if !project.isLibraryType(classFile)
            superclass ← project.classHierarchy.allSupertypes(classFile.thisType)
            if (isAdapter(superclass))
            adapterClassFile ← project.classFile(superclass).toSeq
        } yield {

            // If the subclass has any methods with same name as one from the Adapter
            // class, but the signature differs from that of the method in the Adapter
            // class, we want to report it.
            //
            // However, if there are multiple overloads, one of which implements the
            // Adapter method properly, then we do not want to report the other overloads.
            //
            // Additionally, if there are multiple overloads, and none of them implement
            // the Adapter method properly, then we want to report all of them at once,
            // not just the first one that was found, or similar.
            //
            // Note: This does not give special treatment to the case where the Adapter
            // itself provides multiple overloads that would then have to be overridden
            // by multiple overloaded methods in the subclass, because it is not
            // necessary:
            // 1) If we've found a method that has the same name as a method from the
            //    super class but still does not properly override it because it has a
            //    different signature, then we can't exactly tell which super method
            //    overload it should override anyways.
            // 2) We don't check for "missing implementations" of certain Adapter methods
            //    here anyways.

            // Group the subclass'es methods by their name
            // (i.e. determine the groups of overloads)
            val methodsWithoutConstructors = classFile.methods.filter(!_.isConstructor)
            val distinctMethodNames = methodsWithoutConstructors.map(_.name).distinct
            val methodsByName = distinctMethodNames.map(
                name ⇒ (name, methodsWithoutConstructors.filter(_.name == name))
            )

            // For every group of overloads, if none of the overloads in that group
            // properly overrides an Adapter method...
            for {
                (name, methods) ← methodsByName
                if adapterClassFile.findMethod(name).isDefined &&
                    !methods.exists(overridesTheAdapter(_, adapterClassFile))
            } yield {
                val supermethodname = adapterClassFile.thisType.toJava+"."+name+"()"

                // If there's just a single overload, report this individual method.
                if (methods.size == 1) {
                    val method = methods.head
                    MethodBasedReport(
                        project.source(classFile.thisType),
                        Severity.Warning,
                        classFile.thisType,
                        method,
                        "Does not override "+supermethodname+
                            " (incompatible signatures)."
                    )
                } else {
                    // Otherwise, report the whole group of overloads all at once.
                    ClassBasedReport(
                        project.source(classFile.thisType),
                        Severity.Warning,
                        classFile.thisType,
                        "Has multiple '"+name+"()' methods, "+
                            "but not one of them overrides "+supermethodname+
                            " (incompatible signatures)."
                    )
                }
            }
        }).flatten
    }
}
