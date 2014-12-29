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

import scala.collection.Map
import scala.collection.mutable.HashMap

import org.opalj.br.ObjectType
import org.opalj.br.analyses.Project
import org.opalj.ai.analyses.MutabilityRating._

/**
 * This analysis determines which classes in a project are immutable,
 * conditionally immutable or mutable. If this analysis cannot finally assess
 * the mutability of a class, the result will be unknown. In general, the analysis
 * will always only assess a class as conditionally immutable or immutable if the
 * analysis can guarantee the property to always hold.
 *
 * @author Andre Pacak
 * @author Michael Eichberg
 */
object ImmutabilityAnalysis {

    /**
     * Rates the mutability of all class files of the project.
     *
     * @param project the project that contains the classfiles that get classified.
     * @param isInterrupted a function that can interrupt the algorithm from the outside.
     * @return Map that maps from object type to immutabililty classification.
     */
    def doAnalyze(
        project: Project[URL],
        isInterrupted: () ⇒ Boolean = () ⇒ false): Map[ObjectType, MutabilityRating] = {
        val classHierarchy = project.classHierarchy
        val classification = HashMap.empty[ObjectType, MutabilityRating]

        //java.lang.Object is (by default) Immutable
        classification(ObjectType.Object) = Immutable

        def classify(objectType: ObjectType): MutabilityRating = {
            val classFile = project.classFile(objectType)
            if (objectType eq ObjectType.Object)
                return MutabilityRating.Immutable
            if (classification contains objectType)
                return classification(objectType)
            //objectType can not be classified
            if (classFile.isEmpty)
                return MutabilityRating.Unknown
            val fields = classFile.get.fields
            val allFieldsFinalAndBaseType = fields.forall {
                field ⇒
                    field.isFinal && field.fieldType.isBaseType
            }
            if (allFieldsFinalAndBaseType)
                MutabilityRating.Immutable
            else
                MutabilityRating.Unknown

        }

        def traverse(objectType: ObjectType): Unit = {
            if (isInterrupted())
                return ;
            var result = MutabilityRating.Unknown
            if (!(classification contains objectType)) {
                result = classify(objectType)
                classification(objectType) = result
            } else {
                result = classification(objectType)
            }

            val subtypes = classHierarchy.directSubtypesOf(objectType)
            result match {
                case (Immutable | ConditionallyImmutable) ⇒
                    subtypes.foreach {
                        subtype ⇒
                            traverse(subtype)
                    }
                case mutabilityRating @ _ ⇒
                    classHierarchy.allSubtypes(objectType, false).foreach { subtype ⇒
                        classification(subtype) = mutabilityRating
                    }
            }
        }

        //java.lang.Object is the root of the class hierarchy
        traverse(ObjectType.Object)
        classification
    }
}