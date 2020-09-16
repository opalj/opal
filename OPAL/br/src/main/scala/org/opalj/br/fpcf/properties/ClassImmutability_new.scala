/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package properties

import org.opalj.fpcf.Entity
import org.opalj.fpcf.OrderedProperty
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation

sealed trait ClassImmutabilityPropertyMetaInformation_new extends PropertyMetaInformation {
    final type Self = ClassImmutability_new
}

/**
 * Describes the class immutability of org.opalj.br.ClassFile.
 * The immutability of the classes are represented via their instance fields
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
 * @author Tobias Peter Roth
 */
sealed trait ClassImmutability_new
    extends OrderedProperty
    with ClassImmutabilityPropertyMetaInformation_new {
    final def key: PropertyKey[ClassImmutability_new] = ClassImmutability_new.key
    def correspondingTypeImmutability: TypeImmutability_new
}

object ClassImmutability_new extends ClassImmutabilityPropertyMetaInformation_new {

    /**
     * The key associated with every [[ClassImmutability_new]] property.
     */
    final val key: PropertyKey[ClassImmutability_new] = PropertyKey.create(
        "opalj.ClassImmutability_new",
        MutableClass
    )
}

case object DeepImmutableClass extends ClassImmutability_new {

    override def correspondingTypeImmutability: TypeImmutability_new = DeepImmutableType

    override def checkIsEqualOrBetterThan(e: Entity, other: Self): Unit = {}

    def meet(that: ClassImmutability_new): ClassImmutability_new = that
}

case object DependentImmutableClass extends ClassImmutability_new {

    override def correspondingTypeImmutability: TypeImmutability_new = DependentImmutableType

    def meet(that: ClassImmutability_new): ClassImmutability_new =
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

case object ShallowImmutableClass extends ClassImmutability_new {

    override def correspondingTypeImmutability: TypeImmutability_new = ShallowImmutableType

    def meet(that: ClassImmutability_new): ClassImmutability_new = {
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

case object MutableClass extends ClassImmutability_new {

    def correspondingTypeImmutability = MutableType_new

    def meet(other: ClassImmutability_new): ClassImmutability_new = this

    override def checkIsEqualOrBetterThan(e: Entity, other: Self): Unit = {
        if (other != MutableClass) {
            throw new IllegalArgumentException(s"$e: impossible refinement: $other ⇒ $this")
        }
    }
}
