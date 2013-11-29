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

/**
 * A domain the performs computations w.r.t. reference values at the type level, but
 * which also traces the properties whether the type is precise or whether the value
 * is `null`.
 *
 * @author Michael Eichberg
 */
trait PreciseReferenceValues[+I] extends Domain[I] {

    /**
     * Abstracts over all values with computational type `reference`.
     */
    trait ReferenceValue extends Value { this: DomainValue ⇒

        /**
         * Returns `ComputationalTypeReference`.
         */
        final override def computationalType: ComputationalType = ComputationalTypeReference

        /**
         * The nullness property of this `ReferenceValue`.
         */
        def isNull: Answer

        /**
         * Indirectly called by BATAI when it determines that the `null`-value property
         * of this type-level reference should be updated.
         */
        def updateIsNull(pc: PC, isNull: Answer): DomainValue

        /**
         * Checks if the type of this value is a subtype of the specified
         * reference type under the assumption that this value is not null!
         *
         * Basically, this method implements the same semantics as the `ClassHierarchy`'s
         * `isSubtypeOf` method. But, additionally it checks if the type of this value
         * '''could be a subtype'' of the given supertype.
         *
         * For example, assume that the type of this reference value is
         * `java.util.Collection` and we know/ have to assume that this is only an
         * upper bound. In this case an answer is `No` if and only if it is impossible
         * that the runtime type is a subtype of the given supertype. This
         * condition holds, for example, for `java.io.File` which is not a subclass
         * of `java.util.Collection` and which does not have any further subclasses (in
         * the JDK). I.e., the classes `java.io.File` and `java.util.Collection` are
         * not in an inheritance relationship. However, if the specified supertype would be
         * `java.util.List` the answer would be unknown.
         */
        def isSubtypeOf(supertype: ReferenceType): Answer

        /**
         * Adds an upper bound. This call can be ignored if the type
         * information related to this value is precise, i.e., if we know that we
         * precisely capture the runtime type of this value.
         */
        def addUpperBound(pc: PC, upperBound: ReferenceType): DomainValue

        /**
         * Returns `true` if the type information about this value is precise.
         * I.e., if `isPrecise` returns `true` and the value's type is
         * reported to be `java.lang.Object` then the current value is known to be an
         * instance of the class `java.lang.Object` and of no other (sub)class.
         * Hence, for an interface type `isPrecise` will always return false.
         */
        def isPrecise: Boolean
    }

    //
    // QUESTION'S ABOUT VALUES
    //

    protected def asReferenceValue(value: DomainValue): ReferenceValue =
        value.asInstanceOf[ReferenceValue]

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
        val v1 = asReferenceValue(value1)
        val v2 = asReferenceValue(value2)
        val value1IsNull = v1.isNull
        val value2IsNull = v2.isNull
        if (value1IsNull.isDefined &&
            value2IsNull.isDefined &&
            (value1IsNull.yes || value2IsNull.yes)) {
            Answer(value1IsNull == value2IsNull)
        }  else {
            // TODO [IMPROVE - areEqualReferences] If the two values are not in a subtype relationship they cannot be equal.
            Unknown
        }
    }

    /**
     * Determines the nullness-property of the given value.
     *
     * @param value A value of type `ReferenceValue`.
     */
    def isNull(value: DomainValue): Answer = asReferenceValue(value).isNull

    def isSubtypeOf(
        value: DomainValue,
        supertype: ReferenceType): Answer = asReferenceValue(value).isSubtypeOf(supertype)

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

    // -----------------------------------------------------------------------------------
    //
    // HANDLING OF ARRAY RELATED COMPUTATIONS
    //
    // -----------------------------------------------------------------------------------

    def newArray(pc: PC, referenceType: ReferenceType): DomainValue

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
        ComputedValue(newArray(pc, arrayType))

    //
    // LOAD FROM AND STORE VALUE IN ARRAYS
    //
    def aaload(pc: PC, index: DomainValue, arrayref: DomainValue): ArrayLoadResult =
        typeOfValue(arrayref) match {
            case IsReferenceValueWithSingleBound(ArrayType(componentType)) ⇒
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
