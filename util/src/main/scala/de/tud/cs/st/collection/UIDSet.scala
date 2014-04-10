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
package de.tud.cs.st
package collection

import scala.annotation.tailrec

/**
 * A sorted set of elements of type `UID`. The decision whether the element is already
 * contained in this set is solely based on the element's unique id.
 *
 * [[UIDSet$]]s are constructed using the factory methods of the companion object.
 *
 * @author Michael Eichberg
 */
sealed trait UIDSet[+T <: UID] { thisSet ⇒

    /**
     * Number of elements of this set.
     *
     * In general, this operation takes O(1) steps.
     */
    /* ABSTRACT */ def size: Int

    /**
     * Returns `true` if this list is empty, `false` otherwise.
     */
    /* ABSTRACT */ def isEmpty: Boolean

    /**
     * Returns `false` if this list is empty, `true` otherwise.
     */
    def nonEmpty: Boolean = !isEmpty

    /**
     * Tests if this set contains exactly one element. This test has guaranteed complexity
     * O(1).
     */
    def containsOneElement: Boolean = false

    /**
     * Adds the given element, if the element is not already stored in
     * this set. Otherwise, a new set is created and the element is added to it.
     * If the element is already in this set, `this` set is returned.
     */
    /* ABSTRACT */ def +[X >: T <: UID](e: X): UIDSet[X]

    /**
     * Returns the first element of this set. I.e., the element with the smalles
     * unique id.
     */
    @throws[NoSuchElementException]("If the set is empty.") /* ABSTRACT */
    def first(): T

    /**
     * Returns the remaining elements of this set. This operation has linear complexity.
     */
    @throws[NoSuchElementException]("If the set is empty.") /* ABSTRACT */
    def tail(): UIDSet[T]

    /**
     * Passes all elements of this list to the given function.
     */
    /* ABSTRACT */ def foreach(f: T ⇒ Unit): Unit

    /**
     * Returns `true` if all elements satisfy the given predicate, `false` otherwise.
     */
    def forall[X >: T](f: X ⇒ Boolean): Boolean = {
        foreach { e ⇒ if (!f(e)) return false }
        true
    }

    /**
     * Returns `true` if an element satisfies the given predicate, `false` otherwise.
     */
    def exists[X >: T](f: X ⇒ Boolean): Boolean = {
        foreach { e ⇒ if (f(e)) return true }
        false
    }

    /**
     * Returns `true` if the given element is already in this list, `false` otherwise.
     */
    def contains[X >: T <: UID](o: X): Boolean = {
        foreach { e ⇒ if (e.id == o.id) return true }
        false
    }

    /**
     * Returns the first element that satisfies the given predicate.
     */
    def find[X >: T](f: X ⇒ Boolean): Option[T] = {
        foreach { e ⇒ if (f(e)) return Some(e) }
        None
    }

    /**
     * Creates a new list which contains the mapped values as specified by the given
     * function `f`.
     */
    def map[X](f: T ⇒ X): Set[X] = {
        var newSet = Set.empty[X]
        foreach { e ⇒ newSet += f(e) }
        newSet
    }

    /**
     * Returns a new `UIDSet` that contains all elements which satisfy the given
     * predicate.
     */
    def filter[X >: T](f: X ⇒ Boolean): UIDSet[T] = {
        var newSet: UIDSet[T] = UIDSet0
        foreach { e ⇒ if (f(e)) newSet += e }
        newSet
    }

    /**
     * Returns a new `UIDList` that contains all elements which '''do not satisfy'''
     * the given predicate.
     */
    def filterNot[X >: T](f: X ⇒ Boolean): UIDSet[T] = {
        var newSet: UIDSet[T] = UIDSet0
        foreach { e ⇒ if (!f(e)) newSet += e }
        newSet
    }

    /**
     * Performs a fold left over all elements of this set.
     */
    def foldLeft[B](b: B)(op: (B, T) ⇒ B): B = {
        var result: B = b
        foreach { elem ⇒ result = op(result, elem) }
        result
    }

    def reduce[X >: T](op: (X, X) ⇒ X): X

    /**
     * Converts this set into a sequence. The elements are sorted in ascending order
     * using the unique ids of the elements.
     */
    def toSeq: Seq[T] = {
        var seq = List.empty[T]
        foreach { e ⇒ seq = e :: seq }
        seq
    }

    /*ABSTRACT*/ override def equals(other: Any): Boolean

    override def hashCode: Int = {
        var hc = 1
        foreach { e ⇒ hc * 41 + e.hashCode }
        hc
    }

    def mkString(start: String, sep: String, end: String): String = {
        var s = ""
        var pre = start
        for { e ← this } {
            s = s + pre + e.id+":"+e.toString()
            pre = sep
        }
        if (s == "")
            start + end
        else
            s + end
    }

