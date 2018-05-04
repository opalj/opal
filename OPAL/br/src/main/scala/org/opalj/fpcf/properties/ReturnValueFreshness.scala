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

sealed trait ReturnValueFreshnessPropertyMetaInformation extends PropertyMetaInformation {
    final type Self = ReturnValueFreshness
}

/**
 * The property describes, whether a method returns a value that is allocated in that method or its
 * callees and only has escape state [[EscapeViaReturn]].
 *
 * Values are [[FreshReturnValue]] for always freshly allocated return values, [[Getter]] for
 * return values that can be treated as fresh if the methods's receiver is known to be fresh and
 * [[ExtensibleGetter]] if additionally the method's receiver type must not implement
 * [[java.lang.Cloneable]].
 * If the return value of a method may not be fresh, the method has [[NoFreshReturnValue]].
 * [[PrimitiveReturnValue]] is used for methods with a return type that is a base type or void. The
 * return value freshness should never be relevant for such methods.
 *
 * @author Florian Kuebler
 */
sealed abstract class ReturnValueFreshness extends Property
        with ReturnValueFreshnessPropertyMetaInformation {

    final def key: PropertyKey[ReturnValueFreshness] = ReturnValueFreshness.key

    def asVirtualMethodReturnValueFreshness: VirtualMethodReturnValueFreshness

    def meet(other: ReturnValueFreshness): ReturnValueFreshness
}

object ReturnValueFreshness extends ReturnValueFreshnessPropertyMetaInformation {
    final lazy val key: PropertyKey[ReturnValueFreshness] = PropertyKey.create(
        // Name of the property
        "ReturnValueFreshness",
        // fallback value
        NoFreshReturnValue
    )
}

/**
 * The return value is freshly allocated by the method or its callees and does not escape the method
 * before being returned.
 */
case object FreshReturnValue extends ReturnValueFreshness {

    override def meet(other: ReturnValueFreshness): ReturnValueFreshness = other match {
        case PrimitiveReturnValue ⇒ throw new UnsupportedOperationException()
        case _                    ⇒ other
    }

    override def asVirtualMethodReturnValueFreshness: VirtualMethodReturnValueFreshness = {
        VFreshReturnValue
    }
}

/**
 * The return value is either freshly allocated by the method or its callees or it is read from a
 * local field (cf. [[FieldLocality]]) of the method's receiver. It can be treated as fresh when
 * the method's receiver object is fresh in the caller.
 */
case object Getter extends ReturnValueFreshness {

    override def meet(other: ReturnValueFreshness): ReturnValueFreshness = other match {
        case FreshReturnValue     ⇒ this
        case PrimitiveReturnValue ⇒ throw new UnsupportedOperationException()
        case _                    ⇒ other
    }

    override def asVirtualMethodReturnValueFreshness: VirtualMethodReturnValueFreshness = VGetter
}

/**
 * The return value is either freshly allocated by the method or its callees or it is read from an
 * extensible local field (cf. [[FieldLocality]]) of the method's receiver. It can be treated as
 * fresh when the method's receiver object is fresh in the caller and its type is known to be
 * not cloneable.
 */
case object ExtensibleGetter extends ReturnValueFreshness {

    override def asVirtualMethodReturnValueFreshness: VirtualMethodReturnValueFreshness = {
        VExtensibleGetter
    }

    override def meet(other: ReturnValueFreshness): ReturnValueFreshness = other match {
        case FreshReturnValue     ⇒ this
        case Getter               ⇒ this
        case PrimitiveReturnValue ⇒ throw new UnsupportedOperationException()
        case _                    ⇒ other
    }

}

/**
 * The method's return type is a base type or void. The question of return value freshness should
 * never arise for such methods.
 */
case object PrimitiveReturnValue extends ReturnValueFreshness {

    override def meet(other: ReturnValueFreshness): ReturnValueFreshness = {
        throw new UnsupportedOperationException()
    }

    override def asVirtualMethodReturnValueFreshness: VirtualMethodReturnValueFreshness = {
        VPrimitiveReturnValue
    }
}

/**
 * The return value is not guaranteed to be fresh, it may be a parameter or loaded from a
 * (non-local) field.
 */
case object NoFreshReturnValue extends ReturnValueFreshness {

    override def meet(other: ReturnValueFreshness): ReturnValueFreshness = other match {
        case PrimitiveReturnValue ⇒ throw new UnsupportedOperationException()
        case _                    ⇒ this
    }

    override def asVirtualMethodReturnValueFreshness: VirtualMethodReturnValueFreshness = {
        VNoFreshReturnValue
    }
}