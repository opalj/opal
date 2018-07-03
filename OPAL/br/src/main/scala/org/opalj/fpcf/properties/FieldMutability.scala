/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.opalj
package fpcf
package properties

import org.opalj.br.Field

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
 *
 * - effectively final
 *   - all field writes are known (see above)
 *   - all writes happen unconditionally at initialization time
 *   - as soon as the field is read no more writes will ever occur
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

    final def key = FieldMutability.key // All instances have to share the SAME key!

    def isEffectivelyFinal: Boolean
}

object FieldMutability extends FieldMutabilityPropertyMetaInformation {

    final val PropertyKeyName = "FieldMutability"

    final val key: PropertyKey[FieldMutability] = {
        PropertyKey.create(
            PropertyKeyName,
            (_: PropertyStore, _: FallbackReason, e: Entity) ⇒ {
                e match {
                    case f: Field ⇒
                        if (f.isFinal) DeclaredFinalField else NonFinalFieldByAnalysis
                    case x ⇒
                        val m = x.getClass.getSimpleName+" is not an org.opalj.br.Field"
                        throw new IllegalArgumentException(m)
                }
            },
            (_, eps: EPS[Field, FieldMutability]) ⇒ eps.ub,
            (_: PropertyStore, _: Entity) ⇒ None
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
