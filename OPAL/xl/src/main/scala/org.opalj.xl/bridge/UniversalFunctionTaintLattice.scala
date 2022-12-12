/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.xl.bridge

import org.opalj.fpcf.Entity
import org.opalj.fpcf.FallbackReason
import org.opalj.fpcf.OrderedProperty
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation
import org.opalj.fpcf.PropertyStore

sealed trait UniversalFunctionTaintsPropertyMetaInformation extends PropertyMetaInformation {
  final type Self = UniversalFunctionTaintLattice
}

sealed trait UniversalFunctionTaintLattice extends UniversalFunctionTaintsPropertyMetaInformation with OrderedProperty {
  def meet(other: UniversalFunctionTaintLattice): UniversalFunctionTaintLattice = {
    (this, other) match {
      case (UniFUntainted, _)       => other
      case (_, UniFUntainted)       => this
      case (UniFTainted, _) | (_, UniFTainted) => UniFTainted
    }
  }

   def checkIsEqualOrBetterThan(e: Entity, other: UniversalFunctionTaintLattice): Unit = {
    if (meet(other) != other) {
      throw new IllegalArgumentException(s"$e: impossible refinement: $other => $this")
    }
  }

  final def key: PropertyKey[UniversalFunctionTaintLattice] = UniversalFunctionTaintLattice.key
}

object UniversalFunctionTaintLattice extends UniversalFunctionTaintsPropertyMetaInformation {
  final val key: PropertyKey[UniversalFunctionTaintLattice] = PropertyKey.create(
    "UniversalFunctionTaintFact",
    (_: PropertyStore, _: FallbackReason, e: Entity) => {
      e match {
        case e: Entity => UniFTainted
        //  case f: Field =>
        //    if (f.isFinal) NonTransitivelyImmutableField else MutableField
        case x =>
          throw new IllegalArgumentException(s"$x is not a Field")
      }
    }
  )
}

case object UniFTainted extends UniversalFunctionTaintLattice

case object UniFUntainted extends UniversalFunctionTaintLattice
