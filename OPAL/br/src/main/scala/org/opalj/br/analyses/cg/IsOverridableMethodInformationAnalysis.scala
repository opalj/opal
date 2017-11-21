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

/**
 * This analysis provides information about whether a method can be overridden by a yet unknown type.
 *
 * @note This class does not provide any caching, i.e., if this information is queried multiple times
 *       per method it is recomputed.
 * @author Michael Reif
 */
private[analyses] class IsOverridableMethodInformationAnalysis(
        project:              SomeProject,
        isDirectlyExtensible: ObjectType ⇒ Answer,
        isTypeExtensible:     ObjectType ⇒ Answer
) extends (Method ⇒ Answer) {

    private[this] def isOverriddenOnPath(objectType: ObjectType, method: Method): Answer = {
        if (isDirectlyExtensible(objectType).isYes && !method.isFinal)
            return No;

        import project.classHierarchy
        val worklist = scala.collection.mutable.Queue.empty[ObjectType]
        var isUnknown = false;

        val methodName = method.name
        val methodDescriptor = method.descriptor

        def addDirectSubclasses(ot: ObjectType): Unit = {
            classHierarchy.directSubclassesOf(ot).foreach(worklist.enqueue(_))
        }

        while (worklist.nonEmpty) {
            val ot = worklist.dequeue()
            val isExtensible = isTypeExtensible(ot)
            if (isExtensible.isYesOrUnknown) {
                val cf = project.classFile(ot)
                val method: Option[Method] = cf.map {
                    cf ⇒ cf.findMethod(methodName, methodDescriptor)
                }.getOrElse(None)

                if (!method.isDefined) {
                    val isDirExtensible = isDirectlyExtensible(ot)
                    if (isDirExtensible.isYes) {
                        // it has not been overridden so far and the type is extensible
                        return No;
                    } else {
                        isUnknown |= isDirExtensible.isUnknown
                        addDirectSubclasses(ot)
                    }
                }
            }
        }

        if (isUnknown)
            return Unknown
        else return Yes;
    }

    def apply(method: Method): Answer = {
        val ot = method.declaringClassFile.thisType
        val isExtensibleType = isTypeExtensible(ot)

        if (isExtensibleType.isUnknown)
            return Unknown;

        if (isExtensibleType.isYes) {
            val isOverridden = isOverriddenOnPath(ot, method)
            if (isOverridden.isUnknown)
                return Unknown;

            if (isOverridden.isNo)
                return Yes;
        }

        return No
    }
}
