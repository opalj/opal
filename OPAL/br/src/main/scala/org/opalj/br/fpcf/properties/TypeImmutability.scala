/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package properties

import org.opalj.fpcf.OrderedProperty
import org.opalj.fpcf.Entity
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation

sealed trait TypeImmutabilityPropertyMetaInformation extends PropertyMetaInformation {
    final type Self = TypeImmutability
}

/**
 * Specifies whether all instances of a respective type (this includes the instances of the
 * type's subtypes) are (conditionally) immutable. Conditionally immutable means that only the
 * instance of the type itself is guaranteed to be immutable, but not all reachable objects.
 * In general, all -- so called -- immutable collections are only conditionally immutable. I.e.,
 * the collection as a whole is only immutable if only immutable objects are stored in the
 * collection. If this is not the case, the collection is only conditionally immutable.
 *
 * This property is of particular interest if the precise type cannot be computed statically. This
 * property basically depends on the [[org.opalj.br.analyses.cg.TypeExtensibilityKey]] and
 * [[ClassImmutability]].
 *
 * @author Michael Eichberg
 * @author Tobias Roth
 */
sealed trait TypeImmutability
    extends OrderedProperty
    with TypeImmutabilityPropertyMetaInformation {

    /**
     * Returns the key used by all `TypeImmutability` properties.
     */
    final def key = TypeImmutability.key

    def isDeepImmutable: Boolean
    def isShallowImmutable: Boolean
    def isDependentImmutable: Boolean

    /** `true` if the immutability is unknown or if the type is mutable.*/
    def isMutable: Boolean

    def meet(other: TypeImmutability): TypeImmutability
}

object TypeImmutability extends TypeImmutabilityPropertyMetaInformation {

    /**
     * The key associated with every [[TypeImmutability]] property.
     */
    final val key: PropertyKey[TypeImmutability] = PropertyKey.create(
        "org.opalj.TypeImmutability_new",
        MutableType
    )
}

/**
 * An instance of the respective class is effectively immutable. I.e., after creation it is not
 * possible for a client to set a field or to call a method that updates the internal state
 */
case object DeepImmutableType extends TypeImmutability {

    override def isDeepImmutable: Boolean = true
    override def isShallowImmutable: Boolean = false
    override def isMutable: Boolean = false
    override def isDependentImmutable: Boolean = false

    override def checkIsEqualOrBetterThan(e: Entity, other: Self): Unit = {}

    def meet(that: TypeImmutability): TypeImmutability = that
}

case object DependentImmutableType extends TypeImmutability {
    override def isDeepImmutable: Boolean = false
    override def isShallowImmutable: Boolean = false
    override def isMutable: Boolean = false
    override def isDependentImmutable: Boolean = true

    def meet(that: TypeImmutability): TypeImmutability =
        if (that == MutableType || that == ShallowImmutableType)
            that
        else
            this

    override def checkIsEqualOrBetterThan(e: Entity, other: Self): Unit = {
        if (other == DeepImmutableType) {
            throw new IllegalArgumentException(s"$e: impossible refinement: $other ⇒ $this")
        }
    }
}

case object ShallowImmutableType extends TypeImmutability {

    override def isDeepImmutable: Boolean = false
    override def isShallowImmutable: Boolean = true
    override def isMutable: Boolean = false
    override def isDependentImmutable: Boolean = false

    def meet(that: TypeImmutability): TypeImmutability =
        if (that == MutableType)
            that
        else
            this

    override def checkIsEqualOrBetterThan(e: Entity, other: Self): Unit = {

        if (other == DeepImmutableType || other == DependentImmutableType) {
            throw new IllegalArgumentException(s"$e: impossible refinement: $other ⇒ $this")
        }
    }
}

case object MutableType extends TypeImmutability {

    override def isDeepImmutable: Boolean = false
    override def isShallowImmutable: Boolean = false
    override def isMutable: Boolean = true
    override def isDependentImmutable: Boolean = false

    def meet(other: TypeImmutability): this.type = this

    override def checkIsEqualOrBetterThan(e: Entity, other: Self): Unit = {

        if (other != MutableType) {
            throw new IllegalArgumentException(s"$e: impossible refinement: $other ⇒ $this")
        }
    }
}
