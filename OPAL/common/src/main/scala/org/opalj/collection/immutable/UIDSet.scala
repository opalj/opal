/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
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

import scala.collection.GenTraversableOnce
import scala.collection.generic.CanBuildFrom
import scala.collection.generic.FilterMonadic
import scala.collection.mutable.Builder

import org.opalj.collection.UID.areEqual
import org.opalj.collection.immutable.ChainedList.ChainedListBuilder

/**
 * An immutable, sorted set of elements of type `UID` based on a simple search tree.
 *
 * Contains checks etc. are based on the element's unique id.
 *
 * [[UIDSet$]]s are constructed using the factory methods of the companion object and
 * are generally expected to be small.
 *
 * @author Michael Eichberg
 */
sealed trait UIDSet[+T <: UID]
        extends TraversableOnce[T]
        with FilterMonadic[T, UIDSet[T]]
        with Serializable { self ⇒

    /**
     * Represents a filtered [[UIDSet]]. Instances of [[WithFilter]] are typically
     * created by [[UIDSet]]'s `withFilter` method.
     */
    class WithFilter(p: T ⇒ Boolean) extends FilterMonadic[T, UIDSet[T]] {

        def map[B, That](f: T ⇒ B)(implicit bf: CanBuildFrom[UIDSet[T], B, That]): That = {
            val set = self
            val b = bf(set)
            set.foreach { e ⇒ if (p(e)) b += f(e) }
            b.result
        }

        def flatMap[B, That](
            f: T ⇒ GenTraversableOnce[B]
        )(
            implicit
            bf: CanBuildFrom[UIDSet[T], B, That]
        ): That = {
            val set = self
            val b = bf(set)
            set.foreach { e ⇒ if (p(e)) b ++= f(e).seq }
            b.result
        }

        def foreach[U](f: T ⇒ U): Unit = self.foreach { e ⇒ if (p(e)) f(e) }
        def withFilter(q: T ⇒ Boolean): WithFilter = new WithFilter(x ⇒ p(x) && q(x))
    }

    //
    // 1: QUERY THE PROPERTIES OF THIS SET OR THE PROPPERTIES OF ITS ELEMENTS
    //

    final def hasDefiniteSize: Boolean = true

    final def isTraversableAgain: Boolean = true

    /**
     * Tests if the size of this set is "1" (Guaranteed complexity O(1)).
     */
    def isSingletonSet: Boolean

    /**
     * Returns the first element of this set. I.e., the element with the smallest
     * unique id.
     */
    @throws[NoSuchElementException]("If the set is empty.") def head: T

    /**
     * Returns the first element of this set. I.e., the element with the smallest
     * unique id.
     */
    @throws[NoSuchElementException]("If the set is empty.") final def first: T = head

    /**
     * Returns the last element of this set. I.e., the element with the largest
     * unique id.
     */
    @throws[NoSuchElementException]("If the set is empty.") def last: T

    /**
     * Returns `true` if the an element with the same id as the given element is already
     * in this list, `false` otherwise.
     */
    def contains[X <: UID](o: X): Boolean

    /**
     * Performs a qualified comparison of this set with the given set.
     */
    def compare[X >: T <: UID](that: UIDSet[X]): SetRelation = {
        val thisSize = this.size
        val thatSize = that.size

        if (thisSize < thatSize) {
            if (this.forall { that.contains(_) })
                return StrictSubset;
        } else if (this.size == that.size) {
            if (this == that)
                return EqualSets;
        } else /*this.size > that.size*/ {
            if (that.forall { this.contains(_) })
                return StrictSuperset;
        }

        UncomparableSets
    }

    //
    // 2: PROCESS THE ELEMENTS OF THIS SET
    //

    // def foreach ...
    // def foldLeft ...

    //
    // 3: EXTEND THIS SET
    //

    /**
     * Adds the given value to this set if the value is not already stored in
     * this set.
     */
    def +[X >: T <: UID](e: X): UIDSet[X]

    /**
     * Adds the given elements to this set. Each new element is added using the primitive
     * [[+]] operation.
     */
    def ++[X >: T <: UID](es: TraversableOnce[X]): UIDSet[X] = {
        es.foldLeft[UIDSet[X]](this)((c, v) ⇒ c + v)
    }

    //
    // 4: SELECT (MULTIPLE ELEMENTS)
    //

    /**
     * Returns the remaining elements of this set. This operation has linear complexity.
     */
    @throws[NoSuchElementException]("If the set is empty.") def tail(): UIDSet[T]

    /**
     * Returns a new `UIDSet` that contains all elements which satisfy the given
     * predicate.
     */
    def filter(p: T ⇒ Boolean): UIDSet[T]

    /**
     * Returns a new `UIDList` that contains all elements which '''do not satisfy'''
     * the given predicate.
     */
    final def filterNot(p: T ⇒ Boolean): UIDSet[T] = filter((e) ⇒ !p(e))

    def withFilter(p: T ⇒ Boolean): WithFilter = new WithFilter(p)

    override def copyToArray[B >: T](xs: Array[B], start: Int, len: Int): Unit = {
        if (len == 0)
            return ;

        val max = xs.length
        var index = -1
        foreach { e ⇒
            index += 1
            if (index < len && start + index < max) {
                xs(start + index) = e
            } else {
                return ;
            }
        }
    }

    def intersect[X >: T <: UID](that: UIDSet[X]): UIDSet[X] = {
        val thisIt = this.toIterator
        val thatIt = that.toIterator
        var result: UIDSet[X] = UIDSet0
        while (thisIt.hasNext && thatIt.hasNext) {
            var thisNext = thisIt.next
            var thisNextId = thisNext.id
            var thatNext = thatIt.next
            var thatNextId = thatNext.id
            while (thisNextId < thatNextId && thisIt.hasNext) {
                thisNext = thisIt.next
                thisNextId = thisNext.id
            }
            while (thatNextId < thisNextId && thatIt.hasNext) {
                thatNext = thatIt.next
                thatNextId = thatNext.id
            }
            if (thisNextId == thatNextId) {
                result += thisNext
            }
        }
        result
    }

    //
    // 5: TRANSFORM
    //
    final def seq: TraversableOnce[T] = this

    /**
     * Creates a new `(UID)Set` which contains the mapped values as specified by the given
     * function `f`.
     *
     * @example
     * 		This class predefines two implicit functions create either UIDSets or regular sets.
     * 		If the compiler is not able to select the correct one, it is usually sufficient
     * 		to explicitly specify the target type.
     * {{{
     * scala> case class MyUID(val id : Int) extends org.opalj.collection.UID
     * scala> var us = UIDSet(MyUID(1),MyUID(2)) + MyUID(-3)
     * scala> us.map(e => MyUID(e.id+1))
     * 		  <console>:21: error: ambiguous implicit values:
     * 		  both method canBuildFrom in object UIDSet of type ...
     * 		  and method canBuildSetFromUIDSet in object UIDSet of type ...
     * 		  match expected type ...
     * 		  us.map(e => MyUID(e.id+1))
     * 		  ^
     * scala> // this problem is easily fixed by; e.g., specifying the type of the target
     * scala> // collection (e.g. by assigning the list to an appropriately typed variable)
     * scala> us = us.map(e => MyUID(e.id+1))
     * us: org.opalj.collection.immutable.UIDSet[MyUID] = UIDSet(MyUID(-2), MyUID(2), MyUID(3))
     * scala> val s : Set[MyUID] = us.map(e => MyUID(e.id+1))
     * s: scala.collection.immutable.Set[MyUID] = Set(MyUID(-2), MyUID(2), MyUID(3))
     * }}}
     */
    def map[B, That](f: (T) ⇒ B)(implicit bf: CanBuildFrom[UIDSet[T], B, That]): That = {
        var b = bf(this)
        foreach { e ⇒ b += f(e) }
        b.result
    }

    def flatMap[B, That](
        f: T ⇒ GenTraversableOnce[B]
    )(
        implicit
        bf: CanBuildFrom[UIDSet[T], B, That]
    ): That = {
        val set = self
        val b = bf(set)
        set.foreach { e ⇒ b ++= f(e).seq }
        b.result
    }

    override def toStream: Stream[T] = toIterator.toStream

    override def toTraversable: Traversable[T] = {
        new Traversable[T] { def foreach[U](f: T ⇒ U): Unit = self.foreach(f) }
    }

    override def toString: String = mkString("UIDSet(", ", ", ")")

    //
    // 6: Other
    //

    // DO NOT FORGET TO OVERRIDE:
    def equals(other: Any): Boolean
    def hashCode: Int
}

