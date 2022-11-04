/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.collection

import java.util.function.LongPredicate
import java.util.function.LongConsumer

import scala.collection.AbstractIterator
import org.opalj.collection.immutable.LongTrieSet

/**
 * Iterator over a collection of longs; basically all methods are overridden to avoid
 * (un)boxing operations.
 *
 * @author Michael Eichberg
 */
abstract class LongIterator extends AbstractIterator[Long] { self =>

    def ++(other: LongIterator): LongIterator = {
        new LongIterator {
            private[this] var it = self
            override def hasNext = it != null
            override def next(): Long = {
                val v = it.next()
                if (!it.hasNext) {
                    it = if (it eq self) other else null
                }
                v
            }
        }
    }

    /**
     * Returns the next value if `hasNext` has returned `true`; if hasNext has returned `false`
     * and `next` is called, the result is undefined. The method may throw an
     * `UnsupportedOperationException` or just return the last value; however, the behavior
     * is undefined and subject to change without notice!
     */
    def next(): Long

    override def exists(p: Long => Boolean): Boolean = {
        while (this.hasNext) { if (p(this.next())) return true; }
        false
    }
    override def forall(p: Long => Boolean): Boolean = {
        while (this.hasNext) { if (!p(this.next())) return false; }
        true
    }
    def contains(i: Long): Boolean = {
        while (this.hasNext) { if (i == this.next()) return true; }
        false
    }
    override def foldLeft[B](start: B)(f: (B, Long) => B): B = {
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
                false
            }
        }) { /*empty*/ }
    }

    def map(f: Long => Long): LongIterator = {
        new LongIterator {
            def hasNext: Boolean = self.hasNext
            def next(): Long = f(self.next())
        }
    }

    override def take(n: Int): LongIterator = {
        new LongIterator {
            private[this] var i: Int = 0
            def hasNext: Boolean = self.hasNext && i < n
            def next(): Long = { i += 1; self.next() }
        }
    }

    def map(f: Long => Int): IntIterator = new IntIterator {
        def hasNext: Boolean = self.hasNext
        def next(): Int = f(self.next())
    }
    override def map[X](m: Long => X): Iterator[X] = new Iterator[X] {
        def hasNext: Boolean = self.hasNext
        def next(): X = m(self.next())
    }

    override def foreach[U](f: Long => U): Unit = while (hasNext) f(next())

    def flatMap(f: Long => LongIterator): LongIterator = {
        new LongIterator {
            private[this] var it: LongIterator = LongIterator.empty
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
            def next(): Long = { val e = it.next(); advanceIterator(); e }
        }
    }

    override def withFilter(p: Long => Boolean): LongIterator = filter(p)

    override def filter(p: Long => Boolean): LongIterator = new LongIterator {
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

    def toSet: LongSet = {
        var s = LongTrieSet.empty
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
        override def toSet: LongTrieSet = LongTrieSet.empty
    }

    def apply(i: Long): LongIterator = new LongIterator {
        private[this] var returned = false
        def hasNext: Boolean = !returned
        def next(): Long = { returned = true; i }
        override def toArray: Array[Long] = { val as = new Array[Long](1); as(0) = i; as }
        override def toSet: LongTrieSet = LongTrieSet(i)
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

    def apply(i1: Long, i2: Long, i3: Long, i4: Long): LongIterator = new LongIterator {
        private[this] var nextId: Int = 0
        def hasNext: Boolean = nextId < 4
        def next(): Long = {
            nextId += 1
            nextId match {
                case 1 => i1
                case 2 => i2
                case 3 => i3
                case _ => i4
            }
        }
        override def toArray: Array[Long] = {
            val as = new Array[Long](3)
            as(0) = i1
            as(1) = i2
            as(2) = i3
            as(3) = i4
            as
        }
        override def toSet: LongTrieSet = LongTrieSet(i1, i2, i3, i4)
    }

}
