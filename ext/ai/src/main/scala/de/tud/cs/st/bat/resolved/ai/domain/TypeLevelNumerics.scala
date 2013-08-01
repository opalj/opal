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

import reflect.ClassTag

/**
 * Support for handling of numeric values at the type-level.
 *
 * This trait provides support for handling numeric calculations
 * at the type level. I.e., if you mix in this trait, all Integer,
 * Float, Long and Double values will be represented as such.
 */
trait TypeLevelNumericValues extends ConstraintsHandlingHelper { this: Domain ⇒

    // -----------------------------------------------------------------------------------
    //
    // HANDLING OF VALUES
    //
    // -----------------------------------------------------------------------------------

    val IntegerConstant0: DomainValue = intValue(Int.MinValue, 0)

    /**
     * Abstracts over all values with computational type `integer` and also
     * represents Integer values.
     */
    trait CTIntegerValue extends Value {
        final def computationalType: ComputationalType = ComputationalTypeInt
    }

    /**
     * Abstracts over all values with computational type `float`.
     */
    trait SomeFloatValue extends TypedValue[FloatType] {

        final def computationalType: ComputationalType = ComputationalTypeFloat

        final def valueType = FloatType
    }

    trait SomeLongValue extends TypedValue[LongType] {

        final def computationalType: ComputationalType = ComputationalTypeLong

        final def valueType = LongType
    }

    trait SomeDoubleValue extends TypedValue[DoubleType] {

        final def computationalType: ComputationalType = ComputationalTypeDouble

        final def valueType = DoubleType
    }

    //
    // QUESTION'S ABOUT VALUES
    //

    def areEqualIntegers(value1: DomainValue, value2: DomainValue): Answer = {
        Unknown
    }

    def isLessThan(smallerValue: DomainValue, largerValue: DomainValue): Answer = {
        Unknown
    }

    def isLessThanOrEqualTo(smallerOrEqualValue: DomainValue, equalOrLargerValue: DomainValue): Answer = {
        Unknown
    }

    def isValueInRange(value: DomainValue, lowerBound: Int, upperBound: Int): Boolean =
        true

    def isValueNotInRange(value: DomainValue, lowerBound: Int, upperBound: Int): Boolean =
        true

    // -----------------------------------------------------------------------------------
    //
    // HANDLING OF CONSTRAINTS
    //
    // -----------------------------------------------------------------------------------

    def hasValue: SingleValueConstraintWithBound[Int] =
        IgnoreSingleValueConstraintWithIntegerValueBound

    def AreEqualIntegers: TwoValuesConstraint =
        IgnoreTwoValuesConstraint

    def AreNotEqualIntegers: TwoValuesConstraint =
        IgnoreTwoValuesConstraint

    def IsLessThan: TwoValuesConstraint =
        IgnoreTwoValuesConstraint

    def IsLessThanOrEqualTo: TwoValuesConstraint =
        IgnoreTwoValuesConstraint

    // -----------------------------------------------------------------------------------
    //
    // HANDLING OF COMPUTATIONS
    //
    // -----------------------------------------------------------------------------------

    //
    // PUSH CONSTANT VALUE
    //
    def byteValue(pc: Int, value: Int) = SomeByteValue
    def shortValue(pc: Int, value: Int) = SomeShortValue
    def intValue(pc: Int, value: Int) = SomeIntegerValue
    def longValue(pc: Int, value: Long) = SomeLongValue
    def floatValue(pc: Int, value: Float) = SomeFloatValue
    def doubleValue(pc: Int, value: Double) = SomeDoubleValue

    //
    // TYPE CONVERSIONS
    //

    def d2f(pc: Int, value: DomainValue): DomainValue = SomeFloatValue
    def d2i(pc: Int, value: DomainValue): DomainValue = SomeIntegerValue
    def d2l(pc: Int, value: DomainValue): DomainValue = SomeLongValue

    def f2d(pc: Int, value: DomainValue): DomainValue = SomeDoubleValue
    def f2i(pc: Int, value: DomainValue): DomainValue = SomeIntegerValue
    def f2l(pc: Int, value: DomainValue): DomainValue = SomeLongValue

    def i2b(pc: Int, value: DomainValue): DomainValue = SomeByteValue
    def i2c(pc: Int, value: DomainValue): DomainValue = SomeCharValue
    def i2d(pc: Int, value: DomainValue): DomainValue = SomeDoubleValue
    def i2f(pc: Int, value: DomainValue): DomainValue = SomeFloatValue
    def i2l(pc: Int, value: DomainValue): DomainValue = SomeLongValue
    def i2s(pc: Int, value: DomainValue): DomainValue = SomeShortValue

    def l2d(pc: Int, value: DomainValue): DomainValue = SomeDoubleValue
    def l2f(pc: Int, value: DomainValue): DomainValue = SomeFloatValue
    def l2i(pc: Int, value: DomainValue): DomainValue = SomeIntegerValue

