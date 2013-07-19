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

    /**
     * Merges this value with the given value; has to return `this` when this value
     * subsumes the given value or is structurally identical to the given
     * value.
     */
    def merge(value: Value): Value

    @throws[AIImplementationError]
    protected def impossibleToMergeWith(other: Value): AIImplementationError =
        new AIImplementationError(
            "missing support for merging: "+
                this.getClass().getName()+
                " and "+
                other.getClass().getName())

    @throws[BATError]
    protected def incompatibleValue(other: Value): BATError =
        new BATError(
            "incompatible values: "+
                this.getClass().getName()+
                " and "+
                other.getClass().getName())

}

case class NoLegalValue(initialReason: RuntimeException) extends Value {

    def computationalType: ComputationalType =
        BATError("the value \"NoLegalValue\" does not have a computational type (underlying initial reason:"+initialReason+")")

    def merge(value: Value): Value = this
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
sealed trait CTIntegerValue extends ComputationalTypeCategory1Value {
    final def computationalType: ComputationalType = ComputationalTypeInt

    def merge(value: Value): Value = value match { case CTIntegerValue() ⇒ this }
}
object CTIntegerValue {
    private[this] val instance = new CTIntegerValue {}
    def apply() = instance
    def unapply(ctIntVal: CTIntegerValue): Boolean = ctIntVal ne null
}

trait SomeBooleanValue extends CTIntegerValue with TypedValue {
    def valueType = BooleanType

    // Note, we cannot implement some general merging over here!
    // E.g., if the other value is of type CTIntegerValue and represents 
    // other values than 0 and 1 then some special handling needs to be performed!
}
case object SomeBooleanValue extends SomeBooleanValue {
    override def merge(value: Value): Value = value match {
        case other: SomeBooleanValue  ⇒ this
        case other @ SomeByteValue    ⇒ other
        case other @ SomeShortValue   ⇒ other
        case other @ SomeIntegerValue ⇒ other
        case other @ SomeCharValue    ⇒ other
        case other: CTIntegerValue    ⇒ NoLegalValue(impossibleToMergeWith(other))
        case other                    ⇒ NoLegalValue(incompatibleValue(other))
    }
}

trait SomeByteValue extends CTIntegerValue with TypedValue {
    def valueType = ByteType
}
case object SomeByteValue extends SomeByteValue {
    override def merge(value: Value): Value = value match {
        case other: SomeByteValue     ⇒ this
        case other: SomeBooleanValue  ⇒ this
        case other @ SomeShortValue   ⇒ other
        case other @ SomeIntegerValue ⇒ other
        case other @ SomeCharValue    ⇒ other
        case other: CTIntegerValue    ⇒ NoLegalValue(impossibleToMergeWith(other))
        case other                    ⇒ NoLegalValue(incompatibleValue(other))
    }
}

trait SomeShortValue extends CTIntegerValue with TypedValue {
    def valueType = ShortType
}
case object SomeShortValue extends SomeShortValue {
    override def merge(value: Value): Value = value match {
        case other: SomeShortValue    ⇒ this
        case other: SomeBooleanValue  ⇒ this
        case other: SomeByteValue     ⇒ this
        case other @ SomeIntegerValue ⇒ other
        case other @ SomeCharValue    ⇒ SomeIntegerValue
        case other: CTIntegerValue    ⇒ NoLegalValue(impossibleToMergeWith(other))
        case other                    ⇒ NoLegalValue(incompatibleValue(other))
    }
}

trait SomeCharValue extends CTIntegerValue with TypedValue {
    def valueType = CharType
}
case object SomeCharValue extends SomeCharValue {
    override def merge(value: Value): Value = value match {
        case other: SomeCharValue     ⇒ this
        case other: SomeBooleanValue  ⇒ this
        case other: SomeByteValue     ⇒ this
        case other @ SomeShortValue   ⇒ SomeIntegerValue
        case other @ SomeIntegerValue ⇒ other
        case other: CTIntegerValue    ⇒ NoLegalValue(impossibleToMergeWith(other))
        case other                    ⇒ NoLegalValue(incompatibleValue(other))
    }
}

trait SomeIntegerValue extends CTIntegerValue with TypedValue {
    def valueType = IntegerType
}
case object SomeIntegerValue extends SomeIntegerValue {
    override def merge(value: Value): Value = value match {
        case other: CTIntegerValue ⇒ this
        case other                 ⇒ NoLegalValue(incompatibleValue(other))
    }
}

/**
 * Abstracts over all values with computational type `float`.
 *
 * @author Michael Eichberg
 */
trait SomeFloatValue extends ComputationalTypeCategory1Value with TypedValue {
    final def computationalType: ComputationalType = ComputationalTypeFloat
    final def valueType = FloatType
}
case object SomeFloatValue extends SomeFloatValue {
    override def merge(value: Value): Value = value match {
        case other: SomeFloatValue ⇒ this
        case other                 ⇒ NoLegalValue(incompatibleValue(other))
    }
}

/**
 * Abstracts over all values with computational type `reference`.
 *
 * @author Michael Eichberg
 */
sealed trait CTReferenceValue extends ComputationalTypeCategory1Value {
    final def computationalType: ComputationalType = ComputationalTypeReference
}
case object CTReferenceValue extends CTReferenceValue {
    override def merge(value: Value): Value = value match {
        case other: CTReferenceValue ⇒ this
        case other                   ⇒ NoLegalValue(incompatibleValue(other))
    }
}
case object NullValue extends CTReferenceValue {

    // TODO [AI] We need some support to consult the domain to decide what we want to do.
    override def merge(value: Value): Value = value match {
        case NullValue               ⇒ this
        case other: CTReferenceValue ⇒ this
        case other                   ⇒ NoLegalValue(incompatibleValue(other))
    }
}

trait SomeReferenceTypeValue extends CTReferenceValue with TypedValue {
    def valueType: ReferenceType = ObjectType.Object
}
case class AReferenceTypeValue(
    override val valueType: ReferenceType)
        extends SomeReferenceTypeValue {
    // TODO [AI] We need some support to consult the domain to decide what we want to do.

    override def merge(value: Value): Value = value match {
        // What we do here is extremely simplistic, but this is basically all we can
        // do when we do not have the class hierarchy available.
        case AReferenceTypeValue(`valueType`) ⇒ this
        case other: SomeReferenceTypeValue    ⇒ AReferenceTypeValue(ObjectType.Object)
        case other: CTReferenceValue          ⇒ CTReferenceValue
        case other                            ⇒ NoLegalValue(incompatibleValue(other))
    }
}

/**
 * Represents a value of type return address.
 *
 * @note The framework completely handles all aspects related to return address values.
 */
sealed trait CTReturnAddressValue extends ComputationalTypeCategory1Value {
    final def computationalType: ComputationalType = ComputationalTypeReturnAddress
}
final case class ReturnAddressValue(
    addresses: Set[Int])
        extends CTReturnAddressValue {

    override def merge(value: Value): Value = value match {
        case ReturnAddressValue(otherAddresses) ⇒ {
            if (otherAddresses subsetOf this.addresses)
                this
            else
                ReturnAddressValue(this.addresses ++ otherAddresses)
        }
        case other ⇒ NoLegalValue(incompatibleValue(other))
    }
}
object ReturnAddressValue {
    def apply(address: Int) = new ReturnAddressValue(Set(address))
}

/**
 * Abstracts over all values with computational type category `2`.
 *
 * @author Michael Eichberg
 */
sealed trait ComputationalTypeCategory2Value extends Value

trait SomeLongValue extends ComputationalTypeCategory2Value with TypedValue {
    final def computationalType: ComputationalType = ComputationalTypeLong
    final def valueType = LongType
}
case object SomeLongValue extends SomeLongValue {
    override def merge(value: Value): Value = value match {
        case other: SomeLongValue ⇒ this
        case other                ⇒ NoLegalValue(incompatibleValue(other))
    }
}

trait SomeDoubleValue extends ComputationalTypeCategory2Value with TypedValue {
    final def computationalType: ComputationalType = ComputationalTypeDouble
    final def valueType = DoubleType
}
case object SomeDoubleValue extends SomeDoubleValue {
    override def merge(value: Value): Value = value match {
        case other: SomeDoubleValue ⇒ this
        case other                  ⇒ NoLegalValue(incompatibleValue(other))
    }
}





