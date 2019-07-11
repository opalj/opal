/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package collection
package immutable

sealed abstract class GrowableIntTrieSet { intSet ⇒

    def isEmpty: Boolean
    def isSingletonSet: Boolean
    def size: Int
    def contains(value: Int): Boolean
    def foreach[U](f: Int ⇒ U): Unit
    def iterator: IntIterator
    def +(value: Int): GrowableIntTrieSet

    final override def equals(other: Any): Boolean = {
        other match {
            case that: GrowableIntTrieSet ⇒ this.equals(that)
            case _                        ⇒ false
        }
    }

    def equals(other: GrowableIntTrieSet): Boolean
}

object GrowableIntTrieSet {

    def empty: GrowableIntTrieSet = GrowableIntTrieSet0

}

/** The node of an IntTrie. */
private[immutable] sealed trait GrowableIntTrieSetN {

    def foreach[U](f: Int ⇒ U): Unit

    //
    // IMPLEMENTATION "INTERNAL" METHODS
    //
    private[immutable] def +(i: Int, level: Int): GrowableIntTrieSetN
    private[immutable] def contains(value: Int, key: Int): Boolean
    private[immutable] def toString(indent: Int): String

    final private[immutable] def bitsToString(bits: Int): String = {
        bits.toBinaryString.reverse.padTo(3, '0').reverse
    }
}

/** The (potential) leaves of an IntTrie. */
private[immutable] sealed abstract class GrowableIntTrieSetL extends GrowableIntTrieSet with GrowableIntTrieSetN {

    final override private[immutable] def contains(value: Int, key: Int): Boolean = {
        this.contains(value)
    }

    final override private[immutable] def toString(indent: Int): String = {
        (" " * indent) + toString()
    }
}

case object GrowableIntTrieSet0 extends GrowableIntTrieSet {
    override def isSingletonSet: Boolean = false
    override def isEmpty: Boolean = true
    override def size: Int = 0
    override def foreach[U](f: Int ⇒ U): Unit = {}
    override def +(i: Int): GrowableIntTrieSet1 = GrowableIntTrieSet1(i)
    override def iterator: IntIterator = IntIterator.empty
    override def contains(value: Int): Boolean = false

    override def equals(other: GrowableIntTrieSet): Boolean = other eq this
    override def hashCode: Int = 0
    override def toString: String = "GrowableIntTrieSet()"
}

final case class GrowableIntTrieSet1 private (i: Int) extends GrowableIntTrieSetL {
    override def isEmpty: Boolean = false
    override def isSingletonSet: Boolean = true
    override def size: Int = 1
    override def foreach[U](f: Int ⇒ U): Unit = { f(i) }
    override def +(i: Int): GrowableIntTrieSetL = if (this.i == i) this else new GrowableIntTrieSet2(this.i, i)
    override def iterator: IntIterator = IntIterator(i)
    override def contains(value: Int): Boolean = value == i

    override def equals(other: GrowableIntTrieSet): Boolean = {
        (other eq this) || (other match {
            case that: GrowableIntTrieSet1 ⇒ this.i == that.i
            case that                      ⇒ false
        })
    }

    override def hashCode: Int = 31 + i

    override def toString: String = s"GrowableIntTrieSet($i)"

    override private[immutable] def +(i: Int, level: Int): GrowableIntTrieSetN = this.+(i)
}

object GrowableIntTrieSet1 {

    // The preallocation of the data structures costs ~2Mb memory;
    // however, we use it as the backbone infrastructure for storing CFGs and
    // def-use information; in both cases, we generally require HUGE numbers
    // of such sets in the preconfigured ranges and therefore we avoid allocating
    // several hundred million instances (in case of a thorough analysis of the
    // JDK) and corresponding memory.
    val Cache1LowerBound = -100000 - (48 * 1024) // inclusive
    val Cache1UpperBound = -99999 // exclusive
    val Cache2LowerBound = -2048 // inclusive
    val Cache2UpperBound = 48 * 1024 // exclusive

