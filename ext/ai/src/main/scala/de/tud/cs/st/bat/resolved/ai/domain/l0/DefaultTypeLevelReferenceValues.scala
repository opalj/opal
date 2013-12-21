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

    // -----------------------------------------------------------------------------------
    //
    // REPRESENTATION OF REFERENCE VALUES
    //
    // -----------------------------------------------------------------------------------

    protected class ANullValue extends super.NullValue { this: DomainValue ⇒

        override def doJoin(joinPC: Int, other: DomainValue): Update[DomainValue] = {
            val that = asReferenceValue(other)
            if (that.isNull.yes)
                NoUpdate
            else
                StructuralUpdate(
                    if (that.isNull.maybeNo)
                        other
                    else that match {
                        case AReferenceValue(typeBound) ⇒
                            ReferenceValue(joinPC, typeBound)
                        case MReferenceValue(typeBound) ⇒
                            ReferenceValue(joinPC, typeBound)
                    }
                )
        }
    }

    protected class AReferenceValue(
        val theUpperTypeBound: ReferenceType)
            extends super.ClassValue { value ⇒

        override def referenceValues: Iterable[IsAReferenceValue] = Iterable(this)

        override def upperTypeBound: UpperTypeBound = UIDList(theUpperTypeBound)

        override def isSubtypeOf(supertype: ReferenceType): Answer = {
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
            // BATAI calls this method only if a previous "subtype of" test 
            // (this.typeOfvalue <: supertype ?) 
            // returned unknown or no and we are now on the branch where this relation
            // has to hold. Hence, we only need to handle the case where 
            // supertype is more strict than/is a subtype of typeOfValue.
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
            val thisTypeBound = this.theUpperTypeBound
            other match {
                case AReferenceValue(thatTypeBound) ⇒
                    if ((thisTypeBound eq thatTypeBound) ||
                        domain.isSubtypeOf(thatTypeBound, thisTypeBound).yes)
                        NoUpdate
                    else if (domain.isSubtypeOf(thisTypeBound, thatTypeBound).yes)
                        StructuralUpdate(other)
                    else
                        StructuralUpdate(
                            ReferenceValue(joinPC, UIDList(thisTypeBound, thatTypeBound))
                        )
                case MReferenceValue(thatUpperTypeBound) ⇒
                    val newTypeBound = thatUpperTypeBound filter { thatType ⇒
                        domain.isSubtypeOf(thisTypeBound, thatType) match {
                            case Yes ⇒
                                return StructuralUpdate(other)
                            case No ⇒
                                if (domain.isSubtypeOf(thatType, thisTypeBound).yes)
                                    false
                                else
                                    true
                            case Unknown ⇒
                                true
                        }
                    }
                    if (newTypeBound.isEmpty)
                        // all upper bounds of "other" are a subtype of this upper bound
                        NoUpdate
                    else
                        StructuralUpdate(
                            ReferenceValue(joinPC, newTypeBound + thisTypeBound)
                        )
                case NullValue() ⇒
                    NoUpdate
            }
        }

        override def adapt[ThatI >: I](target: Domain[ThatI], pc: PC): target.DomainValue =
            target.ReferenceValue(pc, theUpperTypeBound)

        override def summarize(pc: PC): DomainValue = this

        override def summarize(pc: PC, value: DomainValue): DomainValue =
            this.join(pc, value) match {
                case StructuralUpdate(value) ⇒ value
                case _                       ⇒ this
            }
    }

    object AReferenceValue {
        def unapply(that: AReferenceValue): Option[ReferenceType] =
            Some(that.theUpperTypeBound)
    }

    protected class MReferenceValue(
        val upperTypeBound: UpperTypeBound)
            extends super.ReferenceValue { value ⇒

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
        override def isSubtypeOf(supertype: ReferenceType): Answer = {
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
                case AReferenceValue(thatUpperTypeBound) ⇒
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

        override def summarize(pc: PC): DomainValue = this

        override def summarize(pc: PC, value: DomainValue): DomainValue = {
            join(pc, value) match {
                case NoUpdate                 ⇒ this
                case SomeUpdate(updatedValue) ⇒ updatedValue
            }
        }

        //        override def equals(other: Any): Boolean =
        //            other match {
        //                case that: MReferenceValue ⇒
        //                    (that eq this) || (
        //                        (that canEqual this) &&
        //                        this.upperTypeBound == that.upperTypeBound)
        //                case _ ⇒ false
        //            }
        //
        //        protected def canEqual(other: MReferenceValue): Boolean = true
        //
        //        override lazy val hashCode: Int = (upperTypeBound.hashCode() * 41)

        override def toString() =
            "TypeLevelReferenceValue("+upperTypeBound.map(_.toJava).mkString(" with ")+")"
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

    private[this] val TheNullValue: ReferenceValue = new ANullValue()
    override def NullValue(pc: PC): ReferenceValue = TheNullValue

    override def NonNullReferenceValue(pc: PC, objectType: ObjectType): ReferenceValue =
        new AReferenceValue(objectType)

    override def ReferenceValue(pc: PC, referenceType: ReferenceType): ReferenceValue =
        new AReferenceValue(referenceType)

    /**
     * Factory method to create a `DomainValue` that represents ''either a reference
     * value that has the given type bound and is initialized or the value `null`''. However, the
     * information whether the value is `null` or not is not available. Furthermore, the
     * type may also be an upper bound. I.e., we may have multiple types that and
     * the runtime type is guaranteed to be a subtype of all given types.
     *
     * The domain may ignore the information about the value and the origin (`pc`), but
     * it has to remain possible for the domain to identify the component type of an
     * array.
     *
     * ==Summary==
     * The properties of the domain value are:
     *
     *  - Initialized: '''yes''' (the constructor was called)
     *  - Type: '''Upper Bound'''
     *  - Null: '''MayBe''' (It is unknown whether the value is `null` or not.)
     */
    def ReferenceValue(pc: PC, upperTypeBound: UpperTypeBound): ReferenceValue =
        new MReferenceValue(upperTypeBound)

    override def NewObject(pc: PC, referenceType: ReferenceType): ReferenceValue =
        new AReferenceValue(referenceType)

    override def InitializedObject(pc: PC, referenceType: ReferenceType): ReferenceValue =
        new AReferenceValue(referenceType)

    override def StringValue(pc: PC, value: String): DomainValue =
        new AReferenceValue(ObjectType.String)

    override def ClassValue(pc: PC, t: Type): DomainValue =
        new AReferenceValue(ObjectType.Class)

    // -----------------------------------------------------------------------------------
    //
    // CREATING ARRAY REPRESENTATIONS
    //
    // -----------------------------------------------------------------------------------

    //
    // CREATE ARRAY
    //
    override def newarray(
        pc: PC,
        count: DomainValue,
        componentType: FieldType): Computation[DomainValue, DomainValue] =
        //ComputedValueAndException(TypedValue(ArrayType(componentType)), TypedValue(ObjectType.NegativeArraySizeException))
        ComputedValue(InitializedObject(pc, ArrayType(componentType)))

    /**
     * @note The componentType may be (again) an array type.
     */
    override def multianewarray(
        pc: PC,
        counts: List[DomainValue],
        arrayType: ArrayType): Computation[DomainValue, DomainValue] =
        //ComputedValueAndException(TypedValue(arrayType), TypedValue(ObjectType.NegativeArraySizeException))
        ComputedValue(InitializedObject(pc, arrayType))

}
