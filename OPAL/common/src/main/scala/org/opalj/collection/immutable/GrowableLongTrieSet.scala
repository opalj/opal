/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.collection.immutable

import org.opalj.collection.LongIterator
import java.lang.Long.{hashCode ⇒ lHashCode}

sealed abstract class GrowableLongTrieSet { intSet ⇒

    def isEmpty: Boolean
    def isSingletonSet: Boolean
    def size: Int
    def contains(value: Long): Boolean
    def foreach[U](f: Long ⇒ U): Unit
    def iterator: LongIterator
    def +(value: Long): GrowableLongTrieSet

    final override def equals(other: Any): Boolean = {
        other match {
            case that: GrowableLongTrieSet ⇒ this.equals(that)
            case _                         ⇒ false
        }
    }

    def equals(other: GrowableLongTrieSet): Boolean
}

object GrowableLongTrieSet {

    def empty: GrowableLongTrieSet = GrowableLongTrieSet0

}

/** The node of an IntTrie. */
private[immutable] sealed trait GrowableLongTrieSetN {

    def foreach[U](f: Long ⇒ U): Unit

    //
    // IMPLEMENTATION "INTERNAL" METHODS
    //
    private[immutable] def +(i: Long, level: Int): GrowableLongTrieSetN
    private[immutable] def contains(value: Long, key: Long): Boolean
    private[immutable] def toString(indent: Int): String

    final private[immutable] def bitsToString(bits: Int): String = {
        bits.toBinaryString.reverse.padTo(3, '0').reverse
    }
}

/** The (potential) leaves of an IntTrie. */
private[immutable] sealed abstract class GrowableLongTrieSetL extends GrowableLongTrieSet with GrowableLongTrieSetN {

    final override private[immutable] def contains(value: Long, key: Long): Boolean = {
        this.contains(value)
    }

    final override private[immutable] def toString(indent: Int): String = {
        (" " * indent) + toString()
    }
}

case object GrowableLongTrieSet0 extends GrowableLongTrieSetL {
    override def isSingletonSet: Boolean = false
    override def isEmpty: Boolean = true
    override def size: Int = 0
    override def foreach[U](f: Long ⇒ U): Unit = {}
    override def +(i: Long): GrowableLongTrieSet1 = GrowableLongTrieSet1(i)
    override def iterator: LongIterator = LongIterator.empty
    override def contains(value: Long): Boolean = false

    override def equals(other: GrowableLongTrieSet): Boolean = other eq this
    override def hashCode: Int = 0 // compatible to Arrays.hashCode
    override def toString: String = "GrowableLongTrieSet()"

    private[immutable] override def +(i: Long, level: Int): GrowableLongTrieSetN = this.+(i)

}

final case class GrowableLongTrieSet1 private (i: Long) extends GrowableLongTrieSetL {
    override def isEmpty: Boolean = false
    override def isSingletonSet: Boolean = true
    override def size: Int = 1
    override def foreach[U](f: Long ⇒ U): Unit = { f(i) }
    override def +(i: Long): GrowableLongTrieSetL = if (this.i == i) this else new GrowableLongTrieSet2(this.i, i)
    override def iterator: LongIterator = LongIterator(i)
    override def contains(value: Long): Boolean = value == i

    override def equals(other: GrowableLongTrieSet): Boolean = {
        (other eq this) || (other match {
            case that: GrowableLongTrieSet1 ⇒ this.i == that.i
            case that                       ⇒ false
        })
    }

    override def hashCode: Int = 31 + lHashCode(i)

    override def toString: String = s"GrowableLongTrieSet($i)"

    override private[immutable] def +(i: Long, level: Int): GrowableLongTrieSetN = this.+(i)
}

/**
 * Represents an ordered set of two values where i1 has to be smaller than i2.
 */
