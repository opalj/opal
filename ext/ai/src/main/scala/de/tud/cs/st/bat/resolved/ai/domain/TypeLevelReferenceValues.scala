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
 * Implementation of a basic domain for handling reference values.
 *
 * This domain traces the type of a value and the nullness property of reference
 * values.
 *
 * @author Michael Eichberg
 */
trait TypeLevelReferenceValues { this: Domain[_] ⇒

    /**
     * Abstracts over all values with computational type `reference`.
     */
    trait ReferenceValue extends Value {

        /**
         * Returns `ComputationalTypeReference`.
         */
        final def computationalType: ComputationalType = ComputationalTypeReference

        /**
         * The nullness property of this `ReferenceValue`.
         */
        def isNull: Answer

        /**
         * Called by BATAI when the AI determines that the null-value property
         * of this type-level reference could be updated.
         */
        def updateIsNull(pc: Int, isNull: Answer): DomainValue

        /**
         * Checks if the type of this value is a subtype of the specified
         * reference type.
         *
         * Basically, this method implements the same semantics as the `ClassHierarchy`'s
         * `isSubtypeOf` method. But, additionally it checks if the type of the value
         * could be a subtpye of the given supertype. For example, assume that the type
         * of this reference value is `java.util.Collection` and we know that this is only
         * an upper bound. In this case an answer is `No` if and only if it is impossible
         * that the runtime type is a subtype of the given supertype. This
         * condition holds, for example, for `java.io.File`. The classes `java.io.File`
         * and `java.util.Collection` are not in an inheritance relationship. However,
         * if the specified supertype would be `java.util.List` the answer would be unknown.
         *
         * @param onNull If this value is known to be `null` and, hence, no type
         *      information is available the result of evaluating this function
         *      is returned.
         */
        def isSubtypeOf(supertype: ReferenceType, onNull: ⇒ Answer): Answer

        def addUpperBound(pc: Int, upperBound: ReferenceType): DomainValue
    }

    //
    // QUESTION'S ABOUT VALUES
    //

    /**
     * Tests if both values refer to the same object instance.
     *
     * Though this is in general intractable, there are some cases where a definitive
     * answer is possible.
     *
     * This implementation completely handles the case where at least one value
     * definitively represents the `null` value.
     * If both values represent non-null values (or just maybe `null` values) `Unknown` is
     * returned.
     *
     * @note This method is intended to be overridden by subclasses and may be the first
     *      one this is called (super call) by the overriding method to handle checks
     *      related to null. E.g.
     *      {{{
     *      super.areEqualReferences(value1,value2).
     *      orElse {
     *          ...
     *      }
     *      }}}
     *
     * @param value1 A value of type `ReferenceValue`.
     * @param value2 A value of type `ReferenceValue`.
     */
    def areEqualReferences(value1: DomainValue, value2: DomainValue): Answer = {
        val value1IsNull = isNull(value1)
        val value2IsNull = isNull(value2)
        if (value1IsNull.isDefined &&
            value2IsNull.isDefined &&
            (value1IsNull.yes || value2IsNull.yes)) {
            Answer(value1IsNull == value2IsNull)
        } else {
            // TODO [IMPROVE - areEqualReferences] If the two values are not in a subtype relationship they cannot be equal.
            Unknown
        }
    }

    /**
     * Determines the nullness-property of the given value.
     *
     * @param value A value of type `ReferenceValue`.
     */
    def isNull(value: DomainValue): Answer = value match {
        case r: ReferenceValue ⇒
            r.isNull
        case _ ⇒
            domainException(this, "\"isNull\" is not defined for non-reference values: "+value)
    }

    def isSubtypeOf(value: DomainValue, supertype: ReferenceType, onNull: ⇒ Answer): Answer =
        value match {
            case rv: ReferenceValue ⇒
                rv.isSubtypeOf(supertype, onNull)
            case _ ⇒
                domainException(this, "isSubtypeOf is not defined for non-reference values: "+value)
        }

    // -----------------------------------------------------------------------------------
    //
    // HANDLING OF CONSTRAINTS
    //
    // -----------------------------------------------------------------------------------

