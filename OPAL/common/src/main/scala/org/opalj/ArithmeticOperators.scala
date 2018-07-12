/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj

/**
 * All standard binary arithmetic operators defined in the Java Virtual Machine/Java
 * Language Specification.
 *
 * @note The type of a value of this enumeration is [[org.opalj.BinaryArithmeticOperator]].
 *
 * @author Michael Eichberg
 */
object BinaryArithmeticOperators extends Enumeration {

    final val Add = Value("+")
    final val Subtract = Value("-")
    final val Multiply = Value("*")
    final val Divide = Value("/")
    final val Modulo = Value("%")

    final val And = Value("&")
    final val Or = Value("|")
    final val XOr = Value("^")

    final val ShiftLeft = Value("<<")
    final val ShiftRight = Value(">>")
    final val UnsignedShiftRight = Value(">>>")
}

/**
 * All standard unary arithmetic operators defined in the Java Virtual Machine/Java
 * Language Specification.
 *
 * @note The type of a value of this enumeration is [[org.opalj.UnaryArithmeticOperator]].
 *
 * @author Michael Eichberg
 */
object UnaryArithmeticOperators extends Enumeration {

    final val Negate = Value("-")

    final val Not = Value("!")

}
