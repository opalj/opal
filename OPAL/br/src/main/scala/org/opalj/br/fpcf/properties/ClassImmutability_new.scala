/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.br.fpcf.properties

import org.opalj.fpcf.Property
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
    extends Property
    with ClassImmutabilityPropertyMetaInformation_new {
  final def key: PropertyKey[ClassImmutability_new] = ClassImmutability_new.key
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

case object MutableClass extends ClassImmutability_new

case object DependentImmutableClass extends ClassImmutability_new

case object ShallowImmutableClass extends ClassImmutability_new

case object DeepImmutableClass extends ClassImmutability_new
