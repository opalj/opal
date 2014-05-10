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

/**
 * This analysis reports `public|protected static` fields (excluding arrays/hash tables)
 * that aren't `final`. Such fields should be made `final`, to disallow modification by
 * other code.
 *
 * @author Ralf Mitschke
 * @author Daniel Klauer
 */
class FieldShouldBeFinal[Source]
        extends MultipleResultsAnalysis[Source, FieldBasedReport[Source]] {

    /**
     * Returns a description text for this analysis.
     * @return analysis description
     */
    def description: String =
        "Reports Public/Protected Static fields that should be made Final"

    /**
     * Runs this analysis on the given project.
     *
     * @param project The project to analyze.
     * @param parameters Options for the analysis. Currently unused.
     * @return A list of reports, or an empty list.
     */
    def analyze(
        project: Project[Source],
        parameters: Seq[String] = List.empty): Iterable[FieldBasedReport[Source]] = {

        // In all class declarations (excluding interfaces), search for `static` fields,
        // either `public` or `protected`, that are neither arrays nor hash tables,
        // and aren't `final`.
        for {
            classFile ← project.classFiles if !classFile.isInterfaceDeclaration
            if !project.isLibraryType(classFile)
            field ← classFile.fields
            if !field.isFinal &&
                field.isStatic &&
                !field.isSynthetic &&
                !field.isVolatile &&
                (field.isPublic || field.isProtected) &&
                !field.fieldType.isArrayType && field.fieldType != HashtableType
        } yield {
            FieldBasedReport(
                project.source(classFile.thisType),
                Severity.Info,
                classFile.thisType,
                field,
                "Should be final")
        }
    }
}
