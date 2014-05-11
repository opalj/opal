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
package domain
package l1

import org.opalj.util.{ Answer, Yes, No, Unknown }

import br._

/**
 * Domain to track long values at a configurable level of precision.
 *
 * @author Riadh Chtara
 * @author Michael Eichberg
 */
trait PreciseLongValues extends Domain with Configuration {

    // -----------------------------------------------------------------------------------
    //
    // REPRESENTATION OF LONG LIKE VALUES
    //
    // -----------------------------------------------------------------------------------

    /**
     * Determines for a long value that is updated how large the update can be
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
    def maxSpreadLong: Long = 25l

    protected def spread(a: Long, b: Long): Long = Math.abs(a - b)

    /**
     * Abstracts over all values with computational type `long`.
     */
    sealed trait LongLikeValue
            extends Value
            with IsLongValue { this: DomainValue ⇒

        final def computationalType: ComputationalType = ComputationalTypeLong

    }

    /**
     * Represents a specific, but unknown long value.
     *
     * Models the top value of this domain's lattice.
     */
    trait ALongValue extends LongLikeValue { this: DomainValue ⇒ }

    /**
     * Represents a concrete long value.
     */
    trait LongValue extends LongLikeValue { this: DomainValue ⇒

        val initial: Long

        val value: Long

    }

    //
    // QUESTION'S ABOUT VALUES
    //

    def withLongValueOrElse[T](value: DomainValue)(f: Long ⇒ T)(orElse: ⇒ T): T =
        value match {
            case v: LongValue ⇒ f(v.value)
            case _            ⇒ orElse
        }

    def withLongValuesOrElse[T](
        value1: DomainValue,
        value2: DomainValue)(
            f: (Long, Long) ⇒ T)(orElse: ⇒ T): T =
        withLongValueOrElse(value1) { v1 ⇒
            withLongValueOrElse(value2) { v2 ⇒
                f(v1, v2)
            } {
                orElse
            }
        } {
            orElse
        }

    def longIsSomeValueInRange(
        value: DomainValue,
        lowerBound: Long,
        upperBound: Long): Boolean =
        withLongValueOrElse(value) {
            v ⇒ lowerBound <= v && v <= upperBound
        } {
            true
        }

    def longIsSomeValueNotInRange(
        value: DomainValue,
        lowerBound: Long,
        upperBound: Long): Boolean =
        withLongValueOrElse(value) { v ⇒
            v < lowerBound || v > upperBound
        } {
            !(lowerBound == Long.MinValue && upperBound == Long.MaxValue)
        }

    def longIsLessThan(
        smallerValue: DomainValue,
        largerValue: DomainValue): Answer =
        withLongValuesOrElse(smallerValue, largerValue) { (v1, v2) ⇒
            Answer(v1 < v2)
        } {
            Unknown
        }

    def longIsLessThanOrEqualTo(
        smallerOrEqualValue: DomainValue,
        equalOrLargerValue: DomainValue): Answer =
        withLongValuesOrElse(smallerOrEqualValue, equalOrLargerValue) { (v1, v2) ⇒
            Answer(v1 <= v2)
        } {
            Unknown
        }

    // TODO Does this make sense?
    def longUpdateValue(
        oldValue: DomainValue,
        newValue: DomainValue,
        operands: Operands,
        locals: Locals): (Operands, Locals) =
        (
            operands.map { operand ⇒ if (operand eq oldValue) newValue else operand },
            locals.map { local ⇒ if (local eq oldValue) newValue else local }
        )

    // -----------------------------------------------------------------------------------
    //
    // HANDLING OF COMPUTATIONS
    //
    // -----------------------------------------------------------------------------------

    //
    // RELATIONAL OPERATORS
    //

