/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.collection
package immutable

import scala.annotation.switch

import java.lang.{Long => JLong}
import java.lang.{Integer => JInt}

/**
 * An effectively immutable trie set of long values where the elements are sorted based on the
 * insertion order.
 *
 * All traversing operations are defined based on the insertion order. The latest added elements
 * will be iterated over first.
 *
 * The trie set is specialized for sizes up to three elements.
 *
 * @author Michael Eichberg
 */
sealed abstract class LongLinkedTrieSet extends LongLinkedSet {

    final type ThisSet = LongLinkedTrieSet

}

object LongLinkedTrieSet {

    def empty: LongLinkedTrieSet = LongLinkedTrieSet0

    def apply(v: Long): LongLinkedTrieSet = new LongLinkedTrieSet1(v)

    def apply(head: Long, last: Long): LongLinkedTrieSet = {
        if (head == last)
            new LongLinkedTrieSet1(head)
        else
            new LongLinkedTrieSet2(head, last)
    }

}

case object LongLinkedTrieSet0 extends LongLinkedTrieSet {
    final override def size: Int = 0
    final override def isEmpty: Boolean = true
    final override def isSingletonSet: Boolean = false

    final override def head: Long = throw new UnsupportedOperationException
    final override def contains(v: Long): Boolean = false

    final override def foreach[U](f: Long => U): Unit = { /*nothing to do*/ }
    final override def foldLeft[T](op: T)(f: (T, Long) => T): T = op
    final override def forall(f: Long => Boolean): Boolean = true
    final override def forFirstN[U](n: Int)(f: Long => U): Unit = { /*nothing to do*/ }
    final override def iterator: LongIterator = LongIterator.empty

    final override def +(v: Long): LongLinkedTrieSet = LongLinkedTrieSet1(v)
}

final case class LongLinkedTrieSet1(v1: Long) extends LongLinkedTrieSet {
    final override def size: Int = 1
    final override def isEmpty: Boolean = false
    final override def isSingletonSet: Boolean = true

    final override def head: Long = v1
    final override def contains(v: Long): Boolean = v == v1

    final override def foreach[U](f: Long => U): Unit = f(v1)
    final override def foldLeft[T](op: T)(f: (T, Long) => T): T = f(op, v1)
    final override def forall(f: Long => Boolean): Boolean = f(v1)
    final override def forFirstN[U](n: Int)(f: Long => U): Unit = if (n > 0) f(v1)
    final override def iterator: LongIterator = LongIterator(v1)

    final override def +(v: Long): LongLinkedTrieSet = {
        if (v != v1) new LongLinkedTrieSet2(v, v1) else this
    }
}

final private[immutable] case class LongLinkedTrieSet2(
        v1: Long, v2: Long
) extends LongLinkedTrieSet {

    final override def size: Int = 2
    final override def isEmpty: Boolean = false
    final override def isSingletonSet: Boolean = false

    final override def head: Long = v1
    final override def contains(v: Long): Boolean = v == v1 || v == v2

    final override def foreach[U](f: Long => U): Unit = { f(v1); f(v2) }
    final override def forall(f: Long => Boolean): Boolean = f(v1) && f(v2)
    final override def foldLeft[T](z: T)(f: (T, Long) => T): T = f(f(z, v1), v2)
    final override def forFirstN[U](n: Int)(f: Long => U): Unit = {
        if (n == 0) return ;
        f(v1)
        if (n == 1) return ;
        f(v2)
    }
    final override def iterator: LongIterator = LongIterator(v1, v2)

    final override def +(v: Long): LongLinkedTrieSet = {
        if (v != v1 && v != v2) new LongLinkedTrieSet3(v, v1, v2) else this
    }
}

final private[immutable] case class LongLinkedTrieSet3(
        v1: Long, v2: Long, v3: Long
) extends LongLinkedTrieSet {

    final override def size: Int = 3
    final override def isEmpty: Boolean = false
    final override def isSingletonSet: Boolean = false

    final override def head: Long = v1
    final override def contains(v: Long): Boolean = v == v1 || v == v2 || v == v3

    final override def foreach[U](f: Long => U): Unit = { f(v1); f(v2); f(v3) }
    final override def forall(f: Long => Boolean): Boolean = f(v1) && f(v2) && f(v3)
    final override def foldLeft[T](z: T)(f: (T, Long) => T): T = f(f(f(z, v1), v2), v3)
    final override def forFirstN[U](n: Int)(f: Long => U): Unit = {
        if (n == 0) return ;
        f(v1)
        if (n == 1) return ;
        f(v2)
        if (n == 2) return ;
        f(v3)
    }
    final override def iterator: LongIterator = LongIterator(v1, v2, v3)

    final override def +(v: Long): LongLinkedTrieSet = {
        if (v != v1 && v != v2 && v != v3) {
            // We have to ensure that we keep the order!
            val set = new LargeLongLinkedTrieSet()
            set += v3
            set += v2
            set += v1
            set += v
            set
        } else {
            this
        }
    }
}

/** The super type of the nodes of the trie set. */
private[immutable] sealed abstract class LongLinkedTrieSetNode {

    /** `true` if this is an inner node. */
    def isN: Boolean
    def isN4: Boolean
    /** `true` if this is a leaf node. */
    def isL: Boolean
    def asL: LongLinkedTrieSetL

    def contains(v: Long, key: Long): Boolean

    /**
     * Returns the node at the next trie level if the bit at the current level is 0.
     *
     * The top node in the trie has level "0".
     */
    def split_0(level: Int): LongLinkedTrieSetNode
    /**
     * Returns the node at the next trie level if the bit at the current level is 1.
     *
     * The top node in the trie has level "0".
     */
    def split_1(level: Int): LongLinkedTrieSetNode

    def add(l: LongLinkedTrieSetL, level: Int): LongLinkedTrieSetNode

    def toString(level: Int): String

}

