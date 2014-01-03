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
    domain: Configuration with IntegerValuesComparison ⇒

    protected class NullValue
            extends super.NullValue {

        override def doJoin(joinPC: Int, other: DomainValue): Update[DomainValue] = {
            val that = asReferenceValue(other)
            val thatIsNull = that.isNull
            if (thatIsNull.yes)
                NoUpdate
            else
                StructuralUpdate(
                    if (thatIsNull.maybeYes) other else that.updateIsNull(joinPC, Unknown)
                )
        }
    }

    protected class ArrayValue(
        val theUpperTypeBound: ArrayType)
            extends super.ArrayValue with SReferenceValue[ArrayType] {

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

        override def updateIsNull(pc: PC, isNull: Answer): DomainValue = {
            isNull match {
                case Yes ⇒ NullValue(pc)
                case _   ⇒ this
            }
        }

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
                domainException(domain, "refining the upper type bound of an array value to a non-array value is not supported")
            }
        }

        // WIDENING OPERATION
        override def doJoin(joinPC: Int, other: DomainValue): Update[DomainValue] = {
            val thisUpperTypeBound = this.theUpperTypeBound
            other match {
                case SObjectValue(thatUpperTypeBound) ⇒
                    import ObjectType._
                    if ((thatUpperTypeBound eq Object) ||
                        (thatUpperTypeBound eq Serializable) ||
                        (thatUpperTypeBound eq Cloneable))
                        StructuralUpdate(other)
                    else {
                        var newUpperTypeBound: UpperTypeBound = UIDList.empty
                        if (domain.isSubtypeOf(thatUpperTypeBound, ObjectType.Serializable).yes)
                            newUpperTypeBound += ObjectType.Serializable
                        if (domain.isSubtypeOf(thatUpperTypeBound, ObjectType.Cloneable).yes)
                            newUpperTypeBound += ObjectType.Cloneable
                        StructuralUpdate(
                            if (newUpperTypeBound.isEmpty)
                                ReferenceValue(joinPC, ObjectType.Object)
                            else
                                ReferenceValue(joinPC, newUpperTypeBound)
                )
                    }

                case MReferenceValue(thatUpperTypeBound) ⇒
                    if (thatUpperTypeBound == TypeLevelReferenceValues.SerializableAndCloneable)
                        StructuralUpdate(other)
                    else {
                        val isSerializable =
                            thatUpperTypeBound exists { thatType ⇒
                                domain.isSubtypeOf(thatType, ObjectType.Serializable).yes
                            }
                        val isCloneable =
                            thatUpperTypeBound exists { thatType ⇒
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

                case ArrayValue(thatUpperTypeBound) ⇒
                    if (thatUpperTypeBound eq thisUpperTypeBound)
                        NoUpdate
                    else if (thisUpperTypeBound.componentType.isBaseType ||
                        thatUpperTypeBound.componentType.isBaseType) {
                        // Scenario:
                        // E.g., imagine that we have a method that "just" wants to 
                        // serialize some data. In such a case the method may be passed 
                        // different arrays with different values.
                        StructuralUpdate(
                            ReferenceValue(
                                joinPC,
                                TypeLevelReferenceValues.SerializableAndCloneable)
                        )
                    } else {
                        // When we reach this point, 
                        // both component types are reference types
                        val thatComponentType = thatUpperTypeBound.componentType.asReferenceType
                        val thisComponentType = thisUpperTypeBound.componentType.asReferenceType
                        if (domain.isSubtypeOf(thatComponentType, thisComponentType).yes)
                            NoUpdate
                        else if (domain.isSubtypeOf(thisComponentType, thatComponentType).yes)
                            StructuralUpdate(other)
                        else
                            // This is the most general fallback and we are losing some information
                            // when compared to a solution that calculates the least 
                            // upper type bound. However, in that case we need - 
                            // in geneal - to support array values with multiple type 
                            // bounds, which we currently don't do.
                            StructuralUpdate(ArrayValue(joinPC, ArrayType.ArrayOfObjects))
            }
                case NullValue() ⇒
                    NoUpdate
            }

        }
    }

    object ArrayValue {
        def unapply(value: ArrayValue): Some[ArrayType] = Some(value.theUpperTypeBound)
    }

    protected class SObjectValue(
        val theUpperTypeBound: ObjectType)
            extends super.ObjectValue with SReferenceValue[ObjectType] { value ⇒

        // NARROWING OPERATION
        override def refineUpperTypeBound(
            pc: PC,
            supertype: ReferenceType): ReferenceValue = {
            // BATAI calls this method only if a previous "subtype of" test 
            // (this.typeOfvalue <: supertype ?) 
            // returned unknown and we are now on the branch where this relation
            // has to hold. Hence, we only need to handle the case where 
            // supertype is more strict than this type's upper type bound.
            val isSubtypeOf = domain.isSubtypeOf(supertype, theUpperTypeBound)
            if (isSubtypeOf.yes)
                ReferenceValue(pc, supertype)
            else
                // probably some (abstract) class and an interface or two
                // unrelated interfaces
                ReferenceValue(pc, UIDList(theUpperTypeBound, supertype))
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
                    else
                        StructuralUpdate(
                            ReferenceValue(joinPC, UIDList(thisUpperTypeBound, thatUpperTypeBound))
                        )
                case MReferenceValue(thatUpperTypeBound) ⇒
                    val newUpperTypeBound = thatUpperTypeBound filter { thatType ⇒
                        domain.isSubtypeOf(thisUpperTypeBound, thatType) match {
                            case Yes ⇒
                                return StructuralUpdate(other)
                            case No ⇒
                                if (domain.isSubtypeOf(thatType, thisUpperTypeBound).yes)
                                    false
                                else
                                    true
                            case Unknown ⇒
                                true
                        }
                    }
                    if (newUpperTypeBound.isEmpty)
                        // all upper bounds of "other" are a subtype of this upper bound
                        NoUpdate
                    else
                        StructuralUpdate(
                            ReferenceValue(joinPC, newUpperTypeBound + thisUpperTypeBound)
                        )
                case ArrayValue(thatUpperTypeBound) ⇒
                    import ObjectType._
                    if ((theUpperTypeBound eq Object) ||
                        (theUpperTypeBound eq Serializable) ||
                        (theUpperTypeBound eq Cloneable))
                        NoUpdate
                    else {
                        var newUpperTypeBound: UpperTypeBound = UIDList.empty
                        if (domain.isSubtypeOf(theUpperTypeBound, ObjectType.Serializable).yes)
                            newUpperTypeBound += ObjectType.Serializable
                        if (domain.isSubtypeOf(theUpperTypeBound, ObjectType.Cloneable).yes)
                            newUpperTypeBound += ObjectType.Cloneable
                        StructuralUpdate(
                            if (newUpperTypeBound.isEmpty)
                                ReferenceValue(joinPC, ObjectType.Object)
                            else
                                ReferenceValue(joinPC, newUpperTypeBound)
                        )
            }
                case NullValue() ⇒
                    NoUpdate
        }
        }

        override def adapt[ThatI >: I](target: Domain[ThatI], pc: PC): target.DomainValue =
            target.ReferenceValue(pc, theUpperTypeBound)

        override def updateIsNull(pc: PC, isNull: Answer): DomainValue = {
            isNull match {
                case Yes ⇒ NullValue(pc)
                case _   ⇒ this
            }
        }
    }

    object SObjectValue {
        def unapply(that: SObjectValue): Some[ReferenceType] =
            Some(that.theUpperTypeBound)
    }

    protected class MReferenceValue(
        val upperTypeBound: UpperTypeBound)
            extends super.ObjectValue { value ⇒

        override def referenceValues: Iterable[IsAReferenceValue] = Iterable(this)

        /**
         * Determines if this value is a subtype of the given supertype by
         * delegating to the `isSubtypeOf(ReferenceType,ReferenceType)` method of the
         * domain.
         *
         * @note This is a very basic implementation that cannot determine that this
         * 		value is '''not''' a subtype of the given type as this implementation
         *   	does not distinguish between class types and interface types.
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
            supertype: ReferenceType): ReferenceValue = {
            // BATAI calls this method only if a previous "subtype of" test 
            // (typeOf(this.value) <: additionalUpperBound ?) 
            // returned unknown. Hence, we only handle the case where the new bound
            // is more strict than the previous bound.

            var newUpperTypeBound: UIDList[ReferenceType] = UIDList.empty
            upperTypeBound foreach { (anUpperTypeBound: ReferenceType) ⇒
                // ATTENTION: "!..yes" is not the same as "no" (there is also unknown)
                if (!domain.isSubtypeOf(supertype, anUpperTypeBound).yes)
                    newUpperTypeBound = newUpperTypeBound + anUpperTypeBound
            }
            if (newUpperTypeBound.size == 0)
                ReferenceValue(pc, supertype)
            else
                ReferenceValue(pc, newUpperTypeBound + supertype)
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
                case MReferenceValue(thatUpperTypeBound) ⇒
                    val thisUpperTypeBound = this.upperTypeBound
                    if (thisUpperTypeBound == thatUpperTypeBound)
                        return NoUpdate
                    val thisDomain = domain
                    var newUpperTypeBound = thisUpperTypeBound
                    thatUpperTypeBound foreach { (otherUpperTypeBound: ReferenceType) ⇒
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
                    } else if (newUpperTypeBound eq thatUpperTypeBound) {
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

        override def updateIsNull(pc: PC, isNull: Answer): DomainValue = {
            isNull match {
                case Yes ⇒ NullValue(pc)
                case _   ⇒ this
            }
        }

        override def summarize(pc: PC): DomainValue = this

        override def toString() =
            "ReferenceValue("+upperTypeBound.map(_.toJava).mkString(" with ")+")"
            }

    object MReferenceValue {
        def unapply(that: MReferenceValue): Option[UpperTypeBound] =
            Some(that.upperTypeBound)
    }

    /**
     * Determines if the type described by the first set of upper type bounds is
     * a subtype of the second type.
     */
    protected def isSubtypeOf(
        typeBoundsA: UpperTypeBound,
        typeBoundsB: UpperTypeBound): Boolean = {
        typeBoundsA forall { aType ⇒
            typeBoundsB exists { bType ⇒
                domain.isSubtypeOf(aType, bType).yes
            }
        }
    }

    protected def summarizeReferenceValues(
        pc: PC,
        values: Iterable[DomainValue]): DomainValue =
        (values.head.summarize(pc) /: values.tail) {
            (c, n) ⇒ c.summarize(pc, n)
        }

    //
    // FACTORY METHODS
    //

    private[this] val TheNullValue: ReferenceValue = new NullValue()
    override def NullValue(pc: PC): ReferenceValue = TheNullValue

    override def NonNullReferenceValue(pc: PC, objectType: ObjectType): ReferenceValue =
        new SObjectValue(objectType)

    /**
     * ==Summary==
     * The properties of the domain value are:
     *
     *  - Type: '''Precise'''
     *  - Null: '''No'''
     */
    override def NewArray(pc: PC, arrayType: ArrayType): ArrayValue =
        new ArrayValue(arrayType)

    /**
     *
     * ==Summary==
     * The properties of the domain value are:
     *
     *  - Type: '''Upper Bound'''
     *  - Null: '''MayBe'''
     */
    override def ArrayValue(pc: PC, arrayType: ArrayType): ArrayValue =
        new ArrayValue(arrayType)

    override def ReferenceValue(pc: PC, referenceType: ReferenceType): ReferenceValue =
        referenceType match {
            case ot: ObjectType ⇒ new SObjectValue(ot)
            case at: ArrayType  ⇒ new ArrayValue(at)
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
    def ReferenceValue(pc: PC, upperTypeBound: UpperTypeBound): ReferenceValue = {
        assert(upperTypeBound.nonEmpty)
        if (upperTypeBound.tail.isEmpty)
            ReferenceValue(pc, upperTypeBound.head)
        else
            new MReferenceValue(upperTypeBound)
    }

    override def NewObject(pc: PC, objectType: ObjectType): ReferenceValue =
        new SObjectValue(objectType)

    override def InitializedObject(pc: PC, referenceType: ReferenceType): ReferenceValue =
        if (referenceType.isArrayType)
            new ArrayValue(referenceType.asArrayType)
        else
            new SObjectValue(referenceType.asObjectType)

    override def StringValue(pc: PC, value: String): DomainValue =
        new SObjectValue(ObjectType.String)

    override def ClassValue(pc: PC, t: Type): DomainValue =
        new SObjectValue(ObjectType.Class)

}
