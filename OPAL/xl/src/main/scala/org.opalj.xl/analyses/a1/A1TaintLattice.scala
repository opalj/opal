/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.xl.analyses.a1

import org.opalj.fpcf.Entity
import org.opalj.fpcf.FallbackReason
import org.opalj.fpcf.OrderedProperty
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation
import org.opalj.fpcf.PropertyStore

sealed trait A1TaintPropertyMetaInformation extends PropertyMetaInformation {
  final type Self = A1TaintLattice
}

sealed trait A1TaintLattice extends A1TaintPropertyMetaInformation with OrderedProperty {
  def meet(other: A1TaintLattice): A1TaintLattice = {
    (this, other) match {
      case (A1Untainted, _)       => other
      case (_, A1Untainted)       => this
      case (A1Tainted, _) | (_, A1Tainted) => A1Tainted
    }
  }

   def checkIsEqualOrBetterThan(e: Entity, other: A1TaintLattice): Unit = {
    if (meet(other) != other) {
      throw new IllegalArgumentException(s"$e: impossible refinement: $other => $this")
    }
  }

  final def key: PropertyKey[A1TaintLattice] = A1TaintLattice.key
}

object A1TaintLattice extends A1TaintPropertyMetaInformation {
  final val key: PropertyKey[A1TaintLattice] = PropertyKey.create(
    "A1TaintFact",
    (_: PropertyStore, _: FallbackReason, e: Entity) => {
      e match {
        case e: Entity => A1Tainted
        //  case f: Field =>
        //    if (f.isFinal) NonTransitivelyImmutableField else MutableField
        case x =>
          throw new IllegalArgumentException(s"$x is not a Field")
      }
    }
  )
}

case object A1Tainted extends A1TaintLattice

case object A1Untainted extends A1TaintLattice
