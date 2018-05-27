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
package analyses

import org.opalj.fpcf.properties._
import org.opalj.br.analyses.SomeProject
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.ai.analyses.cg.CallGraphFactory

/**
 * Determines which methods of a library are the entry points.
 *
 * @author Michael Reif
 */
// ONLY TO BE USED/CALLED BY THE ENTRYPOINTSANALYSIS!
private[analyses] class LibraryEntryPointsAnalysis private[analyses] (
        val project: SomeProject
) extends {
    private[this] final val AccessKey = ProjectAccessibility.Key
    private[this] final val InstantiabilityKey = Instantiability.key
    private[this] final val SerializableType = ObjectType.Serializable
} with FPCFAnalysis {

    @inline private[this] def isClientCallable(method: Method): EntryPoint = {
        propertyStore(method, ClientCallable.Key) match {
            case EP(_, NotClientCallable)                             ⇒ NoEntryPoint
            case _ /* <=> EP(_,methods.IsClientCallable) | EPK(_,_)*/ ⇒ IsEntryPoint
        }
    }

    def determineEntrypoints(method: Method): PropertyComputationResult = {
        val classFile = method.classFile

        if (project.libraryClassFilesAreInterfacesOnly && project.isLibraryType(classFile))
            // if the analyze the library with the the public interface of third party libraries
            // we don't want the public API only as entry points.
            return Result(method, NoEntryPoint);

        if (classFile.isInterfaceDeclaration) {
            if (isOpenLibrary || classFile.isPublic)
                return Result(method, IsEntryPoint);
            else {
                val epProperty = isClientCallable(method)
                return Result(method, epProperty);
            }
        }

        /* Code from CallGraphFactory.defaultEntryPointsForLibraries */
        if (CallGraphFactory.isPotentiallySerializationRelated(method)(project.classHierarchy)) {
            return Result(method, IsEntryPoint);
        }

        if (method.isPrivate || (isClosedLibrary && method.isPackagePrivate))
            return Result(method, NoEntryPoint);

        // Now: the method is neither an (static or default) interface method nor a method
        // which relates somehow to object serialization.

        // ALL REQUIRED PROPERTIES ARE AVAILABLE (IF POSSIBLE)
        propertyStore(classFile, InstantiabilityKey) match {
            case EP(e, Instantiable) if method.isStaticInitializer ⇒ Result(method, IsEntryPoint)
            case EP(_, p) ⇒
                val isInstantiable = p == Instantiable
                val isAbstractOrInterface = classFile.isAbstract || classFile.isInterfaceDeclaration
                propertyStore(method, AccessKey) match {
                    case EP(_, ClassLocal) ⇒ Result(method, NoEntryPoint)
                    case EP(_, Global) if isInstantiable || method.isStatic ⇒
                        Result(method, IsEntryPoint)
                    case EP(_, PackageLocal) ⇒ Result(method, isClientCallable(method))
                    case EP(_, Global) if !isInstantiable && isAbstractOrInterface ⇒
                        Result(method, isClientCallable(method))
                    case _ ⇒ Result(method, NoEntryPoint)
                }
            case _ ⇒ throw new UnknownError("the other analyses have to be executed first")
        }
    }
}
