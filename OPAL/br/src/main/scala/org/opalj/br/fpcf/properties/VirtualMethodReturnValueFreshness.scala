/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package properties

import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation

sealed trait VirtualMethodReturnValueFreshnessMetaInformation extends PropertyMetaInformation {
    final override type Self = VirtualMethodReturnValueFreshness
}

/**
 * Aggregate property for [[ReturnValueFreshness]]. Describes whether all methods that can be the
 * target of a virtual method call will return fresh values.
 *
 * @author Florian Kuebler
 */
sealed abstract class VirtualMethodReturnValueFreshness extends Property
    with VirtualMethodReturnValueFreshnessMetaInformation {

    final def key: PropertyKey[VirtualMethodReturnValueFreshness] =
        VirtualMethodReturnValueFreshness.key

    def asReturnValueFreshness: ReturnValueFreshness

    def meet(other: VirtualMethodReturnValueFreshness): VirtualMethodReturnValueFreshness = {
        val m = this.asReturnValueFreshness meet other.asReturnValueFreshness
        m.asVirtualMethodReturnValueFreshness
    }
}

object VirtualMethodReturnValueFreshness extends VirtualMethodReturnValueFreshnessMetaInformation {
    final lazy val key: PropertyKey[VirtualMethodReturnValueFreshness] = PropertyKey.create(
        "VirtualMethodReturnValueFreshness",
        VNoFreshReturnValue
    )
}

case object VFreshReturnValue extends VirtualMethodReturnValueFreshness {
    override def asReturnValueFreshness: ReturnValueFreshness = FreshReturnValue
}

case object VPrimitiveReturnValue extends VirtualMethodReturnValueFreshness {

    override def asReturnValueFreshness: ReturnValueFreshness = PrimitiveReturnValue
}

case object VNoFreshReturnValue extends VirtualMethodReturnValueFreshness {
    override def asReturnValueFreshness: ReturnValueFreshness = NoFreshReturnValue
}

case object VGetter extends VirtualMethodReturnValueFreshness {
    override def asReturnValueFreshness: ReturnValueFreshness = Getter
}

case object VExtensibleGetter extends VirtualMethodReturnValueFreshness {
    override def asReturnValueFreshness: ReturnValueFreshness = ExtensibleGetter
}