/**
 * Representation of the empty set of `UID` elements.
 *
 * @author Michael Eichberg
 */
object UIDSet0 extends UIDSet[Nothing] {

    override def size = 0

    override def isEmpty = true

    override def nonEmpty = false

    override def isSingletonSet = false

    override def head: Nothing = throw new NoSuchElementException("the set is empty")

    override def last: Nothing = throw new NoSuchElementException("the set is empty")

    override def exists(p: Nothing ⇒ Boolean): Boolean = false

    override def forall(p: Nothing ⇒ Boolean) = true

    override def contains[X <: UID](o: X): Boolean = false

    override def find(p: Nothing ⇒ Boolean): None.type = None

    override def foreach[U](f: Nothing ⇒ U): Unit = {}

    override def foldLeft[B](b: B)(op: (B, Nothing) ⇒ B): B = b

    override def +[X <: UID](e: X): UIDSet[X] = new UIDSet1(e)

    override def tail: UIDSet[Nothing] = throw new NoSuchElementException("the set is empty")

    override def filter(p: Nothing ⇒ Boolean): this.type = this

    override def toIterator: Iterator[Nothing] = Iterator.empty

    override def equals(other: Any): Boolean = other.isInstanceOf[UIDSet0.type]

    override def hashCode: Int = 34237

