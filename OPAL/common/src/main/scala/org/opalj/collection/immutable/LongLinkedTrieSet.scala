/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.collection
package immutable

import scala.annotation.switch

import java.lang.{Long ⇒ JLong}
import java.lang.Long.{hashCode ⇒ lHashCode}
import java.lang.Math.abs

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
sealed abstract class LongLinkedTrieSet {
    def size: Int
    def isEmpty: Boolean
    def isSingletonSet: Boolean
    def contains(v: Long): Boolean
    def foreach[U](f: Long ⇒ U): Unit
    def foldLeft[T](op: T)(f: (T, Long) ⇒ T): T
    def forFirstN[U](n: Int)(f: Long ⇒ U): Unit
    def head: Long
    def iterator: LongIterator
    def +(v: Long): LongLinkedTrieSet
}

object LongLinkedTrieSet {

    def empty: LongLinkedTrieSet = EmptyLongLinkedTrieSet

    def apply(v: Long): LongLinkedTrieSet = new LongLinkedTrieSet1(v)

    def apply(vNewer: Long, vOlder: Long): LongLinkedTrieSet = {
        if (vNewer == vOlder)
            new LongLinkedTrieSet1(vNewer)
        else
            new LongLinkedTrieSet2(vNewer, vOlder)
    }

}

case object EmptyLongLinkedTrieSet extends LongLinkedTrieSet {
    final override def size: Int = 0
    final override def isEmpty: Boolean = true
    final override def isSingletonSet: Boolean = false
    final override def contains(v: Long): Boolean = false
    final override def foreach[U](f: Long ⇒ U): Unit = { /*nothing to do*/ }
    final override def foldLeft[T](op: T)(f: (T, Long) ⇒ T): T = op
    final override def forFirstN[U](n: Int)(f: Long ⇒ U): Unit = { /*nothing to do*/ }
    final override def head: Long = throw new UnsupportedOperationException
    final override def iterator: LongIterator = LongIterator.empty
    final override def +(v: Long): LongLinkedTrieSet = LongLinkedTrieSet1(v)
}

final case class LongLinkedTrieSet1(v1: Long) extends LongLinkedTrieSet {
    final override def size: Int = 1
    final override def isEmpty: Boolean = false
    final override def isSingletonSet: Boolean = true
    final override def contains(v: Long): Boolean = v == v1
    final override def foreach[U](f: Long ⇒ U): Unit = f(v1)
    final override def foldLeft[T](op: T)(f: (T, Long) ⇒ T): T = f(op, v1)
    final override def forFirstN[U](n: Int)(f: Long ⇒ U): Unit = if (n > 0) f(v1)
    final override def head: Long = v1
    final override def iterator: LongIterator = LongIterator(v1)
    final override def +(v: Long): LongLinkedTrieSet = {
        if (v != v1) new LongLinkedTrieSet2(v, v1) else this
    }
}

final private[immutable] case class LongLinkedTrieSet2(v1: Long, v2: Long) extends LongLinkedTrieSet {
    final override def size: Int = 2
    final override def isEmpty: Boolean = false
    final override def isSingletonSet: Boolean = false
    final override def contains(v: Long): Boolean = v == v1 || v == v2
    final override def foreach[U](f: Long ⇒ U): Unit = { f(v1); f(v2) }
    final override def foldLeft[T](op: T)(f: (T, Long) ⇒ T): T = f(f(op, v1), v2)
    final override def forFirstN[U](n: Int)(f: Long ⇒ U): Unit = {
        if (n > 0) f(v1)
        if (n > 1) f(v2)
    }
    final override def head: Long = v1
    final override def iterator: LongIterator = LongIterator(v1, v2)
    final override def +(v: Long): LongLinkedTrieSet = {
        if (v != v1 && v != v2) new LongLinkedTrieSet3(v, v1, v2) else this
    }
}

final private[immutable] case class LongLinkedTrieSet3(v1: Long, v2: Long, v3: Long) extends LongLinkedTrieSet {
    final override def size: Int = 3
    final override def isEmpty: Boolean = false
    final override def isSingletonSet: Boolean = false
    final override def contains(v: Long): Boolean = v == v1 || v == v2 || v == v3
    final override def foreach[U](f: Long ⇒ U): Unit = { f(v1); f(v2); f(v3) }
    final override def foldLeft[T](op: T)(f: (T, Long) ⇒ T): T = f(f(f(op, v1), v2), v3)
    final override def forFirstN[U](n: Int)(f: Long ⇒ U): Unit = {
        if (n > 0) f(v1)
        if (n > 1) f(v2)
        if (n > 2) f(v3)
    }
    final override def head: Long = v1
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

    /** Returns the node for the path which has another "0" bit. */
    def split_0(level: Int): LongLinkedTrieSetNode
    /** Returns the node for the path which has another "1" bit. */
    def split_1(level: Int): LongLinkedTrieSetNode

    def +(l: LongLinkedTrieSetL, level: Int): LongLinkedTrieSetNode

    def toString(level: Int): String

}

