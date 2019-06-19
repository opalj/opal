/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package properties
package pointsto

import org.opalj.collection.immutable.Chain
import org.opalj.collection.immutable.EmptyLongList
import org.opalj.collection.immutable.LongList
import org.opalj.collection.immutable.LongTrieSet
import org.opalj.collection.immutable.Naught
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

trait AllocationSitePointsToSet
    extends PointsToSetLike[AllocationSite, LongTrieSet, AllocationSitePointsToSet]
    with OrderedProperty
    with AllocationSitePointsToSetPropertyMetaInformation {

    final def key: PropertyKey[AllocationSitePointsToSet] = AllocationSitePointsToSet.key

    override def toString: String = s"PointsTo(size=${elements.size})"

    override def checkIsEqualOrBetterThan(e: Entity, other: AllocationSitePointsToSet): Unit = {
        if (!types.subsetOf(other.types)) {
            throw new IllegalArgumentException(s"$e: illegal refinement of property $other to $this")
        }
    }

}

object AllocationSitePointsToSet extends AllocationSitePointsToSetPropertyMetaInformation {

    def apply(
        allocationSite: AllocationSite, allocatedType: ObjectType
    ): AllocationSitePointsToSet = {
        AllocationSitePointsToSet1(allocationSite, allocatedType)
    }

    def apply(
        allocationSiteNew: AllocationSite,
        allocatedTypeNew:  ObjectType,
        allocationSiteOld: AllocationSite,
        allocatedTypeOld:  ObjectType
    ): AllocationSitePointsToSet = {
        assert(allocationSiteOld != allocationSiteNew)
        AllocationSitePointsToSetN(
            LongTrieSet(allocationSiteOld, allocationSiteNew),
            allocationSiteNew +: allocationSiteOld +: EmptyLongList,
            UIDSet(allocatedTypeOld, allocatedTypeNew),
            if (allocatedTypeNew != allocatedTypeOld)
                allocatedTypeNew :&: allocatedTypeOld :&: Naught
            else
                allocatedTypeNew :&: Naught
        )
    }

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

case class AllocationSitePointsToSetN private[pointsto] (
        override val elements:       LongTrieSet,
        private val orderedElements: LongList,
        override val types:          UIDSet[ObjectType],
        private val orderedTypes:    Chain[ObjectType]
) extends AllocationSitePointsToSet {

    override def numTypes: Int = types.size

    override def included(other: AllocationSitePointsToSet): AllocationSitePointsToSet = {
        included(other, 0)
    }

    override def numElements: Int = elements.size

    override def included(
        other: AllocationSitePointsToSet, seenElements: Int
    ): AllocationSitePointsToSet = {
        var newAllocationSites = elements
        var newOrderedAllocationSites = orderedElements

        other.forNewestNElements(other.numElements - seenElements)(e ⇒ {
            val old = newAllocationSites
            newAllocationSites += e
            if (old ne newAllocationSites) {
                newOrderedAllocationSites +:= e
            }
        })

        var newTypes = types
        // IMPROVE: Somehow also use seenElements
        var newOrderedTypes = orderedTypes
        // IMPROVE: iterating over
        other.types.foreach { newType ⇒
            val old = newTypes
            newTypes += newType
            if (newTypes ne old) {
                newOrderedTypes :&:= newType
            }
        }
        if ((elements eq newAllocationSites) && (newTypes eq types))
            return this;

        new AllocationSitePointsToSetN(
            newAllocationSites, newOrderedAllocationSites, newTypes, newOrderedTypes
        )
    }

    override def equals(obj: Any): Boolean = {
        obj match {
            case that: AllocationSitePointsToSetN ⇒
                that.numTypes == this.numTypes &&
                    that.elements.size == this.elements.size &&
                    that.orderedTypes == this.orderedTypes &&
                    that.elements == this.elements
            case _ ⇒ false
        }
    }

    override def hashCode: Int = elements.hashCode() * types.hashCode() * 31

    override def forNewestNTypes[U](n: Int)(f: ObjectType ⇒ U): Unit = {
        orderedTypes.forFirstN(n)(f)
    }

    override def forNewestNElements[U](n: Int)(f: AllocationSite ⇒ U): Unit = {
        orderedElements.forFirstN(n, f)
    }
}

object NoAllocationSites extends AllocationSitePointsToSet {
    override def included(other: AllocationSitePointsToSet): AllocationSitePointsToSet = {
        other
    }

    override def included(
        other: AllocationSitePointsToSet, seenElements: Int
    ): AllocationSitePointsToSet = {
        assert(seenElements == 0)
        other
    }

    override def numTypes: Int = 0

    override def types: UIDSet[ObjectType] = UIDSet.empty

    override def numElements: Int = 0

    override def elements: LongTrieSet = LongTrieSet.empty

    override def forNewestNTypes[U](n: Int)(f: ObjectType ⇒ U): Unit = {
        assert(n == 0)
    }

    override def forNewestNElements[U](n: Int)(f: AllocationSite ⇒ U): Unit = {
        assert(n == 0)
    }
}

case class AllocationSitePointsToSet1(
        allocationSite: AllocationSite, allocatedType: ObjectType
) extends AllocationSitePointsToSet {

    override def numTypes: Int = 1

    override def types: UIDSet[ObjectType] = UIDSet(allocatedType)

    override def numElements: Int = 1

    override def elements: LongTrieSet = LongTrieSet(allocationSite)

    override def included(other: AllocationSitePointsToSet): AllocationSitePointsToSet = {
        other match {
            case AllocationSitePointsToSet1(`allocationSite`, `allocatedType`) ⇒
                this

            case AllocationSitePointsToSet1(otherAllocationSite, otherAllocatedType) ⇒
                AllocationSitePointsToSet(
                    otherAllocationSite, otherAllocatedType, allocationSite, allocatedType
                )

            case NoAllocationSites ⇒
                this

            case AllocationSitePointsToSetN(otherAllocationSites, otherOrderedAllocationSites, otherTypes, otherOrderedTypes) ⇒
                var newOrderedAllocations = allocationSite +: EmptyLongList
                otherOrderedAllocationSites.foreach { as ⇒
                    if (as != allocationSite) {
                        newOrderedAllocations +:= as
                    }
                }

                val newOrderedTypes = otherOrderedTypes.foldLeft(allocatedType :&: Naught) { (l, at) ⇒
                    if (at != allocatedType) at :&: l else l
                }

                AllocationSitePointsToSetN(
                    otherAllocationSites + allocationSite,
                    newOrderedAllocations,
                    otherTypes + allocatedType,
                    newOrderedTypes
                )
            case _ ⇒
                throw new IllegalArgumentException(s"unexpected list $other")
        }
    }

    override def included(
        other: AllocationSitePointsToSet, seenElements: Int
    ): AllocationSitePointsToSet = {
        assert(seenElements >= 0 && seenElements <= other.numElements)
        // Note, that we can not assert, that seenElements is between 0 and 1, as this can
        // happen by unordered partial results.
        included(other)
    }

    override def forNewestNTypes[U](n: Int)(f: ObjectType ⇒ U): Unit = {
        assert(n == 0 || n == 1)
        if (n == 1)
            f(allocatedType)
    }

    override def forNewestNElements[U](n: Int)(f: AllocationSite ⇒ U): Unit = {
        assert(n == 0 || n == 1)
        if (n == 1)
            f(allocationSite)
    }
}