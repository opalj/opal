/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package properties
package string_definition

/**
 * @author Maximilian Rüsch
 */
case class StringConstancyInformation(
    constancyType: StringConstancyType.Value = StringConstancyType.APPEND,
    tree:          StringTreeNode            = StringTreeNeutralElement
) {

    def isTheNeutralElement: Boolean =
        constancyLevel == StringConstancyLevel.CONSTANT &&
            constancyType == StringConstancyType.APPEND &&
            tree.isNeutralElement

    def constancyLevel: StringConstancyLevel.Value = tree.constancyLevel

    def toRegex: String = tree.toRegex

    def replaceParameters(parameters: Map[Int, StringConstancyInformation]): StringConstancyInformation = {
        val newTree = tree.replaceParameters(parameters.map {
            case (index, sci) => (index, sci.tree)
        })

        StringConstancyInformation(constancyType, newTree)
    }
}

/**
 * Provides a collection of instance-independent but string-constancy related values.
 */
object StringConstancyInformation {

    /**
     * Takes a list of [[StringConstancyInformation]] and reduces them to a single one by or-ing
     * them together (the level is determined by finding the most general level; the type is set to
     * [[StringConstancyType.APPEND]] and the possible strings are concatenated using a pipe and
     * then enclosed by brackets.
     *
     * @param scis The information to reduce. If a list with one element is passed, this element is
     *             returned (without being modified in any way); a list with > 1 element is reduced
     *             as described above; the empty list will throw an error!
     * @return Returns the reduced information in the fashion described above.
     */
    def reduceMultiple(scis: Seq[StringConstancyInformation]): StringConstancyInformation = {
        val relScis = scis.filter(!_.isTheNeutralElement)
        relScis.size match {
            case 0 => neutralElement
            case 1 => relScis.head
            case _ => StringConstancyInformation(StringConstancyType.APPEND, StringTreeOr(relScis.map(_.tree)))
        }
    }

    def lb: StringConstancyInformation = StringConstancyInformation(tree = StringTreeDynamicString)
    def ub: StringConstancyInformation = StringConstancyInformation(tree = StringTreeNeutralElement)
    def dynamicInt: StringConstancyInformation = StringConstancyInformation(tree = StringTreeDynamicInt)
    def dynamicFloat: StringConstancyInformation = StringConstancyInformation(tree = StringTreeDynamicFloat)

    def neutralElement: StringConstancyInformation = StringConstancyInformation(tree = StringTreeNeutralElement)
    def nullElement: StringConstancyInformation = StringConstancyInformation(tree = StringTreeNull)

    def getElementForParameterPC(paramPC: Int): StringConstancyInformation = {
        if (paramPC >= -1) {
            throw new IllegalArgumentException(s"Invalid parameter pc given: $paramPC")
        }
        // Parameters start at PC -2 downwards
        val paramPosition = Math.abs(paramPC + 2)
        StringConstancyInformation(tree = StringTreeParameter(paramPosition))
    }
}
