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

    protected class NoLegalValue(
        val initialReason: String)
            extends super[Domain].NoLegalValue {
        this: DomainValue ⇒

        override def computationalType: ComputationalType =
            BATError(
                "the value \"NoLegalValue\" does not have a computational type "+
                    "(underlying initial reason:"+initialReason+")")

    }
    object NoLegalValue {
        def unapply(value: NoLegalValue): Option[String] = Some(value.initialReason)
    }
    type DomainNoLegalValue <: NoLegalValue with DomainValue
    def NoLegalValue(initialReason: String): DomainNoLegalValue

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
     *
     * @author Michael Eichberg
     */
    trait ComputationalTypeCategory1Value extends Value

    /**
     * Abstracts over all values with computational type `integer`.
     *
     * @author Michael Eichberg
     */
    trait CTIntegerValue
            extends ComputationalTypeCategory1Value {

        final def computationalType: ComputationalType = ComputationalTypeInt
    }

    /**
     * Represents some unknown value of computational type Integer.
     */
    val CTIntegerValue: CTIntegerValue with DomainValue

    /**
     * Abstracts over all values with computational type `float`.
     *
     * @author Michael Eichberg
     */
    trait SomeFloatValue
            extends ComputationalTypeCategory1Value
            with TypedValue {

        final def computationalType: ComputationalType = ComputationalTypeFloat
        final def valueType = FloatType

    }

    /**
     * Abstracts over all values with computational type `reference`.
     *
     * @author Michael Eichberg
     */
    trait ReferenceValue extends ComputationalTypeCategory1Value with TypedValue {
        final def computationalType: ComputationalType = ComputationalTypeReference
        def valueType: ReferenceType = ObjectType.Object
    }

    val SomeReferenceValue: ReferenceValue with DomainValue

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
     *
     * @author Michael Eichberg
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

    def areEqual(value1: DomainValue, value2: DomainValue): Answer = {
        Unknown
    }

    def isLess(smallerValue: DomainValue, largerValue: DomainValue): Answer = {
        Unknown
    }

    def isLessOrEqual(smallerOrEqualValue: DomainValue, equalOrLargerValue: DomainValue): Answer = {
        Unknown
    }

    //
    // HANDLING CONSTRAINTS
    //
    // The default domain does not handle constraints; i.e. does not update the 
    // memory layout.
    //
    /**
     * ==Note==
     * Returning a plain `NullValue` may not be the optimal solution as the type
     * information is lost.
     */
    def addConstraint(
        constraint: ValueConstraint,
        pc: Int,
        memoryLayout: DomainMemoryLayout): DomainMemoryLayout = {
        memoryLayout
    }

}





