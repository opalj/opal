/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package xl
package axa
package bridge

import org.opalj.br.Method
import org.opalj.fpcf.Entity
import org.opalj.fpcf.FallbackReason
import org.opalj.fpcf.OrderedProperty
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation
import org.opalj.fpcf.PropertyStore
import org.opalj.tac.common.DefinitionSite

import scala.collection.mutable

sealed trait JavaScriptBridgePropertyMetaInformation extends PropertyMetaInformation {
    final type Self = JavaScriptBridgeLattice
}

sealed trait JavaScriptBridgeLattice extends JavaScriptBridgePropertyMetaInformation with OrderedProperty {
    def meet(other: JavaScriptBridgeLattice): JavaScriptBridgeLattice = {
        (this, other) match {
            case (_, _)                  => NoJavaScriptCall
        }
    }

    def checkIsEqualOrBetterThan(e: Entity, other: JavaScriptBridgeLattice): Unit = {
        if (meet(other) != other) {
            throw new IllegalArgumentException(s"$e: impossible refinement: $other => $this")
        }
    }
    final def key: PropertyKey[JavaScriptBridgeLattice] = JavaScriptBridgeLattice.key
}

object JavaScriptBridgeLattice extends JavaScriptBridgePropertyMetaInformation {
    final val key: PropertyKey[JavaScriptBridgeLattice] = PropertyKey.create(
        "JavaScriptBridgeLattice",
        (_: PropertyStore, _: FallbackReason, e: Entity) => {
            e match {
                case Method => NoJavaScriptCall
                case x => throw new IllegalArgumentException(s"$x is not a method")
            }
        }
    )
}

case object NoJavaScriptCall extends JavaScriptBridgeLattice

case class JavaScriptCall(code: String, assignments: mutable.Map[String,Set[DefinitionSite]])
extends JavaScriptBridgeLattice




