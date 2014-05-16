/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
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
package org.opalj
package ai
package domain
package l0

import org.opalj.util.{ Answer, Yes, No, Unknown }
import org.opalj.collection.immutable.UIDSet

import br._

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
 *  - Instances of `DomainValue`s are always immutable or are at least considered and
 *    treated as immutable. Every
 *    update of a value's properties creates a new value. This is a general design
 *    decision underlying OPAL-AI and should not be changed.
 *  - A new instance of a `DomainValue` is always exclusively created by one of the
 *    factory methods. (The factory methods generally start with a capital letter
 *    and are correspondingly documented.) This greatly facilitates domain adaptability
 *    and selective customizations.
 *
 * @author Michael Eichberg
 */
trait TypeLevelReferenceValues
        extends Domain
        with GeneralizedArrayHandling {
    domain: Configuration with IntegerValuesComparison with ClassHierarchy ⇒

    /**
     * Thrown to indicate that some refinement was not possible.
     *
     * @author Michael Eichberg
     */
    case class ImpossibleRefinement(
        value: DomainValue,
        refinementGoal: String)
            extends Exception(
                "refining "+value+" failed: "+refinementGoal,
                null, true, false)

    /**
     * Merges those exceptions that have the same upper type bound. This ensures
     * that per exception type only one DomainValue (which may be a
     * `MultipleReferenceValues`) is used. The standard join/merge operation does
     * the merge based on the origin of a value.
     */
    def mergeMultipleExceptionValues(
        pc: PC,
        v1s: ExceptionValues,
        v2s: ExceptionValues): ExceptionValues = {
        var v: List[ExceptionValue] = Nil
        var remainingv2s = v2s
        v1s foreach { v1 ⇒
            val v1UTB = domain.asObjectValue(v1).upperTypeBound
            remainingv2s find (domain.asObjectValue(_).upperTypeBound == v1UTB) match {
                case Some(v2) ⇒
                    remainingv2s = remainingv2s filterNot (_ == v2)
                    v = (mergeDomainValues(pc, v1, v2)) :: v
                case None ⇒
                    v = v1 :: v
            }
        }
        v ++ remainingv2s
    }

    protected[this] def mergeDEsComputations(
        pc: PC,
        c1: Computation[DomainValue, ExceptionValues],
        c2: Computation[DomainValue, ExceptionValues]): Computation[DomainValue, ExceptionValues] = {

        c1 match {
            case ComputationWithResultAndException(r1, e1) ⇒
                c2 match {
                    case ComputationWithResultAndException(r2, e2) ⇒
                        ComputedValueAndException(mergeDomainValues(pc, r1, r2), mergeMultipleExceptionValues(pc, e1, e2))
                    case ComputationWithResult(r2) ⇒
                        ComputedValueAndException(mergeDomainValues(pc, r1, r2), e1)
                    case ComputationWithException(e2) ⇒
                        ComputedValueAndException(r1, mergeMultipleExceptionValues(pc, e1, e2))
                }

            case ComputationWithResult(r1) ⇒
                c2 match {
                    case ComputationWithResultAndException(r2, e2) ⇒
                        ComputedValueAndException(mergeDomainValues(pc, r1, r2), e2)
                    case ComputationWithResult(r2) ⇒
                        ComputedValue(mergeDomainValues(pc, r1, r2))
                    case ComputationWithException(e2) ⇒
                        ComputedValueAndException(r1, e2)
                }

            case ComputationWithException(e1) ⇒
                c2 match {
                    case ComputationWithResultAndException(r2, e2) ⇒
                        ComputedValueAndException(r2, mergeMultipleExceptionValues(pc, e1, e2))
                    case ComputationWithResult(r2) ⇒
                        ComputedValueAndException(r2, e1)
                    case ComputationWithException(e2) ⇒
                        ThrowsException(mergeMultipleExceptionValues(pc, e1, e2))
                }
        }
    }

    protected[this] def mergeEsComputations(
        pc: PC,
        c1: Computation[Nothing, ExceptionValues],
        c2: Computation[Nothing, ExceptionValues]): Computation[Nothing, ExceptionValues] = {

        (c1, c2) match {
            case (ComputationWithException(e1), ComputationWithException(e2)) ⇒
                ComputationWithSideEffectOrException(mergeMultipleExceptionValues(pc, e1, e2))
            case (ComputationWithException(e1), _ /*ComputationWithoutException*/ ) ⇒
                c1
            case (_ /*ComputationWithoutException*/ , ComputationWithException(e2)) ⇒
                c2
            case _ ⇒
                ComputationWithSideEffectOnly
        }
    }

    protected[this] def mergeDEComputations(
        pc: PC,
        c1: Computation[DomainValue, ExceptionValue],
        c2: Computation[DomainValue, ExceptionValue]): Computation[DomainValue, ExceptionValue] = {

        c1 match {
            case ComputationWithResultAndException(r1, e1) ⇒
                c2 match {
                    case ComputationWithResultAndException(r2, e2) ⇒
                        ComputedValueAndException(mergeDomainValues(pc, r1, r2), mergeDomainValues(pc, e1, e2))
                    case ComputationWithResult(r2) ⇒
                        ComputedValueAndException(mergeDomainValues(pc, r1, r2), e1)
                    case ComputationWithException(e2) ⇒
                        ComputedValueAndException(r1, mergeDomainValues(pc, e1, e2))
                }

            case ComputationWithResult(r1) ⇒
                c2 match {
                    case ComputationWithResultAndException(r2, e2) ⇒
                        ComputedValueAndException(mergeDomainValues(pc, r1, r2), e2)
                    case ComputationWithResult(r2) ⇒
                        ComputedValue(mergeDomainValues(pc, r1, r2))
                    case ComputationWithException(e2) ⇒
                        ComputedValueAndException(r1, e2)
                }

            case ComputationWithException(e1) ⇒
                c2 match {
                    case ComputationWithResultAndException(r2, e2) ⇒
                        ComputedValueAndException(r2, mergeDomainValues(pc, e1, e2))
                    case ComputationWithResult(r2) ⇒
                        ComputedValueAndException(r2, e1)
                    case ComputationWithException(e2) ⇒
                        ThrowsException(mergeDomainValues(pc, e1, e2))
                }
        }
    }

    // -----------------------------------------------------------------------------------
    //
    // COMMON FUNCTIONALITY TO CALCULATE THE MOST SPECIFIC COMMON SUPERTYPE OF TWO 
    // TYPES / TWO UPPER TYPE BOUNDS
    //
    // -----------------------------------------------------------------------------------

    // TODO [Next Step] Think about how to calculate common super types if the class hierarchy is not complete

    /**
     * Calculates the set of all supertypes of the given `types`.
     */
    protected def allSupertypesOf(
        types: UIDSet[ObjectType],
        reflexive: Boolean): scala.collection.Set[ObjectType] = {
        val allSupertypesOf = scala.collection.mutable.HashSet.empty[ObjectType]
        types foreach { (t: ObjectType) ⇒
            if (!allSupertypesOf.contains(t) && classHierarchy.isKnown(t))
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
     *      condition. If `types` is empty, the returned leaf type is `ObjectType.Object`.
     *      which should always be a safe fallback.
     */
    protected def leafTypes(
        types: scala.collection.Set[ObjectType]): Either[ObjectType, UIDSet[ObjectType]] = {
        if (types.isEmpty)
            return Left(ObjectType.Object)

        if (types.size == 1)
            return Left(types.head)

        val lts = types filter { aType ⇒
            classHierarchy.isUnknown(aType) ||
                !(classHierarchy.directSubtypesOf(aType) exists { t ⇒ types.contains(t) })
        }
        if (lts.size == 1)
            Left(lts.head)
        else {
            Right(UIDSet(lts))
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
        upperTypeBoundsA: UIDSet[ObjectType],
        upperTypeBoundsB: UIDSet[ObjectType],
        reflexive: Boolean): Either[ObjectType, UIDSet[ObjectType]] = {

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
        upperTypeBoundB: UIDSet[ObjectType],
        reflexive: Boolean): Either[ObjectType, UIDSet[ObjectType]] = {

        if (reflexive) {
            var aIsSubtypeOfAllOfb = true
            val newUpperTypeBound = upperTypeBoundB filter { (b: ObjectType) ⇒
                if (domain.isSubtypeOf(b, upperTypeBoundA).isYes)
                    return Left(upperTypeBoundA)

                if (domain.isSubtypeOf(upperTypeBoundA, b).isYes) {
                    true // => in newUpperTypeBound
                } else {
                    aIsSubtypeOfAllOfb = false
                    false // => no in newUpperTypeBound
                }
            }
            if (aIsSubtypeOfAllOfb)
                return Right(upperTypeBoundB)
            if (newUpperTypeBound.nonEmpty) {
                if (newUpperTypeBound.containsOneElement)
                    Left(newUpperTypeBound.first)
                else
                    return Right(newUpperTypeBound)
            }
        }
        // if we reach this point the types are in no inheritance relationship

        if (classHierarchy.isUnknown(upperTypeBoundA)) {
            // there is nothing that we can do...
            return Left(ObjectType.Object)
        }

        val allSupertypesOfA = classHierarchy.allSupertypes(upperTypeBoundA, false)
        val allSupertypesOfB = allSupertypesOf(upperTypeBoundB, false)
        val commonSupertypes = allSupertypesOfA intersect allSupertypesOfB
        leafTypes(commonSupertypes)
    }

    /**
     * Tries to calculate the most specific common supertype of the two given types.
     * If `reflexive` is `false`, the two types do not have to be in an inheritance
     * relation.
     *
     * If the class hierarchy is not complete, a best guess is made.
     */
    protected def joinObjectTypes(
        upperTypeBoundA: ObjectType,
        upperTypeBoundB: ObjectType,
        reflexive: Boolean): Either[ObjectType, UIDSet[ObjectType]] = {

        if (reflexive) {
            if (upperTypeBoundA eq upperTypeBoundB)
                return Left(upperTypeBoundA)
            if (domain.isSubtypeOf(upperTypeBoundB, upperTypeBoundA).isYes)
                return Left(upperTypeBoundA)
            if (domain.isSubtypeOf(upperTypeBoundA, upperTypeBoundB).isYes)
                return Left(upperTypeBoundB)
        }

        if (classHierarchy.isUnknown(upperTypeBoundA) ||
            classHierarchy.isUnknown(upperTypeBoundB)) {
            // there is not too much that we can do...
            return Left(ObjectType.Object)
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
        thatUpperTypeBound: UIDSet[ObjectType]): Either[ObjectType, UIDSet[ObjectType]] = {
        import ObjectType._
        import TypeLevelReferenceValues.SerializableAndCloneable
        if (thatUpperTypeBound == SerializableAndCloneable)
            Right(thatUpperTypeBound)
        else {
            val isSerializable =
                thatUpperTypeBound exists { thatType ⇒
                    domain.isSubtypeOf(thatType, Serializable).isYes
                }
            val isCloneable =
                thatUpperTypeBound exists { thatType ⇒
                    domain.isSubtypeOf(thatType, Cloneable).isYes
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
        thatUpperTypeBound: ObjectType): Either[ObjectType, UIDSet[ObjectType]] = {
        import ObjectType._
        if ((thatUpperTypeBound eq Object) ||
            (thatUpperTypeBound eq Serializable) ||
            (thatUpperTypeBound eq Cloneable))
            Left(thatUpperTypeBound)
        else {
            var newUpperTypeBound: UIDSet[ObjectType] = UIDSet.empty
            if (domain.isSubtypeOf(thatUpperTypeBound, Serializable).isYes)
                newUpperTypeBound += Serializable
            if (domain.isSubtypeOf(thatUpperTypeBound, Cloneable).isYes)
                newUpperTypeBound += Cloneable
            if (newUpperTypeBound.isEmpty)
                Left(Object)
            else if (newUpperTypeBound.containsOneElement)
                Left(newUpperTypeBound.first)
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
        thatUpperTypeBound: ArrayType): Either[ArrayType, UIDSet[ObjectType]] = {
        // We have ALSO to consider the following corner cases:
        // Foo[][] and Bar[][] => Object[][] (Object is the common super class)
        // Object[] and int[][] => Object[] (which may contain arrays of int values...)
        // Foo[] and int[][] => Object[]
        // int[] and Object[][] => SerializableAndCloneable

        if (thisUpperTypeBound eq thatUpperTypeBound)
            return Left(thisUpperTypeBound)

        val thisUTBDim = thisUpperTypeBound.dimensions
        val thatUTBDim = thatUpperTypeBound.dimensions

        if (thisUTBDim < thatUTBDim) {
            if (thisUpperTypeBound.elementType.isBaseType) {
                if (thisUTBDim == 1)
                    Right(TypeLevelReferenceValues.SerializableAndCloneable)
                else
                    Left(ArrayType(thisUTBDim - 1, ObjectType.Object))
            } else {
                Left(ArrayType(thisUTBDim, ObjectType.Object))
            }
        } else if (thisUTBDim > thatUTBDim) {
            if (thatUpperTypeBound.elementType.isBaseType) {
                if (thisUTBDim == 1)
                    Right(TypeLevelReferenceValues.SerializableAndCloneable)
                else
                    Left(ArrayType(thatUTBDim - 1, ObjectType.Object))
            } else {
                Left(ArrayType(thatUTBDim, ObjectType.Object))
            }
        } else if (thisUpperTypeBound.elementType.isBaseType ||
            thatUpperTypeBound.elementType.isBaseType) {
            // => the number of dimensions is the same, but the elementType isn't
            //    (if the element type would be the same, both object reference would 
            //    refer to the same object and this would have been handled the very 
            //    first test)            
            // Scenario:
            // E.g., imagine that we have a method that "just" wants to 
            // serialize some data. In such a case the method may be passed 
            // different arrays with different primitive values.
            if (thisUTBDim == 1 /* && thatUTBDim == 1*/ )
                Right(TypeLevelReferenceValues.SerializableAndCloneable)
            else {
                Left(ArrayType(thisUTBDim - 1, ObjectType.Object))
            }
        } else {
            // When we reach this point, the dimensions are identical and both 
            // elementTypes are reference types
            val thatElementType = thatUpperTypeBound.elementType.asObjectType
            val thisElementType = thisUpperTypeBound.elementType.asObjectType
            Left(
                ArrayType(
                    thisUTBDim,
                    joinObjectTypesUntilSingleUpperBound(
                        thisElementType,
                        thatElementType,
                        true)
                )
            )
        }
    }

    protected[this] def joinObjectTypesUntilSingleUpperBound(
        upperTypeBoundA: ObjectType,
        upperTypeBoundB: ObjectType,
        reflexive: Boolean): ObjectType = {
        joinObjectTypes(upperTypeBoundA, upperTypeBoundB, reflexive) match {
            case Left(newUpperTypeBound) ⇒
                newUpperTypeBound
            case Right(newUpperTypeBounds) ⇒
                newUpperTypeBounds reduce { (c, n) ⇒
                    joinObjectTypesUntilSingleUpperBound(c, n, false)
                }
        }
    }

    // ---------------------------------1-------------------------------------------------
    //
    // REPRESENTATION OF REFERENCE VALUES
    //
    // -----------------------------------------------------------------------------------

    type DomainReferenceValue <: ReferenceValue with DomainValue
    type DomainNullValue <: NullValue with DomainReferenceValue
    type DomainObjectValue <: ObjectValue with DomainReferenceValue
    type DomainArrayValue <: ArrayValue with DomainReferenceValue

    trait ArrayAbstraction {
        def load(pc: PC, index: DomainValue): ArrayLoadResult
        def store(pc: PC, value: DomainValue, index: DomainValue): ArrayStoreResult
        def length(pc: PC): Computation[DomainValue, ExceptionValue]
    }

    /**
     * Abstracts over all values with computational type `reference`. I.e.,
     * abstracts over class and array values and also the `null` value.
     */
    protected trait ReferenceValue
            extends Value
            with IsReferenceValue
            with ArrayAbstraction {
        this: DomainReferenceValue ⇒

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
        def refineIsNull(pc: PC, isNull: Answer): DomainReferenceValue

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
        @throws[DomainException]("If this value is null (isNull.yes == true).")
        override def isValueSubtypeOf(referenceType: ReferenceType): Answer = Unknown

        /**
         * Refines the upper bound to this value's type.
         *
         * This call can be ignored if the type
         * information related to this value is precise, i.e., if we know that we
         * precisely capture the runtime type of this value. However, refining
         * the upper type bound for a `null` value is not supported.
         */
        @throws[ImpossibleRefinement]("If the refinement is not meaningful.")
        def refineUpperTypeBound(pc: PC, supertype: ReferenceType): DomainReferenceValue

    }

    /**
     * Represents the value `null`. Null values are basically found in the following two
     * cases:
     *  1. The value `null` was pushed onto the stack using `aconst_null`.
     *  2. A reference value that is not guaranteed to be non-null is tested against
     *    `null` using `ifnull` or `ifnonnull` and we are now on the branch where
     *    the value has to be `null`.
     */
    protected trait NullValue extends ReferenceValue {
        this: DomainNullValue ⇒

        final override def referenceValues: Iterator[IsAReferenceValue] = Iterator(this)

        /**
         * Returns `Yes`.
         */
        final override def isNull = Yes

        final override def refineIsNull(pc: PC, isNull: Answer): DomainNullValue = this

        /**
         * Returns `true`.
         */
        final override def isPrecise = true

        /**
         * Returns an empty upper type bound.
         */
        final override def upperTypeBound: UpperTypeBound = UIDSet.empty

        final override def load(pc: PC, index: DomainValue): ArrayLoadResult =
            justThrows(NullPointerException(pc))

        final override def store(pc: PC, value: DomainValue, index: DomainValue): ArrayStoreResult =
            justThrows(NullPointerException(pc))

        final override def length(pc: PC): Computation[DomainValue, ExceptionValue] =
            throws(NullPointerException(pc))

        /**
         * Throws a new `DomainException` that states that this method is not supported.
         */
        @throws[DomainException]("Always thrown (it is not possible to give a generic answer, as the answer depends on the context (instanceof/classcast/...)).")
        final override def isValueSubtypeOf(referenceType: ReferenceType): Nothing =
            throw DomainException("isSubtypeOf is not defined for \"null\" values")

        override def refineUpperTypeBound(pc: PC, supertype: ReferenceType): this.type =
            this

        override def summarize(pc: PC): this.type = this

        override def adapt(target: Domain, pc: PC): target.DomainValue =
            target.NullValue(pc)

        override def toString: String = "ReferenceValue(null)"
    }

    /**
     * A reference value that is associated with a single (upper) type (bound).
     *
     * @note This class was introduced for performance reasons.
     */
    protected trait SReferenceValue[T <: ReferenceType] {
        this: DomainReferenceValue ⇒

        val theUpperTypeBound: T

        final override def referenceValues: Iterator[IsAReferenceValue] = Iterator(this)

        final override def upperTypeBound: UpperTypeBound = UIDSet(theUpperTypeBound)

        final override def summarize(pc: PC): this.type = this

        override def toString: String = "ReferenceValue("+theUpperTypeBound.toJava+")"

    }

    /**
     * Represents a class/interface value which may have a single class and/or
     * multiple interfaces as its upper type bound.
     */
    protected trait ObjectValue extends ReferenceValue {
        this: DomainObjectValue ⇒

    }

    /**
     * Represents an array value.
     */
    protected trait ArrayValue extends ReferenceValue {
        this: DomainArrayValue ⇒

        /**
         * Returns `Yes` if we can statically determine that the given value can
         * be stored in the array represented by this `ArrayValue`.
         */
        /*ABSTRACT*/ def isAssignable(value: DomainValue): Answer

        /*ABSTRACT*/ protected def doLoad(
            pc: PC,
            index: DomainValue,
            potentialExceptions: ExceptionValues): ArrayLoadResult

        override def load(pc: PC, index: DomainValue): ArrayLoadResult = {
            // @note
            // The case "this.isNull == Yes" will not occur as the value "null" is always
            // represented by an instance of the respective class and this situation
            // is checked for by the domain-level method.

            val isIndexValid =
                length.
                    map((l: Int) ⇒ intIsSomeValueInRange(index, 0, l - 1)).
                    getOrElse(intIsLessThan0(index).negate)
            if (isIndexValid.isNo)
                return justThrows(ArrayIndexOutOfBoundsException(pc))

            var thrownExceptions = List.empty[ExceptionValue]
            if (isNull.isYesOrUnknown && throwNullPointerException)
                thrownExceptions = NullPointerException(pc) :: thrownExceptions
            if (isIndexValid.isNoOrUnknown && throwArrayIndexOutOfBoundsException)
                thrownExceptions = ArrayIndexOutOfBoundsException(pc) :: thrownExceptions

            doLoad(pc, index, thrownExceptions)
        }

        /*ABSTRACT*/ protected def doStore(
            pc: PC,
            value: DomainValue,
            index: DomainValue,
            thrownExceptions: ExceptionValues): ArrayStoreResult

        /**
         * @note It is in general not necessary to override this method. If you need some
         *      special handling if a value is stored in an array, override the method
         *      `doArraystore`.
         * @see `doArraystore` for further information.
         */
        override def store(
            pc: PC,
            value: DomainValue,
            index: DomainValue): ArrayStoreResult = {
            // @note
            // The case "this.isNull == Yes" will not occur as the value "null" is always
            // represented by an instance of the respective class

            val isIndexValid =
                length.
                    map((l: Int) ⇒ intIsSomeValueInRange(index, 0, l - 1)).
                    getOrElse(intIsLessThan0(index).negate)
            if (isIndexValid.isNo)
                return justThrows(ArrayIndexOutOfBoundsException(pc))

            val isAssignable = this.isAssignable(value)
            if (isAssignable.isNo)
                return justThrows(ArrayStoreException(pc))

            var thrownExceptions = List.empty[ExceptionValue]
            if (isIndexValid.isUnknown && throwArrayIndexOutOfBoundsException)
                thrownExceptions = ArrayIndexOutOfBoundsException(pc) :: thrownExceptions
            if (isAssignable.isUnknown && throwArrayStoreException)
                thrownExceptions = ArrayStoreException(pc) :: thrownExceptions
            if (isNull.isYesOrUnknown && throwNullPointerException)
                thrownExceptions = NullPointerException(pc) :: thrownExceptions

            doStore(pc, value, index, thrownExceptions)
        }

        protected def length: Option[Int] = None

        protected final def doGetLength(pc: PC): DomainValue =
            length.map(IntegerValue(pc, _)).getOrElse(IntegerValue(pc))

        override def length(pc: PC): Computation[DomainValue, ExceptionValue] = {
            if (isNull == Unknown && throwNullPointerException)
                ComputedValueAndException(doGetLength(pc), NullPointerException(pc))
            else
                ComputedValue(doGetLength(pc))
        }
    }

    /**
     * Returns the given value as a DomainValue. Basically just performs a type cast
     * and is intended to be used to communicate that the value has to be a reference
     * value (if the underlying byte code is valid.)
     */
    def asReferenceValue(value: DomainValue): DomainReferenceValue =
        value.asInstanceOf[DomainReferenceValue]

    def asObjectValue(value: DomainValue): DomainObjectValue =
        value.asInstanceOf[DomainObjectValue]

    def asArrayAbstraction(value: DomainValue): ArrayAbstraction = {
        value match {
            case aa: ArrayAbstraction ⇒ aa
            case _                    ⇒ throw new ClassCastException("no array value: "+value)
        }
    }

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
                domain.isSubtypeOf(subtype, supertype).isYes
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
    override def refAreEqual(value1: DomainValue, value2: DomainValue): Answer = {
        val v1 = asReferenceValue(value1)
        val v2 = asReferenceValue(value2)
        val value1IsNull = v1.isNull
        val value2IsNull = v2.isNull
        if (value1IsNull.isYes && value2IsNull.isYesOrNo)
            // both are null or the second one is definitively not null
            Answer(value2IsNull.isYes)
        else if (value2IsNull.isYes && value1IsNull.isYesOrNo)
            // both are null or the first one is definitively not null
            Answer(value1IsNull.isYes)
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
    final override def refIsNull(value: DomainValue): Answer =
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
        val validCount = intIsSomeValueInRange(count, 0, Int.MaxValue)
        if (validCount.isNo)
            return throws(NegativeArraySizeException(pc))

        val newarray = NewArray(pc, count, ArrayType(componentType))
        if (validCount.isUnknown && throwNegativeArraySizeException)
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
        arrayType: ArrayType): Computation[DomainArrayValue, ExceptionValue] = {
        var validCounts: Answer = Yes
        counts foreach { (count) ⇒
            val validCount = intIsSomeValueInRange(count, 0, Int.MaxValue)
            if (validCount.isNo)
                return throws(NegativeArraySizeException(pc))
            else if (validCount.isUnknown)
                validCounts = Unknown
        }

        val newarray =
            if (counts.tail.isEmpty)
                NewArray(pc, counts.head, arrayType)
            else
                NewArray(pc, counts, arrayType)
        if (validCounts.isUnknown && throwNegativeArraySizeException)
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
        if (refIsNull(arrayref).isYes)
            justThrows(NullPointerException(pc))
        else
            // if the bytecode is valid, the type cast (asArrayValue) is safe
            asArrayAbstraction(arrayref).load(pc, index)
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
        if (refIsNull(arrayref).isYes)
            justThrows(NullPointerException(pc))
        else
            // if the bytecode is valid, the type cast (asArrayValue) is safe
            asArrayAbstraction(arrayref).store(pc, value, index)
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
        if (refIsNull(arrayref).isYes)
            throws(NullPointerException(pc))
        else
            asArrayAbstraction(arrayref).length(pc)
    }

    // -----------------------------------------------------------------------------------
    //
    // FACTORY METHODS
    //
    // -----------------------------------------------------------------------------------

    //
    // REFINEMENT OF EXISTING DOMAIN VALUE FACTORY METHODS
    //

    override def NullValue(pc: PC): DomainNullValue

    override def ReferenceValue(
        pc: PC,
        upperTypeBound: ReferenceType): DomainReferenceValue = {
        if (upperTypeBound.isArrayType)
            ArrayValue(pc, upperTypeBound.asArrayType)
        else
            ObjectValue(pc, upperTypeBound.asObjectType)
    }

    override def NonNullObjectValue(pc: PC, objectType: ObjectType): DomainObjectValue =
        ObjectValue(pc, objectType)

    override def NewObject(pc: PC, objectType: ObjectType): DomainObjectValue =
        ObjectValue(pc, objectType)

    override def InitializedObjectValue(pc: PC, objectType: ObjectType): DomainObjectValue =
        ObjectValue(pc, objectType)

    override def StringValue(pc: PC, value: String): DomainObjectValue =
        ObjectValue(pc, ObjectType.String)

    override def ClassValue(pc: PC, t: Type): DomainObjectValue =
        ObjectValue(pc, ObjectType.Class)

    override def InitializedArrayValue(
        pc: PC, counts: 
        List[Int], 
        arrayType: ArrayType): DomainValue =
        ArrayValue(pc, arrayType)

    //
    // DECLARATION OF ADDITIONAL DOMAIN VALUE FACTORY METHODS
    //

    /**
     * Factory method to create a `DomainValue` that represents ''either an class-/interface
     * value that has the given type or the value `null`''. However, the
     * information whether the value is `null` or not is not available. Furthermore, the
     * type may also just be an upper bound and it is not known if the value is
     * properly initialized.
     *
     * ==Summary==
     * The properties of the domain value are:
     *
     *  - Initialized: '''Unknown'''
     *  - Type: '''Upper Bound'''
     *  - Null: '''Unknown'''
     *  - Content: '''Unknown'''
     */
    protected[domain] def ObjectValue(pc: PC, objectType: ObjectType): DomainObjectValue

    /**
     * Factory method to create a `DomainValue` that represents ''either an class-/interface
     * value that has the given types as an upper bound or the value `null`''. However, the
     * information whether the value is `null` or not is not available. Furthermore, the
     * type may also just be an upper bound and it is not known if the value is
     * properly initialized.
     *
     * ==Summary==
     * The properties of the domain value are:
     *
     *  - Initialized: '''Unknown''' (i.e., it is not guaranteed that the constructor was called.)
     *  - Type: '''Upper Bound'''
     *  - Null: '''Unknown'''
     *  - Content: '''Unknown'''
     */
    protected[domain] def ObjectValue(pc: PC, upperTypeBound: UIDSet[ObjectType]): DomainObjectValue

    /**
     * Factory method to create a new domain value that represents a newly created
     * array (non-null) with the size determined by count that is empty.
     *
     * ==Typical Usage==
     * This factory method is (implicitly) used, e.g., by OPAL-AI when a new array
     * instruction is found.
     *
     * ==Summary==
     * The properties of the value are:
     *
     *  - Type: '''Precise'''
     *  - Null: '''No'''
     *  - Size: '''Count'''
     *  - Content: '''Empty'''
     */
    def NewArray(pc: PC, count: DomainValue, arrayType: ArrayType): DomainArrayValue =
        ArrayValue(pc, arrayType)

    /**
     * Factory method to create a new domain value that represents a newly created
     * array (non-null) with the size determined by count that is empty.
     *
     * ==Typical Usage==
     * This factory method is (implicitly) used, e.g., by OPAL-AI when a new array
     * instruction is found.
     *
     * ==Summary==
     * The properties of the value are:
     *
     *  - Type: '''Precise'''
     *  - Null: '''No'''
     *  - Size: '''Depending on the values in `counts`'''
     *  - Content: '''Empty'''
     */
    def NewArray(pc: PC, counts: List[DomainValue], arrayType: ArrayType): DomainArrayValue =
        ArrayValue(pc, arrayType)

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
     *  - Content: '''Unknown'''
     * @note Java Arrays are covariant. I.e., `Object[] a = new Serializable[100];`
     *      is valid.
     */
    protected[domain] def ArrayValue(pc: PC, arrayType: ArrayType): DomainArrayValue

    // -----------------------------------------------------------------------------------
    //
    // HANDLING OF CONSTRAINTS
    //
    // -----------------------------------------------------------------------------------

    protected[this] def updateOperandsAndLocals(
        oldValue: DomainValue,
        newValue: DomainValue,
        operands: Operands,
        locals: Locals): (Operands, Locals) = {
        if (oldValue == newValue) // FIXME Should be eq, shouldn't it?
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

    override def refEstablishUpperBound(
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
    override def refEstablishIsNonNull(
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
    override def refEstablishIsNull(
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
    val SerializableAndCloneable: UIDSet[ObjectType] =
        UIDSet(ObjectType.Serializable, ObjectType.Cloneable)

}
