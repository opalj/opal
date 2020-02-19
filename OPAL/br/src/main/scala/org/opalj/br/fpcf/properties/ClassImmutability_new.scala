/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.br.fpcf.properties

import org.opalj.fpcf.Entity
import org.opalj.fpcf.OrderedProperty
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation

sealed trait ClassImmutabilityPropertyMetaInformation_new extends PropertyMetaInformation {
  final type Self = ClassImmutability_new
}

/**
 * Describes the class immutability of org.opalj.br.ClassFile
 *
 * [[MutableClass]] A class with minimum 1 mutable field
 *
 * [[DependentImmutableClass]] A class with no mutable field but with at least one generic field where the
 *  immutability depends on the generic type of the field
 *
 * [[ShallowImmutableClass]] A class with no mutable field, no field with generic type but with at least one
 *  shallow immutable field
 *
 * [[DeepImmutableClass]] A class with only deep immutable fields
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

  def meet(that: ClassImmutability_new): ClassImmutability_new =
    if (this == that)
      this
    else
      that
}

case object DependentImmutableClass extends ClassImmutability_new {
  override def correspondingTypeImmutability: TypeImmutability_new =
    DependentImmutableType //TODO check

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
  def meet(that: ClassImmutability_new): ClassImmutability_new =
    if (that == MutableClass)
      that
    else
      this

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
