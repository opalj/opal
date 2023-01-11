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

sealed trait TypeImmutabilityPropertyMetaInformation extends PropertyMetaInformation {
    final type Self = TypeImmutability
}

/**
 * Specifies the immutability of a given type.
 *
 * This property is of particular interest if the precise type cannot be computed statically. This
 * property basically depends on the [[org.opalj.br.analyses.cg.TypeExtensibilityKey]] and
 * [[ClassImmutability]].
 *
 * @author Michael Eichberg
 * @author Tobias Roth
 */
sealed trait TypeImmutability extends OrderedProperty with TypeImmutabilityPropertyMetaInformation {

    /**
     * Returns the key used by all `TypeImmutability` properties.
     */
    final def key = TypeImmutability.key

    def isTransitivelyImmutable: Boolean
    def isNonTransitivelyImmutable: Boolean
    def isDependentlyImmutable: Boolean = false
    def isMutable: Boolean = true
    def meet(other: TypeImmutability): TypeImmutability
}

object TypeImmutability extends TypeImmutabilityPropertyMetaInformation {

    /**
     * The key associated with every [[TypeImmutability]] property.
     */
    final val key: PropertyKey[TypeImmutability] = PropertyKey.create(
        "org.opalj.TypeImmutability",
        MutableType
    )
}

/**
 * An instance of the respective class is effectively immutable. I.e., after creation it is not
 * possible for a client to set a field or to call a method that updates the internal state
 */
case object TransitivelyImmutableType extends TypeImmutability {

    override def isTransitivelyImmutable: Boolean = true
    override def isNonTransitivelyImmutable: Boolean = false
    override def isMutable: Boolean = false

    override def checkIsEqualOrBetterThan(e: Entity, other: Self): Unit = {}

    def meet(that: TypeImmutability): TypeImmutability = that
}

case class DependentlyImmutableType(parameter: SortedSet[String]) extends TypeImmutability {
    override def isTransitivelyImmutable: Boolean = false
    override def isNonTransitivelyImmutable: Boolean = false
    override def isMutable: Boolean = false
    override def isDependentlyImmutable: Boolean = true

    def meet(that: TypeImmutability): TypeImmutability =
        if (that == MutableType || that == NonTransitivelyImmutableType)
            that
        else
            this

    override def checkIsEqualOrBetterThan(e: Entity, other: Self): Unit = {
        if (other == TransitivelyImmutableType) {
            throw new IllegalArgumentException(s"$e: impossible refinement: $other => $this")
        }
    }
}

case object NonTransitivelyImmutableType extends TypeImmutability {

    override def isTransitivelyImmutable: Boolean = false
    override def isNonTransitivelyImmutable: Boolean = true
    override def isMutable: Boolean = false

    def meet(that: TypeImmutability): TypeImmutability =
        if (that == MutableType)
            that
        else
            this

    override def checkIsEqualOrBetterThan(e: Entity, other: Self): Unit = {
        other match {
            case TransitivelyImmutableType | DependentlyImmutableType(_) =>
                throw new IllegalArgumentException(s"$e: impossible refinement: $other => $this")
            case _ =>
        }
    }
}

case object MutableType extends TypeImmutability {

    override def isTransitivelyImmutable: Boolean = false
    override def isNonTransitivelyImmutable: Boolean = false
    override def isMutable: Boolean = true

    def meet(other: TypeImmutability): TypeImmutability = this

    override def checkIsEqualOrBetterThan(e: Entity, other: Self): Unit = {
        if (other != MutableType) {
            throw new IllegalArgumentException(s"$e: impossible refinement: $other => $this")
        }
    }
}
