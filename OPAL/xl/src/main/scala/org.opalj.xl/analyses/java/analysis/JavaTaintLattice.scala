/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.xl.analyses.java.analysis

import org.opalj.fpcf.Entity
import org.opalj.fpcf.FallbackReason
import org.opalj.fpcf.OrderedProperty
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation
import org.opalj.fpcf.PropertyStore

sealed trait JavaTaintPropertyMetaInformation extends PropertyMetaInformation {
  final type Self = JavaTaintLattice
}

sealed trait JavaTaintLattice extends JavaTaintPropertyMetaInformation with OrderedProperty {
  def meet(other: JavaTaintLattice): JavaTaintLattice = {
    (this, other) match {
      case (JavaUntainted, _)       => other
      case (_, JavaUntainted)       => this
      case (JavaTainted, _) | (_, JavaTainted) => JavaTainted
    }
  }

   def checkIsEqualOrBetterThan(e: Entity, other: JavaTaintLattice): Unit = {
    if (meet(other) != other) {
      throw new IllegalArgumentException(s"$e: impossible refinement: $other => $this")
    }
  }
  final def key: PropertyKey[JavaTaintLattice] = JavaTaintLattice.key
}

object JavaTaintLattice extends JavaTaintPropertyMetaInformation {
  final val key: PropertyKey[JavaTaintLattice] = PropertyKey.create(
    "A2TaintFact",
    (_: PropertyStore, _: FallbackReason, e: Entity) => {
      e match {
        case e: Entity => JavaTainted
        //  case f: Field =>
        //    if (f.isFinal) NonTransitivelyImmutableField else MutableField
        case x =>
          throw new IllegalArgumentException(s"$x is not a Field")
      }
    }
  )
}

case object JavaTainted extends JavaTaintLattice

case object JavaUntainted extends JavaTaintLattice
