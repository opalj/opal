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
package la

import org.opalj.br.ObjectType
import org.opalj.br.FieldType
import org.opalj.ai.domain.TheProject
import org.opalj.ai.domain.ProjectBasedClassHierarchy
import org.opalj.ai.analyses.FieldValueInformation
import org.opalj.ai.domain.l0.TypeLevelFieldAccessInstructions

/**
 * Queries the project information to identify fields with refined field type information.
 *
 * @author Michael Eichberg (eichberg@informatik.tu-darmstadt.de)
 */
trait RefinedTypeLevelFieldAccessInstructions extends TypeLevelFieldAccessInstructions {
    domain: ReferenceValuesDomain with ValuesFactory with Configuration with TheProject with ProjectBasedClassHierarchy ⇒

    val fieldValueInformation: FieldValueInformation

    override def getfield(
        pc: PC,
        objectref: DomainValue,
        declaringClass: ObjectType,
        fieldName: String,
        fieldType: FieldType): Computation[DomainValue, ExceptionValue] = {

        val field = classHierarchy.resolveFieldReference(
            declaringClass, fieldName, fieldType, project
        )
        if (field.isDefined) {
            val fieldValue = fieldValueInformation.get(field.get)
            if (fieldValue.isDefined) {
                return doGetfield(pc, objectref, fieldValue.get.adapt(domain, pc))
            }
        }

        // fallback
        super.getfield(pc, objectref, declaringClass, fieldName, fieldType)
    }

    /**
     * Returns the field's value.
     */
    override def getstatic(
        pc: PC,
        declaringClass: ObjectType,
        fieldName: String,
        fieldType: FieldType): Computation[DomainValue, Nothing] = {
        val field = classHierarchy.resolveFieldReference(
            declaringClass, fieldName, fieldType, project
        )
        if (field.isDefined) {
            val fieldValue = fieldValueInformation.get(field.get)
            if (fieldValue.isDefined) {
                return doGetstatic(pc, fieldValue.get.adapt(domain, pc))
            }
        }

        super.getstatic(pc, declaringClass, fieldName, fieldType)
    }

}
