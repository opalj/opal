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
package fpcf
package analysis

import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.analyses.SomeProject

sealed trait EntryPoint extends Property {
    final def key = EntryPoint.Key // All instances have to share the SAME key!
}

object EntryPoint {
    final val Key = PropertyKey.create("EntryPoint", IsEntryPoint)
    final val Id = Key.id
}

case object IsEntryPoint extends EntryPoint { final val isRefineable = false }

//
case object NoEntryPoint extends EntryPoint { final val isRefineable = false }

class EntryPointsAnalysis private (
    project: SomeProject
)
        extends {
            private[this] final val AccessKey = ProjectAccessibility.Key
            private[this] final val InstantiabilityKey = Instantiability.Key
            private[this] final val LibraryLeakageKey = LibraryLeakage.Key
            private[this] final val SerializableType = ObjectType.Serializable
        } with DefaultFPCFAnalysis[Method](
            project, EntryPointsAnalysis.entitySelector
        ) {

    val propertyKey = EntryPoint.Key

    @inline private[this] def leakageContinuation(method: Method): Continuation = {
        (dependeeE: Entity, dependeeP: Property) ⇒
            if (dependeeP == Leakage)
                Result(method, IsEntryPoint)
            else
                Result(method, NoEntryPoint)
    }

    /**
     * Identifies those private static non-final fields that are initialized exactly once.
     */
    def determineProperty(
        method: Method
    ): PropertyComputationResult = {

        val classFile = project.classFile(method)

        if (classFile.isInterfaceDeclaration) {
            if (isOpenLibrary)
                return ImmediateResult(method, IsEntryPoint)
            else if (classFile.isPublic && (method.isStatic || method.isPublic))
                return ImmediateResult(method, IsEntryPoint)
            else {
                import propertyStore.require
                require(method, propertyKey, method, LibraryLeakageKey)(
                    leakageContinuation(method)
                )
            }
        }

        /* Code from CallGraphFactory.defaultEntryPointsForLibraries */
        if (Method.isObjectSerializationRelated(method) &&
            (
                !classFile.isFinal /*we may inherit from Serializable later on...*/ ||
                project.classHierarchy.isSubtypeOf(
                    classFile.thisType,
                    SerializableType
                ).isYesOrUnknown
            )) {
            return ImmediateResult(method, IsEntryPoint)
        }

        // Now: the method is neither an (static or default) interface method nor and method
        // which relates somehow to object serialization.

        import propertyStore.require
        val c_inst: Continuation = (dependeeE: Entity, dependeeP: Property) ⇒ {
            val isInstantiable = (dependeeP == Instantiable)
            if (isInstantiable && method.isStaticInitializer)
                Result(method, IsEntryPoint)
            else {
                val c_vis: Continuation = (dependeeE: Entity, dependeeP: Property) ⇒ {
                    if (dependeeP == ClassLocal)
                        Result(method, NoEntryPoint)
                    else if (dependeeP == Global &&
                        (isInstantiable || method.isStatic))
                        Result(method, IsEntryPoint)
                    else if (dependeeP == PackageLocal)
                        require(method, propertyKey, method, LibraryLeakageKey)(
                            leakageContinuation(method)
                        )
                    else Result(method, NoEntryPoint)
                }

                require(method, propertyKey, method, AccessKey)(c_vis)
            }
        }

        return require(method, propertyKey, classFile, InstantiabilityKey)(c_inst)
    }
}

object EntryPointsAnalysis extends FPCFAnalysisRunner[EntryPointsAnalysis] {

    private[EntryPointsAnalysis] def entitySelector: PartialFunction[Entity, Method] = {
        case m: Method if !m.isAbstract && !m.isNative ⇒ m
    }

    protected[analysis] def start(project: SomeProject): Unit = {
        new EntryPointsAnalysis(project)
    }

    override protected[analysis] def derivedProperties = Set(EntryPoint.Id)

    override protected[analysis] def usedProperties = Set(ProjectAccessibility.Id, LibraryLeakage.Id, Instantiability.Id)

    /*
     * This recommendations are not transitive. All (even indirect) dependencies are listed here.
     */
    //override def recommendations = Set(FactoryMethodAnalysis, InstantiabilityAnalysis, LibraryLeakageAnalysis, MethodAccessibilityAnalysis)
    override def recommendations = Set(SimpleInstantiabilityAnalysis, LibraryLeakageAnalysis, MethodAccessibilityAnalysis)
}