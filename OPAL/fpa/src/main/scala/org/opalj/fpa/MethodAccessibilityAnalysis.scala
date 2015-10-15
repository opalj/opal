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
package org.opalj.fpa

import org.opalj.br.Method
import org.opalj.br.analyses.Project
import org.opalj.fp.Entity
import org.opalj.fp.PropertyStore
import org.opalj.fp.PropertyComputationResult
import java.net.URL
import org.opalj.fp.ImmediateResult
import org.opalj.fp.Continuation
import org.opalj.fp.Property

/**
 *
 *
 * @author Michael Reif
 */
object MethodAccessibilityAnalysis
        extends AssumptionBasedFixpointAnalysis
        with FilterEntities[Method] {

    val propertyKey = ProjectAccessibility.Key

    val entitySelector: PartialFunction[Entity, Method] = {
        case m: Method if !m.isStatic && m.body.nonEmpty ⇒ m
    }

    override def determineProperty(
        method: Method)(
            implicit project: Project[URL],
            propertyStore: PropertyStore): PropertyComputationResult = {

        if (method.isPrivate)
            return ImmediateResult(method, ClassLocal)

        if (isOpenPackagesAssumption)
            return ImmediateResult(method, Global)

        val declClass = project.classFile(method)

        if (declClass.isPublic && method.isPublic)
            ImmediateResult(method, Global)

        val numSubtypes = project.classHierarchy.allSubtypes(declClass.thisType, false).size
        if ((method.isPublic || method.isProtected) &&
            numSubtypes > 0) {
            val c: Continuation =
                (dependeeE: Entity, dependeeP: Property) ⇒
                    if (dependeeP == IsOverridden)
                        ImmediateResult(method, PackageLocal)
                    else ImmediateResult(method, Global)

            import propertyStore.require
            return require(method, propertyKey,
                method, Overridden.Key)(c)
        }

        /*
         * declClass could also escape through a supertype that also has a method with
         * the same signature. In that case, the method could be called indirectly over a supertype.
         */

        ImmediateResult(method, PackageLocal)
    }
}