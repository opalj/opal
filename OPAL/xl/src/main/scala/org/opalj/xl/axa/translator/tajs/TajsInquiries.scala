/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.xl.axa.translator.tajs

import dk.brics.tajs.lattice.PKey
import dk.brics.tajs.lattice.Value
import org.opalj.br.Method
import org.opalj.fpcf.Entity
import org.opalj.fpcf.FallbackReason
import org.opalj.fpcf.OrderedProperty
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation
import org.opalj.fpcf.PropertyStore

import java.io.File
import scala.collection.mutable

sealed trait TajsInquiriesPropertyMetaInformation extends PropertyMetaInformation {
    final type Self = TajsInquiries
}

sealed trait TajsInquiries extends TajsInquiriesPropertyMetaInformation with OrderedProperty {
    def meet(other: TajsInquiries): TajsInquiries = {
        (this, other) match {
            case (_, _) => NoJavaScriptCall
        }
    }

    def checkIsEqualOrBetterThan(e: Entity, other: TajsInquiries): Unit = {
        if (meet(other) != other) {
            throw new IllegalArgumentException(s"$e: impossible refinement: $other => $this")
        }
    }
    final def key: PropertyKey[TajsInquiries] = TajsInquiries.key
}

object TajsInquiries extends TajsInquiriesPropertyMetaInformation {
    final val key: PropertyKey[TajsInquiries] = PropertyKey.create(
        "JavaScriptBridgeLattice",
        (_: PropertyStore, _: FallbackReason, e: Entity) => {
            e match {
                case Method => NoJavaScriptCall
                case x      => throw new IllegalArgumentException(s"$x is not a method")
            }
        }
    )
}

case object NoJavaScriptCall extends TajsInquiries

case class JavaScriptCall(file: File, propertyChanges: mutable.Map[PKey.StringPKey, Value]) extends TajsInquiries

