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
 * This analysis reports classes with `Serializable` subclasses but without a
 * zero-argument constructor. `Serializable` subclasses however require their superclasses
 * to have zero-argument constructors.
 *
 * @author Ralf Mitschke
 * @author Daniel Klauer
 */
class SerializableNoSuitableConstructor[Source]
        extends MultipleResultsAnalysis[Source, ClassBasedReport[Source]] {

    def description: String =
        "Reports superclasses of Serializable classes without zero-arguments constructor."

    /**
     * Runs this analysis on the given project. Returns reports based around
     * the superclass without zero-argument constructors.
     *
     * @param project The project to analyze.
     * @param parameters Options for the analysis. Currently unused.
     * @return A list of reports, or an empty list.
     */
    def analyze(
        project: Project[Source],
        parameters: Seq[String] = List.empty): Iterable[ClassBasedReport[Source]] = {

        // If it's unknown, it's neither possible nor necessary to collect subtypes
        if (project.classHierarchy.isUnknown(ObjectType.Serializable))
            return Iterable.empty

        // For all classes with Serializable subclasses but no zero-arguments constructor.
        for {
            serializableClass ← project.classHierarchy.allSubtypes(
                ObjectType.Serializable, false)
            superClass ← project.classHierarchy.allSupertypes(serializableClass)
            superClassFile ← project.classFile(superClass)
            if !superClassFile.isInterfaceDeclaration &&
                !superClassFile.constructors.exists(
                    _.descriptor.parameterTypes.length == 0)
        } yield {
            ClassBasedReport(
                project.source(superClass),
                Severity.Error,
                superClass,
                "Is superclass of a Serializable class but does not "+
                    "define a zero-arguments constructor.")
        }
    }
}
