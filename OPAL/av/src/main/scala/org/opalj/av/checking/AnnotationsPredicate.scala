/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package av
package checking

import scala.collection.Set
import org.opalj.br._

/**
 * @author Marco Torsello
 */
trait AnnotationsPredicate extends (Iterable[Annotation] => Boolean)

/**
 * @author Marco Torsello
 */
case object NoAnnotations extends AnnotationsPredicate {

    def apply(others: Iterable[Annotation]): Boolean = false

}

/**
 * @author Michael Eichberg
 */
case object AnyAnnotations extends AnnotationsPredicate {

    def apply(others: Iterable[Annotation]): Boolean = true

}

/**
 * @author Marco Torsello
 */
case class HasAtLeastTheAnnotations(
        annotationPredicates: Set[_ <: AnnotationPredicate]
) extends AnnotationsPredicate {

    def apply(others: Iterable[Annotation]): Boolean = {
        annotationPredicates.forall(p => others.exists(a => p(a)))
    }
}
object HasAtLeastTheAnnotations {

    def apply(annotationPredicate: AnnotationPredicate): HasAtLeastTheAnnotations = {
        new HasAtLeastTheAnnotations(Set(annotationPredicate))
    }
}

/**
 * @author Marco Torsello
 */
case class HasTheAnnotations(
        annotationPredicates: Set[_ <: AnnotationPredicate]
) extends AnnotationsPredicate {

    def apply(others: Iterable[Annotation]): Boolean = {
        others.size == annotationPredicates.size &&
            annotationPredicates.forall(p => others.exists(a => p(a)))
    }

}
object HasTheAnnotations {

    def apply(annotationPredicate: AnnotationPredicate): HasTheAnnotations = {
        new HasTheAnnotations(Set(annotationPredicate))
    }
}

/**
 * @author Marco Torsello
 */
case class HasAtLeastOneAnnotation(
        annotationPredicates: Set[_ <: AnnotationPredicate]
) extends AnnotationsPredicate {

    def apply(annotations: Iterable[Annotation]): Boolean = {
        annotationPredicates.exists(p => annotations.exists(a => p(a)))
    }
}