private[immutable] final class GrowableLongTrieSet2 private[immutable] (
        val i1: Long, val i2: Long
) extends GrowableLongTrieSetL {

    override def isEmpty: Boolean = false
    override def isSingletonSet: Boolean = false
    override def size: Int = 2
    override def iterator: LongIterator = LongIterator(i1, i2)
    override def foreach[U](f: Long ⇒ U): Unit = { f(i1); f(i2) }
    override def +(i: Long): GrowableLongTrieSetL = if (i1 == i | i2 == i) this else new GrowableLongTrieSet3(i1, i2, i)
    override def contains(value: Long): Boolean = value == i1 || value == i2

    override def equals(other: GrowableLongTrieSet): Boolean = {
        (other eq this) || (
            other match {
                case that: GrowableLongTrieSet2 ⇒ this.i1 == that.i1 && this.i2 == that.i2
                case that                       ⇒ false
            }
        )
    }

    override def hashCode: Int = 31 * (31 + lHashCode(i1)) + lHashCode(i2)

    override def toString: String = s"GrowableLongTrieSet($i1, $i2)"

    override private[immutable] def +(i: Long, level: Int): GrowableLongTrieSetN = this.+(i)
}

/**
 * Represents an ordered set of three int values: i1 < i2 < i3.
 */
private[immutable] final class GrowableLongTrieSet3 private[immutable] (
        val i1: Long, val i2: Long, val i3: Long
) extends GrowableLongTrieSetL {

    override def size: Int = 3
    override def isEmpty: Boolean = false
    override def isSingletonSet: Boolean = false
    override def contains(value: Long): Boolean = value == i1 || value == i2 || value == i3
    override def iterator: LongIterator = LongIterator(i1, i2, i3)
    override def foreach[U](f: Long ⇒ U): Unit = { f(i1); f(i2); f(i3) }

    override def +(i: Long): GrowableLongTrieSet = {
        if (i == i1 || i == i2 || i == i3)
            this
        else
            new LargeGrowableLongTrieSet(4, this + (i, 0))
    }

    override def equals(other: GrowableLongTrieSet): Boolean = {
        (other eq this) || (
            other match {
                case that: GrowableLongTrieSet3 ⇒
                    this.i1 == that.i1 && this.i2 == that.i2 && this.i3 == that.i3
                case that ⇒
                    that.size == 3 && that.contains(i1) && that.contains(i2) && that.contains(i3)
            }
        )
    }

    override def hashCode: Int = 31 * (31 * (31 + lHashCode(i1)) + lHashCode(i2)) + lHashCode(i3) // compatible to Arrays.hashCode

    override def toString: String = s"GrowableLongTrieSet($i1, $i2, $i3)"

    override private[immutable] def +(i: Long, level: Int): GrowableLongTrieSetN = {
        if (i == i1 || i == i2 || i == i3)
            this
        else {
            val l = GrowableLongTrieSet1(i)
            var r: GrowableLongTrieSetN = new GrowableLongTrieSetN1(((i >> level) & 7L).toInt, l)
            r = r + (i1, level)
            r = r + (i2, level)
            r = r + (i3, level)
            r
        }
    }
}

private[immutable] class LargeGrowableLongTrieSet(
        val size: Int,
        root:     GrowableLongTrieSetN
) extends GrowableLongTrieSet {

    assert(size >= 4)

    override def isEmpty: Boolean = false
    override def isSingletonSet: Boolean = false
    override def contains(value: Long): Boolean = {
        root.contains(value, value)
    }
    override def foreach[U](f: Long ⇒ U): Unit = {
        root.foreach(f)
    }

    override def iterator: LongIterator = ???

    override def equals(other: GrowableLongTrieSet): Boolean = ??? /*{
        (other eq this) || {
          other.size == this.size &&
            ???
        }
    }*/

    override def +(i: Long): GrowableLongTrieSet = {
        val root = this.root
        val newRoot = root + (i, 0)
        if (newRoot ne root) {
            new LargeGrowableLongTrieSet(size + 1, newRoot)
        } else {
            this
        }
    }

    override def toString: String = s"GrowableLongTrieSet(#$size, ${root.toString(1)})"

}

