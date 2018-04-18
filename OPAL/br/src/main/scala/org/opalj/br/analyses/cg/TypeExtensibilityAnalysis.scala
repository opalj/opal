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

import scala.annotation.tailrec
import scala.collection.mutable

import org.opalj.collection.mutable.ArrayMap

/**
 * Determines if a type (class or interface) is further (transitively) extensible by yet unknown
 * types (that is, can be (transitively) inherited from).
 *
 * == Special cases ==
 * If a class is defined in a package starting with '''java.*''', it always has to be treated like
 * classes that belong to a closed package. This is necessary because the default `ClassLoader`
 * prevents the definition of further classes within these packages, hence, they are closed by
 * definition.
 *
 * If the analyzed codebase has an incomplete type hierarchy, which leads to unknown subtype
 * relationships, it is necessary to add these particular classes to the computed set of
 * extensible classes.
 *
 * @author Michael Reif
 */
class TypeExtensibilityAnalysis(val project: SomeProject) extends (ObjectType ⇒ Answer) {

    import project.classHierarchy
    import classHierarchy.foreachDirectSupertype

    @tailrec private[this] def determineExtensibility(
        typesToProcess:       mutable.Queue[ObjectType],
        subtypeExtensibility: Array[Answer],
        isEnqueued:           Array[Boolean],
        typeExtensibility:    ArrayMap[Answer]
    )(
        implicit
        isClassExtensible: ObjectType ⇒ Answer
    ): ArrayMap[Answer] = {
        val objectType = typesToProcess.dequeue
        val oid = objectType.id

        val thisSubtypeExtensibility = {
            val thisSubtypeExtensibility = subtypeExtensibility(oid)
            if (thisSubtypeExtensibility eq null) No else thisSubtypeExtensibility
        }
        val thisTypeExtensbility = isClassExtensible(objectType) match {
            case Yes     ⇒ Yes
            case Unknown ⇒ if (thisSubtypeExtensibility.isYes) Yes else Unknown
            case No      ⇒ thisSubtypeExtensibility
        }
        typeExtensibility(oid) = thisTypeExtensbility
        var update = false
        foreachDirectSupertype(objectType) { st ⇒
            val soid = st.id
            subtypeExtensibility(soid) match {
                case null | No ⇒ {
                    update = subtypeExtensibility(soid) ne thisTypeExtensbility
                    subtypeExtensibility(soid) = thisTypeExtensbility
                }
                case Yes ⇒ // do nothing
                case Unknown ⇒ {
                    update = subtypeExtensibility(soid) ne thisTypeExtensbility
                    if (thisTypeExtensbility.isYes) subtypeExtensibility(soid) = Yes
                }
            }

            // schedule supertypes
            if (!isEnqueued(soid) || update) {
                typesToProcess.enqueue(st)
                isEnqueued(soid) = true
            }
        }

        if (typesToProcess.nonEmpty) {
            // tail-recursive call... to process the workqueue
            determineExtensibility(
                typesToProcess,
                subtypeExtensibility,
                isEnqueued,
                typeExtensibility
            )
        } else {
            typeExtensibility
        }
    }

    private[this] val typeExtensibility: ArrayMap[Answer] = {
        implicit val isClassExtensible = project.get(ClassExtensibilityKey)

        val leafTypes = classHierarchy.leafTypes
        val objectTypesCount = ObjectType.objectTypesCount
        val typeExtensibility = ArrayMap[Answer](objectTypesCount)
        val subtypeExtensibility = new Array[Answer](objectTypesCount)
        val isEnqueued = new Array[Boolean](objectTypesCount)

        // We use a queue to ensure that we always first process all subtypes of a type. This
        // guarantees that we have final knowledge about the extensibility of all subtypes
        // of a type before we are processing the supertype.
        val typesToProcess = mutable.Queue.empty[ObjectType] ++ leafTypes
        typesToProcess.foreach { ot ⇒ isEnqueued(ot.id) = true }

        determineExtensibility(
            typesToProcess,
            subtypeExtensibility,
            isEnqueued,
            typeExtensibility
        )
    }

    override def apply(t: ObjectType): Answer = typeExtensibility.getOrElse(t.id, Unknown)
}
