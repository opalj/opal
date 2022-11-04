/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.collection

/**
 * Defines a view on some indexed data structure.
 *
 * @note The type bound `T <: AnyRef` has to be ex-/implicitly enforced by subclasses.
 *
 * @author Michael Eichberg
 */
trait RefIndexedView[+T] {
    def isEmpty: Boolean
    def size: Int
    def apply(index: Int): T
    def iterator: Iterator[T]
}

object RefIndexedView {

    final val Empty = new RefIndexedView[Nothing] {
        override def isEmpty: Boolean = true
        override def size: Int = 0
        override def apply(index: Int): Nothing = throw new IndexOutOfBoundsException("empty view")
        override def iterator: Iterator[Nothing] = Iterator.empty
    }

    def empty[T <: AnyRef]: RefIndexedView[T] = Empty
}
