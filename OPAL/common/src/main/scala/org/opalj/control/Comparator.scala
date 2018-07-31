/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package control

/**
 * Defines common control abstractions.
 *
 * @author Michael Eichberg
 */
trait Comparator[T] {

    /**
     * Compares the given value with the implicit value encapsulated by an instance
     * of this comparator.
     * Returns a value < 0 if the given value is smaller than the implicitly encapsulated value,
     * 0 if the values are equal, and a value > 0 if the given value is larger than the implicitly
     * encapsulated value is larger.
     */
    def evaluate(t: T): Int

}
object Comparator {

    def apply[T](f: (T) ⇒ Int): Comparator[T] = {
        new Comparator[T] { def evaluate(t: T): Int = f(t) }
    }

}
