/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
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

import scala.language.implicitConversions

import scala.collection.GenIterable
import scala.collection.GenTraversableOnce
import scala.collection.AbstractIterator
import scala.collection.generic.CanBuildFrom
import scala.collection.mutable.Builder
import scala.collection.generic.FilterMonadic
import scala.collection.AbstractIterable

/**
 * A linked list which does not perform any length related checks. I.e., it fails in
 * case of `drop` and `take` etc. if the size of the list is smaller than expected.
 * Furthermore, all directly implemented methods use `while` loops for maximum
 * efficiency and the list is also specialized for primitive `int` values which
 * makes this list far more efficient when used for storing lists of `int` values.
 *
 * @note    In most cases a `Chain` can be used as a drop-in replacement for a standard
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
sealed trait Chain[@specialized(Int) +T]
    extends TraversableOnce[T]
    with FilterMonadic[T, Chain[T]]
    with Serializable { self ⇒

    /**
     * Represents a filtered [[Chain]]. Instances of [[ChainWithFilter]] are typically
     * created by [[Chain]]'s `withFilter` method.
     */
    class ChainWithFilter(p: T ⇒ Boolean) extends FilterMonadic[T, Chain[T]] {

        def map[B, That](f: T ⇒ B)(implicit bf: CanBuildFrom[Chain[T], B, That]): That = {
            val list = self
            var rest = list

            val b = bf(list)
            while (rest.nonEmpty) {
                val x = rest.head
                if (p(x)) b += f(x)
                rest = rest.tail
            }
            b.result
        }

        def flatMap[B, That](
            f: T ⇒ GenTraversableOnce[B]
        )(
            implicit
            bf: CanBuildFrom[Chain[T], B, That]
        ): That = {
            val list = self
            val b = bf(list)
            var rest = list
            while (rest.nonEmpty) {
                val x = rest.head
                if (p(x)) b ++= f(x).seq
                rest = rest.tail
            }
            b.result
        }

        def foreach[U](f: T ⇒ U): Unit = {
            var rest = self
            while (rest.nonEmpty) {
                val x = rest.head
                if (p(x)) f(x)
                rest = rest.tail
            }
        }

        def withFilter(q: T ⇒ Boolean): ChainWithFilter = {
            new ChainWithFilter(x ⇒ p(x) && q(x))
        }
    }

    final override def hasDefiniteSize: Boolean = true

    final override def isTraversableAgain: Boolean = true

    final override def seq: this.type = this

    override def foreach[U](f: T ⇒ U): Unit = {
        var rest = this
        while (rest.nonEmpty) {
            f(rest.head)
            rest = rest.tail
        }
    }

    /**
     * Executes the given function `f` for each element of this chain as long as
     * it returns `true`.
     */
    def foreachWhile(f: T ⇒ Boolean): Boolean = {
        var rest = this
        while (rest.nonEmpty) {
            if (!f(rest.head))
                return false;

            rest = rest.tail
        }
        return true;
    }

    def startsWith[X >: T](other: Chain[X]): Boolean = {
        var thisRest = this
        var otherRest = other
        while (otherRest.nonEmpty) {
            if (thisRest.isEmpty || thisRest.head != otherRest.head)
                return false;
            thisRest = thisRest.tail
            otherRest = otherRest.tail
        }
        true
    }

    /**
     *  Computes the shared prefix.
     */
    def sharedPrefix[X >: T](other: Chain[X]): Chain[T] = {
        val Nil = Naught
        var prefixHead: Chain[T] = Nil
        var prefixLast: :&:[T] = null
        var thisRest = this
        var otherRest = other
        while (thisRest.nonEmpty && otherRest.nonEmpty && thisRest.head == otherRest.head) {
            if (prefixLast == null) {
                prefixLast = new :&:[T](thisRest.head, Nil)
                prefixHead = prefixLast
            } else {
                prefixLast.rest = new :&:[T](thisRest.head, Nil)
            }
            thisRest = thisRest.tail
            otherRest = otherRest.tail
        }
        if (thisRest.isEmpty)
            this
        else if (otherRest.isEmpty)
            other.asInstanceOf[Chain[T]]
        else
            prefixHead
    }

    def forFirstN[U](n: Int)(f: (T) ⇒ U): Unit = {
        var rest = this
        var i = 0
        while (i < n) {
            val head = rest.head
            rest = rest.tail
            f(head)
            i += 1
        }
    }

    def flatMap[B, That](
        f: (T) ⇒ GenTraversableOnce[B]
    )(
        implicit
        bf: CanBuildFrom[Chain[T], B, That]
    ): That = {
        val b = bf(this)
        //OLD: foreach { t ⇒ f(t) foreach { e ⇒ builder += e } }
        var rest = this
        while (rest.nonEmpty) {
            val t = rest.head
            b ++= f(t).seq
            rest = rest.tail
        }
        b.result
    }

    def map[B, That](f: (T) ⇒ B)(implicit bf: CanBuildFrom[Chain[T], B, That]): That = {
        val builder = bf(this)
        var rest = this
        while (rest.nonEmpty) {
            val t = rest.head
            builder += f(t)
            rest = rest.tail
        }
        builder.result
    }

    def withFilter(p: (T) ⇒ Boolean): ChainWithFilter = new ChainWithFilter(p)

    def head: T

    def headOption: Option[T]

    def tail: Chain[T]

    def last: T = {
        var rest = this
        while (rest.tail.nonEmpty) { rest = rest.tail }
        rest.head
    }

    def isSingletonList: Boolean

    def hasMultipleElements: Boolean

    override def nonEmpty: Boolean

    /**
     * Returns the value of the element of this list with the given index.
     *
     * @param index A valid index. A value in the range [0...this.size-1].
     */
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

    def find(p: T ⇒ Boolean): Option[T] = {
        var rest = this
        while (rest.nonEmpty) {
            val e = rest.head
            if (p(e))
                return Some(e);
            rest = rest.tail
        }
        None
    }

    /**
     * Counts the number of elements.
     *
     * @note   This operation has complexity O(n).
     * @return The size of this list.
     */
    override def size: Int = {
        var result = 0
        var rest = this
        while (rest.nonEmpty) {
            result += 1
            rest = rest.tail
        }
        result
    }

    /**
     * Prepends the given element to this Chain.
     */
    def :&:[X >: T](x: X): Chain[X] = new :&:(x, this)

    /**
     * Prepends the given `int` value to this Chain if this chain is a chain of int values.
     */
    def :&:(x: Int)(implicit ev: this.type <:< Chain[Int]): Chain[Int] = {
        new :&:[Int](x, ev(this))
    }

    /**
     * Prepends the given `Chain` to `this` chain.
     */
    def :&::[X >: T](x: Chain[X]): Chain[X]

    /**
     * Prepends the given list to '''this list''' by setting the end of the given list to
     * this list.
     *
     * @note     '''This mutates the given list unless the given list is empty; hence
     *           The return value ''must not be ignored''.'''
     *
     * @note    Using this function is save if and only if no alias of this list
     *          or the given list exists.
     */
    private[opalj] def ++!:[X >: T](x: Chain[X]): Chain[X] = {
        if (x.isEmpty)
            return this;

        var lastNode = x.asInstanceOf[:&:[X]]
        while (lastNode.rest.nonEmpty) {
            lastNode = lastNode.rest.asInstanceOf[:&:[X]]
        }
        lastNode.rest = this
        x
    }

    /**
     * @see [[++!:]]
     */
    private[opalj] def ++![X >: T](x: Chain[X]): Chain[X] = x.++!:(this)

    /**
     * Clones this list and returns the cloned list as well as the last element of the list; using
     * the last element it is possible to immediately attach further elements to the list at its end.
     * If this list is empty, the last element is null.
     */
    private[opalj] def copy[X >: T](): (Chain[X], :&:[X]) = {
        val Nil: Chain[X] = Naught
        if (isEmpty)
            return (this, null);

        val result = new :&:[X](head, Nil)
        var last = result
        var rest: Chain[X] = this.tail
        while (rest.nonEmpty) {
            val x = rest.head
            rest = rest.tail
            val newLast = new :&:[X](x, Nil)
            last.rest = newLast
            last = newLast
        }
        (result, last)
    }

    // TODO Manually specialize Chain!
    def ++[X >: T](that: Chain[X]): Chain[X] = {
        if (that.isEmpty)
            return this;
        if (this.isEmpty)
            return that;

        val (head, last) = copy[X]
        last.rest = that
        head
    }

    // TODO Manually specialize Chain!
    def ++[X >: T](other: Traversable[X]): Chain[X] = {
        if (other.isEmpty)
            return this;

        val that = other.foldLeft(new Chain.ChainBuilder[X])(_ += _).result
        if (this.isEmpty)
            that
        else {
            val (head, last) = copy[X]()
            last.rest = that
            head
        }
    }

    /**
     * Takes the first n elements of this list. If this list does not contain at
     * least n elements an IllegalStateException  will be thrown.
     * @param n    An int value in the range [0...this.size].
     * @return     A list consisting of the first n value.
     */
    def take(n: Int): Chain[T]

    /**
     * Takes up to the first n elements of this list. The returned list will contain at most
     * `this.size` elements.
     * @param n    An int value euqal or larger than 0.
     * @return     A list consisting of the first n value.
     */
    def takeUpTo(n: Int): Chain[T]

    def takeWhile(f: T ⇒ Boolean): Chain[T]

    def filter(f: T ⇒ Boolean): Chain[T]

    def filterNot(f: T ⇒ Boolean): Chain[T] = filter(t ⇒ !f(t))

    def drop(n: Int): Chain[T]

    def dropWhile(f: T ⇒ Boolean): Chain[T] = {
        var rest = this
        while (rest.nonEmpty && f(rest.head)) { rest = rest.tail }
        rest
    }

    def zip[X](other: GenIterable[X]): Chain[(T, X)] = {
        if (this.isEmpty)
            return this.asInstanceOf[Naught.type];
        val otherIt = other.iterator
        if (!otherIt.hasNext)
            return Naught;

        var thisIt = this.tail
        val result: :&:[(T, X)] = new :&:((this.head, otherIt.next), Naught)
        var last = result
        while (thisIt.nonEmpty && otherIt.hasNext) {
            val newLast = new :&:((thisIt.head, otherIt.next), Naught)
            last.rest = newLast
            last = newLast
            thisIt = thisIt.tail
        }
        result
    }

    def zip[X](other: Chain[X]): Chain[(T, X)] = {
        if (this.isEmpty)
            return this.asInstanceOf[Naught.type];
        if (other.isEmpty)
            return other.asInstanceOf[Naught.type];

        var thisIt = this.tail
        var otherIt = other.tail
        val result: :&:[(T, X)] = new :&:((this.head, other.head), Naught)
        var last = result
        while (thisIt.nonEmpty && otherIt.nonEmpty) {
            val newLast = new :&:((thisIt.head, otherIt.head), Naught)
            last.rest = newLast
            last = newLast
            thisIt = thisIt.tail
            otherIt = otherIt.tail
        }
        result
    }

    def zipWithIndex: Chain[(T, Int)] = {
        var index = 0
        map[(T, Int), Chain[(T, Int)]] { e ⇒
            val currentIndex = index
            index += 1
            (e, currentIndex)
        }
    }

    /**
     * @see    `merge`
     */
    def corresponds[X](other: Chain[X])(f: (T, X) ⇒ Boolean): Boolean = {
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

    def mapConserve[X >: T <: AnyRef](f: T ⇒ X): Chain[X]

    def reverse: Chain[T]

    override def mkString: String = mkString("", "", "")

    override def mkString(pre: String, sep: String, post: String): String = {
        val result = new StringBuilder(pre)
        var rest = this
        if (rest.nonEmpty) {
            result.append(head)
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

    override def toIterable: Iterable[T] = {
        new AbstractIterable[T] { def iterator: Iterator[T] = self.toIterator }
    }

    def toIterator: Iterator[T] = {
        new AbstractIterator[T] {
            private var rest = self
            def hasNext: Boolean = rest.nonEmpty
            def next(): T = {
                val result = rest.head
                rest = rest.tail
                result
            }
        }
    }

    def mapToIntIterator(f: T ⇒ Int): IntIterator = {
        new IntIterator {
            private var rest = self
            def hasNext: Boolean = rest.nonEmpty
            def next(): Int = {
                val result = f(rest.head)
                rest = rest.tail
                result
            }
        }
    }

    /**
     * Returns a newly created `Traversable[T]` collection.
     */
    def toTraversable: Traversable[T] = {
        new Traversable[T] { def foreach[U](f: T ⇒ U): Unit = self.foreach(f) }
    }

    def toIntArraySet(implicit ev: T <:< Int): IntArraySet = {
        foldLeft(new IntArraySetBuilder())(_ += _).result()
    }

    def toIntTrieSet(implicit ev: T <:< Int): IntTrieSet = {
        // foldLeft(EmptyIntTrieSet: IntTrieSet)(_ + _)
        var set: IntTrieSet = EmptyIntTrieSet
        var rest = this
        while (rest ne Naught) {
            set += rest.head
            rest = rest.tail
        }
        set
    }

    def toStream: Stream[T] = toTraversable.toStream

    def copyToArray[B >: T](xs: Array[B], start: Int, len: Int): Unit = {
        val max = xs.length
        var copied = 0
        var rest = this
        while (copied < len && start + copied < max && rest.nonEmpty) {
            xs(start + copied) = rest.head
            copied += 1
            rest = rest.tail
        }
    }

    /**
     * Merges this chain with the given chain by merging the values using the given function.
     * If all results are the same (reference equality) as this chain's elements then the result
     * will be `this`. Otherwise, only the tail that is identical will be kept.
     *
     * @param     other A chain with the same number of elements as this chain. If the size of
     *            the chains it not equal, the result is undefined.
     */
    def merge[X <: AnyRef, Z >: T <: AnyRef](that: Chain[X])(f: (T, X) ⇒ Z): Chain[Z]

    /**
     * Fuses this chain with the given chain by fusing the values using the given function.
     * The function `onDiff` is only called if the given list's element and this list's
     * element differ. Hence, when the tail of both lists is equal fusing both lists
     * will terminate immediately and the common tail is attached to the new heading.
     *
     * @param     other A chain with the same number of elements as this chain. If the size of
     *            the chains it not equal, the result is undefined.
     */
    def fuse[X >: T <: AnyRef](that: Chain[X], onDiff: (T, X) ⇒ X): Chain[X]
}

//trait ChainLowPriorityImplicits

/**
 * Factory for [[Chain]]s.
 *
 * @author Michael Eichberg
 */
object Chain /* extends ChainLowPriorityImplicits */ {

    /**
     * Builder for [[Chain]]s. The builder is specialized for the primitive
     * type `Int` to enable the creation of new instances of the correspondingly
     * specialized list.
     *
     * @note     The builder must not be used after a `result` call.
     *
     * @tparam T The type of the list's element.
     */
    class ChainBuilder[@specialized(Int) T] extends Builder[T, Chain[T]] {
        private[this] var list: Chain[T] = null
        private[this] var last: :&:[T] = null
        def +=(elem: T): this.type = {
            val newLast = new :&:[T](elem, Naught)
            if (list == null) {
                list = newLast
            } else {
                last.rest = newLast
            }
            last = newLast
            this
        }
        def clear(): Unit = list = null

        /** Returns the constructed list. The builder must not be used afterwards. */
        def result(): Chain[T] = { val list = this.list; if (list == null) Naught else list }
    }

    private[this] val baseCanBuildFrom = new CanBuildFrom[Chain[_], AnyRef, Chain[AnyRef]] {
        def apply(from: Chain[_]) = new ChainBuilder[AnyRef]
        def apply() = new ChainBuilder[AnyRef]
    }
    implicit def canBuildFrom[A <: AnyRef]: CanBuildFrom[Chain[_], A, Chain[A]] = {
        baseCanBuildFrom.asInstanceOf[CanBuildFrom[Chain[_], A, Chain[A]]]
    }
    private[this] val specializedCanBuildFrom = new CanBuildFrom[Chain[_], Int, Chain[Int]] {
        def apply(from: Chain[_]) = new ChainBuilder[Int]
        def apply() = new ChainBuilder[Int]
    }
    implicit def canBuildIntChainFrom: CanBuildFrom[Chain[_], Int, Chain[Int]] = {
        specializedCanBuildFrom
    }

    val GenericSpecializedCBF = new CanBuildFrom[Any, Int, Chain[Int]] {
        def apply(from: Any) = new ChainBuilder[Int]
        def apply() = new ChainBuilder[Int]
    }

    implicit def toTraversable[T](cl: Chain[T]): Traversable[T] = cl.toIterable

    def newBuilder[T](implicit t: scala.reflect.ClassTag[T]): ChainBuilder[T] = {
        if (t.runtimeClass == classOf[Int])
            new ChainBuilder[Int].asInstanceOf[ChainBuilder[T]]
        else
            new ChainBuilder[T]
    }

    final val IncompleteEmptyChain = new IncompleteCollection(Naught: Chain[Nothing])

    final val CompleteEmptyChain = new CompleteCollection(Naught: Chain[Nothing])

    /**
     * Returns an empty list.
     *
     * @note    In general it is preferable to directly use [[Naught]].
     */
    def empty[T]: Chain[T] = Naught

    def singleton[@specialized(Int) T](e: T): Chain[T] = new :&:[T](e, Naught)

    /**
     * @note     The recommended way to create a Chain with one element is to
     *           use the `singleton` method.
     */
    def apply[@specialized(Int) T](es: T*): Chain[T] = {
        val naught = Naught
        if (es.isEmpty)
            return naught;
        val result = new :&:[T](es.head, naught)
        var last = result
        val it = es.iterator
        it.next // es is non-empty
        it foreach { e ⇒
            val newLast = new :&:[T](e, naught)
            last.rest = newLast
            last = newLast
        }
        result
    }

}

/**
 * An empty [[Chain]]s.
 *
 * @author Michael Eichberg
 */
case object Naught extends Chain[Nothing] {

    private def listIsEmpty = new NoSuchElementException("the list is empty")

    def head: Nothing = throw listIsEmpty
    def headOption: Option[Nothing] = None
    def tail: Nothing = throw listIsEmpty
    def isEmpty: Boolean = true
    def isSingletonList: Boolean = false
    def hasMultipleElements: Boolean = false
    override def nonEmpty: Boolean = false
    def :&::[X >: Nothing](x: Chain[X]): Chain[X] = x
    def take(n: Int): Naught.type = { if (n == 0) this else throw listIsEmpty }
    def takeUpTo(n: Int): this.type = this
    def takeWhile(f: Nothing ⇒ Boolean): Chain[Nothing] = this
    def filter(f: Nothing ⇒ Boolean): Chain[Nothing] = this
    def drop(n: Int): Naught.type = { if (n == 0) this else throw listIsEmpty }
    def mapConserve[X >: Nothing <: AnyRef](f: Nothing ⇒ X): Chain[X] = this
    def reverse: Chain[Nothing] = this

    def merge[X <: AnyRef, Z >: Nothing <: AnyRef](
        that: Chain[X]
    )(
        f: (Nothing, X) ⇒ Z
    ): Chain[Z] = this

    def fuse[X >: Nothing <: AnyRef](that: Chain[X], onDiff: (Nothing, X) ⇒ X): Chain[X] = this
}

/**
 * An container for a list element.
 *
 * @author Michael Eichberg
 */
final case class :&:[@specialized(Int) T](
        head:                    T,
        private[opalj] var rest: Chain[T] = Naught
) extends Chain[T] {

    def headOption: Option[T] = Some(head)

    def tail: Chain[T] = rest

    def isSingletonList: Boolean = rest eq Naught

    def hasMultipleElements: Boolean = rest ne Naught

    def isEmpty: Boolean = false

    override def nonEmpty: Boolean = true

    // prepends the given list... to make sure that
    // we keep the specialization we have to ask the
    // other list to append this one...
    def :&::[X >: T](x: Chain[X]): Chain[X] = {
        x match {
            case Naught        ⇒ this
            case other: :&:[X] ⇒ other ++ this
        }
    }

    def take(n: Int): Chain[T] = {
        val Nil = Naught

        if (n == 0)
            return Nil;

        var taken = 1
        val result = new :&:[T](head, Nil)
        var last = result
        var rest: Chain[T] = this.rest
        while (taken < n) {
            val x = rest.head
            rest = rest.tail
            val newLast = new :&:[T](x, Nil)
            last.rest = newLast
            last = newLast
            taken += 1
        }
        result
    }

    def takeUpTo(n: Int): Chain[T] = {
        val Nil = Naught

        if (n == 0)
            return Nil;

        var taken = 1
        val result = new :&:[T](head, Nil)
        var last = result
        var rest: Chain[T] = this.rest
        while (taken < n && rest.nonEmpty) {
            val x = rest.head
            rest = rest.tail
            val newLast = new :&:[T](x, Nil)
            last.rest = newLast
            last = newLast
            taken += 1
        }
        result

    }

    def takeWhile(f: T ⇒ Boolean): Chain[T] = {
        val head = this.head
        val Nil = Naught

        if (!f(head))
            return Nil;

        val result = new :&:(head, Nil)
        var last = result
        var rest: Chain[T] = this.rest
        while (rest.nonEmpty && f(rest.head)) {
            val x = rest.head
            rest = rest.tail
            val newLast = new :&:[T](x, Nil)
            last.rest = newLast
            last = newLast
        }
        result
    }

    def filter(f: T ⇒ Boolean): Chain[T] = {
        val Nil = Naught

        var result: Chain[T] = Nil
        var last = result
        var rest: Chain[T] = this
        do {
            val x = rest.head
            rest = rest.tail
            if (f(x)) {
                val newLast = new :&:[T](x, Nil)
                if (last.nonEmpty) {
                    last.asInstanceOf[:&:[T]].rest = newLast
                } else {
                    result = newLast
                }
                last = newLast
            }
        } while (rest.nonEmpty)
        result
    }

    def drop(n: Int): Chain[T] = {
        if (n == 0)
            return this;

        var dropped = 1
        var result: Chain[T] = this.rest
        while (dropped < n) {
            dropped += 1
            result = result.tail
        }
        result
    }

    def mapConserve[X >: T <: AnyRef](f: T ⇒ X): Chain[X] = {
        val head = this.head
        val newHead = f(head)
        var updated = (head.asInstanceOf[AnyRef] ne newHead)
        val result = new :&:[X](newHead, Naught)
        var last = result
        var rest: Chain[T] = this.rest
        while (rest.nonEmpty) {
            val e = rest.head
            val x = f(e)
            updated = updated || (x ne e.asInstanceOf[AnyRef])
            rest = rest.tail
            val newLast = new :&:[X](x, Naught)
            last.rest = newLast
            last = newLast
        }
        if (updated)
            result
        else
            this
    }

    def reverse: Chain[T] = {
        var result: Chain[T] = new :&:[T](head, Naught)
        var rest = this.rest
        while (rest.nonEmpty) {
            // NOTE: WE CAN'T USE THE STANDARD :&: OPERATOR
            // BECAUSE WE WOULD LOOSE THE SPECIALIZATION OF THE LIST!
            // DOESN'T WORK: result :&:= rest.head
            result = new :&:[T](rest.head, result)
            rest = rest.tail
        }
        result
    }

    /**
     * @note    The `merge` function first calls the given function and then checks if the
     *             result is reference equal to the element of the first list while `fuse` first
     *             checks the reference equality of the members before it calls the given function.
     *             Therefore `fuse` can abort checking all further values when the
     *             remaining list fragments are reference equal because both lists are immutable.
     *             In other words: `fuse` is an optimized version of `merge` where the function `f`
     *             has the following shape: `(x,y) => if(x eq y) x else /*whatever*/`.
     */
    def fuse[X >: T <: AnyRef](
        that:   Chain[X],
        onDiff: (T, X) ⇒ X
    ): Chain[X] = {

        var thisHead: Chain[T] = this
        var thatHead: Chain[X] = that

        var equalHead: Chain[X] = null

        var newHead: :&:[X] = null
        var newLast: :&:[X] = null
        def appendToNewLast(t: X): Unit = {
            if (newLast eq null) {
                newLast = new :&:[X](t, Naught)
                newHead = newLast
            } else {
                val e = new :&:[X](t, Naught)
                newLast.rest = e
                newLast = e
            }
        }

        do {
            val thisValue: T = thisHead.head
            val thatValue: X = thatHead.head

            if (thatValue eq thisValue.asInstanceOf[AnyRef]) {
                if (equalHead eq null) equalHead = thisHead
            } else {
                val mergedValue: X = onDiff(thisValue, thatValue)
                if (mergedValue eq thisValue.asInstanceOf[AnyRef]) {
                    if (equalHead eq null) equalHead = thisHead
                } else {
                    if (equalHead ne null) {
                        // we have to clone all elements in the range [equalNode...thisHead)
                        // to make it possible to attach a new element.
                        appendToNewLast(equalHead.head)
                        equalHead = equalHead.tail
                        while (equalHead ne thisHead) {
                            appendToNewLast(equalHead.head)
                            equalHead = equalHead.tail
                        }
                        equalHead = null
                    }
                    appendToNewLast(mergedValue)
                }
            }
            thisHead = thisHead.tail
            thatHead = thatHead.tail
        } while (thisHead.nonEmpty && (thisHead ne thatHead))

        if (newHead eq null) {
            this // or equalHead
        } else if (equalHead ne null) {
            newLast.rest = equalHead
            newHead
        } else if (thisHead.nonEmpty) {
            newLast.rest = thisHead
            newHead
        } else {
            newHead
        }
    }

    def merge[X <: AnyRef, Z >: T <: AnyRef](
        that: Chain[X]
    )(
        f: (T, X) ⇒ Z
    ): Chain[Z] = {
        // The idea: iterate over both lists in parallel, when the merge results in the
        // same value as this list's value, then we do not create a new list element, but
        // instead store this information and otherwise wait until we see a change.
        var thisHead: Chain[T] = this
        var thatHead: Chain[X] = that

        var equalHead: Chain[Z] = null

        var newHead: :&:[Z] = null
        var newLast: :&:[Z] = null
        def appendToNewLast(t: Z): Unit = {
            if (newLast eq null) {
                newLast = new :&:[Z](t, Naught)
                newHead = newLast
            } else {
                val e = new :&:[Z](t, Naught)
                newLast.rest = e
                newLast = e
            }
        }

        do {
            val thisValue: T = thisHead.head
            val thatValue: X = thatHead.head

            val mergedValue: Z = f(thisValue, thatValue)
            if (mergedValue eq thisValue.asInstanceOf[AnyRef]) {
                if (equalHead eq null) {
                    equalHead = thisHead
                }
            } else {
                if (equalHead ne null) {
                    // we have to clone all elements in the range [equalNode...thisHead)
                    // to make it possible to attach a new element.
                    appendToNewLast(equalHead.head)
                    equalHead = equalHead.tail
                    while (equalHead ne thisHead) {
                        appendToNewLast(equalHead.head)
                        equalHead = equalHead.tail
                    }
                    equalHead = null
                }
                appendToNewLast(mergedValue)
            }
            thisHead = thisHead.tail
            thatHead = thatHead.tail
        } while (thisHead.nonEmpty)

        if (newHead eq null) {
            this // or equalHead
        } else if (equalHead ne null) {
            newLast.rest = equalHead
            newHead
        } else
            newHead
    }

    override def toString: String = {
        //s"$head :&: ${rest.toString}" // cannot handle very long lists (uses recursion)...
        mkString("", " :&: ", " :&: Naught")
    }
}
