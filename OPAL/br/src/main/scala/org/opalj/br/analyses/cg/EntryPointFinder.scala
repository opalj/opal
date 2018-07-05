/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2018
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
package cg

import scala.collection.mutable.ArrayBuffer

/**
 * The EntryPointFinder trait is a common trait for all analyses that can derive an programs entry
 * points. The concrete entry point finder that is used to determines a programs entry points directly
 * impacts the computation of a programs call graph.
 *
 * All subclasses should be implemented in a way that it is possible to chain them. (Decorator)
 *
 * @author Michael Reif
 */
sealed trait EntryPointFinder {

    /*
    * Returns the entry points with respect to a concrete scenario.
    *
    * This method must be implemented by any subtype.
    */
    def collectEntryPoints(project: SomeProject): Traversable[Method] = Set.empty[Method]
}

trait ApplicationEntryPointsFinder extends EntryPointFinder {

    override def collectEntryPoints(project: SomeProject): Traversable[Method] = {
        val MAIN_METHOD_DESCRIPTOR = MethodDescriptor.JustTakes(FieldType.apply("[Ljava/lang/String;"))

        super.collectEntryPoints(project) ++ project.allMethodsWithBody.collect {
            case m: Method if m.isStatic
                && (m.descriptor == MAIN_METHOD_DESCRIPTOR)
                && (m.name == "main") ⇒ m
        }
    }
}

object ApplicationEntryPointsFinder extends ApplicationEntryPointsFinder

trait LibraryEntryPointsFinder extends EntryPointFinder {

    override def collectEntryPoints(project: SomeProject): Traversable[Method] = {
        val isOverridable = project.get(IsOverridableMethodKey)
        val isClosedPackage = project.get(ClosedPackagesKey).isClosed _

        @inline def isEntryPoint(method: Method): Boolean = {
            val classFile = method.classFile
            val ot = classFile.thisType

            if (isClosedPackage(ot.packageName)) {
                if (method.isFinal) {
                    classFile.isPublic && method.isPublic
                } else {
                    isOverridable(method).isYesOrUnknown
                }
            } else {
                // all methods in an open package are accessible
                !method.isPrivate
            }
        }

        val eps = ArrayBuffer.empty[Method]

        project.allMethodsWithBody.foreach { method ⇒
            if (isEntryPoint(method))
                eps.append(method)
        }
        super.collectEntryPoints(project) ++ eps
    }
}

object LibraryEntryPointsFinder extends LibraryEntryPointsFinder

/*
* The MetaEntryPointsFinder is a conservative EntryPoints finder triggers all known finders.
*/
object MetaEntryPointsFinder
    extends ApplicationEntryPointsFinder
    with LibraryEntryPointsFinder
