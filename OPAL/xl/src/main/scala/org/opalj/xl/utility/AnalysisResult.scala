/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package xl
package utility

import org.opalj.fpcf.Entity
import org.opalj.fpcf.FallbackReason
import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation
import org.opalj.fpcf.PropertyStore

sealed trait AnalysisResultPropertyMetaInformation extends PropertyMetaInformation {
    final type Self = AnalysisResult
}

sealed trait AnalysisResult extends AnalysisResultPropertyMetaInformation with Property {

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

case object Bottom extends AnalysisResult
case class FinalAnalysisResult[Key, Value](store: Map[Key, Value]) extends AnalysisResult

case class InterimAnalysisResult[Key, Value](store: Map[Key, Value]) extends AnalysisResult

case object NoAnalysisResult extends AnalysisResult