/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package collection
package immutable

/**
 * A growable, immutable linked list based data store for '''int values''' except `Int.MinValue`.
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
 * efficiency and the list is also specialized for primitive `int` values which
 * makes this list far more efficient when used for storing lists of `int` values.
 *
 * @author Michael Eichberg
 */
sealed trait Int2List extends Serializable { self =>

    private[immutable] def h: Int
    private[immutable] def t: Int
    private[immutable] def rest: Int2List

    def isEmpty: Boolean
    def isSingletonList: Boolean
    def nonEmpty: Boolean

    def foreach[U](f: Int => U): Unit

    /** Iterates over the first N values. */
    def forFirstN[U](n: Int)(f: Int => U): Unit

    /**
     * @return An iterator over this list's values. In general, you should prefer using foreach or
     *         the other specialized methods to avoid the overhead incurred by the generation of
     *         the iterator.
     */
    def iterator: IntIterator

    /** Prepends the given value to this list. E.g., `l = 2 +: l`. */
    def +:(v: Int): Int2List

    final override def equals(other: Any): Boolean = {
        other match {
            case l: Int2List => equals(l)
            case _           => false
        }
    }
    def equals(that: Int2List): Boolean

    override def hashCode(): Int // just added to ensure that we have to override hashCode!
}

object Int2List {

    def empty: Int2List = Int2ListEnd

    def apply(v: Int): Int2List = new Int2ListNode(Int.MinValue, v, Int2ListEnd)

    def apply(head: Int, last: Int): Int2List = new Int2ListNode(head, last, Int2ListEnd)

}

/**
 * An empty [[Int2List]].
 *
 * @author Michael Eichberg
 */
case object Int2ListEnd extends Int2List {

    override def isEmpty: Boolean = true
    override def isSingletonList: Boolean = false
    override def nonEmpty: Boolean = false

    override private[immutable] def h: Int = throw new UnsupportedOperationException
    override private[immutable] def t: Int = throw new UnsupportedOperationException
    override private[immutable] def rest: Int2List = throw new UnsupportedOperationException

    override def foreach[U](f: Int => U): Unit = {}
    /** Iterates over the first N values. */
    override def forFirstN[U](n: Int)(f: Int => U): Unit = {}
    override def iterator: IntIterator = IntIterator.empty
    /** Prepends the given value to this list. E.g., `l = 2l +: l`. */
    override def +:(v: Int): Int2List = new Int2ListNode(Int.MinValue, v, this)

    override def equals(that: Int2List): Boolean = this eq that
    override def hashCode(): Int = 37
}

/**
 * An container for a list element.
 *
 * @author Michael Eichberg
 */
final case class Int2ListNode(
        private[immutable] var h:    Int,
        private[immutable] var t:    Int,
        private[immutable] var rest: Int2List = Int2ListEnd
) extends Int2List { list =>

    override def isEmpty: Boolean = false
    override def isSingletonList: Boolean = h == Int.MinValue && (rest eq Int2ListEnd)
    override def nonEmpty: Boolean = true

    override def foreach[U](f: Int => U): Unit = {
        if (h != Int.MinValue) f(h)
        f(t)
        var list: Int2List = this.rest
        while (list.nonEmpty) {
            f(list.h)
            f(list.t)
            list = list.rest
        }
    }

    override def forFirstN[U](n: Int)(f: Int => U): Unit = {
        n match {
            case 0 =>
                return ;
            case 1 =>
                if (h != Int.MinValue)
                    f(h)
                else
                    f(t)
                return ;
            case _ =>
                // ... n >= 2
                var i = n - 1 // <= for the second element...
                if (h != Int.MinValue) { f(h); i -= 1 }
                f(t)
                var list: Int2List = rest
                while (i > 0) {
                    i -= 2
                    f(list.h)
                    if (i >= 0) f(list.t)
                    list = list.rest
                }
        }
    }

    override def iterator: IntIterator = {
        new IntIterator {
            private[this] var currentList: Int2List = list
            private[this] var head: Boolean = list.h != Int.MinValue
            def hasNext: Boolean = currentList ne Int2ListEnd
            def next(): Int = {
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

    override def +:(v: Int): Int2List = {
        if (h != Int.MinValue)
            new Int2ListNode(Int.MinValue, v, this)
        else
            new Int2ListNode(v, t, this.rest)
    }

    override def equals(that: Int2List): Boolean = {
        (that eq this) || {
            var thisList: Int2List = this
            var thatList = that
            while ((thisList ne Int2ListEnd) && (thatList ne Int2ListEnd)) {
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
        var list: Int2List = this
        while (list ne Int2ListEnd) {
            h = ((h + list.h) * 31 + (list.t)) * 31
            list = list.rest
        }
        h
    }
}
