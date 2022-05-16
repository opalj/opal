/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package properties
package pointsto

import org.opalj.collection.immutable.IntTrieSet
import org.opalj.collection.immutable.LongLinkedSet
import org.opalj.collection.immutable.LongTrieSetWithList
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

sealed trait AllocationSitePointsToSet
    extends PointsToSetLike[AllocationSite, LongLinkedSet, AllocationSitePointsToSet]
    with OrderedProperty
    with AllocationSitePointsToSetPropertyMetaInformation {

    final def key: PropertyKey[AllocationSitePointsToSet] = AllocationSitePointsToSet.key

    override def toString: String = s"PointsTo(size=${elements.size})"

    override def checkIsEqualOrBetterThan(e: Entity, other: AllocationSitePointsToSet): Unit = {
        if (!types.subsetOf(other.types)) {
            throw new IllegalArgumentException(s"$e: illegal refinement of property $other to $this")
        }
    }

    protected[this] def orderedTypes: List[ReferenceType]
    override def types: UIDSet[ReferenceType]

    override def included(other: AllocationSitePointsToSet): AllocationSitePointsToSet = {
        included(other, 0)
    }

    override def included(
        other: AllocationSitePointsToSet, seenElements: Int
    ): AllocationSitePointsToSet = {
        var newAllocationSites = elements
        var newTypes = types
        var newOrderedTypes = orderedTypes

        other.forNewestNElements(other.numElements - seenElements) { allocationSite =>
            val tpe = ReferenceType.lookup(allocationSiteLongToTypeId(allocationSite))
            val oldAllocationSites = newAllocationSites
            newAllocationSites += allocationSite
            if (newAllocationSites ne oldAllocationSites) {
                val oldTypes = newTypes
                newTypes += tpe
                if (newTypes ne oldTypes)
                    newOrderedTypes ::= tpe
            }
        }

        if (newAllocationSites eq elements)
            return this;

        AllocationSitePointsToSet(newAllocationSites, newTypes, newOrderedTypes)
    }

    override def included(
        other: AllocationSitePointsToSet, typeFilter: ReferenceType => Boolean
    ): AllocationSitePointsToSet = {
        if (typeFilter eq PointsToSetLike.noFilter)
            return included(other);

        var newTypes = types
        var newOrderedTypes = orderedTypes

        val newAllocationSites = other.elements.foldLeft(elements) { (r, allocationSite) =>
            val tpe = ReferenceType.lookup(allocationSiteLongToTypeId(allocationSite))
            if (typeFilter(tpe)) {
                val newAllocationSites = r + allocationSite
                if (newAllocationSites ne r) {
                    val oldTypes = newTypes
                    newTypes += tpe
                    if (newTypes ne oldTypes)
                        newOrderedTypes ::= tpe
                }
                newAllocationSites
            } else {
                r
            }
        }

        if (newAllocationSites eq elements)
            return this;

        AllocationSitePointsToSet(newAllocationSites, newTypes, newOrderedTypes)
    }

    override def included(
        other:        AllocationSitePointsToSet,
        seenElements: Int,
        typeFilter:   ReferenceType => Boolean
    ): AllocationSitePointsToSet = {
        if (typeFilter eq PointsToSetLike.noFilter)
            return included(other, seenElements);

        var newAllocationSites = elements
        var newTypes = types
        var newOrderedTypes = orderedTypes

        other.forNewestNElements(other.numElements - seenElements) { allocationSite =>
            val tpe = ReferenceType.lookup(allocationSiteLongToTypeId(allocationSite))
            if (typeFilter(tpe)) {
                val oldAllocationSites = newAllocationSites
                newAllocationSites += allocationSite
                if (newAllocationSites ne oldAllocationSites) {
                    val oldTypes = newTypes
                    newTypes += tpe
                    if (newTypes ne oldTypes)
                        newOrderedTypes ::= tpe
                }
            }
        }

        if (newAllocationSites eq elements)
            return this;

        AllocationSitePointsToSet(newAllocationSites, newTypes, newOrderedTypes)
    }

    override def filter(typeFilter: ReferenceType => Boolean): AllocationSitePointsToSet = {
        if (typeFilter eq PointsToSetLike.noFilter)
            return this;

        var newTypes = UIDSet.empty[ReferenceType]
        var newOrderedTypes = List.empty[ReferenceType]

        val newAllocationSites =
            elements.foldLeft(LongTrieSetWithList.empty) { (r, allocationSite) =>
                val tpe = ReferenceType.lookup(allocationSiteLongToTypeId(allocationSite))
                if (typeFilter(tpe)) {
                    val newAllocationSites = r + allocationSite
                    if (newAllocationSites ne r) {
                        val oldTypes = newTypes
                        newTypes += tpe
                        if (newTypes ne oldTypes)
                            newOrderedTypes ::= tpe
                    }
                    newAllocationSites
                } else {
                    r
                }
            }

        if (newAllocationSites.size == elements.size)
            return this;

        AllocationSitePointsToSet(newAllocationSites, newTypes, newOrderedTypes)
    }

    assert {
        var asTypes = IntTrieSet.empty
        elements.foreach { allocationSite =>
            asTypes += allocationSiteLongToTypeId(allocationSite)
        }

        val typeIds = types.foldLeft(IntTrieSet.empty) { (r, t) => r + t.id }
        typeIds == asTypes
    }
    assert(numElements >= numTypes)
}

