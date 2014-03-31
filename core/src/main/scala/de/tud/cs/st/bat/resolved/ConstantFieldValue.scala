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
 * A representation of the constant value of a field.
 *
 * @author Michael Eichberg
 */
sealed trait ConstantFieldValue[T >: Nothing] extends Attribute with ConstantValue[T]

final case class ConstantLong(value: Long) extends ConstantFieldValue[Long] {

    override def toLong = value

    override def valueToString = value.toString

    override def valueType = LongType

}

final case class ConstantInteger(value: Int) extends ConstantFieldValue[Int] {

    override def toBoolean = value != 0

    override def toByte = value.toByte

    override def toChar = value.toChar

    override def toShort = value.toShort

    override def toInt = value

    override def valueToString = value.toString

    override def valueType = IntegerType

}

final case class ConstantDouble(value: Double) extends ConstantFieldValue[Double] {

    override def toDouble = value

    override def valueToString = value.toString

    override def valueType = DoubleType

}

final case class ConstantFloat(value: Float) extends ConstantFieldValue[Float] {

    override def toFloat = value

    override def valueToString = value.toString

    override def valueType = FloatType

}

final case class ConstantString(value: String) extends ConstantFieldValue[String] {

    override def toUTF8 = value

    override def valueToString = value.toString

    override def valueType = ObjectType.String

}



