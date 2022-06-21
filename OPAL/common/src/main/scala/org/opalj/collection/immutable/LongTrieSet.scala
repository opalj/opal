/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.collection
package immutable

import scala.annotation.tailrec

import org.opalj.collection.LongIterator
import java.lang.Long.{hashCode => lHashCode}

sealed abstract class LongTrieSet extends LongSet { intSet =>

    final type ThisSet = LongTrieSet

    override def equals(other: Any): Boolean
    override def hashCode: Int
}

object LongTrieSet {

    def empty: LongTrieSet = LongTrieSet0

    def apply(v1: Long): LongTrieSet = new LongTrieSet1(v1)

    def apply(v1: Long, v2: Long): LongTrieSetLeaf = {
        if (v1 == v2)
            new LongTrieSet1(v1)
        else if (v1 < v2)
            new LongTrieSet2(v1, v2)
        else
            new LongTrieSet2(v2, v1)
    }

    def apply(v1: Long, v2: Long, v3: Long): LongTrieSetLeaf = {
        if (v1 == v2)
            apply(v1, v3)
        else if (v2 == v3)
            apply(v1, v2)
        else if (v1 == v3)
            apply(v2, v3)
        else {
            // all three values are different...
            if (v1 < v2) {
                if (v2 < v3)
                    new LongTrieSet3(v1, v2, v3)
                else if (v1 < v3)
                    new LongTrieSet3(v1, v3, v2)
                else
                    new LongTrieSet3(v3, v1, v2)
            } else {
                // v2 <!< v1
                if (v3 < v2)
                    new LongTrieSet3(v3, v2, v1)
                else if (v1 < v3)
                    new LongTrieSet3(v2, v1, v3)
                else
                    new LongTrieSet3(v2, v3, v1)
            }
        }
    }

    def apply(v1: Long, v2: Long, v3: Long, v4: Long): LongTrieSet = {
        apply(v1, v2, v3) + v4
    }

}

private[immutable] case object LongTrieSet0 extends LongTrieSet {
    override def isSingletonSet: Boolean = false
    override def isEmpty: Boolean = true
    override def size: Int = 0

    override def contains(value: Long): Boolean = false

    override def forall(p: Long => Boolean): Boolean = true
    override def foreach[U](f: Long => U): Unit = {}
    override def iterator: LongIterator = LongIterator.empty
    override def foldLeft[B](z: B)(op: (B, Long) => B): B = z

    override def +(i: Long): LongTrieSet1 = new LongTrieSet1(i)

    override def equals(other: Any): Boolean = {
        other match {
            case that: AnyRef => that eq this
            case _            => false
        }
    }
    override def hashCode: Int = 0
    override def toString: String = "LongTrieSet()"
}

/** A node of the trie. */
private[immutable] sealed trait LongTrieSetNode {

    def forall(p: Long => Boolean): Boolean
    def foreach[U](f: Long => U): Unit
    def foldLeft[B](z: B)(op: (B, Long) => B): B

    //
    // IMPLEMENTATION "INTERNAL" METHODS
    //
    private[immutable] def add(i: Long, level: Int): LongTrieSetNode
    private[immutable] def contains(value: Long, key: Long): Boolean
    private[immutable] def toString(indent: Int): String

    final private[immutable] def bitsToString(bits: Int): String = {
        bits.toBinaryString.reverse.padTo(3, '0').reverse
    }
}

/** The (potential) leaves of an IntTrie. */
private[immutable] sealed abstract class LongTrieSetLeaf
    extends LongTrieSet
    with LongTrieSetNode {

    /** Returns the nth value (0 based). */
    def apply(index: Int): Long

    /** The number of values stored by this leaf node. */
    def size: Int

    final override private[immutable] def contains(value: Long, key: Long): Boolean = {
        this.contains(value)
    }

    final override private[immutable] def toString(indent: Int): String = {
        (" " * indent) + toString()
    }
}

private[immutable] final class LongTrieSet1(val i1: Long) extends LongTrieSetLeaf {

    override def isEmpty: Boolean = false
    override def isSingletonSet: Boolean = true
    override def size: Int = 1

    override def contains(i: Long): Boolean = i == i1

    override def apply(index: Int): Long = i1

    override def forall(p: Long => Boolean): Boolean = p(i1)
    override def foreach[U](f: Long => U): Unit = f(i1)
    override def iterator: LongIterator = LongIterator(i1)
    override def foldLeft[B](z: B)(op: (B, Long) => B): B = op(z, i1)

    override def +(i: Long): LongTrieSetLeaf = {
        val i1 = this.i1
        if (i1 == i)
            this
        else {
            if (i1 < i)
                new LongTrieSet2(i1, i)
            else
                new LongTrieSet2(i, i1)
        }
    }
    override private[immutable] def add(i: Long, level: Int): LongTrieSetNode = this.+(i)

    override def equals(other: Any): Boolean = {
        other match {
            case that: LongTrieSet1 => (that eq this) || this.i1 == that.i1
            case that               => false
        }
    }
    override def hashCode: Int = 31 + lHashCode(i1)
    override def toString: String = s"LongTrieSet($i1)"

}

/**
 * Represents an ordered set of two values where i1 has to be smaller than i2.
 */
