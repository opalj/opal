/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.collection
package immutable

import org.opalj.collection.LongIterator
import java.lang.Long.{hashCode => lHashCode}

/**
 * An immutable set of long values which maintains an additional list to enable
 * an access of the values in insertion order (newest first).
 * Additionally, the list is used to provide more efficient iterate and foreach
 * methods when compared to doing both on the underlying trie itself.
 *
 * Compared to the LongLinkedTrieSet this implementation uses less memory and
 * is faster to create; however, the LongLinkedTrieSet offers faster contains
 * and foreach methods than this implementation. Hence, this representation
 * is faster to create and requires significantly less memory. However,
 * accessing the data structure is generally slower. (Internally every node in
 * the trie uses a lookup table to determine the successor node w.r.t. the
 * next bits of a value; this requires an indirection which costs time.)
 */
sealed abstract class LongTrieSetWithList extends LongLinkedSet { intSet =>

    final type ThisSet = LongTrieSetWithList

    override def equals(other: Any): Boolean
    override def hashCode: Int
}

object LongTrieSetWithList {

    def empty: LongTrieSetWithList = LongTrieSetWithList0

    def apply(v: Long): LongTrieSetWithList = new LongTrieSetWithList1(v)

    def apply(head: Long, last: Long): LongTrieSetWithList = {
        if (head == last)
            new LongTrieSetWithList1(head)
        else
            new LongTrieSetWithList2(head, last)
    }

}

private[immutable] case object LongTrieSetWithList0 extends LongTrieSetWithList {
    override def isSingletonSet: Boolean = false
    override def isEmpty: Boolean = true
    override def size: Int = 0

    override def head: Long = throw new UnsupportedOperationException
    override def contains(value: Long): Boolean = false

    override def forall(p: Long => Boolean): Boolean = true
    override def foreach[U](f: Long => U): Unit = {}
    override def iterator: LongIterator = LongIterator.empty
    override def foldLeft[B](z: B)(op: (B, Long) => B): B = z
    override def forFirstN[U](n: Int)(f: Long => U): Unit = { /*nothing to do*/ }

    override def +(i: Long): LongTrieSetWithList1 = new LongTrieSetWithList1(i)

    override def equals(other: Any): Boolean = {
        other match {
            case that: AnyRef => that eq this
            case _            => false
        }
    }
    override def hashCode: Int = 0
    override def toString: String = "LongTrieSetWithList()"
}

private[immutable] final class LongTrieSetWithList1 private[immutable] (
        private[immutable] final val i1: Long
) extends LongTrieSetWithList {

    override def isEmpty: Boolean = false
    override def isSingletonSet: Boolean = true
    override def size: Int = 1

    override def contains(i: Long): Boolean = i == i1
    override def head: Long = i1

    override def forall(p: Long => Boolean): Boolean = p(i1)
    override def foreach[U](f: Long => U): Unit = f(i1)
    override def iterator: LongIterator = LongIterator(i1)
    override def foldLeft[B](z: B)(op: (B, Long) => B): B = op(z, i1)
    override def forFirstN[U](n: Int)(f: Long => U): Unit = {
        if (n == 0) return ;
        f(i1)
    }

    override def +(i: Long): LongTrieSetWithList = {
        val i1 = this.i1
        if (i1 == i)
            this
        else {
            new LongTrieSetWithList2(i, i1)
        }
    }

    override def equals(other: Any): Boolean = {
        other match {
            case that: LongTrieSetWithList1 => (that eq this) || this.i1 == that.i1
            case that                       => false
        }
    }
    override def hashCode: Int = 31 + lHashCode(i1)
    override def toString: String = s"LongTrieSetWithList($i1)"

}

