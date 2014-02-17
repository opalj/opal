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
 * This analysis reports methods which access their class's fields and are called by
 * their superclass's constructor.
 *
 * This is a bug, because while the super constructor is running, the derived class's
 * constructor hasn't run yet, and the derived class's fields aren't initialized yet.
 * Thus it's not safe to access them, and it's not safe for the superclass's constructor
 * to call a method in the subclass which does that.
 *
 * TODO: Ideas for improvement:
 * - Should also check all methods called from such methods
 * - Don't complain about accesses to Static Final fields from such methods, assuming
 *   such fields don't need initialization by a constructor (find out whether that's
 *   correct)
 * - Don't complain about accesses to fields if they don't have a specific initializer,
 *   then the constructor wouldn't do anything anyways and it doesn't make a difference
 *   whether the field is being accessed earlier or not.
 * - Don't complain if the field is written before being read (or written only and never
 *   read) in such methods. If it's written first, then there's no "uninitialized" access.
 * - Test on real Java projects and check whether/how many false-positives are reported
 *
 * @author Roberts Kolosovs
 * @author Ralf Mitschke
 * @author Daniel Klauer
 */
class UrUninitReadCalledFromSuperConstructor[Source]
        extends MultipleResultsAnalysis[Source, MethodBasedReport[Source]] {

    def description: String =
        "Reports classes calling methods of their subclasses in the constructor."

    /**
     * Runs this analysis on the given project.
     *
     * @param project The project to analyze.
     * @param parameters Options for the analysis. Currently unused.
     * @return A list of reports, or an empty list.
     */
    def analyze(
        project: Project[Source],
        parameters: Seq[String] = List.empty): Iterable[MethodBasedReport[Source]] = {

        import project.classHierarchy

        /**
         * Returns true if classFile declares the given Field
         *
         * @param classFile The ClassFile that is supposed to declare a given field
         * @param name The name of the field supposedly declared.
         * @param fieldType Type of the field supposedly declared.
         */
        def declaresField(classFile: ClassFile, name: String,
                          fieldType: FieldType): Boolean = {
            classFile.fields.exists {
                case Field(_, `name`, `fieldType`) ⇒ true
                case _                             ⇒ false
            }
        }

        /**
         * Check whether a method overrides a method in the super type.
         *
         * We only need to check abstract super classes but no super interfaces here,
         * because this analysis checks for a super constructor containing code calling
         * the overridden method. Interfaces, however, can't have constructors.
         *
         * @param classFile Subclass with the supposed overriding method.
         * @param method Method to be checked for being an overriding method.
         * @return True if method in classFile is overriding a method in some superclass.
         */
        def isOverridingMethod(classFile: ClassFile, method: Method): Boolean = {
            // We could also check for an @Override annotation, but that is not reliable
            // as the use of @Override is not required.
            classHierarchy.allSupertypes(classFile.thisType).
                filter(!classHierarchy.isInterface(_)).
                map(classHierarchy.lookupMethodDefinition(_, method.name,
                    method.descriptor, project)).nonEmpty
        }

        /**
         * Checks whether the source method contains calls to the given target method.
         *
         * @param source The method which may contain calls to other methods.
         * @param targetType Type of the method supposedly called.
         * @param target The name of the method supposedly called.
         * @return True if source contains a call to target method of the given type.
         */
        def calls(source: Method, targetType: Type, target: Method): Boolean = {
            source.body.get.instructions.exists {
                // TODO: Simplify by using a matcher that combines all these
                case INVOKEINTERFACE(`targetType`, target.name, target.descriptor) ⇒ true
                case INVOKEVIRTUAL(`targetType`, target.name, target.descriptor) ⇒ true
                case INVOKESTATIC(`targetType`, target.name, target.descriptor) ⇒ true
                case INVOKESPECIAL(`targetType`, target.name, target.descriptor) ⇒ true
                case _ ⇒ false
            }
        }

        /**
         * Returns the super constructor called in the given constructor or None
         *
         * @param constructor Constructor which may or may not call a superconstructor.
         * @return The first superconstructor to be called or None.
         */
        def findCalledSuperConstructor(constructor: Method): Option[Method] = {
            constructor.body.get.instructions.collectFirst({
                case INVOKESPECIAL(targetType, name, desc) ⇒
                    classHierarchy.resolveMethodReference(targetType, name, desc, project)
            }).flatten
        }

        for {
            classFile ← project.classFiles
            method ← classFile.methods
            if !method.isStatic &&
                method.body.isDefined &&
                method.name != "<init>" &&
                isOverridingMethod(classFile, method)
            GETFIELD(declaringClass, fieldName, fieldType) ← method.body.get.instructions
            constructor ← classFile.constructors
            if declaresField(classFile, fieldName, fieldType)
            superConstructor ← findCalledSuperConstructor(constructor)
            superClass = project.classFile(superConstructor)
            if superConstructor.body.isDefined &&
                calls(superConstructor, superClass.thisType, method)
        } yield {
            MethodBasedReport(
                project.source(superClass.thisType),
                Severity.Error,
                method,
                "Called by super constructor ("+superClass.fqn+"), while the class' "+
                    "fields are still uninitialized")
        }
    }
}
