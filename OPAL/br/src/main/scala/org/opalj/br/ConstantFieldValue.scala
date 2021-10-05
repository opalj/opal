/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

import org.opalj.value.IsDoubleValue
import org.opalj.value.IsFloatValue
import org.opalj.value.IsIntegerValue
import org.opalj.value.IsLongValue
import org.opalj.value.IsStringValue
import org.opalj.value.KnownTypedValue

/**
 * A representation of the constant value of a field.
 *
 * @author Michael Eichberg
 */
sealed abstract class ConstantFieldValue[T >: Nothing]
    extends Attribute
    with ConstantValue[T]
    with KnownTypedValue {

    override def similar(other: Attribute, config: SimilarityTestConfiguration): Boolean = this == other
}

final case class ConstantLong(value: Long) extends ConstantFieldValue[Long] with IsLongValue {

    override def constantValue: Option[Long] = Some(value)

    override def toLong: Long = value

    override def valueToString: String = value.toString

    override def toJava: String = valueToString+"l"

    override def runtimeValueType: LongType = LongType

    override def kindId: Int = ConstantLong.KindId

}
object ConstantLong {

    final val KindId = 1

}

final case class ConstantInteger(value: Int) extends ConstantFieldValue[Int] with IsIntegerValue {

    override def constantValue: Option[Int] = Some(value)

    override def lowerBound: Int = value

    override def upperBound: Int = value

    override def toBoolean: Boolean = value != 0

    override def toByte: Byte = value.toByte

    override def toChar: Char = value.toChar

    override def toShort: Short = value.toShort

    override def toInt: Int = value

    override def valueToString: String = value.toString

    override def toJava: String = valueToString

    override def runtimeValueType: IntegerType = IntegerType

    override def kindId: Int = ConstantInteger.KindId

}
object ConstantInteger {

    final val KindId = 2

}

final case class ConstantDouble(value: Double) extends ConstantFieldValue[Double] with IsDoubleValue {

    override def constantValue: Option[Double] = Some(value)

    override def toDouble: Double = value

    override def valueToString: String = value.toString

    override def toJava: String = valueToString+"d"

    override def runtimeValueType: DoubleType = DoubleType

    override def kindId: Int = ConstantDouble.KindId

    override def equals(other: Any): Boolean = {
        other match {
            case ConstantDouble(thatValue) =>
                (this.value == thatValue) || (this.value.isNaN && thatValue.isNaN)
            case _ =>
                false
        }
    }

}
object ConstantDouble {

    final val KindId = 3

}

final case class ConstantFloat(value: Float) extends ConstantFieldValue[Float] with IsFloatValue {

    override def constantValue: Option[Float] = Some(value)

    override def toFloat: Float = value

    override def valueToString: String = value.toString

    override def toJava: String = valueToString+"f"

    override def runtimeValueType: FloatType = FloatType

    override def kindId: Int = ConstantFloat.KindId

    override def equals(other: Any): Boolean = {
        other match {
            case ConstantFloat(thatValue) =>
                (this.value == thatValue) || (this.value.isNaN && thatValue.isNaN)
            case _ =>
                false
        }
    }

}
object ConstantFloat {

    final val KindId = 4

}

final case class ConstantString(
        value: String
) extends ConstantFieldValue[String]
    with IsStringValue {

    override def toUTF8: String = value

    override def valueToString: String = value.toString

    override def toJava: String = s""""$valueToString""""

    override def runtimeValueType: ObjectType.String.type = ObjectType.String

    override def kindId: Int = ConstantString.KindId

    override def constantValue: Option[String] = Some(value)

    override def toCanonicalForm: IsStringValue = this
}
object ConstantString {

    final val KindId = 5

}
