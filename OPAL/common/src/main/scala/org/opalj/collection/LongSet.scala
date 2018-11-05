/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.collection

import scala.collection.mutable.Builder

/**
 * A set of long values.
 *
 * @author Michael Eichberg
 */
trait LongSet[T <: LongSet[T]] { longSet: T ⇒

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

    def foreach[U](f: Long ⇒ U): Unit
    def withFilter(p: Long ⇒ Boolean): T
    def map(f: Long ⇒ Long): T
    def map[A <: AnyRef](f: Long ⇒ A): Set[A] = foldLeft(Set.empty[A])(_ + f(_))
    def flatMap(f: Long ⇒ T): T
    def foldLeft[B](z: B)(f: (B, Long) ⇒ B): B

    def head: Long
    def contains(value: Long): Boolean
    def exists(p: Long ⇒ Boolean): Boolean
    def forall(f: Long ⇒ Boolean): Boolean

    def -(i: Long): T
    def +(i: Long): T

    final def --(is: TraversableOnce[Long]): T = {
        var r = this
        is.foreach { i ⇒ r -= i }
        r
    }
    final def --(is: LongSet[_]): T = {
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

    final def ++(that: TraversableOnce[Long]): T = that.foldLeft(this)(_ + _)

    final def ++(that: LongIterator): T = that.foldLeft(this)(_ + _)

    def iterator: LongIterator

    final def transform[B, To](f: Long ⇒ B, b: Builder[B, To]): To = {
        foreach(i ⇒ b += f(i))
        b.result()
    }

    final def mkString(pre: String, in: String, post: String): String = {
        val sb = new StringBuilder(pre)
        val it = iterator
        var hasNext = it.hasNext
        while (hasNext) {
            sb.append(it.next().toString)
            hasNext = it.hasNext
            if (hasNext) sb.append(in)
        }
        sb.append(post)
        sb.toString()
    }

    final def mkString(in: String): String = mkString("", in, "")

}
