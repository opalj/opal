/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.collection
package immutable

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
sealed abstract class LongLinkedTrieSet
    extends LongCollectionWithStableOrdering[LongLinkedTrieSet] {

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

case class LongLinkedTrieSet1(v1: Long) extends LongLinkedTrieSet {
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

private[immutable] case class LongLinkedTrieSet2(v1: Long, v2: Long) extends LongLinkedTrieSet {
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

private[immutable] case class LongLinkedTrieSet3(v1: Long, v2: Long, v3: Long) extends LongLinkedTrieSet {
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
            val last = new LongLinkedTrieSetL(v3)
            val set = new DefaultLongLinkedTrieSet(1, last, last)
            set +=! v2
            set +=! v1
            set +=! v
            set
        } else
            this
    }
}

/** The super type of the nodes stored in the linked trie set. */
private[immutable] abstract class LongLinkedTrieSetNN {

    private[immutable] def isN: Boolean
    private[immutable] def isL: Boolean

    private[immutable] def +=!(level: Int, l: LongLinkedTrieSetL): LongLinkedTrieSetNN

    private[immutable] def +(level: Int, size: Int, l: LongLinkedTrieSetL): LongLinkedTrieSetNN

    private[immutable] def toString(level: Int): String

}

/** The leaves of the trie set. */
final private[immutable] case class LongLinkedTrieSetL(
        value: Long,
        next:  LongLinkedTrieSetL = null // `null` if this leaf is the first element that was added to the set.
) extends LongLinkedTrieSetNN {

    override private[immutable] def isN: Boolean = false
    override private[immutable] def isL: Boolean = true

    override private[immutable] def +=!(level: Int, l: LongLinkedTrieSetL): LongLinkedTrieSetNN = {
        val thisValue = this.value

        val LorR = (thisValue >> level) & 1
        if (LorR == 0) {
            val trie = new LongLinkedTrieSetN_0(this)
            trie.+=!(level, l)
        } else {
            val trie = new LongLinkedTrieSetN_1(this)
            trie.+=!(level, l)
        }
    }

    private[immutable] def +(level: Int, size: Int, l: LongLinkedTrieSetL): LongLinkedTrieSetNN = {
        if (value == l.value)
            return this;

        val LorR = (this.value >> level) & 1
        if (LorR == 0) {
            val trie = new LongLinkedTrieSetN_0(this)
            trie + (level, size, l)
        } else {
            val trie = new LongLinkedTrieSetN_1(this)
            trie + (level, size, l)
        }
    }

    def toString(level: Int): String = s"L(${value.toBinaryString}=$value)"

}

/** The inner nodes of the trie set. */
private[immutable] abstract class LongLinkedTrieSetN2Like extends LongLinkedTrieSetNN {

    final override private[immutable] def isN: Boolean = true
    final override private[immutable] def isL: Boolean = false

    def _0: LongLinkedTrieSetNN // a tree node, a leaf node or null

    def _1: LongLinkedTrieSetNN // a tree node, a leaf node or null

    private[immutable] def +(level: Int, size: Int, l: LongLinkedTrieSetL): LongLinkedTrieSetNN = {
        if (((l.value >> level) & 1) == 0) {
            if (_0 == null) {
                new LongLinkedTrieSetN2(l, this._1)
            } else {
                val old_0 = this._0
                val new_0 = old_0 + (level + 1, size, l)
                if (old_0 ne new_0) {
                    // We have an update, let's check if we want to move to a node with a higher
                    // branching factor; we do so if – assuming a reasonably balanced trie – we
                    // expect that most references to the successor nodes are used.
                    if (level % 2 == 0 && size > (1 << (level + 2))) {
                        val _0 = new_0.asInstanceOf[LongLinkedTrieSetN2Like]
                        val _00: LongLinkedTrieSetNN = _0._0
                        val _10: LongLinkedTrieSetNN = _0._1
                        var _01: LongLinkedTrieSetNN = null
                        var _11: LongLinkedTrieSetNN = null
                        _1 match {
                            case _1: LongLinkedTrieSetN2Like ⇒
                                _01 = _1._0
                                _11 = _1._1
                            case l: LongLinkedTrieSetL ⇒
                                if (((l.value >> (level + 1)) & 1) == 0) {
                                    _01 = l
                                } else {
                                    _11 = l
                                }
                            case null ⇒ // nothing to do
                        }
                        new LongLinkedTrieSetN4(_00, _01, _10, _11)
                    } else {
                        LongLinkedTrieSetN2Like(new_0, this._1)
                    }
                } else {
                    this
                }
            }
        } else {
            if (_1 == null) {
                new LongLinkedTrieSetN2(this._0, l)
            } else {
                /* without increasing the branching factor:
                val oldRight = this._1
                val newRight = oldRight + (level + 1, size, l)
                if (oldRight ne newRight) {
                    new LongLinkedTrieSetN2(this._0, newRight)
                } else {
                    this
                }
                */
                val old_1 = this._1
                val new_1 = old_1 + (level + 1, size, l)
                if (old_1 ne new_1) {
                    // We have an update, let's check if we want to move to a node with a higher
                    // branching factor; we do so if – assuming a reasonably balanced trie – we
                    // expect that most references to the successor nodes are used.
                    if (level % 2 == 0 && size > (1 << (level + 2))) {
                        val _1 = new_1.asInstanceOf[LongLinkedTrieSetN2Like]
                        val _01: LongLinkedTrieSetNN = _1._0
                        val _11: LongLinkedTrieSetNN = _1._1
                        var _00: LongLinkedTrieSetNN = null
                        var _10: LongLinkedTrieSetNN = null
                        _0 match {
                            case _0: LongLinkedTrieSetN2Like ⇒
                                _00 = _0._0
                                _10 = _0._1
                            case l: LongLinkedTrieSetL ⇒
                                if (((l.value >> (level + 1)) & 1) == 0) {
                                    _00 = l
                                } else {
                                    _10 = l
                                }
                            case null ⇒ // nothing to do
                        }
                        new LongLinkedTrieSetN4(_00, _01, _10, _11)
                    } else {
                        LongLinkedTrieSetN2Like(this._0, new_1)
                    }
                } else {
                    this
                }
            }
        }
    }
}

private[immutable] object LongLinkedTrieSetN2Like {

    def apply(_0: LongLinkedTrieSetNN, _1: LongLinkedTrieSetNN): LongLinkedTrieSetN2Like = {
        if (_0 == null) {
            new LongLinkedTrieSetN_1(_1)
        } else if (_1 == null) {
            new LongLinkedTrieSetN_0(_0)
        } else {
            new LongLinkedTrieSetN2(_0, _1)
        }
    }
}

/** The inner nodes of the trie set. */
final private[immutable] class LongLinkedTrieSetN2(
        var _0: LongLinkedTrieSetNN, // a tree node or a leaf node
        var _1: LongLinkedTrieSetNN // a tree node or a leaf node
) extends LongLinkedTrieSetN2Like {


    override private[immutable] def +=!(level: Int, l: LongLinkedTrieSetL): LongLinkedTrieSetNN = {
        if (((l.value >> level) & 1) == 0) {
            this._0 = _0.+=!(level + 1, l)
        } else {
            this._1 = _1.+=!(level + 1, l)
        }
        this
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
) extends LongLinkedTrieSetN2Like {


    override def _1: LongLinkedTrieSetNN = null

    override private[immutable] def +=!(level: Int, l: LongLinkedTrieSetL): LongLinkedTrieSetNN = {
        if (((l.value >> level) & 1) == 0) {
            this._0 = this._0.+=!(level + 1, l)
            this
        } else {
            new LongLinkedTrieSetN2(_0, l)
        }

    }

    def toString(level: Int): String = s"_0=>${_0.toString(level + 1)}"
}

/** The inner nodes of the trie set. */
final private[immutable] class LongLinkedTrieSetN_1(
        var _1: LongLinkedTrieSetNN // a tree node, a leaf node or null
) extends LongLinkedTrieSetN2Like {


    override def _0: LongLinkedTrieSetNN = null

    override private[immutable] def +=!(level: Int, l: LongLinkedTrieSetL): LongLinkedTrieSetNN = {
        if (((l.value >> level) & 1) == 0) {
            new LongLinkedTrieSetN2(l, _1)
        } else {
            this._1 = this._1.+=!(level + 1, l)
            this
        }
    }

    def toString(level: Int): String = s"_1=>${_1.toString(level + 1)}"
}

/** The inner nodes of the trie set. */
final private[immutable] class LongLinkedTrieSetN4(
        var _00: LongLinkedTrieSetNN, // a tree node, a leaf node or null
        var _01: LongLinkedTrieSetNN, // a tree node, a leaf node or null
        var _10: LongLinkedTrieSetNN, // a tree node, a leaf node or null
        var _11: LongLinkedTrieSetNN // a tree node, a leaf node or null
) extends LongLinkedTrieSetNN {

    override private[immutable] def isN: Boolean = true
    override private[immutable] def isL: Boolean = false

    override private[immutable] def +=!(level: Int, l: LongLinkedTrieSetL): LongLinkedTrieSetNN = {
        throw new UnknownError("only to be used internally when creating an overall set of 4 values")
    }

    private[immutable] def +(level: Int, size: Int, l: LongLinkedTrieSetL): LongLinkedTrieSetNN = {
        // Basic assumption: the trie is nearly balanced...
        ((l.value >> level) & 3) match {
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

    def toString(level: Int): String = {
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
            node match {
                case n: LongLinkedTrieSetN2Like ⇒
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
                case l: LongLinkedTrieSetL ⇒ return v == l.value;
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
    final private[immutable] def +=!(v: Long): Unit = {
        size += 1

        val newL = new LongLinkedTrieSetL(v, this.l)
        this.l = newL

        this.trie = this.trie.+=!(0, newL)
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
