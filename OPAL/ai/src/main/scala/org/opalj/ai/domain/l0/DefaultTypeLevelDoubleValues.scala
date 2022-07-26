/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l0

/**
 * Base implementation of the `TypeLevelDoubleValues` trait that requires that
 * the domain's `Value` trait is not extended. This implementation just satisfies
 * the basic requirements of OPAL w.r.t. the domain's computational type.
 *
 * @author Michael Eichberg
 */
trait DefaultTypeLevelDoubleValues
    extends DefaultSpecialDomainValuesBinding
    with TypeLevelDoubleValues {
    domain: IntegerValuesFactory =>

    /**
     * Represents an unknown double value.
     */
    case object ADoubleValue extends super.DoubleValue {

        override def doJoin(pc: Int, value: DomainValue): Update[DomainValue] =
            // Since `value` is guaranteed to have computational type double and we
            // don't care about the precise value, as this DomainValue already
            // just represents "some" double value, we can always safely return
            // NoUpdate.
            NoUpdate

        override def abstractsOver(other: DomainValue): Boolean = other eq this

        override def summarize(pc: Int): DomainValue = this

        override def adapt(target: TargetDomain, valueOrigin: Int): target.DomainValue = {
            target.DoubleValue(valueOrigin)
        }

        override def constantValue: Option[Double] = None

    }

    final override def DoubleValue(valueOrigin: Int): DoubleValue = ADoubleValue

    final override def DoubleValue(valueOrigin: Int, value: Double): DoubleValue = ADoubleValue
}

