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
import de.tud.cs.st.bat.resolved.ai.ComputationWithSideEffectOrException

/**
 * This (partial-)domain implements the basic support for performing
 * computations related to reference values.
 *
 * @author Michael Eichberg
 */
trait TypeLevelReferenceValues[+I] extends Domain[I] with GeneralizedArrayHandling {
    domain: Configuration with IntegerValuesComparison ⇒

    /**
     * Abstracts over all values with computational type `reference`.
     */
    protected trait ReferenceValue extends Value with IsReferenceValue {
        this: DomainValue ⇒

        /**
         * Returns `ComputationalTypeReference`.
         */
        final override def computationalType: ComputationalType = ComputationalTypeReference

        override def summarize(pc: PC, value: DomainValue): DomainValue =
            this.join(pc, value) match {
                case SomeUpdate(value) ⇒ value
                case _                 ⇒ this
            }

        override def isNull: Answer = Unknown

        override def isPrecise: Boolean = false

        override def isValueSubtypeOf(referenceType: ReferenceType): Answer = Unknown

        /**
         * Adds a new, additional upper bound to this value's type.
         */
        def refineUpperTypeBound(pc: PC, supertype: ReferenceType): DomainValue

        def updateIsNull(pc: PC, isNull: Answer): DomainValue

    }

    protected trait NullValue extends ReferenceValue {
        this: DomainValue ⇒

        final override def referenceValues: Iterable[IsAReferenceValue] = Iterable(this)

        final override def isNull = Yes

        final override def isPrecise = true

        final override def upperTypeBound: UpperTypeBound = UIDList.empty

        final override def updateIsNull(pc: PC, isNull: Answer): DomainValue =
            domainException(domain, "this value is null; changing that doesn't make sense")

        final override def isValueSubtypeOf(referenceType: ReferenceType): Answer =
            domainException(domain, "isSubtypeOf is not defined for \"null\" values")

        override def refineUpperTypeBound(
            pc: PC,
            supertype: ReferenceType): DomainValue = this

        override def summarize(pc: PC): DomainValue = this

        override def adapt[ThatI >: I](target: Domain[ThatI], pc: PC): target.DomainValue =
            target.NullValue(pc)

        override def toString: String = "ReferenceValue(null)"
    }

    /**
     * A reference value that is associated with a single (upper) type (bound).
     *
     * @note This class was introduced for performance reasons.
     */
    protected trait SReferenceValue[T <: ReferenceType] extends ReferenceValue {
        this: DomainValue ⇒

        val theUpperTypeBound: T

        override def referenceValues: Iterable[IsAReferenceValue] = Iterable(this)

        override def upperTypeBound: UpperTypeBound = UIDList(theUpperTypeBound)

        override def isValueSubtypeOf(supertype: ReferenceType): Answer = {
            val isSubtypeOf = domain.isSubtypeOf(theUpperTypeBound, supertype)
            isSubtypeOf match {
                case Yes             ⇒ Yes
                case No if isPrecise ⇒ No
                case _               ⇒ Unknown
            }
        }

        override def summarize(pc: PC): DomainValue = this

        override def toString: String = "ReferenceValue("+theUpperTypeBound.toJava+")"

    }

    protected trait ObjectValue extends ReferenceValue {
        this: DomainValue ⇒

    }

    protected trait ArrayValue extends ReferenceValue {
        this: DomainValue ⇒

        /*ABSTRACT*/ def isAssignable(value: DomainValue): Answer

        /*ABSTRACT*/ def doLoad(
            pc: PC,
            index: DomainValue,
            potentialExceptions: ExceptionValues): ArrayLoadResult

        def load(pc: PC, index: DomainValue): ArrayLoadResult = {

            val validIndex =
                isSomeValueInRange(index, IntegerConstant0, getLength(pc))
            if (validIndex.no)
                return justThrows(ArrayIndexOutOfBoundsException(pc))

            var thrownExceptions = List.empty[ExceptionValue]
            if (validIndex.maybeNo && throwArrayIndexOutOfBoundsException)
                thrownExceptions = ArrayIndexOutOfBoundsException(pc) :: thrownExceptions
            if (isNull.maybeYes && throwNullPointerException)
                thrownExceptions = NullPointerException(pc) :: thrownExceptions

            doLoad(pc, index, thrownExceptions)
        }

        def doArraystore(
            pc: PC,
            value: DomainValue,
            index: DomainValue): Unit = { /* Empty by default. */ }

        /**
         * @note It is in general not necessary to override this method. If you need some
         *      special handling if a value is stored in an array, override the method
         *      `doArraystore`.
         * @see `doArraystore` for further information.
         */
        def store(
            pc: PC,
            value: DomainValue,
            index: DomainValue): ArrayStoreResult = {
            // the case "isNull == Yes" will not occur as the value "null" is always
            // represented by an instance of the respective class

            val validIndex =
                isSomeValueInRange(index, IntegerConstant0, getLength(pc))
            if (validIndex.no)
                return justThrows(ArrayIndexOutOfBoundsException(pc))

            if (isAssignable(value).no)
                return justThrows(ArrayStoreException(pc))

            var thrownExceptions = List.empty[ExceptionValue]
            if (validIndex.maybeNo && throwArrayIndexOutOfBoundsException)
                thrownExceptions = ArrayIndexOutOfBoundsException(pc) :: thrownExceptions
            if (isNull.maybeYes && throwNullPointerException)
                thrownExceptions = NullPointerException(pc) :: thrownExceptions

            doArraystore(pc, value, index)

            ComputationWithSideEffectOrException(thrownExceptions)
        }

