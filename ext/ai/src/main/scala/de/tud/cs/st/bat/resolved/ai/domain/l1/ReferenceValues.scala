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
package l1

import de.tud.cs.st.util.{ Answer, Yes, No, Unknown }

import scala.collection.SortedSet

/**
 * @author Michael Eichberg
 */
trait ReferenceValues[+I] extends l0.DefaultTypeLevelReferenceValues[I] with Origin {
    domain: Configuration with IntegerValuesComparison with ClassHierarchy ⇒

    type DomainSingleOriginReferenceValue <: SingleOriginReferenceValue with DomainReferenceValue
    type DomainNullValue <: NullValue with DomainSingleOriginReferenceValue
    type DomainObjectValue <: ObjectValue with DomainSingleOriginReferenceValue
    type DomainArrayValue <: ArrayValue with DomainSingleOriginReferenceValue
    
    type DomainMultipleReferenceValues <: MultipleReferenceValues with DomainReferenceValue

    /**
     * Functionality common to all DomainValues that represent a reference value where
     * – in the current analysis context – the value has a single origin.
     */
    trait SingleOriginReferenceValue extends ReferenceValue with SingleOriginValue {
        this: DomainSingleOriginReferenceValue ⇒

        /**
         * Copy constructor.
         */
        def apply(
            pc: PC = this.pc,
            isNull: Answer = this.isNull): DomainSingleOriginReferenceValue

        override def refineIsNull(pc: PC, isNull: Answer): DomainSingleOriginReferenceValue

        /*ABSTRACT*/ protected def doJoinWithNonNullValueWithSameOrigin(
            joinPC: PC,
            that: DomainReferenceValue): Update[DomainValue]

        protected def doJoinWithMultipleReferenceValues(
            joinPC: PC,
            other: DomainMultipleReferenceValues): Update[DomainValue] = {

            other.values foreach { that ⇒
                if (this.pc == that.pc)
                    // Invariant:
                    // At most one value represented by MultipleReferenceValues
                    // has the same pc as this value.
                    this.join(joinPC, that) match {
                        case NoUpdate ⇒
                            // This value is more general than the value
                            // in MultipleReferenceValues.
                            return StructuralUpdate(
                                MultipleReferenceValues(other.values - that + this))
                        case SomeUpdate(`that`) ⇒
                            return StructuralUpdate(other)
                        case SomeUpdate(newValue: SingleOriginReferenceValue) ⇒
                            return StructuralUpdate(
                                MultipleReferenceValues(other.values - that + newValue))
                        case _ ⇒
                            domainException(
                                domain,
                                "unexpected: update led to a value with multiple origins")
                    }
            }

            StructuralUpdate(MultipleReferenceValues(other.values + this))
        }

        protected def doJoinWithNullValueWithSameOrigin(
            joinPC: PC,
            that: DomainNullValue): Update[DomainValue] = {
            if (this.isNull.maybeYes)
                // the other value is also a null value or maybe "null"
                NoUpdate
            else
                StructuralUpdate(this(isNull = Unknown))
        }

        override protected def doJoin(
            joinPC: PC,
            other: DomainValue): Update[DomainValue] = {
            if (this eq other)
                return NoUpdate
            else {
                other match {
                    case that: DomainSingleOriginReferenceValue ⇒
                        if (this.pc == that.pc)
                            that match {
                                case that: DomainNullValue ⇒
                                    doJoinWithNullValueWithSameOrigin(joinPC, that)
                                case _ ⇒
                                    doJoinWithNonNullValueWithSameOrigin(joinPC, that)
                            }
                        else
                            StructuralUpdate(MultipleReferenceValues(Set(this, that)))
                    case that: DomainMultipleReferenceValues ⇒
                        doJoinWithMultipleReferenceValues(joinPC, that)
                }
            }
        }
    }

    protected class NullValue(
        val pc: PC)
            extends super.NullValue with SingleOriginReferenceValue {
        this: DomainNullValue ⇒

        /**
         * Copy constructor.
         */
        override def apply(
            pc: PC = this.pc,
            isNull: Answer = Yes): DomainNullValue = {
            require(isNull.yes)

            new NullValue(pc)
        }

        protected override def doJoinWithNonNullValueWithSameOrigin(
            joinPC: PC,
            that: SingleOriginReferenceValue): Update[DomainValue] = {
            if (that.isNull.isUndefined)
                StructuralUpdate(that)
            else
                StructuralUpdate(that(isNull = Unknown))
        }

        override def equals(other: Any): Boolean = {
            other match {
                case that: NullValue ⇒ (that canEqual this) && that.pc == this.pc
                case _               ⇒ false
            }
        }

        def canEqual(other: NullValue): Boolean = true

        override def hashCode: Int = pc

        override def toString() = "null(pc="+pc+")"
    }

