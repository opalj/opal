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
            val set = new DefaultLongLinkedTrieSet(1, v3N, v3N)
            set _UNSAFE_addNew v2
            set _UNSAFE_addNew v1
            set _UNSAFE_addNew v
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

    def +(level: Int, size: Int, l: LongLinkedTrieSetL): LongLinkedTrieSetNN

    def toString(level: Int): String

    def _UNSAFE_addNew(level: Int, l: LongLinkedTrieSetL): LongLinkedTrieSetNN

}

/** The leaves of the trie set. */
final private[immutable] case class LongLinkedTrieSetL(
        value: Long,
        next:  LongLinkedTrieSetL = null // `null` if this leaf is the first element that was added to the set.
) extends LongLinkedTrieSetNN {

    override def isN: Boolean = false
    override def isL: Boolean = true

    override def _UNSAFE_addNew(level: Int, l: LongLinkedTrieSetL): LongLinkedTrieSetNN = {
        val thisValue = this.value
        if (((thisValue >> level) & 1) == 0) {
            val trie = new LongLinkedTrieSetN_0(this)
            trie._UNSAFE_addNew(level, l)
        } else {
            val trie = new LongLinkedTrieSetN_1(this)
            trie._UNSAFE_addNew(level, l)
        }
    }

    override def +(level: Int, size: Int, l: LongLinkedTrieSetL): LongLinkedTrieSetNN = {
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
                    if ((thisValueShifted >> 1 & 1) == 0) {
                        new LongLinkedTrieSetN_0(new LongLinkedTrieSetN2(this, l))
                    } else {
                        new LongLinkedTrieSetN_0(new LongLinkedTrieSetN2(l, this))
                    }
                } else {
                    if ((thisValueShifted >> 1 & 1) == 0) {
                        new LongLinkedTrieSetN_1(new LongLinkedTrieSetN2(this, l))
                    } else {
                        new LongLinkedTrieSetN_1(new LongLinkedTrieSetN2(l, this))
                    }
                }
            case length ⇒
                 val bitPattern = thisValueShifted & ((1L << (length+1))-1)
                 val n = 
                 if (((thisValueShifted >> length) & 1) == 0) 
                    new LongLinkedTrieSetN2(this,l)
                    else
                    new LongLinkedTrieSetN2(l,this)
                new LongLinkedTrieSetNShared(bitPattern,length,n                )

                /*
                if ((thisValueShifted & 1) == 0) {
                    val trie = new LongLinkedTrieSetN_0(this)
                    trie _UNSAFE_addNew (level, l)
                } else {
                    val trie = new LongLinkedTrieSetN_1(this)
                    trie _UNSAFE_addNew (level, l)
                }
                */
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
        var bitPattern: Long,
         var length : Int,
        var n:    LongLinkedTrieSetNN
) extends LongLinkedTrieSetNLike {

    override def _UNSAFE_addNew(level: Int, l: LongLinkedTrieSetL): LongLinkedTrieSetNN = {
        if (((l.value >> level) & 1) == 0) {
            this._0 = _0._UNSAFE_addNew(level + 1, l)
        } else {
            this._1 = _1._UNSAFE_addNew(level + 1, l)
        }
        this
    }

    def +(level: Int, size: Int, l: LongLinkedTrieSetL): LongLinkedTrieSetNN = {
        val lValue = l.value
        if ((lValue & sharedBits) == sharedBits)
            val old_n = this._n
            val new_n = old_n + (level + length, size, l)
            if (old_n ne new_n) {
                new LongLinkedTrieSetN2(new_0, this._1)
            } else {
                this
            }
        } else {
            ???
        }
    }

    def toString(level: Int): String = {
        val lP2 = level + length+2
        s"NShared(_${bitPattern.toBinaryString}=>${n.toString(lP2)}"
    }
}


