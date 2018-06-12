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
