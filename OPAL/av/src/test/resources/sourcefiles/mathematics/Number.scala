/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package mathematics

/**
 * @author Samuel Beracasa
 */
class Number(var value: Int) extends Operations {
    //Number operator Number
    def +(other: Number): Number = new Number(this.value + other.value)
    def -(other: Number): Number = new Number(this.value - other.value)
    def *(other: Number): Number = new Number(this.value * other.value)
    def /(other: Number): Number = new Number(this.value / other.value)

    //Number operator Rational
    def +(other: Rational): Rational =
        new Rational(((this / other.denom) + other.numer), other.denom)
    def -(other: Rational): Rational =
        new Rational((this / other.denom) - other.numer, other.denom)
    def *(other: Rational): Rational =
        new Rational(this * other.numer, other.denom)
    def /(other: Rational): Rational =
        new Rational(other.numer, other.denom / this)

    //Print
    override def toString() = ""+this.value
}