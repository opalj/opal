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
    domain: CorrelationalDomain with IntegerValuesDomain with TypedValuesFactory with Configuration with TheClassHierarchy ⇒

    type DomainClassValue = ClassValue
    val DomainClassValue: ClassTag[DomainClassValue] = implicitly

    //
    // FACTORY METHODS
    //

    override def ClassValue(origin: ValueOrigin, value: Type): DomainClassValue = {
        new ClassValue(origin, value, nextRefId())
    }

    def ClassValue(origin: ValueOrigin, value: Type, refId: RefId): DomainClassValue = {
        new ClassValue(origin, value, refId)
    }
}
