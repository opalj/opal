/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package pointsto

import org.opalj.fpcf.PropertyKey
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.properties.pointsto.PointsToSetLike

// TODO remove this
trait AbstractPointsToBasedAnalysis[Depender, PointsToSet <: PointsToSetLike[_, _, _]] extends FPCFAnalysis {
    protected[this] val pointsToPropertyKey: PropertyKey[PointsToSet]
    protected[this] def emptyPointsToSet: PointsToSet
}
