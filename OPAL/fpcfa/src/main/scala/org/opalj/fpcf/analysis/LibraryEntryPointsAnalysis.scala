/**
 * BSD 2-Clause License:
 * Copyright (c) 2009 - 2016
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice,
 *  this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *  this list of conditions and the following disclaimer in the documentation
 *  and/or other materials provided with the distribution.
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

import org.opalj.ai.analyses.cg.CallGraphFactory
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.analyses.SomeProject
import org.opalj.br.ClassFile

/**
 * Determines which methods of a library are the entry points of
 *
 *
 * @author Michael Reif
 */
class LibraryEntryPointsAnalysis private (
    val project: SomeProject
) extends {
    private[this] final val AccessKey = ProjectAccessibility.key
    private[this] final val InstantiabilityKey = Instantiability.key
    private[this] final val SerializableType = ObjectType.Serializable
} with FPCFAnalysis {

    @inline private[this] def isClientCallable(method: Method): EntryPoint = {
        propertyStore(method, methods.ClientCallableKey) match {
            case Some(methods.IsClientCallable) | None ⇒ IsEntryPoint
            case Some(methods.NotClientCallable)       ⇒ NoEntryPoint
        }
    }

    def determineProperty(method: Method): PropertyComputationResult = {
        val classFile = project.classFile(method)

        // TODO ask the project whether third party libraries are fully available
        if (project.isLibraryType(classFile))
            // if the analyze the library with the the public interface of third party libraries
            // we don't want the public API only as entry points.
            return ImmediateResult(method, NoEntryPoint);

        if (classFile.isInterfaceDeclaration) {
            if (isOpenLibrary || classFile.isPublic)
                return ImmediateResult(method, IsEntryPoint);
            else {
                val epProperty = isClientCallable(method)
                return ImmediateResult(method, epProperty)
            }
        }

        /* Code from CallGraphFactory.defaultEntryPointsForLibraries */
        if (CallGraphFactory.isPotentiallySerializationRelated(classFile, method)(project.classHierarchy)) {
            return ImmediateResult(method, IsEntryPoint);
        }

        // Now: the method is neither an (static or default) interface method nor and method
        // which relates somehow to object serialization.

        import propertyStore.require
        val c_inst: Continuation = (dependeeE: Entity, dependeeP: Property) ⇒ {
            val isInstantiable = (dependeeP eq Instantiable)
            if (isInstantiable && method.isStaticInitializer)
                Result(method, IsEntryPoint)
            else {
                val isAbstractOrInterface = dependeeE match {
                    case cf: ClassFile ⇒ cf.isAbstract || cf.isInterfaceDeclaration
                    case _             ⇒ false
                }
                val c_vis: Continuation = (dependeeE: Entity, dependeeP: Property) ⇒ {
                    if (dependeeP eq ClassLocal)
                        Result(method, NoEntryPoint)
                    else if ((dependeeP eq Global) && (isInstantiable || method.isStatic))
                        Result(method, IsEntryPoint)
                    else if ((dependeeP eq Global) && !isInstantiable && isAbstractOrInterface ||
                        (dependeeP eq PackageLocal)) {
                        val epProperty = isClientCallable(method)
                        return Result(method, epProperty)
                    } else
                        Result(method, NoEntryPoint)
                }

                require(method, EntryPoint.key, method, AccessKey)(c_vis)
            }
        }

        require(method, EntryPoint.key, classFile, InstantiabilityKey)(c_inst);
    }
}

object LibraryEntryPointsAnalysis extends FPCFAnalysisRunner {

    final def entitySelector: PartialFunction[Entity, Method] = {
        case m: Method if !m.isAbstract && !m.isNative ⇒ m
    }

    override def derivedProperties: Set[PropertyKind] = Set(EntryPoint)

    override def usedProperties: Set[PropertyKind] = {
        Set(ProjectAccessibility, methods.ClientCallableKey, Instantiability)
    }

    /*
     * This recommendations are not transitive. All (even indirect) dependencies are listed here.
     */
    //override def recommendations = Set(FactoryMethodAnalysis, InstantiabilityAnalysis, LibraryLeakageAnalysis, MethodAccessibilityAnalysis)
    override def recommendations: Set[FPCFAnalysisRunner] = {
        Set(
            SimpleInstantiabilityAnalysis,
            methods.CallableFromClassesInOtherPackagesAnalysis,
            MethodAccessibilityAnalysis
        )
    }

    protected[analysis] def start(
        project:       SomeProject,
        propertyStore: PropertyStore
    ): FPCFAnalysis = {
        val analysis = new LibraryEntryPointsAnalysis(project)
        propertyStore <||< (entitySelector, analysis.determineProperty)
        analysis
    }
}
