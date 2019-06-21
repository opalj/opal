/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package collection
package immutable

import java.lang.{Long ⇒ JLong}

/**
 * A growable, immutable linked list based data store for '''long values''' except `Long.MinValue`.
 * Though this data store is backed by a linked list, it is not a classical linked list and it
 * does not offer (efficient) head and tail operations. However, compared to a classical linked
 * list the memory usage is reduced by ~33% and a simple foreach is also ~17% faster, the time
 * to create the list is comparable and iteration using an Iterator is marginally slower if at all.
 * The basic idea is to two store 2 values in each node of the linked list to reduce the overhead
 * incurred by the creation of the objects.
 *
 * This list does not perform any length related checks. I.e., it fails,e.g., in
 * case of `forFirstN` if the size of the list is smaller than expected.
 *
 * Furthermore, all directly implemented methods use `while` loops for maximum
 * efficiency and the list is also specialized for primitive `unsigned long` values which
 * makes this list far more efficient when used for storing lists of `long` values.
 *
 * @author Michael Eichberg
 */
sealed trait Long2List extends Serializable { self ⇒

    private[immutable] def h: Long
    private[immutable] def t: Long
    private[immutable] def rest: Long2List

    def isEmpty: Boolean
    def isSingletonList: Boolean
    def nonEmpty: Boolean

    def foreach[U](f: Long ⇒ U): Unit

    /** Iterates over the first N values. */
    def forFirstN[U](n: Int)(f: Long ⇒ U): Unit

    /**
     * @return An iterator over this list's values. In general, you should prefer using foreach or
     *         the other specialized methods to avoid the overhead incurred by the generation of
     *         the iterator.
     */
    def iterator: LongIterator

    /** Prepends the given value to this list. E.g., `l = 2L +: l`. */
    def +:(v: Long): Long2List

    final override def equals(other: Any): Boolean = {
        other match {
            case l: Long2List ⇒ equals(l)
            case _            ⇒ false
        }
    }
    def equals(that: Long2List): Boolean

    override def hashCode(): Int // just added to ensure that we have to override hashCode!
}

object Long2List {

    def empty: Long2List = EmptyLong2List

    def apply(v: Long): Long2List = new Long2ListNode(Long.MinValue, v, EmptyLong2List)

    def apply(head: Long, last: Long): Long2List = new Long2ListNode(head, last, EmptyLong2List)

}

/**
 * An empty [[Long2List]].
 *
 * @author Michael Eichberg
 */
case object EmptyLong2List extends Long2List {

    override def isEmpty: Boolean = true
    override def isSingletonList: Boolean = false
    override def nonEmpty: Boolean = false

    override private[immutable] def h: Long = throw new UnsupportedOperationException
    override private[immutable] def t: Long = throw new UnsupportedOperationException
    override private[immutable] def rest: Long2List = throw new UnsupportedOperationException

    override def foreach[U](f: Long ⇒ U): Unit = {}
    /** Iterates over the first N values. */
    override def forFirstN[U](n: Int)(f: Long ⇒ U): Unit = {}
    override def iterator: LongIterator = LongIterator.empty
    /** Prepends the given value to this list. E.g., `l = 2l +: l`. */
    override def +:(v: Long): Long2List = new Long2ListNode(Long.MinValue, v, this)

    override def equals(that: Long2List): Boolean = this eq that
    override def hashCode(): Int = 37
}

/**
 * An container for a list element.
 *
 * @author Michael Eichberg
 */
final case class Long2ListNode(
        private[immutable] var h:    Long,
        private[immutable] var t:    Long,
        private[immutable] var rest: Long2List = EmptyLong2List
) extends Long2List { list ⇒

    override def isEmpty: Boolean = false
    override def isSingletonList: Boolean = h == Long.MinValue && (rest eq EmptyLong2List)
    override def nonEmpty: Boolean = true

    override def foreach[U](f: Long ⇒ U): Unit = {
        if (h != Long.MinValue) f(h)
        f(t)
        var list: Long2List = this.rest
        while (list.nonEmpty) {
            f(list.h)
            f(list.t)
            list = list.rest
        }
    }

    override def forFirstN[U](n: Int)(f: Long ⇒ U): Unit = {
        n match {
            case 0 ⇒
                return ;
            case 1 ⇒
                if (h != Long.MinValue)
                    f(h)
                else
                    f(t)
                return ;
            case _ ⇒
                // ... n >= 2
                var i = n - 1 // <= for the second element...
                if (h != Long.MinValue) { f(h); i -= 1 }
                f(t)
                var list: Long2List = rest
                while (i > 0) {
                    i -= 2
                    f(list.h)
                    if (i >= 0) f(list.t)
                    list = list.rest
                }
        }
    }

    override def iterator: LongIterator = {
        new LongIterator {
            private[this] var currentList: Long2List = list
            private[this] var head: Boolean = list.h != Long.MinValue
            def hasNext: Boolean = currentList ne EmptyLong2List
            def next: Long = {
                if (head) {
                    head = false
                    currentList.h
                } else {
                    head = true
                    val v = currentList.t
                    currentList = currentList.rest
                    v
                }
            }
        }
    }

    override def +:(v: Long): Long2List = {
        if (h != Long.MinValue)
            new Long2ListNode(Long.MinValue, v, this)
        else
            new Long2ListNode(v, t, this.rest)
    }

    override def equals(that: Long2List): Boolean = {
        (that eq this) || {
            var thisList: Long2List = this
            var thatList = that
            while ((thisList ne EmptyLong2List) && (thatList ne EmptyLong2List)) {
                if (thisList.h != thatList.h || thisList.t != thatList.t)
                    return false;
                thisList = thisList.rest
                thatList = thatList.rest
            }
            thisList eq thatList //... <=> true iff both lists are empty
        }
    }

    override def hashCode(): Int = {
        var h = 31
        var list: Long2List = this
        while (list ne EmptyLong2List) {
            h = ((h + JLong.hashCode(list.h)) * 31 + JLong.hashCode(list.t)) * 31
            list = list.rest
        }
        h
    }
}
