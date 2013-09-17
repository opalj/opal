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

import de.tud.cs.st.util.{ Answer, Yes, No, Unknown }

trait PreciseIntegerValues[I] extends Domain[I] {

    // -----------------------------------------------------------------------------------
    //
    // REPRESENTATION OF INTEGER LIKE VALUES
    //
    // -----------------------------------------------------------------------------------

    /**
     * Abstracts over all values with computational type `integer` and also
     * represents Integer values.
     */
    trait IntegerLikeValue extends Value {

        final def computationalType: ComputationalType = ComputationalTypeInt

        final def types: TypesAnswer[_] = typesAnswerIntegerLike
    }

    trait IntegerValueRange extends IntegerLikeValue {
        val min: Int
        val max: Int
    }

    trait IntegerValue extends IntegerLikeValue {
        val value: Int
    }

    private val typesAnswerIntegerLike: IsPrimitiveType = IsPrimitiveType(IntegerType)

    abstract override def types(value: DomainValue): TypesAnswer[_] = value match {
        case integerLikeValue: IntegerLikeValue ⇒ integerLikeValue.types
        case _                                  ⇒ super.types(value)
    }

    //
    // QUESTION'S ABOUT VALUES
    //

    def areEqual(value1: DomainValue, value2: DomainValue): Answer = Unknown

    def isSomeValueInRange(
        value: DomainValue,
        lowerBound: Int,
        upperBound: Int): Boolean =
        true

    def isSomeValueNotInRange(
        value: DomainValue,
        lowerBound: Int,
        upperBound: Int): Boolean =
        true

    def isLessThan(
        smallerValue: DomainValue,
        largerValue: DomainValue): Answer = {
        Unknown
    }

    def isLessThanOrEqualTo(
        smallerOrEqualValue: DomainValue,
        equalOrLargerValue: DomainValue): Answer = {
        Unknown
    }

    override def establishValue(pc: Int,
                                theValue: Int,
                                value: DomainValue,
                                operands: Operands,
                                locals: Locals): (Operands, Locals) =
        (operands, locals)

    override def establishAreEqual(pc: Int,
                                   value1: DomainValue,
                                   value2: DomainValue,
                                   operands: Operands,
                                   locals: Locals): (Operands, Locals) =
        (operands, locals)

    override def establishAreNotEqual(pc: Int,
                                      value1: DomainValue,
                                      value2: DomainValue,
                                      operands: Operands,
                                      locals: Locals): (Operands, Locals) =
        (operands, locals)

    override def establishIsLessThan(pc: Int,
                                     value1: DomainValue,
                                     value2: DomainValue,
                                     operands: Operands,
                                     locals: Locals): (Operands, Locals) =
        (operands, locals)

    override def establishIsLessThanOrEqualTo(pc: Int,
                                              value1: DomainValue,
                                              value2: DomainValue,
                                              operands: Operands,
                                              locals: Locals): (Operands, Locals) =
        (operands, locals)

    // -----------------------------------------------------------------------------------
    //
    // HANDLING OF COMPUTATIONS
    //
    // -----------------------------------------------------------------------------------

    //
    // UNARY EXPRESSIONS
    //
    def ineg(pc: Int, value: DomainValue) = newIntegerValue

    //
    // BINARY EXPRESSIONS
    //

