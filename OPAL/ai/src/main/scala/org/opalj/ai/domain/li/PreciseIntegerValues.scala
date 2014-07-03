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

import org.opalj.util.{ Answer, Yes, No, Unknown }

import org.opalj.br.{ ComputationalType, ComputationalTypeInt }

/**
 * Domain to track integer values at a configurable level of precision.
 *
 * @author Michael Eichberg
 */
trait PreciseIntegerValues extends Domain with ConcreteIntegerValues {
    this: Configuration ⇒

    // -----------------------------------------------------------------------------------
    //
    // REPRESENTATION OF INTEGER LIKE VALUES
    //
    // -----------------------------------------------------------------------------------

    /**
     * Determines how often a value can be update before we assume the value to be
     * AnIntegerValue. The number of updates is determined by joins of incompatible
     * values; simple mathematical operations are not counted as updates.
     */
    protected def maxUpdatesForIntegerValues: Long

    /**
     * Abstracts over all values with computational type `integer`.
     */
    sealed trait IntegerLikeValue extends Value with IsIntegerValue { this: DomainValue ⇒

        final def computationalType: ComputationalType = ComputationalTypeInt

    }

    /**
     * Represents an (unknown) integer value.
     *
     * Models the top value of this domain's lattice.
     */
    trait AnIntegerValue extends IntegerLikeValue { this: DomainValue ⇒ }

    /**
     * Represents a concrete integer value.
     */
    trait IntegerValue extends IntegerLikeValue { this: DomainValue ⇒

        val updateCount: Int

        val value: Int

        /**
         * Creates a new `IntegerValue` with the given value as the current value,
         * and an increased updateCount.
         */
        def update(newValue: Int): DomainValue

    }

    object IntegerValue {
        def unapply(v: IntegerValue): Option[Int] = Some(v.value)
    }

    protected[this] def updateValue(
        oldValue: DomainValue,
        newValue: DomainValue,
        operands: Operands,
        locals: Locals): (Operands, Locals) =
        (
            operands.map { operand ⇒ if (operand eq oldValue) newValue else operand },
            locals.map { local ⇒ if (local eq oldValue) newValue else local }
        )

    //
    // QUESTION'S ABOUT VALUES
    //

    @inline final override def intValue[T](
        value: DomainValue)(
            f: Int ⇒ T)(
                orElse: ⇒ T): T =
        value match {
            case v: IntegerValue ⇒ f(v.value)
            case _               ⇒ orElse
        }

    @inline final override def intValueOption(value: DomainValue): Option[Int] =
        value match {
            case v: IntegerValue ⇒ Some(v.value)
            case _               ⇒ None
        }

    @inline protected final def intValues[T](
        value1: DomainValue,
        value2: DomainValue)(
            f: (Int, Int) ⇒ T)(
                orElse: ⇒ T): T =
        intValue(value1) { v1 ⇒
            intValue(value2) { v2 ⇒
                f(v1, v2)
            } {
                orElse
            }
        } {
            orElse
        }

    override def intAreEqual(value1: DomainValue, value2: DomainValue): Answer =
        intValues(value1, value2) { (v1, v2) ⇒ Answer(v1 == v2) } { Unknown }

    override def intIsSomeValueInRange(
        value: DomainValue,
        lowerBound: Int,
        upperBound: Int): Answer = {
        if (lowerBound == Int.MinValue && upperBound == Int.MaxValue)
            return Yes

        intValue(value) { v ⇒
            Answer(lowerBound <= v && v <= upperBound)
        } {
            Unknown
        }
    }

    override def intIsSomeValueNotInRange(
        value: DomainValue,
        lowerBound: Int,
        upperBound: Int): Answer = {
        if (lowerBound == Int.MinValue && upperBound == Int.MaxValue)
            return No

        intValue(value) { v ⇒
            Answer(v < lowerBound || v > upperBound)
        } {
            Unknown
        }
    }

    override def intIsLessThan(
        smallerValue: DomainValue,
        largerValue: DomainValue): Answer =
        intValues(smallerValue, largerValue) { (v1, v2) ⇒
            Answer(v1 < v2)
        } { Unknown }

    override def intIsLessThanOrEqualTo(
        smallerOrEqualValue: DomainValue,
        equalOrLargerValue: DomainValue): Answer =
        intValues(smallerOrEqualValue, equalOrLargerValue) { (v1, v2) ⇒
            Answer(v1 <= v2)
        } { Unknown }

    override def intEstablishValue(
        pc: PC,
        theValue: Int,
        value: DomainValue,
        operands: Operands,
        locals: Locals): (Operands, Locals) =
        updateValue(value, IntegerValue(pc, theValue), operands, locals)

