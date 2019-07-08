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
    def forall(p: Long ⇒ Boolean): Boolean
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

    def apply(v: Long): GrowableLongTrieSet = new GrowableLongTrieSet1(v)
}

private[immutable] case object GrowableLongTrieSet0 extends GrowableLongTrieSet {
    override def isSingletonSet: Boolean = false
    override def isEmpty: Boolean = true
    override def size: Int = 0

    override def contains(value: Long): Boolean = false

    override def foreach[U](f: Long ⇒ U): Unit = {}
    override def forall(p: Long ⇒ Boolean): Boolean = true
    override def iterator: LongIterator = LongIterator.empty

    override def +(i: Long): GrowableLongTrieSet1 = new GrowableLongTrieSet1(i)

    override def equals(other: GrowableLongTrieSet): Boolean = other eq this
    override def hashCode: Int = 0
    override def toString: String = "GrowableLongTrieSet()"
}

/** A node of the trie. */
private[immutable] sealed trait GrowableLongTrieSetNode {

    def foreach[U](f: Long ⇒ U): Unit
    def forall(p: Long ⇒ Boolean): Boolean

    //
    // IMPLEMENTATION "INTERNAL" METHODS
    //
    private[immutable] def +(i: Long, level: Int): GrowableLongTrieSetNode
    private[immutable] def contains(value: Long, key: Long): Boolean
    private[immutable] def toString(indent: Int): String

    final private[immutable] def bitsToString(bits: Int): String = {
        bits.toBinaryString.reverse.padTo(3, '0').reverse
    }
}

/** The (potential) leaves of an IntTrie. */
private[immutable] sealed abstract class GrowableLongTrieSetLeaf
    extends GrowableLongTrieSet
    with GrowableLongTrieSetNode {

    /** Returns the nth value (0 based). */
    def apply(index: Int): Long
    def size: Int

    final override private[immutable] def contains(value: Long, key: Long): Boolean = {
        this.contains(value)
    }

    final override private[immutable] def toString(indent: Int): String = {
        (" " * indent) + toString()
    }
}

private[immutable] final class GrowableLongTrieSet1(val i1: Long) extends GrowableLongTrieSetLeaf {

    override def isEmpty: Boolean = false
    override def isSingletonSet: Boolean = true
    override def size: Int = 1

    override def apply(index: Int): Long = i1
    override def contains(i: Long): Boolean = i == i1

    override def foreach[U](f: Long ⇒ U): Unit = f(i1)
    override def forall(p: Long ⇒ Boolean): Boolean = p(i1)
    override def iterator: LongIterator = LongIterator(i1)

    override def +(i: Long): GrowableLongTrieSetLeaf = {
        val i1 = this.i1
        if (i1 == i)
            this
        else {
            if (i1 < i)
                new GrowableLongTrieSet2(i1, i)
            else
                new GrowableLongTrieSet2(i, i1)
        }
    }
    override private[immutable] def +(i: Long, level: Int): GrowableLongTrieSetNode = this.+(i)

    override def equals(other: GrowableLongTrieSet): Boolean = {
        (other eq this) || (other match {
            case that: GrowableLongTrieSet1 ⇒ this.i1 == that.i1
            case that                       ⇒ false
        })
    }
    override def hashCode: Int = 31 + lHashCode(i1)
    override def toString: String = s"GrowableLongTrieSet($i1)"

}

/**
 * Represents an ordered set of two values where i1 has to be smaller than i2.
 */
private[immutable] final class GrowableLongTrieSet2(
        val i1: Long, val i2: Long
) extends GrowableLongTrieSetLeaf {

    override def isEmpty: Boolean = false
    override def isSingletonSet: Boolean = false
    override def size: Int = 2

    override def apply(index: Int): Long = if (index == 0) i1 else i2
    override def contains(i: Long): Boolean = i == i1 || i == i2

    override def foreach[U](f: Long ⇒ U): Unit = { f(i1); f(i2) }
    override def forall(p: Long ⇒ Boolean): Boolean = { p(i1) && p(i2) }
    override def iterator: LongIterator = LongIterator(i1, i2)

    override def +(i: Long): GrowableLongTrieSetLeaf = {
        val i1 = this.i1
        val i2 = this.i2
        if (i1 == i || i2 == i)
            this
        else {
            if (i < i2) {
                if (i < i1) {
                    new GrowableLongTrieSet3(i, i1, i2)
                } else {
                    new GrowableLongTrieSet3(i1, i, i2)
                }
            } else {
                new GrowableLongTrieSet3(i1, i2, i)
            }

        }
    }
    override private[immutable] def +(i: Long, level: Int): GrowableLongTrieSetNode = this.+(i)

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

}

