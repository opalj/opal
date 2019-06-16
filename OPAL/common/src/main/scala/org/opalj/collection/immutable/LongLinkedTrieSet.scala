/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.collection
package immutable

import scala.annotation.switch

import java.lang.{Long ⇒ JLong}

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
    def forFirstN[U](n: Int)(f: Long ⇒ U): Unit
    def head: Long
    def iterator: LongIterator
    def +(v: Long): LongLinkedTrieSet
}

object LongLinkedTrieSet {

    def empty: LongLinkedTrieSet = EmptyLongLinkedTrieSet

}

case object EmptyLongLinkedTrieSet extends LongLinkedTrieSet {
    final override def size: Int = 0
    final override def isEmpty: Boolean = true
    final override def isSingletonSet: Boolean = false
    final override def contains(v: Long): Boolean = false
    final override def foreach[U](f: Long ⇒ U): Unit = { /*nothing to do*/ }
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
            val v3N = new LongLinkedTrieSetL(v3)
            val v2N = new LongLinkedTrieSetL(v2, v3N)
            val set = new LargeLongLinkedTrieSet(2, v3N + (v2N, 0), v2N)
            set += v1
            set += v
            set
        } else
            this
    }
}

/** The super type of the nodes of the trie set. */
private[immutable] abstract class LongLinkedTrieSetNode {

    /** `true` if this is an inner node. */
    def isN: Boolean
    /** `true` if this is a leaf node. */
    def isL: Boolean
    def asL: LongLinkedTrieSeL

    def +(l: LongLinkedTrieSetL, level: Int): LongLinkedTrieSetNode

    def toString(level: Int): String

}

/** The leaves of the trie set. */
final private[immutable] case class LongLinkedTrieSetL(
        value: Long,
        next:  LongLinkedTrieSetL = null // `null` if this leaf is the first element that was added to the set.
) extends LongLinkedTrieSetNode {

    override def isN: Boolean = false
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
                if ((thisValueShifted & 1) == 0) {
                    new LongLinkedTrieSetN2(this, l)
                } else {
                    new LongLinkedTrieSetN2(l, this)
                }
            case 1 ⇒
                if ((thisValueShifted & 1) == 0) {
                    if (((thisValueShifted >> 1) & 1) == 0) {
                        new LongLinkedTrieSetN4(this, l, null, null)
                    } else {
                        new LongLinkedTrieSetN4(l, null, this, null)
                    }
                } else {
                    if (((thisValueShifted >> 1) & 1) == 0) {
                        new LongLinkedTrieSetN4(null, this, null, l)
                    } else {
                        new LongLinkedTrieSetN4(null, null, l, this)
                    }
                }
            case length ⇒
                val sharedBits = thisValueShifted & LongSet.bitMask(length)
                val n =
                    if (((thisValueShifted >> length) & 1) == 0)
                        new LongLinkedTrieSetN2(this, l)
                    else
                        new LongLinkedTrieSetN2(l, this)
                new LongLinkedTrieSetNShared(sharedBits, length, n)
        }
    }

    final override def toString(level: Int): String = s"L(${value.toBinaryString}=$value)"

}

/** The inner nodes of the trie set. */
private[immutable] abstract class LongLinkedTrieSetInnerNode extends LongLinkedTrieSetNode {
    final override def isN: Boolean = true
    final override def isL: Boolean = false
    final override def asL: LongLinkedTrieSeL = throw new ClassCastException()
}

final private[immutable] class LongLinkedTrieSetNShared(
        val sharedBits: Long, // at least 1...
        val length:     Int,
        val n:          LongLinkedTrieSetNode
) extends LongLinkedTrieSetInnerNode {

    def +(l: LongLinkedTrieSetL, level: Int): LongLinkedTrieSetNode = {
        val length = this.length
        val lValue = l.value
        if ((lValue & LongSet.bitMask(length)) == sharedBits) {
            val old_n = this.n
            val new_n = old_n + (l, level + length)
            if (old_n ne new_n) {
                new LongLinkedTrieSetNShared(sharedBits, length, new_n)
            } else {
                this
            }
        } else {
            // The length is at least 2 and
            // the number of shared bits is smaller than the current length.
            val lValueShifted = lValue >> level
            val lengthOfLead = JLong.numberOfTrailingZeros(sharedBits ^ lValueShifted)
            val lengthOfTail = length -lengthOfLead -1 /* -1 for the differing bit */  

            // We have to fold the tail if the number of shared remaining bits is one
            // We have to fold the lead if the number of shared initial bits is one
            
             match {
                case 0         ⇒
                case 1         ⇒
                case newLength ⇒
            }
            ???
        }
    }

    def toString(level: Int): String = {
        val lP2 = level + length + 2
        s"NShared(_${sharedBits.toBinaryString}(#$length)=>${n.toString(lP2)}"
    }
}

