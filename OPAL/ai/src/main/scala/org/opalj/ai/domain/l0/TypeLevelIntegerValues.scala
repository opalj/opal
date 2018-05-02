/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
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

import org.opalj.br.ComputationalType
import org.opalj.br.ComputationalTypeInt
import org.opalj.br.BooleanType
import org.opalj.br.CharType
import org.opalj.br.ByteType
import org.opalj.br.ShortType
import org.opalj.br.IntegerType
import org.opalj.br.CTIntType
import org.opalj.br.IntegerVariableInfo
import org.opalj.br.VerificationTypeInfo

/**
 * Domain that performs computations related to integer values at the type level.
 *
 * @author Michael Eichberg
 */
trait TypeLevelIntegerValues extends Domain { this: Configuration ⇒

    // -----------------------------------------------------------------------------------
    //
    // REPRESENTATION OF INTEGER LIKE VALUES
    //
    // -----------------------------------------------------------------------------------

    /**
     * Abstracts over values with computational type `integer`.
     */
    protected[this] trait ComputationalTypeIntegerValue[T <: CTIntType] extends TypedValue[T] {
        this: DomainTypedValue[T] ⇒

        final override def computationalType: ComputationalType = ComputationalTypeInt

        final override def verificationTypeInfo: VerificationTypeInfo = IntegerVariableInfo

        override def summarize(pc: Int): DomainValue = this

    }

    trait BooleanValue
        extends ComputationalTypeIntegerValue[BooleanType]
        with IsBooleanValue[BooleanValue] {
        this: DomainTypedValue[BooleanType] ⇒

        override def adapt(target: TargetDomain, vo: ValueOrigin): target.DomainValue = {
            target.BooleanValue(vo)
        }
    }

    trait ByteValue extends ComputationalTypeIntegerValue[ByteType] with IsByteValue[ByteValue] {
        this: DomainTypedValue[ByteType] ⇒

        override def adapt(target: TargetDomain, vo: ValueOrigin): target.DomainValue = {
            target.ByteValue(vo)
        }

    }

    trait CharValue
        extends ComputationalTypeIntegerValue[CharType]
        with IsCharValue[CharValue] {
        this: DomainTypedValue[CharType] ⇒

        override def adapt(target: TargetDomain, vo: ValueOrigin): target.DomainValue = {
            target.CharValue(vo)
        }

    }

    trait ShortValue
        extends ComputationalTypeIntegerValue[ShortType]
        with IsShortValue[ShortValue] {
        this: DomainTypedValue[ShortType] ⇒

        override def adapt(target: TargetDomain, vo: ValueOrigin): target.DomainValue = {
            target.ShortValue(vo)
        }

    }

    trait IntegerValue
        extends ComputationalTypeIntegerValue[IntegerType]
        with IsIntegerValue[IntegerValue] {
        this: DomainTypedValue[IntegerType] ⇒

        override def adapt(target: TargetDomain, vo: ValueOrigin): target.DomainValue = {
            target.IntegerValue(vo)
        }

    }

    //
    // QUESTION'S ABOUT VALUES
    //

    override def intAreEqual(pc: Int, value1: DomainValue, value2: DomainValue): Answer =
        Unknown

    override def intIsSomeValueInRange(
        pc:         Int,
        value:      DomainValue,
        lowerBound: Int,
        upperBound: Int
    ): Answer =
        Unknown

    override def intIsSomeValueNotInRange(
        pc:         Int,
        value:      DomainValue,
        lowerBound: Int,
        upperBound: Int
    ): Answer =
        Unknown

    override def intIsLessThan(
        pc:    Int,
        left:  DomainValue,
        right: DomainValue
    ): Answer =
        Unknown

    override def intIsLessThanOrEqualTo(
        pc:    Int,
        left:  DomainValue,
        right: DomainValue
    ): Answer =
        Unknown

    // -----------------------------------------------------------------------------------
    //
    // HANDLING OF COMPUTATIONS
    //
    // -----------------------------------------------------------------------------------

    //
    // UNARY EXPRESSIONS
    //

    override def ineg(pc: Int, value: DomainValue): DomainValue = IntegerValue(pc)

    //
    // BINARY EXPRESSIONS
    //

    override def iadd(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue =
        IntegerValue(pc)

    override def iand(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue =
        IntegerValue(pc)

    override def idiv(
        pc:    Int,
        left:  DomainValue,
        right: DomainValue
    ): IntegerValueOrArithmeticException = {
        if (throwArithmeticExceptions)
            ComputedValueOrException(IntegerValue(pc), VMArithmeticException(pc))
        else
            ComputedValue(IntegerValue(pc))
    }

    override def imul(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue =
        IntegerValue(pc)

    override def ior(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue =
        IntegerValue(pc)

    override def irem(
        pc:    Int,
        left:  DomainValue,
        right: DomainValue
    ): IntegerValueOrArithmeticException = {
        if (throwArithmeticExceptions)
            ComputedValueOrException(IntegerValue(pc), VMArithmeticException(pc))
        else
            ComputedValue(IntegerValue(pc))
    }

    override def ishl(pc: Int, left: DomainValue, right: DomainValue): DomainValue =
        IntegerValue(pc)

    override def ishr(pc: Int, left: DomainValue, right: DomainValue): DomainValue =
        IntegerValue(pc)

    override def isub(pc: Int, left: DomainValue, right: DomainValue): DomainValue =
        IntegerValue(pc)

    override def iushr(pc: Int, left: DomainValue, right: DomainValue): DomainValue =
        IntegerValue(pc)

    override def ixor(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue =
        IntegerValue(pc)

    override def iinc(pc: Int, left: DomainValue, right: Int): DomainValue =
        IntegerValue(pc)

    //
    // TYPE CONVERSION INSTRUCTIONS
    //
    override def i2b(pc: Int, value: DomainValue): DomainValue = ByteValue(pc)

    override def i2c(pc: Int, value: DomainValue): DomainValue = CharValue(pc)

    override def i2s(pc: Int, value: DomainValue): DomainValue = ShortValue(pc)

}

