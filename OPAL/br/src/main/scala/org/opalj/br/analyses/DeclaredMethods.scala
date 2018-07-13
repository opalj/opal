/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import org.opalj.br.MethodDescriptor.SignaturePolymorphicMethod
import org.opalj.br.ObjectType.MethodHandle
import org.opalj.br.ObjectType.VarHandle
import org.opalj.br.analyses.DeclaredMethodsKey.MethodContext
import org.opalj.br.analyses.DeclaredMethodsKey.MethodContextQuery

import scala.collection.mutable.ArrayBuffer

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
        private[this] val id2method: ArrayBuffer[DeclaredMethod],
        private[this] val method2id: Object2IntOpenHashMap[DeclaredMethod],
        private[this] var _size:     Int
) {
    val id = new AtomicInteger(id2method.size)

    def apply(
        declaredType: ObjectType,
        packageName:  String,
        classType:    ObjectType,
        name:         String,
        descriptor:   MethodDescriptor
    ): DeclaredMethod = {
        val dmSet = data.computeIfAbsent(classType, _ ⇒ new ConcurrentHashMap)

        var method =
            dmSet.get(new MethodContextQuery(p, declaredType, packageName, name, descriptor))

        if (method == null && ((classType eq MethodHandle) || (classType eq VarHandle))) {
            method = dmSet.get(
                new MethodContextQuery(
                    p,
                    declaredType,
                    packageName,
                    name,
                    SignaturePolymorphicMethod
                )
            )
        }

        if (method == null) {
            // No matching declared method found, need to construct a virtual declared method, but
            // a concurrent execution of this method may have put the virtual declared method into
            // the set already already
            dmSet.computeIfAbsent(
                new MethodContext(name, descriptor),
                _ ⇒ {
                    val vm = VirtualDeclaredMethod(classType, name, descriptor)
                    val vmId = id.getAndIncrement()
                    method2id.put(vm, vmId)
                    id2method += vm
                    _size += 1
                    ???
                }
            )
        } else {
            method
        }
    }

    def apply(method: Method): DefinedMethod = {
        val classType = method.classFile.thisType
        data.get(classType).get(MethodContext(p, classType, method)).asInstanceOf[DefinedMethod]
    }

    def apply(methodId: Int): DeclaredMethod = {
        id2method(methodId)
    }

    def methodID(dm: DeclaredMethod): Int = {
        method2id.getInt(dm)
    }

    def declaredMethods: Iterator[DeclaredMethod] = {
        import scala.collection.JavaConverters._
        // Thread-safe as .values() creates a view of the current state
        data.values().asScala.iterator.flatMap { _.values().asScala }
    }

    def size: Int = _size
}
