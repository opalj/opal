/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

/**
 * The runtime invisible class, method, or field annotations.
 *
 * @author Michael Eichberg
 */
case class RuntimeInvisibleAnnotationTable(annotations: Annotations) extends AnnotationTable {

    final def isRuntimeVisible: Boolean = false

    override def kindId: Int = RuntimeInvisibleAnnotationTable.KindId

}
object RuntimeInvisibleAnnotationTable {

    final val KindId = 24

}