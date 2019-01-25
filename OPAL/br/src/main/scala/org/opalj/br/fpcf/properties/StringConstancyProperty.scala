/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.br.fpcf.properties

import org.opalj.fpcf.Entity
import org.opalj.fpcf.FallbackReason
import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation
import org.opalj.fpcf.PropertyStore
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.br.fpcf.properties.string_definition.StringConstancyLevel
import org.opalj.br.fpcf.properties.string_definition.StringConstancyType

sealed trait StringConstancyPropertyMetaInformation extends PropertyMetaInformation {
    final type Self = StringConstancyProperty
}

class StringConstancyProperty(
        val stringConstancyInformation: StringConstancyInformation
) extends Property with StringConstancyPropertyMetaInformation {

    final def key: PropertyKey[StringConstancyProperty] = StringConstancyProperty.key

    override def toString: String = {
        val level = stringConstancyInformation.constancyLevel.toString.toLowerCase
        s"Level: $level, Possible Strings: ${stringConstancyInformation.possibleStrings}"
    }

}

object StringConstancyProperty extends Property with StringConstancyPropertyMetaInformation {

    final val PropertyKeyName = "StringConstancy"

    final val key: PropertyKey[StringConstancyProperty] = {
        PropertyKey.create(
            PropertyKeyName,
            (_: PropertyStore, _: FallbackReason, _: Entity) ⇒ {
                // TODO: Using simple heuristics, return a better value for some easy cases
                lowerBound
            },
        )
    }

    def apply(
        stringConstancyInformation: StringConstancyInformation
    ): StringConstancyProperty = new StringConstancyProperty(stringConstancyInformation)

    /**
     * @return Returns the upper bound from a lattice-point of view.
     */
    def upperBound: StringConstancyProperty =
        StringConstancyProperty(StringConstancyInformation(
            StringConstancyLevel.CONSTANT, StringConstancyType.APPEND
        ))

    /**
     * @return Returns the lower bound from a lattice-point of view.
     */
    def lowerBound: StringConstancyProperty =
        StringConstancyProperty(StringConstancyInformation(
            StringConstancyLevel.DYNAMIC,
            StringConstancyType.APPEND,
            StringConstancyInformation.UnknownWordSymbol
        ))

}