private[immutable] final class GrowableLongTrieSetN1(
        n1Bits: Int,
        n1:     GrowableLongTrieSetN
) extends GrowableLongTrieSetN {

    override def foreach[U](f: Long ⇒ U): Unit = n1.foreach(f)

    override private[immutable] def contains(value: Long, key: Long): Boolean = {
        if (n1Bits == (key & 7L).toInt) {
            n1.contains(value, key >> 3)
        } else {
            false
        }
    }

    override private[immutable] def +(v: Long, level: Int): GrowableLongTrieSetN = {
        val vBits = ((v >> level) & 7L).toInt
        if (vBits == n1Bits) {
            val newN1 = n1 + (v, level + 3)
            if (newN1 ne n1) {
                new GrowableLongTrieSetN1(n1Bits, newN1)
            } else {
                this
            }
        } else {
            val lookupTable = 1 << (n1Bits * 4) | 2 << (vBits * 4)
            new GrowableLongTrieSetN2(lookupTable, n1, GrowableLongTrieSet1(v))
        }
    }

    override def toString(indent: Int): String = {
        s"N(${bitsToString(n1Bits)}=>${n1.toString(indent + 1)})"
    }

}

private[immutable] abstract class GrowableLongTrieSetN2x extends GrowableLongTrieSetN {

    // The mapping between the three (relevant) bits of the value to the slot where the value
    // is stored.
    // def lookupTable : Int
    val lookupTable: Int

    // The index is "1" based.
    def node(index: Int): GrowableLongTrieSetN

    final override private[immutable] def contains(v: Long, key: Long): Boolean = {
        val vBits = (key & 7L).toInt
        val vIndex = (lookupTable >> (vBits * 4)) & 15
        val n = node(vIndex)
        if (n ne null) {
            n.contains(v, key >> 3)
        } else {
            false
        }
    }

    override def toString(level: Int): String = {
        val indent = " " * level * 3
        var s = s"N("
        var i = 0
        while (i < 8) {
            val index = ((lookupTable >> (i * 4)) & 15)
            val n = node(index)
            if (n ne null) {
                s += s"\n$indent${bitsToString(i)}=>${n.toString(level + 1)}"
            }
            i += 1
        }
        s+")"
    }
}

private[immutable] final class GrowableLongTrieSetN2(
        val lookupTable: Int,
        n1:              GrowableLongTrieSetN,
        n2:              GrowableLongTrieSetN
) extends GrowableLongTrieSetN2x {

    override def node(index: Int): GrowableLongTrieSetN = {
        index match {
            case 1 ⇒ n1
            case 2 ⇒ n2
            case _ ⇒ null
        }
    }

    override def foreach[U](f: Long ⇒ U): Unit = {
        n1.foreach(f)
        n2.foreach(f)
    }

    override private[immutable] def +(v: Long, level: Int): GrowableLongTrieSetN = {
        val vBits = ((v >> level) & 7L).toInt
        val vIndex = (lookupTable >> (vBits * 4)) & 15
        vIndex match {
            case 0 ⇒
                val newLookupTable = lookupTable | (3 << (vBits * 4))
                new GrowableLongTrieSetN3(newLookupTable, n1, n2, GrowableLongTrieSet1(v))
            case 1 ⇒
                val newN1 = n1 + (v, level + 3)
                if (newN1 ne n1) {
                    new GrowableLongTrieSetN2(lookupTable, newN1, n2)
                } else {
                    this
                }
            case 2 ⇒
                val newN2 = n2 + (v, level + 3)
                if (newN2 ne n2) {
                    new GrowableLongTrieSetN2(lookupTable, n1, newN2)
                } else {
                    this
                }
        }
    }

}

