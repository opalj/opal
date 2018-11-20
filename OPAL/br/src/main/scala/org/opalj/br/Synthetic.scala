/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

/**
 * The synthetic attribute.
 *
 * @author Michael Eichberg
 */
case object Synthetic extends Attribute {

    final val KindId = 11

    override def kindId = KindId

    override def similar(other: Attribute, config: SimilarityTestConfiguration): Boolean = this == other
}