    trait NonNullSingleOriginReferenceValue extends SingleOriginReferenceValue {
        this: DomainSingleOriginReferenceValue ⇒

        override def refineIsNull(pc: PC, isNull: Answer): DomainSingleOriginReferenceValue = {
            if (this.isNull == isNull)
                this
            else if (isNull.yes)
                // TODO [Type Safety] Use the Virtual Classes Pattern over here!
                NullValue(this.pc)
            else if (isNull.no)
                this(isNull = No)
            else
                domainException(
                    domain,
                    "refinement to Unknown is not supported")
        }

    }

    protected class ArrayValue(
        val pc: PC,
        override val isNull: Answer,
        override val isPrecise: Boolean,
        theUpperTypeBound: ArrayType)
            extends super.ArrayValue(theUpperTypeBound)
            with NonNullSingleOriginReferenceValue {
        this: DomainArrayValue ⇒

        require(this.isNull.maybeNo)

        override def apply(pc: PC, isNull: Answer): DomainArrayValue = {
            ArrayValue(pc, isNull, isPrecise, theUpperTypeBound)
        }

        override def doJoinWithNonNullValueWithSameOrigin(
            joinPC: Int,
            other: SingleOriginReferenceValue): Update[DomainValue] = {

            other match {
                case that: ArrayValue ⇒
                    val thisUpperTypeBound = this.theUpperTypeBound
                    val thatUpperTypeBound = that.theUpperTypeBound
                    joinArrayTypes(thisUpperTypeBound, thatUpperTypeBound) match {

                        case Left(`thisUpperTypeBound`) if (
                            this.isNull == that.isNull &&
                            (!this.isPrecise ||
                                ((thisUpperTypeBound eq thatUpperTypeBound) &&
                                    that.isPrecise))
                        ) ⇒
                            NoUpdate

                        case Left(`thatUpperTypeBound`) if (
                            this.isNull == that.isNull &&
                            (!that.isPrecise ||
                                ((thisUpperTypeBound eq thatUpperTypeBound) &&
                                    this.isPrecise))
                        ) ⇒
                            StructuralUpdate(other)

                        case Left(newUpperTypeBound) ⇒
                            val newIsNull = this.isNull merge that.isNull
                            val newIsPrecise = this.isPrecise && that.isPrecise &&
                                (thisUpperTypeBound eq thatUpperTypeBound)
                            StructuralUpdate(
                                ArrayValue(
                                    this.pc, newIsNull, newIsPrecise, newUpperTypeBound))

                        case Right(newUpperTypeBound) ⇒
                            val newIsNull = this.isNull merge that.isNull
                            StructuralUpdate(
                                ReferenceValue(this.pc, newIsNull, newUpperTypeBound))
                    }

                case that: SObjectValue ⇒
                    val thatUpperTypeBound = that.theUpperTypeBound
                    joinAnyArrayTypeWithObjectType(thatUpperTypeBound) match {

                        case Left(`thatUpperTypeBound`) if (
                            this.isNull == that.isNull && !that.isPrecise
                        ) ⇒
                            StructuralUpdate(other)

                        case Left(newUpperTypeBound) ⇒
                            val newIsNull = this.isNull merge that.isNull
                            StructuralUpdate(
                                ObjectValue(this.pc, newIsNull, false, newUpperTypeBound))

                        case Right(newUpperTypeBound) ⇒
                            val newIsNull = this.isNull merge that.isNull
                            StructuralUpdate(
                                ReferenceValue(this.pc, newIsNull, newUpperTypeBound))
                    }

                case that: MObjectValue ⇒
                    val thatUpperTypeBound = that.upperTypeBound
                    joinAnyArrayTypeWithMultipleTypesBound(thatUpperTypeBound) match {
                        case Right(`thatUpperTypeBound`) if this.isNull == that.isNull ⇒
                            StructuralUpdate(other)

                        case Right(newUpperTypeBound) ⇒
                            val newIsNull = this.isNull merge that.isNull
                            StructuralUpdate(
                                ReferenceValue(this.pc, newIsNull, newUpperTypeBound))

                        case Left(newUpperTypeBound) ⇒
                            val newIsNull = this.isNull merge that.isNull
                            StructuralUpdate(
                                ObjectValue(this.pc, newIsNull, false, newUpperTypeBound))
                    }

            }
        }

        override def adapt[ThatI >: I](
            targetDomain: Domain[ThatI],
            pc: PC): targetDomain.DomainValue =
            targetDomain match {
                case thatDomain: l1.ReferenceValues[ThatI] ⇒
                    thatDomain.ArrayValue(
                        pc,
                        isNull,
                        isPrecise,
                        theUpperTypeBound).asInstanceOf[targetDomain.DomainValue]
                case thatDomain: l0.DefaultTypeLevelReferenceValues[ThatI] ⇒
                    thatDomain.ArrayValue(
                        pc,
                        theUpperTypeBound).asInstanceOf[targetDomain.DomainValue]
                case _ ⇒ super.adapt(targetDomain, pc)
            }

        override def equals(other: Any): Boolean = {
            other match {
                case that: ArrayValue ⇒ (
                    (that eq this) ||
                    (
                        (that canEqual this) &&
                        this.pc == that.pc &&
                        this.isPrecise == that.isPrecise &&
                        this.isNull == that.isNull &&
                        (this.upperTypeBound eq that.upperTypeBound)
                    )
                )
                case _ ⇒ false
            }
        }

        protected def canEqual(other: ArrayValue): Boolean = true

        override def hashCode: Int =
            (((pc) * 41 +
                (if (isPrecise) 101 else 3)) * 13 +
                isNull.hashCode()) * 79 +
                upperTypeBound.hashCode()

        override def toString() = {
            var description = theUpperTypeBound.toJava+"(pc="+pc
            if (isNull.isUndefined) description += "; isNull=maybe"
            if (!isPrecise) description += ", isUpperBound"
            description += ")"
            description
        }
    }