private[immutable] final class GrowableLongTrieSetN3(
        val lookupTable: Int,
        n1:              GrowableLongTrieSetN,
        n2:              GrowableLongTrieSetN,
        n3:              GrowableLongTrieSetN
) extends GrowableLongTrieSetN2x {

    override def foreach[U](f: Long ⇒ U): Unit = {
        n1.foreach(f)
        n2.foreach(f)
        n3.foreach(f)
    }

    override def node(index: Int): GrowableLongTrieSetN = {
        index match {
            case 1 ⇒ n1
            case 2 ⇒ n2
            case 3 ⇒ n3
            case _ ⇒ null
        }
    }

    override private[immutable] def +(v: Long, level: Int): GrowableLongTrieSetN = {
        val vBits = ((v >> level) & 7L).toInt
        val vIndex = (lookupTable >> (vBits * 4)) & 15
        vIndex match {
            case 0 ⇒
                val newLookupTable = lookupTable | (4 << (vBits * 4))
                new GrowableLongTrieSetN4(newLookupTable, n1, n2, n3, GrowableLongTrieSet1(v))
            case 1 ⇒
                val newN1 = n1 + (v, level + 3)
                if (newN1 ne n1) {
                    new GrowableLongTrieSetN3(lookupTable, newN1, n2, n3)
                } else {
                    this
                }
            case 2 ⇒
                val newN2 = n2 + (v, level + 3)
                if (newN2 ne n2) {
                    new GrowableLongTrieSetN3(lookupTable, n1, newN2, n3)
                } else {
                    this
                }
            case 3 ⇒
                val newN3 = n3 + (v, level + 3)
                if (newN3 ne n3) {
                    new GrowableLongTrieSetN3(lookupTable, n1, n2, newN3)
                } else {
                    this
                }
        }
    }
}

private[immutable] final class GrowableLongTrieSetN4(
        val lookupTable: Int,
        n1:              GrowableLongTrieSetN,
        n2:              GrowableLongTrieSetN,
        n3:              GrowableLongTrieSetN,
        n4:              GrowableLongTrieSetN
) extends GrowableLongTrieSetN2x {

    override def foreach[U](f: Long ⇒ U): Unit = {
        n1.foreach(f)
        n2.foreach(f)
        n3.foreach(f)
        n4.foreach(f)
    }

    override def node(index: Int): GrowableLongTrieSetN = {
        index match {
            case 1 ⇒ n1
            case 2 ⇒ n2
            case 3 ⇒ n3
            case 4 ⇒ n4
            case _ ⇒ null
        }
    }

    override private[immutable] def +(v: Long, level: Int): GrowableLongTrieSetN = {
        val vBits = ((v >> level) & 7L).toInt
        val vIndex = (lookupTable >> (vBits * 4)) & 15
        vIndex match {
            case 0 ⇒
                val newLookupTable = lookupTable | (5 << (vBits * 4))
                new GrowableLongTrieSetN5(newLookupTable, n1, n2, n3, n4, GrowableLongTrieSet1(v))
            case 1 ⇒
                val newN1 = n1 + (v, level + 3)
                if (newN1 ne n1) {
                    new GrowableLongTrieSetN4(lookupTable, newN1, n2, n3, n4)
                } else {
                    this
                }
            case 2 ⇒
                val newN2 = n2 + (v, level + 3)
                if (newN2 ne n2) {
                    new GrowableLongTrieSetN4(lookupTable, n1, newN2, n3, n4)
                } else {
                    this
                }
            case 3 ⇒
                val newN3 = n3 + (v, level + 3)
                if (newN3 ne n3) {
                    new GrowableLongTrieSetN4(lookupTable, n1, n2, newN3, n4)
                } else {
                    this
                }
            case 4 ⇒
                val newN4 = n4 + (v, level + 3)
                if (newN4 ne n4) {
                    new GrowableLongTrieSetN4(lookupTable, n1, n2, n3, newN4)
                } else {
                    this
                }
        }
    }
}

