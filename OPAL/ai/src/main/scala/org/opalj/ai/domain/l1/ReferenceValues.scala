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

import org.opalj.br.{ Type, ReferenceType, ObjectType, ArrayType, UpperTypeBound }

/**
 * This partial domain enables a tracking of a reference value's null-ness and origin
 * properties.
 *
 * @author Michael Eichberg
 */
trait ReferenceValues extends l0.DefaultTypeLevelReferenceValues with Origin {
    domain: IntegerValuesDomain with TypedValuesFactory with Configuration with ClassHierarchy ⇒

    type DomainReferenceValue <: ReferenceValue
    type DomainSingleOriginReferenceValue <: SingleOriginReferenceValue with DomainReferenceValue
    type DomainNullValue <: NullValue with DomainSingleOriginReferenceValue
    type DomainObjectValue <: ObjectValue with DomainSingleOriginReferenceValue
    type DomainArrayValue <: ArrayValue with DomainSingleOriginReferenceValue

    type DomainMultipleReferenceValues <: MultipleReferenceValues with DomainReferenceValue

    implicit object DomainSingleOriginReferenceValueOrdering
            extends Ordering[DomainSingleOriginReferenceValue] {

        def compare(
            x: DomainSingleOriginReferenceValue,
            y: DomainSingleOriginReferenceValue): Int = {
            x.origin - y.origin
        }
    }

    /**
     * Common supertrait of all [[DomainValue]]s that represent reference values. This
     * trait defines the additional methods needed for the refinement of the new
     * properties.
     */
    trait ReferenceValue extends super.ReferenceValue { this: DomainReferenceValue ⇒

        /**
         * Refines this value's `isNull` property, if meaningful.
         *
         * @param pc The program counter of the instruction that was the reason
         *      for the refinement.
         * @param isNull This value's new null-ness property. `isNull` either
         *      has to be `Yes` or `No`. The refinement to `Unknown` neither makes
         *      sense nor is it supported.
         * @return The operand stack and register values if
         *      there was something to refine.
         */
        def refineIsNull(
            pc: PC,
            isNull: Answer,
            operands: Operands,
            locals: Locals): (Operands, Locals)

        /**
         * Refines this value's `isNull` property if the given value origin (`hasOrigin`)
         * matches this value's origin, otherwise this method just returns the original
         * value.
         */
        def refineIsNullIf(hasOrigin: ValueOrigin)(isNull: Answer): DomainReferenceValue

        protected[this] final def propagateRefineIsNullIf(
            hasOrigin: ValueOrigin)(
                isNull: Answer,
                operands: Operands,
                locals: Locals): (Operands, Locals) =
            propagateRefinementIf(hasOrigin)(isNull, operands, locals)(
                (rv: ReferenceValue, vo: ValueOrigin, isNull: Answer) ⇒
                    rv.refineIsNullIf(vo)(isNull)
            )

        /**
         * Refines the upper bound of this value's type to the given supertype.
         *
         * This call can be ignored if the type information related to this value is
         * precise, i.e., if we know that we precisely capture the runtime type of
         * this value.
         *
         * @return The updated operand stack and register values if there was something
         *      to refine.
         */
        @throws[ImpossibleRefinement]("If the refinement is not meaningful.")
        def refineUpperTypeBound(
            pc: PC,
            supertype: ReferenceType,
            operands: Operands,
            locals: Locals): (Operands, Locals)

        def refineUpperTypeBoundIf(
            hasOrigin: ValueOrigin)(
                supertype: ReferenceType): DomainReferenceValue

        protected[this] final def propagateRefineUpperTypeBoundIf(
            hasOrigin: ValueOrigin)(
                supertype: ReferenceType,
                operands: Operands,
                locals: Locals): (Operands, Locals) =
            propagateRefinementIf(hasOrigin)(supertype, operands, locals)(
                (rv: ReferenceValue, vo: ValueOrigin, supertype: ReferenceType) ⇒
                    rv.refineUpperTypeBoundIf(vo)(supertype)
            )

