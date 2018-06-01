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

import java.util.function.IntConsumer
import scala.collection.AbstractIterator

/**
 * An unordered set of integer values backed by a trie set. The branching is done using
 * the least significant bit and values are only stored in leaf nodes. This ensure that
 * we have a stable iteration order.
 *
 * @author Michael Eichberg
 */
sealed abstract class IntTrieSet
    extends IntSet[IntTrieSet]
    with IntCollectionWithStableOrdering[IntTrieSet]
    with IntWorkSet[IntTrieSet] { intSet ⇒

    /**
     * Returns each pairing of two values. I.e., if the set contains 1, 4, 8, the pairings
     * ((1,4) XOR (4,1)),((1,8) XOR (8,1)) and ((4,8) XOR (8,4)) will be returned; hence,
     * the order between the two values is not defined.
     */
    def foreachPair[U](f: (Int, Int) ⇒ U): Unit
    def intersect(other: IntTrieSet): IntTrieSet = {
        if (other.size <= 2)
            // we have specialized handling for small sets
            return other.intersect(this);

        val (smallerSet, largerSet) = if (other.size > this.size) (this, other) else (other, this)
        var r = smallerSet
        val it = smallerSet.intIterator
        while (it.hasNext) {
            val n = it.next
            if (!largerSet.contains(n)) {
                r -= n
            }
        }
        r
    }

    /** Returns some value and removes it from this set. */
    def getAndRemove: IntHeadAndRestOfSet[IntTrieSet]

    def filter(p: Int ⇒ Boolean): IntTrieSet
    override def withFilter(p: Int ⇒ Boolean): IntTrieSet

    final override def subsetOf(other: IntTrieSet): Boolean = subsetOf(other, 0)

    final override def toString: String = mkString("IntTrieSet(", ",", ")")

    /**
     * Tries to add the given method to this trie set by ''mutating the set if possible''.
     * Due to the internal organization, mutating the set is not always possible. In this case, a
     * new set containing the new value is returned. Hence, the return value ''must not'' be
     * ignored!
     */
    def +!(value: Int): IntTrieSet

    /**
     * @see [[+!]] for details!
     */
    final def ++!(that: IntTrieSet): IntTrieSet = {
        that.foldLeft(this)(_ +! _) // We have to expand `this`!
    }
    //
    // IMPLEMENTATION "INTERNAL" METHODS
    //

    private[immutable] def +(i: Int, level: Int): IntTrieSet
    private[immutable] def +!(i: Int, level: Int): IntTrieSet
    private[immutable] def -(i: Int, key: Int): IntTrieSet
    private[immutable] def contains(value: Int, key: Int): Boolean
    private[immutable] def subsetOf(other: IntTrieSet, level: Int): Boolean

    /** Ensures that this set is represented using its canonical representation. */
    private[immutable] def constringe(): IntTrieSet
}

final class FilteredIntTrieSet(
        private val s: IntTrieSet,
        private val p: Int ⇒ Boolean
) extends IntTrieSet {

    override def iterator: Iterator[Int] = s.iterator.withFilter(p)
    override def intIterator: IntIterator = s.intIterator.withFilter(p)

    override def foreach(f: IntConsumer): Unit = s.foreach { i ⇒ if (p(i)) f.accept(i) }
    override def map(f: Int ⇒ Int): IntTrieSet = {
        s.foldLeft(EmptyIntTrieSet: IntTrieSet) { (c, i) ⇒ if (p(i)) c + f(i) else c }
    }
    override def map(map: Array[Int]): IntTrieSet = {
        s.foldLeft(EmptyIntTrieSet: IntTrieSet) { (c, i) ⇒ if (p(i)) c + map(i) else c }
    }
    override def flatMap(f: Int ⇒ IntTrieSet): IntTrieSet = {
        s.flatMap(i ⇒ if (p(i)) f(i) else EmptyIntTrieSet)
    }
    override def withFilter(p: (Int) ⇒ Boolean): IntTrieSet = {
        new FilteredIntTrieSet(s, i ⇒ p(i) && this.p(i))
    }

    override def exists(p: Int ⇒ Boolean): Boolean = s.exists(i ⇒ this.p(i) && p(i))
    override def forall(f: Int ⇒ Boolean): Boolean = s.forall(i ⇒ !this.p(i) || f(i))
    override def contains(value: Int): Boolean = p(value) && s.contains(value)
    override def toChain: Chain[Int] = intIterator.toChain

    private[this] lazy val filtered: IntTrieSet = s.filter(p)

    override def intersect(other: IntTrieSet): IntTrieSet = filtered.intersect(other)
    override def foreachPair[U](f: (Int, Int) ⇒ U): Unit = filtered.foreachPair(f)
    override def filter(p: Int ⇒ Boolean): IntTrieSet = filtered.filter(p)
    override def isSingletonSet: Boolean = filtered.isSingletonSet
    override def hasMultipleElements: Boolean = filtered.hasMultipleElements
    override def isEmpty: Boolean = filtered.isEmpty
    override def size: Int = filtered.size
    override def head: Int = filtered.head
    override def getAndRemove: IntHeadAndRestOfSet[IntTrieSet] = filtered.getAndRemove
    override def -(i: Int): IntTrieSet = filtered - i
    override def +(i: Int): IntTrieSet = filtered + i
    override def +!(value: Int): IntTrieSet = filtered +! value
    override def foldLeft[B](z: B)(f: (B, Int) ⇒ B): B = filtered.foldLeft(z)(f)
    override def equals(other: Any): Boolean = filtered.equals(other)
    override def hashCode: Int = filtered.hashCode()

    // Actually the following methods should never be called... in a sense they are dead.
    private[immutable] override def constringe(): IntTrieSet = filtered.constringe()
    private[immutable] override def -(i: Int, key: Int): IntTrieSet = filtered - (i, key)
    private[immutable] override def +(i: Int, level: Int): IntTrieSet = filtered + (i, level)
    private[immutable] override def +!(i: Int, level: Int): IntTrieSet = filtered +! (i, level)
    private[immutable] override def contains(value: Int, key: Int): Boolean = {
        filtered.contains(value, key)
    }
    private[immutable] override def subsetOf(other: IntTrieSet, level: Int): Boolean = {
        filtered.subsetOf(other, level)
    }
}

