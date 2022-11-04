/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package properties

import org.opalj.fpcf.FallbackReason
import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation
import org.opalj.fpcf.PropertyStore

sealed trait FieldLocalityMetaInformation extends PropertyMetaInformation {
    final type Self = FieldLocality
}

/**
 * Describe the lifetime of the values stored in an instance field.
 *
 * [[LocalField]]s have a lifetime that is not longer than that of the field's owning instance.
 * [[ExtensibleLocalField]]s provide the same guarantee only if the (dynamic) type of the owning
 * instance is known not to extend `java.lang.Cloneable`.
 * The lifetime of a value in a [[LocalFieldWithGetter]] can only be extended by it being returned
 * by a method. I.e. if the caller of such method knows that it's receiver is fresh, the field's
 * value may also be treated as fresh.
 * [[ExtensibleLocalFieldWithGetter]] is used if both restrictions apply: The type type of the
 * owning instance may not be cloneable and the value's lifetime could be extended by being
 * returned by a method.
 */
sealed abstract class FieldLocality extends Property with FieldLocalityMetaInformation {
    final def key: PropertyKey[FieldLocality] = FieldLocality.key

    def meet(other: FieldLocality): FieldLocality

}

object FieldLocality extends FieldLocalityMetaInformation {
    final lazy val key: PropertyKey[FieldLocality] = PropertyKey.create(
        "FieldLocality",
        (_: PropertyStore, _: FallbackReason, f: Field) => {
            if (f.fieldType.isBaseType) LocalField else NoLocalField
        }
    )
}

/**
 * A field that is always written with a fresh, non-escaping value. Values read from the field never
 * escape. Therefore, the lifetime of such values is restricted by the lifetime of the field's
 * owning instance.
 */
case object LocalField extends FieldLocality {

    override def meet(other: FieldLocality): FieldLocality = other
}

/**
 * The field is a [[LocalField]] only if the owning instance's (dynamic) type does not implement
 * `java.lang.Cloneable`. Otherwise, the field's value may escape through a shallow copy created
 * through `java.lang.Object.clone`.
 */
case object ExtensibleLocalField extends FieldLocality {

    override def meet(other: FieldLocality): FieldLocality = other match {
        case LocalField           => this
        case LocalFieldWithGetter => ExtensibleLocalFieldWithGetter
        case _                    => other
    }
}

/**
 * The field is a [[LocalField]] except that value's read from it may escape by being returned by
 * a method of the owning instance. Clients can treat the value as fresh if the method's receiver is
 * fresh.
 */
case object LocalFieldWithGetter extends FieldLocality {

    override def meet(other: FieldLocality): FieldLocality = other match {
        case LocalField           => this
        case ExtensibleLocalField => ExtensibleLocalFieldWithGetter
        case _                    => other
    }
}

/**
 * The field is a [[LocalField]] except that value's read from it may escape by being returned by
 * a method of the owning instance. Also, the owning instance's (dynamic) type may not
 * implement `java.lang.Cloneable` or the field may escape through a shallow copy. Clients can
 * treat the field's value as fresh if the method's receiver is fresh and not cloneable.
 */
case object ExtensibleLocalFieldWithGetter extends FieldLocality {
    override def meet(other: FieldLocality): FieldLocality = other match {
        case NoLocalField => other
        case _            => this
    }
}

/**
 * The field is not local, i.e. it may be written with non-fresh values or the fields's value may
 * escape other than by being returned by a method of the owning instance.
 */
case object NoLocalField extends FieldLocality {
    override def meet(other: FieldLocality): FieldLocality = this
}
