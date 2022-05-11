/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

/**
 * Representation of a record class.
 *
 * @author Dominik Helm
 */
case class Record(components: RecordComponents)
    extends Attribute
    with (Int => RecordComponent) {

    override def kindId: Int = Record.KindId

    override def similar(other: Attribute, config: SimilarityTestConfiguration): Boolean = {
        this == other
    }

    final override def apply(componentIndex: Int): RecordComponent = components(componentIndex)

}

object Record {

    final val KindId = 49

}
