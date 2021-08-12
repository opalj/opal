/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package av
package checking

/**
 * Helper class to mark those places where a string using binary notation (i.e.,
 * where packages are separated using "/" instead of ".") is expected.
 *
 * A related implicit conversion is defined in the package object.
 */
final class BinaryString private (private val string: String) {

    assert(string.indexOf('.') == -1)

    def asString: String = this.string

    override def equals(other: Any): Boolean = {
        other match {
            case that: BinaryString => that.string == this.string
            case _                  => false
        }
    }

    override def hashCode: Int = string.hashCode()

    override def toString: String = string.toString()
}

object BinaryString {

    def apply(string: String): BinaryString = new BinaryString(string.replace('.', '/'))

}
