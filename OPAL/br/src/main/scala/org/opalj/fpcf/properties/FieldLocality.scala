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

sealed trait FieldLocalityMetaInformation extends PropertyMetaInformation {
    final type Self = FieldLocality
}

/**
 * A property to describe the lifetime of the values stored in an instance field.
 *
 * [[LocalField]]s have a lifetime that is not longer than that of the field's owning instance.
 * [[ExtensibleLocalField]]s provide the same guarantee only if the (dynamic) type of the owning
 * instance is known not to extend `java.lang.Cloneable`.
 * The lifetime of a value in a [[LocalFieldWithGetter]] can only be extended by it being returned
 * by a method. I.e. if the caller of such method knows that it's receiver is fresh, the field's
 * value may also be treated as fresh.
 * [[ExtensibleLocalFieldWithGetter]] is used if both restrictions apply: The type type of the
 * owning instance may not be cloneable and the value's lifetime could be extended by being
 * returned by a method.
 */
sealed abstract class FieldLocality extends Property with FieldLocalityMetaInformation {
    final def key: PropertyKey[FieldLocality] = FieldLocality.key

    def meet(other: FieldLocality): FieldLocality

}

object FieldLocality extends FieldLocalityMetaInformation {
    final lazy val key: PropertyKey[FieldLocality] = PropertyKey.create(
        "FieldLocality",
        (_: PropertyStore, f: Field) ⇒ if (f.fieldType.isBaseType) LocalField else NoLocalField,
        (_: PropertyStore, eps: EPS[Field, FieldLocality]) ⇒ eps.toUBEP
    )
}

/**
 * A field that is always written with a fresh, non-escaping value. Values read from the field never
 * escape. Therefore, the lifetime of such values is restricted by the lifetime of the field's
 * owning instance.
 */
case object LocalField extends FieldLocality {

    override def meet(other: FieldLocality): FieldLocality = other
}

/**
 * The field is a [[LocalField]] only if the owning instance's (dynamic) type does not implement
 * `java.lang.Cloneable`. Otherwise, the field's value may escape through a shallow copy created
 * through `java.lang.Object.clone`.
 */
case object ExtensibleLocalField extends FieldLocality {

    override def meet(other: FieldLocality): FieldLocality = other match {
        case LocalField           ⇒ this
        case LocalFieldWithGetter ⇒ ExtensibleLocalFieldWithGetter
        case _                    ⇒ other
    }
}

/**
 * The field is a [[LocalField]] except that value's read from it may escape by being returned by
 * a method of the owning instance. Clients can treat the value as fresh if the method's receiver is
 * fresh.
 */
case object LocalFieldWithGetter extends FieldLocality {

    override def meet(other: FieldLocality): FieldLocality = other match {
        case LocalField           ⇒ this
        case ExtensibleLocalField ⇒ ExtensibleLocalFieldWithGetter
        case _                    ⇒ other
    }
}

/**
 * The field is a [[LocalField]] except that value's read from it may escape by being returned by
 * a method of the owning instance. Also, the owning instance's (dynamic) type may not
 * implement `java.lang.Cloneable` or the field may escape through a shallow copy. Clients can
 * treat the field's value as fresh if the method's receiver is fresh and not cloneable.
 */
case object ExtensibleLocalFieldWithGetter extends FieldLocality {
    override def meet(other: FieldLocality): FieldLocality = other match {
        case NoLocalField ⇒ other
        case _            ⇒ this
    }
}

/**
 * The field is not local, i.e. it may be written with non-fresh values or the fields's value may
 * escape other than by being returned by a method of the owning instance.
 */
case object NoLocalField extends FieldLocality {
    override def meet(other: FieldLocality): FieldLocality = this
}