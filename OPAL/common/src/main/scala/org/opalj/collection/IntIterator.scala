/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package collection

import java.util.function.IntPredicate
import java.util.function.IntConsumer

import scala.collection.AbstractIterator
import org.opalj.collection.immutable.Chain
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.collection.immutable.IntTrieSet1
import org.opalj.collection.immutable.EmptyIntTrieSet

/**
 * Iterator over a collection of ints; guaranteed to avoid (un)boxing.
 *
 * Compared to a standard Java/Scala iterator, users of IntIterator cannot rely
 * on an exception if the iterator has reached its end.
 *
 * @author Michael Eichberg
 */
trait IntIterator { self ⇒

    /**
     * Returns the next value if `hasNext` has returned `true`; if hasNext has returned `false`
     * and `next` is called, the result is undefined. The method may throw an
     * `UnsupportedOperationException` or just return the last value; however, the behavior
     * is undefined and subject to change without notice!
     */
    def next(): Int
    def hasNext: Boolean

    def nonEmpty: Boolean = hasNext

    def isEmpty: Boolean = !hasNext

    def exists(p: Int ⇒ Boolean): Boolean = {
        while (this.hasNext) { if (p(this.next)) return true; }
        false
    }
    def forall(p: Int ⇒ Boolean): Boolean = {
        while (this.hasNext) { if (!p(this.next)) return false; }
        true
    }
    def contains(i: Int): Boolean = {
        while (this.hasNext) { if (i == this.next) return true; }
        false
    }
    def foldLeft[B](start: B)(f: (B, Int) ⇒ B): B = {
        var c = start
        while (this.hasNext) { c = f(c, next()) }
        c
    }

    def foreachWhile(p: IntPredicate)(f: IntConsumer): Unit = {
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

    def map(f: Int ⇒ Int): IntIterator = {
        new IntIterator {
            def hasNext: Boolean = self.hasNext
            def next(): Int = f(self.next)
        }
    }

    def mapToAny[A](m: Int ⇒ A): Iterator[A] = {
        new AbstractIterator[A] {
            def hasNext: Boolean = self.hasNext
            def next: A = m(self.next())
        }
    }

    def foreach[U](f: Int ⇒ U): Unit = while (hasNext) f(next)

    def flatMap(f: Int ⇒ IntIterator): IntIterator = {
        new IntIterator {
            private[this] var it: IntIterator = null
            private[this] def nextIt(): Unit = {
                do {
                    it = f(self.next)
                } while (!it.hasNext && self.hasNext)
            }

            def hasNext: Boolean = (it != null && it.hasNext) || { nextIt(); it.hasNext }
            def next(): Int = { if (it == null || !it.hasNext) nextIt(); it.next }
        }
    }

    def withFilter(p: Int ⇒ Boolean): IntIterator = {
        new IntIterator {
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
    }

    def filter(p: Int ⇒ Boolean): IntIterator = withFilter(p)

    def toArray: Array[Int] = {
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
     * Copies all elements to a target array of the given size. The size has to be
     * at least the size of elements of this iterator.
     */
    private[opalj] def toArray(size: Int): Array[Int] = {
        val as = new Array[Int](size)
        var i = 0
        while (hasNext) {
            val v = next()
            as(i) = v
            i += 1
        }
        as
    }

    def toChain: Chain[Int] = {
        val b = Chain.newBuilder[Int]
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
    def iterator: Iterator[Int] = {
        new AbstractIterator[Int] {
            def hasNext: Boolean = self.hasNext
            def next: Int = self.next()
        }
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
        def hasNext: Boolean = false
        def next(): Nothing = throw new UnsupportedOperationException
        override def toArray: Array[Int] = new Array[Int](0)
        override def toSet: IntTrieSet = EmptyIntTrieSet
    }

    def apply(i: Int): IntIterator = new IntIterator {
        private[this] var returned = false
        def hasNext: Boolean = !returned
        def next(): Int = { returned = true; i }
        override def toArray: Array[Int] = Array(i)
        override def toSet: IntTrieSet = IntTrieSet1(i)
    }

    def apply(i1: Int, i2: Int): IntIterator = new IntIterator {
        private[this] var next = 0
        def hasNext: Boolean = next < 2
        def next(): Int = { if (next == 0) { next = 1; i1 } else { next = 2; i2 } }
        override def toArray: Array[Int] = Array(i1, i2)
        override def toSet: IntTrieSet = IntTrieSet(i1, i2)
    }

    def apply(i1: Int, i2: Int, i3: Int): IntIterator = new IntIterator {
        private[this] var next: Int = 0
        def hasNext: Boolean = next < 3
        def next(): Int = { next += 1; if (next == 1) i1 else if (next == 2) i2 else i3 }
        override def toArray: Array[Int] = Array(i1, i2, i3)
        override def toSet: IntTrieSet = IntTrieSet(i1, i2, i3)
    }

}
