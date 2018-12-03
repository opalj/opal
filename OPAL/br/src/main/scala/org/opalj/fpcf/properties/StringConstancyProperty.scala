/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties
import org.opalj.br.Field
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EPS
import org.opalj.fpcf.FallbackReason
import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.string_definition.properties.StringConstancyInformation
import org.opalj.fpcf.string_definition.properties.StringConstancyLevel.DYNAMIC
import org.opalj.fpcf.string_definition.properties.StringConstancyType
import org.opalj.fpcf.string_definition.properties.StringTree
import org.opalj.fpcf.string_definition.properties.StringTreeConst

sealed trait StringConstancyPropertyMetaInformation extends PropertyMetaInformation {
    type Self = StringConstancyProperty
}

class StringConstancyProperty(
        val stringTree: StringTree
) extends Property with StringConstancyPropertyMetaInformation {

    final def key: PropertyKey[StringConstancyProperty] = StringConstancyProperty.key

    override def toString: String = {
        stringTree.reduce().toString
    }

}

object StringConstancyProperty extends StringConstancyPropertyMetaInformation {

    final val PropertyKeyName = "StringConstancy"

    final val key: PropertyKey[StringConstancyProperty] = {
        PropertyKey.create(
            PropertyKeyName,
            (_: PropertyStore, _: FallbackReason, _: Entity) ⇒ {
                // TODO: Using simple heuristics, return a better value for some easy cases
                StringConstancyProperty(StringTreeConst(
                    StringConstancyInformation(DYNAMIC, StringConstancyType.APPEND, "*")
                ))
            },
            (_, eps: EPS[Field, StringConstancyProperty]) ⇒ eps.ub,
            (_: PropertyStore, _: Entity) ⇒ None
        )
    }

    def apply(
        stringTree: StringTree
    ): StringConstancyProperty = new StringConstancyProperty(stringTree)

}