private[immutable] final class GrowableLongTrieSetN5(
        val lookupTable: Int,
        n1:              GrowableLongTrieSetN,
        n2:              GrowableLongTrieSetN,
        n3:              GrowableLongTrieSetN,
        n4:              GrowableLongTrieSetN,
        n5:              GrowableLongTrieSetN
) extends GrowableLongTrieSetN2x {

    override def foreach[U](f: Long ⇒ U): Unit = {
        n1.foreach(f)
        n2.foreach(f)
        n3.foreach(f)
        n4.foreach(f)
        n5.foreach(f)
    }

    override def node(index: Int): GrowableLongTrieSetN = {
        index match {
            case 1 ⇒ n1
            case 2 ⇒ n2
            case 3 ⇒ n3
            case 4 ⇒ n4
            case 5 ⇒ n5
            case _ ⇒ null
        }
    }

    override private[immutable] def +(v: Long, level: Int): GrowableLongTrieSetN = {
        val vBits = ((v >> level) & 7L).toInt
        val vIndex = (lookupTable >> (vBits * 4)) & 15
        vIndex match {
            case 0 ⇒
                val newLookupTable = lookupTable | (6 << (vBits * 4))
                new GrowableLongTrieSetN6(newLookupTable, n1, n2, n3, n4, n5, GrowableLongTrieSet1(v))
            case 1 ⇒
                val newN1 = n1 + (v, level + 3)
                if (newN1 ne n1) {
                    new GrowableLongTrieSetN5(lookupTable, newN1, n2, n3, n4, n5)
                } else {
                    this
                }
            case 2 ⇒
                val newN2 = n2 + (v, level + 3)
                if (newN2 ne n2) {
                    new GrowableLongTrieSetN5(lookupTable, n1, newN2, n3, n4, n5)
                } else {
                    this
                }
            case 3 ⇒
                val newN3 = n3 + (v, level + 3)
                if (newN3 ne n3) {
                    new GrowableLongTrieSetN5(lookupTable, n1, n2, newN3, n4, n5)
                } else {
                    this
                }
            case 4 ⇒
                val newN4 = n4 + (v, level + 3)
                if (newN4 ne n4) {
                    new GrowableLongTrieSetN5(lookupTable, n1, n2, n3, newN4, n5)
                } else {
                    this
                }
            case 5 ⇒
                val newN5 = n5 + (v, level + 3)
                if (newN5 ne n5) {
                    new GrowableLongTrieSetN5(lookupTable, n1, n2, n3, n4, newN5)
                } else {
                    this
                }
        }
    }
}

private[immutable] final class GrowableLongTrieSetN6(
        val lookupTable: Int,
        n1:              GrowableLongTrieSetN,
        n2:              GrowableLongTrieSetN,
        n3:              GrowableLongTrieSetN,
        n4:              GrowableLongTrieSetN,
        n5:              GrowableLongTrieSetN,
        n6:              GrowableLongTrieSetN
) extends GrowableLongTrieSetN2x {

    override def foreach[U](f: Long ⇒ U): Unit = {
        n1.foreach(f)
        n2.foreach(f)
        n3.foreach(f)
        n4.foreach(f)
        n5.foreach(f)
        n6.foreach(f)
    }

    override def node(index: Int): GrowableLongTrieSetN = {
        index match {
            case 1 ⇒ n1
            case 2 ⇒ n2
            case 3 ⇒ n3
            case 4 ⇒ n4
            case 5 ⇒ n5
            case 6 ⇒ n6
            case _ ⇒ null
        }
    }

    override private[immutable] def +(v: Long, level: Int): GrowableLongTrieSetN = {
        val vBits = ((v >> level) & 7L).toInt
        val vIndex = (lookupTable >> (vBits * 4)) & 15
        vIndex match {
            case 0 ⇒
                val newLookupTable = lookupTable | (7 << (vBits * 4))
                new GrowableLongTrieSetN7(newLookupTable, n1, n2, n3, n4, n5, n6, GrowableLongTrieSet1(v))
            case 1 ⇒
                val newN1 = n1 + (v, level + 3)
                if (newN1 ne n1) {
                    new GrowableLongTrieSetN6(lookupTable, newN1, n2, n3, n4, n5, n6)
                } else {
                    this
                }
            case 2 ⇒
                val newN2 = n2 + (v, level + 3)
                if (newN2 ne n2) {
                    new GrowableLongTrieSetN6(lookupTable, n1, newN2, n3, n4, n5, n6)
                } else {
                    this
                }
            case 3 ⇒
                val newN3 = n3 + (v, level + 3)
                if (newN3 ne n3) {
                    new GrowableLongTrieSetN6(lookupTable, n1, n2, newN3, n4, n5, n6)
                } else {
                    this
                }
            case 4 ⇒
                val newN4 = n4 + (v, level + 3)
                if (newN4 ne n4) {
                    new GrowableLongTrieSetN6(lookupTable, n1, n2, n3, newN4, n5, n6)
                } else {
                    this
                }
            case 5 ⇒
                val newN5 = n5 + (v, level + 3)
                if (newN5 ne n5) {
                    new GrowableLongTrieSetN6(lookupTable, n1, n2, n3, n4, newN5, n6)
                } else {
                    this
                }
            case 6 ⇒
                val newN6 = n6 + (v, level + 3)
                if (newN6 ne n6) {
                    new GrowableLongTrieSetN6(lookupTable, n1, n2, n3, n4, n5, newN6)
                } else {
                    this
                }
        }
    }
}

