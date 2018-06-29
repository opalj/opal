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

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import org.opalj.br.MethodDescriptor.SignaturePolymorphicMethod
import org.opalj.br.ObjectType.MethodHandle
import org.opalj.br.ObjectType.VarHandle
import org.opalj.collection.immutable.UIDSet
import org.opalj.collection.immutable.ConstArray

import scala.collection.mutable.ArrayBuffer

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
        val objectClassFile = p.classFile(ObjectType.Object)
        def findInterfaceMethod(
            cf:         ClassFile,
            name:       String,
            descriptor: MethodDescriptor
        ): Option[(MethodContext, DeclaredMethod)] = {
            def lookupInObject(): Option[Method] = {
                objectClassFile flatMap { classFile ⇒
                    classFile.findMethod(name, descriptor) filter { m ⇒ m.isPublic && !m.isStatic }
                }
            }

            val classType = cf.thisType

            if (cf.isInterfaceDeclaration) {
                val methodO = cf.findMethod(name, descriptor) orElse lookupInObject()
                if (methodO.isDefined) {
                    val vm = DefinedMethod(classType, methodO.get)
                    val context = MethodContext(p, classType, methodO.get)
                    Some((context, vm))
                } else {
                    val superinterfaceTypes =
                        p.classHierarchy.superinterfaceTypes(classType).get
                    val (_, methods) =
                        p.findMaximallySpecificSuperinterfaceMethods(
                            superinterfaceTypes,
                            name,
                            descriptor,
                            UIDSet.empty[ObjectType]
                        )
                    if (methods.size > 1 && !methods.head.isAbstract) {
                        val vm = MultiplyDefinedMethod(
                            classType,
                            ConstArray(methods.toSeq: _*)
                        )
                        val context = new MethodContext(name, descriptor)
                        Some((context, vm))
                    } else {
                        methods.headOption map { method ⇒
                            val vm = DefinedMethod(classType, method)
                            val context = MethodContext(p, classType, method)
                            (context, vm)
                        }
                    }
                }
            } else
                p.resolveMethodReference(
                    classType,
                    name,
                    descriptor,
                    forceLookupInSuperinterfacesOnFailure = true
                ) map { method ⇒
                    val vm = DefinedMethod(classType, method)
                    val context = MethodContext(p, classType, method)
                    (context, vm)
                }
        }

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
                            val subtypeDms = result.computeIfAbsent(subtype, mapFactory)
                            if (subClassFile.findMethod(m.name, m.descriptor).isEmpty) {
                                val cAndDm = findInterfaceMethod(subClassFile, m.name, m.descriptor)
                                cAndDm foreach {
                                    case (context, dm) ⇒
                                        val oldDm = subtypeDms.put(context, dm)
                                        if (oldDm != null && oldDm != dm) {
                                            throw new UnknownError(
                                                "creation of declared methods failed:\n\t"+
                                                    s"$oldDm\n\t\tvs.(new)\n\t$dm}"
                                            )
                                        }
                                }
                                (null, false, false) // Continue traversal on non-overridden method
                            } else {
                                (null, true, false) // Stop traversal on overridden method
                            }
                    }
                }
                val dm = DefinedMethod(classType, m)
                val oldDm = dms.put(MethodContext(p, classType, m), dm)
                if (oldDm != null && oldDm != dm) {
                    throw new UnknownError(
                        "creation of declared methods failed:\n\t"+
                            s"$oldDm\n\t\tvs.(new)\n\t$dm}"
                    )
                }
            }

            for {
                // all non-private, non-abstract instance methods present in the current class file,
                // including methods derived from any supertype that are not overridden by this type
                mc ← p.instanceMethods(classType)
            } {
                val dm = DefinedMethod(classType, mc.method)
                val oldDm = dms.put(MethodContext(p, classType, mc.method), dm)
                if (oldDm != null && oldDm != dm) {
                    throw new UnknownError(
                        "creation of declared methods failed:\n\t"+
                            s"$oldDm\n\t\tvs.(new)\n\t$dm}"
                    )
                }
            }
        }

        // Special handling for signature-polymorphic methods
        if (p.classFile(MethodHandle).isEmpty) {
            val dms = result.computeIfAbsent(MethodHandle, _ ⇒ new ConcurrentHashMap)
            for (dm ← methodHandleSignaturePolymorphicMethods) {
                if (dms.put(new MethodContext(dm.name, dm.descriptor), dm) != null) {
                    throw new UnknownError("creation of declared methods failed")
                }
            }
        }
        if (p.classFile(VarHandle).isEmpty) {
            val dms = result.computeIfAbsent(VarHandle, _ ⇒ new ConcurrentHashMap)
            for (dm ← varHandleSignaturePolymorphicMethods) {
                if (dms.put(new MethodContext(dm.name, dm.descriptor), dm) != null) {
                    throw new UnknownError("creation of declared methods failed")
                }
            }
        }

        // assign each declared method a unique id
        val method2Id = new Object2IntOpenHashMap[DeclaredMethod]()
        val id2method = new ArrayBuffer[DeclaredMethod](result.size() * 30)

        import scala.collection.JavaConverters._
        var id = 0
        for {
            context2Method ← result.elements().asScala
            dm ← context2Method.elements().asScala
        } {
            method2Id.put(dm, id)
            id2method.append(dm)
            id += 1
        }

        new DeclaredMethods(p, result, id2method, method2Id)
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
        def apply(project: SomeProject, objectType: ObjectType, method: Method): MethodContext = {
            if (method.isPackagePrivate)
                new PackagePrivateMethodContext(
                    method.classFile.thisType.packageName,
                    method.name,
                    method.descriptor
                )
            else if (project.instanceMethods(objectType).exists { m ⇒
                method.name == m.name &&
                    method.descriptor == m.descriptor &&
                    m.method.isPackagePrivate
            })
                new ShadowsPackagePrivateMethodContext(method.name, method.descriptor)
            else
                new MethodContext(method.name, method.descriptor)
        }
    }

    /**
     * Represents a package-private method by its declaring package and signature.
     * The hashCode method is that from [[MethodContext]] (i.e. it does not include the package
     * name), so it can be used to query a HashMap for a `MethodContext`, in which case the
     * equals method guarantees that it matches a `MethodContext` with the same signature regardless
     * of the package name. It only matches another [[PackagePrivateMethodContext]] if the package
     * names are the same, though.
     */
    private[this] class PackagePrivateMethodContext(
            val packageName: String,
            methodName:      String,
            descriptor:      MethodDescriptor
    ) extends MethodContext(methodName, descriptor) {

        override def equals(other: Any): Boolean = other match {
            case that: MethodContextQuery ⇒ that.equals(this)
            case that: PackagePrivateMethodContext ⇒
                packageName == that.packageName &&
                    methodName == that.methodName &&
                    descriptor == that.descriptor
            case _: ShadowsPackagePrivateMethodContext ⇒ false
            case that: MethodContext ⇒
                methodName == that.methodName && descriptor == that.descriptor
            case _ ⇒ false
        }
    }

    /**
     * Represents a protected or public method that shadows one or more package-private methods
     * from supertypes in other packages.
     * The hashCode method is that from [[MethodContext]], so it can be used to query a HashMap for
     * a `MethodContext`, in which case the equals method guarantees that it matches a
     * `MethodContext` with the same signature regardless of the package name.
     */
    private[this] class ShadowsPackagePrivateMethodContext(
            methodName: String,
            descriptor: MethodDescriptor
    ) extends MethodContext(methodName, descriptor) {

        override def equals(other: Any): Boolean = other match {
            case that: MethodContextQuery       ⇒ that.equals(this)
            case _: PackagePrivateMethodContext ⇒ false
            case that: MethodContext ⇒
                methodName == that.methodName && descriptor == that.descriptor
            case _ ⇒ false
        }
    }

    /**
     * Represents a virtual call site by the declared receiver type, the package of the caller and
     * the method signature.
     * Used to query a HashMap for different kinds of [[MethodContext]]s, which all share the same
     * hashCode, but differ in their equality with this type.
     */
    private[analyses] class MethodContextQuery(
            project:          SomeProject,
            val receiverType: ObjectType,
            val packageName:  String,
            methodName:       String,
            descriptor:       MethodDescriptor
    ) extends MethodContext(methodName, descriptor) {

        override def equals(other: Any): Boolean = other match {
            case that: PackagePrivateMethodContext ⇒
                packageName == that.packageName &&
                    methodName == that.methodName &&
                    descriptor == that.descriptor &&
                    !project.instanceMethods(receiverType).exists { m ⇒
                        methodName == m.name &&
                            descriptor == m.descriptor &&
                            (m.isPublic || m.method.isProtected)
                    }
            case that: ShadowsPackagePrivateMethodContext ⇒
                methodName == that.methodName &&
                    descriptor == that.descriptor &&
                    project.instanceMethods(receiverType).exists { m ⇒
                        methodName == m.name &&
                            descriptor == m.descriptor &&
                            (m.isPublic || m.method.isProtected)
                    }
            case that: MethodContext ⇒
                methodName == that.methodName && descriptor == that.descriptor
            case _ ⇒ false
        }
    }
}
