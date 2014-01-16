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
 * Default implementation for handling reference values.
 *
 * @author Michael Eichberg
 */
trait DefaultTypeLevelReferenceValues[+I]
        extends DefaultDomainValueBinding[I]
        with TypeLevelReferenceValues[I] {
    domain: Configuration with IntegerValuesComparison with ClassHierarchy ⇒

    /**
     * Calculates the set of all supertypes of the given `types`.
     */
    protected def allSupertypesOf(
        types: UIDList[ObjectType],
        reflexive: Boolean): scala.collection.Set[ObjectType] = {
        // TODO [Performance] The creation of the set of all supertypes of multiple types could be improved
        val allSupertypesOf = scala.collection.mutable.HashSet.empty[ObjectType]
        types foreach { t ⇒
            allSupertypesOf ++= classHierarchy.allSupertypes(t, reflexive)
        }
        allSupertypesOf
    }

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
        val lts = types filter { aType ⇒
            !(classHierarchy.directSubtypesOf(aType) exists { t ⇒ types.contains(t) })
        }
        if (lts.isEmpty)
            Left(ObjectType.Object)
        else if (lts.size == 1)
            Left(lts.head)
        else {
            Right(UIDList[ObjectType](lts))
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
     */
    protected def joinObjectTypes(
        upperTypeBoundA: ObjectType,
        upperTypeBoundB: UIDList[ObjectType],
        reflexive: Boolean): Either[ObjectType, UIDList[ObjectType]] = {

        val allSupertypesOfA = classHierarchy.allSupertypes(upperTypeBoundA, reflexive)
        val allSupertypesOfB = allSupertypesOf(upperTypeBoundB, reflexive)
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

        val allSupertypesOfA = classHierarchy.allSupertypes(upperTypeBoundA, reflexive)
        val allSupertypesOfB = classHierarchy.allSupertypes(upperTypeBoundB, reflexive)
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

    protected class NullValue extends super.NullValue {

        override def doJoin(joinPC: Int, other: DomainValue): Update[DomainValue] = {
            val that = asReferenceValue(other)
            val thatIsNull = that.isNull
            if (thatIsNull.yes)
                NoUpdate
            else
                StructuralUpdate(
                    if (thatIsNull.maybeYes)
                        other
                    else
                        that.updateIsNull(joinPC, Unknown)
                )
        }
    }

    protected class ArrayValue(
        val theUpperTypeBound: ArrayType)
            extends super.ArrayValue with SReferenceValue[ArrayType] {

        override def isValueSubtypeOf(supertype: ReferenceType): Answer = {
            val isSubtypeOf = domain.isSubtypeOf(theUpperTypeBound, supertype)
            isSubtypeOf match {
                case Yes ⇒ Yes
                case No if isPrecise ||
                    theUpperTypeBound.componentType.isBaseType ||
                    (supertype.isArrayType && supertype.asArrayType.componentType.isBaseType) ⇒ No
                case _ ⇒ Unknown
            }
        }

        override def isAssignable(value: DomainValue): Answer = {
            typeOfValue(value) match {
                case IsPrimitiveValue(primitiveType) ⇒
                    // The following is an overapproximation that makes it theoretically 
                    // possible to store an int value in a byte array.
                    Answer(
                        theUpperTypeBound.componentType.computationalType eq primitiveType.computationalType
                    )
                case _ ⇒
                    // IMPROVE We could check if this array's type and the given value's type are in no inheritance hierarchy
                    // IMPROVE We could check if the type of the other value is precise and if so if this type is a supertype of it
                    Unknown
            }
        }

        override def doLoad(
            pc: PC,
            index: DomainValue,
            potentialExceptions: ExceptionValues): ArrayLoadResult = {
            ComputedValueAndException(
                TypedValue(pc, theUpperTypeBound.componentType),
                potentialExceptions)
        }

        override protected def doUpdateIsNull(pc: PC, isNull: Answer): DomainValue =
            // it does not matter wether isNull is "No" or "Unknown"... we simply
            // ignore it
            this

        // NARROWING OPERATION
        override def refineUpperTypeBound(
            pc: PC,
            supertype: ReferenceType): DomainValue = {
            // BATAI calls this method only if a previous "subtype of" test 
            // (this.typeOfvalue <: supertype ?) 
            // returned unknown and we are now on the branch where this relation
            // has to hold. Hence, we only need to handle the case where 
            // supertype is more strict than this type's upper type bound.
            if (supertype.isArrayType)
                ReferenceValue(pc, supertype)
            else {
                domainException(
                    domain,
                    "cannot refine an array value to a non-array value")
            }
        }

        // WIDENING OPERATION
        override def doJoin(joinPC: Int, other: DomainValue): Update[DomainValue] = {
            val thisUpperTypeBound = this.theUpperTypeBound
            other match {
                case SObjectValue(thatUpperTypeBound) ⇒
                    joinAnyArrayTypeWithObjectType(thatUpperTypeBound) match {
                        case Left(`thatUpperTypeBound`) ⇒
                            StructuralUpdate(other)
                        case Left(newUpperTypeBound) ⇒
                            StructuralUpdate(ReferenceValue(joinPC, newUpperTypeBound))
                        case Right(newUpperTypeBound) ⇒
                            StructuralUpdate(ReferenceValue(joinPC, newUpperTypeBound))
                    }

                case MObjectValue(thatUpperTypeBound) ⇒
                    joinAnyArrayTypeWithMultipleTypesBound(thatUpperTypeBound) match {
                        case Right(`thatUpperTypeBound`) ⇒
                            StructuralUpdate(other)
                        case Left(newUpperTypeBound) ⇒
                            StructuralUpdate(ReferenceValue(joinPC, newUpperTypeBound))
                        case Right(newUpperTypeBound) ⇒
                            // this case should not occur...
                            StructuralUpdate(ReferenceValue(joinPC, newUpperTypeBound))
                    }

                case ArrayValue(thatUpperTypeBound) ⇒
                    joinArrayTypes(this.theUpperTypeBound, thatUpperTypeBound) match {
                        case Left(`thisUpperTypeBound`) ⇒
                            NoUpdate
                        case Left(`thatUpperTypeBound`) ⇒
                            StructuralUpdate(other)
                        case Left(newUpperTypeBound) ⇒
                            StructuralUpdate(ArrayValue(joinPC, newUpperTypeBound))
                        case Right(newUpperTypeBound) ⇒
                            StructuralUpdate(ReferenceValue(joinPC, newUpperTypeBound))
                    }

                case NullValue() ⇒
                    NoUpdate
            }
        }
    }

    /**
     * Enables matching of `DomainValue`s that are array values.
     */
    object ArrayValue {
        def unapply(value: ArrayValue): Some[ArrayType] = Some(value.theUpperTypeBound)
    }

    protected class SObjectValue(
        val theUpperTypeBound: ObjectType)
            extends super.ObjectValue with SReferenceValue[ObjectType] { value ⇒

        /**
         * @inhertdoc
         *
         * @note It is often not necessary to override this method as this method already
         *      takes the property whether the upper type bound '''is precise''' into
         *      account.
         */
        override def isValueSubtypeOf(supertype: ReferenceType): Answer = {
            val isSubtypeOf = domain.isSubtypeOf(theUpperTypeBound, supertype)
            isSubtypeOf match {
                case Yes             ⇒ Yes
                case No if isPrecise ⇒ No
                case _               ⇒ Unknown
            }
        }

        // NARROWING OPERATION
        override def refineUpperTypeBound(
            pc: PC,
            supertype: ReferenceType): ReferenceValue = {
            if (supertype eq theUpperTypeBound)
                return this

            // BATAI calls this method only if a previous "subtype of" test 
            // (this.typeOfvalue <: supertype ?) 
            // returned unknown and we are now on the branch where this relation
            // has to hold. Hence, we only need to handle the case where 
            // supertype is more strict than this type's upper type bound.
            val isSubtypeOf = domain.isSubtypeOf(supertype, theUpperTypeBound)
            if (isSubtypeOf.yes)
                ReferenceValue(pc, supertype)
            else {
                if (supertype.isArrayType)
                    domainException(domain,
                        "impossible refinement "+theUpperTypeBound.toJava+
                            " => "+supertype.toJava)

                // probably some (abstract) class and an interface or two
                // unrelated interfaces
                ReferenceValue(pc, UIDList(theUpperTypeBound, supertype.asObjectType))
            }
        }

        protected def asStructuralUpdate(
            joinPC: PC,
            newUpperTypeBound: Either[ObjectType, UIDList[ObjectType]]): Update[DomainValue] = {
            newUpperTypeBound match {
                case Left(newUpperTypeBound) ⇒
                    StructuralUpdate(ReferenceValue(joinPC, newUpperTypeBound))
                case Right(newUpperTypeBound) ⇒
                    StructuralUpdate(ReferenceValue(joinPC, newUpperTypeBound))
            }
        }

        // WIDENING OPERATION
        override def doJoin(joinPC: Int, other: DomainValue): Update[DomainValue] = {
            val thisUpperTypeBound = this.theUpperTypeBound
            other match {
                case SObjectValue(thatUpperTypeBound) ⇒
                    if ((thisUpperTypeBound eq thatUpperTypeBound) ||
                        domain.isSubtypeOf(thatUpperTypeBound, thisUpperTypeBound).yes)
                        NoUpdate
                    else if (domain.isSubtypeOf(thisUpperTypeBound, thatUpperTypeBound).yes)
                        StructuralUpdate(other)
                    else {
                        asStructuralUpdate(
                            joinPC,
                            joinObjectTypes(thisUpperTypeBound, thatUpperTypeBound, false))
                    }

                case MObjectValue(thatUpperTypeBound) ⇒
                    var isNewUpperTypeBound = false
                    val newUpperTypeBound =
                        thatUpperTypeBound filter { thatType ⇒
                            if (domain.isSubtypeOf(thatType, thisUpperTypeBound).yes)
                                return NoUpdate

                            // collect all types this type is a subtype of
                            val isSubtypeOf = domain.isSubtypeOf(thisUpperTypeBound, thatType).yes
                            if (!isSubtypeOf)
                                // we filtered at least one type
                                isNewUpperTypeBound = true
                            isSubtypeOf
                        }
                    if (newUpperTypeBound.nonEmpty) {
                        if (isNewUpperTypeBound)
                            StructuralUpdate(
                                ReferenceValue(joinPC, newUpperTypeBound + thisUpperTypeBound)
                            )
                        else
                            // thisUpperTypeBound is a subtype of all types of thatUpperTypeBound
                            StructuralUpdate(other)
                    } else {
                        // thisUpperTypeBound was not a sub-/supertype of any type of thatUpperTypeBound
                        asStructuralUpdate(
                            joinPC,
                            joinObjectTypes(thisUpperTypeBound, thatUpperTypeBound, false))
                    }

                case that: ArrayValue ⇒
                    val thisUpperTypeBound = this.theUpperTypeBound
                    joinAnyArrayTypeWithObjectType(thisUpperTypeBound) match {
                        case Left(`thisUpperTypeBound`) ⇒
                            NoUpdate
                        case Left(newUpperTypeBound) ⇒
                            StructuralUpdate(ReferenceValue(joinPC, newUpperTypeBound))
                        case Right(newUpperTypeBound) ⇒
                            StructuralUpdate(ReferenceValue(joinPC, newUpperTypeBound))
                    }

                case NullValue() ⇒
                    NoUpdate
            }
        }

        override def adapt[ThatI >: I](target: Domain[ThatI], pc: PC): target.DomainValue =
            target.ReferenceValue(pc, theUpperTypeBound)

        override protected def doUpdateIsNull(pc: PC, isNull: Answer): ReferenceValue =
            this

    }

    object SObjectValue {
        def unapply(that: SObjectValue): Some[ObjectType] = Some(that.theUpperTypeBound)
    }

    /**
     * @param upperTypeBound All types from which the (precise, but unknown) type of the
     *      represented value inherits. I.e., the value represented by this domain value
     *      is known to have a type that (in)directly inherits from all given types at
     *      the same time.
     */
    protected class MObjectValue(
        val upperTypeBound: UIDList[ObjectType])
            extends super.ObjectValue { value ⇒

        override def referenceValues: Iterable[IsAReferenceValue] = Iterable(this)

        /**
         * Determines if this value is a subtype of the given supertype by
         * delegating to the `isSubtypeOf(ReferenceType,ReferenceType)` method of the
         * domain.
         *
         * @note This is a very basic implementation that cannot determine that this
         *      value is '''not''' a subtype of the given type as this implementation
         *      does not distinguish between class types and interface types.
         */
        override def isValueSubtypeOf(supertype: ReferenceType): Answer = {
            val isSubtypeOf = upperTypeBound exists { anUpperTypeBound ⇒
                domain.isSubtypeOf(anUpperTypeBound, supertype).yes
            }
            if (isSubtypeOf)
                Yes
            else
                Unknown
        }

        override def refineUpperTypeBound(
            pc: PC,
            supertype: ReferenceType): DomainValue = {
            // BATAI calls this method only if a previous "subtype of" test 
            // (typeOf(this.value) <: additionalUpperBound ?) 
            // returned unknown. Hence, we only handle the case where the new bound
            // is more strict than the previous bound.

            var newUpperTypeBound: UIDList[ObjectType] = UIDList.empty
            upperTypeBound foreach { anUpperTypeBound ⇒
                // ATTENTION: "!..yes" is not the same as "no" (there is also unknown)
                if (!domain.isSubtypeOf(supertype, anUpperTypeBound).yes)
                    newUpperTypeBound = newUpperTypeBound + anUpperTypeBound
            }
            if (newUpperTypeBound.size == 0)
                ReferenceValue(pc, supertype)
            else if (supertype.isObjectType)
                ReferenceValue(pc, newUpperTypeBound + supertype.asObjectType)
            else
                TheIllegalValue
        }

        override def doJoin(joinPC: Int, that: DomainValue): Update[DomainValue] = {
            that match {
                case SObjectValue(thatUpperTypeBound) ⇒
                    val newUpperTypeBound = this.upperTypeBound filter { thisBound ⇒
                        domain.isSubtypeOf(thatUpperTypeBound, thisBound) match {
                            case Yes ⇒
                                return NoUpdate
                            case No ⇒
                                if (domain.isSubtypeOf(thisBound, thatUpperTypeBound).yes)
                                    false
                                else
                                    true
                            case Unknown ⇒
                                true
                        }
                    }
                    if (newUpperTypeBound.isEmpty)
                        // all upper bounds of "other" are a subtype of this upper bound
                        StructuralUpdate(that)
                    else
                        StructuralUpdate(
                            ReferenceValue(joinPC, newUpperTypeBound + thatUpperTypeBound)
                        )
                case MObjectValue(thatUpperTypeBound) ⇒
                    val thisUpperTypeBound = this.upperTypeBound
                    if (thisUpperTypeBound == thatUpperTypeBound)
                        return NoUpdate
                    val thisDomain = domain
                    var newUpperTypeBound = thisUpperTypeBound
                    thatUpperTypeBound foreach { (otherUpperTypeBound: ObjectType) ⇒
                        var addOtherTypeBounds = true
                        newUpperTypeBound = newUpperTypeBound filterNot { vt ⇒
                            if (thisDomain.isSubtypeOf(otherUpperTypeBound, vt).yes)
                                true
                            else {
                                if (thisDomain.isSubtypeOf(vt, otherUpperTypeBound).yes)
                                    addOtherTypeBounds = false
                                false
                            }
                        }
                        if (addOtherTypeBounds)
                            newUpperTypeBound = newUpperTypeBound + otherUpperTypeBound
                    }
                    if (newUpperTypeBound eq thisUpperTypeBound) {
                        NoUpdate
                    } else if (newUpperTypeBound == thatUpperTypeBound) {
                        StructuralUpdate(that)
                    } else
                        StructuralUpdate(ReferenceValue(joinPC, newUpperTypeBound))

                case ArrayValue(thatUpperTypeBound) ⇒
                    val thisUpperTypeBound = this.upperTypeBound
                    if (thisUpperTypeBound == TypeLevelReferenceValues.SerializableAndCloneable)
                        NoUpdate
                    else {
                        val isSerializable =
                            thisUpperTypeBound exists { thatType ⇒
                                domain.isSubtypeOf(thatType, ObjectType.Serializable).yes
                            }
                        val isCloneable =
                            thisUpperTypeBound exists { thatType ⇒
                                domain.isSubtypeOf(thatType, ObjectType.Cloneable).yes
                            }
                        if (isSerializable && isCloneable)
                            StructuralUpdate(ReferenceValue(joinPC, TypeLevelReferenceValues.SerializableAndCloneable))
                        else if (isSerializable)
                            StructuralUpdate(ReferenceValue(joinPC, ObjectType.Serializable))
                        else if (isCloneable)
                            StructuralUpdate(ReferenceValue(joinPC, ObjectType.Cloneable))
                        else
                            StructuralUpdate(ReferenceValue(joinPC, ObjectType.Object))
                    }

                case NullValue() ⇒
                    NoUpdate
            }
        }

        override def adapt[ThatI >: I](
            targetDomain: Domain[ThatI],
            pc: PC): targetDomain.DomainValue =
            targetDomain match {
                case td: DefaultTypeLevelReferenceValues[_] ⇒
                    td.ReferenceValue(pc, this.upperTypeBound).asInstanceOf[targetDomain.DomainValue]
                case _ ⇒ super.adapt(targetDomain, pc)
            }

        override protected def doUpdateIsNull(pc: PC, isNull: Answer): DomainValue =
            this

        override def summarize(pc: PC): DomainValue = this

        override def toString() =
            "ReferenceValue("+upperTypeBound.map(_.toJava).mkString(" with ")+")"
    }

    object MObjectValue {
        def unapply(that: MObjectValue): Option[UIDList[ObjectType]] =
            Some(that.upperTypeBound)
    }

    //
    // FACTORY METHODS
    //

    protected[this] val TheNullValue: ReferenceValue = new NullValue()

    /**
     * @inheritdoc
     * This implementation always returns the singleton instance `TheNullValue`.
     */
    override def NullValue(pc: PC): ReferenceValue = TheNullValue

    /**
     * @inheritdoc
     * This implementation always directly creates a new `SObjectValue`.
     */
    override def NonNullReferenceValue(pc: PC, objectType: ObjectType): ReferenceValue =
        new SObjectValue(objectType)

    override def NewArray(pc: PC, count: DomainValue, arrayType: ArrayType): ArrayValue =
        new ArrayValue(arrayType)

    override def NewArray(pc: PC, counts: List[DomainValue], arrayType: ArrayType): ArrayValue =
        new ArrayValue(arrayType)

    override def ArrayValue(pc: PC, arrayType: ArrayType): ArrayValue =
        new ArrayValue(arrayType)

    def ObjectValue(pc: PC, objectType: ObjectType): ReferenceValue =
        new SObjectValue(objectType)

    /**
     * @inheritdoc
     *
     * Depending on the kind of reference type (array or class/interface type) this method
     * just calls the respective factory method: `ArrayValue(PC,ArrayType)`
     * or `ObjectValue(PC,ObjectType)`.
     *
     * @note It is generally not necessary to override this method.
     */
    override def ReferenceValue(pc: PC, referenceType: ReferenceType): ReferenceValue =
        referenceType match {
            case ot: ObjectType ⇒ ObjectValue(pc, ot)
            case at: ArrayType  ⇒ ArrayValue(pc, at)
        }

    /**
     * Factory method to create a `DomainValue` that represents ''either a reference
     * value that has the given type bound and is initialized or the value `null`''.
     * However, the information whether the value is `null` or not is not available.
     * Furthermore, the type may also be an upper bound. I.e., we may have multiple types
     * and the runtime type is guaranteed to be a subtype of all given types.
     *
     * ==Summary==
     * The properties of the domain value are:
     *
     *  - Initialized: '''yes''' (the constructor was called)
     *  - Type: '''Upper Bound'''
     *  - Null: '''MayBe''' (It is unknown whether the value is `null` or not.)
     */
    def ReferenceValue(pc: PC, upperTypeBound: UIDList[ObjectType]): ReferenceValue = {
        assume(upperTypeBound.nonEmpty)
        if (upperTypeBound.tail.isEmpty)
            ReferenceValue(pc, upperTypeBound.head)
        else
            new MObjectValue(upperTypeBound)
    }

    override def NewObject(pc: PC, objectType: ObjectType): ReferenceValue =
        new SObjectValue(objectType)

    /**
     * @inheritdoc
     *
     * Depending on the kind of reference type (array or class type) this method
     * just calls the respective factory method: `ArrayValue(PC,ArrayType)`
     * or `ObjectValue(PC,ObjectType)`.
     *
     * @note It is generally necessary to override this method when you want to track
     *      a value`s properties ('''type''' and '''isPrecise''') more precisely.
     */
    override def InitializedObject(pc: PC, referenceType: ReferenceType): ReferenceValue =
        if (referenceType.isArrayType)
            ArrayValue(pc, referenceType.asArrayType)
        else
            ObjectValue(pc, referenceType.asObjectType)

    override def StringValue(pc: PC, value: String): DomainValue =
        new SObjectValue(ObjectType.String)

    override def ClassValue(pc: PC, t: Type): DomainValue =
        new SObjectValue(ObjectType.Class)

}
