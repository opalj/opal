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

/**
 * An immutable bit set for storing positive int values. An array is used to store
 * the underlying values. The empty bit set and sets where the maximum value is 64
 * use optimized representations.
 *
 * @author Michael Eichberg
 */
sealed abstract class BitArraySet extends BitSet { thisSet ⇒

    def isEmpty: Boolean

    def +(i: Int): BitArraySet

    def ++(that: BitArraySet): BitArraySet

    def -(i: Int): BitArraySet

    final def |(that: BitArraySet): BitArraySet = this ++ that

    override def equals(other: Any): Boolean = {
        other match {
            case that: BitArraySet ⇒ this.intIterator.sameValues(that.intIterator)
            case _                 ⇒ false
        }
    }

    final override def toString: String = mkString("BitArraySet(", ",", ")")

}

private[immutable] final object EmptyBitArraySet extends BitArraySet { thisSet ⇒

    override def isEmpty: Boolean = true

    override def +(i: Int): BitArraySet = BitArraySet(i)

    override def -(i: Int): BitArraySet = this

    override def ++(that: BitArraySet): BitArraySet = that

    override def contains(i: Int): Boolean = false

    override def intIterator: IntIterator = IntIterator.empty

    override def equals(other: Any): Boolean = {
        other match {
            case that: BitArraySet ⇒ that.isEmpty
            case _                 ⇒ false
        }
    }

    override def hashCode: Int = 1 // from j.u.Arrays.hashCode
}

private[immutable] final class BitArraySet64(val set: Long) extends BitArraySet { thisSet ⇒

    assert(set != 0L)

    override def isEmpty: Boolean = false

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

    override def ++(that: BitArraySet): BitArraySet = {
        that match {
            case EmptyBitArraySet ⇒
                this

            case that: BitArraySet64 ⇒
                val thisSet = this.set
                val thatSet = that.set
                val newSet = thisSet | thatSet
                if (newSet == thisSet)
                    this
                else if (newSet == thatSet)
                    that
                else
                    new BitArraySet64(newSet)

            case that: BitArraySetN ⇒
                that | this
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
            do { i += 1 } while (i < 64 && !thisSet.contains(i))
        }
        getNextValue()
        def hasNext: Boolean = i < 64
        def next(): Int = { val i = this.i; getNextValue(); i }
    }

    override def equals(other: Any): Boolean = {
        other match {
            case EmptyBitArraySet    ⇒ false // this set is never empty!
            case that: BitArraySet64 ⇒ this.set == that.set
            case _                   ⇒ super.equals(other)
        }
    }

    override def hashCode: Int = 31 * (set ^ (set >>> 32)).toInt // from j.u.Arrays.hashCode
}

private[immutable] final class BitArraySetN(val set: Array[Long]) extends BitArraySet { thisSet ⇒

    override def isEmpty: Boolean = false

    override def +(i: Int): BitArraySet = {
        val bucket = i / 64
        val setLength = set.length
        if (bucket >= setLength) {
            val newSet = new Array[Long](bucket + 1)
            Array.copy(set, 0, newSet, 0, setLength)
            newSet(bucket) = (1L << (i - 64 * bucket))
            new BitArraySetN(newSet)
        } else {
            val oldBucketValue = set(bucket)
            val newBucketValue = oldBucketValue | (1L << (i - 64 * bucket))
            if (oldBucketValue == newBucketValue)
                this
            else {
                val newSet = new Array[Long](setLength)
                Array.copy(set, 0, newSet, 0, setLength)
                newSet(bucket) = newBucketValue
                new BitArraySetN(newSet)
            }
        }
    }

    override def -(i: Int): BitArraySet = {
        val bucket = i / 64
        if (bucket >= set.length)
            return this;

        val oldBucketValue = set(bucket)
        val newBucketValue = oldBucketValue & ((-1L & ~(1L << (i - 64 * bucket))))
        if (newBucketValue == oldBucketValue)
            return this;

        val setLength = set.length
        val lastBucket = setLength - 1
        if (newBucketValue == 0 && bucket == lastBucket) {
            // check how many buckets can be deleted....
            var emptyBuckets = 1
            while (emptyBuckets < setLength && set(lastBucket - emptyBuckets) == 0) { emptyBuckets += 1 }
            (setLength - emptyBuckets) match {
                case 0 ⇒ EmptyBitArraySet
                case 1 ⇒ new BitArraySet64(set(0))
                case x ⇒
                    val newSet = new Array[Long](x)
                    Array.copy(set, 0, newSet, 0, x)
                    new BitArraySetN(newSet)
            }
        } else {
            val newSet = new Array[Long](setLength)
            Array.copy(set, 0, newSet, 0, setLength)
            newSet(bucket) = newBucketValue
            new BitArraySetN(newSet)
        }
    }

    override def ++(that: BitArraySet): BitArraySet = {
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
                val thisSet = this.set
                val thatSet = that.set
                val thisSetLength = this.set.length
                val thatSetLength = that.set.length
                if (thisSetLength == thatSetLength) {
                    var i = 0
                    var takeThis = true
                    var takeThat = true
                    val newSet = new Array[Long](thisSetLength)
                    while (i < thisSetLength) {
                        val thisSetI = thisSet(i)
                        val thatSetI = thatSet(i)
                        val newSetI = thisSetI | thatSetI
                        takeThis &= (newSetI == thisSetI)
                        takeThat &= (newSetI == thatSetI)
                        newSet(i) = newSetI
                        i += 1
                    }
                    if (takeThis) this
                    else if (takeThat) that
                    else new BitArraySetN(newSet)
                } else if (thisSetLength > thatSetLength) {
                    var i = 0
                    var takeThis = true
                    val newSet = new Array[Long](thisSetLength)
                    while (i < thatSetLength) {
                        val thisSetI = thisSet(i)
                        val thatSetI = thatSet(i)
                        val newSetI = thisSetI | thatSetI
                        takeThis &= (newSetI == thisSetI)
                        newSet(i) = newSetI
                        i += 1
                    }
                    if (takeThis)
                        this
                    else {
                        Array.copy(set, i, newSet, i, thisSetLength - i)
                        new BitArraySetN(newSet)
                    }
                } else /*if (thisSetLength <= thatSetLength)*/ {
                    var i = 0
                    var takeThat = true
                    val newSet = new Array[Long](thatSetLength)
                    while (i < thisSetLength) {
                        val thisSetI = thisSet(i)
                        val thatSetI = thatSet(i)
                        val newSetI = thisSetI | thatSetI
                        takeThat &= (newSetI == thatSetI)
                        newSet(i) = newSetI
                        i += 1
                    }
                    if (takeThat)
                        that
                    else {
                        Array.copy(that.set, i, newSet, i, thatSetLength - i)
                        new BitArraySetN(newSet)
                    }
                }

        }
    }

    override def contains(i: Int): Boolean = {
        val bucket = i / 64
        val set = this.set
        if (bucket >= set.length)
            return false;

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
