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
    def forFirstN[U](n: Int, f: Long ⇒ U): Unit
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
    final override def forFirstN[U](n: Int, f: Long ⇒ U): Unit = { /*nothing to do*/ }
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
    final override def forFirstN[U](n: Int, f: Long ⇒ U): Unit = if (n > 0) f(v1)
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
    final override def forFirstN[U](n: Int, f: Long ⇒ U): Unit = {
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
    final override def forFirstN[U](n: Int, f: Long ⇒ U): Unit = {
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

    private[immutable] def +(level: Int, l: LongLinkedTrieSetL): LongLinkedTrieSetNN

}

/** The leaves of the trie set. */
final private[immutable] case class LongLinkedTrieSetL(
        value: Long,
        next:  LongLinkedTrieSetL = null // `null` if this leaf is the first element that was added to the set.
) extends LongLinkedTrieSetNN {

    override private[immutable] def isN: Boolean = false
    override private[immutable] def isL: Boolean = true

    override private[immutable] def +=!(level: Int, l: LongLinkedTrieSetL): LongLinkedTrieSetNN = {
        val LorR = (this.value >> level) & 1
        if (LorR == 0) {
            val trie = new LongLinkedTrieSetN(this, null)
            trie.+=!(level, l)
        } else {
            val trie = new LongLinkedTrieSetN(null, this)
            trie.+=!(level, l)
        }
    }

    private[immutable] def +(level: Int, l: LongLinkedTrieSetL): LongLinkedTrieSetNN = {
        if (value == l.value)
            return this;

        val LorR = (this.value >> level) & 1
        if (LorR == 0) {
            val trie = new LongLinkedTrieSetN(this, null)
            trie + (level, l)
        } else {
            val trie = new LongLinkedTrieSetN(null, this)
            trie + (level, l)
        }
    }

}

/** The inner nodes of the trie set. */
final private[immutable] class LongLinkedTrieSetN(
        var left:  LongLinkedTrieSetNN, // a tree node, a leaf node or null
        var right: LongLinkedTrieSetNN // a tree node, a leaf node or null
) extends LongLinkedTrieSetNN {

    override private[immutable] def isN: Boolean = true
    override private[immutable] def isL: Boolean = false

    override private[immutable] def +=!(level: Int, l: LongLinkedTrieSetL): LongLinkedTrieSetNN = {
        if (((l.value >> level) & 1) == 0) {
            if (left == null) {
                left = l
            } else if (left.isL) {
                this.left = this.left.+=!(level + 1, l)
            } else {
                left.+=!(level + 1, l)
            }
        } else {
            if (right == null) {
                right = l
            } else if (right.isL) {
                this.right = this.right.+=!(level + 1, l)
            } else {
                right.+=!(level + 1, l)
            }
        }
        this
    }

    private[immutable] def +(level: Int, l: LongLinkedTrieSetL): LongLinkedTrieSetNN = {
        if (((l.value >> level) & 1) == 0) {
            if (left == null) {
                new LongLinkedTrieSetN(l, this.right)
            } else {
                val oldLeft = this.left
                val newLeft = oldLeft + (level + 1, l)
                if (oldLeft ne newLeft) {
                    new LongLinkedTrieSetN(newLeft, this.right)
                } else {
                    this
                }
            }
        } else {
            if (right == null) {
                new LongLinkedTrieSetN(this.left, l)
            } else {
                val oldRight = this.right
                val newRight = oldRight + (level + 1, l)
                if (oldRight ne newRight) {
                    new LongLinkedTrieSetN(this.left, newRight)
                } else {
                    this
                }
            }
        }
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
                case l: LongLinkedTrieSetL ⇒ return v == l.value;
                case n: LongLinkedTrieSetN ⇒
                    if ((key & 1) == 0) {
                        node = n.left
                    } else {
                        node = n.right
                    }
                    key = key >> 1
            }
        } while (node ne null)
        false;
    }

    final override def foreach[U](f: Long ⇒ U): Unit = {
        var currentL = set.l
        while (currentL ne null) {
            val v = currentL.value
            currentL = currentL.next
            f(v)
        }
    }

    final override def forFirstN[U](n: Int, f: Long ⇒ U): Unit = {
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
        val newTrie = oldTrie + (0, newL)
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

}