private[immutable] final class LongTrieSet2(
        val i1: Long, val i2: Long
) extends LongTrieSetLeaf {

    override def isEmpty: Boolean = false
    override def isSingletonSet: Boolean = false
    override def size: Int = 2

    override def contains(i: Long): Boolean = i == i1 || i == i2

    override def apply(index: Int): Long = if (index == 0) i1 else i2

    override def forall(p: Long => Boolean): Boolean = { p(i1) && p(i2) }
    override def foreach[U](f: Long => U): Unit = { f(i1); f(i2) }
    override def iterator: LongIterator = LongIterator(i1, i2)
    override def foldLeft[B](z: B)(op: (B, Long) => B): B = op(op(z, i1), i2)

    override def +(i: Long): LongTrieSetLeaf = {
        val i1 = this.i1
        if (i1 == i)
            return this;

        val i2 = this.i2
        if (i2 == i)
            return this;

        if (i < i2) {
            if (i < i1) {
                new LongTrieSet3(i, i1, i2)
            } else {
                new LongTrieSet3(i1, i, i2)
            }
        } else {
            new LongTrieSet3(i1, i2, i)
        }
    }
    override private[immutable] def add(i: Long, level: Int): LongTrieSetNode = this.+(i)

    override def equals(other: Any): Boolean = {
        other match {
            case that: LongTrieSet2 => (that eq this) || this.i1 == that.i1 && this.i2 == that.i2
            case that               => false
        }
    }
    override def hashCode: Int = 31 * (31 + lHashCode(i1)) + lHashCode(i2)
    override def toString: String = s"LongTrieSet($i1, $i2)"

}

/**
 * Represents an ordered set of three int values: i1 < i2 < i3.
 */
private[immutable] final class LongTrieSet3(
        val i1: Long, val i2: Long, val i3: Long
) extends LongTrieSetLeaf {

    override def size: Int = 3
    override def isEmpty: Boolean = false
    override def isSingletonSet: Boolean = false

    override def contains(i: Long): Boolean = {
        if (i < i2) {
            i == i1
        } else if (i > i2) {
            i == i3
        } else {
            true
        }
    }

    override def apply(index: Int): Long = if (index == 0) i1 else if (index == 1) i2 else i3

    override def forall(p: Long => Boolean): Boolean = { p(i1) && p(i2) && p(i3) }
    override def foreach[U](f: Long => U): Unit = { f(i1); f(i2); f(i3) }
    override def iterator: LongIterator = LongIterator(i1, i2, i3)
    override def foldLeft[B](z: B)(op: (B, Long) => B): B = op(op(op(z, i1), i2), i3)

    override def +(i: Long): LongTrieSet = {
        if (i < i2) {
            if (i == i1)
                return this;
        } else if (i > i2) {
            if (i == i3)
                return this;
        } else {
            // i =!= i2
            return this;
        }

        new LongTrieSetN(4, this.grow(i, 0))
    }
    override private[immutable] def add(i: Long, level: Int): LongTrieSetNode = {
        if (i < i2) {
            if (i == i1)
                return this;
        } else if (i > i2) {
            if (i == i3)
                return this;
        } else {
            // i =!= i2
            return this;
        }

        this.grow(i, level)
    }
    private[this] def grow(i: Long, level: Int): LongTrieSetNode = {
        // we know that i1, i2, i3 and i are all different values
        // Now, let's try to create the final tree in a more direct manner:

        val i1_7L = ((i1 >> level) & 7L).toInt
        val i2_7L = ((i2 >> level) & 7L).toInt
        val i3_7L = ((i3 >> level) & 7L).toInt
        val i_7L = ((i >> level) & 7L).toInt
        if (i1_7L == i2_7L) {
            if (i1_7L == i3_7L) {
                if (i1_7L == i_7L) {
                    // they all have the same 3 bits used for branching purposes...
                    new LongTrieSetNode1(i1_7L.toInt, grow(i, level + 3))
                } else {
                    new LongTrieSetNode2(
                        1 << (i_7L * 4) | 2 << (i1_7L * 4) /*lookuptable*/ ,
                        new LongTrieSet1(i),
                        this
                    )
                }
            } else {
                // i1_7L != i3_7L
                if (i1_7L == i_7L) {
                    new LongTrieSetNode2(
                        1 << (i3_7L * 4) | 2 << (i1_7L * 4) /*lookuptable*/ ,
                        new LongTrieSet1(i3),
                        LongTrieSet(i1, i2, i)
                    )
                } else {
                    // i1_7L != i3_7L
                    // i1_7L != i_7L
                    if (i3_7L == i_7L) {
                        new LongTrieSetNode2(
                            1 << (i3_7L * 4) | 2 << (i1_7L * 4) /*lookuptable*/ ,
                            LongTrieSet(i3, i),
                            new LongTrieSet2(i1, i2)
                        )
                    } else {
                        new LongTrieSetNode3(
                            1 << (i3_7L * 4) | 2 << (i1_7L * 4) | 3 << (i_7L * 4),
                            new LongTrieSet1(i3),
                            new LongTrieSet2(i1, i2),
                            new LongTrieSet1(i)
                        )
                    }
                }
            }
        } else {
            // i1_7L != i2_7L
            if (i2_7L == i3_7L) {
                if (i2_7L == i_7L) {
                    new LongTrieSetNode2(
                        1 << (i1_7L * 4) | 2 << (i2_7L * 4) /*lookuptable*/ ,
                        new LongTrieSet1(i1),
                        LongTrieSet(i2, i3, i)
                    )
                } else {
                    // i1_7L != i2_7L
                    // i2_7L != i_7L
                    if (i1_7L == i_7L) {
                        new LongTrieSetNode2(
                            1 << (i1_7L * 4) | 2 << (i2_7L * 4) /*lookuptable*/ ,
                            LongTrieSet(i1, i),
                            new LongTrieSet2(i2, i3)
                        )
                    } else {
                        new LongTrieSetNode3(
                            1 << (i1_7L * 4) | 2 << (i2_7L * 4) | 3 << (i_7L * 4),
                            new LongTrieSet1(i1),
                            new LongTrieSet2(i2, i3),
                            new LongTrieSet1(i)
                        )
                    }
                }
            } else {
                // i1_7L != i2_7L
                // i2_7L != i3_7L
                if (i1_7L == i3_7L) {
                    if (i1_7L == i_7L) {
                        new LongTrieSetNode2(
                            1 << (i2_7L * 4) | 2 << (i1_7L * 4) /*lookuptable*/ ,
                            new LongTrieSet1(i2),
                            LongTrieSet(i1, i3, i)
                        )
                    } else {
                        // i1_7L != i2_7L
                        // i2_7L != i3_7L
                        // i1_7L != i_7L
                        if (i2_7L == i_7L) {
                            new LongTrieSetNode2(
                                1 << (i1_7L * 4) | 2 << (i2_7L * 4) /*lookuptable*/ ,
                                new LongTrieSet2(i1, i3),
                                LongTrieSet(i2, i)
                            )
                        } else {
                            // i1_7L != i2_7L
                            // i2_7L != i3_7L
                            // i1_7L != i_7L
                            // i2_7L != i_7L
                            new LongTrieSetNode3(
                                1 << (i2_7L * 4) | 2 << (i1_7L * 4) | 3 << (i_7L * 4),
                                new LongTrieSet1(i2),
                                new LongTrieSet2(i1, i3),
                                new LongTrieSet1(i)
                            )
                        }
                    }
                } else {
                    // i1_7L != i2_7L
                    // i2_7L != i3_7L
                    // i1_7L != i3_7L
                    if (i1_7L == i_7L) {
                        new LongTrieSetNode3(
                            1 << (i1_7L * 4) | 2 << (i2_7L * 4) | 3 << (i3_7L * 4),
                            LongTrieSet(i1, i),
                            new LongTrieSet1(i2),
                            new LongTrieSet1(i3)
                        )
                    } else if (i2_7L == i_7L) {
                        new LongTrieSetNode3(
                            1 << (i2_7L * 4) | 2 << (i1_7L * 4) | 3 << (i3_7L * 4),
                            LongTrieSet(i2, i),
                            new LongTrieSet1(i1),
                            new LongTrieSet1(i3)
                        )
                    } else if (i3_7L == i_7L) {
                        new LongTrieSetNode3(
                            1 << (i3_7L * 4) | 2 << (i2_7L * 4) | 3 << (i1_7L * 4),
                            LongTrieSet(i3, i),
                            new LongTrieSet1(i2),
                            new LongTrieSet1(i1)
                        )
                    } else {
                        new LongTrieSetNode4(
                            1 << (i_7L * 4) | 2 << (i3_7L * 4) | 3 << (i2_7L * 4) | 4 << (i1_7L * 4),
                            new LongTrieSet1(i),
                            new LongTrieSet1(i3),
                            new LongTrieSet1(i2),
                            new LongTrieSet1(i1)
                        )
                    }
                }
            }
        }

        /* OLD
        val l = new LongTrieSet1(i)
        var r: LongTrieSetNode = new LongTrieSetNode1(((i >> level) & 7L).toInt, l)
        r = r + (i1, level)
        r = r + (i2, level)
        r = r + (i3, level)
        r
         */
    }

    override def equals(other: Any): Boolean = {
        other match {
            case that: LongTrieSet3 =>
                (this eq that) || (i1 == that.i1 && i2 == that.i2 && i3 == that.i3)
            case _ =>
                false
        }
    }
    override def hashCode: Int = 31 * (31 * (31 + lHashCode(i1)) + lHashCode(i2)) + lHashCode(i3)
    override def toString: String = s"LongTrieSet($i1, $i2, $i3)"

}

