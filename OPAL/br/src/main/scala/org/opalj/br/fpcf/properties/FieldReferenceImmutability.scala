/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package properties

import org.opalj.fpcf.Entity
import org.opalj.fpcf.OrderedProperty
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation

sealed trait FieldReferenceImmutabilityPropertyMetaInformation extends PropertyMetaInformation {
    type Self = FieldReferenceImmutability
}

/**
 * Describes the reference immutability of org.opalj.br.Field.
 *
 * [[MutableFieldReference]] The referenced object can be exchanged.
 *
 * [[LazyInitializedNotThreadSafeFieldReference]] The field reference is lazy initialized in a not thread safe way.
 *
 * [[LazyInitializedNotThreadSafeButDeterministicFieldReference]] The field reference is lazy initialized in a for
 * objects not thread safe way but it has a primitive type.
 *
 * [[LazyInitializedThreadSafeFieldReference]] The field reference is lazy initialized in a thread safe way. The write
 * is atomic
 *
 * [[ImmutableFieldReference]] The value or object the field reference refer can not be exchanged.
 *
 * @author Tobias Peter Roth
 */
sealed trait FieldReferenceImmutability extends OrderedProperty with FieldReferenceImmutabilityPropertyMetaInformation {

    final def key: PropertyKey[FieldReferenceImmutability] = FieldReferenceImmutability.key

    def isImmutable = false
}

object FieldReferenceImmutability extends FieldReferenceImmutabilityPropertyMetaInformation {

    var notEscapes: Boolean = false

    final val PropertyKeyName = "opalj.FieldReferenceImmutability"

    final val key: PropertyKey[FieldReferenceImmutability] = {
        PropertyKey.create(
            PropertyKeyName,
            MutableFieldReference
        )
    }
}

case object ImmutableFieldReference extends FieldReferenceImmutability {

    override def isImmutable = true

    override def checkIsEqualOrBetterThan(e: Entity, other: Self): Unit = {}

    def meet(that: FieldReferenceImmutability): FieldReferenceImmutability = that
}

case object LazyInitializedThreadSafeFieldReference extends FieldReferenceImmutability {

    override def checkIsEqualOrBetterThan(e: Entity, other: Self): Unit = other match {
        case ImmutableFieldReference ⇒
            throw new IllegalArgumentException(s"$e: impossible refinement: $other ⇒ $this");
        case _ ⇒
    }

    def meet(other: FieldReferenceImmutability): FieldReferenceImmutability =
        if (
            other == MutableFieldReference ||
            other == LazyInitializedNotThreadSafeFieldReference ||
            other == LazyInitializedNotThreadSafeButDeterministicFieldReference
        ) {
            other
        } else {
            this
        }
}

case object LazyInitializedNotThreadSafeButDeterministicFieldReference extends FieldReferenceImmutability {

    override def checkIsEqualOrBetterThan(e: Entity, other: Self): Unit = other match {
        case ImmutableFieldReference | LazyInitializedThreadSafeFieldReference ⇒
            throw new IllegalArgumentException(s"$e: impossible refinement: $other ⇒ $this");
        case _ ⇒
    }

    def meet(other: FieldReferenceImmutability): FieldReferenceImmutability = {
        if (other == MutableFieldReference || other == LazyInitializedNotThreadSafeFieldReference) {
            other
        } else {
            this
        }
    }
}
case object LazyInitializedNotThreadSafeFieldReference extends FieldReferenceImmutability {

    def meet(other: FieldReferenceImmutability): properties.FieldReferenceImmutability = {
        if (other == MutableFieldReference) {
            other
        } else {
            this
        }
    }

    override def checkIsEqualOrBetterThan(e: Entity, other: FieldReferenceImmutability): Unit = {
        other match {
            case ImmutableFieldReference | LazyInitializedNotThreadSafeButDeterministicFieldReference |
                LazyInitializedThreadSafeFieldReference ⇒
                throw new IllegalArgumentException(s"$e: impossible refinement: $other ⇒ $this");
            case _ ⇒
        }
    }
}

case object MutableFieldReference extends FieldReferenceImmutability {

    def meet(other: FieldReferenceImmutability): this.type = this

    override def checkIsEqualOrBetterThan(e: Entity, other: FieldReferenceImmutability): Unit = {
        if (other != MutableFieldReference) {
            throw new IllegalArgumentException(s"$e: impossible refinement: $other => $this")
        }
    }
}
