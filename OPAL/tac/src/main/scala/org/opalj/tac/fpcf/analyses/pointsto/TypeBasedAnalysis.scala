/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package pointsto

import org.opalj.br.ReferenceType
import org.opalj.br.fpcf.properties.pointsto.NoTypes
import org.opalj.br.fpcf.properties.pointsto.TypeBasedPointsToSet
import org.opalj.collection.immutable.UIDSet
import org.opalj.fpcf.PropertyKey

/**
 * Mix-in trait for points-to analyses using typesets ([[TypeBasedPointsToSet]]) as points-to sets.
 *
 * @author Dominik Helm
 */
trait TypeBasedAnalysis extends AbstractPointsToBasedAnalysis {

    override protected type ElementType = ReferenceType
    override protected type PointsToSet = TypeBasedPointsToSet

    override protected val pointsToPropertyKey: PropertyKey[TypeBasedPointsToSet] = {
        TypeBasedPointsToSet.key
    }

    override protected def emptyPointsToSet: TypeBasedPointsToSet = NoTypes

    override protected def createPointsToSet(
        pc:            Int,
        callContext:   ContextType,
        allocatedType: ReferenceType,
        isConstant:    Boolean,
        isEmptyArray:  Boolean = false
    ): TypeBasedPointsToSet = TypeBasedPointsToSet(UIDSet(allocatedType))

    @inline protected def getTypeOf(element: ReferenceType): ReferenceType = element

    @inline protected def getTypeIdOf(element: ReferenceType): Int = element.id

    @inline protected def isEmptyArray(element: ReferenceType): Boolean = false
}
