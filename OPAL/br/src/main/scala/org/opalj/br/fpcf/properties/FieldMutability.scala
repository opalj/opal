/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package properties

import org.opalj.fpcf.FallbackReason
import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Entity
import org.opalj.br.analyses.SomeProject

sealed trait FieldMutabilityPropertyMetaInformation extends PropertyMetaInformation {

    type Self = FieldMutability

}

/**
 * Identifies those fields which are effectively final; that is, those fields which are not
 * read before the final value is set. Field reads which are just done to test if the field
 * still contains the default JVM value are ignored if no other field-read can bypass the test.
 * As in case of `final` fields, field writes done by (static) initializers w.r.t. the currently
 * constructed (class) object generally do not prevent the field from being effectively final.
 *
 * Hence, a user of the respective class/object will always see the same value. It is
 * also possible that the initializing field write is not done by the class itself but done by
 * a specific caller that is guaranteed to be always executed before the field is (read)
 * accessed elsewhere.
 * Here, the initialization phase for a specific field always starts with the call of a
 * constructor and ends when the field is set to some value (in a thread-safe manner)
 * or the field is potentially read.
 *
 * == Property Extensions ==
 *
 * - declared final
 *   - actually directly declared as final
 *   - no premature reads occur (through a virtual call in a (super) constructor)
 *
 * - lazy initialized
 *   - all field writes and reads are known, i.e., in case of ...
 *     - ... open package assumption: all private fields
 *     - ... closed package assumption: all private and package private fields and all
 *       protected and public fields that are not accessible by clients of the package
 *     - ... applications: all fields
 *   - all writes have to be guarded by a test if the field still has the default value
 *   - all reads happen after initialization of the field and are either also guarded by
 *     a test or happen directly after a field write
 *   - if the field is set to a value that is not the default value, the field is in all
 *     cases (even in case of concurrent execution!) set to the same value (`0`, `0l`,
 *     `0f`, `0d`, `null`)
 *   - the field's value can never be observed uninitialized and the initialization itself can not
 *     be observed (except for locks required to ensure that the synchronization happens only once)
 *
 * - effectively final
 *   - all field writes are known (see above)
 *   - all writes happen unconditionally at initialization time
 *   - as soon as the field is read no more writes will ever occur
 *   - no premature reads occur (through a virtual call in a (super) constructor)
 *
 * - non-final
 *   - a field is non final if non of the the previous cases holds
 *   - e.g., not all reads and writes of the field are known
 *
 * @note A field's mutability is unrelated to the immutability of the referenced objects!
 *
 * @author Michael Eichberg
 * @author Michael Reif
 */
sealed trait FieldMutability extends Property with FieldMutabilityPropertyMetaInformation {

    final def key: PropertyKey[FieldMutability] = FieldMutability.key

    def isEffectivelyFinal: Boolean
}

object FieldMutability extends FieldMutabilityPropertyMetaInformation {

    final val PropertyKeyName = "opalj.FieldMutability"

    final val key: PropertyKey[FieldMutability] = {
        PropertyKey.create(
            PropertyKeyName,
            (ps: PropertyStore, _: FallbackReason, e: Entity) => {
                e match {
                    case f: Field =>
                        if (f.isStatic) {
                            if (f.isFinal) DeclaredFinalField else NonFinalFieldByLackOfInformation
                        } else if (f.isFinal) {
                            if (FieldPrematurelyRead.isPrematurelyReadFallback(ps.context(classOf[SomeProject]), f))
                                NonFinalFieldByAnalysis
                            else
                                DeclaredFinalField
                        } else {
                            NonFinalFieldByLackOfInformation
                        }
                    case x =>
                        val m = x.getClass.getSimpleName+" is not an org.opalj.br.Field"
                        throw new IllegalArgumentException(m)
                }
            }
        )
    }

}

// TODO Support ConditionallyFinalField... which is final, if, e.g., an initialization
// is guaranteed to always result in the same value independent of the calling scenario

/**
 * The field is only set once to a non-default value and only the updated value is used.
 */
sealed trait FinalField extends FieldMutability {

    /** `true` if the field is already declared as `final`. */
    val byDefinition: Boolean

    final def isEffectivelyFinal: Boolean = true
}

case object LazyInitializedField extends FinalField { final val byDefinition = false }

case object EffectivelyFinalField extends FinalField { final val byDefinition = false }

case object DeclaredFinalField extends FinalField { final val byDefinition = true }

/**
 * The field is potentially updated multiple times.
 */
sealed trait NonFinalField extends FieldMutability {

    final def isEffectivelyFinal: Boolean = false

}

case object NonFinalFieldByAnalysis extends NonFinalField

case object NonFinalFieldByLackOfInformation extends NonFinalField
