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
import org.opalj.br.analyses.ProjectInformationKey
import org.opalj.br.analyses.SomeProject

/**
 * The set of all [[org.opalj.br.DeclaredMethod]]s (potentially used by the property store).
 *
 * @author Dominik Helm
 */
class DeclaredMethods(
        val p:    SomeProject,
        var data: ConcurrentHashMap[DeclaredMethod, DeclaredMethod]
) {

    /**
     * Returns the canonical object representing a specific [[org.opalj.br.DeclaredMethod]].
     */
    def apply(dm: DeclaredMethod): DeclaredMethod = {
        val prev = data.putIfAbsent(dm, dm)
        if (prev == null) dm else prev
    }

    def apply(
        declaringClassType: ObjectType,
        name:               String,
        descriptor:         MethodDescriptor
    ): DeclaredMethod = {
        val cfo = p.classFile(declaringClassType)
        val mo = cfo.flatMap(_.findMethod(name, descriptor))

        mo match {
            case Some(method) ⇒ apply(method)
            case None         ⇒ apply(VirtualDeclaredMethod(declaringClassType, name, descriptor))
        }
    }

    def apply(method: Method): DefinedMethod = {
        data.get(
            DefinedMethod(method.classFile.thisType, method)
        ).asInstanceOf[DefinedMethod]
    }

    def declaredMethods: Iterator[DeclaredMethod] = {
        import scala.collection.JavaConverters.enumerationAsScalaIterator
        enumerationAsScalaIterator(data.keys)
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
        // outweighs the benefits even on systems with 4 or so cores..
        val declaredMethods: ConcurrentHashMap[DeclaredMethod, DeclaredMethod] =
            new ConcurrentHashMap

        p.allClassFiles.foreach { cf ⇒
            val classType = cf.thisType
            for {
                // all methods present in the current class file, excluding methods derived
                // from any supertype that are not overriden by this type.
                m ← cf.methods
                if m.isStatic || m.isPrivate || m.isAbstract || m.isInitializer
            } {
                val vm = DefinedMethod(classType, m)
                declaredMethods.put(vm, vm)
            }
            for {
                // all non-private, non-abstract instance methods present in the current class file,
                // including methods derived from any supertype that are not overriden by this type.
                mc ← p.instanceMethods(classType)
            } {
                val vm = DefinedMethod(classType, mc.method)
                declaredMethods.put(vm, vm)
            }
        }

        new DeclaredMethods(p, declaredMethods)
    }

}
