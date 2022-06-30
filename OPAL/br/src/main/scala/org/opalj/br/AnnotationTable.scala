/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

/**
 * The runtime (in)visible annotations of a class, method, or field.
 *
 * @note At the JVM level, repeating annotations (as supported by Java 8)
 *    ([[http://docs.oracle.com/javase/tutorial/java/annotations/repeating.html]])
 *    have no explicit support.
 *
 * @author Michael Eichberg
 */
trait AnnotationTable extends Attribute {

    /**
     * Returns true if these annotations are visible at runtime.
     */
    def isRuntimeVisible: Boolean

    /**
     * The set of declared annotations; it may be empty.
     */
    def annotations: Annotations

    override def similar(other: Attribute, config: SimilarityTestConfiguration): Boolean = {
        other match {
            case that: AnnotationTable => this.similar(that)
            case _                     => false
        }
    }

    def similar(other: AnnotationTable): Boolean = {
        this.isRuntimeVisible == other.isRuntimeVisible &&
            // the order of two annotation tables does not need to be identical
            this.annotations.size == other.annotations.size &&
            this.annotations.forall(other.annotations.contains)
    }
}

/**
 * Functionality common to annotation tables.
 *
 * @author Michael Eichberg
 */
object AnnotationTable {

    def unapply(aa: AnnotationTable): Option[(Boolean, Annotations)] = {
        Some((aa.isRuntimeVisible, aa.annotations))
    }
}
