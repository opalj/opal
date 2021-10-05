/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai

/**
 * Defines the public interface between the abstract interpreter and the domain
 * that implements the functionality related to the handling of `int`eger values.
 *
 * @author Michael Eichberg
 */
trait IntegerValuesDomain extends IntegerValuesFactory { domain =>

    // -----------------------------------------------------------------------------------
    //
    // QUERY METHODS
    //
    // -----------------------------------------------------------------------------------

    /**
     * Returns `Yes` iff at least one possible extension of the given
     * `value` is in the specified range; that is, if the intersection of the range of
     * values captured by the given `value` and the specified range is non-empty.
     *
     * For example, if the given value captures all positive integer values and the
     * specified range is [-1,1] then the answer has to be `Yes`. If we know nothing
     * about the potential extension of the given value the answer will be `Unknown`.
     * The answer is `No` iff both ranges are non-overlapping.
     *
     * @param value A value that has to be of computational type integer.
     * @param lowerBound The range's lower bound (inclusive).
     * @param upperBound The range's upper bound (inclusive).
     */
    /*ABSTRACT*/ def intIsSomeValueInRange(
        pc:         Int,
        value:      DomainValue,
        lowerBound: Int,
        upperBound: Int
    ): Answer

    /**
     * Returns `Yes` iff at least one (possible) extension of a given value is
     * not in the specified range; that is, if the set difference of the range of
     * values captured by the given `value` and  the specified range is non-empty.
     * For example, if the given `value` has the integer value `10` and the
     * specified range is [0,Integer.MAX_VALUE] then the answer has to be `No`. But,
     * if the given `value` represents the range [-5,Integer.MAX_VALUE] and the specified
     * range is again [0,Integer.MAX_VALUE] then the answer has to be `Yes`.
     *
     * The answer is `Yes` iff the analysis determined that at runtime `value` will have
     * a value that is not in the specified range. If the analysis(domain) is not able
     * to determine whether the value is or is not in the given range then the answer
     * has to be `Unknown`.
     *
     * @param value A value that has to be of computational type integer.
     * @param lowerBound The range's lower bound (inclusive).
     * @param upperBound The range's upper bound (inclusive).
     */
    /*ABSTRACT*/ def intIsSomeValueNotInRange(
        pc:         Int,
        value:      DomainValue,
        lowerBound: Int,
        upperBound: Int
    ): Answer

    /**
     * Tests if the two given integer values are equal.
     *
     * @param value1 A value with computational type integer.
     * @param value2 A value with computational type integer.
     */
    /*ABSTRACT*/ def intAreEqual(pc: Int, value1: DomainValue, value2: DomainValue): Answer

    /**
     * Tests if the two given integer values are not equal.
     *
     * @param value1 A value with computational type integer.
     * @param value2 A value with computational type integer.
     */
    def intAreNotEqual(pc: Int, value1: DomainValue, value2: DomainValue): Answer = {
        intAreEqual(pc, value1, value2).negate
    }

    /**
     * Tests if the first integer value is smaller than the second value.
     *
     * @param smallerValue A value with computational type integer.
     * @param largerValue A value with computational type integer.
     */
    /*ABSTRACT*/ def intIsLessThan(
        pc:           Int,
        smallerValue: DomainValue,
        largerValue:  DomainValue
    ): Answer

    /**
     * Tests if the first integer value is less than or equal to the second value.
     *
     * @param smallerOrEqualValue A value with computational type integer.
     * @param equalOrLargerValue A value with computational type integer.
     */
    /*ABSTRACT*/ def intIsLessThanOrEqualTo(
        pc:                  Int,
        smallerOrEqualValue: DomainValue,
        equalOrLargerValue:  DomainValue
    ): Answer

    /**
     * Tests if the first integer value is larger than the second value.
     *
     * @param largerValue A value with computational type integer.
     * @param smallerValue A value with computational type integer.
     */
    def intIsGreaterThan(
        pc:           Int,
        largerValue:  DomainValue,
        smallerValue: DomainValue
    ): Answer = {
        intIsLessThan(pc, smallerValue, largerValue)
    }

    /**
     * Tests if the first integer value is larger than or equal to the second value.
     *
     * @param largerOrEqualValue A value with computational type integer.
     * @param smallerOrEqualValue A value with computational type integer.
     */
    def intIsGreaterThanOrEqualTo(
        pc:                  Int,
        largerOrEqualValue:  DomainValue,
        smallerOrEqualValue: DomainValue
    ): Answer = {
        intIsLessThanOrEqualTo(pc, smallerOrEqualValue, largerOrEqualValue)
    }

    /**
     * Tests if the given integer value is 0 or maybe 0.
     *
     * @param value A value with computational type integer.
     */
    def intIs0(pc: Int, value: DomainValue): Answer = intAreEqual(pc, value, IntegerConstant0)

    /**
     * Tests if the given integer value is not 0 or maybe not 0.
     *
     * @param value A value with computational type integer.
     */
    def intIsNot0(pc: Int, value: DomainValue): Answer = intAreNotEqual(pc, value, IntegerConstant0)

    /**
     * Tests if the given integer value is &lt; 0 or maybe &lt; 0.
     *
     * @param value A value with computational type integer.
     */
    def intIsLessThan0(pc: Int, value: DomainValue): Answer = {
        intIsLessThan(pc, value, IntegerConstant0)
    }

