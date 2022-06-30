/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package collection
package mutable

import java.io.Serializable

/**
 * A bit set with a given upper bound for the largest value that can be stored in the set.
 * The upper bound is only used to create an optimal underlying representation. It has no
 * impact on equals and/or hashcode computations. I.e., two sets with two different upper bounds
 * which contain the same values, are equal and have the same hashcode.
 *
 * Conceptually, the an array of long values is used to store the values.
 *
 * @note If values are added to the set that are larger than the specified size the behavior is
 *       undefined!
 *
 * @author Michael Eichberg
 */
sealed abstract class FixedSizeBitSet extends BitSet with Serializable {

    def +=(i: Int): this.type

    /**
     * Adds the given value to the set if the value is not in the set and returns true;
     * otherwise returns false. That is, the value is definitively in the set afterwards.
     */
    def add(i: Int): Boolean

    def -=(i: Int): this.type

    override def toString: String = mkString("FixedSizeBitSet(", ",", ")")
}

private[mutable] object ZeroLengthBitSet extends FixedSizeBitSet {
    override def isEmpty: Boolean = true
    override def +=(i: Int): this.type = throw new UnsupportedOperationException("fixed size is 0")
    override def add(i: Int): Boolean = throw new UnsupportedOperationException("fixed size is 0")
    override def -=(i: Int): this.type = this
    override def contains(i: Int): Boolean = false
    override def iterator: IntIterator = IntIterator.empty
    override def equals(other: Any): Boolean = {
        other match {
            case that: FixedSizeBitSet => that.isEmpty
            case _                     => false
        }
    }
    override def hashCode: Int = 1 // same as Arrays.hashCode(empty long array)
}

private[mutable] final class FixedSizeBitSet64 extends FixedSizeBitSet { thisSet =>

    private[mutable] var set: Long = 0L

    override def isEmpty: Boolean = set == 0L
    override def +=(i: Int): this.type = { set |= 1L << i; this }
    override def add(i: Int): Boolean = {
        val oldSet = set
        val newSet = oldSet | 1L << i
        if (newSet != oldSet) { set = newSet; true } else false
    }
    override def -=(i: Int): this.type = { set &= (-1L & ~(1L << i)); this }
    override def contains(i: Int): Boolean = (set & (1L << i)) != 0L

    override def iterator: IntIterator = new IntIterator {
        private[this] var i: Int = -1
        private[this] def advanceIterator(): Unit = {
            do { i += 1 } while (i < 64 && !thisSet.contains(i))
        }
        advanceIterator()
        def hasNext: Boolean = i < 64
        def next(): Int = { val i = this.i; advanceIterator(); i }
    }

    override def equals(other: Any): Boolean = {
        other match {
            case that: FixedSizeBitSet64  => that.set == this.set
            case that: FixedSizeBitSet128 => that.set2 == 0L && that.set1 == this.set
            case that: FixedSizeBitSetN   => that.equals(this)
            case ZeroLengthBitSet         => this.set == 0L
            case _                        => false
        }
    }

    override def hashCode: Int = {
        val elementHash = (set ^ (set >>> 32)).toInt
        if (elementHash == 0)
            1
        else
            31 + elementHash
    }
}

