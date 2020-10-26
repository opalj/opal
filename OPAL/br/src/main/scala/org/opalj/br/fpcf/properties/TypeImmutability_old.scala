/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package properties

import org.opalj.fpcf.OrderedProperty
import org.opalj.fpcf.Entity
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation

sealed trait TypeImmutabilityPropertyMetaInformation_old extends PropertyMetaInformation {

    final type Self = TypeImmutability_old
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
 * [[ClassImmutability_old]].
 *
 * @author Michael Eichberg
 */
sealed trait TypeImmutability_old extends OrderedProperty with TypeImmutabilityPropertyMetaInformation_old {

    /**
     * Returns the key used by all `TypeImmutability` properties.
     */
    final def key = TypeImmutability_old.key

    def isImmutable: Boolean
    def isImmutableContainer: Boolean
    /** `true` if the mutability is unknown or if the type is mutable.*/
    def isMutable: Boolean

    def meet(other: TypeImmutability_old): TypeImmutability_old
}
/**
 * Common constants use by all [[TypeImmutability_old]] properties associated with methods.
 */
object TypeImmutability_old extends TypeImmutabilityPropertyMetaInformation_old {

    /**
     * The key associated with every [[TypeImmutability_old]] property.
     */
    final val key: PropertyKey[TypeImmutability_old] = PropertyKey.create(
        "org.opalj.TypeImmutability",
        MutableType_old
    )
}

/**
 * An instance of the respective class is effectively immutable. I.e., after creation it is not
 * possible for a client to set a field or to call a method that updates the internal state
 */
case object ImmutableType extends TypeImmutability_old {

    override def isImmutable: Boolean = true
    override def isImmutableContainer: Boolean = false
    override def isMutable: Boolean = false

    override def checkIsEqualOrBetterThan(e: Entity, other: Self): Unit = {}

    def meet(that: TypeImmutability_old): TypeImmutability_old = if (this == that) this else that

}

case object ImmutableContainerType extends TypeImmutability_old {

    override def isImmutable: Boolean = false
    override def isImmutableContainer: Boolean = true
    override def isMutable: Boolean = false

    def meet(that: TypeImmutability_old): TypeImmutability_old = if (that == MutableType_old) that else this

    override def checkIsEqualOrBetterThan(e: Entity, other: Self): Unit = {
        if (other == ImmutableType) {
            throw new IllegalArgumentException(s"$e: impossible refinement: $other ⇒ $this");
        }
    }
}

case object MutableType_old extends TypeImmutability_old {

    override def isImmutable: Boolean = false
    override def isImmutableContainer: Boolean = false
    override def isMutable: Boolean = true

    def meet(other: TypeImmutability_old): this.type = this

    override def checkIsEqualOrBetterThan(e: Entity, other: Self): Unit = {
        if (other != MutableType_old) {
            throw new IllegalArgumentException(s"$e: impossible refinement: $other ⇒ $this");
        }
    }
}

