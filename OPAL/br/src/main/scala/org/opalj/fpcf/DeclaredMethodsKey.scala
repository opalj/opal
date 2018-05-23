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
package org.opalj.fpcf

import java.util.concurrent.ConcurrentHashMap

import org.opalj.br.DeclaredMethod
import org.opalj.br.DefinedMethod
import org.opalj.br.Method
import org.opalj.br.MethodDescriptor
import org.opalj.br.ObjectType
import org.opalj.br.VirtualDeclaredMethod
import org.opalj.br.ReferenceType
import org.opalj.br.analyses.ProjectInformationKey
import org.opalj.br.analyses.SomeProject
import org.opalj.collection.immutable.UIDSet

/**
 * The set of all [[org.opalj.br.DeclaredMethod]]s (potentially used by the property store).
 *
 * @author Dominik Helm
 */
class DeclaredMethods(
        val p:    SomeProject,
        var data: ConcurrentHashMap[ReferenceType, ConcurrentHashMap[DeclaredMethod, DeclaredMethod]]
) {

    def apply(
        packageName: String,
        classType:   ObjectType,
        name:        String,
        descriptor:  MethodDescriptor
    ): DeclaredMethod = {
        val newSet: ConcurrentHashMap[DeclaredMethod, DeclaredMethod] = new ConcurrentHashMap
        val prev = data.putIfAbsent(classType, newSet)

        val dmSet = if (prev == null) newSet else data.get(classType)

        val dms = dmSet.keys()

        // Find methods with matching name
        var matchingMethods: List[DeclaredMethod] = Nil
        while (dms.hasMoreElements) {
            val dm = dms.nextElement()
            if (dm.name == name) matchingMethods ::= dm
        }

        if (matchingMethods.size == 1 &&
            ((classType eq ObjectType.MethodHandle) || (classType eq ObjectType.VarHandle)) &&
            matchingMethods.head.hasDefinition &&
            matchingMethods.head.methodDefinition.isNativeAndVarargs &&
            descriptor == MethodDescriptor.SignaturePolymorphicMethod) {
            // Signature polymorphic method
        } else {
            // Find methods with matching package
            matchingMethods = matchingMethods.filter(_.descriptor == descriptor)

            // There may be more than one package private method with the same name and descriptor
            if (matchingMethods.size > 1) {
                matchingMethods = matchingMethods.filter { dm ⇒
                    dm.hasDefinition &&
                        dm.methodDefinition.classFile.thisType.packageName == packageName
                }
            }
        }

        assert(matchingMethods.size < 2)

        if (matchingMethods.isEmpty) {
            // No matching declared method found, construct a virtual declared method
            val vdm = VirtualDeclaredMethod(classType, name, descriptor)

            // A concurrent execution of this method may have put the virtual declared method
            // into the set already already
            val prev = dmSet.putIfAbsent(vdm, vdm)
            if (prev == null) vdm else prev
        } else {
            matchingMethods.head
        }
    }

    def apply(method: Method): DefinedMethod = {
        val classType = method.classFile.thisType
        data.get(classType).get(DefinedMethod(classType, method)).asInstanceOf[DefinedMethod]
    }

    def declaredMethods: Iterator[DeclaredMethod] = {
        import scala.collection.JavaConverters.collectionAsScalaIterable
        import scala.collection.JavaConverters.enumerationAsScalaIterator
        collectionAsScalaIterable(data.values()).iterator.flatMap { dmSet ⇒
            enumerationAsScalaIterator(dmSet.keys())
        }
    }
}

/**
 * The ''key'' object to get information about all declared methods.
 *
 * @note See [[org.opalj.br.DeclaredMethod]] for further details.
 * @example To get the index use the [[org.opalj.br.analyses.Project]]'s `get` method and pass in
 *          `this` object.
 *
 * @author Dominik Helm
 * @author Florian Kuebler
 */
object DeclaredMethodsKey extends ProjectInformationKey[DeclaredMethods, Nothing] {

    /**
     * The analysis has no special prerequisites.
     *
     * @return `Nil`.
     */
    override protected def requirements: Seq[ProjectInformationKey[Nothing, Nothing]] = Nil

    /**
     * Collects all declared methods.
     */
    override protected def compute(p: SomeProject): DeclaredMethods = {
        // Note that this analysis is not parallelized; the synchronization etc. overhead
        // outweighs the benefits even on systems with 4 or so cores.
        // We use a concurrent hash map, as later on analyses may add VirtualDeclaredMethods
        // to this map concurrently.
        // The inner ConcurrentHashMap is used for the set of declared methods for a type.
        val result: ConcurrentHashMap[ReferenceType, ConcurrentHashMap[DeclaredMethod, DeclaredMethod]] =
            new ConcurrentHashMap

        def getOrInsertDMSet(t: ObjectType): ConcurrentHashMap[DeclaredMethod, DeclaredMethod] = {
            val newSet: ConcurrentHashMap[DeclaredMethod, DeclaredMethod] = new ConcurrentHashMap
            val prev = result.putIfAbsent(t, newSet)
            if (prev eq null) newSet else prev
        }

        // TODO test method
        p.allClassFiles.foreach { cf ⇒
            val classType = cf.thisType

            // The set to add the methods for this class to
            val dms = getOrInsertDMSet(classType)

            for {
                // all methods present in the current class file, excluding methods derived
                // from any supertype that are not overridden by this type.
                m ← cf.methods
                if m.isStatic || m.isPrivate || m.isAbstract || m.isInitializer
            } {
                if (m.isAbstract) {
                    // Abstract methods can be inherited, but will not appear as instance methods
                    // for subtypes, so we have to add them manually for all subtypes that don't
                    // override/implement them here
                    p.classHierarchy.processSubtypes(classType)(null) {
                        (t: Null, subtype: ObjectType) ⇒
                            val subClassFile = p.classFile(subtype).get
                            if (subClassFile.findMethod(m.name, m.descriptor).isEmpty) {
                                val method = if (subClassFile.isInterfaceDeclaration)
                                    p.resolveInterfaceMethodReference(subtype, m.name, m.descriptor).get
                                else
                                    p.resolveMethodReference(subtype, m.name, m.descriptor) getOrElse {
                                        p.findMaximallySpecificSuperinterfaceMethods(p
                                            .classHierarchy.allSuperinterfacetypes(subtype), m.name,
                                            m.descriptor, UIDSet.empty[ObjectType])._2.headOption.get
                                    }
                                val subtypeDms = getOrInsertDMSet(subtype)
                                val vm = DefinedMethod(subtype, method)
                                subtypeDms.put(vm, vm)
                                (null, false, false) // Continue traversal on non-overridden method
                            } else {
                                (null, true, false) // Stop traversal on overridden method
                            }
                    }
                }
                val vm = DefinedMethod(classType, m)
                dms.put(vm, vm)
            }
            for {
                // all non-private, non-abstract instance methods present in the current class file,
                // including methods derived from any supertype that are not overridden by this type
                mc ← p.instanceMethods(classType)
            } {
                val vm = DefinedMethod(classType, mc.method)
                dms.put(vm, vm)
            }
        }

        new DeclaredMethods(p, result)
    }

}
