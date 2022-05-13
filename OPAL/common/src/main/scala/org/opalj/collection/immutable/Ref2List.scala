/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package collection
package immutable

/**
 * A growable, immutable linked list based, data store for '''reference values''' except `null`.
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
sealed abstract class Ref2List[+T <: AnyRef] extends Serializable { self =>

    private[immutable] def h: T
    private[immutable] def t: T
    private[immutable] def rest: Ref2List[T]

    def isEmpty: Boolean
    def isSingletonList: Boolean
    def nonEmpty: Boolean

    def foreach[U](f: T => U): Unit

    /** Iterates over the first N values. */
    def forFirstN[U](n: Int)(f: T => U): Unit

    /**
     * @return An iterator over this list's values. In general, you should prefer using foreach or
     *         the other specialized methods to avoid the overhead incurred by the generation of
     *         the iterator.
     */
    def iterator: Iterator[T]

    /** Prepends the given value to this list. E.g., `l = "x" +: l`. */
    def +:[X >: T <: AnyRef](v: X): Ref2List[X]

    final override def equals(other: Any): Boolean = {
        other match {
            case l: Ref2List[AnyRef] => equals(l)
            case _                   => false
        }
    }
    def equals(that: Ref2List[AnyRef]): Boolean

    override def hashCode: Int // just added to ensure that we have to override hashCode!
}

object Ref2List {

    def empty[T <: AnyRef]: Ref2List[T] = Ref2ListEnd

    def apply[T >: Null <: AnyRef](v: T): Ref2List[T] = {
        new Ref2ListNode[T](null, v, Ref2ListEnd)
    }

    def apply[T >: Null <: AnyRef](head: T, last: T): Ref2List[T] = {
        new Ref2ListNode[T](head, last, Ref2ListEnd)
    }

}

/**
 * An empty [[Ref2List]].
 *
 * @author Michael Eichberg
 */
private[immutable] case object Ref2ListEnd extends Ref2List[Nothing] {

    override def isEmpty: Boolean = true
    override def isSingletonList: Boolean = false
    override def nonEmpty: Boolean = false

    override private[immutable] def h: Nothing = throw new UnsupportedOperationException
    override private[immutable] def t: Nothing = throw new UnsupportedOperationException
    override private[immutable] def rest: Ref2List[Nothing] = throw new UnsupportedOperationException

    override def foreach[U](f: Nothing => U): Unit = {}
    /** Iterates over the first N values. */
    override def forFirstN[U](n: Int)(f: Nothing => U): Unit = {}
    override def iterator: Iterator[Nothing] = Iterator.empty
    /** Prepends the given value to this list. E.g., `l = 2l +: l`. */
    override def +:[X <: AnyRef](v: X): Ref2List[X] = {
        new Ref2ListNode[AnyRef](null, v, this).asInstanceOf[Ref2List[X]]
    }

    override def equals(that: Ref2List[AnyRef]): Boolean = this eq that
    override def hashCode: Int = 37
}

/**
 * An container for a list element.
 *
 * @author Michael Eichberg
 */
private[immutable] final case class Ref2ListNode[T >: Null <: AnyRef](
        private[immutable] var h:    T,
        private[immutable] var t:    T,
        private[immutable] var rest: Ref2List[T]
) extends Ref2List[T] { list =>

    override def isEmpty: Boolean = false
    override def isSingletonList: Boolean = h == null && (rest eq Ref2ListEnd)
    override def nonEmpty: Boolean = true

    override def foreach[U](f: T => U): Unit = {
        if (h != null) f(h)
        f(t)
        var list: Ref2List[T] = this.rest
        while (list.nonEmpty) {
            f(list.h)
            f(list.t)
            list = list.rest
        }
    }

    override def forFirstN[U](n: Int)(f: T => U): Unit = {
        n match {
            case 0 =>
                return ;
            case 1 =>
                if (h != null)
                    f(h)
                else
                    f(t)
                return ;
            case _ =>
                // ... n >= 2
                var i = n - 1 // <= -1 for the second element "t"...
                if (h != null) { f(h); i -= 1 }
                f(t)
                var list: Ref2List[T] = rest
                while (i > 0) {
                    i -= 2
                    f(list.h)
                    if (i >= 0) f(list.t)
                    list = list.rest
                }
        }
    }

    override def iterator: Iterator[T] = {
        new Iterator[T] {
            private[this] var currentList: Ref2List[T] = list
            private[this] var head: Boolean = list.h != null
            def hasNext: Boolean = currentList ne Ref2ListEnd
            def next(): T = {
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

    override def +:[X >: T <: AnyRef](v: X): Ref2List[X] = {
        if (h != null)
            new Ref2ListNode(null, v, this)
        else
            new Ref2ListNode(v, t, this.rest)
    }

    override def equals(that: Ref2List[AnyRef]): Boolean = {
        (that eq this) || {
            var thisList: Ref2List[_] = this
            var thatList = that
            while ((thisList ne Ref2ListEnd) && (thatList ne Ref2ListEnd)) {
                if (thisList.h != thatList.h || thisList.t != thatList.t)
                    return false;
                thisList = thisList.rest
                thatList = thatList.rest
            }
            thisList eq thatList //... <=> true iff both lists are empty
        }
    }

    override def hashCode: Int = {
        var h = 31
        var list: Ref2List[_] = this
        while (list ne Ref2ListEnd) {
            if (list.h != null) h += list.h.hashCode * 31
            h += list.t.hashCode * 31
            list = list.rest
        }
        h
    }
}
