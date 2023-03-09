/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package xl
package analyses
package python
package analyses
/*
import org.opalj.fpcf.Entity
import org.opalj.fpcf.FallbackReason
import org.opalj.fpcf.OrderedProperty
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation
import org.opalj.fpcf.PropertyStore

sealed trait TIPTaintPropertyMetaInformation extends PropertyMetaInformation {
    final type Self = TIPTaintLattice
}

sealed trait TIPTaintLattice extends TIPTaintPropertyMetaInformation with OrderedProperty {
    def meet(other: TIPTaintLattice): TIPTaintLattice = {
        (this, other) match {
            case (TIPUntainted, _)                 => other
            case (_, TIPUntainted)                 => this
            case (TIPTainted, _) | (_, TIPTainted) => TIPTainted
        }
    }

    def checkIsEqualOrBetterThan(e: Entity, other: TIPTaintLattice): Unit = {
        if (meet(other) != other) {
            throw new IllegalArgumentException(s"$e: impossible refinement: $other => $this")
        }
    }

    final def key: PropertyKey[TIPTaintLattice] = TIPTaintLattice.key
}

object TIPTaintLattice extends TIPTaintPropertyMetaInformation {
    final val key: PropertyKey[TIPTaintLattice] = PropertyKey.create(
        "TIPTaintFact",
        (_: PropertyStore, _: FallbackReason, e: Entity) => {
            e match {
                case e: Entity => TIPTainted
                //  case f: Field =>
                //    if (f.isFinal) NonTransitivelyImmutableField else MutableField
                case x =>
                    throw new IllegalArgumentException(s"$x is not a Field")
            }
        }
    )
}

case object TIPTainted extends TIPTaintLattice

case object TIPUntainted extends TIPTaintLattice
*/ 