    /**
     * @param pc The origin of the value (or the pseudo-origin (e.g., the index of
     *      the parameter) if the true origin is not known.)
     */
    protected class SObjectValue(
        val pc: PC,
        override val isNull: Answer,
        override val isPrecise: Boolean,
        theUpperTypeBound: ObjectType)
            extends super.SObjectValue(theUpperTypeBound)
            with NonNullSingleOriginReferenceValue {
        this: DomainObjectValue ⇒

        require(this.isNull.maybeNo)

        override def apply(pc: PC, isNull: Answer): DomainSingleOriginReferenceValue = {
            ObjectValue(pc, isNull, isPrecise, theUpperTypeBound)
        }

        override def refineUpperTypeBound(
            pc: PC,
            supertype: ReferenceType): DomainReferenceValue = {
            if (isPrecise)
                // Actually, it doesn't make sense to allow calls to this method if
                // the type is precise. However, we have to handle this case 
                // gracefully since it is possible that the value represented by
                // this "SObjectValue" is just one of many instances represented 
                // by a domain value on the stack/in a register 
                // and in this case it may make sense to establish a more stringent
                // bound for the others.
                return this
            if (supertype eq theUpperTypeBound)
                return this

            if (domain.isSubtypeOf(supertype, theUpperTypeBound).yes) {
                // this also handles the case where we cast an Object to an array
                ReferenceValue(this.pc, isNull, false, supertype)
            } else if (domain.isSubtypeOf(theUpperTypeBound, supertype).yes) {
                // useless refinement...
                this
            } else {
                if (supertype.isArrayType)
                    domainException(domain,
                        "impossible refinement "+theUpperTypeBound.toJava+
                            " => "+supertype.toJava)
                // basically, we are adding another type bound
                ReferenceValue(
                    this.pc,
                    this.isNull,
                    UIDList(supertype.asObjectType, theUpperTypeBound))
            }
        }

        override protected def doJoinWithNonNullValueWithSameOrigin(
            joinPC: Int,
            other: SingleOriginReferenceValue): Update[DomainValue] = {
            val thisUpperTypeBound = this.theUpperTypeBound
            other match {
                case that: SObjectValue ⇒
                    val thatUpperTypeBound = that.theUpperTypeBound
                    joinObjectTypes(thisUpperTypeBound, thatUpperTypeBound, true) match {
                        case Left(`thisUpperTypeBound`) if (
                            this.isNull == that.isNull &&
                            (!this.isPrecise ||
                                ((thisUpperTypeBound eq thatUpperTypeBound) && that.isPrecise))
                        ) ⇒
                            NoUpdate

                        case Left(`thatUpperTypeBound`) if (
                            this.isNull == that.isNull && !that.isPrecise
                        ) ⇒
                            StructuralUpdate(other)

                        case Left(newUpperTypeBound) ⇒
                            // Though the upper type bound of this value may 
                            // also be an upper type bound for the other value
                            // it does not precisely capture the other value's type!
                            val newIsNull = this.isNull merge that.isNull
                            StructuralUpdate(
                                ObjectValue(this.pc, newIsNull, false, newUpperTypeBound))

                        case Right(newUpperTypeBound) ⇒
                            val newIsNull = this.isNull merge that.isNull
                            StructuralUpdate(
                                ReferenceValue(this.pc, newIsNull, newUpperTypeBound))
                    }

                case that: MObjectValue ⇒
                    val thatUpperTypeBound = that.upperTypeBound

                    joinObjectTypes(thisUpperTypeBound, thatUpperTypeBound, true) match {
                        case Left(`thisUpperTypeBound`) if (
                            this.isNull == that.isNull && !this.isPrecise
                        ) ⇒
                            NoUpdate

                        case Left(newUpperTypeBound) ⇒
                            val newIsNull = this.isNull merge that.isNull
                            StructuralUpdate(
                                ObjectValue(this.pc, newIsNull, false, newUpperTypeBound))

                        case Right(`thatUpperTypeBound`) if this.isNull == that.isNull ⇒
                            StructuralUpdate(other)

                        case Right(newUpperTypeBound) ⇒
                            val newIsNull = this.isNull merge that.isNull
                            StructuralUpdate(
                                ReferenceValue(this.pc, newIsNull, newUpperTypeBound))
                    }

                case that: ArrayValue ⇒
                    val thatUpperTypeBound = that.theUpperTypeBound
                    joinAnyArrayTypeWithObjectType(thisUpperTypeBound) match {

                        case Left(`thisUpperTypeBound`) if (
                            this.isNull == that.isNull && !this.isPrecise
                        ) ⇒
                            NoUpdate

                        case Left(newUpperTypeBound) ⇒
                            val newIsNull = this.isNull merge that.isNull
                            StructuralUpdate(
                                ObjectValue(this.pc, newIsNull, false, newUpperTypeBound))

                        case Right(newUpperTypeBound) ⇒
                            val newIsNull = this.isNull merge that.isNull
                            StructuralUpdate(
                                ReferenceValue(this.pc, newIsNull, newUpperTypeBound))
                    }

            }
        }

        override def adapt[ThatI >: I](
            targetDomain: Domain[ThatI],
            pc: PC): targetDomain.DomainValue =
            targetDomain match {
                case thatDomain: l1.ReferenceValues[ThatI] ⇒
                    thatDomain.ReferenceValue(
                        pc,
                        isNull,
                        isPrecise,
                        theUpperTypeBound).asInstanceOf[targetDomain.DomainValue]
                case thatDomain: l0.DefaultTypeLevelReferenceValues[ThatI] ⇒
                    thatDomain.ReferenceValue(
                        pc,
                        theUpperTypeBound).asInstanceOf[targetDomain.DomainValue]
                case _ ⇒ super.adapt(targetDomain, pc)
            }

        override def equals(other: Any): Boolean = {
            other match {
                case that: SObjectValue ⇒ (
                    (that eq this) ||
                    (
                        (that canEqual this) &&
                        this.pc == that.pc &&
                        this.isPrecise == that.isPrecise &&
                        this.isNull == that.isNull &&
                        (this.upperTypeBound eq that.upperTypeBound)
                    )
                )
                case _ ⇒ false
            }
        }

        protected def canEqual(other: SObjectValue): Boolean = true

        override def hashCode: Int =
            (((theUpperTypeBound.hashCode()) * 41 +
                (if (isPrecise) 11 else 101)) * 13 +
                isNull.hashCode()) * 79 +
                pc

        override def toString() = {
            var description = theUpperTypeBound.toJava+"(pc="+pc
            if (isNull.isUndefined) description += "; isNull=maybe"
            if (!isPrecise) description += ", isUpperBound"
            description += ")"
            description
        }

    }

