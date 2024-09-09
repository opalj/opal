/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package properties
package string

import org.opalj.fpcf.Entity
import org.opalj.fpcf.FallbackReason
import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation
import org.opalj.fpcf.PropertyStore

sealed trait StringConstancyPropertyMetaInformation extends PropertyMetaInformation {
    final type Self = StringConstancyProperty
}

class StringConstancyProperty(
    val tree: StringTreeNode
) extends Property with StringConstancyPropertyMetaInformation {

    final def key: PropertyKey[StringConstancyProperty] = StringConstancyProperty.key

    override def toString: String = {
        val level = tree.constancyLevel.toString.toLowerCase
        val strings = if (tree.simplify.isInvalid) {
            "No possible strings - Invalid Flow"
        } else tree.sorted.toRegex

        s"Level: $level, Possible Strings: $strings"
    }

    override def hashCode(): Int = tree.hashCode()

    override def equals(o: Any): Boolean = o match {
        case scp: StringConstancyProperty => tree.equals(scp.tree)
        case _                            => false
    }
}

object StringConstancyProperty extends Property with StringConstancyPropertyMetaInformation {

    final val PropertyKeyName = "opalj.StringConstancy"

    final val key: PropertyKey[StringConstancyProperty] = {
        PropertyKey.create(
            PropertyKeyName,
            (_: PropertyStore, _: FallbackReason, _: Entity) => lb
        )
    }

    def apply(tree: StringTreeNode): StringConstancyProperty = new StringConstancyProperty(tree)

    /**
     * @return Returns the lower bound from a lattice-point of view.
     */
    def lb: StringConstancyProperty = StringConstancyProperty(StringTreeNode.lb)

    /**
     * @return Returns the upper bound from a lattice-point of view.
     */
    def ub: StringConstancyProperty = StringConstancyProperty(StringTreeNode.ub)
}
