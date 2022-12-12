/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.xl.analyses.a2

import org.opalj.fpcf.Entity
import org.opalj.fpcf.FallbackReason
import org.opalj.fpcf.OrderedProperty
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation
import org.opalj.fpcf.PropertyStore

sealed trait A2TaintPropertyMetaInformation extends PropertyMetaInformation {
  final type Self = A2TaintLattice
}

sealed trait A2TaintLattice extends A2TaintPropertyMetaInformation with OrderedProperty {
  def meet(other: A2TaintLattice): A2TaintLattice = {
    (this, other) match {
      case (A2Untainted, _)       => other
      case (_, A2Untainted)       => this
      case (A2Tainted, _) | (_, A2Tainted) => A2Tainted
    }
  }

   def checkIsEqualOrBetterThan(e: Entity, other: A2TaintLattice): Unit = {
    if (meet(other) != other) {
      throw new IllegalArgumentException(s"$e: impossible refinement: $other => $this")
    }
  }

  final def key: PropertyKey[A2TaintLattice] = A2TaintLattice.key
}

object A2TaintLattice extends A2TaintPropertyMetaInformation {
  final val key: PropertyKey[A2TaintLattice] = PropertyKey.create(
    "A2TaintFact",
    (_: PropertyStore, _: FallbackReason, e: Entity) => {
      e match {
        case e: Entity => A2Tainted
        //  case f: Field =>
        //    if (f.isFinal) NonTransitivelyImmutableField else MutableField
        case x =>
          throw new IllegalArgumentException(s"$x is not a Field")
      }
    }
  )
}

case object A2Tainted extends A2TaintLattice

case object A2Untainted extends A2TaintLattice
