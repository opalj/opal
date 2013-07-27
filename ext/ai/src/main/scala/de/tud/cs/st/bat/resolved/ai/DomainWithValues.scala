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
 * The values module of a [[de.tud.cs.st.bat.resolved.ai.Domain]].
 *
 * Some of the value types are fixed and cannot be directly extended. This
 * part is required by BATAI to perform the abstract interpretation.
 */
trait DomainWithValues extends Domain {

    /**
     * Trait that is mixed in by values for which we have more precise type information,
     * but no/limited concrete value information.
     *
     * @author Michael Eichberg
     */
    trait TypedValue extends Value {
        def valueType: Type
    }
    object TypedValue {
        def unapply(tv: TypedValue): Option[Type] = Some(tv.valueType)
    }

    def TypedValue(someType: Type): TypedDomainValue = {
        someType match {
            case BooleanType       ⇒ SomeBooleanValue
            case ByteType          ⇒ SomeByteValue
            case ShortType         ⇒ SomeShortValue
            case CharType          ⇒ SomeCharValue
            case IntegerType       ⇒ SomeIntegerValue
            case FloatType         ⇒ SomeFloatValue
            case LongType          ⇒ SomeLongValue
            case DoubleType        ⇒ SomeDoubleValue
            case rt: ReferenceType ⇒ ReferenceValue(rt)
            case VoidType          ⇒ AIImplementationError("it is not possible to create a typed value of type VoidType")
        }
    }

    type TypedDomainValue = TypedValue with DomainValue

    val SomeBooleanValue: TypedDomainValue
    val SomeByteValue: TypedDomainValue
    val SomeShortValue: TypedDomainValue
    val SomeCharValue: TypedDomainValue
    val SomeIntegerValue: TypedDomainValue
    val SomeFloatValue: TypedDomainValue
    val SomeLongValue: TypedDomainValue
    val SomeDoubleValue: TypedDomainValue

    def ReferenceValue(referenceType: ReferenceType): TypedDomainValue

    val AString = ReferenceValue(ObjectType.String)
    val AClass = ReferenceValue(ObjectType.Class)
    val AnObject = ReferenceValue(ObjectType.Object)

    /**
     * Abstracts over all values with computational type category `1`.
     */
    trait ComputationalTypeCategory1Value extends Value

    /**
     * Abstracts over all values with computational type `integer`.
     */
    trait CTIntegerValue
            extends ComputationalTypeCategory1Value {

        final def computationalType: ComputationalType = ComputationalTypeInt
    }


    /**
     * Abstracts over all values with computational type `float`.
     */
    trait SomeFloatValue
            extends ComputationalTypeCategory1Value
            with TypedValue {

        final def computationalType: ComputationalType = ComputationalTypeFloat
        final def valueType = FloatType

    }

    /**
     * Abstracts over all values with computational type `reference`.
     */
    trait ReferenceValue extends ComputationalTypeCategory1Value with TypedValue {
        final def computationalType: ComputationalType = ComputationalTypeReference
        def valueType: ReferenceType = ObjectType.Object
    }

//    val SomeReferenceValue: ReferenceValue with DomainValue

    val NullValue: ReferenceValue with DomainValue

    /**
     * Represents a value of type return address.
     *
     * @note The framework completely handles all aspects related to return address values.
     */
    sealed trait CTReturnAddressValue extends ComputationalTypeCategory1Value {
        final def computationalType: ComputationalType = ComputationalTypeReturnAddress
    }

    /**
     * Abstracts over all values with computational type category `2`.
     */
    trait ComputationalTypeCategory2Value extends Value

    trait SomeLongValue extends ComputationalTypeCategory2Value with TypedValue {
        final def computationalType: ComputationalType = ComputationalTypeLong
        final def valueType = LongType
    }

    trait SomeDoubleValue extends ComputationalTypeCategory2Value with TypedValue {
        final def computationalType: ComputationalType = ComputationalTypeDouble
        final def valueType = DoubleType
    }

    //
    // QUESTION'S ABOUT VALUES
    //
    def isNull(value: DomainValue): Answer = {
        if (value == NullValue)
            Yes
        else
            Unknown
    }

    def areEqualReferences(value1: DomainValue, value2: DomainValue): Answer = {
        Unknown
    }

    def areEqualIntegers(value1: DomainValue, value2: DomainValue): Answer = {
        Unknown
    }

