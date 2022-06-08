/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai

/**
 * Defines the public interface between the abstract interpreter and the domain
 * that implements the functionality related to the handling of long values.
 *
 * @author Michael Eichberg
 * @author Dennis Siebert
 */
trait LongValuesDomain extends LongValuesFactory { domain =>

    //
    // RELATIONAL OPERATORS
    //

    /**
     * ''Comparison (==)'' of two long values.
     *
     * @param pc The pc of the comparison instruction.
     * @param value1 A long value (guaranteed by the JVM's semantics).
     * @param value2 A long value (guaranteed by the JVM's semantics).
     * @return A domain value that encapsulates an integer value with the value -1,0 or 1.
     */
    def lcmp(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue

    //
    // UNARY ARITHMETIC EXPRESSIONS
    //

    /**
     * ''Negation'' of a long value.
     *
     * @param pc The pc of the neg instruction.
     * @param value A long value (guaranteed by the JVM's semantics).
     */
    def lneg(pc: Int, value: DomainValue): DomainValue

    //
    // BINARY ARITHMETIC EXPRESSIONS
    //

    type LongValueOrArithmeticException = Computation[DomainValue, ExceptionValue]

    /**
     * ''Add'' of two long values.
     *
     * @param pc The pc of the add(+) instruction.
     * @param value1 A long value (guaranteed by the JVM's semantics).
     * @param value2 A long value (guaranteed by the JVM's semantics).
     */
    def ladd(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue

    /**
     * ''Subtraction'' of two long values.
     *
     * @param pc The pc of the sub(-) instruction.
     * @param value1 A long value (guaranteed by the JVM's semantics.)
     * @param value2 A long value (guaranteed by the JVM's semantics.)
     */
    def lsub(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue

    /**
     * ''Division'' of two long values.
     *
     * @param pc The pc of the div (/) instruction.
     * @param value1 A long value (guaranteed by the JVM's semantics).
     * @param value2 A long value (guaranteed by the JVM's semantics).
     * @return The return value is the calculated value and/or (depending on the domain)
     *      an `ArithmeticException` if `value2` is `0`.
     */
    def ldiv(pc: Int, value1: DomainValue, value2: DomainValue): LongValueOrArithmeticException

    /**
     * ''Multiplication'' of two long values.
     *
     * @param pc The pc of the mul (/) instruction.
     * @param value1 A long value (guaranteed by the JVM's semantics).
     * @param value2 A long value (guaranteed by the JVM's semantics).
     */
    def lmul(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue

    /**
     * ''Remainder'' of two long values.
     *
     * @param pc The pc of the div (/) instruction.
     * @param value1 A long value (guaranteed by the JVM's semantics).
     * @param value2 A long value (guaranteed by the JVM's semantics).
     * @return The return value is the calculated value and/or (depending on the domain)
     *      an `ArithmeticException` if `value2` is `0`.
     */
    def lrem(pc: Int, value1: DomainValue, value2: DomainValue): LongValueOrArithmeticException

    /**
     * ''Boolean and'' of two long values.
     *
     * @param pc The pc of the "&" instruction.
     * @param value1 A long value (guaranteed by the JVM's semantics).
     * @param value2 A long value (guaranteed by the JVM's semantics).
     */
    def land(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue

    /**
     * ''Boolean or'' of two long values.
     *
     * @param pc The pc of the "boolean or" (|) instruction.
     * @param value1 A long value (guaranteed by the JVM's semantics).
     * @param value2 A long value (guaranteed by the JVM's semantics).
     */
    def lor(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue

    /**
     * ''xor'' of two long values.
     *
     * @param pc The pc of the "xor" instruction.
     * @param value1 A long value (guaranteed by the JVM's semantics).
     * @param value2 A long value (guaranteed by the JVM's semantics).
     */
    def lxor(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue

    /**
     * ''Shift left'' of a long value.
     *
     * @param pc The pc of the "shift left" instruction.
     * @param value1 A long value (guaranteed by the JVM's semantics).
     * @param value2 A int value (guaranteed by the JVM's semantics) that determines
     *      the number of bits to shift.
     */
    def lshl(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue

    /**
     * ''Shift right'' of a long value.
     *
     * @param pc The pc of the "shift right" instruction.
     * @param value1 A long value (guaranteed by the JVM's semantics).
     * @param value2 An int value (guaranteed by the JVM's semantics) that determines
     *      the number of bits to shift.
     */
    def lshr(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue

    /**
     * ''Unsigned shift right'' of a long value.
     *
     * @param pc The pc of the "unsigned shift right" instruction.
     * @param value1 A long value (guaranteed by the JVM's semantics).
     * @param value2 A int value (guaranteed by the JVM's semantics) that determines
     *      the number of bits to shift.
     */
    def lushr(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue

}
