/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package properties
package pointsto

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
// TODO: we should definition sites (real points-to sets) instead of just the types
sealed trait TypeBasedPointsToSetPropertyMetaInformation extends PropertyMetaInformation {

    final type Self = TypeBasedPointsToSet
}

case class TypeBasedPointsToSet private[properties] (
    private val orderedTypes: List[ObjectType],
    override val types:       UIDSet[ObjectType]
) extends PointsToSetLike[ObjectType, UIDSet[ObjectType], TypeBasedPointsToSet]
        with OrderedProperty
        with TypeBasedPointsToSetPropertyMetaInformation {

    assert(orderedTypes == null || orderedTypes.size == types.size)

    final def key: PropertyKey[TypeBasedPointsToSet] = TypeBasedPointsToSet.key

    override def toString: String = s"PointsTo(size=${types.size})"

    override def checkIsEqualOrBetterThan(e: Entity, other: TypeBasedPointsToSet): Unit = {
        if (!types.subsetOf(other.types)) {
            throw new IllegalArgumentException(s"$e: illegal refinement of property $other to $this")
        }
    }

    override def included(
        newTypes: TraversableOnce[ObjectType], unused: TraversableOnce[ObjectType]
    ): TypeBasedPointsToSet = {
        var newOrderedTypes = orderedTypes
        var typesUnion = types
        for (t ← newTypes) {
            if (!types.contains(t)) {
                newOrderedTypes ::= t
                typesUnion += t
            }
        }
        new TypeBasedPointsToSet(newOrderedTypes, typesUnion)
    }

    override def included(
        other: TypeBasedPointsToSet
    ): TypeBasedPointsToSet = included(other.types, other.types)

    /**
     * Will return the types added most recently, dropping the `seenElements` oldest ones.
     */
    override def dropOldestTypes(seenTypes: Int): Iterator[ObjectType] = {
        orderedTypes.iterator.take(types.size - seenTypes)
    }

    override def numTypes: Int = types.size

    override def elements: UIDSet[ObjectType] = types

    override def equals(obj: Any): Boolean = {
        obj match {
            case that: TypeBasedPointsToSet ⇒
                that.numTypes == this.numTypes && that.orderedTypes == this.orderedTypes
            case _ ⇒ false
        }
    }

    override def hashCode: Int = types.hashCode() * 31

}

object TypeBasedPointsToSet extends TypeBasedPointsToSetPropertyMetaInformation {

    def apply(
        initialPointsTo: UIDSet[ObjectType]
    ): TypeBasedPointsToSet = {
        new TypeBasedPointsToSet(
            initialPointsTo.toList,
            initialPointsTo
        )
    }

    final val key: PropertyKey[TypeBasedPointsToSet] = {
        val name = "opalj.TypeBasedPointsToSet"
        PropertyKey.create(
            name,
            (_: PropertyStore, reason: FallbackReason, _: Entity) ⇒ reason match {
                case PropertyIsNotDerivedByPreviouslyExecutedAnalysis ⇒ NoTypes
                case _ ⇒
                    throw new IllegalStateException(s"no analysis is scheduled for property: $name")
            }
        )
    }
}

object NoTypes extends TypeBasedPointsToSet(List.empty, UIDSet.empty)