/** The inner nodes of the trie set. */
final private[immutable] class LongLinkedTrieSetN2(
        var _0: LongLinkedTrieSetNN, // a tree node or a leaf node
        var _1: LongLinkedTrieSetNN // a tree node or a leaf node
) extends LongLinkedTrieSetNLike {

    override def _UNSAFE_addNew(level: Int, l: LongLinkedTrieSetL): LongLinkedTrieSetNN = {
        if (((l.value >> level) & 1) == 0) {
            this._0 = _0._UNSAFE_addNew(level + 1, l)
        } else {
            this._1 = _1._UNSAFE_addNew(level + 1, l)
        }
        this
    }

    def +(level: Int, size: Int, l: LongLinkedTrieSetL): LongLinkedTrieSetNN = {
        if (((l.value >> level) & 1) == 0) {
            val old_0 = this._0
            val new_0 = old_0 + (level + 1, size, l)
            if (old_0 ne new_0) {
                new LongLinkedTrieSetN2(new_0, this._1)
            } else {
                this
            }
        } else {
            val old_1 = this._1
            val new_1 = old_1 + (level + 1, size, l)
            if (old_1 ne new_1) {
                new LongLinkedTrieSetN2(this._0, new_1)
            } else {
                this
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
final private[immutable] class LongLinkedTrieSetN_0(
        var _0: LongLinkedTrieSetNN // a tree node, a leaf node or null
) extends LongLinkedTrieSetNLike {

    //   override def _1: LongLinkedTrieSetNN = null

    def +(level: Int, size: Int, l: LongLinkedTrieSetL): LongLinkedTrieSetNN = {
        if (((l.value >> level) & 1) == 0) {
            val old_0 = this._0
            val new_0 = old_0 + (level + 1, size, l)
            if (old_0 ne new_0) {
                new LongLinkedTrieSetN_0(new_0)
            } else {
                this
            }
        } else {
            new LongLinkedTrieSetN2(this._0, l)
        }
    }

    def toString(level: Int): String = s"_0=>${_0.toString(level + 1)}"

    override def _UNSAFE_addNew(level: Int, l: LongLinkedTrieSetL): LongLinkedTrieSetNN = {
        if (((l.value >> level) & 1) == 0) {
            this._0 = this._0._UNSAFE_addNew(level + 1, l)
            this
        } else {
            new LongLinkedTrieSetN2(_0, l)
        }
    }

}

/** The inner nodes of the trie set. */
final class LongLinkedTrieSetN_1(
        var _1: LongLinkedTrieSetNN // a tree node, a leaf node or null
) extends LongLinkedTrieSetNLike {

    //    override def _0: LongLinkedTrieSetNN = null

    def +(level: Int, size: Int, l: LongLinkedTrieSetL): LongLinkedTrieSetNN = {
        if (((l.value >> level) & 1) == 0) {
            new LongLinkedTrieSetN2(l, this._1)
        } else {
            val old_1 = _1
            val new_1 = old_1 + (level + 1, size, l)
            if (old_1 ne new_1) {
                new LongLinkedTrieSetN_1(new_1)
            } else {
                this
            }
        }
    }

    def toString(level: Int): String = s"_1=>${_1.toString(level + 1)}"

    override def _UNSAFE_addNew(level: Int, l: LongLinkedTrieSetL): LongLinkedTrieSetNN = {
        if (((l.value >> level) & 1) == 0) {
            new LongLinkedTrieSetN2(l, _1)
        } else {
            this._1 = this._1._UNSAFE_addNew(level + 1, l)
            this
        }
    }
}

/** The inner nodes of the trie set. */
final class LongLinkedTrieSetN4(
        var _00: LongLinkedTrieSetNN, // a tree node, a leaf node or null
        var _01: LongLinkedTrieSetNN, // a tree node, a leaf node or null
        var _10: LongLinkedTrieSetNN, // a tree node, a leaf node or null
        var _11: LongLinkedTrieSetNN // a tree node, a leaf node or null
) extends LongLinkedTrieSetNN {

    override def isN: Boolean = true
    override def isL: Boolean = false

    override def _UNSAFE_addNew(level: Int, l: LongLinkedTrieSetL): LongLinkedTrieSetNN = {
        throw new UnknownError("only to be used internally when creating an overall set of 4 values")
    }

    override def +(level: Int, size: Int, l: LongLinkedTrieSetL): LongLinkedTrieSetNN = {
        // Basic assumption: the trie is nearly balanced...
        val pattern = (l.value >> level) & 3
        (pattern.toInt: @switch) match {
            case 0 ⇒
                if (_00 == null) {
                    new LongLinkedTrieSetN4(l, this._01, this._10, this._11)
                } else {
                    val old00 = this._00
                    val new00 = old00 + (level + 2, size, l)
                    if (old00 ne new00) {
                        new LongLinkedTrieSetN4(new00, this._01, this._10, this._11)
                    } else {
                        this
                    }
                }
            case 1 ⇒
                if (_01 == null) {
                    new LongLinkedTrieSetN4(this._00, l, this._10, this._11)
                } else {
                    val old01 = this._01
                    val new01 = old01 + (level + 2, size, l)
                    if (old01 ne new01) {
                        new LongLinkedTrieSetN4(this._00, new01, this._10, this._11)
                    } else {
                        this
                    }
                }

            case 2 ⇒
                if (_10 == null) {
                    new LongLinkedTrieSetN4(this._00, this._01, l, this._11)
                } else {
                    val old10 = this._10
                    val new10 = old10 + (level + 2, size, l)
                    if (old10 ne new10) {
                        new LongLinkedTrieSetN4(this._00, this._01, new10, this._11)
                    } else {
                        this
                    }
                }

            case 3 ⇒
                if (_11 == null) {
                    new LongLinkedTrieSetN4(this._00, this._01, this._10, l)
                } else {
                    val old11 = this._11
                    val new11 = old11 + (level + 2, size, l)
                    if (old11 ne new11) {
                        new LongLinkedTrieSetN4(this._00, this._01, this._10, new11)
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
            s"\n$indent _00=>${if (_00 ne null) _00.toString(lP2) else null}"+
            s"\n$indent _01=>${if (_01 ne null) _01.toString(lP2) else null}"+
            s"\n$indent _10=>${if (_10 ne null) _10.toString(lP2) else null}"+
            s"\n$indent _11=>${if (_11 ne null) _11.toString(lP2) else null})"
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
                case n: LongLinkedTrieSetN_0 ⇒
                    if ((key & 1) == 0) {
                        node = n._0
                        key = key >> 1
                    } else {
                        return false;
                    }
                case n: LongLinkedTrieSetN_1 ⇒
                    if ((key & 1) == 1) {
                        node = n._1
                        key = key >> 1
                    } else {
                        return false;
                    }
                case n: LongLinkedTrieSetNShared ⇒
                val bitPattern = n.bitPattern
                    if((key & bitPattern) == bitPattern) {
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
                    (key & 3) match {
                        case 0 ⇒ node = n._00
                        case 1 ⇒ node = n._01
                        case 2 ⇒ node = n._10
                        case 3 ⇒ node = n._11
                    }
                    key = key >> 2
                case l: LongLinkedTrieSetL ⇒
                    return v == l.value;
            }
        } while (node ne null)
        false
    }

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

    final override def head: Long = this.l.value

    //final override def head : Long = head.value
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
        val newTrie = oldTrie + (0, size, newL)
        if (oldTrie ne newTrie) {
            new DefaultLongLinkedTrieSet(size + 1, newTrie, newL)
        } else {
            this
        }
    }

    /** Adds the given value that must not be part of this set(!) to this set by mutating it(!). */
    final private[immutable] def _UNSAFE_addNew(v: Long): Unit = {
        size += 1

        val newL = new LongLinkedTrieSetL(v, this.l)
        this.l = newL

        this.trie = this.trie._UNSAFE_addNew(0, newL)
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
        s"LongLinkedTrieSet(#=$size,trie=\n${trie.toString(0)}\n)"
    }

}