/** The (potential) leaves of an IntTrie. */
private[immutable] sealed abstract class IntTrieSetL extends IntTrieSet {

    final override private[immutable] def -(i: Int, key: Int): IntTrieSet = this.-(i)
    final override private[immutable] def constringe(): IntTrieSet = this
    final override private[immutable] def contains(value: Int, key: Int): Boolean = {
        this.contains(value)
    }
}

case object EmptyIntTrieSet extends IntTrieSetL {
    override def isSingletonSet: Boolean = false
    override def hasMultipleElements: Boolean = false
    override def isEmpty: Boolean = true
    override def size: Int = 0
    override def head: Int = throw new UnsupportedOperationException("empty")
    override def getAndRemove: IntHeadAndRestOfSet[IntTrieSet] = {
        throw new UnsupportedOperationException("empty")
    }
    override def foreach(f: IntConsumer): Unit = {}
    override def foreachPair[U](f: (Int, Int) ⇒ U): Unit = {}
    override def filter(p: (Int) ⇒ Boolean): IntTrieSet = this
    override def withFilter(p: (Int) ⇒ Boolean): IntTrieSet = this
    override def map(f: Int ⇒ Int): IntTrieSet = this
    override def map(map: Array[Int]): IntTrieSet = this
    override def -(i: Int): this.type = this
    override def +(i: Int): IntTrieSet1 = IntTrieSet1(i)
    override def +!(i: Int): IntTrieSet = IntTrieSet1(i)
    override def intersect(other: IntTrieSet): IntTrieSet = this
    override def iterator: Iterator[Int] = Iterator.empty
    override def intIterator: IntIterator = IntIterator.empty
    override def contains(value: Int): Boolean = false
    override def exists(p: Int ⇒ Boolean): Boolean = false
    override def foldLeft[B](z: B)(f: (B, Int) ⇒ B): B = z
    override def forall(f: Int ⇒ Boolean): Boolean = true
    override def flatMap(f: Int ⇒ IntTrieSet): IntTrieSet = this
    override def toChain: Chain[Int] = Naught

    override def equals(other: Any): Boolean = {
        other match {
            case that: IntTrieSet ⇒ that.isEmpty
            case _                ⇒ false
        }
    }

    override def hashCode: Int = 0 // compatible to Arrays.hashCode

    private[immutable] override def +(i: Int, level: Int): IntTrieSet = this.+(i)
    private[immutable] override def +!(i: Int, level: Int): IntTrieSet = this.+!(i)
    private[immutable] override def subsetOf(other: IntTrieSet, level: Int): Boolean = true
}

final case class IntTrieSet1 private (i: Int) extends IntTrieSetL {
    override def isEmpty: Boolean = false
    override def isSingletonSet: Boolean = true
    override def hasMultipleElements: Boolean = false
    override def size: Int = 1
    override def foreach(f: java.util.function.IntConsumer): Unit = { f.accept(i) }
    override def foreachPair[U](f: (Int, Int) ⇒ U): Unit = {}
    override def getAndRemove: IntHeadAndRestOfSet[IntTrieSet] = {
        IntHeadAndRestOfSet(i, EmptyIntTrieSet: IntTrieSet)
    }
    override def filter(p: Int ⇒ Boolean): IntTrieSet = if (p(i)) this else EmptyIntTrieSet
    override def withFilter(p: Int ⇒ Boolean): IntTrieSet = new FilteredIntTrieSet(this, p)
    override def map(f: Int ⇒ Int): IntTrieSet = {
        val newI = f(i)
        if (newI != i)
            IntTrieSet1(newI)
        else
            this
    }
    override def map(map: Array[Int]): IntTrieSet = {
        val newI = map(i)
        if (newI != i)
            IntTrieSet1(newI)
        else
            this
    }
    override def flatMap(f: Int ⇒ IntTrieSet): IntTrieSet = f(i)
    override def head: Int = i
    override def -(i: Int): IntTrieSet = if (this.i != i) this else EmptyIntTrieSet
    override def +(i: Int): IntTrieSet = if (this.i == i) this else IntTrieSet.from(this.i, i)
    override def +!(i: Int): IntTrieSet = this + i
    override def iterator: Iterator[Int] = Iterator.single(i)
    override def intIterator: IntIterator = IntIterator(i)
    override def intersect(other: IntTrieSet): IntTrieSet = {
        if (other.contains(this.i)) this else EmptyIntTrieSet
    }
    override def contains(value: Int): Boolean = value == i
    override def exists(p: Int ⇒ Boolean): Boolean = p(i)
    override def foldLeft[B](z: B)(f: (B, Int) ⇒ B): B = f(z, i)
    override def forall(f: Int ⇒ Boolean): Boolean = f(i)
    override def toChain: Chain[Int] = new :&:[Int](i)

    override def equals(other: Any): Boolean = {
        other match {
            case that: IntTrieSet ⇒ that.isSingletonSet && this.i == that.head
            case _                ⇒ false
        }
    }

    override def hashCode: Int = 31 + i // compatible to Arrays.hashCode

    override private[immutable] def +(i: Int, level: Int): IntTrieSet = this.+(i)
    override private[immutable] def +!(i: Int, level: Int): IntTrieSet = this.+!(i)
    override private[immutable] def subsetOf(other: IntTrieSet, level: Int): Boolean = {
        other.contains(i, i >>> level)
    }
}

object IntTrieSet1 {

    // The preallocation of the IntTrieSet1 data structures costs ~2Mb memory;
    // however, we use it as the backbone Infrastructure for storing CFGs and
    // def-use information; in both cases, we generally require HUGE numbers
    // of such sets in the preconfigured ranges and therefore we avoid allocating
    // several hundred million instances (in case of a thorough analysis of the
    // JDK) and corresponding memory.
    val Cache1LowerBound = -100000 - (48 * 1024) // inclusive
    val Cache1UpperBound = -99999 // exclusive
    val Cache2LowerBound = -2048 // inclusive
    val Cache2UpperBound = 48 * 1024 // exclusive