    override def intEstablishAreEqual(
        pc: PC,
        value1: DomainValue,
        value2: DomainValue,
        operands: Operands,
        locals: Locals): (Operands, Locals) = {
        intValue(value1) { v1 ⇒
            updateValue(value2, IntegerValue(pc, v1), operands, locals)
        } {
            intValue(value2) { v2 ⇒
                updateValue(value1, IntegerValue(pc, v2), operands, locals)
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
    // UNARY EXPRESSIONS
    //
    override def ineg(pc: PC, value: DomainValue) = value match {
        case v: IntegerValue ⇒ IntegerValue(pc, -v.value)
        case _               ⇒ value
    }

    //
    // BINARY EXPRESSIONS
    //

    override def iadd(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        intValues(value1, value2) { (v1, v2) ⇒
            IntegerValue(pc, v1 + v2)
        } {
            IntegerValue(pc)
        }

    override def iand(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        intValues(value1, value2) { (v1, v2) ⇒
            IntegerValue(pc, v1 & v2)
        } {
            IntegerValue(pc)
        }

    override def idiv(
        pc: PC,
        value1: DomainValue,
        value2: DomainValue): IntegerValueOrArithmeticException = {
        intValue(value2) { v2 ⇒
            if (v2 == 0)
                ThrowsException(ArithmeticException(pc))
            else {
                intValue(value1) { v1 ⇒
                    ComputedValue(IntegerValue(pc, v1 / v2))
                } {
                    ComputedValue(IntegerValue(pc))
                }
            }
        } {
            if (throwArithmeticExceptions)
                ComputedValueOrException(IntegerValue(pc), ArithmeticException(pc))
            else
                ComputedValue(IntegerValue(pc))
        }
    }

    override def imul(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        intValues(value1, value2) { (v1, v2) ⇒
            IntegerValue(pc, v1 * v2)
        } {
            IntegerValue(pc)
        }

    override def ior(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        intValues(value1, value2) { (v1, v2) ⇒
            IntegerValue(pc, v1 | v2)
        } {
            IntegerValue(pc)
        }

    override def irem(
        pc: PC,
        value1: DomainValue,
        value2: DomainValue): IntegerValueOrArithmeticException = {
        intValue(value2) { v2 ⇒
            if (v2 == 0)
                ThrowsException(ArithmeticException(pc))
            else {
                intValue(value1) { v1 ⇒
                    ComputedValue(IntegerValue(pc, v1 % v2))
                } {
                    ComputedValue(IntegerValue(pc))
                }
            }
        } {
            if (throwArithmeticExceptions)
                ComputedValueOrException(
                    IntegerValue(pc), ArithmeticException(pc))
            else
                ComputedValue(IntegerValue(pc))
        }
    }

    override def ishl(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        intValues(value1, value2) { (v1, v2) ⇒
            IntegerValue(pc, v1 << v2)
        } {
            IntegerValue(pc)
        }

    override def ishr(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        intValues(value1, value2) { (v1, v2) ⇒
            IntegerValue(pc, v1 >> v2)
        } {
            IntegerValue(pc)
        }

    override def isub(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        intValues(value1, value2) { (v1, v2) ⇒
            IntegerValue(pc, v1 - v2)
        } {
            IntegerValue(pc)
        }

    override def iushr(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        intValues(value1, value2) { (v1, v2) ⇒
            IntegerValue(pc, v1 >>> v2)
        } {
            IntegerValue(pc)
        }

    override def ixor(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        intValues(value1, value2) { (v1, v2) ⇒
            IntegerValue(pc, v1 ^ v2)
        } {
            IntegerValue(pc)
        }

    override def iinc(pc: PC, value: DomainValue, increment: Int): DomainValue =
        value match {
            case v: IntegerValue ⇒
                IntegerValue(pc, v.value + increment)
            case _ ⇒
                // The given value is "some (unknown) integer value"
                // hence, we can directly return it.
                value
        }

    //
    // TYPE CONVERSION INSTRUCTIONS
    //

    override def i2b(pc: PC, value: DomainValue): DomainValue =
        intValue(value)(v ⇒ ByteValue(pc, v.toByte))(ByteValue(pc))

    override def i2c(pc: PC, value: DomainValue): DomainValue =
        intValue(value)(v ⇒ CharValue(pc, v.toChar))(CharValue(pc))

    override def i2s(pc: PC, value: DomainValue): DomainValue =
        intValue(value)(v ⇒ ShortValue(pc, v.toShort))(ShortValue(pc))

    override def i2d(pc: PC, value: DomainValue): DomainValue =
        intValue(value)(v ⇒ DoubleValue(pc, v.toDouble))(DoubleValue(pc))

    override def i2f(pc: PC, value: DomainValue): DomainValue =
        intValue(value)(v ⇒ FloatValue(pc, v.toFloat))(FloatValue(pc))

    override def i2l(pc: PC, value: DomainValue): DomainValue =
        intValue(value)(v ⇒ LongValue(pc, v.toLong))(LongValue(pc))
}

