/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package xl
package utility

import org.opalj.fpcf.Entity
import org.opalj.fpcf.FallbackReason
import org.opalj.fpcf.OrderedProperty
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation
import org.opalj.fpcf.PropertyStore

sealed trait AnalysisResultPropertyMetaInformation extends PropertyMetaInformation {
    final type Self = AnalysisResult
}

sealed trait AnalysisResult extends AnalysisResultPropertyMetaInformation with OrderedProperty {
    def meet(other: AnalysisResult): AnalysisResult = {
        (this, other) match {
            case (_, _) => InterimAnalysisResult(null)
        }
    }

    def checkIsEqualOrBetterThan(e: Entity, other: AnalysisResult): Unit = {
        if (meet(other) != other) {
            throw new IllegalArgumentException(s"$e: impossible refinement: $other => $this")
        }
    }

    final def key: PropertyKey[AnalysisResult] = AnalysisResult.key
}

object AnalysisResult extends AnalysisResultPropertyMetaInformation {
    final val key: PropertyKey[AnalysisResult] = PropertyKey.create(
        "AnalysisResultLattice",
        (_: PropertyStore, _: FallbackReason, e: Entity) => {
            e match {
                case e: Entity => InterimAnalysisResult(null)
                case x =>
                    throw new IllegalArgumentException(s"$x is not a Field")
            }
        }
    )
}

case class FinalAnalysisResult(o: Object) extends AnalysisResult

case object NoAnalysisResult extends AnalysisResult

case class InterimAnalysisResult(o: Object) extends AnalysisResult
