/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

/**
 * Parameter annotations.
 *
 * @author Michael Eichberg
 */
case class RuntimeVisibleParameterAnnotationTable(
        parameterAnnotations: ParameterAnnotations
) extends ParameterAnnotationTable {

    final def isRuntimeVisible: Boolean = true

    override def kindId: Int = RuntimeVisibleParameterAnnotationTable.KindId

}
object RuntimeVisibleParameterAnnotationTable {

    final val KindId = 25

}
