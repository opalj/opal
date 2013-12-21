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
package l0

import de.tud.cs.st.util.{ Answer, Yes, No, Unknown }

/**
 * This (partial-)domain implements the necessary support for performing
 * computations related to reference values.
 *
 * @author Michael Eichberg
 */
trait TypeLevelReferenceValues[+I] extends Domain[I] with GeneralizedArrayHandling {
    domain: Configuration with IntegerValuesComparison ⇒

    /**
     * Abstracts over all values with computational type `reference`.
     */
    trait ReferenceValue extends Value with IsReferenceValue { this: DomainValue ⇒

        /**
         * Returns `ComputationalTypeReference`.
         */
        final override def computationalType: ComputationalType = ComputationalTypeReference

        override def isNull: Answer = Unknown

        override def isPrecise: Boolean = false

        override def isSubtypeOf(referenceType: ReferenceType): Answer =
            Unknown

        /**
         * Adds a new, additional upper bound to this value's type.
         */
        def refineUpperTypeBound(pc: PC, supertype: ReferenceType): DomainValue

    }

    trait NullValue extends ReferenceValue { this: DomainValue ⇒

        final override def referenceValues: Iterable[IsAReferenceValue] = Iterable(this)

        final override def isNull = Yes

        final override def isPrecise = true

        final override def upperTypeBound: UpperTypeBound = UIDList.empty

        override def isSubtypeOf(referenceType: ReferenceType): Answer =
            domainException(domain, "isSubtypeOf is not defined for \"null\" values")

        override def refineUpperTypeBound(
            pc: PC,
            supertype: ReferenceType): DomainValue = this

        override def summarize(pc: PC, value: DomainValue): DomainValue =
            doJoin(pc, value) match {
                case NoUpdate             ⇒ this
                case SomeUpdate(newValue) ⇒ newValue
            }

        override def summarize(pc: PC): DomainValue = this

        override def adapt[ThatI >: I](target: Domain[ThatI], pc: PC): target.DomainValue =
            target.NullValue(pc)
    }

    trait ClassValue extends ReferenceValue { this: DomainValue ⇒

    }

    trait ArrayValue extends ReferenceValue { this: DomainValue ⇒

        def load(pc: PC, index: DomainValue): ArrayLoadResult

        def doArraystore(
            pc: PC,
            value: DomainValue,
            index: DomainValue): Unit = { /* Empty by default. */ }

        def isAssignable(value: DomainValue): Answer

        /**
         * @note It is in general not necessary to override this method. If you need some
         *      special handling if a value is stored in an array, override the method
         *      `doArraystore`.
         * @see `doArraystore` for furhter information.
         */
        def store(
            pc: PC,
            value: DomainValue,
            index: DomainValue): ArrayStoreResult = {
            val theArrayIsNull = isNull
            if (theArrayIsNull.yes)
                return justThrows(NullPointerException(pc))

            val validIndex =
                isSomeValueInRange(index, IntegerConstant0, getArraylength(pc))
            if (validIndex.no)
                return justThrows(ArrayIndexOutOfBoundsException(pc))

            if (isAssignable(value).no)
                return justThrows(ArrayStoreException(pc))

            var thrownExceptions = List.empty[ExceptionValue]
            if (validIndex.maybeNo && throwArrayIndexOutOfBoundsException)
                thrownExceptions = ArrayIndexOutOfBoundsException(pc) :: thrownExceptions
            if (theArrayIsNull.maybeYes && throwNullPointerException)
                thrownExceptions = NullPointerException(pc) :: thrownExceptions

            doArraystore(pc, value, index)

            Computation(thrownExceptions)
        }

        def getArraylength(pc: PC): DomainValue =
            IntegerValue(pc)

        /**
         * @note If the domain supports a more precise handling of arrays and can
         *      return the length of an array, this method  needs to be overridden.
         */
        def length(pc: PC): Computation[DomainValue, ExceptionValue] = {
            isNull match {
                case Yes ⇒
                    throws(NullPointerException(pc))
                case Unknown if throwNullPointerException ⇒
                    ComputedValueAndException(
                        getArraylength(pc),
                        NullPointerException(pc))
                case _ /*No | (Unknown if !throwNullPointerException)*/ ⇒
                    ComputedValue(getArraylength(pc))
            }
        }
    }

    def asReferenceValue(value: DomainValue): ReferenceValue =
        value.asInstanceOf[ReferenceValue]

    def asClassValue(value: DomainValue): ClassValue =
        value.asInstanceOf[ClassValue]

    def asArrayValue(value: DomainValue): ArrayValue =
        value.asInstanceOf[ArrayValue]

    //
    // QUESTION'S ABOUT VALUES
    //

    def areEqualReferences(value1: DomainValue, value2: DomainValue): Answer =
        // we could check if it is conceivable that both values are not equal based 
        // on the available type information... However, if we only have a 
        // fragmented/incomplete class hierarchy, the information is most likely of limited
        // value
        Unknown

    /**
     * @param value A reference type value.
     */
    final override def isSubtypeOf(value: DomainValue, supertype: ReferenceType): Answer =
        asReferenceValue(value).isSubtypeOf(supertype)

    /**
     * Determines the nullness-property of the given value.
     *
     * @param value A value of type `ReferenceValue`.
     */
    final override def isNull(value: DomainValue): Answer =
        asReferenceValue(value).isNull

    object NullValue {
        def unapply(dv: ReferenceValue): Boolean = dv.isNull.yes
    }

    //
    // OPERATIONS
    // 

    override def arrayload(
        pc: PC,
        index: DomainValue,
        arrayRef: DomainValue): ArrayLoadResult = {
        asArrayValue(arrayRef).load(pc, index)
    }

    /**
     * @note It is in general not necessary to override this method. If you need some
     *      special handling if a value is stored in an array, override the method
     *      `doArraystore`.
     * @see `doArraystore` for furhter information.
     */
    override def arraystore(
        pc: PC,
        value: DomainValue,
        index: DomainValue,
        arrayref: DomainValue): ArrayStoreResult = {
        asArrayValue(arrayref).store(pc, value, index)
    }

    /**
     * @note If the domain supports a more precise handling of arrays and can
     *      return the length of an array, this method  needs to be overridden.
     */
    override def arraylength(
        pc: PC,
        arrayref: DomainValue): Computation[DomainValue, ExceptionValue] = {
        asArrayValue(arrayref).length(pc)
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
        val referenceValue: ReferenceValue = asReferenceValue(value)
        val newReferenceValue = referenceValue.refineUpperTypeBound(pc, bound)
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

}
