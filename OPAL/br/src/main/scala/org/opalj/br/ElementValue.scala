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
package br

/**
 * An element value represents an annotation's value or an
 * annonation's default value; depending on the context in
 * which it is used.
 *
 * @author Michael Eichberg
 * @author Arne Lottmann
 */
sealed trait ElementValue extends Attribute {

    def valueType: FieldType

    /**
     * The representation of this element value as a Java literal/expression.
     */
    def toJava: String

    def asIntValue: IntValue = throw new ClassCastException()
    def asEnumValue: EnumValue = throw new ClassCastException()
    def asAnnotationValue: AnnotationValue = throw new ClassCastException()
    def asStringValue: StringValue = throw new ClassCastException()
    def asArrayValue: ArrayValue = throw new ClassCastException()
    def asClassValue: ClassValue = throw new ClassCastException()

}
object ElementValue {
    final val minKindId = ByteValue.KindId
    final val maxKindId = AnnotationValue.KindId

    def unapply(attribute: Attribute): Boolean = {
        attribute match {
            case _: ElementValue ⇒ true
            case _               ⇒ false
        }
    }
}

/**
 * Common super trait of all element values with a primitive base type.
 *
 * @author Michael Eichberg
 */
sealed trait BaseTypeElementValue extends ElementValue {

    final override def valueType = baseType

    def baseType: BaseType

    override def similar(other: Attribute, config: SimilarityTestConfiguration): Boolean = this == other
}

case class ByteValue(value: Byte) extends BaseTypeElementValue {

    override def baseType: BaseType = ByteType

    override def toJava: String = value.toString

    override def kindId: Int = ByteValue.KindId

}
object ByteValue {

    final val KindId = 29

}

case class CharValue(value: Char) extends BaseTypeElementValue {

    override def baseType: BaseType = CharType

    override def toJava: String = value.toString

    override def kindId: Int = CharValue.KindId

}
object CharValue {

    final val KindId = 30

}

case class DoubleValue(value: Double) extends BaseTypeElementValue {

    override def baseType: BaseType = DoubleType

    override def toJava: String = value.toString

    override def kindId: Int = DoubleValue.KindId

}
object DoubleValue {

    final val KindId = 31

}

case class FloatValue(value: Float) extends BaseTypeElementValue {

    override def baseType: BaseType = FloatType

    override def toJava: String = value.toString

    override def kindId: Int = FloatValue.KindId

}
object FloatValue {

    final val KindId = 32

}

case class IntValue(value: Int) extends BaseTypeElementValue {

    override def baseType: BaseType = IntegerType

    override def toJava: String = value.toString

    override def kindId: Int = IntValue.KindId

    final override def asIntValue: IntValue = this

}
object IntValue {

    final val KindId = 33

}

case class LongValue(value: Long) extends BaseTypeElementValue {

    override def baseType: BaseType = LongType

    override def toJava: String = value.toString

    override def kindId: Int = LongValue.KindId

}
object LongValue {

    final val KindId = 34

}

case class ShortValue(value: Short) extends BaseTypeElementValue {

    override def baseType: BaseType = ShortType

    override def toJava: String = value.toString

    override def kindId: Int = ShortValue.KindId

}
object ShortValue {

    final val KindId = 35

}

case class BooleanValue(value: Boolean) extends BaseTypeElementValue {

    override def baseType: BaseType = BooleanType

    override def toJava: String = value.toString

    override def kindId: Int = BooleanValue.KindId

}
object BooleanValue {

    final val KindId = 36

}

case class StringValue(value: String) extends ElementValue {

    final override def valueType = ObjectType.String

    override def toJava: String = "\""+value.toString+"\""

    override def kindId: Int = StringValue.KindId

    override def similar(other: Attribute, config: SimilarityTestConfiguration): Boolean = {
        this == other
    }

    final override def asStringValue: StringValue = this

}
object StringValue {

    final val KindId = 37

}

case class ClassValue(value: Type) extends ElementValue {

    final override def valueType = ObjectType.Class

    override def toJava: String = value.toJava+".class"

    override def kindId: Int = ClassValue.KindId

    override def similar(other: Attribute, config: SimilarityTestConfiguration): Boolean = {
        this == other
    }

    final override def asClassValue: ClassValue = this

}
object ClassValue {

    final val KindId = 38

}

case class EnumValue(enumType: ObjectType, constName: String) extends ElementValue {

    final override def valueType = enumType

    override def toJava: String = enumType.toJava+"."+constName

    override def kindId: Int = EnumValue.KindId

    override def similar(other: Attribute, config: SimilarityTestConfiguration): Boolean = {
        this == other
    }

    final override def asEnumValue: EnumValue = this

}
object EnumValue {

    final val KindId = 39

}

case class ArrayValue(values: IndexedSeq[ElementValue]) extends ElementValue {

    // by design/specification the first value determines the type of the Array
    final override def valueType = ArrayType(values(0).valueType)

    override def toJava: String = values.map(_.toJava).mkString("{", ",", "}")

    override def kindId: Int = ArrayValue.KindId

    override def similar(other: Attribute, config: SimilarityTestConfiguration): Boolean = {
        this == other
    }

    final override def asArrayValue: ArrayValue = this

}
object ArrayValue {

    final val KindId = 40

}

case class AnnotationValue(annotation: Annotation) extends ElementValue {

    final override def valueType = annotation.annotationType

    override def toJava: String = annotation.toJava

    override def kindId: Int = AnnotationValue.KindId

    override def similar(other: Attribute, config: SimilarityTestConfiguration): Boolean = {
        other match {
            case AnnotationValue(thatAnnotation) ⇒ this.annotation.similar(thatAnnotation)
            case _                               ⇒ false
        }
    }

    final override def asAnnotationValue: AnnotationValue = this
}
object AnnotationValue {

    final val KindId = 41

}