    override def toString: String = mkString("UIDSet(", ",", ")")

}

/**
 * Representation of the empty set of `UID` elements. To get a "correctly typed
 * [[UIDset]]" use the factory method of [[UIDSet$]].
 *
 * @author Michael Eichberg
 */
object UIDSet0 extends UIDSet[Nothing] {

    override def size = 0

    override def isEmpty = true

    override def nonEmpty = false

    override def +[X <: UID](e: X): UIDSet[X] = UIDSet(e)

    override def first: Nothing = throw new NoSuchElementException

    override def tail: UIDSet[Nothing] = throw new NoSuchElementException

    override def foreach(f: Nothing ⇒ Unit): Unit = {}

    override def exists[X >: Nothing](f: X ⇒ Boolean): Boolean = false

    override def forall[X >: Nothing](f: X ⇒ Boolean) = true

    override def filter[X >: Nothing](f: X ⇒ Boolean): this.type = this

    override def filterNot[X >: Nothing](f: X ⇒ Boolean): this.type = this

    override def foldLeft[B](b: B)(op: (B, Nothing) ⇒ B): B = b

    override def reduce[X >: Nothing](op: (X, X) ⇒ X): X = throw new UnsupportedOperationException

    override def toSeq = Seq.empty[Nothing]

    override def equals(other: Any): Boolean =
        other.isInstanceOf[UIDSet[_]] && (other.asInstanceOf[UIDSet[_]] eq UIDSet0)

    override def hashCode: Int = 1

    def unapply(s: UIDSet[_]): Boolean = s.isEmpty
}

private[collection] trait NonEmptyUIDSet[+T <: UID] extends UIDSet[T] {

    final override def isEmpty = false

    final override def nonEmpty = true
}

/**
 * A [[UIDSet]] that contains a single element.
 */
final class UIDSet1[T <: UID](
    val e: T)
        extends NonEmptyUIDSet[T] { thisSet ⇒

    override final def size = 1

    override final def containsOneElement: Boolean = true

    override def +[X >: T <: UID](o: X): UIDSet[X] = UIDSet(e, o) // <= factory method

    override def first = e

    override def tail: UIDSet[T] = UIDSet0

    override def foreach(f: T ⇒ Unit): Unit = f(e)

    override def forall[X >: T](f: X ⇒ Boolean): Boolean = f(e)

    override def exists[X >: T](f: X ⇒ Boolean): Boolean = f(e)

    override def contains[X >: T <: UID](o: X): Boolean = e.id == o.id

    override def find[X >: T](f: X ⇒ Boolean): Option[T] = if (f(e)) Some(e) else None

    override def map[X](f: T ⇒ X): Set[X] = Set.empty + (f(e))

    override def filter[X >: T](f: X ⇒ Boolean): UIDSet[T] = if (f(e)) this else UIDSet0

    override def filterNot[X >: T](f: X ⇒ Boolean): UIDSet[T] = if (!f(e)) this else UIDSet0

    override def foldLeft[B](b: B)(op: (B, T) ⇒ B): B = op(b, e)

    override def reduce[X >: T](op: (X, X) ⇒ X): X = e

    override def toSeq = Seq(e)

    override def equals(other: Any): Boolean = {
        other match {
            case that: UIDSet1[T] ⇒
                this.e.id == that.e.id
            case _ ⇒
                false
        }
    }

    override def hashCode: Int = 41 + e.id

}
/**
 * Extractor methods for a single element set of UID elements.
 */
object UIDSet1 {

    def unapply[T <: UID](set: UIDSet[T]): Option[T] = {
        if (set != null && set.size == 1)
            Some(set.first())
        else
            None
    }
}

/**
 * A [[UIDSet]] that contains two elements that have different unique ids.
 * To create an instance use the [[UIDSet]] object.
 *
 * @author Michael Eichberg
 */