object AllocationSitePointsToSet extends AllocationSitePointsToSetPropertyMetaInformation {

    def apply(
        allocationSite: AllocationSite, allocatedType: ReferenceType
    ): AllocationSitePointsToSet = {
        new AllocationSitePointsToSet1(allocationSite, allocatedType)
    }

    def apply(
        allocationSiteNew: AllocationSite,
        allocatedTypeNew:  ReferenceType,
        allocationSiteOld: AllocationSite,
        allocatedTypeOld:  ReferenceType
    ): AllocationSitePointsToSet = {
        assert(allocationSiteOld != allocationSiteNew)
        new AllocationSitePointsToSetN(
            LongTrieSetWithList(allocationSiteNew, allocationSiteOld),
            UIDSet(allocatedTypeOld, allocatedTypeNew),
            if (allocatedTypeNew != allocatedTypeOld)
                List(allocatedTypeNew, allocatedTypeOld)
            else
                List(allocatedTypeNew)
        )
    }

    def apply(
        elements: LongLinkedSet, types: UIDSet[ReferenceType], orderedTypes: List[ReferenceType]
    ): AllocationSitePointsToSet = {

        if (elements.isEmpty) {
            NoAllocationSites
        } else if (elements.size == 1) {
            new AllocationSitePointsToSet1(elements.head, orderedTypes.head)
        } else {
            new AllocationSitePointsToSetN(elements, types, orderedTypes)
        }
    }

    final val key: PropertyKey[AllocationSitePointsToSet] = {
        val name = "opalj.AllocationSitePointsToSet"
        PropertyKey.create(
            name,
            (_: PropertyStore, reason: FallbackReason, _: Entity) => reason match {
                case PropertyIsNotDerivedByPreviouslyExecutedAnalysis => NoAllocationSites
                case _ =>
                    throw new IllegalStateException(s"no analysis is scheduled for property: $name")
            }
        )
    }
}

case class AllocationSitePointsToSetN private[pointsto] (
        override val elements:                     LongLinkedSet,
        override val types:                        UIDSet[ReferenceType],
        override protected[this] val orderedTypes: List[ReferenceType]
) extends AllocationSitePointsToSet {

    override def numTypes: Int = types.size
    override def numElements: Int = elements.size

    override def forNewestNTypes[U](n: Int)(f: ReferenceType => U): Unit = {
        orderedTypes.take(n).map(f)
    }

    override def forNewestNElements[U](n: Int)(f: AllocationSite => U): Unit = {
        elements.forFirstN(n)(f)
    }

    // TODO: should the order matter?
    override def equals(obj: Any): Boolean = {
        obj match {
            case that: AllocationSitePointsToSetN =>
                (this eq that) || {
                    that.elements.size == this.elements.size &&
                        that.numTypes == this.numTypes &&
                        // TODO: we should assert that:
                        //  that.elements == this.elements => that.orderedTypes == this.orderedTypes
                        that.elements == this.elements
                }
            case _ => false
        }
    }

    override def hashCode: Int = elements.hashCode()

    override def getNewestElement(): AllocationSite = elements.head
}

object NoAllocationSites extends AllocationSitePointsToSet {

    override def included(other: AllocationSitePointsToSet): AllocationSitePointsToSet = {
        other
    }

    override def included(
        other: AllocationSitePointsToSet, seenElements: Int
    ): AllocationSitePointsToSet = {
        other
    }

    override def included(
        other: AllocationSitePointsToSet, typeFilter: ReferenceType => Boolean
    ): AllocationSitePointsToSet = {
        other.filter(typeFilter)
    }

    override def filter(typeFilter: ReferenceType => Boolean): AllocationSitePointsToSet = {
        this
    }

    override def numTypes: Int = 0

    override def types: UIDSet[ReferenceType] = UIDSet.empty

    override protected[this] def orderedTypes: List[ReferenceType] = List.empty

    override def numElements: Int = 0

    override def elements: LongLinkedSet = LongTrieSetWithList.empty

    override def forNewestNTypes[U](n: Int)(f: ReferenceType => U): Unit = {
        assert(n == 0)
    }