    def unapply(s: UIDSet[_]): Boolean = /*s.isEmpty*/ s eq this
}

private[collection] trait NonEmptyUIDSet[+T <: UID] extends UIDSet[T] {
    final override def isEmpty = false
    final override def nonEmpty = true
}

/**
 * A [[UIDSet]] that contains a single element.
 *
 * @author Michael Eichberg
 */
final class UIDSet1[T <: UID]( final val e: T) extends NonEmptyUIDSet[T] { thisSet ⇒

    override def size = 1

    override def isSingletonSet: Boolean = true

    override def +[X >: T <: UID](o: X): UIDSet[X] = {
        val e = this.e
        if (o eq e)
            return this;

        val oId = o.id
        val eId = e.id
        if (oId == eId)
            this
        else if (oId < eId)
            new UIDSet2(o, e)
        else
            new UIDSet2(e, o)
    }

    override def head: T = e

    override def last: T = e

    override def tail: UIDSet[T] = UIDSet0

    override def foreach[U](f: T ⇒ U): Unit = f(e)

    override def forall(f: T ⇒ Boolean): Boolean = f(e)

    override def exists(f: T ⇒ Boolean): Boolean = f(e)

    override def contains[X <: UID](o: X): Boolean = UID.areEqual(e, o)

    override def find(f: T ⇒ Boolean): Option[T] = { val e = this.e; if (f(e)) Some(e) else None }

    override def filter(f: T ⇒ Boolean): UIDSet[T] = if (f(e)) this else UIDSet0

    override def foldLeft[B](b: B)(op: (B, T) ⇒ B): B = op(b, e)

    override def reduce[X >: T](op: (X, X) ⇒ X): X = e

    override def toSeq = e :: Nil

    override def toIterator: Iterator[T] = Iterator.single(e)

    override def equals(other: Any): Boolean = {
        other match {
            case that: UIDSet1[T] ⇒ areEqual(this.e, that.e)
            case _                ⇒ false
        }
    }

    override def hashCode: Int = e.id * 41

}
/**
 * Extractor for UIDSets with exactly one element.
 */
object UIDSet1 {

    def unapply[T <: UID](set: UIDSet1[T]): Some[T] = Some(set.e)

}

/**
 * A [[UIDSet]] that contains two elements that have different unique ids.
 * To create an instance use the [[UIDSet]] object.
 *
 * @author Michael Eichberg
 */
