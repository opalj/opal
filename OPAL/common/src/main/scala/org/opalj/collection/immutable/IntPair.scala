/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.collection.immutable

/**
 * An immutable pair of int values.
 *
 * @author Michael Eichberg
 */
final class IntPair(val _1: Int, val _2: Int) {

    def first: Int = _1
    def second: Int = _1

    def foreach(f: Int ⇒ Unit): Unit = { f(_1); f(_2) }

    override def equals(other: Any): Boolean = {
        other match {
            case that: IntPair ⇒ this._1 == that._1 && this._2 == that._2
            case _             ⇒ false
        }
    }

    override def hashCode: Int = _1 * 17 + _2
}

object IntPair {

    def apply(_1: Int, _2: Int): IntPair = new IntPair(_1, _2)

}
