/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package collection

import org.opalj.collection.immutable.IntTrieSet
import org.opalj.collection.immutable.IntTrieSet1
import org.opalj.collection.immutable.EmptyIntTrieSet
import org.opalj.collection.immutable.IntArraySet

import scala.collection.AbstractIterator

/**
 * Iterator over a collection of primitive int valuea; basically overrides all inherited methods
 * to avoid (un)boxing.
 *
 * @note   No guarantee is given what will happen if `next` is called after `hasNext` returns or
 *         would have returned false.
 *
 * @author Michael Eichberg
 */
abstract class IntIterator extends AbstractIterator[Int] { self =>

    /**
     * Returns the next value if `hasNext` has returned `true`; if hasNext has returned `false`
     * and `next` is called, the result is undefined. The method may throw any exception, e.g., a
     * `NullPointerException`, or just return the last value; however, the behavior
     * is undefined and subject to change without notice!
     */
    override def next(): Int

    override def exists(p: Int => Boolean): Boolean = {
        while (this.hasNext) { if (p(this.next())) return true; }
        false
    }

    override def forall(p: Int => Boolean): Boolean = {
        while (this.hasNext) { if (!p(this.next())) return false; }
        true
    }

    def contains(i: Int): Boolean = {
        while (this.hasNext) { if (i == this.next()) return true; }
        false
    }

    def foldLeft(start: Int)(f: (Int, Int) => Int): Int = {
        var c = start
        while (this.hasNext) { c = f(c, next()) }
        c
    }

    def foldLeft(start: Long)(f: (Long, Int) => Long): Long = {
        var c = start
        while (this.hasNext) { c = f(c, next()) }
        c
    }

    override def foldLeft[T](start: T)(f: (T, Int) => T): T = {
        var c = start
        while (this.hasNext) { c = f(c, next()) }
        c
    }

    /**
     * Executes the given function `f` until an element is found for which `p` evaluates to `false`
     * or all elements have been processed.
     */
    def foreachWhile[U](p: Int => Boolean)(f: Int => U): Unit = {
        while (this.hasNext) {
            val e = this.next()
            if (p(e)) {
                f(e)
            } else {
                return ;
            }
        }
    }

    def map(f: Int => Int): IntIterator = new IntIterator {
        def hasNext: Boolean = self.hasNext
        def next(): Int = f(self.next())
    }

    def map(f: Int => Long): LongIterator = new LongIterator {
        def hasNext: Boolean = self.hasNext
        def next(): Long = f(self.next())
    }

    override def map[T](f: Int => T): Iterator[T] = new Iterator[T] {
        def hasNext: Boolean = self.hasNext
        def next(): T = f(self.next())
    }

    override def foreach[U](f: Int => U): Unit = while (this.hasNext) f(this.next())

    def flatMap(f: Int => IntIterator): IntIterator = new IntIterator {
        private[this] var it: IntIterator = IntIterator.empty
        private[this] def advanceIterator(): Unit = {
            while (!it.hasNext) {
                if (self.hasNext) {
                    it = f(self.next())
                } else {
                    it = null
                    return ;
                }
            }
        }
        advanceIterator()
        def hasNext: Boolean = it != null
        def next(): Int = { val e = it.next(); advanceIterator(); e }
    }

    def flatMap[T](f: Int => Iterator[T]): Iterator[T] = new Iterator[T] {
        private[this] var it: Iterator[T] = Iterator.empty
        private[this] def advanceIterator(): Unit = {
            while (!it.hasNext) {
                if (self.hasNext) {
                    it = f(self.next())
                } else {
                    it = null
                    return ;
                }
            }
        }
        advanceIterator()
        def hasNext: Boolean = it != null
        def next(): T = { val e = it.next(); advanceIterator(); e }
    }

    override def withFilter(p: Int => Boolean): IntIterator = new IntIterator {
        private[this] var hasNextValue: Boolean = true
        private[this] var v: Int = 0
        private[this] def goToNextValue(): Unit = {
            while (self.hasNext) {
                v = self.next()
                if (p(v)) return ;
            }
            hasNextValue = false
        }

        goToNextValue()

        def hasNext: Boolean = hasNextValue
        def next(): Int = { val v = this.v; goToNextValue(); v }
    }

    final override def filter(p: Int => Boolean): IntIterator = withFilter(p)