/** The leaves of the trie set. */
final private[immutable] class LongLinkedTrieSetL(
        final val value: Long,
        final val next:  LongLinkedTrieSetL = null // `null` if this leaf is the first element that was added to the set.
) extends LongLinkedTrieSetNode {

    override def isN: Boolean = false
    override def isN4: Boolean = false
    override def isL: Boolean = true
    override def asL: LongLinkedTrieSetL = this

    override def contains(v: Long, key: Long): Boolean = v == value

    override def add(l: LongLinkedTrieSetL, level: Int): LongLinkedTrieSetNode = {
        val thisValue = this.value
        val lValue = l.value
        if (thisValue == lValue)
            return this;

        // Let's check if there is some sharing and if so, let's use it.
        val thisValueShifted = thisValue >> level
        val lValueShifted = lValue >> level
        JLong.numberOfTrailingZeros(thisValueShifted ^ lValueShifted) match {
            case 0 =>
                if ((thisValueShifted & 1L) == 0L) {
                    new LongLinkedTrieSetN2(this, l)
                } else {
                    new LongLinkedTrieSetN2(l, this)
                }
            case 1 =>
                if ((thisValueShifted & 1L) == 0L) {
                    if ((thisValueShifted & 2L) == 0L) {
                        new LongLinkedTrieSetN4_0_2(this, l)
                    } else {
                        new LongLinkedTrieSetN4_0_2(l, this)
                    }
                } else {
                    if ((thisValueShifted & 2L) == 0L) {
                        new LongLinkedTrieSetN4_1_3(this, l)
                    } else {
                        new LongLinkedTrieSetN4_1_3(l, this)
                    }
                }
            case length =>
                val sharedBits = thisValueShifted & LongSet.BitMasks(length)
                val n =
                    if (((thisValueShifted >> length) & 1L) == 0L)
                        new LongLinkedTrieSetN2(this, l)
                    else
                        new LongLinkedTrieSetN2(l, this)
                LongLinkedTrieSetNShared(sharedBits, length, n)
        }
    }

    final override def split_0(level: Int): LongLinkedTrieSetNode = {
        if ((((value >> level) & 1L) == 0L)) {
            this
        } else {
            null
        }
    }

    final override def split_1(level: Int): LongLinkedTrieSetNode = {
        if ((((value >> level) & 1L) == 1L)) {
            this
        } else {
            null
        }
    }

    final override def toString(level: Int): String = s"L(${value.toBinaryString}=$value)"

}

/** The inner nodes of the trie set. */
private[immutable] sealed abstract class LongLinkedTrieSetInnerNode extends LongLinkedTrieSetNode {
    final override def isN: Boolean = true
    final override def isL: Boolean = false
    final override def asL: LongLinkedTrieSetL = throw new ClassCastException()
}

private[immutable] sealed abstract class LongLinkedTrieSetNShared
    extends LongLinkedTrieSetInnerNode {

    final override def isN4: Boolean = false

    def sharedBits: Long
    def length: Int // at least "1"
    def n: LongLinkedTrieSetNode

    /* Adds one bit to this sequence of shared bits with the given value. */
    def prependBit(bit: Int): LongLinkedTrieSetNShared

    def add(l: LongLinkedTrieSetL, level: Int): LongLinkedTrieSetNode = {
        val length = this.length
        val sharedBits = this.sharedBits
        val lValue = l.value
        val lValueShifted = lValue >> level
        if ((lValueShifted & LongSet.BitMasks(length)) == sharedBits) {
            val oldN = this.n
            val newN = oldN.add(l, level + length)
            if (oldN ne newN) {
                LongLinkedTrieSetNShared(sharedBits, length, newN)
            } else {
                this
            }
        } else {
            // `length` is at least 1 and the number of shared bits is smaller than the current
            // length; i.e., lengthOfTail is at least 1
            val lengthOfLead = JLong.numberOfTrailingZeros(sharedBits ^ lValueShifted)
            val lengthOfTail = length - lengthOfLead - 1 /* -1 for the differing bit */

            // Potential optimizations:
            // We can fold the tail if the number of shared remaining bits is one
            // We can fold the lead if the number of shared initial bits is one

            // 1. Create new tail (if required):
            val newT =
                if (lengthOfTail == 0)
                    n
                else
                    LongLinkedTrieSetNShared(sharedBits >> lengthOfLead + 1, lengthOfTail, n)
            // 2. Create new node where we have the difference
            val newM =
                if (((sharedBits >> lengthOfLead) & 1L) == 0L) {
                    LongLinkedTrieSetN2(level + lengthOfLead, newT, l)
                } else {
                    LongLinkedTrieSetN2(level + lengthOfLead, l, newT)
                }
            // 3. Create new lead node (if required)
            if (lengthOfLead == 0) {
                newM
            } else {
                LongLinkedTrieSetNShared(sharedBits & LongSet.BitMasks(lengthOfLead), lengthOfLead, newM)
            }
        }
    }

    def toString(level: Int): String = {
        val lP2 = level + length + 2
        s"NShared(_${sharedBits.toBinaryString}(#$length)=>${n.toString(lP2)}"
    }
}

private[immutable] object LongLinkedTrieSetNShared {

    def apply(sharedBits: Long, length: Int, n: LongLinkedTrieSetNode): LongLinkedTrieSetNode = {

        assert(length >= 1)

        length match {
            case 1 =>
                // if (sharedBits == 0L /*test the last bit...*/ )
                //    new LongLinkedTrieSetNShared_0(n)
                // else
                //    new LongLinkedTrieSetNShared_1(n)
                (n: @unchecked) match {
                    case l: LongLinkedTrieSetN2 =>
                        if (sharedBits == 0L /*test the last bit...*/ ) {
                            new LongLinkedTrieSetN4_0_2(l._0, l._1)
                        } else {
                            new LongLinkedTrieSetN4_1_3(l._0, l._1)
                        }
                    case l: LongLinkedTrieSetN4 =>
                        if (sharedBits == 0L /*test the last bit...*/ )
                            new LongLinkedTrieSetNShared_0(n)
                        else
                            new LongLinkedTrieSetNShared_1(n)
                    // won't occur: case _: LongLinkedTrieSetL || _: LongLinkedTrieSetNShared
                }

            case 2 =>
                (sharedBits.toInt: @switch) match {
                    case 0 => new LongLinkedTrieSetNShared_00(n)
                    case 1 => new LongLinkedTrieSetNShared_01(n)
                    case 2 => new LongLinkedTrieSetNShared_10(n)
                    case 3 => new LongLinkedTrieSetNShared_11(n)
                }

            case _ =>
                new LongLinkedTrieSetNShared_X(sharedBits, length, n)
        }

    }

}

