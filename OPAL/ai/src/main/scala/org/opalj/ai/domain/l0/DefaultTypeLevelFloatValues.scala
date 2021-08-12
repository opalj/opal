/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l0

/**
 * Base implementation of the `TypeLevelFloatValues` trait that requires that
 * the domain's Value trait is not extended. This implementation just satisfies
 * the basic requirements of OPAL w.r.t. the domain's computational type.
 *
 * @author Michael Eichberg
 */
trait DefaultTypeLevelFloatValues
    extends DefaultSpecialDomainValuesBinding
    with TypeLevelFloatValues {
    domain: IntegerValuesFactory =>

    case object AFloatValue extends super.FloatValue {

        override def doJoin(pc: Int, value: DomainValue): Update[DomainValue] = NoUpdate

        override def abstractsOver(other: DomainValue): Boolean = other eq this

        override def summarize(pc: Int): DomainValue = this

        override def adapt(target: TargetDomain, pc: Int): target.DomainValue = {
            target.FloatValue(pc)
        }

        override def constantValue: Option[Float] = None
    }

    override def FloatValue(valueOrigin: Int): FloatValue = AFloatValue

    override def FloatValue(valueOrigin: Int, value: Float): FloatValue = AFloatValue
}

