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

import AnalysesHelpers._
import resolved._
import resolved.analyses._
import resolved.instructions._
import resolved.ai.project._
import ai._
import ai.domain.l1._

/**
 * This analysis reports classes that are annotated with an annotation with the simple
 * name Immutable and contain a native method.
 *
 * @author Roberts Kolosovs
 * @author Peter Spieler
 */
class NativeMethodInImmutableClass[Source]
        extends MultipleResultsAnalysis[Source, MethodBasedReport[Source]] {

    /**
     * Returns a description text for this analysis.
     *
     * @return analysis description
     */
    def description: String =
        "Reports Classes annotated with an annotation with the simple name Immutable"+
            " that contain a native method."

    /**
     * Analyzes given project and returns a list of FieldBasesReports containing all
     * instances of the error.
     *
     * @param project The project to analyze.
     * @param parameters Options for the analysis. Currently unused.
     * @return A list of reports, or an empty list.
     */
    def analyze(
        project: Project[Source],
        parameters: Seq[String] = List.empty): Iterable[MethodBasedReport[Source]] = {
        val immutableAnnotationTypes: Set[ObjectType] =
            collectAnnotationTypes(project, "Immutable")
        for {
            classFile ← project.classFiles
            if !project.isLibraryType(classFile)
            if isAnnotatedWith(classFile, immutableAnnotationTypes)
            method ← classFile.methods
            if method.isNative
        } yield {
            MethodBasedReport(
                project.source(classFile.thisType),
                Severity.Info,
                classFile.thisType,
                method,
                "is a native method in an immutable class.")
        }
    }
}
