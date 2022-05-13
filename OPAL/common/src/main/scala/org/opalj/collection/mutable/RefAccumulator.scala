/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.collection.mutable

/**
 * A list based accumulator of values '''and''' collections of values where the collections
 * of values are not copied to the accumulator but only an iterator of those values. '''Hence,
 * the collections that are added to this accumulator must not be mutated after being added.'''
 *
 * @note This is not a general purpose data-structure due to the requirements on the immutability
 *       of added collections.
 *
 * @tparam A A type which is NOT a subtype of `Iterator[_]`.
 *
 * @author Michael Eichberg
 */
final class RefAccumulator[A <: AnyRef] private (
        private var data: List[AnyRef] // either a value of type A or a non-empty iterator of A
) {

    def isEmpty: Boolean = data.isEmpty
    def nonEmpty: Boolean = data.nonEmpty

    def +=(i: A): Unit = {
        data ::= i
    }

    def ++=(is: IterableOnce[A]): Unit = {
        is match {
            case it: Iterator[A] =>
                if (it.hasNext) data ::= it
            case is /*not a traversable once...*/ =>
                if (is.iterator.nonEmpty) data ::= is.iterator
        }
    }

    /**
     * Returns and removes the next value.
     */
    def pop(): A = {
        data.head match {
            case it: Iterator[A @unchecked] =>
                val v = it.next()
                if (!it.hasNext) data = data.tail
                v
            case v: A @unchecked =>
                data = data.tail
                v
            case _ => throw new Exception("Unrecognized type")
        }
    }

}

/**
 * Factory to create [[RefAccumulator]]s.
 */
object RefAccumulator {

    def empty[N >: Null <: AnyRef]: RefAccumulator[N] = new RefAccumulator[N](Nil)

    def apply[N >: Null <: AnyRef](e: N): RefAccumulator[N] = new RefAccumulator[N](List(e))
}

