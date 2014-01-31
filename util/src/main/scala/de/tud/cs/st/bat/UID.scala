/* License (BSD Style License):
 * Copyright (c) 2009 - 2013
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *  - Neither the name of the Software Technology Group or Technische
 *    Universität Darmstadt nor the names of its contributors may be used to
 *    endorse or promote products derived from this software without specific
 *    prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package de.tud.cs.st
package bat

import scala.annotation.tailrec
import scala.collection.SortedSet

/**
 * Mixed in by data structures that have – by construction - unique ids. I.e., two
 * data structures that are not reference equal, have to have two different ids.
 *
 * @author Michael Eichberg
 */
trait UID {
    /**
     * This data structure's unique id.
     */
    def id: Int
}

object UID {
    @inline final def getOrElseUpdate[T <: AnyRef](
        array: Array[T],
        uid: UID,
        orElse: ⇒ T): T = {
        val id = uid.id
        val t = array(id)
        if (t eq null) {
            val result = orElse
            array(id) = result
            result
        } else {
            t
        }
    }
}

private final class UIDIterator[T <: UID](
    private[this] var list: UIDList[T])
        extends Iterator[T] {

    def hasNext = list.nonEmpty

    def next = {
        val l = list
        list = l.tail
        l.head
    }
}

private final class UIDIterable[T <: UID](
    private[this] val list: UIDList[T])
        extends Iterable[T] {

    def iterator = list.iterator
}
/**
 * An immutable, redundancy free, sorted list of elements of type `UID`. This is a highly
 * specialized data structure that is efficient for small lists and should only
 * be used if random access is not generally required.
 *
 * @author Michael Eichberg
 */
sealed trait UIDList[+T <: UID] { thisList ⇒

    /**
     * The head element of this list, if the list is not empty.
     */
    @throws[NoSuchElementException]("if this list is empty")
    def head: T

    /**
     * The tail of this list, if this list is not empty.
     */
    @throws[NoSuchElementException]("if this list is empty")
    def tail: UIDList[T]

    /**
     * Number of elements of this list.
     *
     * This operation take O(n) steps.
     */
    def size: Int

    /**
     * Returns `true` if this list is empty, `false` otherwise.
     */
    def isEmpty: Boolean

    /**
     * Returns `false` if this list is empty, `true` otherwise.
     */
    def nonEmpty: Boolean

    /**
     * If the given element is not already stored in
     * this list, a new list is created and the element is added to it.
     * In the latter case `this` list is returned.
     */
    def +[X >: T <: UID](e: X): UIDList[X]

    /**
     * Iterator to iterate over the elements of this collection. Provided to facilitate
     * the integration with Scala's collections api.
     */
    def iterator: Iterator[T] = new UIDIterator(thisList)

    /**
     * Returns `true` if all elements satisfy the given predicate, `false` otherwise.
     */
    @inline @tailrec final def forall[X >: T <: UID](f: X ⇒ Boolean): Boolean = {
        if (isEmpty) true
        else if (!f(head)) false
        else tail.forall(f)
    }

    /**
     * Returns `true` if an element satisfies the given predicate, `false` otherwise.
     */
    @inline @tailrec final def exists[X >: T <: UID](f: X ⇒ Boolean): Boolean = {
        if (isEmpty) false
        else if (f(head)) true
        else tail.exists(f)
    }

    /**
     * Returns `true` if the given element is already in this list, `false` otherwise.
     */
    @inline @tailrec final def contains(e: UID): Boolean = {
        if (isEmpty) false
        else if (head.id == e.id) true
        else tail.contains(e)
    }

    /**
     * Returns the first element that satisfies the given predicate.
     */
    @inline @tailrec final def find[X >: T <: UID](f: X ⇒ Boolean): Option[T] = {
        if (isEmpty) None
        else if (f(head)) Some(head)
        else tail.find(f)
    }

    /**
     * Creates a new list which contains the mapped values as specified by the given
     * function `f`.
     */
    def map[X >: T](f: T ⇒ X): List[X] = {
        if (isEmpty) Nil
        else f(head) :: tail.map(f)
    }

    /**
     * Returns a new `UIDList` that contains all elements which satisfy the given
     * predicate.
     */
    def filter[X >: T <: UID](f: X ⇒ Boolean): UIDList[T] = {
        val empty = EmptyUIDList
        var newHead: UIDSList[T] = null
        var last: UIDSList[T] = null
        var current = thisList
        var allPass = true
        while (current.nonEmpty) {
            val currentHead = current.head
            if (!f(currentHead)) {
                allPass = false
            } else {
                if (newHead eq null) {
                    newHead = new UIDSList(currentHead, empty)
                    last = newHead
                } else {
                    val newLast = new UIDSList(currentHead, empty)
                    last.tail = newLast
                    last = newLast
                }
            }
            current = current.tail
        }
        if (allPass)
            this
        else if (newHead eq null)
            empty
        else
            newHead
    }

    //    final def filter[X >: T <: UID](f: X ⇒ Boolean): UIDList[T] = {
    //        if (isEmpty)
    //            this
    //        else if (!f(head))
    //            thisList.tail.filter(f)
    //        else {
    //            val newTail = thisList.tail.filter(f)
    //            if (newTail eq thisList.tail)
    //                this // all elements pass the filter
    //            else
    //                new UIDSList[T](head, newTail)
    //        }
    //    }

