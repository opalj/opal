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
    val stringConstancyInformation: StringConstancyInformation
) extends Property with StringConstancyPropertyMetaInformation {

    def sci: StringConstancyInformation = stringConstancyInformation

    final def key: PropertyKey[StringConstancyProperty] = StringConstancyProperty.key

    override def toString: String = {
        val level = stringConstancyInformation.constancyLevel.toString.toLowerCase
        s"Level: $level, Possible Strings: ${stringConstancyInformation.toRegex}"
    }

    /**
     * @return Returns `true` if the [[stringConstancyInformation]] contained in this instance is
     *         the neutral element (see [[StringConstancyInformation.isTheNeutralElement]]).
     */
    def isTheNeutralElement: Boolean = {
        stringConstancyInformation.isTheNeutralElement
    }

    override def hashCode(): Int = stringConstancyInformation.hashCode()

    override def equals(o: Any): Boolean = o match {
        case scp: StringConstancyProperty =>
            stringConstancyInformation.equals(scp.stringConstancyInformation)
        case _ => false
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

    def apply(stringConstancyInformation: StringConstancyInformation): StringConstancyProperty =
        new StringConstancyProperty(stringConstancyInformation)

    /**
     * @return Returns the lower bound from a lattice-point of view.
     */
    def lb: StringConstancyProperty = StringConstancyProperty(StringConstancyInformation.lb)

    /**
     * @return Returns the upper bound from a lattice-point of view.
     */
    def ub: StringConstancyProperty = StringConstancyProperty(StringConstancyInformation.ub)
}