    private[this] val cache1: Array[IntTrieSet1] = {
        val a = new Array[IntTrieSet1](Cache1UpperBound + (-Cache1LowerBound))
        var v = Cache1LowerBound
        var index = 0
        while (v < Cache1UpperBound) {
            a(index) = new IntTrieSet1(v)
            index += 1
            v += 1
        }
        a
    }

    private[this] val cache2: Array[IntTrieSet1] = {
        val a = new Array[IntTrieSet1](Cache2UpperBound + (-Cache2LowerBound))
        var v = Cache2LowerBound
        var index = 0
        while (v < Cache2UpperBound) {
            a(index) = new IntTrieSet1(v)
            index += 1
            v += 1
        }
        a
    }

    def apply(v: Int) = {
        if (v >= Cache1LowerBound && v < Cache1UpperBound) {
            cache1(v + (-Cache1LowerBound))
        } else if (v >= Cache2LowerBound && v < Cache2UpperBound) {
            cache2(v + (-Cache2LowerBound))
        } else {
            new IntTrieSet1(v)
        }
    }

}

/**
 * Represents an ordered set of two values where i1 has to be smaller than i2.
 */
private[immutable] final class IntTrieSet2 private[immutable] (
        val i1: Int, val i2: Int
) extends IntTrieSetL {

    override def isEmpty: Boolean = false
    override def isSingletonSet: Boolean = false
    override def hasMultipleElements: Boolean = true
    override def size: Int = 2
    override def head: Int = i2
    override def getAndRemove: IntHeadAndRestOfSet[IntTrieSet] = {
        IntHeadAndRestOfSet[IntTrieSet](i2, IntTrieSet1(i1))
    }

    override def iterator: Iterator[Int] = new AbstractIterator[Int] {
        private[this] var i = 0
        def hasNext: Boolean = i < 2
        def next: Int = if (i == 0) { i = 1; i1 } else { i = 2; i2 }
    }
    override def intIterator: IntIterator = IntIterator(i1, i2)

    override def foreach(f: IntConsumer): Unit = { f.accept(i1); f.accept(i2) }
    override def foreachPair[U](f: (Int, Int) ⇒ U): Unit = { f(i1, i2) }
    override def filter(p: (Int) ⇒ Boolean): IntTrieSet = {
        if (p(i1)) {
            if (p(i2))
                this
            else
                IntTrieSet1(i1)
        } else {
            if (p(i2))
                IntTrieSet1(i2)
            else
                EmptyIntTrieSet
        }
    }
    override def withFilter(p: (Int) ⇒ Boolean): IntTrieSet = new FilteredIntTrieSet(this, p)
    override def map(f: Int ⇒ Int): IntTrieSet = {
        val i1 = this.i1
        val newI1 = f(i1)
        val i2 = this.i2
        val newI2 = f(i2)
        if (newI1 != i1 || newI2 != i2)
            IntTrieSet(newI1, newI2)
        else
            this
    }
    override def map(map: Array[Int]): IntTrieSet = {
        val newI1 = map(i1)
        val newI2 = map(i2)
        if (newI1 == newI2)
            return IntTrieSet1(newI1);
        else if ((newI1 == i1 && newI2 == i2) || (newI1 == i2 && newI2 == i1))
            this
        else
            IntTrieSet.from(newI1, newI2)
    }
    override def flatMap(f: Int ⇒ IntTrieSet): IntTrieSet = f(i1) ++ f(i2)

    override def -(i: Int): IntTrieSet = {
        if (i == i1) IntTrieSet1(i2)
        else if (i == i2) IntTrieSet1(i1)
        else this
    }
    override def +(i: Int): IntTrieSet = if (i1 == i | i2 == i) this else IntTrieSet.from(i1, i2, i)
    override def +!(i: Int): IntTrieSet = this + i
    override def intersect(other: IntTrieSet): IntTrieSet = {
        other.size match {
            case 0 ⇒ other
            case 1 ⇒ if (other.head == i1 || other.head == i2) other else EmptyIntTrieSet
            case _ ⇒
                if (other.contains(this.i1)) {
                    if (other.contains(this.i2)) {
                        this
                    } else {
                        IntTrieSet1(this.i1)
                    }
                } else if (other.contains(this.i2)) {
                    IntTrieSet1(this.i2)
                } else {
                    EmptyIntTrieSet
                }
        }
    }
    override def contains(value: Int): Boolean = value == i1 || value == i2
    override def exists(p: Int ⇒ Boolean): Boolean = p(i1) || p(i2)
    override def foldLeft[B](z: B)(f: (B, Int) ⇒ B): B = f(f(z, i1), i2)
    override def forall(f: Int ⇒ Boolean): Boolean = f(i1) && f(i2)

    override def toChain: Chain[Int] = i1 :&: i2 :&: Naught

    override private[immutable] def subsetOf(other: IntTrieSet, level: Int): Boolean = {
        other.size match {
            case 0 | 1 ⇒
                false
            case 2 ⇒
                other match {
                    case that: IntTrieSet2 ⇒
                        that.i1 == this.i1 && that.i2 == this.i2
                    case _ ⇒
                        // ... this case should never occur...
                        other.contains(i1, i1 >>> level) && other.contains(i2, i2 >>> level)
                }
            case _ ⇒
                other.contains(i1, i1 >>> level) && other.contains(i2, i2 >>> level)
        }
    }

    override def equals(other: Any): Boolean = {
        other match {
            case that: IntTrieSet2 ⇒ this.i1 == that.i1 && this.i2 == that.i2
            case that: IntTrieSet  ⇒ that.size == 2 && that.contains(i1) && that.contains(i2)
            case _                 ⇒ false
        }
    }

    override def hashCode: Int = 31 * (31 + i1) + i2 // compatible to Arrays.hashCode

    override private[immutable] def +(i: Int, level: Int): IntTrieSet = this.+(i)
    override private[immutable] def +!(i: Int, level: Int): IntTrieSet = this.+!(i)
}

/**
 * Represents an ordered set of three int values: i1 < i2 < i3.
 */
