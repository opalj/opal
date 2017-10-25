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
 * A bit set with a given, fixed maximum size. If values are added to the set that are larger
 * than the specified size the behavior is undefined!
 *
 * @author Michael Eichberg
 */
sealed abstract class BitSet {

    def +=(i: Int): this.type

    def -=(i: Int): this.type

    def contains(i: Int): Boolean

    def intIterator: IntIterator

    def iterator: Iterator[Int] = new AbstractIterator[Int] {
        private[this] val it = intIterator
        def hasNext: Boolean = it.hasNext
        def next(): Int = it.next()
    }

    override def equals(other: scala.Any): Boolean = {
        other match {
            case that: BitSet ⇒
                val thatIt = that.intIterator
                val thisIt = this.intIterator
                while (thisIt.hasNext && thatIt.hasNext) {
                    if (thisIt.next != thatIt.next)
                        return false;
                }
                thisIt.hasNext == thatIt.hasNext // ... i.e., if both are false the sets are equal
            case _ ⇒
                false
        }
    }

    def mkString(pre: String, in: String, post: String): String = {
        intIterator.mkString(pre, in, post)
    }

    override def toString: String = mkString("BitSet(", ", ", ")")
}

private[mutable] final class BitSet64 extends BitSet { bitSet ⇒

    private var set: Long = 0l

    def +=(i: Int): this.type = { set |= 1l << i; this }
    def -=(i: Int): this.type = { set &= (-1l & ~(1l << i)); this }
    def contains(i: Int): Boolean = (set & (1l << i)) != 0l

    def intIterator: IntIterator = new IntIterator {
        private[this] var i: Int = -1
        private[this] def getNextValue(): Unit = {
            do {
                i += 1
            } while (i < 64 && !bitSet.contains(i))
        }
        getNextValue()
        def hasNext: Boolean = i < 64
        def next(): Int = { val i = this.i; getNextValue(); i }
    }

    override def equals(other: scala.Any): Boolean = {
        other match {
            case that: BitSet64 ⇒
                this.set == that.set
            case _ ⇒
                super.equals(other)
        }
    }

    override def hashCode: Int = java.lang.Long.hashCode(set)
}

private[mutable] final class BitSet128 extends BitSet { bitSet ⇒

    private var set1: Long = 0l
    private var set2: Long = 0l

    def +=(i: Int): this.type = {
        if (i <= 63)
            set1 |= 1l << i
        else {
            set2 |= 1l << (i - 64)
        }
        this
    }
    def -=(i: Int): this.type = {
        if (i <= 63)
            set1 &= (-1l & ~(1l << i))
        else
            set2 &= (-1l & ~(1l << (i - 64)))
        this
    }
    def contains(i: Int): Boolean = {
        if (i <= 63)
            (set1 & (1l << i)) != 0l
        else
            (set2 & (1l << (i - 64))) != 0l
    }

    def intIterator: IntIterator = new IntIterator {
        private[this] var i: Int = -1
        private[this] def getNextValue(): Unit = {
            do {
                i += 1
            } while (i < 128 && !bitSet.contains(i))
        }
        getNextValue()
        def hasNext: Boolean = i < 128
        def next(): Int = { val i = this.i; getNextValue(); i }
    }

    override def equals(other: scala.Any): Boolean = {
        other match {
            case that: BitSet128 ⇒
                this.set1 == that.set1 && this.set2 == that.set2
            case _ ⇒
                super.equals(other)
        }
    }

    override def hashCode: Int = java.lang.Long.hashCode(set1 ^ set2)
}

private[mutable] final class BitSetN private[mutable] (
        private val set: Array[Int]
) extends BitSet { bitSet ⇒

    def +=(i: Int): this.type = {
        val bucket = i / 32
        set(bucket) = set(bucket) | (1 << (i - 32 * bucket))
        this
    }
    def -=(i: Int): this.type = {
        val bucket = i / 32
        set(bucket) = set(bucket) & ((-1 & ~(1 << (i - 32 * bucket))))
        this
    }
    def contains(i: Int): Boolean = {
        val bucket = i / 32
        (set(bucket) & (1 << (i - 32 * bucket))) != 0
    }

    def intIterator: IntIterator = new IntIterator {
        private[this] val max: Int = set.length * 32
        private[this] var i: Int = -1
        private[this] def getNextValue(): Unit = {
            do {
                i += 1
            } while (i < max && !bitSet.contains(i))
        }
        getNextValue()
        def hasNext: Boolean = i < max
        def next(): Int = { val i = this.i; getNextValue(); i }
    }

    override def equals(other: scala.Any): Boolean = {
        other match {
            case that: BitSetN if this.set.length == that.set.length ⇒
                java.util.Arrays.equals(this.set, that.set)
            case _ ⇒
                super.equals(other)
        }
    }

    override def hashCode: Int = java.util.Arrays.hashCode(set)
}

object FixedSizeBitSet {

    final val empty = new BitSet {
        def +=(i: Int): this.type = throw new UnsupportedOperationException("fixed size is 0")
        def -=(i: Int): this.type = this
        def contains(i: Int): Boolean = false
        def intIterator: IntIterator = IntIterator.empty
    }

    /** @param max The maximum value you may want to store in the set. */
    def apply(max: Int): BitSet = {
        if (max == 0) empty
        else if (max < 64) new BitSet64
        else if (max < 128) new BitSet128
        else new BitSetN(new Array[Int]((max / 32) + 1))
    }

}
