/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

/**
 * A runtime visible type annotation.
 *
 * @author Michael Eichberg
 */
case class RuntimeVisibleTypeAnnotationTable(
        typeAnnotations: TypeAnnotations
) extends TypeAnnotationTable {

    final def isRuntimeVisible: Boolean = true

    override def kindId: Int = RuntimeVisibleTypeAnnotationTable.KindId

    override def copy(typeAnnotations: TypeAnnotations): RuntimeVisibleTypeAnnotationTable = {
        new RuntimeVisibleTypeAnnotationTable(typeAnnotations)
    }

}
object RuntimeVisibleTypeAnnotationTable {

    final val KindId = 27

}
