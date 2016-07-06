/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2015
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
 * Identifies those fields that are immutable from the perspective of a client of a class/object -
 * that is, whenever a field is (directly or indirectly (for example via a method call))
 * accessed after initialization it will see the same value.
 * From this description it follows that direct field read
 * accesses are possible as long as the value is already finally initialized. In general it is
 * even possible that the initializing field write is not done by the class itself but done by
 * a specific caller that is guaranteed to be always executed before the field is (read)
 * accessed elsewhere.
 * Here, the initialization phase for a specific field always starts with the call of a
 * constructor and ends when the field is set to some value (in a thread-safe manner)
 * or the field is potentially read.
 *
 *
 * == Property manifestations ==
 *
 * 1. declared final
 *   - actually directly declared as final
 *
 * 1. lazy initialized
 *   - all field writes and reads have to be known
 *     - OPA: all private fields
 *     - CPA: all private and package private fields and all protected and public fields that are not accessible by a client
 *     - APP: all fields
 *   - all writes have to be guarded by a test if the field still has the default value
 *   - all reads happen after initialization of the field and are either also guarded by test or happen directly after a field write
 *   - the field is set at most once to a value that is not the default value (`0`, `0l`, `0f`, `0d`, `null`)
 *
 * 1. effectively final
 *   - all criteria of the lazy initialized field have to hold (see previous section)
 *   - all reads and writes have to be guarded by the same (synchronization) lock.
 *
 * 1. non-final
 *   - a field is non final if non of the the previous cases holds
 *   - e.g. not all reads and writes of the field are known
 *
 *
 * @author Michael Eichberg
 * @author Michael Reif
 */
sealed trait FieldMutability extends Property with FieldMutabilityPropertyMetaInformation {

    final def key = FieldMutability.key // All instances have to share the SAME key!

    final val isRefineable: Boolean = false

    def isEffectivelyFinal: Boolean
}

object FieldMutability extends FieldMutabilityPropertyMetaInformation {

    final val key: PropertyKey[FieldMutability] = {
        PropertyKey.create(
            "FieldMutability",
            (p: PropertyStore, e: Entity) ⇒ {
                e match {
                    case f: Field ⇒
                        if (f.isFinal)
                            DeclaredFinalField
                        else
                            NonFinalFieldByAnalysis
                    case x ⇒ throw new Error(x.getClass.getSimpleName+" is not an org.opalj.br.Field")
                }
            },
            NonFinalFieldByLackOfInformation
        )
    }

}

/**
 * The field is only set once to a non-default value and only the updated value is used.
 */
sealed trait FinalField extends FieldMutability {

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
