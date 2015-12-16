/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2015
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
package fpcf
package analysis

import org.opalj.br.ClassFile
import org.opalj.br.analyses.SomeProject
import org.opalj.br.ClassHierarchy
import org.opalj.br.ObjectType

case object IsExtensible extends SetProperty[ClassFile]

class ClassExtensibilityAnalysis private (
        val project: SomeProject
) extends FPCFAnalysis {

    /**
     * Computes the extensibility of a class or an interface and their supertypes w.r.t. to future types. (e.i. in libraries)
     * If the analysis mode is an application mode, no class has to be marked as extensible.
     *
     * A type is considered as extensible if one of the criteria matches:
     * 	- one of its subtypes is extensible
     *  - it is not (effectively) final  and either public or the analyis mode is OPA
     */
    def determineExtensibility(classFile: ClassFile): PropertyComputationResult = {

        val objectType = classFile.thisType

        if (isDesktopApplication || isJEEApplication) {
            // application types can not be extended
            return NoResult
        }

        val classHierarchy = project.classHierarchy

        // from now on we have either an open or a closed library
        if (classFile.isInterfaceDeclaration) {
            if (classFile.isPublic || isOpenLibrary) {
                setExtensibilityForAllSupertypes(objectType, classHierarchy)
            } else {
                classHierarchy.directSupertypes(objectType).foreach { ot ⇒
                    project.classFile(ot) match {
                        case Some(cf) ⇒ determineExtensibility(cf) // return values are ignored here
                        case None     ⇒
                    }
                }
            }
        } else { // we have a class declaration

            if ((classFile.isPublic || isOpenLibrary) &&
                !classFile.isEffectivelyFinal) {

                setExtensibilityForAllSupertypes(objectType, classHierarchy)
            } else {
                classHierarchy.directSupertypes(objectType).foreach { superType ⇒
                    project.classFile(superType) match {
                        case Some(cf) ⇒ determineExtensibility(cf) // return values are ignored here
                        case None     ⇒
                    }
                }
            }
        }

        NoResult
    }

    /**
     * This method sets the IsExtensible set-property to all (reflexive) supertypes
     * of the given `objectType`.
     */
    private[this] def setExtensibilityForAllSupertypes(
        objectType:     ObjectType,
        classHierarchy: ClassHierarchy
    ): Unit = {

        classHierarchy.allSupertypes(objectType, true).foreach { superType ⇒
            project.classFile(superType) match {
                case Some(cf) ⇒ propertyStore.add(IsExtensible)(cf)
                case None     ⇒
            }
        }
    }
}

object ClassExtensibilityAnalysis extends FPCFAnalysisRunner {

    final def entitySelector(leafTypes: Seq[ObjectType]): PartialFunction[Entity, ClassFile] = {
        case cf: ClassFile if leafTypes.contains(cf.thisType) ⇒ cf
    }

    override def derivedProperties: Set[PropertyKind] = Set(IsExtensible)

    protected[analysis] def start(
        project:       SomeProject,
        propertyStore: PropertyStore
    ): FPCFAnalysis = {
        val analysis = new ClassExtensibilityAnalysis(project)
        val leafTypes = project.classHierarchy.leafTypes
        propertyStore <||< (entitySelector(leafTypes), analysis.determineExtensibility)
        analysis
    }
}