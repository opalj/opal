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
package br
package analyses

import java.util.concurrent.ConcurrentHashMap
import java.util.function.{Function ⇒ JFunction}

import org.opalj.br.MethodDescriptor.SignaturePolymorphicMethod
import org.opalj.br.ObjectType.MethodHandle
import org.opalj.br.ObjectType.VarHandle
import org.opalj.collection.immutable.UIDSet

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

    // The following lists were created using the Java 10 specification
    val methodHandleSignaturePolymorphicMethods = List(
        VirtualDeclaredMethod(MethodHandle, "invoke", SignaturePolymorphicMethod),
        VirtualDeclaredMethod(MethodHandle, "invokeExact", SignaturePolymorphicMethod)
    )

    val varHandleSignaturePolymorphicMethods = List(
        VirtualDeclaredMethod(VarHandle, "get", SignaturePolymorphicMethod),
        VirtualDeclaredMethod(VarHandle, "set", SignaturePolymorphicMethod),
        VirtualDeclaredMethod(VarHandle, "getVolatile", SignaturePolymorphicMethod),
        VirtualDeclaredMethod(VarHandle, "setVolatile", SignaturePolymorphicMethod),
        VirtualDeclaredMethod(VarHandle, "getOpaque", SignaturePolymorphicMethod),
        VirtualDeclaredMethod(VarHandle, "setOpaque", SignaturePolymorphicMethod),
        VirtualDeclaredMethod(VarHandle, "getAcquire", SignaturePolymorphicMethod),
        VirtualDeclaredMethod(VarHandle, "setRelease", SignaturePolymorphicMethod),
        VirtualDeclaredMethod(VarHandle, "compareAndSet", SignaturePolymorphicMethod),
        VirtualDeclaredMethod(VarHandle, "compareAndExchange", SignaturePolymorphicMethod),
        VirtualDeclaredMethod(VarHandle, "compareAndExchangeAcquire", SignaturePolymorphicMethod),
        VirtualDeclaredMethod(VarHandle, "compareAndExchangeRelease", SignaturePolymorphicMethod),
        VirtualDeclaredMethod(VarHandle, "weakCompareAndSetPlain", SignaturePolymorphicMethod),
        VirtualDeclaredMethod(VarHandle, "weakCompareAndSet", SignaturePolymorphicMethod),
        VirtualDeclaredMethod(VarHandle, "weakCompareAndSetAcquire", SignaturePolymorphicMethod),
        VirtualDeclaredMethod(VarHandle, "weakCompareAndSetRelease", SignaturePolymorphicMethod),
        VirtualDeclaredMethod(VarHandle, "getAndSet", SignaturePolymorphicMethod),
        VirtualDeclaredMethod(VarHandle, "getAndSetAcquire", SignaturePolymorphicMethod),
        VirtualDeclaredMethod(VarHandle, "getAndSetRelease", SignaturePolymorphicMethod),
        VirtualDeclaredMethod(VarHandle, "getAndAdd", SignaturePolymorphicMethod),
        VirtualDeclaredMethod(VarHandle, "getAndAddAcquire", SignaturePolymorphicMethod),
        VirtualDeclaredMethod(VarHandle, "getAndAddRelease", SignaturePolymorphicMethod),
        VirtualDeclaredMethod(VarHandle, "getAndBitwiseOr", SignaturePolymorphicMethod),
        VirtualDeclaredMethod(VarHandle, "getAndBitwiseOrAcquire", SignaturePolymorphicMethod),
        VirtualDeclaredMethod(VarHandle, "getAndBitwiseOrRelease", SignaturePolymorphicMethod),
        VirtualDeclaredMethod(VarHandle, "getAndBitwiseAnd", SignaturePolymorphicMethod),
        VirtualDeclaredMethod(VarHandle, "getAndBitwiseAndAcquire", SignaturePolymorphicMethod),
        VirtualDeclaredMethod(VarHandle, "getAndBitwiseAndRelease", SignaturePolymorphicMethod),
        VirtualDeclaredMethod(VarHandle, "getAndBitwiseXor", SignaturePolymorphicMethod),
        VirtualDeclaredMethod(VarHandle, "getAndBitwiseXorAcquire", SignaturePolymorphicMethod),
        VirtualDeclaredMethod(VarHandle, "getAndBitwiseXorRelease", SignaturePolymorphicMethod)
    )

    /**
     * The analysis has no special prerequisites.
     *
     * @return `Nil`.
     */
    override protected def requirements: Seq[ProjectInformationKey[Nothing, Nothing]] = Nil

    // TODO [Java9+] Needs to be updated for Java9+ projects which use Modules.
    /**
     * Collects all declared methods.
     */
    override protected def compute(p: SomeProject): DeclaredMethods = {
        val result: ConcurrentHashMap[ReferenceType, ConcurrentHashMap[MethodContext, DeclaredMethod]] =
            new ConcurrentHashMap

        val mapFactory: JFunction[ReferenceType, ConcurrentHashMap[MethodContext, DeclaredMethod]] =
            (_: ReferenceType) ⇒ { new ConcurrentHashMap() }

        p.parForeachClassFile() { cf ⇒
            val classType = cf.thisType

            // The set to add the methods for this class to
            val dms = result.computeIfAbsent(classType, mapFactory)

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
                        (_: Null, subtype: ObjectType) ⇒
                            val subClassFile = p.classFile(subtype).get
                            if (subClassFile.findMethod(m.name, m.descriptor).isEmpty) {
                                val methodO = if (subClassFile.isInterfaceDeclaration)
                                    p.resolveInterfaceMethodReference(subtype, m.name, m.descriptor)
                                else
                                    // TODO Reconsider this code once issue #151 is resolved
                                    p.resolveMethodReference(subtype, m.name, m.descriptor) orElse {
                                        p.findMaximallySpecificSuperinterfaceMethods(
                                            p.classHierarchy.allSuperinterfacetypes(subtype),
                                            m.name,
                                            m.descriptor,
                                            UIDSet.empty[ObjectType]
                                        )._2.headOption
                                    }
                                val subtypeDms = result.computeIfAbsent(subtype, mapFactory)
                                val vm = DefinedMethod(subtype, methodO.get)
                                val oldVM = subtypeDms.put(MethodContext(methodO.get), vm)
                                if (oldVM != null && oldVM != vm) {
                                    //throw new UnknownError("creation of declared methods failed")
                                }
                                (null, false, false) // Continue traversal on non-overridden method
                            } else {
                                (null, true, false) // Stop traversal on overridden method
                            }
                    }
                }
                val vm = DefinedMethod(classType, m)
                val oldVM = dms.put(MethodContext(m), vm)
                if (oldVM != null && oldVM != vm) {
                    //throw new UnknownError("creation of declared methods failed")
                }
            }
            for {
                // all non-private, non-abstract instance methods present in the current class file,
                // including methods derived from any supertype that are not overridden by this type
                mc ← p.instanceMethods(classType)
            } {
                val vm = DefinedMethod(classType, mc.method)
                val oldVM = dms.put(MethodContext(mc.method), vm)
                if (oldVM != null && oldVM != vm) {
                    if (oldVM.methodDefinition.classFile.thisType.simpleName != "JComponent" &&
                        vm.methodDefinition.classFile.thisType.simpleName != "JComponent" &&
                        !(oldVM.methodDefinition.isPackagePrivate && !vm.methodDefinition.isPackagePrivate
                            || vm.methodDefinition.isPackagePrivate && !oldVM.methodDefinition.isPackagePrivate) &&
                        !(!vm.methodDefinition.isAbstract && oldVM.methodDefinition.isAbstract)) {
                        //throw new UnknownError(s"creation of declared methods failed:\n\t$oldVM\n\t\tvs.(new)\n\t$vm}")
                    }
                }
            }
        }

        // Special handling for signature-polymorphic methods
        if (p.classFile(MethodHandle).isEmpty) {
            val dms = result.computeIfAbsent(MethodHandle, _ ⇒ new ConcurrentHashMap)
            for (vm ← methodHandleSignaturePolymorphicMethods) {
                if (dms.put(new MethodContext(vm.name, vm.descriptor), vm) != null) {
                    throw new UnknownError("creation of declared methods failed")
                }
            }
        }
        if (p.classFile(VarHandle).isEmpty) {
            val dms = result.computeIfAbsent(VarHandle, _ ⇒ new ConcurrentHashMap)
            for (vm ← varHandleSignaturePolymorphicMethods) {
                if (dms.put(new MethodContext(vm.name, vm.descriptor), vm) != null) {
                    throw new UnknownError("creation of declared methods failed")
                }
            }
        }

        new DeclaredMethods(p, result)
    }

    /**
     * Represents a non-package-private method by its signature. The hashCode method is the same as
     * the one in [[PackagePrivateMethodContext]], so this is found when a HashMap is queried with
     * a `PackagePrivateMethodContext`, in which case the equals method guarantees that it matches
     * any `PackagePrivateMethodContext` with the same signature.
     */
    private[analyses] sealed class MethodContext(
            val methodName: String,
            val descriptor: MethodDescriptor
    ) {

        override def equals(other: Any): Boolean = other match {
            case that: MethodContext ⇒
                methodName == that.methodName && descriptor == that.descriptor
            case _ ⇒ false
        }

        override def hashCode(): Int = {
            methodName.hashCode * 31 + descriptor.hashCode()
        }
    }

    private[analyses] object MethodContext {
        /**
         * Factory method for [[MethodContext]]/[[PackagePrivateMethodContext]] depending on
         * whether the given method is package-private or not.
         */
        def apply(method: Method): MethodContext = {
            if (method.isPackagePrivate)
                new PackagePrivateMethodContext(
                    method.classFile.thisType.packageName,
                    method.name,
                    method.descriptor
                )
            else
                new MethodContext(method.name, method.descriptor)
        }
    }

    /**
     * Represents a (potentially) package-private method by its declaring package and signature.
     * The hashCode method is the same as the one in [[MethodContext]] (i.e. it does not include
     * the package name), so it can be used to query a HashMap for a `MethodContext`, in which
     * case the equals method guarantees that it matches a `MethodContext` with the same signature
     * regardless of the package name. It only matches another [[PackagePrivateMethodContext]] if
     * the package names are the same, though.
     */
    private[analyses] class PackagePrivateMethodContext(
            val packageName: String,
            methodName:      String,
            descriptor:      MethodDescriptor
    ) extends MethodContext(methodName, descriptor) {

        override def equals(other: Any): Boolean = other match {
            case that: PackagePrivateMethodContext ⇒
                packageName == that.packageName &&
                    methodName == that.methodName &&
                    descriptor == that.descriptor
            case that: MethodContext ⇒
                methodName == that.methodName && descriptor == that.descriptor
            case _ ⇒ false
        }

        override def hashCode(): Int = methodName.hashCode * 31 + descriptor.hashCode()
    }

}
