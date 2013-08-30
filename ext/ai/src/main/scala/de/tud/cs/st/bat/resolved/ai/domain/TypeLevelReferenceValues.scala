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

/**
 * @author Michael Eichberg
 */
trait TypeLevelReferenceValues
        extends ConstraintsHandlingHelper { this: Domain[_] ⇒

    /**
     * Abstracts over all values with computational type `reference`.
     */
    trait ReferenceValue[+T >: Null <: ReferenceType] extends TypedValue[T] {
        final def computationalType: ComputationalType = ComputationalTypeReference
    }

    /**
     * Defines an extractor method to facilitate pattern matching.
     */
    object ReferenceValue {

        def unapply[T >: Null <: ReferenceType](value: ReferenceValue[T]): Some[T] =
            Some(value.valueType)
    }

    //
    // QUESTION'S ABOUT VALUES
    //

    /**
     * Tests if both values refer to the same object instance.
     *
     * This implementation completely handles the case where at least one value
     * definitively represents the `null` value.
     *
     * If both values represent non-null values (or just maybe `null`) `Unknown` is
     * returned.
     */
    def areEqualReferences(value1: DomainValue, value2: DomainValue): Answer = {
        val value1IsNull = isNull(value1)
        val value2IsNull = isNull(value2)
        if (value1IsNull.isDefined &&
            value2IsNull.isDefined &&
            (value1IsNull.yes || value2IsNull.yes))
            Answer(value1IsNull == value2IsNull)
        else
            Unknown
    }

    /**
     * This project's class hierarchy; unless explicitly overridden, the built-in default
     * class hierarchy is used which only reflects the type-hierarchy
     * between the exception types used by JVM exceptions.
     *
     * @note '''This method is intended to be overridden.'''
     */
    def classHierarchy: analyses.ClassHierarchy =
        analyses.ClassHierarchy.preInitializedClassHierarchy

    def isSubtypeOf(value: DomainValue, supertype: ReferenceType): Answer = {
        value match {
            case ReferenceValue(subtype) ⇒ isSubtypeOf(subtype, supertype)
            case _                       ⇒ Unknown
        }
    }

    def isSubtypeOf(subtype: ReferenceType, supertype: ReferenceType): Answer = {
        classHierarchy.isSubtypeOf(subtype, supertype)
    }

    // -----------------------------------------------------------------------------------
    //
    // HANDLING OF CONSTRAINTS
    //
    // -----------------------------------------------------------------------------------

    def IsNull: SingleValueConstraint = IgnoreSingleValueConstraint
    def IsNonNull: SingleValueConstraint = IgnoreSingleValueConstraint
    def AreEqualReferences: TwoValuesConstraint = IgnoreTwoValuesConstraint
    def AreNotEqualReferences: TwoValuesConstraint = IgnoreTwoValuesConstraint
    def UpperBound: SingleValueConstraintWithBound[ReferenceType] = IgnoreSingleValueConstraintWithReferenceTypeBound

    // -----------------------------------------------------------------------------------
    //
    // HANDLING OF COMPUTATIONS
    //
    // -----------------------------------------------------------------------------------

    //
    // PUSH CONSTANT VALUE
    //

    /*new*/ val AStringObject = SomeReferenceValue(ObjectType.String)
    def stringValue(pc: Int, value: String): DomainValue = AStringObject

    /*new*/ val AClassObject = SomeReferenceValue(ObjectType.Class)
    def classValue(pc: Int, t: ReferenceType): DomainValue = AClassObject

    //
    // TYPE CHECKS AND CONVERSION
    //

    def checkcast(pc: Int, value: DomainValue, resolvedType: ReferenceType) =
        ComputedValue(TypedValue(resolvedType))

    def instanceof(pc: Int, value: DomainValue, resolvedType: ReferenceType) =
        SomeBooleanValue
}

