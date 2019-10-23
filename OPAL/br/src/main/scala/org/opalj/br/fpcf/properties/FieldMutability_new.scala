/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.br.fpcf.properties

import org.opalj.fpcf._

sealed trait FieldMutabilityPropertyMetaInformation_new extends PropertyMetaInformation {

    type Self = FieldMutability_new

}
/**
 * @author Tobias Peter Roth
 */
sealed trait FieldMutability_new extends Property with FieldMutabilityPropertyMetaInformation_new {

    final def key: PropertyKey[FieldMutability_new] = FieldMutability_new.key
}

object FieldMutability_new extends FieldMutabilityPropertyMetaInformation_new {

    final val PropertyKeyName = "opalj.FieldMutability_new"

    final val key: PropertyKey[FieldMutability_new] = {
        PropertyKey.create(
            PropertyKeyName,
            MutableField
        )
    }
}

sealed trait ImmutableField extends FieldMutability_new

case object ShallowImmutableField extends ImmutableField

case object DeepImmutableField extends ImmutableField

case object MutableField extends FieldMutability_new