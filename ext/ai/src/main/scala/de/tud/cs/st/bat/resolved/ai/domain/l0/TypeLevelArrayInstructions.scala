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

/**
 * Loading and storing values in/from arrays are handled at the type level and
 * will not throw an exception. I.e., no checks are done w.r.t. `NullPointerExceptions`,
 * `ArrayStoreExceptions`, `ArrayIndexOutOfBoundsExceptions` and
 * `NegativeArraySizeExceptions`; an array load/store always succeeds and returns
 * a value of the respective type.
 *
 * (Linkage related exceptions are currently generally ignored.)
 *
 * @note By ignoring potentially thrown exceptions it may be the case that not all
 *      possible paths in a program are explored and the overall analysis may not be
 *      sound.
 *
 * @author Michael Eichberg
 */
trait TypeLevelArrayInstructions { this: Domain[_] ⇒

    //
    // STORING VALUES IN AND LOADING VALUES FROM ARRAYS
    //

    override def aaload(pc: PC, index: DomainValue, arrayref: DomainValue): ArrayLoadResult =
        typeOfValue(arrayref) match {
            case IsReferenceValueWithSingleBound(ArrayType(componentType)) ⇒
                ComputedValue(TypedValue(pc, componentType))
            case _ ⇒
                domainException(this, "component type unknown: "+arrayref)
        }

    override def aastore(pc: PC, value: DomainValue, index: DomainValue, arrayref: DomainValue) =
        ComputationWithSideEffectOnly

    override def baload(
        pc: PC,
        index: DomainValue,
        arrayref: DomainValue): ArrayLoadResult = {
        val refIsNull = isNull(arrayref)
        if (refIsNull.yes) {
            ThrowsException(Seq(InitializedObject(pc, ObjectType.NullPointerException)))
        } else {
            // in this branch we ingore the maybeYes case...
            // this is a byte or boolean load!
            typeOfValue(arrayref) match {
                case IsReferenceValue(allUpperBounds) ⇒
                    allUpperBounds foreach { perValueUpperBounds ⇒
                        perValueUpperBounds.upperBound foreach { upperBound ⇒
                            // all further types have to be the same!
                            if (upperBound.isArrayType)
                                return ComputedValue(
                                    TypedValue(pc, upperBound.asArrayType.componentType))
                        }
                    }
                    domainException(this, "type information missing: "+arrayref)
                case _ ⇒
                    domainException(this, "array with unknown type: "+arrayref)
            }
        }
    }
    override def bastore(pc: PC, value: DomainValue, index: DomainValue, arrayref: DomainValue) =
        ComputationWithSideEffectOnly

    override def caload(pc: PC, index: DomainValue, arrayref: DomainValue): ArrayLoadResult =
        ComputedValue(CharValue(pc))
    override def castore(pc: PC, value: DomainValue, index: DomainValue, arrayref: DomainValue) =
        ComputationWithSideEffectOnly

    override def daload(pc: PC, index: DomainValue, arrayref: DomainValue): ArrayLoadResult =
        ComputedValue(DoubleValue(pc))
    override def dastore(pc: PC, value: DomainValue, index: DomainValue, arrayref: DomainValue) =
        ComputationWithSideEffectOnly

    override def faload(pc: PC, index: DomainValue, arrayref: DomainValue): ArrayLoadResult =
        ComputedValue(FloatValue(pc))
    override def fastore(pc: PC, value: DomainValue, index: DomainValue, arrayref: DomainValue) =
        ComputationWithSideEffectOnly

    override def iaload(pc: PC, index: DomainValue, arrayref: DomainValue): ArrayLoadResult =
        ComputedValue(IntegerValue(pc))
    override def iastore(pc: PC, value: DomainValue, index: DomainValue, arrayref: DomainValue) =
        ComputationWithSideEffectOnly

    override def laload(pc: PC, index: DomainValue, arrayref: DomainValue): ArrayLoadResult =
        ComputedValue(LongValue(pc))
    override def lastore(pc: PC, value: DomainValue, index: DomainValue, arrayref: DomainValue) =
        ComputationWithSideEffectOnly

    override def saload(pc: PC, index: DomainValue, arrayref: DomainValue): ArrayLoadResult =
        ComputedValue(ShortValue(pc))
    override def sastore(pc: PC, value: DomainValue, index: DomainValue, arrayref: DomainValue) =
        ComputationWithSideEffectOnly

    //
    // LENGTH OF AN ARRAY
    //
    override def arraylength(pc: PC, value: DomainValue): Computation[DomainValue, DomainValue] =
        ComputedValue(IntegerValue(pc))
}


