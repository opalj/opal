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
package l1

import scala.collection.SortedSet

import org.opalj.util.{ Answer, Yes, No, Unknown }
import org.opalj.collection.immutable.{ UIDSet, UIDSet0, UIDSet1 }

import br._

/**
 * @author Michael Eichberg
 */
trait ReferenceValues extends l0.DefaultTypeLevelReferenceValues with Origin {
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
            that: DomainSingleOriginReferenceValue): Update[DomainSingleOriginReferenceValue]

        protected def doJoinWithMultipleReferenceValues(
            joinPC: PC,
            other: DomainMultipleReferenceValues): Update[DomainReferenceValue] = {

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
                        case SomeUpdate(right) if right eq that ⇒
                            return StructuralUpdate(other)
                        case SomeUpdate(newValue: DomainSingleOriginReferenceValue) ⇒
                            return StructuralUpdate(
                                MultipleReferenceValues(other.values - that + newValue))
                        case _ ⇒
                            throw DomainException("internal implementation error; two values with the same origin resulted in a value with multiple origins")
                    }
            }

            StructuralUpdate(MultipleReferenceValues(other.values + this))
        }

        protected def doJoinWithNullValueWithSameOrigin(
            joinPC: PC,
            that: NullValue): Update[DomainSingleOriginReferenceValue] = {
            if (this.isNull.isYesOrUnknown)
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
            require(isNull.isYes)

            NullValue(pc)
        }

        protected override def doJoinWithNonNullValueWithSameOrigin(
            joinPC: PC,
            that: DomainSingleOriginReferenceValue): Update[DomainSingleOriginReferenceValue] = {
            if (that.isNull.isUnknown)
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
            else if (isNull.isYes)
                NullValue(this.pc)
            else if (isNull.isNo)
                this(isNull = No)
            else
                throw DomainException("refining \"isNull\" to Unknown is not supported")
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

        require(this.isNull.isNoOrUnknown)

        override def apply(pc: PC, isNull: Answer): DomainArrayValue = {
            ArrayValue(pc, isNull, isPrecise, theUpperTypeBound)
        }

        override def doJoinWithNonNullValueWithSameOrigin(
            joinPC: PC,
            other: DomainSingleOriginReferenceValue): Update[DomainSingleOriginReferenceValue] = {

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
                            val newIsNull = this.isNull & that.isNull
                            val newIsPrecise = this.isPrecise && that.isPrecise &&
                                (thisUpperTypeBound eq thatUpperTypeBound)
                            StructuralUpdate(
                                ArrayValue(
                                    this.pc, newIsNull, newIsPrecise, newUpperTypeBound))

                        case Right(newUpperTypeBound) ⇒
                            val newIsNull = this.isNull & that.isNull
                            StructuralUpdate(
                                ObjectValue(this.pc, newIsNull, newUpperTypeBound))
                    }

                case that: SObjectValue ⇒
                    val thatUpperTypeBound = that.theUpperTypeBound
                    joinAnyArrayTypeWithObjectType(thatUpperTypeBound) match {

                        case Left(`thatUpperTypeBound`) if (
                            this.isNull == that.isNull && !that.isPrecise
                        ) ⇒
                            StructuralUpdate(other)

                        case Left(newUpperTypeBound) ⇒
                            val newIsNull = this.isNull & that.isNull
                            StructuralUpdate(
                                ObjectValue(this.pc, newIsNull, false, newUpperTypeBound))

                        case Right(newUpperTypeBound) ⇒
                            val newIsNull = this.isNull & that.isNull
                            StructuralUpdate(
                                ObjectValue(this.pc, newIsNull, newUpperTypeBound))
                    }

                case that: MObjectValue ⇒
                    val thatUpperTypeBound = that.upperTypeBound
                    joinAnyArrayTypeWithMultipleTypesBound(thatUpperTypeBound) match {
                        case Right(`thatUpperTypeBound`) if this.isNull == that.isNull ⇒
                            StructuralUpdate(other)

                        case Right(newUpperTypeBound) ⇒
                            val newIsNull = this.isNull & that.isNull
                            StructuralUpdate(
                                ObjectValue(this.pc, newIsNull, newUpperTypeBound))

                        case Left(newUpperTypeBound) ⇒
                            val newIsNull = this.isNull & that.isNull
                            StructuralUpdate(
                                ObjectValue(this.pc, newIsNull, false, newUpperTypeBound))
                    }

            }
        }

        override def adapt(target: Domain, pc: PC): target.DomainValue =
            target match {

                case thatDomain: l1.ReferenceValues ⇒
                    thatDomain.ArrayValue(pc, isNull, isPrecise, theUpperTypeBound).
                        asInstanceOf[target.DomainValue]

                case thatDomain: l0.DefaultTypeLevelReferenceValues ⇒
                    thatDomain.ReferenceValue(pc, theUpperTypeBound).
                        asInstanceOf[target.DomainValue]

                case _ ⇒ super.adapt(target, pc)
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
            if (isNull.isUnknown) description += "; isNull=maybe"
            if (!isPrecise) description += ", isUpperBound"
            description += ")"
            description
        }
    }

    trait ObjectValue extends super.ObjectValue with NonNullSingleOriginReferenceValue {
        this: DomainObjectValue ⇒
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
            with ObjectValue {
        this: DomainObjectValue ⇒

        require(this.isNull.isNoOrUnknown)

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

            if (domain.isSubtypeOf(supertype, theUpperTypeBound).isYes) {
                // this also handles the case where we cast an Object to an array
                ReferenceValue(this.pc, isNull, false, supertype)
            } else if (domain.isSubtypeOf(theUpperTypeBound, supertype).isYes) {
                // useless refinement...
                this
            } else {
                if (supertype.isArrayType)
                    throw ImpossibleRefinement(this, "incompatible bound "+supertype.toJava)

                // basically, we are adding another type bound
                ObjectValue(
                    this.pc,
                    this.isNull,
                    UIDSet(supertype.asObjectType, theUpperTypeBound))
            }
        }

        override protected def doJoinWithNonNullValueWithSameOrigin(
            joinPC: Int,
            other: DomainSingleOriginReferenceValue): Update[DomainSingleOriginReferenceValue] = {
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
                            val newIsNull = this.isNull & that.isNull
                            StructuralUpdate(
                                ObjectValue(this.pc, newIsNull, false, newUpperTypeBound))

                        case Right(newUpperTypeBound) ⇒
                            val newIsNull = this.isNull & that.isNull
                            StructuralUpdate(
                                ObjectValue(this.pc, newIsNull, newUpperTypeBound))
                    }

                case that: MObjectValue ⇒
                    val thatUpperTypeBound = that.upperTypeBound

                    joinObjectTypes(thisUpperTypeBound, thatUpperTypeBound, true) match {
                        case Left(`thisUpperTypeBound`) if (
                            this.isNull == that.isNull && !this.isPrecise
                        ) ⇒
                            NoUpdate

                        case Left(newUpperTypeBound) ⇒
                            val newIsNull = this.isNull & that.isNull
                            StructuralUpdate(
                                ObjectValue(this.pc, newIsNull, false, newUpperTypeBound))

                        case Right(`thatUpperTypeBound`) if this.isNull == that.isNull ⇒
                            StructuralUpdate(other)

                        case Right(newUpperTypeBound) ⇒
                            val newIsNull = this.isNull & that.isNull
                            StructuralUpdate(
                                ObjectValue(this.pc, newIsNull, newUpperTypeBound))
                    }

                case that: ArrayValue ⇒
                    val thatUpperTypeBound = that.theUpperTypeBound
                    joinAnyArrayTypeWithObjectType(thisUpperTypeBound) match {

                        case Left(`thisUpperTypeBound`) if (
                            this.isNull == that.isNull && !this.isPrecise
                        ) ⇒
                            NoUpdate

                        case Left(newUpperTypeBound) ⇒
                            val newIsNull = this.isNull & that.isNull
                            StructuralUpdate(
                                ObjectValue(this.pc, newIsNull, false, newUpperTypeBound))

                        case Right(newUpperTypeBound) ⇒
                            val newIsNull = this.isNull & that.isNull
                            StructuralUpdate(
                                ObjectValue(this.pc, newIsNull, newUpperTypeBound))
                    }
            }
        }

        override def adapt(
            targetDomain: Domain,
            pc: PC): targetDomain.DomainValue = {
            targetDomain match {

                case thatDomain: l1.ReferenceValues ⇒
                    thatDomain.ObjectValue(pc, isNull, isPrecise, theUpperTypeBound).
                        asInstanceOf[targetDomain.DomainValue]

                case thatDomain: l0.DefaultTypeLevelReferenceValues ⇒
                    thatDomain.ReferenceValue(pc, theUpperTypeBound).
                        asInstanceOf[targetDomain.DomainValue]

                case _ ⇒ super.adapt(targetDomain, pc)
            }
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
                        (this.theUpperTypeBound eq that.theUpperTypeBound)
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
            if (isNull.isUnknown) description += "; isNull=maybe"
            if (!isPrecise) description += ", isUpperBound"
            description += ")"
            description
        }

    }

    protected class MObjectValue(
        val pc: PC,
        override val isNull: Answer,
        upperTypeBound: UIDSet[ObjectType])
            extends super.MObjectValue(upperTypeBound)
            with ObjectValue {
        this: DomainObjectValue ⇒

        /**
         * Copy constructor.
         */
        override def apply(pc: PC, isNull: Answer): DomainObjectValue = {
            ObjectValue(pc, isNull, upperTypeBound)
        }

        override def refineUpperTypeBound(
            pc: PC,
            supertype: ReferenceType): DomainReferenceValue = {
            // OPAL-AI calls this method only if a previous "subtype of" test 
            // (typeOf(this.value) <: additionalUpperBound ?) 
            // returned unknown. Hence, we only handle the case where the new bound
            // is more strict than the previous bound.

            var newUpperTypeBound: UIDSet[ObjectType] = UIDSet.empty
            upperTypeBound foreach { (anUpperTypeBound: ObjectType) ⇒
                // ATTENTION: "!..yes" is not the same as "no" (there is also unknown)
                if (!domain.isSubtypeOf(supertype, anUpperTypeBound).isYes)
                    newUpperTypeBound = newUpperTypeBound + anUpperTypeBound
            }
            if (newUpperTypeBound.size == 0)
                ReferenceValue(pc, isNull, false, supertype)
            else if (supertype.isObjectType)
                ObjectValue(pc, isNull, newUpperTypeBound + supertype.asObjectType)
            else
                throw ImpossibleRefinement(this, "incompatible bound "+supertype.toJava)
        }

        protected def doJoinWithNonNullValueWithSameOrigin(
            joinPC: PC,
            other: DomainSingleOriginReferenceValue): Update[DomainSingleOriginReferenceValue] = {
            val thisUpperTypeBound = this.upperTypeBound
            other match {
                case that: MObjectValue ⇒
                    val thatUpperTypeBound = that.upperTypeBound
                    joinUpperTypeBounds(thisUpperTypeBound, thatUpperTypeBound, true) match {
                        case Left(newUpperTypeBound) ⇒
                            val newIsNull = (this.isNull & that.isNull)
                            StructuralUpdate(
                                ObjectValue(this.pc, newIsNull, false, newUpperTypeBound))

                        case Right(`thisUpperTypeBound`) if this.isNull.isUnknown || this.isNull == that.isNull ⇒
                            NoUpdate

                        case Right(`thatUpperTypeBound`) if that.isNull.isUnknown || this.isNull == that.isNull ⇒
                            StructuralUpdate(other)

                        case Right(newUpperTypeBound) ⇒
                            val newIsNull = this.isNull & that.isNull
                            StructuralUpdate(
                                ObjectValue(this.pc, newIsNull, newUpperTypeBound))
                    }

                case that: SObjectValue ⇒
                    val thatUpperTypeBound = that.theUpperTypeBound
                    joinObjectTypes(thatUpperTypeBound, thisUpperTypeBound, true) match {
                        case Left(`thatUpperTypeBound`) if that.isNull.isUnknown || this.isNull == that.isNull ⇒
                            StructuralUpdate(other)

                        case Left(newUpperTypeBound) ⇒
                            // Though the upper type bound of this value may 
                            // also be an upper type bound for the other value
                            // it does not precisely capture the other value's type!
                            val newIsNull = this.isNull & that.isNull
                            StructuralUpdate(
                                ObjectValue(this.pc, newIsNull, false, newUpperTypeBound))

                        case Right(`thisUpperTypeBound`) if this.isNull.isUnknown || this.isNull == that.isNull ⇒
                            NoUpdate

                        case Right(newUpperTypeBound) ⇒
                            val newIsNull = this.isNull & that.isNull
                            StructuralUpdate(
                                ObjectValue(this.pc, newIsNull, newUpperTypeBound))
                    }

                case that: ArrayValue ⇒
                    joinAnyArrayTypeWithMultipleTypesBound(thisUpperTypeBound) match {

                        case Left(newUpperTypeBound) ⇒
                            val newIsNull = this.isNull & that.isNull
                            StructuralUpdate(
                                ObjectValue(this.pc, newIsNull, false, newUpperTypeBound))

                        case Right(`thisUpperTypeBound`) if this.isNull.isUnknown || this.isNull == that.isNull ⇒
                            NoUpdate

                        case Right(newUpperTypeBound) ⇒
                            val newIsNull = this.isNull & that.isNull
                            StructuralUpdate(
                                ObjectValue(this.pc, newIsNull, newUpperTypeBound))
                    }

            }
        }

        override def adapt(target: Domain, pc: PC): target.DomainValue =
            target match {
                case td: ReferenceValues ⇒
                    td.ObjectValue(pc, isNull, this.upperTypeBound).
                        asInstanceOf[target.DomainValue]

                case td: l0.DefaultTypeLevelReferenceValues ⇒
                    td.ObjectValue(pc, this.upperTypeBound).
                        asInstanceOf[target.DomainValue]

                case _ ⇒ super.adapt(target, pc)
            }

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
            if (isNull.isUnknown) description += "; isNull=maybe"
            description += "; isUpperBound)"
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
            val values = this.values.dropWhile(_.isNull.isYes)
            if (values.isEmpty)
                // <=> all values are null values!
                UIDSet.empty[ObjectType]
            else {
                var overallUTB = values.head.upperTypeBound

                def currentUTBisUTBForArrays: Boolean = {
                    overallUTB.containsOneElement &&
                        overallUTB.first.isArrayType
                }

                def asUTBForArrays: ArrayType =
                    overallUTB.first.asArrayType

                def asUTBForObjects: UIDSet[ObjectType] =
                    overallUTB.asInstanceOf[UIDSet[ObjectType]]

                values.tail foreach { value ⇒
                    val newUpperTypeBound: Either[ReferenceType, UIDSet[ReferenceType]] = value match {
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
                        case Left(referenceType)   ⇒ overallUTB = UIDSet(referenceType)
                        case Right(referenceTypes) ⇒ overallUTB = referenceTypes
                    }
                }
                overallUTB
            }
        }

        override def referenceValues: Iterator[IsAReferenceValue] = values.iterator

        /**
         * Returns `true` if the upper type bound of this value precisely captures the
         * runtime type of the value. This basically requires that all '''non-null''' values
         * are precise and have the same upper type bound. Null values are ignored.
         */
        override lazy val isPrecise: Boolean = calculateIsPrecise()

        private[this] def calculateIsPrecise(): Boolean = {
            val values = this.values.filterNot(_.isNull.isYes)
            if (values.nonEmpty) {
                val firstValue = values.head
                if (!firstValue.isPrecise)
                    return false
                val theUpperTypeBound = firstValue.upperTypeBound
                values.tail foreach { value ⇒
                    if (!value.isPrecise ||
                        theUpperTypeBound != value.upperTypeBound)
                        return false
                }
            }
            // <=> all values are null values or have the same bound
            true
        }

        override lazy val isNull: Answer = calculateIsNull()

        private[this] def calculateIsNull(): Answer = {
            val firstAnswer = values.head.isNull
            if (firstAnswer.isUnknown)
                return Unknown

            (firstAnswer /: values.tail) { (currentAnswer, n) ⇒
                val nextAnswer = n.isNull
                if (nextAnswer.isUnknown)
                    return Unknown

                currentAnswer & nextAnswer
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
                case UIDSet0 ⇒ NullValue(pc)
                case UIDSet1(referenceType) ⇒
                    ReferenceValue(pc, isNull, isPrecise, referenceType)
                case utb ⇒
                    ObjectValue(pc, isNull, utb.asInstanceOf[UIDSet[ObjectType]])
            }
        }

        override def adapt(target: Domain, pc: PC): target.DomainValue =
            if (target.isInstanceOf[l1.ReferenceValues]) {
                val thatDomain = target.asInstanceOf[l1.ReferenceValues]
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
            val values = this.values.dropWhile(_.isNull.isYes)
            var answer: Answer = values.head.isValueSubtypeOf(supertype)
            values.tail foreach { value ⇒
                if (answer == Unknown)
                    return Unknown

                if (!value.isNull.isYes) {
                    answer = answer & value.isValueSubtypeOf(supertype)
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
                    case Yes ⇒ this.values filter { _.isNull.isYesOrUnknown }
                    case No  ⇒ this.values filter { _.isNull.isNoOrUnknown }
                    case _   ⇒ throw DomainException("unsupported refinement")
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
                if (value.isNull.isYes)
                    newValues += value
                else {
                    val isSubtypeOf = value.isValueSubtypeOf(supertype)
                    if (isSubtypeOf.isYes)
                        newValues += value
                    else if (isSubtypeOf.isUnknown) {
                        try {
                            val newValue = value.refineUpperTypeBound(pc, supertype)
                            valueRefined = valueRefined || (newValue ne value)
                            newValues += newValue.asInstanceOf[DomainSingleOriginReferenceValue]
                        } catch {
                            case _: ImpossibleRefinement ⇒ /*let's filter this value*/
                            case t: Throwable            ⇒ throw t
                        }
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

        override protected def doJoin(joinPC: PC, other: DomainValue): Update[DomainValue] = {
            other match {

                case that: DomainSingleOriginReferenceValue ⇒
                    this.values foreach { thisValue ⇒
                        if (thisValue.pc == that.pc)
                            // Invariant:
                            // At most one value represented by MultipleReferenceValues
                            // has the same pc as this value.
                            thisValue.join(joinPC, that) match {
                                case NoUpdate ⇒
                                    // "thisValue" is more general than the other value
                                    return NoUpdate

                                case update @ SomeUpdate(newValue: DomainSingleOriginReferenceValue) ⇒
                                    return update.updateValue(
                                        MultipleReferenceValues(this.values - thisValue + newValue))

                                case _ ⇒
                                    throw DomainException("join of two values with the same origin resulted in a value with multiple origins")
                            }
                    }

                    StructuralUpdate(MultipleReferenceValues(this.values + that))

                case that: MultipleReferenceValues ⇒
                    var updateType: UpdateType = NoUpdateType
                    var otherValues = that.values
                    var newValues = scala.collection.mutable.HashSet.empty[DomainSingleOriginReferenceValue]
                    this.values foreach { thisValue ⇒
                        otherValues.find(thisValue.pc == _.pc) match {
                            case Some(otherValue) ⇒
                                otherValues -= otherValue
                                thisValue.join(joinPC, otherValue) match {
                                    case NoUpdate ⇒
                                        newValues += thisValue
                                    case update @ SomeUpdate(otherValue: DomainSingleOriginReferenceValue) ⇒
                                        updateType = updateType &: update
                                        newValues += otherValue
                                }
                            case None ⇒
                                newValues += thisValue
                        }
                    }

                    if (otherValues.nonEmpty) {
                        newValues ++= otherValues
                        updateType = StructuralUpdateType
                    }
                    updateType(MultipleReferenceValues(newValues))
            }
        }

        override def load(pc: PC, index: DomainValue): ArrayLoadResult = {
            (values map (asArrayAbstraction(_).load(pc, index))) reduce {
                (c1, c2) ⇒ mergeDEsComputations(pc, c1, c2)
            }
        }

        override def store(pc: PC, value: DomainValue, index: DomainValue): ArrayStoreResult = {
            (values map (asArrayAbstraction(_).store(pc, value, index))) reduce {
                (c1, c2) ⇒ mergeEsComputations(pc, c1, c2)
            }
        }

        override def length(pc: PC): Computation[DomainValue, ExceptionValue] = {
            val computations = values map (asArrayAbstraction(_).length(pc))
            computations reduce { (c1, c2) ⇒ mergeDEComputations(pc, c1, c2) }
        }

        override def hashCode(): Int = values.hashCode * 47

        override def equals(other: Any): Boolean = {
            other match {
                case that: MultipleReferenceValues ⇒ that.values == this.values
                case _                             ⇒ false
            }
        }

        override def toString() = values.mkString("OneOf(\t", ",\n\t", ")")
    }

    // -----------------------------------------------------------------------------------
    //
    // FACTORY METHODS
    //
    // -----------------------------------------------------------------------------------

    //
    // REFINEMENT OF EXISTING DOMAIN VALUE FACTORY METHODS
    //

    override def NonNullObjectValue(pc: PC, objectType: ObjectType): DomainObjectValue =
        ObjectValue(pc, No, false, objectType)

    override def NewObject(pc: PC, objectType: ObjectType): DomainObjectValue =
        ObjectValue(pc, No, true, objectType)

    override def InitializedObjectValue(pc: PC, objectType: ObjectType): DomainObjectValue =
        ObjectValue(pc, No, true, objectType)

    override def StringValue(pc: PC, value: String): DomainObjectValue =
        ObjectValue(pc, No, true, ObjectType.String)

    override def ClassValue(pc: PC, t: Type): DomainObjectValue =
        ObjectValue(pc, No, true, ObjectType.Class)

    override protected[domain] def ObjectValue(pc: PC, objectType: ObjectType): DomainObjectValue =
        ObjectValue(pc, Unknown, false, objectType)

    override protected[domain] def ObjectValue(pc: PC, upperTypeBound: UIDSet[ObjectType]): DomainObjectValue =
        ObjectValue(pc, Unknown, upperTypeBound)

    override def InitializedArrayValue(pc: PC, counts: List[Int], arrayType: ArrayType): DomainValue =
        ArrayValue(pc, No, true, arrayType)

    override def NewArray(pc: PC, count: DomainValue, arrayType: ArrayType): DomainArrayValue =
        ArrayValue(pc, No, true, arrayType)

    override def NewArray(pc: PC, counts: List[DomainValue], arrayType: ArrayType): DomainArrayValue =
        ArrayValue(pc, No, true, arrayType)

    override protected[domain] def ArrayValue(pc: PC, arrayType: ArrayType): DomainArrayValue =
        ArrayValue(pc, Unknown, false, arrayType)
    //
    // DECLARATION OF ADDITIONAL DOMAIN VALUE FACTORY METHODS
    //

    protected[domain] def ReferenceValue( // for SObjectValue
        pc: PC,
        isNull: Answer,
        isPrecise: Boolean,
        theUpperTypeBound: ReferenceType): DomainReferenceValue = {
        theUpperTypeBound match {
            case ot: ObjectType ⇒
                ObjectValue(pc, isNull, isPrecise, ot)
            case at: ArrayType ⇒
                ArrayValue(pc, isNull, isPrecise, at)
        }
    }

    protected[domain] def ObjectValue( // for SObjectValue
        pc: PC,
        isNull: Answer,
        isPrecise: Boolean,
        theUpperTypeBound: ObjectType): DomainObjectValue

    protected[domain] def ObjectValue( // for MObjectValue
        pc: PC,
        isNull: Answer,
        upperTypeBound: UIDSet[ObjectType]): DomainObjectValue

    protected[domain] def ArrayValue( // for ArrayValue
        pc: PC,
        isNull: Answer,
        isPrecise: Boolean,
        theUpperTypeBound: ArrayType): DomainArrayValue

    protected[domain] def MultipleReferenceValues(
        values: scala.collection.Set[DomainSingleOriginReferenceValue]): DomainMultipleReferenceValues

}
