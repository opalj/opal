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

import org.opalj.util.{ Answer, Yes, No, Unknown }

import org.opalj.br.{ ObjectType, FieldType }

/**
 * Resolves references to final static fields that have simple constant values.
 *
 * '''However, a typical Java compiler automatically resolves all simple references
 * and, hence, this trait has for Java projects in general no effect.''' If we analyze
 * other languages that compile to the JVM platform, the effect might be different.
 *
 * @author Michael Eichberg (eichberg@informatik.tu-darmstadt.de)
 */
trait ConstantFieldValuesResolution[Source] extends Domain {
    domain: TheProject[Source] with ClassHierarchy ⇒

    abstract override def getstatic(
        pc: PC,
        classType: ObjectType,
        fieldName: String,
        fieldType: FieldType): Computation[DomainValue, Nothing] = {

        classHierarchy.resolveFieldReference(classType, fieldName, fieldType, project) match {
            case Some(field) if field.isFinal && field.isStatic &&
                (field.fieldType.isBaseType || (field.fieldType eq ObjectType.String)) ⇒
                field.constantFieldValue.map(cv ⇒
                    ComputedValue(ConstantFieldValue(pc, cv))
                ).getOrElse(
                    super.getstatic(pc, classType, fieldName, fieldType)
                )

            case _ ⇒
                super.getstatic(pc, classType, fieldName, fieldType)
        }
    }
}