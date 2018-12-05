/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package fpcf
package properties

import org.opalj.fpcf.FallbackReason
import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation
import org.opalj.fpcf.PropertyStore
import org.opalj.value.ValueInformation
import org.opalj.br.DeclaredMethod
import org.opalj.br.analyses.SomeProject

sealed trait MethodReturnValuePropertyMetaInformation extends PropertyMetaInformation {

    final type Self = MethodReturnValue
}

/**
 * Stores the information about the value (always) returned by a specific method. '''Overridden
 * methods are generally not taken into account.''' For a method that always end with an exception
 * `returnValue` will be `None`.
 *
 * In the worst case the information about the return value is just the declared type.
 *
 * @param returnValue The value returned by the method when the method does not throw an exception.
 *         If the method always throws an exception, then the returnValue is `None`.
 */
case class MethodReturnValue private (
        returnValue: Option[ValueInformation]
) extends Property with MethodReturnValuePropertyMetaInformation {

    assert(returnValue ne null, "returnValue must not be null")

    final override def key: PropertyKey[MethodReturnValue] = MethodReturnValue.key

}

object MethodReturnValue extends MethodReturnValuePropertyMetaInformation {

    /**
     * The key associated with every purity property.
     */
    final val key = PropertyKey.create[DeclaredMethod, MethodReturnValue](
        "org.opalj.ai.fpcf.properties.MethodReturnValue",
        (ps: PropertyStore, _: FallbackReason, m: DeclaredMethod) ⇒ {
            val p = ps.context(classOf[SomeProject])
            MethodReturnValue(
                Some(ValueInformation.forProperValue(m.descriptor.returnType)(p.classHierarchy))
            )
        }
    )

}