trait StringValuesTracing[I]
        extends Domain[I]
        with DefaultValueBinding
        with TypeLevelReferenceValues {

    protected trait ConcreteStringValue extends ReferenceValue[ObjectType] {
        def valueType: ObjectType = ObjectType.String
    }

    case object SomeConcreteString extends ConcreteStringValue {
        override def merge(value: DomainValue): Update[DomainValue] = value match {
            case SomeConcreteString       ⇒ NoUpdate
            case other: ReferenceValue[_] ⇒ StructuralUpdate(other)
            case _                        ⇒ MetaInformationUpdateNoLegalValue
        }
    }

    case class AConcreteString(val theString: String) extends ConcreteStringValue {
        override def merge(value: DomainValue): Update[DomainValue] = value match {
            case AConcreteString(`theString`) ⇒ NoUpdate
            case other: ReferenceValue[_]     ⇒ StructuralUpdate(other)
            case _                            ⇒ MetaInformationUpdateNoLegalValue
        }
    }

    override def stringValue(pc: Int, value: String) =
        if (value eq null)
            AIImplementationError("it is not possible to create a concrete string given a null value")
        else
            AConcreteString(value)

}

trait DefaultTypeLevelReferenceValues[I]
        extends Domain[I]
        with DefaultValueBinding
        with TypeLevelReferenceValues {

    //
    //
    // HANDLING OF REFERENCE VALUES
    //
    //

    /**
     * Abstracts over all values with computational type `reference`.
     */
    case object SomeReferenceValue extends ReferenceValue[ObjectType] {

        def valueType: ObjectType = ObjectType.Object

        override def merge(value: DomainValue): Update[DomainValue] = value match {
            case other: ReferenceValue[_] ⇒ NoUpdate
            case _                        ⇒ MetaInformationUpdateNoLegalValue
        }
    }

    case object NullValue extends ReferenceValue[Null] {

        def valueType: Null = null

        override def merge(value: Value): Update[DomainValue] = value match {
            case NullValue                ⇒ NoUpdate
            case other: ReferenceValue[_] ⇒ StructuralUpdate(other)
            case _                        ⇒ MetaInformationUpdateNoLegalValue
        }
    }
    def theNullValue(pc: Int) = NullValue

    case class AReferenceValue[+T >: Null <: ReferenceType](
        valueType: T)
            extends ReferenceValue[T] {

        // TODO [AI] We need some support to consult the domain to decide what we want to do.

        override def merge(value: DomainValue): Update[DomainValue] = value match {
            // What we do here is extremely simplistic, but this is basically all we can
            // do when we do not have the class hierarchy available.
            case ReferenceValue(`valueType`) ⇒ NoUpdate
            case NullValue                   ⇒ NoUpdate
            case TheNoLegalValue             ⇒ MetaInformationUpdateNoLegalValue
            case other: ReferenceValue[_]    ⇒ StructuralUpdate(SomeReferenceValue)
            case other                       ⇒ MetaInformationUpdateNoLegalValue
        }

        override def equals(other: Any): Boolean = {
            if (other.isInstanceOf[AReferenceValue[_]]) {
                this.valueType ==
                    other.asInstanceOf[AReferenceValue[_]].valueType
            } else {
                false
            }
        }
        override def hashCode: Int = -valueType.hashCode()

        override def toString: String = "ReferenceTypeValue: "+valueType.toJava
    }

    def SomeReferenceValue(referenceType: ReferenceType): DomainTypedValue[referenceType.type] =
        AReferenceValue(referenceType)

    def isNull(value: DomainValue): Answer = {
        if (value == NullValue)
            Yes
        else
            Unknown
    }

    def types(value: DomainValue): ValuesAnswer[Set[Type]] = {
        value match {
            case ReferenceValue(valueType) ⇒ Values[Set[Type]](Set(valueType))
            case _                         ⇒ ValuesUnknown
        }
    }

    def newObject(pc: Int, t: ObjectType): DomainTypedValue[t.type] = TypedValue(t)

}