/** The inner nodes of the trie set. */
final private[immutable] class LongLinkedTrieSetN2(
        val _0: LongLinkedTrieSetNode,
        val _1: LongLinkedTrieSetNode
) extends LongLinkedTrieSetInnerNode {

    def +(l: LongLinkedTrieSetL, level: Int): LongLinkedTrieSetNode = {
        val _0 = this._0
        val _1 = this._1
        val lValue = l.value
        val lLSB = ((lValue >> level) & 1) // lsb == bit at index `level`

        if (_0.isN || _1.isN) {
            // We can't get rid of this N2 node...
            //if(lLSB == 0) {
            //    if(_0.)
            //}
            ???
        }

        
        if ( l_lsb == 0) {
            val _0Value = this._0.asL.value

            if (_0Value == lValue)
                return this;

            if (((lValue >> level + 1) & 1) == 0) {
                // l = ...00
                if (((_0Value >> level + 1) & 1) == 0) {
                    val newN = _0 + (l, level + 2)
                    if (((_1.value >> level + 1) & 1) == 0) {
                        new LongLinkedTrieSetN4(newN, _1, null, null)
                    } else {
                        new LongLinkedTrieSetN4(newN, null, null, _1)
                    }
                } else {
                    if (((_1.value >> level + 1) & 1) == 0) {
                        new LongLinkedTrieSetN4(l, _1, _0, null)
                    } else {
                        new LongLinkedTrieSetN4(l, null, _0, _1)
                    }
                }
            } else {
                // l = ...10
                if (((_0Value >> level + 1) & 1) == 0) {
                    // l = ...10, _0 = ...00
                    if (((_1.value >> level + 1) & 1) == 0) {
                        new LongLinkedTrieSetN4(_0, _1, l, null)
                    } else {
                        new LongLinkedTrieSetN4(_0, null, l, _1)
                    }
                } else {
                    // l = ...10, _0 =...10
                    val newN = _0 + (l, level + 2)
                    if (((_1.value >> level + 1) & 1) == 0) {
                        new LongLinkedTrieSetN4(null, _1, newN, null)
                    } else {
                        new LongLinkedTrieSetN4(null, null, newN, _1)
                    }
                }
            }
        } else {
            val _1Value = this._1.value

            if (_1Value == lValue)
                return this;

            if (((lValue >> level + 1) & 1) == 0) {
                // l = ...01
                if (((_1Value >> level + 1) & 1) == 0) {
                    // l = ...01, _1 = ...01
                    val newN = _1 + (l, level + 2)
                    if (((_0.value >> level + 1) & 1) == 0) {
                        new LongLinkedTrieSetN4(_0, newN, null, null)
                    } else {
                        new LongLinkedTrieSetN4(null, newN, _0, null)
                    }
                } else {
                    // l = ...01,_1 = ...11
                    if (((_0.value >> level + 1) & 1) == 0) {
                        new LongLinkedTrieSetN4(_0, l, null, _1)
                    } else {
                        new LongLinkedTrieSetN4(null, l, _0, _1)
                    }
                }
            } else {
                // l = ...11
                if (((_1Value >> level + 1) & 1) == 0) {
                    // l = ...11, _1 = ...01
                    if (((_0.value >> level + 1) & 1) == 0) {
                        new LongLinkedTrieSetN4(_0, _1, null, l)
                    } else {
                        new LongLinkedTrieSetN4(null, _1, _0, l)
                    }
                } else {
                    // l = ...11, _1 = ...11
                    val newN = _1 + (l, level + 2)
                    if (((_0.value >> level + 1) & 1) == 0) {
                        new LongLinkedTrieSetN4(_0, null, null, newN)
                    } else {
                        new LongLinkedTrieSetN4(null, null, _0, newN)
                    }
                }
            }
        }
    }

    def toString(level: Int): String = {
        val indent = " " * level
        val lP2 = level + 2
        s"N2(\n$indent 0=>${_0.toString(lP2)}\n$indent 1=>${_1.toString(lP2)})"
    }
}

