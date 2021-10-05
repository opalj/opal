/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package collection
package immutable

import java.lang.{Long => JLong}

/**
 * An immutable linked list for storing long values.
 * This list does not perform any length related checks. I.e., it fails,e.g., in
 * case of `forFirstN` if the size of the list is smaller than expected.
 * Furthermore, all directly implemented methods use `while` loops for maximum
 * efficiency.
 *
 * @note    In most cases a `LongList` can be used as a drop-in replacement for a standard
 *          Scala List.
 *
 * @author Michael Eichberg
 */
sealed trait LongList extends Serializable { self =>

    def isEmpty: Boolean
    def nonEmpty: Boolean

    def head: Long
    def tail: LongList

    def foreach[U](f: Long => U): Unit

    /** Iterates over the first N values. */
    def forFirstN[U](n: Int)(f: Long => U): Unit

    def iterator: LongIterator

    /** Prepends the given value to this list. E.g., `l = 2l +: l`. */
    def +:(v: Long): LongList

    final override def equals(other: Any): Boolean = {
        other match {
            case l: LongList => equals(l)
            case _           => false
        }
    }
    def equals(that: LongList): Boolean

    override def hashCode(): Int // just added to ensure that we have to override hashCode!
}

object LongList {

    def empty: LongList = LongList0

    def apply(v: Long): LongList = new LongListNode(v, LongList0)

    def apply(head: Long, last: Long): LongList = {
        new LongListNode(head, new LongListNode(last, LongList0))
    }

}

/**
 * An empty [[LongList]].
 *
 * @author Michael Eichberg
 */
case object LongList0 extends LongList {
    override def head: Long = throw new UnsupportedOperationException
    override def tail: LongList = throw new UnsupportedOperationException
    override def isEmpty: Boolean = true
    override def nonEmpty: Boolean = false

    override def foreach[U](f: Long => U): Unit = {}
    /** Iterates over the first N values. */
    override def forFirstN[U](n: Int)(f: Long => U): Unit = {}
    override def iterator: LongIterator = LongIterator.empty
    /** Prepends the given value to this list. E.g., `l = 2l +: l`. */
    override def +:(v: Long): LongList = new LongListNode(v, this)

    override def equals(that: LongList): Boolean = that eq this
    override def hashCode(): Int = 31
}

/**
 * An container for a list element.
 *
 * @author Michael Eichberg
 */
final case class LongListNode(
        head:                        Long,
        private[immutable] var rest: LongList = LongList0
) extends LongList { list =>

    override def tail: LongList = rest

    override def isEmpty: Boolean = false
    override def nonEmpty: Boolean = true

    override def foreach[U](f: Long => U): Unit = {
        var list: LongList = this
        do {
            f(list.head)
            list = list.tail
        } while (list.nonEmpty)
    }

    override def forFirstN[U](n: Int)(f: Long => U): Unit = {
        if (n == 0) return ;

        var i = 0
        var list: LongList = this
        do {
            f(list.head)
            list = list.tail
            i += 1
        } while (list.nonEmpty && i < n)
    }

    override def iterator: LongIterator = {
        new LongIterator {
            private[this] var currentList: LongList = list
            def hasNext: Boolean = currentList.nonEmpty
            def next(): Long = {
                val v = currentList.head
                currentList = currentList.tail
                v
            }
        }
    }

    override def +:(v: Long): LongList = new LongListNode(v, this)

    override def equals(that: LongList): Boolean = {
        (that eq this) || {
            var thisList: LongList = this
            var thatList = that
            while ((thisList ne LongList0) && (thatList ne LongList0)) {
                if (thisList.head != thatList.head)
                    return false;
                thisList = thisList.tail
                thatList = thatList.tail
            }
            thisList eq thatList //... <=> true iff both lists are empty
        }
    }

    override def hashCode(): Int = {
        var h = 31
        var list: LongList = this
        while (list ne LongList0) {
            h = (h + JLong.hashCode(list.head)) * 31
            list = list.tail
        }
        h
    }
}