    override def lcmp(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        withLongValuesOrElse(value1, value2) { (v1, v2) ⇒
            if (v1 > v2) IntegerValue(pc, 1)
            else if (v1 == v2) IntegerValue(pc, 0)
            else IntegerValue(pc, -1)
        } {
            IntegerValue(pc)
        }

    //
    // UNARY EXPRESSIONS
    //

    override def lneg(pc: PC, value: DomainValue) =
        withLongValueOrElse(value) { v ⇒
            LongValue(pc, -v)
        } {
            LongValue(pc)
        }

    //
    // BINARY EXPRESSIONS
    //

    def linc(pc: PC, value: DomainValue, increment: Long) =
        withLongValueOrElse(value) { v ⇒
            LongValue(pc, v + increment)
        } {
            LongValue(pc)
        }

    override def ldiv(
        pc: PC,
        value1: DomainValue,
        value2: DomainValue): IntegerLikeValueOrArithmeticException = {
        withLongValuesOrElse(value1, value2) { (v1, v2) ⇒
            if (v2 == 0)
                ThrowsException(InitializedObjectValue(pc, ObjectType.ArithmeticException))
            else
                ComputedValue(LongValue(pc, v1 / v2))
        } {
            if (throwArithmeticExceptions)
                ComputedValueAndException(
                    LongValue(pc),
                    InitializedObjectValue(pc, ObjectType.ArithmeticException))
            else
                ComputedValue(LongValue(pc))
        }
    }

    override def ladd(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        withLongValuesOrElse(value1, value2) { (v1, v2) ⇒
            LongValue(pc, v1 + v2)
        } {
            LongValue(pc)
        }

    override def land(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        withLongValuesOrElse(value1, value2) { (v1, v2) ⇒
            LongValue(pc, v1 & v2)
        } {
            LongValue(pc)
        }

    override def lmul(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        withLongValuesOrElse(value1, value2) { (v1, v2) ⇒
            LongValue(pc, v1 * v2)
        } {
            LongValue(pc)
        }

    override def lor(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        withLongValuesOrElse(value1, value2) { (v1, v2) ⇒
            LongValue(pc, v1 | v2)
        } {
            LongValue(pc)
        }

    override def lrem(
        pc: PC,
        value1: DomainValue,
        value2: DomainValue): IntegerLikeValueOrArithmeticException =
        withLongValuesOrElse(value1, value2) { (v1, v2) ⇒
            if (v2 == 0l)
                ThrowsException(InitializedObjectValue(pc, ObjectType.ArithmeticException))
            else
                ComputedValue(LongValue(pc, v1 % v2))
        } {
            if (throwArithmeticExceptions)
                ComputedValueAndException(
                    LongValue(pc),
                    InitializedObjectValue(pc, ObjectType.ArithmeticException))
            else
                ComputedValue(LongValue(pc))
        }

    override def lshl(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        withLongValuesOrElse(value1, value2) { (v1, v2) ⇒
            LongValue(pc, v1 << v2)
        } {
            LongValue(pc)
        }

    override def lshr(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        withLongValuesOrElse(value1, value2) { (v1, v2) ⇒
            LongValue(pc, v1 >> v2)
        } {
            LongValue(pc)
        }

    override def lsub(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        withLongValuesOrElse(value1, value2) { (v1, v2) ⇒
            LongValue(pc, v1 - v2)
        } {
            LongValue(pc)
        }

    override def lushr(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        withLongValuesOrElse(value1, value2) { (v1, v2) ⇒
            LongValue(pc, v1 >>> v2)
        }(LongValue(pc))

    override def lxor(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        withLongValuesOrElse(value1, value2) { (v1, v2) ⇒
            LongValue(pc, v1 ^ v2)
        } {
            LongValue(pc)
        }

    //
    // TYPE CONVERSION INSTRUCTIONS
    //

    override def l2d(pc: PC, value: DomainValue): DomainValue =
        withLongValueOrElse(value) { v ⇒ DoubleValue(pc, v.toDouble) } { DoubleValue(pc) }

    override def l2f(pc: PC, value: DomainValue): DomainValue =
        withLongValueOrElse(value) { v ⇒ FloatValue(pc, v.toFloat) } { FloatValue(pc) }

    override def l2i(pc: PC, value: DomainValue): DomainValue =
        withLongValueOrElse(value) { v ⇒ IntegerValue(pc, v.toInt) } { IntegerValue(pc) }
}

