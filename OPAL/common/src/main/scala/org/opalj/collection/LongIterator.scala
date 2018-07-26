/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.collection

import java.util.function.LongPredicate
import java.util.function.LongConsumer

import scala.collection.AbstractIterator
import org.opalj.collection.immutable.Chain
import org.opalj.collection.immutable.LongTrieSet
import org.opalj.collection.immutable.LongTrieSet1
import org.opalj.collection.immutable.EmptyLongTrieSet

/**
 * Iterator over a collection of longs; guaranteed to avoid (un)boxing.
 *
 * Compared to a standard Java/Scala iterator, users of LongIterator cannot rely
 * on an exception if the iterator has reached its end.
 *
 * @author Michael Eichberg
 */
trait LongIterator { self ⇒

    /**
     * Returns the next value if `hasNext` has returned `true`; if hasNext has returned `false`
     * and `next` is called, the result is undefined. The method may throw an
     * `UnsupportedOperationException` or just return the last value; however, the behavior
     * is undefined and subject to change without notice!
     */
    def next(): Long
    def hasNext: Boolean

    def nonEmpty: Boolean = hasNext

    def isEmpty: Boolean = !hasNext

    def exists(p: Long ⇒ Boolean): Boolean = {
        while (this.hasNext) { if (p(this.next)) return true; }
        false
    }
    def forall(p: Long ⇒ Boolean): Boolean = {
        while (this.hasNext) { if (!p(this.next)) return false; }
        true
    }
    def contains(i: Long): Boolean = {
        while (this.hasNext) { if (i == this.next) return true; }
        false
    }
    def foldLeft[B](start: B)(f: (B, Long) ⇒ B): B = {
        var c = start
        while (this.hasNext) { c = f(c, next()) }
        c
    }

    def foreachWhile(p: LongPredicate)(f: LongConsumer): Unit = {
        while (this.hasNext && {
            val c = this.next()
            if (p.test(c)) {
                f.accept(c)
                true
            } else {
                false;
            }
        }) {}
    }

    def map(f: Long ⇒ Long): LongIterator = {
        new LongIterator {
            def hasNext: Boolean = self.hasNext
            def next(): Long = f(self.next)
        }
    }

    def mapToAny[A](m: Long ⇒ A): Iterator[A] = {
        new AbstractIterator[A] {
            def hasNext: Boolean = self.hasNext
            def next: A = m(self.next())
        }
    }

    def foreach[U](f: Long ⇒ U): Unit = while (hasNext) f(next)

    def flatMap(f: Long ⇒ LongIterator): LongIterator = {
        new LongIterator {
            private[this] var it: LongIterator = null
            private[this] def nextIt(): Unit = {
                do {
                    it = f(self.next)
                } while (!it.hasNext && self.hasNext)
            }

            def hasNext: Boolean = (it != null && it.hasNext) || { nextIt(); it.hasNext }
            def next(): Long = { if (it == null || !it.hasNext) nextIt(); it.next }
        }
    }

    def withFilter(p: Long ⇒ Boolean): LongIterator = {
        new LongIterator {
            private[this] var hasNextValue: Boolean = true
            private[this] var v: Long = 0
            private[this] def goToNextValue(): Unit = {
                while (self.hasNext) {
                    v = self.next()
                    if (p(v)) return ;
                }
                hasNextValue = false
            }

            goToNextValue()

            def hasNext: Boolean = hasNextValue
            def next(): Long = { val v = this.v; goToNextValue(); v }
        }
    }

    def filter(p: Long ⇒ Boolean): LongIterator = withFilter(p)

    def toArray: Array[Long] = {
        var asLength = 8
        var as = new Array[Long](asLength)

        var i = 0
        while (hasNext) {
            if (i == asLength) {
                val newAS = new Array[Long](Math.min(asLength * 2, asLength + 512))
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
            val resultAs = new Array[Long](i)
            Array.copy(as, 0, resultAs, 0, i)
            resultAs
        }
    }

    def toSet: LongTrieSet = {
        var s: LongTrieSet = EmptyLongTrieSet
        while (hasNext) { s += next() }
        s
    }

    /**
     * Copies all elements to a target array of the given size. The size has to be
     * at least the size of elements of this iterator.
     */
    private[opalj] def toArray(size: Int): Array[Long] = {
        val as = new Array[Long](size)
        var i = 0
        while (hasNext) {
            val v = next()
            as(i) = v
            i += 1
        }
        as
    }

    def toChain: Chain[Long] = {
        val b = Chain.newBuilder[Long]
        while (hasNext) b += next()
        b.result()
    }

    def mkString(pre: String, in: String, post: String): String = {
        val sb = new StringBuilder(pre)
        var hasNext = this.hasNext
        while (hasNext) {
            sb.append(this.next().toString)
            hasNext = this.hasNext
            if (hasNext) sb.append(in)
        }
        sb.append(post)
        sb.toString()
    }

    /**
     * Converts this iterator to a Scala Iterator (which potentially will (un)box
     * the returned values.)
     */
    def iterator: Iterator[Long] = {
        new AbstractIterator[Long] {
            def hasNext: Boolean = self.hasNext
            def next: Long = self.next()
        }
    }

    /**
     * Compares the returned values to check if the iteration order is the same.
     *
     * Both iterators may be consumed up to an arbitrary point.
     */
    def sameValues(that: LongIterator): Boolean = {
        while (this.hasNext) {
            if (!that.hasNext || this.next() != that.next())
                return false;
        }
        !that.hasNext
    }

}

object LongIterator {

    final val empty: LongIterator = new LongIterator {
        def hasNext: Boolean = false
        def next(): Nothing = throw new UnsupportedOperationException
        override def toArray: Array[Long] = new Array[Long](0)
        override def toSet: LongTrieSet = EmptyLongTrieSet
    }

    def apply(i: Long): LongIterator = new LongIterator {
        private[this] var returned = false
        def hasNext: Boolean = !returned
        def next(): Long = { returned = true; i }
        override def toArray: Array[Long] = { val as = new Array[Long](1); as(0) = i; as }
        override def toSet: LongTrieSet = LongTrieSet1(i)
    }

    def apply(i1: Long, i2: Long): LongIterator = new LongIterator {
        private[this] var nextId: Int = 0
        def hasNext: Boolean = nextId < 2
        def next(): Long = { if (nextId == 0) { nextId = 1; i1 } else { nextId = 2; i2 } }
        override def toArray: Array[Long] = {
            val as = new Array[Long](2)
            as(0) = i1
            as(1) = i2
            as
        }
        override def toSet: LongTrieSet = LongTrieSet(i1, i2)
    }

    def apply(i1: Long, i2: Long, i3: Long): LongIterator = new LongIterator {
        private[this] var nextId: Int = 0
        def hasNext: Boolean = nextId < 3
        def next(): Long = { nextId += 1; if (nextId == 1) i1 else if (nextId == 2) i2 else i3 }
        override def toArray: Array[Long] = {
            val as = new Array[Long](3)
            as(0) = i1
            as(1) = i2
            as(2) = i3
            as
        }
        override def toSet: LongTrieSet = LongTrieSet(i1, i2, i3)
    }

}