private[immutable] final class IntTrieSet3 private[immutable] (
        val i1: Int, val i2: Int, val i3: Int
) extends IntTrieSetL {

    override def isEmpty: Boolean = false
    override def isSingletonSet: Boolean = false
    override def hasMultipleElements: Boolean = true
    override def size: Int = 3
    override def getAndRemove: IntHeadAndRestOfSet[IntTrieSet] = {
        IntHeadAndRestOfSet(i3, new IntTrieSet2(i1, i2))
    }
    override def head: Int = i1
    override def flatMap(f: Int ⇒ IntTrieSet): IntTrieSet = f(i1) ++ f(i2) ++ f(i3)
    override def iterator: Iterator[Int] = new AbstractIterator[Int] {
        var i = 0
        def hasNext: Boolean = i < 3
        def next: Int = {
            val v = i
            i += 1
            v match {
                case 0 ⇒ i1
                case 1 ⇒ i2
                case 2 ⇒ i3
                case _ ⇒ throw new IllegalStateException()
            }
        }
    }
    override def intIterator: IntIterator = IntIterator(i1, i2, i3)

    override def foreach(f: IntConsumer): Unit = { f.accept(i1); f.accept(i2); f.accept(i3) }
    override def foreachPair[U](f: (Int, Int) ⇒ U): Unit = { f(i1, i2); f(i1, i3); f(i2, i3) }

    override def filter(p: (Int) ⇒ Boolean): IntTrieSet = {
        if (p(i1)) {
            if (p(i2)) {
                if (p(i3))
                    this
                else
                    new IntTrieSet2(i1, i2)
            } else {
                if (p(i3))
                    new IntTrieSet2(i1, i3)
                else
                    IntTrieSet1(i1)
            }
        } else {
            if (p(i2)) {
                if (p(i3))
                    new IntTrieSet2(i2, i3)
                else
                    IntTrieSet1(i2)
            } else {
                if (p(i3))
                    IntTrieSet1(i3)
                else
                    EmptyIntTrieSet
            }
        }
    }
    override def withFilter(p: (Int) ⇒ Boolean): IntTrieSet = new FilteredIntTrieSet(this, p)
    override def map(f: Int ⇒ Int): IntTrieSet = {
        val i1 = this.i1
        val newI1 = f(i1)
        val i2 = this.i2
        val newI2 = f(i2)
        val i3 = this.i3
        val newI3 = f(i3)
        if (newI1 != i1 || newI2 != i2 || newI3 != i3)
            IntTrieSet(newI1, newI2, newI3) // ensures invariant
        else
            this
    }
    override def map(map: Array[Int]): IntTrieSet = {
        val newI1 = map(i1)
        val newI2 = map(i2)
        val newI3 = map(i3)
        IntTrieSet(newI1, newI2, newI3)
    }
    override def -(i: Int): IntTrieSet = {
        if (i1 == i) new IntTrieSet2(i2, i3)
        else if (i2 == i) new IntTrieSet2(i1, i3)
        else if (i3 == i) new IntTrieSet2(i1, i2)
        else this
    }
    override def +(i: Int): IntTrieSet = this.+(i, 0)
    override def +!(i: Int): IntTrieSet = this.+(i, 0)
    override def contains(value: Int): Boolean = value == i1 || value == i2 || value == i3
    override def exists(p: Int ⇒ Boolean): Boolean = p(i1) || p(i2) || p(i3)
    override def foldLeft[B](z: B)(f: (B, Int) ⇒ B): B = f(f(f(z, i1), i2), i3)
    override def forall(f: Int ⇒ Boolean): Boolean = f(i1) && f(i2) && f(i3)
    override def toChain: Chain[Int] = i1 :&: i2 :&: i3 :&: Naught

    override private[immutable] def subsetOf(other: IntTrieSet, level: Int): Boolean = {
        other.size match {
            case 0 | 1 | 2 ⇒
                false
            case 3 ⇒
                other match {
                    case that: IntTrieSet3 ⇒
                        that.i1 == this.i1 && that.i2 == this.i2 && that.i3 == this.i3
                    case _ ⇒
                        // ... this case should never occur...
                        other.contains(i1, i1 >>> level) &&
                            other.contains(i2, i2 >>> level) &&
                            other.contains(i3, i3 >>> level)
                }
            case _ ⇒
                other.contains(i1, i1 >>> level) &&
                    other.contains(i2, i2 >>> level) &&
                    other.contains(i3, i3 >>> level)
        }
    }

    override def equals(other: Any): Boolean = {
        other match {
            case that: IntTrieSet3 ⇒
                this.i1 == that.i1 && this.i2 == that.i2 && this.i3 == that.i3
            case that: IntTrieSet ⇒
                that.size == 3 && that.contains(i1) && that.contains(i2) && that.contains(i3)
            case _ ⇒
                false
        }
    }

    override def hashCode: Int = 31 * (31 * (31 + i1) + i2) + i3 // compatible to Arrays.hashCode

    override private[immutable] def +(i: Int, level: Int): IntTrieSet = {
        if (i == i1 || i == i2 || i == i3)
            this
        else
            IntTrieSet.from(i, i1, i2, i3, level)
    }
    override private[immutable] def +!(i: Int, level: Int): IntTrieSet = this.+(i, level)
}

private[immutable] abstract class IntTrieSetNN extends IntTrieSet {

    final override def isSingletonSet: Boolean = size == 1
    final override def hasMultipleElements: Boolean = size > 1
    final override def isEmpty: Boolean = false

    final override def map(f: Int ⇒ Int): IntTrieSet = {
        foldLeft(EmptyIntTrieSet: IntTrieSet)(_ +! f(_))
    }
    final override def map(map: Array[Int]): IntTrieSet = {
        foldLeft(EmptyIntTrieSet: IntTrieSet)(_ +! map(_))
    }

    final override def flatMap(f: Int ⇒ IntTrieSet): IntTrieSet = {
        foldLeft(EmptyIntTrieSet: IntTrieSet)(_ ++! f(_))
    }

    final override def withFilter(p: (Int) ⇒ Boolean): IntTrieSet = new FilteredIntTrieSet(this, p)

    final override def toChain: Chain[Int] = {
        val cb = new Chain.ChainBuilder[Int]()
        foreach((i: Int) ⇒ cb += i)
        cb.result()
    }

    final override def equals(other: Any): Boolean = {
        other match {
            case that: IntTrieSet ⇒
                that.size == this.size && {
                    // we have stable orderings!
                    val thisIt = this.intIterator
                    val otherIt = that.intIterator
                    var allEqual = true
                    while (thisIt.hasNext && allEqual) {
                        allEqual = thisIt.next() == otherIt.next()
                    }
                    allEqual
                }
            case _ ⇒
                false
        }
    }

    final override def hashCode: Int = foldLeft(1)(31 * _ + _)
}

