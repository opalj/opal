/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l0

/**
 * Base implementation of the `TypeLevelLongValues` trait that requires that
 * the domain's `Value` trait is not extended. This implementation just satisfies
 * the basic requirements of OPAL w.r.t. the domain's computational type.
 *
 * @author Michael Eichberg
 */
trait DefaultTypeLevelLongValues
    extends DefaultSpecialDomainValuesBinding
    with TypeLevelLongValues {
    this: PrimitiveValuesFactory with ExceptionsFactory with Configuration =>

    case object ALongValue extends super.LongValue {

        override def doJoin(pc: Int, value: DomainValue): Update[DomainValue] = NoUpdate

        override def abstractsOver(other: DomainValue): Boolean = other eq this

        override def summarize(pc: Int): DomainValue = this

        override def adapt(target: TargetDomain, pc: Int): target.DomainValue = {
            target.LongValue(pc)
        }

        override def constantValue: Option[Long] = None
    }

    override def LongValue(valueOrigin: ValueOrigin): LongValue = ALongValue

    override def LongValue(valueOrigin: ValueOrigin, value: Long): LongValue = ALongValue
}

