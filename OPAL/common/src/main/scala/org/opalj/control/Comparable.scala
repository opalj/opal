/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package control

/**
 * Factory for creating a `Comparable` based on a function that enables the
 * (one-way) comparison with value of a specific type.
 *
 * @author Michael Eichberg
 */
object Comparable {

    def apply[T](f: T => Int): Comparable[T] = {
        new Comparable[T] { def compareTo(t: T): Int = f(t) }
    }

}