    protected class MObjectValue(
        val pc: PC,
        override val isNull: Answer,
        upperTypeBound: UIDList[ObjectType])
            extends super.MObjectValue(upperTypeBound)
            with NonNullSingleOriginReferenceValue {
        this: DomainObjectValue ⇒

        /**
         * Copy constructor.
         */
        override def apply(pc: PC, isNull: Answer): DomainObjectValue = {
            ReferenceValue(pc, isNull, upperTypeBound)
        }

        /*
        override def referenceValues: Iterable[IsAReferenceValue] = Iterable(this)

        /**
         * Determines if this reference value is a subtype of the given supertype by
         * delegating to the `isSubtypeOf(ReferenceType,ReferenceType)` method of the
         * domain.
         *
         * Additionally, the `isPrecise` property is taken into consideration to ensure
         * that a `No` answer means that it is impossible that any represented runtime
         * value is actually a subtype of the given supertype.
         */
        override def isValueSubtypeOf(supertype: ReferenceType): Answer = {
            assume(this.isNull.maybeNo)

            val answer: Answer = {
                var answer: Answer = No
                upperTypeBound foreach { t ⇒
                    val isSubtypeOf = domain.isSubtypeOf(t, supertype)
                    if (isSubtypeOf.yes) {
                        return Yes
                    } else {
                        answer merge isSubtypeOf
                    }
                }
                answer
            }
            answer match {
                case No if isPrecise ⇒
                    No
                case _ /* <=> No | Unknown*/ ⇒
                    // In general, we have to check whether a type exists that is a
                    // proper subtype of the type identified by this value's type bounds 
                    // and that is also a subtype of the given `supertype`. 
                    //
                    // If such a type does not exist the answer is truly `no` (if we 
                    // assume that we know the complete type hierarchy); 
                    // if we don't know the complete hierarchy or if we currently 
                    // analyze a library the answer generally has to be `Unknown`
                    // unless we also consider the classes that are final or .... 
                    Unknown
            }
        }

        def updateIsNull(pc: PC, isNull: Answer): AReferenceValue = {
            assume(this.isNull.isUndefined)

            if (isNull.yes)
                AReferenceValue(this.pc, UIDList.empty, Yes, true)
            else
                AReferenceValue(this.pc, upperTypeBound, isNull, isPrecise)
        }

        def refineUpperTypeBound(pc: PC, supertype: ReferenceType): AReferenceValue = {
            assume(this.isNull.maybeNo)

            isValueSubtypeOf(supertype) match {
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
                case No if upperTypeBound.forall(domain.isSubtypeOf(supertype, _).yes) ⇒
                    // The new upperBound is a subtype of all previous bounds and 
                    // hence completely replaces this value's type bound.
                    AReferenceValue(this.pc, UIDList(supertype), isNull, isPrecise)
                case _ /* (No && !isPrecise || Unknown) */ ⇒
                    var newUpperBoundType: UIDList[ReferenceType] = UIDList.empty
                    var addSupertype = true
                    upperTypeBound foreach { (anUpperTypeBound: ReferenceType) ⇒
                        if (supertype == anUpperTypeBound) {
                            /* do nothing */
                        } else if (domain.isSubtypeOf(supertype, anUpperTypeBound).yes) {
                            /* do nothing (we may add theUpperBound later) */
                        } else if (domain.isSubtypeOf(anUpperTypeBound, supertype).yes) {
                            addSupertype = false
                            newUpperBoundType = newUpperBoundType + anUpperTypeBound
                        } else {
                            newUpperBoundType = newUpperBoundType + anUpperTypeBound
                        }
                    }
                    if (addSupertype)
                        newUpperBoundType = newUpperBoundType + supertype

                    AReferenceValue(this.pc, newUpperBoundType, isNull, isPrecise)
            }
        }

        override def doJoin(mergePC: Int, value: DomainValue): Update[DomainValue] = {
            value match {
                case v @ AReferenceValue(otherPC, otherUpperTypeBounds, otherIsNull, otherIsPrecise) ⇒
                    if (otherPC != this.pc)
                        return StructuralUpdate(MultipleReferenceValues(Set(this, v)))
                    if (this.isNull == otherIsNull &&
                        this.upperTypeBound == otherUpperTypeBounds &&
                        (this.isPrecise == false || this.isPrecise == otherIsPrecise))
                        return NoUpdate

                    var newUpperTypeBound = this.upperTypeBound
                    otherUpperTypeBounds foreach { (otherUpperTypeBound: ReferenceType) ⇒
                        var addOtherUpperTypeBounds = true
                        newUpperTypeBound = newUpperTypeBound filterNot { vt ⇒
                            domain.isSubtypeOf(otherUpperTypeBound, vt) match {
                                case Yes ⇒
                                    true
                                case _ ⇒
                                    if (domain.isSubtypeOf(vt, otherUpperTypeBound).yes)
                                        addOtherUpperTypeBounds = false
                                    false
                            }
                        }
                        if (addOtherUpperTypeBounds)
                            newUpperTypeBound = newUpperTypeBound + otherUpperTypeBound
                    }
                    StructuralUpdate(
                        AReferenceValue(
                            this.pc,
                            newUpperTypeBound,
                            this.isNull merge otherIsNull,
                            this.isPrecise && otherIsPrecise))

                case mrv: MultipleReferenceValues ⇒
                    mrv.join(mergePC, this) match {
                        case NoUpdate                 ⇒ StructuralUpdate(mrv)
                        case SomeUpdate(updatedValue) ⇒ StructuralUpdate(updatedValue)
                    }
            }
        }

        override def adapt[ThatI >: I](
            targetDomain: Domain[ThatI],
            pc: PC): targetDomain.DomainValue =
            targetDomain match {
                case thatDomain: DefaultPreciseReferenceValues[ThatI] ⇒
                    adaptAReferenceValue(thatDomain, pc).asInstanceOf[targetDomain.DomainValue]
                case _ ⇒ super.adapt(targetDomain, pc)
            }

        protected[DefaultPreciseReferenceValues] def adaptAReferenceValue[ThatI >: I](
            targetDomain: DefaultPreciseReferenceValues[ThatI],
            pc: PC): targetDomain.AReferenceValue =
            targetDomain.AReferenceValue(pc, this.upperTypeBound, this.isNull, this.isPrecise)
*/

        override def equals(other: Any): Boolean = {
            other match {
                case that: MObjectValue ⇒
                    (this eq that) || (
                        (this canEqual that) &&
                        this.pc == that.pc &&
                        this.isNull == that.isNull &&
                        (this.upperTypeBound == that.upperTypeBound))
                case _ ⇒ false
            }
        }

        protected def canEqual(other: MObjectValue): Boolean = true

        override def hashCode: Int =
            (((upperTypeBound.hashCode()) * 41 +
                (if (isPrecise) 11 else 101)) * 13 +
                isNull.hashCode()) * 79 +
                pc

        override def toString() = {
            var description = upperTypeBound.map(_.toJava).mkString(" with ")+"(pc="+pc
            if (isNull.isUndefined) description += "; isNull=maybe"
            if (!isPrecise) description += ", isUpperBound"
            description += ")"
            description
        }

    }

