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

/**
 * Abstracts over a concrete operand stack value or a value stored in one of the local
 * variables.
 *
 * ==Note==
 * The minimal information that is required by BAT is the computational type of the
 * values.
 * This information is required to correctly model the effect of `dup_...` instructions
 * or the other generic instructions (swap) and to make the analysis of bytecode at
 * least possible.
 *
 * @author Michael Eichberg
 */
sealed trait Value {
    /**
     * The computational type of the value.
     */
    def computationalType: ComputationalType
}
/**
 * Trait that is mixed in by values for which we have more precise type information.
 *
 * @author Michael Eichberg
 */
sealed trait TypedValue extends Value {
    def valueType: Type
}
object TypedValue {

    val AString = AReferenceTypeValue(ObjectType.String)
    val AClass = AReferenceTypeValue(ObjectType.Class)
    val AnObject = AReferenceTypeValue(ObjectType.Object)

    def apply(t: Type): TypedValue = {
        t match {
            case BooleanType          ⇒ SomeBooleanValue
            case ByteType             ⇒ SomeByteValue
            case ShortType            ⇒ SomeShortValue
            case CharType             ⇒ SomeCharValue
            case IntegerType          ⇒ SomeIntegerValue
            case FloatType            ⇒ SomeFloatValue
            case LongType             ⇒ SomeLongValue
            case DoubleType           ⇒ SomeDoubleValue
            case rt @ ReferenceType() ⇒ AReferenceTypeValue(rt)
        }
    }

    def unapply(tv: TypedValue): Option[Type] = Some(tv.valueType)
}

/**
 * Abstracts over all values with computational type category `1`.
 *
 * @author Michael Eichberg
 */
sealed trait ComputationalTypeCategory1Value extends Value

/**
 * Abstracts over all values with computational type `integer`.
 *
 * @author Michael Eichberg
 */
sealed class CTIntegerValue extends ComputationalTypeCategory1Value {
    final def computationalType: ComputationalType = ComputationalTypeInt
}
final object CTIntegerValue extends CTIntegerValue

class SomeBooleanValue extends CTIntegerValue with TypedValue {
    def valueType = BooleanType
}
final object SomeBooleanValue extends SomeBooleanValue

class SomeByteValue extends CTIntegerValue with TypedValue {
    def valueType = ByteType
}
final object SomeByteValue extends SomeByteValue

class SomeShortValue extends CTIntegerValue with TypedValue {
    def valueType = ShortType
}
final object SomeShortValue extends SomeShortValue

class SomeCharValue extends CTIntegerValue with TypedValue {
    def valueType = CharType
}
final object SomeCharValue extends SomeCharValue

class SomeIntegerValue extends CTIntegerValue with TypedValue {
    def valueType = IntegerType
}
final object SomeIntegerValue extends SomeIntegerValue

/**
 * Abstracts over all values with computational type `float`.
 *
 * @author Michael Eichberg
 */
class SomeFloatValue extends ComputationalTypeCategory1Value with TypedValue {
    final def computationalType: ComputationalType = ComputationalTypeFloat
    final def valueType = FloatType
}
final object SomeFloatValue extends SomeFloatValue

/**
 * Abstracts over all values with computational type `reference`.
 *
 * @author Michael Eichberg
 */
sealed class CTReferenceValue extends ComputationalTypeCategory1Value {
    final def computationalType: ComputationalType = ComputationalTypeReference
}
final object CTReferenceValue extends CTReferenceValue

final case object NullValue extends CTReferenceValue

class SomeReferenceTypeValue extends CTReferenceValue with TypedValue {
    def valueType: ReferenceType = ObjectType.Object
}
case class AReferenceTypeValue(override val valueType: ReferenceType) extends SomeReferenceTypeValue

/**
 * Represents a value of type return address.
 *
 * @note The framework completely handles all aspects related to return address values.
 */
sealed class CTReturnAddressValue extends ComputationalTypeCategory1Value {
    final def computationalType: ComputationalType = ComputationalTypeReturnAddress
}
final case class ReturnAddressValue(address: Int) extends CTReturnAddressValue

/**
 * Abstracts over all values with computational type category `2`.
 *
 * @author Michael Eichberg
 */
sealed trait ComputationalTypeCategory2Value extends Value

class SomeLongValue extends ComputationalTypeCategory2Value with TypedValue {
    final def computationalType: ComputationalType = ComputationalTypeLong
    final def valueType = LongType
}

final object SomeLongValue extends SomeLongValue

class SomeDoubleValue extends ComputationalTypeCategory2Value with TypedValue {
    final def computationalType: ComputationalType = ComputationalTypeDouble
    final def valueType = DoubleType
}
final object SomeDoubleValue extends SomeDoubleValue






