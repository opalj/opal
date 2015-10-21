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
package fpa

import org.opalj.fp.PropertyStore
import org.opalj.fp.PropertyComputationResult
import org.opalj.br.Method
import org.opalj.fp.Entity
import org.opalj.fp.Property
import org.opalj.fp.PropertyKey
import org.opalj.br.analyses.Project
import org.opalj.br.ObjectType
import org.opalj.fp.ImmediateResult
import org.opalj.br.analyses.SomeProject

/**
 * This property expresses the leakage of methods to the client such that
 * the method is called by a client. A method does only leak if it gets accessible
 * though inheritance where a immediate non-abstract subclass inherits the target method.
 */
sealed trait LibraryLeakage extends Property {
    final def key = LibraryLeakage.Key
}

object LibraryLeakage {
    final val Key = PropertyKey.create("Leakage", Leakage)
}

case object Leakage extends LibraryLeakage { final val isRefineable = false }

case object PotentialLeakage extends LibraryLeakage { final val isRefineable = false }

case object NoLeakage extends LibraryLeakage { final val isRefineable = false }

/**
 * This Analysis determines the ´LibraryLeakage´ property of a method. A method is considered as leaked
 * if it is overridden in every immediate non-abstract subclass.
 *
 * In the following scenario, m defined by B overrides m in C and (in this specific scenario) m in C is
 * also always overridden.
 * {{{
 * /*package visible*/ class C { public Object m() }
 * /*package visible*/ abstract class A extends C { /*empty*/ }
 * public class B extends A { public Object m() }
 * }}}
 *
 *  @author Michael Reif
 */
object LibraryLeakageAnalysis
        extends AssumptionBasedFixpointAnalysis
        with FilterEntities[Method] {

    val propertyKey = ProjectAccessibility.Key

    val objectType = ObjectType.Object

    /**
     * Determines the [[LibraryLeakage]] property of non-static methods. It is tailored to entry point
     *  set computation where we have to consider different kind of program/library usage scenarios.
     * Computational differences regarding static methods are :
     *  - private methods can be handled equal in every context
     *  - if OPA is met, all package visible classes are visible which implies that all non-private methods are
     *    visible too
     *  - if CPA is met, methods in package visible classes are not visible by default.
     *
     */
    def determineProperty(
        method: Method)(
            implicit project: SomeProject,
            store: PropertyStore): PropertyComputationResult = {

        if (method.isPrivate || method.isConstructor)
            return ImmediateResult(method, NoLeakage)

        val declClass = project.classFile(method)

        if (declClass.isFinal)
            return ImmediateResult(method, NoLeakage)

        if (isOpenLibrary)
            return ImmediateResult(method, Leakage)

        /* A package private method can not leak to the client, if CPA is applied */
        if (isClosedLibrary && method.isPackagePrivate)
            return ImmediateResult(method, NoLeakage)

        var relevantSubtypes = project.classHierarchy.directSubtypesOf(declClass.thisType)

        /*
         * At this point: 
         *  - A class can't be final.
         *  - A method can't be package visible.
         *  - A method can't be private or final.
         */
        if (relevantSubtypes.isEmpty && declClass.isPublic)
            return ImmediateResult(method, Leakage)

        while (relevantSubtypes.nonEmpty) {
            val curSubtype = relevantSubtypes.head
            val classFileO = project.classFile(curSubtype)
            if (classFileO.isDefined) {
                val classFile = classFileO.get
                val declMethod = classFile.findMethod(method.name, method.descriptor)
                if (declMethod.isDefined) {
                    relevantSubtypes -= curSubtype
                } else {
                    if (!classFile.isPublic || method.isPackagePrivate) {
                        relevantSubtypes -= curSubtype
                        relevantSubtypes ++= project.classHierarchy.directSubtypesOf(curSubtype)
                    } else return ImmediateResult(method, Leakage)
                }
            } else return ImmediateResult(method, Leakage)
        }

        ImmediateResult(method, NoLeakage)
    }

    val entitySelector: PartialFunction[Entity, Method] = {
        case m: Method if !m.isStatic && !m.isAbstract ⇒ m
    }
}