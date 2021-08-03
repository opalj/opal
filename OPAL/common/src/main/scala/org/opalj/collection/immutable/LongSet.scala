/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.collection

import scala.collection.mutable.Builder

/**
 * A set of long values.
 *
 * @author Michael Eichberg
 */
trait LongSet extends AnyRef {

    type ThisSet <: LongSet

    def isEmpty: Boolean

    def nonEmpty: Boolean = !isEmpty

    /**
     * Tests if this set has exactly one element (complexity: O(1)).
     */
    def isSingletonSet: Boolean

    /**
     * The size of the set (complexity: O(1)).
     */
    def size: Int

    def contains(value: Long): Boolean

    def foreach[U](f: Long => U): Unit

    def forall(p: Long => Boolean): Boolean

    def iterator: LongIterator

    def foldLeft[B](z: B)(op: (B, Long) => B): B

    final def transform[B, To](f: Long => B, b: Builder[B, To]): To = {
        b.sizeHint(size)
        foreach(i => b += f(i))
        b.result()
    }

    /**
     * Adds the given value to this set; returns `this` if this set already contains the value
     * otherwise a new set is returned.
     */
    def +(i: Long): ThisSet

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

/**
 * Defines convenience functions and data-structures used by OPAL's data-structures.
 */
object LongSet {

    @inline def bitMask(length: Int): Long = (1L << length) - 1

    final val BitMasks: Array[Long] = {
        val bitMasks = new Array[Long](64)
        var i = 1
        while (i < 64) {
            bitMasks(i) = (1L << i) - 1L
            i += 1
        }
        bitMasks
    }

}
