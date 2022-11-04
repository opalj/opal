/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

import scala.collection.immutable.ArraySeq

/**
 * An annotation of a class, field, method or method parameter.
 *
 * Annotations are associated with a class, field, or method using the attribute
 * [[org.opalj.br.RuntimeInvisibleAnnotationTable]] or
 * [[org.opalj.br.RuntimeVisibleAnnotationTable]].
 *
 * Annotations are associated with a method parameter using the attribute
 * [[org.opalj.br.RuntimeInvisibleParameterAnnotationTable]] or
 * a [[org.opalj.br.RuntimeVisibleParameterAnnotationTable]].
 *
 * @author Michael Eichberg
 * @author Arne Lottmann
 */
case class Annotation(
        annotationType:    FieldType,
        elementValuePairs: ElementValuePairs = NoElementValuePairs
) extends AnnotationLike {

    def similar(other: Annotation): Boolean = {
        (this.annotationType eq other.annotationType) &&
            this.elementValuePairs.size == other.elementValuePairs.size &&
            this.elementValuePairs.forall(other.elementValuePairs.contains)
    }

    def toJava: String = {
        val name = annotationType.toJava
        val parameters =
            if (elementValuePairs.isEmpty)
                ""
            else if (elementValuePairs.size == 1)
                "("+elementValuePairs.head.toJava+")"
            else
                elementValuePairs.map[String](_.toJava).mkString("(\n\t", ",\n\t", "\n)")
        "@"+name + parameters
    }

}
/**
 * Factory object to create [[Annotation]] objects.
 */
object Annotation {

    def apply(annotationType: FieldType, elementValuePairs: ElementValuePair*): Annotation = {
        new Annotation(
            annotationType,
            ArraySeq.unsafeWrapArray(elementValuePairs.toArray)
        )
    }

    def build(
        annotationType:    FieldType,
        elementValuePairs: (String, ElementValue)*
    ): Annotation = {
        new Annotation(
            annotationType,
            ArraySeq.from(elementValuePairs.map(ElementValuePair(_)))
        )
    }

}
