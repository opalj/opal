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
package br
package analyses
package fp

import org.opalj.fp.Property
import org.opalj.fp.PropertyKey
import org.opalj.br.analyses.Project
import java.net.URL
import org.opalj.br.Method
import org.opalj.fp.PropertyStore
import org.opalj.fp.PropertyComputationResult
import org.opalj.fp.Result
import org.opalj.AnalysisModes
import org.opalj.fpa.FilterEntities
import org.opalj.fp.Entity
import org.opalj.fpa.AssumptionBasedFixpointAnalysis

/**
 * @author Michael Reif
 */

sealed trait ProjectAccessibility extends Property {
    final def key = ProjectAccessibility.Key
}

object ProjectAccessibility {
    final val Key = PropertyKey.create("Accessible", Global)
}

case object Global extends ProjectAccessibility { final val isRefineable = false }

case object PackageLocal extends ProjectAccessibility { final val isRefineable = false }

case object ClassLocal extends ProjectAccessibility { final val isRefineable = false }

object ShadowingAnalysis
        extends AssumptionBasedFixpointAnalysis
        with FilterEntities[Method] {

    val propertyKey = ProjectAccessibility.Key

    /**
     * Determines the [[ProjectAccessibility]] property of static methods considering shadowing of methods
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

        if (method.isPrivate)
            return Result(method, ClassLocal)

        if (isOpenPackagesAssumption)
            return Result(method, Global)

        val declaringClass = project.classFile(method)
        val pgkVisibleMethod = method.isPackagePrivate

        if (pgkVisibleMethod)
            return Result(method, PackageLocal)

        if (declaringClass.isPublic && (method.isPublic || method.isProtected))
            return Result(method, Global)

        val declaringClassType = declaringClass.thisType

        val methodDescriptor = method.descriptor
        val methodName = method.name

        val subtypes = project.classHierarchy.allSubtypes(declaringClassType, false)

        subtypes foreach { subtype ⇒
            project.classFile(subtype) map { classFile ⇒
                if (classFile.isPublic) {
                    val potentialMethod = classFile.findMethod(methodName, methodDescriptor)
                    val hasMethod = potentialMethod.nonEmpty && potentialMethod.map { curMethod ⇒
                        curMethod.visibilityModifier.equals(method.visibilityModifier)
                    }.getOrElse(false)

                    if (!hasMethod) {
                        val curSuperTypes = project.classHierarchy.allSupertypes(subtype, false)

                        val inheritsMethod = curSuperTypes.exists { supertype ⇒
                            project.classFile(supertype).map { supClassFile ⇒
                                supClassFile.findMethod(methodName, methodDescriptor).nonEmpty
                            }.getOrElse(true) && (supertype ne declaringClassType)
                        }

                        if (inheritsMethod)
                            return Result(method, Global)
                    }
                }
            }
        }

        // If no subtype is found, the method is not accessible
        Result(method, PackageLocal)
    }

    val entitySelector: PartialFunction[Entity, Method] = {
        case m: Method if m.isStatic && !m.isStaticInitializer ⇒ m
    }
}