    /**
     * Tests if the given integer value is less than or equal to 0 or maybe
     * less than or equal to 0.
     *
     * @param value A value with computational type integer.
     */
    def intIsLessThanOrEqualTo0(pc: Int, value: DomainValue): Answer = {
        intIsLessThanOrEqualTo(pc, value, IntegerConstant0)
    }

    /**
     * Tests if the given integer value is &gt; 0 or maybe &gt; 0.
     *
     * @param value A value with computational type integer.
     */
    def intIsGreaterThan0(pc: Int, value: DomainValue): Answer = {
        intIsGreaterThan(pc, value, IntegerConstant0)
    }

    /**
     * Tests if the given value is greater than or equal to 0 or maybe greater
     * than or equal to 0.
     *
     * @param value A value with computational type integer.
     */
    def intIsGreaterThanOrEqualTo0(pc: Int, value: DomainValue): Answer = {
        intIsGreaterThanOrEqualTo(pc, value, IntegerConstant0)
    }

    // -----------------------------------------------------------------------------------
    //
    // HANDLING CONSTRAINTS RELATED TO VALUES
    //
    // -----------------------------------------------------------------------------------

    //
    // W.r.t. Integer values

    /**
     * Sets the given domain value to `theValue`.
     *
     * This function is called by OPAL '''before''' it starts to explore the branch
     * where this condition has to hold. (This function is, e.g., called whenever we explore
     * the branches of a switch-case statement.) I.e., the constraint is established
     * before a potential join operation.
     *
     * @param value An integer domain value that does also, but not exclusively represents
     *      `theValue`.
     *
     */
    def intEstablishValue(
        pc:       Int,
        theValue: Int,
        value:    DomainValue,
        operands: Operands,
        locals:   Locals
    ): (Operands, Locals) = (operands, locals)

    /**
     *
     * @note This function is ONLY defined if a corresponding test (`value1 == value2`)
     *      returned [[org.opalj.Unknown]]. I.e., this method is only allowed to be
     *      called if there is something to establish!
     *      I.e., the domain values are real ranges (not single values, e.g., `[1,1]`)
     *      that overlap.
     */
    def intEstablishAreEqual(
        pc:       Int,
        value1:   DomainValue,
        value2:   DomainValue,
        operands: Operands,
        locals:   Locals
    ): (Operands, Locals) = (operands, locals)

    /**
     * @note This function is ONLY defined if a corresponding test (`value1 != value2`)
     *      returned [[org.opalj.Unknown]]. I.e., this method is only allowed to be
     *      called if there is something to establish!
     *      I.e., the domain values are real ranges (not single values, e.g., `[1,1]`)
     *      that overlap.
     */
    def intEstablishAreNotEqual(
        pc:       Int,
        value1:   DomainValue,
        value2:   DomainValue,
        operands: Operands,
        locals:   Locals
    ): (Operands, Locals) = (operands, locals)

    /**
     * @note This function is ONLY defined if a corresponding test (`value1 < value2`)
     *      returned [[org.opalj.Unknown]]. I.e., this method is only allowed to be
     *      called if there is something to establish!
     *      I.e., the domain values are real ranges (not single values, e.g., `[1,1]`)
     *      that overlap.
     */
    def intEstablishIsLessThan(
        pc:       Int,
        value1:   DomainValue,
        value2:   DomainValue,
        operands: Operands,
        locals:   Locals
    ): (Operands, Locals) = (operands, locals)

    /**
     * @note This function is ONLY defined if a corresponding test (`value1 <= value2`)
     *      returned [[org.opalj.Unknown]]. I.e., this method is only allowed to be
     *      called if there is something to establish!
     *      I.e., the domain values are real ranges (not single values, e.g., `[1,1]`)
     *      that overlap.
     */
    def intEstablishIsLessThanOrEqualTo(
        pc:       Int,
        value1:   DomainValue,
        value2:   DomainValue,
        operands: Operands,
        locals:   Locals
    ): (Operands, Locals) = (operands, locals)

    // -----------------------------------------------------------------------------------
    //
    // ABSTRACTIONS RELATED TO INSTRUCTIONS
    //
    // -----------------------------------------------------------------------------------

    //
    // TYPE CONVERSION
    //

    def i2b(pc: Int, value: DomainValue): DomainValue
    def i2c(pc: Int, value: DomainValue): DomainValue
    def i2s(pc: Int, value: DomainValue): DomainValue

    //
    // UNARY ARITHMETIC EXPRESSIONS
    //

    def ineg(pc: Int, value: DomainValue): DomainValue

    //
    // BINARY ARITHMETIC EXPRESSIONS
    //

    /**
     * Computation that returns a numeric value or an `ObjectType.ArithmeticException`.
     */
    type IntegerValueOrArithmeticException = Computation[DomainValue, ExceptionValue]

    def iadd(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue
    def iand(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue
    def idiv(pc: Int, value1: DomainValue, value2: DomainValue): IntegerValueOrArithmeticException
    def imul(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue
    def ior(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue
    def irem(pc: Int, value1: DomainValue, value2: DomainValue): IntegerValueOrArithmeticException
    def ishl(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue
    def ishr(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue
    def isub(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue
    def iushr(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue
    def ixor(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue
    def iinc(pc: Int, value: DomainValue, increment: Int): DomainValue

}