/** The leaves of the trie set. */
final private[immutable] class LongLinkedTrieSetL(
        val value: Long,
        val next:  LongLinkedTrieSetL = null // `null` if this leaf is the first element that was added to the set.
) extends LongLinkedTrieSetNode {

    override def isN: Boolean = false
    override def isN4: Boolean = false
    override def isL: Boolean = true
    override def asL: LongLinkedTrieSetL = this

    override def +(l: LongLinkedTrieSetL, level: Int): LongLinkedTrieSetNode = {
        val thisValue = this.value
        val lValue = l.value
        if (thisValue == lValue)
            return this;

        // Let's check if there is some sharing and if so, let's use it.
        val thisValueShifted = thisValue >> level
        val lValueShifted = lValue >> level
        JLong.numberOfTrailingZeros(thisValueShifted ^ lValueShifted) match {
            case 0 ⇒
                if ((thisValueShifted & 1L) == 0L) {
                    new LongLinkedTrieSetN2(this, l)
                } else {
                    new LongLinkedTrieSetN2(l, this)
                }
            case 1 ⇒
                if ((thisValueShifted & 1L) == 0L) {
                    if ((thisValueShifted & 2L) == 0L) {
                        new LongLinkedTrieSetN4(this, null, l, null)
                    } else {
                        new LongLinkedTrieSetN4(l, null, this, null)
                    }
                } else {
                    if ((thisValueShifted & 2L) == 0L) {
                        new LongLinkedTrieSetN4(null, this, null, l)
                    } else {
                        new LongLinkedTrieSetN4(null, l, null, this)
                    }
                }
            case length ⇒
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

        // IMPROVE Implement LongLinkedTrieSet.contains here...?

    final override def isN4: Boolean = false

    def sharedBits: Long
    def length: Int // at least "1"
    def n: LongLinkedTrieSetNode

    def +(l: LongLinkedTrieSetL, level: Int): LongLinkedTrieSetNode = {
        val length = this.length
        val sharedBits = this.sharedBits
        val lValue = l.value
        val lValueShifted = lValue >> level
        if ((lValueShifted & LongSet.BitMasks(length)) == sharedBits) {
            val oldN = this.n
            val newN = oldN + (l, level + length)
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

        (length: @switch) match {
            case 1 ⇒
                // if (sharedBits == 0L /*test the last bit...*/ )
                //    new LongLinkedTrieSetNShared_0(n)
                // else
                //    new LongLinkedTrieSetNShared_1(n)
                (n: @unchecked) match {
                    case l: LongLinkedTrieSetN2 ⇒
                        if (sharedBits == 0L /*test the last bit...*/ ) {
                            new LongLinkedTrieSetN4(l._0, null, l._1, null)
                        } else {
                            new LongLinkedTrieSetN4(null, l._0, null, l._1)
                        }
                    case l: LongLinkedTrieSetN4 ⇒
                        if (sharedBits == 0L /*test the last bit...*/ )
                            new LongLinkedTrieSetNShared_0(n)
                        else
                            new LongLinkedTrieSetNShared_1(n)
                    // [will never occur!] case l: LongLinkedTrieSetL       ⇒ ...
                    // [will never occur!] case l: LongLinkedTrieSetNShared ⇒ ...
                }

            case 2 ⇒
                (sharedBits.toInt: @switch) match {
                    case 0 ⇒ new LongLinkedTrieSetNShared_00(n)
                    case 1 ⇒ new LongLinkedTrieSetNShared_01(n)
                    case 2 ⇒ new LongLinkedTrieSetNShared_10(n)
                    case 3 ⇒ new LongLinkedTrieSetNShared_11(n)
                }

            case _ ⇒
                new LongLinkedTrieSetNShared_X(sharedBits, length, n)
                /*
                Using the (current) optimized representation of shorter sequences
                of shared leads to less memory, but increases the overall time too
                much...
                if (length <= 26) {
                    val sharedBitsAndLength = (sharedBits.toInt << 5 | length)
                    new LongLinkedTrieSetNShared_X_Small(sharedBitsAndLength, n)
                } else {
                    new LongLinkedTrieSetNShared_X(sharedBits, length, n)
                }
                */
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

}

final private[immutable] class LongLinkedTrieSetNShared_1(
        final val n: LongLinkedTrieSetNode
) extends LongLinkedTrieSetNShared {
    override def sharedBits: Long = 1
    override def length: Int = 1
    override def split_0(level: Int): LongLinkedTrieSetNode = null
    override def split_1(level: Int): LongLinkedTrieSetNode = n

}

final private[immutable] class LongLinkedTrieSetNShared_00(
        final val n: LongLinkedTrieSetNode
) extends LongLinkedTrieSetNShared {
    override def sharedBits: Long = 0
    override def length: Int = 2
    override def split_0(level: Int): LongLinkedTrieSetNode = new LongLinkedTrieSetNShared_0(n)
    override def split_1(level: Int): LongLinkedTrieSetNode = null
}

final private[immutable] class LongLinkedTrieSetNShared_01(
        final val n: LongLinkedTrieSetNode
) extends LongLinkedTrieSetNShared {
    def sharedBits: Long = 1
    def length: Int = 2
    override def split_0(level: Int): LongLinkedTrieSetNode = null
    override def split_1(level: Int): LongLinkedTrieSetNode = new LongLinkedTrieSetNShared_0(n)
}

final private[immutable] class LongLinkedTrieSetNShared_10(
        final val n: LongLinkedTrieSetNode
) extends LongLinkedTrieSetNShared {
    def sharedBits: Long = 2
    def length: Int = 2
    override def split_0(level: Int): LongLinkedTrieSetNode = new LongLinkedTrieSetNShared_1(n)
    override def split_1(level: Int): LongLinkedTrieSetNode = null
}

final private[immutable] class LongLinkedTrieSetNShared_11(
        final val n: LongLinkedTrieSetNode
) extends LongLinkedTrieSetNShared {
    def sharedBits: Long = 3
    def length: Int = 2
    override def split_0(level: Int): LongLinkedTrieSetNode = null
    override def split_1(level: Int): LongLinkedTrieSetNode = new LongLinkedTrieSetNShared_1(n)
}

private[immutable] class LongLinkedTrieSetNShared_X_Small(
        // the last 5 bits determine the number of shared bits: up to 26 (32-5-"sign bit")
        val sharedBitsAndLength: Int,
        val n:                   LongLinkedTrieSetNode
) extends LongLinkedTrieSetNShared {

    def sharedBits: Long = (sharedBitsAndLength >> 5).toLong
    def length: Int = sharedBitsAndLength & 31

    override def split_0(level: Int): LongLinkedTrieSetNode = {
        if ((sharedBitsAndLength & 32) == 0) {
            LongLinkedTrieSetNShared((sharedBitsAndLength >> 6).toLong, length - 1, n)
        } else {
            null
        }
    }
    override def split_1(level: Int): LongLinkedTrieSetNode = {
        if ((sharedBitsAndLength & 32) == 32) {
            LongLinkedTrieSetNShared(
                (sharedBitsAndLength >> 6).toLong,
                length - 1, n
            )
        } else {
            null
        }
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
}

/** The inner nodes of the trie set. */
final private[immutable] class LongLinkedTrieSetN2(
        final val _0: LongLinkedTrieSetNode,
        final val _1: LongLinkedTrieSetNode
) extends LongLinkedTrieSetInnerNode {

    override def isN4: Boolean = false

    def +(l: LongLinkedTrieSetL, level: Int): LongLinkedTrieSetNode = {
        val _0 = this._0
        val _1 = this._1
        val lValue = l.value
        val lLSB = ((lValue >> level) & 1L) // lsb == bit at index `level`

        if (_0.isN || _1.isN) {
            // We can't get rid of this N2 node... a successor node is an inner node and we
            // do not want to perform "large" changes to the overall trie.
            return {
                if (lLSB == 0) {
                    val new_0 = _0 + (l, level + 1)
                    if (_0 ne new_0) {
                        new LongLinkedTrieSetN2(new_0, this._1)
                    } else {
                        this
                    }
                } else {
                    val new_1 = _1 + (l, level + 1)
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
                    val newN = _0 + (l, level + 2)
                    if (((_1.asL.value >> level + 1) & 1L) == 0L) {
                        new LongLinkedTrieSetN4(newN, _1, null, null)
                    } else {
                        new LongLinkedTrieSetN4(newN, null, null, _1)
                    }
                } else {
                    if (((_1.asL.value >> level + 1) & 1L) == 0L) {
                        new LongLinkedTrieSetN4(l, _1, _0, null)
                    } else {
                        new LongLinkedTrieSetN4(l, null, _0, _1)
                    }
                }
            } else {
                // l = ...10
                if (((_0Value >> level + 1) & 1) == 0L) {
                    // l = ...10, _0 = ...00
                    if (((_1.asL.value >> level + 1) & 1L) == 0L) {
                        new LongLinkedTrieSetN4(_0, _1, l, null)
                    } else {
                        new LongLinkedTrieSetN4(_0, null, l, _1)
                    }
                } else {
                    // l = ...10, _0 =...10
                    val newN = _0 + (l, level + 2)
                    if (((_1.asL.value >> level + 1) & 1L) == 0L) {
                        new LongLinkedTrieSetN4(null, _1, newN, null)
                    } else {
                        new LongLinkedTrieSetN4(null, null, newN, _1)
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
                    val newN = _1 + (l, level + 2)
                    if (((_0.asL.value >> level + 1) & 1L) == 0) {
                        new LongLinkedTrieSetN4(_0, newN, null, null)
                    } else {
                        new LongLinkedTrieSetN4(null, newN, _0, null)
                    }
                } else {
                    // l = ...01,_1 = ...11
                    if (((_0.asL.value >> level + 1) & 1L) == 0L) {
                        new LongLinkedTrieSetN4(_0, l, null, _1)
                    } else {
                        new LongLinkedTrieSetN4(null, l, _0, _1)
                    }
                }
            } else {
                // l = ...11
                if (((_1Value >> level + 1) & 1L) == 0L) {
                    // l = ...11, _1 = ...01
                    if (((_0.asL.value >> level + 1) & 1L) == 0L) {
                        new LongLinkedTrieSetN4(_0, _1, null, l)
                    } else {
                        new LongLinkedTrieSetN4(null, _1, _0, l)
                    }
                } else {
                    // l = ...11, _1 = ...11
                    val newN = _1 + (l, level + 2)
                    if (((_0.asL.value >> level + 1) & 1L) == 0L) {
                        new LongLinkedTrieSetN4_0_3(_0, null, null, newN)
                    } else {
                        new LongLinkedTrieSetN4_2_3(null, null, _0, newN)
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
        level: Int,
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

/** The inner nodes of the trie set. */
 private[immutable] abstract class LongLinkedTrieSetN4 extends LongLinkedTrieSetInnerNode {
       
    // IMPROVE make them protected... they are defined primarily for "toString"
        def _0: LongLinkedTrieSetNode
        def _1: LongLinkedTrieSetNode
        def _2: LongLinkedTrieSetNode
        def _3: LongLinkedTrieSetNode

    final override def isN4: Boolean = true

    final override def split_0(level: Int): LongLinkedTrieSetNode = throw new UnknownError()
    final override def split_1(level: Int): LongLinkedTrieSetNode = throw new UnknownError()

    final override def toString(level: Int): String = {
        val indent = " " * level
        val lP2 = level + 4
        var s = "N4("
        if (_0 ne null) s += s"\n$indent 00=>${ _0.toString(lP2)}" 
        if (_1 ne null) s += s"\n$indent 01=>${ _1.toString(lP2)}"
        if (_2 ne null) s += s"\n$indent 10=>${_2.toString(lP2)}"
        if (_3 ne null) s += s"\n$indent 11=>${_3.toString(lP2)})"
            s
    }
}


final private[immutable] class LongLinkedTrieSetN4_0_1(
        final val _0: LongLinkedTrieSetNode,
        final val _1: LongLinkedTrieSetNode
) extends LongLinkedTrieSetN4 {
    final override def _2: LongLinkedTrieSetNode = null
    final override def _3: LongLinkedTrieSetNode = null

    override def +(l: LongLinkedTrieSetL, level: Int): LongLinkedTrieSetNode = {
        val consideredBits = ((l.value >> level) & 3L).toInt
        (consideredBits: @switch) match {
            case 0 ⇒
                val _0 = this._0
                if (_0 == null) {
                    new LongLinkedTrieSetN4(l, this._1, this._2, this._3)
                } else {
                    val new0 = _0 + (l, level + 2)
                    if (_0 ne new0) {
                        new LongLinkedTrieSetN4(new0, this._1, this._2, this._3)
                    } else {
                        this
                    }
                }
            case 1 ⇒
                val _1 = this._1
                if (_1 == null) {
                    new LongLinkedTrieSetN4(this._0, l, this._2, this._3)
                } else {
                    val new1 = _1 + (l, level + 2)
                    if (_1 ne new1) {
                        new LongLinkedTrieSetN4(this._0, new1, this._2, this._3)
                    } else {
                        this
                    }
                }

            case 2 ⇒
                val _2 = this._2
                if (_2 == null) {
                    new LongLinkedTrieSetN4(this._0, this._1, l, this._3)
                } else {
                    val new2 = _2 + (l, level + 2)
                    if (_2 ne new2) {
                        new LongLinkedTrieSetN4(this._0, this._1, new2, this._3)
                    } else {
                        this
                    }
                }

            case 3 ⇒
                val _3 = this._3
                if (_3 == null) {
                    new LongLinkedTrieSetN4(this._0, this._1, this._2, l)
                } else {
                    val new3 = _3 + (l, level + 2)
                    if (_3 ne new3) {
                        new LongLinkedTrieSetN4(this._0, this._1, this._2, new3)
                    } else {
                        this
                    }
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

    override def +(l: LongLinkedTrieSetL, level: Int): LongLinkedTrieSetNode = {
        // Basic assumption: the trie is nearly balanced...
        val consideredBits = ((l.value >> level) & 3L).toInt
        (consideredBits: @switch) match {
            case 0 ⇒
                val _0 = this._0
                if (_0 == null) {
                    new LongLinkedTrieSetN4_0_1_2_3(l, this._1, this._2, this._3)
                } else {
                    val new0 = _0 + (l, level + 2)
                    if (_0 ne new0) {
                        new LongLinkedTrieSetN4_0_1_2_3(new0, this._1, this._2, this._3)
                    } else {
                        this
                    }
                }
            case 1 ⇒
                val _1 = this._1
                if (_1 == null) {
                    new LongLinkedTrieSetN4_0_1_2_3(this._0, l, this._2, this._3)
                } else {
                    val new1 = _1 + (l, level + 2)
                    if (_1 ne new1) {
                        new LongLinkedTrieSetN4_0_1_2_3(this._0, new1, this._2, this._3)
                    } else {
                        this
                    }
                }

            case 2 ⇒
                val _2 = this._2
                if (_2 == null) {
                    new LongLinkedTrieSetN4_0_1_2_3(this._0, this._1, l, this._3)
                } else {
                    val new2 = _2 + (l, level + 2)
                    if (_2 ne new2) {
                        new LongLinkedTrieSetN4_0_1_2_3(this._0, this._1, new2, this._3)
                    } else {
                        this
                    }
                }

            case 3 ⇒
                val _3 = this._3
                if (_3 == null) {
                    new LongLinkedTrieSetN4_0_1_2_3(this._0, this._1, this._2, l)
                } else {
                    val new3 = _3 + (l, level + 2)
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
    final val InitialBucketsCount = 16

    def initialBucketsCount: Int = InitialBucketsCount
}

private[immutable] class LargeLongLinkedTrieSet(
        var size:  Int                          = 0,
        val tries: Array[LongLinkedTrieSetNode] = new Array(LargeLongLinkedTrieSet.InitialBucketsCount),
        var l:     LongLinkedTrieSetL           = null // points to the latest element that was added...
) extends LongLinkedTrieSet { set ⇒

    final override def isEmpty: Boolean = size == 0
    final override def isSingletonSet: Boolean = size == 1

    final override def contains(v: Long): Boolean = {
        val tries = this.tries
        val trie = tries(Math.abs(JLong.hashCode(v)) % tries.length)
        if (trie == null) return false;

        var key = v
        var node = trie
        do {
            // Type based switch (proofed to be faster than introducing node ids and using them...):
            node match {
                case n: LongLinkedTrieSetNShared ⇒
                    val sharedBits = n.sharedBits
                    val length = n.length
                    if ((key & LongSet.BitMasks(length)) == sharedBits) {
                        node = n.n
                        key = key >> n.length
                    } else {
                        return false;
                    }
                case n: LongLinkedTrieSetN2 ⇒
                    if ((key & 1L) == 0L) {
                        node = n._0
                    } else {
                        node = n._1
                    }
                    key = key >> 1
                case n: LongLinkedTrieSetN4 ⇒
                    ((key & 3L /*binary:11*/ ).toInt: @switch) match {
                        case 0 ⇒ node = n._0
                        case 1 ⇒ node = n._1
                        case 2 ⇒ node = n._2
                        case 3 ⇒ node = n._3
                    }
                    key = key >> 2
                case l: LongLinkedTrieSetL ⇒
                    return v == l.value;
            }
        } while (node ne null)
        false
    }

    final override def head: Long = {
        val thisL = this.l
        if (thisL eq null)
            throw new IllegalStateException("the set is empty");

        this.l.value
    }

    final override def foreach[U](f: Long ⇒ U): Unit = {
        var currentL = set.l
        while (currentL ne null) {
            val v = currentL.value
            currentL = currentL.next
            f(v)
        }
    }

    final override def foldLeft[T](op: T)(f: (T, Long) ⇒ T): T = {
        var r = op
        var currentL = set.l
        while (currentL ne null) {
            val v = currentL.value
            currentL = currentL.next
            r = f(r, v)
        }
        r
    }

    final override def forFirstN[U](n: Int)(f: Long ⇒ U): Unit = {
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
        override def next: Long = {
            val v = currentL.value
            currentL = currentL.next
            v
        }
    }

    final override def +(v: Long): LargeLongLinkedTrieSet = {
        def extend(s: LargeLongLinkedTrieSet, newL: LongLinkedTrieSetL): Unit = {
            val tries = s.tries
            val trieId = abs(lHashCode(newL.value)) % tries.length
            val oldTrie = tries(trieId)
            if (oldTrie == null) {
                tries(trieId) = newL
            } else {
                tries(trieId) = oldTrie + (newL, 0)
            }
        }

        def rehash(newSize: Int, newL: LongLinkedTrieSetL, bucketsCount: Int): LargeLongLinkedTrieSet = {
            val newLLLTS = new LargeLongLinkedTrieSet(newSize, new Array(bucketsCount), newL)
            var currentL = newL
            while (currentL ne null) {
                extend(newLLLTS, currentL)
                currentL = currentL.next
            }
            newLLLTS
        }

        val tries = this.tries
        val trieId = abs(lHashCode(v)) % tries.length
        val oldTrie = tries(trieId)
        if (oldTrie == null) {
            val newSize = size + 1
            val newL = new LongLinkedTrieSetL(v, this.l)
            newSize match {
                case 24 ⇒ rehash(newSize, newL, 32)
                case 48 ⇒ rehash(newSize, newL, 64)
                case _ ⇒
                    val newTries = tries.clone()
                    newTries(trieId) = newL
                    new LargeLongLinkedTrieSet(newSize, newTries, newL)
            }
        } else {
            val newL = new LongLinkedTrieSetL(v, this.l)
            val newTrie = oldTrie + (newL, 0)
            if (oldTrie ne newTrie) {
                val newSize = size + 1
                newSize match {
                    case 24 ⇒ rehash(newSize, newL, 32)
                    case 48 ⇒ rehash(newSize, newL, 64)
                    case _ ⇒
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
        val trieId = abs(lHashCode(v)) % tries.length
        val oldTrie = tries(trieId)
        if (oldTrie == null) {
            val newL = new LongLinkedTrieSetL(v, this.l)
            tries(trieId) = newL
            this.l = newL
            size += 1
        } else {
            val newL = new LongLinkedTrieSetL(v, this.l)
            val newTrie = oldTrie + (newL, 0)
            if (oldTrie ne newTrie) {
                tries(trieId) = newTrie
                this.l = newL
                size += 1
            }
        }
    }

    final override def equals(other: Any): Boolean = {
        other match {
            case that: LargeLongLinkedTrieSet ⇒ (this eq that) || {
                this.size == that.size &&
                    this.iterator.sameValues(that.iterator)
            }
            case _ ⇒ false
        }
    }

    final override def hashCode: Int = {
        iterator.foldLeft(0)((hashCode, v) ⇒ (hashCode * 31) + java.lang.Long.hashCode(v))
    }

    final override def toString: String = {
        val triesString =
            tries.
                zipWithIndex.
                map { e ⇒
                    val (trie, index) = e
                    s"[$index] "+(if (trie ne null) trie.toString(0) else "N/A")
                }.
                mkString("\n")
        s"LongLinkedTrieSet(#$size,tries=\n$triesString\n)"
    }

}

