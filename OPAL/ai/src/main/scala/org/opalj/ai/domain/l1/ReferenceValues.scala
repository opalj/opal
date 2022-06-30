/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l1

import scala.language.existentials
import scala.annotation.tailrec
import scala.reflect.ClassTag

import java.util.IdentityHashMap

import org.opalj.collection.IntIterator
import org.opalj.collection.UID
import org.opalj.collection.immutable.IdentityPair
import org.opalj.collection.immutable.UIDSet
import org.opalj.collection.immutable.UIDSet1
import org.opalj.collection.immutable.UIDSet2
import org.opalj.value.IsMultipleReferenceValue
import org.opalj.value.IsNullValue
import org.opalj.value.IsReferenceValue
import org.opalj.value.ValueInformation
import org.opalj.br.ArrayType
import org.opalj.br.ComputationalType
import org.opalj.br.ComputationalTypeReference
import org.opalj.br.ObjectType
import org.opalj.br.ReferenceType
import org.opalj.br.Type
import org.opalj.ai.domain.Origin.MultipleOriginsValue
import org.opalj.ai.domain.Origin.SingleOriginValue

/**
 * This partial domain enables tracking of a reference value's null-ness and must-alias information.
 *
 * @author Michael Eichberg
 */
trait ReferenceValues extends l0.DefaultTypeLevelReferenceValues with Origin {
    domain: CorrelationalDomainSupport with IntegerValuesDomain with TypedValuesFactory with Configuration =>

    type AReferenceValue <: TheReferenceValue with DomainReferenceValue
    val AReferenceValueTag: ClassTag[AReferenceValue]

    type DomainSingleOriginReferenceValue <: SingleOriginReferenceValue with AReferenceValue
    val DomainSingleOriginReferenceValueTag: ClassTag[DomainSingleOriginReferenceValue]

    type DomainNullValue <: NullValue with DomainSingleOriginReferenceValue
    val DomainNullValueTag: ClassTag[DomainNullValue]

    type DomainObjectValue <: ObjectValue with DomainSingleOriginReferenceValue
    val DomainObjectValueTag: ClassTag[DomainObjectValue]

    type DomainArrayValue <: ArrayValue with DomainSingleOriginReferenceValue
    val DomainArrayValueTag: ClassTag[DomainArrayValue]

    type DomainMultipleReferenceValues <: MultipleReferenceValues with AReferenceValue
    val DomainMultipleReferenceValuesTag: ClassTag[DomainMultipleReferenceValues]

    abstract override def providesOriginInformationFor(ct: ComputationalType): Boolean = {
        ct == ComputationalTypeReference || super.providesOriginInformationFor(ct)
    }

    /**
     * A map that contains the refined values (the map's values) of some old values (the
     * map's keys).
     */
    type Refinements = IdentityHashMap[AReferenceValue /*old*/ , AReferenceValue /*new*/ ]

    /**
     * Defines a total order on reference values with a single origin by subtracting
     * both origins.
     */
    implicit object DomainSingleOriginReferenceValueOrdering
        extends Ordering[DomainSingleOriginReferenceValue] {

        def compare(
            x: DomainSingleOriginReferenceValue,
            y: DomainSingleOriginReferenceValue
        ): Int = {
            x.origin - y.origin
        }
    }

    /**
     * Two domain values that have the same refid are guaranteed to refer
     * to the same object at runtime (must-alias).
     *
     * The refid enables us to distinguish two values created/returned by the same
     * instruction but at a different point in time (recall, both values have the same origin).
     * Such values may or may not be different; i.e., those values may or may not refer
     * to the same object on the heap/stack.
     *
     * RefIds are required to determine changes in the memory layout. I.e., to
     * determine if two values created by the same instruction are aliases or "just"
     * maybe aliases. This information is particularly relevant if two values -
     * stored in registers - are no longer guaranteed to be aliases!
     */
    type RefId = Int

    final def nullRefId: RefId = 100
    private[this] final val initialRefId: RefId = 101 // 101 is chosen for readability purposes

    /**
     * The next free reference id.
     */
    private[this] var unusedRefId: RefId = initialRefId

    /**
     * Returns the next unused time stamp.
     */
    def nextRefId(): RefId = { unusedRefId += 1; unusedRefId }

    /**
     * Creates an update object that characterizes a reference id update.
     *
     * Basically, just a wrapper for a `MetaInformationUpdate`; the purpose is to
     * better communicate the underlying purpose.
     */
    @inline final def RefIdUpdate[T](value: T) = MetaInformationUpdate(value)

    /**
     * Returns `Yes` if both `DomainReferenceValues` definitively identify
     * the same object at runtime.
     *
     * Using this domain, it is in general not possible to determine that two
     * values are definitively not reference equal unless they are type incompatible.
     */
    override def refAreEqual(pc: Int, v1: DomainValue, v2: DomainValue): Answer = {
        assert(v1.isInstanceOf[TheReferenceValue] && v2.isInstanceOf[TheReferenceValue])
        if (v1 eq v2)
            return Yes;

        if (asReferenceValue(v1).refId == asReferenceValue(v2).refId) {
            Yes
        } else {
            super.refAreEqual(pc, v1, v2)
        }
    }

    /**
     * Computes the '''effective upper-type bound''' which is the (single) final type in the utb
     * if it contains one. In this case the other types have to be in a super-/subtype
     * relation and were added "just" as a result of explicit CHECKCAST instructions.
     *
     * @note This method is generally only useful when we have to handle incomplete type hierarchies.
     */
    protected def effectiveUTB(utb: UIDSet[_ <: ReferenceType]): UIDSet[_ <: ReferenceType] = {
        val it = utb.iterator
        while (it.hasNext) {
            val t: ReferenceType = it.next()
            if (t.isArrayType)
                return utb;

            if (classHierarchy.isKnownToBeFinal(t.asObjectType))
                return UIDSet1(t);
        }
        utb
    }

    /**
     * Determines if the runtime object type referred to by the given `values` is always
     * the same. I.e., it determines if all values are precise and have the same `upperTypeBound`.
     *
     * `Null` values are ignored when determining the precision; i.e., if all values represent
     * `Null` `true` will be returned.
     */
    protected def isPrecise(values: Iterable[AReferenceValue]): Boolean = {
        val vIt = values.iterator
        var theUTB: UIDSet[_ <: ReferenceType] = null
        while (vIt.hasNext) {
            val v = vIt.next()
            if (v.isNull.isNoOrUnknown) {
                val vUTB = effectiveUTB(v.upperTypeBound.toUIDSet[ReferenceType])
                if (!v.isPrecise || ((theUTB ne null) && theUTB != vUTB))
                    return false; // <===== early return from method
                else if (theUTB == null)
                    theUTB = vUTB
            }
        }
        true
    }

    /**
     * Determines the common null-ness property of the given reference values.
     */
    protected def isNull(values: Iterable[AReferenceValue]): Answer = {
        val vIt = values.iterator
        var isNull: Answer = vIt.next().isNull
        while (isNull.isYesOrNo && vIt.hasNext) { isNull = isNull join vIt.next().isNull }
        isNull
    }

    /**
     * Calculates the most specific common upper type bound of the upper type bounds of
     * all values. `NullValue`s are ignored unless all values are representing `Null`.
     */
    def upperTypeBound(
        theValues: UIDSet[DomainSingleOriginReferenceValue]
    ): UIDSet[_ <: ReferenceType] = {
        val values = theValues.filterNot(_.isNull.isYes)
        if (values.isEmpty)
            // <=> all values are null values!
            return EmptyUpperTypeBound; // <====== early return from method

        var overallUTB: UIDSet[_ <: ReferenceType] = values.head.upperTypeBound

        def currentUTBisUTBForArrays: Boolean =
            overallUTB.isSingletonSet && overallUTB.head.isArrayType

        def asUTBForArrays: ArrayType = overallUTB.head.asArrayType
        def asUTBForObjects: UIDSet[ObjectType] = overallUTB.asInstanceOf[UIDSet[ObjectType]]

        values.tail foreach { value =>
            overallUTB = value match {

                case SObjectValueLike(nextUTB) =>
                    if (currentUTBisUTBForArrays)
                        classHierarchy.joinAnyArrayTypeWithObjectType(nextUTB)
                    else
                        classHierarchy.joinObjectTypes(nextUTB, asUTBForObjects, true)

                case MObjectValueLike(nextUTB) =>
                    if (currentUTBisUTBForArrays)
                        classHierarchy.joinAnyArrayTypeWithMultipleTypesBound(nextUTB)
                    else
                        classHierarchy.joinUpperTypeBounds(asUTBForObjects, nextUTB, true)

                case AnArrayValue(nextUTB) =>
                    if (currentUTBisUTBForArrays)
                        classHierarchy.joinArrayTypes(asUTBForArrays, nextUTB) match {
                            case Left(arrayType)       => UIDSet(arrayType)
                            case Right(upperTypeBound) => upperTypeBound.asInstanceOf[UIDSet[ReferenceType]]
                        }
                    else
                        classHierarchy.joinAnyArrayTypeWithMultipleTypesBound(asUTBForObjects)

                case _: NullValue => /*"Do Nothing"*/ overallUTB
            }
        }
        overallUTB
    }

