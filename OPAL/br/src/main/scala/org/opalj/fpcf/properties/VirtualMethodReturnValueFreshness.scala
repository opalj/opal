/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.opalj
package fpcf
package properties

sealed trait VirtualMethodReturnValueFreshnessMetaInformation extends PropertyMetaInformation {
    override final type Self = VirtualMethodReturnValueFreshness
}

/**
 * TODO
 * @author Florian Kuebler
 */
sealed abstract class VirtualMethodReturnValueFreshness extends Property
    with VirtualMethodReturnValueFreshnessMetaInformation {

    final def key: PropertyKey[VirtualMethodReturnValueFreshness] =
        VirtualMethodReturnValueFreshness.key

    def asReturnValueFreshness: ReturnValueFreshness

    def asConditional: VirtualMethodReturnValueFreshness

    def asUnconditional: VirtualMethodReturnValueFreshness

    def meet(other: VirtualMethodReturnValueFreshness): VirtualMethodReturnValueFreshness = {
        val m = this.asReturnValueFreshness meet other.asReturnValueFreshness
        m.asVirtualMethodReturnValueFreshness
    }
}

object VirtualMethodReturnValueFreshness extends VirtualMethodReturnValueFreshnessMetaInformation {
    final lazy val key: PropertyKey[VirtualMethodReturnValueFreshness] = PropertyKey.create(
        "VirtualMethodReturnValueFreshness",
        VNoFreshReturnValue,
        VNoFreshReturnValue
    )
}

case object VFreshReturnValue extends VirtualMethodReturnValueFreshness {
    override def isRefinable: Boolean = false

    override def asReturnValueFreshness: ReturnValueFreshness = FreshReturnValue

    override def asConditional: VirtualMethodReturnValueFreshness = VConditionalFreshReturnValue

    override def asUnconditional: VirtualMethodReturnValueFreshness = this
}

case object VPrimitiveReturnValue extends VirtualMethodReturnValueFreshness {
    override def isRefinable: Boolean = false

    override def asReturnValueFreshness: ReturnValueFreshness = PrimitiveReturnValue

    override def asConditional: VirtualMethodReturnValueFreshness = throw new UnsupportedOperationException()

    override def asUnconditional: VirtualMethodReturnValueFreshness = this
}

case object VNoFreshReturnValue extends VirtualMethodReturnValueFreshness {
    override def isRefinable: Boolean = true

    override def asReturnValueFreshness: ReturnValueFreshness = NoFreshReturnValue

    override def asConditional: VirtualMethodReturnValueFreshness = throw new UnsupportedOperationException()

    override def asUnconditional: VirtualMethodReturnValueFreshness = this
}

case object VGetter extends VirtualMethodReturnValueFreshness {
    override def isRefinable: Boolean = true

    override def asReturnValueFreshness: ReturnValueFreshness = Getter

    override def asConditional: VirtualMethodReturnValueFreshness = VConditionalGetter

    override def asUnconditional: VirtualMethodReturnValueFreshness = this
}

case object VExtensibleGetter extends VirtualMethodReturnValueFreshness {
    override def asReturnValueFreshness: ReturnValueFreshness = ExtensibleGetter

    override def asConditional: VirtualMethodReturnValueFreshness = VConditionalExtensibleGetter

    override def asUnconditional: VirtualMethodReturnValueFreshness = this

    override def isRefinable: Boolean = true
}

case object VConditionalFreshReturnValue extends VirtualMethodReturnValueFreshness {
    override def isRefinable: Boolean = true

    override def asReturnValueFreshness: ReturnValueFreshness = ConditionalFreshReturnValue

    override def asConditional: VirtualMethodReturnValueFreshness = this

    override def asUnconditional: VirtualMethodReturnValueFreshness = VFreshReturnValue
}

case object VConditionalGetter extends VirtualMethodReturnValueFreshness {
    override def isRefinable: Boolean = true

    override def asReturnValueFreshness: ReturnValueFreshness = ConditionalGetter

    override def asConditional: VirtualMethodReturnValueFreshness = this

    override def asUnconditional: VirtualMethodReturnValueFreshness = VGetter
}

case object VConditionalExtensibleGetter extends VirtualMethodReturnValueFreshness {
    override def asReturnValueFreshness: ReturnValueFreshness = ConditionalExtensibleGetter

    override def asConditional: VirtualMethodReturnValueFreshness = this

    override def asUnconditional: VirtualMethodReturnValueFreshness = VExtensibleGetter

    override def isRefinable: Boolean = true
}