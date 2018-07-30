/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

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
        elementValuePairs: ElementValuePairs = IndexedSeq.empty
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
                elementValuePairs.map("("+_.toJava+")")
            else
                elementValuePairs.map(_.toJava).mkString("(\n\t", ",\n\t", "\n)")
        "@"+name + parameters
    }

}
/**
 * Factory object to create [[Annotation]] objects.
 */
object Annotation {

    def apply(
        annotationType:    FieldType,
        elementValuePairs: (String, ElementValue)*
    ): Annotation = {
        new Annotation(annotationType, elementValuePairs.map(e ⇒ ElementValuePair(e)).toIndexedSeq)
    }

    def apply(
        annotationType:    FieldType,
        elementValuePair:  ElementValuePair,
        elementValuePairs: ElementValuePair*
    ): Annotation = {
        new Annotation(annotationType, (elementValuePair +: elementValuePairs).toIndexedSeq)
    }

}
