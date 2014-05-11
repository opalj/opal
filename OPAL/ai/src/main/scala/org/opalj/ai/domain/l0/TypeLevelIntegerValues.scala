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
package l0

import org.opalj.util.{ Answer, Yes, No, Unknown }

import br._

/**
 * Domain that performs computations related to integer values at the type level.
 *
 * @author Michael Eichberg
 */
trait TypeLevelIntegerValues extends Domain {

    // -----------------------------------------------------------------------------------
    //
    // REPRESENTATION OF INTEGER LIKE VALUES
    //
    // -----------------------------------------------------------------------------------

    /**
     * Abstracts over values with computational type `integer`.
     */
    trait ComputationalTypeIntegerValue extends Value { this: DomainValue ⇒

        override final def computationalType: ComputationalType = ComputationalTypeInt

        override def summarize(pc: PC): DomainValue = this

    }

    trait BooleanValue
        extends ComputationalTypeIntegerValue
        with IsBooleanValue { this: DomainValue ⇒ }

    trait ByteValue
        extends ComputationalTypeIntegerValue
        with IsByteValue { this: DomainValue ⇒ }

    trait CharValue
        extends ComputationalTypeIntegerValue
        with IsCharValue { this: DomainValue ⇒ }

    trait ShortValue
        extends ComputationalTypeIntegerValue
        with IsShortValue { this: DomainValue ⇒ }

    trait IntegerValue
        extends ComputationalTypeIntegerValue
        with IsIntegerValue { this: DomainValue ⇒ }

    //
    // QUESTION'S ABOUT VALUES
    //

    override def intAreEqual(value1: DomainValue, value2: DomainValue): Answer = Unknown

    override def intIsSomeValueInRange(
        value: DomainValue,
        lowerBound: Int,
        upperBound: Int): Answer =
        Unknown

    override def intIsSomeValueNotInRange(
        value: DomainValue,
        lowerBound: Int,
        upperBound: Int): Answer =
        Unknown

    override def intIsLessThan(
        smallerValue: DomainValue,
        largerValue: DomainValue): Answer =
        Unknown

    override def intIsLessThanOrEqualTo(
        smallerOrEqualValue: DomainValue,
        equalOrLargerValue: DomainValue): Answer =
        Unknown

    // -----------------------------------------------------------------------------------
    //
    // HANDLING OF COMPUTATIONS
    //
    // -----------------------------------------------------------------------------------

    //
    // UNARY EXPRESSIONS
    //
    override def ineg(pc: PC, value: DomainValue) = IntegerValue(pc)

    //
    // BINARY EXPRESSIONS
    //

    override def iadd(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        IntegerValue(pc)

    override def iand(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        IntegerValue(pc)

    override def idiv(
        pc: PC,
        value1: DomainValue,
        value2: DomainValue): IntegerLikeValueOrArithmeticException =
        ComputedValue(IntegerValue(pc))

    override def imul(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        IntegerValue(pc)

    override def ior(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        IntegerValue(pc)

    override def irem(
        pc: PC,
        value1: DomainValue,
        value2: DomainValue): IntegerLikeValueOrArithmeticException =
        ComputedValue(IntegerValue(pc))

    override def ishl(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        IntegerValue(pc)

    override def ishr(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        IntegerValue(pc)

    override def isub(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        IntegerValue(pc)

    override def iushr(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        IntegerValue(pc)

    override def ixor(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        IntegerValue(pc)

    override def iinc(pc: PC, value: DomainValue, increment: Int) = IntegerValue(pc)

    //
    // TYPE CONVERSION INSTRUCTIONS
    //

    override def i2b(pc: PC, value: DomainValue): DomainValue = ByteValue(pc)
    override def i2c(pc: PC, value: DomainValue): DomainValue = CharValue(pc)
    override def i2d(pc: PC, value: DomainValue): DomainValue = DoubleValue(pc)
    override def i2f(pc: PC, value: DomainValue): DomainValue = FloatValue(pc)
    override def i2l(pc: PC, value: DomainValue): DomainValue = LongValue(pc)
    override def i2s(pc: PC, value: DomainValue): DomainValue = ShortValue(pc)

}

