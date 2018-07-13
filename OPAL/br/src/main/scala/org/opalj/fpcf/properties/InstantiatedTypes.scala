/* BSD 2-Clause License - see OPAL/LICENSE for details. */
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
            (_: PropertyStore, _: FallbackReason, _: ProjectLike) ⇒ {
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