final private[immutable] class LongLinkedTrieSetNShared_0(
        final val n: LongLinkedTrieSetNode
) extends LongLinkedTrieSetNShared {

    override def sharedBits: Long = 0
    override def length: Int = 1

    override def split_0(level: Int): LongLinkedTrieSetNode = n
    override def split_1(level: Int): LongLinkedTrieSetNode = null

    override def contains(v: Long, key: Long): Boolean = {
        ((key & 1L) == 0L) && n.contains(v, key >> 1)
    }

    override def prependBit(bit: Int): LongLinkedTrieSetNShared = {
        if (bit == 0)
            new LongLinkedTrieSetNShared_00(n)
        else
            new LongLinkedTrieSetNShared_01(n)
    }

    override def add(l: LongLinkedTrieSetL, level: Int): LongLinkedTrieSetNode = {
        val lValue = l.value
        val lValueShifted = lValue >> level
        if ((lValueShifted & 1L) == 0L) {
            val oldN = this.n
            val newN = oldN.add(l, level + length)
            if (oldN ne newN) {
                new LongLinkedTrieSetNShared_0(newN)
            } else {
                this
            }
        } else {
            LongLinkedTrieSetN2(level, n, l)
        }
    }
}

final private[immutable] class LongLinkedTrieSetNShared_1(
        final val n: LongLinkedTrieSetNode
) extends LongLinkedTrieSetNShared {

    override def sharedBits: Long = 1
    override def length: Int = 1

    override def split_0(level: Int): LongLinkedTrieSetNode = null
    override def split_1(level: Int): LongLinkedTrieSetNode = n

    override def contains(v: Long, key: Long): Boolean = {
        ((key & 1L) == 1L) && n.contains(v, key >> 1)
    }

    override def prependBit(bit: Int): LongLinkedTrieSetNShared = {
        if (bit == 0)
            new LongLinkedTrieSetNShared_10(n)
        else
            new LongLinkedTrieSetNShared_11(n)
    }

    override def add(l: LongLinkedTrieSetL, level: Int): LongLinkedTrieSetNode = {
        val lValue = l.value
        val lValueShifted = lValue >> level
        if ((lValueShifted & 1L) == 1L) {
            val oldN = this.n
            val newN = oldN.add(l, level + length)
            if (oldN ne newN) {
                new LongLinkedTrieSetNShared_1(newN)
            } else {
                this
            }
        } else {
            LongLinkedTrieSetN2(level, l, n)
        }
    }
}

final private[immutable] class LongLinkedTrieSetNShared_00(
        final val n: LongLinkedTrieSetNode
) extends LongLinkedTrieSetNShared {
    override def sharedBits: Long = 0
    override def length: Int = 2
    override def split_0(level: Int): LongLinkedTrieSetNode = new LongLinkedTrieSetNShared_0(n)
    override def split_1(level: Int): LongLinkedTrieSetNode = null
    override def contains(v: Long, key: Long): Boolean = {
        ((key & 3L) == 0L) && n.contains(v, key >> 2)
    }
    override def prependBit(bit: Int): LongLinkedTrieSetNShared = {
        new LongLinkedTrieSetNShared_X(if (bit == 0) 0L else 1L, 3, n)
    }

}

final private[immutable] class LongLinkedTrieSetNShared_01(
        final val n: LongLinkedTrieSetNode
) extends LongLinkedTrieSetNShared {
    def sharedBits: Long = 1
    def length: Int = 2
    override def split_0(level: Int): LongLinkedTrieSetNode = null
    override def split_1(level: Int): LongLinkedTrieSetNode = new LongLinkedTrieSetNShared_0(n)
    override def contains(v: Long, key: Long): Boolean = {
        ((key & 3L) == 1L) && n.contains(v, key >> 2)
    }
    override def prependBit(bit: Int): LongLinkedTrieSetNShared = {
        new LongLinkedTrieSetNShared_X(if (bit == 0) 2L else 3L, 3, n)
    }
}

final private[immutable] class LongLinkedTrieSetNShared_10(
        final val n: LongLinkedTrieSetNode
) extends LongLinkedTrieSetNShared {
    def sharedBits: Long = 2
    def length: Int = 2
    override def split_0(level: Int): LongLinkedTrieSetNode = new LongLinkedTrieSetNShared_1(n)
    override def split_1(level: Int): LongLinkedTrieSetNode = null
    override def contains(v: Long, key: Long): Boolean = {
        ((key & 3L) == 2L) && n.contains(v, key >> 2)
    }
    override def prependBit(bit: Int): LongLinkedTrieSetNShared = {
        new LongLinkedTrieSetNShared_X(if (bit == 0) 4L else 5L, 3, n)
    }
}

final private[immutable] class LongLinkedTrieSetNShared_11(
        final val n: LongLinkedTrieSetNode
) extends LongLinkedTrieSetNShared {
    def sharedBits: Long = 3
    def length: Int = 2
    override def split_0(level: Int): LongLinkedTrieSetNode = null
    override def split_1(level: Int): LongLinkedTrieSetNode = new LongLinkedTrieSetNShared_1(n)
    override def contains(v: Long, key: Long): Boolean = {
        ((key & 3L) == 3L) && n.contains(v, key >> 2)
    }
    override def prependBit(bit: Int): LongLinkedTrieSetNShared = {
        new LongLinkedTrieSetNShared_X(if (bit == 0) 6L else 7L, 3, n)
    }
}