    def iadd(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = newIntegerValue
    def iand(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = newIntegerValue
    def idiv(pc: Int, value1: DomainValue, value2: DomainValue) = ComputedValue(newIntegerValue)
    def imul(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = newIntegerValue
    def ior(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = newIntegerValue
    def irem(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = newIntegerValue
    def ishl(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = newIntegerValue
    def ishr(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = newIntegerValue
    def isub(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = newIntegerValue
    def iushr(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = newIntegerValue
    def ixor(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = newIntegerValue

    def iinc(pc: Int, value: DomainValue, increment: Int) = newIntegerValue

    //
    // TYPE CONVERSION INSTRUCTIONS
    //

    def i2b(pc: Int, value: DomainValue): DomainValue = newByteValue
    def i2c(pc: Int, value: DomainValue): DomainValue = newCharValue
    def i2d(pc: Int, value: DomainValue): DomainValue = newDoubleValue
    def i2f(pc: Int, value: DomainValue): DomainValue = newFloatValue
    def i2l(pc: Int, value: DomainValue): DomainValue = newLongValue
    def i2s(pc: Int, value: DomainValue): DomainValue = newShortValue

}

trait DefaultPreciseIntegerValues[I]
        extends DefaultValueBinding[I]
        with PreciseIntegerValues[I] {

    case object IntegerLikeValue extends super.IntegerLikeValue {
        override def merge(pc: Int, value: DomainValue): Update[DomainValue] = value match {
            case _: IntegerLikeValue ⇒ NoUpdate
            case other               ⇒ MetaInformationUpdateIllegalValue
        }
    }

    private def newIntegerValueRange(min1: Int, min2: Int, max1: Int, max2: Int) = {
        val newMin = Math.min(min1, min2)
        val newMax = Math.max(max1, max2)
        if (newMin == Int.MinValue && newMax == Int.MaxValue)
            StructuralUpdate(IntegerLikeValue)
        else
            StructuralUpdate(IntegerValueRange(newMin, newMax))
    }

    /**
     * Both bounds are inclusive.
     */
    case class IntegerValueRange(
        min: Int,
        max: Int)
            extends super.IntegerValueRange {

        override def merge(pc: Int, value: DomainValue): Update[DomainValue] =
            value match {
                case IntegerLikeValue ⇒ StructuralUpdate(value)
                case IntegerValueRange(otherMin, otherMax) ⇒
                    if (this.min <= otherMin && this.max >= otherMax)
                        NoUpdate
                    else
                        newIntegerValueRange(this.min, otherMin, this.max, otherMax)
                case IntegerValue(value) ⇒
                    if (value >= min && value <= max)
                        NoUpdate
                    else
                        newIntegerValueRange(this.min, value, this.max, value)
                case other ⇒ MetaInformationUpdateIllegalValue
            }
    }

    case class IntegerValue(
        value: Int)
            extends super.IntegerValue {

        override def merge(pc: Int, value: DomainValue): Update[DomainValue] =
            value match {
                case IntegerLikeValue ⇒ StructuralUpdate(value)
                case IntegerValueRange(otherMin, otherMax) ⇒
                    newIntegerValueRange(this.value, otherMin, this.value, otherMax)
                case IntegerValue(otherValue) ⇒
                    if (this.value == otherValue)
                        NoUpdate
                    else
                        newIntegerValueRange(this.value, otherValue, this.value, otherValue)
                case other ⇒ MetaInformationUpdateIllegalValue
            }
    }

    private val value0 = IntegerValue(0) // <=> false
    private val value1 = IntegerValue(1) // <=> true
    private val range0_1 = IntegerValueRange(0, 1)
    private val byteRange = IntegerValueRange(Byte.MinValue, Byte.MaxValue)
    private val shortRange = IntegerValueRange(Short.MinValue, Short.MaxValue)
    private val charRange = IntegerValueRange(Char.MinValue, Char.MaxValue)

    def newBooleanValue(): DomainValue = range0_1
    def newBooleanValue(pc: Int): DomainValue = range0_1
    def newBooleanValue(pc: Int, value: Boolean): DomainValue =
        if (value) value1 else value0

    def newByteValue() = byteRange
    def newByteValue(pc: Int, value: Byte) = IntegerValue(value)

    def newShortValue() = shortRange
    def newShortValue(pc: Int, value: Short) = IntegerValue(value)

    def newCharValue() = charRange
    def newCharValue(pc: Int, value: Byte) = IntegerValue(value)

    def newIntegerValue() = IntegerLikeValue
    def newIntegerValue(pc: Int, value: Int) = IntegerValue(value)
    val newIntegerConstant0: DomainValue = value0
    def intValuesRange(pc: Int, start: Int, end: Int): DomainValue =
        IntegerValueRange(start, end)

}

