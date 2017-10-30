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
package immutable

import scala.collection.AbstractIterator

/**
 * An immutable bit set which uses an array to store the underlying values.
 *
 * @author Michael Eichberg
 */
sealed abstract class BitArraySet { thisSet ⇒

    def +(i: Int): BitArraySet

    def -(i: Int): BitArraySet

    def |(that: BitArraySet): BitArraySet

    def contains(i: Int): Boolean

    def intIterator: IntIterator

    final def iterator: Iterator[Int] = new AbstractIterator[Int] {
        private[this] val it = thisSet.intIterator
        def hasNext: Boolean = it.hasNext
        def next(): Int = it.next()
    }

    final def mkString(pre: String, in: String, post: String): String = {
        intIterator.mkString(pre, in, post)
    }

    override def toString: String = mkString("BitArraySet(", ", ", ")")
}

private[immutable] final object EmptyBitArraySet extends BitArraySet { thisSet ⇒

    override def +(i: Int): BitArraySet = BitArraySet(i)

    override def -(i: Int): BitArraySet = this

    override def |(that: BitArraySet): BitArraySet = that

    override def contains(i: Int): Boolean = false

    override def intIterator: IntIterator = IntIterator.empty
}

private[immutable] final class BitArraySet64(val set: Long) extends BitArraySet { thisSet ⇒

    assert(set != 0L)

    override def +(i: Int): BitArraySet = {
        if (i < 64) {
            val set = this.set
            val newSet = set | (1L << i)
            if (newSet != set) new BitArraySet64(newSet) else this
        } else {
            val newSet = new BitArraySetN(new Array[Long]((i / 64) + 1))
            newSet.set(0) = set
            newSet + i
        }
    }

    override def -(i: Int): BitArraySet = {
        if (i < 64) {
            val set = this.set
            val newSet = set & (-1L & ~(1L << i))
            if (newSet == set)
                this
            else if (newSet == 0L)
                EmptyBitArraySet
            else
                new BitArraySet64(newSet)
        } else {
            this
        }
    }

    override def |(that: BitArraySet): BitArraySet = {
        that match {
            case EmptyBitArraySet ⇒ this
            case that: BitArraySet64 ⇒
                val thisSet = this.set
                val thatSet = that.set
                val newSet = thisSet | thatSet
                if (newSet == thisSet) this
                else if (newSet == thatSet) that
                else new BitArraySet64(newSet)
            case that: BitArraySetN ⇒ that | this
        }
    }

    override def contains(i: Int): Boolean = {
        if (i < 64)
            (set & (1L << i)) != 0L
        else
            false
    }

    override def intIterator: IntIterator = new IntIterator {
        private[this] var i: Int = -1
        private[this] def getNextValue(): Unit = {
            do {
                i += 1
            } while (i < 64 && !thisSet.contains(i))
        }
        getNextValue()
        def hasNext: Boolean = i < 64
        def next(): Int = { val i = this.i; getNextValue(); i }
    }

    override def equals(other: Any): Boolean = {
        other match {
            case that: BitArraySet64 ⇒ this.set == that.set
            case EmptyBitArraySet    ⇒ false // this set is never empty!
            case _                   ⇒ super.equals(other)
        }
    }

    override def hashCode: Int = 31 * (set ^ (set >>> 32)).toInt // from j.u.Arrays.hashCode
}

private[immutable] final class BitArraySetN(
        val set: Array[Long]
) extends BitArraySet { thisSet ⇒

    override def +(i: Int): this.type = {
        val bucket = i / 64
        set(bucket) = set(bucket) | (1L << (i - 32 * bucket))
        this
    }
    override def -(i: Int): this.type = {
        val bucket = i / 64
        set(bucket) = set(bucket) & ((-1L & ~(1L << (i - 32 * bucket))))
        this
    }

    override def |(that: BitArraySet): BitArraySet = {
        that match {
            case EmptyBitArraySet ⇒ this

            case that: BitArraySet64 ⇒
                val thisSet0 = this.set(0)
                val thatSet = that.set
                val newSet0 = thisSet0 | thatSet
                if (newSet0 == thisSet0)
                    this
                else {
                    val newSet = set.clone
                    newSet(0) = newSet0
                    new BitArraySetN(newSet)
                }

            case that: BitArraySetN ⇒
                val thisSetLength = this.set.length
                val thatSetLength = that.set.length
                if (thisSetLength >= thatSetLength) {
                    var i = 0
                    while (i < thisSetLength) { i += 1 }
                    ???
                }
                ???

        }
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
            case that: BitArraySetN if this.set.length == that.set.length ⇒
                java.util.Arrays.equals(this.set, that.set)
            case _ ⇒
                super.equals(other)
        }
    }

    override def hashCode: Int = java.util.Arrays.hashCode(set)
}

object BitArraySet {

    final def empty: BitArraySet = EmptyBitArraySet

    /** @param max The maximum value you may want to store in the set. */
    def apply(i: Int): BitArraySet = {
        if (i < 64) new BitArraySet64(1L << i)
        else new BitArraySetN(new Array[Long]((i / 64) + 1)) + i
    }
}