    override def establishUpperBound(
        pc: Int,
        bound: ReferenceType,
        value: DomainValue,
        operands: Operands,
        locals: Locals): (Operands, Locals) =
        value match {
            case referenceValue: ReferenceValue ⇒
                val newReferenceValue = referenceValue.addUpperBound(pc, bound)
                if (referenceValue eq newReferenceValue) (
                    operands,
                    locals
                )
                else (
                    operands.map(op ⇒ if (op eq value) newReferenceValue else op),
                    locals.map(l ⇒ if (l eq value) newReferenceValue else l)
                )
            case _ ⇒
                domainException(this, "setting a type bound is only possible for reference values")
        }

    protected def updateIsNull(
        pc: Int,
        value: DomainValue,
        isNull: Answer,
        operands: Operands,
        locals: Locals): (Operands, Locals) =
        value match {
            case r: ReferenceValue ⇒
                val newReferenceValue = r.updateIsNull(pc, isNull)
                if (r eq newReferenceValue) (
                    operands,
                    locals
                )
                else (
                    operands.map(op ⇒ if (op eq value) newReferenceValue else op),
                    locals.map(l ⇒ if (l eq value) newReferenceValue else l)
                )
        }

    /**
     * Updates the nullness property (`isNull == No`) of the given value.
     *
     * Calls `updateIsNull` on the given `ReferenceValue` and replaces every occurrence
     * on the stack/in a register with the updated value.
     *
     * @param value A `ReferenceValue`.
     */
    override def establishIsNonNull(
        pc: Int,
        value: DomainValue,
        operands: Operands,
        locals: Locals): (Operands, Locals) =
        updateIsNull(pc, value, No, operands, locals)

    /**
     * Updates the nullness property (`isNull == Yes`) of the given value.
     *
     * Calls `updateIsNull` on the given `ReferenceValue` and replaces every occurrence
     * on the stack/in a register with the updated value.
     *
     * @param value A `ReferenceValue`.
     */
    override def establishIsNull(
        pc: Int,
        value: DomainValue,
        operands: Operands,
        locals: Locals): (Operands, Locals) =
        updateIsNull(pc, value, Yes, operands, locals)

}

/**
 * @author Michael Eichberg
 */