    //
    // RELATIONAL OPERATORS
    //
    def fcmpg(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = SomeIntegerValue
    def fcmpl(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = SomeIntegerValue
    def dcmpg(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = SomeIntegerValue
    def dcmpl(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = SomeIntegerValue
    def lcmp(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = SomeIntegerValue

    //
    // UNARY EXPRESSIONS
    //
    def dneg(pc: Int, value: DomainValue) = SomeDoubleValue
    def fneg(pc: Int, value: DomainValue) = SomeFloatValue
    def ineg(pc: Int, value: DomainValue) = SomeIntegerValue
    def lneg(pc: Int, value: DomainValue) = SomeLongValue

    //
    // BINARY EXPRESSIONS
    //
    def dadd(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = SomeDoubleValue
    def ddiv(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = SomeDoubleValue
    def dmul(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = SomeDoubleValue
    def drem(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = SomeDoubleValue
    def dsub(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = SomeDoubleValue

    def fadd(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = SomeFloatValue
    def fdiv(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = SomeFloatValue
    def fmul(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = SomeFloatValue
    def frem(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = SomeFloatValue
    def fsub(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = SomeFloatValue

    def iadd(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = SomeIntegerValue
    def iand(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = SomeIntegerValue
    def idiv(pc: Int, value1: DomainValue, value2: DomainValue) = ComputedValue(SomeIntegerValue)
    def imul(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = SomeIntegerValue
    def ior(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = SomeIntegerValue
    def irem(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = SomeIntegerValue
    def ishl(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = SomeIntegerValue
    def ishr(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = SomeIntegerValue
    def isub(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = SomeIntegerValue
    def iushr(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = SomeIntegerValue
    def ixor(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = SomeIntegerValue

    def ladd(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = SomeLongValue
    def land(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = SomeLongValue
    def ldiv(pc: Int, value1: DomainValue, value2: DomainValue) = ComputedValue(SomeLongValue)
    def lmul(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = SomeLongValue
    def lor(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = SomeLongValue
    def lrem(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = SomeLongValue
    def lshl(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = SomeLongValue
    def lshr(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = SomeLongValue
    def lsub(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = SomeLongValue
    def lushr(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = SomeLongValue
    def lxor(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = SomeLongValue

    //
    // "OTHER" INSTRUCTIONS
    //
    def iinc(pc: Int, value: DomainValue, increment: Int) = SomeIntegerValue
}

trait DefaultTypeLevelNumericValues
        extends Domain
        with DefaultValueBinding
        with TypeLevelNumericValues {

    case object SomeBooleanValue extends CTIntegerValue with TypedValue[BooleanType] {

        final def valueType = BooleanType

        override def merge(value: DomainValue): Update[DomainValue] = value match {
            case SomeBooleanValue         ⇒ NoUpdate
            case other @ SomeIntegerValue ⇒ StructuralUpdate(other)
            case TheNoLegalValue          ⇒ MetaInformationUpdateNoLegalValue
            case _                        ⇒ MetaInformationUpdateNoLegalValue
        }
    }
    case object SomeByteValue extends CTIntegerValue with TypedValue[ByteType] {

        final def valueType = ByteType

        override def merge(value: DomainValue): Update[DomainValue] = value match {
            case SomeByteValue            ⇒ NoUpdate
            case TheNoLegalValue          ⇒ MetaInformationUpdateNoLegalValue
            case other @ SomeIntegerValue ⇒ StructuralUpdate(other)
            case other @ SomeShortValue   ⇒ StructuralUpdate(other)
            case other @ SomeCharValue    ⇒ StructuralUpdate(other)
            case other                    ⇒ MetaInformationUpdateNoLegalValue
        }
    }

    case object SomeShortValue extends CTIntegerValue with TypedValue[ShortType] {

        final def valueType = ShortType

        override def merge(value: DomainValue): Update[DomainValue] = value match {
            case SomeShortValue           ⇒ NoUpdate
            case TheNoLegalValue          ⇒ MetaInformationUpdateNoLegalValue
            case other @ SomeIntegerValue ⇒ StructuralUpdate(other)
            case SomeByteValue            ⇒ NoUpdate
            case other @ SomeCharValue    ⇒ StructuralUpdate(SomeIntegerValue)
            case other                    ⇒ MetaInformationUpdateNoLegalValue
        }
    }

    case object SomeCharValue extends CTIntegerValue with TypedValue[CharType] {

        final def valueType = CharType

        override def merge(value: DomainValue): Update[DomainValue] = value match {
            case SomeCharValue            ⇒ NoUpdate
            case TheNoLegalValue          ⇒ MetaInformationUpdateNoLegalValue
            case SomeByteValue            ⇒ NoUpdate
            case SomeShortValue           ⇒ StructuralUpdate(SomeIntegerValue)
            case other @ SomeIntegerValue ⇒ StructuralUpdate(other)
            case other                    ⇒ MetaInformationUpdateNoLegalValue
        }
    }

    case object SomeIntegerValue extends CTIntegerValue with TypedValue[IntegerType] {

        final def valueType = IntegerType

        override def merge(value: DomainValue): Update[DomainValue] = value match {
            case SomeIntegerValue ⇒ NoUpdate
            case TheNoLegalValue  ⇒ MetaInformationUpdateNoLegalValue
            case SomeBooleanValue ⇒ NoUpdate
            case SomeByteValue    ⇒ NoUpdate
            case SomeCharValue    ⇒ NoUpdate
            case SomeShortValue   ⇒ NoUpdate
            case other            ⇒ MetaInformationUpdateNoLegalValue
        }
    }

    case object SomeFloatValue extends SomeFloatValue {
        override def merge(value: DomainValue): Update[DomainValue] = value match {
            case SomeFloatValue ⇒ NoUpdate
            case _              ⇒ MetaInformationUpdateNoLegalValue
        }
    }

    case object SomeLongValue extends SomeLongValue {
        override def merge(value: DomainValue): Update[DomainValue] = value match {
            case SomeLongValue ⇒ NoUpdate
            case _             ⇒ MetaInformationUpdateNoLegalValue
        }
    }

    case object SomeDoubleValue extends SomeDoubleValue {
        override def merge(value: DomainValue): Update[DomainValue] = value match {
            case SomeDoubleValue ⇒ NoUpdate
            case _               ⇒ MetaInformationUpdateNoLegalValue
        }
    }

}