    private[this] val cache1: Array[GrowableIntTrieSet1] = {
        val a = new Array[GrowableIntTrieSet1](Cache1UpperBound + (-Cache1LowerBound))
        var v = Cache1LowerBound
        var index = 0
        while (v < Cache1UpperBound) {
            a(index) = new GrowableIntTrieSet1(v)
            index += 1
            v += 1
        }
        a
    }

    private[this] val cache2: Array[GrowableIntTrieSet1] = {
        val a = new Array[GrowableIntTrieSet1](Cache2UpperBound + (-Cache2LowerBound))
        var v = Cache2LowerBound
        var index = 0
        while (v < Cache2UpperBound) {
            a(index) = new GrowableIntTrieSet1(v)
            index += 1
            v += 1
        }
        a
    }

    def apply(v: Int): GrowableIntTrieSet1 = {
        if (v >= Cache1LowerBound && v < Cache1UpperBound) {
            cache1(v + (-Cache1LowerBound))
        } else if (v >= Cache2LowerBound && v < Cache2UpperBound) {
            cache2(v + (-Cache2LowerBound))
        } else {
            new GrowableIntTrieSet1(v)
        }
    }
}

/**
 * Represents an ordered set of two values where i1 has to be smaller than i2.
 */
private[immutable] final class GrowableIntTrieSet2 private[immutable] (
        val i1: Int, val i2: Int
) extends GrowableIntTrieSetL {

    override def isEmpty: Boolean = false
    override def isSingletonSet: Boolean = false
    override def size: Int = 2
    override def iterator: IntIterator = IntIterator(i1, i2)
    override def foreach[U](f: Int ⇒ U): Unit = { f(i1); f(i2) }
    override def +(i: Int): GrowableIntTrieSetL = if (i1 == i || i2 == i) this else new GrowableIntTrieSet3(i1, i2, i)
    override def contains(value: Int): Boolean = value == i1 || value == i2

    override def equals(other: GrowableIntTrieSet): Boolean = {
        (other eq this) || (
            other match {
                case that: GrowableIntTrieSet2 ⇒ this.i1 == that.i1 && this.i2 == that.i2
                case that                      ⇒ false
            }
        )
    }

    override def hashCode: Int = 31 * (31 + i1) + i2

    override def toString: String = s"GrowableIntTrieSet($i1, $i2)"

    override private[immutable] def +(i: Int, level: Int): GrowableIntTrieSetN = this.+(i)
}

/**
 * Represents an ordered set of three int values: i1 < i2 < i3.
 */
private[immutable] final class GrowableIntTrieSet3 private[immutable] (
        val i1: Int, val i2: Int, val i3: Int
) extends GrowableIntTrieSetL {

    override def size: Int = 3
    override def isEmpty: Boolean = false
    override def isSingletonSet: Boolean = false
    override def contains(value: Int): Boolean = value == i1 || value == i2 || value == i3
    override def iterator: IntIterator = IntIterator(i1, i2, i3)
    override def foreach[U](f: Int ⇒ U): Unit = { f(i1); f(i2); f(i3) }

    override def +(i: Int): GrowableIntTrieSet = {
        if (i == i1 || i == i2 || i == i3)
            this
        else
            new LargeGrowableIntTrieSet(4, this + (i, 0))
    }

    override def equals(other: GrowableIntTrieSet): Boolean = {
        (other eq this) || (
            other match {
                case that: GrowableIntTrieSet3 ⇒
                    this.i1 == that.i1 && this.i2 == that.i2 && this.i3 == that.i3
                case that ⇒
                    that.size == 3 && that.contains(i1) && that.contains(i2) && that.contains(i3)
            }
        )
    }

    override def hashCode: Int = 31 * (31 * (31 + i1) + i2) + i3 // compatible to Arrays.hashCode

    override def toString: String = s"GrowableIntTrieSet($i1, $i2, $i3)"

    override private[immutable] def +(i: Int, level: Int): GrowableIntTrieSetN = {
        if (i == i1 || i == i2 || i == i3)
            this
        else {
            val l = GrowableIntTrieSet1(i)
            var r: GrowableIntTrieSetN = new GrowableIntTrieSetN1((i >> level) & 7, l)
            r = r + (i1, level)
            r = r + (i2, level)
            r = r + (i3, level)
            r
        }
    }
}

