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

    def apply(
        declaredType: ObjectType,
        packageName:  String,
        runtimeType:  ObjectType,
        name:         String,
        descriptor:   MethodDescriptor
    ): DeclaredMethod = {
        val dmSet = data.computeIfAbsent(runtimeType, _ ⇒ new ConcurrentHashMap)

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

        // in case of an unseen method, compute id
        if (!dmSet.contains(context)) {
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
                        //IMPROVE use variable increment
                        val id2methodExt = new Array[DeclaredMethod](id2method.length + 1000)
                        Array.copy(id2method, 0, id2methodExt, 0, id2method.length)
                        id2method = id2methodExt
                    }
                    id2method(vm.id) = vm
                }
            } finally {
                lock.writeLock().unlock()
            }
        }
        dmSet.get(context)
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

    def declaredMethods: Iterator[DeclaredMethod] = {
        import scala.collection.JavaConverters._
        // Thread-safe as .values() creates a view of the current state
        data.values().asScala.iterator.flatMap { _.values().asScala }
    }
}

