/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

/**
 * Parameter annotations.
 *
 * @author Michael Eichberg
 */
case class RuntimeInvisibleParameterAnnotationTable(
        parameterAnnotations: ParameterAnnotations
) extends ParameterAnnotationTable {

    final def isRuntimeVisible: Boolean = false

    final override def kindId: Int = RuntimeInvisibleParameterAnnotationTable.KindId

}
object RuntimeInvisibleParameterAnnotationTable {

    final val KindId = 26

}