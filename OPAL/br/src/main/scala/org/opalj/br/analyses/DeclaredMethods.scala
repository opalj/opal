/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses

import java.util.concurrent.ConcurrentHashMap

import org.opalj.br.MethodDescriptor.SignaturePolymorphicMethod
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
        val p: SomeProject,
        // We need concurrent, mutable maps here, as VirtualDeclaredMethods may be added when they
        // are queried. This can result in DeclaredMethods added for a type not yet seen, too (e.g.
        // methods on type Object when not analyzing the JDK.
        val data: ConcurrentHashMap[ReferenceType, ConcurrentHashMap[MethodContext, DeclaredMethod]]
) {

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
                _ ⇒ VirtualDeclaredMethod(classType, name, descriptor)
            )
        } else {
            method
        }
    }

    def apply(method: Method): DefinedMethod = {
        val classType = method.classFile.thisType
        data.get(classType).get(MethodContext(p, classType, method)).asInstanceOf[DefinedMethod]
    }

    def declaredMethods: Iterator[DeclaredMethod] = {
        import scala.collection.JavaConverters._
        // Thread-safe as .values() creates a view of the current state
        data.values().asScala.iterator.flatMap { _.values().asScala }
    }
}
