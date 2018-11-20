/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain

import scala.reflect.ClassTag

import org.opalj.br.Type

/**
 * Final binding of a `Domain`'s type `DomainValue` as well as all subtypes of it that are
 * also defined by `Domain`.
 *
 * The type `DomainValue` is set to the type [[org.opalj.ai.Domain.Value]].
 *
 * @author Michael Eichberg
 */
trait DefaultSpecialDomainValuesBinding extends ValuesDomain {

    final type DomainValue = Value

    final type DomainTypedValue[+T <: Type] = TypedValue[T]

    final override val DomainValueTag: ClassTag[DomainValue] = implicitly

    final type DomainIllegalValue = IllegalValue

    final override val TheIllegalValue: DomainIllegalValue = new IllegalValue

    final override val MetaInformationUpdateIllegalValue = MetaInformationUpdate(TheIllegalValue)

    final type DomainReturnAddressValue = ReturnAddressValue

    final override def ReturnAddressValue(address: Int): ReturnAddressValue = {
        new ReturnAddressValue(address)
    }

    final type DomainReturnAddressValues = ReturnAddressValues

    final val TheReturnAddressValues = new ReturnAddressValues

}
