/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.br.fpcf.properties

import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation

sealed trait FieldImmutabilityPropertyMetaInformation extends PropertyMetaInformation {

    type Self = FieldImmutability

}

/**
 * Describes the field immutability of org.opalj.br.Field.
 * [[MutableField]] A field with an immutable reference
 *
 * [[ShallowImmutableField]] A field with an immutable reference and a shallow immutable or mutable data type
 *
 * [[DependentImmutableField]] A field which immutability depends on its type.
 *
 * [[DeepImmutableField]] A field with an immutable reference and a deep immutable field type
 *
 * @author Tobias Peter Roth
 */
sealed trait FieldImmutability extends Property with FieldImmutabilityPropertyMetaInformation {

    final def key: PropertyKey[FieldImmutability] = FieldImmutability.key
}

object FieldImmutability extends FieldImmutabilityPropertyMetaInformation {

    final val PropertyKeyName = "opalj.FieldImmutability"

    final val key: PropertyKey[FieldImmutability] = {
        PropertyKey.create(
            PropertyKeyName,
            MutableField
        )
    }
}

case object MutableField extends FieldImmutability

sealed trait ImmutableField extends FieldImmutability

case object ShallowImmutableField extends ImmutableField

case object DependentImmutableField extends ImmutableField

case object DeepImmutableField extends ImmutableField