private[immutable] class LargeGrowableIntTrieSet(
        val size: Int,
        root:     GrowableIntTrieSetN
) extends GrowableIntTrieSet {

    assert(size >= 4)

    override def isEmpty: Boolean = false
    override def isSingletonSet: Boolean = false
    override def contains(value: Int): Boolean = {
        root.contains(value, value)
    }
    override def foreach[U](f: Int ⇒ U): Unit = {
        root.foreach(f)
    }

    override def iterator: IntIterator = ???

    override def equals(other: GrowableIntTrieSet): Boolean = ??? /*{
        (other eq this) || {
          other.size == this.size &&
            ???
        }
    }*/

    override def +(i: Int): GrowableIntTrieSet = {
        val root = this.root
        val newRoot = root + (i, 0)
        if (newRoot ne root) {
            new LargeGrowableIntTrieSet(size + 1, newRoot)
        } else {
            this
        }
    }

    override def toString: String = s"GrowableIntTrieSet(#$size, ${root.toString(1)})"

}

private[immutable] final class GrowableIntTrieSetN1(
        n1Bits: Int,
        n1:     GrowableIntTrieSetN
) extends GrowableIntTrieSetN {

    override def foreach[U](f: Int ⇒ U): Unit = n1.foreach(f)

    override private[immutable] def contains(value: Int, key: Int): Boolean = {
        if (n1Bits == (key & 7)) {
            n1.contains(value, key >> 3)
        } else {
            false
        }
    }

    override private[immutable] def +(v: Int, level: Int): GrowableIntTrieSetN = {
        val vBits = (v >> level) & 7
        if (vBits == n1Bits) {
            val newN1 = n1 + (v, level + 3)
            if (newN1 ne n1) {
                new GrowableIntTrieSetN1(n1Bits, newN1)
            } else {
                this
            }
        } else {
            val lookupTable = 1 << (n1Bits * 4) | 2 << (vBits * 4)
            new GrowableIntTrieSetN2(lookupTable, n1, GrowableIntTrieSet1(v))
        }
    }

    override def toString(indent: Int): String = {
        s"N(${bitsToString(n1Bits)}=>${n1.toString(indent + 1)})"
    }

}

