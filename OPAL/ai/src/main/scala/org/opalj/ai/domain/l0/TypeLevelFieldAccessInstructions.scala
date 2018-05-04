/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
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
        pc:             Int,
        objectref:      DomainValue,
        declaringClass: ObjectType,
        fieldName:      String,
        fieldType:      FieldType
    ): Computation[DomainValue, ExceptionValue] =
        doGetfield(pc, objectref, TypedValue(pc, fieldType))

    /*override*/ def doGetfield(
        pc:         Int,
        objectref:  DomainValue,
        fieldValue: DomainValue
    ): Computation[DomainValue, ExceptionValue] = {

        refIsNull(pc, objectref) match {
            case Yes ⇒ throws(VMNullPointerException(pc))
            case Unknown if throwNullPointerExceptionOnFieldAccess ⇒
                ComputedValueOrException(fieldValue, VMNullPointerException(pc))
            case _ ⇒ ComputedValue(fieldValue)
        }
    }

    /*override*/ def getstatic(
        pc:             Int,
        declaringClass: ObjectType,
        fieldName:      String,
        fieldType:      FieldType
    ): Computation[DomainValue, Nothing] = {
        doGetstatic(pc, TypedValue(pc, fieldType))
    }

    /*override*/ def doGetstatic(
        pc:         Int,
        fieldValue: DomainValue
    ): Computation[DomainValue, Nothing] = {
        ComputedValue(fieldValue)
    }

    /*override*/ def putfield(
        pc:             Int,
        objectref:      DomainValue,
        value:          DomainValue,
        declaringClass: ObjectType,
        fieldName:      String,
        fieldType:      FieldType
    ): Computation[Nothing, ExceptionValue] =
        refIsNull(pc, objectref) match {
            case Yes ⇒ throws(VMNullPointerException(pc))
            case Unknown if throwNullPointerExceptionOnFieldAccess ⇒
                ComputationWithSideEffectOrException(VMNullPointerException(pc))
            case _ ⇒ ComputationWithSideEffectOnly
        }

    /*override*/ def putstatic(
        pc:             Int,
        value:          DomainValue,
        declaringClass: ObjectType,
        fieldName:      String,
        fieldType:      FieldType
    ): Computation[Nothing, Nothing] =
        ComputationWithSideEffectOnly

}
