/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.br.fpcf.properties

import org.opalj.fpcf.Property
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
    extends Property
    with ReferenceImmutabilityPropertyMetaInformation {
  final def key: PropertyKey[ReferenceImmutability] = ReferenceImmutability.key
}

object ReferenceImmutability extends ReferenceImmutabilityPropertyMetaInformation {

  final val PropertyKeyName = "opalj.ReferenceImmutability"

  final val key: PropertyKey[ReferenceImmutability] = {
    PropertyKey.create(
      PropertyKeyName,
      MutableReference
    )
  }
}

case object MutableReference extends ReferenceImmutability

case object LazyInitializedReference extends ReferenceImmutability

case object ImmutableReference extends ReferenceImmutability
