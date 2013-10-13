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

/**
 * Domain to trace integer values at the type level.
 *
 * @author Michael Eichberg
 */
trait TypeLevelIntegerValues[+I] extends Domain[I] {

    // -----------------------------------------------------------------------------------
    //
    // REPRESENTATION OF INTEGER LIKE VALUES
    //
    // -----------------------------------------------------------------------------------

    /**
     * Abstracts over all values with computational type `integer` and also
     * represents Integer values.
     */
    trait ComputationalTypeIntegerValue extends Value { this: DomainValue ⇒
        final def computationalType: ComputationalType = ComputationalTypeInt

        override def summarize(pc: PC): DomainValue = this

        override def summarize(pc: PC, value: DomainValue): DomainValue = {
            this.join(pc, value) match {
                case NoUpdate          ⇒ this
                case SomeUpdate(value) ⇒ value
            }
        }

        protected[TypeLevelIntegerValues] def types: TypesAnswer[_]
    }

    trait BooleanValue extends ComputationalTypeIntegerValue { this: DomainValue ⇒
        final def types: TypesAnswer[_] = typesAnswerBoolean
    }
    trait ByteValue extends ComputationalTypeIntegerValue { this: DomainValue ⇒
        final def types: TypesAnswer[_] = typesAnswerByte
    }
    trait CharValue extends ComputationalTypeIntegerValue { this: DomainValue ⇒
        final def types: TypesAnswer[_] = typesAnswerChar
    }
    trait ShortValue extends ComputationalTypeIntegerValue { this: DomainValue ⇒
        final def types: TypesAnswer[_] = typesAnswerShort
    }
    trait IntegerValue extends ComputationalTypeIntegerValue { this: DomainValue ⇒
        final def types: TypesAnswer[_] = typesAnswerInteger
    }

    protected def newIntegerValue(): DomainValue

    private val typesAnswerBoolean: IsPrimitiveType = IsPrimitiveType(BooleanType)

    private val typesAnswerByte: IsPrimitiveType = IsPrimitiveType(ByteType)

    private val typesAnswerChar: IsPrimitiveType = IsPrimitiveType(CharType)

    private val typesAnswerShort: IsPrimitiveType = IsPrimitiveType(ShortType)

    private val typesAnswerInteger: IsPrimitiveType = IsPrimitiveType(IntegerType)

    abstract override def types(value: DomainValue): TypesAnswer[_] = value match {
        case ctiv: ComputationalTypeIntegerValue ⇒ ctiv.types
        case _                                   ⇒ super.types(value)
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
        largerValue: DomainValue): Answer =
        Unknown

    def isLessThanOrEqualTo(
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
    def ineg(pc: PC, value: DomainValue) = newIntegerValue()

    //
    // BINARY EXPRESSIONS
    //

    def iadd(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        newIntegerValue()

    def iand(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        newIntegerValue()

    def idiv(pc: PC, value1: DomainValue, value2: DomainValue) =
        ComputedValue(newIntegerValue())

    def imul(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        newIntegerValue()

    def ior(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        newIntegerValue()

    def irem(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        newIntegerValue()

    def ishl(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        newIntegerValue()

    def ishr(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        newIntegerValue()

    def isub(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        newIntegerValue()

    def iushr(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        newIntegerValue()

    def ixor(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        newIntegerValue()

    def iinc(pc: PC, value: DomainValue, increment: Int) = newIntegerValue()

    //
    // TYPE CONVERSION INSTRUCTIONS
    //

    def i2b(pc: PC, value: DomainValue): DomainValue = newByteValue(pc)
    def i2c(pc: PC, value: DomainValue): DomainValue = newCharValue(pc)
    def i2d(pc: PC, value: DomainValue): DomainValue = newDoubleValue(pc)
    def i2f(pc: PC, value: DomainValue): DomainValue = newFloatValue(pc)
    def i2l(pc: PC, value: DomainValue): DomainValue = newLongValue(pc)
    def i2s(pc: PC, value: DomainValue): DomainValue = newShortValue(pc)

}


