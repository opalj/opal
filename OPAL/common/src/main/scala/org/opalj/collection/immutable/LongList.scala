/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package collection
package immutable

/**
 * A linked list which does not perform any length related checks. I.e., it fails in
 * case of `drop` and `take` etc. if the size of the list is smaller than expected.
 * Furthermore, all directly implemented methods use `while` loops for maximum
 * efficiency and the list is also specialized for primitive `int` values which
 * makes this list far more efficient when used for storing lists of `int` values.
 *
 * @note    In most cases a `LongList` can be used as a drop-in replacement for a standard
 *          Scala List.
 *
 * @note    Some core methods, e.g. `drop` and `take`, have different
 *          semantics when compared to the methods with the same name defined
 *          by the Scala collections API. In this case these methods may
 *          fail arbitrarily if the list is not long enough.
 *          Therefore, `Chain` does not inherit from `scala...Seq`.
 *
 * @author Michael Eichberg
 */
sealed trait LongList extends Serializable { self ⇒

    def head: Long
    def tail: LongList
    def isEmpty: Boolean
    def nonEmpty: Boolean

    def foreach[U](f: Long ⇒ U): Unit
    /** Iterates over the first N values. */
    def forFirstN[U](n: Int, f: Long ⇒ U): Unit
    def iterator: LongIterator
    /** Prepends the given value to this list. E.g., `l = 2l +: l`. */
    def +:(v: Long): LongList

}

object LongList {

    def empty: LongList = EmptyLongList

}

/**
 * An empty [[LongList]].
 *
 * @author Michael Eichberg
 */
case object EmptyLongList extends LongList {
    override def head: Long = throw new UnsupportedOperationException
    override def tail: LongList = throw new UnsupportedOperationException
    override def isEmpty: Boolean = true
    override def nonEmpty: Boolean = false

    override def foreach[U](f: Long ⇒ U): Unit = {}
    /** Iterates over the first N values. */
    override def forFirstN[U](n: Int, f: Long ⇒ U): Unit = {}
    override def iterator: LongIterator = LongIterator.empty
    /** Prepends the given value to this list. E.g., `l = 2l +: l`. */
    override def +:(v: Long): LongList = new LongListNode(v, this)
}

/**
 * An container for a list element.
 *
 * @author Michael Eichberg
 */
final case class LongListNode(
        head:                        Long,
        private[immutable] var rest: LongList = EmptyLongList
) extends LongList { list ⇒

    override def tail: LongList = rest

    override def isEmpty: Boolean = false
    override def nonEmpty: Boolean = true

    override def foreach[U](f: Long ⇒ U): Unit = {
        var list: LongList = this
        do {
            f(list.head)
            list = list.tail
        } while (list.nonEmpty)
    }

    override def forFirstN[U](n: Int, f: Long ⇒ U): Unit = {
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
            def next: Long = {
                val v = currentList.head
                currentList = currentList.tail
                v
            }
        }
    }

    override def +:(v: Long): LongList = new LongListNode(v, this)
}
