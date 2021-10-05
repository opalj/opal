/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l1

/**
 * Default implementation of a domain that performs basic conversions between primitive values.
 *
 * @author Riadh Chtara
 * @author Michael Eichberg
 */
trait ConcretePrimitiveValuesConversions extends l0.TypeLevelPrimitiveValuesConversions {
    domain: PrimitiveValuesFactory with Configuration with ConcreteLongValues with ConcreteIntegerValues =>

    override def i2d(pc: Int, value: DomainValue): DomainValue = {
        intValue(value)(v => DoubleValue(pc, v.toDouble))(DoubleValue(pc))
    }

    override def i2f(pc: Int, value: DomainValue): DomainValue = {
        intValue(value)(v => FloatValue(pc, v.toFloat))(FloatValue(pc))
    }

    override def i2l(pc: Int, value: DomainValue): DomainValue = {
        intValue(value)(v => LongValue(pc, v.toLong))(LongValue(pc))
    }

    override def l2d(pc: Int, value: DomainValue): DomainValue = {
        longValue(value) { v => DoubleValue(pc, v.toDouble) } { DoubleValue(pc) }
    }

    override def l2f(pc: Int, value: DomainValue): DomainValue = {
        longValue(value) { v => FloatValue(pc, v.toFloat) } { FloatValue(pc) }
    }

    override def l2i(pc: Int, value: DomainValue): DomainValue = {
        longValue(value) { v => IntegerValue(pc, v.toInt) } { IntegerValue(pc) }
    }
}
