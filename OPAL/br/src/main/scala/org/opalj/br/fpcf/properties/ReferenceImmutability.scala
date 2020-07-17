/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.br.fpcf.properties

import org.opalj.br.fpcf.properties
import org.opalj.fpcf.Entity
import org.opalj.fpcf.OrderedProperty
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation

sealed trait ReferenceImmutabilityPropertyMetaInformation extends PropertyMetaInformation {

    type Self = ReferenceImmutability

}

/**
 * Describes the reference immutability of org.opalj.br.Field.
 * The analysis is used from the old implementation of the L2FieldMutabilityAnalysis.
 *
 * [[MutableReference]] A reference through which the object it points to could be changed.
 *
 * [[LazyInitializedReference]] A reference through which the object it points to is changed in a initial like phase
 * and afterwards never changed through this reference
 *
 * [[ImmutableReference]] A reference through which the object it leads to is set in the constructor and afterwards
 * never changed. And there is no possibility to change it afterwards through this reference..
 *
 * @author Tobias Peter Roth
 */
sealed trait ReferenceImmutability
    extends OrderedProperty
    with ReferenceImmutabilityPropertyMetaInformation {
    final def key: PropertyKey[ReferenceImmutability] = ReferenceImmutability.key
    def isImmutableReference = false
    def doesNotEscapes = false
}

object ReferenceImmutability extends ReferenceImmutabilityPropertyMetaInformation {

    var notEscapes: Boolean = false

    final val PropertyKeyName = "opalj.ReferenceImmutability"

    final val key: PropertyKey[ReferenceImmutability] = {
        PropertyKey.create(
            PropertyKeyName,
            MutableReference
        )
    }
}

case class ImmutableReference(notEscapes: Boolean) extends ReferenceImmutability {
    override def isImmutableReference = true
    override def doesNotEscapes: Boolean = notEscapes
    override def checkIsEqualOrBetterThan(e: Entity, other: Self): Unit = {}
    def meet(that: ReferenceImmutability): ReferenceImmutability =
        if (this == that)
            this
        else
            that
}

case object LazyInitializedThreadSafeReference extends ReferenceImmutability {
    override def checkIsEqualOrBetterThan(e: Entity, other: Self): Unit = other match {
        case ImmutableReference(_) ⇒
            throw new IllegalArgumentException(s"$e: impossible refinement: $other ⇒ $this");
        case _ ⇒
    }
    def meet(other: ReferenceImmutability): ReferenceImmutability =
        if (other == MutableReference || other == LazyInitializedNotThreadSafeOrNotDeterministicReference ||
            other == LazyInitializedNotThreadSafeButDeterministicReference) {
            other
        } else {
            this
        }
}

case object LazyInitializedNotThreadSafeButDeterministicReference extends ReferenceImmutability {
    override def checkIsEqualOrBetterThan(e: Entity, other: Self): Unit = other match {
        case ImmutableReference(_) | LazyInitializedThreadSafeReference ⇒
            throw new IllegalArgumentException(s"$e: impossible refinement: $other ⇒ $this");
        case _ ⇒
    }

    def meet(other: ReferenceImmutability): ReferenceImmutability = {
        if (other == MutableReference || other == LazyInitializedNotThreadSafeOrNotDeterministicReference) {
            other
        } else {
            this
        }
    }
}
case object LazyInitializedNotThreadSafeOrNotDeterministicReference extends ReferenceImmutability {
    def meet(other: ReferenceImmutability): properties.ReferenceImmutability = {
        if (other == MutableReference) {
            other
        } else {
            this
        }
    }
    override def checkIsEqualOrBetterThan(e: Entity, other: ReferenceImmutability): Unit = {
        other match {
            case ImmutableReference(_) | LazyInitializedNotThreadSafeButDeterministicReference |
                LazyInitializedThreadSafeReference ⇒
                throw new IllegalArgumentException(s"$e: impossible refinement: $other ⇒ $this");
            case _ ⇒
        }
    }
}

case object MutableReference extends ReferenceImmutability {
    def meet(other: ReferenceImmutability): this.type = this

    override def checkIsEqualOrBetterThan(e: Entity, other: ReferenceImmutability): Unit = {
        if (other != MutableReference) {
            throw new IllegalArgumentException(s"$e: impossible refinement: $other => $this")
        }
    }
}
