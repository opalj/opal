/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package properties

import org.opalj.br.DeclaredMethod
import org.opalj.value.ValueInformation

sealed trait MethodReturnValuePropertyMetaInformation extends PropertyMetaInformation {

    final type Self = MethodReturnValue
}

/**
 * Stores the information about the value (always) returned by a specific method. '''Overridden
 * methods are generally not taken into account.'''
 *
 * In the worst case the information about the return value is just the declared type.
 *
 * @param returnValue The value returned by the method when the method does not throw an exception.
 *      If the method always throws an exception, then the returnValue is `None`.
 */
case class MethodReturnValue private (
        returnValue: ValueInformation
) extends Property with MethodReturnValuePropertyMetaInformation {

    assert(returnValue ne null, "returnValue must not be null")

    final override def key: PropertyKey[MethodReturnValue] = MethodReturnValue.key

}

object MethodReturnValue extends MethodReturnValuePropertyMetaInformation {

    /**
     * The key associated with every purity property.
     */
    final val key = PropertyKey.create[DeclaredMethod, MethodReturnValue](
        "org.opalj.value.MethodReturnValue",
        (_: PropertyStore, _: FallbackReason, m: DeclaredMethod) ⇒ {
            MethodReturnValue(ValueInformation(m.descriptor.returnType))
        },
        (_: PropertyStore, eps: EPS[DeclaredMethod, MethodReturnValue]) ⇒ eps.ub,
        (_: PropertyStore, _: Entity) ⇒ None
    )

}
