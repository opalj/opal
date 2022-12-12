/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.xl.analyses.a0

import org.opalj.fpcf.Entity
import org.opalj.fpcf.FallbackReason
import org.opalj.fpcf.OrderedProperty
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation
import org.opalj.fpcf.PropertyStore

sealed trait A0TaintPropertyMetaInformation extends PropertyMetaInformation {
  final type Self = A0TaintLattice
}

sealed trait A0TaintLattice extends A0TaintPropertyMetaInformation with OrderedProperty {
  def meet(other: A0TaintLattice): A0TaintLattice = {
    (this, other) match {
      case (A0Untainted, _)       => other
      case (_, A0Untainted)       => this
      case (A0Tainted, _) | (_, A0Tainted) => A0Tainted
    }
  }

   def checkIsEqualOrBetterThan(e: Entity, other: A0TaintLattice): Unit = {
    if (meet(other) != other) {
      throw new IllegalArgumentException(s"$e: impossible refinement: $other => $this")
    }
  }

  final def key: PropertyKey[A0TaintLattice] = A0TaintLattice.key
}

object A0TaintLattice extends A0TaintPropertyMetaInformation {
  final val key: PropertyKey[A0TaintLattice] = PropertyKey.create(
    "A0TaintFact",
    (_: PropertyStore, _: FallbackReason, e: Entity) => {
      e match {
        case e: Entity => A0Tainted
        //  case f: Field =>
        //    if (f.isFinal) NonTransitivelyImmutableField else MutableField
        case x =>
          throw new IllegalArgumentException(s"$x is not a Field")
      }
    }
  )
}

case object A0Tainted extends A0TaintLattice

case object A0Untainted extends A0TaintLattice
