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
            val set = new DefaultLongLinkedTrieSet(2, v3N + (v2N, 0), v2N)
            set += v1
            set += v
            set
        } else
            this
    }
}

/** The super type of the nodes of the trie set. */
private[immutable] abstract class LongLinkedTrieSetNN {

    /** `true` if this is an inner node. */
    def isN: Boolean
    /** `true` if this is a leaf node. */
    def isL: Boolean

    def +(l: LongLinkedTrieSetL, level: Int): LongLinkedTrieSetNN

    def toString(level: Int): String

}

/** The leaves of the trie set. */
final private[immutable] case class LongLinkedTrieSetL(
        value: Long,
        next:  LongLinkedTrieSetL = null // `null` if this leaf is the first element that was added to the set.
) extends LongLinkedTrieSetNN {

    override def isN: Boolean = false
    override def isL: Boolean = true

    override def +(l: LongLinkedTrieSetL, level: Int): LongLinkedTrieSetNN = {
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
                        new LongLinkedTrieSetN4(l, null, null, null)
                    }
                } else {
                    if (((thisValueShifted >> 1) & 1) == 0) {
                        new LongLinkedTrieSetN4(null, null, this, l)
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
private[immutable] abstract class LongLinkedTrieSetNLike extends LongLinkedTrieSetNN {
    final override def isN: Boolean = true
    final override def isL: Boolean = false
}

final private[immutable] class LongLinkedTrieSetNShared(
        val sharedBits: Long, // at least 1...
        val length:     Int,
        val n:          LongLinkedTrieSetNN
) extends LongLinkedTrieSetNLike {

    def +(l: LongLinkedTrieSetL, level: Int): LongLinkedTrieSetNN = {
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
            // number of shared bits is smaller than the current length...
            // We have to fold the tail if the number of shared remaining bits is one
            // We have to fold the lead if the number of shared initial bits is one
            val lValueShifted = lValue >> level
            JLong.numberOfTrailingZeros(sharedBits ^ lValueShifted) match {
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
        var _0: LongLinkedTrieSetL,
        var _1: LongLinkedTrieSetL
) extends LongLinkedTrieSetNLike {

    def +(l: LongLinkedTrieSetL, level: Int): LongLinkedTrieSetNN = {
        val lValue = l.value

        if (((lValue >> level) & 1) == 0) {
            val _0Value = this._0.value

            if (_0Value == lValue)
                return this;

            if (((lValue >> level + 1) & 1) == 0) {
                // l = ...00
                if (((_0Value >> level + 1) & 1) == 0) {
                    val newN = _0 + (l, level + 2)
                    if (((_1.value >> level + 1) & 1) == 0) {
                        new LongLinkedTrieSetN4(newN, null, _1, null)
                    } else {
                        new LongLinkedTrieSetN4(newN, null, null, _1)
                    }
                } else {
                    if (((_1.value >> level + 1) & 1) == 0) {
                        new LongLinkedTrieSetN4(l, _0, _1, null)
                    } else {
                        new LongLinkedTrieSetN4(l, _0, null, _1)
                    }
                }
            } else {
                // l = ...01
                if (((_0Value >> level + 1) & 1) == 0) {
                    // l = ...01, _0 = ...0
                    if (((_1.value >> level + 1) & 1) == 0) {
                        new LongLinkedTrieSetN4(_0, l, _1, null)
                    } else {
                        new LongLinkedTrieSetN4(_0, l, null, _1)
                    }
                } else {
                    val newN = _0 + (l, level + 2)
                    if (((_1.value >> level + 1) & 1) == 0) {
                        new LongLinkedTrieSetN4(null, newN, _1, null)
                    } else {
                        new LongLinkedTrieSetN4(null, newN, null, _1)
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
                        new LongLinkedTrieSetN4(_0, null, newN, null)
                    } else {
                        new LongLinkedTrieSetN4(null, _0, newN, null)
                    }
                } else {
                    // l = ...01,_1 = ...11
                    if (((_0.value >> level + 1) & 1) == 0) {
                        new LongLinkedTrieSetN4(_0, null, l, _1)
                    } else {
                        new LongLinkedTrieSetN4(null, _0, l, _1)
                    }
                }
            } else {
                // l = ...11
                if (((_1Value >> level + 1) & 1) == 0) {
                    // l = ...11, _1 = ...01
                    if (((_0.value >> level + 1) & 1) == 0) {
                        new LongLinkedTrieSetN4(_0, null, _1, l)
                    } else {
                        new LongLinkedTrieSetN4(null, _0, _1, l)
                    }
                } else {
                    // l = ...11, _1 = ...11
                    val newN = _1 + (l, level + 2)
                    if (((_0.value >> level + 1) & 1) == 0) {
                        new LongLinkedTrieSetN4(_0, null, null, newN)
                    } else {
                        new LongLinkedTrieSetN4(_0, null, newN, null)
                    }
                }
            }
        }
    }

    def toString(level: Int): String = {
        val indent = " " * level
        val lP2 = level + 2
        s"N2(\n$indent _0=>${_0.toString(lP2)}\n$indent _1=>${_1.toString(lP2)})"
    }
}

/** The inner nodes of the trie set. */
final private[immutable] class LongLinkedTrieSetN4(
                                                      // least significant bits _ (current) second most important bit _ (current) most important bit
                                                      val _0: LongLinkedTrieSetNN, // a tree node, a leaf node or null
                                                      val _1: LongLinkedTrieSetNN, // a tree node, a leaf node or null
                                                      val _2: LongLinkedTrieSetNN, // a tree node, a leaf node or null
                                                      val _3: LongLinkedTrieSetNN // a tree node, a leaf node or null
) extends LongLinkedTrieSetNLike {

    override def +(l: LongLinkedTrieSetL, level: Int): LongLinkedTrieSetNN = {
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
            s"\n$indent _00=>${if (_0 ne null) _0.toString(lP2) else null}"+
            s"\n$indent _01=>${if (_1 ne null) _1.toString(lP2) else null}"+
            s"\n$indent _10=>${if (_2 ne null) _2.toString(lP2) else null}"+
            s"\n$indent _11=>${if (_3 ne null) _3.toString(lP2) else null})"
    }
}

private[immutable] class DefaultLongLinkedTrieSet(
        var size: Int,
        var trie: LongLinkedTrieSetNN,
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
                    (key & 3/*binary:11*/) match {
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

    final override def +(v: Long): DefaultLongLinkedTrieSet = {
        val oldTrie = this.trie
        val newL = LongLinkedTrieSetL(v, this.l)
        val newTrie = oldTrie + (newL, 0)
        if (oldTrie ne newTrie) {
            new DefaultLongLinkedTrieSet(size + 1, newTrie, newL)
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
            case that: DefaultLongLinkedTrieSet ⇒ this.iterator.sameValues(that.iterator)
            case _                              ⇒ false
        }
    }

    final override def hashCode(): Int = {
        iterator.foldLeft(0)((hashCode, v) ⇒ (hashCode * 31) + java.lang.Long.hashCode(v))
    }

    final override def toString: String = {
        s"LongLinkedTrieSet(#$size,trie=\n${trie.toString(0)}\n)"
    }

}

