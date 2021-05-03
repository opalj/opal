/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package properties

import org.opalj.fpcf.Entity
import org.opalj.fpcf.OrderedProperty
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation

sealed trait FieldAssignabilityPropertyMetaInformation extends PropertyMetaInformation {
    type Self = FieldAssignability
}

/**
 * Describes the reference immutability of org.opalj.br.Field.
 *
 * [[Assignable]] The referenced object can be exchanged.
 *
 * [[UnsafelyLazilyInitialized]] The field reference is lazy initialized in a not thread safe way.
 *
 * [[LazilyInitialized]] The field reference is lazy initialized in a thread safe way. The write
 * is atomic
 *
 * [[EffectivelyNonAssignable]] The value or object the field reference refer can not be exchanged.
 *
 * [[NonAssignable]]
 *
 * @author Tobias Peter Roth
 */
sealed trait FieldAssignability extends OrderedProperty with FieldAssignabilityPropertyMetaInformation {

    final def key: PropertyKey[FieldAssignability] = FieldAssignability.key

    def isImmutable = false
}

object FieldAssignability extends FieldAssignabilityPropertyMetaInformation {

    var notEscapes: Boolean = false

    final val PropertyKeyName = "opalj.FieldAssignability"

    final val key: PropertyKey[FieldAssignability] = {
        PropertyKey.create(
            PropertyKeyName,
            Assignable
        )
    }
}
case object NonAssignable extends FieldAssignability {

    override def isImmutable = true

    override def checkIsEqualOrBetterThan(e: Entity, other: FieldAssignability): Unit = {}

    def meet(that: FieldAssignability): FieldAssignability = that
}

case object EffectivelyNonAssignable extends FieldAssignability {

    override def isImmutable = true

    override def checkIsEqualOrBetterThan(e: Entity, other: Self): Unit = other match {
        case NonAssignable ⇒
            throw new IllegalArgumentException(s"$e: impossible refinement: $other ⇒ $this");
        case _ ⇒
    }

    def meet(other: FieldAssignability): FieldAssignability =
        if (other == NonAssignable) {
            other
        } else {
            this
        }
}

case object LazilyInitialized extends FieldAssignability {

    override def checkIsEqualOrBetterThan(e: Entity, other: Self): Unit = other match {
        case EffectivelyNonAssignable | NonAssignable ⇒
            throw new IllegalArgumentException(s"$e: impossible refinement: $other ⇒ $this");
        case _ ⇒
    }

    def meet(other: FieldAssignability): FieldAssignability =
        if (other == Assignable || other == UnsafelyLazilyInitialized) {
            other
        } else {
            this
        }
}

case object UnsafelyLazilyInitialized extends FieldAssignability {

    def meet(other: FieldAssignability): properties.FieldAssignability = {
        if (other == Assignable) {
            other
        } else {
            this
        }
    }

    override def checkIsEqualOrBetterThan(e: Entity, other: FieldAssignability): Unit = {
        other match {
            case NonAssignable | EffectivelyNonAssignable | LazilyInitialized ⇒
                throw new IllegalArgumentException(s"$e: impossible refinement: $other ⇒ $this");
            case _ ⇒
        }
    }
}

case object Assignable extends FieldAssignability {

    def meet(other: FieldAssignability): FieldAssignability = this

    override def checkIsEqualOrBetterThan(e: Entity, other: FieldAssignability): Unit = {
        if (other != Assignable) {
            throw new IllegalArgumentException(s"$e: impossible refinement: $other => $this")
        }
    }
}
