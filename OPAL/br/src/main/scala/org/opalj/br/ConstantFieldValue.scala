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
 * A representation of the constant value of a field.
 *
 * @author Michael Eichberg
 */
sealed abstract class ConstantFieldValue[T >: Nothing] extends Attribute with ConstantValue[T] {

    override def similar(other: Attribute, config: SimilarityTestConfiguration): Boolean = this == other
}

final case class ConstantLong(value: Long) extends ConstantFieldValue[Long] {

    override def toLong: Long = value

    override def valueToString: String = value.toString

    final def toJava: String = valueToString+"l"

    override def valueType: LongType = LongType

    override def kindId: Int = ConstantLong.KindId

}
object ConstantLong {

    final val KindId = 1

}

final case class ConstantInteger(value: Int) extends ConstantFieldValue[Int] {

    override def toBoolean: Boolean = value != 0

    override def toByte: Byte = value.toByte

    override def toChar: Char = value.toChar

    override def toShort: Short = value.toShort

    override def toInt: Int = value

    override def valueToString: String = value.toString

    final def toJava: String = valueToString

    override def valueType: IntegerType = IntegerType

    override def kindId: Int = ConstantInteger.KindId

}
object ConstantInteger {

    final val KindId = 2

}

final case class ConstantDouble(value: Double) extends ConstantFieldValue[Double] {

    override def toDouble: Double = value

    override def valueToString: String = value.toString

    final def toJava: String = valueToString+"d"

    override def valueType: DoubleType = DoubleType

    override def kindId: Int = ConstantDouble.KindId

    override def equals(other: Any): Boolean = {
        other match {
            case ConstantDouble(thatValue) ⇒
                (this.value == thatValue) || (this.value.isNaN && thatValue.isNaN)
            case _ ⇒
                false
        }
    }

}
object ConstantDouble {

    final val KindId = 3

}

final case class ConstantFloat(value: Float) extends ConstantFieldValue[Float] {

    override def toFloat: Float = value

    override def valueToString: String = value.toString

    final def toJava: String = valueToString+"f"

    override def valueType: FloatType = FloatType

    override def kindId: Int = ConstantFloat.KindId

    override def equals(other: Any): Boolean = {
        other match {
            case ConstantFloat(thatValue) ⇒
                (this.value == thatValue) || (this.value.isNaN && thatValue.isNaN)
            case _ ⇒
                false
        }
    }

}
object ConstantFloat {

    final val KindId = 4

}

final case class ConstantString(value: String) extends ConstantFieldValue[String] {

    override def toUTF8: String = value

    override def valueToString: String = value.toString

    final def toJava: String = s""""$valueToString""""

    override def valueType: ObjectType = ObjectType.String

    override def kindId: Int = ConstantString.KindId

}
object ConstantString {

    final val KindId = 5

}
