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

import scala.reflect.ClassTag

import java.util.IdentityHashMap
import scala.annotation.elidable
import scala.annotation.elidable.ASSERTION
import scala.annotation.tailrec
import scala.collection.SortedSet
import org.opalj.util.Answer
import org.opalj.util.No
import org.opalj.util.Unknown
import org.opalj.util.Yes
import org.opalj.collection.immutable.IdentityPair
import org.opalj.collection.immutable.UIDSet
import org.opalj.collection.immutable.UIDSet0
import org.opalj.collection.immutable.UIDSet1
import org.opalj.br.ArrayType
import org.opalj.br.ObjectType
import org.opalj.br.ReferenceType
import org.opalj.br.Type
import org.opalj.br.UpperTypeBound
import org.opalj.ai.Computation
import org.opalj.ai.CorrelationalDomainSupport
import org.opalj.ai.DomainException
import org.opalj.ai.IntegerValuesDomain
import org.opalj.ai.IsAReferenceValue
import org.opalj.ai.MetaInformationUpdate
import org.opalj.ai.NoUpdate
import org.opalj.ai.NoUpdateType
import org.opalj.ai.SomeUpdate
import org.opalj.ai.StructuralUpdate
import org.opalj.ai.StructuralUpdateType
import org.opalj.ai.TypedValuesFactory
import org.opalj.ai.Update
import org.opalj.ai.UpdateType
import org.opalj.ai.domain.ClassHierarchy
import org.opalj.ai.domain.Configuration
import org.opalj.ai.domain.ImpossibleRefinement
import org.opalj.ai.domain.Origin

/**
 * This partial domain enables a tracking of a reference value's null-ness and origin
 * properties.
 *
 * This domain always abstracts over all values that have the same origin. I.e.,
 * two values created at the same origin (but at a different point in time), will
 * always be represented by one `DomainReferenceValue`.
 *
 * @author Michael Eichberg
 */
trait ReferenceValues extends l0.DefaultTypeLevelReferenceValues with Origin {
    domain: CorrelationalDomainSupport with IntegerValuesDomain with TypedValuesFactory with Configuration with ClassHierarchy ⇒

    type AReferenceValue <: ReferenceValue with DomainReferenceValue
    val AReferenceValue: ClassTag[AReferenceValue]

    type DomainSingleOriginReferenceValue <: SingleOriginReferenceValue with AReferenceValue
    val DomainSingleOriginReferenceValue: ClassTag[DomainSingleOriginReferenceValue]

    type DomainNullValue <: NullValue with DomainSingleOriginReferenceValue
    val DomainNullValue: ClassTag[DomainNullValue]

    type DomainObjectValue <: ObjectValue with DomainSingleOriginReferenceValue
    val DomainObjectValue: ClassTag[DomainObjectValue]

    type DomainArrayValue <: ArrayValue with DomainSingleOriginReferenceValue
    val DomainArrayValue: ClassTag[DomainArrayValue]

    type DomainMultipleReferenceValues <: MultipleReferenceValues with AReferenceValue
    val DomainMultipleReferenceValues: ClassTag[DomainMultipleReferenceValues]

    type Refinements = IdentityHashMap[ /*old*/ AReferenceValue, /*new*/ AReferenceValue]

    implicit object DomainSingleOriginReferenceValueOrdering
            extends Ordering[DomainSingleOriginReferenceValue] {

        def compare(
            x: DomainSingleOriginReferenceValue,
            y: DomainSingleOriginReferenceValue): Int = {
            x.origin - y.origin
        }
    }

    type Timestamp = Int

    private[this] var unusedT: Timestamp = 100

    def nextT(): Timestamp = { unusedT += 1; unusedT }

    object T {
        def unapply(value: ReferenceValue): Option[Timestamp] = Some(value.t)
    }

    @inline final def TimestampUpdate[T](value: T) = MetaInformationUpdate(value)

    /**
     * Identifies two `DomainReferenceValues` as equal if they definitively identify
     * the same object at runtime.
     * Hence, it is in general not possible to determine that two values are definitvely
     * not reference equal.
     */
    override def refAreEqual(pc: PC, value1: DomainValue, value2: DomainValue): Answer = {
        assert(value1.isInstanceOf[ReferenceValue] && value2.isInstanceOf[ReferenceValue])

        if (value1 eq value2)
            Yes
        else
            (value1, value2) match {
                case (T(t1), T(t2)) if t1 == t2 ⇒ Yes
                case _                          ⇒ super.refAreEqual(pc, value1, value2)
            }
    }

    /**
     * Determines if the runtime object type referred to by the given `values` is always
     * the same (or null). I.e., it determines if all values are precise
     * and have the same `upperTypeBound` or the empty `upperTypeBound`.
     */
    protected def isPrecise(values: Iterable[AReferenceValue]): Boolean = {
        val vIt = values.iterator
        var isPrecise: Boolean = true
        var theUpperTypeBound: UpperTypeBound = null
        while (isPrecise && vIt.hasNext) {
            val v = vIt.next()
            if (v.isPrecise) {
                if (v.isNull.isNoOrUnknown) {
                    val upperTypeBound = v.upperTypeBound
                    if (theUpperTypeBound != null) {
                        isPrecise = theUpperTypeBound == upperTypeBound
                    } else
                        theUpperTypeBound = upperTypeBound
                }
                // else // v.isNull.isYes === true
            } else {
                isPrecise = false
            }
        }
        isPrecise
    }

    protected def isNull(values: Iterable[AReferenceValue]): Answer = {
        val vIt = values.iterator
        var isNull: Answer = vIt.next().isNull
        while (isNull.isYesOrNo && vIt.hasNext) {
            isNull = isNull & vIt.next().isNull
        }
        isNull
    }

