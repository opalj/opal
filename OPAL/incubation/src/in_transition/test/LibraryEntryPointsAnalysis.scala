/* BSD 2-Clause License - see OPAL/LICENSE for details. */
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