/**
 * Represents an ordered set of three int values: i1 < i2 < i3.
 */
private[immutable] final class GrowableLongTrieSet3(
        val i1: Long, val i2: Long, val i3: Long
) extends GrowableLongTrieSetLeaf {

    override def size: Int = 3
    override def isEmpty: Boolean = false
    override def isSingletonSet: Boolean = false

    override def apply(index: Int): Long = if (index == 0) i1 else if (index == 1) i2 else i3
    override def contains(i: Long): Boolean = i == i1 || i == i2 || i == i3

    override def foreach[U](f: Long ⇒ U): Unit = { f(i1); f(i2); f(i3) }
    override def forall(p: Long ⇒ Boolean): Boolean = { p(i1) && p(i2) && p(i3) }
    override def iterator: LongIterator = LongIterator(i1, i2, i3)

    override def +(i: Long): GrowableLongTrieSet = {
        if (i == i1 || i == i2 || i == i3)
            this
        else
            new GrowableLongTrieSetN(4, this.grow(i, 0))
    }
    override private[immutable] def +(i: Long, level: Int): GrowableLongTrieSetNode = {
        if (i == i1 || i == i2 || i == i3)
            this
        else {
            this.grow(i, level)
        }
    }
    private[this] def grow(i: Long, level: Int): GrowableLongTrieSetNode = {
        val l = new GrowableLongTrieSet1(i)
        var r: GrowableLongTrieSetNode = new GrowableLongTrieSetNode1(((i >> level) & 7L).toInt, l)
        r = r + (i1, level)
        r = r + (i2, level)
        r = r + (i3, level)
        r
    }

    override def equals(other: GrowableLongTrieSet): Boolean = {
        (other eq this) || (
            other match {
                case that: GrowableLongTrieSet3 ⇒ i1 == that.i1 && i2 == that.i2 && i3 == that.i3
                case _                          ⇒ false
            }
        )
    }
    override def hashCode: Int = 31 * (31 * (31 + lHashCode(i1)) + lHashCode(i2)) + lHashCode(i3)
    override def toString: String = s"GrowableLongTrieSet($i1, $i2, $i3)"

}

private[immutable] final class GrowableLongTrieSetN(
        final val size: Int,
        final val root: GrowableLongTrieSetNode
) extends GrowableLongTrieSet {

    // assert(size >= 4)

    override def isEmpty: Boolean = false
    override def isSingletonSet: Boolean = false
    override def contains(value: Long): Boolean = root.contains(value, value)
    override def foreach[U](f: Long ⇒ U): Unit = root.foreach(f)
    override def forall(p: Long ⇒ Boolean): Boolean = root.forall(p)

    override def iterator: LongIterator = ???
    /*
    override def iterator: LongIterator = new LongIterator {
            private[this] var leafNodes : GrowableLongTrieSetLeaf = null
            private[this] var index = 0
            private[this] val nodes = RefArrayStack(root,Math.min(16,size/2))
            private[this] def moveToNextLeafNode() : Unit = {
                innerNodes.pop() match {
                    case n : GrowableLongTrieSetLeaf =>
                        leafNode =
                        index = 0
                }
            }
            moveToNextLeafNode
            def hasNext: Boolean = leafNode ne null
            def next: Long = {
                var index = this.index
                val i = leafNode(index)
                index += 1
                if (index >= leafNode.size) {
                    moveToNextLeafNode()
                } else {
                    this.index = index
                }
                i
            }
    }
*/

    override def equals(other: GrowableLongTrieSet): Boolean = {
        (other eq this) || {
            other.size == this.size &&
                // Recall that the iteration order of the values is dependent on the
                // insertion order, but two sets should be considered equal even they
                // contain the same values - independent of the insertion order.
                this.forall(other.contains)
        }
    }

    override def +(i: Long): GrowableLongTrieSet = {
        val root = this.root
        val newRoot = root + (i, 0)
        if (newRoot ne root) {
            new GrowableLongTrieSetN(size + 1, newRoot)
        } else {
            this
        }
    }

    override def toString: String = s"GrowableLongTrieSet(#$size, ${root.toString(1)})"

}

