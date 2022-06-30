/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock

import org.opalj.log.LogContext
import org.opalj.log.OPALLogger.info
import org.opalj.br.MethodDescriptor.SignaturePolymorphicMethodBoolean
import org.opalj.br.MethodDescriptor.SignaturePolymorphicMethodObject
import org.opalj.br.MethodDescriptor.SignaturePolymorphicMethodVoid
import org.opalj.br.ObjectType.MethodHandle
import org.opalj.br.ObjectType.VarHandle
import org.opalj.br.analyses.DeclaredMethodsKey.MethodContext
import org.opalj.br.analyses.DeclaredMethodsKey.MethodContextQuery

/**
 * The set of all [[org.opalj.br.DeclaredMethod]]s (potentially used by the property store).
 *
 * @author Dominik Helm
 */
class DeclaredMethods(
        private[this] val p: SomeProject,
        // We need concurrent, mutable maps here, as VirtualDeclaredMethods may be added when they
        // are queried. This can result in DeclaredMethods added for a type not yet seen, too (e.g.
        // methods on type Object when not analyzing the JDK.
        private[this] val data:      ConcurrentHashMap[ReferenceType, ConcurrentHashMap[MethodContext, DeclaredMethod]],
        private[this] var id2method: Array[DeclaredMethod],
        private[this] var idCounter: Int
) {

    private[this] final val lock = new ReentrantReadWriteLock()

    private var extensionSize = 1000

    /**
     * A (possibly stale, i.e., too low) view of the number of declared methods
     */
    def _UNSAFE_size: Int = idCounter

    def apply(
        declaredType: ObjectType,
        packageName:  String,
        runtimeType:  ObjectType,
        name:         String,
        descriptor:   MethodDescriptor
    ): DeclaredMethod = {
        val dmSet = data.computeIfAbsent(runtimeType, _ => new ConcurrentHashMap)

        val context = new MethodContextQuery(p, declaredType, packageName, name, descriptor)
        var method = dmSet.get(context)
        if (method != null) return method;

        if (p.isSignaturePolymorphic(runtimeType, name, descriptor)) {
            val signaturePolymorphicMethodDescriptor =
                if (runtimeType eq VarHandle) {
                    if (name == "compareAndSet" || name.startsWith("weak"))
                        SignaturePolymorphicMethodBoolean
                    else if (name.startsWith("set"))
                        SignaturePolymorphicMethodVoid
                    else if (name.startsWith("get") || name.startsWith("compare"))
                        SignaturePolymorphicMethodObject
                    else
                        throw new IllegalArgumentException(
                            s"Unexpected signature polymorphic method $name"
                        )
                } else if ((runtimeType eq MethodHandle) &&
                    (name == "invoke" || name == "invokeExact")) {
                    SignaturePolymorphicMethodObject
                } else
                    throw new IllegalArgumentException(
                        s"Unexpected signature polymorphic method $name"
                    )

            method = dmSet.get(
                new MethodContextQuery(
                    p,
                    declaredType,
                    packageName,
                    name,
                    signaturePolymorphicMethodDescriptor
                )
            )
            if (method != null) return method;
        }

        // Package private methods can be invoked from other packages if they override a public
        // method. Thus, try a query with the receiver's package name.
        method = dmSet.get(
            new MethodContextQuery(p, runtimeType, runtimeType.packageName, name, descriptor)
        )
        if (method != null) return method;

        // In case of an unseen method, compute id
        lock.writeLock().lock()
        try {
            if (!dmSet.contains(context)) {
                val vm = new VirtualDeclaredMethod(runtimeType, name, descriptor, idCounter)
                idCounter += 1
                dmSet.put(MethodContext(p, runtimeType, "", name, descriptor, false), vm)
                if (id2method.size <= vm.id) {
                    implicit val logContext: LogContext = p.logContext
                    info(
                        "project",
                        "too many virtual declared methods; extended the underlying array"
                    )
                    val id2methodExt = new Array[DeclaredMethod](id2method.length + extensionSize)
                    extensionSize = Math.min(extensionSize * 2, 32000)
                    Array.copy(id2method, 0, id2methodExt, 0, id2method.length)
                    id2method = id2methodExt
                }
                id2method(vm.id) = vm
            }
        } finally {
            lock.writeLock().unlock()
        }

        method = dmSet.get(context)
        assert(method ne null)
        method
    }

    def apply(method: Method): DefinedMethod = {
        val classType = method.classFile.thisType
        data.get(classType).get(MethodContext(p, classType, method)).asInstanceOf[DefinedMethod]
    }

    def apply(methodId: Int): DeclaredMethod = {
        lock.readLock().lock()
        try {
            id2method(methodId)
        } finally {
            lock.readLock().unlock()
        }
    }

    def get(methodId: Int): Option[DeclaredMethod] = {
        lock.readLock().lock()
        try {
            if (methodId < id2method.length) Some(id2method(methodId))
            else None
        } finally {
            lock.readLock().unlock()
        }
    }

    def declaredMethods: Iterator[DeclaredMethod] = {
        import scala.jdk.CollectionConverters._
        // Thread-safe as .values() creates a view of the current state
        data.values().asScala.iterator.flatMap { _.values().asScala }
    }
}