final class UIDSet2[T <: UID] private[collection] (
        final val e1: T,
        final val e2: T
) extends NonEmptyUIDSet[T] { thisSet ⇒

    override def isSingletonSet: Boolean = false

    override def size = 2

    override def +[X >: T <: UID](o: X): UIDSet[X] = {
        val oId = o.id
        val e1 = this.e1
        val e2 = this.e2

        val e1Id = e1.id
        if (oId < e1Id)
            new UIDSet3(o, e1, e2)
        else if (oId == e1Id)
            this
        else {
            val e2Id = e2.id
            if (oId < e2Id)
                new UIDSet3(e1, o, e2)
            else if (oId == e2Id)
                this
            else
                new UIDSet3(e1, e2, o)
        }
    }

    override def head: T = e1

    override def last: T = e2

    override def tail: UIDSet[T] = new UIDSet1(e2)

    override def foreach[U](f: T ⇒ U): Unit = { f(e1); f(e2) }

    override def forall(f: T ⇒ Boolean): Boolean = f(e1) && f(e2)

    override def exists(f: T ⇒ Boolean): Boolean = f(e1) || f(e2)

    override def contains[X <: UID](o: X): Boolean = {
        val oId = o.id
        e1.id == oId || e2.id == oId
    }

    override def find(f: T ⇒ Boolean): Option[T] = {
        if (f(e1))
            Some(e1)
        else if (f(e2))
            Some(e2)
        else
            None
    }

    override def filter(f: T ⇒ Boolean): UIDSet[T] = {
        val e1 = this.e1
        val e2 = this.e2

        if (f(e1)) {
            if (f(e2)) {
                this
            } else {
                new UIDSet1(e1)
            }
        } else if (f(e2)) {
            new UIDSet1(e2)
        } else
            UIDSet0
    }

    override def foldLeft[B](b: B)(op: (B, T) ⇒ B): B = op(op(b, e1), e2)

    override def reduce[X >: T](op: (X, X) ⇒ X): X = op(e1, e2)

    override def toSeq = e1 :: e2 :: Nil

    override def toIterator = new Iterator[T] {
        var index = 0
        def hasNext = index < 2
        def next = {
            val value = index match {
                case 0 ⇒ e1
                case 1 ⇒ e2
                case _ ⇒ throw new NoSuchElementException
            }
            index += 1
            value
        }
    }

    override def equals(other: Any): Boolean = {
        other match {
            case that: UIDSet2[T] ⇒ this.e1 === that.e1 && this.e2 === that.e2
            case _                ⇒ false
        }
    }

    override def hashCode: Int = e1.id * 41 + e2.id

}

/**
 * A set of three elements with different unique ids.
 */