final private[immutable] class LongLinkedTrieSetNShared_X(
        val sharedBits: Long,
        val length:     Int,
        val n:          LongLinkedTrieSetNode
) extends LongLinkedTrieSetNShared {

    override def split_0(level: Int): LongLinkedTrieSetNode = {
        if ((sharedBits & 1L) == 0L) {
            LongLinkedTrieSetNShared(sharedBits >> 1, length - 1, n)
        } else {
            null
        }
    }

    override def split_1(level: Int): LongLinkedTrieSetNode = {
        if ((sharedBits & 1L) == 1L) {
            LongLinkedTrieSetNShared(sharedBits >> 1, length - 1, n)
        } else {
            null
        }
    }

    override def prependBit(bit: Int): LongLinkedTrieSetNShared = {
        new LongLinkedTrieSetNShared_X(
            if (bit == 0) sharedBits << 1 else (sharedBits << 1) | 1L,
            length + 1,
            n
        )
    }

    override def contains(v: Long, key: Long): Boolean = {
        val length = this.length
        ((key & LongSet.BitMasks(length)) == sharedBits) &&
            n.contains(v, key >> length)
    }
}

/** The inner nodes of the trie set. */
final private[immutable] class LongLinkedTrieSetN2(
        final val _0: LongLinkedTrieSetNode,
        final val _1: LongLinkedTrieSetNode
) extends LongLinkedTrieSetInnerNode {

    override def isN4: Boolean = false

    override def contains(v: Long, key: Long): Boolean = {
        if ((key & 1L) == 0L)
            _0.contains(v, key >> 1)
        else
            _1.contains(v, key >> 1)
    }

    def add(l: LongLinkedTrieSetL, level: Int): LongLinkedTrieSetNode = {
        val _0 = this._0
        val _1 = this._1
        val lValue = l.value
        val lLSB = ((lValue >> level) & 1L) // lsb == bit at index `level`

        if (_0.isN || _1.isN) {
            // We can't get rid of this N2 node... a successor node is an inner node and we
            // do not want to perform "large" changes to the overall trie.
            return {
                if (lLSB == 0) {
                    val new_0 = _0.add(l, level + 1)
                    if (_0 ne new_0) {
                        new LongLinkedTrieSetN2(new_0, this._1)
                    } else {
                        this
                    }
                } else {
                    val new_1 = _1.add(l, level + 1)
                    if (_1 ne new_1) {
                        new LongLinkedTrieSetN2(this._0, new_1)
                    } else {
                        this
                    }
                }
            };
        }

        if (lLSB == 0) {
            val _0Value = _0.asL.value

            if (_0Value == lValue)
                return this;

            if (((lValue >> level + 1) & 1L) == 0L) {
                // l = ...00
                if (((_0Value >> level + 1) & 1L) == 0L) {
                    val newN = _0.add(l, level + 2)
                    if (((_1.asL.value >> level + 1) & 1L) == 0L) {
                        new LongLinkedTrieSetN4_0_1(newN, _1)
                    } else {
                        new LongLinkedTrieSetN4_0_3(newN, _1)
                    }
                } else {
                    if (((_1.asL.value >> level + 1) & 1L) == 0L) {
                        LongLinkedTrieSetN4(l, _1, _0, null)
                    } else {
                        LongLinkedTrieSetN4(l, null, _0, _1)
                    }
                }
            } else {
                // l = ...10
                if (((_0Value >> level + 1) & 1) == 0L) {
                    // l = ...10, _0 = ...00
                    if (((_1.asL.value >> level + 1) & 1L) == 0L) {
                        LongLinkedTrieSetN4(_0, _1, l, null)
                    } else {
                        LongLinkedTrieSetN4(_0, null, l, _1)
                    }
                } else {
                    // l = ...10, _0 =...10
                    val newN = _0.add(l, level + 2)
                    if (((_1.asL.value >> level + 1) & 1L) == 0L) {
                        new LongLinkedTrieSetN4_1_2(_1, newN)
                    } else {
                        new LongLinkedTrieSetN4_2_3(newN, _1)
                    }
                }
            }
        } else {
            val _1Value = _1.asL.value

            if (_1Value == lValue)
                return this;

            if (((lValue >> level + 1) & 1L) == 0L) {
                // l = ...01
                if (((_1Value >> level + 1) & 1L) == 0L) {
                    // l = ...01, _1 = ...01
                    val newN = _1.add(l, level + 2)
                    if (((_0.asL.value >> level + 1) & 1L) == 0) {
                        new LongLinkedTrieSetN4_0_1(_0, newN)
                    } else {
                        new LongLinkedTrieSetN4_1_2(newN, _0)
                    }
                } else {
                    // l = ...01,_1 = ...11
                    if (((_0.asL.value >> level + 1) & 1L) == 0L) {
                        LongLinkedTrieSetN4(_0, l, null, _1)
                    } else {
                        LongLinkedTrieSetN4(null, l, _0, _1)
                    }
                }
            } else {
                // l = ...11
                if (((_1Value >> level + 1) & 1L) == 0L) {
                    // l = ...11, _1 = ...01
                    if (((_0.asL.value >> level + 1) & 1L) == 0L) {
                        LongLinkedTrieSetN4(_0, _1, null, l)
                    } else {
                        LongLinkedTrieSetN4(null, _1, _0, l)
                    }
                } else {
                    // l = ...11, _1 = ...11
                    val newN = _1.add(l, level + 2)
                    if (((_0.asL.value >> level + 1) & 1L) == 0L) {
                        new LongLinkedTrieSetN4_0_3(_0, newN)
                    } else {
                        new LongLinkedTrieSetN4_2_3(_0, newN)
                    }
                }
            }
        }
    }

    override def split_0(level: Int): LongLinkedTrieSetNode = _0

    override def split_1(level: Int): LongLinkedTrieSetNode = _1

    override def toString(level: Int): String = {
        val indent = " " * level
        val lP2 = level + 2
        s"N2(\n$indent 0=>${_0.toString(lP2)}\n$indent 1=>${_1.toString(lP2)})"
    }
}