    /**
     * Representation of some reference value; this includes `Object`, `Array` and `Null` values.
     *
     * This trait defines the additional methods needed for the refinement of the new
     * properties.
     */
    trait TheReferenceValue extends super.ReferenceValueLike { this: AReferenceValue =>

        /**
         * Returns the reference id of this object. I.e., an approximation of the object's
         * identity.
         */
        def refId: RefId

        /**
         * Refines this value's `isNull` property.
         *
         * ==Precondition==
         * This method is only defined if a previous `isNull` test
         * returned `Unknown` and we are now on the branch where we know that the value is
         * now null or is not null.
         *
         * @param pc The program counter of the instruction that was the reason
         *      for the refinement.
         * @param isNull This value's new null-ness property. `isNull` either
         *      has to be `Yes` or `No`. The refinement to `Unknown` neither makes
         *      sense nor is it supported.
         * @return The updated operand stack and register values.
         */
        def refineIsNull(
            pc:       Int,
            isNull:   Answer,
            operands: Operands,
            locals:   Locals
        ): (Operands, Locals)

        /**
         * Refines the upper bound of this value's type to the given supertype.
         *
         * ==Precondition==
         * This method is only to be called if a previous "subtype of" test
         * (`this.isValueASubtypeOf(supertype)`)
         * returned `Unknown` and we are now on the branch where the value has to be of
         * the respective type. '''Hence, this method only handles the case where
         * supertype is more strict than this type's upper type bound.'''
         *
         * @return The updated operand stack and register values.
         */
        def refineUpperTypeBound(
            pc:        Int,
            supertype: ReferenceType,
            operands:  Operands,
            locals:    Locals
        ): (Operands, Locals)

        /**
         * Returns `true` - and updates the refinements map - if this value was refined
         * because it depended on a value that was already refined.
         *
         * @note   The refinements map must not contain `this` value as a key.
         *         The template method [[doPropagateRefinement]] already applies all
         *         standard refinements.
         *
         * @return `true` if a refinement was added to the refinements map.
         */
        protected def refineIf(refinements: Refinements): Boolean

        protected[this] final def doPropagateRefinement(
            refinements: Refinements,
            operands:    Operands,
            locals:      Locals
        ): (Operands, Locals) = {

            // We have to perform a fixpoint computation as one refinement can
            // lead to another refinement that can lead to yet another refinement
            // that...
            // In this case, whenever a new refinement is added to the list of
            // refinements the whole propagation process is restarted.

            /* Returns the refined value unless no refinement exists. In this case the given value
             * is returned.  */
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
                operands mapConserve {
                    case AReferenceValueTag(op) =>
                        val newOp = refine(op)
                        if (newOp.refineIf(refinements))
                            // RESTART REFINEMENT PROCESS!
                            return doPropagateRefinement(refinements, operands, locals);
                        newOp
                    case op =>
                        op
                },
                // REGISTERS
                locals mapConserve {
                    case AReferenceValueTag(l) =>
                        val newL = refine(l)
                        if (newL.refineIf(refinements))
                            // RESTART REFINEMENT PROCESS!
                            return doPropagateRefinement(refinements, operands, locals);
                        newL
                    case l =>
                        l
                }
            )
        }

        /**
         * Propagate some refinement of the value's properties.
         */
        protected[this] final def propagateRefinement(
            oldValue: AReferenceValue, newValue: AReferenceValue,
            operands: Operands, locals: Locals
        ): (Operands, Locals) = {
            assert(oldValue ne newValue)

            val refinements = new Refinements()
            refinements.put(oldValue, newValue)
            doPropagateRefinement(refinements, operands, locals)
        }

