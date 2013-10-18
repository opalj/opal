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
 * A domain that performs computations w.r.t. reference values at the type level.
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
        final override def computationalType: ComputationalType = ComputationalTypeReference

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
         * not in an inheritance relationship. However, if the specified supertype would 
         * be `java.util.List` the answer would be unknown.
         */
        def isSubtypeOf(supertype: ReferenceType): Answer

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

    def areEqualReferences(value1: DomainValue, value2: DomainValue): Answer =
        // we could check if it is conceivable that both value are not equal based 
        // on the available type information...
        Unknown

    protected def asReferenceValue(value: DomainValue): ReferenceValue =
        value.asInstanceOf[ReferenceValue]

    /**
     * Determines the nullness-property of the given value.
     *
     * @param value A value of type `ReferenceValue`.
     */
    def isNull(value: DomainValue): Answer = Unknown

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
    def multianewarray(pc: PC, counts: List[DomainValue], arrayType: ArrayType) =
        //ComputedValueAndException(TypedValue(arrayType), TypedValue(ObjectType.NegativeArraySizeException))
        ComputedValue(newArray(pc, arrayType))

    //
    // LOAD FROM AND STORE VALUE IN ARRAYS
    //
    def aaload(pc: PC, index: DomainValue, arrayref: DomainValue): ArrayLoadResult =
        types(arrayref) match {
            case TheTypeBound(ArrayType(componentType)) ⇒
                ComputedValue(newTypedValue(pc, componentType))
            case _ ⇒
                domainException(this, "component type unknown: "+arrayref)
        }

    def aastore(pc: PC, value: DomainValue, index: DomainValue, arrayref: DomainValue) =
        ComputationWithSideEffectOnly
}
