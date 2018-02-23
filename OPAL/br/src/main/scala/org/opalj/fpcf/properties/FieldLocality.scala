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

import org.opalj.fpcf.PropertyKey.SomeEPKs

sealed trait FieldLocalityMetaInformation extends PropertyMetaInformation {
    final type Self = FieldLocality
}

sealed abstract class FieldLocality extends Property with FieldLocalityMetaInformation {
    final def key: PropertyKey[FieldLocality] = FieldLocality.key

    def meet(other: FieldLocality): FieldLocality

    def asConditional: FieldLocality

}

object FieldLocality extends FieldLocalityMetaInformation {
    val cycleResolutionStrategy: (PropertyStore, SomeEPKs) ⇒ Iterable[PropertyComputationResult] =
        (ps: PropertyStore, epks: SomeEPKs) ⇒ {
            epks.map { epk ⇒
                ps(epk) match {
                    case EP(e, ConditionalLocalField) ⇒
                        Result(e, LocalField)
                    case EP(e, ConditionalExtensibleLocalField) ⇒
                        Result(e, ExtensibleLocalField)

                    case _ ⇒ throw new RuntimeException("Non-conditional in cycle")
                }
            }
        }

    final lazy val key: PropertyKey[FieldLocality] = PropertyKey.create(
        "FieldLocality",
        NoLocalField,
        cycleResolutionStrategy
    )
}

case object LocalField extends FieldLocality {
    override def isRefinable: Boolean = false

    override def meet(other: FieldLocality): FieldLocality = other

    override def asConditional: FieldLocality = ConditionalLocalField
}

case object ExtensibleLocalField extends FieldLocality {
    override def isRefinable: Boolean = true

    override def meet(other: FieldLocality): FieldLocality = other match {
        case LocalField            ⇒ this
        case ConditionalLocalField ⇒ ConditionalExtensibleLocalField
        case _                     ⇒ other
    }

    override def asConditional: FieldLocality = ConditionalExtensibleLocalField
}

case object NoLocalField extends FieldLocality {
    override def isRefinable: Boolean = true

    override def meet(other: FieldLocality): FieldLocality = other

    override def asConditional: FieldLocality = throw new UnsupportedOperationException()
}

case object ConditionalLocalField extends FieldLocality {
    override def isRefinable: Boolean = true

    override def meet(other: FieldLocality): FieldLocality = other match {
        case LocalField           ⇒ this
        case ExtensibleLocalField ⇒ ConditionalExtensibleLocalField
        case _                    ⇒ other
    }

    override def asConditional: FieldLocality = this
}

case object ConditionalExtensibleLocalField extends FieldLocality {
    override def isRefinable: Boolean = true

    override def meet(other: FieldLocality): FieldLocality = other match {
        case NoLocalField ⇒ other
        case _            ⇒ this
    }

    override def asConditional: FieldLocality = this
}