private[immutable] final class GrowableLongTrieSetNode1(
        final val n1Bits: Int,
        final val n1:     GrowableLongTrieSetNode
) extends GrowableLongTrieSetNode {

    override def foreach[U](f: Long ⇒ U): Unit = n1.foreach(f)
    override def forall(p: Long ⇒ Boolean): Boolean = n1.forall(p)

    override private[immutable] def contains(value: Long, key: Long): Boolean = {
        if (n1Bits == (key & 7L).toInt) {
            n1.contains(value, key >> 3)
        } else {
            false
        }
    }

    override private[immutable] def +(v: Long, level: Int): GrowableLongTrieSetNode = {
        val vBits = ((v >> level) & 7L).toInt
        if (vBits == n1Bits) {
            val newN1 = n1 + (v, level + 3)
            if (newN1 ne n1) {
                new GrowableLongTrieSetNode1(n1Bits, newN1)
            } else {
                this
            }
        } else {
            val lookupTable = 1 << (n1Bits * 4) | 2 << (vBits * 4)
            new GrowableLongTrieSetNode2(lookupTable, n1, new GrowableLongTrieSet1(v))
        }
    }

    override def toString(indent: Int): String = {
        s"N(${bitsToString(n1Bits)}=>${n1.toString(indent + 1)})"
    }

}

