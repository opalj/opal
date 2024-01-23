/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

/**
 * The runtime (in)visible type annotations of a class, method, field or code block.
 *
 * @note For further information about type-level annotations go to:
 *    [[http://cr.openjdk.java.net/~abuckley/8misc.pdf]].
 *
 * @author Michael Eichberg
 */
trait TypeAnnotationTable extends CodeAttribute {

    /**
     * Returns true if these annotations are visible at runtime.
     */
    def isRuntimeVisible: Boolean

    /**
     * The set of declared annotations; it may be empty.
     */
    def typeAnnotations: TypeAnnotations

    def copy(typeAnnotations: TypeAnnotations): TypeAnnotationTable

    override def remapPCs(codeSize: Int, f: PC => PC): TypeAnnotationTable = {
        copy(typeAnnotations.flatMap[TypeAnnotation](_.remapPCs(codeSize, f)))
    }

    override def similar(other: Attribute, config: SimilarityTestConfiguration): Boolean = {
        other match {
            case that: TypeAnnotationTable => this.similar(that)
            case _                         => false
        }
    }

    def similar(other: TypeAnnotationTable): Boolean = {
        this.isRuntimeVisible == other.isRuntimeVisible &&
            // the order of two annotation tables does not need to be identical
            this.typeAnnotations.size == other.typeAnnotations.size &&
            this.typeAnnotations.forall(other.typeAnnotations.contains)
    }
}

/**
 * Functionality common to annotation tables.
 *
 * @author Michael Eichberg
 */
object TypeAnnotationTable {

    def unapply(tat: TypeAnnotationTable): Option[(Boolean, TypeAnnotations)] = {
        Some((tat.isRuntimeVisible, tat.typeAnnotations))
    }
}
