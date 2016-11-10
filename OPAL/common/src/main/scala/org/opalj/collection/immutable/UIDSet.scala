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

import scala.collection.GenTraversableOnce
import scala.collection.generic.CanBuildFrom
import scala.collection.generic.FilterMonadic
import scala.collection.mutable.Builder

import org.opalj.collection.UID.areEqual
import org.opalj.collection.immutable.Chain.ChainBuilder

/**
 * An immutable set of elements of type `UID`. Underlying the implementation
 * is a modified binary search tree. The primary use case of this structure is its
 * use for small(er) sets or sets where the order of additions of elements is
 * very unlikely to be accidentially ordered.
 *
 * Contains checks etc. are based on the element's unique id.
 *
 * [[UIDSet$]]s are constructed using the factory methods of the companion object and
 * are generally expected to be small.
 *
 * @author Michael Eichberg
 */
/*
NOTE: SETS OF SIZE [0..4] are always represented using one of the following
canonical representations.
*/
sealed trait UIDSet[+T <: UID]
        extends TraversableOnce[T]
        with FilterMonadic[T, UIDSet[T]]
        with Serializable { self ⇒

    /**
     * Represents a filtered [[UIDSet]]. Instances of [[UIDSetWithFilter]] are typically
     * created by [[UIDSet]]'s `withFilter` method.
     */
    class UIDSetWithFilter(p: T ⇒ Boolean) extends FilterMonadic[T, UIDSet[T]] {

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

        def withFilter(q: T ⇒ Boolean): UIDSetWithFilter = {
            new UIDSetWithFilter(x ⇒ p(x) && q(x))
        }
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
     * Returns `true` if an element with the same id as the given element is already
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
        } else if (thisSize == thatSize) {
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

    def -[X >: T <: UID](t: X): UIDSet[T] = filter(_.id != t.id)

    def --[X >: T <: UID](ts: UIDSet[X]): UIDSet[T] = filter(!ts.contains(_))

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

    def withFilter(p: T ⇒ Boolean): UIDSetWithFilter = new UIDSetWithFilter(p)

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
            do {
                while (thisNextId < thatNextId && thisIt.hasNext) {
                    thisNext = thisIt.next
                    thisNextId = thisNext.id
                }
                while (thatNextId < thisNextId && thatIt.hasNext) {
                    thatNext = thatIt.next
                    thatNextId = thatNext.id
                }
            } while (thisNextId != thatNextId &&
                (thisIt.hasNext && thisNextId < thatNextId) ||
                (thatIt.hasNext && thatNextId < thisNextId))
            if (thisNextId == thatNextId) {
                result += thisNext
            }
        }
        result
    }

    //
    // 5: TRANSFORM
    //
    final def seq: this.type = this

    /**
     * Creates a new `collection` which contains the mapped values as specified by the given
     * function `f`.
     *
     * @example
     *      This class predefines two implicit functions create either UIDSets or regular sets.
     *      If the compiler is not able to select the correct one, it is usually sufficient
     *      to explicitly specify the target type.
     * {{{
     * scala> case class MyUID(val id : Int) extends org.opalj.collection.UID
     * scala> var us = UIDSet(MyUID(1),MyUID(2)) + MyUID(-3)
     * scala> us.map(e => MyUID(e.id+1))
     *        <console>:21: error: ambiguous implicit values:
     *        both method canBuildFrom in object UIDSet of type ...
     *        and method canBuildSetFromUIDSet in object UIDSet of type ...
     *        match expected type ...
     *        us.map(e => MyUID(e.id+1))
     *        ^
     * scala> // this problem is easily fixed by; e.g., specifying the type of the target
     * scala> // collection (e.g. by assigning the list to an appropriately typed variable)
     * scala> us = us.map(e => MyUID(e.id+1))
     * us: org.opalj.collection.immutable.UIDSet[MyUID] = UIDSet(MyUID(-2), MyUID(2), MyUID(3))
     * scala> val s : Set[MyUID] = us.map(e => MyUID(e.id+1))
     * s: scala.collection.immutable.Set[MyUID] = Set(MyUID(-2), MyUID(2), MyUID(3))
     * }}}
     */
    def map[B, That](f: (T) ⇒ B)(implicit bf: CanBuildFrom[UIDSet[T], B, That]): That = {
        val b = bf(this)
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

    override def size: Int = 0

    override def isEmpty: Boolean = true

    override def nonEmpty: Boolean = false

    override def isSingletonSet: Boolean = false

    override def head: Nothing = throw new NoSuchElementException("the set is empty")

    override def last: Nothing = throw new NoSuchElementException("the set is empty")

    override def exists(p: Nothing ⇒ Boolean): Boolean = false

    override def forall(p: Nothing ⇒ Boolean): Boolean = true

    override def contains[X <: UID](o: X): Boolean = false

    override def find(p: Nothing ⇒ Boolean): None.type = None

    override def foreach[U](f: Nothing ⇒ U): Unit = {}

    override def foldLeft[B](b: B)(op: (B, Nothing) ⇒ B): B = b

    override def +[X <: UID](e: X): UIDSet[X] = new UIDSet1(e)

    override def tail: UIDSet[Nothing] = throw new NoSuchElementException("the set is empty")

    override def filter(p: Nothing ⇒ Boolean): this.type = this

    override def toIterator: Iterator[Nothing] = Iterator.empty

    override def ++[X >: Nothing <: UID](es: TraversableOnce[X]): UIDSet[X] = {
        es match {
            case s: UIDSet[X] ⇒ s
            case _            ⇒ super.++[X](es)
        }
    }

    override def intersect[X >: Nothing <: UID](that: UIDSet[X]): UIDSet[X] = this

    override def compare[X >: Nothing <: UID](that: UIDSet[X]): SetRelation = {
        if (that eq this)
            EqualSets
        else
            StrictSubset
    }

    override def equals(other: Any): Boolean = other.isInstanceOf[UIDSet0.type]

    override def hashCode: Int = 34237

    def unapply(s: UIDSet[_]): Boolean = /*s.isEmpty*/ s eq this
}

