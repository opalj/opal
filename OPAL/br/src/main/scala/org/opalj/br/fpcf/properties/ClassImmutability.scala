/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package properties

import org.opalj.fpcf.Entity
import org.opalj.fpcf.OrderedProperty
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation

sealed trait ClassImmutabilityPropertyMetaInformation extends PropertyMetaInformation {
    final type Self = ClassImmutability
}

/**
 * Describes the class immutability of org.opalj.br.ClassFile.
 * The immutability of the classes are represented via their instance fields and the immutability of its supertype.
 *
 * [[MutableClass]] A class with a mutable state.
 *
 * [[ShallowImmutableClass]] A class which transitive state is not immutable but the values or objects representing
 * this transitive state (are not / can not be) exchanged.
 *
 * [[DependentImmutableClass]] A class that is at least shallow immutable.
 * Whether it is shallow or deep immutable depends on generic parameters.
 *
 * [[DeepImmutableClass]] A class with a transitive immutable state.
 *
 * @author Tobias Roth
 */
sealed trait ClassImmutability
    extends OrderedProperty
    with ClassImmutabilityPropertyMetaInformation {
    final def key: PropertyKey[ClassImmutability] = ClassImmutability.key
    def correspondingTypeImmutability: TypeImmutability
}

object ClassImmutability extends ClassImmutabilityPropertyMetaInformation {

    /**
     * The key associated with every [[ClassImmutability]] property.
     */
    final val key: PropertyKey[ClassImmutability] = PropertyKey.create(
        "opalj.ClassImmutability_new",
        MutableClass
    )
}

case object DeepImmutableClass extends ClassImmutability {

    override def correspondingTypeImmutability: TypeImmutability = DeepImmutableType

    override def checkIsEqualOrBetterThan(e: Entity, other: Self): Unit = {}

    def meet(that: ClassImmutability): ClassImmutability = that
}

case object DependentImmutableClass extends ClassImmutability {

    override def correspondingTypeImmutability: TypeImmutability = DependentImmutableType

    def meet(that: ClassImmutability): ClassImmutability =
        if (that == MutableClass || that == ShallowImmutableClass)
            that
        else
            this

    override def checkIsEqualOrBetterThan(e: Entity, other: Self): Unit = {
        if (other == DeepImmutableClass) {
            throw new IllegalArgumentException(s"$e: impossible refinement: $other ⇒ $this")
        }
    }
}

case object ShallowImmutableClass extends ClassImmutability {

    override def correspondingTypeImmutability: TypeImmutability = ShallowImmutableType

    def meet(that: ClassImmutability): ClassImmutability = {
        if (that == MutableClass)
            that
        else
            this
    }

    override def checkIsEqualOrBetterThan(e: Entity, other: Self): Unit = {
        if (other == DeepImmutableClass || other == DependentImmutableClass) {
            throw new IllegalArgumentException(s"$e: impossible refinement: $other ⇒ $this")
        }
    }
}

case object MutableClass extends ClassImmutability {

    def correspondingTypeImmutability = MutableType

    def meet(other: ClassImmutability): ClassImmutability = this

    override def checkIsEqualOrBetterThan(e: Entity, other: Self): Unit = {
        if (other != MutableClass) {
            throw new IllegalArgumentException(s"$e: impossible refinement: $other ⇒ $this")
        }
    }
}