private[immutable] final class IntTrieSetN private[immutable] (
        private[immutable] var left:  IntTrieSet, // can be empty, but never null!
        private[immutable] var right: IntTrieSet, // can be empty, but never null!
        var size:                     Int
) extends IntTrieSetNN { intSet ⇒

    assert(left.size + right.size == size)
    assert(size > 0) // <= can be "one" at construction time

    override def head: Int = if (left.nonEmpty) left.head else right.head

    override def exists(p: Int ⇒ Boolean): Boolean = left.exists(p) || right.exists(p)
    override def forall(p: Int ⇒ Boolean): Boolean = left.forall(p) && right.forall(p)

    override private[immutable] def subsetOf(other: IntTrieSet, level: Int): Boolean = {
        if (this.size > other.size)
            return false;

        other match {
            case that: IntTrieSetN ⇒
                this.right.size <= that.right.size && // check if we have a chance...
                    this.left.subsetOf(that.left, level + 1) &&
                    this.right.subsetOf(that.right, level + 1)
            case that: IntTrieSetNJustLeft ⇒
                this.right.isEmpty && this.left.subsetOf(that.left, level + 1)
            case that: IntTrieSetNJustRight ⇒
                this.left.isEmpty && this.right.subsetOf(that.right, level + 1)
            case that ⇒
                // Here, the level is actually not relevant...
                this.left.subsetOf(that, level + 1) && this.right.subsetOf(that, level + 1)
        }
    }

    override def foreach(f: IntConsumer): Unit = {
        left.foreach(f)
        right.foreach(f)
    }
    override def foreachPair[U](f: (Int, Int) ⇒ U): Unit = {
        val is = intIterator.toArray(size = size)
        val max = size
        var i = 0
        while (i < max) {
            var j = i + 1
            while (j < max) {
                f(is(i), is(j))
                j += 1
            }
            i += 1
        }
    }

    override def foldLeft[B](z: B)(f: (B, Int) ⇒ B): B = {
        right.foldLeft(left.foldLeft(z)(f))(f)
    }

    override private[immutable] def +(i: Int, level: Int): IntTrieSet = {
        if (((i >>> level) & 1) == 0) {
            val left = this.left
            val newLeft = left + (i, level + 1)
            if (newLeft eq left)
                this
            else
                IntTrieSetN(newLeft, right, size + 1)
        } else {
            val right = this.right
            val newRight = right + (i, level + 1)
            if (newRight eq right)
                this
            else
                IntTrieSetN(left, newRight, size + 1)
        }
    }

    override def +(i: Int): IntTrieSet = this.+(i, 0)

    override private[immutable] def +!(i: Int, level: Int): IntTrieSet = {
        if (((i >>> level) & 1) == 0) {
            val oldLeft = this.left
            val oldLeftSize = oldLeft.size
            val newLeft = oldLeft +! (i, level + 1)
            if (newLeft.size != oldLeftSize) {
                this.left = newLeft
                this.size += 1
            }
        } else {
            val oldRight = this.right
            val oldRightSize = oldRight.size
            val newRight = oldRight +! (i, level + 1)
            if (newRight.size != oldRightSize) {
                this.right = newRight
                this.size += 1
            }
        }
        this
    }

    override def +!(i: Int): IntTrieSet = this.+!(i, 0)

    override private[immutable] def contains(value: Int, key: Int): Boolean = {
        if ((key & 1) == 0)
            left.contains(value, key >>> 1)
        else
            right.contains(value, key >>> 1)
    }

    override def contains(value: Int): Boolean = this.contains(value, value)

    /**
     * Ensures that subtrees which contain less than 3 elements are represented using
     * a cannonical representation.
     */
    override private[immutable] def constringe(): IntTrieSet = {
        assert(size <= 2)
        if (left.isEmpty)
            right.constringe()
        else if (right.isEmpty)
            left.constringe()
        else
            new IntTrieSet2(left.head, right.head)
    }

    private[immutable] def -(i: Int, key: Int): IntTrieSet = {
        if ((key & 1) == 0) {
            val left = this.left
            val newLeft = left.-(i, key >>> 1)
            if (newLeft eq left)
                this
            else {
                (size - 1) match {
                    case 0 ⇒
                        EmptyIntTrieSet
                    case 1 ⇒
                        if (newLeft.isEmpty)
                            right.constringe()
                        else
                            newLeft.constringe()
                    case newSize ⇒
                        IntTrieSetN(newLeft, right, newSize)
                }
            }
        } else {
            val right = this.right
            val newRight = right.-(i, key >>> 1)
            if (newRight eq right)
                this
            else {
                val newSize = size - 1
                if (newSize == 0)
                    EmptyIntTrieSet
                else if (newSize == 1) {
                    if (newRight.isEmpty)
                        left.constringe()
                    else
                        newRight.constringe()
                } else {
                    IntTrieSetN(left, newRight, newSize)
                }
            }
        }
    }

    def -(i: Int): IntTrieSet = this.-(i, i)

    def intIterator: IntIterator = {
        new IntIterator {
            private[this] var it: IntIterator = left.intIterator
            private[this] var isRightIterator: Boolean = false
            private[this] def checkIterator(): Unit = {
                if (!it.hasNext && !isRightIterator) {
                    isRightIterator = true
                    it = right.intIterator
                }
            }
            override def toSet: IntTrieSet = intSet
            checkIterator()
            def hasNext: Boolean = it.hasNext
            def next(): Int = { val v = it.next(); checkIterator(); v }
        }
    }

    def iterator: Iterator[Int] = {
        new AbstractIterator[Int] {
            private[this] val it = intIterator
            override def hasNext: Boolean = it.hasNext
            override def next(): Int = it.next()
        }
    }

    override def getAndRemove: IntHeadAndRestOfSet[IntTrieSet] = {
        // try to reduce the tree size by removing an element from the
        // bigger subtree
        val left = this.left
        val right = this.right
        val leftSize = left.size
        val rightSize = right.size
        if (leftSize > rightSize) {
            // => left has at least one element
            if (leftSize == 1) { // => right is empty!
                IntHeadAndRestOfSet(left.head, EmptyIntTrieSet)
            } else {
                val IntHeadAndRestOfSet(v, newLeft) = left.getAndRemove
                val theNewLeft = if (leftSize == 2) newLeft.constringe() else newLeft
                IntHeadAndRestOfSet(v, IntTrieSetN(theNewLeft, right, leftSize - 1 + rightSize))
            }
        } else {
            // ...leftSize <= right.size
            assert(right.nonEmpty)
            if (right.isSingletonSet) {
                // left.size \in {0,1}
                IntHeadAndRestOfSet(right.head, left.constringe())
            } else {
                val IntHeadAndRestOfSet(v, newRight) = right.getAndRemove
                val theNewRight = if (rightSize == 2) newRight.constringe() else newRight
                IntHeadAndRestOfSet(v, IntTrieSetN(left, theNewRight, size - 1))
            }
        }
    }

    override def filter(p: Int ⇒ Boolean): IntTrieSet = {
        val left = this.left
        val right = this.right
        var newLeft = left.filter(p)
        var newRight = right.filter(p)
        if ((newLeft eq left) && (newRight eq right))
            return this;

        val newLeftSize = newLeft.size
        val newRightSize = newRight.size

        if (newLeftSize + newRightSize <= 2) {
            val newSet =
                if (newLeft.isEmpty) newRight
                else if (newRight.isEmpty) newLeft
                else newLeft + newRight.head
            return newSet;
        }

        if (newLeftSize <= 2) {
            newLeft = newLeft.constringe()
        }
        if (newRightSize <= 2) {
            newRight = newRight.constringe()
        }
        IntTrieSetN(newLeft, newRight, newLeftSize + newRightSize)
    }

}