/** The inner nodes of the trie set. */
final private[immutable] class LongLinkedTrieSetN4(
        // least significant bits _ (current) second most important bit _ (current) most important bit
        val _0: LongLinkedTrieSetNode, // a tree node, a leaf node or null
        val _1: LongLinkedTrieSetNode, // a tree node, a leaf node or null
        val _2: LongLinkedTrieSetNode, // a tree node, a leaf node or null
        val _3: LongLinkedTrieSetNode // a tree node, a leaf node or null
) extends LongLinkedTrieSetInnerNode {

    override def +(l: LongLinkedTrieSetL, level: Int): LongLinkedTrieSetNode = {
        // Basic assumption: the trie is nearly balanced...
        val consideredBits = ((l.value >> level) & 3).toInt
        (consideredBits: @switch) match {
            case 0 ⇒
                if (_0 == null) {
                    new LongLinkedTrieSetN4(l, this._1, this._2, this._3)
                } else {
                    val old0 = this._0
                    val new0 = old0 + (l, level + 2)
                    if (old0 ne new0) {
                        new LongLinkedTrieSetN4(new0, this._1, this._2, this._3)
                    } else {
                        this
                    }
                }
            case 1 ⇒
                if (_1 == null) {
                    new LongLinkedTrieSetN4(this._0, l, this._2, this._3)
                } else {
                    val old1 = this._1
                    val new1 = old1 + (l, level + 2)
                    if (old1 ne new1) {
                        new LongLinkedTrieSetN4(this._0, new1, this._2, this._3)
                    } else {
                        this
                    }
                }

            case 2 ⇒
                if (_2 == null) {
                    new LongLinkedTrieSetN4(this._0, this._1, l, this._3)
                } else {
                    val old2 = this._2
                    val new2 = old2 + (l, level + 2)
                    if (old2 ne new2) {
                        new LongLinkedTrieSetN4(this._0, this._1, new2, this._3)
                    } else {
                        this
                    }
                }

            case 3 ⇒
                if (_3 == null) {
                    new LongLinkedTrieSetN4(this._0, this._1, this._2, l)
                } else {
                    val old3 = this._3
                    val new3 = old3 + (l, level + 2)
                    if (old3 ne new3) {
                        new LongLinkedTrieSetN4(this._0, this._1, this._2, new3)
                    } else {
                        this
                    }
                }
        }
    }

    final override def toString(level: Int): String = {
        val indent = " " * level
        val lP2 = level + 4
        "N4("+
            s"\n$indent 00=>${if (_0 ne null) _0.toString(lP2) else null}"+
            s"\n$indent 01=>${if (_1 ne null) _1.toString(lP2) else null}"+
            s"\n$indent 10=>${if (_2 ne null) _2.toString(lP2) else null}"+
            s"\n$indent 11=>${if (_3 ne null) _3.toString(lP2) else null})"
    }
}

private[immutable] class LargeLongLinkedTrieSet(
        var size: Int,
        var trie: LongLinkedTrieSetNode,
        var l:    LongLinkedTrieSetL // points to the latest element that was added...
) extends LongLinkedTrieSet { set ⇒

    final override def isEmpty: Boolean = false
    final override def isSingletonSet: Boolean = false

    final override def contains(v: Long): Boolean = {
        var key = v
        var node = trie
        do {
            // Type based switch (proofed to be faster than introducing node ids and using them...):
            node match {
                case n: LongLinkedTrieSetNShared ⇒
                    val sharedBits = n.sharedBits
                    val length = n.length
                    if ((key & LongSet.bitMask(length)) == sharedBits) {
                        node = n.n
                        key = key >> n.length
                    } else {
                        return false;
                    }
                case n: LongLinkedTrieSetN2 ⇒
                    if ((key & 1) == 0) {
                        node = n._0
                    } else {
                        node = n._1
                    }
                    key = key >> 1
                case n: LongLinkedTrieSetN4 ⇒
                    (key & 3 /*binary:11*/ ) match {
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

    final override def head: Long = this.l.value

    final override def foreach[U](f: Long ⇒ U): Unit = {
        var currentL = set.l
        while (currentL ne null) {
            val v = currentL.value
            currentL = currentL.next
            f(v)
        }
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
        val oldTrie = this.trie
        val newL = LongLinkedTrieSetL(v, this.l)
        val newTrie = oldTrie + (newL, 0)
        if (oldTrie ne newTrie) {
            new LargeLongLinkedTrieSet(size + 1, newTrie, newL)
        } else {
            this
        }
    }

    /** Adds the given value that must not be part of this set(!) to this set by mutating it(!). */
    final private[immutable] def +=(v: Long): Unit = {
        val oldTrie = this.trie
        val newL = new LongLinkedTrieSetL(v, this.l)
        val newTrie = oldTrie + (newL, 0)
        if (newTrie ne oldTrie) {
            this.l = newL
            this.trie = newTrie
            this.size += 1
        }
    }

    final override def equals(other: Any): Boolean = {
        other match {
            case that: LargeLongLinkedTrieSet ⇒ this.iterator.sameValues(that.iterator)
            case _                            ⇒ false
        }
    }

    final override def hashCode(): Int = {
        iterator.foldLeft(0)((hashCode, v) ⇒ (hashCode * 31) + java.lang.Long.hashCode(v))
    }

    final override def toString: String = {
        s"LongLinkedTrieSet(#$size,trie=\n${trie.toString(0)}\n)"
    }

}

