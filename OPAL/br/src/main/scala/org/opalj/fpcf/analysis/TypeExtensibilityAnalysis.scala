/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
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

import scala.annotation.tailrec
import scala.collection.mutable.Queue

import org.opalj.br.analyses.SomeProject
import org.opalj.br.ObjectType
import org.opalj.fpcf.properties.TypeExtensibility
import org.opalj.fpcf.properties.ExtensibleType
import org.opalj.fpcf.properties.NotExtensibleType
import org.opalj.fpcf.properties.MaybeExtensibleType

/**
 * Determines if a class is extensible (i.e., can be inherited from). This property generally
 * depends on the kind of the project. If the project is an application, all classes
 * are considered to be closed; i.e., the class hierarchy is considered to be fixed; if the
 * analyzed project is a library, then the result depends on the concrete assumption about the
 * openness of the library.
 *
 * However, package visible classes in packages starting with "java." are always treated as not
 * client extensible as this would require that those classes are defined in the respective
 * package which is prevented by the default `ClassLoader` in all cases.
 *
 * @note   Since the computed property is a set property, other analyses have to wait till the
 *         property is computed.
 *
 * @author Michael Reif
 */
// Add Java9 support for modules
class TypeExtensibilityAnalysis private (val project: SomeProject) extends FPCFAnalysis {

    /**
     * Computes the extensibility of the given class or interface and also of all supertypes of it.
     *
     * A type is considered as extensible if one of the criteria matches:
     *  - it is not (effectively) final and either public or the analysis mode is OPA,
     *  - one of its subtypes is extensible.
     */
    @tailrec final def determineExtensibility(
        typesToProcess:       Queue[ObjectType],
        hasExtensibleSubtype: Array[Boolean],
        hasUnknownSubtype:    Array[Boolean],
        isEnqueued:           Array[Boolean]
    ): Unit = {
        // We use a queue to ensure that we always first process all subtypes of a type to
        // ensure that we have final knowledge about the subtypes' extensibility.

        val objectType = typesToProcess.dequeue()
        val oid = objectType.id
        project.classFile(objectType) match {
            case Some(classFile) ⇒

                val isExtensible =
                    hasExtensibleSubtype(oid) || {
                        !classFile.isEffectivelyFinal /* <= true only in VERY broken projects... */ &&
                            (
                                classFile.isPublic ||
                                (isOpenLibrary && !classFile.thisType.packageName.startsWith("java."))
                            )
                    }

                if (isExtensible) {
                    classFile.superclassType.foreach(st ⇒ hasExtensibleSubtype(st.id) = true)
                    classFile.interfaceTypes.foreach(it ⇒ hasExtensibleSubtype(it.id) = true)
                    propertyStore.set(classFile, ExtensibleType)
                } else if (hasUnknownSubtype(oid)) {
                    propertyStore.set(classFile, MaybeExtensibleType)
                } else {
                    propertyStore.set(classFile, NotExtensibleType)
                }

            case None ⇒
                classHierarchy.directSupertypes(objectType).foreach { superType ⇒
                    hasUnknownSubtype(superType.id) = true
                }
        }

        classHierarchy.directSupertypes(objectType).foreach { superType ⇒
            if (!isEnqueued(superType.id)) {
                typesToProcess.enqueue(superType)
                isEnqueued(superType.id) = true
            }
        }

        if (typesToProcess.nonEmpty) {
            // tail-recursive call... to process the workqueue
            determineExtensibility(
                typesToProcess,
                hasExtensibleSubtype, hasUnknownSubtype,
                isEnqueued
            )
        }
    }
}

object TypeExtensibilityAnalysis extends FPCFAnalysisRunner {

    override def derivedProperties: Set[PropertyKind] = Set(TypeExtensibility)

    def start(project: SomeProject, propertyStore: PropertyStore): FPCFAnalysis = {

        import AnalysisModes.{isDesktopApplication, isJEE6WebApplication}
        val analysis = new TypeExtensibilityAnalysis(project)
        val analysisMode = project.analysisMode

        if (isDesktopApplication(analysisMode) || isJEE6WebApplication(analysisMode)) {
            // application types can not be extended
            project.allClassFiles.foreach { cf ⇒ propertyStore.set(cf, NotExtensibleType) }
        } else {

            val leafTypes = project.classHierarchy.leafTypes
            if (leafTypes.isEmpty)
                return analysis;

            val typesToProcess = Queue.empty[ObjectType] ++ leafTypes
            val hasExtensibleSubtype = new Array[Boolean](ObjectType.objectTypesCount)
            val hasUnknownSubtype = new Array[Boolean](ObjectType.objectTypesCount)
            val isEnqueued = new Array[Boolean](ObjectType.objectTypesCount)
            typesToProcess foreach { ot ⇒
                isEnqueued(ot.id) = true
                if (project.classHierarchy.hasSubtypes(ot).isYesOrUnknown) {
                    // this test should never succeed.... because we start with the leaf types...
                    // hence, the types are known...
                    throw new UnknownError()
                    // hasUnknownSubtype(oid) = true
                }
            }

            analysis.determineExtensibility(
                typesToProcess,
                hasExtensibleSubtype, hasUnknownSubtype,
                isEnqueued
            )
        }
        analysis
    }
}
