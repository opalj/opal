/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

/**
 * The runtime invisible type annotations.
 *
 * @author Michael Eichberg
 */
case class RuntimeInvisibleTypeAnnotationTable(
        typeAnnotations: TypeAnnotations
) extends TypeAnnotationTable {

    final def isRuntimeVisible: Boolean = false

    override def kindId: Int = RuntimeInvisibleTypeAnnotationTable.KindId

    override def copy(typeAnnotations: TypeAnnotations): RuntimeInvisibleTypeAnnotationTable = {
        new RuntimeInvisibleTypeAnnotationTable(typeAnnotations)
    }

}
object RuntimeInvisibleTypeAnnotationTable {

    final val KindId = 28

}
