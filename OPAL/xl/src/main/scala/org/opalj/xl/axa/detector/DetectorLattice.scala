/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package xl
package axa
package detector

import org.opalj.br.FieldType
import org.opalj.br.Method
import org.opalj.fpcf.Entity
import org.opalj.fpcf.FallbackReason
import org.opalj.fpcf.OrderedProperty
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation
import org.opalj.fpcf.PropertyStore
import org.opalj.tac.common.DefinitionSite
import org.opalj.xl.axa.common.Language.Language

import scala.collection.mutable

sealed trait DetectorPropertyMetaInformation extends PropertyMetaInformation {
    final type Self = DetectorLattice
}

sealed trait DetectorLattice extends DetectorPropertyMetaInformation with OrderedProperty {
    def meet(other: DetectorLattice): DetectorLattice = {
        (this, other) match {
            case (_, _) => NoCrossLanguageCall
        }
    }

    def checkIsEqualOrBetterThan(e: Entity, other: DetectorLattice): Unit = {
        if (meet(other) != other) {
            throw new IllegalArgumentException(s"$e: impossible refinement: $other => $this")
        }
    }
    final def key: PropertyKey[DetectorLattice] = DetectorLattice.key
}

object DetectorLattice extends DetectorPropertyMetaInformation {
    final val key: PropertyKey[DetectorLattice] = PropertyKey.create(
        "DetectorLattice",
        (_: PropertyStore, _: FallbackReason, e: Entity) => {
            e match {
                case Method => NoCrossLanguageCall
                case x      => throw new IllegalArgumentException(s"$x is not a method")
            }
        }
    )
}

case object NoCrossLanguageCall extends DetectorLattice

case class CrossLanguageCall(language: Language, code: String,
                             assignments: mutable.Map[String, Tuple2[FieldType, Set[DefinitionSite]]]
)
    extends DetectorLattice

