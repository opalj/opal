/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package properties
package cg

import org.opalj.collection.immutable.UIDSet
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EPK
import org.opalj.fpcf.Entity
import org.opalj.fpcf.FallbackReason
import org.opalj.fpcf.InterimEP
import org.opalj.fpcf.InterimEUBP
import org.opalj.fpcf.InterimUBP
import org.opalj.fpcf.OrderedProperty
import org.opalj.fpcf.PropertyIsNotDerivedByPreviouslyExecutedAnalysis
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation
import org.opalj.fpcf.PropertyStore
import org.opalj.br.ReferenceType

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
        private val orderedTypes: List[ReferenceType],
        types:                    UIDSet[ReferenceType]
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

    def updated(newTypes: IterableOnce[ReferenceType]): InstantiatedTypes = {
        var newOrderedTypes = orderedTypes
        for { t <- newTypes.iterator if !types.contains(t) } {
            newOrderedTypes ::= t
        }
        new InstantiatedTypes(newOrderedTypes, types ++ newTypes)
    }

    /**
     * Will return the instantiated types added most recently, dropping the `num` oldest ones.
     */
    def dropOldest(num: Int): Iterator[ReferenceType] = {
        orderedTypes.iterator.take(types.size - num)
    }

    def numElements: Int = types.size
}

object InstantiatedTypes extends InstantiatedTypesPropertyMetaInformation {

    def apply(
        initialInstantiatedTypes: UIDSet[ReferenceType]
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
            (_: PropertyStore, reason: FallbackReason, _: Entity) => reason match {
                case PropertyIsNotDerivedByPreviouslyExecutedAnalysis => NoInstantiatedTypes
                case _ =>
                    throw new IllegalStateException(s"No analysis is scheduled for property: $name")
            }
        )
    }

    /**
     * Returns an update function to use for PartialResults.
     *
     * If the entity has a pre-existing type set, the update function performs a set union of
     * the existing and new types. If the type set does not exist, it will be newly created with
     * the given type set.
     *
     * @param entity The entity to update.
     * @param newInstantiatedTypes Set of new types to add to the entity's type set.
     * @param eop The state of the property when the update takes place.
     * @tparam E The type of the entity.
     * @return Returns the new property state when the update changed the property, otherwise None.
     */
    def update[E >: Null <: Entity](
        entity:               E,
        newInstantiatedTypes: UIDSet[ReferenceType]
    )(
        eop: EOptionP[E, InstantiatedTypes]
    ): Option[InterimEP[E, InstantiatedTypes]] = eop match {
        case InterimUBP(ub: InstantiatedTypes) =>
            val newUB = ub.updated(newInstantiatedTypes)
            if (newUB.types.size > ub.types.size)
                Some(InterimEUBP(entity, newUB))
            else
                None

        case _: EPK[_, _] =>
            val newUB = InstantiatedTypes.apply(newInstantiatedTypes)
            Some(InterimEUBP(entity, newUB))

        case r => throw new IllegalStateException(s"unexpected previous result $r")
    }
}

object NoInstantiatedTypes extends InstantiatedTypes(List.empty, UIDSet.empty)