private[immutable] object LongLinkedTrieSetN2 {

    def apply(
        level: Int, // the level at which this LongLinkedTrieSetNode will be added to the trie
        _0:    LongLinkedTrieSetNode,
        _1:    LongLinkedTrieSetNode
    ): LongLinkedTrieSetNode = {
        if (_0.isN4 || _1.isN4)
            return new LongLinkedTrieSetN2(_0, _1);

        val newLevel = level + 1
        LongLinkedTrieSetN4(
            _0.split_0(newLevel), _1.split_0(newLevel), _0.split_1(newLevel), _1.split_1(newLevel)
        )
    }
}

object LongLinkedTrieSetN4 {

    def apply(
        _0: LongLinkedTrieSetNode,
        _1: LongLinkedTrieSetNode,
        _2: LongLinkedTrieSetNode,
        _3: LongLinkedTrieSetNode
    ): LongLinkedTrieSetN4 = {
        if (_0 eq null) {
            if (_1 eq null) {
                //(0,0,?,?)
                new LongLinkedTrieSetN4_2_3(_2, _3)
            } else {
                //(0,1,?,?)
                if (_2 eq null) {
                    //(0,1,0,?)
                    new LongLinkedTrieSetN4_1_3(_1, _3)
                } else {
                    //(0,1,1,?)
                    if (_3 eq null) {
                        //(0,1,1,0)
                        new LongLinkedTrieSetN4_1_2(_1, _2)
                    } else {
                        //(0,1,1,1)
                        new LongLinkedTrieSetN4_0_1_2_3(null, _1, _2, _3)
                    }
                }
            }
        } else {
            if (_1 eq null) {
                //(1,0,?,?)
                if (_2 eq null) {
                    //(1,0,0,?)
                    new LongLinkedTrieSetN4_0_3(_0, _3)
                } else {
                    //(1,0,1,?)
                    if (_3 eq null) {
                        //(1,0,1,0)
                        new LongLinkedTrieSetN4_0_2(_0, _2)
                    } else {
                        //(1,0,1,1)
                        new LongLinkedTrieSetN4_0_1_2_3(_0, null, _2, _3)
                    }
                }
            } else {
                //(1,1,?,?)
                if (_2 eq null) {
                    //(1,1,0,?)
                    if (_3 eq null) {
                        //(1,1,0,0)
                        new LongLinkedTrieSetN4_0_1(_0, _1)
                    } else {
                        //(1,1,0,1)
                        new LongLinkedTrieSetN4_0_1_2_3(_0, _1, null, _3)
                    }
                } else {
                    //(1,1,1,?)
                    new LongLinkedTrieSetN4_0_1_2_3(_0, _1, _2, _3)
                }
            }
        }
    }

}

/** The inner nodes of the trie set. */
private[immutable] abstract class LongLinkedTrieSetN4 extends LongLinkedTrieSetInnerNode {

    protected def _0: LongLinkedTrieSetNode
    protected def _1: LongLinkedTrieSetNode
    protected def _2: LongLinkedTrieSetNode
    protected def _3: LongLinkedTrieSetNode

    final override def isN4: Boolean = true

    final override def split_0(level: Int): LongLinkedTrieSetNode = {
        // We keep 00 and 10... i.e. we extract 0 and 2
        if (_0 eq null) {
            if (_2 eq null)
                null
            else
                _2 match {
                    case n: LongLinkedTrieSetL       => n
                    case n: LongLinkedTrieSetN2      => new LongLinkedTrieSetN4_1_3(n._0, n._1)
                    case n: LongLinkedTrieSetN4      => new LongLinkedTrieSetNShared_1(n)
                    case n: LongLinkedTrieSetNShared => n.prependBit(1)
                }
        } else if (_2 eq null) {
            _0 match {
                case n: LongLinkedTrieSetL       => n
                case n: LongLinkedTrieSetN2      => new LongLinkedTrieSetN4_0_2(n._0, n._1)
                case n: LongLinkedTrieSetN4      => new LongLinkedTrieSetNShared_0(n)
                case n: LongLinkedTrieSetNShared => n.prependBit(0)
            }
        } else {
            LongLinkedTrieSetN2(level + 1, _0, _2)
        }
    }
    final override def split_1(level: Int): LongLinkedTrieSetNode = {
        // We keep 01 and 11... i.e. we extract 1 and 3
        if (_1 eq null) {
            if (_3 eq null)
                null
            else
                _3 match {
                    case n: LongLinkedTrieSetL       => n
                    case n: LongLinkedTrieSetN2      => new LongLinkedTrieSetN4_1_3(n._0, n._1)
                    case n: LongLinkedTrieSetN4      => new LongLinkedTrieSetNShared_1(n)
                    case n: LongLinkedTrieSetNShared => n.prependBit(1)
                }
        } else if (_3 eq null) {
            _1 match {
                case n: LongLinkedTrieSetL       => n
                case n: LongLinkedTrieSetN2      => new LongLinkedTrieSetN4_0_2(n._0, n._1)
                case n: LongLinkedTrieSetN4      => new LongLinkedTrieSetNShared_0(n)
                case n: LongLinkedTrieSetNShared => n.prependBit(0)
            }
        } else {
            LongLinkedTrieSetN2(level + 1, _1, _3)
        }
    }

    final override def toString(level: Int): String = {
        val indent = " " * level
        val lP2 = level + 4
        var s = "N4("
        if (_0 ne null) s += s"\n$indent 00=>${_0.toString(lP2)}"
        if (_1 ne null) s += s"\n$indent 01=>${_1.toString(lP2)}"
        if (_2 ne null) s += s"\n$indent 10=>${_2.toString(lP2)}"
        if (_3 ne null) s += s"\n$indent 11=>${_3.toString(lP2)}"
        s+")"
    }
}

