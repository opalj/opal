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

sealed trait ClassImmutabilityPropertyMetaInformation extends PropertyMetaInformation {
    final type Self = ClassImmutability
}

/**
 * Describes the class immutability of org.opalj.br.ClassFile.
 * The immutability of the classes are represented via the lower bound of the immutability of
 * their instance fields and the immutability of its supertype.
 *
 * [[MutableClass]] The class has mutable fields.
 *
 * [[NonTransitivelyImmutableClass]] A class that's transitive state is not immutable but the values or objects representing
 * this transitive state (are not / can not be) exchanged.
 *
 * [[DependentlyImmutableClass]] A class that is at least non-transitively immutable.
 * Whether it is non-transitively or transitively immutable depends on (a) generic parameter(s).
 *
 * [[TransitivelyImmutableClass]] A class with a transitively immutable state.
 *
 * @author Tobias Roth
 */
sealed trait ClassImmutability extends OrderedProperty with ClassImmutabilityPropertyMetaInformation {

    final def key: PropertyKey[ClassImmutability] = ClassImmutability.key

    def correspondingTypeImmutability: TypeImmutability
    def isDependentlyImmutable: Boolean = false
}

object ClassImmutability extends ClassImmutabilityPropertyMetaInformation {

    /**
     * The key associated with every [[ClassImmutability]] property.
     */
    final val key: PropertyKey[ClassImmutability] = PropertyKey.create(
        "opalj.ClassImmutability",
        MutableClass
    )
}

case object TransitivelyImmutableClass extends ClassImmutability {

    override def correspondingTypeImmutability: TypeImmutability = TransitivelyImmutableType

    override def checkIsEqualOrBetterThan(e: Entity, other: Self): Unit = {}

    def meet(that: ClassImmutability): ClassImmutability = that
}

case class DependentlyImmutableClass(parameters: SortedSet[String]) extends ClassImmutability {

    override def correspondingTypeImmutability: TypeImmutability = DependentlyImmutableType(parameters)

    override def isDependentlyImmutable: Boolean = true
    def meet(that: ClassImmutability): ClassImmutability =
        if (that == MutableClass || that == NonTransitivelyImmutableClass)
            that
        else
            this

    override def checkIsEqualOrBetterThan(e: Entity, other: Self): Unit = {
        if (other == TransitivelyImmutableClass) {
            throw new IllegalArgumentException(s"$e: impossible refinement: $other => $this")
        }
    }
}

case object NonTransitivelyImmutableClass extends ClassImmutability {
    override def correspondingTypeImmutability: TypeImmutability = NonTransitivelyImmutableType

    def meet(that: ClassImmutability): ClassImmutability = {
        if (that == MutableClass)
            that
        else
            this
    }

    override def checkIsEqualOrBetterThan(e: Entity, other: Self): Unit = {
        other match {
            case TransitivelyImmutableClass | DependentlyImmutableClass(_) =>
                throw new IllegalArgumentException(s"$e: impossible refinement: $other => $this")
            case _ =>
        }
    }
}

case object MutableClass extends ClassImmutability {

    def correspondingTypeImmutability: TypeImmutability = MutableType

    def meet(other: ClassImmutability): ClassImmutability = this

    override def checkIsEqualOrBetterThan(e: Entity, other: Self): Unit = {
        if (other != MutableClass) {
            throw new IllegalArgumentException(s"$e: impossible refinement: $other => $this")
        }
    }
}
