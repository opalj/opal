/* BSD 2-Clause License - see OPAL/LICENSE for details. */
/*package org.opalj
package xl
package translator
package tajs

import dk.brics.tajs.lattice.PKey
import dk.brics.tajs.lattice.Value
import org.opalj.br.Method
import org.opalj.fpcf.Entity
import org.opalj.fpcf.FallbackReason
import org.opalj.fpcf.OrderedProperty
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation
import org.opalj.fpcf.PropertyStore
import org.opalj.xl.common.ForeignFunctionCall

import java.io.File

import scala.collection.mutable

sealed trait JavaScriptInteractionTempPropertyMetaInformation extends PropertyMetaInformation {
    final type Self = JavaScriptInteractionTemp
}

sealed trait JavaScriptInteractionTemp extends JavaScriptInteractionTempPropertyMetaInformation with OrderedProperty {
    def meet(other: JavaScriptInteractionTemp): JavaScriptInteractionTemp = {
        (this, other) match {
            case (_, _) => NoJavaScriptCallTemp
        }
    }

    def checkIsEqualOrBetterThan(e: Entity, other: JavaScriptInteractionTemp): Unit = {
        if (meet(other) != other) {
            throw new IllegalArgumentException(s"$e: impossible refinement: $other => $this")
        }
    }
    final def key: PropertyKey[JavaScriptInteractionTemp] = JavaScriptInteractionTemp.key
}

object JavaScriptInteractionTemp extends JavaScriptInteractionTempPropertyMetaInformation {
    final val key: PropertyKey[JavaScriptInteractionTemp] = PropertyKey.create(
        "JavaScriptInteractionTemp",
        (_: PropertyStore, _: FallbackReason, e: Entity) => {
            e match {
                case Method => NoJavaScriptCallTemp
                case x      => throw new IllegalArgumentException(s"$x is not a method")
            }
        }
    )
}

case object NoJavaScriptCallTemp extends JavaScriptInteractionTemp

case class JavaScriptCallTemp(
                           files: List[File],
                           foreignFunctionCall: ForeignFunctionCall,
                           propertyChanges: mutable.Map[PKey.StringPKey, Value]
) extends JavaScriptInteractionTemp
*/

