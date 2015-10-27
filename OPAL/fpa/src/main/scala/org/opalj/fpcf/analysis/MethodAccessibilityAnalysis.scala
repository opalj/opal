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

import org.opalj.br.Method
import org.opalj.br.analyses.SomeProject

/**
 *
 *
 * @author Michael Reif
 */
object MethodAccessibilityAnalysis
        extends AssumptionBasedFixpointAnalysis
        with FilterEntities[Method] {

    val propertyKey = ProjectAccessibility.Key

    private[this] final val ObjectType = org.opalj.br.ObjectType.Object

    val entitySelector: PartialFunction[Entity, Method] = {
        case m: Method if !m.isStatic && m.body.nonEmpty /*FIXME.... native methods are also filtered*/ ⇒ m
    }

    override def determineProperty(
        method: Method)(
            implicit project: SomeProject,
            propertyStore: PropertyStore): PropertyComputationResult = {

        if (method.isPrivate)
            return ImmediateResult(method, ClassLocal);

        if (isOpenLibrary)
            return ImmediateResult(method, Global);

        val classFile = project.classFile(method)
        val isFinalClass = classFile.isFinal
        val isPublicClass = classFile.isPublic

        val isPublicMethod = method.isPublic
        val isProtectedMethod = method.isProtected

        if (isPublicClass && (isPublicMethod || (!isFinalClass && isProtectedMethod)))
            return ImmediateResult(method, Global);

        val classHierarchy = project.classHierarchy
        val classType = classFile.thisType
        val hasSubtypes = classHierarchy.hasSubtypes(classType).isYesOrUnknown

        val numSupertypes = classHierarchy.directSupertypes(classType).
            filter { supertype ⇒ supertype ne ObjectType }.size // FIXME Smells

        if ((isPublicMethod || isProtectedMethod) && hasSubtypes || numSupertypes > 0) {
            def c(dependeeE: Entity, dependeeP: Property) = {
                if (dependeeP == NoLeakage)
                    Result(method, PackageLocal)
                else
                    Result(method, Global)
            }

            return propertyStore.require(method, propertyKey, method, LibraryLeakage.Key)(c);
        }

        ImmediateResult(method, PackageLocal)
    }
}