        def getLength(pc: PC): DomainValue =
            IntegerValue(pc)

        def length(pc: PC): Computation[DomainValue, ExceptionValue] = {
            if (isNull == Unknown && throwNullPointerException)
                ComputedValueAndException(getLength(pc), NullPointerException(pc))
            else
                ComputedValue(getLength(pc))
        }
    }

    def asReferenceValue(value: DomainValue): ReferenceValue =
        value.asInstanceOf[ReferenceValue]

    def asObjectValue(value: DomainValue): ClassValue =
        value.asInstanceOf[ClassValue]

    def asArrayValue(value: DomainValue): ArrayValue =
        value.asInstanceOf[ArrayValue]

    //
    // QUESTION'S ABOUT VALUES
    //

    override def areEqualReferences(value1: DomainValue, value2: DomainValue): Answer =
        // we could check if it is conceivable that both values are not equal based 
        // on the available type information... However, if we only have a 
        // fragmented/incomplete class hierarchy, the information is most likely of limited
        // value
        Unknown

    final override def isValueSubtypeOf(
        value: DomainValue,
        supertype: ReferenceType): Answer =
        asReferenceValue(value).isValueSubtypeOf(supertype)

    /**
     * Determines the nullness-property of the given value.
     *
     * @param value A value of type `ReferenceValue`.
     */
    final override def isNull(value: DomainValue): Answer =
        asReferenceValue(value).isNull

    object NullValue {
        def unapply(value: DomainValue): Boolean = asReferenceValue(value).isNull.yes
    }

    // -----------------------------------------------------------------------------------
    //
    // ARRAY RELATED OPERATIONS
    //
    // -----------------------------------------------------------------------------------

    //
    // CREATE ARRAY
    //
    override def newarray(
        pc: PC,
        count: DomainValue,
        componentType: FieldType): Computation[DomainValue, DomainValue] = {
        //ComputedValueAndException(TypedValue(ArrayType(componentType)), TypedValue(ObjectType.NegativeArraySizeException))

        ComputedValue(NewArray(pc, ArrayType(componentType)))
    }

    /**
     * @note The componentType may be (again) an array type.
     */
    override def multianewarray(
        pc: PC,
        counts: List[DomainValue],
        arrayType: ArrayType): Computation[DomainValue, DomainValue] = {
        //ComputedValueAndException(TypedValue(arrayType), TypedValue(ObjectType.NegativeArraySizeException))
        ComputedValue(NewArray(pc, arrayType))
    }

    //
    // OPERATIONS ON ARRAYS
    // 

    /**
     * Loads the value stored in the array at the given index or throws an
     * exception (`NullPointerException` or `IndexOutOfBoundsException`).
     *
     * @note It is in general not necessary to override this method. If you need
     *      some special handling refine the trait `ArrayValue`.
     */
    override def arrayload(
        pc: PC,
        index: DomainValue,
        arrayRef: DomainValue): ArrayLoadResult = {
        asArrayValue(arrayRef).load(pc, index)
    }

    /**
     * Stores the given value in the array at the given index or throws an exception
     * (`NullPointerException`, `ArrayStoreException` or `IndexOutOfBoundsException`).
     *
     * @note It is in general not necessary to override this method. If you need
     *      some special handling refine the trait `ArrayValue`.
     */
    override def arraystore(
        pc: PC,
        value: DomainValue,
        index: DomainValue,
        arrayref: DomainValue): ArrayStoreResult = {
        asArrayValue(arrayref).store(pc, value, index)
    }

    /**
     * Returns the array's length or throws a `NullPointerException` if the given
     * reference is `null`.
     *
     * @note It is in general not necessary to override this method. If you need
     *      some special handling refine the trait `ArrayValue`.
     */
    override def arraylength(
        pc: PC,
        arrayref: DomainValue): Computation[DomainValue, ExceptionValue] = {
        asArrayValue(arrayref).length(pc)
    }

    // -----------------------------------------------------------------------------------
    //
    // ADDITIONAL FACTORY METHODS
    //
    // -----------------------------------------------------------------------------------

    /**
     * ==Summary==
     * The properties of the domain value are:
     *
     *  - Type: '''Precise'''
     *  - Null: '''No'''
     */
    def NewArray(pc: PC, arrayType: ArrayType): DomainValue

    /**
     *
     * ==Summary==
     * The properties of the domain value are:
     *
     *  - Type: '''Upper Bound'''
     *  - Null: '''MayBe'''
     */
    def ArrayValue(pc: PC, arrayType: ArrayType): DomainValue

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

    protected def updateIsNull(
        pc: PC,
        value: DomainValue,
        isNull: Answer,
        operands: Operands,
        locals: Locals): (Operands, Locals) = {
        val referenceValue: ReferenceValue = asReferenceValue(value)
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
object TypeLevelReferenceValues {

    val SerializableAndCloneable: UpperTypeBound =
        UIDList.empty + ObjectType.Serializable + ObjectType.Cloneable

}