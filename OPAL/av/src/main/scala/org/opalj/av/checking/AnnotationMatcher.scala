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
package av
package checking

import scala.collection.mutable.ArrayBuffer
import org.opalj.br._

/**
 * Matches an annotation of a field, method or class.
 *
 * @author Marco Torsello
 */
case class AnnotationMatcher(
        annotationType: FieldType,
        elementValuePairs: Option[ElementValuePairs]) {

    	/**
	 * Checks if the given annotation matches the one defined by this matcher.
	 * The type of the annotation should be equal. If the matcher defines specific
	 * [[ElementValuePairs]] the given annotation should have exactly the same
	 * ones regardless of the order.
	 * 
	 * ==Example Scenario==
     * If the matchers annotation type is `a` and the other annotations type is `b`
     * they should not match.
     * 
     * If the matcher defines specific [[ElementValuePairs]] like
     * `ArrayBuffer(ElementValuePair("target", ClassValue("org.opalj.av.checking.AnnotationMatcher")),
     * 				ElementValuePair("name", StringValue("Annotation_Matcher"))`
     * it should match even if the given annotations [[ElementValuePairs]] has a different order like
     * `ArrayBuffer(ElementValuePair("name", StringValue("Annotation_Matcher"),
     * 				ElementValuePair("target", ClassValue("org.opalj.av.checking.AnnotationMatcher")))`
     *     
     *  But it should not match if one/all of the two [[ElementValuePair]] are missing or there is
     *  another [[ElementValuePair]] not defined by this matcher.
	 */
    def doesMatch(otherAnnotation: Annotation): Boolean = {
        (otherAnnotation.annotationType eq annotationType) &&
            (elementValuePairs.isEmpty ||
                (otherAnnotation.elementValuePairs.size == elementValuePairs.get.size &&
                    elementValuePairs.get.forall(e ⇒
                        otherAnnotation.elementValuePairs.exists(_.toJava == e.toJava))))
    }

    override def toString: String = {
        elementValuePairs match {
            case Some(e) ⇒ "@"+annotationType.toJava+
                "("+e.foldLeft("")((s: String, ev: ElementValuePair) ⇒ s + ev.toJava)
            case _ ⇒ "@"+annotationType.toJava
        }
    }
}

object AnnotationMatcher {

    def apply(
        annotationType: String,
        elementValuePairs: Option[ElementValuePairs] = None): AnnotationMatcher = {
        this(ObjectType(annotationType.replace('.', '/')), elementValuePairs)
    }

    def apply(
        annotationType: String,
        elementValuePairs: ElementValuePairs): AnnotationMatcher = {
        this(ObjectType(annotationType.replace('.', '/')), Some(elementValuePairs))
    }

    def apply(
        annotationType: String,
        elementValuePairs: Map[String, ElementValue]): AnnotationMatcher = {

        var pairs: ElementValuePairs = ArrayBuffer[ElementValuePair]()
        elementValuePairs.map { element ⇒ pairs = pairs :+ ElementValuePair(element._1, element._2) }

        this(ObjectType(annotationType.replace('.', '/')),
            Some(pairs))
    }

}
