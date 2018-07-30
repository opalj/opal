/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package mathematics

/**
 * @author Samuel Beracasa
 */
class Mathematics {
    val num1 = new Number(1)
    val num2 = new Number(2)
    val num4 = new Number(4)
    val num8 = new Number(8)

    val rat1 = new Rational(num1, num2)
    val rat2 = new Rational(num4, num1)
    val rat3 = new Rational(num1, num8)

    def operation1() = { println(num1 + num4 * rat2 - num2 / rat3 * num8) }
}