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
package cg

import scala.collection.mutable

/**
 * This analysis provides information about whether a method can be overridden by a yet unknown
 * type. I.e., it determines whether the set of methods which potentially overrides a given method
 * is downwards (w.r.t. the class hierarchy) closed or not. For those methods, where the set is
 * downwards closed, it is always possible and meaningful to compute abstractions that abstract
 * over all (virtual) methods defined by all subtypes. For convenience purposes, it is possible
 * to also test non-overridable methods (constructors, static initializers, static methods, and
 * private instance methods) for overridability. In that case the answer will always be No.
 *
 * @note This class does not provide any caching, i.e., if this information will be queried over
 *       and over again some caching should be implemented.
 *
 * @author Michael Reif
 * @author Michael Eichberg
 */
private[analyses] class IsOverridableMethodAnalysis(
        project:              SomeProject,
        isDirectlyExtensible: ObjectType ⇒ Answer,
        isTypeExtensible:     ObjectType ⇒ Answer
) extends (Method ⇒ Answer) {

    private[this] def isAlwaysFinallyOverridden(objectType: ObjectType, method: Method): Answer = {
        if (isDirectlyExtensible(objectType).isYes && !method.isFinal)
            return No;

        import project.classHierarchy
        import project.instanceMethods
        val methodName = method.name
        val methodDescriptor = method.descriptor
        val methodPackageName = method.classFile.thisType.packageName

        val worklist = mutable.Queue.empty[ObjectType]

        def addDirectSubclasses(ot: ObjectType): Unit = {
            classHierarchy.directSubclassesOf(ot).foreach(worklist.enqueue(_))
        }

        while (worklist.nonEmpty) {
            val ot = worklist.dequeue()
            if (isTypeExtensible(ot).isYesOrUnknown) {
                val cf = project.classFile(ot)
                val subtypeMethod = cf.flatMap(_.findMethod(methodName, methodDescriptor))
                if (subtypeMethod.isEmpty ||
                    !subtypeMethod.get.isFinal ||
                    (
                        // let's test if this "final override", is for a different method...
                        method.isPackagePrivate &&
                        subtypeMethod.get.declaringClassFile.thisType.packageName !=
                        objectType.packageName &&
                        !subtypeMethod.get.isPackagePrivate /**/ &&
                        {
                            // ... the original method is package private
                            // ... both methods are defined in different packages
                            // ... the subtypeMethod is protected or public
                            val candidateMethods = instanceMethods(ot).iterator.filter(mdc ⇒
                                mdc.name == methodName && mdc.descriptor == methodDescriptor)
                            // if we still have the original method in the list then this method
                            // does not override that method...
                            candidateMethods.exists(mdc ⇒ mdc.packageName == methodPackageName)
                        }
                    )) {
                    // the type as a whole is extensible...
                    // the method is not (finally) overridden by this type...
                    isDirectlyExtensible(ot) match {
                        case Yes     ⇒ return No;
                        case Unknown ⇒ return Unknown;
                        case No      ⇒ addDirectSubclasses(ot)
                    }
                }
                // ... if the method is final we do not need to consider further subtypes
            }
            // ... if the type as a whole is not extensible the method cannot be overridden
        }

        Yes
    }

    def apply(method: Method): Answer = {
        if (method.isPrivate || method.isStatic || method.isInitializer)
            return No;

        val ot = method.declaringClassFile.thisType
        val isExtensibleType = isTypeExtensible(ot)

        if (isExtensibleType.isNoOrUnknown)
            // We "inherit" the (non/unknown)extensibility of the class in this case:
            return isExtensibleType;

        // the type is extensible... Let's check the method:
        isAlwaysFinallyOverridden(ot, method).negate
    }
}
