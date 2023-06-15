/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package xl
package detector

import org.opalj.br.FieldType
import org.opalj.br.Method
import org.opalj.fpcf.Entity
import org.opalj.fpcf.FallbackReason
import org.opalj.fpcf.OrderedProperty
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation
import org.opalj.fpcf.PropertyStore
import org.opalj.xl.common.ForeignFunctionCall

import scala.collection.mutable

sealed trait JavaScriptInteractionPropertyMetaInformation extends PropertyMetaInformation {
    final type Self = JavaScriptInteraction
}

sealed trait JavaScriptInteraction extends JavaScriptInteractionPropertyMetaInformation with OrderedProperty {
    def meet(other: JavaScriptInteraction): JavaScriptInteraction = {
        (this, other) match {
            case (_, _) => NoJavaScriptCall
        }
    }

    def checkIsEqualOrBetterThan(e: Entity, other: JavaScriptInteraction): Unit = {
        if (meet(other) != other) {
            throw new IllegalArgumentException(s"$e: impossible refinement: $other => $this")
        }
    }
    final def key: PropertyKey[JavaScriptInteraction] = JavaScriptInteraction.key
}

object JavaScriptInteraction extends JavaScriptInteractionPropertyMetaInformation {
    final val key: PropertyKey[JavaScriptInteraction] = PropertyKey.create(
        "JavaScriptInteraction",
        (_: PropertyStore, _: FallbackReason, e: Entity) => {
            e match {
                case Method => NoJavaScriptCall
                case x      => throw new IllegalArgumentException(s"$x is not a method")
            }
        }
    )
}

case object NoJavaScriptCall extends JavaScriptInteraction

case class JavaScriptFunctionCall(code: String, foreignFunctionCall: ForeignFunctionCall,
                                  assignments: mutable.Map[String, Tuple2[FieldType, Set[AnyRef]]]) extends JavaScriptInteraction

case class JavaScriptExecution(code: String, assignments: mutable.Map[String, Tuple2[FieldType, Set[AnyRef]]]) extends JavaScriptInteraction

