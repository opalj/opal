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

import de.tud.cs.st.util.{ Answer, Yes, No, Unknown }

/**
 * Implementation of a basic domain for handling reference values.
 *
 * This domain traces the type of a value and whether the value is `null`, maybe `null`
 * or is not `null`.
 *
 * @author Michael Eichberg
 */
trait TypeLevelReferenceValues[+I] extends Domain[I] {

    /**
     * Abstracts over all values with computational type `reference`.
     */
    trait ReferenceValue extends Value { this: DomainValue ⇒

        /**
         * Returns `ComputationalTypeReference`.
         */
        final def computationalType: ComputationalType = ComputationalTypeReference

        /**
         * The nullness property of this `ReferenceValue`.
         */
        def isNull: Answer

        /**
         * Indirectly called by BATAI when it determines that the null-value property
         * of this type-level reference should be updated.
         */
        def updateIsNull(pc: PC, isNull: Answer): DomainValue

        /**
         * Checks if the type of this value is a subtype of the specified
         * reference type.
         *
         * Basically, this method implements the same semantics as the `ClassHierarchy`'s
         * `isSubtypeOf` method. But, additionally it checks if the type of the value
         * could be a subtype of the given supertype. For example, assume that the type
         * of this reference value is `java.util.Collection` and we know that this is only
         * an upper bound. In this case an answer is `No` if and only if it is impossible
         * that the runtime type is a subtype of the given supertype. This
         * condition holds, for example, for `java.io.File` which is not a subclass
         * of `java.util.Collection` and which does not have any further subclasses (in
         * the JDK). I.e., the classes `java.io.File` and `java.util.Collection` are
         * not in an inheritance relationship. However, if the specified supertype would be
         * `java.util.List` the answer would be unknown.
         *
         * @param onNull If this value is known to be `null` and, hence, no type
         *      information is available the result of evaluating this function
         *      is returned.
         *
         *      This enables it to use this method as the basis for
         *      the implementation of "instanceof" and "checkcast" as both methods
         *      handle `null` values differently.
         */
        def isSubtypeOf(supertype: ReferenceType, onNull: ⇒ Answer): Answer

        /**
         * Adds an upper bound. This call can be ignored if the type
         * information related to this value is precise, i.e., if we know that we
         * precisely capture the runtime type of this value.
         */
        def addUpperBound(pc: PC, upperBound: ReferenceType): DomainValue
    }

    //
    // QUESTION'S ABOUT VALUES
    //

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
    def areEqualReferences(value1: DomainValue, value2: DomainValue): Answer = {
        val value1IsNull = isNull(value1)
        val value2IsNull = isNull(value2)
        if (value1IsNull.isDefined &&
            value2IsNull.isDefined &&
            (value1IsNull.yes || value2IsNull.yes)) {
            Answer(value1IsNull == value2IsNull)
        } else {
            // TODO [IMPROVE - areEqualReferences] If the two values are not in a subtype relationship they cannot be equal.
            Unknown
        }
    }

    /**
     * Helper object that facilitates general matching against reference values.
     */
    object AsReference {
        def unapply(value: DomainValue): Option[ReferenceValue] =
            if (value.isInstanceOf[ReferenceValue])
                Some(value.asInstanceOf[ReferenceValue])
            else
                None
    }

    /**
     * Determines the nullness-property of the given value.
     *
     * @param value A value of type `ReferenceValue`.
     */
    def isNull(value: DomainValue): Answer = {
        val AsReference(objectref) = value
        objectref.isNull
    }

    def isSubtypeOf(
        value: DomainValue,
        supertype: ReferenceType,
        onNull: ⇒ Answer): Answer = {
        val AsReference(objectref) = value
        objectref.isSubtypeOf(supertype, onNull)
    }

    // -----------------------------------------------------------------------------------
    //
    // HANDLING OF CONSTRAINTS
    //
    // -----------------------------------------------------------------------------------

    override def establishUpperBound(
        pc: PC,
        bound: ReferenceType,
        value: DomainValue,
        operands: Operands,
        locals: Locals): (Operands, Locals) = {
        val AsReference(referenceValue) = value
        val newReferenceValue = referenceValue.addUpperBound(pc, bound)
        if (referenceValue eq newReferenceValue)
            (
                operands,
                locals
            )
        else
            (
                operands.map(op ⇒ if (op eq value) newReferenceValue else op),
                locals.map(l ⇒ if (l eq value) newReferenceValue else l)
            )
    }

