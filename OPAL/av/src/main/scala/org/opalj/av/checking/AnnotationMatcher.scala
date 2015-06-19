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

import org.opalj.br._

/**
 * Matches an annotation of a class, field, method or method parameter.
 *
 * {{{
 * scala> val am = org.opalj.av.checking.AnnotationMatcher("java.lang.Foo",Map("clazz" -> org.opalj.br.StringValue("")))
 * am: org.opalj.av.checking.AnnotationMatcher = @java.lang.Foo(clazz="")
 * scala> am.doesMatch(org.opalj.br.Annotation(org.opalj.br.ObjectType("java/lang/Foo"),IndexedSeq(org.opalj.br.ElementValuePair("clazz",org.opalj.br.StringValue("")))))
 * res: Boolean = true
 * scala> am.doesMatch(org.opalj.br.Annotation(org.opalj.br.ObjectType("java/lang/Foo"),IndexedSeq(org.opalj.br.ElementValuePair("clazz",org.opalj.br.StringValue("-+-")))))
 * res: Boolean = false
 *
 * // match the annotation independent of the number of specified annotations
 * scala> val am = org.opalj.av.checking.AnnotationMatcher("java.lang.Foo")
 * am: org.opalj.av.checking.AnnotationMatcher = @java.lang.Foo
 * scala> am.doesMatch(org.opalj.br.Annotation(org.opalj.br.ObjectType("java/lang/Foo"),IndexedSeq(org.opalj.br.ElementValuePair("clazz",org.opalj.br.StringValue(" ")))))
 * res: Boolean = true
 *
 * // match the annotation only if no values are specified
 * scala> val am = org.opalj.av.checking.AnnotationMatcher("java.lang.Foo",Some(IndexedSeq.empty))
 * am: org.opalj.av.checking.AnnotationMatcher = @java.lang.Foo()
 * scala> am.doesMatch(org.opalj.br.Annotation(org.opalj.br.ObjectType("java/lang/Foo"),IndexedSeq(org.opalj.br.ElementValuePair("clazz",org.opalj.br.StringValue(" ")))))
 * res: Boolean = false
 * }}}
 * @author Marco Torsello
 */
case class AnnotationMatcher(
        annotationType: FieldType,
        elementValuePairs: Option[ElementValuePairs]) {

    /**
     * Checks if the given annotation is matched by this matcher.
     * The given annotation is matched if it has this matcher's specified type
     * and – if this matcher defines an [[org.opalj.br.ElementValuePair]]s matcher -
     * if all element value pairs are matched.	When matching element value
     * pairs the order is ignored.
     *
     * ==Example Scenarios==
     *  - If the matcher's annotation type is `A` and the other annotation's type is `B`
     *    then the elements will not match.
     *
     *  - If the matcher defines specific [[org.opalj.br.ElementValuePair]]s such as
     * 		`ArrayBuffer(ElementValuePair("target", ClassValue("org.opalj.av.checking.AnnotationMatcher")),
     * 				ElementValuePair("name", StringValue("Annotation_Matcher"))`
     * 		it will then match annotations where the [[org.opalj.br.ElementValuePair]]s have a different order:
     * 		`ArrayBuffer(ElementValuePair("name", StringValue("Annotation_Matcher"),
     * 				ElementValuePair("target", ClassValue("org.opalj.av.checking.AnnotationMatcher")))`
     *  	But it will not match if one or both of the two [[org.opalj.br.ElementValuePair]]s are missing or there is
     *  	another [[org.opalj.br.ElementValuePair]] not defined by this matcher.
     */
    def doesMatch(other: Annotation): Boolean = {
        (other.annotationType eq this.annotationType) &&
            (this.elementValuePairs.isEmpty ||
                (other.elementValuePairs.size == this.elementValuePairs.get.size &&
                    this.elementValuePairs.get.forall(e ⇒
                        other.elementValuePairs.exists(_ == e))))
    }

    def toDescription: String = {
        val at = "@"+annotationType.toJava
        if (elementValuePairs.isDefined)
            elementValuePairs.get.map(_.toJava).mkString(at+"(", ",", ")")
        else
            at

    }

}

/**
 * Defines several additional factory methods to facilitate the creation of
 * [[AnnotationMatcher]]s.
 *
 * @author Marco Torsello
 */
object AnnotationMatcher {

    def apply(
        annotationType: String,
        elementValuePairs: Option[ElementValuePairs] = None): AnnotationMatcher = {
        new AnnotationMatcher(
            ObjectType(annotationType.replace('.', '/')),
            elementValuePairs)
    }

    def apply(
        annotationType: String,
        elementValuePairs: ElementValuePairs): AnnotationMatcher = {
        new AnnotationMatcher(
            ObjectType(annotationType.replace('.', '/')),
            Some(elementValuePairs))
    }

    def apply(
        annotationType: String,
        elementValuePairs: Map[String, ElementValue]): AnnotationMatcher = {

        new AnnotationMatcher(
            ObjectType(annotationType.replace('.', '/')),
            Some(
                (elementValuePairs.map { kv ⇒
                    val (name, value) = kv
                    ElementValuePair(name, value)
                }).toIndexedSeq
            )
        )
    }

}
