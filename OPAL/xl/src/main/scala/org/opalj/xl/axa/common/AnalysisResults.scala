/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package xl
package axa
package common

import org.opalj.fpcf.Entity
import org.opalj.fpcf.FallbackReason
import org.opalj.fpcf.OrderedProperty
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation
import org.opalj.fpcf.PropertyStore

sealed trait AnalysisResultsPropertyMetaInformation extends PropertyMetaInformation {
    final type Self = AnalysisResults
}

sealed trait AnalysisResults extends AnalysisResultsPropertyMetaInformation with OrderedProperty {
    def meet(other: AnalysisResults): AnalysisResults = {
        (this, other) match {
            case (_, _) => InterimAnalysisResult(null)
        }
    }

    def checkIsEqualOrBetterThan(e: Entity, other: AnalysisResults): Unit = {
        if (meet(other) != other) {
            throw new IllegalArgumentException(s"$e: impossible refinement: $other => $this")
        }
    }

    final def key: PropertyKey[AnalysisResults] = AnalysisResults.key
}

object AnalysisResults extends AnalysisResultsPropertyMetaInformation {
    final val key: PropertyKey[AnalysisResults] = PropertyKey.create(
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

case class FinalAnalysisResult(o: Object) extends AnalysisResults

case object NoAnalysisResult extends AnalysisResults

case class InterimAnalysisResult(o: Object) extends AnalysisResults
