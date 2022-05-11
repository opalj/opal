/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package collection

/**
 * Specialized variant of an internal iterator. The only way to iterate over a foreach
 * iterator is to use `foreach`. Compared to a classical iterator, iteration can be repeated
 * and more efficient implementation strategies are easily possible.
 *
 * @note The type bound `T <: AnyRef` is expected to be ex-/implicitly enforced by subclasses.
 *
 * @author Michael Eichberg
 */
abstract class ForeachRefIterator[+T] { self =>

    def foreach[U](f: T => U): Unit

    final def filter(p: T => Boolean): ForeachRefIterator[T] = {
        new ForeachRefIterator[T] {
            def foreach[U](f: T => U): Unit = {
                self.foreach { e =>
                    if (p(e)) f(e)
                }
            }
        }
    }

    final def +[X >: T <: AnyRef](that: X): ForeachRefIterator[X] = {
        new ForeachRefIterator[X] {
            def foreach[U](f: X => U): Unit = {
                self.foreach(f)
                f(that)
            }
        }
    }

    final def ++[X >: T <: AnyRef](that: ForeachRefIterator[X]): ForeachRefIterator[X] = {
        new ForeachRefIterator[X] {
            def foreach[U](f: X => U): Unit = {
                self.foreach(f)
                that.foreach(f)
            }
        }
    }

    final def map[X <: AnyRef](m: T => X): ForeachRefIterator[X] = {
        new ForeachRefIterator[X] {
            def foreach[U](f: X => U): Unit = {
                self.foreach(e => f(m(e)))
            }
        }
    }

    final def withFilter(p: T => Boolean): ForeachRefIterator[T] = filter(p)

    final def zipWithIndex: ForeachRefIterator[(T, Int)] = {
        new ForeachRefIterator[(T, Int)] {
            def foreach[U](f: ((T, Int)) => U): Unit = {
                var i = 0
                self.foreach { e =>
                    f((e, i))
                    i += 1
                }
            }
        }
    }

    final def foreachWithIndex[U](f: (T, Int) => U): Unit = {
        var i = 0
        self.foreach { e =>
            f(e, i)
            i += 1
        }
    }
}

object ForeachRefIterator {

    final val empty: ForeachRefIterator[Nothing] = new ForeachRefIterator[Nothing] {
        def foreach[U](f: Nothing => U): Unit = {}
    }

    // Defined to match the interface of scala....Iterator.single
    def single[T <: AnyRef](v: T): ForeachRefIterator[T] = this(v)

    def apply[T <: AnyRef](v: T): ForeachRefIterator[T] = new ForeachRefIterator[T] {
        def foreach[U](f: T => U): Unit = { f(v) }
    }

    def fromNonNullValues[T <: AnyRef](data: Array[_ <: T]): ForeachRefIterator[T] = {
        new ForeachRefIterator[T] {
            def foreach[U](f: T => U): Unit = {
                var i = 0
                val max = data.length
                while (i < max) { val d = data(i); if (d != null) f(d); i += 1 }
            }
        }
    }
}

