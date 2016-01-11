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
package bugpicker
package core
package analysis

import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.FieldAccessInformation
import org.opalj.br.ClassFile
import org.opalj.br.ObjectType
import org.opalj.br.ConstantString
import org.opalj.br.analyses.StringConstantsInformation

/**
 * Identifies fields (static or instance) that are not used and which are also not useable.
 *
 * @author Michael Eichberg
 */
object UnusedFields {

    def apply(
        theProject:                 SomeProject,
        fieldAccessInformation:     FieldAccessInformation,
        stringConstantsInformation: StringConstantsInformation,
        classFile:                  ClassFile
    ): Seq[StandardIssue] = {

        val candidateFields = classFile.fields.filterNot { field ⇒
            // These fields are inlined by compilers; hence, even if the field is not accessed 
            // it may be used in the source code.
            (field.isSynthetic) ||
                (field.isFinal && field.fieldType.isBaseType) ||
                // The field is read at least once...
                (fieldAccessInformation.readAccesses(classFile, field).nonEmpty)
        }

        if (candidateFields.isEmpty)
            return Nil;

        val unusedFields = candidateFields.filterNot { field ⇒
            // test if the field defines a (probably inlined) constant string             
            field.isFinal && (field.fieldType eq ObjectType.String) &&
                {
                    field.constantFieldValue match {
                        case Some(ConstantString(value)) ⇒
                            stringConstantsInformation.get(value).isDefined
                        case _ ⇒
                            false
                    }
                }
        }

        val unusedAndNoReflectiveAccessFields = unusedFields.filterNot { field ⇒
            // Let's if we can find:
            //  - the field's name,
            //  - or the simpleName followed by the field's name
            //  - or the fully qualified name followed by the field's name
            // in the code; if so we assume that the field is reflectively accessed
            // ignore it
            val fieldName = field.name
            stringConstantsInformation.get(fieldName).isDefined || {
                val thisSimpleTypeName = classFile.thisType.simpleName.replace('$', '.')
                val qualifiedFieldName = thisSimpleTypeName + '.' + fieldName
                stringConstantsInformation.get(qualifiedFieldName).isDefined
            } || {
                val thisFullyQualifiedTypeName = classFile.thisType.toJava.replace('$', '.')
                val fullyQualifiedFieldName = thisFullyQualifiedTypeName + '.' + fieldName
                stringConstantsInformation.get(fullyQualifiedFieldName).isDefined
            }
        }

        val unusedAndUnusableFields = unusedAndNoReflectiveAccessFields
        // TODO!!!!

        for (unusedField ← unusedAndUnusableFields) yield {
            StandardIssue(
                "UnusedField",
                theProject,
                classFile,
                None,
                None,
                None,
                None,
                None,
                s"the field ${unusedField.toJava} is unused",
                None,
                Set(IssueCategory.Smell, IssueCategory.Comprehensibility),
                Set(IssueKind.Unused),
                Nil,
                Relevance.DefaultRelevance
            )
        }
    }
}
