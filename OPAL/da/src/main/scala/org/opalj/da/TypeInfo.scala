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
package da

/**
 * @author Michael Eichberg
 */
sealed trait TypeInfo {
    def javaTypeName: String
    def elementTypeIsBaseType: Boolean

}

object TypeInfo {
    def unapply(ti: TypeInfo): Some[(String, Boolean)] = {
        Some((ti.javaTypeName, ti.elementTypeIsBaseType))
    }
}

abstract class PrimitiveTypeInfo protected (val javaTypeName: String) extends TypeInfo {
    final def elementTypeIsBaseType = true
}

case object BooleanTypeInfo extends PrimitiveTypeInfo("boolean")
case object ByteTypeInfo extends PrimitiveTypeInfo("byte")
case object CharTypeInfo extends PrimitiveTypeInfo("char")
case object ShortTypeInfo extends PrimitiveTypeInfo("short")
case object IntTypeInfo extends PrimitiveTypeInfo("int")
case object LongTypeInfo extends PrimitiveTypeInfo("long")
case object FloatTypeInfo extends PrimitiveTypeInfo("float")
case object DoubleTypeInfo extends PrimitiveTypeInfo("double")

case class ObjectTypeInfo(javaTypeName: String) extends TypeInfo {
    def elementTypeIsBaseType = false
}

case class ArrayTypeInfo(javaTypeName: String, elementTypeIsBaseType: Boolean) extends TypeInfo