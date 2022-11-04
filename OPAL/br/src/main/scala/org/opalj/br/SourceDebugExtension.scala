/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

/**
 * Attribute to associate additional debug information with a class. The source
 * debug extension attribute is an optional attribute of a class declaration
 * ([[org.opalj.br.ClassFile]]).
 *
 * @author Michael Eichberg
 */
case class SourceDebugExtension(debug_extension: Array[Byte]) extends Attribute {

    override def kindId: Int = SourceDebugExtension.KindId

    override def similar(other: Attribute, config: SimilarityTestConfiguration): Boolean = {
        other match {
            case that: SourceDebugExtension => this.similar(that)
            case _                          => false
        }
    }

    def similar(other: SourceDebugExtension): Boolean = {
        // Since we have no further knowledge of the content, we make the assumption
        // that the order is relevant.
        java.util.Arrays.equals(this.debug_extension, other.debug_extension)
    }

}
object SourceDebugExtension {

    final val KindId = 18

}