final class UIDSet3[T <: UID] private[collection] (
        final val e1: T,
        final val e2: T,
        final val e3: T
) extends NonEmptyUIDSet[T] { thisSet ⇒

    override def isSingletonSet: Boolean = false

    override def size = 3

    override def +[X >: T <: UID](o: X): UIDSet[X] = {
        val oId = o.id
        val e1 = this.e1
        val e2 = this.e2
        val e3 = this.e3
        val e1Id = e1.id
        if (oId < e1Id)
            new UIDSet4(o, e1, e2, e3)
        else if (oId == e1Id)
            this
        else {
            val e2Id = e2.id
            if (oId < e2Id)
                new UIDSet4(e1, o, e2, e3)
            else if (oId == e2Id)
                this
            else {
                val e3Id = e3.id
                if (oId < e3Id)
                    new UIDSet4(e1, e2, o, e3)
                else if (oId == e3Id)
                    this
                else
                    new UIDSet4(e1, e2, e3, o)
            }
        }
    }

    override def head = e1

    override def last: T = e3

    override def foreach[U](f: T ⇒ U): Unit = { f(e1); f(e2); f(e3) }

    override def forall(f: T ⇒ Boolean): Boolean = f(e1) && f(e2) && f(e3)

    override def exists(f: T ⇒ Boolean): Boolean = f(e1) || f(e2) || f(e3)

    override def contains[X <: UID](o: X): Boolean = {
        val oId = o.id
        e1.id == oId || e2.id == oId || e3.id == oId
    }

    override def tail = new UIDSet2(e2, e3)

    override def foldLeft[B](b: B)(op: (B, T) ⇒ B): B = op(op(op(b, e1), e2), e3)

    override def find(f: T ⇒ Boolean): Option[T] = {
        if (f(e1))
            Some(e1)
        else if (f(e2))
            Some(e2)
        else if (f(e3))
            Some(e3)
        else
            None
    }

    def filter(f: T ⇒ Boolean): UIDSet[T] = {
        val e1 = this.e1
        val e2 = this.e2
        val e3 = this.e3
        if (f(e1)) {
            if (f(e2)) {
                if (f(e3))
                    this
                else
                    new UIDSet2(e1, e2)
            } else if (f(e3)) {
                new UIDSet2(e1, e3)
            } else
                UIDSet(e1)
        } else if (f(e2)) {
            if (f(e3)) {
                new UIDSet2(e2, e3)
            } else {
                new UIDSet1(e2)
            }
        } else if (f(e3)) {
            new UIDSet1(e3)
        } else {
            UIDSet0
        }
    }

    override def reduce[X >: T](op: (X, X) ⇒ X): X = op(op(e1, e2), e3)

    override def toSeq = e1 :: e2 :: e3 :: Nil

    override def toIterator = new Iterator[T] {
        var index = 0
        def hasNext = index < 3
        def next = {
            val value = index match {
                case 0 ⇒ e1
                case 1 ⇒ e2
                case 2 ⇒ e3
                case _ ⇒ throw new NoSuchElementException
            }
            index += 1
            value
        }
    }

    override def equals(other: Any): Boolean = {
        other match {
            case that: UIDSet3[T] ⇒
                this.e1 === that.e1 && this.e2 === that.e2 && this.e3 === that.e3
            case _ ⇒
                false
        }
    }

    override def hashCode: Int = (e1.id * 41 + e2.id) * 41 + e3.id

}

/**
 * A set of four elements with different unique ids.
 */
final class UIDSet4[T <: UID] private[collection] (
        final val e1: T,
        final val e2: T,
        final val e3: T,
        final val e4: T
) extends NonEmptyUIDSet[T] { thisSet ⇒

    override def isSingletonSet: Boolean = false

    override def size = 4

    override def +[X >: T <: UID](o: X): UIDSet[X] = {
        val oId = o.id
        val e1 = this.e1
        val e2 = this.e2
        val e3 = this.e3
        val e4 = this.e4
        val e2Id = e2.id
        if (oId < e2Id) {
            val e1Id = e1.id
            if (oId < e1Id) {
                new UIDSetNode(new UIDSet1(o), oId, this)
            } else if (oId == e1Id) {
                this
            } else /* e1id < oid < e2id */ {
                new UIDSetNode(new UIDSet2(e1, o), oId, new UIDSet3(e2, e3, e4))
            }
        } else if (oId == e2Id) {
            this
        } else /* oId > e2Id */ {
            val e3Id = e3.id
            if (oId < e3Id) {
                new UIDSetNode(new UIDSet3(e1, e2, o), oId, new UIDSet2(e3, e4))
            } else if (oId == e3Id) {
                this
            } else {
                val e4Id = e4.id
                if (oId < e4Id) {
                    new UIDSetNode(new UIDSet4(e1, e2, e3, o), oId, new UIDSet1(e4))
                } else if (oId == e4Id) {
                    this
                } else {
                    new UIDSetNode(this, e4Id, new UIDSet1(o))
                }
            }
        }
    }

    override def head = e1

    override def last: T = e4

    override def tail = new UIDSet3(e2, e3, e4)

    override def foreach[U](f: T ⇒ U): Unit = { f(e1); f(e2); f(e3); f(e4) }

    override def filter(f: T ⇒ Boolean): UIDSet[T] = {
        foldLeft(UIDSet0: UIDSet[T])((c, n) ⇒ if (f(n)) c + n else c)
    }

    override def reduce[X >: T](op: (X, X) ⇒ X): X = op(op(op(e1, e2), e3), e4)

    override def forall(f: T ⇒ Boolean): Boolean = f(e1) && f(e2) && f(e3) && f(e4)

    override def exists(f: T ⇒ Boolean): Boolean = f(e1) || f(e2) || f(e3) || f(e4)

    override def foldLeft[B](b: B)(op: (B, T) ⇒ B): B = op(op(op(op(b, e1), e2), e3), e4)

    override def contains[X <: UID](o: X): Boolean = {
        val oId = o.id
        e1.id == oId || e2.id == oId || e3.id == oId || e4.id == oId
    }

    override def find(f: T ⇒ Boolean): Option[T] = {
        if (f(e1))
            Some(e1)
        else if (f(e2))
            Some(e2)
        else if (f(e3))
            Some(e3)
        else if (f(e4))
            Some(e4)
        else
            None
    }

    override def toSeq: List[T] = e1 :: e2 :: e3 :: e4 :: Nil

    override def toIterator = new Iterator[T] {
        var index = 0
        def hasNext = index < 4
        def next = {
            val value = index match {
                case 0 ⇒ e1
                case 1 ⇒ e2
                case 2 ⇒ e3
                case 3 ⇒ e4
                case _ ⇒ throw new NoSuchElementException
            }
            index += 1
            value
        }
    }

    override def equals(other: Any): Boolean = {
        other match {
            case that: UIDSet4[T] ⇒
                e1 === that.e1 && e2 === that.e2 && e3 === that.e3 && e4 === that.e4
            case _ ⇒
                false
        }
    }

    override def hashCode: Int = ((e1.id * 41 + e2.id) * 41 + e3.id) * 41 + e4.id
}

