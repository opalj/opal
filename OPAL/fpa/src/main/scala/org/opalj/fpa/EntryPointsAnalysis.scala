/**
 * BSD 2-Clause License:
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

import org.opalj.fp.Entity
import org.opalj.fp.PropertyStore
import org.opalj.fp.PropertyKey
import org.opalj.fp.Property
import org.opalj.fp.PropertyComputationResult
import org.opalj.br.analyses.SomeProject
import org.opalj.fp.Result
import org.opalj.fp.ImmediateResult
import org.opalj.br.Method
import org.opalj.fp.Continuation
import org.opalj.br.ObjectType

sealed trait EntryPoint extends Property {
    final def key = EntryPoint.Key // All instances have to share the SAME key!
}

object EntryPoint {
    final val Key = PropertyKey.create("EntryPoint", IsEntryPoint)
}

case object IsEntryPoint extends EntryPoint { final val isRefineable = false }

case object NoEntryPoint extends EntryPoint { final val isRefineable = false }

object EntryPointsAnalysis
        extends AssumptionBasedFixpointAnalysis
        with FilterEntities[Method] {

    private[this] final val accessKey = ProjectAccessibility.Key
    private[this] final val instantiabilityKey = Instantiability.Key

    private[this] final val serializableType = ObjectType.Serializable

    val propertyKey = EntryPoint.Key

    val entitySelector: PartialFunction[Entity, Method] = {
        case m: Method if !m.isAbstract ⇒ m
    }

    /**
     * Identifies those private static non-final fields that are initialized exactly once.
     */
    def determineProperty(
        method: Method)(
            implicit project: SomeProject,
            propertyStore: PropertyStore): PropertyComputationResult = {

        val classFile = project.classFile(method)

        /* Code from CallGraphFactory.defaultEntryPointsForLibraries */
        if (Method.isObjectSerializationRelated(method) &&
            (
                !classFile.isFinal /*we may inherit from Serializable later on...*/ ||
                project.classHierarchy.isSubtypeOf(
                    classFile.thisType,
                    serializableType).isYesOrUnknown
            ))
            return ImmediateResult(method, IsEntryPoint)

        import propertyStore.require
        val c_inst: Continuation =
            (dependeeE: Entity, dependeeP: Property) ⇒ {

                val isInstantiable = (dependeeP eq Instantiable)
                if (isInstantiable) {
                    if (method.isStaticInitializer)
                        return ImmediateResult(method, IsEntryPoint)
                }

                val c_vis: Continuation =
                    (dependeeE: Entity, dependeeP: Property) ⇒
                        if (dependeeP == Global &&
                            (isInstantiable && !method.isStatic) ||
                            (method.isStatic || method.isConstructor))
                            Result(method, IsEntryPoint)
                        else Result(method, NoEntryPoint)

                return require(method, propertyKey, method, accessKey)(c_vis)
            }

        return require(method, propertyKey, classFile, instantiabilityKey)(c_inst)
    }
}