final private[immutable] class LongLinkedTrieSetN4_0_1(
        final val _0: LongLinkedTrieSetNode,
        final val _1: LongLinkedTrieSetNode
) extends LongLinkedTrieSetN4 {
    final override protected def _2: LongLinkedTrieSetNode = null
    final override protected def _3: LongLinkedTrieSetNode = null

    override def contains(v: Long, key: Long): Boolean = {
        (key & 3L) match {
            case 0L => _0.contains(v, key >> 2)
            case 1L => _1.contains(v, key >> 2)
            case _  => false
        }
    }

    override def add(l: LongLinkedTrieSetL, level: Int): LongLinkedTrieSetNode = {
        val consideredBits = ((l.value >> level) & 3L).toInt
        (consideredBits: @switch) match {
            case 0 =>
                val new0 = _0.add(l, level + 2)
                if (_0 ne new0) {
                    new LongLinkedTrieSetN4_0_1(new0, this._1)
                } else {
                    this
                }

            case 1 =>
                val new1 = _1.add(l, level + 2)
                if (_1 ne new1) {
                    new LongLinkedTrieSetN4_0_1(this._0, new1)
                } else {
                    this
                }

            case 2 => new LongLinkedTrieSetN4_0_1_2_3(this._0, this._1, l, null)

            case 3 => new LongLinkedTrieSetN4_0_1_2_3(this._0, this._1, null, l)
        }
    }
}

final private[immutable] class LongLinkedTrieSetN4_0_2(
        final val _0: LongLinkedTrieSetNode,
        final val _2: LongLinkedTrieSetNode
) extends LongLinkedTrieSetN4 {
    final override protected def _1: LongLinkedTrieSetNode = null
    final override protected def _3: LongLinkedTrieSetNode = null

    override def contains(v: Long, key: Long): Boolean = {
        (key & 3L) match {
            case 0L => _0.contains(v, key >> 2)
            case 2L => _2.contains(v, key >> 2)
            case _  => false
        }
    }

    override def add(l: LongLinkedTrieSetL, level: Int): LongLinkedTrieSetNode = {
        // Basic assumption: the trie is nearly balanced...
        val consideredBits = ((l.value >> level) & 3L).toInt
        (consideredBits: @switch) match {
            case 0 =>
                val new0 = _0.add(l, level + 2)
                if (_0 ne new0) {
                    new LongLinkedTrieSetN4_0_2(new0, this._2)
                } else {
                    this
                }

            case 1 => new LongLinkedTrieSetN4_0_1_2_3(this._0, l, this._2, null)

            case 2 =>
                val new2 = _2.add(l, level + 2)
                if (_2 ne new2) {
                    new LongLinkedTrieSetN4_0_2(this._0, new2)
                } else {
                    this
                }

            case 3 => new LongLinkedTrieSetN4_0_1_2_3(this._0, null, this._2, l)
        }
    }
}

final private[immutable] class LongLinkedTrieSetN4_0_3(
        final val _0: LongLinkedTrieSetNode,
        final val _3: LongLinkedTrieSetNode
) extends LongLinkedTrieSetN4 {

    final override protected def _1: LongLinkedTrieSetNode = null
    final override protected def _2: LongLinkedTrieSetNode = null

    override def contains(v: Long, key: Long): Boolean = {
        (key & 3L) match {
            case 0L => _0.contains(v, key >> 2)
            case 3L => _3.contains(v, key >> 2)
            case _  => false
        }
    }

    override def add(l: LongLinkedTrieSetL, level: Int): LongLinkedTrieSetNode = {
        // Basic assumption: the trie is nearly balanced...
        val consideredBits = ((l.value >> level) & 3L).toInt
        (consideredBits: @switch) match {
            case 0 =>
                val new0 = _0.add(l, level + 2)
                if (_0 ne new0) {
                    new LongLinkedTrieSetN4_0_3(new0, this._3)
                } else {
                    this
                }

            case 1 => new LongLinkedTrieSetN4_0_1_2_3(this._0, l, null, this._3)

            case 2 => new LongLinkedTrieSetN4_0_1_2_3(this._0, null, l, this._3)

            case 3 =>
                val new3 = _3.add(l, level + 2)
                if (_3 ne new3) {
                    new LongLinkedTrieSetN4_0_3(this._0, new3)
                } else {
                    this
                }
        }
    }
}

final private[immutable] class LongLinkedTrieSetN4_1_2(
        final val _1: LongLinkedTrieSetNode,
        final val _2: LongLinkedTrieSetNode
) extends LongLinkedTrieSetN4 {
    final override protected def _0: LongLinkedTrieSetNode = null
    final override protected def _3: LongLinkedTrieSetNode = null

    override def contains(v: Long, key: Long): Boolean = {
        (key & 3L) match {
            case 1L => _1.contains(v, key >> 2)
            case 2L => _2.contains(v, key >> 2)
            case _  => false
        }
    }

    override def add(l: LongLinkedTrieSetL, level: Int): LongLinkedTrieSetNode = {
        // Basic assumption: the trie is nearly balanced...
        val consideredBits = ((l.value >> level) & 3L).toInt
        (consideredBits: @switch) match {
            case 0 => new LongLinkedTrieSetN4_0_1_2_3(l, this._1, this._2, null)

            case 1 =>
                val new1 = _1.add(l, level + 2)
                if (_1 ne new1) {
                    new LongLinkedTrieSetN4_1_2(new1, this._2)
                } else {
                    this
                }

            case 2 =>
                val new2 = _2.add(l, level + 2)
                if (_2 ne new2) {
                    new LongLinkedTrieSetN4_1_2(this._1, new2)
                } else {
                    this
                }

            case 3 => new LongLinkedTrieSetN4_0_1_2_3(null, this._1, this._2, l)

        }
    }
}

