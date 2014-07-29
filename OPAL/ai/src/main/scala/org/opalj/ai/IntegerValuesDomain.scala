/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.opalj
package ai

import org.opalj.util.Answer

/**
 * Defines the public interface between the abstract interpreter and the domain
 * that implements the functionality related to the handling of integer values.
 *
 * @author Michael Eichberg (eichberg@informatik.tu-darmstadt.de)
 */
trait IntegerValuesDomain extends IntegerValuesFactory { domain ⇒

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
        pc: PC,
        value: DomainValue,
        lowerBound: Int,
        upperBound: Int): Answer

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
        pc: PC,
        value: DomainValue,
        lowerBound: Int,
        upperBound: Int): Answer

    /**
     * Tests if the two given integer values are equal.
     *
     * @param value1 A value with computational type integer.
     * @param value2 A value with computational type integer.
     */
    /*ABSTRACT*/ def intAreEqual(pc: PC, value1: DomainValue, value2: DomainValue): Answer

    /**
     * Tests if the two given integer values are not equal.
     *
     * @param value1 A value with computational type integer.
     * @param value2 A value with computational type integer.
     */
    def intAreNotEqual(
        pc: PC,
        value1: DomainValue,
        value2: DomainValue): Answer =
        intAreEqual(pc, value1, value2).negate

    /**
     * Tests if the first integer value is smaller than the second value.
     *
     * @param smallerValue A value with computational type integer.
     * @param largerValue A value with computational type integer.
     */
    /*ABSTRACT*/ def intIsLessThan(
        pc: PC,
        smallerValue: DomainValue,
        largerValue: DomainValue): Answer

    /**
     * Tests if the first integer value is less than or equal to the second value.
     *
     * @param smallerOrEqualValue A value with computational type integer.
     * @param equalOrLargerValue A value with computational type integer.
     */
    /*ABSTRACT*/ def intIsLessThanOrEqualTo(
        pc: PC,
        smallerOrEqualValue: DomainValue,
        equalOrLargerValue: DomainValue): Answer

    /**
     * Tests if the first integer value is larger than the second value.
     *
     * @param largerValue A value with computational type integer.
     * @param smallerValue A value with computational type integer.
     */
    def intIsGreaterThan(
        pc: PC,
        largerValue: DomainValue,
        smallerValue: DomainValue): Answer =
        intIsLessThan(pc, smallerValue, largerValue)

    /**
     * Tests if the first integer value is larger than or equal to the second value.
     *
     * @param largerOrEqualValue A value with computational type integer.
     * @param smallerOrEqualValue A value with computational type integer.
     */
    def intIsGreaterThanOrEqualTo(
        pc: PC,
        largerOrEqualValue: DomainValue,
        smallerOrEqualValue: DomainValue): Answer =
        intIsLessThanOrEqualTo(pc, smallerOrEqualValue, largerOrEqualValue)

    /**
     * Tests if the given integer value is 0 or maybe 0.
     *
     * @param value A value with computational type integer.
     */
    def intIs0(pc: PC, value: DomainValue): Answer =
        intAreEqual(pc, value, IntegerConstant0)

    /**
     * Tests if the given integer value is not 0 or maybe not 0.
     *
     * @param value A value with computational type integer.
     */
    def intIsNot0(pc: PC, value: DomainValue): Answer =
        intAreNotEqual(pc, value, IntegerConstant0)

    /**
     * Tests if the given integer value is &lt; 0 or maybe &lt; 0.
     *
     * @param value A value with computational type integer.
     */
    def intIsLessThan0(pc: PC, value: DomainValue): Answer =
        intIsLessThan(pc, value, IntegerConstant0)

    /**
     * Tests if the given integer value is less than or equal to 0 or maybe
     * less than or equal to 0.
     *
     * @param value A value with computational type integer.
     */
    def intIsLessThanOrEqualTo0(pc: PC, value: DomainValue): Answer =
        intIsLessThanOrEqualTo(pc, value, IntegerConstant0)

    /**
     * Tests if the given integer value is &gt; 0 or maybe &gt; 0.
     *
     * @param value A value with computational type integer.
     */
    def intIsGreaterThan0(pc: PC, value: DomainValue): Answer =
        intIsGreaterThan(pc, value, IntegerConstant0)

    /**
     * Tests if the given value is greater than or equal to 0 or maybe greater
     * than or equal to 0.
     *
     * @param value A value with computational type integer.
     */
    def intIsGreaterThanOrEqualTo0(pc: PC, value: DomainValue): Answer =
        intIsGreaterThanOrEqualTo(pc, value, IntegerConstant0)

    // -----------------------------------------------------------------------------------
    //
    // HANDLING CONSTRAINTS RELATED TO VALUES
    //
    // -----------------------------------------------------------------------------------

    //
    // W.r.t. Integer values

    /**
     * Sets the given domain value to the `theValue`.
     *
     * This function is called by OPAL '''before''' it starts to explore the branch
     * where this condition has to hold. (This function is, e.g., called whenever we explore
     * the branches of a switch-case statement.) I.e., the constraint is established
     * before a potential join operation.
     *
     * @param value An integer value that does not have the value `theValue` as it single
     *      possible value. (I.e., intHasValue(
     */
    def intEstablishValue(
        pc: PC,
        theValue: Int,
        value: DomainValue,
        operands: Operands,
        locals: Locals): (Operands, Locals) = (operands, locals)

    def intEstablishAreEqual(
        pc: PC,
        value1: DomainValue,
        value2: DomainValue,
        operands: Operands,
        locals: Locals): (Operands, Locals) = (operands, locals)
    private[ai] final def IntAreEqual = intEstablishAreEqual _

    def intEstablishAreNotEqual(
        pc: PC,
        value1: DomainValue,
        value2: DomainValue,
        operands: Operands,
        locals: Locals): (Operands, Locals) = (operands, locals)
    private[ai] final def IntAreNotEqual = intEstablishAreNotEqual _

    def intEstablishIsLessThan(
        pc: PC,
        value1: DomainValue,
        value2: DomainValue,
        operands: Operands,
        locals: Locals): (Operands, Locals) = (operands, locals)
    private[ai] final def IntIsLessThan = intEstablishIsLessThan _

    def intEstablishIsLessThanOrEqualTo(
        pc: PC,
        value1: DomainValue,
        value2: DomainValue,
        operands: Operands,
        locals: Locals): (Operands, Locals) = (operands, locals)
    private[ai] final def IntIsLessThanOrEqualTo = intEstablishIsLessThanOrEqualTo _

    /**
     * A function that takes a program counter (`PC`), a value, the current operands
     * and the register assignment and updates the operands and the register
     * assignment w.r.t. the given value and the modeled constraint.
     */
    private[ai]type SingleValueConstraint = ((PC, DomainValue, Operands, Locals) ⇒ (Operands, Locals))

    /**
     * A function that takes a program counter (`PC`), two values, the current operands
     * and the register assignment and updates the operands and the register
     * assignment w.r.t. the given values and the modeled constraint.
     */
    private[ai]type TwoValuesConstraint = ((PC, DomainValue, DomainValue, Operands, Locals) ⇒ (Operands, Locals))

    private[ai] final def IntIsGreaterThan: TwoValuesConstraint =
        (pc: PC, value1: DomainValue, value2: DomainValue, operands: Operands, locals: Locals) ⇒
            intEstablishIsLessThan(pc, value2, value1, operands, locals)

    private[ai] final def IntIsGreaterThanOrEqualTo: TwoValuesConstraint =
        (pc: PC, value1: DomainValue, value2: DomainValue, operands: Operands, locals: Locals) ⇒
            intEstablishIsLessThanOrEqualTo(pc, value2, value1, operands, locals)

    private[ai] final def IntIs0: SingleValueConstraint =
        (pc: PC, value: DomainValue, operands: Operands, locals: Locals) ⇒
            intEstablishAreEqual(pc, value, IntegerConstant0, operands, locals)

    private[ai] final def IntIsNot0: SingleValueConstraint =
        (pc: PC, value: DomainValue, operands: Operands, locals: Locals) ⇒
            intEstablishAreNotEqual(pc, value, IntegerConstant0, operands, locals)

    private[ai] final def IntIsLessThan0: SingleValueConstraint =
        (pc: PC, value: DomainValue, operands: Operands, locals: Locals) ⇒
            intEstablishIsLessThan(pc, value, IntegerConstant0, operands, locals)

    private[ai] final def IntIsLessThanOrEqualTo0: SingleValueConstraint =
        (pc: PC, value: DomainValue, operands: Operands, locals: Locals) ⇒
            intEstablishIsLessThanOrEqualTo(pc, value, IntegerConstant0, operands, locals)

    private[ai] final def IntIsGreaterThan0: SingleValueConstraint =
        (pc: PC, value: DomainValue, operands: Operands, locals: Locals) ⇒
            intEstablishIsLessThan(pc, IntegerConstant0, value, operands, locals)

    private[ai] final def IntIsGreaterThanOrEqualTo0: SingleValueConstraint =
        (pc: PC, value: DomainValue, operands: Operands, locals: Locals) ⇒
            intEstablishIsLessThanOrEqualTo(pc, IntegerConstant0, value, operands, locals)

    // -----------------------------------------------------------------------------------
    //
    // ABSTRACTIONS RELATED TO INSTRUCTIONS
    //
    // -----------------------------------------------------------------------------------

    //
    // TYPE CONVERSION
    //

    def i2b(pc: PC, value: DomainValue): DomainValue
    def i2c(pc: PC, value: DomainValue): DomainValue
    def i2s(pc: PC, value: DomainValue): DomainValue

    //
    // UNARY EXPRESSIONS
    //

    def ineg(pc: PC, value: DomainValue): DomainValue

    //
    // BINARY EXPRESSIONS
    //

    /**
     * Computation that returns a numeric value or an `ObjectType.ArithmeticException`.
     */
    type IntegerValueOrArithmeticException = Computation[DomainValue, ExceptionValue]

    def iadd(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue
    def iand(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue
    def idiv(pc: PC, value1: DomainValue, value2: DomainValue): IntegerValueOrArithmeticException
    def imul(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue
    def ior(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue
    def irem(pc: PC, value1: DomainValue, value2: DomainValue): IntegerValueOrArithmeticException
    def ishl(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue
    def ishr(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue
    def isub(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue
    def iushr(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue
    def ixor(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue
    def iinc(pc: PC, value: DomainValue, increment: Int): DomainValue

}
