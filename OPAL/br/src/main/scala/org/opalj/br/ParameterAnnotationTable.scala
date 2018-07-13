/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

/**
 * Parameter annotations.
 *
 * @author Michael Eichberg
 */
trait ParameterAnnotationTable extends Attribute {

    def parameterAnnotations: ParameterAnnotations

    def isRuntimeVisible: Boolean

    override def similar(other: Attribute, config: SimilarityTestConfiguration): Boolean = this == other

}

object ParameterAnnotationTable {

    def unapply(paa: ParameterAnnotationTable): Option[(Boolean, ParameterAnnotations)] = {
        Some((paa.isRuntimeVisible, paa.parameterAnnotations))
    }

}