trait DefaultTypeLevelReferenceValues[I]
        extends DefaultValueBinding[I]
        with TypeLevelReferenceValues { domain ⇒

    // -----------------------------------------------------------------------------------
    //
    // REPRESENTATION OF REFERENCE VALUES
    //
    // -----------------------------------------------------------------------------------

    trait ReferenceValue
            extends super.ReferenceValue
            with IsReferenceType {

        /**
         * A type bound represents the available information about a reference value's type.
         *
         * In case of reference types, a type bound may, e.g., be a set of interface types
         * which are known to be implemented by the current object. Even if the type contains
         * a class type it may just be a super class of the concrete type and, hence,
         * just represents an abstraction.
         *
         * How type bounds related to reference types are handled and
         * whether the domain makes it possible to distinguish between precise types and
         * type bounds is at the sole discretion of the domain.
         */
        type TypeBound = Set[ReferenceType]

        /**
         * Returns the set of types of the represented value.
         *
         * Basically, we have to distinguish two situations:
         * 1. a value that may have (depending on the control flow) different
         *    independent types (different values with corresponding types).
         * 2. a type for which we have multiple bounds; i.e., we don't know the precise
         *    type, but we know (e.g., due to typechecks) that it (has to) implements
         *    multiple interfaces.
         */
        def valueType: TypeBound

        def isPrecise: Boolean
    }

    object ReferenceValue {
        def unapply(rv: ReferenceValue) = Some((rv.valueType, rv.isNull, rv.isPrecise))
    }

    //
    // REPRESENTATIONS OF CONCRETE REFERENCE VALUES
    //    

    class AReferenceValue(
        val pc: Int,
        val valueType: TypeBound,
        val isNull: Answer,
        val isPrecise: Boolean)
            extends ReferenceValue {

        override def nonEmpty = valueType.nonEmpty

        override def size = valueType.size

        def foreach[U](f: TypeBound ⇒ U): Unit = f(valueType)

        def headType = valueType.head

        def foreachType[U](f: ReferenceType ⇒ U): Unit = valueType.foreach(f(_))

        def forallTypes(f: ReferenceType ⇒ Boolean): Boolean = valueType.forall(f)

        /**
         * Determines if this reference value is a subtype of the given supertype by
         * delegating to the `isSubtypeOf(ReferenceType,ReferenceType)` method of the
         * domain.
         *
         * Additionally, the `isPrecise` property is taken into consideration to ensure
         * that a `No` answer means that it is impossible that any runtime value is
         * actually a subtype of the given supertype.
         */
        def isSubtypeOf(supertype: ReferenceType, onNull: ⇒ Answer): Answer = {
            if (isNull.yes)
                return onNull

            var answer = domain.isSubtypeOf(valueType.head, supertype)
            for (
                referenceType ← valueType.tail;
                if answer.isDefined // when the answer is Unknown we do not need to continue
            ) { answer = answer.merge(domain.isSubtypeOf(referenceType, supertype)) }
            answer match {
                case Unknown ⇒ Unknown
                case Yes     ⇒ Yes
                case No ⇒
                    if (isPrecise)
                        No
                    else {
                        // .. is it conceivable that this value is still a subtype of the
                        // given reference type?
                        if (forallTypes { subtype ⇒ domain.isSubtypeOf(supertype, subtype).yes })
                            // Well it is conceivable that the value at runtime is a subtype
                            Unknown
                        else
                            No
                    }
            }
        }

        def updateIsNull(pc: Int, isNull: Answer): ReferenceValue = {
            if (this.isNull == isNull)
                this
            else if (this.isNull.isUndefined)
                if (isNull.yes)
                    AReferenceValue(this.pc, Set.empty[ReferenceType], Yes, true)
                else
                    AReferenceValue(this.pc, valueType, isNull, isPrecise)
            else
                // this update of the value's isNull property doesn't make sense
                // hence, we swallow it to facilitate the implementation of 
                // MultipleReferenceValues
                this
        }

        def addUpperBound(pc: Int, theUpperBound: ReferenceType): AReferenceValue = {
            val isSubtypeOfAnswer = isSubtypeOf(theUpperBound, Yes)
            isSubtypeOfAnswer match {
                case Yes ⇒ this
                case No if isPrecise ⇒
                    // Actually, it does not make sense to establish a new bound for a 
                    // precise object type. However, we have to handle this case 
                    // gracefully since it is possible that the value represented by
                    // this "AReferenceValue" is just one of many instances represented 
                    // by a domain value on the stack/in a register 
                    // and in this case it may make sense to establish a more stringent
                    // bound for the others.
                    this
                case No if forallTypes(domain.isSubtypeOf(theUpperBound, _).yes) ⇒
                    AReferenceValue(this.pc, Set(theUpperBound), isNull, isPrecise)
                case _ /* (No && !isPrecise || Unknown) */ ⇒
                    var newValueTypes = Set.empty[ReferenceType]
                    var addTheUpperBound = true
                    valueType.foreach { anUpperBound ⇒
                        if (theUpperBound == anUpperBound) {
                            /* do nothing */
                        } else if (domain.isSubtypeOf(theUpperBound, anUpperBound).yes) {
                            /* do nothing (we may add theUpperBound later) */
                        } else if (domain.isSubtypeOf(anUpperBound, theUpperBound).yes) {
                            addTheUpperBound = false
                            newValueTypes = newValueTypes + anUpperBound
                        } else {
                            newValueTypes = newValueTypes + anUpperBound
                        }
                    }
                    if (addTheUpperBound)
                        newValueTypes = newValueTypes + theUpperBound

                    AReferenceValue(this.pc, newValueTypes, isNull, isPrecise)
            }
        }

        override def merge(mergePC: Int, value: DomainValue): Update[DomainValue] = {
            if (value eq this)
                return NoUpdate

            value match {
                case v @ AReferenceValue(otherPC, otherValueType, otherIsNull, otherIsPrecise) ⇒
                    if (otherPC != this.pc)
                        return StructuralUpdate(MultipleReferenceValues(Set[AReferenceValue](this, v)))

                    if (this.isNull == otherIsNull &&
                        this.valueType == otherValueType &&
                        (this.isPrecise == false || this.isPrecise == otherIsPrecise))
                        return NoUpdate

                    var newValueType = this.valueType
                    otherValueType.foreach { otherValueType ⇒
                        var addOtherValueType = true
                        newValueType = newValueType.filterNot { vt ⇒
                            domain.isSubtypeOf(otherValueType, vt) match {
                                case Yes ⇒
                                    true
                                case _ ⇒
                                    if (domain.isSubtypeOf(vt, otherValueType).yes)
                                        addOtherValueType = false
                                    false
                            }
                        }
                        if (addOtherValueType)
                            newValueType = newValueType + otherValueType
                    }
                    StructuralUpdate(AReferenceValue(
                        this.pc,
                        newValueType,
                        this.isNull merge otherIsNull,
                        this.isPrecise && otherIsPrecise
                    ))

                case mrv: MultipleReferenceValues ⇒
                    mrv.merge(mergePC, this) match {
                        case NoUpdate                 ⇒ StructuralUpdate(mrv)
                        case SomeUpdate(updatedValue) ⇒ StructuralUpdate(updatedValue)
                    }
                case _ ⇒ MetaInformationUpdateIllegalValue
            }
        }

        override def equals(other: Any): Boolean = {
            other match {
                case that: AReferenceValue ⇒
                    (that eq this) || (
                        (that canEqual this) &&
                        this.pc == that.pc &&
                        this.isPrecise == that.isPrecise &&
                        this.isNull == that.isNull &&
                        this.valueType == that.valueType)
                case _ ⇒ false
            }
        }

        protected def canEqual(other: AReferenceValue): Boolean = true

        override lazy val hashCode: Int = {
            (((41 + pc) * 41 + isPrecise.hashCode()) *
                41 + isNull.hashCode()) *
                41 + valueType.hashCode()
        }

        override def toString() = {
            isNull match {
                case Yes ⇒
                    "Null(pc="+pc+")"
                case _ ⇒
                    valueType.map(_.toJava).mkString(" with ")+
                        "(pc="+pc+
                        ";isNull="+isNull+
                        ";isPrecise="+isPrecise+")"
            }
        }
    }

    /**
     * Factory and extractor for `AReferenceValue`s.
     */
    object AReferenceValue {

        def apply(
            pc: Int,
            valueType: TypeBound,
            isNull: Answer,
            isPrecise: Boolean) =
            new AReferenceValue(pc, valueType, isNull, isPrecise)

        def apply(
            pc: Int,
            referenceType: ReferenceType,
            isNull: Answer = Unknown,
            isPrecise: Boolean = false) =
            new AReferenceValue(pc, Set(referenceType), isNull, isPrecise)

        def unapply(arv: AReferenceValue): Option[(Int, TypeBound, Answer, Boolean)] =
            Some((arv.pc, arv.valueType, arv.isNull, arv.isPrecise))
    }

    case class MultipleReferenceValues(
        val values: Set[AReferenceValue])
            extends ReferenceValue {

        def isSubtypeOf(supertype: ReferenceType, onNull: ⇒ Answer): Answer = {
            val firstAnswer = values.head.isSubtypeOf(supertype, onNull)
            (firstAnswer /: values.tail) { (answer, nextReferenceValue) ⇒
                (answer merge nextReferenceValue.isSubtypeOf(supertype, onNull)).
                    orElse { return Unknown }
            }
        }

        /**
         * The set of all upper type bounds.
         */
        lazy val valueType: Set[ReferenceType] = values.flatMap(_.valueType)

        def foreach[U](f: TypeBound ⇒ U): Unit = values.foreach(_.foreach(f))

        override def nonEmpty = valueType.nonEmpty // values.forall(_.nonEmpty) // FIXME doesn't it have to be "values.exists(_.nonEmpty)"

        override def size = valueType.size

        def foreachType[U](f: ReferenceType ⇒ U): Unit = valueType.foreach(f)

        def forallTypes(f: ReferenceType ⇒ Boolean): Boolean = valueType.forall(f)

        def headType: ReferenceType = valueType.head

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

        private def updateValues(update: AReferenceValue ⇒ AReferenceValue) = {
            var createNew = false
            val updatedValues = values.map { v ⇒
                val updatedValue = update(v)
                if (updatedValue ne v)
                    createNew = true
                updatedValue
            }
            if (createNew)
                MultipleReferenceValues(updatedValues)
            else
                this
        }

        def addUpperBound(pc: Int, upperBound: ReferenceType): DomainValue = {
            updateValues { (aReferenceValue: AReferenceValue) ⇒
                aReferenceValue.addUpperBound(pc, upperBound)
            }
        }

        lazy val isPrecise: Boolean = values.forall(_.isPrecise)

        lazy val isNull: Answer = (values.head.isNull /: values.tail)(_ merge _.isNull)

        override def merge(mergePC: Int, value: DomainValue): Update[DomainValue] = {
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
                    var newValues: Set[AReferenceValue] = values.map { thisArv ⇒
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
                    }
                    if (otherRemainingArvs.size > 0) {
                        newValues ++= otherRemainingArvs
                        updateType = StructuralUpdateType
                    }
                    updateType(MultipleReferenceValues(newValues))
                case _ ⇒ MetaInformationUpdateIllegalValue
            }
        }

        override def toString() = {
            values.mkString("OneOf(\n\t", ",\n\t", ")")
        }
    }

    abstract override def types(value: DomainValue): TypesAnswer[_] = {
        value match {
            case r: ReferenceValue ⇒ r
            case _                 ⇒ super.types(value)
        }
    }

    //
    // FACTORY METHODS
    //

    def newNullValue(pc: Int): DomainValue =
        AReferenceValue(pc, Set.empty[ReferenceType], Yes, true)

    def newReferenceValue(referenceType: ReferenceType): DomainValue =
        AReferenceValue(-1, Set(referenceType), Unknown, false)

    def newObject(pc: Int, objectType: ObjectType): DomainValue =
        AReferenceValue(pc, Set[ReferenceType](objectType), No, true)

    def newVMObject(pc: Int, objectType: ObjectType): DomainValue =
        newObject(pc, objectType)

    def newStringValue(pc: Int, value: String): DomainValue =
        AReferenceValue(pc, Set[ReferenceType](ObjectType.String), No, true)

    def newClassValue(pc: Int, t: Type): DomainValue =
        AReferenceValue(pc, Set[ReferenceType](ObjectType.Class), No, true)

    def newArray(pc: Int, arrayType: ArrayType, isNull: Answer = No, isPrecise: Boolean = true): DomainValue =
        AReferenceValue(pc, Set[ReferenceType](arrayType), isNull, isPrecise)

    // -----------------------------------------------------------------------------------
    //
    // HANDLING OF COMPUTATIONS
    //
    // -----------------------------------------------------------------------------------

    //
    // CREATE ARRAY
    //
    def newarray(pc: Int,
                 count: DomainValue,
                 componentType: FieldType): NewArrayOrNegativeArraySizeException =
        //ComputedValueAndException(TypedValue(ArrayType(componentType)), TypedValue(ObjectType.NegativeArraySizeException))
        ComputedValue(newArray(pc, ArrayType(componentType)))

    /**
     * @note The componentType may be (again) an array type.
     */
    def multianewarray(pc: Int,
                       counts: List[DomainValue],
                       arrayType: ArrayType): NewArrayOrNegativeArraySizeException =
        //ComputedValueAndException(TypedValue(arrayType), TypedValue(ObjectType.NegativeArraySizeException))
        ComputedValue(newArray(pc, arrayType, No, true))

    //
    // LOAD FROM AND STORE VALUE IN ARRAYS
    //
    def aaload(pc: Int, index: DomainValue, arrayref: DomainValue): ArrayLoadResult =
        types(arrayref) match {
            case HasSingleReferenceTypeBound(ArrayType(componentType)) ⇒
                ComputedValue(newTypedValue(componentType)) // TODO use the aaload's pc as the source location...
            case _ ⇒ domainException(this,
                "cannot determine the type of the array's content, the array may contain either booleans or byte values: "+arrayref
            )
        }

    def aastore(pc: Int, value: DomainValue, index: DomainValue, arrayref: DomainValue) =
        ComputationWithSideEffectOnly
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
//            case _                        ⇒ MetaInformationUpdateIllegalValue
//        }
//    }
//
//    case class AConcreteString(val theString: String) extends ConcreteStringValue {
//        override def merge(value: DomainValue): Update[DomainValue] = value match {
//            case AConcreteString(`theString`) ⇒ NoUpdate
//            case other: ReferenceValue[_]     ⇒ StructuralUpdate(other)
//            case _                            ⇒ MetaInformationUpdateIllegalValue
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