final class UIDSet2[T <: UID] private[collection] (
    val e1: T, val e2: T)
        extends NonEmptyUIDSet[T] { thisSet ⇒

    override def size = 2

    override def +[X >: T <: UID](o: X): UIDSet[X] = {
        val oId = o.id
        val e1Id = e1.id
        val e2Id = e2.id
        if (oId < e1Id)
            new UIDSet3(o, e1, e2)
        else if (oId == e1Id)
            this
        else if (oId < e2Id)
            new UIDSet3(e1, o, e2)
        else if (oId == e2Id)
            this
        else
            new UIDSet3(e1, e2, o)
    }

    override def first: T = e1

    override def tail: UIDSet[T] = new UIDSet1(e2)

    override def foreach(f: T ⇒ Unit): Unit = { f(e1); f(e2) }

    override def forall[X >: T](f: X ⇒ Boolean): Boolean = f(e1) && f(e2)

    override def exists[X >: T](f: X ⇒ Boolean): Boolean = f(e1) || f(e2)

    override def contains[X >: T <: UID](o: X): Boolean = {
        val oId = o.id
        e1.id == oId || e2.id == oId
    }

    override def find[X >: T](f: X ⇒ Boolean): Option[T] =
        if (f(e1))
            Some(e1)
        else if (f(e2))
            Some(e2)
        else
            None

    override def map[X](f: T ⇒ X): Set[X] = Set.empty + f(e1) + f(e2)

    override def filter[X >: T](f: X ⇒ Boolean): UIDSet[T] = {
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

    override def toSeq = Seq(e1, e2)

    override def equals(other: Any): Boolean = {
        other match {
            case that: UIDSet2[T] ⇒
                this.e1.id == that.e1.id &&
                    this.e2.id == that.e2.id
            case _ ⇒
                false
        }
    }

    override def hashCode: Int = (41 + e1.id) * 41 + e2.id

}
object UIDSet2 {
    def unapply[T <: UID](uidSet: UIDSet[T]): Option[UIDSet2[T]] = {
        if (uidSet.isInstanceOf[UIDSet2[T]])
            Some(uidSet.asInstanceOf[UIDSet2[T]])
        else
            None
    }
}
/**
 * A set of three elements with different unique ids.
 */
final class UIDSet3[T <: UID] private[collection] (
    val e1: T, val e2: T, val e3: T)
        extends NonEmptyUIDSet[T] { thisSet ⇒

    override def size = 3

    override def +[X >: T <: UID](o: X): UIDSet[X] = {
        val oId = o.id
        val e1Id = e1.id
        val e2Id = e2.id
        val e3Id = e3.id
        if (oId < e1Id)
            new UIDSet4(o, e1, e2, e3)
        else if (oId == e1Id)
            this
        else if (oId < e2Id)
            new UIDSet4(e1, o, e2, e3)
        else if (oId == e2Id)
            this
        else if (oId < e3Id)
            new UIDSet4(e1, e2, o, e3)
        else if (oId == e3Id)
            this
        else
            new UIDSet4(e1, e2, e3, o)
    }

    override def first = e1

    override def tail = new UIDSet2(e2, e3)

    override def foreach(f: T ⇒ Unit): Unit = { f(e1); f(e2); f(e3) }

    override def forall[X >: T](f: X ⇒ Boolean): Boolean = f(e1) && f(e2) && f(e3)

    override def exists[X >: T](f: X ⇒ Boolean): Boolean = f(e1) || f(e2) || f(e3)

    override def contains[X >: T <: UID](o: X): Boolean = {
        val oId = o.id
        e1.id == oId || e2.id == oId || e3.id == oId
    }

    override def find[X >: T](f: X ⇒ Boolean): Option[T] =
        if (f(e1))
            Some(e1)
        else if (f(e2))
            Some(e2)
        else if (f(e3))
            Some(e3)
        else
            None

    override def map[X](f: T ⇒ X): Set[X] = Set.empty + f(e1) + f(e2) + f(e3)

    override def reduce[X >: T](op: (X, X) ⇒ X): X = op(op(e1, e2), e3)

    override def toSeq = Seq(e1, e2, e3)

    override def equals(other: Any): Boolean = {
        other match {
            case that: UIDSet3[T] ⇒
                this.e1.id == that.e1.id &&
                    this.e2.id == that.e2.id &&
                    this.e3.id == that.e3.id
            case _ ⇒
                false
        }
    }

    override def hashCode: Int = ((41 + e1.id) * 41 + e2.id) * 41 + e3.id

}
/**
 * A set of three elements with different unique ids.
 */
final class UIDSet4[T <: UID] private[collection] (
    val e1: T, val e2: T, val e3: T, val e4: T)
        extends NonEmptyUIDSet[T] { thisSet ⇒

    override def size = 4

    override def +[X >: T <: UID](o: X): UIDSet[X] = {
        val oId = o.id
        val e1Id = e1.id
        val e2Id = e2.id
        val e3Id = e3.id
        val e4Id = e4.id
        if (oId < e1Id)
            new UIDArraySet(o, e1, e2, e3, e4)
        else if (oId == e1Id)
            this
        else if (oId < e2Id)
            new UIDArraySet(e1, o, e2, e3, e4)
        else if (oId == e2Id)
            this
        else if (oId < e3Id)
            new UIDArraySet(e1, e2, o, e3, e4)
        else if (oId == e3Id)
            this
        else if (oId < e4Id)
            new UIDArraySet(e1, e2, e3, o, e4)
        else if (oId == e4Id)
            this
        else
            new UIDArraySet(e1, e2, e3, e4, o)
    }

    override def first = e1

    override def tail = new UIDSet3(e2, e3, e4)

    override def foreach(f: T ⇒ Unit): Unit = { f(e1); f(e2); f(e3); f(e4) }

    override def forall[X >: T](f: X ⇒ Boolean): Boolean = f(e1) && f(e2) && f(e3) && f(e4)

    override def exists[X >: T](f: X ⇒ Boolean): Boolean = f(e1) || f(e2) || f(e3) || f(e4)

    override def contains[X >: T <: UID](o: X): Boolean = {
        val oId = o.id
        e1.id == oId || e2.id == oId || e3.id == oId || e4.id == oId
    }

    override def find[X >: T](f: X ⇒ Boolean): Option[T] =
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

    override def map[X](f: T ⇒ X): Set[X] = Set.empty + f(e1) + f(e2) + f(e3) + f(e4)

    override def reduce[X >: T](op: (X, X) ⇒ X): X = op(op(op(e1, e2), e3), e4)

    override def toSeq = Seq(e1, e2, e3, e4)

    override def equals(other: Any): Boolean = {
        other match {
            case that: UIDSet4[T] ⇒
                this.e1.id == that.e1.id &&
                    this.e2.id == that.e2.id &&
                    this.e3.id == that.e3.id &&
                    this.e4.id == that.e4.id
            case _ ⇒
                false
        }
    }

    override def hashCode: Int = (((41 + e1.id) * 41 + e2.id) * 41 + e3.id) * 41 + e4.id

}
private final class UIDArraySet[T <: UID](
    private val es: Array[UID])
        extends NonEmptyUIDSet[T] { thisSet ⇒

    def this(e1: T, e2: T, e3: T, e4: T, e5: T) {
        this(Array(e1, e2, e3, e4, e5))
    }
    override def size = es.size

    override def foreach(f: T ⇒ Unit): Unit = { es.foreach(e ⇒ f(e.asInstanceOf[T])) }

    override def first = es(0).asInstanceOf[T]

    override def tail = {
        if (es.size == 5)
            new UIDSet4(
                es(1).asInstanceOf[T],
                es(2).asInstanceOf[T],
                es(3).asInstanceOf[T],
                es(4).asInstanceOf[T])
        else {
            val newLength = es.length - 1
            val newEs = new Array[UID](newLength)
            System.arraycopy(es, 0, newEs, 1, newLength)
            new UIDArraySet(newEs)
        }
    }

    override def +[X >: T <: UID](o: X): UIDSet[X] = {
        val index = java.util.Arrays.binarySearch(es, o, UIDBasedOrdering)
        if (index >= 0)
            this
        else {
            val newOIndex = -index - 1
            val newEs = new Array[UID](es.length + 1)
            System.arraycopy(es, 0, newEs, 0, newOIndex)
            newEs(newOIndex) = o
            System.arraycopy(es, newOIndex, newEs, newOIndex + 1, es.length - newOIndex)
            new UIDArraySet(newEs)
        }
    }

    override def contains[X >: T <: UID](o: X): Boolean = {
        val index = java.util.Arrays.binarySearch(es, o, UIDBasedOrdering)
        index >= 0
    }

    override def reduce[X >: T](op: (X, X) ⇒ X): X = {
        var result: X = es(0).asInstanceOf[T]
        for (i ← 1 until es.length) {
            result = op(result, es(i).asInstanceOf[T])
        }
        result
    }

    override def equals(other: Any): Boolean = {
        other match {
            case that: UIDArraySet[T] ⇒
                this.es == that.es
            case _ ⇒
                false
        }
    }

    override def hashCode: Int = es.foldLeft(41)(_ * 41 + _.id)
}

/**
 * Factory methods to create `UIDSet`s.
 *
 * @author Michael Eichberg
 */
object UIDSet {

    /**
     * Returns an empty [[UIDSet]].
     */
    def empty[T <: UID]: UIDSet[T] = UIDSet0

    /**
     * Creates a new [[UIDSet]] with the given element.
     *
     * @param e The non-null value of the created set.
     */
    def apply[T <: UID](e: T): UIDSet[T] =
        new UIDSet1(e)

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
    def apply[T <: UID](set: scala.collection.TraversableOnce[T]): UIDSet[T] = {
        var list: UIDSet[T] = UIDSet0
        set foreach { list += _ }
        list
    }

    /**
     * Extractor method for [[UIDSet]]s. The sequence contain the elements in ascending
     * order.
     */
    def unapplySeq[T <: UID](set: UIDSet[T]): Option[Seq[T]] = {
        if (set == null || set.isEmpty)
            None
        else
            Some(set.toSeq)
    }
}
