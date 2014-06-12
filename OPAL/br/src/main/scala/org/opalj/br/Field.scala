/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
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
package br

import bi.ACC_TRANSIENT
import bi.ACC_VOLATILE
import bi.AccessFlagsContexts
import bi.AccessFlags

/**
 * Represents a single field declaration/definition.
 *
 * @note Identity (w.r.t. `equals`/`hashCode`) is intentionally by reference (default
 *      behavior).
 * @param accessFlags This field's access flags. To analyze the access flags
 *      bit vector use [[org.opalj.bi.AccessFlag]] or
 *      [[org.opalj.bi.AccessFlagsIterator]] or use pattern matching.
 * @param name The name of this field. The name is interned (see `String.intern()` for
 *     details.)
 *     Note, that this name is not required to be a valid Java programming
 *     language identifier.
 * @param fieldType The (erased) type of this field.
 * @param attributes The defined attributes. The JVM 8 specification defines
 *     the following attributes for fields:
 *     * [[ConstantValue]],
 *     * [[Synthetic]],
 *     * [[Signature]],
 *     * [[Deprecated]],
 *     * [[RuntimeVisibleAnnotationTable]],
 *     * [[RuntimeInvisibleAnnotationTable]],
 *     * [[RuntimeVisibleTypeAnnotationTable]] and
 *     * [[RuntimeInvisibleTypeAnnotationTable]].
 *
 * @author Michael Eichberg
 */
final class Field private (
    val accessFlags: Int,
    val name: String, // the name is interned to enable reference comparisons!
    val fieldType: FieldType,
    val attributes: Attributes)
        extends ClassMember
        with scala.math.Ordered[Field] {

    final override def isField = true

    final override def asField = this

    def asVirtualField(declaringClassType: ObjectType): VirtualField =
        VirtualField(declaringClassType, name, fieldType)

    def isTransient: Boolean = (ACC_TRANSIENT.mask & accessFlags) != 0

    def isVolatile: Boolean = (ACC_VOLATILE.mask & accessFlags) != 0

    /**
     * Returns this field's type signature.
     */
    def fieldTypeSignature: Option[FieldTypeSignature] =
        attributes collectFirst { case s: FieldTypeSignature ⇒ s }

    /**
     * Returns this field's constant value.
     */
    def constantFieldValue: Option[ConstantFieldValue[_]] =
        attributes collectFirst { case cv: ConstantFieldValue[_] ⇒ cv }

    def toJavaSignature: String = fieldType.toJava+" "+name

    /**
     * Defines an absolute order on `Field` objects w.r.t. their names and types.
     * The order is defined by first lexicographically comparing the names of the
     * fields and – if the names are identical – by comparing the types.
     */
    def compare(other: Field): Int = {
        if (this.name eq other.name) {
            this.fieldType.compare(other.fieldType)
        } else if (this.name < other.name) {
            -1
        } else {
            1
        }
    }

    override def toString(): String = {
        AccessFlags.toStrings(accessFlags, AccessFlagsContexts.FIELD).mkString("", " ", " ") +
            fieldType.toJava+" "+name +
            attributes.view.map(_.getClass().getSimpleName()).mkString(" « ", ", ", " »")
    }
}

/**
 * Defines factory and extractor methods for `Field` objects.
 */
object Field {

    def apply(
        accessFlags: Int,
        name: String,
        fieldType: FieldType,
        attributes: Attributes): Field = {        
        new Field(
            accessFlags,
            name.intern(),
            fieldType,
            attributes)
    }

    def unapply(field: Field): Option[(Int, String, FieldType)] =
        Some((field.accessFlags, field.name, field.fieldType))
}