    override def forNewestNElements[U](n: Int)(f: AllocationSite => U): Unit = {
        assert(n == 0)
    }

    override def getNewestElement(): AllocationSite = throw new NoSuchElementException
}

case class AllocationSitePointsToSet1(
        allocationSite: AllocationSite, allocatedType: ReferenceType
) extends AllocationSitePointsToSet {

    override def numTypes: Int = 1

    override def types: UIDSet[ReferenceType] = UIDSet(allocatedType)

    override protected[this] def orderedTypes: List[ReferenceType] = List(allocatedType)

    override def numElements: Int = 1

    override def elements: LongLinkedSet = LongTrieSetWithList(allocationSite)

    override def included(other: AllocationSitePointsToSet): AllocationSitePointsToSet = {
        other match {
            case AllocationSitePointsToSet1(`allocationSite`, `allocatedType`) =>
                this

            case AllocationSitePointsToSet1(otherAllocationSite, otherAllocatedType) =>
                AllocationSitePointsToSet(
                    otherAllocationSite, otherAllocatedType, allocationSite, allocatedType
                )

            case NoAllocationSites =>
                this

            case AllocationSitePointsToSetN(otherAllocationSites, otherTypes, otherOrderedTypes) =>
                val newAllocations = otherAllocationSites.foldLeft(elements) { (l, as) =>
                    if (as != allocationSite) {
                        l + as
                    } else {
                        l
                    }
                }

                val newOrderedTypes = otherOrderedTypes.foldLeft(List(allocatedType)) { (l, at) =>
                    if (at != allocatedType) at :: l else l
                }

                new AllocationSitePointsToSetN(
                    newAllocations,
                    otherTypes + allocatedType,
                    newOrderedTypes
                )
            case _ =>
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

    override def included(
        other: AllocationSitePointsToSet, typeFilter: ReferenceType => Boolean
    ): AllocationSitePointsToSet = {
        if (typeFilter eq PointsToSetLike.noFilter)
            return included(other);

        var newTypes = types
        var newOrderedTypes = orderedTypes

        val newAllocationSites = other.elements.foldLeft(elements) { (r, allocationSite) =>
            val tpe = ReferenceType.lookup(allocationSiteLongToTypeId(allocationSite))
            if (typeFilter(tpe)) {
                val newAllocationSites = r + allocationSite
                if (newAllocationSites ne r) {
                    val oldTypes = newTypes
                    newTypes += tpe
                    if (newTypes ne oldTypes)
                        newOrderedTypes ::= tpe
                }
                newAllocationSites
            } else {
                r
            }
        }

        if (newAllocationSites.size == 1)
            return this;

        AllocationSitePointsToSet(newAllocationSites, newTypes, newOrderedTypes)
    }

    override def included(
        other:        AllocationSitePointsToSet,
        seenElements: Int,
        typeFilter:   ReferenceType => Boolean
    ): AllocationSitePointsToSet = {
        if (typeFilter eq PointsToSetLike.noFilter)
            return included(other, seenElements);

        var newAllocationSites = elements
        var newTypes = types
        var newOrderedTypes = orderedTypes

        other.forNewestNElements(other.numElements - seenElements) { allocationSite =>
            val tpe = ReferenceType.lookup(allocationSiteLongToTypeId(allocationSite))
            if (typeFilter(tpe)) {
                val oldAllocationSites = newAllocationSites
                newAllocationSites += allocationSite
                if (newAllocationSites ne oldAllocationSites) {
                    val oldTypes = newTypes
                    newTypes += tpe
                    if (newTypes ne oldTypes)
                        newOrderedTypes ::= tpe
                }
            }
        }

        if (newAllocationSites.size == 1)
            return this;

        AllocationSitePointsToSet(newAllocationSites, newTypes, newOrderedTypes)
    }

    override def filter(typeFilter: ReferenceType => Boolean): AllocationSitePointsToSet = {
        if ((typeFilter eq PointsToSetLike.noFilter) || typeFilter(allocatedType)) {
            this
        } else {
            NoAllocationSites
        }
    }

    override def forNewestNTypes[U](n: Int)(f: ReferenceType => U): Unit = {
        assert(n == 0 || n == 1)
        if (n == 1)
            f(allocatedType)
    }

    override def forNewestNElements[U](n: Int)(f: AllocationSite => U): Unit = {
        assert(n == 0 || n == 1)
        if (n == 1)
            f(allocationSite)
    }

    override def equals(obj: Any): Boolean = {
        obj match {
            case that: AllocationSitePointsToSet1 =>
                that.allocationSite == this.allocationSite

            case _ => false
        }
    }

    override def hashCode: Int = (allocationSite ^ (allocationSite >> 32)).toInt

    override def getNewestElement(): AllocationSite = allocationSite
}