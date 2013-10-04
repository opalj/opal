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

        def types: TypesAnswer[_]

        override def adapt[TDI >: I](targetDomain: Domain[TDI], pc: Int): targetDomain.DomainValue =
            targetDomain match {
                case typeLevelIntegerValuesDomain: TypeLevelIntegerValues[I] ⇒
                    // "this" value does not have a dependency on this domain instance  
                    this.asInstanceOf[targetDomain.DomainValue]
                case _ ⇒ super.adapt(targetDomain, pc)
            }
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

    def isSomeValueInRange(value: DomainValue, lowerBound: Int, upperBound: Int): Boolean =
        true

    def isSomeValueNotInRange(value: DomainValue, lowerBound: Int, upperBound: Int): Boolean =
        true

    def isLessThan(smallerValue: DomainValue, largerValue: DomainValue): Answer = {
        Unknown
    }

    def isLessThanOrEqualTo(smallerOrEqualValue: DomainValue, equalOrLargerValue: DomainValue): Answer = {
        Unknown
    }

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

    def i2b(pc: Int, value: DomainValue): DomainValue = newByteValue(pc)
    def i2c(pc: Int, value: DomainValue): DomainValue = newCharValue(pc)
    def i2d(pc: Int, value: DomainValue): DomainValue = newDoubleValue(pc)
    def i2f(pc: Int, value: DomainValue): DomainValue = newFloatValue(pc)
    def i2l(pc: Int, value: DomainValue): DomainValue = newLongValue(pc)
    def i2s(pc: Int, value: DomainValue): DomainValue = newShortValue(pc)

}

trait DefaultTypeLevelIntegerValues[+I]
        extends DefaultValueBinding[I]
        with TypeLevelIntegerValues[I] {

    case object BooleanValue extends super.BooleanValue {
        override def merge(pc: Int, value: DomainValue): Update[DomainValue] = value match {
            case BooleanValue         ⇒ NoUpdate
            case other @ IntegerValue ⇒ StructuralUpdate(other)
            case TheIllegalValue      ⇒ MetaInformationUpdateIllegalValue
            case _                    ⇒ MetaInformationUpdateIllegalValue
        }

        // REMOVE?def onCopyToRegister = this
    }
    case object ByteValue extends super.ByteValue {
        override def merge(pc: Int, value: DomainValue): Update[DomainValue] = value match {
            case ByteValue            ⇒ NoUpdate
            case TheIllegalValue      ⇒ MetaInformationUpdateIllegalValue
            case other @ IntegerValue ⇒ StructuralUpdate(other)
            case other @ ShortValue   ⇒ StructuralUpdate(other)
            case other @ CharValue    ⇒ StructuralUpdate(other)
            case other                ⇒ MetaInformationUpdateIllegalValue
        }

        // REMOVE?def onCopyToRegister = this
    }

    case object ShortValue extends super.ShortValue {
        override def merge(pc: Int, value: DomainValue): Update[DomainValue] = value match {
            case ShortValue           ⇒ NoUpdate
            case TheIllegalValue      ⇒ MetaInformationUpdateIllegalValue
            case other @ IntegerValue ⇒ StructuralUpdate(other)
            case ByteValue            ⇒ NoUpdate
            case other @ CharValue    ⇒ StructuralUpdate(IntegerValue)
            case other                ⇒ MetaInformationUpdateIllegalValue
        }

        // REMOVE?def onCopyToRegister = this
    }

    case object CharValue extends super.CharValue {
        override def merge(pc: Int, value: DomainValue): Update[DomainValue] = value match {
            case CharValue            ⇒ NoUpdate
            case TheIllegalValue      ⇒ MetaInformationUpdateIllegalValue
            case ByteValue            ⇒ NoUpdate
            case ShortValue           ⇒ StructuralUpdate(IntegerValue)
            case other @ IntegerValue ⇒ StructuralUpdate(other)
            case other                ⇒ MetaInformationUpdateIllegalValue
        }

        // REMOVE?def onCopyToRegister = this
    }

    case object IntegerValue extends super.IntegerValue {
        override def merge(pc: Int, value: DomainValue): Update[DomainValue] = value match {
            case IntegerValue    ⇒ NoUpdate
            case TheIllegalValue ⇒ MetaInformationUpdateIllegalValue
            case BooleanValue    ⇒ NoUpdate
            case ByteValue       ⇒ NoUpdate
            case CharValue       ⇒ NoUpdate
            case ShortValue      ⇒ NoUpdate
            case other           ⇒ MetaInformationUpdateIllegalValue
        }

        // REMOVE?def onCopyToRegister = this
    }

    def newBooleanValue(): DomainValue = BooleanValue
    def newBooleanValue(pc: Int): DomainValue = BooleanValue
    def newBooleanValue(pc: Int, value: Boolean): DomainValue = BooleanValue

    def newByteValue() = ByteValue
    def newByteValue(pc: Int): DomainValue = ByteValue
    def newByteValue(pc: Int, value: Byte) = ByteValue

    def newShortValue() = ShortValue
    def newShortValue(pc: Int): DomainValue = ShortValue
    def newShortValue(pc: Int, value: Short) = ShortValue

    def newCharValue() = CharValue
    def newCharValue(pc: Int): DomainValue = CharValue
    def newCharValue(pc: Int, value: Byte) = CharValue

    def newIntegerValue() = IntegerValue
    def newIntegerValue(pc: Int): DomainValue = IntegerValue
    def newIntegerValue(pc: Int, value: Int) = IntegerValue
    val newIntegerConstant0: DomainValue = newIntegerValue(Int.MinValue, 0)
    def intValuesRange(pc: Int, start: Int, end: Int): DomainValue = IntegerValue

}

