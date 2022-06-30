/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package collection
package immutable

import java.lang.Integer.toUnsignedLong

/**
 * An immutable bit set for storing positive int values.
 *
 * An array is used to store the underlying values.
 * The empty bit set and sets where the maximum value is 64 use optimized representations.
 *
 * @author Michael Eichberg
 */
sealed abstract class BitArraySet extends BitSet { thisSet =>

    def isEmpty: Boolean

    def +(i: Int): BitArraySet

    def ++(that: BitArraySet): BitArraySet

    def -(i: Int): BitArraySet

    final def |(that: BitArraySet): BitArraySet = this ++ that

    override def equals(other: Any): Boolean
    override def hashCode: Int

    final override def toString: String = mkString("BitArraySet(", ",", ")")

}

private[immutable] object BitArraySet0 extends BitArraySet { thisSet =>

    override def isEmpty: Boolean = true

    override def +(i: Int): BitArraySet = BitArraySet(i)

    override def -(i: Int): BitArraySet = this

    override def ++(that: BitArraySet): BitArraySet = that

    override def contains(i: Int): Boolean = false

    override def iterator: IntIterator = IntIterator.empty

    override def equals(other: Any): Boolean = {
        other match {
            case that: AnyRef => this eq that
            case _            => false
        }
    }
    override def hashCode: Int = 1 // from j.u.Arrays.hashCode
}

private[immutable] final class BitArraySet32(val set: Int) extends BitArraySet { thisSet =>

    assert(set != 0L)

    override def isEmpty: Boolean = false

    override def +(i: Int): BitArraySet = {
        if (i < 32) {
            val set = this.set
            val newSet = set | (1 << i)
            if (newSet != set) new BitArraySet32(newSet) else this
        } else if (i < 64) {
            val newSet = Integer.toUnsignedLong(this.set) | (1L << i)
            new BitArraySet64(newSet)
        } else {
            val newSet = new BitArraySetN(new Array[Int]((i / 32) + 1))
            newSet.set(0) = set
            newSet + i
        }
    }

    override def -(i: Int): BitArraySet = {
        if (i < 32) {
            val set = this.set
            val newSet = set & (-1 & ~(1 << i))
            if (newSet == set)
                this
            else if (newSet == 0)
                BitArraySet0
            else
                new BitArraySet32(newSet)
        } else {
            this
        }
    }

    override def ++(that: BitArraySet): BitArraySet = {
        that match {
            case BitArraySet0 =>
                this

            case that: BitArraySet32 =>
                val thisSet = this.set
                val thatSet = that.set
                val newSet = thisSet | thatSet
                if (newSet == thisSet)
                    this
                else if (newSet == thatSet)
                    that
                else
                    new BitArraySet32(newSet)

            case that: BitArraySet64 =>
                val thatSet = that.set
                val newSet = Integer.toUnsignedLong(this.set) | thatSet
                if (newSet == thatSet)
                    that
                else
                    new BitArraySet64(newSet)

            case that: BitArraySetN =>
                that | this
        }
    }

    override def contains(i: Int): Boolean = {
        if (i < 32)
            (set & (1 << i)) != 0
        else
            false
    }

    override def iterator: IntIterator = new IntIterator {
        private[this] var i: Int = java.lang.Integer.numberOfTrailingZeros(set)
        def hasNext: Boolean = i < 32
        def next(): Int = {
            val currentI = this.i
            var i = currentI
            do { i += 1 } while (i < 32 && (set & (1 << i)) == 0);
            this.i = i
            currentI
        }
    }

    override def equals(other: Any): Boolean = {
        other match {
            case that: BitArraySet32 => this.set == that.set
            case _                   => false
        }
    }

    override def hashCode: Int = 31 * set
}

private[immutable] final class BitArraySet64(val set: Long) extends BitArraySet { thisSet =>

    assert(set != 0L)

    override def isEmpty: Boolean = false

    override def +(i: Int): BitArraySet = {
        if (i < 64) {
            val set = this.set
            val newSet = set | (1L << i)
            if (newSet != set) new BitArraySet64(newSet) else this
        } else {
            val newSet = new BitArraySetN(new Array[Int]((i / 32) + 1))
            newSet.set(0) = set.toInt
            newSet.set(1) = (set >>> 32).toInt
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
                BitArraySet0
            else if ((newSet & BitArraySet.HigherWordMask) == 0)
                new BitArraySet32(newSet.toInt)
            else
                new BitArraySet64(newSet)
        } else {
            this
        }
    }

    override def ++(that: BitArraySet): BitArraySet = {
        that match {
            case BitArraySet0 =>
                this

            case that: BitArraySet32 =>
                val thisSet = this.set
                val newSet = thisSet | toUnsignedLong(that.set)
                if (newSet == thisSet)
                    this
                else
                    new BitArraySet64(newSet)

            case that: BitArraySet64 =>
                val thisSet = this.set
                val thatSet = that.set
                val newSet = thisSet | thatSet
                if (newSet == thisSet)
                    this
                else if (newSet == thatSet)
                    that
                else
                    new BitArraySet64(newSet)

            case that: BitArraySetN =>
                that | this
        }
    }

    override def contains(i: Int): Boolean = {
        if (i < 64)
            (set & (1L << i)) != 0L
        else
            false
    }

    override def iterator: IntIterator = new IntIterator {
        private[this] var i: Int = java.lang.Long.numberOfTrailingZeros(set)
        def hasNext: Boolean = i < 64
        def next(): Int = {
            val currentI = this.i
            var i = currentI
            do { i += 1 } while (i < 64 && (set & (1L << i)) == 0L)
            this.i = i
            currentI
        }
    }

    override def equals(other: Any): Boolean = {
        other match {
            case that: BitArraySet64 => this.set == that.set
            case _                   => false
        }
    }

    override def hashCode: Int = 31 * (set ^ (set >>> 32)).toInt // from j.u.Arrays.hashCode
}

