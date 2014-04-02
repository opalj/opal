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
package de.tud.cs.st
package bat
package resolved

/**
 * An annotation's value.
 *
 * @author Michael Eichberg
 * @author Arne Lottmann
 */
sealed trait ElementValue
        extends Attribute {

    def toJava: String
}

case class ByteValue(
    value: Byte)
        extends ElementValue {

    def toJava = value.toString
}

case class CharValue(
    value: Char)
        extends ElementValue {

    def toJava = value.toString
}

case class DoubleValue(
    value: Double)
        extends ElementValue {

    def toJava = value.toString
}

case class FloatValue(
    value: Float)
        extends ElementValue {

    def toJava = value.toString
}

case class IntValue(
    value: Int)
        extends ElementValue {

    def toJava = value.toString
}

case class LongValue(
    value: Long)
        extends ElementValue {

    def toJava = value.toString
}

case class ShortValue(
    value: Short)
        extends ElementValue {

    def toJava = value.toString
}

case class BooleanValue(
    value: Boolean)
        extends ElementValue {

    def toJava = value.toString
}

case class StringValue(
    value: String)
        extends ElementValue {

    def toJava = "\""+value.toString+"\""
}

case class ClassValue(
    value: Type)
        extends ElementValue {

    def toJava = value.toJava+".class"
}

case class EnumValue(
    enumType: ObjectType,
    constName: String)
        extends ElementValue {

    def toJava = enumType.toJava+"."+constName
}

case class ArrayValue(
    values: IndexedSeq[ElementValue])
        extends ElementValue {

    def toJava = values.map(_.toJava).mkString("{", ",", "}")
}

case class AnnotationValue(
    annotation: Annotation)
        extends ElementValue {

    def toJava = annotation.toJava
}


