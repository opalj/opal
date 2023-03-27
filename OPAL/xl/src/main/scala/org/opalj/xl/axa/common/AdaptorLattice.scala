/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.xl.axa.common

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

sealed trait AdaptorPropertyMetaInformation extends PropertyMetaInformation {
    final type Self = AdaptorLattice
}

sealed trait AdaptorLattice extends AdaptorPropertyMetaInformation with OrderedProperty {
    def meet(other: AdaptorLattice): AdaptorLattice = {
        (this, other) match {
            case (_, _)                  => NoCrossLanguageCall
        }
    }

    def checkIsEqualOrBetterThan(e: Entity, other: AdaptorLattice): Unit = {
        if (meet(other) != other) {
            throw new IllegalArgumentException(s"$e: impossible refinement: $other => $this")
        }
    }
    final def key: PropertyKey[AdaptorLattice] = AdaptorLattice.key
}

object AdaptorLattice extends AdaptorPropertyMetaInformation {
    final val key: PropertyKey[AdaptorLattice] = PropertyKey.create(
        "AdaptorLattice",
        (_: PropertyStore, _: FallbackReason, e: Entity) => {
            e match {
                case Method => NoCrossLanguageCall
                case x => throw new IllegalArgumentException(s"$x is not a method")
            }
        }
    )
}

case object NoCrossLanguageCall extends AdaptorLattice

case class CrossLanguageCall(language: Language, code: String, assignments: mutable.Map[String,Set[DefinitionSite]])
extends AdaptorLattice




