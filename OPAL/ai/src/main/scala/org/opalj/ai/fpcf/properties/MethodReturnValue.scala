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
import org.opalj.br.analyses.SomeProject
import org.opalj.br.Method

sealed trait MethodReturnValuePropertyMetaInformation extends PropertyMetaInformation {

    final type Self = MethodReturnValue
}

trait MethodReturnValue extends Property with MethodReturnValuePropertyMetaInformation {

    def returnValue: Option[ValueInformation]

    final override def key: PropertyKey[MethodReturnValue] = MethodReturnValue.key

}

/**
 * Stores the information about the value (always) returned by a specific method. '''Overridden
 * methods are generally not taken into account.''' For a method that always end with an exception
 * `returnValue` will be `None`.
 *
 * In the worst case the information about the return value is just the declared type.
 *
 * @param theReturnValue The value returned by the method when the method does not throw
 *                       an exception. If the method always throws an exception, then the
 *                       returnValue is `None`.
 */
case class TheMethodReturnValue(theReturnValue: ValueInformation) extends MethodReturnValue {

    assert(theReturnValue ne null, "returnValue must not be null")

    override def returnValue: Option[ValueInformation] = Some(theReturnValue)

}
// Used for methods which throw an exception.
case object NoMethodReturnValue extends MethodReturnValue {

    override def returnValue: Option[ValueInformation] = None

}

object MethodReturnValue extends MethodReturnValuePropertyMetaInformation {

    def apply(mrvOption: Option[ValueInformation]): MethodReturnValue = {
        mrvOption match {
            case Some(vi)   => TheMethodReturnValue(vi)
            case _ /*None*/ => NoMethodReturnValue
        }
    }

    /**
     * The key associated with every purity property.
     */
    final val key = PropertyKey.create[Method, MethodReturnValue](
        "opalj.MethodReturnValue",
        (ps: PropertyStore, _: FallbackReason, m: Method) => {
            val p = ps.context(classOf[SomeProject])
            MethodReturnValue(
                Some(ValueInformation.forProperValue(m.descriptor.returnType)(p.classHierarchy))
            )
        }
    )

}
