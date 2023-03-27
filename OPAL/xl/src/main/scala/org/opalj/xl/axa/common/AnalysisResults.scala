/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.xl.axa.common

import org.opalj.fpcf.Entity
import org.opalj.fpcf.FallbackReason
import org.opalj.fpcf.OrderedProperty
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation
import org.opalj.fpcf.PropertyStore

sealed trait AnalysisResultsPropertyMetaInformation extends PropertyMetaInformation {
    final type Self = AnalysisResultsLattice
}

sealed trait AnalysisResultsLattice extends AnalysisResultsPropertyMetaInformation with OrderedProperty {
    def meet(other: AnalysisResultsLattice): AnalysisResultsLattice = {
        (this, other) match {
            case (_, _)                 => InterimAnalysisResult(null)
        }
    }

    def checkIsEqualOrBetterThan(e: Entity, other: AnalysisResultsLattice): Unit = {
        if (meet(other) != other) {
            throw new IllegalArgumentException(s"$e: impossible refinement: $other => $this")
        }
    }

    final def key: PropertyKey[AnalysisResultsLattice] = AnalysisResultsLattice.key
}

object AnalysisResultsLattice extends AnalysisResultsPropertyMetaInformation {
    final val key: PropertyKey[AnalysisResultsLattice] = PropertyKey.create(
        "AnalysisResultsLattice",
        (_: PropertyStore, _: FallbackReason, e: Entity) => {
            e match {
                case e: Entity => InterimAnalysisResult(null)
                case x =>
                    throw new IllegalArgumentException(s"$x is not a Field")
            }
        }
    )
}

case class FinalAnalysisResult(o:Object) extends AnalysisResultsLattice

case object NoAnalysisResult extends AnalysisResultsLattice

case class InterimAnalysisResult(o:Object) extends AnalysisResultsLattice
