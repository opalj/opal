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

        def updateIsNull(pc: Int, isNull: Answer): DomainValue
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

    def IsNonNull: SingleValueConstraint = new SingleValueConstraint {
        def apply(
            pc: Int,
            value: DomainValue,
            operands: Operands,
            locals: Locals): (Operands, Locals) = {

            value match {
                case r: ReferenceValue ⇒ {
                    val newReferenceValue = r.updateIsNull(pc, No)
                    (
                        operands.map(op ⇒ if (op eq value) newReferenceValue else op),
                        locals.map(l ⇒ if (l eq value) newReferenceValue else l)
                    )
                }
            }
        }
    }

    def IsNull: SingleValueConstraint = new SingleValueConstraint {
        def apply(
            pc: Int,
            value: DomainValue,
            operands: Operands,
            locals: Locals): (Operands, Locals) = {

            value match {
                case r: ReferenceValue ⇒ {
                    val newReferenceValue = r.updateIsNull(pc, Yes)
                    (
                        operands.map(op ⇒ if (op eq value) newReferenceValue else op),
                        locals.map(l ⇒ if (l eq value) newReferenceValue else l)
                    )
                }
            }
        }
    }

    def AreEqualReferences: TwoValuesConstraint = IgnoreTwoValuesConstraint

    def AreNotEqualReferences: TwoValuesConstraint = IgnoreTwoValuesConstraint

    def UpperBound: SingleValueConstraintWithBound[ReferenceType] = IgnoreSingleValueConstraintWithReferenceTypeBound

}

