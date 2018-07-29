/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package collection

import java.util.function.IntConsumer
import java.util.function.IntFunction

import scala.collection.mutable.Builder
import org.opalj.collection.immutable.Chain

/**
 * A set of integer values.
 *
 * @author Michael Eichberg
 */
trait IntSet[T <: IntSet[T]] { intSet: T ⇒

    def isEmpty: Boolean
    def nonEmpty: Boolean = !isEmpty
    /** Tests if this set has exactly one element (complexity: O(1)). */
    def isSingletonSet: Boolean
    /** Tests if this set has more than one element (complexity: O(1)). */
    def hasMultipleElements: Boolean

    /**
     * The size of the set; may not be a constant operation; if possible use isEmpty, nonEmpty,
     * etc.; or lookup the complexity in the concrete data structures.
     */
    def size: Int

    def foreach(f: IntConsumer): Unit
    def withFilter(p: (Int) ⇒ Boolean): T
    def map(f: Int ⇒ Int): T
    /**
     * Uses the keys of this set to map them to the value found in
     * the given array at the respective index.
     */
    def map(map: Array[Int]): T
    def mapToAny[A](f: IntFunction[A]): Set[A] = {
        var r = Set.empty[A]
        foreach { v ⇒ r += f(v) }
        r
    }
    def flatMap(f: Int ⇒ T): T

    def foldLeft[B](z: B)(f: (B, Int) ⇒ B): B

    def head: Int
    def contains(value: Int): Boolean
    def exists(p: Int ⇒ Boolean): Boolean
    def forall(f: Int ⇒ Boolean): Boolean

    def -(i: Int): T
    def +(i: Int): T

    final def --(is: TraversableOnce[Int]): T = {
        var r = this
        is.foreach { i ⇒ r -= i }
        r
    }
    final def --(is: IntSet[_]): T = {
        var r = this
        is.foreach { i ⇒ r -= i }
        r
    }

    final def ++(that: T): T = {
        if (this.size > that.size)
            that.foldLeft(this)(_ + _) // we expand `this` since `this` is larger
        else
            this.foldLeft(that)(_ + _) // we expand `that`
    }

    final def ++(that: TraversableOnce[Int]): T = that.foldLeft(this)(_ + _)

    final def ++(that: IntIterator): T = that.foldLeft(this)(_ + _)

    def iterator: Iterator[Int]
    def intIterator: IntIterator
    def toChain: Chain[Int]
    final def transform[B, To](f: Int ⇒ B, b: Builder[B, To]): To = {
        foreach(i ⇒ b += f(i))
        b.result()
    }

    final def mkString(pre: String, in: String, post: String): String = {
        val sb = new StringBuilder(pre)
        val it = intIterator
        var hasNext = it.hasNext
        while (hasNext) {
            sb.append(it.next.toString)
            hasNext = it.hasNext
            if (hasNext) sb.append(in)
        }
        sb.append(post)
        sb.toString()
    }

    final def mkString(in: String): String = mkString("", in, "")

}