private[immutable] final class GrowableLongTrieSetN7(
        val lookupTable: Int,
        n1:              GrowableLongTrieSetN,
        n2:              GrowableLongTrieSetN,
        n3:              GrowableLongTrieSetN,
        n4:              GrowableLongTrieSetN,
        n5:              GrowableLongTrieSetN,
        n6:              GrowableLongTrieSetN,
        n7:              GrowableLongTrieSetN
) extends GrowableLongTrieSetN2x {

    override def foreach[U](f: Long ⇒ U): Unit = {
        n1.foreach(f)
        n2.foreach(f)
        n3.foreach(f)
        n4.foreach(f)
        n5.foreach(f)
        n6.foreach(f)
        n7.foreach(f)
    }

    override def node(index: Int): GrowableLongTrieSetN = {
        index match {
            case 1 ⇒ n1
            case 2 ⇒ n2
            case 3 ⇒ n3
            case 4 ⇒ n4
            case 5 ⇒ n5
            case 6 ⇒ n6
            case 7 ⇒ n7
            case _ ⇒ null
        }
    }

    override private[immutable] def +(v: Long, level: Int): GrowableLongTrieSetN = {
        val vBits = ((v >> level) & 7L).toInt
        val vIndex = (lookupTable >> (vBits * 4)) & 15
        vIndex match {
            case 0 ⇒
                val newLookupTable = lookupTable | (8 << (vBits * 4))
                new GrowableLongTrieSetN8(newLookupTable, n1, n2, n3, n4, n5, n6, n7, GrowableLongTrieSet1(v))
            case 1 ⇒
                val newN1 = n1 + (v, level + 3)
                if (newN1 ne n1) {
                    new GrowableLongTrieSetN7(lookupTable, newN1, n2, n3, n4, n5, n6, n7)
                } else {
                    this
                }
            case 2 ⇒
                val newN2 = n2 + (v, level + 3)
                if (newN2 ne n2) {
                    new GrowableLongTrieSetN7(lookupTable, n1, newN2, n3, n4, n5, n6, n7)
                } else {
                    this
                }
            case 3 ⇒
                val newN3 = n3 + (v, level + 3)
                if (newN3 ne n3) {
                    new GrowableLongTrieSetN7(lookupTable, n1, n2, newN3, n4, n5, n6, n7)
                } else {
                    this
                }
            case 4 ⇒
                val newN4 = n4 + (v, level + 3)
                if (newN4 ne n4) {
                    new GrowableLongTrieSetN7(lookupTable, n1, n2, n3, newN4, n5, n6, n7)
                } else {
                    this
                }
            case 5 ⇒
                val newN5 = n5 + (v, level + 3)
                if (newN5 ne n5) {
                    new GrowableLongTrieSetN7(lookupTable, n1, n2, n3, n4, newN5, n6, n7)
                } else {
                    this
                }
            case 6 ⇒
                val newN6 = n6 + (v, level + 3)
                if (newN6 ne n6) {
                    new GrowableLongTrieSetN7(lookupTable, n1, n2, n3, n4, n5, newN6, n7)
                } else {
                    this
                }
            case 7 ⇒
                val newN7 = n7 + (v, level + 3)
                if (newN7 ne n7) {
                    new GrowableLongTrieSetN7(lookupTable, n1, n2, n3, n4, n5, n6, newN7)
                } else {
                    this
                }

        }
    }
}

