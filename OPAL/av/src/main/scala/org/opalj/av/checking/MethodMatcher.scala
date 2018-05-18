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
package av
package checking

import scala.collection.Set
import org.opalj.br.ClassFile
import org.opalj.br.Method
import org.opalj.br.analyses.SomeProject
import org.opalj.br.VirtualSourceElement

/**
 * Matches methods based on their attributes, annotations and class.
 *
 * @author Marco Torsello
 */
case class MethodMatcher(
        classLevelMatcher:    ClassLevelMatcher              = AllClasses,
        annotationsPredicate: AnnotationsPredicate           = AnyAnnotations,
        methodPredicate:      SourceElementPredicate[Method] = AnyMethod
)
    extends SourceElementsMatcher {

    def doesClassFileMatch(classFile: ClassFile)(implicit project: SomeProject): Boolean = {
        classLevelMatcher.doesMatch(classFile)
    }

    def doesMethodMatch(method: Method): Boolean = {
        annotationsPredicate(method.annotations) &&
            methodPredicate(method)
    }

    def extension(implicit project: SomeProject): Set[VirtualSourceElement] = {
        val allMatchedMethods = project.allClassFiles collect {
            case classFile if doesClassFileMatch(classFile) ⇒
                classFile.methods collect {
                    case m if doesMethodMatch(m) ⇒ m.asVirtualMethod(classFile.thisType)
                }
        }
        allMatchedMethods.flatten.toSet
    }

}

/**
 * Defines several additional factory methods to facilitate the creation of
 * [[MethodMatcher]]s.
 *
 * @author Marco Torsello
 */
object MethodMatcher {

    def apply(methodPredicate: SourceElementPredicate[_ >: Method]): MethodMatcher = {
        new MethodMatcher(methodPredicate = methodPredicate)
    }

    def apply(
        annotationsPredicate: AnnotationsPredicate,
        methodPredicate:      SourceElementPredicate[_ >: Method]
    ): MethodMatcher = {
        new MethodMatcher(annotationsPredicate = annotationsPredicate, methodPredicate = methodPredicate)
    }

    def apply(
        classLevelMatcher: ClassLevelMatcher,
        methodPredicate:   SourceElementPredicate[Method]
    ): MethodMatcher = {
        new MethodMatcher(classLevelMatcher, methodPredicate = methodPredicate)
    }

    def apply(
        annotationsPredicate: AnnotationsPredicate
    ): MethodMatcher = {
        new MethodMatcher(annotationsPredicate = annotationsPredicate)
    }

    /**
     * Creates a MethodMatcher, that relies on an AllAnnotationsPredicate for matching
     * the given AnnotationPredicate.
     */
    def apply(annotationPredicate: AnnotationPredicate): MethodMatcher = {
        apply(HasAtLeastTheAnnotations(annotationPredicate))
    }

    def apply(
        classLevelMatcher:   ClassLevelMatcher,
        annotationPredicate: AnnotationPredicate
    ): MethodMatcher = {
        new MethodMatcher(classLevelMatcher, HasAtLeastTheAnnotations(annotationPredicate))
    }

}
