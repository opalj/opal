/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package properties
package string

/**
 * @author Maximilian Rüsch
 */
trait StringConstancyInformation {

    def treeFn: StringTreeNode => StringTreeNode
    def tree: StringTreeNode

    final def isTheNeutralElement: Boolean = tree.isNeutralElement
    final def constancyLevel: StringConstancyLevel.Value = tree.constancyLevel
    final def toRegex: String = tree.toRegex
}

case class StringConstancyInformationConst(override val tree: StringTreeNode) extends StringConstancyInformation {

    override def treeFn: StringTreeNode => StringTreeNode = _ => tree
}

case class StringConstancyInformationFunction(override val treeFn: StringTreeNode => StringTreeNode)
    extends StringConstancyInformation {

    override def tree: StringTreeNode = treeFn(StringTreeNeutralElement)
}

object StringConstancyInformation {

    def lb: StringConstancyInformation = StringConstancyInformationConst(StringTreeDynamicString)
    def ub: StringConstancyInformation = StringConstancyInformationConst(StringTreeNeutralElement)

    def nullElement: StringConstancyInformation = StringConstancyInformationConst(StringTreeNull)

    def getElementForParameterPC(paramPC: Int): StringConstancyInformation = {
        if (paramPC >= -1) {
            throw new IllegalArgumentException(s"Invalid parameter pc given: $paramPC")
        }
        // Parameters start at PC -2 downwards
        val paramPosition = Math.abs(paramPC + 2)
        StringConstancyInformationConst(StringTreeParameter(paramPosition))
    }
}
