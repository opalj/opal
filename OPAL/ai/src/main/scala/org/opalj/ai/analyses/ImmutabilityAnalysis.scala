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
package analyses

import java.net.URL
import scala.collection.mutable.HashMap
import org.opalj.br.ClassFile
import org.opalj.br.Field
import org.opalj.br.ObjectType
import org.opalj.br.analyses.Project

object Immutability extends Enumeration {
    type Immutability = Value
    val Immutable, Mutable, ConditionallyImmutable, Unknown = Value
}

/**
 * An analysis that determines which classes in a project are immutable,
 * conditionally immutable, mutable and cannot be classified.
 *
 * We traverse of the class hierarchy. We classify java.lang.Object as
 * Immutability.Immutable manually because it contains non-static native methods
 * and would be normally be classified as Immutability.Mutable.
 *
 * If a object type gets classified as Immutability.Mutable or Immutability.Unknown
 * all subtypes get classified the same as the supertype.
 *
 * An object type gets classified as Immutability.Immutable if all fields are final and
 * of base type. Every other object type gets classified as Immutability.Unknown.
 *
 * @author Andre Pacak
 */
object ImmutabilityAnalysis {

    /**
     * This method classifies the Immutability of all object types that the
     * project contains.
     *
     * @param project the project that contains the classfiles that get classified.
     * @param isInterrupted a function that can interrupt the algorithm from the outside.
     * @return Map that maps from object type to immutabililty classification.
     */
    def doAnalyze(
        project: Project[URL],
        isInterrupted: () ⇒ Boolean): HashMap[ObjectType, Immutability.Value] = {

        val objectClassification = HashMap.empty[ObjectType, Immutability.Value]

        //insert java.lang.Object
        //assign Immutability.Immutable even when it contains native methods.
        objectClassification(ObjectType.Object) = Immutability.Immutable

        def traverse(objectType: ObjectType): Unit = {
            if (isInterrupted())
                return
            var result = Immutability.Unknown
            if (!(objectClassification contains objectType)) {
                result = classify(objectType)
                objectClassification(objectType) = result
            } else {
                result = objectClassification(objectType)
            }

            val subtypes = project.classHierarchy.directSubtypesOf(objectType)
            result match {
                case (Immutability.Immutable | Immutability.ConditionallyImmutable) ⇒
                    subtypes.foreach {
                        subtype ⇒
                            traverse(subtype)
                    }
                case classification @ _ ⇒
                    project.classHierarchy.allSubtypes(objectType, false).foreach {
                        subtype ⇒
                            objectClassification(subtype) = classification
                    }
            }
        }

        def classify(objectType: ObjectType): Immutability.Value = {
            val classFile = project.classFile(objectType)
            if (objectType == ObjectType.Object)
                return Immutability.Immutable
            if (objectClassification contains objectType)
                return objectClassification(objectType)
            //objectType can not be classified
            if (classFile.isEmpty)
                return Immutability.Unknown
            val fields = classFile.get.fields
            val allFieldsFinalAndBaseType = fields.forall {
                field ⇒
                    field.isFinal && field.fieldType.isBaseType
            }
            if (allFieldsFinalAndBaseType)
                Immutability.Immutable
            else
                Immutability.Unknown

        }

        //java.lang.Object is the root of the class hierarchy
        traverse(ObjectType.Object)
        objectClassification
    }
}