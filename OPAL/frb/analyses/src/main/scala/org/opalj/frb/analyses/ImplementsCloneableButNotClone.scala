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

import AnalysesHelpers._
import br._
import br.analyses._
import br.instructions._

/**
 * This analysis reports classes which implement `java.lang.Cloneable` but do not
 * implement the `clone()` method. If `Cloneable` was implemented explicitly, then it was
 * probably the programmer's intention to implement a `clone()` too.
 *
 * @author Ralf Mitschke
 * @author Peter Spieler
 */
class ImplementsCloneableButNotClone[Source] extends FindRealBugsAnalysis[Source] {

    /**
     * Returns a description text for this analysis.
     * @return analysis description
     */
    override def description: String =
        "Reports classes that implement the interface cloneable, "+
            "but not the clone() method."

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

        // If it's unknown, it's neither possible nor necessary to check the subtypes
        if (project.classHierarchy.isUnknown(ObjectType.Cloneable)) {
            return Iterable.empty
        }

        for {
            cloneable ← project.classHierarchy.allSubtypes(ObjectType.Cloneable, false)
            classFile ← project.classFile(cloneable)
            if !project.isLibraryType(classFile)
            if !classFile.methods.exists {
                case Method(_, "clone", NoArgsAndReturnObject) ⇒ true
                case _                                         ⇒ false
            }
        } yield {
            ClassBasedReport(
                project.source(classFile.thisType),
                Severity.Error,
                classFile.thisType,
                "Implements java.lang.Cloneable but not clone()")
        }
    }
}
