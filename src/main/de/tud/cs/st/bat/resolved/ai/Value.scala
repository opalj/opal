/*
 * License (BSD Style License):
 * Copyright (c) 2012
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 * - Neither the name of the Software Technology Group or Technische
 *   Universität Darmstadt nor the names of its contributors may be used to
 *   endorse or promote products derived from this software without specific
 *   prior written permission.
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
package de.tud.cs.st.bat
package resolved
package ai

/**
 * A value on the stack or a value stored in one of the local variables.
 *
 * @note The minimal information that is
 * required by this framework is the computational type of the values. This specific type information is
 * required, e.g., to determine the effect of `dup_...` instructions or the other generic instructions.
 *
 * @author Michael Eichberg
 */
sealed trait Value {
    /**
     * The computational type of the value.
     */
    def computationalType: ComputationalType
}
// TODO Reconsider the type hierarchy

case class ComputationalTypeValue(val computationalType: ComputationalType) extends Value
object ComputationalTypeValue {
    val CTIntegerValue = ComputationalTypeValue(ComputationalTypeInt)
    val CTFloatValue = ComputationalTypeValue(ComputationalTypeFloat)
    val CTReferenceValue = ComputationalTypeValue(ComputationalTypeReference)
    val CTReturnAddressValue = ComputationalTypeValue(ComputationalTypeReturnAddress)
    val CTLongValue = ComputationalTypeValue(ComputationalTypeLong)
    val CTDoubleValue = ComputationalTypeValue(ComputationalTypeDouble)
}

/**
 * Represents a value on the stack or in a local variable for which we have more precise type information
 * than just the computational type value.
 *
 * @author Michael Eichberg
 */
case class TypedValue(valueType: Type) extends Value {
    def computationalType = valueType.computationalType
}
object TypedValue {
    val BooleanValue = TypedValue(BooleanType)
    val ByteValue = TypedValue(ByteType)
    val CharValue = TypedValue(CharType)
    val ShortValue = TypedValue(ShortType)
    val IntegerValue = TypedValue(IntegerType)
    val LongValue = TypedValue(LongType)
    val FloatValue = TypedValue(FloatType)
    val DoubleValue = TypedValue(DoubleType)
    val StringValue = TypedValue(ObjectType("java/lang/String"))
    val ClassValue = TypedValue(ObjectType("java/lang/Class"))
}

case object NullValue extends Value {
    def computationalType = ComputationalTypeReference
}

case class ReturnAddressValue(address: Int) extends Value {
    def computationalType = ComputationalTypeReturnAddress
}