final private[immutable] class LongLinkedTrieSetN4_1_3(
        final val _1: LongLinkedTrieSetNode,
        final val _3: LongLinkedTrieSetNode
) extends LongLinkedTrieSetN4 {
    final override protected def _0: LongLinkedTrieSetNode = null
    final override protected def _2: LongLinkedTrieSetNode = null

    override def contains(v: Long, key: Long): Boolean = {
        (key & 3L) match {
            case 1L => _1.contains(v, key >> 2)
            case 3L => _3.contains(v, key >> 2)
            case _  => false
        }
    }

    override def add(l: LongLinkedTrieSetL, level: Int): LongLinkedTrieSetNode = {
        // Basic assumption: the trie is nearly balanced...
        val consideredBits = ((l.value >> level) & 3L).toInt
        (consideredBits: @switch) match {
            case 0 => new LongLinkedTrieSetN4_0_1_2_3(l, this._1, null, this._3)

            case 1 =>
                val new1 = _1.add(l, level + 2)
                if (_1 ne new1) {
                    new LongLinkedTrieSetN4_1_3(new1, this._3)
                } else {
                    this
                }

            case 2 => new LongLinkedTrieSetN4_0_1_2_3(null, this._1, l, this._3)

            case 3 =>
                val new3 = _3.add(l, level + 2)
                if (_3 ne new3) {
                    new LongLinkedTrieSetN4_1_3(this._1, new3)
                } else {
                    this
                }
        }
    }
}

final private[immutable] class LongLinkedTrieSetN4_2_3(
        final val _2: LongLinkedTrieSetNode,
        final val _3: LongLinkedTrieSetNode
) extends LongLinkedTrieSetN4 {
    final override protected def _0: LongLinkedTrieSetNode = null
    final override protected def _1: LongLinkedTrieSetNode = null

    override def contains(v: Long, key: Long): Boolean = {
        (key & 3L) match {
            case 2L => _2.contains(v, key >> 2)
            case 3L => _3.contains(v, key >> 2)
            case _  => false
        }
    }

    override def add(l: LongLinkedTrieSetL, level: Int): LongLinkedTrieSetNode = {
        // Basic assumption: the trie is nearly balanced...
        val consideredBits = ((l.value >> level) & 3L).toInt
        (consideredBits: @switch) match {

            case 0 => new LongLinkedTrieSetN4_0_1_2_3(l, null, this._2, this._3)

            case 1 => new LongLinkedTrieSetN4_0_1_2_3(null, l, this._2, this._3)

            case 2 =>
                val new2 = _2.add(l, level + 2)
                if (_2 ne new2) {
                    new LongLinkedTrieSetN4_2_3(new2, this._3)
                } else {
                    this
                }

            case 3 =>
                val new3 = _3.add(l, level + 2)
                if (_3 ne new3) {
                    new LongLinkedTrieSetN4_2_3(this._2, new3)
                } else {
                    this
                }
        }
    }
}

final private[immutable] class LongLinkedTrieSetN4_0_1_2_3(
        // least significant bits _ (current) second most important bit _ (current) most important bit
        final val _0: LongLinkedTrieSetNode, // a tree node, a leaf node or null
        final val _1: LongLinkedTrieSetNode, // a tree node, a leaf node or null
        final val _2: LongLinkedTrieSetNode, // a tree node, a leaf node or null
        final val _3: LongLinkedTrieSetNode // a tree node, a leaf node or null
) extends LongLinkedTrieSetN4 {

    override def contains(v: Long, key: Long): Boolean = {
        (key & 3L).toInt match {
            case 0 => _0 != null && _0.contains(v, key >> 2)
            case 1 => _1 != null && _1.contains(v, key >> 2)
            case 2 => _2 != null && _2.contains(v, key >> 2)
            case 3 => _3 != null && _3.contains(v, key >> 2)
        }
    }

    override def add(l: LongLinkedTrieSetL, level: Int): LongLinkedTrieSetNode = {
        // Basic assumption: the trie is nearly balanced...
        val consideredBits = ((l.value >> level) & 3L).toInt
        (consideredBits: @switch) match {
            case 0 =>
                val _0 = this._0
                if (_0 == null) {
                    new LongLinkedTrieSetN4_0_1_2_3(l, this._1, this._2, this._3)
                } else {
                    val new0 = _0.add(l, level + 2)
                    if (_0 ne new0) {
                        new LongLinkedTrieSetN4_0_1_2_3(new0, this._1, this._2, this._3)
                    } else {
                        this
                    }
                }
            case 1 =>
                val _1 = this._1
                if (_1 == null) {
                    new LongLinkedTrieSetN4_0_1_2_3(this._0, l, this._2, this._3)
                } else {
                    val new1 = _1.add(l, level + 2)
                    if (_1 ne new1) {
                        new LongLinkedTrieSetN4_0_1_2_3(this._0, new1, this._2, this._3)
                    } else {
                        this
                    }
                }

            case 2 =>
                val _2 = this._2
                if (_2 == null) {
                    new LongLinkedTrieSetN4_0_1_2_3(this._0, this._1, l, this._3)
                } else {
                    val new2 = _2.add(l, level + 2)
                    if (_2 ne new2) {
                        new LongLinkedTrieSetN4_0_1_2_3(this._0, this._1, new2, this._3)
                    } else {
                        this
                    }
                }

            case 3 =>
                val _3 = this._3
                if (_3 == null) {
                    new LongLinkedTrieSetN4_0_1_2_3(this._0, this._1, this._2, l)
                } else {
                    val new3 = _3.add(l, level + 2)
                    if (_3 ne new3) {
                        new LongLinkedTrieSetN4_0_1_2_3(this._0, this._1, this._2, new3)
                    } else {
                        this
                    }
                }
        }
    }
}

object LargeLongLinkedTrieSet {

    final val InitialBucketsCount = 8

    def initialBucketsCount: Int = InitialBucketsCount
}