    /**
     * Calculates the most specific common upper type bound of the upper type bounds of
     * all values.
     */
    def upperTypeBound(theValues: SortedSet[DomainSingleOriginReferenceValue]): UpperTypeBound = {
        val values = theValues.view.filterNot(_.isNull.isYes)
        if (values.isEmpty)
            // <=> all values are null values!
            UIDSet.empty[ObjectType]
        else {
            var overallUTB = values.head.upperTypeBound

            def currentUTBisUTBForArrays: Boolean =
                overallUTB.hasOneElement && overallUTB.first.isArrayType

            def asUTBForArrays: ArrayType =
                overallUTB.first.asArrayType

            def asUTBForObjects: UIDSet[ObjectType] =
                overallUTB.asInstanceOf[UIDSet[ObjectType]]

            values.tail foreach { value ⇒
                overallUTB = value match {

                    case SObjectValue(nextUTB) ⇒
                        if (currentUTBisUTBForArrays)
                            classHierarchy.joinAnyArrayTypeWithObjectType(nextUTB)
                        else
                            classHierarchy.joinObjectTypes(nextUTB, asUTBForObjects, true)

                    case MObjectValue(nextUTB) ⇒
                        if (currentUTBisUTBForArrays)
                            classHierarchy.joinAnyArrayTypeWithMultipleTypesBound(nextUTB)
                        else
                            classHierarchy.joinUpperTypeBounds(asUTBForObjects, nextUTB, true)

                    case ArrayValue(nextUTB) ⇒
                        if (currentUTBisUTBForArrays)
                            classHierarchy.joinArrayTypes(asUTBForArrays, nextUTB) match {
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

    /**
     * Common supertrait of all values that represent reference values.
     *
     * This trait defines the additional methods needed for the refinement of the new
     * properties.
     */
    trait ReferenceValue extends super.ReferenceValue { this: AReferenceValue ⇒

        /**
         * Returns the timestamp of this object. I.e., an approximation of the point
         * in time at which this value was created.
         */
        def t: Timestamp

        /**
         * Refines this value's `isNull` property, if meaningful.
         *
         * ==Precondition==
         * OPAL calls this method only if a previous `isNull` test
         * returned `Unknown` and we are now on the branch where this relation
         * has to hold.
         *
         * @param pc The program counter of the instruction that was the reason
         *      for the refinement.
         * @param isNull This value's new null-ness property. `isNull` either
         *      has to be `Yes` or `No`. The refinement to `Unknown` neither makes
         *      sense nor is it supported.
         * @return The updated operand stack and register values if
         *      there was something to refine.
         */
        def refineIsNull(
            pc: PC,
            isNull: Answer,
            operands: Operands, locals: Locals): (Operands, Locals)

        /**
         * Refines the upper bound of this value's type to the given supertype.
         *
         * ==Precondition==
         * OPAL calls this method only if a previous "subtype of" test
         * (`this.isValueSubtypeOf(supertype)`)
         * returned `Unknown` and we are now on the branch where this relation
         * has to hold. '''Hence, we only need to handle the case where
         * supertype is more strict than this type's upper type bound.'''
         *
         * @return The updated operand stack and register values if there was something
         *      to refine otherwise the given stack and register values.
         */
        def refineUpperTypeBound(
            pc: PC,
            supertype: ReferenceType,
            operands: Operands, locals: Locals): (Operands, Locals)

        /**
         * Refine this value, if it depends on a value that was already refined.
         *
         * @note The refinements map will never contain `this` value as a key.
         *      The template method [[doPropagateRefinement]] already applies all
         *      standard refinements.
         * @return `true` if a refinement was added to the refinements map.
         */
        protected def refineIf(refinements: Refinements): Boolean

        /**
         * Helper method to propagate some refinement of the value's properties.
         */
        protected[this] final def propagateRefinement(
            oldValue: AReferenceValue, newValue: AReferenceValue,
            operands: Operands, locals: Locals): (Operands, Locals) = {

            assert(oldValue ne newValue)

            val refinements = new Refinements()
            refinements.put(oldValue, newValue)
            doPropagateRefinement(refinements, operands, locals)
        }

        protected[this] final def doPropagateRefinement(
            refinements: Refinements,
            operands: Operands, locals: Locals): (Operands, Locals) = {

            // We have to perform a fixpoint computation as one refinement can
            // lead to another refinement that can lead to yet another refinement
            // that...
            // In this case, whenever a new refinement is added to the list of
            // refinements the whole propagation process is restarted.

            @tailrec def refine(value: AReferenceValue): AReferenceValue = {
                val refinedValue = refinements.get(value)
                if (refinedValue != null) {
                    assert(refinedValue ne value)
                    refine(refinedValue)
                } else {
                    value
                }
            }

            (
                // OPERANDS
                if (operands.nonEmpty) {
                    var opsUpdated = false
                    var newOps: Operands = Nil
                    val opIt = operands.iterator
                    while (opIt.hasNext) {
                        val op = opIt.next
                        val newOp =
                            op match {
                                case AReferenceValue(op) ⇒
                                    val newOp = refine(op)
                                    if (newOp.refineIf(refinements))
                                        // RESTART REFINEMENT PROCESS!
                                        return doPropagateRefinement(
                                            refinements, operands, locals)
                                    if (newOp ne op) {
                                        opsUpdated = true
                                    }
                                    newOp
                                case _ ⇒ op
                            }
                        newOps = newOp :: newOps
                    }
                    if (opsUpdated) newOps.reverse else operands
                } else {
                    operands
                },
                // REGISTERS
                locals.transform {
                    case AReferenceValue(l) ⇒
                        val newL = refine(l)
                        if (newL.refineIf(refinements))
                            // RESTART REFINEMENT PROCESS!
                            return doPropagateRefinement(refinements, operands, locals)
                        newL
                    case l ⇒ l
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

        final def update(
            origin: ValueOrigin = this.origin,
            isNull: Answer = this.isNull): DomainSingleOriginReferenceValue = {
            updateT(this.t, origin, isNull)
        }

        /**
         * Creates a new instance of this object where the timestamp is set to the
         * given timestamp `t`. Optionally, it is also possible to update the `origin`
         * and `isNull` information.
         *
         * @example A typical usage:
         *  {{{
         *  val v : SingleOriginReferenceValue = ???
         *  val newV = v.updateT(nextT, isNull = Unknown)
         *  }}}
         */
        /*ABSTRACT*/ def updateT(
            t: Timestamp,
            origin: ValueOrigin = this.origin,
            isNull: Answer = this.isNull): DomainSingleOriginReferenceValue

        protected def refineIf(refinements: Refinements): Boolean = false

        final def refineIsNull(
            pc: PC,
            isNull: Answer,
            operands: Operands, locals: Locals): (Operands, Locals) = {

            assert(this.isNull.isUnknown)
            assert(isNull.isYesOrNo)

            val refinedValue = doRefineIsNull(isNull)
            propagateRefinement(this, refinedValue, operands, locals)
        }

        def doRefineIsNull(isNull: Answer): DomainSingleOriginReferenceValue

        final def refineUpperTypeBound(
            pc: PC,
            supertype: ReferenceType,
            operands: Operands, locals: Locals): (Operands, Locals) = {

            val refinedValue = doRefineUpperTypeBound(supertype)
            propagateRefinement(this, refinedValue, operands, locals)
        }

        def doRefineUpperTypeBound(supertype: ReferenceType): DomainSingleOriginReferenceValue

        def doRefineUpperTypeBound(supertypes: UIDSet[ReferenceType]): DomainSingleOriginReferenceValue = {
            assert(supertypes.nonEmpty)

            if (supertypes.hasOneElement) {
                doRefineUpperTypeBound(supertypes.first)
            } else {
                val newSupertypes = supertypes.asInstanceOf[UIDSet[ObjectType]]
                ObjectValue(this.origin, this.isNull, newSupertypes, this.t)
            }
        }

        /*ABSTRACT*/ protected def doJoinWithNonNullValueWithSameOrigin(
            pc: PC,
            that: DomainSingleOriginReferenceValue): Update[DomainSingleOriginReferenceValue]

        protected def doJoinWithMultipleReferenceValues(
            pc: PC,
            other: DomainMultipleReferenceValues): StructuralUpdate[DomainMultipleReferenceValues] = {

            // Invariant:
            // At most one value represented by MultipleReferenceValues
            // has the same origin as this value.
            other.values find { that ⇒ this.origin == that.origin } match {
                case None ⇒ StructuralUpdate(other.joinValue(this))
                case Some(that) ⇒
                    if (this eq that)
                        // <=> this value is part of the other "MultipleReferenceValues",
                        // however the MultipleReferenceValues (as a whole) may need
                        // to be updated if it was refined in the meantime!
                        StructuralUpdate(
                            other.update(other.values, valuesUpdated = false, this, other.t)
                        )

                    else {
                        // This value has the the same origin as the value found in 
                        // MultipleRefrenceValues.
                        val key = new IdentityPair(this, that)
                        val joinResult =
                            joinedValues.getOrElseUpdate(key, this.join(pc, that))

                        if (joinResult.isNoUpdate)
                            StructuralUpdate(other.rejoinValue(that, this, this))
                        else if (joinResult.value eq that) {
                            // Though the referenced value does not need to be updated,
                            // (this join that (<=> joinResult) => that)
                            // the MultipleReferenceValues (as a whole) may still need
                            // to be updated (to relax some constraints)
                            StructuralUpdate(
                                other.update(
                                    other.values, valuesUpdated = false,
                                    this,
                                    if (that.t == this.t) other.t else nextT()))
                        } else {
                            val joinedValue =
                                joinResult.value.asInstanceOf[DomainSingleOriginReferenceValue]
                            StructuralUpdate(other.rejoinValue(that, this, joinedValue))
                        }
                    }
            }
        }

        final protected def doJoinWithNullValueWithSameOrigin(
            joinPC: PC,
            that: DomainNullValue): Update[DomainSingleOriginReferenceValue] = {
            this.isNull match {
                case Yes ⇒
                    if (this.t == that.t)
                        NoUpdate
                    else
                        TimestampUpdate(that)
                case Unknown ⇒
                    if (this.t == that.t)
                        NoUpdate
                    else
                        TimestampUpdate(this.updateT(that.t))
                case No ⇒
                    StructuralUpdate(
                        this.updateT(that.t, isNull = Unknown)
                    )
            }
        }

        override protected def doJoin(
            joinPC: PC,
            other: DomainValue): Update[DomainValue] = {

            assert(this ne other)

            other match {
                case DomainSingleOriginReferenceValue(that) ⇒
                    if (this.origin == that.origin)
                        that match {
                            case DomainNullValue(that) ⇒
                                doJoinWithNullValueWithSameOrigin(joinPC, that)
                            case _ ⇒
                                doJoinWithNonNullValueWithSameOrigin(joinPC, that)
                        }
                    else {
                        val values =
                            SortedSet[DomainSingleOriginReferenceValue](this, that)
                        StructuralUpdate(MultipleReferenceValues(values))
                    }
                case DomainMultipleReferenceValues(that) ⇒
                    doJoinWithMultipleReferenceValues(joinPC, that)
            }
        }
    }

    protected class NullValue(
        override val origin: ValueOrigin,
        override val t: Int)
            extends super.NullValue with SingleOriginReferenceValue {
        this: DomainNullValue ⇒

        /**
         * @inheritdoc
         *
         * @param isNull Has to be `Yes`.
         */
        override def updateT(
            t: Timestamp,
            origin: ValueOrigin = this.origin,
            isNull: Answer = Yes): DomainNullValue = {
            assert(isNull.isYes, "a Null value's isNull property must be Yes")

            NullValue(origin, t)
        }

        override def doRefineIsNull(isNull: Answer): DomainSingleOriginReferenceValue =
            throw new ImpossibleRefinement(this, "nullness property of null value")

        def doRefineUpperTypeBound(supertype: ReferenceType): DomainSingleOriginReferenceValue =
            throw new ImpossibleRefinement(this, "refinement of type of null value")

        protected override def doJoinWithNonNullValueWithSameOrigin(
            joinPC: PC,
            that: DomainSingleOriginReferenceValue): StructuralUpdate[DomainSingleOriginReferenceValue] = {
            StructuralUpdate(
                if (that.isNull.isUnknown && that.t > this.t)
                    that
                else
                    that.updateT(Math.max(this.t, that.t), isNull = Unknown)
            )
        }

        override def abstractsOver(other: DomainValue): Boolean =
            (this eq other) || (asReferenceValue(other).isNull.isYes)

        override def equals(other: Any): Boolean = {
            other match {
                case that: NullValue ⇒ that.origin == this.origin && (that canEqual this)
                case _               ⇒ false
            }
        }

        def canEqual(other: NullValue): Boolean = true

        override def hashCode: Int = origin

        override def toString() = s"null(origin=$origin;t=$t)"
    }

    trait NonNullSingleOriginReferenceValue extends SingleOriginReferenceValue {
        this: DomainSingleOriginReferenceValue ⇒

        override def doRefineIsNull(isNull: Answer): DomainSingleOriginReferenceValue = {
            if (isNull.isYes) {
                NullValue(this.origin, this.t)
            } else {
                update(isNull = No)
            }
        }

        protected def doPeformJoinWithNonNullValueWithSameOrigin(
            that: DomainSingleOriginReferenceValue,
            newT: Timestamp): DomainSingleOriginReferenceValue

        override def doJoinWithNonNullValueWithSameOrigin(
            joinPC: PC,
            that: DomainSingleOriginReferenceValue): Update[DomainSingleOriginReferenceValue] = {

            if (this.t == that.t) {
                if (this.abstractsOver(that))
                    return NoUpdate
                else if (that.abstractsOver(this))
                    return StructuralUpdate(that);
                else
                    return StructuralUpdate(
                        doPeformJoinWithNonNullValueWithSameOrigin(that, t));
            }

            // the timestamps are different
            //
            if (this == that)
                return TimestampUpdate(that);
            if (this.abstractsOver(that))
                return TimestampUpdate(this.updateT(that.t));
            else if (that.abstractsOver(this))
                return StructuralUpdate(that); // StructuralUpdate(that.updateT());
            else
                return StructuralUpdate(
                    doPeformJoinWithNonNullValueWithSameOrigin(that, that.t));
        }
    }

    trait NonNullSingleOriginSReferenceValue extends NonNullSingleOriginReferenceValue {
        this: DomainSingleOriginReferenceValue ⇒

        def theUpperTypeBound: ReferenceType

        def doPeformJoinWithNonNullValueWithSameOrigin(
            that: DomainSingleOriginReferenceValue,
            newT: Timestamp): DomainSingleOriginReferenceValue = {
            val thisUTB = this.theUpperTypeBound
            val thatUTB = that.upperTypeBound
            val newIsNull = this.isNull & that.isNull
            val newIsPrecise =
                this.isPrecise && that.isPrecise &&
                    thatUTB.hasOneElement &&
                    (thisUTB eq thatUTB.first)
            val newUTB = classHierarchy.joinReferenceType(thisUTB, thatUTB)
            ReferenceValue(origin, newIsNull, newIsPrecise, newUTB, t)
        }

    }

    protected class ArrayValue(
        override val origin: ValueOrigin,
        override val isNull: Answer,
        override val isPrecise: Boolean,
        theUpperTypeBound: ArrayType,
        override val t: Timestamp)
            extends super.ArrayValue(theUpperTypeBound)
            with NonNullSingleOriginSReferenceValue {
        this: DomainArrayValue ⇒

        assert(isNull.isNoOrUnknown)
        assert(theUpperTypeBound.isBaseType && isPrecise || !theUpperTypeBound.isBaseType)

        override def updateT(
            t: Timestamp,
            origin: ValueOrigin, isNull: Answer): DomainArrayValue = {
            ArrayValue(origin, isNull, isPrecise, theUpperTypeBound, t)
        }

        def doRefineUpperTypeBound(supertype: ReferenceType): DomainSingleOriginReferenceValue = {
            assert(!isPrecise)

            ArrayValue(origin, isNull, isPrecise = false, supertype.asArrayType, t)
        }

        override def abstractsOver(other: DomainValue): Boolean = {
            if (this eq other)
                return true;

            val that = asReferenceValue(other)

            if (this.isNull.isUnknown && that.isNull.isYes)
                return true;

            val result =
                (this.isNull.isUnknown || that.isNull.isNo) &&
                    (!this.isPrecise || that.isPrecise) && {
                        val thatUTB = that.upperTypeBound
                        thatUTB.hasOneElement &&
                            thatUTB.first.isArrayType &&
                            isSubtypeOf(thatUTB.first.asArrayType, this.theUpperTypeBound).isYes
                    }
            result
        }

        override def adapt(
            targetDomain: TargetDomain,
            targetOrigin: ValueOrigin): targetDomain.DomainValue =
            targetDomain match {

                case thatDomain: l1.ReferenceValues ⇒
                    thatDomain.
                        ArrayValue(targetOrigin, isNull, isPrecise, theUpperTypeBound, thatDomain.nextT()).
                        asInstanceOf[targetDomain.DomainValue]

                case thatDomain: l0.DefaultTypeLevelReferenceValues ⇒
                    thatDomain.
                        ReferenceValue(targetOrigin, theUpperTypeBound).
                        asInstanceOf[targetDomain.DomainValue]

                case _ ⇒ super.adapt(targetDomain, targetOrigin)
            }

        protected def canEqual(other: ArrayValue): Boolean = true

        override def equals(other: Any): Boolean = {
            other match {
                case that: ArrayValue ⇒
                    (
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

        override def hashCode: Int =
            (((origin) * 41 +
                (if (isPrecise) 101 else 3)) * 13 +
                isNull.hashCode()) * 79 +
                upperTypeBound.hashCode()

        override def toString() = {
            var description = theUpperTypeBound.toJava+"(origin="+origin
            description += ";isNull="+isNull
            if (!isPrecise) description += ";isUpperBound"

            description += s";t=$t)"
            description
        }
    }

    trait ObjectValue extends super.ObjectValue with NonNullSingleOriginReferenceValue {
        this: DomainObjectValue ⇒
    }

    /**
     * @param origin The origin of the value (or the pseudo-origin (e.g., the index of
     *      the parameter) if the true origin is not known.)
     */
    protected class SObjectValue(
        override val origin: ValueOrigin,
        override val isNull: Answer,
        override val isPrecise: Boolean,
        theUpperTypeBound: ObjectType,
        override val t: Timestamp)
            extends super.SObjectValue(theUpperTypeBound)
            with ObjectValue
            with NonNullSingleOriginSReferenceValue {
        this: DomainObjectValue ⇒

        assert(this.isNull.isNoOrUnknown)

        override def updateT(
            t: Timestamp,
            origin: ValueOrigin, isNull: Answer): DomainObjectValue = {

            ObjectValue(origin, isNull, isPrecise, theUpperTypeBound, t)
        }

        def doRefineUpperTypeBound(supertype: ReferenceType): DomainSingleOriginReferenceValue = {
            val thisUTB = this.theUpperTypeBound

            assert(
                !isPrecise ||
                    // IMPROVE Do we want to support MObjectValues that have multiple types as an upper bound which are precise?
                    (isPrecise && domain.isSubtypeOf(supertype, thisUTB).isUnknown),
                s"this type is precise ${theUpperTypeBound.toJava}; "+
                    s"refinement goal: ${supertype.toJava} "+
                    s"(is this type a subtype of the given type: "+
                    s"${classHierarchy.isSubtypeOf(theUpperTypeBound, supertype)})")
            assert(thisUTB ne supertype)

            if (domain.isSubtypeOf(supertype, thisUTB).isYes) {
                // this also handles the case where we cast an Object to an array
                ReferenceValue(this.origin, this.isNull, false, supertype)
            } else {
                // this handles both cases:
                // Unknown => we just add it as another type bound (in this case
                //        the type bound may contain redundant information w.r.t.
                //        the overall type hierarchy)
                // No => we add it as another type bound
                if (supertype.isArrayType)
                    throw ImpossibleRefinement(
                        this,
                        s"incompatible refinement ${thisUTB.toJava} => ${supertype.toJava}")

                // basically, we are adding another type bound
                val newUTB = UIDSet(supertype.asObjectType, thisUTB)
                ObjectValue(this.origin, this.isNull, newUTB, t)
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
                        isSubtypeOf(that.theUpperTypeBound, this.theUpperTypeBound).isYes

                case that: NullValue ⇒
                    this.isNull.isUnknown

                case that: ArrayValue ⇒
                    checkPrecisionAndNullness(that) &&
                        isSubtypeOf(that.theUpperTypeBound, this.theUpperTypeBound).isYes

                case that: MultipleReferenceValues ⇒
                    checkPrecisionAndNullness(that) &&
                        classHierarchy.
                        isSubtypeOf(that.upperTypeBound, theUpperTypeBound).isYes

                case that: MObjectValue ⇒
                    checkPrecisionAndNullness(that) &&
                        classHierarchy.isSubtypeOf(that.upperTypeBound, this.theUpperTypeBound).isYes

                case _ ⇒
                    false
            }
        }

        override def adapt(target: TargetDomain, pc: PC): target.DomainValue = {
            target match {

                case thatDomain: l1.ReferenceValues ⇒
                    thatDomain.ObjectValue(pc, isNull, isPrecise, theUpperTypeBound, thatDomain.nextT()).
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
            ((theUpperTypeBound.hashCode * 41 +
                (if (isPrecise) 11 else 101)) * 13 +
                isNull.hashCode()) * 79 +
                origin

        override def toString() = {
            var description = theUpperTypeBound.toJava+"(origin="+origin
            description += ";isNull="+isNull
            if (!isPrecise) description += ";isUpperBound"
            description += s";t=$t)"
            description
        }

    }

    protected class MObjectValue(
        override val origin: ValueOrigin,
        override val isNull: Answer,
        upperTypeBound: UIDSet[ObjectType],
        override val t: Timestamp)
            extends super.MObjectValue(upperTypeBound)
            with ObjectValue {
        this: DomainObjectValue ⇒

        override def updateT(
            t: Timestamp,
            origin: ValueOrigin, isNull: Answer): DomainObjectValue = {
            ObjectValue(origin, isNull, upperTypeBound, t)
        }

        def doRefineUpperTypeBound(supertype: ReferenceType): DomainSingleOriginReferenceValue = {
            if (supertype.isObjectType) {
                val theSupertype = supertype.asObjectType
                var newUTB: UIDSet[ObjectType] = UIDSet.empty
                upperTypeBound foreach { (anUTB: ObjectType) ⇒
                    domain.isSubtypeOf(supertype, anUTB) match {
                        case Yes ⇒
                            newUTB += theSupertype
                        case _ ⇒
                            // supertype is either a supertype of anUTB or the
                            // the relationship is unknown; in both cases 
                            // we have to keep "anUTB"; however, we also have
                            // to add supertype if the relation is unknown.
                            newUTB += anUTB
                            if (domain.isSubtypeOf(anUTB, supertype).isUnknown)
                                newUTB += theSupertype
                    }
                }
                if (newUTB.size == 1) {
                    ObjectValue(origin, isNull, false, newUTB.first, t)
                } else {
                    ObjectValue(origin, isNull, newUTB + theSupertype, t)
                }
            } else {
                /* The supertype is an array type; this implies that this MObjectValue
                 * models the upper type bound "Serializable & Cloneable"; otherwise
                 * the refinement is illegal
                 */
                assert(upperTypeBound == ObjectType.SerializableAndCloneable)
                ArrayValue(origin, isNull, false, supertype.asArrayType, t)
            }
        }

        def doPeformJoinWithNonNullValueWithSameOrigin(
            that: DomainSingleOriginReferenceValue,
            newT: Timestamp): DomainSingleOriginReferenceValue = {
            val thisUTB = this.upperTypeBound
            val thatUTB = that.upperTypeBound
            val newIsNull = this.isNull & that.isNull
            val newIsPrecise = this.isPrecise && that.isPrecise && thisUTB == thatUTB
            val newUTB = classHierarchy.joinReferenceTypes(thisUTB, thatUTB)
            ReferenceValue(origin, newIsNull, newIsPrecise, newUTB, newT)
        }

        override def abstractsOver(other: DomainValue): Boolean = {
            if (this eq other)
                return true;

            val that = asReferenceValue(other)

            if (this.isNull.isNo && that.isNull.isYesOrUnknown)
                return false;

            val thatUTB = that.upperTypeBound
            classHierarchy.isSubtypeOf(thatUTB, this.upperTypeBound).isYes
        }

        override def adapt(target: TargetDomain, origin: ValueOrigin): target.DomainValue =
            target match {
                case td: ReferenceValues ⇒
                    td.ObjectValue(origin, isNull, this.upperTypeBound, td.nextT()).
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
            ((upperTypeBound.hashCode * 41 +
                (if (isPrecise) 11 else 101)) * 13 +
                isNull.hashCode()) * 79 +
                origin

        override def toString() = {
            upperTypeBound.map(_.toJava).mkString(" with ") +
                s"(origin=$origin;isNull=$isNull;isUpperBound;t=$t)"
        }
    }

    /**
     * A `MultipleReferenceValues` tracks multiple reference values (of type `NullValue`,
     * `ArrayValue`, `SObjectValue` and `MObjectValue`) that have different
     * origins. I.e., per value origin one domain value is used
     * to abstract over the properties of that respective value.
     *
     * @param  isPrecise `true` if the upper type bound of this value precisely
     *      captures the runtime type of the value.
     *      This ba-sically requires that all '''non-null''' values
     *      are precise and have the same upper type bound. Null values are ignored.
     */
    protected class MultipleReferenceValues(
        val values: SortedSet[DomainSingleOriginReferenceValue],
        override val isNull: Answer,
        override val isPrecise: Boolean,
        val upperTypeBound: UpperTypeBound,
        override val t: Timestamp)
            extends ReferenceValue
            with MultipleOriginsValue {
        this: DomainMultipleReferenceValues ⇒

        def this(values: SortedSet[DomainSingleOriginReferenceValue]) {
            this(
                values,
                domain.isNull(values),
                domain.isPrecise(values),
                domain.upperTypeBound(values),
                nextT()
            )
        }

        assert(
            values.size > 1,
            "a multiple reference value must reference multiple values")
        assert(
            isNull.isNoOrUnknown || values.forall { _.isNull.isYesOrUnknown },
            s"inconsistent null property(isNull == $isNull): ${values.mkString(",")}")
        assert(
            (isNull.isYes && upperTypeBound.isEmpty) ||
                (
                    isNull.isNoOrUnknown && upperTypeBound.nonEmpty &&
                    (
                        domain.upperTypeBound(values) == upperTypeBound ||
                        classHierarchy.isSubtypeOf(
                            domain.upperTypeBound(values),
                            upperTypeBound).isNoOrUnknown
                    )
                ),
            s"the upper type bound (isNull == $isNull) of ${values.mkString(",")} == ${domain.upperTypeBound(values)} <: $upperTypeBound"
        )

        def joinValue( // TODO: addValue
            newValue: DomainSingleOriginReferenceValue): DomainMultipleReferenceValues = {

            assert(values.find(_.origin == newValue.origin).isEmpty)

            val thisUTB = this.upperTypeBound
            val newValueUTB = newValue.upperTypeBound
            val joinedUTB = classHierarchy.joinUpperTypeBounds(thisUTB, newValueUTB)

            //   println(s"joinValue: $thisUTB join $newValueUTB ==> $joinedUTB")

            MultipleReferenceValues(
                this.values + newValue,
                this.isNull & newValue.isNull,
                this.isPrecise && newValue.isPrecise &&
                    this.upperTypeBound == newValue.upperTypeBound,
                joinedUTB,
                nextT()
            )
        }

        def rejoinValue( // TODO: updateValueWithSameOrigin
            oldValue: DomainSingleOriginReferenceValue,
            joinValue: DomainSingleOriginReferenceValue,
            joinedValue: DomainSingleOriginReferenceValue): DomainMultipleReferenceValues = {

            assert(oldValue ne joinValue)
            assert(oldValue ne joinedValue)
            assert(oldValue.origin == joinValue.origin)
            assert(oldValue.origin == joinedValue.origin)

            assert(values.find(_ eq oldValue).isDefined)

            val newValues = this.values - oldValue + joinedValue
            val newT = if (oldValue.t == joinedValue.t) this.t else nextT()
            update(newValues, valuesUpdated = true, joinValue, newT)
        }

        protected[ReferenceValues] def update(
            newValues: SortedSet[DomainSingleOriginReferenceValue],
            valuesUpdated: Boolean,
            joinedValue: DomainSingleOriginReferenceValue,
            newT: Timestamp): DomainMultipleReferenceValues = {

            val newIsNull = {
                val newIsNull = domain.isNull(newValues)
                if (newIsNull.isUnknown)
                    this.isNull & joinedValue.isNull
                else
                    newIsNull
            }
            val newIsPrecise = newIsNull.isYes || domain.isPrecise(newValues)
            val newUTB =
                if (newIsNull.isYes)
                    UIDSet.empty[ReferenceType]
                else {
                    val newValuesUTB = domain.upperTypeBound(newValues)
                    val baseUTB =
                        classHierarchy.joinUpperTypeBounds(
                            this.upperTypeBound, joinedValue.upperTypeBound)
                    if (newValuesUTB != baseUTB &&
                        classHierarchy.isSubtypeOf(newValuesUTB, baseUTB).isYes)
                        newValuesUTB
                    else
                        baseUTB
                }
            if (!valuesUpdated &&
                newT == this.t &&
                newIsNull == this.isNull &&
                newIsPrecise == this.isPrecise &&
                newUTB == this.upperTypeBound)
                this
            else
                MultipleReferenceValues(newValues, newIsNull, newIsPrecise, newUTB, newT)
        }

        override def origins: Iterable[ValueOrigin] = values.view.map(_.origin)

        override def referenceValues: Iterable[IsAReferenceValue] = values

        /**
         * Summarizes this value by creating a new domain value that abstracts over
         * the properties of all values.
         *
         * The given `pc` is used as the program counter of the newly created value.
         */
        override def summarize(pc: PC): DomainReferenceValue = {
            upperTypeBound /*<= basically creates the summary*/ match {
                case UIDSet0 ⇒ NullValue(pc, t)
                case UIDSet1(referenceType) ⇒
                    ReferenceValue(pc, isNull, isPrecise, referenceType, t)
                case utb ⇒
                    // We have an UpperTypeBound that has multiple types. Such bounds 
                    // cannot contain array types.
                    ObjectValue(pc, isNull, utb.asInstanceOf[UIDSet[ObjectType]], t)
            }
        }

        override def adapt(target: TargetDomain, pc: PC): target.DomainValue = {
            summarize(pc).adapt(target, pc)
        }

        override def isValueSubtypeOf(supertype: ReferenceType): Answer = {
            // Recall that the client has to make an "isNull" check before calling
            // isValueSubtypeOf. Hence, at least one of the possible reference values 
            // has to be non null and this value's upper type bound has to be non-empty.

            // It may the case that the subtype relation of each individual value – 
            // when compared with supertype - is Unknown, but that the type of the
            // value as a whole is still known to be a subtype
            val isSubtype =
                if (classHierarchy.isSubtypeOf(this.upperTypeBound, supertype).isYes)
                    Yes
                else
                    // Recall that the runtime type of this value can still be a 
                    // subtype of supertype even if this upperTypeBound is not
                    // a subtype of supertype.
                    Unknown

            val values = this.values.filter(_.isNull.isNoOrUnknown)
            var answer: Answer = values.head.isValueSubtypeOf(supertype)
            values.tail foreach { value ⇒
                if (answer == Unknown)
                    return isSubtype;

                answer = answer & value.isValueSubtypeOf(supertype)
            }
            // if we reach this point, `answer` is either Yes or No
            if (answer == Unknown)
                isSubtype
            else
                answer
        }

        protected def refineIf(refinements: Refinements): Boolean = {
            // [DEFERRED REFINEMENT]
            // In general, it may be the case that the refinement of a value
            // that is referred to by a multiple reference value, no longer satisfies
            // the general constraints imposed on the "MultipleReferenceValues"
            // as such. In this case the value is removed from the MultipleReferenceValues. 
            // E.g.,
            // Object n = maybeNull();
            // Object o = maybeNull();
            // Object m = .. ? n : o;
            // if(m == null) {
            //    // here m, may still refer to "n" and "o"
            //    // (m - as a whole - has to be null, but we
            //    // don't know to which value m is referring to)
            //    if(o != null) {
            //        // here m, may only refer to "n" and "n" __must be null__
            //        ...
            //        if(n != null) {
            //            ... dead
            //        }
            //    }
            // }

            // this value (as whole) was not previously refined
            val thisIsNull = this.isNull
            var refined = false
            var refinedValues = SortedSet.empty[DomainSingleOriginReferenceValue]
            this.values.foreach { value ⇒
                val refinedValue = refinements.get(value)
                // INVARIANT: refinedValue ne value
                if (refinedValue == null) refinedValues += value
                else {
                    refined = true

                    // we now have to check if the refined value can still be
                    // part of the "MultipleReferenceValues"
                    if ((thisIsNull.isUnknown || thisIsNull == refinedValue.isNull) &&
                        (!refinedValue.isPrecise ||
                            classHierarchy.isSubtypeOf(refinedValue.upperTypeBound, upperTypeBound).isYesOrUnknown)) {

                        refinedValues += refinedValue.asInstanceOf[DomainSingleOriginReferenceValue]
                    }
                    // else {
                    //     println(s"removed $value from ${this} due to refinement to $refinedValue")
                    // }
                }
            }

            if (!refined)
                return false;

            val thisUpperTypeBound = this.upperTypeBound

            if (refinedValues.size == 1) {
                val remainingValue = refinedValues.head
                var refinedValue = remainingValue
                // we now have to impose the conditions of this "MultipleReferenceValue"
                // on the refinedValue
                if (thisIsNull.isYesOrNo && refinedValue.isNull.isUnknown)
                    refinedValue = refinedValue.doRefineIsNull(isNull).asInstanceOf[DomainSingleOriginReferenceValue]
                if (thisIsNull.isNoOrUnknown /*if the value is null then there is nothing (more) to do*/ &&
                    !refinedValue.isPrecise /*if the value isPrecise then there is nothing (more) to do*/ &&
                    thisUpperTypeBound != refinedValue.upperTypeBound &&
                    classHierarchy.isSubtypeOf(thisUpperTypeBound, refinedValue.upperTypeBound).isYes) {
                    if (thisUpperTypeBound.hasOneElement)
                        refinedValue = refinedValue.doRefineUpperTypeBound(thisUpperTypeBound.first()).asInstanceOf[DomainSingleOriginReferenceValue]
                    else
                        refinedValue =
                            ObjectValue(
                                refinedValue.origin,
                                refinedValue.isNull,
                                thisUpperTypeBound.asInstanceOf[UIDSet[ObjectType]],
                                refinedValue.t
                            )
                }

                refinements.put(this, refinedValue)
                if (remainingValue ne refinedValue)
                    refinements.put(remainingValue, refinedValue)
                true

            } else {
                val newIsNull =
                    if (thisIsNull.isYesOrNo)
                        thisIsNull
                    else
                        domain.isNull(refinedValues)

                // The upper type bound can be independent from the least common 
                // upper type of a all values if, e.g., the value as a whole 
                // was casted to a specific value. 
                val newUTB =
                    if (newIsNull.isYes)
                        UIDSet.empty[ReferenceType]
                    else {
                        val newRefinedValuesUTB = domain.upperTypeBound(refinedValues)
                        if (newRefinedValuesUTB != upperTypeBound &&
                            classHierarchy.isSubtypeOf(newRefinedValuesUTB, upperTypeBound).isYes)
                            newRefinedValuesUTB
                        else
                            upperTypeBound
                    }

                refinements.put(
                    this,
                    MultipleReferenceValues(
                        refinedValues,
                        newIsNull,
                        isPrecise || domain.isPrecise(refinedValues),
                        newUTB,
                        nextT()
                    )
                )
            }
            true

        }

        protected[this] def refineToValue(
            value: DomainSingleOriginReferenceValue,
            isNullGoal: Answer,
            upperTypeBoundGoal: UpperTypeBound,
            operands: Operands, locals: Locals): (Operands, Locals) = {

            var newValue = value

            if (isNullGoal.isYesOrNo && newValue.isNull != isNullGoal) {
                newValue = newValue.doRefineIsNull(isNullGoal)
            }

            if (newValue.isNull.isNoOrUnknown) {
                val newValueUTB = newValue.upperTypeBound
                if (upperTypeBoundGoal != newValueUTB) {
                    // ALSO have to handle the case where upperTypeBoundGoal and 
                    // newValueUTB are NOT in an inheritance relationship!
                    val goalIsSubtype = classHierarchy.isSubtypeOf(upperTypeBoundGoal, newValueUTB)
                    if (goalIsSubtype.isYes)
                        newValue = newValue.doRefineUpperTypeBound(upperTypeBoundGoal)
                    else if (goalIsSubtype.isUnknown)
                        newValue = newValue.doRefineUpperTypeBound(upperTypeBoundGoal ++ newValueUTB)
                    else if (classHierarchy.isSubtypeOf(newValueUTB, upperTypeBoundGoal).isNoOrUnknown)
                        newValue = newValue.doRefineUpperTypeBound(upperTypeBoundGoal ++ newValueUTB)
                }
            }

            // we (at least) propagate the refinement of this value 
            val memoryLayout @ (operands1, locals1) =
                propagateRefinement(this, newValue, operands, locals)

            if (value ne newValue)
                propagateRefinement(value, newValue, operands1, locals1)
            else
                memoryLayout
        }

        override def refineIsNull(
            pc: PC,
            isNull: Answer,
            operands: Operands, locals: Locals): (Operands, Locals) = {

            assert(this.isNull.isUnknown)
            assert(isNull.isYesOrNo)

            // Recall that this value's property – as a whole – can be undefined also 
            // each individual value's property is well defined (Yes, No).
            // Furthermore, the parameter isNull is either Yes or No and we are 
            // going to filter those values that do not satisfy the constraint.

            val newValues = values.filter(v ⇒ v.isNull == isNull || v.isNull.isUnknown)

            if (newValues.size == 1) {
                refineToValue(newValues.head, isNull, this.upperTypeBound, operands, locals)
            } else {
                val newT = if (newValues.size == values.size) t else nextT()
                val newValuesUTB = domain.upperTypeBound(newValues)
                // we have to choose the more "precise" utb
                val newValue =
                    if (isNull.isYes)
                        MultipleReferenceValues(
                            newValues,
                            Yes, // we refined the "isNull" property!
                            true, // all values are null...
                            UIDSet.empty[ReferenceType],
                            newT
                        )
                    else
                        MultipleReferenceValues(
                            newValues,
                            No, // we refined the "isNull" property!
                            domain.isPrecise(newValues),
                            if (classHierarchy.isSubtypeOf(this.upperTypeBound, newValuesUTB).isYesOrUnknown)
                                this.upperTypeBound
                            else
                                newValuesUTB,
                            newT
                        )
                propagateRefinement(this, newValue, operands, locals)
            }
        }

        override def refineUpperTypeBound(
            pc: PC,
            supertype: ReferenceType,
            operands: Operands,
            locals: Locals): (Operands, Locals) = {

            // let's filter all values that are precise and which are not a 
            // subtype of the new supertype

            val filteredValues =
                this.values.filter { value ⇒
                    value.isNull.isYes || {
                        value.isValueSubtypeOf(supertype) match {
                            case Yes | Unknown ⇒ true
                            case No            ⇒ false
                        }
                    }
                }

            if (filteredValues.size == 1) {
                refineToValue(filteredValues.head, this.isNull, UIDSet(supertype), operands, locals)
                //                val valueToRefine = filteredValues.head
                //                val valueToRefineUTB = valueToRefine.upperTypeBound
                //                if ((valueToRefineUTB.hasOneElement &&
                //                    (valueToRefineUTB.first eq supertype)) ||
                //                    classHierarchy.isSubtypeOf(valueToRefineUTB.first, supertype).isYes)
                //                    // the encapsulated values does not need to be refined; it 
                //                    // already has the required properties
                //                    propagateRefinement(this, valueToRefine, operands, locals)
                //                else {
                //                    val newValue = valueToRefine.doRefineUpperTypeBound(supertype)
                //                    // propagate the refinement of this value
                //                    val (operands1, locals1) =
                //                        propagateRefinement(this, newValue, operands, locals)
                //                    // propagate the refinement of the single encapsulated value
                //                    propagateRefinement(valueToRefine, newValue, operands1, locals1)
                //                }
            } else {
                // there are no individual values to refine....
                // we have to choose the more "precise" utb
                val filteredValuesUTB = domain.upperTypeBound(filteredValues)

                // we have to support the case where we cast a value with an interface
                // as an upper type bound to a second interface
                val supertypeUTB =
                    if (classHierarchy.isSubtypeOf(supertype, this.upperTypeBound).isNoOrUnknown)
                        this.upperTypeBound + supertype
                    else
                        UIDSet[ReferenceType](supertype)
                val newUTB =
                    if (classHierarchy.isSubtypeOf(filteredValuesUTB, supertypeUTB).isYes)
                        filteredValuesUTB
                    else
                        supertypeUTB
                val newT = if (filteredValues.size == values.size) t else nextT
                val newValue =
                    MultipleReferenceValues(
                        filteredValues,
                        if (isNull.isYesOrNo)
                            isNull
                        else
                            domain.isNull(filteredValues),
                        domain.isPrecise(filteredValues),
                        newUTB,
                        newT
                    )
                propagateRefinement(this, newValue, operands, locals)
            }
        }

        protected[this] def doReJoinSingleOriginReferenceValue(
            joinPC: PC,
            thisValue: DomainSingleOriginReferenceValue,
            thatValue: DomainSingleOriginReferenceValue): Update[DomainValue] = {

            if (thisValue eq thatValue)
                return NoUpdate;

            // we may have seen the "inner join" previously, i.e., 
            // a join of thisValue with thatValue
            val joinKey = new IdentityPair(thisValue, thatValue)
            val joinResult =
                joinedValues.getOrElseUpdate(joinKey, thisValue.join(joinPC, thatValue))

            joinResult match {
                case NoUpdate ⇒
                    var updateType: UpdateType = NoUpdateType
                    // though thisValue abstracts over the "joined" value
                    // we still have to check that this value (as a whole)
                    // also abstract over `thatValue`
                    // E.g., consider the following case:                
                    // given OneOf(null(origin=7;t=103),int[](origin=15;isNull=Unknown;t=887));lutb=;isPrecise=true;isNull=Yes
                    // join                             int[](origin=15;isNull=No;t=887)
                    // => As a whole "isNull" has to be Unknown
                    val thisUTB = this.upperTypeBound
                    val thatUTB = thatValue.upperTypeBound
                    val newIsNull = this.isNull & thatValue.isNull
                    val newUTB =
                        if (newIsNull.isYes) {
                            UIDSet.empty[ReferenceType]
                        } else {
                            classHierarchy.joinUpperTypeBounds(thisUTB, thatUTB)
                        }
                    if (newIsNull != this.isNull || newUTB != thisUTB)
                        updateType = StructuralUpdateType
                    val newIsPrecise = this.isPrecise && thatValue.isPrecise && (
                        thisUTB.isEmpty || thatUTB.isEmpty || thisUTB == thatUTB
                    )

                    if (updateType != NoUpdateType) {
                        updateType(
                            MultipleReferenceValues(
                                this.values,
                                newIsNull,
                                newIsPrecise,
                                newUTB,
                                this.t
                            )
                        )
                    } else
                        NoUpdate

                case update @ SomeUpdate(newValue) ⇒
                    val joinedValue = newValue.asInstanceOf[DomainSingleOriginReferenceValue]

                    //println(this+" :::"+thisValue+"   joined     "+joinedValue)
                    update.updateValue(
                        rejoinValue(thisValue, thatValue, joinedValue)
                    )
            }
        }

        override protected def doJoin(joinPC: PC, other: DomainValue): Update[DomainValue] = {
            assert(this ne other)

            other match {

                case DomainSingleOriginReferenceValue(thatValue) ⇒
                    this.values.find(_.origin == thatValue.origin) match {
                        case Some(thisValue) ⇒
                            doReJoinSingleOriginReferenceValue(joinPC, thisValue, thatValue)
                        case None ⇒
                            StructuralUpdate(this.joinValue(thatValue))
                    }

                case that: MultipleReferenceValues ⇒
                    var updateType: UpdateType = NoUpdateType
                    var otherValues = that.values
                    var newValues = SortedSet.empty[DomainSingleOriginReferenceValue]
                    this.values foreach { thisValue ⇒
                        otherValues.find(thisValue.origin == _.origin) match {
                            case Some(otherValue) ⇒
                                otherValues -= otherValue
                                if (thisValue eq otherValue) {
                                    newValues += thisValue
                                } else {
                                    val joinResult =
                                        joinedValues.getOrElseUpdate(
                                            new IdentityPair(thisValue, otherValue),
                                            thisValue.join(joinPC, otherValue))

                                    joinResult match {
                                        case NoUpdate ⇒
                                            newValues += thisValue
                                        case update @ SomeUpdate(DomainSingleOriginReferenceValue(otherValue)) ⇒
                                            updateType = updateType &: update
                                            newValues += otherValue
                                    }
                                }
                            case None ⇒
                                newValues += thisValue
                        }
                    }

                    if (otherValues.nonEmpty) {
                        newValues ++= otherValues
                        updateType = StructuralUpdateType
                    }
                    val thisUTB = this.upperTypeBound
                    val thatUTB = that.upperTypeBound
                    val newIsNull = domain.isNull(newValues).ifUnknown(this.isNull & that.isNull)
                    val newUTB =
                        if (newIsNull.isYes) {
                            UIDSet.empty[ReferenceType]
                        } else {
                            val baseUTB =
                                classHierarchy.joinUpperTypeBounds(thisUTB, thatUTB)
                            val newValuesUTB = domain.upperTypeBound(newValues)

                            if (newValuesUTB != baseUTB &&
                                classHierarchy.isSubtypeOf(newValuesUTB, baseUTB).isYes) {
                                newValuesUTB
                            } else
                                baseUTB
                        }
                    if (newIsNull != this.isNull || newUTB != thisUTB)
                        updateType = StructuralUpdateType
                    val newIsPrecise = this.isPrecise && that.isPrecise && (
                        thisUTB.isEmpty || thatUTB.isEmpty || thisUTB == thatUTB
                    )

                    val newT = if (this.t == that.t) this.t else nextT()

                    updateType(
                        MultipleReferenceValues(
                            newValues,
                            newIsNull,
                            newIsPrecise,
                            newUTB,
                            newT
                        )
                    )
            }
        }

        // We have to handle a case such as:
        // Object o = "some Object A"
        // if(...) o = "some Object B"
        // ((String[])o)[0]
        //

        override def load(pc: PC, index: DomainValue): ArrayLoadResult = {
            if (isNull.isYes)
                return justThrows(NullPointerException(pc));

            assert(upperTypeBound.hasOneElement,
                "no array type: "+this.upperTypeBound)
            assert(upperTypeBound.first.isArrayType, s"$upperTypeBound")

            if (values.find(_.isInstanceOf[ObjectValue]).nonEmpty) {
                var thrownExceptions: List[ExceptionValue] = Nil
                if (isNull.isUnknown && throwNullPointerExceptionOnArrayAccess)
                    thrownExceptions = NullPointerException(pc) :: thrownExceptions
                if (throwArrayIndexOutOfBoundsException)
                    thrownExceptions = ArrayIndexOutOfBoundsException(pc) :: thrownExceptions

                ComputedValueOrException(
                    TypedValue(pc, upperTypeBound.first.asArrayType.componentType),
                    thrownExceptions)
            } else {
                (values map (_.load(pc, index))) reduce {
                    (c1, c2) ⇒ mergeDEsComputations(pc, c1, c2)
                }
            }
        }

        override def store(pc: PC, value: DomainValue, index: DomainValue): ArrayStoreResult = {
            if (isNull.isYes)
                return justThrows(NullPointerException(pc));

            assert(upperTypeBound.hasOneElement)
            assert(upperTypeBound.first.isArrayType, s"$upperTypeBound")

            if (values.find(_.isInstanceOf[ObjectValue]).nonEmpty) {
                var thrownExceptions: List[ExceptionValue] = Nil
                if (isNull.isUnknown && throwNullPointerExceptionOnArrayAccess)
                    thrownExceptions = NullPointerException(pc) :: thrownExceptions
                if (throwArrayIndexOutOfBoundsException)
                    thrownExceptions = ArrayIndexOutOfBoundsException(pc) :: thrownExceptions
                if (throwArrayStoreException)
                    thrownExceptions = ArrayStoreException(pc) :: thrownExceptions

                ComputationWithSideEffectOrException(thrownExceptions)
            } else {
                (values map (_.store(pc, value, index))) reduce {
                    (c1, c2) ⇒ mergeEsComputations(pc, c1, c2)
                }
            }
        }

        override def length(pc: PC): Computation[DomainValue, ExceptionValue] = {
            if (isNull.isYes)
                return throws(NullPointerException(pc));

            assert(upperTypeBound.hasOneElement)
            assert(upperTypeBound.first.isArrayType, s"$upperTypeBound (values=$values)")

            if (values.find(_.isInstanceOf[ObjectValue]).nonEmpty) {
                if (isNull.isUnknown && throwNullPointerExceptionOnArrayAccess)
                    ComputedValueOrException(IntegerValue(pc), NullPointerException(pc))
                else
                    ComputedValue(IntegerValue(pc))
            } else {
                val computations = values map (_.length(pc))
                computations reduce { (c1, c2) ⇒ mergeDEComputations(pc, c1, c2) }
            }
        }

        override lazy val hashCode: Int = values.hashCode * 47

        override def equals(other: Any): Boolean = {
            other match {
                case that: MultipleReferenceValues ⇒
                    this.isNull == that.isNull &&
                        this.isPrecise == that.isPrecise &&
                        this.upperTypeBound == that.upperTypeBound &&
                        that.values == this.values
                case _ ⇒
                    false
            }
        }

        override def toString() = {
            var s = values.mkString("OneOf["+values.size+"](", ", ", ")")
            val lutb = upperTypeBound
            if (lutb.nonEmpty)
                s += lutb.map(_.toJava).mkString(";lutb=", " with ", "")
            if (!isPrecise) s += ";isUpperBound"
            s += s";isNull=$isNull;t=$t"
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
        ObjectValue(pc, No, false, objectType, nextT())

    override def NewObject(pc: PC, objectType: ObjectType): DomainObjectValue =
        ObjectValue(pc, No, true, objectType, nextT())

    override def InitializedObjectValue(pc: PC, objectType: ObjectType): DomainObjectValue =
        ObjectValue(pc, No, true, objectType, nextT())

    override def StringValue(pc: PC, value: String): DomainObjectValue =
        ObjectValue(pc, No, true, ObjectType.String, nextT())

    override def ClassValue(pc: PC, t: Type): DomainObjectValue =
        ObjectValue(pc, No, true, ObjectType.Class, nextT())

    override def ObjectValue(pc: PC, objectType: ObjectType): DomainObjectValue =
        ObjectValue(pc, Unknown, false, objectType, nextT())

    override def ObjectValue(pc: PC, upperTypeBound: UIDSet[ObjectType]): DomainObjectValue =
        ObjectValue(pc, Unknown, upperTypeBound, nextT())

    override def InitializedArrayValue(pc: PC, counts: List[Int], arrayType: ArrayType): DomainArrayValue =
        ArrayValue(pc, No, true, arrayType, nextT())

    override def NewArray(pc: PC, count: DomainValue, arrayType: ArrayType): DomainArrayValue =
        ArrayValue(pc, No, true, arrayType, nextT())

    override def NewArray(pc: PC, counts: List[DomainValue], arrayType: ArrayType): DomainArrayValue =
        ArrayValue(pc, No, true, arrayType, nextT())

    override protected[domain] def ArrayValue(pc: PC, arrayType: ArrayType): DomainArrayValue = {
        if (arrayType.elementType.isBaseType)
            ArrayValue(pc, Unknown, true, arrayType, nextT())
        else
            ArrayValue(pc, Unknown, false, arrayType, nextT())
    }

    override def NullValue(pc: PC): DomainNullValue = NullValue(pc, nextT())

    protected[domain] def ReferenceValue( // for SObjectValue
        origin: ValueOrigin,
        isNull: Answer,
        isPrecise: Boolean,
        theUpperTypeBound: ReferenceType,
        t: Timestamp): DomainSingleOriginReferenceValue = {
        theUpperTypeBound match {
            case ot: ObjectType ⇒
                ObjectValue(origin, isNull, isPrecise, ot, t)
            case at: ArrayType ⇒
                ArrayValue(origin, isNull, isPrecise || at.elementType.isBaseType, at, t)
        }
    }

    final protected[domain] def ReferenceValue( // for SObjectValue
        origin: ValueOrigin,
        isNull: Answer,
        isPrecise: Boolean,
        theUpperTypeBound: ReferenceType): DomainSingleOriginReferenceValue = {
        ReferenceValue(origin, isNull, isPrecise, theUpperTypeBound, nextT())
    }

    final protected[domain] def ReferenceValue( // for SObjectValue
        origin: ValueOrigin,
        isNull: Answer,
        isPrecise: Boolean,
        upperTypeBound: UIDSet[ReferenceType],
        t: Timestamp): DomainSingleOriginReferenceValue = {
        upperTypeBound match {
            case UIDSet1(referenceType) ⇒
                ReferenceValue(origin, isNull, isPrecise, referenceType, t)
            case _ ⇒
                val utb = upperTypeBound.asInstanceOf[UIDSet[ObjectType]]
                ObjectValue(origin, isNull, utb, t)
        }
    }

    // TODO pc => origin
    protected[domain] def ObjectValue( // for MObjectValue
        pc: PC,
        isNull: Answer,
        upperTypeBound: UIDSet[ObjectType]): DomainObjectValue =
        ObjectValue(pc, isNull, upperTypeBound, nextT)

    protected[domain] def ObjectValue( // for SObjectValue
        pc: PC,
        isNull: Answer,
        isPrecise: Boolean,
        theUpperTypeBound: ObjectType): DomainObjectValue =
        ObjectValue(pc, isNull, isPrecise, theUpperTypeBound, nextT())

    //
    // DECLARATION OF ADDITIONAL DOMAIN VALUE FACTORY METHODS
    //

    protected[domain] def NullValue(pc: PC, t: Int): DomainNullValue

    protected[domain] def ObjectValue( // for SObjectValue
        pc: PC,
        isNull: Answer,
        isPrecise: Boolean,
        theUpperTypeBound: ObjectType,
        t: Int): DomainObjectValue

    protected[domain] def ObjectValue( // for MObjectValue
        pc: PC,
        isNull: Answer,
        upperTypeBound: UIDSet[ObjectType],
        t: Int): DomainObjectValue

    protected[domain] def ArrayValue( // for ArrayValue
        pc: PC,
        isNull: Answer,
        isPrecise: Boolean,
        theUpperTypeBound: ArrayType,
        t: Int): DomainArrayValue

    protected[domain] def MultipleReferenceValues(
        values: SortedSet[DomainSingleOriginReferenceValue]): DomainMultipleReferenceValues

    protected[domain] def MultipleReferenceValues(
        values: SortedSet[DomainSingleOriginReferenceValue],
        isNull: Answer,
        isPrecise: Boolean,
        upperTypeBound: UpperTypeBound,
        t: Int): DomainMultipleReferenceValues

    // TODO [Refactor] isPrecise => typeIsPrecise
}

