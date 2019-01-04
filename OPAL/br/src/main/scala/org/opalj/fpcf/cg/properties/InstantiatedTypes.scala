/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package cg
package properties
import org.opalj.br.ObjectType
import org.opalj.collection.immutable.UIDSet

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
        private val orderedTypes: List[ObjectType],
        types:                    UIDSet[ObjectType]
) extends Property with OrderedProperty with InstantiatedTypesPropertyMetaInformation {

    assert(orderedTypes == null || orderedTypes.size == types.size)

    final def key: PropertyKey[InstantiatedTypes] = InstantiatedTypes.key

    override def toString: String = s"InstantiatedTypes(size=${types.size})"

    override def checkIsEqualOrBetterThan(e: Entity, other: InstantiatedTypes): Unit = {
        if (!types.subsetOf(other.types)) {
            throw new IllegalArgumentException(s"$e: illegal refinement of property $other to $this")
        }
    }

    def updated(newTypes: Set[ObjectType]): InstantiatedTypes = {

        var newOrderedTypes = orderedTypes
        for { t ← newTypes if !types.contains(t) } {
            newOrderedTypes ::= t
        }
        new InstantiatedTypes(newOrderedTypes, types ++ newTypes)
    }

    def getNewTypes(index: Int): Iterator[ObjectType] = {
        orderedTypes.iterator.take(types.size - index)
    }

    def numElements: Int = types.size
}

object InstantiatedTypes extends InstantiatedTypesPropertyMetaInformation {

    def apply(
        initialInstantiatedTypes: UIDSet[ObjectType]
    ): InstantiatedTypes = {
        new InstantiatedTypes(
            initialInstantiatedTypes.toList,
            initialInstantiatedTypes
        )
    }

    final val key: PropertyKey[InstantiatedTypes] = {
        val name = "opalj.InstantiatedTypes"
        PropertyKey.create(
            name,
            (_: PropertyStore, reason: FallbackReason, _: Entity) ⇒ reason match {
                case PropertyIsNotDerivedByPreviouslyExecutedAnalysis ⇒ NoTypes
                case _ ⇒
                    throw new IllegalStateException(s"No analysis is scheduled for property: $name")
            }
        )
    }
}

object NoTypes extends InstantiatedTypes(List.empty, UIDSet.empty)