private[immutable] abstract class GrowableLongTrieSetNode2x extends GrowableLongTrieSetNode {

    /**
     * The mapping between the three (relevant) bits of the value to the slot where the value
     * is stored.
     */
    val lookupTable: Int

    /**
     * The index starts with "1"!
     */
    def node(index: Int): GrowableLongTrieSetNode

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

    final override def toString(level: Int): String = {
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

private[immutable] final class GrowableLongTrieSetNode2(
        final val lookupTable: Int,
        final val n1:          GrowableLongTrieSetNode,
        final val n2:          GrowableLongTrieSetNode
) extends GrowableLongTrieSetNode2x {

    override def node(index: Int): GrowableLongTrieSetNode = {
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

    override def forall(p: Long ⇒ Boolean): Boolean = {
        n1.forall(p) &&
            n2.forall(p)
    }

    override private[immutable] def +(v: Long, level: Int): GrowableLongTrieSetNode = {
        val vBits = ((v >> level) & 7L).toInt
        val vIndex = (lookupTable >> (vBits * 4)) & 15
        vIndex match {
            case 0 ⇒
                val newLookupTable = lookupTable | (3 << (vBits * 4))
                val newN3 = new GrowableLongTrieSet1(v)
                new GrowableLongTrieSetNode3(newLookupTable, n1, n2, newN3)
            case 1 ⇒
                val newN1 = n1 + (v, level + 3)
                if (newN1 ne n1) {
                    new GrowableLongTrieSetNode2(lookupTable, newN1, n2)
                } else {
                    this
                }
            case 2 ⇒
                val newN2 = n2 + (v, level + 3)
                if (newN2 ne n2) {
                    new GrowableLongTrieSetNode2(lookupTable, n1, newN2)
                } else {
                    this
                }
        }
    }

}

private[immutable] final class GrowableLongTrieSetNode3(
        final val lookupTable: Int,
        final val n1:          GrowableLongTrieSetNode,
        final val n2:          GrowableLongTrieSetNode,
        final val n3:          GrowableLongTrieSetNode
) extends GrowableLongTrieSetNode2x {

    override def foreach[U](f: Long ⇒ U): Unit = {
        n1.foreach(f)
        n2.foreach(f)
        n3.foreach(f)
    }

    override def forall(p: Long ⇒ Boolean): Boolean = {
        n1.forall(p) &&
            n2.forall(p) &&
            n3.forall(p)
    }

    override def node(index: Int): GrowableLongTrieSetNode = {
        index match {
            case 1 ⇒ n1
            case 2 ⇒ n2
            case 3 ⇒ n3
            case _ ⇒ null
        }
    }

    override private[immutable] def +(v: Long, level: Int): GrowableLongTrieSetNode = {
        val vBits = ((v >> level) & 7L).toInt
        val vIndex = (lookupTable >> (vBits * 4)) & 15
        vIndex match {
            case 0 ⇒
                val newLookupTable = lookupTable | (4 << (vBits * 4))
                val newN4 = new GrowableLongTrieSet1(v)
                new GrowableLongTrieSetNode4(newLookupTable, n1, n2, n3, newN4)
            case 1 ⇒
                val newN1 = n1 + (v, level + 3)
                if (newN1 ne n1) {
                    new GrowableLongTrieSetNode3(lookupTable, newN1, n2, n3)
                } else {
                    this
                }
            case 2 ⇒
                val newN2 = n2 + (v, level + 3)
                if (newN2 ne n2) {
                    new GrowableLongTrieSetNode3(lookupTable, n1, newN2, n3)
                } else {
                    this
                }
            case 3 ⇒
                val newN3 = n3 + (v, level + 3)
                if (newN3 ne n3) {
                    new GrowableLongTrieSetNode3(lookupTable, n1, n2, newN3)
                } else {
                    this
                }
        }
    }
}

private[immutable] final class GrowableLongTrieSetNode4(
        final val lookupTable: Int,
        final val n1:          GrowableLongTrieSetNode,
        final val n2:          GrowableLongTrieSetNode,
        final val n3:          GrowableLongTrieSetNode,
        final val n4:          GrowableLongTrieSetNode
) extends GrowableLongTrieSetNode2x {

    override def foreach[U](f: Long ⇒ U): Unit = {
        n1.foreach(f)
        n2.foreach(f)
        n3.foreach(f)
        n4.foreach(f)
    }

    override def forall(p: Long ⇒ Boolean): Boolean = {
        n1.forall(p) &&
            n2.forall(p) &&
            n3.forall(p) &&
            n4.forall(p)
    }

    override def node(index: Int): GrowableLongTrieSetNode = {
        index match {
            case 1 ⇒ n1
            case 2 ⇒ n2
            case 3 ⇒ n3
            case 4 ⇒ n4
            case _ ⇒ null
        }
    }

    override private[immutable] def +(v: Long, level: Int): GrowableLongTrieSetNode = {
        val vBits = ((v >> level) & 7L).toInt
        val vIndex = (lookupTable >> (vBits * 4)) & 15
        vIndex match {
            case 0 ⇒
                val newLookupTable = lookupTable | (5 << (vBits * 4))
                val newN5 = new GrowableLongTrieSet1(v)
                new GrowableLongTrieSetNode5(newLookupTable, n1, n2, n3, n4, newN5)
            case 1 ⇒
                val newN1 = n1 + (v, level + 3)
                if (newN1 ne n1) {
                    new GrowableLongTrieSetNode4(lookupTable, newN1, n2, n3, n4)
                } else {
                    this
                }
            case 2 ⇒
                val newN2 = n2 + (v, level + 3)
                if (newN2 ne n2) {
                    new GrowableLongTrieSetNode4(lookupTable, n1, newN2, n3, n4)
                } else {
                    this
                }
            case 3 ⇒
                val newN3 = n3 + (v, level + 3)
                if (newN3 ne n3) {
                    new GrowableLongTrieSetNode4(lookupTable, n1, n2, newN3, n4)
                } else {
                    this
                }
            case 4 ⇒
                val newN4 = n4 + (v, level + 3)
                if (newN4 ne n4) {
                    new GrowableLongTrieSetNode4(lookupTable, n1, n2, n3, newN4)
                } else {
                    this
                }
        }
    }
}

private[immutable] final class GrowableLongTrieSetNode5(
        final val lookupTable: Int,
        final val n1:          GrowableLongTrieSetNode,
        final val n2:          GrowableLongTrieSetNode,
        final val n3:          GrowableLongTrieSetNode,
        final val n4:          GrowableLongTrieSetNode,
        final val n5:          GrowableLongTrieSetNode
) extends GrowableLongTrieSetNode2x {

    override def foreach[U](f: Long ⇒ U): Unit = {
        n1.foreach(f)
        n2.foreach(f)
        n3.foreach(f)
        n4.foreach(f)
        n5.foreach(f)
    }

    override def forall(p: Long ⇒ Boolean): Boolean = {
        n1.forall(p) &&
            n2.forall(p) &&
            n3.forall(p) &&
            n4.forall(p) &&
            n5.forall(p)
    }

    override def node(index: Int): GrowableLongTrieSetNode = {
        index match {
            case 1 ⇒ n1
            case 2 ⇒ n2
            case 3 ⇒ n3
            case 4 ⇒ n4
            case 5 ⇒ n5
            case _ ⇒ null
        }
    }

    override private[immutable] def +(v: Long, level: Int): GrowableLongTrieSetNode = {
        val vBits = ((v >> level) & 7L).toInt
        val vIndex = (lookupTable >> (vBits * 4)) & 15
        vIndex match {
            case 0 ⇒
                val newLookupTable = lookupTable | (6 << (vBits * 4))
                val newN6 = new GrowableLongTrieSet1(v)
                new GrowableLongTrieSetNode6(newLookupTable, n1, n2, n3, n4, n5, newN6)
            case 1 ⇒
                val newN1 = n1 + (v, level + 3)
                if (newN1 ne n1) {
                    new GrowableLongTrieSetNode5(lookupTable, newN1, n2, n3, n4, n5)
                } else {
                    this
                }
            case 2 ⇒
                val newN2 = n2 + (v, level + 3)
                if (newN2 ne n2) {
                    new GrowableLongTrieSetNode5(lookupTable, n1, newN2, n3, n4, n5)
                } else {
                    this
                }
            case 3 ⇒
                val newN3 = n3 + (v, level + 3)
                if (newN3 ne n3) {
                    new GrowableLongTrieSetNode5(lookupTable, n1, n2, newN3, n4, n5)
                } else {
                    this
                }
            case 4 ⇒
                val newN4 = n4 + (v, level + 3)
                if (newN4 ne n4) {
                    new GrowableLongTrieSetNode5(lookupTable, n1, n2, n3, newN4, n5)
                } else {
                    this
                }
            case 5 ⇒
                val newN5 = n5 + (v, level + 3)
                if (newN5 ne n5) {
                    new GrowableLongTrieSetNode5(lookupTable, n1, n2, n3, n4, newN5)
                } else {
                    this
                }
        }
    }
}

private[immutable] final class GrowableLongTrieSetNode6(
        final val lookupTable: Int,
        final val n1:          GrowableLongTrieSetNode,
        final val n2:          GrowableLongTrieSetNode,
        final val n3:          GrowableLongTrieSetNode,
        final val n4:          GrowableLongTrieSetNode,
        final val n5:          GrowableLongTrieSetNode,
        final val n6:          GrowableLongTrieSetNode
) extends GrowableLongTrieSetNode2x {

    override def foreach[U](f: Long ⇒ U): Unit = {
        n1.foreach(f)
        n2.foreach(f)
        n3.foreach(f)
        n4.foreach(f)
        n5.foreach(f)
        n6.foreach(f)
    }

    override def forall(p: Long ⇒ Boolean): Boolean = {
        n1.forall(p) &&
            n2.forall(p) &&
            n3.forall(p) &&
            n4.forall(p) &&
            n5.forall(p) &&
            n6.forall(p)
    }

    override def node(index: Int): GrowableLongTrieSetNode = {
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

    override private[immutable] def +(v: Long, level: Int): GrowableLongTrieSetNode = {
        val vBits = ((v >> level) & 7L).toInt
        val vIndex = (lookupTable >> (vBits * 4)) & 15
        vIndex match {
            case 0 ⇒
                val newLookupTable = lookupTable | (7 << (vBits * 4))
                val newN7 = new GrowableLongTrieSet1(v)
                new GrowableLongTrieSetNode7(newLookupTable, n1, n2, n3, n4, n5, n6, newN7)
            case 1 ⇒
                val newN1 = n1 + (v, level + 3)
                if (newN1 ne n1) {
                    new GrowableLongTrieSetNode6(lookupTable, newN1, n2, n3, n4, n5, n6)
                } else {
                    this
                }
            case 2 ⇒
                val newN2 = n2 + (v, level + 3)
                if (newN2 ne n2) {
                    new GrowableLongTrieSetNode6(lookupTable, n1, newN2, n3, n4, n5, n6)
                } else {
                    this
                }
            case 3 ⇒
                val newN3 = n3 + (v, level + 3)
                if (newN3 ne n3) {
                    new GrowableLongTrieSetNode6(lookupTable, n1, n2, newN3, n4, n5, n6)
                } else {
                    this
                }
            case 4 ⇒
                val newN4 = n4 + (v, level + 3)
                if (newN4 ne n4) {
                    new GrowableLongTrieSetNode6(lookupTable, n1, n2, n3, newN4, n5, n6)
                } else {
                    this
                }
            case 5 ⇒
                val newN5 = n5 + (v, level + 3)
                if (newN5 ne n5) {
                    new GrowableLongTrieSetNode6(lookupTable, n1, n2, n3, n4, newN5, n6)
                } else {
                    this
                }
            case 6 ⇒
                val newN6 = n6 + (v, level + 3)
                if (newN6 ne n6) {
                    new GrowableLongTrieSetNode6(lookupTable, n1, n2, n3, n4, n5, newN6)
                } else {
                    this
                }
        }
    }
}

private[immutable] final class GrowableLongTrieSetNode7(
        final val lookupTable: Int,
        final val n1:          GrowableLongTrieSetNode,
        final val n2:          GrowableLongTrieSetNode,
        final val n3:          GrowableLongTrieSetNode,
        final val n4:          GrowableLongTrieSetNode,
        final val n5:          GrowableLongTrieSetNode,
        final val n6:          GrowableLongTrieSetNode,
        final val n7:          GrowableLongTrieSetNode
) extends GrowableLongTrieSetNode2x {

    override def foreach[U](f: Long ⇒ U): Unit = {
        n1.foreach(f)
        n2.foreach(f)
        n3.foreach(f)
        n4.foreach(f)
        n5.foreach(f)
        n6.foreach(f)
        n7.foreach(f)
    }

    override def forall(p: Long ⇒ Boolean): Boolean = {
        n1.forall(p) &&
            n2.forall(p) &&
            n3.forall(p) &&
            n4.forall(p) &&
            n5.forall(p) &&
            n6.forall(p) &&
            n7.forall(p)
    }

    override def node(index: Int): GrowableLongTrieSetNode = {
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

    override private[immutable] def +(v: Long, level: Int): GrowableLongTrieSetNode = {
        val vBits = ((v >> level) & 7L).toInt
        val vIndex = (lookupTable >> (vBits * 4)) & 15
        vIndex match {
            case 0 ⇒
                val newLookupTable = lookupTable | (8 << (vBits * 4))
                val newN8 = new GrowableLongTrieSet1(v)
                new GrowableLongTrieSetNode8(newLookupTable, n1, n2, n3, n4, n5, n6, n7, newN8)
            case 1 ⇒
                val newN1 = n1 + (v, level + 3)
                if (newN1 ne n1) {
                    new GrowableLongTrieSetNode7(lookupTable, newN1, n2, n3, n4, n5, n6, n7)
                } else {
                    this
                }
            case 2 ⇒
                val newN2 = n2 + (v, level + 3)
                if (newN2 ne n2) {
                    new GrowableLongTrieSetNode7(lookupTable, n1, newN2, n3, n4, n5, n6, n7)
                } else {
                    this
                }
            case 3 ⇒
                val newN3 = n3 + (v, level + 3)
                if (newN3 ne n3) {
                    new GrowableLongTrieSetNode7(lookupTable, n1, n2, newN3, n4, n5, n6, n7)
                } else {
                    this
                }
            case 4 ⇒
                val newN4 = n4 + (v, level + 3)
                if (newN4 ne n4) {
                    new GrowableLongTrieSetNode7(lookupTable, n1, n2, n3, newN4, n5, n6, n7)
                } else {
                    this
                }
            case 5 ⇒
                val newN5 = n5 + (v, level + 3)
                if (newN5 ne n5) {
                    new GrowableLongTrieSetNode7(lookupTable, n1, n2, n3, n4, newN5, n6, n7)
                } else {
                    this
                }
            case 6 ⇒
                val newN6 = n6 + (v, level + 3)
                if (newN6 ne n6) {
                    new GrowableLongTrieSetNode7(lookupTable, n1, n2, n3, n4, n5, newN6, n7)
                } else {
                    this
                }
            case 7 ⇒
                val newN7 = n7 + (v, level + 3)
                if (newN7 ne n7) {
                    new GrowableLongTrieSetNode7(lookupTable, n1, n2, n3, n4, n5, n6, newN7)
                } else {
                    this
                }

        }
    }
}

private[immutable] final class GrowableLongTrieSetNode8(
        final val lookupTable: Int,
        final val n1:          GrowableLongTrieSetNode,
        final val n2:          GrowableLongTrieSetNode,
        final val n3:          GrowableLongTrieSetNode,
        final val n4:          GrowableLongTrieSetNode,
        final val n5:          GrowableLongTrieSetNode,
        final val n6:          GrowableLongTrieSetNode,
        final val n7:          GrowableLongTrieSetNode,
        final val n8:          GrowableLongTrieSetNode
) extends GrowableLongTrieSetNode2x {

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

    override def forall(p: Long ⇒ Boolean): Boolean = {
        n1.forall(p) &&
            n2.forall(p) &&
            n3.forall(p) &&
            n4.forall(p) &&
            n5.forall(p) &&
            n6.forall(p) &&
            n7.forall(p) &&
            n8.forall(p)
    }

    override def node(index: Int): GrowableLongTrieSetNode = {
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

    override private[immutable] def +(v: Long, level: Int): GrowableLongTrieSetNode = {
        val vBits = ((v >> level) & 7L).toInt
        val vIndex = (lookupTable >> (vBits * 4)) & 15
        vIndex match {
            case 1 ⇒
                val newN1 = n1 + (v, level + 3)
                if (newN1 ne n1) {
                    new GrowableLongTrieSetNode8(lookupTable, newN1, n2, n3, n4, n5, n6, n7, n8)
                } else {
                    this
                }
            case 2 ⇒
                val newN2 = n2 + (v, level + 3)
                if (newN2 ne n2) {
                    new GrowableLongTrieSetNode8(lookupTable, n1, newN2, n3, n4, n5, n6, n7, n8)
                } else {
                    this
                }
            case 3 ⇒
                val newN3 = n3 + (v, level + 3)
                if (newN3 ne n3) {
                    new GrowableLongTrieSetNode8(lookupTable, n1, n2, newN3, n4, n5, n6, n7, n8)
                } else {
                    this
                }
            case 4 ⇒
                val newN4 = n4 + (v, level + 3)
                if (newN4 ne n4) {
                    new GrowableLongTrieSetNode8(lookupTable, n1, n2, n3, newN4, n5, n6, n7, n8)
                } else {
                    this
                }
            case 5 ⇒
                val newN5 = n5 + (v, level + 3)
                if (newN5 ne n5) {
                    new GrowableLongTrieSetNode8(lookupTable, n1, n2, n3, n4, newN5, n6, n7, n8)
                } else {
                    this
                }
            case 6 ⇒
                val newN6 = n6 + (v, level + 3)
                if (newN6 ne n6) {
                    new GrowableLongTrieSetNode8(lookupTable, n1, n2, n3, n4, n5, newN6, n7, n8)
                } else {
                    this
                }
            case 7 ⇒
                val newN7 = n7 + (v, level + 3)
                if (newN7 ne n7) {
                    new GrowableLongTrieSetNode8(lookupTable, n1, n2, n3, n4, n5, n6, newN7, n8)
                } else {
                    this
                }
            case 8 ⇒
                val newN8 = n8 + (v, level + 3)
                if (newN8 ne n8) {
                    new GrowableLongTrieSetNode8(lookupTable, n1, n2, n3, n4, n5, n6, n7, newN8)
                } else {
                    this
                }

        }
    }
}
