/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

/**
 * Representation of a method's information about the method parameters.
 *
 * @author Michael Eichberg
 */
case class MethodParameterTable(parameters: MethodParameters)
    extends (Int => MethodParameter)
    with Attribute {

    override def kindId: Int = MethodParameterTable.KindId

    override def similar(other: Attribute, config: SimilarityTestConfiguration): Boolean = {
        this == other
    }

    final override def apply(parameterIndex: Int): MethodParameter = parameters(parameterIndex)

}

object MethodParameterTable {

    final val KindId = 43

}
