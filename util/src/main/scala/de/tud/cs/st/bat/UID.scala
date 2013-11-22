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

object UIDBasedOrdering extends Ordering[UID] {
    def compare(a: UID, b: UID): Int = a.id - b.id
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

sealed trait UIDList[+T <: UID] { thisList ⇒

    def head: T

    def tail: UIDList[T]

    def size: Int

    def isEmpty: Boolean

    def nonEmpty: Boolean

    def +[X >: T <: UID](e: X): UIDList[X]

    def iterator: Iterator[T] = new UIDIterator(thisList)

    @inline @tailrec final def forall[X >: T <: UID](f: X ⇒ Boolean): Boolean = {
        if (isEmpty) true
        else if (!f(head)) false
        else tail.forall(f)
    }

    @inline @tailrec final def exists[X >: T <: UID](f: X ⇒ Boolean): Boolean = {
        if (isEmpty) false
        else if (f(head)) true
        else tail.exists(f)
    }

    @inline @tailrec final def contains(e: UID): Boolean = {
        if (isEmpty) false
        else if (head.id == e.id) true
        else tail.contains(e)
    }

    @inline @tailrec final def find[X >: T <: UID](f: X ⇒ Boolean): Option[T] = {
        if (isEmpty) None
        else if (f(head)) Some(head)
        else tail.find(f)
    }

    def map[X >: T](f: T ⇒ X): List[X] = {
        if (isEmpty) Nil
        else f(head) :: tail.map(f)
    }

    final def filter[X >: T <: UID](f: X ⇒ Boolean): UIDList[T] = {
        val empty = EmptyUIDList
        var newHead: UIDSList[T] = null
        var last: UIDSList[T] = null
        var current = thisList
        var allPass = true
        while (current.nonEmpty) {
            val currentHead = current.head
            if (!f(currentHead)) {
                allPass = false
            }
            else {
                if (newHead eq null) {
                    newHead = new UIDSList(currentHead, empty)
                    last = newHead
                }
                else {
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

    final def filterNot[X >: T <: UID](f: X ⇒ Boolean): UIDList[T] = {
        val empty = EmptyUIDList
        var newHead: UIDSList[T] = null
        var last: UIDSList[T] = null
        var current = thisList
        var allPass = true
        while (current.nonEmpty) {
            val currentHead = current.head
            if (f(currentHead)) {
                allPass = false
            }
            else {
                if (newHead eq null) {
                    newHead = new UIDSList(currentHead, empty)
                    last = newHead
                }
                else {
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

    @inline final def foreach[X >: T <: UID, U](f: X ⇒ U): Unit = {
        var rest: UIDList[T] = thisList
        while (rest.nonEmpty) {
            f(rest.head)
            rest = rest.tail
        }
    }

    final def toIterable: Iterable[T] = new UIDIterable(thisList)
}

private final class UIDSList[T <: UID](
    val head: T,
    var tail: UIDList[T] /*It is manipulated during construction time only (if at all!)*/ )
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
                new UIDSList[X](head, newTail)
        }
    }

    override def equals(other: Any): Boolean = {
        other match {
            case that: UIDList[T] ⇒
                var thisRest: UIDList[T] = this
                var thatRest: UIDList[T] = that
                while (thisRest.nonEmpty && thatRest.nonEmpty) {
                    if (thisRest.head.id != thatRest.head.id)
                        return false;
                    thisRest = thisRest.tail
                    thatRest = thatRest.tail
                }
                thisRest.isEmpty && thatRest.isEmpty
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

object EmptyUIDList extends UIDList[Nothing] {

    def head = throw new UnsupportedOperationException

    def tail = throw new UnsupportedOperationException

    def isEmpty = true

    def nonEmpty = false

    def size = 0

    def +[T <: UID](e: T): UIDList[T] = UIDList(e)

    override def iterator = Iterator.empty

    override def equals(other: Any): Boolean =
        other.isInstanceOf[UIDList[_]] && other.asInstanceOf[UIDList[_]].isEmpty

    override def hashCode: Int = -1
}

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
}