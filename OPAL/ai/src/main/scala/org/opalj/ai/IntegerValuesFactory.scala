/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai

import org.opalj.br.CTIntType

/**
 * Defines the primary factory methods to create `Integer` values.
 *
 * @author Michael Eichberg
 */
trait IntegerValuesFactory extends ValuesDomain { domain =>

    /**
     * Factory method to create a representation of a boolean value if we know the
     * origin of the value.
     *
     * The domain may ignore the information about the origin (`origin`).
     */
    def BooleanValue(origin: ValueOrigin): DomainTypedValue[CTIntType]

    /**
     * Factory method to create a representation of a boolean value with the given
     * initial value and origin.
     *
     * The domain may ignore the information about the value and the origin (`origin`).
     */
    def BooleanValue(origin: ValueOrigin, value: Boolean): DomainTypedValue[CTIntType]

    /**
     * Factory method to create a representation of the integer constant value 0.
     *
     * OPAL in particular uses this special value for performing subsequent
     * computations against the fixed value 0 (e.g., for if_XX instructions).
     *
     * (The origin ([[ValueOrigin]]) that is used is the [[ConstantValueOrigin]] to
     * signify that this value was not created by the program.)
     *
     * The domain may ignore the information about the value.
     */
    final def IntegerConstant0: DomainTypedValue[CTIntType] = IntegerValue(ConstantValueOrigin, 0)

    /**
     * Factory method to create a `DomainValue` that was created (explicitly or
     * implicitly) by the instruction with the specified program counter.
     *
     * The domain may ignore the information about the origin (`origin`).
     */
    def IntegerValue(origin: ValueOrigin): DomainTypedValue[CTIntType]

    /**
     * Factory method to create a `DomainValue` that represents the given integer value
     * and that was created (explicitly or implicitly) by the instruction with the
     * specified program counter.
     *
     * The domain may ignore the information about the value and the origin (`origin`).
     */
    def IntegerValue(origin: ValueOrigin, value: Int): DomainTypedValue[CTIntType]

    /**
     * Factory method to create a `DomainValue` that was created (explicitly or
     * implicitly) by the instruction with the specified program counter.
     *
     * The domain may ignore the information about the origin (`origin`).
     */
    def ByteValue(origin: ValueOrigin): DomainTypedValue[CTIntType]

    /**
     * Factory method to create a `DomainValue` that represents the given byte value
     * and that was created (explicitly or implicitly) by the instruction with the
     * specified program counter.
     *
     * The domain may ignore the information about the value and the origin (`origin`).
     */
    def ByteValue(origin: ValueOrigin, value: Byte): DomainTypedValue[CTIntType]

    /**
     * Factory method to create a `DomainValue` that was created (explicitly or
     * implicitly) by the instruction with the specified program counter.
     *
     * The domain may ignore the information about the origin (`origin`).
     */
    def ShortValue(origin: ValueOrigin): DomainTypedValue[CTIntType]

    /**
     * Factory method to create a `DomainValue` that represents the given short value
     * and that was created (explicitly or implicitly) by the instruction with the
     * specified program counter.
     */
    def ShortValue(origin: ValueOrigin, value: Short): DomainTypedValue[CTIntType]

    /**
     * Factory method to create a `DomainValue` that was created (explicitly or
     * implicitly) by the instruction with the specified program counter.
     *
     * The domain may ignore the information about the origin (`origin`).
     */
    def CharValue(origin: ValueOrigin): DomainTypedValue[CTIntType]

    /**
     * Factory method to create a `DomainValue` that represents the given char value
     * and that was created (explicitly or implicitly) by the instruction with the
     * specified program counter.
     */
    def CharValue(origin: ValueOrigin, value: Char): DomainTypedValue[CTIntType]
}

