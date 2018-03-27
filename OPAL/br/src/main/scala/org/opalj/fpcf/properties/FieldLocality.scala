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

    def asUnconditional: FieldLocality

    def isConditional: Boolean

}

object FieldLocality extends FieldLocalityMetaInformation {
    val cycleResolutionStrategy: (PropertyStore, SomeEPKs) ⇒ Iterable[PropertyComputationResult] =
        (ps: PropertyStore, epks: SomeEPKs) ⇒ {
            epks.map { epk ⇒
                ps(epk) match {
                    case EP(e, p: ReturnValueFreshness) if p.isConditional ⇒ Result(e, p.asUnconditional)
                    case EP(e, p: VirtualMethodReturnValueFreshness) if p.isConditional ⇒ Result(e, p.asUnconditional)
                    case EP(e, p: FieldLocality) if p.isConditional ⇒ Result(e, p.asUnconditional)

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

    override def asUnconditional: FieldLocality = this

    override def isConditional: Boolean = false
}

case object ExtensibleLocalField extends FieldLocality {
    override def isRefinable: Boolean = true

    override def meet(other: FieldLocality): FieldLocality = other match {
        case LocalField                      ⇒ this
        case LocalFieldWithGetter            ⇒ ExtensibleLocalFieldWithGetter
        case ConditionalLocalField           ⇒ ConditionalExtensibleLocalField
        case ConditionalLocalFieldWithGetter ⇒ ConditionalExtensibleLocalFieldWithGetter
        case _                               ⇒ other
    }

    override def asConditional: FieldLocality = ConditionalExtensibleLocalField

    override def asUnconditional: FieldLocality = this

    override def isConditional: Boolean = false
}

case object LocalFieldWithGetter extends FieldLocality {
    override def isRefinable: Boolean = true

    override def meet(other: FieldLocality): FieldLocality = other match {
        case LocalField                      ⇒ this
        case ExtensibleLocalField            ⇒ ExtensibleLocalFieldWithGetter
        case ConditionalLocalField           ⇒ ConditionalLocalFieldWithGetter
        case ConditionalExtensibleLocalField ⇒ ConditionalExtensibleLocalFieldWithGetter
        case _                               ⇒ other
    }

    override def asConditional: FieldLocality = ConditionalLocalFieldWithGetter

    override def asUnconditional: FieldLocality = this

    override def isConditional: Boolean = false
}

case object ExtensibleLocalFieldWithGetter extends FieldLocality {
    override def meet(other: FieldLocality): FieldLocality = other match {
        case NoLocalField                 ⇒ other
        case other if other.isConditional ⇒ this.asConditional
        case _                            ⇒ this
    }

    override def asConditional: FieldLocality = ConditionalExtensibleLocalFieldWithGetter

    override def asUnconditional: FieldLocality = this

    override def isConditional: Boolean = false

    override def isRefinable: Boolean = true
}

case object NoLocalField extends FieldLocality {
    override def isRefinable: Boolean = true

    override def meet(other: FieldLocality): FieldLocality = this

    override def asConditional: FieldLocality = throw new UnsupportedOperationException()

    override def asUnconditional: FieldLocality = this

    override def isConditional: Boolean = false
}

sealed abstract class ConditionalFieldLocality extends FieldLocality {
    override def isRefinable: Boolean = true

    override def meet(other: FieldLocality): FieldLocality = {
        (other.asUnconditional meet this.asUnconditional).asConditional
    }

    override def asConditional: FieldLocality = this

    override def isConditional: Boolean = true
}

case object ConditionalLocalField extends ConditionalFieldLocality {
    override def asUnconditional: FieldLocality = LocalField
}

case object ConditionalExtensibleLocalField extends ConditionalFieldLocality {
    override def asUnconditional: FieldLocality = ExtensibleLocalField
}

case object ConditionalLocalFieldWithGetter extends ConditionalFieldLocality {
    override def asUnconditional: FieldLocality = LocalFieldWithGetter
}

case object ConditionalExtensibleLocalFieldWithGetter extends ConditionalFieldLocality {
    override def asUnconditional: FieldLocality = ExtensibleLocalFieldWithGetter
}