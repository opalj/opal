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

/**
 * Defines the public interface between the abstract interpreter and the domain
 * that implements the functionality related to the handling of long values.
 *
 * @author Michael Eichberg (eichberg@informatik.tu-darmstadt.de)
 * @author Dennis Siebert
 */
trait LongValuesDomain extends LongValuesFactory { domain ⇒

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
    def lcmp(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue

    //
    // UNARY ARITHMETIC EXPRESSIONS
    //

    /**
     * ''Negation'' of a long value.
     *
     * @param pc The pc of the neg instruction.
     * @param value A long value (guaranteed by the JVM's semantics).
     */
    def lneg(pc: PC, value: DomainValue): DomainValue

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
    def ladd(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue

    /**
     * ''Subtraction'' of two long values.
     *
     * @param pc The pc of the sub(-) instruction.
     * @param value1 A long value (guaranteed by the JVM's semantics.)
     * @param value2 A long value (guaranteed by the JVM's semantics.)
     */
    def lsub(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue

    /**
     * ''Division'' of two long values.
     *
     * @param pc The pc of the div (/) instruction.
     * @param value1 A long value (guaranteed by the JVM's semantics).
     * @param value2 A long value (guaranteed by the JVM's semantics).
     * @return The return value is the calculated value and/or (depending on the domain)
     *      an `ArithmeticException` if `value2` is `0`.
     */
    def ldiv(pc: PC, value1: DomainValue, value2: DomainValue): LongValueOrArithmeticException

    /**
     * ''Multiplication'' of two long values.
     *
     * @param pc The pc of the mul (/) instruction.
     * @param value1 A long value (guaranteed by the JVM's semantics).
     * @param value2 A long value (guaranteed by the JVM's semantics).
     */
    def lmul(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue

    /**
     * ''Remainder'' of two long values.
     *
     * @param pc The pc of the div (/) instruction.
     * @param value1 A long value (guaranteed by the JVM's semantics).
     * @param value2 A long value (guaranteed by the JVM's semantics).
     * @return The return value is the calculated value and/or (depending on the domain)
     *      an `ArithmeticException` if `value2` is `0`.
     */
    def lrem(pc: PC, value1: DomainValue, value2: DomainValue): LongValueOrArithmeticException

    /**
     * ''Boolean and'' of two long values.
     *
     * @param pc The pc of the "&" instruction.
     * @param value1 A long value (guaranteed by the JVM's semantics).
     * @param value2 A long value (guaranteed by the JVM's semantics).
     */
    def land(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue

    /**
     * ''Boolean or'' of two long values.
     *
     * @param pc The pc of the "boolean or" (|) instruction.
     * @param value1 A long value (guaranteed by the JVM's semantics).
     * @param value2 A long value (guaranteed by the JVM's semantics).
     */
    def lor(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue

    /**
     * ''xor'' of two long values.
     *
     * @param pc The pc of the "xor" instruction.
     * @param value1 A long value (guaranteed by the JVM's semantics).
     * @param value2 A long value (guaranteed by the JVM's semantics).
     */
    def lxor(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue

    /**
     * ''Shift left'' of a long value.
     *
     * @param pc The pc of the "shift left" instruction.
     * @param value1 A long value (guaranteed by the JVM's semantics).
     * @param value2 A int value (guaranteed by the JVM's semantics) that determines
     *      the number of bits to shift.
     */
    def lshl(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue

    /**
     * ''Shift right'' of a long value.
     *
     * @param pc The pc of the "shift right" instruction.
     * @param value1 A long value (guaranteed by the JVM's semantics).
     * @param value2 An int value (guaranteed by the JVM's semantics) that determines
     *      the number of bits to shift.
     */
    def lshr(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue

    /**
     * ''Unsigned shift right'' of a long value.
     *
     * @param pc The pc of the "unsigned shift right" instruction.
     * @param value1 A long value (guaranteed by the JVM's semantics).
     * @param value2 A int value (guaranteed by the JVM's semantics) that determines
     *      the number of bits to shift.
     */
    def lushr(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue

}
