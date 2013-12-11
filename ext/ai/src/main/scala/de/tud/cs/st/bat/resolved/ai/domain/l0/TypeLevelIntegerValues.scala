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
package l0

import de.tud.cs.st.util.{ Answer, Yes, No, Unknown }

/**
 * Domain that performs computations related to integer values at the type level.
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
     * Abstracts over values with computational type `integer`.
     */
    trait ComputationalTypeIntegerValue extends Value { this: DomainValue ⇒

        override final def computationalType: ComputationalType = ComputationalTypeInt

        override def summarize(pc: PC): DomainValue = this

        override def summarize(pc: PC, value: DomainValue): DomainValue =
            this.join(pc, value) match {
                case NoUpdate          ⇒ this
                case SomeUpdate(value) ⇒ value
            }

        protected[TypeLevelIntegerValues] def typesAnswer: TypesAnswer
    }

    trait BooleanValue extends ComputationalTypeIntegerValue { this: DomainValue ⇒
        override final def typesAnswer: TypesAnswer = IsBooleanValue
    }
    trait ByteValue extends ComputationalTypeIntegerValue { this: DomainValue ⇒
        override final def typesAnswer: TypesAnswer = IsByteValue
    }
    trait CharValue extends ComputationalTypeIntegerValue { this: DomainValue ⇒
        override final def typesAnswer: TypesAnswer = IsCharValue
    }
    trait ShortValue extends ComputationalTypeIntegerValue { this: DomainValue ⇒
        override final def typesAnswer: TypesAnswer = IsShortValue
    }
    trait IntegerValue extends ComputationalTypeIntegerValue { this: DomainValue ⇒
        override final def typesAnswer: TypesAnswer = IsIntegerValue
    }

    //
    // QUESTION'S ABOUT VALUES
    //

    abstract override def typeOfValue(value: DomainValue): TypesAnswer = value match {
        case ctiv: ComputationalTypeIntegerValue ⇒ ctiv.typesAnswer
        case _                                   ⇒ super.typeOfValue(value)
    }

    override def areEqual(value1: DomainValue, value2: DomainValue): Answer = Unknown

    override def isSomeValueInRange(
        value: DomainValue,
        lowerBound: Int,
        upperBound: Int): Boolean =
        true

    override def isSomeValueNotInRange(
        value: DomainValue,
        lowerBound: Int,
        upperBound: Int): Boolean =
        true

    override def isLessThan(
        smallerValue: DomainValue,
        largerValue: DomainValue): Answer =
        Unknown

    override def isLessThanOrEqualTo(
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

    override def idiv(pc: PC, value1: DomainValue, value2: DomainValue) =
        ComputedValue(IntegerValue(pc))

    override def imul(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        IntegerValue(pc)

    override def ior(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        IntegerValue(pc)

    override def irem(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        IntegerValue(pc)

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