private[immutable] final class BitArraySetN(val set: Array[Int]) extends BitArraySet { self =>

    override def isEmpty: Boolean = false

    override def +(i: Int): BitArraySet = {
        val bucket = i / 32
        val setLength = set.length
        if (bucket >= setLength) {
            val newSet = new Array[Int](bucket + 1)
            Array.copy(set, 0, newSet, 0, setLength)
            newSet(bucket) = (1 << (i - 32 * bucket))
            new BitArraySetN(newSet)
        } else {
            val oldBucketValue = set(bucket)
            val newBucketValue = oldBucketValue | (1 << (i - 32 * bucket))
            if (oldBucketValue == newBucketValue)
                this
            else {
                val newSet = new Array[Int](setLength)
                Array.copy(set, 0, newSet, 0, setLength)
                newSet(bucket) = newBucketValue
                new BitArraySetN(newSet)
            }
        }
    }

    override def -(i: Int): BitArraySet = {
        val setLength = set.length

        val bucket = i / 32
        if (bucket >= setLength)
            return this;

        val oldBucketValue = set(bucket)
        val newBucketValue = oldBucketValue & ((-1 & ~(1 << (i - 32 * bucket))))
        if (newBucketValue == oldBucketValue)
            return this;

        val lastBucket = setLength - 1
        if (newBucketValue == 0 && bucket == lastBucket) {
            // check how many buckets can be deleted....
            var emptyBuckets = 1
            while (emptyBuckets < setLength && set(lastBucket - emptyBuckets) == 0) {
                emptyBuckets += 1
            }
            (setLength - emptyBuckets) match {
                case 0 => BitArraySet0
                case 1 => new BitArraySet32(set(0))
                case 2 =>
                    val newSet = toUnsignedLong(set(0)) | toUnsignedLong(set(1)) << 32
                    new BitArraySet64(newSet)
                case x =>
                    val newSet = new Array[Int](x)
                    Array.copy(set, 0, newSet, 0, x)
                    new BitArraySetN(newSet)
            }
        } else {
            val newSet = new Array[Int](setLength)
            Array.copy(set, 0, newSet, 0, setLength)
            newSet(bucket) = newBucketValue
            new BitArraySetN(newSet)
        }
    }

    override def ++(that: BitArraySet): BitArraySet = {
        that match {
            case BitArraySet0 => this

            case that: BitArraySet32 =>
                val thisSet0 = this.set(0)
                val newSet0 = thisSet0 | that.set
                if (newSet0 == thisSet0)
                    this
                else {
                    val newSet = set.clone
                    newSet(0) = newSet0
                    new BitArraySetN(newSet)
                }

            case that: BitArraySet64 =>
                val thisSet = toUnsignedLong(this.set(0)) | toUnsignedLong(this.set(1)) << 32
                val newSet64 = thisSet | that.set
                if (newSet64 == thisSet)
                    this
                else {
                    val newSet = set.clone
                    newSet(0) = newSet64.toInt
                    newSet(1) = (newSet64 >>> 32).toInt
                    new BitArraySetN(newSet)
                }

            case that: BitArraySetN =>
                val thisSet = this.set
                val thatSet = that.set
                val thisSetLength = this.set.length
                val thatSetLength = that.set.length
                if (thisSetLength == thatSetLength) {
                    var i = 0
                    var takeThis = true
                    var takeThat = true
                    val newSet = new Array[Int](thisSetLength)
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
                    val newSet = new Array[Int](thisSetLength)
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
                    val newSet = new Array[Int](thatSetLength)
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
        val set = this.set

        val bucket = i / 32
        if (bucket >= set.length)
            return false;

        (set(bucket) & (1 << (i - 32 * bucket))) != 0
    }

    override def iterator: IntIterator = new IntIterator {
        private[this] final val max: Int = set.length * 32
        private[this] var i: Int = -1
        private[this] def advanceIterator(): Unit = {
            val set = self.set
            var bucket = -1
            do {
                i += 1
                bucket = i / 32
            } while (i < max && (set(bucket) & (1 << (i - 32 * bucket))) == 0)
        }
        advanceIterator()
        def hasNext: Boolean = i < max
        def next(): Int = { val i = this.i; advanceIterator(); i }
    }

    override def equals(other: Any): Boolean = {
        other match {
            case that: BitArraySetN => java.util.Arrays.equals(this.set, that.set)
            case _                  => false
        }
    }

    override def hashCode: Int = java.util.Arrays.hashCode(set)
}

object BitArraySet {

    /**
     * Masks the higher word.
     */
    final val HigherWordMask = ~java.lang.Integer.toUnsignedLong(-1)

    final def empty: BitArraySet = BitArraySet0

    def apply(i: Int): BitArraySet = {
        if (i < 32)
            new BitArraySet32(1 << i)
        else if (i < 64)
            new BitArraySet64(1L << i)
        else
            new BitArraySetN(new Array[Int]((i / 32) + 1)) + i
    }
}