private[collection] trait NonEmptyUIDSet[+T <: UID] extends UIDSet[T] {
    final override def isEmpty: Boolean = false
    final override def nonEmpty: Boolean = true

    /** This nodes value. */
    private[collection] val e: T

    private[collection] def left: NonEmptyUIDSet[T]
    private[collection] def right: NonEmptyUIDSet[T]

    /** Creates a internally mutable shallow copy. */
    private[collection] def copy[X >: T <: UID](
        left:  NonEmptyUIDSet[X] = this.left,
        e:     X                 = this.e,
        right: NonEmptyUIDSet[X] = this.right,
        size:  Int               = this.size
    ): UIDSetNode[X] = {
        new UIDSetNode[X](left, e, right, size)
    }

}

/**
 * A [[UIDSet]] that contains a single element.
 *
 * @author Michael Eichberg
 */
final class UIDSet1[T <: UID]( final val e: T) extends NonEmptyUIDSet[T] { thisSet ⇒

    private[collection] def left: NonEmptyUIDSet[T] = null
    private[collection] def right: NonEmptyUIDSet[T] = null

    override def size: Int = 1

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
            new UIDSetNode(null, o, this, 2)
        else
            new UIDSetNode(this, o, null, 2)
    }

    override def head: T = e

    override def last: T = e

    override def tail: UIDSet[T] = UIDSet0

    override def foreach[U](f: T ⇒ U): Unit = f(e)

    override def forall(f: T ⇒ Boolean): Boolean = f(e)

    override def exists(f: T ⇒ Boolean): Boolean = f(e)

    override def contains[X <: UID](o: X): Boolean = UID.areEqual(e, o)

    override def find(f: T ⇒ Boolean): Option[T] = {
        val e = this.e
        if (f(e)) Some(e) else None
    }

    override def filter(f: T ⇒ Boolean): UIDSet[T] = if (f(e)) this else UIDSet0

    override def foldLeft[B](b: B)(op: (B, T) ⇒ B): B = op(b, e)

    override def reduce[X >: T](op: (X, X) ⇒ X): X = e

    override def toSeq: List[T] = e :: Nil

    override def toIterator: Iterator[T] = Iterator.single(e)

    override def ++[X >: T <: UID](es: TraversableOnce[X]): UIDSet[X] = {
        es match {
            case s: UIDSet[X] ⇒ if (s.isEmpty) this else s + this.head
            case _            ⇒ super.++(es)
        }
    }

    override def intersect[X >: T <: UID](that: UIDSet[X]): UIDSet[X] = {
        if (that.contains(this.head)) this else UIDSet0
    }

    override def compare[X >: T <: UID](that: UIDSet[X]): SetRelation = {
        that.size match {
            case 0 ⇒ StrictSuperset
            case 1 ⇒ if (that.head.id == this.head.id) EqualSets else UncomparableSets
            case _ ⇒ if (that.contains(this.head)) StrictSubset else UncomparableSets
        }
    }

    override def equals(other: Any): Boolean = {
        other match {
            case that: UIDSet[T] ⇒ that.isSingletonSet && areEqual(this.e, that.head)
            case _               ⇒ false
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

private[collection] final class UIDSetNode[T <: UID] private[collection] (
        var left:          NonEmptyUIDSet[T], // can be null
        val e:             T, // left.last.id < pivot.id < right.first.id
        var right:         NonEmptyUIDSet[T], // can be null
        override val size: Int
) extends NonEmptyUIDSet[T] { thisSet ⇒

    /**
     * Traverses all nodes in tree order until the given function fails.
     *
     * @param cont A function that is called to determine if the traversal of the tree
     *              should be continued.
     * @return `true` if all nodes were processed.
     */
    private[collection] def traverse(cont: T ⇒ Boolean): Boolean = {
        var nodes = Chain[NonEmptyUIDSet[T]](this)
        while (nodes.nonEmpty) {
            var currentNode = nodes.head
            nodes = nodes.tail
            do {
                if (!cont(currentNode.e))
                    return false;

                val left = currentNode.left
                val right = currentNode.right
                if (left ne null) {
                    if (right ne null)
                        nodes :&:= right
                    currentNode = left
                } else {
                    currentNode = right
                }
            } while (currentNode ne null)
        }
        true
    }

    override def isSingletonSet = size == 1

    override def head: T = {
        var currentNode: NonEmptyUIDSet[T] = this
        while (currentNode.left ne null) currentNode = currentNode.left
        currentNode.e
    }

    override def last: T = {
        var currentNode: NonEmptyUIDSet[T] = this
        while (currentNode.right ne null) currentNode = currentNode.right
        currentNode.e
    }

    override def exists(p: T ⇒ Boolean): Boolean = !traverse(t ⇒ !p(t))

    override def forall(p: T ⇒ Boolean) = traverse(p)

    override def contains[X <: UID](o: X): Boolean = {
        val oid = o.id
        !traverse(t ⇒ t.id != oid)
    }

    override def find(p: T ⇒ Boolean): Option[T] = {
        var result: Option[T] = None
        traverse { t ⇒ if (p(t)) { result = Some(t); false } else true }
        result
    }

    override def foreach[U](f: T ⇒ U): Unit = toIterator foreach { t ⇒ f(t); true }

    override def +[X >: T <: UID](o: X): UIDSet[X] = {
        // The idea is to find the way to the node to which we need to attach the value
        // update the internal nodes on the way down.
        val oId = o.id
        val newRoot = this.copy[X](size = size + 1)

        var lastNew = newRoot
        var current: NonEmptyUIDSet[T] = this
        var result: UIDSet[X] = null
        do {
            val eId = current.e.id
            if (oId < eId) {
                val nextLeft = current.left
                if (nextLeft eq null) {
                    lastNew.left = new UIDSet1(o)
                    result = newRoot
                } else {
                    current = nextLeft
                    val newNode = current.copy[X](size = current.size + 1)
                    lastNew.left = newNode
                    lastNew = newNode
                }
            } else if (oId > eId) {
                val nextRight = current.right
                if (nextRight eq null) {
                    lastNew.right = new UIDSet1(o)
                    result = newRoot
                } else {
                    current = nextRight
                    val newNode = current.copy[X](size = current.size + 1)
                    lastNew.right = newNode
                    lastNew = newNode
                }
            } else {
                result = this; // the node was/is already in the set...
            }
        } while (result eq null)
        result
    }

    override def toIterator: Iterator[T] = {
        // conceptually: (left.toIterator + e) ++ right.toIterator
        new Iterator[T] {
            var nodesToProcess: Chain[NonEmptyUIDSet[T]] = Naught

            private[this] def buildStack(n: NonEmptyUIDSet[T]) = {
                nodesToProcess :&:= n
                var left = n.left
                while (left ne null) {
                    nodesToProcess :&:= left
                    left = left.left
                }
            }

            buildStack(thisSet)

            def hasNext: Boolean = nodesToProcess.nonEmpty

            def next: T = {
                val current = nodesToProcess.head
                nodesToProcess = nodesToProcess.tail
                val right = current.right
                if (right ne null) buildStack(right)
                current.e
            }
        }
    }

    override def map[B, That](f: (T) ⇒ B)(implicit bf: CanBuildFrom[UIDSet[T], B, That]): That = {
        val b = bf(this)
        traverse { e ⇒ b += f(e); true }
        b.result
    }

    override def foldLeft[B](b: B)(op: (B, T) ⇒ B): B = toIterator.foldLeft(b)(op)

    override def tail: UIDSet[T] = {
        if (this.left eq null) {
            if (this.right eq null)
                return UIDSet0;
            else
                return right;
        }

        val newRoot = this.copy[T](size = size - 1)
        var lastNew = newRoot
        var current = this.left
        while (current.left ne null) {
            val newNode = current.copy(size = current.size - 1)
            lastNew.left = newNode
            lastNew = newNode
            current = current.left
        }
        if (current.right ne null)
            lastNew.left = current.right
        else
            lastNew.left = null
        newRoot
    }

    override def filter(p: T ⇒ Boolean): UIDSet[T] = {
        // IMPROVE Try to reuse the existing structure to enable sharing.
        var newRoot: UIDSet[T] = UIDSet0
        // we have to use traverse to avoid that we add the values in ascending order,
        // this would lead to a heavily unbalanced tree
        traverse { e ⇒ if (p(e)) newRoot += e; true }
        if (newRoot.size == this.size)
            this // reuse this instance to potentially save memory...
        else
            newRoot
    }

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

    override def hashCode: Int = {
        (if (left ne null) left.hashCode else 0) * 13 +
            (if (right ne null) right.hashCode else 0)
    }

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

    implicit def canBuildChainFromUIDSet[A]: CanBuildFrom[UIDSet[_], Int, Chain[Int]] = {
        new CanBuildFrom[UIDSet[_], Int, Chain[Int]] {
            def apply(from: UIDSet[_]) = new ChainBuilder[Int]
            def apply() = new ChainBuilder[Int]
        }
    }

    def canBuildUIDSet[T <: UID]: CanBuildFrom[AnyRef, T, UIDSet[T]] = {
        new CanBuildFrom[AnyRef, T, UIDSet[T]] {
            def apply(from: AnyRef) = new UIDSetBuilder[T]
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
        new UIDSet1(e1) + e2
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
        new UIDSet1(e1) + e2 ++ elems
    }

}
