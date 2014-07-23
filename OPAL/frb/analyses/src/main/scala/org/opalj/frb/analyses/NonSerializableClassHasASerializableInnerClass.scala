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

import bi.AccessFlagsMatcher

import br._
import br.analyses._

/**
 * This analysis reports outer classes that have (non-`static`) inner `Serializable`
 * classes without themselves being `Serializable`.
 *
 * This situation is problematic, because the serialization of the inner class would
 * require – due to the link to its outer class – always the serialization of the outer
 * class which will, however, fail.
 *
 * ==Implementation Note==
 * This analysis is implemented using the traditional approach where each analysis
 * analyzes the project's resources on its own and fully controls the process.
 *
 * @author Michael Eichberg
 */
class NonSerializableClassHasASerializableInnerClass[Source]
        extends FindRealBugsAnalysis[Source] {

    def description: String =
        "Identifies (non-static) inner classes that are serializable, "+
            "but where the outer class is not."

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

        import project.classHierarchy.isSubtypeOf

        val Serializable = ObjectType.Serializable

        // If it's unknown, it's neither possible nor necessary to collect subtypes
        if (project.classHierarchy.isUnknown(Serializable)) {
            return Iterable.empty
        }

        for {
            serializableType ← project.classHierarchy.allSubtypes(Serializable, false)
            classFile ← project.classFile(serializableType)
            if !project.isLibraryType(classFile)
            (outerType, AccessFlagsMatcher.NOT_STATIC()) ← classFile.outerType
            /* if we know nothing about the class, then we never generate a warning */
            if isSubtypeOf(outerType, Serializable).isNo
        } yield {
            ClassBasedReport(
                project.source(outerType),
                Severity.Error,
                outerType,
                "Has a serializable non-static inner class ("+serializableType.toJava+
                    "), but is not serializable itself"
            )
        }
    }
}
