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
package fpcf
package properties

import org.opalj.br.ObjectType
import org.opalj.br.analyses.ProjectLike
import org.opalj.collection.immutable.UIDSet
import org.opalj.collection.immutable.UIDSet1
import org.opalj.collection.mutable.AnyRefArrayBuffer

import scala.collection.Set

/**
 * Represent the set of types that have allocations reachable from the respective entry points.
 *
 * @author Florian Kuebler
 */
sealed trait InstantiatedTypesPropertyMetaInformation extends PropertyMetaInformation {

    final type Self = InstantiatedTypes
}

case class InstantiatedTypes private[properties] (
        // todo use boolean array here
        private val _orderedTypes: AnyRefArrayBuffer[ObjectType], private val _types: UIDSet[ObjectType]
) extends Property with OrderedProperty with InstantiatedTypesPropertyMetaInformation {

    def types: UIDSet[ObjectType] = _types

    assert(_orderedTypes == null || _orderedTypes.size == _types.size)

    final def key: PropertyKey[InstantiatedTypes] = InstantiatedTypes.key

    override def toString: String = s"InstantiatedTypes(size=${types.size}\n\t$types)"

    override def checkIsEqualOrBetterThan(e: Entity, other: InstantiatedTypes): Unit = {
        if ((other ne AllTypes) && !types.subsetOf(other.types)) {
            throw new IllegalArgumentException(s"$e: illegal refinement of property $other to $this")
        }
    }

    def updated(newTypes: Set[ObjectType]): InstantiatedTypes = {

        val newOrderedTypes = new AnyRefArrayBuffer[ObjectType](_orderedTypes.size + newTypes.size)
        newOrderedTypes ++= _orderedTypes
        for { t ← newTypes; if !types.contains(t) } {
            newOrderedTypes += t
        }
        new InstantiatedTypes(newOrderedTypes, _types ++ newTypes)
    }

    def getNewTypes(index: Int): Iterator[ObjectType] = _orderedTypes.iteratorFrom(index)

    def numElements: Int = _types.size
}

object InstantiatedTypes extends InstantiatedTypesPropertyMetaInformation {

    def initial(types: UIDSet[ObjectType]): InstantiatedTypes = {
        new InstantiatedTypes(new AnyRefArrayBuffer[ObjectType]() ++= types.iterator, types)
    }

    final val key: PropertyKey[InstantiatedTypes] = {
        PropertyKey.create[ProjectLike, InstantiatedTypes](
            "InstantiatedTypes",
            (_: PropertyStore, _: ProjectLike) ⇒ {
                AllTypes
            },
            (_, eps: EPS[ProjectLike, InstantiatedTypes]) ⇒ eps.ub,
            (_: PropertyStore, _: ProjectLike) ⇒ None
        )
    }

    def initialTypes: UIDSet[ObjectType] = {
        // TODO make this configurable
        UIDSet1(ObjectType.String)
    }
}

// todo we can not use null here, so we need special handling
object AllTypes extends InstantiatedTypes(null, null) {
    override def checkIsEqualOrBetterThan(e: Entity, other: InstantiatedTypes): Unit = {}

    override def updated(newTypes: Set[ObjectType]): InstantiatedTypes = this

    override def toString: String = "AllTypesInstantiated"

    override def getNewTypes(index: Int): Iterator[ObjectType] = Iterator.empty

    override def numElements: Int = throw new UnsupportedOperationException()

    override def types: UIDSet[ObjectType] = throw new UnsupportedOperationException()
}
