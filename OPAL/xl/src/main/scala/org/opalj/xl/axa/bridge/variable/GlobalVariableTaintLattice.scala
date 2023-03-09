/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.xl.axa.bridge.variable

import org.opalj.fpcf.Entity
import org.opalj.fpcf.FallbackReason
import org.opalj.fpcf.OrderedProperty
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation
import org.opalj.fpcf.PropertyStore

sealed trait UniversalVariableTaintsPropertyMetaInformation extends PropertyMetaInformation {
    final type Self = GlobalVariableTaintLattice
}

sealed trait GlobalVariableTaintLattice extends UniversalVariableTaintsPropertyMetaInformation with OrderedProperty {
    def meet(other: GlobalVariableTaintLattice): GlobalVariableTaintLattice = {
        (this, other) match {
            case (UniVUntainted, _)                  => other
            case (_, UniVUntainted)                  => this
            case (UniVTainted, _) | (_, UniVTainted) => UniVTainted
        }
    }

    def checkIsEqualOrBetterThan(e: Entity, other: GlobalVariableTaintLattice): Unit = {
        if (meet(other) != other) {
            throw new IllegalArgumentException(s"$e: impossible refinement: $other => $this")
        }
    }

    final def key: PropertyKey[GlobalVariableTaintLattice] = GlobalVariableTaintLattice.key

}

object GlobalVariableTaintLattice extends UniversalVariableTaintsPropertyMetaInformation {
    final val key: PropertyKey[GlobalVariableTaintLattice] = PropertyKey.create(
        "ForeignVariableTaintFact",
        (_: PropertyStore, _: FallbackReason, e: Entity) => {
            e match {
                case e: Entity => UniVTainted
                case x =>
                    throw new IllegalArgumentException(s"$x is not a Field")
            }
        }
    )
}

case object UniVTainted extends GlobalVariableTaintLattice

case object UniVUntainted extends GlobalVariableTaintLattice
