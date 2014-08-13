/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
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
package org.opalj
package ai
package domain
package l0

import org.opalj.br.FieldType
import org.opalj.br.ObjectType
import org.opalj.util.Unknown
import org.opalj.util.Yes

/**
 * Implements the handling of field access instructions at the type level.
 *
 * (Linkage related exceptions are currently generally ignored.)
 *
 * @author Michael Eichberg (eichberg@informatik.tu-darmstadt.de)
 */
trait TypeLevelFieldAccessInstructions extends FieldAccessesDomain {
    domain: ReferenceValuesDomain with TypedValuesFactory with Configuration ⇒

    /*override*/ def getfield(
        pc: PC,
        objectref: DomainValue,
        declaringClass: ObjectType,
        name: String,
        fieldType: FieldType): Computation[DomainValue, ExceptionValue] =
        refIsNull(pc, objectref) match {
            case Yes ⇒ throws(NullPointerException(pc))
            case Unknown if throwNullPointerExceptionOnFieldAccess ⇒
                ComputedValueOrException(TypedValue(pc, fieldType), NullPointerException(pc))
            case _ ⇒
                ComputedValue(TypedValue(pc, fieldType))
        }

    /*override*/ def getstatic(
        pc: PC,
        declaringClass: ObjectType,
        name: String,
        fieldType: FieldType): Computation[DomainValue, Nothing] =
        ComputedValue(TypedValue(pc, fieldType))

    /*override*/ def putfield(
        pc: PC,
        objectref: DomainValue,
        value: DomainValue,
        declaringClass: ObjectType,
        name: String,
        fieldType: FieldType): Computation[Nothing, ExceptionValue] =
        refIsNull(pc, objectref) match {
            case Yes ⇒
                throws(NullPointerException(pc))
            case Unknown if throwNullPointerExceptionOnFieldAccess ⇒
                ComputationWithSideEffectOrException(NullPointerException(pc))
            case _ ⇒
                ComputationWithSideEffectOnly
        }

    /*override*/ def putstatic(
        pc: PC,
        value: DomainValue,
        declaringClass: ObjectType,
        name: String,
        fieldType: FieldType): Computation[Nothing, Nothing] =
        ComputationWithSideEffectOnly

}