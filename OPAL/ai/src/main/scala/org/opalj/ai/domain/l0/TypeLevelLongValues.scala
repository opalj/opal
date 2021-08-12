/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l0

import org.opalj.value.IsLongValue
import org.opalj.br.LongType

/**
 * This partial `Domain` performs all computations related to primitive long
 * values at the type level.
 *
 * This domain can be used as a foundation for building more complex domains.
 *
 * @author Michael Eichberg
 */
trait TypeLevelLongValues extends LongValuesDomain {
    this: IntegerValuesFactory with ExceptionsFactory with Configuration =>

    // -----------------------------------------------------------------------------------
    //
    // REPRESENTATION OF LONG VALUES
    //
    // -----------------------------------------------------------------------------------

    /**
     * Common supertrait of all `DomainValue`s that represent long values.
     */
    trait LongValue extends TypedValue[LongType] with IsLongValue {
        this: DomainTypedValue[LongType] =>

        final override def leastUpperType: Option[LongType] = Some(LongType)

    }

    // -----------------------------------------------------------------------------------
    //
    // HANDLING OF COMPUTATIONS
    //
    // -----------------------------------------------------------------------------------

    //
    // UNARY EXPRESSIONS
    //

    /**
     * @inheritdoc
     *
     * @return The result of calling `LongValue(pc)`.
     */
    /*override*/ def lneg(pc: Int, value: DomainValue): DomainValue = LongValue(pc)

    //
    // RELATIONAL OPERATORS
    //

    /**
     * @inheritdoc
     *
     * @return The result of calling `IntegerValue(pc)`.
     */
    /*override*/ def lcmp(pc: Int, left: DomainValue, right: DomainValue): DomainValue =
        IntegerValue(pc)

    //
    // BINARY EXPRESSIONS
    //

    /**
     * @inheritdoc
     *
     * @return The result of calling `LongValue(pc)`.
     */
    /*override*/ def ladd(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue =
        LongValue(pc)

    /**
     * @inheritdoc
     *
     * @return Either `ComputedValue(LongValue(pc))` if arithmetic exceptions should
     *      not be thrown if nothing is known about the precise value or – if the
     *      policy is to throw an ArithmeticException if in doubt – a
     *      `ComputedValueOrException(LongValue(pc), ArithmeticException(pc))`
     */
    /*override*/ def ldiv(
        pc:    Int,
        left:  DomainValue,
        right: DomainValue
    ): LongValueOrArithmeticException = {
        if (throwArithmeticExceptions)
            ComputedValueOrException(LongValue(pc), VMArithmeticException(pc))
        else
            ComputedValue(LongValue(pc))
    }

    /**
     * @inheritdoc
     *
     * @return The result of calling `LongValue(pc)`.
     */
    /*override*/ def lmul(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue =
        LongValue(pc)

    /**
     * @inheritdoc
     *
     * @return Either `ComputedValue(LongValue(pc))` if arithmetic exceptions should
     *      not be thrown if nothing is known about the precise value or – if the
     *      policy is to throw an ArithmeticException if in doubt – a
     *      `ComputedValueOrException(LongValue(pc), ArithmeticException(pc))`
     */
    /*override*/ def lrem(
        pc:    Int,
        left:  DomainValue,
        right: DomainValue
    ): LongValueOrArithmeticException = {
        if (throwArithmeticExceptions)
            ComputedValueOrException(LongValue(pc), VMArithmeticException(pc))
        else
            ComputedValue(LongValue(pc))
    }

    /**
     * @inheritdoc
     *
     * @return The result of calling `LongValue(pc)`.
     */
    /*override*/ def lsub(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue =
        LongValue(pc)

    /**
     * @inheritdoc
     *
     * @return The result of calling `LongValue(pc)`.
     */
    /*override*/ def land(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue =
        LongValue(pc)

    /**
     * @inheritdoc
     *
     * @return The result of calling `LongValue(pc)`.
     */
    /*override*/ def lor(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue =
        LongValue(pc)

    /**
     * @inheritdoc
     *
     * @return The result of calling `LongValue(pc)`.
     */
    /*override*/ def lxor(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue =
        LongValue(pc)

}

