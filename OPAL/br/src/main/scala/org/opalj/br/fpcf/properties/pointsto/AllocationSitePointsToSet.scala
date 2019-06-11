/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package properties
package pointsto

import org.opalj.collection.immutable.LongTrieSet
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
sealed trait AllocationSitePointsToSetPropertyMetaInformation extends PropertyMetaInformation {

    final type Self = AllocationSitePointsToSet
}

case class AllocationSitePointsToSet private[pointsto] (
        override val elements:       LongTrieSet,
        private val orderedElements: List[Long],
        override val types:          UIDSet[ObjectType],
        private val orderedTypes:    List[ObjectType]
) extends PointsToSetLike[AllocationSite, LongTrieSet, AllocationSitePointsToSet]
    with OrderedProperty
    with AllocationSitePointsToSetPropertyMetaInformation {

    final def key: PropertyKey[AllocationSitePointsToSet] = AllocationSitePointsToSet.key

    override def toString: String = s"PointsTo(size=${elements.size})"

    override def checkIsEqualOrBetterThan(e: Entity, other: AllocationSitePointsToSet): Unit = {
        if (!types.subsetOf(other.types)) {
            throw new IllegalArgumentException(s"$e: illegal refinement of property $other to $this")
        }
    }

    /**
     * Will return the types added most recently, dropping the `seenElements` oldest ones.
     */
    override def dropOldestTypes(seenTypes: Int): Iterator[ObjectType] = {
        orderedTypes.iterator.take(types.size - seenTypes)
    }

    override def numTypes: Int = types.size

    override def included(other: AllocationSitePointsToSet): AllocationSitePointsToSet = {
        included(other, 0)
    }

    override def numElements: Int = elements.size

    override def dropOldestElements(seenElements: Int): Iterator[AllocationSite] = {
        orderedElements.iterator.take(elements.size - seenElements)
    }

    override def included(
        other: AllocationSitePointsToSet, seenElements: Int
    ): AllocationSitePointsToSet = {
        var newAllocationSites = elements
        var newOrderedAllocationSites = orderedElements
        other.dropOldestElements(seenElements).foreach { e ⇒
            val old = newAllocationSites
            newAllocationSites += e
            if (old ne newAllocationSites) {
                newOrderedAllocationSites ::= e
            }
        }

        var newTypes = types
        // IMPROVE: Somehow also use seenElements
        var newOrderedTypes = orderedTypes
        other.orderedTypes.foreach { newType ⇒
            val old = newTypes
            newTypes += newType
            if (newTypes ne old) {
                newOrderedTypes ::= newType
            }
        }
        if ((elements eq newAllocationSites) && (newTypes eq types))
            return this;

        new AllocationSitePointsToSet(
            newAllocationSites, newOrderedAllocationSites, newTypes, newOrderedTypes
        )

    }

    override def equals(obj: Any): Boolean = {
        obj match {
            case that: AllocationSitePointsToSet ⇒
                that.numTypes == this.numTypes &&
                    that.elements.size == this.elements.size &&
                    that.orderedTypes == this.orderedTypes &&
                    that.elements == this.elements
            case _ ⇒ false
        }
    }

    override def hashCode: Int = elements.hashCode() * types.hashCode() * 31
}

object AllocationSitePointsToSet extends AllocationSitePointsToSetPropertyMetaInformation {

    final val key: PropertyKey[AllocationSitePointsToSet] = {
        val name = "opalj.AllocationSitePointsToSet"
        PropertyKey.create(
            name,
            (_: PropertyStore, reason: FallbackReason, _: Entity) ⇒ reason match {
                case PropertyIsNotDerivedByPreviouslyExecutedAnalysis ⇒ NoAllocationSites
                case _ ⇒
                    throw new IllegalStateException(s"no analysis is scheduled for property: $name")
            }
        )
    }
}

object NoAllocationSites extends AllocationSitePointsToSet(
    LongTrieSet.empty, List.empty, UIDSet.empty, List.empty
) {
    override def included(other: AllocationSitePointsToSet): AllocationSitePointsToSet = {
        other
    }
}
