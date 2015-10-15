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

package org.opalj.fpa

import org.opalj.fp.Result
import java.net.URL
import org.opalj.fp.PropertyStore
import org.opalj.fp.PropertyComputationResult
import org.opalj.br.Method
import org.opalj.fp.Entity
import org.opalj.fp.Property
import org.opalj.fp.PropertyKey
import org.opalj.br.ClassFile
import org.opalj.br.analyses.Project
import org.opalj.br.ObjectType

sealed trait Overridden extends Property {
    final def key = Overridden.Key
}

object Overridden {
    final val Key = PropertyKey.create("Overridden", NonOverridden)
}

case object IsOverridden extends Overridden { final val isRefineable = false }

case object NonOverridden extends Overridden { final val isRefineable = false }

case object CantNotBeOverridden extends Overridden { final val isRefineable = false }

/**
 * @author Michael Reif
 *
 * This Analysis determines the ´Overridden´ property of a method. A method is considered as overridden
 * if it is overridden in every subclass.
 */
object OverridingAnalysis
        extends AssumptionBasedFixpointAnalysis
        with FilterEntities[Method] {

    val propertyKey = ProjectAccessibility.Key

    val objectType = ObjectType.Object

    /**
     * Determines the [[Overridden]] property of static methods considering shadowing of methods
     * provided by super classes. It is tailored to entry point set computation where we have to consider different kind
     * of assumption depending on the analyzed program.
     *
     * Computational differences regarding static methods are :
     *  - private methods can be handled equal in every context
     *  - if OPA is met, all package visible classes are visible which implies that all non-private methods are
     *    visible too
     *  - if CPA is met, methods in package visible classes are not visible by default.
     *
     */
    def determineProperty(
        method: Method)(
            implicit project: Project[URL],
            store: PropertyStore): PropertyComputationResult = {

        if (method.isPrivate || method.isFinal)
            return Result(method, CantNotBeOverridden)

        // this would be way more efficient when caluclating entry points
        // but if you want to have a dedicated looking at the isOverridden property,
        // it will lead to incorrect results
        /*if (isOpenPackagesAssumption)
            return Result(method, Global) */

        val declaringClass = project.classFile(method)

        if (declaringClass.isFinal)
            return Result(method, CantNotBeOverridden)

        val declClassType = declaringClass.thisType
        val methodDescriptor = method.descriptor
        val methodName = method.name
        val packageVisible = method.isPackagePrivate

        val subtypes = project.classHierarchy.allSubtypes(declClassType, false)

        if (subtypes.size == 0)
            return Result(method, NonOverridden)

        subtypes foreach { subtype ⇒
            project.classFile(subtype) map { classFile ⇒
                val potentialMethod = classFile.findMethod(methodName, methodDescriptor)
                val couldInheritMethod = potentialMethod.isEmpty || !potentialMethod.map { curMethod ⇒
                    curMethod.visibilityModifier.equals(method.visibilityModifier)
                }.getOrElse(false)

                val packagePrivateCondition =
                    if (packageVisible) subtype.packageName == declClassType.packageName
                    else true

                if (couldInheritMethod && packagePrivateCondition) {
                    val superuperTypes = project.classHierarchy.allSupertypes(subtype, false)

                    val inheritsMethod = !superuperTypes.exists { supertype ⇒
                        (supertype ne declClassType) &&
                            (supertype ne objectType) &&
                            project.classFile(supertype).map { supClassFile ⇒
                                supClassFile.findMethod(methodName, methodDescriptor).nonEmpty
                            }.getOrElse(true)
                    }

                    if (inheritsMethod)
                        return Result(method, NonOverridden)
                }
            }
        }

        //        if (visibleClass(declaringClass))
        //      return Result(method, PotentiallyOverridden)

        // If no subtype is found, the method is not accessible
        Result(method, IsOverridden)
    }

    val entitySelector: PartialFunction[Entity, Method] = {
        case m: Method if !m.isStatic && m.body.isDefined ⇒ m
    }
}