private[immutable] final class LongTrieSetN(
        final val size: Int,
        final val root: LongTrieSetNode
) extends LongTrieSet {

    // assert(size >= 4)

    override def isEmpty: Boolean = false
    override def isSingletonSet: Boolean = false
    override def contains(value: Long): Boolean = root.contains(value, value)
    override def forall(p: Long => Boolean): Boolean = root.forall(p)
    override def foreach[U](f: Long => U): Unit = root.foreach(f)

    override def iterator: LongIterator = new LongIterator {
        private[this] var leafNode: LongTrieSetLeaf = null
        private[this] var index = 0
        private[this] val nodes = new scala.collection.mutable.Stack(initialSize = Math.min(16, size / 2)) += root
        @tailrec private[this] def moveToNextLeafNode(): Unit = {
            if (nodes.isEmpty) {
                leafNode = null
                return ;
            }
            (nodes.pop(): @unchecked) match {
                case n: LongTrieSetLeaf =>
                    leafNode = n
                    index = 0
                    return ;

                case n: LongTrieSetNode1 =>
                    nodes.push(n.n1)

                case n: LongTrieSetNode2 =>
                    nodes.push(n.n1)
                    nodes.push(n.n2)

                case n: LongTrieSetNode3 =>
                    nodes.push(n.n1)
                    nodes.push(n.n2)
                    nodes.push(n.n3)

                case n: LongTrieSetNode4 =>
                    nodes.push(n.n1)
                    nodes.push(n.n2)
                    nodes.push(n.n3)
                    nodes.push(n.n4)

                case n: LongTrieSetNode5 =>
                    nodes.push(n.n1)
                    nodes.push(n.n2)
                    nodes.push(n.n3)
                    nodes.push(n.n4)
                    nodes.push(n.n5)

                case n: LongTrieSetNode6 =>
                    nodes.push(n.n1)
                    nodes.push(n.n2)
                    nodes.push(n.n3)
                    nodes.push(n.n4)
                    nodes.push(n.n5)
                    nodes.push(n.n6)

                case n: LongTrieSetNode7 =>
                    nodes.push(n.n1)
                    nodes.push(n.n2)
                    nodes.push(n.n3)
                    nodes.push(n.n4)
                    nodes.push(n.n5)
                    nodes.push(n.n6)
                    nodes.push(n.n7)

                case n: LongTrieSetNode8 =>
                    nodes.push(n.n1)
                    nodes.push(n.n2)
                    nodes.push(n.n3)
                    nodes.push(n.n4)
                    nodes.push(n.n5)
                    nodes.push(n.n6)
                    nodes.push(n.n7)
                    nodes.push(n.n8)
            }
            moveToNextLeafNode()
        }
        moveToNextLeafNode()
        def hasNext: Boolean = leafNode ne null
        def next(): Long = {
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

    override def foldLeft[B](z: B)(op: (B, Long) => B): B = root.foldLeft(z)(op)

    override def equals(other: Any): Boolean = {
        other match {
            case that: LongTrieSetN =>
                (that eq this) || (
                    that.size == this.size &&
                    // Recall that the iteration order of the values is dependent on the
                    // insertion order, but the shape of the tree – reflected by the size
                    // of nodes and also the captured bit patterns has to be equal. Hence,
                    // comparison can be done by traversing both trees in parallel which
                    // is more efficient than iterating over this (or that) tree and
                    // performing a contains check for each element (the naive approach).
                    this.root == that.root
                )
            case _ => false
        }
    }
    override def hashCode: Int = root.hashCode * 31 + size

    override def +(i: Long): LongTrieSet = {
        val root = this.root
        val newRoot = root.add(i, 0)
        if (newRoot ne root) {
            new LongTrieSetN(size + 1, newRoot)
        } else {
            this
        }
    }

    override def toString: String = s"LongTrieSet(#$size, ${root.toString(1)})"

}

private[immutable] final class LongTrieSetNode1(
        final val n1Bits: Int,
        final val n1:     LongTrieSetNode
) extends LongTrieSetNode {

    override def foreach[U](f: Long => U): Unit = n1.foreach(f)
    override def forall(p: Long => Boolean): Boolean = n1.forall(p)
    override def foldLeft[B](z: B)(op: (B, Long) => B): B = n1.foldLeft(z)(op)

    override private[immutable] def contains(value: Long, key: Long): Boolean = {
        if (n1Bits == (key & 7L).toInt) {
            n1.contains(value, key >> 3)
        } else {
            false
        }
    }

    override private[immutable] def add(v: Long, level: Int): LongTrieSetNode = {
        val vBits = ((v >> level) & 7L).toInt
        if (vBits == n1Bits) {
            val newN1 = n1.add(v, level + 3)
            if (newN1 ne n1) {
                new LongTrieSetNode1(n1Bits, newN1)
            } else {
                this
            }
        } else {
            val lookupTable = 1 << (n1Bits * 4) | 2 << (vBits * 4)
            new LongTrieSetNode2(lookupTable, n1, new LongTrieSet1(v))
        }
    }

    override def equals(other: Any): Boolean = {
        other match {
            case that: LongTrieSetNode1 =>
                this.n1Bits == that.n1Bits && this.n1 == that.n1
            case _ => false
        }
    }
    override def hashCode: Int = n1.hashCode

    override def toString(indent: Int): String = {
        s"N(${bitsToString(n1Bits)}=>${n1.toString(indent + 1)})"
    }

}

private[immutable] sealed abstract class LongTrieSetNode2_7 extends LongTrieSetNode {

    /**
     * The mapping between the three (relevant) bits of the value to the slot where the value
     * is stored.
     */
    val lookupTable: Int

    /**
     * The index starts with "1"!
     */
    def node(index: Int): LongTrieSetNode

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

    final override def equals(other: Any): Boolean = {
        other match {
            case that: LongTrieSetNode2_7 =>
                val thisLookupTable = this.lookupTable
                val thatLookupTable = that.lookupTable
                var index = 0
                do {
                    val thisIndex = thisLookupTable >> (index * 4) & 15
                    val thatIndex = thatLookupTable >> (index * 4) & 15
                    if (thisIndex == 0) {
                        if (thatIndex == 0)
                            index += 1
                        else
                            return false;
                    } else {
                        if (thatIndex == 0)
                            return false;
                        else {
                            if (that.node(thatIndex) == this.node(thisIndex)) {
                                index += 1
                            } else {
                                return false;
                            }
                        }
                    }
                } while (index < 8)
                true
            case _ =>
                false
        }
    }
}

private[immutable] final class LongTrieSetNode2(
        final val lookupTable: Int,
        final val n1:          LongTrieSetNode,
        final val n2:          LongTrieSetNode
) extends LongTrieSetNode2_7 {

    override def node(index: Int): LongTrieSetNode = {
        index match {
            case 1 => n1
            case 2 => n2
            case _ => null
        }
    }

    override def foreach[U](f: Long => U): Unit = {
        n1.foreach(f)
        n2.foreach(f)
    }

    override def forall(p: Long => Boolean): Boolean = {
        n1.forall(p) && n2.forall(p)
    }

    override def foldLeft[B](z: B)(op: (B, Long) => B): B = {
        n2.foldLeft(n1.foldLeft(z)(op))(op)
    }

    override private[immutable] def add(v: Long, level: Int): LongTrieSetNode = {
        val vBits = ((v >> level) & 7L).toInt
        val vIndex = (lookupTable >> (vBits * 4)) & 15
        vIndex match {
            case 0 =>
                val newLookupTable = lookupTable | (3 << (vBits * 4))
                val newN3 = new LongTrieSet1(v)
                new LongTrieSetNode3(newLookupTable, n1, n2, newN3)
            case 1 =>
                val newN1 = n1.add(v, level + 3)
                if (newN1 ne n1) {
                    new LongTrieSetNode2(lookupTable, newN1, n2)
                } else {
                    this
                }
            case 2 =>
                val newN2 = n2.add(v, level + 3)
                if (newN2 ne n2) {
                    new LongTrieSetNode2(lookupTable, n1, newN2)
                } else {
                    this
                }
        }
    }

    override def hashCode: Int = n1.hashCode ^ n2.hashCode // to ensure order independence

}

private[immutable] final class LongTrieSetNode3(
        final val lookupTable: Int,
        final val n1:          LongTrieSetNode,
        final val n2:          LongTrieSetNode,
        final val n3:          LongTrieSetNode
) extends LongTrieSetNode2_7 {

    override def foreach[U](f: Long => U): Unit = {
        n1.foreach(f)
        n2.foreach(f)
        n3.foreach(f)
    }

    override def forall(p: Long => Boolean): Boolean = {
        n1.forall(p) && n2.forall(p) && n3.forall(p)
    }

    override def foldLeft[B](z: B)(op: (B, Long) => B): B = {
        n3.foldLeft(n2.foldLeft(n1.foldLeft(z)(op))(op))(op)
    }

    override def node(index: Int): LongTrieSetNode = {
        index match {
            case 1 => n1
            case 2 => n2
            case 3 => n3
            case _ => null
        }
    }

    override private[immutable] def add(v: Long, level: Int): LongTrieSetNode = {
        val vBits = ((v >> level) & 7L).toInt
        val vIndex = (lookupTable >> (vBits * 4)) & 15
        vIndex match {
            case 0 =>
                val newLookupTable = lookupTable | (4 << (vBits * 4))
                val newN4 = new LongTrieSet1(v)
                new LongTrieSetNode4(newLookupTable, n1, n2, n3, newN4)
            case 1 =>
                val newN1 = n1.add(v, level + 3)
                if (newN1 ne n1) {
                    new LongTrieSetNode3(lookupTable, newN1, n2, n3)
                } else {
                    this
                }
            case 2 =>
                val newN2 = n2.add(v, level + 3)
                if (newN2 ne n2) {
                    new LongTrieSetNode3(lookupTable, n1, newN2, n3)
                } else {
                    this
                }
            case 3 =>
                val newN3 = n3.add(v, level + 3)
                if (newN3 ne n3) {
                    new LongTrieSetNode3(lookupTable, n1, n2, newN3)
                } else {
                    this
                }
        }
    }

    override def hashCode: Int = n1.hashCode ^ n2.hashCode ^ n3.hashCode
}

private[immutable] final class LongTrieSetNode4(
        final val lookupTable: Int,
        final val n1:          LongTrieSetNode,
        final val n2:          LongTrieSetNode,
        final val n3:          LongTrieSetNode,
        final val n4:          LongTrieSetNode
) extends LongTrieSetNode2_7 {

    override def foreach[U](f: Long => U): Unit = {
        n1.foreach(f)
        n2.foreach(f)
        n3.foreach(f)
        n4.foreach(f)
    }

    override def forall(p: Long => Boolean): Boolean = {
        n1.forall(p) && n2.forall(p) && n3.forall(p) && n4.forall(p)
    }

    override def foldLeft[B](z: B)(op: (B, Long) => B): B = {
        n4.foldLeft(n3.foldLeft(n2.foldLeft(n1.foldLeft(z)(op))(op))(op))(op)
    }

    override def node(index: Int): LongTrieSetNode = {
        index match {
            case 1 => n1
            case 2 => n2
            case 3 => n3
            case 4 => n4
            case _ => null
        }
    }

    override private[immutable] def add(v: Long, level: Int): LongTrieSetNode = {
        val vBits = ((v >> level) & 7L).toInt
        val vIndex = (lookupTable >> (vBits * 4)) & 15
        vIndex match {
            case 0 =>
                val newLookupTable = lookupTable | (5 << (vBits * 4))
                val newN5 = new LongTrieSet1(v)
                new LongTrieSetNode5(newLookupTable, n1, n2, n3, n4, newN5)
            case 1 =>
                val newN1 = n1.add(v, level + 3)
                if (newN1 ne n1) {
                    new LongTrieSetNode4(lookupTable, newN1, n2, n3, n4)
                } else {
                    this
                }
            case 2 =>
                val newN2 = n2.add(v, level + 3)
                if (newN2 ne n2) {
                    new LongTrieSetNode4(lookupTable, n1, newN2, n3, n4)
                } else {
                    this
                }
            case 3 =>
                val newN3 = n3.add(v, level + 3)
                if (newN3 ne n3) {
                    new LongTrieSetNode4(lookupTable, n1, n2, newN3, n4)
                } else {
                    this
                }
            case 4 =>
                val newN4 = n4.add(v, level + 3)
                if (newN4 ne n4) {
                    new LongTrieSetNode4(lookupTable, n1, n2, n3, newN4)
                } else {
                    this
                }
        }
    }

    override def hashCode: Int = {
        n1.hashCode ^ n2.hashCode ^ n3.hashCode ^ n4.hashCode
    }
}

private[immutable] final class LongTrieSetNode5(
        final val lookupTable: Int,
        final val n1:          LongTrieSetNode,
        final val n2:          LongTrieSetNode,
        final val n3:          LongTrieSetNode,
        final val n4:          LongTrieSetNode,
        final val n5:          LongTrieSetNode
) extends LongTrieSetNode2_7 {

    override def foreach[U](f: Long => U): Unit = {
        n1.foreach(f)
        n2.foreach(f)
        n3.foreach(f)
        n4.foreach(f)
        n5.foreach(f)
    }

    override def forall(p: Long => Boolean): Boolean = {
        n1.forall(p) && n2.forall(p) && n3.forall(p) && n4.forall(p) && n5.forall(p)
    }

    override def foldLeft[B](z: B)(op: (B, Long) => B): B = {
        n5.foldLeft(n4.foldLeft(n3.foldLeft(n2.foldLeft(n1.foldLeft(z)(op))(op))(op))(op))(op)
    }

    override def node(index: Int): LongTrieSetNode = {
        index match {
            case 1 => n1
            case 2 => n2
            case 3 => n3
            case 4 => n4
            case 5 => n5
            case _ => null
        }
    }

    override private[immutable] def add(v: Long, level: Int): LongTrieSetNode = {
        val vBits = ((v >> level) & 7L).toInt
        val vIndex = (lookupTable >> (vBits * 4)) & 15
        vIndex match {
            case 0 =>
                val newLookupTable = lookupTable | (6 << (vBits * 4))
                val newN6 = new LongTrieSet1(v)
                new LongTrieSetNode6(newLookupTable, n1, n2, n3, n4, n5, newN6)
            case 1 =>
                val newN1 = n1.add(v, level + 3)
                if (newN1 ne n1) {
                    new LongTrieSetNode5(lookupTable, newN1, n2, n3, n4, n5)
                } else {
                    this
                }
            case 2 =>
                val newN2 = n2.add(v, level + 3)
                if (newN2 ne n2) {
                    new LongTrieSetNode5(lookupTable, n1, newN2, n3, n4, n5)
                } else {
                    this
                }
            case 3 =>
                val newN3 = n3.add(v, level + 3)
                if (newN3 ne n3) {
                    new LongTrieSetNode5(lookupTable, n1, n2, newN3, n4, n5)
                } else {
                    this
                }
            case 4 =>
                val newN4 = n4.add(v, level + 3)
                if (newN4 ne n4) {
                    new LongTrieSetNode5(lookupTable, n1, n2, n3, newN4, n5)
                } else {
                    this
                }
            case 5 =>
                val newN5 = n5.add(v, level + 3)
                if (newN5 ne n5) {
                    new LongTrieSetNode5(lookupTable, n1, n2, n3, n4, newN5)
                } else {
                    this
                }
        }
    }

    override def hashCode: Int = {
        n1.hashCode ^ n2.hashCode ^ n3.hashCode ^ n4.hashCode ^ n5.hashCode
    }
}

private[immutable] final class LongTrieSetNode6(
        final val lookupTable: Int,
        final val n1:          LongTrieSetNode,
        final val n2:          LongTrieSetNode,
        final val n3:          LongTrieSetNode,
        final val n4:          LongTrieSetNode,
        final val n5:          LongTrieSetNode,
        final val n6:          LongTrieSetNode
) extends LongTrieSetNode2_7 {

    override def foreach[U](f: Long => U): Unit = {
        n1.foreach(f)
        n2.foreach(f)
        n3.foreach(f)
        n4.foreach(f)
        n5.foreach(f)
        n6.foreach(f)
    }

    override def forall(p: Long => Boolean): Boolean = {
        n1.forall(p) &&
            n2.forall(p) &&
            n3.forall(p) &&
            n4.forall(p) &&
            n5.forall(p) &&
            n6.forall(p)
    }

    override def foldLeft[B](z: B)(op: (B, Long) => B): B = {
        val f4 = n4.foldLeft(n3.foldLeft(n2.foldLeft(n1.foldLeft(z)(op))(op))(op))(op)
        n6.foldLeft(n5.foldLeft(f4)(op))(op)
    }

    override def node(index: Int): LongTrieSetNode = {
        index match {
            case 1 => n1
            case 2 => n2
            case 3 => n3
            case 4 => n4
            case 5 => n5
            case 6 => n6
            case _ => null
        }
    }

    override private[immutable] def add(v: Long, level: Int): LongTrieSetNode = {
        val vBits = ((v >> level) & 7L).toInt
        val vIndex = (lookupTable >> (vBits * 4)) & 15
        vIndex match {
            case 0 =>
                val newLookupTable = lookupTable | (7 << (vBits * 4))
                val newN7 = new LongTrieSet1(v)
                new LongTrieSetNode7(newLookupTable, n1, n2, n3, n4, n5, n6, newN7)
            case 1 =>
                val newN1 = n1.add(v, level + 3)
                if (newN1 ne n1) {
                    new LongTrieSetNode6(lookupTable, newN1, n2, n3, n4, n5, n6)
                } else {
                    this
                }
            case 2 =>
                val newN2 = n2.add(v, level + 3)
                if (newN2 ne n2) {
                    new LongTrieSetNode6(lookupTable, n1, newN2, n3, n4, n5, n6)
                } else {
                    this
                }
            case 3 =>
                val newN3 = n3.add(v, level + 3)
                if (newN3 ne n3) {
                    new LongTrieSetNode6(lookupTable, n1, n2, newN3, n4, n5, n6)
                } else {
                    this
                }
            case 4 =>
                val newN4 = n4.add(v, level + 3)
                if (newN4 ne n4) {
                    new LongTrieSetNode6(lookupTable, n1, n2, n3, newN4, n5, n6)
                } else {
                    this
                }
            case 5 =>
                val newN5 = n5.add(v, level + 3)
                if (newN5 ne n5) {
                    new LongTrieSetNode6(lookupTable, n1, n2, n3, n4, newN5, n6)
                } else {
                    this
                }
            case 6 =>
                val newN6 = n6.add(v, level + 3)
                if (newN6 ne n6) {
                    new LongTrieSetNode6(lookupTable, n1, n2, n3, n4, n5, newN6)
                } else {
                    this
                }
        }
    }

    override def hashCode: Int = {
        n1.hashCode ^ n2.hashCode ^ n3.hashCode ^ n4.hashCode ^ n5.hashCode ^ n6.hashCode
    }
}

private[immutable] final class LongTrieSetNode7(
        final val lookupTable: Int,
        final val n1:          LongTrieSetNode,
        final val n2:          LongTrieSetNode,
        final val n3:          LongTrieSetNode,
        final val n4:          LongTrieSetNode,
        final val n5:          LongTrieSetNode,
        final val n6:          LongTrieSetNode,
        final val n7:          LongTrieSetNode
) extends LongTrieSetNode2_7 {

    override def foreach[U](f: Long => U): Unit = {
        n1.foreach(f)
        n2.foreach(f)
        n3.foreach(f)
        n4.foreach(f)
        n5.foreach(f)
        n6.foreach(f)
        n7.foreach(f)
    }

    override def forall(p: Long => Boolean): Boolean = {
        n1.forall(p) &&
            n2.forall(p) &&
            n3.forall(p) &&
            n4.forall(p) &&
            n5.forall(p) &&
            n6.forall(p) &&
            n7.forall(p)
    }

    override def foldLeft[B](z: B)(op: (B, Long) => B): B = {
        val f4 = n4.foldLeft(n3.foldLeft(n2.foldLeft(n1.foldLeft(z)(op))(op))(op))(op)
        n7.foldLeft(n6.foldLeft(n5.foldLeft(f4)(op))(op))(op)
    }

    override def node(index: Int): LongTrieSetNode = {
        index match {
            case 1 => n1
            case 2 => n2
            case 3 => n3
            case 4 => n4
            case 5 => n5
            case 6 => n6
            case 7 => n7
            case _ => null
        }
    }

    override private[immutable] def add(v: Long, level: Int): LongTrieSetNode = {
        val vBits = ((v >> level) & 7L).toInt
        val vIndex = (lookupTable >> (vBits * 4)) & 15
        vIndex match {
            case 0 =>
                // We resolve the lookup table because we don't need it any longe
                val newV = new LongTrieSet1(v)
                val newN1 = if (vBits == 0) newV else node(lookupTable & 15)
                val newN2 = if (vBits == 1) newV else node(lookupTable >> 4 & 15)
                val newN3 = if (vBits == 2) newV else node(lookupTable >> 8 & 15)
                val newN4 = if (vBits == 3) newV else node(lookupTable >> 12 & 15)
                val newN5 = if (vBits == 4) newV else node(lookupTable >> 16 & 15)
                val newN6 = if (vBits == 5) newV else node(lookupTable >> 20 & 15)
                val newN7 = if (vBits == 6) newV else node(lookupTable >> 24 & 15)
                val newN8 = if (vBits == 7) newV else node(lookupTable >> 28 & 15)
                new LongTrieSetNode8(newN1, newN2, newN3, newN4, newN5, newN6, newN7, newN8)
            case 1 =>
                val newN1 = n1.add(v, level + 3)
                if (newN1 ne n1) {
                    new LongTrieSetNode7(lookupTable, newN1, n2, n3, n4, n5, n6, n7)
                } else {
                    this
                }
            case 2 =>
                val newN2 = n2.add(v, level + 3)
                if (newN2 ne n2) {
                    new LongTrieSetNode7(lookupTable, n1, newN2, n3, n4, n5, n6, n7)
                } else {
                    this
                }
            case 3 =>
                val newN3 = n3.add(v, level + 3)
                if (newN3 ne n3) {
                    new LongTrieSetNode7(lookupTable, n1, n2, newN3, n4, n5, n6, n7)
                } else {
                    this
                }
            case 4 =>
                val newN4 = n4.add(v, level + 3)
                if (newN4 ne n4) {
                    new LongTrieSetNode7(lookupTable, n1, n2, n3, newN4, n5, n6, n7)
                } else {
                    this
                }
            case 5 =>
                val newN5 = n5.add(v, level + 3)
                if (newN5 ne n5) {
                    new LongTrieSetNode7(lookupTable, n1, n2, n3, n4, newN5, n6, n7)
                } else {
                    this
                }
            case 6 =>
                val newN6 = n6.add(v, level + 3)
                if (newN6 ne n6) {
                    new LongTrieSetNode7(lookupTable, n1, n2, n3, n4, n5, newN6, n7)
                } else {
                    this
                }
            case 7 =>
                val newN7 = n7.add(v, level + 3)
                if (newN7 ne n7) {
                    new LongTrieSetNode7(lookupTable, n1, n2, n3, n4, n5, n6, newN7)
                } else {
                    this
                }

        }
    }

    override def hashCode: Int = {
        val h6 = n1.hashCode ^ n2.hashCode ^ n3.hashCode ^ n4.hashCode ^ n5.hashCode ^ n6.hashCode
        h6 ^ n7.hashCode
    }
}

private[immutable] final class LongTrieSetNode8(
        final val n1: LongTrieSetNode,
        final val n2: LongTrieSetNode,
        final val n3: LongTrieSetNode,
        final val n4: LongTrieSetNode,
        final val n5: LongTrieSetNode,
        final val n6: LongTrieSetNode,
        final val n7: LongTrieSetNode,
        final val n8: LongTrieSetNode
) extends LongTrieSetNode {

    override def foreach[U](f: Long => U): Unit = {
        n1.foreach(f)
        n2.foreach(f)
        n3.foreach(f)
        n4.foreach(f)
        n5.foreach(f)
        n6.foreach(f)
        n7.foreach(f)
        n8.foreach(f)
    }

    override def forall(p: Long => Boolean): Boolean = {
        n1.forall(p) &&
            n2.forall(p) &&
            n3.forall(p) &&
            n4.forall(p) &&
            n5.forall(p) &&
            n6.forall(p) &&
            n7.forall(p) &&
            n8.forall(p)
    }

    override def foldLeft[B](z: B)(op: (B, Long) => B): B = {
        val f4 = n4.foldLeft(n3.foldLeft(n2.foldLeft(n1.foldLeft(z)(op))(op))(op))(op)
        n8.foldLeft(n7.foldLeft(n6.foldLeft(n5.foldLeft(f4)(op))(op))(op))(op)
    }

    override private[immutable] def contains(value: Long, key: Long): Boolean = {
        ((key & 7L).toInt match {
            case 0 => n1
            case 1 => n2
            case 2 => n3
            case 3 => n4
            case 4 => n5
            case 5 => n6
            case 6 => n7
            case 7 => n8
        }).contains(value, key >> 3)
    }

    override private[immutable] def add(v: Long, level: Int): LongTrieSetNode = {
        ((v >> level) & 7L).toInt match {
            case 0 =>
                val newN1 = n1.add(v, level + 3)
                if (newN1 ne n1) {
                    new LongTrieSetNode8(newN1, n2, n3, n4, n5, n6, n7, n8)
                } else {
                    this
                }
            case 1 =>
                val newN2 = n2.add(v, level + 3)
                if (newN2 ne n2) {
                    new LongTrieSetNode8(n1, newN2, n3, n4, n5, n6, n7, n8)
                } else {
                    this
                }
            case 2 =>
                val newN3 = n3.add(v, level + 3)
                if (newN3 ne n3) {
                    new LongTrieSetNode8(n1, n2, newN3, n4, n5, n6, n7, n8)
                } else {
                    this
                }
            case 3 =>
                val newN4 = n4.add(v, level + 3)
                if (newN4 ne n4) {
                    new LongTrieSetNode8(n1, n2, n3, newN4, n5, n6, n7, n8)
                } else {
                    this
                }
            case 4 =>
                val newN5 = n5.add(v, level + 3)
                if (newN5 ne n5) {
                    new LongTrieSetNode8(n1, n2, n3, n4, newN5, n6, n7, n8)
                } else {
                    this
                }
            case 5 =>
                val newN6 = n6.add(v, level + 3)
                if (newN6 ne n6) {
                    new LongTrieSetNode8(n1, n2, n3, n4, n5, newN6, n7, n8)
                } else {
                    this
                }
            case 6 =>
                val newN7 = n7.add(v, level + 3)
                if (newN7 ne n7) {
                    new LongTrieSetNode8(n1, n2, n3, n4, n5, n6, newN7, n8)
                } else {
                    this
                }
            case 7 =>
                val newN8 = n8.add(v, level + 3)
                if (newN8 ne n8) {
                    new LongTrieSetNode8(n1, n2, n3, n4, n5, n6, n7, newN8)
                } else {
                    this
                }

        }
    }

    final override def toString(level: Int): String = {
        val indent = " " * level * 3
        var s = s"N("
        var i = 0
        while (i < 8) {
            val n = i match {
                case 0 => n1
                case 1 => n2
                case 2 => n3
                case 3 => n4
                case 4 => n5
                case 5 => n6
                case 6 => n7
                case 7 => n8
            }
            s += s"\n$indent${bitsToString(i)}=>${n.toString(level + 1)}"
            i += 1
        }
        s+")"
    }

    override def equals(other: Any): Boolean = {
        other match {
            case that: LongTrieSetNode8 =>
                this.n1 == that.n1 &&
                    this.n2 == that.n2 &&
                    this.n3 == that.n3 &&
                    this.n4 == that.n4 &&
                    this.n5 == that.n5 &&
                    this.n6 == that.n6 &&
                    this.n7 == that.n7 &&
                    this.n8 == that.n8
            case _ => false
        }
    }
    override def hashCode: Int = {
        val h6 = n1.hashCode ^ n2.hashCode ^ n3.hashCode ^ n4.hashCode ^ n5.hashCode ^ n6.hashCode
        h6 ^ n7.hashCode ^ n8.hashCode
    }
}
