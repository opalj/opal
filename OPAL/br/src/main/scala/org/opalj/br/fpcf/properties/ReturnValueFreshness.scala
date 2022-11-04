/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package properties

import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation

sealed trait ReturnValueFreshnessPropertyMetaInformation extends PropertyMetaInformation {
    final type Self = ReturnValueFreshness
}

/**
 * Describes whether a method returns a value that is allocated in that method or its
 * callees and only has escape state [[EscapeViaReturn]].
 *
 * Values are [[FreshReturnValue]] for always freshly allocated return values, [[Getter]] for
 * return values that can be treated as fresh if the methods's receiver is known to be fresh and
 * [[ExtensibleGetter]] if additionally the method's receiver type must not implement
 * `java.lang.Cloneable`.
 * If the return value of a method may not be fresh, the method has [[NoFreshReturnValue]].
 * [[PrimitiveReturnValue]] is used for methods with a return type that is a base type or void. The
 * return value freshness should never be relevant for such methods.
 *
 * @author Florian Kuebler
 */
sealed abstract class ReturnValueFreshness
    extends Property
    with ReturnValueFreshnessPropertyMetaInformation {

    final def key: PropertyKey[ReturnValueFreshness] = ReturnValueFreshness.key

    def asVirtualMethodReturnValueFreshness: VirtualMethodReturnValueFreshness

    def meet(other: ReturnValueFreshness): ReturnValueFreshness
}

object ReturnValueFreshness extends ReturnValueFreshnessPropertyMetaInformation {

    final val key: PropertyKey[ReturnValueFreshness] = PropertyKey.create(
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
        case PrimitiveReturnValue => throw new UnsupportedOperationException()
        case _                    => other
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
        case FreshReturnValue     => this
        case PrimitiveReturnValue => throw new UnsupportedOperationException()
        case _                    => other
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
        case FreshReturnValue     => this
        case Getter               => this
        case PrimitiveReturnValue => throw new UnsupportedOperationException()
        case _                    => other
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
        case PrimitiveReturnValue => throw new UnsupportedOperationException()
        case _                    => this
    }

    override def asVirtualMethodReturnValueFreshness: VirtualMethodReturnValueFreshness = {
        VNoFreshReturnValue
    }
}
