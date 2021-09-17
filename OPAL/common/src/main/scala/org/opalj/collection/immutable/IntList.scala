/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package collection
package immutable

/**
 * An immutable linked list for storing int values.
 * This list does not perform any length related checks. I.e., it fails,e.g., in
 * case of `forFirstN` if the size of the list is smaller than expected.
 * Furthermore, all directly implemented methods use `while` loops for maximum
 * efficiency.
 *
 * @note    In many cases the [[Int2List]] provides better performance than this list if your
 *          problem fits the requirements of [[Int2List]]s.
 *
 * @note    In most cases a `IntList` can be used as a drop-in replacement for a standard
 *          Scala List.
 *
 * @author Michael Eichberg
 */
sealed trait IntList extends Serializable { self =>

    def isEmpty: Boolean
    def nonEmpty: Boolean

    def head: Int
    def tail: IntList

    def foreach[U](f: Int => U): Unit

    /** Iterates over the first N values. */
    def forFirstN[U](n: Int)(f: Int => U): Unit

    def iterator: IntIterator

    /** Prepends the given value to this list. E.g., `l = 2l +: l`. */
    def +:(v: Int): IntList

    final override def equals(other: Any): Boolean = {
        other match {
            case l: IntList => equals(l)
            case _          => false
        }
    }
    def equals(that: IntList): Boolean

    override def hashCode(): Int // just added to ensure that we have to override hashCode!
}

object IntList {

    def empty: IntList = EmptyIntList

    def apply(v: Int): IntList = new IntListNode(v, EmptyIntList)

    def apply(head: Int, last: Int): IntList = {
        new IntListNode(head, new IntListNode(last, EmptyIntList))
    }

}

/**
 * An empty [[IntList]].
 *
 * @author Michael Eichberg
 */
case object EmptyIntList extends IntList {
    override def head: Int = throw new UnsupportedOperationException
    override def tail: IntList = throw new UnsupportedOperationException
    override def isEmpty: Boolean = true
    override def nonEmpty: Boolean = false

    override def foreach[U](f: Int => U): Unit = {}
    /** Iterates over the first N values. */
    override def forFirstN[U](n: Int)(f: Int => U): Unit = {}
    override def iterator: IntIterator = IntIterator.empty
    /** Prepends the given value to this list. E.g., `l = 2 +: l`. */
    override def +:(v: Int): IntList = new IntListNode(v, this)

    override def equals(that: IntList): Boolean = that eq this
    override def hashCode(): Int = 31
}

/**
 * An container for a list element.
 *
 * @author Michael Eichberg
 */
final case class IntListNode(
        head:                        Int,
        private[immutable] var rest: IntList = EmptyIntList
) extends IntList { list =>

    override def tail: IntList = rest

    override def isEmpty: Boolean = false
    override def nonEmpty: Boolean = true

    override def foreach[U](f: Int => U): Unit = {
        var list: IntList = this
        do {
            f(list.head)
            list = list.tail
        } while (list.nonEmpty)
    }

    override def forFirstN[U](n: Int)(f: Int => U): Unit = {
        if (n == 0) return ;

        var i = 0
        var list: IntList = this
        do {
            f(list.head)
            list = list.tail
            i += 1
        } while (list.nonEmpty && i < n)
    }

    override def iterator: IntIterator = {
        new IntIterator {
            private[this] var currentList: IntList = list
            def hasNext: Boolean = currentList.nonEmpty
            def next(): Int = {
                val v = currentList.head
                currentList = currentList.tail
                v
            }
        }
    }

    override def +:(v: Int): IntList = new IntListNode(v, this)

    override def equals(that: IntList): Boolean = {
        (that eq this) || {
            var thisList: IntList = this
            var thatList = that
            while ((thisList ne EmptyIntList) && (thatList ne EmptyIntList)) {
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
        var list: IntList = this
        while (list ne EmptyIntList) {
            h = (h + list.head) * 31
            list = list.tail
        }
        h
    }
}
