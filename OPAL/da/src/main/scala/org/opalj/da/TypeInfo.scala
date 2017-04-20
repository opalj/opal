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
package da

/**
 * Encapsulates basic type information.
 *
 * @author Michael Eichberg
 */
sealed abstract class TypeInfo {
    def asJavaType: String

    /**
     * `true` if the underlying type (in case of an array the element type) is a base type/
     * primitive type; `false` in all other cases except if the "type" is void. In that case
     * an exception is thrown.
     */
    def elementTypeIsBaseType: Boolean
    def isVoid: Boolean
}

object TypeInfo {
    def unapply(ti: TypeInfo): Some[(String, Boolean)] = {
        Some((ti.asJavaType, ti.elementTypeIsBaseType))
    }
}

case object VoidTypeInfo extends TypeInfo {
    final val asJavaType: String = "void"
    def elementTypeIsBaseType: Boolean = throw new UnsupportedOperationException
    def isVoid: Boolean = true
}

sealed abstract class FieldTypeInfo extends TypeInfo {
    def isVoid: Boolean = false
}

sealed abstract class PrimitiveTypeInfo protected (val asJavaType: String) extends FieldTypeInfo {
    final def elementTypeIsBaseType: Boolean = true
}

case object BooleanTypeInfo extends PrimitiveTypeInfo("boolean")
case object ByteTypeInfo extends PrimitiveTypeInfo("byte")
case object CharTypeInfo extends PrimitiveTypeInfo("char")
case object ShortTypeInfo extends PrimitiveTypeInfo("short")
case object IntTypeInfo extends PrimitiveTypeInfo("int")
case object LongTypeInfo extends PrimitiveTypeInfo("long")
case object FloatTypeInfo extends PrimitiveTypeInfo("float")
case object DoubleTypeInfo extends PrimitiveTypeInfo("double")

case class ObjectTypeInfo(asJavaType: String) extends FieldTypeInfo {
    def elementTypeIsBaseType: Boolean = false
}

case class ArrayTypeInfo(
        elementTypeAsJavaType: String,
        dimensions:            Int,
        elementTypeIsBaseType: Boolean
) extends FieldTypeInfo {

    assert(dimensions > 0)

    def asJavaType: String = elementTypeAsJavaType + ("[]" * dimensions)
}
