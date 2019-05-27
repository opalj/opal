/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package cg
package properties

import scala.collection.Set

import org.opalj.collection.immutable.UIDSet
import org.opalj.fpcf.Entity
import org.opalj.fpcf.FallbackReason
import org.opalj.fpcf.OrderedProperty
import org.opalj.fpcf.PropertyIsNotDerivedByPreviouslyExecutedAnalysis
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation
import org.opalj.fpcf.PropertyStore

/**
 * Represent the set of types that have allocations reachable from the respective entry points.
 *
 * @author Florian Kuebler
 */
// todo code duplication in [[LoadedClasses]]
sealed trait InstantiatedTypesPropertyMetaInformation extends PropertyMetaInformation {

    final type Self = InstantiatedTypes
}

case class InstantiatedTypes private[properties] (
        private val orderedTypes: List[ObjectType],
        types:                    UIDSet[ObjectType]
) extends OrderedProperty
    with InstantiatedTypesPropertyMetaInformation {

    assert(orderedTypes == null || orderedTypes.size == types.size)

    final def key: PropertyKey[InstantiatedTypes] = InstantiatedTypes.key

    override def toString: String = s"InstantiatedTypes(size=${types.size})"

    override def checkIsEqualOrBetterThan(e: Entity, other: InstantiatedTypes): Unit = {
        if (!types.subsetOf(other.types)) {
            throw new IllegalArgumentException(s"$e: illegal refinement of $other to $this")
        }
    }

    def updated(newTypes: Set[ObjectType]): InstantiatedTypes = {
        var newOrderedTypes = orderedTypes
        for { t ← newTypes if !types.contains(t) } {
            newOrderedTypes ::= t
        }
        new InstantiatedTypes(newOrderedTypes, types ++ newTypes)
    }

    /**
     * Will return the instantiated types added most recently, dropping the `num` oldest ones.
     */
    def dropOldest(num: Int): Iterator[ObjectType] = {
        orderedTypes.iterator.take(types.size - num)
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
