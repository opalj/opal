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
package li

import org.opalj.br._

/**
 * Domain to track long values at a configurable level of precision.
 *
 * THIS DOMAIN IS CURRENTLY BUGGY AND SHOULD NOT BE USED
 *
 * @author Michael Eichberg
 * @author Riadh Chtara
 * @author David Becker
 */
trait PreciseLongValues extends LongValuesDomain with ConcreteLongValues {
    domain: IntegerValuesFactory with ExceptionsFactory with Configuration ⇒

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
    sealed trait LongValue
            extends Value
            with IsLongValue { this: DomainValue ⇒

        final def computationalType: ComputationalType = ComputationalTypeLong

    }

    /**
     * Represents a specific, but unknown long value.
     *
     * Models the top value of this domain's lattice.
     */
    trait ALongValue extends LongValue { this: DomainValue ⇒ }

    /**
     * Represents a concrete long value.
     */
    trait DefiniteLongValue extends LongValue { this: DomainValue ⇒

        val initial: Long

        val value: Long

    }

    object DefiniteLongValue {
        def unapply(v: DefiniteLongValue): Option[Long] =
            Some(v.value)
    }

    //
    // QUESTION'S ABOUT VALUES
    //

    def longValue[T](value: DomainValue)(f: Long ⇒ T)(orElse: ⇒ T): T =
        value match {
            case v: DefiniteLongValue ⇒ f(v.value)
            case _                    ⇒ orElse
        }

    def longValueOption(value: DomainValue): Option[Long] =
        value match {
            case v: DefiniteLongValue ⇒ Some(v.value)
            case _                    ⇒ None
        }

    def withLongValuesOrElse[T](
        value1: DomainValue,
        value2: DomainValue
    )(
        f: (Long, Long) ⇒ T
    )(orElse: ⇒ T): T =
        longValue(value1) { v1 ⇒
            longValue(value2) { v2 ⇒
                f(v1, v2)
            } {
                orElse
            }
        } {
            orElse
        }

    def longIsSomeValueInRange(
        value:      DomainValue,
        lowerBound: Long,
        upperBound: Long
    ): Boolean =
        longValue(value) {
            v ⇒ lowerBound <= v && v <= upperBound
        } {
            true
        }

    def longIsSomeValueNotInRange(
        value:      DomainValue,
        lowerBound: Long,
        upperBound: Long
    ): Boolean =
        longValue(value) { v ⇒
            v < lowerBound || v > upperBound
        } {
            !(lowerBound == Long.MinValue && upperBound == Long.MaxValue)
        }

    def longIsLessThan(
        smallerValue: DomainValue,
        largerValue:  DomainValue
    ): Answer =
        withLongValuesOrElse(smallerValue, largerValue) { (v1, v2) ⇒
            Answer(v1 < v2)
        } {
            Unknown
        }

