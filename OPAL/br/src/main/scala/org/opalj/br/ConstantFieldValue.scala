/* BSD 2-Clause License - see OPAL/LICENSE for details. */
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