private[immutable] abstract class GrowableIntTrieSetN2x extends GrowableIntTrieSetN {

    // The mapping between the three (relevant) bits of the value to the slot where the value
    // is stored.
    // def lookupTable : Int
    val lookupTable: Int

    // The index is "1" based.
    def node(index: Int): GrowableIntTrieSetN

    final override private[immutable] def contains(v: Int, key: Int): Boolean = {
        val vBits = key & 7
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

private[immutable] final class GrowableIntTrieSetN2(
        val lookupTable: Int,
        n1:              GrowableIntTrieSetN,
        n2:              GrowableIntTrieSetN
) extends GrowableIntTrieSetN2x {

    override def node(index: Int): GrowableIntTrieSetN = {
        index match {
            case 1 ⇒ n1
            case 2 ⇒ n2
            case _ ⇒ null
        }
    }

    override def foreach[U](f: Int ⇒ U): Unit = {
        n1.foreach(f)
        n2.foreach(f)
    }

    override private[immutable] def +(v: Int, level: Int): GrowableIntTrieSetN = {
        val vBits = (v >> level) & 7
        val vIndex = (lookupTable >> (vBits * 4)) & 15
        vIndex match {
            case 0 ⇒
                val newLookupTable = lookupTable | (3 << (vBits * 4))
                new GrowableIntTrieSetN3(newLookupTable, n1, n2, GrowableIntTrieSet1(v))
            case 1 ⇒
                val newN1 = n1 + (v, level + 3)
                if (newN1 ne n1) {
                    new GrowableIntTrieSetN2(lookupTable, newN1, n2)
                } else {
                    this
                }
            case 2 ⇒
                val newN2 = n2 + (v, level + 3)
                if (newN2 ne n2) {
                    new GrowableIntTrieSetN2(lookupTable, n1, newN2)
                } else {
                    this
                }
        }
    }

}

private[immutable] final class GrowableIntTrieSetN3(
        val lookupTable: Int,
        n1:              GrowableIntTrieSetN,
        n2:              GrowableIntTrieSetN,
        n3:              GrowableIntTrieSetN
) extends GrowableIntTrieSetN2x {

    override def foreach[U](f: Int ⇒ U): Unit = {
        n1.foreach(f)
        n2.foreach(f)
        n3.foreach(f)
    }

    override def node(index: Int): GrowableIntTrieSetN = {
        index match {
            case 1 ⇒ n1
            case 2 ⇒ n2
            case 3 ⇒ n3
            case _ ⇒ null
        }
    }

    override private[immutable] def +(v: Int, level: Int): GrowableIntTrieSetN = {
        val vBits = (v >> level) & 7
        val vIndex = (lookupTable >> (vBits * 4)) & 15
        vIndex match {
            case 0 ⇒
                val newLookupTable = lookupTable | (4 << (vBits * 4))
                new GrowableIntTrieSetN4(newLookupTable, n1, n2, n3, GrowableIntTrieSet1(v))
            case 1 ⇒
                val newN1 = n1 + (v, level + 3)
                if (newN1 ne n1) {
                    new GrowableIntTrieSetN3(lookupTable, newN1, n2, n3)
                } else {
                    this
                }
            case 2 ⇒
                val newN2 = n2 + (v, level + 3)
                if (newN2 ne n2) {
                    new GrowableIntTrieSetN3(lookupTable, n1, newN2, n3)
                } else {
                    this
                }
            case 3 ⇒
                val newN3 = n3 + (v, level + 3)
                if (newN3 ne n3) {
                    new GrowableIntTrieSetN3(lookupTable, n1, n2, newN3)
                } else {
                    this
                }
        }
    }
}

private[immutable] final class GrowableIntTrieSetN4(
        val lookupTable: Int,
        n1:              GrowableIntTrieSetN,
        n2:              GrowableIntTrieSetN,
        n3:              GrowableIntTrieSetN,
        n4:              GrowableIntTrieSetN
) extends GrowableIntTrieSetN2x {

    override def foreach[U](f: Int ⇒ U): Unit = {
        n1.foreach(f)
        n2.foreach(f)
        n3.foreach(f)
        n4.foreach(f)
    }

    override def node(index: Int): GrowableIntTrieSetN = {
        index match {
            case 1 ⇒ n1
            case 2 ⇒ n2
            case 3 ⇒ n3
            case 4 ⇒ n4
            case _ ⇒ null
        }
    }

    override private[immutable] def +(v: Int, level: Int): GrowableIntTrieSetN = {
        val vBits = (v >> level) & 7
        val vIndex = (lookupTable >> (vBits * 4)) & 15
        vIndex match {
            case 0 ⇒
                val newLookupTable = lookupTable | (5 << (vBits * 4))
                new GrowableIntTrieSetN5(newLookupTable, n1, n2, n3, n4, GrowableIntTrieSet1(v))
            case 1 ⇒
                val newN1 = n1 + (v, level + 3)
                if (newN1 ne n1) {
                    new GrowableIntTrieSetN4(lookupTable, newN1, n2, n3, n4)
                } else {
                    this
                }
            case 2 ⇒
                val newN2 = n2 + (v, level + 3)
                if (newN2 ne n2) {
                    new GrowableIntTrieSetN4(lookupTable, n1, newN2, n3, n4)
                } else {
                    this
                }
            case 3 ⇒
                val newN3 = n3 + (v, level + 3)
                if (newN3 ne n3) {
                    new GrowableIntTrieSetN4(lookupTable, n1, n2, newN3, n4)
                } else {
                    this
                }
            case 4 ⇒
                val newN4 = n4 + (v, level + 3)
                if (newN4 ne n4) {
                    new GrowableIntTrieSetN4(lookupTable, n1, n2, n3, newN4)
                } else {
                    this
                }
        }
    }
}

private[immutable] final class GrowableIntTrieSetN5(
        val lookupTable: Int,
        n1:              GrowableIntTrieSetN,
        n2:              GrowableIntTrieSetN,
        n3:              GrowableIntTrieSetN,
        n4:              GrowableIntTrieSetN,
        n5:              GrowableIntTrieSetN
) extends GrowableIntTrieSetN2x {

    override def foreach[U](f: Int ⇒ U): Unit = {
        n1.foreach(f)
        n2.foreach(f)
        n3.foreach(f)
        n4.foreach(f)
        n5.foreach(f)
    }

    override def node(index: Int): GrowableIntTrieSetN = {
        index match {
            case 1 ⇒ n1
            case 2 ⇒ n2
            case 3 ⇒ n3
            case 4 ⇒ n4
            case 5 ⇒ n5
            case _ ⇒ null
        }
    }

    override private[immutable] def +(v: Int, level: Int): GrowableIntTrieSetN = {
        val vBits = (v >> level) & 7
        val vIndex = (lookupTable >> (vBits * 4)) & 15
        vIndex match {
            case 0 ⇒
                val newLookupTable = lookupTable | (6 << (vBits * 4))
                new GrowableIntTrieSetN6(newLookupTable, n1, n2, n3, n4, n5, GrowableIntTrieSet1(v))
            case 1 ⇒
                val newN1 = n1 + (v, level + 3)
                if (newN1 ne n1) {
                    new GrowableIntTrieSetN5(lookupTable, newN1, n2, n3, n4, n5)
                } else {
                    this
                }
            case 2 ⇒
                val newN2 = n2 + (v, level + 3)
                if (newN2 ne n2) {
                    new GrowableIntTrieSetN5(lookupTable, n1, newN2, n3, n4, n5)
                } else {
                    this
                }
            case 3 ⇒
                val newN3 = n3 + (v, level + 3)
                if (newN3 ne n3) {
                    new GrowableIntTrieSetN5(lookupTable, n1, n2, newN3, n4, n5)
                } else {
                    this
                }
            case 4 ⇒
                val newN4 = n4 + (v, level + 3)
                if (newN4 ne n4) {
                    new GrowableIntTrieSetN5(lookupTable, n1, n2, n3, newN4, n5)
                } else {
                    this
                }
            case 5 ⇒
                val newN5 = n5 + (v, level + 3)
                if (newN5 ne n5) {
                    new GrowableIntTrieSetN5(lookupTable, n1, n2, n3, n4, newN5)
                } else {
                    this
                }
        }
    }
}

private[immutable] final class GrowableIntTrieSetN6(
        val lookupTable: Int,
        n1:              GrowableIntTrieSetN,
        n2:              GrowableIntTrieSetN,
        n3:              GrowableIntTrieSetN,
        n4:              GrowableIntTrieSetN,
        n5:              GrowableIntTrieSetN,
        n6:              GrowableIntTrieSetN
) extends GrowableIntTrieSetN2x {

    override def foreach[U](f: Int ⇒ U): Unit = {
        n1.foreach(f)
        n2.foreach(f)
        n3.foreach(f)
        n4.foreach(f)
        n5.foreach(f)
        n6.foreach(f)
    }

    override def node(index: Int): GrowableIntTrieSetN = {
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

    override private[immutable] def +(v: Int, level: Int): GrowableIntTrieSetN = {
        val vBits = (v >> level) & 7
        val vIndex = (lookupTable >> (vBits * 4)) & 15
        vIndex match {
            case 0 ⇒
                val newLookupTable = lookupTable | (7 << (vBits * 4))
                new GrowableIntTrieSetN7(newLookupTable, n1, n2, n3, n4, n5, n6, GrowableIntTrieSet1(v))
            case 1 ⇒
                val newN1 = n1 + (v, level + 3)
                if (newN1 ne n1) {
                    new GrowableIntTrieSetN6(lookupTable, newN1, n2, n3, n4, n5, n6)
                } else {
                    this
                }
            case 2 ⇒
                val newN2 = n2 + (v, level + 3)
                if (newN2 ne n2) {
                    new GrowableIntTrieSetN6(lookupTable, n1, newN2, n3, n4, n5, n6)
                } else {
                    this
                }
            case 3 ⇒
                val newN3 = n3 + (v, level + 3)
                if (newN3 ne n3) {
                    new GrowableIntTrieSetN6(lookupTable, n1, n2, newN3, n4, n5, n6)
                } else {
                    this
                }
            case 4 ⇒
                val newN4 = n4 + (v, level + 3)
                if (newN4 ne n4) {
                    new GrowableIntTrieSetN6(lookupTable, n1, n2, n3, newN4, n5, n6)
                } else {
                    this
                }
            case 5 ⇒
                val newN5 = n5 + (v, level + 3)
                if (newN5 ne n5) {
                    new GrowableIntTrieSetN6(lookupTable, n1, n2, n3, n4, newN5, n6)
                } else {
                    this
                }
            case 6 ⇒
                val newN6 = n6 + (v, level + 3)
                if (newN6 ne n6) {
                    new GrowableIntTrieSetN6(lookupTable, n1, n2, n3, n4, n5, newN6)
                } else {
                    this
                }
        }
    }
}

private[immutable] final class GrowableIntTrieSetN7(
        val lookupTable: Int,
        n1:              GrowableIntTrieSetN,
        n2:              GrowableIntTrieSetN,
        n3:              GrowableIntTrieSetN,
        n4:              GrowableIntTrieSetN,
        n5:              GrowableIntTrieSetN,
        n6:              GrowableIntTrieSetN,
        n7:              GrowableIntTrieSetN
) extends GrowableIntTrieSetN2x {

    override def foreach[U](f: Int ⇒ U): Unit = {
        n1.foreach(f)
        n2.foreach(f)
        n3.foreach(f)
        n4.foreach(f)
        n5.foreach(f)
        n6.foreach(f)
        n7.foreach(f)
    }

    override def node(index: Int): GrowableIntTrieSetN = {
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

    override private[immutable] def +(v: Int, level: Int): GrowableIntTrieSetN = {
        val vBits = (v >> level) & 7
        val vIndex = (lookupTable >> (vBits * 4)) & 15
        vIndex match {
            case 0 ⇒
                val newLookupTable = lookupTable | (8 << (vBits * 4))
                new GrowableIntTrieSetN8(newLookupTable, n1, n2, n3, n4, n5, n6, n7, GrowableIntTrieSet1(v))
            case 1 ⇒
                val newN1 = n1 + (v, level + 3)
                if (newN1 ne n1) {
                    new GrowableIntTrieSetN7(lookupTable, newN1, n2, n3, n4, n5, n6, n7)
                } else {
                    this
                }
            case 2 ⇒
                val newN2 = n2 + (v, level + 3)
                if (newN2 ne n2) {
                    new GrowableIntTrieSetN7(lookupTable, n1, newN2, n3, n4, n5, n6, n7)
                } else {
                    this
                }
            case 3 ⇒
                val newN3 = n3 + (v, level + 3)
                if (newN3 ne n3) {
                    new GrowableIntTrieSetN7(lookupTable, n1, n2, newN3, n4, n5, n6, n7)
                } else {
                    this
                }
            case 4 ⇒
                val newN4 = n4 + (v, level + 3)
                if (newN4 ne n4) {
                    new GrowableIntTrieSetN7(lookupTable, n1, n2, n3, newN4, n5, n6, n7)
                } else {
                    this
                }
            case 5 ⇒
                val newN5 = n5 + (v, level + 3)
                if (newN5 ne n5) {
                    new GrowableIntTrieSetN7(lookupTable, n1, n2, n3, n4, newN5, n6, n7)
                } else {
                    this
                }
            case 6 ⇒
                val newN6 = n6 + (v, level + 3)
                if (newN6 ne n6) {
                    new GrowableIntTrieSetN7(lookupTable, n1, n2, n3, n4, n5, newN6, n7)
                } else {
                    this
                }
            case 7 ⇒
                val newN7 = n7 + (v, level + 3)
                if (newN7 ne n7) {
                    new GrowableIntTrieSetN7(lookupTable, n1, n2, n3, n4, n5, n6, newN7)
                } else {
                    this
                }

        }
    }
}

private[immutable] final class GrowableIntTrieSetN8(
        val lookupTable: Int,
        n1:              GrowableIntTrieSetN,
        n2:              GrowableIntTrieSetN,
        n3:              GrowableIntTrieSetN,
        n4:              GrowableIntTrieSetN,
        n5:              GrowableIntTrieSetN,
        n6:              GrowableIntTrieSetN,
        n7:              GrowableIntTrieSetN,
        n8:              GrowableIntTrieSetN
) extends GrowableIntTrieSetN2x {

    override def foreach[U](f: Int ⇒ U): Unit = {
        n1.foreach(f)
        n2.foreach(f)
        n3.foreach(f)
        n4.foreach(f)
        n5.foreach(f)
        n6.foreach(f)
        n7.foreach(f)
        n8.foreach(f)
    }

    override def node(index: Int): GrowableIntTrieSetN = {
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

    override private[immutable] def +(v: Int, level: Int): GrowableIntTrieSetN = {
        val vBits = (v >> level) & 7
        val vIndex = (lookupTable >> (vBits * 4)) & 15
        vIndex match {
            case 1 ⇒
                val newN1 = n1 + (v, level + 3)
                if (newN1 ne n1) {
                    new GrowableIntTrieSetN8(lookupTable, newN1, n2, n3, n4, n5, n6, n7, n8)
                } else {
                    this
                }
            case 2 ⇒
                val newN2 = n2 + (v, level + 3)
                if (newN2 ne n2) {
                    new GrowableIntTrieSetN8(lookupTable, n1, newN2, n3, n4, n5, n6, n7, n8)
                } else {
                    this
                }
            case 3 ⇒
                val newN3 = n3 + (v, level + 3)
                if (newN3 ne n3) {
                    new GrowableIntTrieSetN8(lookupTable, n1, n2, newN3, n4, n5, n6, n7, n8)
                } else {
                    this
                }
            case 4 ⇒
                val newN4 = n4 + (v, level + 3)
                if (newN4 ne n4) {
                    new GrowableIntTrieSetN8(lookupTable, n1, n2, n3, newN4, n5, n6, n7, n8)
                } else {
                    this
                }
            case 5 ⇒
                val newN5 = n5 + (v, level + 3)
                if (newN5 ne n5) {
                    new GrowableIntTrieSetN8(lookupTable, n1, n2, n3, n4, newN5, n6, n7, n8)
                } else {
                    this
                }
            case 6 ⇒
                val newN6 = n6 + (v, level + 3)
                if (newN6 ne n6) {
                    new GrowableIntTrieSetN8(lookupTable, n1, n2, n3, n4, n5, newN6, n7, n8)
                } else {
                    this
                }
            case 7 ⇒
                val newN7 = n7 + (v, level + 3)
                if (newN7 ne n7) {
                    new GrowableIntTrieSetN8(lookupTable, n1, n2, n3, n4, n5, n6, newN7, n8)
                } else {
                    this
                }
            case 8 ⇒
                val newN8 = n8 + (v, level + 3)
                if (newN8 ne n8) {
                    new GrowableIntTrieSetN8(lookupTable, n1, n2, n3, n4, n5, n6, n7, newN8)
                } else {
                    this
                }

        }
    }
}
