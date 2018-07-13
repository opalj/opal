/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

/**
 * A class, method, or field annotation.
 *
 * @author Michael Eichberg
 */
case class RuntimeVisibleAnnotationTable(annotations: Annotations) extends AnnotationTable {

    final def isRuntimeVisible: Boolean = true

    override def kindId: Int = RuntimeVisibleAnnotationTable.KindId

}
object RuntimeVisibleAnnotationTable {

    final val KindId = 23

}