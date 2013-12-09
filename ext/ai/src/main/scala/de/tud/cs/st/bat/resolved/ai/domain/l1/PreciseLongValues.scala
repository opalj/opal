/* License (BSD Style License):
 * Copyright (c) 2009 - 2013
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
 *  - Neither the name of the Software Technology Group or Technische
 *    Universität Darmstadt nor the names of its contributors may be used to
 *    endorse or promote products derived from this software without specific
 *    prior written permission.
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
package de.tud.cs.st
package bat
package resolved
package ai
package domain
package l1

import de.tud.cs.st.util.{ Answer, Yes, No, Unknown }

/**
 * Domain to track long values at a configurable level of precision.
 *
 * @author Riadh Chtara
 */
trait PreciseLongValues[+I] extends Domain[I] {

    // -----------------------------------------------------------------------------------
    //
    // REPRESENTATION OF LONG LIKE VALUES
    //
    // -----------------------------------------------------------------------------------

    /**
     * Determines for an long value that is updated how large the update can be
     * before we stop the precise tracking of the value and represent the respective
     * value as "some long value".
     *
     * '''This value is only taken into consideration when two paths converge'''.
     *
     * The default value is 25 which will, e.g., effectively unroll a loop with a loop
     * counter that starts with 0 and which is incremented by one in each round up to
     * 25 times.
     *
     * This is a runtime configurable setting that may affect the overall precision of
     * subsequent analyses that require knowledge about longs.
     */
    def maxSpreadLong = 25

    protected def spread(a: Long, b: Long) = Math.abs(a - b)

    /**
     * Determines if an exception is thrown in case of a '''potential''' division by zero.
     * I.e., this setting controls whether we throw a division by zero exception if we
     * know nothing about the concrete value of the denominator or not.
     * However, if we know that the denominator is 0 a corresponding exception will be
     * thrown.
     */
    def divisionByZeroIfUnknownLong = true

    /**
     * Abstracts over all values with computational type `long`.
     */
    sealed trait LongLikeValue extends Value { this: DomainValue ⇒

        final def computationalType: ComputationalType = ComputationalTypeLong

    }

    /**
     * Represents a specific, but unknown long value.
     *
     * Models the top value of this domain's lattice.
     */
    trait AnLongValue extends LongLikeValue { this: DomainValue ⇒ }

    /**
     * Represents a concrete long value.
     */
    trait LongValue extends LongLikeValue { this: DomainValue ⇒

        val initial: Long

        val value: Long

        /**
         * Creates a new LongValue with the given value as the current value,
         * but the same initial value. Please note that it is ok if the new value
         * is between the current value and the initial value. It is only required
         * that the join operation is monotonic.
         *
         * @note
         * This method must not check whether the initial value and the new value
         * exceed the spread. This is done by the join method.
         */
        def update(newValue: Long): DomainValue

    }

    abstract override def typeOfValue(value: DomainValue): TypesAnswer =
        value match {
            case longLikeValueegerLikeValue: LongLikeValue ⇒ IsLongValue
            case _                                  ⇒ super.typeOfValue(value)
        }

    //
    // QUESTION'S ABOUT VALUES
    //

    def getLongValue[T](value: DomainValue)(f: Long ⇒ T)(orElse: ⇒ T): T =
        value match {
            case v: LongValue ⇒ f(v.value)
            case _               ⇒ orElse
        }

    def getLongValues[T](
        value1: DomainValue,
        value2: DomainValue)(
            f: (Long, Long) ⇒ T)(orElse: ⇒ T): T =
        getLongValue(value1) { v1 ⇒ getLongValue(value2) { v2 ⇒ f(v1, v2) }(orElse) } {
            orElse
        }

    abstract override def areEqual(value1: DomainValue, value2: DomainValue): Answer =
        getLongValues(value1, value2) {(v1, v2) ⇒ Answer(v1 == v2) } { Unknown }

    def isSomeValueInRange(
        value: DomainValue,
        lowerBound: Long,
        upperBound: Long): Boolean =
        getLongValue(value) { v ⇒ lowerBound <= v && v <= upperBound } { true }

    def isSomeValueNotInRange(
        value: DomainValue,
        lowerBound: Long,
        upperBound: Long): Boolean =
        getLongValue(value) { v ⇒
            v < lowerBound || v > upperBound
        } {
            !(lowerBound == Long.MinValue && upperBound == Long.MaxValue)
        }

    abstract override def isLessThan(
        smallerValue: DomainValue,
        largerValue: DomainValue): Answer =
        getLongValues(smallerValue, largerValue) { (v1, v2) ⇒
            Answer(v1 < v2)
        } { Unknown }