    /**
     * A `MultipleReferenceValues` tracks multiple reference values (`NullValue`,
     * `ArrayValue`, `SObjectValue` and `MObjectValue`) that have different
     * origins. I.e., per origin (by default per program counter) one domain value is used
     * to abstract over the properties of that respective value.
     */
    protected class MultipleReferenceValues(
        val values: scala.collection.Set[DomainSingleOriginReferenceValue])
            extends ReferenceValue
            with MultipleOriginsValue {
        this: DomainMultipleReferenceValues ⇒

        override def pcs: Iterable[PC] = values.view.map(_.pc)

        /**
         * Calculates the most specific common supertype of all upper type bounds of
         * all values.
         */
        override lazy val upperTypeBound: UpperTypeBound = {
            val values = this.values.dropWhile(_.isNull.yes)
            if (values.isEmpty)
                // <=> all values are null values!
                UIDList.empty
            else {
                var overallUTB = values.head.upperTypeBound

                def currentUTBisUTBForArrays: Boolean = {
                    overallUTB.tail.isEmpty &&
                        overallUTB.head.isArrayType
                }

                def asUTBForArrays: ArrayType =
                    overallUTB.head.asArrayType

                def asUTBForObjects: UIDList[ObjectType] =
                    overallUTB.asInstanceOf[UIDList[ObjectType]]

                values.tail foreach { value ⇒
                    val newUpperTypeBound: Either[ReferenceType, UIDList[ReferenceType]] = value match {
                        case _: NullValue ⇒ /*"Do Nothing"*/ Right(overallUTB)
                        case SObjectValue(upperTypeBound) ⇒
                            if (currentUTBisUTBForArrays)
                                joinAnyArrayTypeWithObjectType(upperTypeBound)
                            else
                                joinObjectTypes(upperTypeBound, asUTBForObjects, true)
                        case MObjectValue(upperTypeBound) ⇒
                            if (currentUTBisUTBForArrays)
                                joinAnyArrayTypeWithMultipleTypesBound(upperTypeBound)
                            else
                                joinUpperTypeBounds(asUTBForObjects, upperTypeBound, true)

                        case ArrayValue(upperTypeBound) ⇒
                            if (currentUTBisUTBForArrays)
                                joinArrayTypes(asUTBForArrays, upperTypeBound)
                            else
                                joinAnyArrayTypeWithMultipleTypesBound(asUTBForObjects)

                    }
                    newUpperTypeBound match {
                        case Left(referenceType)   ⇒ overallUTB = UIDList(referenceType)
                        case Right(referenceTypes) ⇒ overallUTB = referenceTypes
                    }
                }
                overallUTB
            }
        }

        override def referenceValues: Iterable[IsAReferenceValue] = values

        /**
         * Returns `true` if the upper type bound of this value precisely captures the
         * runtime type of the value. This basically requires that all '''non-null''' values
         * are precise and have the same upper type bound. Null values are ignored.
         */
        override lazy val isPrecise: Boolean = calculateIsPrecise()

        private[this] def calculateIsPrecise(): Boolean = {
            val values = this.values.dropWhile(_.isNull.yes)
            if (values.nonEmpty) {
                val theUpperTypeBound = values.head.upperTypeBound
                values.tail foreach { value ⇒
                    if (!value.isPrecise ||
                        (value.isNull.maybeNo && value.upperTypeBound != theUpperTypeBound))
                        return false
                }
            }
            // <=> all values are null values or have the same bound
            true
        }

        override lazy val isNull: Answer = calculateIsNull()

        private[this] def calculateIsNull(): Answer = {
            val firstAnswer = values.head.isNull
            if (firstAnswer.isUndefined)
                return Unknown

            (firstAnswer /: values.tail) { (currentAnswer, n) ⇒
                val nextAnswer = n.isNull
                if (nextAnswer.isUndefined)
                    return Unknown

                currentAnswer merge nextAnswer
            }
        }

        /**
         * Summarizes this value by creating a new domain value that abstracts over
         * the properties of all values.
         *
         * The given `pc` is used as the program counter of the newly created value.
         */
        override def summarize(pc: PC): DomainReferenceValue = {
            upperTypeBound match {
                case UIDList.empty ⇒ NullValue(pc)
                case SingleElementUIDList(at: ArrayType) ⇒
                    ArrayValue(pc, isNull, isPrecise, at)
                case SingleElementUIDList(ot: ObjectType) ⇒
                    ObjectValue(pc, isNull, isPrecise, ot)
                case utb ⇒
                    ReferenceValue(pc, isNull, utb.asInstanceOf[UIDList[ObjectType]])
            }
        }

        override def adapt[TDI >: I](target: Domain[TDI], pc: PC): target.DomainValue =
            if (target.isInstanceOf[l1.ReferenceValues[TDI]]) {
                val thatDomain = target.asInstanceOf[l1.ReferenceValues[TDI]]
                val newValues =
                    (this.values.map { value ⇒ value.adapt(thatDomain, pc) }).
                        asInstanceOf[Set[thatDomain.DomainSingleOriginReferenceValue]]
                thatDomain.MultipleReferenceValues(newValues).asInstanceOf[target.DomainValue]
            } else
                // TODO [Improvement] Add support for the target Domain l0.DefaultReferenceValues
                super.adapt(target, pc)

        override def isValueSubtypeOf(supertype: ReferenceType): Answer = {
            // Recall that the client has to make an "isNull" check before calling
            // isValueSubtypeOf. Hence, at least one of the possible reference values 
            // has to be non null.
            val values = this.values.dropWhile(_.isNull.yes)
            var answer: Answer = values.head.isValueSubtypeOf(supertype)
            values.tail foreach { value ⇒
                if (answer == Unknown)
                    return Unknown

                if (!value.isNull.yes) {
                    answer = answer merge value.isValueSubtypeOf(supertype)
                }
            }
            // either Yes or No
            answer
        }

        override def refineIsNull(pc: PC, isNull: Answer): DomainReferenceValue = {
            // Recall that this value's property – as a whole – can be undefined also 
            // each value's property is well defined (Yes, No)
            // Furthermore, isNull is either Yes or No and we are going to ignore
            // those updates that are meaningless.
            val relevantValues =
                isNull match {
                    case Yes ⇒ this.values filter { _.isNull.maybeYes }
                    case No  ⇒ this.values filter { _.isNull.maybeNo }
                    case _   ⇒ domainException(domain, "unsupported refinement")
                }
            var valueRefined = false
            val refinedValues: scala.collection.Set[DomainSingleOriginReferenceValue] =
                relevantValues map { value ⇒
                    val refinedValue = value.refineIsNull(pc, isNull)
                    if (refinedValue ne value)
                        valueRefined = true
                    refinedValue
                }
            if (refinedValues.size == 1)
                refinedValues.head
            // The following test should not be necessary, as this method is
            // only intended to be called, if this value as a whole needs
            // refinement:
            else if (valueRefined || this.values.size != refinedValues.size)
                MultipleReferenceValues(refinedValues)
            else
                // defensive programming...
                this
        }

        override def refineUpperTypeBound(pc: PC, supertype: ReferenceType): DomainReferenceValue = {
            import scala.collection.mutable.HashSet
            val newValues = HashSet.empty[DomainSingleOriginReferenceValue]
            var valueRefined = false
            this.values foreach { value ⇒
                if (value.isNull.yes)
                    newValues += value
                else {
                    val isSubtypeOf = value.isValueSubtypeOf(supertype)
                    if (isSubtypeOf.yes)
                        newValues += value
                    else if (isSubtypeOf.isUndefined) {
                        val newValue = value.refineUpperTypeBound(pc, supertype)
                        valueRefined = valueRefined || (newValue ne value)
                        newValues += newValue.asInstanceOf[DomainSingleOriginReferenceValue]
                    }
                    // if isSubtypeOf.no then we can just remove it
                }
            }

            if (newValues.size == 1)
                newValues.head
            // The following test should not be necessary, as this method is
            // only intended to be called, if this value as a whole needs
            // refinement:
            else if (valueRefined || this.values.size != newValues.size)
                MultipleReferenceValues(newValues)
            else
                this
        }

        override protected def doJoin(joinPC: PC, value: DomainValue): Update[DomainValue] = {
            null
            //            value match {
            //                case otherArv: AReferenceValue ⇒
            //                    values.find(_.pc == otherArv.pc) match {
            //                        case None ⇒
            //                            StructuralUpdate(MultipleReferenceValues(values + otherArv))
            //                        case Some(thisArv) ⇒
            //                            thisArv.doJoin(mergePC, otherArv) match {
            //                                case NoUpdate ⇒ NoUpdate
            //                                case update @ SomeUpdate(updatedArv: AReferenceValue) ⇒
            //                                    update.updateValue(
            //                                        MultipleReferenceValues((values - thisArv) + updatedArv)
            //                                    )
            //                            }
            //                    }
            //                case otherMrv: MultipleReferenceValues ⇒
            //                    var updateType: UpdateType = NoUpdateType
            //                    var otherRemainingArvs = otherMrv.values
            //                    var newValues: Set[AReferenceValue] = values.map { thisArv ⇒
            //                        otherRemainingArvs.find(_.pc == thisArv.pc) match {
            //                            case None ⇒ thisArv
            //                            case Some(otherArv) ⇒
            //                                otherRemainingArvs -= otherArv
            //                                thisArv.join(mergePC, otherArv) match {
            //                                    case NoUpdate ⇒
            //                                        thisArv
            //                                    case update @ SomeUpdate(updatedArv: AReferenceValue) ⇒
            //                                        updateType = updateType &: update
            //                                        updatedArv
            //                                }
            //                        }
            //                    }
            //                    if (otherRemainingArvs.size > 0) {
            //                        newValues ++= otherRemainingArvs
            //                        updateType = StructuralUpdateType
            //                    }
            //                    updateType(MultiReferencesValue(newValues))
            //            }
        }

        override def hashCode(): Int = values.hashCode

        override def equals(other: Any): Boolean = {
            other match {
                case that: MultipleReferenceValues ⇒ that.values == this.values
                case _                             ⇒ false
            }
        }

        override def toString() = values.mkString("OneOf(\n\t", ",\n\t", ")")
    }

