/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package mathematics

/**
 * @author Samuel Beracasa
 */
class Rational(val numer: Number, val denom: Number) extends Operations {

    //Rational operator Number = Rational
    def +(other: Number) =
        new Rational(((other / this.denom) + this.numer), this.denom)
    def -(other: Number) =
        new Rational((other / this.denom) - this.numer, this.denom)
    def *(other: Number) =
        new Rational(other * this.numer, this.denom)
    def /(other: Number) =
        new Rational(this.numer, this.denom / other)

    //Rational operator Rational = Rational
    def +(other: Rational) =
        new Rational((this.numer / other.denom) + (other.numer / this.denom), other.denom * this.denom)
    def -(other: Rational) =
        new Rational((this.numer / other.denom) - (other.numer / this.denom), other.denom * this.denom)
    def *(other: Rational) =
        new Rational((this.numer * other.numer), (other.denom / this.denom))
    def /(other: Rational) =
        new Rational((this.numer * other.denom), (other.numer * this.denom))

    //Print
    override def toString() = numer.toString+"/"+denom.toString
}