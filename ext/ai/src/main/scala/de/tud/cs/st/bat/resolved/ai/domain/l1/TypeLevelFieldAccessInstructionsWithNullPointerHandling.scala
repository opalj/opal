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

import reflect.ClassTag

import de.tud.cs.st.util.{ Answer, Yes, No, Unknown }

/**
 * Implements the handling of field access instructions at the type level ignoring
 * potential linkage related exceptions but checking the `objectref`.
 *
 * @note By ignoring potentially thrown exceptions it may be the case that not all
 *      possible paths in a program are explored and the overall analysis may not be
 *      sound.
 *
 * @author Michael Eichberg (eichberg@informatik.tu-darmstadt.de)
 */
trait TypeLevelFieldAccessInstructionsWithNullPointerHandling { this: SomeDomain ⇒

    import ObjectType.NullPointerException

    def getfield(
        pc: PC,
        objectref: DomainValue,
        declaringClass: ObjectType,
        name: String,
        fieldType: FieldType): Computation[DomainValue, DomainValue] =
        isNull(objectref) match {
            case Yes ⇒
                ThrowsException(InitializedObject(pc, NullPointerException))
            case Unknown ⇒
                ComputedValueAndException(
                    TypedValue(pc, fieldType),
                    InitializedObject(pc, NullPointerException))
            case No ⇒
                ComputedValue(TypedValue(pc, fieldType))
        }

    def getstatic(
        pc: PC,
        declaringClass: ObjectType,
        name: String,
        fieldType: FieldType): Computation[DomainValue, Nothing] =
        ComputedValue(TypedValue(pc, fieldType))

    def putfield(
        pc: PC,
        objectref: DomainValue,
        value: DomainValue,
        declaringClass: ObjectType,
        name: String,
        fieldType: FieldType): Computation[Nothing, DomainValue] =
        isNull(objectref) match {
            case Yes ⇒
                ThrowsException(InitializedObject(pc, NullPointerException))
            case Unknown ⇒
                ComputationWithSideEffectOrException(
                    InitializedObject(pc, NullPointerException))
            case No ⇒
                ComputationWithSideEffectOnly
        }

    def putstatic(
        pc: PC,
        value: DomainValue,
        declaringClass: ObjectType,
        name: String,
        fieldType: FieldType): Computation[Nothing, Nothing] =
        ComputationWithSideEffectOnly

}