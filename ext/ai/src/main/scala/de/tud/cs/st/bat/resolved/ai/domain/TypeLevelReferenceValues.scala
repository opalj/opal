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
    trait ReferenceValue extends Value {

        final def computationalType: ComputationalType = ComputationalTypeReference

        def isNull: Answer
    }

    //
    // QUESTION'S ABOUT VALUES
    //

    /**
     * Tests if both values refer to the same object instance. Though this is in general
     * intractable, there are some cases where a definitive answer is possible.
     *
     * This implementation completely handles the case where at least one value
     * definitively represents the `null` value.
     * If both values represent non-null values (or just maybe `null` values) `Unknown` is
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

    def isNull(value: DomainValue): Answer = value match {
        case r: ReferenceValue ⇒ r.isNull
        case _                 ⇒ AIImplementationError("a non-reference value cannot be (non-)null")
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

    def stringValue(pc: Int, value: String): DomainValue =
        SomeReferenceValue(ObjectType.String)

    def classValue(pc: Int, t: ReferenceType): DomainValue =
        SomeReferenceValue(ObjectType.Class)

    //
    // TYPE CHECKS AND CONVERSION
    //

    def checkcast(pc: Int, value: DomainValue, resolvedType: ReferenceType) =
        ComputedValue(TypedValue(resolvedType))

    def instanceof(pc: Int, value: DomainValue, resolvedType: ReferenceType) =
        SomeBooleanValue
}

trait DefaultTypeLevelReferenceValues[I]
        extends DefaultValueBinding
        with TypeLevelReferenceValues { domain: Domain[I] ⇒

    import language.existentials

    //
    //
    // HANDLING OF REFERENCE VALUES
    //
    //

    trait TypeBound {
        def valueTypes: Set[_ <: ReferenceType]
    }

    case class PreciseType private (valueTypes: Set[_ <: ReferenceType]) extends TypeBound

    object PreciseType {
        def apply(valueType: ReferenceType): PreciseType =
            new PreciseType(Set.empty[ReferenceType] + valueType)

    }

    case class UnionType(valueTypes: Set[_ <: ReferenceType]) extends TypeBound {
        assume(valueTypes.size >= 2, "a union type with only one bound is meaningless")
    }

    /**
     * @note Subclasses are not expected to add further state. If so, the implementation
     * of equals and hashCode need to be revised.
     */
    trait ReferenceValue extends super.ReferenceValue { outer ⇒

        def pc: Int

        def valueTypes: Set[TypeBound] // Basically, we have to distinguish two situations: (1) a value that may have (depending on the control flow) different independent types (2) a type for which we have multiple bounds (i.e., we don't know the precise type, but we know (e.g., due to typechecks) that it implements multiple interfaces) 

        def isNull: Answer

        final val context: I = domain.identifier

        override def merge(mergePC: Int, value: DomainValue): Update[DomainValue] = {
            if (this eq value)
                return NoUpdate

            value match {
                case other: ReferenceValue if this.valueTypes == other.valueTypes && this.isNull == other.isNull ⇒ {
                    if (this.pc == other.pc) {
                        NoUpdate
                    } else {
                        MetaInformationUpdate(
                            new ReferenceValue {
                                def pc = mergePC
                                def isNull = outer.isNull
                                def valueTypes = outer.valueTypes
                            }
                        )
                    }
                }
                case other: ReferenceValue ⇒ {
                    StructuralUpdate(
                        new ReferenceValue {
                            def pc = mergePC
                            def isNull = outer.isNull.merge(other.isNull)
                            def valueTypes = outer.valueTypes ++ other.valueTypes
                        }
                    )
                }
                case _ ⇒ MetaInformationUpdateNoLegalValue
            }
        }

        override def equals(other: Any): Boolean = {
            other match {
                case other: ReferenceValue ⇒
                    this.context == other.context &&
                        this.pc == other.pc &&
                        this.isNull == other.isNull &&
                        this.valueTypes == other.valueTypes
                case _ ⇒ false
            }
        }

        override def hashCode(): Int = {
            valueTypes.hashCode() ^ isNull.hashCode() ^ (pc << 16)
        }

        override def toString: String =
            "ReferenceValue(isNull="+isNull+"; pc="+pc+"; types="+
                valueTypes.map(
                    _.valueTypes.map(_.toJava).mkString("(", ", ", ")")
                ).mkString(", ")
    }

    object ReferenceValue {
        def unapply(rv: ReferenceValue): Option[(Int, Set[_ <: TypeBound], Answer)] =
            Some((rv.pc, rv.valueTypes, rv.isNull))
    }

    def SomeReferenceValue(t: ReferenceType): DomainTypedValue[t.type] =
        new ReferenceValue with TypedValue[t.type] {
            def pc = -1
            val valueTypes: Set[TypeBound] = Set.empty + PreciseType(t)
            val isNull = Unknown
            def valueType: t.type = t
        }

    def SomeReferenceValue(thePC: Int,
                           theValueTypes: Set[TypeBound],
                           theIsNull: Answer): ReferenceValue = {
        new ReferenceValue {
            def pc = thePC
            def valueTypes = theValueTypes
            val isNull = theIsNull
        }
    }

    // FIXME It doesn't make sense to convert type bounds to types ... BATAI should directly calculate with type bounds
    def types(value: DomainValue): ValuesAnswer[Set[_ <: Type]] = {
        value match {
            case r: ReferenceValue ⇒ Values(r.valueTypes.map(_.valueTypes).flatten)
            case t: TypedValue[_]  ⇒ Values(Set(t.valueType))
        }
    }

    def isSubtypeOf(value: DomainValue, superType: ReferenceType): Answer = {
        Unknown // TODO make this MORE meaningful
    }

    def newObject(newObjectPC: Int, t: ObjectType): DomainTypedValue[t.type] =
        new ReferenceValue with TypedValue[t.type] {
            def pc = newObjectPC
            def valueTypes = Set.empty + PreciseType(t)
            def isNull = No
            def valueType: t.type = t
        }

    def theNullValue(nullValuePC: Int): DomainValue =
        new ReferenceValue {
            def pc: Int = nullValuePC
            def valueTypes: Set[TypeBound] = Set.empty
            def isNull: Answer = Yes
        }

    override def stringValue(loadStringPC: Int, value: String): DomainValue =
        new ReferenceValue with TypedValue[ObjectType.String.type] {
            def pc = loadStringPC
            def valueTypes = Set.empty + PreciseType(ObjectType.String)
            def isNull = No
            def valueType = ObjectType.String
        }

    override def classValue(loadClassPC: Int, t: ReferenceType): DomainValue =
        new ReferenceValue with TypedValue[ObjectType.Class.type] {
            def pc = loadClassPC
            def valueTypes = Set.empty + PreciseType(ObjectType.Class)
            def isNull: Answer = No
            def valueType: ObjectType.Class.type = ObjectType.Class
        }

}

//trait StringValuesTracing[I]
//        extends Domain[I]
//        with DefaultValueBinding
//        with TypeLevelReferenceValues {
//
//    protected trait ConcreteStringValue extends ReferenceValue[ObjectType] {
//        def valueType: ObjectType = ObjectType.String
//    }
//
//    case object SomeConcreteString extends ConcreteStringValue {
//        override def merge(value: DomainValue): Update[DomainValue] = value match {
//            case SomeConcreteString       ⇒ NoUpdate
//            case other: ReferenceValue[_] ⇒ StructuralUpdate(other)
//            case _                        ⇒ MetaInformationUpdateNoLegalValue
//        }
//    }
//
//    case class AConcreteString(val theString: String) extends ConcreteStringValue {
//        override def merge(value: DomainValue): Update[DomainValue] = value match {
//            case AConcreteString(`theString`) ⇒ NoUpdate
//            case other: ReferenceValue[_]     ⇒ StructuralUpdate(other)
//            case _                            ⇒ MetaInformationUpdateNoLegalValue
//        }
//    }
//
//    override def stringValue(pc: Int, value: String) =
//        if (value eq null)
//            AIImplementationError("it is not possible to create a concrete string given a null value")
//        else
//            AConcreteString(value)
//
//}