        /* TODO XXXX FIXME Implement! */
        def abstractOverMutableState(): AReferenceValue = ???
    }

    /**
     * Represents all `DomainReferenceValue`s that represent a reference value where
     * – in the current analysis context – the value has a single origin.
     *
     * @note To make it possible to store `SingleOriginReferenceValue`s in UIDSets -
     *       which in particular provide fast `filter` and `tail` methods compared to the
     *       standard sets - the UID trait is implemented.
     */
    trait SingleOriginReferenceValue
        extends TheReferenceValue
        with SingleOriginValue
        with UID {
        this: DomainSingleOriginReferenceValue =>

        final override def id: Int = origin

        /**
         * Updates the `origin` and/or `isNull` property; keeps the reference id.
         */
        final def update(
            origin: ValueOrigin = this.origin,
            isNull: Answer      = this.isNull
        ): DomainSingleOriginReferenceValue = {
            updateRefId(this.refId, origin, isNull)
        }

        /**
         * Creates a new instance of this object where the reference id is set to the
         * given reference id `refId`. Optionally, it is also possible to update the `origin`
         * and `isNull` information.
         *
         * @example A typical usage:
         *  {{{
         *  val v : SingleOriginReferenceValue = ???
         *  val newV = v.updateRefId(nextRefId(), isNull = Unknown)
         *  }}}
         */
        /*ABSTRACT*/ def updateRefId(
            refId:  RefId,
            origin: ValueOrigin = this.origin,
            isNull: Answer      = this.isNull
        ): DomainSingleOriginReferenceValue

        protected def refineIf(refinements: Refinements): Boolean = false

        final def refineIsNull(
            pc:       Int,
            isNull:   Answer,
            operands: Operands,
            locals:   Locals
        ): (Operands, Locals) = {
            assert(this.isNull.isUnknown)
            assert( /*parameter*/ isNull.isYesOrNo)

            val refinedValue = doRefineIsNull(isNull)
            propagateRefinement(this, refinedValue, operands, locals)
        }

        def doRefineIsNull(isNull: Answer): DomainSingleOriginReferenceValue

        final def refineUpperTypeBound(
            pc:        Int,
            supertype: ReferenceType,
            operands:  Operands,
            locals:    Locals
        ): (Operands, Locals) = {

            val refinedValue = doRefineUpperTypeBound(supertype)
            propagateRefinement(this, refinedValue, operands, locals)
        }

        def doRefineUpperTypeBound(supertype: ReferenceType): DomainSingleOriginReferenceValue

        def doRefineUpperTypeBound(
            supertypes: UIDSet[_ <: ReferenceType]
        ): DomainSingleOriginReferenceValue = {
            assert(supertypes.nonEmpty)

            if (supertypes.isSingletonSet) {
                doRefineUpperTypeBound(supertypes.head)
            } else {
                val newSupertypes = supertypes.asInstanceOf[UIDSet[ObjectType]]
                ObjectValue(this.origin, this.isNull, newSupertypes, this.refId)
            }
        }

        /*ABSTRACT*/ protected def doJoinWithNonNullValueWithSameOrigin(
            pc:   Int,
            that: DomainSingleOriginReferenceValue
        ): Update[DomainSingleOriginReferenceValue]

        protected def doJoinWithMultipleReferenceValues(
            pc:    Int,
            other: DomainMultipleReferenceValues
        ): StructuralUpdate[DomainMultipleReferenceValues] = {

            // Invariant:
            // At most one value represented by MultipleReferenceValues
            // has the same origin as this value.
            other.values find { that => this.origin == that.origin } match {
                case None => StructuralUpdate(other.addValue(this))
                case Some(that) =>
                    if (this eq that) {
                        // <=> this value is part of the other "MultipleReferenceValues",
                        // however the MultipleReferenceValues (as a whole) may need
                        // to be updated if it was refined in the meantime!
                        val mrv = other.update(other.values, valuesUpdated = false, this, other.refId)
                        StructuralUpdate(mrv)

                    } else {
                        // This value has the the same origin as a value found in
                        // MultipleReferenceValues.
                        val key = IdentityPair(this, that)
                        val joinResult = joinedValues.computeIfAbsent(key, _ => this.join(pc, that))

                        if (joinResult.isNoUpdate)
                            StructuralUpdate(other.rejoinValue(that, this, this))
                        else if (joinResult.value eq that) {
                            // Though the referenced value does not need to be updated,
                            // (this join that (<=> joinResult) => that)
                            // the MultipleReferenceValues (as a whole) may still need
                            // to be updated (to relax some constraints)
                            val newRefId = if (that.refId == this.refId) other.refId else nextRefId()
                            val mrv = other.update(other.values, valuesUpdated = false, this, newRefId)
                            StructuralUpdate(mrv)
                        } else {
                            val joinedValue =
                                joinResult.value.asInstanceOf[DomainSingleOriginReferenceValue]
                            StructuralUpdate(other.rejoinValue(that, this, joinedValue))
                        }
                    }
            }
        }

        final protected def doJoinWithNullValueWithSameOrigin(
            joinPC: Int,
            that:   DomainNullValue
        ): Update[DomainSingleOriginReferenceValue] = {
            this.isNull match {
                case Yes | Unknown => NoUpdate
                case No            => StructuralUpdate(this.update(isNull = Unknown))
            }
        }

        override protected def doJoin(
            joinPC: Int,
            other:  DomainValue
        ): Update[DomainValue] = {

            assert(this ne other)

            other match {
                case DomainSingleOriginReferenceValueTag(that) =>
                    if (this.origin == that.origin)
                        that match {
                            case DomainNullValueTag(that) =>
                                doJoinWithNullValueWithSameOrigin(joinPC, that)
                            case _ =>
                                doJoinWithNonNullValueWithSameOrigin(joinPC, that)
                        }
                    else {
                        val values = UIDSet2[DomainSingleOriginReferenceValue](this, that)
                        StructuralUpdate(MultipleReferenceValues(values))
                    }
                case DomainMultipleReferenceValuesTag(that) =>
                    doJoinWithMultipleReferenceValues(joinPC, that)
            }
        }
    }

    protected class NullValue(
            override val origin: ValueOrigin
    ) extends super.ANullValue with SingleOriginReferenceValue { this: DomainNullValue =>

        final def refId: RefId = domain.nullRefId

        /**
         * @inheritdoc
         *
         * @param isNull Has to be `Yes`.
         */
        override def updateRefId(
            refId:  RefId,
            origin: ValueOrigin = this.origin,
            isNull: Answer      = Yes
        ): DomainNullValue = {
            assert(refId == domain.nullRefId, "null value with unexpected reference id")
            assert(isNull.isYes, "a Null value's isNull property must be Yes")

            NullValue(origin)
        }

        override def doRefineIsNull(isNull: Answer): DomainSingleOriginReferenceValue = {
            throw ImpossibleRefinement(this, "nullness property of null value")
        }

        def doRefineUpperTypeBound(supertype: ReferenceType): DomainSingleOriginReferenceValue = {
            throw ImpossibleRefinement(this, "refinement of type of null value")
        }

        protected override def doJoinWithNonNullValueWithSameOrigin(
            joinPC: Int,
            that:   DomainSingleOriginReferenceValue
        ): StructuralUpdate[DomainSingleOriginReferenceValue] = {
            StructuralUpdate(
                // Basically, the reference id is not relevant in combination with definitive
                // null values. The other object, which has the reference id specified
                // by the other object may have been null.
                if (that.isNull.isUnknown)
                    that
                else
                    that.update(isNull = Unknown)
            )
        }

        override def abstractsOver(other: DomainValue): Boolean = {
            (this eq other) || (other match {
                case DomainReferenceValueTag(v) => v.isNull.isYes
                case _                          => false
            })
        }

        override def equals(other: Any): Boolean = {
            other match {
                case that: NullValue => that.origin == this.origin && (that canEqual this)
                case _               => false
            }
        }

        def canEqual(other: NullValue): Boolean = true

        override def hashCode: Int = origin

        override def toString = s"null[↦$origin]"
    }

    trait NonNullSingleOriginReferenceValue extends SingleOriginReferenceValue {
        this: DomainSingleOriginReferenceValue =>

        override def doRefineIsNull(isNull: Answer): DomainSingleOriginReferenceValue = {
            if (isNull.isYes) {
                NullValue(this.origin)
            } else {
                update(isNull = No)
            }
        }

        protected def doPeformJoinWithNonNullValueWithSameOrigin(
            that:     DomainSingleOriginReferenceValue,
            newRefId: RefId
        ): DomainSingleOriginReferenceValue

        override def doJoinWithNonNullValueWithSameOrigin(
            joinPC: Int,
            that:   DomainSingleOriginReferenceValue
        ): Update[DomainSingleOriginReferenceValue] = {

            if (this.refId == that.refId)
                return {
                    if (this.abstractsOver(that))
                        NoUpdate
                    else if (that.abstractsOver(this))
                        StructuralUpdate(that)
                    else
                        StructuralUpdate(doPeformJoinWithNonNullValueWithSameOrigin(that, this.refId))
                };

            // The reference ids are different...
            if (this == that)
                return RefIdUpdate(that);
            if (this.abstractsOver(that))
                return RefIdUpdate(this.updateRefId(that.refId));
            else if (that.abstractsOver(this))
                return StructuralUpdate(that); // StructuralUpdate(that.updateRefId());
            else
                return StructuralUpdate(doPeformJoinWithNonNullValueWithSameOrigin(that, that.refId));
        }

        def toString(upperTypeBound: String): String = {
            var description = upperTypeBound
            if (!isPrecise) description = "_ <: "+description
            if (isNull.isUnknown) description = s"{$description, null}"
            description += s"[↦$origin;refId=$refId]"
            description
        }
    }

    trait NonNullSingleOriginSReferenceValue[T <: ReferenceType]
        extends NonNullSingleOriginReferenceValue {
        this: DomainSingleOriginReferenceValue =>

        def theUpperTypeBound: T

        override def doPeformJoinWithNonNullValueWithSameOrigin(
            that:     DomainSingleOriginReferenceValue,
            newRefId: RefId
        ): DomainSingleOriginReferenceValue = {
            val thisUTB = this.theUpperTypeBound
            val thatUTB = that.upperTypeBound
            val newIsNull = this.isNull join that.isNull
            val newIsPrecise =
                this.isPrecise && that.isPrecise &&
                    thatUTB.isSingletonSet &&
                    (thisUTB eq thatUTB.head)
            val newUTB = classHierarchy.joinReferenceType(thisUTB, thatUTB.toUIDSet)
            ReferenceValue(origin, newIsNull, newIsPrecise, newUTB, newRefId)
        }
    }

    protected trait ArrayValue
        extends super.AnArrayValue
        with NonNullSingleOriginSReferenceValue[ArrayType] {
        this: DomainArrayValue =>

        assert(isNull.isNoOrUnknown)
        assert(isPrecise || !classHierarchy.isKnownToBeFinal(theUpperTypeBound))

        override def updateRefId(
            refId:  RefId,
            origin: ValueOrigin,
            isNull: Answer
        ): DomainArrayValue = {
            ArrayValue(origin, isNull, isPrecise, theUpperTypeBound, refId)
        }

        def doRefineUpperTypeBound(supertype: ReferenceType): DomainSingleOriginReferenceValue = {
            // Please note, that an "assert(!isPrecise)" is not possible, it may be the
            // case that a developer performs a deliberate "upcast" of a type, but that
            // the type hierarchy is not known to OPAL. In this case, we will definitively
            // perform the upcast - even if the type is precise.

            ArrayValue(origin, isNull, isPrecise = false, supertype.asArrayType, refId)
        }

        override def abstractsOver(other: DomainValue): Boolean = {
            if (this eq other)
                return true;

            val that = domain.asReferenceValue(other)

            if (this.isNull.isUnknown && that.isNull.isYes)
                return true;

            val result =
                (this.isNull.isUnknown || that.isNull.isNo) &&
                    (!this.isPrecise || that.isPrecise) && {
                        val thatUTB = that.upperTypeBound
                        thatUTB.isSingletonSet &&
                            thatUTB.head.isArrayType &&
                            isSubtypeOf(thatUTB.head.asArrayType, this.theUpperTypeBound)
                    }
            result
        }

        override def adapt(
            targetDomain: TargetDomain,
            targetOrigin: ValueOrigin
        ): targetDomain.DomainValue = {
            val adaptedValue = targetDomain match {

                case thatDomain: l1.ReferenceValues =>
                    val thatT = thatDomain.nextRefId()
                    thatDomain.ArrayValue(targetOrigin, isNull, isPrecise, theUpperTypeBound, thatT)

                case thatDomain: l0.DefaultTypeLevelReferenceValues =>
                    thatDomain.ReferenceValue(targetOrigin, theUpperTypeBound)

                case _ =>
                    super.adapt(targetDomain, targetOrigin)
            }
            adaptedValue.asInstanceOf[targetDomain.DomainValue]
        }

        protected def canEqual(other: ArrayValue): Boolean = true

        override def equals(other: Any): Boolean = {
            other match {
                case that: ArrayValue =>
                    (that eq this) ||
                        (
                            (that canEqual this) &&
                            this.origin == that.origin &&
                            this.isPrecise == that.isPrecise &&
                            this.isNull == that.isNull &&
                            (this.upperTypeBound eq that.upperTypeBound)
                        )
                case _ =>
                    false
            }
        }

        override def hashCode: Int = {
            ((origin * 41 +
                (if (isPrecise) 101 else 3)) * 13 +
                isNull.hashCode()) * 79 +
                upperTypeBound.hashCode()
        }

        override def toString: String = toString(theUpperTypeBound.toJava)

    }

    trait ObjectValue extends super.AnObjectValue with NonNullSingleOriginReferenceValue {
        this: DomainObjectValue =>
    }

    protected trait SObjectValue
        extends super.SObjectValueLike
        with NonNullSingleOriginSReferenceValue[ObjectType]
        with ObjectValue {
        this: DomainObjectValue =>

        assert(this.isNull.isNoOrUnknown)
        assert(!classHierarchy.isKnownToBeFinal(theUpperTypeBound) || isPrecise)
        assert(
            !isPrecise ||
                !classHierarchy.isKnown(theUpperTypeBound) ||
                classHierarchy.isInterface(theUpperTypeBound).isNo,
            s"the type ${theUpperTypeBound.toJava} defines an interface and, "+
                "hence, cannnot be the concrete(precise) type of an object instance "+
                "(if this assertion fails, the project configuration may be bogus))"
        )

        override def updateRefId(
            refId:  RefId,
            origin: ValueOrigin,
            isNull: Answer
        ): DomainObjectValue = {
            ObjectValue(origin, isNull, isPrecise, theUpperTypeBound, refId)
        }

        def doRefineUpperTypeBound(supertype: ReferenceType): DomainSingleOriginReferenceValue = {
            val thisUTB = this.theUpperTypeBound

            assert(thisUTB ne supertype)
            assert(
                !isPrecise || !domain.isSubtypeOf(supertype, thisUTB),
                s"this type is precise ${thisUTB.toJava}; "+
                    s"refinement goal: ${supertype.toJava} "+
                    "(is this type a subtype of the given type: "+
                    s"${domain.isSubtypeOf(thisUTB, supertype)})"
            )

            if (domain.isSubtypeOf(supertype, thisUTB)) {
                // this also handles the case where we cast an object to an array
                ReferenceValue(this.origin, this.isNull, false, supertype)
            } else {
                // this handles both cases:
                // Unknown => we just add it as another type bound (in this case
                //        the type bound may contain redundant information w.r.t.
                //        the overall type hierarchy)
                // No => we add it as another type bound
                if (supertype.isArrayType) {
                    val message = s"incompatible refinement ${thisUTB.toJava} => ${supertype.toJava}"
                    throw ImpossibleRefinement(this, message)
                }

                // basically, we are adding another type bound
                val newUTB = UIDSet(supertype.asObjectType, thisUTB)
                ObjectValue(this.origin, this.isNull, newUTB, refId)
            }
        }

        override def abstractsOver(other: DomainValue): Boolean = {
            if (this eq other)
                return true;

            def checkPrecisionAndNullness(that: TheReferenceValue): Boolean = {
                (!this.isPrecise || that.isPrecise) &&
                    (this.isNull.isUnknown || that.isNull.isNo)
            }

            other match {

                case that: SObjectValue =>
                    checkPrecisionAndNullness(that) &&
                        isSubtypeOf(that.theUpperTypeBound, this.theUpperTypeBound)

                case that: ArrayValue =>
                    checkPrecisionAndNullness(that) &&
                        isSubtypeOf(that.theUpperTypeBound, this.theUpperTypeBound)

                case that: MultipleReferenceValues =>
                    checkPrecisionAndNullness(that) &&
                        classHierarchy.isSubtypeOf(that.upperTypeBound, theUpperTypeBound)

                case that: MObjectValue =>
                    val thisUTB = this.theUpperTypeBound
                    val thatUTB = that.upperTypeBound.asInstanceOf[UIDSet[ReferenceType]]
                    checkPrecisionAndNullness(that) &&
                        classHierarchy.isSubtypeOf(thatUTB, thisUTB)

                case _: NullValue => this.isNull.isUnknown

                case _            => false
            }
        }

        override def adapt(target: TargetDomain, pc: Int): target.DomainValue = {
            val adaptedValue = target match {
                case thatDomain: l1.ReferenceValues =>
                    val thatT = thatDomain.nextRefId()
                    thatDomain.ObjectValue(pc, isNull, isPrecise, theUpperTypeBound, thatT)

                case thatDomain: l0.DefaultTypeLevelReferenceValues =>
                    thatDomain.ReferenceValue(pc, theUpperTypeBound)

                case _ =>
                    super.adapt(target, pc)
            }
            adaptedValue.asInstanceOf[target.DomainValue]
        }

        override def equals(other: Any): Boolean = {
            other match {
                case that: SObjectValue =>
                    (that eq this) ||
                        (
                            (that canEqual this) &&
                            this.origin == that.origin &&
                            this.isPrecise == that.isPrecise &&
                            this.isNull == that.isNull &&
                            (this.theUpperTypeBound eq that.theUpperTypeBound)
                        )

                case _ => false
            }
        }

        protected def canEqual(other: SObjectValue): Boolean = true

        override def hashCode: Int = {
            ((theUpperTypeBound.hashCode * 41 +
                (if (isPrecise) 11 else 101)) * 13 +
                isNull.hashCode()) * 79 +
                origin
        }

        override def toString: String = toString(theUpperTypeBound.toJava)
    }

    protected trait MObjectValue extends super.MObjectValueLike with ObjectValue {
        value: DomainObjectValue =>

        /**
         * If we have an incomplete type hierarchy it may happen that we ceate an
         * MObjectValue where the types of the bound are in a sub-/supertype relation to ensure
         * that after a checkcast a corresponding test will return Yes and not Unknown.
         * In that case – and if one of the types is final – we can nevertheless determine that we
         * know the precise type of the value.
         */
        override def isPrecise: Boolean = {
            upperTypeBound.exists(classHierarchy.isKnownToBeFinal)
        }

        override def updateRefId(
            refId:  RefId,
            origin: ValueOrigin,
            isNull: Answer
        ): DomainObjectValue = {
            ObjectValue(origin, isNull, upperTypeBound, refId)
        }

        def doRefineUpperTypeBound(supertype: ReferenceType): DomainSingleOriginReferenceValue = {
            if (supertype.isObjectType) {
                val theSupertype = supertype.asObjectType
                var newUTB: UIDSet[ObjectType] = UIDSet.empty
                upperTypeBound foreach { (anUTB: ObjectType) =>
                    if (domain.isSubtypeOf(supertype, anUTB))
                        newUTB += theSupertype
                    else {
                        // supertype is either a supertype of anUTB or the
                        // the relationship is unknown; in both cases
                        // we have to keep "anUTB"; however, we also have
                        // to add supertype if the relation is unknown.
                        newUTB += anUTB
                        if (domain.isASubtypeOf(anUTB, supertype).isUnknown)
                            newUTB += theSupertype
                    }
                }
                if (newUTB.isSingletonSet) {
                    ObjectValue(origin, isNull, isPrecise = false, newUTB.head, refId)
                } else {
                    ObjectValue(origin, isNull, newUTB + theSupertype, refId)
                }
            } else {
                /* The supertype is an array type; this implies that this MObjectValue
                 * models the upper type bound "Serializable & Cloneable"; otherwise
                 * the refinement is illegal
                 */
                assert(upperTypeBound == ObjectType.SerializableAndCloneable)
                ArrayValue(origin, isNull, isPrecise = false, supertype.asArrayType, refId)
            }
        }

        def doPeformJoinWithNonNullValueWithSameOrigin(
            that:     DomainSingleOriginReferenceValue,
            newRefId: RefId
        ): DomainSingleOriginReferenceValue = {
            val thisUTB: UIDSet[_ <: ReferenceType] = this.upperTypeBound
            val thatUTB: UIDSet[_ <: ReferenceType] = that.upperTypeBound
            val newIsNull = this.isNull join that.isNull
            val newIsPrecise = this.isPrecise && that.isPrecise && thisUTB == thatUTB
            val newUTB = classHierarchy.joinReferenceTypes(thisUTB, thatUTB)
            ReferenceValue(origin, newIsNull, newIsPrecise, newUTB, newRefId)
        }

        override def abstractsOver(other: DomainValue): Boolean = {
            if (this eq other)
                return true;

            val DomainReferenceValueTag(that) = other

            if (this.isNull.isNo && that.isNull.isYesOrUnknown)
                return false;

            val thatUTB = that.upperTypeBound
            classHierarchy.isSubtypeOf(thatUTB, this.upperTypeBound)
        }

        override def adapt(target: TargetDomain, origin: ValueOrigin): target.DomainValue =
            target match {
                case td: ReferenceValues =>
                    td.ObjectValue(origin, isNull, this.upperTypeBound, td.nextRefId()).
                        asInstanceOf[target.DomainValue]

                case td: l0.DefaultTypeLevelReferenceValues =>
                    td.ObjectValue(origin, this.upperTypeBound).
                        asInstanceOf[target.DomainValue]

                case _ => super.adapt(target, origin)
            }

        override def equals(other: Any): Boolean = {
            other match {
                case that: MObjectValue =>
                    (this eq that) || (
                        this.origin == that.origin &&
                        this.isNull == that.isNull &&
                        (this canEqual that) &&
                        (this.upperTypeBound == that.upperTypeBound)
                    )
                case _ => false
            }
        }

        protected def canEqual(other: MObjectValue): Boolean = true

        override lazy val hashCode: Int = {
            ((upperTypeBound.hashCode * 41 +
                (if (isPrecise) 11 else 101)) * 13 +
                isNull.hashCode()) * 79 +
                origin
        }

        override def toString: String = {
            toString(upperTypeBound.map(_.toJava).mkString(" with "))
        }
    }

    /**
     * A `MultipleReferenceValues` tracks multiple reference values (of type `NullValue`,
     * `ArrayValue`, `SObjectValue` and `MObjectValue`) that have different
     * origins. I.e., per value origin one domain value is used
     * to abstract over the properties of that respective value.
     *
     * @param   isPrecise `true` if the upper type bound of this value precisely
     *          captures the runtime type of the value.
     *          This basically requires that all '''non-null''' values
     *          are precise and have the same upper type bound. Null values are ignored.
     *          Please note, that the type bound can still have multiple types as long
     *          as one type is final and can be assumed to be the subtype of all other
     *          types!
     */
    protected class MultipleReferenceValues(
            val values:             UIDSet[DomainSingleOriginReferenceValue],
            val origins:            ValueOrigins,
            override val isNull:    Answer,
            override val isPrecise: Boolean,
            val upperTypeBound:     UIDSet[_ <: ReferenceType],
            override val refId:     RefId
    ) extends IsMultipleReferenceValue
        with TheReferenceValue
        with MultipleOriginsValue {
        this: DomainMultipleReferenceValues =>

        def this(values: UIDSet[DomainSingleOriginReferenceValue]) =
            this(
                values,
                values.idSet,
                domain.isNull(values),
                domain.isPrecise(values),
                domain.upperTypeBound(values),
                nextRefId()
            )

        assert(values.size > 1, "a MultipleReferenceValue must have multiple values")
        assert(
            isNull.isNoOrUnknown || values.forall(_.isNull.isYesOrUnknown),
            s"inconsistent null property(isNull == $isNull): ${values.mkString(",")}"
        )
        assert(
            (isNull.isYes && isPrecise) || {
                val nonNullValues = values.filter(_.isNull.isNoOrUnknown)
                (nonNullValues.isEmpty && isPrecise) || (
                    nonNullValues.nonEmpty && (
                        nonNullValues.exists(!_.isPrecise) || domain.isPrecise(values) == isPrecise
                    )
                )
            },
            s"unexpected precision (precise == $isPrecise): $this"
        )
        assert(
            !upperTypeBound.isSingletonSet || (
                !classHierarchy.isKnownToBeFinal(upperTypeBound.head) || isPrecise
            ),
            s"isPrecise has to be true if the upper type bound belongs to final type $upperTypeBound"
        )
        assert(
            (isNull.isYes && upperTypeBound.isEmpty) || (
                isNull.isNoOrUnknown &&
                upperTypeBound.nonEmpty && (
                    domain.upperTypeBound(values) == upperTypeBound ||
                    !classHierarchy.isSubtypeOf(
                        domain.upperTypeBound(values),
                        upperTypeBound
                    )
                )
            ),
            s"the upper type bound (isNull == $isNull) of ${values.mkString(",")} "+
                s"== ${domain.upperTypeBound(values)} which is a strict subtype of "+
                s"the given bound $upperTypeBound"
        )
        assert(
            upperTypeBound.size < 2 || upperTypeBound.forall(_.isObjectType),
            s"invalid upper type bound: $upperTypeBound for: ${values.mkString("[", ";", "]")}"
        )

        override def leastUpperType: Option[ReferenceType] = {
            if (isNull.isYes)
                None
            else
                Some(classHierarchy.joinReferenceTypesUntilSingleUpperBound(upperTypeBound))
        }

        final override def isArrayValue: Answer = {
            // Please recall that the upperTypeBound contains exactly one value if the value
            // may be an ArrayValue. (There are no union types related to array values.)
            isNull match {
                case Yes     => No
                case Unknown => if (upperTypeBound.head.isArrayType) Unknown else No
                case No      => if (upperTypeBound.head.isArrayType) Yes else No
            }
        }

        override def baseValues: Iterable[DomainSingleOriginReferenceValue] = values

        override def allValues: Iterable[DomainSingleOriginReferenceValue] = values

        def addValue(newValue: DomainSingleOriginReferenceValue): DomainMultipleReferenceValues = {

            assert(!values.exists(_.origin == newValue.origin))

            val thisUTB = this.upperTypeBound
            val newValueUTB = newValue.upperTypeBound
            val joinedUTB = classHierarchy.joinUpperTypeBounds(thisUTB, newValueUTB)
            val newIsNull = this.isNull join newValue.isNull
            MultipleReferenceValues(
                this.values + newValue,
                this.origins + newValue.origin,
                newIsNull,
                this.isPrecise && newValue.isPrecise &&
                    (
                        effectiveUTB(this.upperTypeBound) == effectiveUTB(newValue.upperTypeBound) ||
                        this.upperTypeBound.isEmpty ||
                        newValue.upperTypeBound.isEmpty
                    ),
                joinedUTB,
                nextRefId()
            )
        }

        def rejoinValue(
            oldValue:    DomainSingleOriginReferenceValue,
            joinValue:   DomainSingleOriginReferenceValue,
            joinedValue: DomainSingleOriginReferenceValue
        ): DomainMultipleReferenceValues = {

            assert(oldValue ne joinValue)
            assert(oldValue ne joinedValue)
            assert(oldValue.origin == joinValue.origin)
            assert(oldValue.origin == joinedValue.origin)

            assert(values.exists(_ eq oldValue))

            val newValues = this.values - oldValue + joinedValue
            val newRefId = if (oldValue.refId == joinedValue.refId) this.refId else nextRefId()
            update(newValues, valuesUpdated = true, joinValue, newRefId)
        }

        protected[ReferenceValues] def update(
            newValues:     UIDSet[DomainSingleOriginReferenceValue],
            valuesUpdated: Boolean,
            joinedValue:   DomainSingleOriginReferenceValue,
            newRefId:      RefId
        ): DomainMultipleReferenceValues = {

            val newIsNull = {
                val newIsNull = domain.isNull(newValues)
                if (newIsNull.isUnknown)
                    this.isNull join joinedValue.isNull
                else
                    newIsNull
            }

            val newUTB =
                if (newIsNull.isYes)
                    UIDSet.empty[ReferenceType]
                else {
                    val newValuesUTB = domain.upperTypeBound(newValues)
                    val baseUTB =
                        classHierarchy.joinUpperTypeBounds(
                            this.upperTypeBound, joinedValue.upperTypeBound
                        )
                    if (newValuesUTB != baseUTB &&
                        classHierarchy.isSubtypeOf(newValuesUTB, baseUTB))
                        newValuesUTB
                    else
                        baseUTB
                }

            val newIsPrecise =
                newIsNull.isYes || domain.isPrecise(newValues) ||
                    (newUTB.isSingletonSet && classHierarchy.isKnownToBeFinal(newUTB.head))

            if (!valuesUpdated &&
                newRefId == this.refId &&
                newIsNull == this.isNull &&
                newIsPrecise == this.isPrecise &&
                newUTB == this.upperTypeBound)
                this
            else
                MultipleReferenceValues(
                    newValues,
                    /*OLD!*/ origins,
                    newIsNull,
                    newIsPrecise,
                    newUTB,
                    newRefId
                )
        }

        override def originsIterator: IntIterator = values.idIterator

        /**
         * Summarizes this value by creating a new domain value that abstracts over
         * the properties of all values.
         *
         * The given `pc` is used as the program counter of the newly created value.
         */
        override def summarize(pc: Int): DomainReferenceValue = {
            upperTypeBound /*<= basically creates the summary*/ match {
                case EmptyUpperTypeBound => NullValue(pc)
                case UIDSet1(referenceType) =>
                    ReferenceValue(pc, isNull, isPrecise, referenceType, refId)
                case utb =>
                    // We have an UpperTypeBound that has multiple types. Such bounds
                    // cannot contain array types.
                    ObjectValue(pc, isNull, utb.asInstanceOf[UIDSet[ObjectType]], refId)
            }
        }

        override def adapt(target: TargetDomain, pc: Int): target.DomainValue = {
            summarize(pc).adapt(target, pc)
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

            // this value (as a whole) was not previously refined
            val thisIsNull = this.isNull
            var refined = false
            // [CONCEPTUALLY] var refinedValues = SortedSet.empty[DomainSingleOriginReferenceValue]
            // we can use a buffer here, since the refinement will not change
            // the origin of a value
            val thisValues = this.values
            var refinedValues = UIDSet.empty[DomainSingleOriginReferenceValue]
            thisValues.foreach { value =>
                val refinedValue = refinements.get(value)
                // INVARIANT: refinedValue ne value
                if (refinedValue == null) {
                    refinedValues += value
                } else {

                    refined = true

                    // we now have to check if the refined value can still be
                    // part of the "MultipleReferenceValues"
                    if ((thisIsNull.isUnknown || thisIsNull == refinedValue.isNull) &&
                        (!refinedValue.isPrecise ||
                            classHierarchy.isASubtypeOf(
                                refinedValue.upperTypeBound, upperTypeBound
                            ).isYesOrUnknown)) {

                        val refinedSingleOriginValue =
                            refinedValue.asInstanceOf[DomainSingleOriginReferenceValue]

                        assert(refinedSingleOriginValue.origin == value.origin)

                        refinedValues += refinedSingleOriginValue
                    }
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
                    classHierarchy.isSubtypeOf(thisUpperTypeBound, refinedValue.upperTypeBound)) {
                    if (thisUpperTypeBound.isSingletonSet)
                        refinedValue =
                            refinedValue.
                                doRefineUpperTypeBound(thisUpperTypeBound.head).
                                asInstanceOf[DomainSingleOriginReferenceValue]
                    else
                        refinedValue =
                            ObjectValue(
                                refinedValue.origin,
                                refinedValue.isNull,
                                thisUpperTypeBound.asInstanceOf[UIDSet[ObjectType]],
                                refinedValue.refId
                            )
                }

                refinements.put(this, refinedValue)
                if (remainingValue ne refinedValue)
                    refinements.put(remainingValue, refinedValue)

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
                        val newRefinedValuesUTB =
                            domain.upperTypeBound(refinedValues)
                        if (newRefinedValuesUTB != upperTypeBound &&
                            classHierarchy.isSubtypeOf(newRefinedValuesUTB, upperTypeBound))
                            newRefinedValuesUTB
                        else
                            upperTypeBound
                    }

                refinements.put(
                    this,
                    MultipleReferenceValues(
                        refinedValues,
                        origins,
                        newIsNull,
                        isPrecise || domain.isPrecise(refinedValues),
                        newUTB,
                        nextRefId()
                    )
                )
            }

            true
        }

        protected[this] def refineToValue(
            value:              DomainSingleOriginReferenceValue,
            isNullGoal:         Answer,
            upperTypeBoundGoal: UIDSet[_ <: ReferenceType],
            operands:           Operands, locals: Locals
        ): (Operands, Locals) = {

            var newValue = value
            val upperBoundGoal = upperTypeBoundGoal.asInstanceOf[UIDSet[ReferenceType]]

            if (isNullGoal.isYesOrNo && newValue.isNull != isNullGoal) {
                newValue = newValue.doRefineIsNull(isNullGoal)
            }

            if (newValue.isNull.isNoOrUnknown) {
                val newValueUTB = newValue.upperTypeBound.asInstanceOf[UIDSet[ReferenceType]]
                if (upperBoundGoal != newValueUTB) {
                    // ALSO have to handle the case where upperTypeBoundGoal and
                    // newValueUTB are NOT in an inheritance relationship!
                    val goalIsASubtype = classHierarchy.isASubtypeOf(upperBoundGoal, newValueUTB)
                    if (goalIsASubtype.isYes)
                        newValue = newValue.doRefineUpperTypeBound(upperBoundGoal)
                    else if (goalIsASubtype.isUnknown)
                        newValue = newValue.doRefineUpperTypeBound(upperBoundGoal ++ newValueUTB)
                    else if (!classHierarchy.isSubtypeOf(newValueUTB, upperBoundGoal))
                        newValue = newValue.doRefineUpperTypeBound(upperBoundGoal ++ newValueUTB)
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
            pc:       Int,
            isNull:   Answer,
            operands: Operands, locals: Locals
        ): (Operands, Locals) = {

            assert(this.isNull.isUnknown)
            assert(isNull.isYesOrNo)

            // Recall that this value's property – as a whole – can be undefined also
            // each individual value's property is well defined (Yes, No).
            // Furthermore, the parameter isNull is either Yes or No and we are
            // going to filter those values that do not satisfy the constraint.

            val newValues = values.filter(v => v.isNull == isNull || v.isNull.isUnknown)

            if (newValues.isSingletonSet) {
                refineToValue(newValues.head, isNull, this.upperTypeBound, operands, locals)
            } else {
                val newRefId = if (newValues.size == values.size) refId else nextRefId()
                val newValuesUTB = domain.upperTypeBound(newValues)
                // we have to choose the more "precise" utb
                val newValue =
                    if (isNull.isYes)
                        MultipleReferenceValues(
                            newValues,
                            origins,
                            Yes, // we refined the "isNull" property!
                            true, // all values are null...
                            UIDSet.empty[ReferenceType],
                            newRefId
                        )
                    else {
                        val thisUTB = this.upperTypeBound
                        val newUTB =
                            if (classHierarchy.isASubtypeOf(thisUTB, newValuesUTB).isYesOrUnknown)
                                thisUTB
                            else
                                newValuesUTB
                        val newIsPrecise =
                            (newUTB.isSingletonSet && classHierarchy.isKnownToBeFinal(newUTB.head)) ||
                                domain.isPrecise(newValues)
                        MultipleReferenceValues(
                            newValues,
                            origins,
                            No, // we refined the "isNull" property!
                            newIsPrecise,
                            newUTB,
                            newRefId
                        )
                    }
                propagateRefinement(this, newValue, operands, locals)
            }
        }

        override def refineUpperTypeBound(
            pc:        Int,
            supertype: ReferenceType,
            operands:  Operands,
            locals:    Locals
        ): (Operands, Locals) = {

            // Let's keep all values with a type that is a potential subtype of the
            // given supertype.
            var filteredValuesOrigins = origins
            val filteredValues =
                this.values filter { value =>
                    value.isNull.isYes || {
                        value.isValueASubtypeOf(supertype) match {
                            case Yes | Unknown => true
                            case No            => { filteredValuesOrigins -= value.id; false }
                        }
                    }
                }

            if (filteredValues.isSingletonSet) {
                refineToValue(filteredValues.head, this.isNull, UIDSet(supertype), operands, locals)
            } else {
                // There are no individual values to refine -
                // we have to choose the more "precise" utb.
                val filteredValuesUTB = domain.upperTypeBound(filteredValues)

                // We have to support (1) the case where we cast a value for which we only have
                // an interface as an upper type bound to a second interface. In this case
                // the value has to be a subtype of both interfaces.
                //
                // We have to support (2) the case where we cast an array of reference values of
                // type X to an array of reference values of type Y (which may succeed if at least
                // one type is an interface type or if both types are in an effective
                // inheritance relation! For example,
                // {{{
                // scala> val ss = new Array[java.util.ArrayList[AnyRef]](1)
                // ss: Array[java.util.ArrayList[AnyRef]] = Array(null)
                //
                // scala> ss(0) = new java.util.ArrayList[AnyRef]()
                //
                // scala> val os : Array[Object] = ss.asInstanceOf[Array[Object]]
                // os: Array[Object] = Array([])
                //
                // scala> os.asInstanceOf[Array[java.io.Serializable]]
                // Array[java.io.Serializable] = Array([])
                // }}}
                // However, we currently have no support to model the case "Array of <X with Y>";
                // therefore, we simply accept the target refinement type.
                val supertypeUTB: UIDSet[_ <: ReferenceType] =
                    if (supertype.isObjectType &&
                        !classHierarchy.isSubtypeOf(supertype, this.upperTypeBound))
                        this.upperTypeBound add supertype
                    else
                        UIDSet[ReferenceType](supertype)
                val newUTB: UIDSet[_ <: ReferenceType] =
                    if (classHierarchy.isSubtypeOf(filteredValuesUTB, supertypeUTB))
                        filteredValuesUTB
                    else
                        supertypeUTB
                val newRefId = if (filteredValues.size == values.size) refId else nextRefId()
                val newIsPrecise = {
                    (newUTB.isSingletonSet && classHierarchy.isKnownToBeFinal(newUTB.head)) ||
                        domain.isPrecise(filteredValues)
                }
                val newValue =
                    MultipleReferenceValues(
                        filteredValues,
                        filteredValuesOrigins,
                        if (isNull.isYesOrNo)
                            isNull
                        else
                            domain.isNull(filteredValues),
                        newIsPrecise,
                        newUTB,
                        newRefId
                    )
                propagateRefinement(this, newValue, operands, locals)
            }
        }

        /**
         * Join of a value (`thatValue`)  with a value (`thisValue`) referenced by this value.
         */
        protected[this] def doRejoinSingleOriginReferenceValue(
            joinPC:    Int,
            thisValue: DomainSingleOriginReferenceValue,
            thatValue: DomainSingleOriginReferenceValue
        ): Update[DomainValue] = {

            if (thisValue eq thatValue)
                return NoUpdate;

            // we may have seen the "inner join" previously, i.e.,
            // a join of thisValue with thatValue
            val joinKey = IdentityPair(thisValue, thatValue)
            val joinResult =
                joinedValues.computeIfAbsent(joinKey, _ => thisValue.join(joinPC, thatValue))

            joinResult match {
                case NoUpdate =>
                    var updateType: UpdateType = NoUpdateType
                    // though thisValue abstracts over the "joined" value
                    // we still have to check that this value (as a whole)
                    // also abstracts over `thatValue`
                    // E.g., consider the following case:
                    // given OneOf(null(origin=7;t=103),int[](origin=15;isNull=Unknown;t=887));lutb=;isPrecise=true;isNull=Yes
                    // join                             int[](origin=15;isNull=No;t=887)
                    // => As a whole "isNull" has to be Unknown
                    val thisUTB = this.upperTypeBound
                    val thatUTB = thatValue.upperTypeBound
                    val newIsNull = this.isNull join thatValue.isNull
                    val newUTB =
                        if (newIsNull.isYes) {
                            UIDSet.empty[ReferenceType]
                        } else {
                            classHierarchy.joinUpperTypeBounds(thisUTB, thatUTB)
                        }
                    if (newIsNull != this.isNull || newUTB != thisUTB) {
                        updateType = StructuralUpdateType
                    }
                    val newIsPrecise = this.isPrecise && thatValue.isPrecise && (
                        thisUTB.isEmpty || thatUTB.isEmpty || thisUTB == thatUTB
                    )
                    if (updateType != NoUpdateType) {
                        updateType(
                            MultipleReferenceValues(
                                this.values,
                                origins,
                                newIsNull,
                                newIsPrecise,
                                newUTB,
                                this.refId
                            )
                        )
                    } else
                        NoUpdate

                case update @ SomeUpdate(DomainSingleOriginReferenceValueTag(joinedValue)) =>
                    update.updateValue(rejoinValue(thisValue, thatValue, joinedValue))

                case MetaInformationUpdate(_) | StructuralUpdate(_) => throw new MatchError(joinResult)
            }
        }

        override protected def doJoin(joinPC: Int, other: DomainValue): Update[DomainValue] = {
            assert(this ne other)

            other match {

                case DomainSingleOriginReferenceValueTag(thatValue) =>
                    this.values.find(_.origin == thatValue.origin) match {
                        case Some(thisValue) =>
                            doRejoinSingleOriginReferenceValue(joinPC, thisValue, thatValue)
                        case None =>
                            StructuralUpdate(this.addValue(thatValue))
                    }

                case DomainMultipleReferenceValuesTag(that) =>
                    var updateType: UpdateType = NoUpdateType
                    var otherValues = that.values
                    var newValues = UIDSet.empty[DomainSingleOriginReferenceValue]
                    var newOrigins = that.origins
                    this.values foreach { thisValue =>
                        otherValues.findById(thisValue.origin) match {
                            case Some(otherValue) =>
                                otherValues -= otherValue
                                if (thisValue eq otherValue) {
                                    newValues += thisValue
                                } else {
                                    val joinResult =
                                        joinedValues.computeIfAbsent(
                                            new IdentityPair(thisValue, otherValue),
                                            _ => thisValue.join(joinPC, otherValue)
                                        )

                                    joinResult match {
                                        case NoUpdate =>
                                            newValues += thisValue
                                        case update @ SomeUpdate(DomainSingleOriginReferenceValueTag(otherValue)) =>
                                            updateType = updateType &: update
                                            newValues += otherValue
                                        case MetaInformationUpdate(_) | StructuralUpdate(_) => throw new MatchError(joinResult)
                                    }
                                }
                            case None =>
                                newValues += thisValue
                                newOrigins += thisValue.origin
                        }
                    }

                    if (otherValues.nonEmpty) {
                        newValues ++= otherValues
                        updateType = StructuralUpdateType
                    }
                    val thisUTB = this.upperTypeBound
                    val thatUTB = that.upperTypeBound
                    val newIsNull = domain.isNull(newValues).ifUnknown(this.isNull join that.isNull)
                    val newUTB =
                        if (newIsNull.isYes) {
                            UIDSet.empty[ReferenceType]
                        } else {
                            val baseUTB =
                                classHierarchy.joinUpperTypeBounds(thisUTB, thatUTB)
                            val newValuesUTB = domain.upperTypeBound(newValues)

                            if (newValuesUTB != baseUTB &&
                                classHierarchy.isSubtypeOf(newValuesUTB, baseUTB)) {
                                newValuesUTB
                            } else
                                baseUTB
                        }
                    if (newIsNull != this.isNull || newUTB != thisUTB)
                        updateType = StructuralUpdateType
                    val newIsPrecise =
                        (
                            // The following is necessary if we have an incomplete type hierarchy
                            // where we join an MObjectType -- which represents a final type, but
                            // which also explicitly lists a super type to satisfy the CHECKCAST
                            // contract (the super type is not part of the code base!) -- with
                            // an SObjectType representing the final type...).
                            newUTB.isSingletonSet && classHierarchy.isKnownToBeFinal(newUTB.head)
                        ) || {
                                this.isPrecise && that.isPrecise && (
                                    thisUTB.isEmpty || thatUTB.isEmpty || thisUTB == thatUTB
                                )
                            }

                    val newRefId = if (this.refId == that.refId) this.refId else nextRefId()
                    updateType(
                        MultipleReferenceValues(
                            newValues,
                            newOrigins,
                            newIsNull,
                            newIsPrecise,
                            newUTB,
                            newRefId
                        )
                    )
            }
        }

        // We have to handle a case such as:
        // Object o = "some Object A"
        // if(...) o = "some Object B"
        // ((String[])o)[0]
        //

        override def load(pc: Int, index: DomainValue): ArrayLoadResult = {
            if (isNull.isYes)
                return justThrows(VMNullPointerException(pc));

            assert(upperTypeBound.isSingletonSet, s"$upperTypeBound is not an array type")
            assert(upperTypeBound.head.isArrayType, s"$upperTypeBound is not an array type")

            if (values.exists(_.isInstanceOf[ObjectValue])) {
                var thrownExceptions: List[ExceptionValue] = Nil
                if (isNull.isUnknown && throwNullPointerExceptionOnArrayAccess)
                    thrownExceptions = VMNullPointerException(pc) :: thrownExceptions
                if (throwArrayIndexOutOfBoundsException)
                    thrownExceptions = VMArrayIndexOutOfBoundsException(pc) :: thrownExceptions

                val value = TypedValue(pc, upperTypeBound.head.asArrayType.componentType)
                ComputedValueOrException(value, thrownExceptions)
            } else {
                values.map(_.load(pc, index)).reduce((c1, c2) => mergeDEsComputations(pc, c1, c2))
            }
        }

        override def store(pc: Int, value: DomainValue, index: DomainValue): ArrayStoreResult = {
            if (isNull.isYes)
                return justThrows(VMNullPointerException(pc));

            assert(upperTypeBound.isSingletonSet)
            assert(upperTypeBound.head.isArrayType, s"$upperTypeBound is not an array type")

            if (values.exists(_.isInstanceOf[ObjectValue])) {
                var thrownExceptions: List[ExceptionValue] = Nil
                if (isNull.isUnknown && throwNullPointerExceptionOnArrayAccess)
                    thrownExceptions = VMNullPointerException(pc) :: thrownExceptions
                if (throwArrayIndexOutOfBoundsException)
                    thrownExceptions = VMArrayIndexOutOfBoundsException(pc) :: thrownExceptions
                if (throwArrayStoreException)
                    thrownExceptions = VMArrayStoreException(pc) :: thrownExceptions

                ComputationWithSideEffectOrException(thrownExceptions)
            } else {
                (values map (_.store(pc, value, index))) reduce {
                    (c1, c2) => mergeEsComputations(pc, c1, c2)
                }
            }
        }

        override def length(pc: Int): Computation[DomainValue, ExceptionValue] = {
            if (isNull.isYes)
                return throws(VMNullPointerException(pc)); // <====== early return

            assert(upperTypeBound.isSingletonSet)
            assert(upperTypeBound.head.isArrayType, s"$upperTypeBound (values=$values)")

            if (values.exists(_.isInstanceOf[ObjectValue])) {
                if (isNull.isUnknown && throwNullPointerExceptionOnArrayAccess)
                    ComputedValueOrException(IntegerValue(pc), VMNullPointerException(pc))
                else
                    ComputedValue(IntegerValue(pc))
            } else {
                val computations = values map (_.length(pc))
                computations reduce { (c1, c2) => mergeDEComputations(pc, c1, c2) }
            }
        }

        override lazy val hashCode: Int = values.hashCode * 47

        override def equals(other: Any): Boolean = {
            other match {
                case that: MultipleReferenceValues =>
                    this.isNull == that.isNull &&
                        this.isPrecise == that.isPrecise &&
                        this.upperTypeBound == that.upperTypeBound &&
                        that.values == this.values
                case _ =>
                    false
            }
        }

        override def toString: String = {
            val s =
                if (isNull.isYes) {
                    "null"
                } else {
                    var ss = upperTypeBound.map(_.toJava).mkString(" with ")
                    if (!isPrecise) ss = "_ <: "+ss
                    if (isNull.isUnknown) ss = s"{$ss, null}"
                    ss
                }
            values.mkString(s"$s[refId=$refId; values=«", ", ", "»]")
        }
    }

    object MultipleReferenceValues {
        def unapply(value: DomainValue): Option[UIDSet[DomainSingleOriginReferenceValue]] = {
            value match {
                case mrv: MultipleReferenceValues => Some(mrv.values)
                case _                            => None
            }
        }
    }

    // -----------------------------------------------------------------------------------
    //
    // HANDLING OF CONSTRAINTS
    //
    // -----------------------------------------------------------------------------------

    override def refSetUpperTypeBoundOfTopOperand(
        pc:       Int,
        bound:    ReferenceType,
        operands: Operands,
        locals:   Locals
    ): (Operands, Locals) = {
        asReferenceValue(operands.head).refineUpperTypeBound(pc, bound, operands, locals)
    }

    protected[this] def refineIsNull(
        pc:       Int,
        value:    DomainValue,
        isNull:   Answer,
        operands: Operands,
        locals:   Locals
    ): (Operands, Locals) = {
        asReferenceValue(value).refineIsNull(pc, isNull, operands, locals)
    }

    override def refTopOperandIsNull(
        pc:       Int,
        operands: Operands,
        locals:   Locals
    ): (Operands, Locals) = {
        val value = asReferenceValue(operands.head)
        refineIsNull(pc, value, Yes, operands, locals)
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
        pc:       Int,
        value:    DomainValue,
        operands: Operands,
        locals:   Locals
    ): (Operands, Locals) = {
        refineIsNull(pc, value, No, operands, locals)
    }

    /**
     * Updates the "null"ness property (`isNull == Yes`) of the given value.
     *
     * Calls `refineIsNull` on the given `ReferenceValue` and replaces every occurrence
     * on the stack/in a register with the updated value.
     *
     * @param value A `ReferenceValue`.
     */
    override def refEstablishIsNull(
        pc:       Int,
        value:    DomainValue,
        operands: Operands,
        locals:   Locals
    ): (Operands, Locals) = {
        refineIsNull(pc, value, Yes, operands, locals)
    }

    // -----------------------------------------------------------------------------------
    //
    // FACTORY METHODS
    //
    // -----------------------------------------------------------------------------------

    abstract override def toJavaObject(pc: Int, value: DomainValue): Option[Object] = {
        value match {
            case sov: SObjectValue if sov.isPrecise && sov.isNull.isNo &&
                (sov.upperTypeBound.head eq ObjectType.Object) =>
                Some(new Object)
            case _ =>
                super.toJavaObject(pc, value)
        }
    }

    //
    // REFINEMENT OF EXISTING DOMAIN VALUE FACTORY METHODS
    //

    override def NonNullObjectValue(pc: Int, objectType: ObjectType): DomainObjectValue = {
        ObjectValue(pc, No, false, objectType, nextRefId())
    }

    override def NewObject(pc: Int, objectType: ObjectType): DomainObjectValue = {
        ObjectValue(pc, No, true, objectType, nextRefId())
    }

    override def UninitializedThis(objectType: ObjectType): DomainObjectValue = {
        ObjectValue(-1, No, false, objectType, nextRefId())
    }

    override def InitializedObjectValue(pc: Int, objectType: ObjectType): DomainObjectValue = {
        ObjectValue(pc, No, true, objectType, nextRefId())
    }

    override def StringValue(pc: Int, value: String): DomainObjectValue = {
        ObjectValue(pc, No, true, ObjectType.String, nextRefId())
    }

    override def ClassValue(pc: Int, t: Type): DomainObjectValue = {
        ObjectValue(pc, No, true, ObjectType.Class, nextRefId())
    }

    override def ObjectValue(pc: Int, objectType: ObjectType): DomainObjectValue = {
        ObjectValue(pc, Unknown, false, objectType, nextRefId())
    }

    override def ObjectValue(pc: Int, upperTypeBound: UIDSet[ObjectType]): DomainObjectValue = {
        ObjectValue(pc, Unknown, upperTypeBound, nextRefId())
    }

    override def NewArray(pc: Int, count: DomainValue, arrayType: ArrayType): DomainArrayValue = {
        ArrayValue(pc, No, true, arrayType, nextRefId())
    }

    override def NewArray(pc: Int, counts: Operands, arrayType: ArrayType): DomainArrayValue = {
        ArrayValue(pc, No, true, arrayType, nextRefId())
    }

    override def ArrayValue(pc: Int, arrayType: ArrayType): DomainArrayValue = {
        if (arrayType.elementType.isBaseType)
            ArrayValue(pc, Unknown, true, arrayType, nextRefId())
        else
            ArrayValue(pc, Unknown, false, arrayType, nextRefId())
    }

    protected[domain] def ReferenceValue( // for SObjectValue
        origin:            ValueOrigin,
        isNull:            Answer,
        isPrecise:         Boolean,
        theUpperTypeBound: ReferenceType,
        refId:             RefId
    ): DomainSingleOriginReferenceValue = {
        theUpperTypeBound match {
            case ot: ObjectType => ObjectValue(origin, isNull, isPrecise, ot, refId)
            case at: ArrayType  => ArrayValue(origin, isNull, isPrecise, at, refId)
        }
    }

    final protected[domain] def ReferenceValue( // for SObjectValue
        origin:            ValueOrigin,
        isNull:            Answer,
        isPrecise:         Boolean,
        theUpperTypeBound: ReferenceType
    ): DomainSingleOriginReferenceValue = {
        ReferenceValue(origin, isNull, isPrecise, theUpperTypeBound, nextRefId())
    }

    final protected[domain] def ReferenceValue( // for S|MObjectValue
        origin:         ValueOrigin,
        isNull:         Answer,
        isPrecise:      Boolean,
        upperTypeBound: UIDSet[_ <: ReferenceType],
        refId:          RefId
    ): DomainSingleOriginReferenceValue = {
        upperTypeBound match {
            case UIDSet1(referenceType) =>
                ReferenceValue(origin, isNull, isPrecise, referenceType, refId)
            case _ =>
                val utb = upperTypeBound.asInstanceOf[UIDSet[ObjectType]]
                ObjectValue(origin, isNull, utb, refId)
        }
    }

    protected[domain] def ObjectValue( // for MObjectValue
        origin:         ValueOrigin,
        isNull:         Answer,
        upperTypeBound: UIDSet[ObjectType]
    ): DomainObjectValue = {
        ObjectValue(origin, isNull, upperTypeBound, nextRefId())
    }

    protected[domain] def ObjectValue( // for SObjectValue
        origin:            ValueOrigin,
        isNull:            Answer,
        isPrecise:         Boolean,
        theUpperTypeBound: ObjectType
    ): DomainObjectValue = {
        ObjectValue(origin, isNull, isPrecise, theUpperTypeBound, nextRefId())
    }

    //
    // DECLARATION OF ADDITIONAL DOMAIN VALUE FACTORY METHODS
    //

    protected[domain] def ObjectValue( // for SObjectValue
        pc:                Int,
        isNull:            Answer,
        isPrecise:         Boolean,
        theUpperTypeBound: ObjectType,
        refId:             Int
    ): DomainObjectValue

    protected[domain] def ObjectValue( // for MObjectValue
        pc:             Int,
        isNull:         Answer,
        upperTypeBound: UIDSet[ObjectType],
        refId:          Int
    ): DomainObjectValue

    protected[domain] def ArrayValue( // for ArrayValue
        pc:                Int,
        isNull:            Answer,
        isPrecise:         Boolean,
        theUpperTypeBound: ArrayType,
        refId:             Int
    ): DomainArrayValue

    protected[domain] def MultipleReferenceValues(
        values: UIDSet[DomainSingleOriginReferenceValue]
    ): DomainMultipleReferenceValues

    protected[domain] def MultipleReferenceValues(
        values:         UIDSet[DomainSingleOriginReferenceValue],
        origins:        ValueOrigins,
        isNull:         Answer,
        isPrecise:      Boolean,
        upperTypeBound: UIDSet[_ <: ReferenceType],
        refId:          Int
    ): DomainMultipleReferenceValues

    // Only intended to be used to map values from the outside into the context of a method.
    abstract override def InitializedDomainValue(
        origin: ValueOrigin,
        vi:     ValueInformation
    ): DomainValue = {
        vi match {
            // Matching:
            //
            //      IsMultipleReferenceValue
            //
            // is NOT YET SUPPORTED because THE INDIVIDUAL VALUES OF A MultipleReferenceValues
            // have to have unique origins.

            case _: IsNullValue =>
                NullValue(origin)

            case v: IsReferenceValue =>
                if (v.upperTypeBound.size > 1)
                    // handles MObjectValue
                    ObjectValue(origin, v.isNull, v.upperTypeBound.asInstanceOf[UIDSet[ObjectType]])
                else if (v.upperTypeBound.isEmpty) {
                    // Should not happen unless we have a MultipleReferenceValue over a
                    // collection of null values.
                    NullValue(origin)
                } else
                    ReferenceValue(origin, v.isNull, v.isPrecise, v.leastUpperType.get)

            case vi => super.InitializedDomainValue(origin, vi)
        }
    }

}
