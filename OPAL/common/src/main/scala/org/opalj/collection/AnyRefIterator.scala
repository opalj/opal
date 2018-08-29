/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package collection

import scala.reflect.ClassTag
import scala.collection.AbstractIterator
import scala.collection.GenTraversableOnce

/**
 * Iterator over a collection of any ref values.
 *
 * @note The type bound `T <: AnyRef` is expected to be ex-/implicitly enforced by subclasses.
 *
 * @author Michael Eichberg
 */
abstract class AnyRefIterator[+T] extends AbstractIterator[T] { self ⇒

    def ++[X >: T <: AnyRef](that: ⇒ AnyRefIterator[X]): AnyRefIterator[X] = new AnyRefIterator[X] {
        def hasNext: Boolean = self.hasNext || that.hasNext
        def next(): X = if (self.hasNext) self.next() else that.next()
    }

    def sum(f: T ⇒ Int): Int = {
        var sum = 0
        while (hasNext) sum += f(next())
        sum
    }

    def collect(pf: PartialFunction[T, Int]): IntIterator = {
        ???
    }

    def collect(pf: PartialFunction[T, Long]): LongIterator = {
        ???
    }

    override def drop(n: Int): this.type = {
        var i = 0
        while (i < n && hasNext) { i += 1; next() }
        this
    }

    override def filter(p: T ⇒ Boolean): AnyRefIterator[T] = new AnyRefIterator[T] {
        private[this] var hasNextValue: Boolean = true
        private[this] var v: T = _
        private[this] def goToNextValue(): Unit = {
            while (self.hasNext) {
                v = self.next()
                if (p(v)) return ;
            }
            hasNextValue = false
        }

        goToNextValue()

        def hasNext: Boolean = hasNextValue
        def next(): T = { val v = this.v; goToNextValue(); v }
    }

    override def filterNot(p: T ⇒ Boolean): AnyRefIterator[T] = filter(e ⇒ !p(e))

    override def flatMap[X](f: T ⇒ GenTraversableOnce[X]): AnyRefIterator[X] = new AnyRefIterator[X] {
        private[this] var it: Iterator[X] = null
        private[this] def nextIt(): Unit = {
            do { it = f(self.next()).toIterator } while (!it.hasNext && self.hasNext)
        }
        def hasNext: Boolean = { (it != null && it.hasNext) || { nextIt(); it.hasNext } }
        def next(): X = { if (it == null || !it.hasNext) nextIt(); it.next() }
    }

    def flatMap(f: T ⇒ IntIterator): IntIterator = new IntIterator {
        private[this] var it: IntIterator = null
        private[this] def nextIt(): Unit = {
            do { it = f(self.next()) } while (!it.hasNext && self.hasNext)
        }
        def hasNext: Boolean = (it != null && it.hasNext) || { nextIt(); it.hasNext }
        def next(): Int = { if (it == null || !it.hasNext) nextIt(); it.next() }
    }

    def flatMap(f: T ⇒ LongIterator): LongIterator = new LongIterator {
        private[this] var it: LongIterator = null
        private[this] def nextIt(): Unit = {
            do { it = f(self.next()) } while (!it.hasNext && self.hasNext)
        }
        def hasNext: Boolean = (it != null && it.hasNext) || { nextIt(); it.hasNext }
        def next(): Long = { if (it == null || !it.hasNext) nextIt(); it.next() }
    }

    def foldLeft(z: Int)(op: (Int, T) ⇒ Int): Int = {
        var v = z
        while (hasNext) v = op(v, next())
        v
    }

    def foldLeft(z: Long)(op: (Long, T) ⇒ Long): Long = {
        var v = z
        while (hasNext) v = op(v, next())
        v
    }

    override def map[X](f: T ⇒ X): AnyRefIterator[X] = new AnyRefIterator[X] {
        def hasNext: Boolean = self.hasNext
        def next(): X = f(self.next())
    }

    def map(f: T ⇒ Int): IntIterator = new IntIterator {
        def hasNext: Boolean = self.hasNext
        def next(): Int = f(self.next())
    }

    def map(f: T ⇒ Long): LongIterator = new LongIterator {
        def hasNext: Boolean = self.hasNext
        def next(): Long = f(self.next())
    }

    override def withFilter(p: T ⇒ Boolean): AnyRefIterator[T] = filter(p)

    def zip[X <: AnyRef](that: AnyRefIterator[X]): AnyRefIterator[(T, X)] = new AnyRefIterator[(T, X)] {
        def hasNext: Boolean = self.hasNext && that.hasNext
        def next: (T, X) = (self.next(), that.next())
    }

    override def zipWithIndex: AnyRefIterator[(T, Int)] = new AnyRefIterator[(T, Int)] {
        var idx = 0
        def hasNext: Boolean = self.hasNext
        def next: (T, Int) = { val ret = (self.next(), idx); idx += 1; ret }
    }

}

object AnyRefIterator {

    final val empty: AnyRefIterator[Nothing] = new AnyRefIterator[Nothing] {
        def hasNext: Boolean = false
        def next(): Nothing = throw new UnsupportedOperationException
    }

    def apply[T <: AnyRef](v: T): AnyRefIterator[T] = new AnyRefIterator[T] {
        private[this] var returned = false
        def hasNext: Boolean = !returned
        def next(): T = { returned = true; v }
    }

    def apply[T <: AnyRef](v1: T, v2: T): AnyRefIterator[T] = new AnyRefIterator[T] {
        private[this] var nextId = 0
        def hasNext: Boolean = nextId < 2
        def next(): T = { if (nextId == 0) { nextId = 1; v1 } else { nextId = 2; v2 } }
        override def toArray[X >: T: ClassTag]: Array[X] = {
            val as = new Array[X](2)
            as(0) = v1
            as(1) = v2
            as
        }
    }

    def apply[T <: AnyRef](v1: T, i2: T, i3: T): AnyRefIterator[T] = new AnyRefIterator[T] {
        private[this] var nextId: Int = 0
        def hasNext: Boolean = nextId < 3
        def next(): T = { nextId += 1; if (nextId == 1) v1 else if (nextId == 2) i2 else i3 }
        override def toArray[X >: T: ClassTag]: Array[X] = {
            val as = new Array[X](3)
            as(0) = v1
            as(1) = i2
            as(2) = i3
            as
        }
    }

    def fromNonNullValues[T <: AnyRef](data: Array[T]): AnyRefIterator[T] = new AnyRefIterator[T] {
        private[this] var i = -1
        private[this] def advanceIterator(): Unit = {
            i += 1
            val max = data.length
            while (i < max && data(i) == null) i += 1
        }
        advanceIterator()
        def hasNext: Boolean = i < data.length
        def next(): T = { val v = data(i); advanceIterator(); v }
    }

}

