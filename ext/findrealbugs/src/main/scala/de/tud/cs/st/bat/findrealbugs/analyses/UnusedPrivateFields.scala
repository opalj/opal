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
 * This analysis reports `private` fields that are not used.
 *
 * @author Ralf Mitschke
 * @author Daniel Klauer
 * @author Peter Spieler
 */
class UnusedPrivateFields[Source]
        extends MultipleResultsAnalysis[Source, FieldBasedReport[Source]] {

    def description: String = "Reports unused private fields."

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

        // TODO: Currently doesn't detect cases where Serializable is implemented 
        //indirectly, e.g. through java.io.File which implements Serializable but is 
        //typically not analyzed by BAT. Thus, in general, if some super types are 
        //unknown, this analysis should generate reports with lower severity, or perhaps 
        //none at all, about serialVersionUID.
        val serializables = project.classHierarchy.allSubtypes(ObjectType.Serializable,
            false)

        /**
         * Check whether a field is the special `serialVersionUID` field of a
         * `Serializable` class. It is always used internally by the JVM, and should not
         * be reported by this analysis.
         */
        def isSerialVersionUID(declaringClass: ObjectType, field: Field): Boolean = {
            // Declaring class must implement Serializable, or else serialVersionUID
            // fields are not special.
            serializables.contains(declaringClass) &&
                // The field must be "long serialVersionUID". Access flags do not matter
                // though.
                (field match {
                    case Field(_, "serialVersionUID", LongType) ⇒ true
                    case _                                      ⇒ false
                })
        }

        val unusedFields = for (
            classFile ← project.classFiles if !classFile.isInterfaceDeclaration
        ) yield {
            val declaringClass = classFile.thisType

            var privateFields: Map[String, (ClassFile, Field)] = Map.empty

            for (
                field ← classFile.fields if field.isPrivate &&
                    !isSerialVersionUID(declaringClass, field)
            ) {
                privateFields += field.name -> (classFile, field)
            }

            for {
                method ← classFile.methods if method.body.isDefined
                FieldReadAccess(`declaringClass`, name, _) ← method.body.get.instructions
            } {
                privateFields -= name
            }

            privateFields.values
        }

        for ((classFile, field) ← unusedFields.flatten) yield {
            FieldBasedReport(
                project.source(classFile.thisType),
                Severity.Info,
                classFile.thisType,
                field,
                "Is private and unused")
        }
    }
}
