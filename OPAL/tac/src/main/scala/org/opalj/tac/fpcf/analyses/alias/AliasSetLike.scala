/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.alias

import scala.collection.mutable

/**
 * A base trait for alias sets that store the elements that an [[org.opalj.br.fpcf.properties.alias.AliasSourceElement]] can point to.
 * It is possible to denote that the set can point to any arbitrary element if an analysis cannot limit the points-to set.
 * This is handled by the [[pointsToAny]] and [[setPointsToAny()]] method.
 *
 * @tparam ElementType The type of the elements that can be stored in the set.
 * @tparam T The concrete type of the alias set.
 */
trait AliasSetLike[ElementType, T <: AliasSetLike[ElementType, T]] {

    protected var _pointsToAny: Boolean = false

    def addPointsTo(pointsTo: ElementType): Unit = allPointsTo.add(pointsTo)

    def pointsTo(element: ElementType): Boolean = allPointsTo.contains(element)

    def setPointsToAny(): Unit = _pointsToAny = true

    def pointsToAny: Boolean = _pointsToAny

    def isEmpty: Boolean = allPointsTo.isEmpty && !_pointsToAny

    def size: Int = allPointsTo.size

    def allPointsTo: mutable.Set[ElementType]

    def intersection(other: T): T

}
