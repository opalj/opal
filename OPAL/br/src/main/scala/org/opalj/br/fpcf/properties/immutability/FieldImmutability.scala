/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package properties
package immutability

import scala.collection.immutable.SortedSet

import org.opalj.fpcf.Entity
import org.opalj.fpcf.OrderedProperty
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation

sealed trait FieldImmutabilityPropertyMetaInformation extends PropertyMetaInformation {

    type Self = FieldImmutability
}

/**
 * Describes the field immutability of org.opalj.br.Field
 *
 * [[MutableField]] The field is assignable
 *
 * [[NonTransitivelyImmutableField]] A not assignable field and a non-transitively immutable or mutable data type
 *
 * [[DependentlyImmutableField]] A not assignable field with a generic type and parts of it are not
 * substantiated in an non-transitively or transitively immutable
 *
 * [[TransitivelyImmutableField]] A not assignable field with a transitively immutable field type or
 * a referenced object that can not escape or its state cannot be mutated.
 *
 * @author Tobias Roth
 */
sealed trait FieldImmutability extends OrderedProperty with FieldImmutabilityPropertyMetaInformation {
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

case object TransitivelyImmutableField extends FieldImmutability {

    override def checkIsEqualOrBetterThan(e: Entity, other: Self): Unit = {}

    def meet(that: FieldImmutability): FieldImmutability = that
}

case class DependentlyImmutableField(parameters: SortedSet[String]) extends FieldImmutability {

    override def checkIsEqualOrBetterThan(e: Entity, other: Self): Unit = {
        if (other == TransitivelyImmutableField) {
            throw new IllegalArgumentException(s"$e" +
                s": impossible refinement: $other => $this");
        }
    }

    def meet(that: FieldImmutability): FieldImmutability = that match {
        case MutableField | NonTransitivelyImmutableField => that
        case DependentlyImmutableField(thatParameters) =>
            DependentlyImmutableField(parameters ++ thatParameters)
        case _ => this
    }
}

case object NonTransitivelyImmutableField extends FieldImmutability {

    def meet(that: FieldImmutability): FieldImmutability = {
        if (that == MutableField)
            that
        else
            this
    }

    override def checkIsEqualOrBetterThan(e: Entity, other: Self): Unit = {
        if (other == TransitivelyImmutableField || other.isInstanceOf[DependentlyImmutableField]) {
            throw new IllegalArgumentException(s"$e: impossible refinement: $other => $this");
        }
    }
}

case object MutableField extends FieldImmutability {

    def meet(other: FieldImmutability): this.type = this

    override def checkIsEqualOrBetterThan(e: Entity, other: Self): Unit = {
        if (other != MutableField) {
            throw new IllegalArgumentException(s"$e: impossible refinement: $other => $this")
        }
    }
}