private final class UIDSetNode[T <: UID](
        val left:  UIDSet[T],
        val pivot: Int, // the id of the largest value of the left set
        val right: UIDSet[T]
) extends NonEmptyUIDSet[T] {

    assert(right.nonEmpty)

    override def size = left.size + right.size

    override def isSingletonSet = false

    override def head: T = left.head

    override def last: T = right.last

    override def exists(p: T ⇒ Boolean): Boolean = left.exists(p) || right.exists(p)

    override def forall(p: T ⇒ Boolean) = left.forall(p) && right.forall(p)

    override def contains[X <: UID](o: X): Boolean = left.contains(o) || right.contains(o)

    override def find(p: T ⇒ Boolean): Option[T] = {
        left.find(p) match {
            case r @ Some(_) ⇒ r
            case None        ⇒ right.find(p)
        }
    }

    override def foreach[U](f: T ⇒ U): Unit = { left.foreach(f); right.foreach(f) }

    override def foldLeft[B](b: B)(op: (B, T) ⇒ B): B = {
        right.foldLeft(left.foldLeft(b)(op))(op)
    }

    override def +[X >: T <: UID](e: X): UIDSet[X] = {
        val eId = e.id
        if (eId < pivot) {
            val newLeft = left + e
            if (newLeft eq left)
                this
            else
                new UIDSetNode[X](newLeft, pivot, right)
        } else if (eId > pivot) {
            val newRight = right + e
            if (newRight eq right)
                this
            else
                new UIDSetNode[X](left, pivot, newRight)
        } else {
            this
        }
    }

    override def tail: UIDSet[T] = {
        val leftTail = left.tail
        if (leftTail.isEmpty)
            right
        else
            new UIDSetNode(leftTail, pivot, right)
    }

    override def filter(p: T ⇒ Boolean): UIDSet[T] = {
        val leftFiltered = left.filter(p)
        val rightFiltered = right.filter(p)
        if (leftFiltered.isEmpty)
            rightFiltered
        else if (rightFiltered.isEmpty)
            leftFiltered
        else if (leftFiltered.size + rightFiltered.size <= 4)
            // UIDSets which contain less than 5 elements have to use the
            // canonical representation to ensure that the equality 
            // tests succeed!
            leftFiltered ++ rightFiltered
        else
            new UIDSetNode(leftFiltered, leftFiltered.last.id, rightFiltered)
    }

    override def toIterator: Iterator[T] = left.toIterator ++ right.toIterator

    override def equals(other: Any): Boolean = {
        other match {
            case that: UIDSetNode[T] ⇒
                val thisIt = this.toIterator
                val thatIt = that.toIterator
                while (thisIt.hasNext && thatIt.hasNext && thisIt.next == thatIt.next) { ; }
                !thisIt.hasNext && !thatIt.hasNext
            case _ ⇒
                false
        }
    }

    override def hashCode: Int = left.hashCode * 13 + right.hashCode

}

