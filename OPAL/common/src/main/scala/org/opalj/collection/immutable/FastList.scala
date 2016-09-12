/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2016
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
package org.opalj
package collection
package immutable

import scala.collection.GenIterable

/**
 * A linked list which does not perform any length related checks. I.e., it fails if the size
 * of the list is smaller than expected.
 *
 * @note 	Some core methods, e.g. `drop` and `take`, have different
 * 			semantics when compared to the methods with the same name defined
 * 			by the Scala collections API. In this case these methods may
 * 			fail arbitrarily if the list is not long enough.
 * 			Therefore, `FastList` does not inherit from `scala...Seq`.
 *
 * @author Michael Eichberg
 */
// TODO Add "with FilterMonadic[T,FastList[T]]
sealed trait FastList[@specialized(Int) +T] { self ⇒
    def head: T
    def tail: FastList[T]
    def last: T = {
        var rest = this
        while (rest.tail.nonEmpty) { rest = rest.tail }
        rest.head
    }
    def isEmpty: Boolean
    def nonEmpty: Boolean
    def apply(index: Int): T = {
        var count = index
        var rest = this
        while (count > 0) {
            rest = rest.tail
            count -= 1
        }
        rest.head
    }
    def exists(f: T ⇒ Boolean): Boolean = {
        var rest = this
        while (rest.nonEmpty) {
            if (f(rest.head))
                return true;
            rest = rest.tail
        }
        false
    }
    def forall(f: T ⇒ Boolean): Boolean = {
        var rest = this
        while (rest.nonEmpty) {
            if (!f(rest.head))
                return false;
            rest = rest.tail
        }
        true
    }
    def contains[X >: T](e: X): Boolean = {
        var rest = this
        while (rest.nonEmpty) {
            if (rest.head == e)
                return true;
            rest = rest.tail
        }
        false
    }
    def size: Int = {
        var result = 0
        var rest = this
        while (rest.nonEmpty) {
            result += 1
            rest = rest.tail
        }
        result
    }
    def :!:[X >: T](x: X): FastList[X] = new :!:(x, this)
    def foreach[U](f: T ⇒ U): Unit = {
        var rest = this
        while (rest.nonEmpty) {
            f(rest.head)
            rest = rest.tail
        }
    }
    def take(n: Int): FastList[T]
    def takeWhile(f: T ⇒ Boolean): FastList[T]
    def filter(f: T ⇒ Boolean): FastList[T]
    def drop(n: Int): FastList[T]
    def map[X](f: T ⇒ X): FastList[X]
    def zip[X](other: GenIterable[X]): FastList[(T, X)] = {
        if (this.isEmpty)
            return this.asInstanceOf[FastNil.type];
        val otherIt = other.iterator
        if (!otherIt.hasNext)
            return FastNil;

        var thisIt = this.tail
        val result: :!:[(T, X)] = new :!:((this.head, otherIt.next), FastNil)
        var last = result
        while (thisIt.nonEmpty && otherIt.hasNext) {
            val newLast = new :!:((thisIt.head, otherIt.next), FastNil)
            last.rest = newLast
            last = newLast
            thisIt = thisIt.tail
        }
        result
    }
    def zip[X](other: FastList[X]): FastList[(T, X)] = {
        if (this.isEmpty)
            return this.asInstanceOf[FastNil.type];
        if (other.isEmpty)
            return other.asInstanceOf[FastNil.type];

        var thisIt = this.tail
        var otherIt = other.tail
        val result: :!:[(T, X)] = new :!:((this.head, other.head), FastNil)
        var last = result
        while (thisIt.nonEmpty && otherIt.nonEmpty) {
            val newLast = new :!:((thisIt.head, otherIt.head), FastNil)
            last.rest = newLast
            last = newLast
            thisIt = thisIt.tail
            otherIt = otherIt.tail
        }
        result
    }
    def zipWithIndex: FastList[(T, Int)] = {
        var index = 0
        map { e ⇒
            val currentIndex = index
            index += 1
            (e, currentIndex)
        }
    }
    def corresponds[X](other: FastList[X])(f: (T, X) ⇒ Boolean): Boolean = {
        if (this.isEmpty)
            return other.isEmpty;
        if (other.isEmpty)
            return false;
        // both lists have at least one element...
        if (!f(this.head, other.head))
            return false;

        var thisIt = this.tail
        var otherIt = other.tail
        while (thisIt.nonEmpty && otherIt.nonEmpty) {
            if (!f(thisIt.head, otherIt.head))
                return false;
            thisIt = thisIt.tail
            otherIt = otherIt.tail
        }
        thisIt.isEmpty && otherIt.isEmpty
    }
    def mapConserve[X >: T <: AnyRef](f: T ⇒ X): FastList[X]
    def reverse: FastList[T] = {
        var result: FastList[T] = FastNil
        var rest = this
        while (rest.nonEmpty) {
            result :!:= rest.head
            rest = rest.tail
        }
        result
    }
    def mkString(pre: String, sep: String, post: String): String = {
        val result = new StringBuilder(pre)
        var rest = this
        if (rest.nonEmpty) {
            result.append(head.toString)
            rest = rest.tail
            while (rest.nonEmpty) {
                result.append(sep)
                result.append(rest.head.toString)
                rest = rest.tail
            }
        }

        result.append(post)
        result.toString
    }

