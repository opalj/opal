/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.br.fpcf.properties

import org.opalj.fpcf.Entity
import org.opalj.fpcf.OrderedProperty
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
sealed trait FieldImmutability
    extends OrderedProperty
    with FieldImmutabilityPropertyMetaInformation {

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

case object DeepImmutableField extends FieldImmutability {
    override def checkIsEqualOrBetterThan(e: Entity, other: Self): Unit = {}
    def meet(that: FieldImmutability): FieldImmutability =
        if (this == that)
            this
        else
            that
}

case object DependentImmutableField extends FieldImmutability {
    override def checkIsEqualOrBetterThan(e: Entity, other: Self): Unit = {

        if (other == DeepImmutableField) {
            throw new IllegalArgumentException(s"$e: impossible refinement: $other ⇒ $this");
        }
    }

    def meet(that: FieldImmutability): FieldImmutability =
        if (that == MutableField || that == ShallowImmutableField)
            that
        else
            this

}

case object ShallowImmutableField extends FieldImmutability {
    def meet(that: FieldImmutability): FieldImmutability =
        if (that == MutableField)
            that
        else
            this

    override def checkIsEqualOrBetterThan(e: Entity, other: Self): Unit = {

        if (other == DeepImmutableField || other == DependentImmutableField) {
            throw new IllegalArgumentException(s"$e: impossible refinement: $other ⇒ $this");
        }
    }
}

case object MutableField extends FieldImmutability {

    def meet(other: FieldImmutability): this.type = this

    override def checkIsEqualOrBetterThan(e: Entity, other: Self): Unit = {
        if (other != MutableField) {
            throw new IllegalArgumentException(s"$e: impossible refinement: $other ⇒ $this")
        }
    }

}