private[immutable] object IntTrieSetN {

    def apply(
        left:  IntTrieSet, // can be empty, but never null!
        right: IntTrieSet, // can be empty, but never null!
        size:  Int
    ): IntTrieSet = {
        if (right.isEmpty)
            new IntTrieSetNJustLeft(left)
        else if (left.isEmpty)
            new IntTrieSetNJustRight(right)
        else
            new IntTrieSetN(left, right, size)
    }
}

private[immutable] final class IntTrieSetNJustRight private[immutable] (
        private[immutable] var right: IntTrieSet // can't be empty, left is already empty
) extends IntTrieSetNN { intSet ⇒

    assert(size > 0) // <= can be "one" at construction time

    override def size: Int = right.size
    override def head: Int = right.head
    override def exists(p: Int ⇒ Boolean): Boolean = right.exists(p)
    override def forall(p: Int ⇒ Boolean): Boolean = right.forall(p)
    override def foreach(f: IntConsumer): Unit = right.foreach(f)
    override def foreachPair[U](f: (Int, Int) ⇒ U): Unit = right.foreachPair(f)
    override def foldLeft[B](z: B)(f: (B, Int) ⇒ B): B = right.foldLeft(z)(f)

    override private[immutable] def +(i: Int, level: Int): IntTrieSet = {
        if (((i >>> level) & 1) == 0) {
            IntTrieSetN(IntTrieSet1(i), right, size + 1)
        } else {
            val right = this.right
            val newRight = right + (i, level + 1)
            if (newRight eq right)
                this
            else
                new IntTrieSetNJustRight(newRight)
        }
    }

    override private[immutable] def +!(i: Int, level: Int): IntTrieSet = {
        if (((i >>> level) & 1) == 0) {
            IntTrieSetN(IntTrieSet1(i), right, size + 1)
        } else {
            this.right = this.right +! (i, level + 1)
            this
        }
    }

    override def +(i: Int): IntTrieSet = this.+(i, 0)
    override def +!(i: Int): IntTrieSet = this.+!(i, 0)

    override private[immutable] def subsetOf(other: IntTrieSet, level: Int): Boolean = {
        if (this.size > other.size)
            return false;

        other match {
            case that: IntTrieSetN          ⇒ this.right.subsetOf(that.right, level + 1)
            case that: IntTrieSetNJustLeft  ⇒ false
            case that: IntTrieSetNJustRight ⇒ this.right.subsetOf(that.right, level + 1)
            case that                       ⇒ this.right.subsetOf(that, level + 1)
        }
    }

    override private[immutable] def contains(value: Int, key: Int): Boolean = {
        if ((key & 1) == 0)
            false
        else
            right.contains(value, key >>> 1)
    }

    override def contains(value: Int): Boolean = this.contains(value, value)

    /**
     * Ensures that subtrees which contain less than 3 elements are represented using
     * a cannonical representation.
     */
    override private[immutable] def constringe(): IntTrieSet = {
        assert(size <= 2)
        right.constringe()
    }

    private[immutable] def -(i: Int, key: Int): IntTrieSet = {
        if ((key & 1) == 0) {
            this
        } else {
            val right = this.right
            val newRight = right.-(i, key >>> 1)
            if (newRight eq right)
                this
            else {
                val newSize = size - 1
                if (newSize == 0)
                    EmptyIntTrieSet
                else if (newSize == 1) {
                    newRight.constringe()
                } else {
                    new IntTrieSetNJustRight(newRight)
                }
            }
        }
    }

    def -(i: Int): IntTrieSet = this.-(i, i)

    def intIterator: IntIterator = right.intIterator
    def iterator: Iterator[Int] = right.iterator

    override def getAndRemove: IntHeadAndRestOfSet[IntTrieSet] = {
        // try to reduce the tree size by removing an element from the
        // bigger subtree
        val right = this.right
        val rightSize = right.size
        if (right.isSingletonSet) {
            IntHeadAndRestOfSet(right.head, EmptyIntTrieSet)
        } else {
            val IntHeadAndRestOfSet(v, newRight) = right.getAndRemove
            val theNewRight = if (rightSize == 2) newRight.constringe() else newRight
            IntHeadAndRestOfSet(v, new IntTrieSetNJustRight(theNewRight))
        }

    }

    override def filter(p: (Int) ⇒ Boolean): IntTrieSet = {
        val right = this.right
        val newRight = right.filter(p)
        if (newRight eq right)
            return this;

        val newRightSize = newRight.size
        if (newRightSize <= 2) {
            newRight.constringe()
        } else {
            new IntTrieSetNJustRight(newRight)
        }
    }

}