    def toIterable(): Iterable[T] = {
        new scala.collection.Iterable[T] {
            def iterator: Iterator[T] = self.toIterator
        }
    }

    def toIterator(): Iterator[T] = {
        new scala.collection.Iterator[T] {
            var rest = self
            def hasNext: Boolean = rest.nonEmpty
            def next(): T = {
                val result = rest.head
                rest = rest.tail
                result
            }
        }
    }

    def toTraversable(): Traversable[T] = {
        new scala.collection.Traversable[T] {
            def foreach[U](f: T ⇒ U): Unit = self.foreach(f)
        }
    }
}
object FastList {

    def empty[T]: FastList[T] = FastNil

    def apply[T](e: T) = new :!:(e, FastNil)

    def apply[T](t: Traversable[T]): FastList[T] = {
        if (t.isEmpty)
            return FastNil;
        val result = new :!:(t.head, FastNil)
        var last = result
        t.tail.foreach { e ⇒
            val newLast = new :!:(e, FastNil)
            last.rest = newLast
            last = newLast
        }
        result
    }

}
case object FastNil extends FastList[Nothing] {
    def head: Nothing = throw new IllegalStateException("the list is empty")
    def tail: Nothing = throw new IllegalStateException("the list is empty")
    def isEmpty: Boolean = true
    def nonEmpty: Boolean = false
    def take(n: Int): FastNil.type = {
        if (n == 0) this else throw new IllegalStateException("the list is empty")
    }
    def takeWhile(f: Nothing ⇒ Boolean): FastList[Nothing] = this
    def filter(f: Nothing ⇒ Boolean): FastList[Nothing] = this
    def drop(n: Int): FastNil.type = {
        if (n == 0) this else throw new IllegalStateException("the list is empty")
    }
    def map[X](f: Nothing ⇒ X): FastList[X] = this
    def mapConserve[X >: Nothing <: AnyRef](f: Nothing ⇒ X): FastList[X] = this

}
case class :!:[@specialized(Int) T](head: T, private[immutable] var rest: FastList[T]) extends FastList[T] {

    def tail: FastList[T] = rest
    def isEmpty: Boolean = false
    def nonEmpty: Boolean = true

    def take(n: Int): FastList[T] = {
        if (n == 0)
            return FastNil;
        var taken = 1
        val result = new :!:(head, FastNil)
        var last = result
        var rest: FastList[T] = this.rest
        while (taken < n) {
            val x = rest.head
            rest = rest.tail
            val newLast = new :!:(x, FastNil)
            last.rest = newLast
            last = newLast
            taken += 1
        }
        result
    }

    def takeWhile(f: T ⇒ Boolean): FastList[T] = {
        val head = this.head
        if (!f(head))
            return FastNil;

        val result = new :!:(head, FastNil)
        var last = result
        var rest: FastList[T] = this.rest
        while (rest.nonEmpty && f(rest.head)) {
            val x = rest.head
            rest = rest.tail
            val newLast = new :!:(x, FastNil)
            last.rest = newLast
            last = newLast
        }
        result
    }

    def filter(f: T ⇒ Boolean): FastList[T] = {
        var result: FastList[T] = FastNil
        var last = result
        var rest: FastList[T] = this
        do {
            val x = rest.head
            rest = rest.tail
            if (f(x)) {
                val newLast = new :!:(x, FastNil)
                if (last.nonEmpty) {
                    last.asInstanceOf[:!:[T]].rest = newLast
                } else {
                    result = newLast
                }
                last = newLast
            }
        } while (rest.nonEmpty)
        result
    }

    def drop(n: Int): FastList[T] = {
        if (n == 0)
            return this;
        var dropped = 1
        var result: FastList[T] = this.rest
        while (dropped < n) {
            dropped += 1
            result = result.tail
        }
        result
    }

    def map[X](f: T ⇒ X): FastList[X] = {
        val result = new :!:(f(head), FastNil)
        var last = result
        var rest: FastList[T] = this.rest
        while (rest.nonEmpty) {
            val x = f(rest.head)
            rest = rest.tail
            val newLast = new :!:(x, FastNil)
            last.rest = newLast
            last = newLast
        }
        result
    }

    def mapConserve[X >: T <: AnyRef](f: T ⇒ X): FastList[X] = {
        val head = this.head
        val newHead = f(head)
        var updated = (head.asInstanceOf[AnyRef] ne newHead)
        val result = new :!:(newHead, FastNil)
        var last = result
        var rest: FastList[T] = this.rest
        while (rest.nonEmpty) {
            val e = rest.head
            val x = f(e)
            updated = updated || (x ne e.asInstanceOf[AnyRef])
            rest = rest.tail
            val newLast = new :!:(x, FastNil)
            last.rest = newLast
            last = newLast
        }
        if (updated)
            result
        else
            this
    }
}
