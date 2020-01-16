/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.br.fpcf.properties

import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation

sealed trait ReferenceImmutabilityLazyInstantiationPropertyMetaInformation
    extends PropertyMetaInformation {

    type Self = ReferenceImmutabilityLazyInitialization

}

/**
 * Describes if the reference of a org.opalj.br.Field was lazyliy initialized.
 *
 * [[NoLazyInitialization]] The reference is not lazily initialized
 *
 * [[NotThreadSafeLazyInitialization]] The reference is lazily initialized, but not threadsafe
 *
 * [[LazyInitialization]] The reference is lazy initialized
 *
 * @author Tobias Peter Roth
 */
sealed trait ReferenceImmutabilityLazyInitialization
    extends Property
    with ReferenceImmutabilityLazyInstantiationPropertyMetaInformation {
    final def key: PropertyKey[ReferenceImmutabilityLazyInitialization] =
        ReferenceImmutabilityLazyInitialization.key
}

object ReferenceImmutabilityLazyInitialization
    extends ReferenceImmutabilityLazyInstantiationPropertyMetaInformation {

    final val PropertyKeyName = "opalj.ReferenceImmutabilityLazyInitialization"

    final val key: PropertyKey[ReferenceImmutabilityLazyInitialization] = {
        PropertyKey.create(
            PropertyKeyName,
            NoLazyInitialization
        )
    }
}

case object NoLazyInitialization extends ReferenceImmutabilityLazyInitialization

case object NotThreadSafeLazyInitialization extends ReferenceImmutabilityLazyInitialization

case object LazyInitialization extends ReferenceImmutabilityLazyInitialization
