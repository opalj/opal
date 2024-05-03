/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package properties
package string_definition

/**
 * @author Maximilian Rüsch
 */
trait StringConstancyInformation {

    def treeFn: StringTreeNode => StringTreeNode
    def tree: StringTreeNode

    def replaceParameters(parameters: Map[Int, StringConstancyInformation]): StringConstancyInformation

    final def isTheNeutralElement: Boolean = tree.isNeutralElement
    final def constancyLevel: StringConstancyLevel.Value = tree.constancyLevel
    final def toRegex: String = tree.toRegex
}

case class StringConstancyInformationConst(override val tree: StringTreeNode) extends StringConstancyInformation {

    override def treeFn: StringTreeNode => StringTreeNode = _ => tree

    override def replaceParameters(parameters: Map[Int, StringConstancyInformation]): StringConstancyInformation = {
        StringConstancyInformationConst(tree.replaceParameters(parameters.map {
            case (index, sci) => (index, sci.tree)
        }))
    }
}

case class StringConstancyInformationFunction(override val treeFn: StringTreeNode => StringTreeNode)
    extends StringConstancyInformation {

    override def tree: StringTreeNode = treeFn(StringTreeNeutralElement)

    override def replaceParameters(parameters: Map[Int, StringConstancyInformation]): StringConstancyInformation = {
        StringConstancyInformationFunction((pv: StringTreeNode) =>
            treeFn(pv).replaceParameters(parameters.map {
                case (index, sci) => (index, sci.tree)
            })
        )
    }
}

object StringConstancyInformation {

    def reduceMultiple(scis: Seq[StringConstancyInformation]): StringConstancyInformation = {
        val relScis = scis.filter(!_.isTheNeutralElement)
        relScis.size match {
            case 0 => neutralElement
            case 1 => relScis.head
            case _ if relScis.forall(_.isInstanceOf[StringConstancyInformationConst]) =>
                StringConstancyInformationConst(StringTreeOr(relScis.map(_.tree)))
            case _ =>
                StringConstancyInformationFunction((pv: StringTreeNode) => StringTreeOr(relScis.map(_.treeFn(pv))))
        }
    }

    def lb: StringConstancyInformation = StringConstancyInformationConst(StringTreeDynamicString)
    def ub: StringConstancyInformation = StringConstancyInformationConst(StringTreeNeutralElement)
    def dynamicInt: StringConstancyInformation = StringConstancyInformationConst(StringTreeDynamicInt)
    def dynamicFloat: StringConstancyInformation = StringConstancyInformationConst(StringTreeDynamicFloat)

    def neutralElement: StringConstancyInformation = StringConstancyInformationConst(StringTreeNeutralElement)
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
