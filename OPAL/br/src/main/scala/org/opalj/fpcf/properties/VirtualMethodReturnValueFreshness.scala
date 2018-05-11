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