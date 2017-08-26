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

import scala.annotation.tailrec
import scala.collection.mutable
import scala.collection.mutable.Queue

/**
 * Determines if a type (class, interface) is further extensible by yet unknown
 * types (that is, can be (transitively) inherited from).
 * == Special cases ==
 *
 * If a class is defined in a package starting with '''java.*''', it always has to be treated like
 * classes that belong to a closed package. This is necessary because the
 * default `ClassLoader` prevents the definition of further classes within these packages, hence,
 * they are closed by definition.
 *
 * If the analyzed codebase has an incomplete type hierarchy, which leads to unknown subtype
 * relationships, it is necessary to add these particular classes to the computed set of
 * extensible classes.
 *
 * == Extensibility w.r.t. Open Packages ==
 * A class is extensible if:
 *  - the class is not (effectively) final
 *  - one of its subclasses is extensible
 *
 * == Extensibility w.r.t. Closed Packages ==
 * A class is extensible if:
 *  - the class is public and not (effectively) final
 *  - one of its subclasses is extensible
 *
 * @author Michael Reif
 */
class TypeExtensibilityInformationAnalysis(
        val project: SomeProject
) extends (ObjectType ⇒ Answer) {

    lazy val typeExtensibility: Map[ObjectType, Answer] = initTypeExtensibilityInformation.toMap

    override def apply(t: ObjectType): Answer = typeExtensibility.getOrElse(t, Unknown)

    private[this] def initTypeExtensibilityInformation: mutable.Map[ObjectType, Answer] = {
        val leafTypes = project.classHierarchy.leafTypes

        if (leafTypes.isEmpty)
            return mutable.Map.empty;

        val isExtensibleMap = mutable.Map.empty[ObjectType, Answer]

        val typesToProcess = Queue.empty[ObjectType] ++ leafTypes
        val hasExtensibleSubtype = new Array[Boolean](ObjectType.objectTypesCount)
        val hasUnknownSubtype = new Array[Boolean](ObjectType.objectTypesCount)
        val isEnqueued = new Array[Boolean](ObjectType.objectTypesCount)
        typesToProcess foreach { ot ⇒
            isEnqueued(ot.id) = true
            if (project.classHierarchy.hasSubtypes(ot).isYesOrUnknown) {
                // this test should never succeed.... because we start with the leaf types...
                // hence, the types are known...
                throw new UnknownError()
                // hasUnknownSubtype(oid) = true
            }
        }

        determineExtensibility(
            typesToProcess,
            hasExtensibleSubtype, hasUnknownSubtype,
            isEnqueued,
            isExtensibleMap
        )(project.get(DirectTypeExtensibilityKey))
    }

    @tailrec final private[this] def determineExtensibility(
        typesToProcess:       Queue[ObjectType],
        hasExtensibleSubtype: Array[Boolean],
        hasUnknownSubtype:    Array[Boolean],
        isEnqueued:           Array[Boolean],
        typeExtensibilityMap: mutable.Map[ObjectType, Answer]
    )(
        implicit
        isDirectlyExtensible: ObjectType ⇒ Answer
    ): mutable.Map[ObjectType, Answer] = {
        //         We use a queue to ensure that we always first process all subtypes of a type to
        //         ensure that we have final knowledge about the subtypes' extensibility.

        val objectType = typesToProcess.dequeue()
        val oid = objectType.id
        project.classFile(objectType) match {
            case Some(classFile) ⇒
                val directlyExtensible = isDirectlyExtensible(objectType)
                val isExtensible = hasExtensibleSubtype(oid)

                if (isExtensible || directlyExtensible.isYes) {
                    classFile.superclassType.foreach(st ⇒ hasExtensibleSubtype(st.id) = true)
                    classFile.interfaceTypes.foreach(it ⇒ hasExtensibleSubtype(it.id) = true)
                    typeExtensibilityMap.put(objectType, Yes)
                } else if (hasUnknownSubtype(oid) || directlyExtensible.isUnknown) {
                    typeExtensibilityMap.put(objectType, Unknown)
                } else {
                    typeExtensibilityMap.put(objectType, No)
                }

            case None ⇒
                project.classHierarchy.directSupertypes(objectType).foreach { superType ⇒
                    hasUnknownSubtype(superType.id) = true
                }
        }

        project.classHierarchy.directSupertypes(objectType).foreach { superType ⇒
            if (!isEnqueued(superType.id)) {
                typesToProcess.enqueue(superType)
                isEnqueued(superType.id) = true
            }
        }

        if (typesToProcess.nonEmpty) {
            // tail-recursive call... to process the workqueue
            determineExtensibility(
                typesToProcess,
                hasExtensibleSubtype, hasUnknownSubtype,
                isEnqueued,
                typeExtensibilityMap
            )
        } else
            typeExtensibilityMap
    }

}
