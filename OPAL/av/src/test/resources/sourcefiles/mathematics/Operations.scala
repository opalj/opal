/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package mathematics

/**
 * @author Samuel Beracasa
 */
trait Operations {
    def +(other: Number): Any
    def -(other: Number): Any
    def *(other: Number): Any
    def /(other: Number): Any
    def +(other: Rational): Rational
    def -(other: Rational): Rational
    def *(other: Rational): Rational
    def /(other: Rational): Rational

    def toString(): String
}