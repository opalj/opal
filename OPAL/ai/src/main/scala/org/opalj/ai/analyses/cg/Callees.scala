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
package ai
package analyses
package cg

import scala.collection.Set
import org.opalj.br.Method
import org.opalj.br.MethodDescriptor
import org.opalj.br.MethodSignature
import org.opalj.br.ObjectType
import org.opalj.br.ClassHierarchy
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.cg.InstantiableClassesKey

trait Callees {

    implicit def project: SomeProject

    final private[this] lazy val instantiableClasses = project.get(InstantiableClassesKey)

    final private[this] def classHierarchy: ClassHierarchy = project.classHierarchy

    def cache: CallGraphCache[MethodSignature, Set[Method]]

    @inline def callees(
        caller:             Method,
        declaringClassType: ObjectType,
        isInterface:        Boolean,
        name:               String,
        descriptor:         MethodDescriptor
    ): Set[Method] = {

        assert(
            classHierarchy.isInterface(declaringClassType) == Answer(isInterface),
            s"callees - inconsistent isInterface information for $declaringClassType"
        )

        def classesFilter(m: Method) = !instantiableClasses.isNotInstantiable(m.classFile.thisType)

        def callTargets() = {
            if (isInterface)
                project.
                    interfaceCall(declaringClassType, name, descriptor).
                    filter(classesFilter)
            else {
                val callerPackage = caller.classFile.thisType.packageName
                project.
                    virtualCall(callerPackage, declaringClassType, name, descriptor).
                    filter(classesFilter)
            }
        }

        if (!isInterface) {
            // Only if the method is public, we can cache the result to avoid that we cache
            // wrong results...
            project.resolveClassMethodReference(declaringClassType, name, descriptor) match {
                case Success(resolvedMethod) ⇒
                    if (!resolvedMethod.isPublic)
                        return callTargets();
                // else ... go on with the analysis and try to cache the results.
                case _ ⇒
                    // no caching...
                    callTargets()
            }
        }

        classHierarchy.hasSubtypes(declaringClassType) match {
            case Yes ⇒
                val methodSignature = new MethodSignature(name, descriptor)
                cache.getOrElseUpdate(declaringClassType, methodSignature)(
                    callTargets(),
                    syncOnEvaluation = true //false
                )

            case /* no caching; not worth the effort... */ No ⇒ callTargets()

            case /*Unknown <=> the type is unknown */ _       ⇒ Set.empty
        }
    }

}
