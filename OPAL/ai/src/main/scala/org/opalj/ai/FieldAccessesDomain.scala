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

import org.opalj.br.ObjectType
import org.opalj.br.FieldType

/**
 *
 * @author Michael Eichberg (eichberg@informatik.tu-darmstadt.de)
 */
trait FieldAccessesDomain { this: CoreDomain ⇒

    /**
     * Returns the field's value and/or a new `NullPointerException` if the given
     * `objectref` represents the value `null`.
     *
     * @return The field's value or a new `NullPointerException`.
     */
    def getfield(
        pc: PC,
        objectref: DomainValue,
        declaringClass: ObjectType,
        name: String,
        fieldType: FieldType): Computation[DomainValue, ExceptionValue]

    /**
     * Returns the field's value.
     */
    def getstatic(
        pc: PC,
        declaringClass: ObjectType,
        name: String,
        fieldType: FieldType): Computation[DomainValue, Nothing]

    /**
     * Sets the field's value if the given `objectref` is not `null`(in the [[Domain]]).
     * In the latter case a `NullPointerException` is thrown.
     */
    def putfield(
        pc: PC,
        objectref: DomainValue,
        value: DomainValue,
        declaringClass: ObjectType,
        name: String,
        fieldType: FieldType): Computation[Nothing, ExceptionValue]

    /**
     * Sets the field's value.
     */
    def putstatic(
        pc: PC,
        value: DomainValue,
        declaringClass: ObjectType,
        name: String,
        fieldType: FieldType): Computation[Nothing, Nothing]

}
