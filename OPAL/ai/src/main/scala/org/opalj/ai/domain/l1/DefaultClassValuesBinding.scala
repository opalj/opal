/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l1

import scala.reflect.ClassTag

import org.opalj.br.Type

/**
 * @author Arne Lottmann
 */
trait DefaultClassValuesBinding extends DefaultStringValuesBinding with ClassValues {
    domain: CorrelationalDomain with IntegerValuesDomain with TypedValuesFactory with Configuration =>

    type DomainClassValue = ClassValue
    val DomainClassValueTag: ClassTag[DomainClassValue] = implicitly

    //
    // FACTORY METHODS
    //

    protected case class DefaultClassValue(
            origin: ValueOrigin,
            value:  Type,
            refId:  RefId
    ) extends ClassValue

    override def ClassValue(origin: ValueOrigin, value: Type): DomainClassValue = {
        DefaultClassValue(origin, value, nextRefId())
    }

    def ClassValue(origin: ValueOrigin, value: Type, refId: RefId): DomainClassValue = {
        DefaultClassValue(origin, value, refId)
    }
}
