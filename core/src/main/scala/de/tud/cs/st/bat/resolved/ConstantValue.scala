/* License (BSD Style License):
 * Copyright (c) 2009 - 2013
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
 *  - Neither the name of the Software Technology Group or Technische
 *    Universität Darmstadt nor the names of its contributors may be used to
 *    endorse or promote products derived from this software without specific
 *    prior written permission.
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
 * Represents constant values.
 *
 * @author Michael Eichberg
 */
sealed trait ConstantValue[T >: Nothing] extends Attribute with BootstrapArgument {

    /**
     * The concrete value.
     */
    def value: T

    /**
     * The type of the concrete value.
     */
    def valueType: Type

    /**
     * A string representation of the concrete value.
     */
    def valueToString: String

    def toBoolean: Boolean =
        BATError("This value ("+this+") cannot be converted to a boolean value.")

    def toByte: Byte =
        BATError("This value ("+this+") cannot be converted to a byte value.")

    def toChar: Char =
        BATError("This value ("+this+") cannot be converted to an char value.")

    def toShort: Short =
        BATError("This value ("+this+") cannot be converted to a short value.")

    def toInt: Int =
        BATError("This value ("+this+") cannot be converted to an int value.")

    def toLong: Long =
        BATError("This value ("+this+") cannot be converted to a long value.")

    def toFloat: Float =
        BATError("This value ("+this+") cannot be converted to a float value.")

    def toDouble: Double =
        BATError("This value ("+this+") cannot be converted to a double value.")

    def toUTF8: String =
        BATError("This value ("+this+") cannot be converted to a String(UTF8) value.")

    def toClass: ReferenceType =
        BATError("This value ("+this+") cannot be converted to a class value.")

}
object ConstantValue {

    def unapply[T](constantValue: ConstantValue[T]): Option[(T, Type)] =
        Some((constantValue.value, constantValue.valueType))
}

case class ConstantLong(value: Long) extends ConstantValue[Long] {

    override def toLong = value

    def valueToString = value.toString

    def valueType = LongType

}

case class ConstantInteger(value: Int) extends ConstantValue[Int] {

    override def toBoolean = value != 0 // TODO Does this method: ConstantInteger.toBoolean makes sense?

    override def toByte = value.toByte

    override def toChar = value.toChar

    override def toShort = value.toShort

    override def toInt = value

    def valueToString = value.toString

    def valueType = IntegerType

}

case class ConstantDouble(value: Double) extends ConstantValue[Double] {

    override def toDouble = value

    def valueToString = value.toString

    def valueType = DoubleType

}

case class ConstantFloat(value: Float) extends ConstantValue[Float] {

    override def toFloat = value

    def valueToString = value.toString

    def valueType = FloatType

}

case class ConstantString(value: String) extends ConstantValue[String] {

    override def toUTF8 = value

    def valueToString = value.toString

    def valueType = ObjectType.String

}

// ConstantClass is used by anewarray and multianewarray
case class ConstantClass(value: ReferenceType) extends ConstantValue[ReferenceType] {

    override def toClass = value

    def valueToString = value.toJava

    def valueType = ObjectType.Class

}


