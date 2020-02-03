/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package pointsto

import org.opalj.collection.immutable.UIDSet
import org.opalj.fpcf.PropertyKey
import org.opalj.br.ReferenceType
import org.opalj.br.fpcf.properties.pointsto.NoTypes
import org.opalj.br.fpcf.properties.pointsto.TypeBasedPointsToSet
import org.opalj.br.DeclaredMethod

/**
 * Mix-in trait for points-to analyses using typesets ([[TypeBasedPointsToSet]]) as points-to sets.
 *
 * @author Dominik Helm
 */
trait TypeBasedAnalysis extends AbstractPointsToBasedAnalysis {

    override protected[this] type ElementType = ReferenceType
    override protected[this] type PointsToSet = TypeBasedPointsToSet

    override protected[this] val pointsToPropertyKey: PropertyKey[TypeBasedPointsToSet] = {
        TypeBasedPointsToSet.key
    }

    override protected[this] def emptyPointsToSet: TypeBasedPointsToSet = NoTypes

    override protected[this] def createPointsToSet(
        pc:             Int,
        declaredMethod: DeclaredMethod,
        allocatedType:  ReferenceType,
        isConstant:     Boolean,
        isEmptyArray:   Boolean        = false
    ): TypeBasedPointsToSet = TypeBasedPointsToSet(UIDSet(allocatedType))

    override protected[this] def getTypeOf(element: ReferenceType): ReferenceType = element
}
