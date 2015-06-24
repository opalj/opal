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
 * @author Marco Torsello
 */
trait AnnotationPredicate {

    def apply(other: Annotation): Boolean

    def toDescription: String

}

/**
 * Matches an annotation of a class, field, method or method parameter.
 *
 * {{{
 * // match the annotation independent of the number of specified annotations
 * scala> val am = org.opalj.av.checking.AnnotationPredicate("java.lang.Foo")
 * am: org.opalj.av.checking.AnnotationPredicate = @java.lang.Foo
 * scala> am(org.opalj.br.Annotation(org.opalj.br.ObjectType("java/lang/Foo"),IndexedSeq(org.opalj.br.ElementValuePair("clazz",org.opalj.br.StringValue(" ")))))
 * res: Boolean = true
 * }}}
 * @author Marco Torsello
 */
case class FieldTypeAnnotationPredicate(
        annotationType: FieldType) extends AnnotationPredicate {

    /**
     * Checks if the given annotation is matched by this matcher.
     * The given annotation is matched if it has this matcher's specified type.
     *
     * ==Example Scenarios==
     *  - If the matcher's annotation type is `A` and the other annotation's type is `B`
     *    then the elements will not match.
     */
    override def apply(other: Annotation): Boolean = {
        other.annotationType eq this.annotationType
    }

    override def toDescription: String = {
        "@"+annotationType.toJava
    }

}

/**
 * Matches an annotation of a class, field, method or method parameter.
 *
 * {{{
 * scala> val am = org.opalj.av.checking.AnnotationPredicate("java.lang.Foo",Map("clazz" -> org.opalj.br.StringValue("")))
 * am: org.opalj.av.checking.AnnotationPredicate = @java.lang.Foo(clazz="")
 * scala> am(org.opalj.br.Annotation(org.opalj.br.ObjectType("java/lang/Foo"),IndexedSeq(org.opalj.br.ElementValuePair("clazz",org.opalj.br.StringValue("")))))
 * res: Boolean = true
 * scala> am(org.opalj.br.Annotation(org.opalj.br.ObjectType("java/lang/Foo"),IndexedSeq(org.opalj.br.ElementValuePair("clazz",org.opalj.br.StringValue("-+-")))))
 * res: Boolean = false
 *
 * // match the annotation only if no values are specified
 * scala> val am = org.opalj.av.checking.DefaultAnnotationPredicate("java.lang.Foo",IndexedSeq.empty)
 * am: org.opalj.av.checking.DefaultAnnotationPredicate = @java.lang.Foo()
 * scala> am(org.opalj.br.Annotation(org.opalj.br.ObjectType("java/lang/Foo"),IndexedSeq(org.opalj.br.ElementValuePair("clazz",org.opalj.br.StringValue(" ")))))
 * res: Boolean = false
 * }}}
 * @author Marco Torsello
 */
case class DefaultAnnotationPredicate(
        annotationType: FieldType,
        elementValuePairs: ElementValuePairs) extends AnnotationPredicate {

    /**
     * Checks if the given annotation is matched by this predicate.
     * The given annotation is matched if it has this matcher's specified type
     * and – if this predicate defines an [[org.opalj.br.ElementValuePair]]s matcher -
     * if all element value pairs are matched.	When matching element value
     * pairs the order is ignored.
     *
     * ==Example Scenarios==
     *  - If the predicate's annotation type is `A` and the other annotation's type is `B`
     *    then the elements will not match.
     *
     *  - If the predicate defines specific [[org.opalj.br.ElementValuePair]]s such as
     * 		`ArrayBuffer(ElementValuePair("target", ClassValue("org.opalj.av.checking.AnnotationPredicate")),
     * 				ElementValuePair("name", StringValue("Annotation_Predicate"))`
     * 		it will then match annotations where the [[org.opalj.br.ElementValuePair]]s have a different order:
     * 		`ArrayBuffer(ElementValuePair("name", StringValue("Annotation_Predicate"),
     * 				ElementValuePair("target", ClassValue("org.opalj.av.checking.AnnotationPredicate")))`
     *  	But it will not match if one or both of the two [[org.opalj.br.ElementValuePair]]s are missing or there is
     *  	another [[org.opalj.br.ElementValuePair]] not defined by this predicate.
     */
    override def apply(other: Annotation): Boolean = {
        (other.annotationType eq this.annotationType) &&
            (this.elementValuePairs.isEmpty ||
                (other.elementValuePairs.size == this.elementValuePairs.size &&
                    this.elementValuePairs.forall(e ⇒
                        other.elementValuePairs.exists(_ == e))))
    }

    override def toDescription: String = {
        val at = "@"+annotationType.toJava
        elementValuePairs.map(_.toJava).mkString(at+"(", ",", ")")
    }

}

/**
 * Returns always true as there is no predicate to match.
 * @author Marco Torsello
 */
case object NoAnnotationPredicate extends AnnotationPredicate {

    override def apply(other: Annotation): Boolean = true

    override def toDescription: String = "";

}

/**
 * Defines several additional factory methods to facilitate the creation of
 * [[AnnotationPredicate]]s.
 *
 * @author Marco Torsello
 */
object AnnotationPredicate {

    def apply(
        annotationType: String): AnnotationPredicate = {
        new FieldTypeAnnotationPredicate(
            ObjectType(annotationType.replace('.', '/')))
    }

    def apply(
        annotationType: String,
        elementValuePairs: ElementValuePairs): AnnotationPredicate = {
        new DefaultAnnotationPredicate(
            ObjectType(annotationType.replace('.', '/')),
            elementValuePairs)
    }

    def apply(
        annotationType: String,
        elementValuePairs: Map[String, ElementValue]): AnnotationPredicate = {

        new DefaultAnnotationPredicate(
            ObjectType(annotationType.replace('.', '/')),
            (elementValuePairs.map { kv ⇒
                val (name, value) = kv
                ElementValuePair(name, value)
            }).toIndexedSeq
        )
    }

}
