/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

/**
 * Java 7's `BootstrapMethods_attribute`.
 *
 * @author Michael Eichberg
 */
case class BootstrapMethodTable(methods: BootstrapMethods) extends Attribute {

    override def kindId: Int = BootstrapMethodTable.KindId

    override def similar(other: Attribute, config: SimilarityTestConfiguration): Boolean = {
        other match {
            case that: BootstrapMethodTable => this.similar(that)
            case _                          => false
        }
    }

    def similar(other: BootstrapMethodTable): Boolean = {
        // the order does not have to be stable!
        this.methods.size == other.methods.size && this.methods.forall(other.methods.contains)
    }

}
object BootstrapMethodTable {

    final val KindId = 42

}
