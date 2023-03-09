/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package xl
package analyses
package javascript
package analyses

import org.opalj.fpcf.Entity
import org.opalj.fpcf.FallbackReason
import org.opalj.fpcf.OrderedProperty
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation
import org.opalj.fpcf.PropertyStore

sealed trait JavaScriptPointsToLatticePropertyMetaInformation extends PropertyMetaInformation {
    final type Self = JavaScriptPointsToLattice
}

sealed trait JavaScriptPointsToLattice extends JavaScriptPointsToLatticePropertyMetaInformation with OrderedProperty {
    def meet(other: JavaScriptPointsToLattice): JavaScriptPointsToLattice = {
        (this, other) match {
            case (_, _)                 => NoPointsTo
         /*   case (_, TIPUntainted)                 => this
            case (TIPTainted, _) | (_, TIPTainted) => TIPTainted */
        }
    }

    def checkIsEqualOrBetterThan(e: Entity, other: JavaScriptPointsToLattice): Unit = {
        if (meet(other) != other) {
            throw new IllegalArgumentException(s"$e: impossible refinement: $other => $this")
        }
    }

    final def key: PropertyKey[JavaScriptPointsToLattice] = TIPTaintLattice.key
}

object TIPTaintLattice extends JavaScriptPointsToLatticePropertyMetaInformation {
    final val key: PropertyKey[JavaScriptPointsToLattice] = PropertyKey.create(
        "JavaScriptPointsToLattice",
        (_: PropertyStore, _: FallbackReason, e: Entity) => {
            e match {
                case e: Entity => NoPointsTo
                case x =>
                    throw new IllegalArgumentException(s"$x is not a Field")
            }
        }
    )
}

case class PointsTo(pointsTo:Set[Any]) extends JavaScriptPointsToLattice

case object NoPointsTo extends JavaScriptPointsToLattice
