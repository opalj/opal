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
import scala.collection.concurrent.{ TrieMap ⇒ ConcurrentMap }
import org.opalj.br.ObjectType
import org.opalj.br.analyses.Project
import org.opalj.ai.analyses.MutabilityRating._
import org.opalj.br.ClassFile

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

    private def fieldBasedRating(
        classFile: ClassFile,
        superclassTypeRating: MutabilityRating): MutabilityRating = {

        assert(!(classFile.thisType eq ObjectType.Object))
        assert(!classFile.isInterfaceDeclaration)
        assert(superclassTypeRating.id > Mutable.id)

        var rating = superclassTypeRating

        classFile.fields.foreach { field ⇒
            if (field.isFinal) {
                if (field.fieldType.isReferenceType)
                    rating = ConditionallyImmutable
                // else 
                //  (field.isBaseType === true) => nothing to do
            } else {
                return Unknown;
            }
        }

        rating
    }

    /**
     * Rates the mutability of all class files of the project.
     *
     * @param project The project that we are analyzing.
     * @param isInterrupted A function that can interrupt the algorithm from the outside.
     * @return A map with mutability ratings for class types.
     */
    def doAnalyze(
        project: Project[URL],
        isInterrupted: () ⇒ Boolean = () ⇒ false): Map[ObjectType, MutabilityRating] = {
        val classHierarchy = project.classHierarchy
        import classHierarchy.foreachDirectSubclass

        val classification = ConcurrentMap.empty[ObjectType, MutabilityRating]
        classification(ObjectType.Object) = Immutable // initial configuration

        def traverse(classFile: ClassFile, superclassTypeRating: MutabilityRating): Unit = {
            if (isInterrupted())
                return ;

            val classType = classFile.thisType
            fieldBasedRating(classFile, superclassTypeRating) match {
                case r @ (Immutable | ConditionallyImmutable) ⇒
                    classification(classType) = r
                    foreachDirectSubclass(classType, project) { subclass ⇒
                        traverse(subclass, superclassTypeRating)
                    }
                case r @ (Mutable | Unknown) ⇒
                    classification(classType) = r
                    classHierarchy.foreachSubtype(classType) { subclassType ⇒
                        classification(subclassType) = r
                    }
            }
        }

        // 1. Do the basic classification
        // "java.lang.Object" is at the root of the class hierarchy and we can only 
        // assess classes for which all super class type information exists.
        foreachDirectSubclass(ObjectType.Object, project) { subclass ⇒
            traverse(subclass, Immutable)
        }

        // 2. (Re-)Analyze all class files marked as ConditionallyImmutable to 
        // check if the transitive hull only contains ConditionallyImmutable or Immutable
        // classes. If so, the class can be reranked to Immutable.

        // TODO... Fixpoint computation

        // we are done...
        classification
    }
}