    def longIsLessThanOrEqualTo(
        smallerOrEqualValue: DomainValue,
        equalOrLargerValue:  DomainValue
    ): Answer =
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
        locals:   Locals
    ): (Operands, Locals) =
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
    // UNARY EXPRESSIONS
    //

    override def lneg(pc: PC, value: DomainValue): DomainValue = value match {
        case v: DefiniteLongValue ⇒ LongValue(pc, -v.value)
        case _                    ⇒ LongValue(origin = pc)
    }

    //
    // RELATIONAL OPERATORS
    //

    override def lcmp(pc: PC, left: DomainValue, right: DomainValue): DomainValue = {
        (left, right) match {
            case (DefiniteLongValue(l), DefiniteLongValue(r)) ⇒
                if (l > r)
                    IntegerValue(pc, 1)
                else if (l == r)
                    IntegerValue(pc, 0)
                else
                    IntegerValue(pc, -1)
            case _ ⇒
                IntegerValue(origin = pc)
        }
    }

    //
    // BINARY EXPRESSIONS
    //

    def linc(pc: PC, value: DomainValue, increment: Long) = value match {
        case v: DefiniteLongValue ⇒ LongValue(pc, v.value + increment)
        case _                    ⇒ LongValue(origin = pc)
    }

    override def ladd(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue = {
        (value1, value2) match {
            case (DefiniteLongValue(l), DefiniteLongValue(r)) ⇒ LongValue(pc, l + r)
            case _                                            ⇒ LongValue(origin = pc)
        }
    }

    override def lsub(pc: PC, left: DomainValue, right: DomainValue): DomainValue = {
        (left, right) match {
            case (DefiniteLongValue(l), DefiniteLongValue(r)) ⇒ LongValue(pc, l - r)
            case _                                            ⇒ LongValue(origin = pc)
        }
    }

    override def lmul(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue = {
        (value1, value2) match {
            case (_, DefiniteLongValue(0))                    ⇒ value2
            case (_, DefiniteLongValue(1))                    ⇒ value1
            case (DefiniteLongValue(0), _)                    ⇒ value1
            case (DefiniteLongValue(1), _)                    ⇒ value2

            case (DefiniteLongValue(l), DefiniteLongValue(r)) ⇒ LongValue(pc, l * r)

            case _                                            ⇒ LongValue(origin = pc)
        }
    }

    override def ldiv(
        pc:     PC,
        value1: DomainValue,
        value2: DomainValue
    ): LongValueOrArithmeticException = {
        longValue(value2) { v2 ⇒
            if (v2 == 0)
                ThrowsException(VMArithmeticException(pc))
            else {
                longValue(value1) { v1 ⇒
                    ComputedValue(LongValue(pc, v1 / v2))
                } {
                    ComputedValue(LongValue(pc))
                }
            }
        } {
            if (throwArithmeticExceptions)
                ComputedValueOrException(LongValue(pc), VMArithmeticException(pc))
            else
                ComputedValue(LongValue(pc))
        }
    }

    override def lrem(
        pc:     PC,
        value1: DomainValue,
        value2: DomainValue
    ): LongValueOrArithmeticException =
        longValue(value2) { v2 ⇒
            if (v2 == 0l)
                ThrowsException(VMArithmeticException(pc))
            else {
                longValue(value1) { v1 ⇒
                    ComputedValue(LongValue(pc, v1 % v2))
                } {
                    ComputedValue(LongValue(pc))
                }
            }
        } {
            if (throwArithmeticExceptions)
                ComputedValueOrException(LongValue(pc), VMArithmeticException(pc))
            else
                ComputedValue(LongValue(pc))
        }

    override def land(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue = {
        (value1, value2) match {
            case (_, DefiniteLongValue(-1)) ⇒ value1
            case (_, DefiniteLongValue(0))  ⇒ value2
            case (DefiniteLongValue(-1), _) ⇒ value2
            case (DefiniteLongValue(0), _)  ⇒ value1

            case (DefiniteLongValue(l), DefiniteLongValue(r)) ⇒
                LongValue(pc, l & r)

            case _ ⇒ LongValue(origin = pc)
        }
    }

    override def lor(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue = {
        (value1, value2) match {
            case (_, DefiniteLongValue(-1)) ⇒ value2
            case (_, DefiniteLongValue(0))  ⇒ value1
            case (DefiniteLongValue(-1), _) ⇒ value1
            case (DefiniteLongValue(0), _)  ⇒ value2

            case (DefiniteLongValue(l), DefiniteLongValue(r)) ⇒
                LongValue(pc, l | r)

            case _ ⇒ LongValue(origin = pc)
        }
    }

    override def lshl(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue = {
        (value1, value2) match {
            case (DefiniteLongValue(l), DefiniteLongValue(s)) ⇒ LongValue(pc, l << s)
            case _                                            ⇒ LongValue(origin = pc)
        }
    }

    override def lshr(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue = {
        (value1, value2) match {
            case (DefiniteLongValue(l), DefiniteLongValue(s)) ⇒ LongValue(pc, l >> s)
            case _                                            ⇒ LongValue(origin = pc)
        }
    }

    override def lushr(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue = {
        (value1, value2) match {
            case (DefiniteLongValue(l), DefiniteLongValue(s)) ⇒ LongValue(pc, l >>> s)
            case _                                            ⇒ LongValue(origin = pc)
        }
    }

    override def lxor(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue = {
        (value1, value2) match {
            case (DefiniteLongValue(l), DefiniteLongValue(r)) ⇒ LongValue(pc, l ^ r)
            case _                                            ⇒ LongValue(origin = pc)
        }
    }
}

