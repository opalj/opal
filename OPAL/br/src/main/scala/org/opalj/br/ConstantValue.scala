/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

import org.opalj.bytecode.BytecodeProcessingFailedException

/**
 * Represents constant values; that is, values pushed onto the stack by the `ldc(2)(_w)`
 * instructions or type information required by the instructions to create arrays.
 *
 * @note    A `MethodHandle` or ''MethodType'' (i.e., a `MethodDescriptor`) is also
 *          a `ConstantValue`.
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
    def runtimeValueType: Type

    /**
     * A string representation of the concrete value; used for debugging purposes.
     */
    def valueToString: String

    private[this] def className: String = this.getClass.getSimpleName

    def toBoolean: Boolean =
        throw new BytecodeProcessingFailedException(
            className+" cannot be converted to a boolean value"
        )

    def toByte: Byte =
        throw new BytecodeProcessingFailedException(
            className+" cannot be converted to a byte value"
        )

    def toChar: Char =
        throw new BytecodeProcessingFailedException(
            className+" cannot be converted to an char value"
        )

    def toShort: Short =
        throw new BytecodeProcessingFailedException(
            className+" cannot be converted to a short value"
        )

    def toInt: Int =
        throw new BytecodeProcessingFailedException(
            className+" cannot be converted to an int value"
        )

    def toLong: Long =
        throw new BytecodeProcessingFailedException(
            className+" cannot be converted to a long value"
        )

    def toFloat: Float =
        throw new BytecodeProcessingFailedException(
            className+" cannot be converted to a float value"
        )

    def toDouble: Double =
        throw new BytecodeProcessingFailedException(
            className+" cannot be converted to a double value"
        )

    def toUTF8: String =
        throw new BytecodeProcessingFailedException(
            className+" cannot be converted to a String(UTF8) value"
        )

    def toReferenceType: ReferenceType =
        throw new BytecodeProcessingFailedException(
            className+" cannot be converted to a reference type"
        )
}

/**
 * Facilitates matching constant values.
 *
 * @author Michael Eichberg
 */
object ConstantValue {

    def unapply[T](constantValue: ConstantValue[T]): Some[(T, Type)] = {
        Some((constantValue.value, constantValue.runtimeValueType))
    }
}