    def isLessThan(smallerValue: DomainValue, largerValue: DomainValue): Answer = {
        Unknown
    }

    def isLessThanOrEqualTo(smallerOrEqualValue: DomainValue, equalOrLargerValue: DomainValue): Answer = {
        Unknown
    }

    def isValueInRange(value: DomainValue, lowerBound: Int, upperBound: Int): Answer =
        Unknown

    def isValueNotInRange(value: DomainValue, lowerBound: Int, upperBound: Int): Answer =
        Unknown

    //
    // HANDLING CONSTRAINTS
    //
    // The default domain does not handle constraints; i.e. does not update the 
    // memory layout.
    //

    object IgnoreSingleValueConstraint extends SingleValueConstraint {
        def apply(pc: Int, value: DomainValue, operands: Operands, locals: Locals): (Operands, Locals) = {
            (operands, locals)
        }
    }
    class IgnoreSingleValueConstraintWithBound[Bound] extends SingleValueConstraintWithBound[Bound] {
        def apply(pc: Int, bound: Bound, value: DomainValue, operands: Operands, locals: Locals): (Operands, Locals) = {
            (operands, locals)
        }
    }
    object IgnoreSingleValueConstraintWithReferenceTypeBound extends IgnoreSingleValueConstraintWithBound[ReferenceType]
    object IgnoreSingleValueConstraintWithIntegerValueBound extends IgnoreSingleValueConstraintWithBound[Int]

    object IgnoreTwoValuesConstraint extends TwoValuesConstraint {
        def apply(pc: Int, value1: DomainValue, value2: DomainValue, operands: Operands, locals: Locals): (Operands, Locals) = {
            (operands, locals)
        }
    }

    def IsNull: SingleValueConstraint = IgnoreSingleValueConstraint
    def IsNonNull: SingleValueConstraint = IgnoreSingleValueConstraint
    def AreEqualReferences: TwoValuesConstraint = IgnoreTwoValuesConstraint
    def AreNotEqualReferences: TwoValuesConstraint = IgnoreTwoValuesConstraint
    def UpperBound: SingleValueConstraintWithBound[ReferenceType] = IgnoreSingleValueConstraintWithReferenceTypeBound
    //
    // W.r.t. Integer values
    def hasValue: SingleValueConstraintWithBound[Int] = IgnoreSingleValueConstraintWithIntegerValueBound
    def AreEqualIntegers: TwoValuesConstraint = IgnoreTwoValuesConstraint
    def AreNotEqualIntegers: TwoValuesConstraint = IgnoreTwoValuesConstraint
    def IsLessThan: TwoValuesConstraint = IgnoreTwoValuesConstraint
    def IsLessThanOrEqualTo: TwoValuesConstraint = IgnoreTwoValuesConstraint
}

/**
 * Mixin this trait if you want to reify the stated constraints.
 */
trait ConstraintManifestation extends Domain {

    trait ReifiedConstraint

    case class IsNullConstraint(pc: Int, value: Value) extends ReifiedConstraint
    case class IsNonNullConstraint(pc: Int, value: Value) extends ReifiedConstraint

    case class SingleValueReifiedConstraint(
        r: (Int, DomainValue) ⇒ ReifiedConstraint,
        sv: ( /* pc :*/ Int, DomainValue, Operands, Locals) ⇒ (Operands, Locals))
            extends SingleValueConstraint {

        def apply(pc: Int, v: DomainValue, o: Operands, l: Locals) = {
            addConstraint(r(pc, v))
            sv(pc, v, o, l)
        }
    }

    abstract override def IsNull: SingleValueConstraint =
        SingleValueReifiedConstraint(IsNullConstraint, super.IsNull)

    abstract override def IsNonNull: SingleValueConstraint =
        SingleValueReifiedConstraint(IsNonNullConstraint, super.IsNonNull)

    //    abstract override def UpperBound: SingleValueConstraintWithBound[ReferenceType]
    //
    //    abstract override def AreEqualReferences: TwoValuesConstraint
    //    abstract override def AreNotEqualReferences: TwoValuesConstraint
    //    abstract override def AreEqualIntegers: TwoValuesConstraint
    //    abstract override def AreNotEqualIntegers: TwoValuesConstraint
    //    abstract override def IsLessThan: TwoValuesConstraint
    //    abstract override def IsLessThanOrEqualTo: TwoValuesConstraint

    def addConstraint(constraint: ReifiedConstraint)
}




