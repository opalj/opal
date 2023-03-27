/* BSD 2-Clause License - see OPAL/LICENSE for details. */
/*package org.opalj
package xl
package axa
package bridge
package javajavascript

import org.opalj.br.Method
import org.opalj.fpcf.Entity
import org.opalj.fpcf.FallbackReason
import org.opalj.fpcf.OrderedProperty
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation
import org.opalj.fpcf.PropertyStore
import org.opalj.tac.common.DefinitionSite

import scala.collection.mutable

sealed trait JavaJavaScriptAdaptorPropertyMetaInformation extends PropertyMetaInformation {
    final type Self = JavaJavaScriptAdaptorLattice
}

sealed trait JavaJavaScriptAdaptorLattice extends JavaJavaScriptAdaptorPropertyMetaInformation with OrderedProperty {
    def meet(other: JavaJavaScriptAdaptorLattice): JavaJavaScriptAdaptorLattice = {
        (this, other) match {
            case (_, _)                  => NoCrossLanguageCall
        }
    }

    def checkIsEqualOrBetterThan(e: Entity, other: JavaJavaScriptAdaptorLattice): Unit = {
        if (meet(other) != other) {
            throw new IllegalArgumentException(s"$e: impossible refinement: $other => $this")
        }
    }
    final def key: PropertyKey[JavaJavaScriptAdaptorLattice] = JavaJavaScriptAdaptorLattice.key
}

object JavaJavaScriptAdaptorLattice extends JavaJavaScriptAdaptorPropertyMetaInformation {
    final val key: PropertyKey[JavaJavaScriptAdaptorLattice] = PropertyKey.create(
        "JavaJavaScriptAdaptorLattice",
        (_: PropertyStore, _: FallbackReason, e: Entity) => {
            e match {
                case Method => NoCrossLanguageCall
                case x =>
                    throw new IllegalArgumentException(s"$x is not a Method")
            }
        }
    )
}

case object NoCrossLanguageCall extends JavaJavaScriptAdaptorLattice

case class CrossLanguageCall(language:String, code: String, assignments: mutable.Map[String,Set[DefinitionSite]])
  extends JavaJavaScriptAdaptorLattice
*/