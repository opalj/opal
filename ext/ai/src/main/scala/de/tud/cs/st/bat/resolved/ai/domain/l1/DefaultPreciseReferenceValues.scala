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
trait DefaultPreciseReferenceValues[+I]
        extends DefaultDomainValueBinding[I]
        with Origin
        with PreciseReferenceValues[I] { domain ⇒

    // ---------------------------------1--------------------------------------------------
    //
    // REPRESENTATION OF REFERENCE VALUES
    //
    // -----------------------------------------------------------------------------------

    trait ReferenceValue extends super.ReferenceValue  {
        this: DomainValue ⇒
    }

    //
    // REPRESENTATIONS OF CONCRETE REFERENCE VALUES
    //    

    class AReferenceValue protected[DefaultPreciseReferenceValues] (
        val pc: PC,
        val upperTypeBound: UpperTypeBound,
        override val isNull: Answer,
        val isPrecise: Boolean)
            extends ReferenceValue { value: DomainValue ⇒

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

        override def summarize(pc: PC): DomainValue = this

        override def summarize(pc: PC, value: DomainValue): DomainValue = {
            if (this eq value)
                return this

            value match {
                case mrv: MultipleReferenceValues ⇒
                    domain.summarizeReferenceValues(pc, mrv.values + this)
                case other @ AReferenceValue(otherPC, otherUpperTypeBounds, otherIsNull, otherIsPrecise) ⇒
                    if (this.isNull.yes) {
                        if (otherIsNull.yes)
                            if (this.pc == otherPC)
                                this
                            else
                                AReferenceValue(pc, UIDList.empty, Yes, true)
                        else
                            MultipleReferenceValues(Set(this, other))
                    } else if (otherIsNull.yes) {
                        MultipleReferenceValues(Set(this, other))
                    } else if (this.upperTypeBound == otherUpperTypeBounds) {
                        AReferenceValue(
                            pc,
                            this.upperTypeBound,
                            this.isNull merge otherIsNull,
                            this.isPrecise && otherIsPrecise)
                    } else if (domain.isSubtypeOf(this.upperTypeBound, otherUpperTypeBounds)) {
                        AReferenceValue(pc, otherUpperTypeBounds, this.isNull merge otherIsNull, false)
                    } else if (domain.isSubtypeOf(otherUpperTypeBounds, this.upperTypeBound)) {
                        AReferenceValue(pc, this.upperTypeBound, this.isNull merge otherIsNull, false)
                    } else {
                        MultipleReferenceValues(Set(this, other))
                    }
            }
        }

        override def equals(other: Any): Boolean = {
            other match {
                case that: AReferenceValue ⇒
                    (that eq this) || (
                        (that canEqual this) &&
                        this.pc == that.pc &&
                        this.isPrecise == that.isPrecise &&
                        this.isNull == that.isNull &&
                        this.upperTypeBound == that.upperTypeBound)
                case _ ⇒ false
            }
        }

        protected def canEqual(other: AReferenceValue): Boolean = true

        override def hashCode: Int = // TODO How to cache lazy vals?
            (((41 + pc) * 41 + isPrecise.hashCode()) *
                41 + isNull.hashCode()) *
                41 + upperTypeBound.hashCode()

        override def toString() =
            isNull match {
                case Yes ⇒ "Null(pc="+pc+")"
                case _ ⇒ upperTypeBound.map(_.toJava).mkString(" with ")+
                    "(pc="+pc+
                    ", isNull="+isNull+
                    ", isPrecise="+isPrecise+")"
            }

    }

    /**
     * Determines if the type described by the first set of upper type bounds is
     * a subtype of the second type.
     */
    protected def isSubtypeOf(
        upperTypeBoundsA: UpperTypeBound,
        upperTypeBoundsB: UpperTypeBound): Boolean = {
        upperTypeBoundsA forall { aType ⇒
            upperTypeBoundsB exists { bType ⇒
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

    /**
     * Extractor for `AReferenceValue`s.
     */
    object AReferenceValue {
        def unapply(arv: AReferenceValue): Option[(PC, UpperTypeBound, Answer, Boolean)] =
            Some((arv.pc, arv.upperTypeBound, arv.isNull, arv.isPrecise))
    }

    case class MultipleReferenceValues(
        val values: Set[AReferenceValue])
            extends ReferenceValue { this: DomainValue ⇒

        override def upperTypeBound: UpperTypeBound = {
            // we have to calculate the least common supertype of all values...
            sys.error("not yet implemented")
        }

        override def referenceValues: Iterable[IsAReferenceValue] = values

        override lazy val isPrecise: Boolean = values.forall(_.isPrecise)

        private[this] def calculateIsNull(): Answer =
            (values.head.isNull /: values.tail) { (c, n) ⇒
                val answer = c merge n.isNull
                if (answer.isUndefined)
                    return Unknown
                else
                    answer
            }
        override lazy val isNull: Answer = calculateIsNull()

        override def summarize(pc: PC): DomainValue =
            domain.summarizeReferenceValues(pc, values)

        override def summarize(pc: PC, value: DomainValue): DomainValue =
            value match {
                case aRefVal: AReferenceValue ⇒
                    domain.summarizeReferenceValues(pc, this.values + aRefVal)
                case MultipleReferenceValues(otherValues) ⇒
                    domain.summarizeReferenceValues(pc, this.values ++ otherValues)
            }

        override def adapt[TDI >: I](
            targetDomain: Domain[TDI],
            pc: PC): targetDomain.DomainValue =
            if (targetDomain.isInstanceOf[DefaultPreciseReferenceValues[TDI]]) {
                val thatDomain = targetDomain.asInstanceOf[DefaultPreciseReferenceValues[TDI]]
                val newValues = this.values.map { value: AReferenceValue ⇒
                    value.adaptAReferenceValue(thatDomain, pc)
                }
                if (newValues.size == 1)
                    newValues.head.asInstanceOf[targetDomain.DomainValue]
                else
                    thatDomain.MultipleReferenceValues(newValues).asInstanceOf[targetDomain.DomainValue]
            } else
                super.adapt(targetDomain, pc)

        def isValueSubtypeOf(supertype: ReferenceType): Answer = {
            // Recall that the client has to make an "isNull" check before calling
            // isSubtypeOf. Hence, at least one AReferenceValue has to be
            // non null.
            val relevantValues = values.view.filter(_.isNull.maybeNo)
            val firstAnswer = relevantValues.head.isValueSubtypeOf(supertype)
            (firstAnswer /: relevantValues.tail) { (answer, nextReferenceValue) ⇒
                (answer merge nextReferenceValue.isValueSubtypeOf(supertype)).
                    orElse { return Unknown }
            }
        }

        private[this] def updateValues(update: AReferenceValue ⇒ Option[AReferenceValue]) = {
            var createNew = false
            val updatedValues =
                for {
                    value ← values
                    updatedValue ← update(value)
                } yield {
                    if (value ne updatedValue) createNew = true
                    updatedValue
                }
            if (createNew || updatedValues.size < values.size) {
                if (updatedValues.size < 2)
                    updatedValues.head
                else
                    MultipleReferenceValues(updatedValues)
            } else
                this
        }

        override def updateIsNull(pc: PC, isNull: Answer): ReferenceValue = {
            assume(!isNull.isUndefined)
            updateValues { aReferenceValue: AReferenceValue ⇒
                if (aReferenceValue.isNull == isNull) {
                    // nothing to update over here... 
                    Some(aReferenceValue)
                } else if (aReferenceValue.isNull == isNull.negate)
                    // let's filter those values that are no longer relevant
                    None
                else
                    Some(aReferenceValue.updateIsNull(pc, isNull))
            }
        }

        override def refineUpperTypeBound(pc: PC, supertype: ReferenceType): DomainValue =
            updateValues { aReferenceValue: AReferenceValue ⇒
                if (aReferenceValue.isNull.yes)
                    None
                else
                    Some(aReferenceValue.refineUpperTypeBound(pc, supertype))
            }

        override def doJoin(mergePC: Int, value: DomainValue): Update[DomainValue] = {
            value match {
                case otherArv: AReferenceValue ⇒
                    values.find(_.pc == otherArv.pc) match {
                        case None ⇒
                            StructuralUpdate(MultipleReferenceValues(values + otherArv))
                        case Some(thisArv) ⇒
                            thisArv.doJoin(mergePC, otherArv) match {
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
            }
        }

        override def toString() = values.mkString("OneOf(\n\t", ",\n\t", ")")
    }

    //
    // INFORMATION ABOUT REFERENCE VALUES
    //

    abstract override def origin(value: DomainValue): Iterable[PC] =
        value match {
            case aRefVal: AReferenceValue        ⇒ Iterable[PC](aRefVal.pc)
            case MultipleReferenceValues(values) ⇒ values.map(aRefVal ⇒ aRefVal.pc)
            case _                               ⇒ super.origin(value)
        }

    abstract override def typeOfValue(value: DomainValue): TypesAnswer =
        value match {
            case r: IsReferenceValue ⇒ r
            case _                   ⇒ super.typeOfValue(value)
        }

    //
    // FACTORY METHODS
    //

    /**
     * ___The factory method___ that creates a new instance of an `AReferenceValue`.
     */
    def AReferenceValue(
        pc: PC,
        upperTypeBound: UpperTypeBound,
        isNull: Answer,
        isPrecise: Boolean): AReferenceValue =
        new AReferenceValue(pc, upperTypeBound, isNull, isPrecise)

    final def AReferenceValue(
        pc: PC,
        referenceType: ReferenceType,
        isNull: Answer,
        isPrecise: Boolean): AReferenceValue =
        AReferenceValue(pc, UIDList(referenceType), isNull, isPrecise)

    override def NullValue(pc: PC): DomainValue =
        AReferenceValue(pc, UIDList.empty, Yes, true)

    override def ReferenceValue(pc: PC, referenceType: ReferenceType): DomainValue =
        AReferenceValue(pc, UIDList(referenceType), Unknown, false)

    override def NonNullReferenceValue(pc: PC, objectType: ObjectType): DomainValue =
        AReferenceValue(pc, UIDList[ReferenceType](objectType), No, false)

    override def NewObject(pc: PC, objectType: ObjectType): DomainValue =
        AReferenceValue(pc, UIDList[ReferenceType](objectType), No, true)

    override def InitializedObject(pc: PC, referenceType: ReferenceType): DomainValue =
        AReferenceValue(pc, UIDList[ReferenceType](referenceType), No, true)

    override def StringValue(pc: PC, value: String): DomainValue =
        AReferenceValue(pc, UIDList[ReferenceType](ObjectType.String), No, true)

    override def ClassValue(pc: PC, t: Type): DomainValue =
        AReferenceValue(pc, UIDList[ReferenceType](ObjectType.Class), No, true)

}
