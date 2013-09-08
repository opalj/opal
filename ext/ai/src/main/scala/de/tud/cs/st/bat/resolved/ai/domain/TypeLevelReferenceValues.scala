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
 * @author Michael Eichberg
 */
trait TypeLevelReferenceValues { this: Domain[_] ⇒

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
        case _ ⇒
            AIImplementationError("a non-reference value cannot be (non-)null")
    }

    // -----------------------------------------------------------------------------------
    //
    // HANDLING OF CONSTRAINTS
    //
    // -----------------------------------------------------------------------------------

    override def establishIsNonNull(
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

    override def establishIsNull(
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

trait DefaultTypeLevelReferenceValues[I]
        extends DefaultValueBinding[I]
        with TypeLevelReferenceValues { domain ⇒

    def booleanValue(pc: Int, value: Boolean): DomainValue

    def someBooleanValue(pc: Int): DomainValue

    //
    //
    // HANDLING OF REFERENCE VALUES
    //
    //

    /**
     * @note Subclasses are not expected to add further state. If so, the implementation
     *      of all `equals` and `hashCode` methods need to be revised.
     */
    trait ReferenceValue
            extends super.ReferenceValue
            with IsReferenceType { outer ⇒

        //        final def context: I = domain.identifier
        //
        //        /**
        //         * Typically, the program counter where this reference value was created or
        //         * (depending on the implementation of the domain) where this reference
        //         * value was merged.
        //         */
        //        def pc: Int

        /**
         * Returns the set of types of the represented value.
         *
         * Basically, we have to distinguish two situations:
         * 1. a value that may have (depending on the control flow) different
         *    independent types
         * 2. a type for which we have multiple bounds; i.e., we don't know the precise
         *    type, but we know (e.g., due to typechecks) that it (has to) implements
         *    multiple interfaces.
         * @note It may be more efficient to use the `types` method to get a
         *    reference value's types.
         */
        def typeBound: TypeBound

        type ValueType = TypeBound
        def valueType = typeBound

        def isNull: Answer

        def updateIsNull(pc: Int, isNull: Answer): ReferenceValue

    }

    //
    // REPRESENTATIONS OF CONCRETE REFERENCE VALUES
    //    

    case class AReferenceValue(
        val pc: Int,
        val typeBound: TypeBound,
        val isNull: Answer,
        val isPrecise: Boolean)
            extends ReferenceValue {

        def foreach[U](f: TypeBound ⇒ U): Unit = f(typeBound)

        override def nonEmpty = typeBound.nonEmpty

        override def size = typeBound.size

        def headType = typeBound.head

        def foreachType[U](f: ReferenceType ⇒ U): Unit = typeBound.foreach(f(_))

        def forallTypes(f: ReferenceType ⇒ Boolean): Boolean = typeBound.forall(f)

        override def updateIsNull(pc: Int, isNull: Answer): ReferenceValue = {
            if (this.isNull == isNull)
                this
            else if (this.isNull.isUndefined)
                if (isNull.yes)
                    AReferenceValue(this.pc, Set.empty, Yes, true)
                else
                    AReferenceValue(this.pc, typeBound, isNull, isPrecise)
            else
                // this update of the value's isNull property doesn't make sense
                // hence, we swallow it to facilitate the implementation of 
                // MultipleReferenceValues
                this
        }

        override def merge(mergePC: Int, value: DomainValue): Update[ReferenceValue] = {
            if (value eq this)
                return NoUpdate

            value match {
                case v @ AReferenceValue(otherPC, otherTypeBound, otherIsNull, otherIsPrecise) ⇒
                    if (otherPC == this.pc) {
                        if (this.isNull != otherIsNull || this.typeBound != otherTypeBound) {
                            StructuralUpdate(AReferenceValue(
                                this.pc,
                                otherTypeBound ++ this.typeBound,
                                this.isNull merge otherIsNull,
                                this.isPrecise && otherIsPrecise
                            ))
                        } else
                            NoUpdate
                    } else
                        StructuralUpdate(MultipleReferenceValues(Set[AReferenceValue](this, v)))
                case mrv: MultipleReferenceValues ⇒
                    mrv.merge(mergePC, this) match {
                        case NoUpdate                 ⇒ StructuralUpdate(mrv)
                        case SomeUpdate(updatedValue) ⇒ StructuralUpdate(updatedValue)
                    }

            }
        }

        override def toString() = {
            isNull match {
                case Yes ⇒
                    "Null(pc="+pc+")"
                case _ ⇒
                    typeBound.map(_.toJava).mkString(" with ")+
                        "(pc="+pc+
                        ";isNull="+isNull+
                        ";isPrecise="+isPrecise+")"
            }
        }
    }
    object AReferenceValue {

        def apply(pc: Int,
                  referenceType: ReferenceType,
                  isNull: Answer = Unknown,
                  isPrecise: Boolean = false) = {
            new AReferenceValue(pc, Set(referenceType), isNull, isPrecise)
        }
    }

    case class MultipleReferenceValues(
        val values: Set[AReferenceValue])
            extends ReferenceValue {

        lazy val typeBound: TypeBound = values.flatMap(_.typeBound)

        def foreach[U](f: TypeBound ⇒ U): Unit = values.foreach(_.foreach(f))

        override lazy val nonEmpty = values.forall(_.nonEmpty)

        override lazy val size = values.foldLeft(0)(_ + _.size)

        def foreachType[U](f: ReferenceType ⇒ U): Unit =
            values.foreach(_.foreachType(f))

        def forallTypes(f: ReferenceType ⇒ Boolean): Boolean =
            values.forall(_.forallTypes(f))

        def headType: ReferenceType = values.collectFirst(
            { case v if v.nonEmpty ⇒ v.headType }
        ).get

        override def updateIsNull(pc: Int, isNull: Answer): ReferenceValue = {
            var createNew = false
            val updatedValues = values.map(v ⇒ {
                val updatedValue = v.updateIsNull(pc, isNull)
                if (updatedValue ne v)
                    createNew = true
                v
            })
            if (createNew)
                MultipleReferenceValues(updatedValues)
            else
                this
        }

        lazy val isPrecise: Boolean = values.forall(_.isPrecise)

        lazy val isNull: Answer = (values.head.isNull /: values.tail)(_ merge _.isNull)

        override def merge(mergePC: Int, value: DomainValue): Update[MultipleReferenceValues] = {
            if (value eq this)
                return NoUpdate

            value match {
                case otherArv: AReferenceValue ⇒
                    values.find(_.pc == otherArv.pc) match {
                        case None ⇒
                            StructuralUpdate(MultipleReferenceValues(values + otherArv))
                        case Some(thisArv) ⇒
                            thisArv.merge(mergePC, otherArv) match {
                                case NoUpdate ⇒ NoUpdate
                                case update @ SomeUpdate(updatedArv: AReferenceValue) ⇒
                                    update.updateValue(
                                        MultipleReferenceValues((values - thisArv) + updatedArv)
                                    )
                            }
                    }
                case otherMrv: MultipleReferenceValues ⇒
                    var updateType: UpdateType = NoUpdateType
                    var otherRemainingArvs = otherMrv.values
                    val newValues: Set[AReferenceValue] = values.map(thisArv ⇒ {
                        otherRemainingArvs.find(_.pc == thisArv.pc) match {
                            case None ⇒ thisArv
                            case Some(otherArv) ⇒
                                otherRemainingArvs -= otherArv
                                thisArv.merge(mergePC, otherArv) match {
                                    case NoUpdate ⇒
                                        thisArv
                                    case update @ SomeUpdate(updatedArv: AReferenceValue) ⇒
                                        updateType = updateType &: update
                                        updatedArv
                                }
                        }
                    })
                    updateType(MultipleReferenceValues(newValues ++ otherRemainingArvs))
            }
        }

        override def toString() = {
            values.mkString("Values(\n\t", ",\n\t", "\n)")
        }

    }

    //
    // FACTORY METHODS
    //

    override def nullValue(pc: Int): DomainValue =
        AReferenceValue(pc, Set.empty[ReferenceType], Yes, true)

    def SomeReferenceValue(referenceType: ReferenceType): DomainValue =
        AReferenceValue(-1, Set(referenceType), Unknown, false)

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
    // CREATE ARRAY
    //
    def newarray(pc: Int,
                 count: DomainValue,
                 componentType: FieldType): NewArrayOrNegativeArraySizeException =
        //ComputedValueAndException(TypedValue(ArrayType(componentType)), TypedValue(ObjectType.NegativeArraySizeException))
        ComputedValue(TypedValue(ArrayType(componentType)))

    /**
     * @note The componentType may be (again) an array type.
     */
    def multianewarray(pc: Int,
                       counts: List[DomainValue],
                       arrayType: ArrayType): NewArrayOrNegativeArraySizeException =
        //ComputedValueAndException(TypedValue(arrayType), TypedValue(ObjectType.NegativeArraySizeException))
        ComputedValue(TypedValue(arrayType))

    //
    // LOAD FROM AND STORE VALUE IN ARRAYS
    //
    def aaload(pc: Int, index: DomainValue, arrayref: DomainValue): ArrayLoadResult =
        types(arrayref) match {
            case HasSingleReferenceTypeBound(ArrayType(componentType)) ⇒
                ComputedValue(TypedValue(componentType))
            case _ ⇒ AIImplementationError(
                "cannot determine the type of the array's content, the array may contain either booleans or byte values: "+arrayref
            )
        }

    def aastore(pc: Int, value: DomainValue, index: DomainValue, arrayref: DomainValue) =
        ComputationWithSideEffectOnly

    //
    // PUSH CONSTANT VALUE
    //

    def newObject(pc: Int, objectType: ObjectType): DomainValue =
        AReferenceValue(pc, Set(objectType), No, true)

    def stringValue(pc: Int, value: String): DomainValue =
        AReferenceValue(pc, Set(ObjectType.String), No, true)

    def classValue(pc: Int, t: Type): DomainValue =
        AReferenceValue(pc, Set(ObjectType.Class), No, true)

    //
    // QUESTION'S ABOUT VALUES
    // 

    abstract override def types(value: DomainValue): TypesAnswer[_] = {
        value match {
            case r: ReferenceValue ⇒ r
            case _                 ⇒ super.types(value)
        }
    }

    def isSubtypeOf(value: DomainValue, superType: ReferenceType): Answer = {
        value match {
            case r: ReferenceValue ⇒
                var answer = Yes
                for (
                    referenceType ← r.typeBound
                ) {
                    isSubtypeOf(referenceType, superType) match {
                        case No         ⇒ return No
                        case nextAnswer ⇒ answer.merge(nextAnswer)
                    }
                }
                answer
            case _ ⇒
                AIImplementationError("checking the subtype for non-reference type values: "+value)
        }
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
            case Yes ⇒ /* => FALSE */ booleanValue(pc, false)
            case No  ⇒ /* We need to check... */ SomeBooleanValue
            case _   ⇒ /* => FALSE, but also TRUE?*/ SomeBooleanValue
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