private[mutable] final class FixedSizeBitSet128 extends FixedSizeBitSet { thisSet =>

    private[mutable] var set1: Long = 0L
    private[mutable] var set2: Long = 0L

    override def isEmpty: Boolean = set1 == 0L && set2 == 0L

    override def +=(i: Int): this.type = {
        if (i <= 63)
            set1 |= 1L << i
        else {
            set2 |= 1L << (i - 64)
        }
        this
    }
    override def add(i: Int): Boolean = {
        if (i <= 63) {
            val oldSet1 = set1
            val newSet1 = oldSet1 | 1L << i
            if (oldSet1 != newSet1) { set1 = newSet1; true } else false
        } else {
            val oldSet2 = set2
            val newSet2 = oldSet2 | 1L << (i - 64)
            if (oldSet2 != newSet2) { set2 = newSet2; true } else false
        }
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

    override def iterator: IntIterator = new IntIterator {
        private[this] var i: Int = -1
        private[this] def advanceIterator(): Unit = {
            do { i += 1 } while (i < 128 && !thisSet.contains(i))
        }
        advanceIterator()
        def hasNext: Boolean = i < 128
        def next(): Int = { val i = this.i; advanceIterator(); i }
    }

    override def equals(other: Any): Boolean = {
        other match {
            case that: FixedSizeBitSet128 => this.set1 == that.set1 && this.set2 == that.set2
            case ZeroLengthBitSet         => this.set1 == 0L && this.set2 == 0L
            case that: FixedSizeBitSet64  => this.set2 == 0L && this.set1 == that.set
            case that: FixedSizeBitSetN   => that.equals(this)
            case _                        => false
        }
    }

    override def hashCode: Int = {
        var result = 1
        val set1Hash = (set1 ^ (set1 >>> 32)).toInt
        if (set1Hash != 0) result = 31 + set1Hash
        val set2Hash = (set2 ^ (set2 >>> 32)).toInt
        if (set2Hash != 0) 31 * result + set2Hash
        result
    }
}

private[mutable] final class FixedSizeBitSetN private[mutable] (
        private val set: Array[Long]
) extends FixedSizeBitSet { thisSet =>

    assert(set.length > 2)

    override def isEmpty: Boolean = {
        val set = this.set
        var i = 0
        val max = set.length
        while (i < max) { if (set(i) != 0L) return false; i += 1 }
        true
    }

    override def +=(i: Int): this.type = {
        val bucket = i / 64
        set(bucket) = set(bucket) | (1L << (i - 64 * bucket))
        this
    }
    override def add(i: Int): Boolean = {
        val bucket = i / 64
        val oldSet = set(bucket)
        val newSet = oldSet | (1L << (i - 64 * bucket))
        if (newSet != oldSet) { set(bucket) = newSet; true } else false
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

    override def iterator: IntIterator = new IntIterator {
        private[this] val max: Int = set.length * 64
        private[this] var i: Int = -1
        private[this] def advanceIterator(): Unit = {
            do { i += 1 } while (i < max && !thisSet.contains(i))
        }
        advanceIterator()
        def hasNext: Boolean = i < max
        def next(): Int = { val i = this.i; advanceIterator(); i }
    }

    override def equals(other: Any): Boolean = {
        other match {
            case that: FixedSizeBitSetN if this.set.length == that.set.length =>
                java.util.Arrays.equals(this.set, that.set)

            case ZeroLengthBitSet =>
                var i = 0
                val length = set.length
                while (i < length) { if (set(i) != 0) return false; i += 1 }
                true

            case that: FixedSizeBitSet64 =>
                var i = 1
                val length = set.length
                while (i < length) { if (set(i) != 0) return false; i += 1 }
                set(0) == that.set

            case that: FixedSizeBitSet128 =>
                var i = 2
                val length = set.length
                while (i < length) { if (set(i) != 0) return false; i += 1 }
                set(0) == that.set1 && set(1) == that.set2

            case _ =>
                super.equals(other)
        }
    }

    override def hashCode: Int = {
        val set = this.set
        var result = 1;
        var i = 0
        val max = set.length
        while (i < max) {
            val elementHash = (set(i) ^ (set(i) >>> 32)).toInt
            if (elementHash != 0)
                result = 31 * result + elementHash
            i += 1
        }
        result
    }
}

/** Factory to create fixed size bit arrays. */
object FixedSizeBitSet {

    final val empty: FixedSizeBitSet = ZeroLengthBitSet

    /**
     * Creates a new mutable bit set with a fixed size.
     *
     * @param max The maximum value (inclusive) you may want to store in the set.
     */
    def create(max: Int): FixedSizeBitSet = {
        // Note if max is zero, we may still want to be able to store the value 0!
        if (max < 64) new FixedSizeBitSet64
        else if (max < 128) new FixedSizeBitSet128
        else new FixedSizeBitSetN(new Array[Long]((max / 64) + 1))
    }

}