    /**
     * @note This method, as well as the generic `toArray` should be overwritten when the size is
     *       known.
     */
    def toArray: Array[Int] = {
        // TODO [Scala 2.13.x] Use the IterableOnce.size method to determine the best strategy.
        var asLength = 8
        var as = new Array[Int](asLength)

        var i = 0
        while (hasNext) {
            if (i == asLength) {
                val newAS = new Array[Int](Math.min(asLength * 2, asLength + 512))
                Array.copy(as, 0, newAS, 0, asLength)
                as = newAS
                asLength = as.length
            }
            val v = next()
            as(i) = v
            i += 1
        }
        if (i == asLength)
            as
        else {
            val resultAs = new Array[Int](i)
            Array.copy(as, 0, resultAs, 0, i)
            resultAs
        }
    }

    def toSet: IntTrieSet = {
        var s: IntTrieSet = EmptyIntTrieSet
        while (hasNext) { s += next() }
        s
    }

    /**
     * @note This method should be overwritten, when the underlying collection is already sorted.
     */
    def toSortedSet: IntArraySet = {
        // TODO [Scala 2.13.x] Use the IterableOnce.size method to determine the best strategy.
        IntArraySet._UNSAFE_from(toArray)
    }

    /**
     * Copies all elements to a new array of the given size.
     *
     * @note This method should be overwritten, when the underlying collection is already
     *       any array and more efficient operation, aka System.arrayCopy can be used.
     */
    def copyToArray(size: Int): Array[Int] = {
        val as = new Array[Int](size)
        var i = 0
        while (hasNext && i < size) {
            val v = next()
            as(i) = v
            i += 1
        }
        as
    }

    override def toList: List[Int] = {
        val b = List.newBuilder[Int]
        while (hasNext) b += next()
        b.result()
    }

    /**
     * Compares the returned values to check if the iteration order is the same.
     *
     * Both iterators may be consumed up to an arbitrary point.
     */
    def sameValues(that: IntIterator): Boolean = {
        while (this.hasNext) {
            if (!that.hasNext || this.next() != that.next())
                return false;
        }
        !that.hasNext
    }
}

object IntIterator {

    final val empty: IntIterator = new IntIterator {
        override def hasNext: Boolean = false
        override def next(): Nothing = throw new NoSuchElementException("next on empty iterator")
        override def toArray: Array[Int] = Array()
        override def toSet: IntTrieSet = IntTrieSet.empty
        override def toSortedSet: IntArraySet = IntArraySet.empty
    }

    /**
     * Creates a new iterator to iterate over the values in the defined range.
     *
     * @param start The first value (inclusive).
     * @param end The last value (inclusive).
     * @return An iterator over the given values.
     */
    def upTo(start: Int, end: Int): IntIterator = new IntIterator {
        private[this] var i: Int = start
        override def hasNext: Boolean = i <= end
        override def next(): Int = {
            if (i > end) IntIterator.empty.next(); // <= will throw the expected exception
            val r = i
            i += 1
            r
        }
    }

    /**
     * Creates a new iterator to iterate over the values in the defined range.
     *
     * @param start The first value (inclusive).
     * @param end The last value (exclusive).
     * @return An iterator over the given values.
     */
    def upUntil(start: Int, end: Int): IntIterator = new IntIterator {
        private[this] var i: Int = start
        override def hasNext: Boolean = i < end
        override def next(): Int = {
            if (i >= end) IntIterator.empty.next(); // <= will throw the expected exception
            val r = i
            i += 1
            r
        }
    }

    def apply(i: Int): IntIterator = new IntIterator {
        private[this] var returned = false
        def hasNext: Boolean = !returned
        def next(): Int = { returned = true; i }
        override def toArray: Array[Int] = Array(i)
        override def toSet: IntTrieSet = IntTrieSet1(i)
    }

    def apply(i1: Int, i2: Int): IntIterator = new IntIterator {
        private[this] var nextId = 0
        def hasNext: Boolean = nextId < 2
        def next(): Int = { if (nextId == 0) { nextId = 1; i1 } else { nextId = 2; i2 } }
        override def toArray: Array[Int] = Array(i1, i2)
        override def toSet: IntTrieSet = IntTrieSet(i1, i2)
    }

    def apply(i1: Int, i2: Int, i3: Int): IntIterator = new IntIterator {
        private[this] var nextId: Int = 0
        def hasNext: Boolean = nextId < 3
        def next(): Int = { nextId += 1; if (nextId == 1) i1 else if (nextId == 2) i2 else i3 }
        override def toArray: Array[Int] = Array(i1, i2, i3)
        override def toSet: IntTrieSet = IntTrieSet(i1, i2, i3)
    }

}
