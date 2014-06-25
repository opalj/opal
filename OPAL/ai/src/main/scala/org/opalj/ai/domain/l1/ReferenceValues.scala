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
    domain: Configuration with ClassHierarchy ⇒

    type DomainSingleOriginReferenceValue <: SingleOriginReferenceValue with DomainReferenceValue
    type DomainNullValue <: NullValue with DomainSingleOriginReferenceValue
    type DomainObjectValue <: ObjectValue with DomainSingleOriginReferenceValue
    type DomainArrayValue <: ArrayValue with DomainSingleOriginReferenceValue

    type DomainMultipleReferenceValues <: MultipleReferenceValues with DomainReferenceValue

    implicit object DomainSingleOriginReferenceValueOrdering
            extends Ordering[DomainSingleOriginReferenceValue] {

        def compare(x: DomainSingleOriginReferenceValue, y: DomainSingleOriginReferenceValue): Int = {
            x.origin - y.origin
        }
    }

    trait ReferenceValue extends super.ReferenceValue {
        this: DomainReferenceValue ⇒

        def refineIsNullIf(hasPC: PC)(isNull: Answer): DomainReferenceValue

        protected[this] final def propagateRefineIsNull(
            origin: ValueOrigin,
            isNull: Answer,
            operands: Operands,
            locals: Locals): (Operands, Locals) = {
            (
                if (operands.nonEmpty) {
                    var opsUpdated = false
                    var newOps: Operands = Nil
                    val opIt = operands.iterator
                    while (opIt.hasNext) {
                        opIt.next match {
                            case rv: ReferenceValue ⇒
                                val newRV = rv.refineIsNullIf(origin)(isNull)
                                opsUpdated = opsUpdated || (rv ne newRV)
                                newOps = newRV :: newOps
                            case v ⇒
                                newOps = v :: newOps
                        }
                    }
                    if (opsUpdated) newOps.reverse else operands
                } else {
                    operands
                },
                locals.transform { l ⇒
                    l match {
                        case referenceValue: ReferenceValue ⇒
                            referenceValue.refineIsNullIf(origin)(isNull)
                        case other ⇒
                            other
                    }
                }
            )
        }
    }

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
            origin: ValueOrigin = this.origin,
            isNull: Answer = this.isNull): DomainSingleOriginReferenceValue

        def refineIsNullIf(hasPC: PC)(isNull: Answer): DomainSingleOriginReferenceValue

        /*ABSTRACT*/ protected def doJoinWithNonNullValueWithSameOrigin(
            joinPC: PC,
            that: DomainSingleOriginReferenceValue): Update[DomainSingleOriginReferenceValue]

        protected def doJoinWithMultipleReferenceValues(
            joinPC: PC,
            other: DomainMultipleReferenceValues): Update[DomainReferenceValue] = {

            // Invariant:
            // At most one value represented by MultipleReferenceValues
            // has the same pc as this value.
            other.values find { that ⇒ this.origin == that.origin } match {
                case None ⇒
                    StructuralUpdate(MultipleReferenceValues(other.values + this))
                case Some(that) ⇒
                    this.join(joinPC, that) match {
                        case NoUpdate ⇒
                            // This value is more general than the value
                            // in MultipleReferenceValues.
                            val newValues = other.values - that + this
                            StructuralUpdate(MultipleReferenceValues(newValues))
                        case SomeUpdate(newValue) ⇒
                            if (newValue eq that)
                                StructuralUpdate(other)
                            else {
                                StructuralUpdate(
                                    MultipleReferenceValues(
                                        other.values - that +
                                            newValue.asInstanceOf[DomainSingleOriginReferenceValue]
                                    ))
                            }
                    }
            }
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
                        if (this.origin == that.origin)
                            that match {
                                case that: DomainNullValue ⇒
                                    doJoinWithNullValueWithSameOrigin(joinPC, that)
                                case _ ⇒
                                    doJoinWithNonNullValueWithSameOrigin(joinPC, that)
                            }
                        else
                            StructuralUpdate(MultipleReferenceValues(
                                SortedSet[DomainSingleOriginReferenceValue](this, that)
                            ))
                    case that: DomainMultipleReferenceValues ⇒
                        doJoinWithMultipleReferenceValues(joinPC, that)
                }
            }
        }
    }

    protected class NullValue(
        override val origin: ValueOrigin)
            extends super.NullValue with SingleOriginReferenceValue {
        this: DomainNullValue ⇒

        /**
         * Copy constructor.
         */
        override def apply(
            origin: ValueOrigin = this.origin,
            isNull: Answer = Yes): DomainNullValue = {

            NullValue(origin)
        }

        def refineIsNullIf(hasPC: PC)(isNull: Answer): DomainSingleOriginReferenceValue =
            // there is nothing to refine in this case since this value has definite properties
            this

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
                case that: NullValue ⇒ that.origin == this.origin && (that canEqual this)
                case _               ⇒ false
            }
        }

        def canEqual(other: NullValue): Boolean = true

        override def hashCode: Int = origin

        override def toString() = "null(origin="+origin+")"
    }

    trait NonNullSingleOriginReferenceValue extends SingleOriginReferenceValue {
        this: DomainSingleOriginReferenceValue ⇒

        final override def refineIsNull(
            pc: PC,
            isNull: Answer,
            operands: Operands,
            locals: Locals): (Operands, Locals) =
            propagateRefineIsNull(this.origin, isNull, operands, locals)

        def refineIsNullIf(hasOrigin: ValueOrigin)(isNull: Answer): DomainSingleOriginReferenceValue = {
            if (origin != hasOrigin)
                return this

            if (this.isNull == isNull)
                this
            else if (isNull.isYes) {
                NullValue(this.origin)
            } else if (isNull.isNo) {
                this(isNull = No)
            } else
                throw ImpossibleRefinement(this, "\"refining\" null property to Unknown")
        }
    }

    protected class ArrayValue(
        override val origin: ValueOrigin,
        override val isNull: Answer,
        override val isPrecise: Boolean,
        theUpperTypeBound: ArrayType)
            extends super.ArrayValue(theUpperTypeBound)
            with NonNullSingleOriginReferenceValue {
        this: DomainArrayValue ⇒

        require(this.isNull.isNoOrUnknown)

        override def apply(vo: ValueOrigin, isNull: Answer): DomainArrayValue = {
            ArrayValue(vo, isNull, isPrecise, theUpperTypeBound)
        }

        override def doJoinWithNonNullValueWithSameOrigin(
            joinPC: PC,
            other: DomainSingleOriginReferenceValue): Update[DomainSingleOriginReferenceValue] = {

            other match {
                case that: ArrayValue ⇒
                    val thisUpperTypeBound = this.theUpperTypeBound
                    val thatUpperTypeBound = that.theUpperTypeBound
                    classHierarchy.joinArrayTypes(thisUpperTypeBound, thatUpperTypeBound) match {

                        case Left(`thisUpperTypeBound`) if (
                            ((this.isNull == that.isNull) || this.isNull.isUnknown) &&
                            (!this.isPrecise ||
                                ((thisUpperTypeBound eq thatUpperTypeBound) &&
                                    that.isPrecise))
                        ) ⇒
                            NoUpdate

                        case Left(`thatUpperTypeBound`) if (
                            ((this.isNull == that.isNull) || that.isNull.isUnknown) &&
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
                                    this.origin, newIsNull, newIsPrecise, newUpperTypeBound))

                        case Right(newUpperTypeBound) ⇒
                            val newIsNull = this.isNull & that.isNull
                            StructuralUpdate(
                                ObjectValue(this.origin, newIsNull, newUpperTypeBound))
                    }

                case that: SObjectValue ⇒
                    val thatUpperTypeBound = that.theUpperTypeBound
                    classHierarchy.joinAnyArrayTypeWithObjectType(thatUpperTypeBound) match {

                        case UIDSet1(`thatUpperTypeBound`) if (
                            this.isNull == that.isNull && !that.isPrecise
                        ) ⇒
                            StructuralUpdate(other)

                        case UIDSet1(newUpperTypeBound) ⇒
                            val newIsNull = this.isNull & that.isNull
                            StructuralUpdate(
                                ObjectValue(this.origin, newIsNull, false, newUpperTypeBound))

                        case newUpperTypeBound ⇒
                            val newIsNull = this.isNull & that.isNull
                            StructuralUpdate(
                                ObjectValue(this.origin, newIsNull, newUpperTypeBound))
                    }

                case that: MObjectValue ⇒
                    val thatUpperTypeBound = that.upperTypeBound
                    classHierarchy.joinAnyArrayTypeWithMultipleTypesBound(thatUpperTypeBound) match {
                        case `thatUpperTypeBound` if this.isNull == that.isNull ⇒
                            StructuralUpdate(other)

                        case UIDSet1(newUpperTypeBound) ⇒
                            val newIsNull = this.isNull & that.isNull
                            StructuralUpdate(
                                ObjectValue(this.origin, newIsNull, false, newUpperTypeBound))

                        case newUpperTypeBound ⇒
                            val newIsNull = this.isNull & that.isNull
                            StructuralUpdate(
                                ObjectValue(this.origin, newIsNull, newUpperTypeBound))
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
                        this.origin == that.origin &&
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
            (((origin) * 41 +
                (if (isPrecise) 101 else 3)) * 13 +
                isNull.hashCode()) * 79 +
                upperTypeBound.hashCode()

        override def toString() = {
            var description = theUpperTypeBound.toJava+"(origin="+origin
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
        override val origin: ValueOrigin,
        override val isNull: Answer,
        override val isPrecise: Boolean,
        theUpperTypeBound: ObjectType)
            extends super.SObjectValue(theUpperTypeBound)
            with ObjectValue {
        this: DomainObjectValue ⇒

        require(isPrecise == false || (theUpperTypeBound ne ObjectType("java/nio/charset/CharsetEncoder")))
        require(this.isNull.isNoOrUnknown)

        override def apply(vo: ValueOrigin, isNull: Answer): DomainSingleOriginReferenceValue = {
            ObjectValue(vo, isNull, isPrecise, theUpperTypeBound)
        }

        override def refineUpperTypeBound(
            pc: PC,
            supertype: ReferenceType,
            operands: Operands,
            locals: Locals): (DomainReferenceValue, (Operands, Locals)) = {
            if (isPrecise)
                // Actually, it doesn't make sense to allow calls to this method if
                // the type is precise. However, we have to handle this case 
                // gracefully since it is possible that the value represented by
                // this "SObjectValue" is just one of many instances represented 
                // by a domain value on the stack/in a register 
                // and in this case it may make sense to establish a more stringent
                // bound for the others.
                return (this, (operands, locals))
            if (supertype eq theUpperTypeBound)
                return (this, (operands, locals))

            if (domain.isSubtypeOf(supertype, theUpperTypeBound).isYes) {
                // this also handles the case where we cast an Object to an array
                val newValue = ReferenceValue(this.origin, isNull, false, supertype)
                (newValue, updateMemoryLayout(this, newValue, operands, locals))
            } else if (domain.isSubtypeOf(theUpperTypeBound, supertype).isYes) {
                // useless refinement...
                (this, (operands, locals))
            } else {
                if (supertype.isArrayType)
                    throw ImpossibleRefinement(this, "incompatible bound "+supertype.toJava)

                // basically, we are adding another type bound
                val newValue = ObjectValue(
                    this.origin,
                    this.isNull,
                    UIDSet(supertype.asObjectType, theUpperTypeBound))
                (newValue, updateMemoryLayout(this, newValue, operands, locals))
            }
        }

        override protected def doJoinWithNonNullValueWithSameOrigin(
            joinPC: Int,
            other: DomainSingleOriginReferenceValue): Update[DomainSingleOriginReferenceValue] = {
            val thisUpperTypeBound = this.theUpperTypeBound
            other match {
                case that: SObjectValue ⇒
                    val thatUpperTypeBound = that.theUpperTypeBound
                    classHierarchy.joinObjectTypes(thisUpperTypeBound, thatUpperTypeBound, true) match {
                        case UIDSet1(`thisUpperTypeBound`) if (
                            (this.isNull == that.isNull || this.isNull.isUnknown) &&
                            (!this.isPrecise ||
                                ((thisUpperTypeBound eq thatUpperTypeBound) && that.isPrecise))
                        ) ⇒
                            NoUpdate

                        case UIDSet1(`thatUpperTypeBound`) if (
                            (this.isNull == that.isNull || that.isNull.isUnknown) &&
                            !that.isPrecise
                        ) ⇒
                            StructuralUpdate(other)

                        case UIDSet1(newUpperTypeBound) ⇒
                            // Though the upper type bound of this value may 
                            // also be an upper type bound for the other value
                            // it does not precisely capture the other value's type!
                            val newIsNull = this.isNull & that.isNull
                            StructuralUpdate(
                                ObjectValue(this.origin, newIsNull, false, newUpperTypeBound))

                        case newUpperTypeBound ⇒
                            val newIsNull = this.isNull & that.isNull
                            StructuralUpdate(
                                ObjectValue(this.origin, newIsNull, newUpperTypeBound))
                    }

                case that: MObjectValue ⇒
                    val thatUpperTypeBound = that.upperTypeBound

                    classHierarchy.joinObjectTypes(thisUpperTypeBound, thatUpperTypeBound, true) match {
                        case UIDSet1(`thisUpperTypeBound`) if (
                            this.isNull == that.isNull && !this.isPrecise
                        ) ⇒
                            NoUpdate

                        case UIDSet1(newUpperTypeBound) ⇒
                            val newIsNull = this.isNull & that.isNull
                            StructuralUpdate(
                                ObjectValue(this.origin, newIsNull, false, newUpperTypeBound))

                        case `thatUpperTypeBound` if this.isNull == that.isNull ⇒
                            StructuralUpdate(other)

                        case newUpperTypeBound ⇒
                            val newIsNull = this.isNull & that.isNull
                            StructuralUpdate(
                                ObjectValue(this.origin, newIsNull, newUpperTypeBound))
                    }

                case that: ArrayValue ⇒
                    val thatUpperTypeBound = that.theUpperTypeBound
                    classHierarchy.joinAnyArrayTypeWithObjectType(thisUpperTypeBound) match {

                        case UIDSet1(`thisUpperTypeBound`) if (
                            this.isNull == that.isNull && !this.isPrecise
                        ) ⇒
                            NoUpdate

                        case UIDSet1(newUpperTypeBound) ⇒
                            val newIsNull = this.isNull & that.isNull
                            StructuralUpdate(
                                ObjectValue(this.origin, newIsNull, false, newUpperTypeBound))

                        case newUpperTypeBound ⇒
                            val newIsNull = this.isNull & that.isNull
                            StructuralUpdate(
                                ObjectValue(this.origin, newIsNull, newUpperTypeBound))
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
                        this.origin == that.origin &&
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
                origin

        override def toString() = {
            var description = theUpperTypeBound.toJava+"(origin="+origin
            if (isNull.isUnknown) description += ";maybeNull"
            if (!isPrecise) description += ";isUpperBound"
            description += ")"
            description
        }

    }

    protected class MObjectValue(
        override val origin: ValueOrigin,
        override val isNull: Answer,
        upperTypeBound: UIDSet[ObjectType])
            extends super.MObjectValue(upperTypeBound)
            with ObjectValue {
        this: DomainObjectValue ⇒

        /**
         * Copy constructor.
         */
        override def apply(vo: ValueOrigin, isNull: Answer): DomainObjectValue = {
            ObjectValue(vo, isNull, upperTypeBound)
        }

        override def refineUpperTypeBound(
            pc: PC,
            supertype: ReferenceType,
            operands: Operands,
            locals: Locals): (DomainSingleOriginReferenceValue, (Operands, Locals)) = {
            // OPAL calls this method only if a previous "subtype of" test 
            // (typeOf(this.value) <: additionalUpperBound ?) 
            // returned unknown. Hence, we only handle the case where the new bound
            // is more strict than the previous bound.

            var newUpperTypeBound: UIDSet[ObjectType] = UIDSet.empty
            upperTypeBound foreach { (anUpperTypeBound: ObjectType) ⇒
                // ATTENTION: "!..yes" is not the same as "no" (there is also unknown)
                if (!domain.isSubtypeOf(supertype, anUpperTypeBound).isYes)
                    newUpperTypeBound = newUpperTypeBound + anUpperTypeBound
            }
            if (newUpperTypeBound.size == 0) {
                val newValue = ReferenceValue(pc, isNull, false, supertype)
                (newValue, updateMemoryLayout(this, newValue, operands, locals))
            } else if (supertype.isObjectType) {
                val newValue = ObjectValue(pc, isNull, newUpperTypeBound + supertype.asObjectType)
                (newValue, updateMemoryLayout(this, newValue, operands, locals))
            } else
                throw ImpossibleRefinement(this, "incompatible bound "+supertype.toJava)
        }

        protected def doJoinWithNonNullValueWithSameOrigin(
            joinPC: PC,
            other: DomainSingleOriginReferenceValue): Update[DomainSingleOriginReferenceValue] = {
            val thisUpperTypeBound = this.upperTypeBound
            other match {
                case that: MObjectValue ⇒
                    val thatUpperTypeBound = that.upperTypeBound
                    classHierarchy.joinUpperTypeBounds(thisUpperTypeBound, thatUpperTypeBound, true) match {
                        case UIDSet1(newUpperTypeBound) ⇒
                            val newIsNull = (this.isNull & that.isNull)
                            StructuralUpdate(
                                ObjectValue(this.origin, newIsNull, false, newUpperTypeBound))

                        case `thisUpperTypeBound` if this.isNull.isUnknown || this.isNull == that.isNull ⇒
                            NoUpdate

                        case `thatUpperTypeBound` if that.isNull.isUnknown || this.isNull == that.isNull ⇒
                            StructuralUpdate(other)

                        case newUpperTypeBound ⇒
                            val newIsNull = this.isNull & that.isNull
                            StructuralUpdate(
                                ObjectValue(this.origin, newIsNull, newUpperTypeBound))
                    }

                case that: SObjectValue ⇒
                    val thatUpperTypeBound = that.theUpperTypeBound
                    classHierarchy.joinObjectTypes(thatUpperTypeBound, thisUpperTypeBound, true) match {
                        case UIDSet1(`thatUpperTypeBound`) if that.isNull.isUnknown || this.isNull == that.isNull ⇒
                            StructuralUpdate(other)

                        case UIDSet1(newUpperTypeBound) ⇒
                            // Though the upper type bound of this value may 
                            // also be an upper type bound for the other value
                            // it does not precisely capture the other value's type!
                            val newIsNull = this.isNull & that.isNull
                            StructuralUpdate(
                                ObjectValue(this.origin, newIsNull, false, newUpperTypeBound))

                        case `thisUpperTypeBound` if this.isNull.isUnknown || this.isNull == that.isNull ⇒
                            NoUpdate

                        case newUpperTypeBound ⇒
                            val newIsNull = this.isNull & that.isNull
                            StructuralUpdate(
                                ObjectValue(this.origin, newIsNull, newUpperTypeBound))
                    }

                case that: ArrayValue ⇒
                    classHierarchy.joinAnyArrayTypeWithMultipleTypesBound(thisUpperTypeBound) match {

                        case UIDSet1(newUpperTypeBound) ⇒
                            val newIsNull = this.isNull & that.isNull
                            StructuralUpdate(
                                ObjectValue(this.origin, newIsNull, false, newUpperTypeBound))

                        case `thisUpperTypeBound` if this.isNull.isUnknown || this.isNull == that.isNull ⇒
                            NoUpdate

                        case newUpperTypeBound ⇒
                            val newIsNull = this.isNull & that.isNull
                            StructuralUpdate(
                                ObjectValue(this.origin, newIsNull, newUpperTypeBound))
                    }
            }
        }

        override def adapt(target: Domain, origin: ValueOrigin): target.DomainValue =
            target match {
                case td: ReferenceValues ⇒
                    td.ObjectValue(origin, isNull, this.upperTypeBound).
                        asInstanceOf[target.DomainValue]

                case td: l0.DefaultTypeLevelReferenceValues ⇒
                    td.ObjectValue(origin, this.upperTypeBound).
                        asInstanceOf[target.DomainValue]

                case _ ⇒ super.adapt(target, origin)
            }

        override def equals(other: Any): Boolean = {
            other match {
                case that: MObjectValue ⇒
                    (this eq that) || (
                        this.origin == that.origin &&
                        this.isNull == that.isNull &&
                        (this canEqual that) &&
                        (this.upperTypeBound == that.upperTypeBound))
                case _ ⇒ false
            }
        }

        protected def canEqual(other: MObjectValue): Boolean = true

        override lazy val hashCode: Int =
            (((upperTypeBound.hashCode()) * 41 +
                (if (isPrecise) 11 else 101)) * 13 +
                isNull.hashCode()) * 79 +
                origin

        override def toString() = {
            var description = upperTypeBound.map(_.toJava).mkString(" with ")+"(origin="+origin
            if (isNull.isUnknown) description += "; isNull=maybe"
            description += "; isUpperBound)"
            description
        }
    }

    /**
     * A `MultipleReferenceValues` tracks multiple reference values (of type `NullValue`,
     * `ArrayValue`, `SObjectValue` and `MObjectValue`) that have different
     * origins. I.e., per value origin one domain value is used
     * to abstract over the properties of that respective value.
     */
    protected class MultipleReferenceValues(
        val values: scala.collection.SortedSet[DomainSingleOriginReferenceValue])
            extends ReferenceValue
            with MultipleOriginsValue {
        this: DomainMultipleReferenceValues ⇒

        require(values.size > 1)

        override def origins: Iterable[ValueOrigin] = values.view.map(_.origin)

        /**
         * Calculates the most specific common supertype of all upper type bounds of
         * all values.
         */
        override lazy val upperTypeBound: UpperTypeBound = {
            val values = this.values.view.filterNot(_.isNull.isYes)
            if (values.isEmpty)
                // <=> all values are null values!
                UIDSet.empty[ObjectType]
            else {
                var overallUTB = values.head.upperTypeBound

                def currentUTBisUTBForArrays: Boolean =
                    overallUTB.containsOneElement && overallUTB.first.isArrayType

                def asUTBForArrays: ArrayType =
                    overallUTB.first.asArrayType

                def asUTBForObjects: UIDSet[ObjectType] =
                    overallUTB.asInstanceOf[UIDSet[ObjectType]]

                values.tail foreach { value ⇒
                    overallUTB = value match {
                        case SObjectValue(upperTypeBound) ⇒
                            if (currentUTBisUTBForArrays)
                                classHierarchy.joinAnyArrayTypeWithObjectType(upperTypeBound)
                            else
                                classHierarchy.joinObjectTypes(upperTypeBound, asUTBForObjects, true)
                        case MObjectValue(upperTypeBound) ⇒
                            if (currentUTBisUTBForArrays)
                                classHierarchy.joinAnyArrayTypeWithMultipleTypesBound(upperTypeBound)
                            else
                                classHierarchy.joinUpperTypeBounds(asUTBForObjects, upperTypeBound, true)

                        case ArrayValue(upperTypeBound) ⇒
                            if (currentUTBisUTBForArrays)
                                classHierarchy.joinArrayTypes(asUTBForArrays, upperTypeBound) match {
                                    case Left(arrayType)       ⇒ UIDSet(arrayType)
                                    case Right(upperTypeBound) ⇒ upperTypeBound
                                }
                            else
                                classHierarchy.joinAnyArrayTypeWithMultipleTypesBound(asUTBForObjects)

                        case _: NullValue ⇒ /*"Do Nothing"*/ overallUTB
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
            val vIt = values.iterator
            val firstV = vIt.next
            var isPrecise: Boolean = firstV.isPrecise
            var theUpperTypeBound: UpperTypeBound = firstV.upperTypeBound
            while (isPrecise && vIt.hasNext) {
                val v = vIt.next
                if (v.isPrecise) {
                    val upperTypeBound = v.upperTypeBound
                    isPrecise = theUpperTypeBound == upperTypeBound
                    theUpperTypeBound = upperTypeBound
                } else {
                    isPrecise = false
                }
            }
            isPrecise
        }

        override lazy val isNull: Answer = calculateIsNull()

        private[this] def calculateIsNull(): Answer = {
            val vIt = values.iterator
            var isNull: Answer = vIt.next.isNull
            while (isNull.isYesOrNo && vIt.hasNext) {
                isNull = isNull & vIt.next.isNull
            }
            isNull
        }

        /**
         * Summarizes this value by creating a new domain value that abstracts over
         * the properties of all values.
         *
         * The given `pc` is used as the program counter of the newly created value.
         */
        override def summarize(pc: PC): DomainReferenceValue = {
            upperTypeBound /*<= basically creates the summary*/ match {
                case UIDSet0 ⇒ NullValue(pc)
                case UIDSet1(referenceType) ⇒
                    ReferenceValue(pc, isNull, isPrecise, referenceType)
                case utb ⇒
                    ObjectValue(pc, isNull, utb.asInstanceOf[UIDSet[ObjectType]])
            }
        }

        override def adapt(target: Domain, pc: PC): target.DomainValue = {
            // All values are mapped to one PC and – because a MultipleReference
            // Value only supports one Value per PC – hence, this is equivalent
            // to calculating a summary!
            target.summarize(
                pc, this.values.view.toList.map { value ⇒ value.adapt(target, pc) }
            )
        }

        override def isValueSubtypeOf(supertype: ReferenceType): Answer = {
            // Recall that the client has to make an "isNull" check before calling
            // isValueSubtypeOf. Hence, at least one of the possible reference values 
            // has to be non null.
            val values = this.values.filterNot(_.isNull.isYes)
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

        override def refineIsNull(
            pc: PC,
            isNull: Answer,
            operands: Operands,
            locals: Locals): (Operands, Locals) = {
            values.foldLeft((operands, locals)) { (memoryLayout, nextValue) ⇒
                val (operands, locals) = memoryLayout
                propagateRefineIsNull(nextValue.origin, isNull, operands, locals)
            }
        }

        override def refineIsNullIf(
            hasOrigin: ValueOrigin)(
                isNull: Answer): DomainReferenceValue = {
            // Recall that this value's property – as a whole – can be undefined also 
            // each value's property is well defined (Yes, No)
            // Furthermore, isNull is either Yes or No and we are going to ignore
            // those updates that are meaningless.

            var valuesToKeep = SortedSet.empty[DomainSingleOriginReferenceValue]
            var valuesToRefine = SortedSet.empty[DomainSingleOriginReferenceValue]
            isNull match {
                case Yes ⇒ this.values foreach { v ⇒
                    val isNull = v.isNull
                    if (v.origin != hasOrigin || isNull.isYes) valuesToKeep += v
                    else if (isNull.isUnknown) valuesToRefine += v
                }
                case No ⇒ this.values foreach { v ⇒
                    val isNull = v.isNull
                    if (v.origin != hasOrigin || isNull.isNo) valuesToKeep += v
                    else if (isNull.isUnknown) valuesToRefine += v
                }
                case _ ⇒ throw DomainException("unsupported refinement")
            }

            var valueRefined = false
            val refinedValues: SortedSet[DomainSingleOriginReferenceValue] =
                (valuesToRefine map { value ⇒
                    val refinedValue = value.refineIsNullIf(hasOrigin)(isNull)
                    if (refinedValue ne value) {
                        valueRefined = true
                    }
                    refinedValue
                }) ++ valuesToKeep
            if (refinedValues.size == 1) {
                refinedValues.head
            } else if (valueRefined || this.values.size != refinedValues.size) {
                MultipleReferenceValues(refinedValues)
            } else {
                this
            }
        }

        override def refineUpperTypeBound(
            pc: PC,
            supertype: ReferenceType,
            operands: Operands,
            locals: Locals): (DomainValue, (Operands, Locals)) = {
            var updatedOperands: Operands = operands
            var updatedLocals: Locals = locals
            var newValues = SortedSet.empty[DomainSingleOriginReferenceValue]
            var valueRefined = false
            this.values foreach { value ⇒
                if (value.isNull.isYes)
                    newValues = newValues + value
                else {
                    val isSubtypeOf = value.isValueSubtypeOf(supertype)
                    if (isSubtypeOf.isYes)
                        newValues += value
                    else if (isSubtypeOf.isUnknown) {
                        val (newValue, (newOperands, newLocals)) =
                            value.refineUpperTypeBound(
                                pc, supertype,
                                updatedOperands, updatedLocals)
                        if ((newOperands ne updatedOperands) || (newLocals ne updatedLocals)) {
                            updatedOperands = newOperands
                            updatedLocals = newLocals
                        }
                        valueRefined = valueRefined || (value ne newValue)
                        newValues += newValue.asInstanceOf[DomainSingleOriginReferenceValue]
                    }
                    // if isSubtypeOf.no then we can just remove it
                }
            }

            if (newValues.size == 1) {
                val newValue = newValues.head
                (newValue, updateMemoryLayout(this, newValue, operands, locals))
                // The following test should not be necessary, as this method is
                // only intended to be called, if this value as a whole needs
                // refinement:
            } else if (valueRefined || this.values.size != newValues.size) {
                val newValue = MultipleReferenceValues(newValues)
                (newValue, updateMemoryLayout(this, newValue, operands, locals))
            } else
                (this, (operands, locals))
        }

        override protected def doJoin(joinPC: PC, other: DomainValue): Update[DomainValue] = {
            other match {

                case that: DomainSingleOriginReferenceValue ⇒
                    this.values foreach { thisValue ⇒
                        if (thisValue.origin == that.origin)
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
                    var newValues = SortedSet.empty[DomainSingleOriginReferenceValue]
                    this.values foreach { thisValue ⇒
                        otherValues.find(thisValue.origin == _.origin) match {
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

        override lazy val hashCode: Int = values.hashCode * 47

        override def equals(other: Any): Boolean = {
            other match {
                case that: MultipleReferenceValues ⇒ that.values == this.values
                case _                             ⇒ false
            }
        }

        override def toString() = {
            var s = values.mkString("OneOf(", ", ", ")")
            s += upperTypeBound.map(_.toJava).mkString(";lutb=", " with ", "")
            if (!isPrecise) s += ";isUpperBound"
            s += ";isNull="+isNull
            s
        }
    }

    object MultipleReferenceValues {
        def unapply(value: MultipleReferenceValues): Some[SortedSet[DomainSingleOriginReferenceValue]] = {
            Some(value.values)
        }
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

    override def ObjectValue(pc: PC, objectType: ObjectType): DomainObjectValue =
        ObjectValue(pc, Unknown, false, objectType)

    override def ObjectValue(pc: PC, upperTypeBound: UIDSet[ObjectType]): DomainObjectValue =
        ObjectValue(pc, Unknown, upperTypeBound)

    override def InitializedArrayValue(pc: PC, counts: List[Int], arrayType: ArrayType): DomainArrayValue =
        ArrayValue(pc, No, true, arrayType)

    override def NewArray(pc: PC, count: DomainValue, arrayType: ArrayType): DomainArrayValue =
        ArrayValue(pc, No, true, arrayType)

    override def NewArray(pc: PC, counts: List[DomainValue], arrayType: ArrayType): DomainArrayValue =
        ArrayValue(pc, No, true, arrayType)

    override protected[domain] def ArrayValue(pc: PC, arrayType: ArrayType): DomainArrayValue = {
        if (arrayType.elementType.isBaseType)
            ArrayValue(pc, Unknown, true, arrayType)
        else
            ArrayValue(pc, Unknown, false, arrayType)
    }

    //
    // DECLARATION OF ADDITIONAL DOMAIN VALUE FACTORY METHODS
    //

    protected[domain] def ReferenceValue( // for SObjectValue
        pc: PC,
        isNull: Answer,
        isPrecise: Boolean,
        theUpperTypeBound: ReferenceType): DomainSingleOriginReferenceValue = {
        theUpperTypeBound match {
            case ot: ObjectType ⇒
                ObjectValue(pc, isNull, isPrecise, ot)
            case at: ArrayType ⇒
                ArrayValue(pc, isNull, isPrecise || at.elementType.isBaseType, at)
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
        values: SortedSet[DomainSingleOriginReferenceValue]): DomainMultipleReferenceValues

}
