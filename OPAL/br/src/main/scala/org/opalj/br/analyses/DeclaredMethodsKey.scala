/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.{Function => JFunction}
import org.opalj.br.ObjectType.MethodHandle
import org.opalj.br.ObjectType.VarHandle
import org.opalj.br.MethodDescriptor.SignaturePolymorphicMethodBoolean
import org.opalj.br.MethodDescriptor.SignaturePolymorphicMethodObject
import org.opalj.br.MethodDescriptor.SignaturePolymorphicMethodVoid

import scala.collection.immutable.ArraySeq
import scala.jdk.CollectionConverters._

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
    private val methodHandleSignaturePolymorphicMethods = List(
        "invoke",
        "invokeExact"
    )

    private val varHandleSignaturePolymorphicMethods = List(
        "get",
        "set",
        "getVolatile",
        "setVolatile",
        "getOpaque",
        "setOpaque",
        "getAcquire",
        "setRelease",
        "compareAndSet",
        "compareAndExchange",
        "compareAndExchangeAcquire",
        "compareAndExchangeRelease",
        "weakCompareAndSetPlain",
        "weakCompareAndSet",
        "weakCompareAndSetAcquire",
        "weakCompareAndSetRelease",
        "getAndSet",
        "getAndSetAcquire",
        "getAndSetRelease",
        "getAndAdd",
        "getAndAddAcquire",
        "getAndAddRelease",
        "getAndBitwiseOr",
        "getAndBitwiseOrAcquire",
        "getAndBitwiseOrRelease",
        "getAndBitwiseAnd",
        "getAndBitwiseAndAcquire",
        "getAndBitwiseAndRelease",
        "getAndBitwiseXor",
        "getAndBitwiseXorAcquire",
        "getAndBitwiseXorRelease"
    )

    /**
     * The analysis has no special prerequisites.
     *
     * @return `Nil`.
     */
    override def requirements(project: SomeProject): Seq[ProjectInformationKey[Nothing, Nothing]] = Nil

    // TODO [Java9+] Needs to be updated for Java9+ projects which use Modules.
    /**
     * Collects all declared methods.
     */
    override def compute(p: SomeProject): DeclaredMethods = {

        val result: ConcurrentHashMap[ReferenceType, ConcurrentHashMap[MethodContext, DeclaredMethod]] =
            new ConcurrentHashMap

        val mapFactory: JFunction[ReferenceType, ConcurrentHashMap[MethodContext, DeclaredMethod]] =
            (_: ReferenceType) => { new ConcurrentHashMap() }

        val idCounter = new AtomicInteger()

        def insertDeclaredMethod(
            dms:                   ConcurrentHashMap[MethodContext, DeclaredMethod],
            context:               MethodContext,
            computeDeclaredMethod: Int => DeclaredMethod
        ): Unit = {
            var computedDM: DeclaredMethod = null
            val oldDm = dms.computeIfAbsent(context, _ => {
                computedDM = computeDeclaredMethod(idCounter.getAndIncrement())
                computedDM
            })

            assert(
                (computedDM ne null) || {
                    computedDM = computeDeclaredMethod(0)
                    oldDm match {
                        case dm: DefinedMethod =>
                            computedDM.hasSingleDefinedMethod &&
                                (dm.definedMethod eq computedDM.definedMethod)
                        case mdm: MultipleDefinedMethods =>
                            mdm.hasMultipleDefinedMethods &&
                                mdm.definedMethods.size == computedDM.definedMethods.size &&
                                mdm.definedMethods.forall(computedDM.definedMethods.contains)
                        case _: VirtualDeclaredMethod => true
                    }
                },
                "creation of declared methods failed:\n\t"+
                    s"$oldDm\n\t\tvs.(new)\n\t$computedDM}"
            )
        }

        p.parForeachClassFile() { cf =>
            val classType = cf.thisType

            // The set to add the methods for this class to
            val dms = result.computeIfAbsent(classType, mapFactory)

            for {
                // all methods present in the current class file, excluding methods derived
                // from any supertype that are not overridden by this type.
                m <- cf.methods
                if m.isStatic || m.isAbstract || m.isInitializer
            } {
                if (m.isAbstract) {
                    // Abstract methods can be inherited, but will not appear as instance methods
                    // for subtypes, so we have to add them manually for all subtypes that don't
                    // override/implement them here
                    p.classHierarchy.processSubtypes(classType)(null) {
                        (_: Null, subtype: ObjectType) =>
                            val subClassFile = p.classFile(subtype).get
                            val subtypeDms = result.computeIfAbsent(subtype, mapFactory)
                            if (subClassFile.findMethod(m.name, m.descriptor).isEmpty) {
                                val interfaceMethods =
                                    p.resolveAllMethodReferences(subtype, m.name, m.descriptor)
                                interfaceMethods.size match {
                                    case 0 =>
                                    case 1 =>
                                        val interfaceMethod = interfaceMethods.head
                                        insertDeclaredMethod(
                                            subtypeDms,
                                            MethodContext(p, subtype, interfaceMethod),
                                            id => new DefinedMethod(subtype, interfaceMethod, id)
                                        )
                                    case _ =>
                                        val methods = ArraySeq.from(interfaceMethods)
                                        insertDeclaredMethod(
                                            subtypeDms,
                                            new MethodContext(m.name, m.descriptor),
                                            id => new MultipleDefinedMethods(
                                                subtype,
                                                methods,
                                                id
                                            )
                                        )
                                }

                                (null, false, false) // Continue traversal on non-overridden method
                            } else {
                                (null, true, false) // Stop traversal on overridden method
                            }
                    }
                } else if (m.isStatic && !m.isPrivate &&
                    !m.isStaticInitializer && !cf.isInterfaceDeclaration) {
                    // Static methods are inherited as well - they can be invoked on subtypes
                    // this is not true for static initializers and static methods on interfaces
                    p.classHierarchy.processSubtypes(classType)(initial = null) {
                        (_: Null, subtype: ObjectType) =>
                            val subClassFile = p.classFile(subtype).get
                            val subtypeDms = result.computeIfAbsent(subtype, mapFactory)
                            if (subClassFile.findMethod(m.name, m.descriptor).isEmpty) {
                                val staticMethodResult = p.staticCall(
                                    subtype,
                                    subtype,
                                    isInterface = false,
                                    m.name,
                                    m.descriptor
                                )
                                if (staticMethodResult.hasValue) {
                                    val staticMethod = staticMethodResult.value
                                    insertDeclaredMethod(
                                        subtypeDms,
                                        MethodContext(p, subtype, staticMethod),
                                        id => new DefinedMethod(subtype, staticMethod, id)
                                    )
                                    // Continue traversal on non-overridden method
                                    (null, false, false)
                                } else {
                                    // This can only happen with a broken class hierarchy such that
                                    // `subtype` implements `classType` but `classType` isn't an
                                    // interface

                                    // Stop traversal
                                    (null, true, false)
                                }
                            } else {
                                (null, true, false) // Stop traversal on overridden method
                            }
                    }
                }
                val context = MethodContext(p, classType, m)
                insertDeclaredMethod(dms, context, id => new DefinedMethod(classType, m, id))
            }

            for {
                // all non-abstract instance methods present in the current class file,
                // including methods derived from any supertype that are not overridden by this type
                mc <- p.instanceMethods(classType)
            } {
                val context = MethodContext(p, classType, mc.method)
                insertDeclaredMethod(dms, context, id => new DefinedMethod(classType, mc.method, id))
            }
        }

        // Special handling for signature-polymorphic methods
        if (p.MethodHandleClassFile.isEmpty) {
            val dms = result.computeIfAbsent(MethodHandle, _ => new ConcurrentHashMap)
            for (name <- methodHandleSignaturePolymorphicMethods) {
                val context = new MethodContext(name, SignaturePolymorphicMethodObject)
                insertDeclaredMethod(dms, context, id => new VirtualDeclaredMethod(
                    MethodHandle, name, SignaturePolymorphicMethodObject, id
                ))
            }
        }
        if (p.VarHandleClassFile.isEmpty) {
            val dms = result.computeIfAbsent(VarHandle, _ => new ConcurrentHashMap)
            for (name <- varHandleSignaturePolymorphicMethods) {
                val descriptor = if (name == "compareAndSet" || name.startsWith("weak"))
                    SignaturePolymorphicMethodBoolean
                else if (name.startsWith("set"))
                    SignaturePolymorphicMethodVoid
                else if (name.startsWith("get") || name.startsWith("compare"))
                    SignaturePolymorphicMethodObject
                else
                    throw new IllegalArgumentException(
                        s"Unexpected signature polymorphic method $name"
                    )
                val context = new MethodContext(name, descriptor)
                insertDeclaredMethod(dms, context, id => new VirtualDeclaredMethod(
                    VarHandle, name, descriptor, id
                ))
            }
        }

        val id2method = new Array[DeclaredMethod](idCounter.get() + 1000)

        for {
            context2Method <- result.elements().asScala
            dm <- context2Method.elements().asScala
        } {
            id2method(dm.id) = dm
        }

        new DeclaredMethods(p, result, id2method, idCounter.get)
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
            case that: MethodContext =>
                methodName == that.methodName && descriptor == that.descriptor
            case _ => false
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
            MethodContext(
                project,
                objectType,
                method.classFile.thisType.packageName,
                method.name,
                method.descriptor,
                method.isPackagePrivate
            )
        }

        /**
         * Factory method for [[MethodContext]]/[[PackagePrivateMethodContext]] depending on
         * whether the given method is package-private or not.
         */
        def apply(
            project:          SomeProject,
            objectType:       ObjectType,
            declaringPackage: String,
            methodName:       String,
            descriptor:       MethodDescriptor,
            isPackagePrivate: Boolean
        ): MethodContext = {
            if (isPackagePrivate)
                new PackagePrivateMethodContext(declaringPackage, methodName, descriptor)
            else if (project.classFile(objectType).isDefined &&
                project.hasInstanceMethod(objectType, methodName, descriptor, true))
                new ShadowsPackagePrivateMethodContext(methodName, descriptor)
            else
                new MethodContext(methodName, descriptor)
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
            case that: MethodContextQuery => that.equals(this)
            case that: PackagePrivateMethodContext =>
                packageName == that.packageName &&
                    methodName == that.methodName &&
                    descriptor == that.descriptor
            case _: ShadowsPackagePrivateMethodContext => false
            case that: MethodContext =>
                methodName == that.methodName && descriptor == that.descriptor
            case _ => false
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
            case that: MethodContextQuery       => that.equals(this)
            case _: PackagePrivateMethodContext => false
            case that: MethodContext =>
                methodName == that.methodName && descriptor == that.descriptor
            case _ => false
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
            case that: PackagePrivateMethodContext =>
                packageName == that.packageName &&
                    methodName == that.methodName &&
                    descriptor == that.descriptor &&
                    isPackagePrivateMethod
            case that: ShadowsPackagePrivateMethodContext =>
                methodName == that.methodName &&
                    descriptor == that.descriptor &&
                    !isPackagePrivateMethod
            case that: MethodContext =>
                methodName == that.methodName && descriptor == that.descriptor
            case _ => false
        }

        private def isPackagePrivateMethod: Boolean = {
            if (project.classHierarchy.isInterface(receiverType).isYes) {
                false
            } else {
                val method = project.resolveClassMethodReference(
                    receiverType,
                    methodName,
                    descriptor
                )
                method.hasValue && method.value.isPackagePrivate &&
                    method.value.declaringClassFile.thisType.packageName == packageName
            }
        }
    }
}