private[immutable] final class IntTrieSetNJustLeft private[immutable] (
        private[immutable] var left: IntTrieSet // cannot be empty; right is empty
) extends IntTrieSetNN { intSet ⇒

    assert(size > 0) // <= can be "one" at construction time

    override def size: Int = left.size

    override def head: Int = left.head
    override def exists(p: Int ⇒ Boolean): Boolean = left.exists(p)
    override def forall(p: Int ⇒ Boolean): Boolean = left.forall(p)
    override def foreach(f: IntConsumer): Unit = left.foreach(f)
    override def foreachPair[U](f: (Int, Int) ⇒ U): Unit = left.foreachPair(f)
    override def foldLeft[B](z: B)(f: (B, Int) ⇒ B): B = left.foldLeft(z)(f)

    override private[immutable] def +(i: Int, level: Int): IntTrieSet = {
        if (((i >>> level) & 1) == 0) {
            val left = this.left
            val newLeft = left + (i, level + 1)
            if (newLeft eq left)
                this
            else
                new IntTrieSetNJustLeft(newLeft)
        } else {
            new IntTrieSetN(left, IntTrieSet1(i), size + 1)
        }
    }

    override private[immutable] def +!(i: Int, level: Int): IntTrieSet = {
        if (((i >>> level) & 1) == 0) {
            this.left = this.left +! (i, level + 1)
            this
        } else {
            new IntTrieSetN(left, IntTrieSet1(i), size + 1)
        }
    }

    override def +(i: Int): IntTrieSet = this.+(i, 0)
    override def +!(i: Int): IntTrieSet = this.+!(i, 0)

    override def subsetOf(other: IntTrieSet, level: Int): Boolean = {
        if (this.size > other.size)
            return false;

        other match {
            case that: IntTrieSetN          ⇒ this.left.subsetOf(that.left, level + 1)
            case that: IntTrieSetNJustLeft  ⇒ this.left.subsetOf(that.left, level + 1)
            case that: IntTrieSetNJustRight ⇒ false
            case that                       ⇒ this.left.subsetOf(that, level + 1)
        }
    }

    override private[immutable] def contains(value: Int, key: Int): Boolean = {
        if ((key & 1) == 0)
            left.contains(value, key >>> 1)
        else
            false
    }

    override def contains(value: Int): Boolean = this.contains(value, value)

    /**
     * Ensures that subtrees which contain less than 3 elements are represented using
     * a cannonical representation.
     */
    override private[immutable] def constringe(): IntTrieSet = {
        assert(size <= 2)
        left.constringe()
    }

    private[immutable] def -(i: Int, key: Int): IntTrieSet = {
        if ((key & 1) == 0) {
            val left = this.left
            val newLeft = left.-(i, key >>> 1)
            if (newLeft eq left)
                this
            else {
                val newSize = size - 1
                newSize match {
                    case 0 ⇒
                        EmptyIntTrieSet
                    case 1 ⇒
                        newLeft.constringe()
                    case _ ⇒
                        new IntTrieSetNJustLeft(newLeft)
                }
            }
        } else {
            this
        }
    }

    def -(i: Int): IntTrieSet = this.-(i, i)

    def intIterator: IntIterator = left.intIterator

    def iterator: Iterator[Int] = left.iterator

    override def getAndRemove: IntHeadAndRestOfSet[IntTrieSet] = {
        // try to reduce the tree size by removing an element from the
        // bigger subtree
        val left = this.left
        val leftSize = left.size
        if (leftSize == 1) { // => right is empty!
            IntHeadAndRestOfSet(left.head, EmptyIntTrieSet)
        } else {
            val IntHeadAndRestOfSet(v, newLeft) = left.getAndRemove
            val theNewLeft = if (leftSize == 2) newLeft.constringe() else newLeft
            IntHeadAndRestOfSet(v, new IntTrieSetNJustLeft(theNewLeft))
        }

    }

    override def filter(p: (Int) ⇒ Boolean): IntTrieSet = {
        val left = this.left
        val newLeft = left.filter(p)
        if (newLeft eq left)
            return this;

        val newLeftSize = newLeft.size
        if (newLeftSize <= 2) {
            newLeft.constringe()
        } else {
            new IntTrieSetNJustLeft(newLeft)
        }
    }

}

class IntTrieSetBuilder extends scala.collection.mutable.Builder[Int, IntTrieSet] {
    private[this] var s: IntTrieSet = EmptyIntTrieSet
    def +=(i: Int): this.type = { s +!= i; this }
    def clear(): Unit = s = EmptyIntTrieSet
    def result(): IntTrieSet = s
}

/**
 * Factory to create IntTrieSets.
 */
object IntTrieSet {

    def empty: IntTrieSet = EmptyIntTrieSet

    def apply(i1: Int): IntTrieSet = IntTrieSet1(i1)

    def apply(i1: Int, i2: Int): IntTrieSet = {
        if (i1 == i2)
            IntTrieSet1(i1)
        else {
            from(i1, i2)
        }
    }

    /** Constructs a new IntTrie from the two distinct(!) values. */
    def from(i1: Int, i2: Int): IntTrieSet = {
        assert(i1 != i2)
        // we have to ensure the same ordering as used when the values are
        // stored in the trie
        if ((Integer.lowestOneBit(i1 ^ i2) & i1) == 0) {
            // ... i2 is the value with a 0 at the bit position where both values differ
            new IntTrieSet2(i1, i2)
        } else {
            new IntTrieSet2(i2, i1)
        }
    }