trait DefaultTypeLevelReferenceValues[I]
        extends DefaultValueBinding[I]
        with TypeLevelReferenceValues { domain ⇒

    import language.existentials

    def intValuesRange(pc: Int, start: Int, end: Int): DomainValue

    //
    //
    // HANDLING OF REFERENCE VALUES
    //
    //

    case class UnionType(valueTypes: Set[_ <: ReferenceType]) extends TypeBound {
        assume(valueTypes.size >= 2, "a union type with only one bound is meaningless")
    }

    /**
     * @note Subclasses are not expected to add further state. If so, the implementation
     * of equals and hashCode need to be revised.
     */
    trait ReferenceValue extends super.ReferenceValue { outer ⇒

        def pc: Int

        type ValueType = Set[TypeBound]
        def valueType: Set[TypeBound] // Basically, we have to distinguish two situations: (1) a value that may have (depending on the control flow) different independent types (2) a type for which we have multiple bounds (i.e., we don't know the precise type, but we know (e.g., due to typechecks) that it implements multiple interfaces) 

        def isNull: Answer
        def updateIsNull(pc: Int, isNull: Answer): ReferenceValue =
            AIImplementationError("implementation missing or constraint stated on a definitive value")

        final val context: I = domain.identifier

        override def merge(mergePC: Int, value: DomainValue): Update[DomainValue] = {
            if (this eq value)
                return NoUpdate

            value match {
                case other: ReferenceValue ⇒
                    if (this.pc == other.pc) {
                        // REMARK we are coalescing reference value information
                        // both values have the same origin, i.e., we are effectively
                        // merging two views (w.r.t. its nullness property and
                        // the seen types) of the same value
                        if (this.isNull == other.isNull && this.valueType == other.valueType) {
                            NoUpdate
                        } else {
                            StructuralUpdate(SomeReferenceValue(
                                this.pc,
                                this.valueType ++ other.valueType,
                                outer.isNull.merge(other.isNull)
                            ))
                        }
                    } else if (this.isNull == other.isNull && this.valueType == other.valueType) {
                        MetaInformationUpdate(
                            SomeReferenceValue(mergePC, outer.valueType, outer.isNull)
                        )
                    } else {
                        StructuralUpdate(
                            SomeReferenceValue(
                                mergePC,
                                outer.valueType ++ other.valueType,
                                outer.isNull.merge(other.isNull)
                            )
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
                        this.valueType == other.valueType
                case _ ⇒ false
            }
        }

        override def hashCode(): Int = {
            valueType.hashCode() ^ isNull.hashCode() ^ (pc << 16)
        }

        override def toString: String =
            "ReferenceValue(isNull="+isNull+"; pc="+pc+"; types="+
                valueType.map(
                    _.valueTypes.map(_.toJava).mkString("{", ", ", "}")
                ).mkString("{", ", ", "}")+")"
    }

    object ReferenceValue {
        def unapply(rv: ReferenceValue): Option[(Int, Set[_ <: TypeBound], Answer)] =
            Some((rv.pc, rv.valueType, rv.isNull))
    }

    //
    // REPRESENTATIONS OF CONCRETE REFERENCE VALUES
    //    
    protected class NullValue(
        val pc: Int)
            extends ReferenceValue {

        def valueType: Set[TypeBound] = Set.empty
        def isNull: Yes.type = Yes
    }

    protected class NonNullReferenceValue(
        val pc: Int,
        val referenceValueType: PreciseType)
            extends ReferenceValue {
        def valueType: Set[TypeBound] = Set.empty + referenceValueType
        def isNull: No.type = No
    }

    protected class TypedReferenceValue(
        val referenceValueType: PreciseType)
            extends ReferenceValue {

        def pc = -1
        def valueType: Set[TypeBound] = Set.empty + referenceValueType
        def isNull: Unknown.type = Unknown

        override def updateIsNull(pc: Int, isNull: Answer): ReferenceValue = {
            assume(isNull.isDefined)
            // we keep the old pcs because it is basically the old value
            if (isNull.no) {
                new NonNullReferenceValue(this.pc, referenceValueType)
            } else {
                new NullValue(this.pc)
            }
        }
    }

    protected class GenericReferenceValue(
        val pc: Int,
        val valueType: Set[TypeBound],
        val isNull: Answer)
            extends ReferenceValue {

        override def updateIsNull(pc: Int, isNull: Answer): ReferenceValue = {
            assume(this.isNull.isUndefined)
            assume(isNull.isDefined)
            assume(this.isNull != isNull)

            new GenericReferenceValue(this.pc, valueType, isNull)
        }
    }

    //
    // FACTORY METHODS
    //

    override def nullValue(pc: Int): DomainValue =
        new NullValue(pc)

    def SomeReferenceValue(referenceType: ReferenceType): DomainValue =
        new TypedReferenceValue(PreciseType(referenceType))

    def SomeReferenceValue(
        pc: Int,
        valueType: Set[TypeBound],
        isNull: Answer): DomainValue = {
        new GenericReferenceValue(pc, valueType, isNull)
    }

    def SomeReferenceValue(
        pc: Int,
        theType: ReferenceType,
        isNull: Answer): DomainValue = {
        isNull match {
            case Yes     ⇒ nullValue(pc)
            case No      ⇒ new NonNullReferenceValue(pc, PreciseType(theType))
            case Unknown ⇒ SomeReferenceValue(pc, Set[TypeBound](PreciseType(theType)), isNull)
        }
    }

    // -----------------------------------------------------------------------------------
    //
    // HANDLING OF COMPUTATIONS
    //
    // -----------------------------------------------------------------------------------

    override protected def givenValueOrNullPointerException(
        pc: Int,
        value: DomainValue): ReferenceValueOrNullPointerException = {

        isNull(value) match {
            case Yes ⇒ ThrowsException(newObject(pc, ObjectType.NullPointerException))
            case No  ⇒ ComputedValue(value)
            case Unknown ⇒ ComputedValueAndException(
                value.asInstanceOf[ReferenceValue].updateIsNull(pc, No),
                newObject(pc, ObjectType.NullPointerException)
            )
        }
    }

    //
    // PUSH CONSTANT VALUE
    //

    def newObject(pc: Int, objectType: ObjectType): DomainValue =
        new NonNullReferenceValue(pc, PreciseType(objectType))

    def stringValue(pc: Int, value: String): DomainValue =
        new NonNullReferenceValue(pc, PreciseType(ObjectType.String))

    def classValue(pc: Int, t: Type): DomainValue =
        new NonNullReferenceValue(pc, PreciseType(ObjectType.Class))

    //
    // QUESTION'S ABOUT VALUES
    // 

    abstract override def types(value: DomainValue): ValuesAnswer[Set[TypeBound]] = {
        value match {
            case r: ReferenceValue ⇒ Values(r.valueType)
            case _                 ⇒ super.types(value)
        }
    }

    def isSubtypeOf(value: DomainValue, superType: ReferenceType): Answer = {
        Unknown // TODO make this MORE meaningful
    }

    //
    // TYPE CHECKS AND CONVERSION
    //

    def checkcast(pc: Int, value: DomainValue, resolvedType: ReferenceType) =
        isNull(value) match {
            case Yes ⇒ /* => UNCHANGED (see spec. for details) */ ComputedValue(value)
            case _ /* <=> No | Unknown */ ⇒
                /* We need to check... */ ComputedValue(SomeReferenceValue(resolvedType))

        }

    def instanceof(pc: Int, value: DomainValue, resolvedType: ReferenceType) =
        isNull(value) match {
            case Yes ⇒ /* => FALSE */ intValue(pc, 0)
            case No  ⇒ /* We need to check... */ intValuesRange(pc, 0, 1)
            case _   ⇒ /* => FALSE, but also TRUE?*/ intValuesRange(pc, 0, 1)
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


