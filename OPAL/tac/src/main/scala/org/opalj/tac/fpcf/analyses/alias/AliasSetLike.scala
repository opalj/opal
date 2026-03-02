/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package alias

import scala.util.boundary
import scala.util.boundary.break

import org.opalj.br.ReferenceType

/**
 * A base trait for alias sets that store the elements that an [[org.opalj.br.fpcf.properties.alias.AliasSourceElement]] can point to.
 * It is possible to denote that the set can point to any arbitrary element if an analysis cannot limit the points-to set.
 * This is handled by the [[pointsToAny]] and [[setPointsToAny()]] method.
 *
 * @tparam ElementType The type of the elements that can be stored in the set.
 * @tparam T The concrete type of the alias set.
 */
trait AliasSetLike[ElementType, T <: AliasSetLike[ElementType, T]] {

    private var _pointsToAny: Boolean = false

    private var _pointsTo: Set[ElementType] = Set.empty[ElementType]

    /**
     * Add the given element to the set of elements the associated [[org.opalj.br.fpcf.properties.alias.AliasSourceElement]] can point to.
     *
     * @param pointsTo The element to add to the set.
     */
    def addPointsTo(pointsTo: ElementType): Unit = _pointsTo += pointsTo

    /**
     * Checks if the set contains the given element.
     *
     * @param element The element to check for.
     * @return `true` if the set contains the element, `false` otherwise.
     */
    def pointsTo(element: ElementType): Boolean = allPointsTo.contains(element)

    /**
     * Marks that this set can point to any arbitrary element.
     */
    def setPointsToAny(): Unit = _pointsToAny = true

    /**
     * Checks if this set can point to any arbitrary element.
     *
     * @return `true` if the set can point to any arbitrary element, `false` otherwise.
     */
    def pointsToAny: Boolean = _pointsToAny

    /**
     * Checks if the set is empty, i.e., it does not contain any elements it can point to and also cannot point to any
     * arbitrary element.
     *
     * @return `true` if the set is empty, `false` otherwise.
     */
    def isEmpty: Boolean = allPointsTo.isEmpty && !pointsToAny

    /**
     * Returns the number of elements the set can point to. Note that the size being 0 does not imply that the
     * [[isEmpty]] method returns `true`, as the set can still point to any arbitrary element.
     *
     * @return The number of elements the set can point to.
     */
    def size: Int = allPointsTo.size

    /**
     * Returns all elements this set can point to. Note that the returned set being empty does not imply that the
     * [[isEmpty]] method returns `true`, as this set can still point to any arbitrary element.
     *
     * @return The set of elements the set can point to.
     */
    def allPointsTo: Set[ElementType] = _pointsTo

    /**
     * Tries to find and return two elements that are in both this set and the given other set as a tuple of options.
     * If only one element is in both sets, the second element is `None`. If the sets are disjoint, both elements
     * are `None`.
     *
     * This is used instead of computing the whole intersection because more than two elements in the intersection
     * would not change the result of the alias analysis.
     *
     * Note that this does not check if one of the sets can point to any arbitrary element.
     *
     * @param other The other set to check for intersections.
     * @return A tuple containing two elements that are in both this set and the other set.
     */
    def findTwoIntersections(other: T): (Option[ElementType], Option[ElementType]) = boundary {

        var firstIntersection: Option[ElementType] = None

        // optimized version of allPointsTo.intersect(other.allPointsTo)
        allPointsTo.foreach(element => {
            if (other.pointsTo(element)) {
                if (firstIntersection.isEmpty) {
                    firstIntersection = Some(element)
                } else {
                    break((firstIntersection, Some(element)));
                }
            }
        })

        (firstIntersection, None)
    }
}

/**
 * Implementation of an [[AliasSetLike]] that is based on allocation sites, i.e., it stores elements of the type [[AllocationSite]].
 */
class AllocationSiteBasedAliasSet extends AliasSetLike[AllocationSite, AllocationSiteBasedAliasSet] {}

/**
 * Implementation of an [[AliasSetLike]] that is based on types, i.e., it stores elements of the type [[ReferenceType]].
 */
class TypeBasedAliasSet extends AliasSetLike[ReferenceType, TypeBasedAliasSet] {}