private[immutable] final class LongTrieSetWithList2 private[immutable] (
        private[immutable] final val i1: Long,
        private[immutable] final val i2: Long
) extends LongTrieSetWithList {

    override def isEmpty: Boolean = false
    override def isSingletonSet: Boolean = false
    override def size: Int = 2

    override def contains(i: Long): Boolean = i == i1 || i == i2
    override def head: Long = i1

    override def forall(p: Long => Boolean): Boolean = { p(i1) && p(i2) }
    override def foreach[U](f: Long => U): Unit = { f(i1); f(i2) }
    override def iterator: LongIterator = LongIterator(i1, i2)
    override def foldLeft[B](z: B)(op: (B, Long) => B): B = op(op(z, i1), i2)
    override def forFirstN[U](n: Int)(f: Long => U): Unit = {
        if (n == 0) return ;
        f(i1)
        if (n == 1) return ;
        f(i2)
    }

    override def +(i: Long): LongTrieSetWithList = {
        val i1 = this.i1
        if (i1 == i)
            return this;

        val i2 = this.i2
        if (i2 == i)
            return this;

        new LongTrieSetWithList3(i, i1, i2)
    }

    override def equals(other: Any): Boolean = {
        other match {
            case that: LongTrieSetWithList2 => (that eq this) || this.i1 == that.i1 && this.i2 == that.i2
            case that                       => false
        }
    }
    override def hashCode: Int = 31 * (31 + lHashCode(i1)) + lHashCode(i2)
    override def toString: String = s"LongTrieSetWithList($i1, $i2)"

}

private[immutable] final class LongTrieSetWithList3 private[immutable] (
        private[immutable] final val i1: Long,
        private[immutable] final val i2: Long,
        private[immutable] final val i3: Long
) extends LongTrieSetWithList {

    override def size: Int = 3
    override def isEmpty: Boolean = false
    override def isSingletonSet: Boolean = false

    override def contains(i: Long): Boolean = i == i1 || i == i2 || i == i3
    override def head: Long = i1

    override def forall(p: Long => Boolean): Boolean = { p(i1) && p(i2) && p(i3) }
    override def foreach[U](f: Long => U): Unit = { f(i1); f(i2); f(i3) }
    override def iterator: LongIterator = LongIterator(i1, i2, i3)
    override def foldLeft[B](z: B)(op: (B, Long) => B): B = op(op(op(z, i1), i2), i3)
    override def forFirstN[U](n: Int)(f: Long => U): Unit = {
        if (n == 0) return ;
        f(i1)
        if (n == 1) return ;
        f(i2)
        if (n == 2) return ;
        f(i3)
    }

    override def +(i: Long): LongTrieSetWithList = {
        if (i == i1 || i == i2 || i == i3)
            this
        else
            new LongTrieSetWithListN(4, this.grow(i, 0), Long2List(i, i1, i2, i3))
    }

    private[this] def grow(i: Long, level: Int): LongTrieSetNode = {
        val l = new LongTrieSet1(i)
        var r: LongTrieSetNode = new LongTrieSetNode1(((i >> level) & 7L).toInt, l)
        r = r.add(i1, level)
        r = r.add(i2, level)
        r = r.add(i3, level)
        r
    }

    override def equals(other: Any): Boolean = {
        other match {
            case that: LongTrieSetWithList3 =>
                (this eq that) || (i1 == that.i1 && i2 == that.i2 && i3 == that.i3)
            case _ =>
                false
        }
    }
    override def hashCode: Int = 31 * (31 * (31 + lHashCode(i1)) + lHashCode(i2)) + lHashCode(i3)
    override def toString: String = s"LongTrieSetWithList($i1, $i2, $i3)"

}

private[immutable] final class LongTrieSetWithListN(
        final val size: Int,
        final val root: LongTrieSetNode,
        final val list: Long2List
) extends LongTrieSetWithList {

    // assert(size >= 4)

    override def isEmpty: Boolean = false
    override def isSingletonSet: Boolean = false
    override def contains(value: Long): Boolean = root.contains(value, value)
    override def head: Long = list.head
    override def forall(p: Long => Boolean): Boolean = list.forall(p)
    override def foreach[U](f: Long => U): Unit = list.foreach(f)
    override def forFirstN[U](n: Int)(f: Long => U): Unit = list.forFirstN(n)(f)
    override def iterator: LongIterator = list.iterator
    override def foldLeft[B](z: B)(op: (B, Long) => B): B = list.foldLeft(z)(op)

    override def equals(other: Any): Boolean = {
        other match {
            case that: LongTrieSetWithListN =>
                (that eq this) || (that.size == this.size && this.list == that.list)
            case _ => false
        }
    }
    override def hashCode: Int = list.hashCode * 31 + size

    override def +(i: Long): LongTrieSetWithList = {
        val root = this.root
        val newRoot = root.add(i, 0)
        if (newRoot ne root) {
            new LongTrieSetWithListN(size + 1, newRoot, i +: list)
        } else {
            this
        }
    }

    override def toString: String = s"LongTrieSetWithList(#$size, $list)"

}