    def apply(i1: Int, i2: Int, i3: Int): IntTrieSet = {
        if (i1 == i2)
            IntTrieSet(i1, i3) // this also handles the case i1 == i3
        else if (i1 == i3 || i2 == i3) { // we have i1 =!= i2
            IntTrieSet.from(i1, i2)
        } else { // i1 =!= i2 && i2 =!= i3 && i1 =!= i3
            IntTrieSet.from(i1, i2, i3)
        }
    }

    /** Constructs a new IntTrie from the three distinct(!) values! */
    def from(i1: Int, i2: Int, i3: Int): IntTrieSet = {
        // We have to ensure the same ordering as used when the values are stored in the trie...
        var v1, v2, v3 = 0
        if ((Integer.lowestOneBit(i1 ^ i2) & i1) == 0) {
            // ... i1 is the value with a 0 at the lowest one bit position...
            v1 = i1
            v2 = i2
        } else {
            v1 = i2
            v2 = i1
        }

        if ((Integer.lowestOneBit(v2 ^ i3) & v2) == 0) {
            // v2 is the value with the 0 and the distinguishing position...
            v3 = i3
        } else {
            v3 = v2
            if ((Integer.lowestOneBit(v1 ^ i3) & v1) == 0) {
                v2 = i3
            } else {
                v2 = v1
                v1 = i3
            }
        }

        new IntTrieSet3(v1, v2, v3)
    }

    def apply(i1: Int, i2: Int, i3: Int, i4: Int): IntTrieSet = {
        if (i1 == i2) {
            IntTrieSet(i2, i3, i4)
        } else if (i1 == i3 || i2 == i3 || i3 == i4) { // we have i1 =!= i2
            IntTrieSet(i1, i2, i4)
        } else if (i1 == i4 || i2 == i4) {
            IntTrieSet(i1, i2, i3)
        } else {
            IntTrieSet.from(i1, i2, i3, i4, 0)
        }
    }

    def from(i1: Int, i2: Int, i3: Int, i4: Int): IntTrieSet = {
        if ((i1 & 1) == 0) {
            if ((i2 & 1) == 0) {
                if ((i3 & 1) == 0) {
                    if ((i4 & 1) == 0) { // first bit of all "0"
                        new IntTrieSetNJustLeft(from(i1, i2, i3, i4, 1))
                    } else { // first bit of i4 is "1"
                        new IntTrieSetN(IntTrieSet.from(i1, i2, i3), IntTrieSet1(i4), 4)
                    }
                } else {
                    if ((i4 & 1) == 0) { // first bit of i3 is "1"
                        new IntTrieSetN(IntTrieSet.from(i1, i2, i4), IntTrieSet1(i3), 4)
                    } else { // first bit of i3, i4 is "1"
                        new IntTrieSetN(IntTrieSet.from(i1, i2), IntTrieSet.from(i3, i4), 4)
                    }
                }
            } else {
                if ((i3 & 1) == 0) {
                    if ((i4 & 1) == 0) { // first bit of i2 is "1"
                        new IntTrieSetN(IntTrieSet.from(i1, i3, i4), IntTrieSet1(i2), 4)
                    } else { // first bit of i2 and i4 is "1"
                        new IntTrieSetN(IntTrieSet.from(i1, i3), IntTrieSet.from(i2, i4), 4)
                    }
                } else {
                    if ((i4 & 1) == 0) { // first bit of i2, i3 is "1"
                        new IntTrieSetN(IntTrieSet.from(i1, i4), IntTrieSet.from(i2, i3), 4)
                    } else { // first bit of i2, i3, i4 is "1"
                        new IntTrieSetN(IntTrieSet1(i1), IntTrieSet.from(i2, i3, i4), 4)
                    }
                }
            }
        } else {
            if ((i2 & 1) == 0) {
                if ((i3 & 1) == 0) {
                    if ((i4 & 1) == 0) { // first bit of i1 is "1"
                        new IntTrieSetN(IntTrieSet.from(i2, i3, i4), IntTrieSet1(i1), 4)
                    } else { // first bit of i1, i4 is "1"
                        new IntTrieSetN(IntTrieSet.from(i2, i3), IntTrieSet.from(i1, i4), 4)
                    }
                } else {
                    if ((i4 & 1) == 0) { // first bit of i1, i3 is "1"
                        new IntTrieSetN(IntTrieSet.from(i2, i4), IntTrieSet.from(i1, i3), 4)
                    } else { // first bit of i1, i3, i4 is "1"
                        new IntTrieSetN(IntTrieSet1(i2), IntTrieSet.from(i1, i3, i4), 4)
                    }
                }
            } else {
                if ((i3 & 1) == 0) {
                    if ((i4 & 1) == 0) { // first bit of i1, i2 is "1"
                        new IntTrieSetN(IntTrieSet.from(i3, i4), IntTrieSet.from(i1, i2), 4)
                    } else { // first bit of i1, i2 and i4 is "1"
                        new IntTrieSetN(IntTrieSet1(i3), IntTrieSet.from(i1, i2, i4), 4)
                    }
                } else {
                    if ((i4 & 1) == 0) { // first bit of i1, i2, i3 is "1"
                        new IntTrieSetN(IntTrieSet1(i4), IntTrieSet.from(i1, i2, i3), 4)
                    } else { // first bit of i1, i2, i3, i4 is "1"
                        new IntTrieSetNJustRight(from(i1, i2, i3, i4, 1))
                    }
                }
            }
        }
    }

    /**
     * Constructs a new `IntTrieSet` from the given distinct values.
     *
     * If level is > 0 then all values have to have the same least significant bits up until level!
     */
    private[immutable] def from(i1: Int, i2: Int, i3: Int, i4: Int, level: Int): IntTrieSet = {
        val root =
            if (((i1 >>> level) & 1) == 0)
                new IntTrieSetNJustLeft(IntTrieSet1(i1))
            else
                new IntTrieSetNJustRight(IntTrieSet1(i1))

        root +! (i2, level) +! (i3, level) +! (i4, level)
    }

}
