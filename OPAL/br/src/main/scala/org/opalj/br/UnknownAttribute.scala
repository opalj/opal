/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

/**
 * Represents (as a byte array) attributes that are not directly supported by OPAL.
 *
 * @author Michael Eichberg
 */
case class UnknownAttribute(attributeName: String, info: Array[Byte]) extends Attribute {

    override def kindId: Int = UnknownAttribute.KindId

    override def similar(other: Attribute, config: SimilarityTestConfiguration): Boolean = {
        other match {
            case that: UnknownAttribute => this.similar(that)
            case _                      => false
        }
    }

    def similar(other: UnknownAttribute): Boolean = {
        this.attributeName.size == other.attributeName.size &&
            java.util.Arrays.equals(this.info, other.info)
    }
}
object UnknownAttribute {

    final val KindId = -1

}
