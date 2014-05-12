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
import AnalysesHelpers._

/**
 * This analysis reports classes with an `@Immutable` annotation inheriting a class
 * without such annotation. It is to be assumed, that the superclass is mutable and thus
 * introduces mutable behaviour into the reported class.
 *
 * @author Roberts Kolosovs
 */
class ImmutableClassInheritsMutableClass[Source]
        extends MultipleResultsAnalysis[Source, ClassBasedReport[Source]] {

    def description: String =
        "Reports classes annotated with an annotation with the simple name Immutable"+
            " that inherit from classes not annotated as immutable."

    /**
     * Checks if a given supertype is annotated with an immutable annotation.
     *
     * @param project the project where the anaylsis is run on
     * @param supertype the supertype to be checked
     * @param allTheImmutableTypes a list of all immutable types of the project
     * @return true if the supertype is annotated with an immutable annotation
     */
    private def superTypeIsAnnotatedWithImmutable(
        project: Project[Source],
        supertype: ObjectType,
        allTheImmutableTypes: Set[ObjectType]): Boolean = {
        val classFile = project.classFile(supertype)
        if (classFile.isDefined) {
            isAnnotatedWith(classFile.get, allTheImmutableTypes)
        } else {
            false
        }
    }

    /**
     * Runs the analysis on the given project.
     *
     * @param project the project to be analysed
     * @param parameters Options for the analysis. Currently unused.
     * @return A list of reports, or an empty list.
     */
    def analyze(
        project: Project[Source],
        parameters: Seq[String] = List.empty): Iterable[ClassBasedReport[Source]] = {
        val immutableAnnotationTypes: Set[ObjectType] =
            collectAnnotationTypes(project, "Immutable")
        for {
            classFile ← project.classFiles
            if !project.isLibraryType(classFile)
            if (isAnnotatedWith(classFile, immutableAnnotationTypes))
            supertype ← project.classHierarchy.superclassType(ObjectType(classFile.fqn))
            if (supertype != ObjectType.Object
                && !superTypeIsAnnotatedWithImmutable(project, supertype,
                    immutableAnnotationTypes))
        } yield {
            ClassBasedReport(
                project.source(classFile.thisType),
                Severity.Warning,
                classFile.thisType,
                "This immutable class inherits a possibly mutable class.")
        }
    }
}
