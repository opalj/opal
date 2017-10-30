/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.opalj
package collection
package mutable

import scala.collection.AbstractIterator

/**
 * A bit set with a given upper bound for the largest value that can be stored in the set.
 * The upper bound is only used to create an optimal underlying representation. It has no
 * impact on equals and/or hashcode computations. I.e., two sets with two different upper bounds
 * which contain the same values, are equal and have the same hashcode.
 *
 * Conceptually, the an array of long values is used to store the values.
 *
 * If values are added to the set that are larger than the specified size the behavior is
 * undefined!
 *
 * @author Michael Eichberg
 */
sealed abstract class FixedSizeBitSet {

    def +=(i: Int): this.type

    def -=(i: Int): this.type

    def contains(i: Int): Boolean

    def intIterator: IntIterator

    def iterator: Iterator[Int] = new AbstractIterator[Int] {
        private[this] val it = intIterator
        def hasNext: Boolean = it.hasNext
        def next(): Int = it.next()
    }

    def mkString(pre: String, in: String, post: String): String = {
        intIterator.mkString(pre, in, post)
    }

    override def toString: String = mkString("FixedSizeBitSet(", ", ", ")")
}

private[mutable] object ZeroLengthBitSet extends FixedSizeBitSet {
    override def +=(i: Int): this.type = throw new UnsupportedOperationException("fixed size is 0")
    override def -=(i: Int): this.type = this
    override def contains(i: Int): Boolean = false
    override def intIterator: IntIterator = IntIterator.empty
    override def equals(other: Any): Boolean = {
        other match {
            case ZeroLengthBitSet         ⇒ true
            case that: FixedSizeBitSet64  ⇒ that.set == 0
            case that: FixedSizeBitSet128 ⇒ that.set1 == 0 && that.set2 == 0
            case that: FixedSizeBitSetN   ⇒ that.equals(this)
            case _                        ⇒ false
        }
    }
    override def hashCode: Int = 1 // same as Arrays.hashCode(empty long array)
}

private[mutable] final class FixedSizeBitSet64 extends FixedSizeBitSet { thisSet ⇒

    private[mutable] var set: Long = 0L

    override def +=(i: Int): this.type = { set |= 1L << i; this }
    override def -=(i: Int): this.type = { set &= (-1L & ~(1L << i)); this }
    override def contains(i: Int): Boolean = (set & (1L << i)) != 0L

    override def intIterator: IntIterator = new IntIterator {
        private[this] var i: Int = -1
        private[this] def getNextValue(): Unit = {
            do { i += 1 } while (i < 64 && !thisSet.contains(i))
        }
        getNextValue()
        def hasNext: Boolean = i < 64
        def next(): Int = { val i = this.i; getNextValue(); i }
    }

    override def equals(other: Any): Boolean = {
        other match {
            case that: FixedSizeBitSet64  ⇒ this.set == that.set
            case ZeroLengthBitSet         ⇒ this.set == 0L
            case that: FixedSizeBitSet128 ⇒ that.set2 == 0L && that.set1 == this.set
            case that: FixedSizeBitSetN   ⇒ that.equals(this)
            case _                        ⇒ false
        }
    }

    override def hashCode: Int = 31 * (set ^ (set >>> 32)).toInt
}

private[mutable] final class FixedSizeBitSet128 extends FixedSizeBitSet { thisSet ⇒

    private[mutable] var set1: Long = 0L
    private[mutable] var set2: Long = 0L

    override def +=(i: Int): this.type = {
        if (i <= 63)
            set1 |= 1L << i
        else {
            set2 |= 1L << (i - 64)
        }
        this
    }
    override def -=(i: Int): this.type = {
        if (i <= 63)
            set1 &= (-1L & ~(1L << i))
        else
            set2 &= (-1L & ~(1L << (i - 64)))
        this
    }
    override def contains(i: Int): Boolean = {
        if (i <= 63)
            (set1 & (1L << i)) != 0L
        else
            (set2 & (1L << (i - 64))) != 0L
    }

    override def intIterator: IntIterator = new IntIterator {
        private[this] var i: Int = -1
        private[this] def getNextValue(): Unit = {
            do { i += 1 } while (i < 128 && !thisSet.contains(i))
        }
        getNextValue()
        def hasNext: Boolean = i < 128
        def next(): Int = { val i = this.i; getNextValue(); i }
    }

    override def equals(other: Any): Boolean = {
        other match {
            case that: FixedSizeBitSet128 ⇒ this.set1 == that.set1 && this.set2 == that.set2
            case ZeroLengthBitSet         ⇒ this.set1 == 0L && this.set2 == 0L
            case that: FixedSizeBitSet64  ⇒ this.set2 == 0L && this.set1 == that.set
            case that: FixedSizeBitSetN   ⇒ that.equals(this)
            case _                        ⇒ false
        }
    }

    override def hashCode: Int = java.lang.Long.hashCode(set1 ^ set2)
}

private[mutable] final class FixedSizeBitSetN private[mutable] (
        private val set: Array[Long]
) extends FixedSizeBitSet { thisSet ⇒

    assert(set.length > 2)

    override def +=(i: Int): this.type = {
        val bucket = i / 64
        set(bucket) = set(bucket) | (1L << (i - 64 * bucket))
        this
    }
    override def -=(i: Int): this.type = {
        val bucket = i / 64
        set(bucket) = set(bucket) & ((-1L & ~(1L << (i - 64 * bucket))))
        this
    }
    override def contains(i: Int): Boolean = {
        val bucket = i / 64
        (set(bucket) & (1L << (i - 64 * bucket))) != 0L
    }

    override def intIterator: IntIterator = new IntIterator {
        private[this] val max: Int = set.length * 64
        private[this] var i: Int = -1
        private[this] def getNextValue(): Unit = {
            do { i += 1 } while (i < max && !thisSet.contains(i))
        }
        getNextValue()
        def hasNext: Boolean = i < max
        def next(): Int = { val i = this.i; getNextValue(); i }
    }

    override def equals(other: Any): Boolean = {
        other match {
            case that: FixedSizeBitSetN if this.set.length == that.set.length ⇒
                java.util.Arrays.equals(this.set, that.set)

            case ZeroLengthBitSet ⇒
                var i = 0
                val length = set.length
                while (i < length) { if (set(i) != 0) return false; i += 1 }
                true

            case that: FixedSizeBitSet64 ⇒
                var i = 1
                val length = set.length
                while (i < length) { if (set(i) != 0) return false; i += 1 }
                set(0) == that.set

            case that: FixedSizeBitSet128 ⇒
                var i = 2
                val length = set.length
                while (i < length) { if (set(i) != 0) return false; i += 1 }
                set(0) == that.set1 && set(1) == that.set2

            case _ ⇒
                super.equals(other)
        }
    }

    override def hashCode: Int = java.util.Arrays.hashCode(set)
}

/** Factory to create fixed size bit arrays. */
object FixedSizeBitSet {

    final val empty: FixedSizeBitSet = ZeroLengthBitSet

    /** @param max The maximum value you may want to store in the set. */
    def create(max: Int): FixedSizeBitSet = {
        if (max == 0) empty
        else if (max < 64) new FixedSizeBitSet64
        else if (max < 128) new FixedSizeBitSet128
        else new FixedSizeBitSetN(new Array[Long]((max / 64) + 1))
    }

}
