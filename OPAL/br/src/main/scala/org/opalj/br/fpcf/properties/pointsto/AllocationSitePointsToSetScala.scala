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
    override val types:    UIDSet[ReferenceType] // TODO: Use normal Set here
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
        AllocationSitePointsToSetScala(elements ++ other.elements, types ++ other.types)
    }

    override def included(
        other: AllocationSitePointsToSetScala, seenElements: Int
    ): AllocationSitePointsToSetScala = {
        included(other)
    }

    override def included(
        other: AllocationSitePointsToSetScala, typeFilter: ReferenceType ⇒ Boolean
    ): AllocationSitePointsToSetScala = {
        val newTypes = types ++ other.types.filter(typeFilter)
        val newElements = elements ++ other.elements.filter(as ⇒ newTypes.containsId(allocationSiteLongToTypeId(as)))
        AllocationSitePointsToSetScala(newElements, newTypes)
    }

    override def included(
        other:        AllocationSitePointsToSetScala,
        seenElements: Int,
        typeFilter:   ReferenceType ⇒ Boolean
    ): AllocationSitePointsToSetScala = {
        included(other, typeFilter)
    }

    override def filter(typeFilter: ReferenceType ⇒ Boolean): AllocationSitePointsToSetScala = {
        val filteredTypes = types.filter(typeFilter)
        val filteredElements = elements.filter(as ⇒ filteredTypes.containsId(allocationSiteLongToTypeId(as)))
        AllocationSitePointsToSetScala(filteredElements, filteredTypes)
    }

    override def forNewestNTypes[U](n: Int)(f: ReferenceType ⇒ U): Unit = types.foreach(f)

    override def numTypes: Int = types.size

    override def numElements: Int = elements.size

    override def forNewestNElements[U](n: Int)(f: AllocationSite ⇒ U): Unit = elements.foreach(f)

    override def getNewestElement(): AllocationSite = {
        if (numElements == 1)
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

object NoAllocationSitesScala extends AllocationSitePointsToSetScala(Set.empty, UIDSet.empty)

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