    abstract override def isLessThanOrEqualTo(
        smallerOrEqualValue: DomainValue,
        equalOrLargerValue: DomainValue): Answer =
        getLongValues(smallerOrEqualValue, equalOrLargerValue) { (v1, v2) ⇒
            Answer(v1 <= v2)
        } { Unknown }

    def updateValueLong(
        oldValue: DomainValue,
        newValue: DomainValue,
        operands: Operands,
        locals: Locals): (Operands, Locals) =
        (
            operands.map { operand ⇒ if (operand eq oldValue) newValue else operand },
            locals.map { local ⇒ if (local eq oldValue) newValue else local }
        )

    def establishValue(
        pc: PC,
        theValue: Long,
        value: DomainValue,
        operands: Operands,
        locals: Locals): (Operands, Locals) =
        updateValueLong(value, LongValue(pc, theValue), operands, locals)

    abstract override def establishAreEqual(
        pc: PC,
        value1: DomainValue,
        value2: DomainValue,
        operands: Operands,
        locals: Locals): (Operands, Locals) = {
        getLongValue(value1) { v1 ⇒
            updateValueLong(value2, LongValue(pc, v1), operands, locals)
        } {
            getLongValue(value2) { v2 ⇒
                updateValueLong(value1, LongValue(pc, v2), operands, locals)
            } {
                (operands, locals)
            }
        }
    }

    // -----------------------------------------------------------------------------------
    //
    // HANDLING OF COMPUTATIONS
    //
    // -----------------------------------------------------------------------------------

    //
    // RELATIONAL OPERATORS
    //
    override def lcmp(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        IntegerValue(pc)
    //
    // UNARY EXPRESSIONS
    //
    override def lneg(pc: PC, value: DomainValue) = value match {
        case v: LongValue ⇒ v.update(-v.value)
        case _               ⇒ value
    }

    //
    // BINARY EXPRESSIONS
    //

    override def ladd(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        getLongValues(value1, value2) { (v1, v2) ⇒
            LongValue(pc, v1 + v2)
        } {
            LongValue(pc)
        }

    override def land(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        getLongValues(value1, value2) { (v1, v2) ⇒
            LongValue(pc, v1 & v2)
        } {
            LongValue(pc)
        }

    override def ldiv(pc: PC, value1: DomainValue, value2: DomainValue): Computation[DomainValue, DomainValue] =
        getLongValues(value1, value2) { (v1, v2) ⇒
            if (v2 == 0)
                ThrowsException(InitializedObject(pc, ObjectType.ArithmeticException))
            else
                ComputedValue(LongValue(pc, v1 / v2))
        } {
            if (divisionByZeroIfUnknownLong)
                ComputedValueAndException(
                    LongValue(pc),
                    InitializedObject(pc, ObjectType.ArithmeticException))
            else
                ComputedValue(LongValue(pc))
        }

    override def lmul(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        getLongValues(value1, value2) { (v1, v2) ⇒
            LongValue(pc, v1 * v2)
        } {
            LongValue(pc)
        }

    override def lor(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        getLongValues(value1, value2) { (v1, v2) ⇒
            LongValue(pc, v1 | v2)
        } {
            LongValue(pc)
        }

    override def lrem(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        getLongValues(value1, value2) { (v1, v2) ⇒
            LongValue(pc, v1 % v2)
        } {
            LongValue(pc)
        }

    override def lshl(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        getLongValues(value1, value2) { (v1, v2) ⇒
            LongValue (pc, v1 << v2)            
        } {
            LongValue(pc)
        }

    override def lshr(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        getLongValues(value1, value2) { (v1, v2) ⇒
            LongValue(pc, v1 >> v2)
        } {
            LongValue(pc)
        }

    override def lsub(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        getLongValues(value1, value2) { (v1, v2) ⇒
            LongValue(pc, v1 - v2)
        } {
            LongValue(pc)
        }

    override def lushr(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        getLongValues(value1, value2) { (v1, v2) ⇒
            LongValue(pc, v1 >>> v2)
        }(LongValue(pc))

    override def lxor(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        getLongValues(value1, value2) { (v1, v2) ⇒
            LongValue(pc, v1 ^ v2)
        } {
            LongValue(pc)
        }

    def linc(pc: PC, value: DomainValue, increment: Long) =
        value match {
            case v: LongValue ⇒ v.update(v.value + increment)
            case _               ⇒ value
        }

    //
    // TYPE CONVERSION INSTRUCTIONS
    //

    override def l2d(pc: PC, value: DomainValue): DomainValue = DoubleValue(pc)
    override def l2f(pc: PC, value: DomainValue): DomainValue = FloatValue(pc)
    override def l2i(pc: PC, value: DomainValue): DomainValue = IntegerValue(pc)
}

