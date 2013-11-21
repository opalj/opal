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
package resolved

import scala.annotation.tailrec

/**
 * Mixed in by source elements that have unique ids.
 *
 * @author Michael Eichberg
 */
trait UniqueID {
    def id: Int
}

object UniqueIDBasedOrdering extends Ordering[UniqueID] {
    def compare(a: UniqueID, b: UniqueID): Int = a.id - b.id
}

sealed trait SortedList[+T <: UniqueID] { thisList ⇒
    def head: T
    def tail: SortedList[T]
    def size: Int
    def isEmpty: Boolean
    def nonEmpty: Boolean
    def foreach[X >: T <: UniqueID, U](f: X ⇒ U): Unit
    def +[X >: T <: UniqueID](e: X): SortedList[X]
    def iterator: Iterator[T] =
        new Iterator[T] {
            private[this] var rest: SortedList[T] = thisList
            def hasNext = rest.nonEmpty
            def next = {
                val old = rest
                rest = old.tail
                old.head
            }
        }
    @tailrec final def forall[X >: T <: UniqueID](f: X ⇒ Boolean): Boolean = {
        if (isEmpty) true
        else if (!f(head)) false
        else tail.forall(f)
    }
    @tailrec final def exists[X >: T <: UniqueID](f: X ⇒ Boolean): Boolean = {
        if (isEmpty) false
        else if (f(head)) true
        else tail.exists(f)
    }
    @tailrec final def contains(e: UniqueID): Boolean = {
        if (isEmpty) false
        else if (head.id == e.id) true
        else tail.contains(e)
    }
    @tailrec final def find[X >: T <: UniqueID](f: X ⇒ Boolean): Option[T] = {
        if (isEmpty) None
        else if (f(head)) Some(head)
        else tail.find(f)
    }
    def map[X >: T](f: T ⇒ X): List[X] = {
        if (isEmpty) Nil
        else f(head) :: tail.map(f)
    }
    final def filter[X >: T <: UniqueID](f: X ⇒ Boolean): SortedList[T] = {
        if (isEmpty) this
        else if (!f(head)) thisList.tail.filter(f)
        else {
            val newTail = thisList.tail.filter(f)
            if (newTail eq thisList.tail)
                this // all elements pass the filter
            else
                new ASortedList[T](head) { val tail = newTail }
        }
    }
    final def filterNot[X >: T <: UniqueID](f: X ⇒ Boolean): SortedList[T] = {
        if (isEmpty) this
        else if (f(head)) thisList.tail.filterNot(f)
        else {
            val newTail = thisList.tail.filterNot(f)
            if (newTail eq thisList.tail)
                this
            else
                new ASortedList[T](head) { def tail = newTail }
        }
    }
    final def toIterable: Iterable[T] = new Iterable[T] { def iterator = thisList.iterator }
}
private abstract class ASortedList[T <: UniqueID](
    val head: T)
        extends SortedList[T] { thisList ⇒

    def size = tail.size + 1
    def isEmpty = false
    def nonEmpty = true
    def foreach[X >: T <: UniqueID, U](f: X ⇒ U): Unit = {
        var rest: SortedList[T] = thisList
        while (rest.nonEmpty) {
            f(rest.head)
            rest = rest.tail
        }
    }
    def +[X >: T <: UniqueID](e: X): SortedList[X] = {
        if (e.id < head.id)
            new ASortedList(e) { def tail = thisList }
        else if (e.id == head.id)
            this
        else {
            val newTail = thisList.tail + e
            if (newTail eq thisList.tail)
                this // if the element is already contained in the rest of the list
            else
                new ASortedList[X](head) { val tail = newTail }
        }
    }
 
    override def equals(other: Any): Boolean = {
        other match {
            case that: SortedList[T] ⇒
                var thisRest: SortedList[T] = this
                var thatRest: SortedList[T] = that
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
        var rest: SortedList[T] = thisList
        while (rest.nonEmpty) {
            hashCode = hashCode * 13 + rest.head.id
            rest = rest.tail
        }
        hashCode
    }
}

object SortedList {

    val empty =
        new SortedList[Nothing] {
            def head = throw new UnsupportedOperationException
            def tail = throw new UnsupportedOperationException
            def isEmpty = true
            def nonEmpty = false
            def size = 0
            override def iterator = Iterator.empty
            def foreach[X >: Nothing <: UniqueID, U](f: X ⇒ U): Unit = { /* Nothing to do */ }
            def +[T <: UniqueID](e: T): SortedList[T] = SortedList(e)
            override def equals(other: Any): Boolean =
                other.isInstanceOf[SortedList[_]] &&
                    other.asInstanceOf[SortedList[_]].isEmpty
            override def hashCode: Int = 101;
        }

    def apply[T <: UniqueID](e: T): SortedList[T] =
        new ASortedList(e) { def tail = empty }

    def apply[T <: UniqueID](e1: T, e2: T): SortedList[T] =
        if (e1.id == e2.id)
            SortedList(e1)
        else
            SortedList(e1) + e2
}