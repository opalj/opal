/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package properties
package string

/**
 * @author Maximilian Rüsch
 */
case class StringConstancyInformation(tree: StringTreeNode) {

    final def constancyLevel: StringConstancyLevel.Value = tree.constancyLevel
    final def toRegex: String = tree.toRegex
}

object StringConstancyInformation {

    def lb: StringConstancyInformation = StringConstancyInformation(StringTreeDynamicString)
    def ub: StringConstancyInformation = StringConstancyInformation(StringTreeNeutralElement)
}