    protected def updateIsNull(
        pc: PC,
        value: DomainValue,
        isNull: Answer,
        operands: Operands,
        locals: Locals): (Operands, Locals) = {
        val AsReference(referenceValue) = value
        val newReferenceValue = referenceValue.updateIsNull(pc, isNull)
        if (referenceValue eq newReferenceValue)
            (
                operands,
                locals
            )
        else
            (
                operands.map(op ⇒ if (op eq value) newReferenceValue else op),
                locals.map(l ⇒ if (l eq value) newReferenceValue else l)
            )
    }

    /**
     * Updates the nullness property (`isNull == No`) of the given value.
     *
     * Calls `updateIsNull` on the given `ReferenceValue` and replaces every occurrence
     * on the stack/in a register with the updated value.
     *
     * @param value A `ReferenceValue`.
     */
    override def establishIsNonNull(
        pc: PC,
        value: DomainValue,
        operands: Operands,
        locals: Locals): (Operands, Locals) =
        updateIsNull(pc, value, No, operands, locals)

    /**
     * Updates the nullness property (`isNull == Yes`) of the given value.
     *
     * Calls `updateIsNull` on the given `ReferenceValue` and replaces every occurrence
     * on the stack/in a register with the updated value.
     *
     * @param value A `ReferenceValue`.
     */
    override def establishIsNull(
        pc: PC,
        value: DomainValue,
        operands: Operands,
        locals: Locals): (Operands, Locals) =
        updateIsNull(pc, value, Yes, operands, locals)

}

/**
 * @author Michael Eichberg
 */