/**
 * Factory methods to create `UIDSet`s.
 *
 * @author Michael Eichberg
 */
object UIDSet {

    class UIDSetBuilder[T <: UID] extends Builder[T, UIDSet[T]] {
        private var uidSet: UIDSet[T] = UIDSet0
        def +=(elem: T): this.type = {
            uidSet += elem
            this
        }
        def clear(): Unit = uidSet = UIDSet0
        def result(): UIDSet[T] = uidSet
    }

    implicit def canBuildFrom[A <: UID]: CanBuildFrom[UIDSet[_], A, UIDSet[A]] = {
        new CanBuildFrom[UIDSet[_], A, UIDSet[A]] {
            def apply(from: UIDSet[_]) = new UIDSetBuilder[A]
            def apply() = new UIDSetBuilder[A]
        }
    }

    implicit def canBuildSetFromUIDSet[A]: CanBuildFrom[UIDSet[_], A, Set[A]] = {
        new CanBuildFrom[UIDSet[_], A, Set[A]] {
            def apply(from: UIDSet[_]) = Set.newBuilder[A]
            def apply() = Set.newBuilder[A]
        }
    }

    implicit def canBuildChainedListFromUIDSet[A]: CanBuildFrom[UIDSet[_], Int, ChainedList[Int]] = {
        new CanBuildFrom[UIDSet[_], Int, ChainedList[Int]] {
            def apply(from: UIDSet[_]) = new ChainedListBuilder[Int]
            def apply() = new ChainedListBuilder[Int]
        }
    }

    def canBuildUIDSetFromTraversableOnce[T <: UID]: CanBuildFrom[TraversableOnce[T], T, UIDSet[T]] = {
        new CanBuildFrom[TraversableOnce[T], T, UIDSet[T]] {
            def apply(from: TraversableOnce[T]) = new UIDSetBuilder[T]
            def apply() = new UIDSetBuilder[T]
        }
    }

    def newBuilder[T <: UID]: UIDSetBuilder[T] = new UIDSetBuilder[T]

    /**
     * Returns an empty [[UIDSet]].
     */
    def empty[T <: UID]: UIDSet[T] = UIDSet0

    /**
     * Creates a new [[UIDSet]] with the given element.
     *
     * @param e The non-null value of the created set.
     */
    def apply[T <: UID](e: T): UIDSet[T] = new UIDSet1(e)

    /**
     * Creates a new [[UIDSet]] with the given elements. The resulting list
     *  has one element – if both elements have the same unique id – otherwise
     *  it has two elements.
     *
     * @param e1 A non-null value of the created list.
     * @param e2 A non-null value of the created list (if it is not a duplicate).
     */
    def apply[T <: UID](e1: T, e2: T): UIDSet[T] = {
        val e1Id = e1.id
        val e2Id = e2.id
        if (e1Id < e2Id)
            new UIDSet2(e1, e2)
        else if (e1Id == e2Id)
            new UIDSet1(e1)
        else
            new UIDSet2(e2, e1)
    }

    /**
     * Creates a new [[UIDSet]] based on the given collection.
     *
     * @note Even if the given collection is already a `Set` it may be possible
     *      that the resulting [[UIDSet]] has less elements. This will happen
     *      if the `equals`/`hashCode` methods of the elements of the set are not
     *      based on the element's unique id and the given set contains two
     *      or more elements that share the same id.
     */
    def apply[T <: UID](e1: T, e2: T, elems: T*): UIDSet[T] = {
        elems.foldLeft[UIDSet[T]](UIDSet(e1, e2))((s, e) ⇒ s + e)
    }

}
