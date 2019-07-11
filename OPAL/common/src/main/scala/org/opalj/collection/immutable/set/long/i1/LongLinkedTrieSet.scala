/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.collection
package immutable
package set
package long
package i1

import org.opalj.collection.LongIterator
import java.lang.Long.{hashCode ⇒ lHashCode}

/**
 * An immutable linked trie set which can store all values except Long.MinValue.
 */
sealed abstract class LongLinkedTrieSet extends LongLinkedSet { intSet ⇒

    final type ThisSet = LongLinkedTrieSet

    override def equals(other: Any): Boolean
    override def hashCode: Int
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

private[immutable] case object LongLinkedTrieSet0 extends LongLinkedTrieSet {
    override def isSingletonSet: Boolean = false
    override def isEmpty: Boolean = true
    override def size: Int = 0

    override def contains(value: Long): Boolean = false

    override def forall(p: Long ⇒ Boolean): Boolean = true
    override def foreach[U](f: Long ⇒ U): Unit = {}
    override def iterator: LongIterator = LongIterator.empty
    override def foldLeft[B](z: B)(op: (B, Long) ⇒ B): B = z
    override def forFirstN[U](n: Int)(f: Long ⇒ U): Unit = { /*nothing to do*/ }

    override def +(i: Long): LongLinkedTrieSet1 = new LongLinkedTrieSet1(i)

    override def equals(other: Any): Boolean = {
        other match {
            case that: AnyRef ⇒ that eq this
            case _            ⇒ false
        }
    }
    override def hashCode: Int = 0
    override def toString: String = "LongLinkedTrieSet()"
}

private[immutable] final class LongLinkedTrieSet1(val i1: Long) extends LongLinkedTrieSet {

    override def isEmpty: Boolean = false
    override def isSingletonSet: Boolean = true
    override def size: Int = 1

    override def contains(i: Long): Boolean = i == i1

    override def forall(p: Long ⇒ Boolean): Boolean = p(i1)
    override def foreach[U](f: Long ⇒ U): Unit = f(i1)
    override def iterator: LongIterator = LongIterator(i1)
    override def foldLeft[B](z: B)(op: (B, Long) ⇒ B): B = op(z, i1)
    override def forFirstN[U](n: Int)(f: Long ⇒ U): Unit = {
        if (n == 0) return ;
        f(i1)
    }

    override def +(i: Long): LongLinkedTrieSet = {
        val i1 = this.i1
        if (i1 == i)
            this
        else {
            new LongLinkedTrieSet2(i, i1)
        }
    }

    override def equals(other: Any): Boolean = {
        other match {
            case that: LongLinkedTrieSet1 ⇒ (that eq this) || this.i1 == that.i1
            case that                     ⇒ false
        }
    }
    override def hashCode: Int = 31 + lHashCode(i1)
    override def toString: String = s"LongLinkedTrieSet($i1)"

}

private[immutable] final class LongLinkedTrieSet2(
        val i1: Long, val i2: Long
) extends LongLinkedTrieSet {

    override def isEmpty: Boolean = false
    override def isSingletonSet: Boolean = false
    override def size: Int = 2

    override def contains(i: Long): Boolean = i == i1 || i == i2

    override def forall(p: Long ⇒ Boolean): Boolean = { p(i1) && p(i2) }
    override def foreach[U](f: Long ⇒ U): Unit = { f(i1); f(i2) }
    override def iterator: LongIterator = LongIterator(i1, i2)
    override def foldLeft[B](z: B)(op: (B, Long) ⇒ B): B = op(op(z, i1), i2)
    override def forFirstN[U](n: Int)(f: Long ⇒ U): Unit = {
        if (n == 0) return ;
        f(i1)
        if (n == 1) return ;
        f(i2)
    }

    override def +(i: Long): LongLinkedTrieSet = {
        val i1 = this.i1
        if (i1 == i)
            return this;

        val i2 = this.i2
        if (i2 == i)
            return this;

        new LongLinkedTrieSet3(i, i1, i2)
    }

    override def equals(other: Any): Boolean = {
        other match {
            case that: LongLinkedTrieSet2 ⇒ (that eq this) || this.i1 == that.i1 && this.i2 == that.i2
            case that                     ⇒ false
        }
    }
    override def hashCode: Int = 31 * (31 + lHashCode(i1)) + lHashCode(i2)
    override def toString: String = s"LongLinkedTrieSet($i1, $i2)"

}

private[immutable] final class LongLinkedTrieSet3(
        val i1: Long, val i2: Long, val i3: Long
) extends LongLinkedTrieSet {

    override def size: Int = 3
    override def isEmpty: Boolean = false
    override def isSingletonSet: Boolean = false

    override def contains(i: Long): Boolean = i == i1 || i == i2 || i == i3

    override def forall(p: Long ⇒ Boolean): Boolean = { p(i1) && p(i2) && p(i3) }
    override def foreach[U](f: Long ⇒ U): Unit = { f(i1); f(i2); f(i3) }
    override def iterator: LongIterator = LongIterator(i1, i2, i3)
    override def foldLeft[B](z: B)(op: (B, Long) ⇒ B): B = op(op(op(z, i1), i2), i3)
    override def forFirstN[U](n: Int)(f: Long ⇒ U): Unit = {
        if (n == 0) return ;
        f(i1)
        if (n == 1) return ;
        f(i2)
        if (n == 2) return ;
        f(i3)
    }

    override def +(i: Long): LongLinkedTrieSet = {
        if (i == i1 || i == i2 || i == i3)
            this
        else
            new LongLinkedTrieSetN(4, this.grow(i, 0), Long2List(i, i1, i2, i3))
    }

    private[this] def grow(i: Long, level: Int): LongTrieSetNode = {
        val l = new LongTrieSet1(i)
        var r: LongTrieSetNode = new LongTrieSetNode1(((i >> level) & 7L).toInt, l)
        r = r + (i1, level)
        r = r + (i2, level)
        r = r + (i3, level)
        r
    }

    override def equals(other: Any): Boolean = {
        other match {
            case that: LongLinkedTrieSet3 ⇒
                (this eq that) || (i1 == that.i1 && i2 == that.i2 && i3 == that.i3)
            case _ ⇒
                false
        }
    }
    override def hashCode: Int = 31 * (31 * (31 + lHashCode(i1)) + lHashCode(i2)) + lHashCode(i3)
    override def toString: String = s"LongLinkedTrieSet($i1, $i2, $i3)"

}

private[immutable] final class LongLinkedTrieSetN(
        final val size: Int,
        final val root: LongTrieSetNode,
        final val list: Long2List
) extends LongLinkedTrieSet {

    // assert(size >= 4)

    override def isEmpty: Boolean = false
    override def isSingletonSet: Boolean = false
    override def contains(value: Long): Boolean = root.contains(value, value)
    override def forall(p: Long ⇒ Boolean): Boolean = list.forall(p)
    override def foreach[U](f: Long ⇒ U): Unit = list.foreach(f)
    override def forFirstN[U](n: Int)(f: Long ⇒ U): Unit = list.forFirstN(n)(f)
    override def iterator: LongIterator = list.iterator
    override def foldLeft[B](z: B)(op: (B, Long) ⇒ B): B = list.foldLeft(z)(op)

    override def equals(other: Any): Boolean = {
        other match {
            case that: LongLinkedTrieSetN ⇒
                (that eq this) || (that.size == this.size && this.list == that.list)
            case _ ⇒ false
        }
    }
    override def hashCode: Int = list.hashCode * 31 + size

    override def +(i: Long): LongLinkedTrieSet = {
        val root = this.root
        val newRoot = root + (i, 0)
        if (newRoot ne root) {
            new LongLinkedTrieSetN(size + 1, newRoot, i +: list)
        } else {
            this
        }
    }

    override def toString: String = s"LongLinkedTrieSet(#$size, $list)"

}