    /**
     * Returns a new `UIDList` that contains all elements which '''do not satisfy'''
     * the given predicate.
     */
    def filterNot[X >: T <: UID](f: X ⇒ Boolean): UIDList[T] = {
        val empty = EmptyUIDList
        var newHead: UIDSList[T] = null
        var last: UIDSList[T] = null
        var current = thisList
        var allPass = true
        while (current.nonEmpty) {
            val currentHead = current.head
            if (f(currentHead)) {
                allPass = false
            } else {
                if (newHead eq null) {
                    newHead = new UIDSList(currentHead, empty)
                    last = newHead
                } else {
                    val newLast = new UIDSList(currentHead, empty)
                    last.tail = newLast
                    last = newLast
                }
            }
            current = current.tail
        }
        if (allPass)
            this
        else if (newHead eq null)
            empty
        else
            newHead
    }

    //    final def filterNot[X >: T <: UID](f: X ⇒ Boolean): UIDList[T] = {
    //        if (isEmpty)
    //            this
    //        else if (f(head))
    //            thisList.tail.filterNot(f)
    //        else {
    //            val newTail = thisList.tail.filterNot(f)
    //            if (newTail eq thisList.tail)
    //                this
    //            else
    //                new UIDSList[T](head, newTail)
    //        }
    //    }

    /**
     * Passes all elements of this list to the given function.
     */
    @inline final def foreach[U](f: T ⇒ U): Unit = {
        var rest: UIDList[T] = thisList
        while (rest.nonEmpty) {
            f(rest.head)
            rest = rest.tail
        }
    }

    final def toIterable: Iterable[T] = new UIDIterable(thisList)

    def mkString(start: String, sep: String, end: String): String =
        toIterable.mkString(start, sep, end)

    override def toString: String = mkString("UIDSList(", ",", ")")

}

private final class UIDSList[T <: UID](
    val head: T,
    /*The tail is manipulated during construction time only (if at all!)*/
    var tail: UIDList[T])
        extends UIDList[T] { thisList ⇒

    def size = tail.size + 1

    def isEmpty = false

    def nonEmpty = true

    def +[X >: T <: UID](e: X): UIDList[X] = {
        if (e.id < head.id)
            new UIDSList(e, thisList)
        else if (e.id == head.id)
            this
        else { /*e.id > head.id*/
            val newTail = thisList.tail + e
            if (newTail eq thisList.tail)
                this // if the element is already contained in the rest of the list
            else
                new UIDSList(head, newTail)
        }
    }

    override def equals(other: Any): Boolean = {
        other match {
            case that: UIDList[T] ⇒
                var thisRest: UIDList[T] = this
                var thatRest: UIDList[T] = that
                if (thisRest.size != thatRest.size)
                    false
                else {
                    while ((thisRest ne thatRest) && thisRest.nonEmpty && thatRest.nonEmpty) {
                        if (thisRest.head.id != thatRest.head.id)
                            return false;
                        thisRest = thisRest.tail
                        thatRest = thatRest.tail
                    }
                    thisRest eq thatRest
                }
            case _ ⇒
                false
        }
    }

    override def hashCode: Int = {
        var hashCode = 1;
        var rest: UIDList[T] = thisList
        while (rest.nonEmpty) {
            hashCode = hashCode * 13 + rest.head.id
            rest = rest.tail
        }
        hashCode
    }
}

private object EmptyUIDList extends UIDList[Nothing] {

    def head = throw new NoSuchElementException

    def tail = throw new NoSuchElementException

    override def filter[X <: UID](f: X ⇒ Boolean): this.type = this

    override def filterNot[X <: UID](f: X ⇒ Boolean): this.type = this

    def isEmpty = true

    def nonEmpty = false

    def size = 0

    def +[T <: UID](e: T): UIDList[T] = UIDList(e)

    override def iterator = Iterator.empty

    override def equals(other: Any): Boolean =
        other.isInstanceOf[UIDList[_]] && other.asInstanceOf[UIDList[_]].isEmpty

    override def hashCode: Int = -1
}
/**
 * Factory methods to create UIDLists.
 *
 * @author Michael Eichberg
 */
object UIDList {

    /**
     * An empty UIDList
     */
    val empty: UIDList[Nothing] = EmptyUIDList

    /**
     * Creates a new UIDList.
     *
     * @param e The non-null value of the created list.
     */
    def apply[T <: UID](e: T): UIDList[T] =
        new UIDSList(e, empty)

    /**
     * Creates a new UIDList.
     *
     * @param e1 A non-null value of the created list.
     * @param e2 A non-null value of the created list (if it is not a duplicate).
     */
    def apply[T <: UID](e1: T, e2: T): UIDList[T] =
        if (e1.id < e2.id)
            new UIDSList(e1, new UIDSList(e2, empty))
        else if (e1.id == e2.id)
            UIDList(e1)
        else
            new UIDSList(e2, new UIDSList(e1, empty))

    def apply[T <: UID](set: scala.collection.Set[T]): UIDList[T] = {
        if (set.isEmpty)
            UIDList.empty
        else if (set.size == 1)
            apply(set.head)
        else {
            var list: UIDList[T] = UIDList.empty
            set foreach { list += _ }
            list
        }
    }

    def unapplySeq[T <: UID](list: UIDList[T]): Option[Seq[T]] = {
        if (list.isEmpty)
            None
        else
            Some(list.iterator.toSeq)
    }
}

object SingleElementUIDList {

    def unapply[T <: UID](list: UIDList[T]): Option[T] = {
        if (list.tail.isEmpty)
            Some(list.head)
        else
            None
    }
}