        /**
         * Helper method to propagate some refinement of the value's properties.
         *
         * @param refine A function that will get a ReferenceValue that should be
         *      refined using the given property, ''if and only if it's origin matches
         *      the passed origin''.
         */
        protected[this] final def propagateRefinementIf[Property](
            hasOrigin: ValueOrigin)(
                property: Property,
                operands: Operands,
                locals: Locals)(
                    refine: (ReferenceValue, ValueOrigin, Property) ⇒ ReferenceValue): (Operands, Locals) = {
            ( // OPERANDS
                if (operands.nonEmpty) {
                    var opsUpdated = false
                    var newOps: Operands = Nil
                    val opIt = operands.iterator
                    while (opIt.hasNext) {
                        opIt.next match {
                            case rv: ReferenceValue ⇒
                                val newRV = refine(rv, hasOrigin, property)
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
                // REGISTERS
                locals.transform { l ⇒
                    l match {
                        case rv: ReferenceValue ⇒ refine(rv, hasOrigin, property)
                        case other              ⇒ other
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

        def refineIsNullIf(
            hasOrigin: ValueOrigin)(
                isNull: Answer): DomainSingleOriginReferenceValue =
            // there is nothing to refine in this case since this value has definite properties
            this

        final override def refineIsNull(
            pc: PC,
            isNull: Answer,
            operands: Operands,
            locals: Locals): (Operands, Locals) =
            throw new ImpossibleRefinement(this, "nullness property of null value")

        final override def refineUpperTypeBound(
            pc: PC,
            supertype: ReferenceType,
            operands: Operands,
            locals: Locals): (Operands, Locals) =
            //(operands, locals)
            throw new ImpossibleRefinement(this, "refinement of type of null value")

        def refineUpperTypeBoundIf(
            hasOrigin: ValueOrigin)(
                supertype: ReferenceType): DomainSingleOriginReferenceValue =
            this

        protected override def doJoinWithNonNullValueWithSameOrigin(
            joinPC: PC,
            that: DomainSingleOriginReferenceValue): Update[DomainSingleOriginReferenceValue] = {
            if (that.isNull.isUnknown)
                StructuralUpdate(that)
            else
                StructuralUpdate(that(isNull = Unknown))
        }

        override def abstractsOver(other: DomainValue): Boolean = {
            (this eq other) || (other match {
                case that: NullValue ⇒
                    true
                case MultipleReferenceValues(values) ⇒
                    values.forall(v ⇒ this.abstractsOver(v))
            })
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
            propagateRefineIsNullIf(this.origin)(isNull, operands, locals)

        def refineIsNullIf(
            hasOrigin: ValueOrigin)(
                isNull: Answer): DomainSingleOriginReferenceValue = {
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

        // NARROWING OPERATION
        // OPAL calls this method only if a previous "subtype of" test 
        // (this.isValueSubtypeOf <: supertype ?) 
        // returned unknown and we are now on the branch where this relation
        // has to hold. Hence, we only need to handle the case where 
        // supertype is more strict than this type's upper type bound.
        @throws[ImpossibleRefinement]("if the refinement of the type is not possible")
        final override def refineUpperTypeBound(
            pc: PC,
            supertype: ReferenceType,
            operands: Operands,
            locals: Locals): (Operands, Locals) =
            propagateRefineUpperTypeBoundIf(this.origin)(supertype, operands, locals)
    }

    protected class ArrayValue(
        override val origin: ValueOrigin,
        override val isNull: Answer,
        override val isPrecise: Boolean,
        theUpperTypeBound: ArrayType)
            extends super.ArrayValue(theUpperTypeBound)
            with NonNullSingleOriginReferenceValue {
        this: DomainArrayValue ⇒

        override def apply(vo: ValueOrigin, isNull: Answer): DomainArrayValue = {
            ArrayValue(vo, isNull, isPrecise, theUpperTypeBound)
        }

        // NARROWING OPERATION
        @throws[ImpossibleRefinement]("if the refinement of the type is not possible")
        override def refineUpperTypeBoundIf(
            hasOrigin: ValueOrigin)(
                supertype: ReferenceType): DomainArrayValue = {
            if (this.origin == hasOrigin) {
                if (supertype.isArrayType) {
                    domain.isValueSubtypeOf(this, supertype) match {
                        case Yes ⇒
                            this
                        case Unknown ⇒
                            ArrayValue(hasOrigin, supertype.asArrayType)
                        case No ⇒
                            throw ImpossibleRefinement(
                                this, "refinement to incompatible type: "+supertype)
                    }
                } else
                    throw ImpossibleRefinement(
                        this, "refinement to a non-array value: "+supertype)
            } else
                this
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

        override def abstractsOver(other: DomainValue): Boolean = {
            (this eq other) || (other match {
                case that @ ArrayValue(thatUpperTypeBound) ⇒
                    (!this.isPrecise || that.isPrecise) &&
                        (this.isNull.isUnknown || that.isNull.isNo) &&
                        domain.isSubtypeOf(
                            that.theUpperTypeBound, this.theUpperTypeBound).isYes

                case that: NullValue ⇒
                    this.isNull.isUnknown

                case MultipleReferenceValues(values) ⇒
                    values.forall(v ⇒ this.abstractsOver(v))

                case _ ⇒
                    false
            })
        }

        override def adapt(target: TargetDomain, pc: PC): target.DomainValue =
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

        override def refineUpperTypeBoundIf(
            hasOrigin: ValueOrigin)(
                supertype: ReferenceType): DomainReferenceValue = {
            if (hasOrigin != this.origin)
                return this
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
                val newValue = ReferenceValue(this.origin, isNull, false, supertype)
                newValue
            } else if (domain.isSubtypeOf(theUpperTypeBound, supertype).isYes) {
                // useless refinement...
                this
            } else {
                if (supertype.isArrayType)
                    throw ImpossibleRefinement(this, "incompatible bound "+supertype.toJava)

                // basically, we are adding another type bound
                val newValue = ObjectValue(
                    this.origin,
                    this.isNull,
                    UIDSet(supertype.asObjectType, theUpperTypeBound))
                newValue
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
                            (this.isNull == that.isNull || this.isNull.isUnknown)
                            && !this.isPrecise
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
                            (this.isNull == that.isNull || this.isNull.isUnknown) &&
                            !this.isPrecise
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

        override def abstractsOver(other: DomainValue): Boolean = {
            if (this eq other)
                return true

            def checkPrecisionAndNullness(that: ReferenceValue): Boolean = {
                (!this.isPrecise || that.isPrecise) &&
                    (this.isNull.isUnknown || that.isNull.isNo)
            }

            other match {

                case that: SObjectValue ⇒
                    checkPrecisionAndNullness(that) &&
                        domain.isSubtypeOf(
                            that.theUpperTypeBound, this.theUpperTypeBound).isYes

                case that: NullValue ⇒
                    this.isNull.isUnknown

                case that: ArrayValue ⇒
                    checkPrecisionAndNullness(that) &&
                        domain.isSubtypeOf(
                            that.theUpperTypeBound, this.theUpperTypeBound).isYes

                case MultipleReferenceValues(values) ⇒
                    values.forall(v ⇒ this.abstractsOver(v))

                case that: MObjectValue ⇒
                    checkPrecisionAndNullness(that) && {
                        val lutb =
                            classHierarchy.joinObjectTypes(
                                this.theUpperTypeBound, that.upperTypeBound, true)
                        lutb.containsOneElement && (lutb.first() eq this.theUpperTypeBound)
                    }

                case _ ⇒
                    false
            }
        }

        override def adapt(target: TargetDomain, pc: PC): target.DomainValue = {
            target match {

                case thatDomain: l1.ReferenceValues ⇒
                    thatDomain.ObjectValue(pc, isNull, isPrecise, theUpperTypeBound).
                        asInstanceOf[target.DomainValue]

                case thatDomain: l0.DefaultTypeLevelReferenceValues ⇒
                    thatDomain.ReferenceValue(pc, theUpperTypeBound).
                        asInstanceOf[target.DomainValue]

                case _ ⇒ super.adapt(target, pc)
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

        override def refineUpperTypeBoundIf(
            hasOrigin: ValueOrigin)(
                supertype: ReferenceType): DomainSingleOriginReferenceValue = {
            if (hasOrigin != this.origin)
                return this;

            val theSupertype = supertype.asObjectType
            var newUpperTypeBound: UIDSet[ObjectType] = UIDSet.empty
            upperTypeBound foreach { (anUpperTypeBound: ObjectType) ⇒
                domain.isSubtypeOf(supertype, anUpperTypeBound) match {
                    case Yes ⇒
                        newUpperTypeBound += theSupertype
                    case No if domain.isSubtypeOf(anUpperTypeBound, supertype).isYes ⇒
                        newUpperTypeBound += anUpperTypeBound
                    case _ ⇒
                        newUpperTypeBound += anUpperTypeBound
                        newUpperTypeBound += theSupertype
                }
            }
            if (newUpperTypeBound.size == 1) {
                val newValue = ReferenceValue(hasOrigin, isNull, false, newUpperTypeBound.first)
                newValue
            } else {
                val newValue = ObjectValue(hasOrigin, isNull, newUpperTypeBound + supertype.asObjectType)
                newValue
            }
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
                    val joinedType = classHierarchy.joinObjectTypes(thatUpperTypeBound, thisUpperTypeBound, true)
                    joinedType match {
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

        override def adapt(target: TargetDomain, origin: ValueOrigin): target.DomainValue =
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

        override def referenceValues: Iterable[IsAReferenceValue] = values

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
                    if (!v.isNull.isYes) {
                        val upperTypeBound = v.upperTypeBound
                        if (theUpperTypeBound != null) {
                            isPrecise = theUpperTypeBound == upperTypeBound
                        }
                        theUpperTypeBound = upperTypeBound
                    }
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

        override def adapt(target: TargetDomain, pc: PC): target.DomainValue = {
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

                answer = answer & value.isValueSubtypeOf(supertype)
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
                propagateRefineIsNullIf(nextValue.origin)(isNull, operands, locals)
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
            locals: Locals): (Operands, Locals) = {
            values.foldLeft((operands, locals)) { (memoryLayout, nextValue) ⇒
                val (operands, locals) = memoryLayout
                propagateRefineUpperTypeBoundIf(nextValue.origin)(supertype, operands, locals)
            }
        }

        override def refineUpperTypeBoundIf(
            hasOrigin: ValueOrigin)(
                supertype: ReferenceType): DomainReferenceValue = {

            var newValues = SortedSet.empty[DomainSingleOriginReferenceValue]
            var valueRefined = false
            this.values foreach { value ⇒
                if (value.origin == hasOrigin && value.isNull.isNoOrUnknown) {
                    val isSubtypeOf = value.isValueSubtypeOf(supertype)
                    if (isSubtypeOf.isYes)
                        newValues += value
                    else if (isSubtypeOf.isUnknown) {
                        val newValue = value.refineUpperTypeBoundIf(hasOrigin)(supertype)
                        valueRefined = valueRefined || (value ne newValue)
                        newValues += newValue.asInstanceOf[DomainSingleOriginReferenceValue]
                    } else {
                        // if isSubtypeOf.no then we can just remove it
                        valueRefined = true
                    }
                } else {
                    newValues += value
                }
            }

            if (valueRefined) {
                if (newValues.size > 1)
                    MultipleReferenceValues(newValues)
                else
                    newValues.head
            } else
                this
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
    // HANDLING OF CONSTRAINTS
    //
    // -----------------------------------------------------------------------------------

    override def refEstablishUpperBound(
        pc: PC,
        bound: ReferenceType,
        operands: Operands,
        locals: Locals): (Operands, Locals) = {
        asReferenceValue(operands.head).refineUpperTypeBound(pc, bound, operands, locals)
    }

    protected[this] def refineIsNull(
        pc: PC,
        value: DomainValue,
        isNull: Answer,
        operands: Operands,
        locals: Locals): (Operands, Locals) = {
        asReferenceValue(value).refineIsNull(pc, isNull, operands, locals)
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