trait DefaultTypeLevelReferenceValues[+I]
        extends DefaultValueBinding[I]
        with TypeLevelReferenceValues[I] { domain ⇒

    /**
     * A type bound represents the available information about a reference
     * value's type.
     *
     * In case of reference types, a type bound may, e.g., be a set of interface types
     * which are known to be implemented by the current object. Even if the type
     * contains a class type it may just be a super class of the concrete type and,
     * hence, just represent an abstraction.
     *
     * How type bounds related to reference types are handled and whether the
     * domain makes it possible to distinguish between precise types and
     * type bounds is at the sole discretion of the domain.
     */
    type TypeBound = Set[ReferenceType]

    def leastCommonSupertype(bound1: TypeBound, bound2: TypeBound): TypeBound = {
null
    }

    // -----------------------------------------------------------------------------------
    //
    // REPRESENTATION OF REFERENCE VALUES
    //
    // -----------------------------------------------------------------------------------

    trait ReferenceValue
            extends super.ReferenceValue
            with IsReferenceType { this: DomainValue ⇒

        /**
         * Returns the set of types of the represented value.
         *
         * Basically, we have to distinguish two situations:
         * 1. a value that actually represents multiple values and that may have
         *    (depending on the control flow) different independent types (different
         *    values with corresponding types).
         * 2. a type for which we have multiple bounds; i.e., we don't know the precise
         *    type, but we know (e.g., due to typechecks) that it (has to) implements
         *    multiple interfaces.
         */
        def valueType: TypeBound

        /**
         * Returns `true` if the type information about this value is precise.
         * I.e., if `isPrecise` returns `true` and the value's type is
         * reported to be `java.lang.Object` then the current value is known to be an
         * instance of the class `java.lang.Object` and of no other (sub)class.
         * Hence, for an interface type `isPrecise` will always return false.
         */
        def isPrecise: Boolean
    }

    object ReferenceValue {
        def unapply(rv: ReferenceValue): Some[(TypeBound, Answer, Boolean)] =
            Some((rv.valueType, rv.isNull, rv.isPrecise))
    }

    //
    // REPRESENTATIONS OF CONCRETE REFERENCE VALUES
    //    

    class AReferenceValue protected[DefaultTypeLevelReferenceValues] (
        val pc: PC,
        val valueType: TypeBound,
        val isNull: Answer,
        val isPrecise: Boolean)
            extends ReferenceValue { this: DomainValue ⇒

        override def nonEmpty = valueType.nonEmpty

        override def size = valueType.size

        def foreach[U](f: TypeBound ⇒ U): Unit = f(valueType)

        def headType = valueType.head

        def foreachType[U](f: ReferenceType ⇒ U): Unit = valueType.foreach(f)

        def forallTypes(f: ReferenceType ⇒ Boolean): Boolean = valueType.forall(f)

        /**
         * Determines if this reference value is a subtype of the given supertype by
         * delegating to the `isSubtypeOf(ReferenceType,ReferenceType)` method of the
         * domain.
         *
         * Additionally, the `isPrecise` property is taken into consideration to ensure
         * that a `No` answer means that it is impossible that any runtime value is
         * actually a subtype of the given supertype.
         */
        def isSubtypeOf(supertype: ReferenceType, onNull: ⇒ Answer): Answer = {
            if (isNull.yes)
                return onNull

            val answer: Answer = ((No: Answer) /: valueType) { (a, t) ⇒
                val isSubtypeOf = domain.isSubtypeOf(t, supertype)
                if (isSubtypeOf.yes) {
                    return Yes
                } else {
                    a merge isSubtypeOf
                }
            }

            answer match {
                case No if isPrecise ⇒ No
                case No ⇒
                    // .. is it conceivable that this value is still a subtype of the
                    // given reference type?
                    if (forallTypes { subtype ⇒ domain.isSubtypeOf(supertype, subtype).maybeYes })
                        // Well it is conceivable that the value at runtime is a subtype
                        Unknown
                    else
                        No
                case Unknown ⇒ Unknown
            }
        }

        def updateIsNull(pc: PC, isNull: Answer): AReferenceValue = {
            if (this.isNull.isUndefined)
                if (isNull.yes)
                    AReferenceValue(this.pc, Set.empty[ReferenceType], Yes, true)
                else
                    AReferenceValue(this.pc, valueType, isNull, isPrecise)
            else
                // this update of the value's isNull property doesn't make sense
                // hence, we swallow it to facilitate the implementation of 
                // MultipleReferenceValues
                this
        }

        def addUpperBound(pc: PC, theUpperBound: ReferenceType): AReferenceValue = {
            val isSubtypeOfAnswer = isSubtypeOf(theUpperBound, Yes)
            isSubtypeOfAnswer match {
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
                case No if forallTypes(domain.isSubtypeOf(theUpperBound, _).yes) ⇒
                    AReferenceValue(this.pc, Set(theUpperBound), isNull, isPrecise)
                case _ /* (No && !isPrecise || Unknown) */ ⇒
                    var newValueTypes = Set.empty[ReferenceType]
                    var addTheUpperBound = true
                    valueType.foreach { anUpperBound ⇒
                        if (theUpperBound == anUpperBound) {
                            /* do nothing */
                        } else if (domain.isSubtypeOf(theUpperBound, anUpperBound).yes) {
                            /* do nothing (we may add theUpperBound later) */
                        } else if (domain.isSubtypeOf(anUpperBound, theUpperBound).yes) {
                            addTheUpperBound = false
                            newValueTypes = newValueTypes + anUpperBound
                        } else {
                            newValueTypes = newValueTypes + anUpperBound
                        }
                    }
                    if (addTheUpperBound)
                        newValueTypes = newValueTypes + theUpperBound

                    AReferenceValue(this.pc, newValueTypes, isNull, isPrecise)
            }
        }

        override def join(mergePC: Int, value: DomainValue): Update[DomainValue] = {
            value match {
                case v @ AReferenceValue(otherPC, otherValueType, otherIsNull, otherIsPrecise) ⇒
                    if (otherPC != this.pc)
                        return StructuralUpdate(MultipleReferenceValues(Set(this, v)))

                    if (this.isNull == otherIsNull &&
                        this.valueType == otherValueType &&
                        (this.isPrecise == false || this.isPrecise == otherIsPrecise))
                        return NoUpdate

                    var newValueType = this.valueType
                    otherValueType.foreach { otherValueType ⇒
                        var addOtherValueType = true
                        newValueType = newValueType.filterNot { vt ⇒
                            domain.isSubtypeOf(otherValueType, vt) match {
                                case Yes ⇒
                                    true
                                case _ ⇒
                                    if (domain.isSubtypeOf(vt, otherValueType).yes)
                                        addOtherValueType = false
                                    false
                            }
                        }
                        if (addOtherValueType)
                            newValueType = newValueType + otherValueType
                    }
                    StructuralUpdate(AReferenceValue(
                        this.pc,
                        newValueType,
                        this.isNull merge otherIsNull,
                        this.isPrecise && otherIsPrecise
                    ))

                case mrv: MultipleReferenceValues ⇒
                    mrv.join(mergePC, this) match {
                        case NoUpdate                 ⇒ StructuralUpdate(mrv)
                        case SomeUpdate(updatedValue) ⇒ StructuralUpdate(updatedValue)
                    }
                case _ ⇒ MetaInformationUpdateIllegalValue
            }
        }

        override def adapt[ThatI >: I](
            targetDomain: Domain[ThatI],
            pc: PC): targetDomain.DomainValue =
            targetDomain match {
                case thatDomain: DefaultTypeLevelReferenceValues[ThatI] ⇒
                    adaptAReferenceValue(thatDomain, pc).asInstanceOf[targetDomain.DomainValue]
                case _ ⇒ super.adapt(targetDomain, pc)
            }

        def adaptAReferenceValue[ThatI >: I](
            targetDomain: DefaultTypeLevelReferenceValues[ThatI],
            pc: PC): targetDomain.AReferenceValue =
            new targetDomain.AReferenceValue(
                pc, this.valueType, this.isNull, this.isPrecise
            )

        def summarize(pc: PC): DomainValue = this

        override def summarize(pc: PC, value: DomainValue): DomainValue = {
            if (this eq value)
                return this
                null
//
//            value match {
//                case v @ AReferenceValue(otherPC, otherValueType, otherIsNull, otherIsPrecise) ⇒
//                    if (otherValueType == this.valueType)
//                        AReferenceValue(
//                            pc,
//                            otherValueType,
//                            this.isNull merge otherIsNull,
//                            this.isPrecise && otherIsPrecise
//                        )
//                    else {
//                        AReferenceValue(
//                            pc,
//                            computeType(this.valueType, otherValueType),
//                            this.isNull merge otherIsNull,
//                            false
//                        )
//                    }
//
//                case MultipleReferenceValues(values) ⇒ domain.summarize(pc, values + this)
//
//            }
        }

        override def equals(other: Any): Boolean = {
            other match {
                case that: AReferenceValue ⇒
                    (that eq this) || (
                        (that canEqual this) &&
                        this.pc == that.pc &&
                        this.isPrecise == that.isPrecise &&
                        this.isNull == that.isNull &&
                        this.valueType == that.valueType)
                case _ ⇒ false
            }
        }

        protected def canEqual(other: AReferenceValue): Boolean = true

        override def hashCode: Int = { // TODO How to cache lazy vals?
            (((41 + pc) * 41 + isPrecise.hashCode()) *
                41 + isNull.hashCode()) *
                41 + valueType.hashCode()
        }

        override def toString() =
            isNull match {
                case Yes ⇒ "Null(pc="+pc+")"
                case _ ⇒
                    valueType.map(_.toJava).mkString(" with ")+
                        "(pc="+pc+
                        ", isNull="+isNull+
                        ", isPrecise="+isPrecise+")"
            }

    }

    // TODO place in some special "Origin trait" to make the method abstract overridable!
    def origin(value: DomainValue): Iterable[Int] = value match {
        case aRefVal: AReferenceValue        ⇒ Iterable[Int](aRefVal.pc)
        case MultipleReferenceValues(values) ⇒ values.map(aRefVal ⇒ aRefVal.pc)
        case _                               ⇒ Iterable.empty[Int] // TODO replace with super call as soon as this method becomes abstract overrideable 
    }

    /**
     * Extractor for `AReferenceValue`s.
     */
    object AReferenceValue {
        def unapply(arv: AReferenceValue): Option[(Int, TypeBound, Answer, Boolean)] =
            Some((arv.pc, arv.valueType, arv.isNull, arv.isPrecise))
    }

    def AReferenceValue(
        pc: PC,
        valueType: TypeBound,
        isNull: Answer,
        isPrecise: Boolean): AReferenceValue =
        new AReferenceValue(pc, valueType, isNull, isPrecise)

    final def AReferenceValue(
        pc: PC,
        referenceType: ReferenceType,
        isNull: Answer = Unknown,
        isPrecise: Boolean = false): AReferenceValue =
        AReferenceValue(pc, Set(referenceType), isNull, isPrecise)

    case class MultipleReferenceValues(
        val values: Set[AReferenceValue])
            extends ReferenceValue { this: DomainValue ⇒

        override def summarize(pc: PC): DomainValue = domain.summarize(pc, values)

        override def summarize(pc: PC, value: DomainValue): DomainValue = {
            value match {
                case aRefVal: AReferenceValue ⇒
                    domain.summarize(pc, values + aRefVal)
                case MultipleReferenceValues(otherValues) ⇒
                    domain.summarize(pc, this.values ++ otherValues)
            }
        }

        override def adapt[TDI >: I](targetDomain: Domain[TDI], pc: PC): targetDomain.DomainValue =
            if (targetDomain.isInstanceOf[DefaultTypeLevelReferenceValues[TDI]]) {
                val thatDomain = targetDomain.asInstanceOf[DefaultTypeLevelReferenceValues[TDI]]
                val newValues = this.values.map { value: AReferenceValue ⇒
                    value.adaptAReferenceValue(thatDomain, pc)
                }
                if (newValues.size == 1)
                    newValues.head.asInstanceOf[targetDomain.DomainValue]
                else
                    thatDomain.MultipleReferenceValues(newValues).asInstanceOf[targetDomain.DomainValue]
            } else
                super.adapt(targetDomain, pc)

        def isSubtypeOf(supertype: ReferenceType, onNull: ⇒ Answer): Answer = {
            val firstAnswer = values.head.isSubtypeOf(supertype, onNull)
            (firstAnswer /: values.tail) { (answer, nextReferenceValue) ⇒
                (answer merge nextReferenceValue.isSubtypeOf(supertype, onNull)).
                    orElse { return Unknown }
            }
        }

        /**
         * The set of all upper type bounds.
         */
        lazy val valueType: Set[ReferenceType] = values.flatMap(_.valueType)

        def foreach[U](f: TypeBound ⇒ U): Unit = values.foreach(_.foreach(f))

        override def nonEmpty = valueType.nonEmpty // values.forall(_.nonEmpty) // FIXME doesn't it have to be "values.exists(_.nonEmpty)"

        override def size = valueType.size

        def foreachType[U](f: ReferenceType ⇒ U): Unit = valueType.foreach(f)

        def forallTypes(f: ReferenceType ⇒ Boolean): Boolean = valueType.forall(f)

        def headType: ReferenceType = valueType.head

        private def updateValues(update: AReferenceValue ⇒ AReferenceValue) = {
            var createNew = false
            val updatedValues = values.map { v ⇒
                val updatedValue = update(v)
                if (updatedValue ne v)
                    createNew = true
                updatedValue
            }
            if (createNew)
                MultipleReferenceValues(updatedValues)
            else
                this
        }

        override def updateIsNull(pc: PC, isNull: Answer): ReferenceValue =
            updateValues { aReferenceValue: AReferenceValue ⇒
                aReferenceValue.updateIsNull(pc, isNull)
            }

        def addUpperBound(pc: PC, upperBound: ReferenceType): DomainValue = {
            updateValues { aReferenceValue: AReferenceValue ⇒
                aReferenceValue.addUpperBound(pc, upperBound)
            }
        }

        lazy val isPrecise: Boolean = values.forall(_.isPrecise)

        lazy val isNull: Answer = (values.head.isNull /: values.tail)(_ merge _.isNull)

        override def join(mergePC: Int, value: DomainValue): Update[DomainValue] = {
            if (value eq this)
                return NoUpdate

            value match {
                case otherArv: AReferenceValue ⇒
                    values.find(_.pc == otherArv.pc) match {
                        case None ⇒
                            StructuralUpdate(MultipleReferenceValues(values + otherArv))
                        case Some(thisArv) ⇒
                            thisArv.join(mergePC, otherArv) match {
                                case NoUpdate ⇒ NoUpdate
                                case update @ SomeUpdate(updatedArv: AReferenceValue) ⇒
                                    update.updateValue(
                                        MultipleReferenceValues((values - thisArv) + updatedArv)
                                    )
                            }
                    }
                case otherMrv: MultipleReferenceValues ⇒
                    var updateType: UpdateType = NoUpdateType
                    var otherRemainingArvs = otherMrv.values
                    var newValues: Set[AReferenceValue] = values.map { thisArv ⇒
                        otherRemainingArvs.find(_.pc == thisArv.pc) match {
                            case None ⇒ thisArv
                            case Some(otherArv) ⇒
                                otherRemainingArvs -= otherArv
                                thisArv.join(mergePC, otherArv) match {
                                    case NoUpdate ⇒
                                        thisArv
                                    case update @ SomeUpdate(updatedArv: AReferenceValue) ⇒
                                        updateType = updateType &: update
                                        updatedArv
                                }
                        }
                    }
                    if (otherRemainingArvs.size > 0) {
                        newValues ++= otherRemainingArvs
                        updateType = StructuralUpdateType
                    }
                    updateType(MultipleReferenceValues(newValues))
                case _ ⇒ MetaInformationUpdateIllegalValue
            }
        }

        override def toString() = {
            values.mkString("OneOf(\n\t", ",\n\t", ")")
        }
    }

    abstract override def types(value: DomainValue): TypesAnswer[_] = {
        value match {
            case r: ReferenceValue ⇒ r
            case _                 ⇒ super.types(value)
        }
    }

    //
    // FACTORY METHODS
    //

    def newNullValue(pc: PC): DomainValue =
        AReferenceValue(pc, Set.empty[ReferenceType], Yes, true)

    def newReferenceValue(referenceType: ReferenceType): DomainValue =
        AReferenceValue(-1, Set(referenceType), Unknown, false)

    def newReferenceValue(pc: PC, referenceType: ReferenceType): DomainValue =
        AReferenceValue(pc, Set(referenceType), Unknown, false)

    def newObject(pc: PC, objectType: ObjectType): DomainValue =
        AReferenceValue(pc, Set[ReferenceType](objectType), No, true)

    def newInitializedObject(pc: PC, objectType: ObjectType): DomainValue =
        newObject(pc, objectType)

    def newStringValue(pc: PC, value: String): DomainValue =
        AReferenceValue(pc, Set[ReferenceType](ObjectType.String), No, true)

    def newClassValue(pc: PC, t: Type): DomainValue =
        AReferenceValue(pc, Set[ReferenceType](ObjectType.Class), No, true)

    def newArray(
        pc: PC,
        arrayType: ArrayType,
        isNull: Answer = No,
        isPrecise: Boolean = true): DomainValue =
        AReferenceValue(pc, Set[ReferenceType](arrayType), isNull, isPrecise)

    // -----------------------------------------------------------------------------------
    //
    // HANDLING OF COMPUTATIONS
    //
    // -----------------------------------------------------------------------------------

    //
    // CREATE ARRAY
    //
    def newarray(pc: PC,
                 count: DomainValue,
                 componentType: FieldType): Computation[DomainValue, DomainValue] =
        //ComputedValueAndException(TypedValue(ArrayType(componentType)), TypedValue(ObjectType.NegativeArraySizeException))
        ComputedValue(newArray(pc, ArrayType(componentType)))

    /**
     * @note The componentType may be (again) an array type.
     */
    def multianewarray(pc: PC,
                       counts: List[DomainValue],
                       arrayType: ArrayType) =
        //ComputedValueAndException(TypedValue(arrayType), TypedValue(ObjectType.NegativeArraySizeException))
        ComputedValue(newArray(pc, arrayType, No, true))

    //
    // LOAD FROM AND STORE VALUE IN ARRAYS
    //
    def aaload(pc: PC, index: DomainValue, arrayref: DomainValue): ArrayLoadResult =
        types(arrayref) match {
            case HasSingleReferenceTypeBound(ArrayType(componentType)) ⇒
                ComputedValue(newTypedValue(pc, componentType))
            case _ ⇒
                domainException(
                    this,
                    "cannot determine the type of the array's content: "+arrayref
                )
        }

    def aastore(pc: PC, value: DomainValue, index: DomainValue, arrayref: DomainValue) =
        ComputationWithSideEffectOnly
}

import analyses.ClassHierarchy

trait DefaultTypeLevelReferenceValuesWithClosedHierarchy[+I]
        extends DefaultTypeLevelReferenceValues[I] {

    def classHierarchy: ClassHierarchy

    override def newReferenceValue(referenceType: ReferenceType): DomainValue =
        referenceType match {
            case ot: ObjectType ⇒
                val isPrecise = classHierarchy.subtypes(ot).isEmpty
                AReferenceValue(-1, Set(referenceType), Unknown, isPrecise)
            case _ ⇒ super.newReferenceValue(referenceType)
        }

    override def newReferenceValue(pc: PC, referenceType: ReferenceType): DomainValue =
        referenceType match {
            case ot: ObjectType ⇒
                val isPrecise = classHierarchy.subtypes(ot).isEmpty
                AReferenceValue(pc, Set(referenceType), Unknown, isPrecise)
            case _ ⇒ super.newReferenceValue(pc, referenceType)
        }

}
