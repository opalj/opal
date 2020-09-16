/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package properties

import org.opalj.fpcf.OrderedProperty
import org.opalj.fpcf.Entity
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation

sealed trait TypeImmutabilityPropertyMetaInformation_new extends PropertyMetaInformation {

    final type Self = TypeImmutability_new
}

/**
 * Specifies if all instances of a respective type (this includes the instances of the
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
 * @author Tobias Peter Roth
 */
sealed trait TypeImmutability_new
    extends OrderedProperty
    with TypeImmutabilityPropertyMetaInformation_new {

    /**
     * Returns the key used by all `TypeImmutability_new` properties.
     */
    final def key = TypeImmutability_new.key

    def isDeepImmutable: Boolean
    def isShallowImmutable: Boolean
    def isDependentImmutable: Boolean

    /** `true` if the immutability is unknown or if the type is mutable.*/
    def isMutable: Boolean

    def meet(other: TypeImmutability_new): TypeImmutability_new
}

object TypeImmutability_new extends TypeImmutabilityPropertyMetaInformation_new {

    /**
     * The key associated with every [[TypeImmutability_new]] property.
     */
    final val key: PropertyKey[TypeImmutability_new] = PropertyKey.create(
        "org.opalj.TypeImmutability_new",
        MutableType_new
    )
}

/**
 * An instance of the respective class is effectively immutable. I.e., after creation it is not
 * possible for a client to set a field or to call a method that updates the internal state
 */
case object DeepImmutableType extends TypeImmutability_new {

    override def isDeepImmutable: Boolean = true
    override def isShallowImmutable: Boolean = false
    override def isMutable: Boolean = false
    override def isDependentImmutable: Boolean = false

    override def checkIsEqualOrBetterThan(e: Entity, other: Self): Unit = {}

    def meet(that: TypeImmutability_new): TypeImmutability_new =
        if (this == that)
            this
        else
            that

}

case object DependentImmutableType extends TypeImmutability_new {
    override def isDeepImmutable: Boolean = false
    override def isShallowImmutable: Boolean = false
    override def isMutable: Boolean = false
    override def isDependentImmutable: Boolean = true

    def meet(that: TypeImmutability_new): TypeImmutability_new =
        if (that == MutableType_new || that == ShallowImmutableType)
            that
        else
            this

    override def checkIsEqualOrBetterThan(e: Entity, other: Self): Unit = {

        if (other == DeepImmutableType) {
            throw new IllegalArgumentException(s"$e: impossible refinement: $other ⇒ $this")
        }
    }

}

case object ShallowImmutableType extends TypeImmutability_new {

    override def isDeepImmutable: Boolean = false
    override def isShallowImmutable: Boolean = true
    override def isMutable: Boolean = false
    override def isDependentImmutable: Boolean = false

    def meet(that: TypeImmutability_new): TypeImmutability_new =
        if (that == MutableType_new)
            that
        else
            this

    override def checkIsEqualOrBetterThan(e: Entity, other: Self): Unit = {

        if (other == DeepImmutableType || other == DependentImmutableType) {
            throw new IllegalArgumentException(s"$e: impossible refinement: $other ⇒ $this")
        }
    }
}

case object MutableType_new extends TypeImmutability_new {

    override def isDeepImmutable: Boolean = false
    override def isShallowImmutable: Boolean = false
    override def isMutable: Boolean = true
    override def isDependentImmutable: Boolean = false

    def meet(other: TypeImmutability_new): this.type = this

    override def checkIsEqualOrBetterThan(e: Entity, other: Self): Unit = {

        if (other != MutableType_new) {
            throw new IllegalArgumentException(s"$e: impossible refinement: $other ⇒ $this")
        }
    }
}
