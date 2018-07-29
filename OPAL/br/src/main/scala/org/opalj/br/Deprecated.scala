/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

/**
 * The deprecated attribute.
 *
 * @author Michael Eichberg
 */
case object Deprecated extends Attribute {

    final val KindId = 22

    override def kindId: Int = KindId

    override def similar(other: Attribute, config: SimilarityTestConfiguration): Boolean = this == other

}
