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
package l0

import de.tud.cs.st.util.{ Answer, Yes, No, Unknown }

/**
 * Implements the foundations for performing computations related to reference values.
 *
 * ==Extending/Implementing This Domain==
 * The following implementation decisions need to be taken into account when
 * inheriting from this trait:
 *  - By default equality of `DomainValue`s that represent reference values is
 *    reference based. I.e., two instances of `DomainValue`s that represent
 *    reference values are never equal. However, subclasses may implement their
 *    own strategy.
 *  - Instances of `DomainValue`s are always treated as immutable. Every
 *    update of a value's properties creates a new value. This is a general design
 *    decision underlying BATAI and should not be changed.
 *  - A new instance of a `DomainValue` is always exclusively created by one of the
 *    factory methods. (The factory methods generally start with a capital letter
 *    and are correspondingly documented.) This greatly facilitates domain adaptability
 *    and selective customizations.
 *
 * @author Michael Eichberg
 */
trait TypeLevelReferenceValues[+I]
        extends Domain[I]
        with GeneralizedArrayHandling {
    domain: Configuration with IntegerValuesComparison with ClassHierarchy ⇒

    // -----------------------------------------------------------------------------------
    //
    // COMMON FUNCTIONALITY TO CALCULATE THE MOST SPECIFIC COMMON SUPERTYPE OF TWO 
    // TYPES / TWO UPPER TYPE BOUNDS
    //
    // -----------------------------------------------------------------------------------

    /**
     * Calculates the set of all supertypes of the given `types`.
     */
    protected def allSupertypesOf(
        types: UIDList[ObjectType],
        reflexive: Boolean): scala.collection.Set[ObjectType] = {
        val allSupertypesOf = scala.collection.mutable.HashSet.empty[ObjectType]
        types foreach { (t: ObjectType) ⇒
            if (!allSupertypesOf.contains(t))
                allSupertypesOf ++= classHierarchy.allSupertypes(t, reflexive)
        }
        allSupertypesOf
    }

    // TODO [Performance] we could implement a function "intersectWithAllSupertypesOf(baseType: ObjectType,types : Set[ObjectType], reflexive : Boolean) to avoid that we first calculate two sets of supertypes and then need to calculate the intersection

    /**
     * Selects all types of the given set of types that do not have any subtype
     * in the given set.
     *
     * @param types A set of types that contains for each value (type) stored in the
     *      set all direct and indirect supertypes or none. For example, the intersection
     *      of the sets of all supertypes (as returned, e.g., by
     *      `ClassHiearchy.allSupertypes`) of two (independent) types satisfies this
     *      condition.
     */
    protected def leafTypes(
        types: scala.collection.Set[ObjectType]): Either[ObjectType, UIDList[ObjectType]] = {
        if (types.size == 1)
            return Left(types.head)

        val lts = types filter { aType ⇒
            !(classHierarchy.directSubtypesOf(aType) exists { t ⇒ types.contains(t) })
        }
        if (lts.isEmpty)
            Left(ObjectType.Object)
        else if (lts.size == 1)
            Left(lts.head)
        else {
            Right(UIDList(lts))
        }
    }

    /**
     * Tries to calculate the most specific common supertype of the given types.
     * If `reflexive` is `false`, no two types across both sets have to be in
     * an inheritance relation; if in doubt use `true`.
     *
     * @param upperTypeBoundB A list (set) of `ObjectType`s that are not in an
     *      inheritance relation.
     */
    protected def joinUpperTypeBounds(
        upperTypeBoundsA: UIDList[ObjectType],
        upperTypeBoundsB: UIDList[ObjectType],
        reflexive: Boolean): Either[ObjectType, UIDList[ObjectType]] = {

        if (upperTypeBoundsA == upperTypeBoundsB)
            return Right(upperTypeBoundsA)

        val allSupertypesOfA = allSupertypesOf(upperTypeBoundsA, reflexive)
        val allSupertypesOfB = allSupertypesOf(upperTypeBoundsB, reflexive)
        val commonSupertypes = allSupertypesOfA intersect allSupertypesOfB
        leafTypes(commonSupertypes)
    }

    /**
     * Tries to calculate the most specific common supertype of the given types.
     * If `reflexive` is `false`, the given types do not have to be in an
     * inheritance relation.
     *
     * @param upperTypeBoundB A list (set) of `ObjectType`s that are not in an
     *      inheritance relation.
     * @return Returns (if reflexive is `true`)
     * 		`upperTypeBoundA` if it is a supertype of at least one type
     * 		of `upperTypeBoundB`. Returns `upperTypeBoundB` if `upperTypeBoundA` is
     *   	a subtype of all types of `upperTypeBoundB`. Otherwise a new upper type
     *    	bound is calculated and returned.
     */
    protected def joinObjectTypes(
        upperTypeBoundA: ObjectType,
        upperTypeBoundB: UIDList[ObjectType],
        reflexive: Boolean): Either[ObjectType, UIDList[ObjectType]] = {

        if (reflexive) {
            var aIsSubtypeOfAllOfb = true
            val newUpperTypeBound = upperTypeBoundB filter { (b: ObjectType) ⇒
                if (domain.isSubtypeOf(b, upperTypeBoundA).yes)
                    return Left(upperTypeBoundA)

                if (domain.isSubtypeOf(upperTypeBoundA, b).yes) {
                    true
                } else {
                    aIsSubtypeOfAllOfb = false
                    false
                }
            }
            if (aIsSubtypeOfAllOfb)
                return Right(upperTypeBoundB)
            if (newUpperTypeBound.nonEmpty) {
                if (newUpperTypeBound.tail.isEmpty)
                    Left(newUpperTypeBound.head)
                else
                    return Right(newUpperTypeBound)
            }
        }
        // if we reach this point the types are in no inheritance relationship

        val allSupertypesOfA = classHierarchy.allSupertypes(upperTypeBoundA, false)
        val allSupertypesOfB = allSupertypesOf(upperTypeBoundB, false)
        val commonSupertypes = allSupertypesOfA intersect allSupertypesOfB
        leafTypes(commonSupertypes)
    }

    /**
     * Tries to calculate the most specific common supertype of the two given types.
     * If `reflexive` is `false`, the two types do not have to be in an inheritance relation.
     */
    protected def joinObjectTypes(
        upperTypeBoundA: ObjectType,
        upperTypeBoundB: ObjectType,
        reflexive: Boolean): Either[ObjectType, UIDList[ObjectType]] = {

        if (reflexive) {
            if (upperTypeBoundA eq upperTypeBoundB)
                return Left(upperTypeBoundA)
            if (domain.isSubtypeOf(upperTypeBoundB, upperTypeBoundA).yes)
                return Left(upperTypeBoundA)
            if (domain.isSubtypeOf(upperTypeBoundA, upperTypeBoundB).yes)
                return Left(upperTypeBoundB)
        }

        val allSupertypesOfA = classHierarchy.allSupertypes(upperTypeBoundA, false)
        val allSupertypesOfB = classHierarchy.allSupertypes(upperTypeBoundB, false)
        val commonSupertypes = allSupertypesOfA intersect allSupertypesOfB
        leafTypes(commonSupertypes)
    }

    /**
     * Calculates the most specific common supertype of any array type and some
     * class-/interface type.
     *
     * Recall that (Java) arrays implement `Cloneable` and `Serializable`.
     */
    protected def joinAnyArrayTypeWithMultipleTypesBound(
        thatUpperTypeBound: UIDList[ObjectType]): Either[ObjectType, UIDList[ObjectType]] = {
        import ObjectType._
        import TypeLevelReferenceValues.SerializableAndCloneable
        if (thatUpperTypeBound == SerializableAndCloneable)
            Right(thatUpperTypeBound)
        else {
            val isSerializable =
                thatUpperTypeBound exists { thatType ⇒
                    domain.isSubtypeOf(thatType, Serializable).yes
                }
            val isCloneable =
                thatUpperTypeBound exists { thatType ⇒
                    domain.isSubtypeOf(thatType, Cloneable).yes
                }
            if (isSerializable && isCloneable)
                Right(SerializableAndCloneable)
            else if (isSerializable)
                Left(Serializable)
            else if (isCloneable)
                Left(Cloneable)
            else
                Left(Object)
        }
    }

    /**
     * Calculates the most specific common supertype of any array type and some
     * class-/interface type.
     *
     * Recall that (Java) arrays implement `Cloneable` and `Serializable`.
     */
    protected def joinAnyArrayTypeWithObjectType(
        thatUpperTypeBound: ObjectType): Either[ObjectType, UIDList[ObjectType]] = {
        import ObjectType._
        if ((thatUpperTypeBound eq Object) ||
            (thatUpperTypeBound eq Serializable) ||
            (thatUpperTypeBound eq Cloneable))
            Left(thatUpperTypeBound)
        else {
            var newUpperTypeBound: UIDList[ObjectType] = UIDList.empty
            if (domain.isSubtypeOf(thatUpperTypeBound, Serializable).yes)
                newUpperTypeBound += Serializable
            if (domain.isSubtypeOf(thatUpperTypeBound, Cloneable).yes)
                newUpperTypeBound += Cloneable
            if (newUpperTypeBound.isEmpty)
                Left(Object)
            else if (newUpperTypeBound.tail.isEmpty)
                Left(newUpperTypeBound.head)
            else
                Right(newUpperTypeBound)
        }
    }

    /**
     * Calculates the most specific common supertype of two array types.
     *
     * @return `Left(<SOME_ARRAYTYPE>)` if the calculated type can be represented using
     *      an `ArrayType` and `Right(UIDList(ObjectType.Serializable, ObjectType.Cloneable))`
     *      if the two arrays do not have an `ArrayType` as a most specific common supertype.
     */
    protected def joinArrayTypes(
        thisUpperTypeBound: ArrayType,
        thatUpperTypeBound: ArrayType): Either[ArrayType, UIDList[ObjectType]] = {
        if (thisUpperTypeBound eq thatUpperTypeBound)
            Left(thisUpperTypeBound)
        else if (thisUpperTypeBound.componentType.isBaseType ||
            thatUpperTypeBound.componentType.isBaseType) {
            // Scenario:
            // E.g., imagine that we have a method that "just" wants to 
            // serialize some data. In such a case the method may be passed 
            // different arrays with different primitive values.
            Right(TypeLevelReferenceValues.SerializableAndCloneable)
        } else {
            // When we reach this point, 
            // both component types are reference types
            val thatComponentType = thatUpperTypeBound.componentType.asReferenceType
            val thisComponentType = thisUpperTypeBound.componentType.asReferenceType
            if (domain.isSubtypeOf(thatComponentType, thisComponentType).yes)
                Left(thisUpperTypeBound)
            else if (domain.isSubtypeOf(thisComponentType, thatComponentType).yes)
                Left(thatUpperTypeBound)
            else
                // This is the most general fallback and we are losing some information
                // when compared to a solution that calculates the least 
                // upper type bound. However, in that case we need - 
                // in general - to support array values with multiple type 
                // bounds, which we currently don't do.
                Left(ArrayType.ArrayOfObjects)
        }
    }

    // ---------------------------------1-------------------------------------------------
    //
    // REPRESENTATION OF REFERENCE VALUES
    //
    // -----------------------------------------------------------------------------------

    /**
     * Abstracts over all values with computational type `reference`. I.e.,
     * abstracts over class and array values and also the `null` value.
     */
    protected trait ReferenceValue extends Value with IsReferenceValue {
        this: DomainValue ⇒

        /**
         * Returns `ComputationalTypeReference`.
         */
        final override def computationalType: ComputationalType =
            ComputationalTypeReference

        /**
         * Returns `Yes` iff this value is guaranteed to be `null` at runtime and
         * returns `No` iff the value is not `null` at runtime, in all other cases
         * `Unknown` is returned.
         *
         * This default implementation always returns `Unknown`.
         */
        override def isNull: Answer = Unknown

        /**
         * Refines this value's `isNull` property if meaningful.
         *
         * @param pc The program counter of the instruction that was the reason
         * 		for the refinement.
         * @param isNull This value's new null-ness property. `isNull` either
         * 		has to be `Yes` or `No`.
         * @return The refined value, if the refinement was meaningful. Otherwise
         * 		`this`. Note, if this value's `isNull` property is `Unknown`
         * 		`this` may also be returned, but in that case subsequent analyses may
         *   	be less precise.
         */
        def refineIsNull(pc: PC, isNull: Answer): DomainValue

        /**
         * Returns `true` if the type information associated with this value is precise.
         * I.e., the type information associated with this value precisely models the
         * runtime type. If, `isPrecise` returns true, the type of this value can
         * generally be assumed to represent a class type (not an interface type) or
         * an array type. However, this domain also supports the case that `isPrecise`
         * returns `true` even though the associated type identifies an interface type
         * or an abstract class type. The later case may be interesting
         *
         * This default implementation always returns `false`.
         */
        override def isPrecise: Boolean = false

        /**
         * Tests if this value's type is potentially a subtype of the given type.
         * This test should take the precision of the type information into account.
         * That is, if the currently available type information is not precise and
         * the given type has a subtype that is always a subtype of the current
         * upper type bound, then `Unknown` should to be returned. Given that it may be
         * computationally intensive to determine whether two types have a common subtype
         * it may be better to just return `Unknown` in case that this type and the
         * given type are not in a direct inheritance relationship.
         *
         * @note If this value represents the `null` value this method is not supported.
         *
         * @return The default implementation always returns `Unknown`.
         */
        @throws[DomainException]("if this value is null")
        override def isValueSubtypeOf(referenceType: ReferenceType): Answer = Unknown

        /**
         * Refines the upper bound to this value's type.
         *
         * This call can be ignored if the type
         * information related to this value is precise, i.e., if we know that we
         * precisely capture the runtime type of this value. However, refining
         * the upper type bound for a `null` value is not supported.
         */
        @throws[DomainException]("if this value is null")
        def refineUpperTypeBound(pc: PC, supertype: ReferenceType): DomainValue

    }

    /**
     * Represents the value `null`. Null values are basically found in the following two
     * cases:
     *  1. A null value was pushed onto the stack using `aconst_null`.
     *  2. A reference value that is not guaranteed to be non-null is tested against
     *    `null` using `ifnull` or `ifnonnull` and we are now on the branch where
     *    the value has to be `null`.
     */
    protected trait NullValue extends ReferenceValue {
        this: DomainValue ⇒

        final override def referenceValues: Iterable[IsAReferenceValue] = Iterable(this)

        /**
         * Returns `Yes`.
         */
        final override def isNull = Yes

        final override def refineIsNull(pc: PC, isNull: Answer): DomainValue = this

        /**
         * Returns `true`.
         */
        final override def isPrecise = true

        /**
         * Returns an empty upper type bound.
         */
        final override def upperTypeBound: UpperTypeBound = UIDList.empty

        /**
         * Throws a new `DomainException` that states that this method is not supported.
         */
        @throws[DomainException]("always - this method is not supported")
        final override def isValueSubtypeOf(referenceType: ReferenceType): Nothing =
            domainException(domain, "isSubtypeOf is not defined for \"null\" values")

        override def refineUpperTypeBound(
            pc: PC,
            supertype: ReferenceType): DomainValue = this

        override def summarize(pc: PC): DomainValue = this

        override def adapt[ThatI >: I](
            target: Domain[ThatI],
            pc: PC): target.DomainValue =
            target.NullValue(pc)

        override def toString: String = "ReferenceValue(null)"
    }

    /**
     * A reference value that is associated with a single (upper) type (bound).
     *
     * @note This class was introduced for performance reasons.
     */
    protected trait SReferenceValue[T <: ReferenceType] extends ReferenceValue {
        this: DomainValue ⇒

        val theUpperTypeBound: T

        override def referenceValues: Iterable[IsAReferenceValue] = Iterable(this)

        override def upperTypeBound: UpperTypeBound = UIDList(theUpperTypeBound)

        override def summarize(pc: PC): DomainValue = this

        override def toString: String = "ReferenceValue("+theUpperTypeBound.toJava+")"

    }

    /**
     * Represents a class/interface value.
     */
    protected trait ObjectValue extends ReferenceValue {
        this: DomainValue ⇒

    }

    /**
     * Represents an array value.
     */
    protected trait ArrayValue extends ReferenceValue {
        this: DomainValue ⇒

        /**
         * Returns `Yes` if we can statically determine that the given value can
         * be stored in the array represented by this `ArrayValue`.
         */
        /*ABSTRACT*/ def isAssignable(value: DomainValue): Answer

        /**
         *
         */
        /*ABSTRACT*/ def doLoad(
            pc: PC,
            index: DomainValue,
            potentialExceptions: ExceptionValues): ArrayLoadResult

        def load(pc: PC, index: DomainValue): ArrayLoadResult = {
            // @note
            // The case "this.isNull == Yes" will not occur as the value "null" is always
            // represented by an instance of the respective class and this situation
            // is checked for by the domain-level method.

            val validIndex =
                isSomeValueInRange(index, IntegerConstant0, getLength(pc))
            if (validIndex.no)
                return justThrows(ArrayIndexOutOfBoundsException(pc))

            var thrownExceptions = List.empty[ExceptionValue]
            if (isNull.maybeYes && throwNullPointerException)
                thrownExceptions = NullPointerException(pc) :: thrownExceptions
            if (validIndex.maybeNo && throwArrayIndexOutOfBoundsException)
                thrownExceptions = ArrayIndexOutOfBoundsException(pc) :: thrownExceptions

            doLoad(pc, index, thrownExceptions)
        }

        def doArraystore(
            pc: PC,
            value: DomainValue,
            index: DomainValue): Unit = { /* Empty by default. */ }

        /**
         * @note It is in general not necessary to override this method. If you need some
         *      special handling if a value is stored in an array, override the method
         *      `doArraystore`.
         * @see `doArraystore` for further information.
         */
        def store(
            pc: PC,
            value: DomainValue,
            index: DomainValue): ArrayStoreResult = {
            // @note
            // The case "this.isNull == Yes" will not occur as the value "null" is always
            // represented by an instance of the respective class

            val validIndex =
                isSomeValueInRange(index, IntegerConstant0, getLength(pc))
            if (validIndex.no)
                return justThrows(ArrayIndexOutOfBoundsException(pc))

            if (isAssignable(value).no)
                return justThrows(ArrayStoreException(pc))

            var thrownExceptions = List.empty[ExceptionValue]
            if (validIndex.maybeNo && throwArrayIndexOutOfBoundsException)
                thrownExceptions = ArrayIndexOutOfBoundsException(pc) :: thrownExceptions
            if (isNull.maybeYes && throwNullPointerException)
                thrownExceptions = NullPointerException(pc) :: thrownExceptions

            doArraystore(pc, value, index)

            ComputationWithSideEffectOrException(thrownExceptions)
        }

        def getLength(pc: PC): DomainValue =
            IntegerValue(pc)

        def length(pc: PC): Computation[DomainValue, ExceptionValue] = {
            if (isNull == Unknown && throwNullPointerException)
                ComputedValueAndException(getLength(pc), NullPointerException(pc))
            else
                ComputedValue(getLength(pc))
        }
    }

    /**
     * Returns the given value as a DomainValue. Basically just performs a type cast
     * and is intended to be used to communicate that the value has to be a reference
     * value (if the underlying byte code is valid.)
     */
    def asReferenceValue(value: DomainValue): ReferenceValue =
        value.asInstanceOf[ReferenceValue]

    def asObjectValue(value: DomainValue): ObjectValue =
        value.asInstanceOf[ObjectValue]

    def asArrayValue(value: DomainValue): ArrayValue =
        value.asInstanceOf[ArrayValue]

    // -----------------------------------------------------------------------------------
    //
    // QUESTION'S ABOUT VALUES
    //
    // -----------------------------------------------------------------------------------

    /**
     * Determines if the type described by the first set of upper type bounds is
     * a subtype of the second type. I.e., it checks if for all types of the
     * subtypes upper type bound a type in the supertypes type exists that is a
     * supertype of the respective subtype.
     */
    protected def isSubtypeOf(
        subtypes: UpperTypeBound,
        supertypes: UpperTypeBound): Boolean = {
        subtypes forall { subtype ⇒
            supertypes exists { supertype ⇒
                domain.isSubtypeOf(subtype, supertype).yes
            }
        }
    }

    protected def summarizeReferenceValues(
        pc: PC,
        values: Iterable[DomainValue]): DomainValue = {
        val summarizedValues = values.map(_.summarize(pc))

        summarizedValues.tail.foldLeft(summarizedValues.head) { (summarizedValue, nextValue) ⇒
            summarizedValue.join(pc, nextValue) match {
                case NoUpdate             ⇒ summarizedValue
                case SomeUpdate(newValue) ⇒ newValue
            }
        }
    }

    /**
     * Tests if both values refer to the same object instance.
     *
     * Though this is in general intractable, there are some cases where a definitive
     * answer is possible.
     *
     * This implementation completely handles the case where at least one value
     * definitively represents the `null` value.
     * If both values represent non-null values (or just maybe `null` values) `Unknown`
     * is returned.
     *
     * @note This method is intended to be overridden by subclasses and may be the first
     *      one this is called (super call) by the overriding method to handle checks
     *      related to null. E.g.
     *      {{{
     *      super.areEqualReferences(value1,value2).orElse {
     *          ...
     *      }
     *      }}}
     *
     * @param value1 A value of type `ReferenceValue`.
     * @param value2 A value of type `ReferenceValue`.
     */
    override def areEqualReferences(value1: DomainValue, value2: DomainValue): Answer = {
        val v1 = asReferenceValue(value1)
        val v2 = asReferenceValue(value2)
        val value1IsNull = v1.isNull
        val value2IsNull = v2.isNull
        if (value1IsNull.yes && value2IsNull.isDefined)
            // both are null or the second one is definitively not null
            Answer(value2IsNull.yes)
        else if (value2IsNull.yes && value1IsNull.isDefined)
            // both are null or the first one is definitively not null
            Answer(value1IsNull.yes)
        else if (v1.isPrecise && v2.isPrecise && v1.upperTypeBound != v2.upperTypeBound)
            No
        else
            // we could also check if it is conceivable that both values are not equal based 
            // on the available type information... However, if we only have a 
            // fragmented/incomplete class hierarchy, the information is most likely of limited
            // value
            Unknown
    }

    final override def isValueSubtypeOf(
        value: DomainValue,
        supertype: ReferenceType): Answer =
        asReferenceValue(value).isValueSubtypeOf(supertype)

    /**
     * Determines the nullness-property of the given value.
     *
     * @param value A value of type `ReferenceValue`.
     */
    final override def isNull(value: DomainValue): Answer =
        asReferenceValue(value).isNull

    /**
     * Defines an extractor method facilitate matching `NullValue`s.
     */
    object NullValue {
        def unapply(value: NullValue): Boolean = true
    }

    // -----------------------------------------------------------------------------------
    //
    // ARRAY RELATED OPERATIONS
    //
    // -----------------------------------------------------------------------------------

    //
    // CREATE ARRAY
    //

    /**
     *
     * @note It is generally not necessary to override this method.
     */
    override def newarray(
        pc: PC,
        count: DomainValue,
        componentType: FieldType): Computation[DomainValue, ExceptionValue] = {
        val validCount =
            isSomeValueInRange(count, 0, Int.MaxValue)
        if (validCount.no)
            return throws(NegativeArraySizeException(pc))

        val newarray = NewArray(pc, count, ArrayType(componentType))
        if (validCount.isUndefined && throwNegativeArraySizeException)
            ComputedValueAndException(newarray, NegativeArraySizeException(pc))
        else
            ComputedValue(newarray)
    }

    /**
     * @note The componentType may be (again) an array type.
     * @note It is generally not necessary to override this method.
     */
    override def multianewarray(
        pc: PC,
        counts: List[DomainValue],
        arrayType: ArrayType): Computation[DomainValue, ExceptionValue] = {
        var validCounts: Answer = Yes
        counts foreach { (count) ⇒
            val validCount = isSomeValueInRange(count, 0, Int.MaxValue)
            if (validCount.no)
                return throws(NegativeArraySizeException(pc))
            else if (validCount.isUndefined)
                validCounts = Unknown
        }

        val newarray =
            if (counts.tail.isEmpty)
                NewArray(pc, counts.head, arrayType)
            else
                NewArray(pc, counts, arrayType)
        if (validCounts.isUndefined && throwNegativeArraySizeException)
            ComputedValueAndException(newarray, NegativeArraySizeException(pc))
        else
            ComputedValue(newarray)
    }

    //
    // OPERATIONS ON ARRAYS
    // 

    /**
     * Loads the value stored in the array at the given index or throws an
     * exception (`NullPointerException` or `IndexOutOfBoundsException`).
     *
     * @note It is in general not necessary to override this method. If you need
     *      some special handling refine the `load` method defined by the trait
     *      `ArrayValue`.
     */
    override def arrayload(
        pc: PC,
        index: DomainValue,
        arrayref: DomainValue): ArrayLoadResult = {
        if (isNull(arrayref).yes)
            justThrows(NullPointerException(pc))
        else
            // if the bytecode is valid, the type cast (asArrayValue) is safe
            asArrayValue(arrayref).load(pc, index)
    }

    /**
     * Stores the given value in the array at the given index or throws an exception
     * (`NullPointerException`, `ArrayStoreException` or `IndexOutOfBoundsException`).
     *
     * @note It is in general not necessary to override this method. If you need
     *      some special handling refine the `store` method defined by the trait
     *      `ArrayValue`.
     */
    override def arraystore(
        pc: PC,
        value: DomainValue,
        index: DomainValue,
        arrayref: DomainValue): ArrayStoreResult = {
        if (isNull(arrayref).yes)
            justThrows(NullPointerException(pc))
        else
            // if the bytecode is valid, the type cast (asArrayValue) is safe
            asArrayValue(arrayref).store(pc, value, index)
    }

    /**
     * Returns the array's length or throws a `NullPointerException` if the given
     * reference is `null`.
     *
     * @note It is in general not necessary to override this method. If you need
     *      some special handling refine the trait `ArrayValue`.
     */
    override def arraylength(
        pc: PC,
        arrayref: DomainValue): Computation[DomainValue, ExceptionValue] = {
        if (isNull(arrayref).yes)
            throws(NullPointerException(pc))
        else
            asArrayValue(arrayref).length(pc)
    }

    // -----------------------------------------------------------------------------------
    //
    // ADDITIONAL FACTORY METHODS
    //
    // -----------------------------------------------------------------------------------

    /**
     * Factory method to create a new domain value that represents a newly created
     * array (non-null) with the size determined by count that is empty.
     *
     * ==Typical Usage==
     * This factory method is (implicitly) used, e.g., by BATAI when a new array
     * instruction is found.
     *
     * ==Summary==
     * The properties of the value are:
     *
     *  - Type: '''Precise'''
     *  - Null: '''No'''
     *  - Size: '''Count'''
     */
    def NewArray(pc: PC, count: DomainValue, arrayType: ArrayType): DomainValue

    /**
     * Factory method to create a new domain value that represents a newly created
     * array (non-null) with the size determined by count that is empty.
     *
     * ==Typical Usage==
     * This factory method is (implicitly) used, e.g., by BATAI when a new array
     * instruction is found.
     *
     * ==Summary==
     * The properties of the value are:
     *
     *  - Type: '''Precise'''
     *  - Null: '''No'''
     *  - Size: '''Counts''' (for the number of dimension for which a
     *  					value (count) is given)
     */
    def NewArray(pc: PC, counts: List[DomainValue], arrayType: ArrayType): DomainValue

    /**
     * Creates a new `DomainValue` that represents an array value with unknown
     * values and where the specified type may also just be an upper type bound
     * (unless the component type is a primitive type or an array of primitives.)
     *
     * ==Typical Usage==
     * This factory method is (typically) used to create a domain value that represents
     * an array if we know nothing specific about the array. E.g., if you want to
     * analyze a method that takes an array as a parameter.
     *
     * ==Summary==
     * The properties of the value are:
     *  - Type: '''Upper Bound'''
     *  - Null: '''Unknown'''
     *  - Size: '''Unknown'''
     *
     * @note Java Arrays are covariant. I.e., `Object[] a = new Serializable[100];`
     *      is valid.
     */
    def ArrayValue(pc: PC, arrayType: ArrayType): DomainValue

    // -----------------------------------------------------------------------------------
    //
    // HANDLING OF CONSTRAINTS
    //
    // -----------------------------------------------------------------------------------

    protected def updateOperandsAndLocals(
        oldValue: DomainValue,
        newValue: DomainValue,
        operands: Operands,
        locals: Locals): (Operands, Locals) = {
        if (oldValue == newValue)
            (
                operands,
                locals
            )
        else
            (
                operands.map(op ⇒ if (op eq oldValue) newValue else op),
                locals.map(l ⇒ if (l eq oldValue) newValue else l)
            )
    }

    override def establishUpperBound(
        pc: PC,
        bound: ReferenceType,
        value: DomainValue,
        operands: Operands,
        locals: Locals): (Operands, Locals) = {
        updateOperandsAndLocals(
            value,
            asReferenceValue(value).refineUpperTypeBound(pc, bound),
            operands,
            locals)
    }

    protected def refineIsNull(
        pc: PC,
        value: DomainValue,
        isNull: Answer,
        operands: Operands,
        locals: Locals): (Operands, Locals) = {
        updateOperandsAndLocals(
            value,
            asReferenceValue(value).refineIsNull(pc, isNull),
            operands,
            locals)
    }

    /**
     * Refines the "null"ness property (`isNull == No`) of the given value.
     *
     * Calls `refineIsNull` on the given `ReferenceValue` and replaces every occurrence
     * on the stack/in a register with the updated value.
     *
     * @param value A `ReferenceValue` that does not represent the value `null`.
     */
    override def establishIsNonNull(
        pc: PC,
        value: DomainValue,
        operands: Operands,
        locals: Locals): (Operands, Locals) =
        refineIsNull(pc, value, No, operands, locals)

    /**
     * Updates the "null"ness property (`isNull == Yes`) of the given value.
     *
     * Calls `refineIsNull` on the given `ReferenceValue` and replaces every occurrence
     * on the stack/in a register with the updated value.
     *
     * @param value A `ReferenceValue`.
     */
    override def establishIsNull(
        pc: PC,
        value: DomainValue,
        operands: Operands,
        locals: Locals): (Operands, Locals) =
        refineIsNull(pc, value, Yes, operands, locals)
}
/**
 * Defines domain-independent, commonly used upper type bounds.
 *
 * @author Michael Eichberg
 */
object TypeLevelReferenceValues {

    /**
     * Least upper type bound of Java arrays. That is, every Java array
     * is always `Serializable` and `Cloneable`.
     */
    val SerializableAndCloneable: UIDList[ObjectType] =
        UIDList(ObjectType.Serializable, ObjectType.Cloneable)

}
