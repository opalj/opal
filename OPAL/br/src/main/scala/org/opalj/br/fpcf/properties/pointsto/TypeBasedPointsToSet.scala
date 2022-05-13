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
sealed trait TypeBasedPointsToSetPropertyMetaInformation extends PropertyMetaInformation {

    final type Self = TypeBasedPointsToSet
}

case class TypeBasedPointsToSet private[properties] (
        private val orderedTypes: List[ReferenceType],
        override val types:       UIDSet[ReferenceType]
) extends PointsToSetLike[ReferenceType, UIDSet[ReferenceType], TypeBasedPointsToSet]
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
        other: TypeBasedPointsToSet
    ): TypeBasedPointsToSet = {
        included(other, 0)
    }

    override def numTypes: Int = types.size

    override def elements: UIDSet[ReferenceType] = types

    override def equals(obj: Any): Boolean = {
        obj match {
            case that: TypeBasedPointsToSet =>
                that.numTypes == this.numTypes && that.orderedTypes == this.orderedTypes
            case _ => false
        }
    }

    override def hashCode: Int = types.hashCode() * 31

    override def numElements: Int = types.size

    override def included(
        other: TypeBasedPointsToSet, seenElements: Int
    ): TypeBasedPointsToSet = {
        var newOrderedTypes = orderedTypes
        var typesUnion = types

        other.orderedTypes.take(other.numElements - seenElements).map { t =>
            if (!types.contains(t)) {
                newOrderedTypes ::= t
                typesUnion += t
            }
        }

        if (types eq typesUnion)
            return this;

        new TypeBasedPointsToSet(newOrderedTypes, typesUnion)
    }

    override def forNewestNTypes[U](n: Int)(f: ReferenceType => U): Unit = {
        orderedTypes.take(n).map(f)
    }

    // here, the elements are the types
    override def forNewestNElements[U](n: Int)(f: ReferenceType => U): Unit = {
        orderedTypes.take(n).map(f)
    }

    override def included(
        other: TypeBasedPointsToSet, typeFilter: ReferenceType => Boolean
    ): TypeBasedPointsToSet = {
        included(other, 0, typeFilter)
    }

    override def included(
        other:        TypeBasedPointsToSet,
        seenElements: Int,
        typeFilter:   ReferenceType => Boolean
    ): TypeBasedPointsToSet = {
        if (typeFilter eq PointsToSetLike.noFilter)
            return included(other, seenElements);

        var newOrderedTypes = orderedTypes
        var typesUnion = types

        other.orderedTypes.take(other.numElements - seenElements).map { t =>
            if (typeFilter(t) && !types.contains(t)) {
                newOrderedTypes ::= t
                typesUnion += t
            }
        }

        if (types eq typesUnion)
            return this;

        new TypeBasedPointsToSet(newOrderedTypes, typesUnion)
    }

    override def filter(
        typeFilter: ReferenceType => Boolean
    ): TypeBasedPointsToSet = {
        if (typeFilter eq PointsToSetLike.noFilter)
            return this;

        var newTypes = UIDSet.empty[ReferenceType]
        val newOrderedTypes = orderedTypes.foldLeft(List.empty[ReferenceType]) { (r, t) =>
            if (typeFilter(t)) {
                newTypes += t
                t :: r
            } else {
                r
            }
        }

        if (newTypes.size == elements.size)
            return this;

        TypeBasedPointsToSet(newOrderedTypes, newTypes)
    }

    override def getNewestElement(): ReferenceType = orderedTypes.head
}

object TypeBasedPointsToSet extends TypeBasedPointsToSetPropertyMetaInformation {

    def apply(
        initialPointsTo: UIDSet[ReferenceType]
    ): TypeBasedPointsToSet = {
        new TypeBasedPointsToSet(
            initialPointsTo.foldLeft(List.empty[ReferenceType])((l, t) => t :: l),
            initialPointsTo
        )
    }

    final val key: PropertyKey[TypeBasedPointsToSet] = {
        val name = "opalj.TypeBasedPointsToSet"
        PropertyKey.create(
            name,
            (_: PropertyStore, reason: FallbackReason, _: Entity) => reason match {
                case PropertyIsNotDerivedByPreviouslyExecutedAnalysis => NoTypes
                case _ =>
                    throw new IllegalStateException(s"no analysis is scheduled for property: $name")
            }
        )
    }
}

object NoTypes extends TypeBasedPointsToSet(List.empty, UIDSet.empty)