    //
    // FACTORY METHODS
    //

    def NonNullReferenceValue(pc: PC, objectType: ObjectType): DomainReferenceValue

    def ArrayValue(
        pc: PC,
        isNull: Answer,
        isPrecise: Boolean,
        theUpperTypeBound: ArrayType): DomainArrayValue

    override def ObjectValue(pc: PC, objectType: ObjectType): DomainObjectValue

    def ObjectValue(
        pc: PC,
        isNull: Answer,
        isPrecise: Boolean,
        theUpperTypeBound: ObjectType): DomainObjectValue

    def MultipleReferenceValues(
        values: scala.collection.Set[DomainSingleOriginReferenceValue]): DomainMultipleReferenceValues

    def ReferenceValue(
        pc: PC,
        isNull: Answer,
        isPrecise: Boolean,
        theUpperTypeBound: ReferenceType): DomainReferenceValue

    override def ReferenceValue(pc: PC, upperTypeBound: UIDList[ObjectType]): DomainReferenceValue

    def ReferenceValue(
        pc: PC,
        isNull: Answer,
        upperTypeBound: UIDList[ObjectType]): DomainSingleOriginReferenceValue

    override def NewObject(pc: PC, objectType: ObjectType): DomainObjectValue

    override def InitializedObject(pc: PC, referenceType: ReferenceType): DomainReferenceValue

}