private[immutable] class LargeLongLinkedTrieSet(
        var size: Int = 0,
        // the length of the array must be a power of 2!
        val tries: Array[LongLinkedTrieSetNode] = new Array(LargeLongLinkedTrieSet.InitialBucketsCount),
        var l:     LongLinkedTrieSetL           = null // points to the latest element that was added...
) extends LongLinkedTrieSet { set =>

    final override def +(v: Long): LargeLongLinkedTrieSet = {
        def split(
            newSize:      Int,
            newL:         LongLinkedTrieSetL,
            oldTrieId:    Int,
            newTrie:      LongLinkedTrieSetNode,
            bucketsCount: Int
        ): LargeLongLinkedTrieSet = {
            val thisTries = this.tries
            val lastOldTrieId = bucketsCount / 2
            val targetTries = new Array[LongLinkedTrieSetNode](bucketsCount)
            val level = JInt.numberOfTrailingZeros(lastOldTrieId)
            var i = 0
            while (i < lastOldTrieId) {
                val trie = if (i == oldTrieId) newTrie else thisTries(i)
                if (trie ne null) {
                    targetTries(i) = trie.split_0(level)
                    targetTries(i + lastOldTrieId) = trie.split_1(level)
                }
                i += 1
            }
            new LargeLongLinkedTrieSet(newSize, targetTries, newL)
        }

        val tries = this.tries
        val triesLength = tries.length
        val trieId = v.toInt & (triesLength - 1)
        val oldTrie = tries(trieId)
        if (oldTrie == null) {
            val newSize = size + 1
            val newL = new LongLinkedTrieSetL(v, this.l)
            newSize match {
                // 6 20 40 80
                case 5  => split(newSize, newL, trieId, newL, 16)
                case 15 => split(newSize, newL, trieId, newL, 32)
                case 25 => split(newSize, newL, trieId, newL, 64)
                case 55 => split(newSize, newL, trieId, newL, 128)
                case _ =>
                    val newTries = tries.clone()
                    newTries(trieId) = newL
                    new LargeLongLinkedTrieSet(newSize, newTries, newL)
            }
        } else {
            val newL = new LongLinkedTrieSetL(v, this.l)
            val level = JInt.numberOfTrailingZeros(triesLength)
            val newTrie = oldTrie.add(newL, level)
            if (oldTrie ne newTrie) {
                val newSize = size + 1
                newSize match {
                    // IMPROVE: 5, 10, 20, 40, 80.... ?
                    case 5 =>
                        //println(this)
                        val updatedTrie = split(newSize, newL, trieId, newTrie, 16)
                        //println("after split: "+updatedTrie)
                        updatedTrie
                    case 15 =>
                        //println(this)
                        val updatedTrie = split(newSize, newL, trieId, newTrie, 32)
                        //println("after split: "+updatedTrie)
                        updatedTrie
                    case 25 =>
                        //println(this)
                        val updatedTrie = split(newSize, newL, trieId, newTrie, 64)
                        //println("after split: "+updatedTrie)
                        updatedTrie
                    case 55 =>
                        split(newSize, newL, trieId, newTrie, 128)
                    case _ =>
                        val newTries = tries.clone()
                        newTries(trieId) = newTrie
                        new LargeLongLinkedTrieSet(newSize, newTries, newL)
                }
            } else {
                this
            }
        }
    }

    final private[immutable] def +=(v: Long): Unit = {
        val tries = this.tries
        val triesLength = tries.length
        val trieId = v.toInt & (triesLength - 1)
        val oldTrie = tries(trieId)
        if (oldTrie == null) {
            val newL = new LongLinkedTrieSetL(v, this.l)
            tries(trieId) = newL
            this.l = newL
            size += 1
        } else {
            val newL = new LongLinkedTrieSetL(v, this.l)
            val level = JInt.numberOfTrailingZeros(triesLength)
            val newTrie = oldTrie.add(newL, level)
            if (oldTrie ne newTrie) {
                tries(trieId) = newTrie
                this.l = newL
                size += 1
            }
        }
    }

    final override def isEmpty: Boolean = size == 0
    final override def isSingletonSet: Boolean = size == 1

    final override def contains(v: Long): Boolean = {
        val tries = this.tries
        val triesLength = tries.length
        val trie = tries(v.toInt & (triesLength - 1))
        if (trie == null) return false;

        trie.contains(v, key = v >> JInt.numberOfTrailingZeros(triesLength))
    }

    final override def head: Long = {
        val thisL = this.l
        if (thisL eq null)
            throw new IllegalStateException("the set is empty");

        this.l.value
    }

    final override def foreach[U](f: Long => U): Unit = {
        var currentL = set.l
        while (currentL ne null) {
            val v = currentL.value
            currentL = currentL.next
            f(v)
        }
    }

    final override def forall(p: Long => Boolean): Boolean = {
        var currentL = set.l
        while (currentL ne null) {
            val v = currentL.value
            currentL = currentL.next
            if (!p(v)) return false;
        }
        true
    }

    final override def foldLeft[T](op: T)(f: (T, Long) => T): T = {
        var r = op
        var currentL = set.l
        while (currentL ne null) {
            val v = currentL.value
            currentL = currentL.next
            r = f(r, v)
        }
        r
    }

    final override def forFirstN[U](n: Int)(f: Long => U): Unit = {
        var i = 0
        var currentL = set.l
        while (i < n && (currentL ne null)) {
            val v = currentL.value
            currentL = currentL.next
            f(v)
            i += 1
        }
    }

    final override def iterator: LongIterator = new LongIterator {
        private[this] var currentL = set.l
        override def hasNext: Boolean = currentL ne null
        override def next(): Long = {
            val v = currentL.value
            currentL = currentL.next
            v
        }
    }

    final override def equals(other: Any): Boolean = {
        other match {
            case that: LargeLongLinkedTrieSet =>
                (this eq that) || {
                    this.size == that.size && this.iterator.sameValues(that.iterator)
                }
            case _ => false
        }
    }

    final override def hashCode: Int = {
        iterator.foldLeft(0)((hashCode, v) => (hashCode * 31) + java.lang.Long.hashCode(v))
    }

    final override def toString: String = {
        val tries = this.tries
        val triesLength = tries.length
        val bitsCount = JInt.numberOfTrailingZeros(triesLength)
        val triesString =
            tries.
                zipWithIndex.
                map { e =>
                    val (trie, index) = e
                    s"[${index.toBinaryString.reverse.padTo(bitsCount, '0').reverse}] "+(if (trie ne null) trie.toString(0) else "N/A")
                }.
                mkString("\n")
        s"LongLinkedTrieSet(#$size,tries=\n$triesString\n)"
    }

}
