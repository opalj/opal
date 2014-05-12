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
package br

/**
 * Represents constant values; i.e., values pushed onto the stack by the ldc(2)(_w)
 * instructions or type information required by the instructions to create arrays.
 *
 * @note A `MethodHandle` or ''MethodType'' (i.e., a `MethodDescriptor`) is also
 *      a `ConstantValue`.
 *
 * @author Michael Eichberg
 */
trait ConstantValue[T >: Nothing] extends BootstrapArgument {

    /**
     * The concrete value.
     */
    def value: T

    /**
     * The (runtime) type of the concrete value.
     */
    def valueType: Type

    /**
     * A string representation of the concrete value; used for debugging purposes.
     */
    def valueToString: String

    private[this] def className: String = this.getClass.getSimpleName

    def toBoolean: Boolean =
        throw new BytecodeProcessingFailedException(
            className+" cannot be converted to a boolean value")

    def toByte: Byte =
        throw new BytecodeProcessingFailedException(
            className+" cannot be converted to a byte value")

    def toChar: Char =
        throw new BytecodeProcessingFailedException(
            className+" cannot be converted to an char value")

    def toShort: Short =
        throw new BytecodeProcessingFailedException(
            className+" cannot be converted to a short value")

    def toInt: Int =
        throw new BytecodeProcessingFailedException(
            className+" cannot be converted to an int value")

    def toLong: Long =
        throw new BytecodeProcessingFailedException(
            className+" cannot be converted to a long value")

    def toFloat: Float =
        throw new BytecodeProcessingFailedException(
            className+" cannot be converted to a float value")

    def toDouble: Double =
        throw new BytecodeProcessingFailedException(
            className+" cannot be converted to a double value")

    def toUTF8: String =
        throw new BytecodeProcessingFailedException(
            className+" cannot be converted to a String(UTF8) value")

    def toReferenceType: ReferenceType =
        throw new BytecodeProcessingFailedException(
            className+" cannot be converted to a reference type")
}

/**
 * Represents a class or interface.
 *
 * `ConstantClass` is, e.g., used by `anewarray` and `multianewarray` instructions.
 * A `ConstantClass` value is never a `Field` value. I.e., it is never used to
 * set the value of a static field.
 */
final case class ConstantClass(value: ReferenceType) extends ConstantValue[ReferenceType] {

    override def valueToString = value.toJava

    override def valueType = ObjectType.Class

    final override def toReferenceType: ReferenceType = value
}

/**
 * Facilitates matching constant values.
 *
 * @author Michael Eichberg
 */
object ConstantValue {

    def unapply[T](constantValue: ConstantValue[T]): Option[(T, Type)] =
        Some((constantValue.value, constantValue.valueType))
}
