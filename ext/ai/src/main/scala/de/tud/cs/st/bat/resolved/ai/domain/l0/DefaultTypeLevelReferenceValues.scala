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

    // -----------------------------------------------------------------------------------
    //
    // REPRESENTATION OF REFERENCE VALUES
    //
    // -----------------------------------------------------------------------------------

    case class AReferenceValue protected[DefaultTypeLevelReferenceValues] (
        val upperBound: UpperBound)
            extends super.ReferenceValue
            with IsReferenceValue { self: DomainValue ⇒

        override def upperBounds: Iterable[ValueBasedUpperBound] =
            Iterable(
                new ValueBasedUpperBound {
                    override def isNull: Answer = Unknown
                    override def isPrecise: Boolean = false
                    override def upperBound: UpperBound = self.upperBound
                    override def isSubtypeOf(referenceType: ReferenceType): Answer =
                        self.isSubtypeOf(referenceType)
                }
            )

        def hasSingleBound: Option[ReferenceType] =
            if (upperBound.size == 1)
                Some(upperBound.head)
            else
                None

        /**
         * Determines if this value is a subtype of the given supertype by
         * delegating to the `isSubtypeOf(ReferenceType,ReferenceType)` method of the
         * domain.
         *
         * @note This is a very basic implementation that cannot determine that this
         * 		value is '''not''' a subtype of the given type as this implementation
         *   	does not distinguish between class types and interface types.
         */
        override def isSubtypeOf(supertype: ReferenceType): Answer =
            if (upperBound exists { tb ⇒ domain.isSubtypeOf(tb, supertype).yes })
                Yes
            else
                Unknown

        override def addUpperBound(
            pc: PC,
            theUpperBound: ReferenceType): AReferenceValue = {
            assume(!upperBound.contains(theUpperBound))

            isSubtypeOf(theUpperBound) match {
                case Yes ⇒ this
                case No if upperBound.forall(domain.isSubtypeOf(theUpperBound, _).yes) ⇒
                    AReferenceValue(Set(theUpperBound))
                case _ ⇒
                    var newValueTypes = Set.empty[ReferenceType]
                    var addTheUpperBound = true
                    upperBound.foreach { anUpperBound ⇒
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

                    AReferenceValue(newValueTypes)
            }
        }

        override def doJoin(mergePC: Int, value: DomainValue): Update[DomainValue] = {
            val AReferenceValue(otherTypeBounds) = value
            if (this.upperBound == otherTypeBounds)
                return NoUpdate

            var newTypeBounds = this.upperBound
            otherTypeBounds.foreach { otherTypeBound ⇒
                var addOtherTypeBounds = true
                newTypeBounds = newTypeBounds.filterNot { vt ⇒
                    domain.isSubtypeOf(otherTypeBound, vt) match {
                        case Yes ⇒
                            true
                        case _ ⇒
                            if (domain.isSubtypeOf(vt, otherTypeBound).yes)
                                addOtherTypeBounds = false
                            false
                    }
                }
                if (addOtherTypeBounds)
                    newTypeBounds = newTypeBounds + otherTypeBound
            }
            StructuralUpdate(AReferenceValue(newTypeBounds))
        }

        override def adapt[ThatI >: I](
            targetDomain: Domain[ThatI],
            pc: PC): targetDomain.DomainValue =
            targetDomain match {
                case thatDomain: DefaultTypeLevelReferenceValues[ThatI] ⇒
                    adaptAReferenceValue(thatDomain, pc).asInstanceOf[targetDomain.DomainValue]
                case _ ⇒ super.adapt(targetDomain, pc)
            }

        protected[DefaultTypeLevelReferenceValues] def adaptAReferenceValue[ThatI >: I](
            targetDomain: DefaultTypeLevelReferenceValues[ThatI],
            pc: PC): targetDomain.AReferenceValue =
            targetDomain.AReferenceValue(this.upperBound)

        override def summarize(pc: PC): DomainValue = this

        override def summarize(pc: PC, value: DomainValue): DomainValue = {
            if (this eq value)
                return this

            val AReferenceValue(otherTypeBounds) = value
            if (this.upperBound == otherTypeBounds ||
                domain.isSubtypeOf(otherTypeBounds, this.upperBound)) {
                this
            } else if (domain.isSubtypeOf(this.upperBound, otherTypeBounds)) {
                value
            } else {
                AReferenceValue(null: ReferenceType) // TODO calculate the least upper bound...
            }
        }

        override def equals(other: Any): Boolean =
            other match {
                case that: AReferenceValue ⇒
                    (that eq this) || (
                        (that canEqual this) &&
                        this.upperBound == that.upperBound)
                case _ ⇒ false
            }

        protected def canEqual(other: AReferenceValue): Boolean = true

        override def hashCode: Int =
            (upperBound.hashCode() * 41)

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
            case r: AReferenceValue ⇒ r
            case _                  ⇒ super.typeOfValue(value)
        }

    //
    // FACTORY METHODS
    //

    final def AReferenceValue(referenceType: ReferenceType): AReferenceValue =
        AReferenceValue(Set(referenceType))

    def newNullValue(pc: PC): DomainValue =
        AReferenceValue(Set.empty[ReferenceType])

    def newReferenceValue(referenceType: ReferenceType): DomainValue =
        AReferenceValue(Set(referenceType))

    def newReferenceValue(pc: PC, referenceType: ReferenceType): DomainValue =
        AReferenceValue(Set(referenceType))

    def newObject(pc: PC, referenceType: ReferenceType): DomainValue =
        AReferenceValue(Set[ReferenceType](referenceType))

    def newInitializedObject(pc: PC, referenceType: ReferenceType): DomainValue =
        newObject(pc, referenceType)

    def newArray(pc: PC, referenceType: ArrayType): DomainValue =
        newInitializedObject(pc, referenceType)

    def newStringValue(pc: PC, value: String): DomainValue =
        AReferenceValue(Set[ReferenceType](ObjectType.String))

    def newClassValue(pc: PC, t: Type): DomainValue =
        AReferenceValue(Set[ReferenceType](ObjectType.Class))

}
