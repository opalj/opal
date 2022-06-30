/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l0

/**
 * Implementation of all primitive values conversion instructions that just use the
 * default factory methods.
 *
 * @author Michael Eichberg
 */
trait TypeLevelPrimitiveValuesConversions extends PrimitiveValuesConversionsDomain {
    this: ValuesDomain with PrimitiveValuesFactory =>

    override def i2d(pc: Int, value: DomainValue): DomainValue = DoubleValue(pc)
    override def i2f(pc: Int, value: DomainValue): DomainValue = FloatValue(pc)
    override def i2l(pc: Int, value: DomainValue): DomainValue = LongValue(pc)

    /**
     * @inheritdoc
     *
     * @return The result of calling `DoubleValue(pc)`.
     */
    override def l2d(pc: Int, value: DomainValue): DomainValue = DoubleValue(pc)
    /**
     * @inheritdoc
     *
     * @return The result of calling `FloatValue(pc)`.
     */
    override def l2f(pc: Int, value: DomainValue): DomainValue = FloatValue(pc)
    /**
     * @inheritdoc
     *
     * @return The result of calling `IntegerValue(pc)`.
     */
    override def l2i(pc: Int, value: DomainValue): DomainValue = IntegerValue(pc)

    override def f2d(pc: Int, value: DomainValue): DomainValue = DoubleValue(pc)
    override def f2i(pc: Int, value: DomainValue): DomainValue = IntegerValue(pc)
    override def f2l(pc: Int, value: DomainValue): DomainValue = LongValue(pc)

    override def d2f(pc: Int, value: DomainValue): DomainValue = FloatValue(pc)
    override def d2i(pc: Int, value: DomainValue): DomainValue = IntegerValue(pc)
    override def d2l(pc: Int, value: DomainValue): DomainValue = LongValue(pc)

}