private[immutable] final class GrowableLongTrieSetN8(
        val lookupTable: Int,
        n1:              GrowableLongTrieSetN,
        n2:              GrowableLongTrieSetN,
        n3:              GrowableLongTrieSetN,
        n4:              GrowableLongTrieSetN,
        n5:              GrowableLongTrieSetN,
        n6:              GrowableLongTrieSetN,
        n7:              GrowableLongTrieSetN,
        n8:              GrowableLongTrieSetN
) extends GrowableLongTrieSetN2x {

    override def foreach[U](f: Long ⇒ U): Unit = {
        n1.foreach(f)
        n2.foreach(f)
        n3.foreach(f)
        n4.foreach(f)
        n5.foreach(f)
        n6.foreach(f)
        n7.foreach(f)
        n8.foreach(f)
    }

    override def node(index: Int): GrowableLongTrieSetN = {
        index match {
            case 1 ⇒ n1
            case 2 ⇒ n2
            case 3 ⇒ n3
            case 4 ⇒ n4
            case 5 ⇒ n5
            case 6 ⇒ n6
            case 7 ⇒ n7
            case 8 ⇒ n8
        }
    }

    override private[immutable] def +(v: Long, level: Int): GrowableLongTrieSetN = {
        val vBits = ((v >> level) & 7L).toInt
        val vIndex = (lookupTable >> (vBits * 4)) & 15
        vIndex match {
            case 1 ⇒
                val newN1 = n1 + (v, level + 3)
                if (newN1 ne n1) {
                    new GrowableLongTrieSetN8(lookupTable, newN1, n2, n3, n4, n5, n6, n7, n8)
                } else {
                    this
                }
            case 2 ⇒
                val newN2 = n2 + (v, level + 3)
                if (newN2 ne n2) {
                    new GrowableLongTrieSetN8(lookupTable, n1, newN2, n3, n4, n5, n6, n7, n8)
                } else {
                    this
                }
            case 3 ⇒
                val newN3 = n3 + (v, level + 3)
                if (newN3 ne n3) {
                    new GrowableLongTrieSetN8(lookupTable, n1, n2, newN3, n4, n5, n6, n7, n8)
                } else {
                    this
                }
            case 4 ⇒
                val newN4 = n4 + (v, level + 3)
                if (newN4 ne n4) {
                    new GrowableLongTrieSetN8(lookupTable, n1, n2, n3, newN4, n5, n6, n7, n8)
                } else {
                    this
                }
            case 5 ⇒
                val newN5 = n5 + (v, level + 3)
                if (newN5 ne n5) {
                    new GrowableLongTrieSetN8(lookupTable, n1, n2, n3, n4, newN5, n6, n7, n8)
                } else {
                    this
                }
            case 6 ⇒
                val newN6 = n6 + (v, level + 3)
                if (newN6 ne n6) {
                    new GrowableLongTrieSetN8(lookupTable, n1, n2, n3, n4, n5, newN6, n7, n8)
                } else {
                    this
                }
            case 7 ⇒
                val newN7 = n7 + (v, level + 3)
                if (newN7 ne n7) {
                    new GrowableLongTrieSetN8(lookupTable, n1, n2, n3, n4, n5, n6, newN7, n8)
                } else {
                    this
                }
            case 8 ⇒
                val newN8 = n8 + (v, level + 3)
                if (newN8 ne n8) {
                    new GrowableLongTrieSetN8(lookupTable, n1, n2, n3, n4, n5, n6, n7, newN8)
                } else {
                    this
                }

        }
    }
}
