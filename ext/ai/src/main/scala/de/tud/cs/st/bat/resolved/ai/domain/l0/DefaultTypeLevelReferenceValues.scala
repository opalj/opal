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
 * Default implementation for handling reference values.
 *
 * @author Michael Eichberg
 */
trait DefaultTypeLevelReferenceValues[+I]
        extends DefaultDomainValueBinding[I]
        with TypeLevelReferenceValues[I] { domain ⇒

    //    import collection.immutable.{Set => SortedList}
    //    type UpperBound = collection.immutable.Set[ReferenceType]
    type UpperBound = UIDList[ReferenceType]

    // -----------------------------------------------------------------------------------
    //
    // REPRESENTATION OF REFERENCE VALUES
    //
    // -----------------------------------------------------------------------------------

    case class AReferenceValue protected[DefaultTypeLevelReferenceValues] (
        upperBound: ReferenceType)
            extends super.ReferenceValue
            with IsReferenceValue { value ⇒

        override lazy val upperBounds: Iterable[ValueBasedUpperBound] =
            Iterable(
                new ValueBasedUpperBound {
                    override def isNull: Answer = Unknown
                    override def isPrecise: Boolean = false // TODO Add a test such as "classHierarchy.hasSubtypes(...)
                    override def upperBound: ai.UpperBound = Iterable(value.upperBound)
                    override def isSubtypeOf(supertype: ReferenceType): Answer =
                        value.isSubtypeOf(supertype)
                }
            )

        override def hasSingleBound: Option[ReferenceType] = Some(upperBound)

        override def isSubtypeOf(supertype: ReferenceType): Answer = {
            // TODO if the class hierarchy has better support to identify whether to types __may be__ in a subtype relation..
            if (domain.isSubtypeOf(upperBound, supertype).yes)
                Yes
            else
                Unknown
        }

        // NARROWING OPERATION
        override def addUpperBound(
            pc: PC,
            additionalUpperBound: ReferenceType): ReferenceValue = {
            // BATAI calls this method only if a previous "subtype of" test 
            // (typeOf(this.value) <: additionalUpperBound ?) 
            // returned unknown or no and we are now on the branch where this relation
            // has to hold. Hence, we only need handle the case where 
            // the new bound is more strict than the previous bound.  
            if (domain.isSubtypeOf(additionalUpperBound, upperBound).yes)
                AReferenceValue(additionalUpperBound)
            else
                // probably some (abstract) class and an interface or two
                // unrelated interfaces
                MReferenceValue(UIDList(upperBound, additionalUpperBound))
        }

        // WIDENING OPERATION
        override def doJoin(joinPC: Int, other: DomainValue): Update[DomainValue] = {
            val thisUpperBound = this.upperBound
            other match {
                case AReferenceValue(thatUpperBound) ⇒
                    if ((thisUpperBound eq thatUpperBound) ||
                        domain.isSubtypeOf(thatUpperBound, thisUpperBound).yes)
                        NoUpdate
                    else if (domain.isSubtypeOf(thisUpperBound, thatUpperBound).yes)
                        StructuralUpdate(other)
                    else
                        StructuralUpdate(
                            MReferenceValue(UIDList(thisUpperBound, thatUpperBound))
                        )
                case MReferenceValue(thatUpperBound) ⇒
                    val newUpperBound = thatUpperBound filter { thatBound ⇒
                        domain.isSubtypeOf(thisUpperBound, thatBound) match {
                            case Yes ⇒
                                return StructuralUpdate(other)
                            case No ⇒
                                if (domain.isSubtypeOf(thatBound, thisUpperBound).yes)
                                    false
                                else
                                    true
                            case Unknown ⇒
                                true
                        }
                    }
                    if (newUpperBound.isEmpty)
                        // all upper bounds of "other" are a subtype of this upper bound
                        NoUpdate
                    else
                        StructuralUpdate(MReferenceValue(newUpperBound + thisUpperBound))
            }
        }

        override def adapt[ThatI >: I](
            targetDomain: Domain[ThatI],
            pc: PC): targetDomain.DomainValue =
            targetDomain match {
                case thatDomain: DefaultTypeLevelReferenceValues[ThatI] ⇒
                    adaptAReferenceValue(thatDomain, pc).asInstanceOf[targetDomain.DomainValue]
                case _ ⇒
                    super.adapt(targetDomain, pc)
            }

        // [Scala 2.10.3] This private method is needed to avoid that the compiler crashes! 
        private[this] def adaptAReferenceValue[ThatI >: I](
            targetDomain: DefaultTypeLevelReferenceValues[ThatI],
            pc: PC): targetDomain.AReferenceValue =
            targetDomain.AReferenceValue(this.upperBound)

        override def summarize(pc: PC): DomainValue = this

        override def summarize(pc: PC, value: DomainValue): DomainValue =
            sys.error("not implemented")
    }

    /**
     * Determines the nullness-property of the given value.
     *
     * @param value A value of type `ReferenceValue`.
     */
    def isNull(value: DomainValue): Answer = value match {
        case MReferenceValue(UIDList.empty) ⇒ Yes
        case _                              ⇒ Unknown
    }

    case class MReferenceValue protected[DefaultTypeLevelReferenceValues] (
        upperBound: UpperBound)
            extends super.ReferenceValue
            with IsReferenceValue { value ⇒

        override lazy val upperBounds: Iterable[ValueBasedUpperBound] =
            Iterable(
                new ValueBasedUpperBound {
                    override def isNull: Answer = Unknown
                    // TODO If we know that there are no subtypes...
                    override def isPrecise: Boolean = false
                    override def upperBound: ai.UpperBound = value.upperBound.toIterable
                    override def isSubtypeOf(supertype: ReferenceType): Answer =
                        value.isSubtypeOf(supertype)
                }
            )

        override def hasSingleBound: Option[ReferenceType] = None

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
            if (upperBound exists { thisType ⇒ domain.isSubtypeOf(thisType, supertype).yes })
                Yes
            else
                Unknown
        }

        override def addUpperBound(
            pc: PC,
            additionalUpperBound: ReferenceType): ReferenceValue = {
            // BATAI calls this method only if a previous "subtype of" test 
            // (typeOf(this.value) <: additionalUpperBound ?) 
            // returned unknown. Hence, we only handle the case where the new bound
            // is more strict than the previous bound.

            var newUpperBound: UIDList[ReferenceType] = UIDList.empty
            upperBound foreach { (existingUpperBound: ReferenceType) ⇒
                // ATTENTION: "!..yes" is not the same as "no" (there is also unknown)
                if (!domain.isSubtypeOf(additionalUpperBound, existingUpperBound).yes)
                    newUpperBound = newUpperBound + existingUpperBound
            }
            if (newUpperBound.size == 0)
                AReferenceValue(additionalUpperBound)
            else
                MReferenceValue(newUpperBound + additionalUpperBound)
        }

        override def doJoin(joinPC: Int, that: DomainValue): Update[DomainValue] = {
            that match {
                case AReferenceValue(thatUpperBound) ⇒
                    val newUpperBound = this.upperBound filter { thisBound ⇒
                        domain.isSubtypeOf(thatUpperBound, thisBound) match {
                            case Yes ⇒
                                return NoUpdate
                            case No ⇒
                                if (domain.isSubtypeOf(thisBound, thatUpperBound).yes)
                                    false
                                else
                                    true
                            case Unknown ⇒
                                true
                        }
                    }
                    if (newUpperBound.isEmpty)
                        // all upper bounds of "other" are a subtype of this upper bound
                        StructuralUpdate(that)
                    else
                        StructuralUpdate(MReferenceValue(newUpperBound + thatUpperBound))
                case MReferenceValue(thatUpperBound) ⇒
                    val thisUpperBound = this.upperBound
                    if (thisUpperBound == thatUpperBound)
                        return NoUpdate
                    val thisDomain = domain
                    var newUpperBound = thisUpperBound
                    thatUpperBound foreach { (otherTypeBound: ReferenceType) ⇒
                        var addOtherTypeBounds = true
                        newUpperBound = newUpperBound filterNot { vt ⇒
                            if (thisDomain.isSubtypeOf(otherTypeBound, vt).yes)
                                true
                            else {
                                if (thisDomain.isSubtypeOf(vt, otherTypeBound).yes)
                                    addOtherTypeBounds = false
                                false
                            }
                        }
                        if (addOtherTypeBounds)
                            newUpperBound = newUpperBound + otherTypeBound
                    }
                    if (newUpperBound eq thisUpperBound) {
                        NoUpdate
                    } else if (newUpperBound eq thatUpperBound) {
                        StructuralUpdate(that)
                    } else
                        StructuralUpdate(MReferenceValue(newUpperBound))
            }
        }

        override def adapt[ThatI >: I](
            targetDomain: Domain[ThatI],
            pc: PC): targetDomain.DomainValue =
            targetDomain match {
                case thatDomain: DefaultTypeLevelReferenceValues[ThatI] ⇒
                    adaptMReferenceValue(thatDomain, pc).asInstanceOf[targetDomain.DomainValue]
                case _ ⇒ super.adapt(targetDomain, pc)
            }

        protected[DefaultTypeLevelReferenceValues] def adaptMReferenceValue[ThatI >: I](
            targetDomain: DefaultTypeLevelReferenceValues[ThatI],
            pc: PC): targetDomain.MReferenceValue =
            targetDomain.MReferenceValue(this.upperBound)

        override def summarize(pc: PC): DomainValue = this

        override def summarize(pc: PC, value: DomainValue): DomainValue = {
            if (this eq value)
                return this

            val MReferenceValue(otherTypeBounds) = value
            if (this.upperBound == otherTypeBounds ||
                domain.isSubtypeOf(otherTypeBounds, this.upperBound)) {
                this
            } else if (domain.isSubtypeOf(this.upperBound, otherTypeBounds)) {
                value
            } else {
                sys.error("needs to be implemented")
                //MReferenceValue(null: ReferenceType) // TODO calculate the least upper bound...
            }
        }

        override def equals(other: Any): Boolean =
            other match {
                case that: MReferenceValue ⇒
                    (that eq this) || (
                        (that canEqual this) &&
                        this.upperBound == that.upperBound)
                case _ ⇒ false
            }

        protected def canEqual(other: MReferenceValue): Boolean = true

        override lazy val hashCode: Int = (upperBound.hashCode() * 41)

        override def toString() =
            "TypeLevelReferenceValue("+upperBound.map(_.toJava).mkString(" with ")+")"
    }

    /**
     * Determines if the type described by the first set of upper type bounds is
     * a subtype of the second type.
     */
    protected def isSubtypeOf(
        typeBoundsA: UpperBound,
        typeBoundsB: UpperBound): Boolean = {
        typeBoundsA.forall(aType ⇒
            typeBoundsB.exists(bType ⇒ domain.isSubtypeOf(aType, bType).yes))
    }

    protected def summarizeReferenceValues(
        pc: PC,
        values: Iterable[DomainValue]): DomainValue =
        (values.head.summarize(pc) /: values.tail) {
            (c, n) ⇒ c.summarize(pc, n)
        }

    //
    // INFORMATION ABOUT REFERENCE VALUES
    //

    abstract override def typeOfValue(value: DomainValue): TypesAnswer =
        value match {
            case that: IsReferenceValue ⇒ that
            case _                      ⇒ super.typeOfValue(value)
        }

    //
    // FACTORY METHODS
    //

    def newNullValue(pc: PC): DomainValue =
        MReferenceValue(UIDList.empty)

    def nonNullReferenceValue(pc: PC, objectType: ObjectType): DomainValue =
        AReferenceValue(objectType)

    def newReferenceValue(referenceType: ReferenceType): DomainValue =
        AReferenceValue(referenceType)

    def newReferenceValue(pc: PC, referenceType: ReferenceType): DomainValue =
        AReferenceValue(referenceType)

    def newObject(pc: PC, referenceType: ReferenceType): DomainValue =
        AReferenceValue(referenceType)

    def newInitializedObject(pc: PC, referenceType: ReferenceType): DomainValue =
        newObject(pc, referenceType)

    def newArray(pc: PC, referenceType: ArrayType): DomainValue =
        newInitializedObject(pc, referenceType)

    def newStringValue(pc: PC, value: String): DomainValue =
        AReferenceValue(ObjectType.String)

    def newClassValue(pc: PC, t: Type): DomainValue =
        AReferenceValue(ObjectType.Class)

}
