/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package properties
package pointsto

import org.opalj.collection.immutable.IntTrieSet
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
sealed trait AllocationSitePointsToSetScalaPropertyMetaInformation extends PropertyMetaInformation {

    final type Self = AllocationSitePointsToSetScala
}

case class AllocationSitePointsToSetScala(
        override val elements: Set[AllocationSite],
        override val types:    Set[ReferenceType], // TODO: Use normal Set here
        val orderedTypes:      List[ReferenceType]
) extends PointsToSetLike[AllocationSite, Set[AllocationSite], AllocationSitePointsToSetScala]
    with OrderedProperty
    with AllocationSitePointsToSetScalaPropertyMetaInformation {

    final def key: PropertyKey[AllocationSitePointsToSetScala] = AllocationSitePointsToSetScala.key

    override def toString: String = s"PointsTo(size=${elements.size})"

    override def checkIsEqualOrBetterThan(e: Entity, other: AllocationSitePointsToSetScala): Unit = {
        if (!types.subsetOf(other.types)) {
            throw new IllegalArgumentException(s"$e: illegal refinement of property $other to $this")
        }
    }

    override def included(other: AllocationSitePointsToSetScala): AllocationSitePointsToSetScala = {
        included(other, 0)
    }

    override def included(
        other: AllocationSitePointsToSetScala, seenElements: Int
    ): AllocationSitePointsToSetScala = {
        var newAllocationSites = elements
        var newTypes = types
        var newOrderedTypes = orderedTypes

        other.forNewestNElements(other.numElements - seenElements) { allocationSite ⇒
            val tpe = ReferenceType.lookup(allocationSiteLongToTypeId(allocationSite))
            val oldAllocationSites = newAllocationSites
            newAllocationSites += allocationSite
            if (newAllocationSites.size != oldAllocationSites.size) {
                val oldTypes = newTypes
                newTypes += tpe
                if (newTypes.size != oldTypes.size)
                    newOrderedTypes ::= tpe
            }
        }

        AllocationSitePointsToSetScala(newAllocationSites, newTypes, newOrderedTypes)
    }

    override def included(
        other: AllocationSitePointsToSetScala, typeFilter: ReferenceType ⇒ Boolean
    ): AllocationSitePointsToSetScala = {
        if (typeFilter eq PointsToSetLike.noFilter)
            return included(other);

        var newTypes = types
        var newOrderedTypes = orderedTypes

        val newAllocationSites = other.elements.foldLeft(elements) { (r, allocationSite) ⇒
            val tpe = ReferenceType.lookup(allocationSiteLongToTypeId(allocationSite))
            if (typeFilter(tpe)) {
                val newAllocationSites = r + allocationSite
                if (newAllocationSites.size != r.size) {
                    val oldTypes = newTypes
                    newTypes += tpe
                    if (newTypes.size != oldTypes.size)
                        newOrderedTypes ::= tpe
                }
                newAllocationSites
            } else {
                r
            }
        }

        AllocationSitePointsToSetScala(newAllocationSites, newTypes, newOrderedTypes)
    }

    override def included(
        other:        AllocationSitePointsToSetScala,
        seenElements: Int,
        typeFilter:   ReferenceType ⇒ Boolean
    ): AllocationSitePointsToSetScala = {
        if (typeFilter eq PointsToSetLike.noFilter)
            return included(other, seenElements);

        var newAllocationSites = elements
        var newTypes = types
        var newOrderedTypes = orderedTypes

        other.forNewestNElements(other.numElements - seenElements) { allocationSite ⇒
            val tpe = ReferenceType.lookup(allocationSiteLongToTypeId(allocationSite))
            if (typeFilter(tpe)) {
                val oldAllocationSites = newAllocationSites
                newAllocationSites += allocationSite
                if (newAllocationSites.size != oldAllocationSites.size) {
                    val oldTypes = newTypes
                    newTypes += tpe
                    if (newTypes.size != oldTypes.size)
                        newOrderedTypes ::= tpe
                }
            }
        }

        AllocationSitePointsToSetScala(newAllocationSites, newTypes, newOrderedTypes)
    }

    override def filter(typeFilter: ReferenceType ⇒ Boolean): AllocationSitePointsToSetScala = {
        if (typeFilter eq PointsToSetLike.noFilter)
            return this;

        var newTypes = UIDSet.empty[ReferenceType]
        var newOrderedTypes = List.empty[ReferenceType]

        val newAllocationSites =
            elements.foldLeft(Set.empty[AllocationSite]) { (r, allocationSite) ⇒
                val tpe = ReferenceType.lookup(allocationSiteLongToTypeId(allocationSite))
                if (typeFilter(tpe)) {
                    val newAllocationSites = r + allocationSite
                    if (newAllocationSites.size != r.size) {
                        val oldTypes = newTypes
                        newTypes += tpe
                        if (newTypes.size != oldTypes.size)
                            newOrderedTypes ::= tpe
                    }
                    newAllocationSites
                } else {
                    r
                }
            }

        if (newAllocationSites.size == elements.size)
            return this;

        AllocationSitePointsToSetScala(newAllocationSites, newTypes, newOrderedTypes)
    }

    override def forNewestNTypes[U](n: Int)(f: ReferenceType ⇒ U): Unit = orderedTypes.iterator.take(n).foreach(f)

    override def numTypes: Int = types.size

    override def numElements: Int = elements.size

    override def forNewestNElements[U](n: Int)(f: AllocationSite ⇒ U): Unit = elements.foreach(f)

    override def getNewestElement(): AllocationSite = {
        if (elements.size == 1)
            elements.head
        else
            throw new NoSuchElementException
    }

    assert {
        var asTypes = IntTrieSet.empty
        elements.foreach { allocationSite ⇒
            asTypes += allocationSiteLongToTypeId(allocationSite)
        }

        val typeIds = types.foldLeft(IntTrieSet.empty) { (r, t) ⇒ r + t.id }
        typeIds == asTypes
    }
    assert(numElements >= numTypes)
}

object NoAllocationSitesScala extends AllocationSitePointsToSetScala(Set.empty, Set.empty, Nil)

object AllocationSitePointsToSetScala extends AllocationSitePointsToSetScalaPropertyMetaInformation {

    val noFilter = { t: ReferenceType ⇒ true }

    final val key: PropertyKey[AllocationSitePointsToSetScala] = {
        val name = "opalj.AllocationSitePointsToSetScala"
        PropertyKey.create(
            name,
            (_: PropertyStore, reason: FallbackReason, _: Entity) ⇒ reason match {
                case PropertyIsNotDerivedByPreviouslyExecutedAnalysis ⇒ NoAllocationSitesScala
                case _ ⇒
                    throw new IllegalStateException(s"no analysis is scheduled for property: $name")
            }
        )
    }
}
