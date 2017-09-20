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

import java.util.Arrays

import scala.collection.AbstractIterator
import scala.collection.mutable.Builder

/**
 * A sorted set of integer values backed by an ordered array to store the values; this
 * guarantees log2(n) lookup.
 *
 * @author Michael Eichberg
 */
abstract class IntArraySet extends ((Int) ⇒ Int) {

    def isSingletonSet: Boolean

    def size: Int

    def isEmpty: Boolean

    def nonEmpty: Boolean = !isEmpty

    /**
     * @return `true` if the set contains two or more values.
     */
    def hasMultipleElements: Boolean

    def min: Int

    def max: Int

    final def head: Int = min

    final def last: Int = max

    /** Returns some value and removes it from this set. */
    def getAndRemove: (Int, IntArraySet)

    def foreach[U](f: Int ⇒ U): Unit

    /**
     * Iterates over all possible pairings of two values of this set; that is, if the set has
     * three elements: {1,2,3}, the pairs (1,2), (1,3) and (2,3) will be iterated over.
     */
    def foreachPair[U](f: (Int, Int) ⇒ U): Unit

    /**
     * Returns a lazily filtered set. However, all operations other operations except of
     * `withFilter` and `iterator` will force the evaluation.
     */
    def withFilter(p: (Int) ⇒ Boolean): IntArraySet

    def map(f: Int ⇒ Int): IntArraySet

    def flatMap(f: Int ⇒ IntArraySet): IntArraySet = {
        val builder = new IntArraySetBuilder
        foreach { i ⇒ builder ++= f(i) }
        builder.result
    }

    def transform[T, To](f: Int ⇒ T, b: Builder[T, To]): To = {
        foreach(i ⇒ b += f(i))
        b.result()
    }

    def -(i: Int): IntArraySet

    def --(is: Traversable[Int]): IntArraySet = this.foldLeft(this)(_ - _)

    def +(i: Int): IntArraySet

    def subsetOf(other: IntArraySet): Boolean = {
        val thisIt = this.iterator
        val otherIt = other.iterator
        while (thisIt.hasNext && otherIt.hasNext) {
            val thisV = thisIt.next()
            var otherV = otherIt.next()
            while (otherV != thisV && otherIt.hasNext) { otherV = otherIt.next() };
            if (thisV != otherV)
                return false;
        }
        !thisIt.hasNext
    }

    def iterator: Iterator[Int]

    def contains(value: Int): Boolean

    def exists(p: Int ⇒ Boolean): Boolean

    def foldLeft[B](z: B)(f: (B, Int) ⇒ B): B

    def forall(f: Int ⇒ Boolean): Boolean

    def ++(that: IntArraySet): IntArraySet = {
        if (this.size > that.size)
            that.foldLeft(this)(_ + _) // we expand `this` since `this` is larger
        else
            this.foldLeft(that)(_ + _) // we expand that
    }

    def mkString(pre: String, in: String, post: String): String = {
        val sb = new StringBuilder(pre)
        val it = iterator
        var hasNext = it.hasNext
        while (hasNext) {
            sb.append(it.next.toString())
            hasNext = it.hasNext
            if (hasNext) sb.append(in)
        }
        sb.append(post)
        sb.toString()
    }

    final def mkString(in: String): String = mkString("", in, "")

    def toChain: Chain[Int]

    final override def toString: String = mkString("IntArraySet(", ",", ")")
}

case object EmptyIntArraySet extends IntArraySet {
    def apply(index: Int): Int = throw new UnsupportedOperationException("empty set")
    def isSingletonSet: Boolean = false
    def hasMultipleElements: Boolean = false
    def isEmpty: Boolean = true
    def size: Int = 0
    def max: Int = throw new UnsupportedOperationException("empty set")
    def min: Int = throw new UnsupportedOperationException("empty set")
    def getAndRemove: (Int, IntArraySet) = throw new UnsupportedOperationException("empty set")
    def foreach[U](f: Int ⇒ U): Unit = {}
    def foreachPair[U](f: (Int, Int) ⇒ U): Unit = {}
    def withFilter(p: (Int) ⇒ Boolean): IntArraySet = this
    def map(f: Int ⇒ Int): IntArraySet = this
    def -(i: Int): this.type = this
    override def subsetOf(other: IntArraySet): Boolean = true
    def +(i: Int): IntArraySet1 = new IntArraySet1(i)
    def iterator: Iterator[Int] = Iterator.empty
    def contains(value: Int): Boolean = false
    def exists(p: Int ⇒ Boolean): Boolean = false
    def foldLeft[B](z: B)(f: (B, Int) ⇒ B): B = z
    def forall(f: Int ⇒ Boolean): Boolean = true

    def toChain: Chain[Int] = Naught

    override def equals(other: Any): Boolean = {
        other match {
            case is: IntArraySet ⇒ is.isEmpty
            case _               ⇒ false
        }
    }

    override def hashCode: Int = 0 // compatible to Arrays.hashCode
}

case class IntArraySet1(i: Int) extends IntArraySet {
    def apply(index: Int): Int = if (index == 0) i else throw new IndexOutOfBoundsException()
    def isEmpty: Boolean = false
    def isSingletonSet: Boolean = true
    def hasMultipleElements: Boolean = false
    def foreach[U](f: Int ⇒ U): Unit = { f(i) }
    def foreachPair[U](f: (Int, Int) ⇒ U): Unit = {}
    def max: Int = this.i
    def min: Int = this.i
    def getAndRemove: (Int, IntArraySet) = (i, EmptyIntArraySet)
    def withFilter(p: (Int) ⇒ Boolean): IntArraySet = if (p(i)) this else EmptyIntArraySet
    def map(f: Int ⇒ Int): IntArraySet = {
        val i = this.i
        val newI = f(i)
        if (newI != i)
            new IntArraySet1(newI)
        else
            this
    }
    def -(i: Int): IntArraySet = {
        if (this.i != i) this else EmptyIntArraySet
    }
    def +(i: Int): IntArraySet = {
        if (this.i == i) this else if (this.i < i) new IntArraySet2(this.i, i) else new IntArraySet2(i, this.i)
    }
    def iterator: Iterator[Int] = Iterator.single(i)
    def size: Int = 1

    def contains(value: Int): Boolean = value == i
    def exists(p: Int ⇒ Boolean): Boolean = p(i)
    def foldLeft[B](z: B)(f: (B, Int) ⇒ B): B = f(z, i)
    def forall(f: Int ⇒ Boolean): Boolean = f(i)

    def toChain: Chain[Int] = new :&:[Int](i)

    override def equals(other: Any): Boolean = {
        other match {
            case is: IntArraySet if is.isSingletonSet ⇒ is.min == i
            case _                                    ⇒ false
        }
    }

    override def hashCode: Int = 31 + i // compatible to Arrays.hashCode
}

/**
 * Represents an orderd set of two values where i1 has to be smaller than i2.
 */
case class IntArraySet2(i1: Int, i2: Int) extends IntArraySet {

    assert(i1 < i2)
    def apply(index: Int): Int = {
        index match {
            case 0 ⇒ i1
            case 1 ⇒ i2
            case _ ⇒ throw new IndexOutOfBoundsException()
        }
    }
    def isEmpty: Boolean = false
    def isSingletonSet: Boolean = false
    def hasMultipleElements: Boolean = true
    def size: Int = 2
    def min: Int = this.i1
    def max: Int = this.i2
    def getAndRemove: (Int, IntArraySet) = (i2, new IntArraySet1(i1))

    def iterator: Iterator[Int] = new AbstractIterator[Int] {
        var i = 0
        def hasNext: Boolean = i < 2
        def next: Int = {
            val v = i
            i += 1
            v match {
                case 0 ⇒ i1
                case 1 ⇒ i2
                case _ ⇒ throw new IllegalStateException()
            }
        }
    }
    def foreach[U](f: Int ⇒ U): Unit = { f(i1); f(i2) }
    def foreachPair[U](f: (Int, Int) ⇒ U): Unit = f(i1, i2)

    def withFilter(p: (Int) ⇒ Boolean): IntArraySet = {
        if (p(i1)) {
            if (p(i2)) this
            else new IntArraySet1(i1)
        } else {
            if (p(i2)) new IntArraySet1(i2)
            else
                EmptyIntArraySet
        }
    }
    def map(f: Int ⇒ Int): IntArraySet = {
        val i1 = this.i1
        val newI1 = f(i1)
        val i2 = this.i2
        val newI2 = f(i2)
        if (newI1 != i1 || newI2 != i2)
            IntArraySet(newI1, newI2) // ensures invariant
        else
            this
    }
    def -(i: Int): IntArraySet = {
        if (i == i1) new IntArraySet1(i2)
        else if (i == i2) new IntArraySet1(i1)
        else this
    }
    def +(i: Int): IntArraySet = {
        if (i <= i1) {
            if (i == i1) this
            else new IntArraySet3(i, i1, i2)
        } else if (i <= i2) {
            if (i == i2) this
            else new IntArraySet3(i1, i, i2)
        } else {
            new IntArraySet3(i1, i2, i)
        }
    }
    def contains(value: Int): Boolean = value == i1 || value == i2
    def exists(p: Int ⇒ Boolean): Boolean = p(i1) || p(i2)
    def foldLeft[B](z: B)(f: (B, Int) ⇒ B): B = f(f(z, i1), i2)
    def forall(f: Int ⇒ Boolean): Boolean = f(i1) && f(i2)

    def toChain: Chain[Int] = i1 :&: i2 :&: Naught

    override def equals(other: Any): Boolean = {
        other match {
            case is: IntArraySet if is.size == 2 ⇒ is.min == this.i1 && is.max == this.i2
            case _                               ⇒ false
        }
    }

    override def hashCode: Int = 31 * (31 + i1) + i2 // compatible to Arrays.hashCode
}

/**
 * Represents an orderd set of three int values: i1 < i2 < i3.
 */
case class IntArraySet3(i1: Int, i2: Int, i3: Int) extends IntArraySet {

    assert(i1 < i2, s"i1 < i2: $i1 >= $i2")
    assert(i2 < i3, s"i2 < i3: $i2 >= $i3")

    def apply(index: Int): Int = {
        index match {
            case 0 ⇒ i1
            case 1 ⇒ i2
            case 2 ⇒ i3
            case _ ⇒ throw new IndexOutOfBoundsException()
        }
    }
    def isEmpty: Boolean = false
    def isSingletonSet: Boolean = false
    def hasMultipleElements: Boolean = true
    def size: Int = 3
    def min: Int = this.i1
    def max: Int = this.i3
    def getAndRemove: (Int, IntArraySet) = (i3, new IntArraySet2(i1, i2))

    def iterator: Iterator[Int] = new AbstractIterator[Int] {
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
    def foreach[U](f: Int ⇒ U): Unit = { f(i1); f(i2); f(i3) }
    def foreachPair[U](f: (Int, Int) ⇒ U): Unit = { f(i1, i2); f(i1, i3); f(i2, i3) }

    def withFilter(p: (Int) ⇒ Boolean): IntArraySet = {
        if (p(i1)) {
            if (p(i2)) {
                if (p(i3))
                    this
                else
                    new IntArraySet2(i1, i2)
            } else {
                if (p(i3))
                    new IntArraySet2(i1, i3)
                else
                    new IntArraySet1(i1)
            }
        } else {
            if (p(i2)) {
                if (p(i3))
                    new IntArraySet2(i2, i3)
                else
                    new IntArraySet1(i2)
            } else {
                if (p(i3))
                    new IntArraySet1(i3)
                else
                    IntArraySet.empty
            }
        }
    }
    def map(f: Int ⇒ Int): IntArraySet = {
        val i1 = this.i1
        val newI1 = f(i1)
        val i2 = this.i2
        val newI2 = f(i2)
        val i3 = this.i3
        val newI3 = f(i3)
        if (newI1 != i1 || newI2 != i2 || newI3 != i3)
            IntArraySet(newI1, newI2, newI3) // ensures invariant
        else
            this
    }
    def -(i: Int): IntArraySet = {
        if (i1 == i) new IntArraySet2(i2, i3)
        else if (i2 == i) new IntArraySet2(i1, i3)
        else if (i3 == i) new IntArraySet2(i1, i2)
        else this
    }
    def +(i: Int): IntArraySet = {
        if (i < i2) {
            if (i < i1)
                new IntArraySetN(Array[Int](i, i1, i2, i3))
            else if (i == i1)
                this
            else
                new IntArraySetN(Array[Int](i1, i, i2, i3))
        } else if (i < i3) {
            if (i == i2)
                this
            else
                new IntArraySetN(Array[Int](i1, i2, i, i3))
        } else if (i == i3)
            this
        else
            new IntArraySetN(Array[Int](i1, i2, i3, i))
    }
    def contains(value: Int): Boolean = value == i1 || value == i2 || value == i3
    def exists(p: Int ⇒ Boolean): Boolean = p(i1) || p(i2) || p(i3)
    def foldLeft[B](z: B)(f: (B, Int) ⇒ B): B = f(f(f(z, i1), i2), i3)
    def forall(f: Int ⇒ Boolean): Boolean = f(i1) && f(i2) && f(i3)

    def toChain: Chain[Int] = i1 :&: i2 :&: i3 :&: Naught

    override def equals(other: Any): Boolean = {
        other match {
            case that: IntArraySet if that.size == 3 ⇒
                this(0) == that(0) && this(1) == that(1) && this(2) == that(2)
            case _ ⇒ false
        }
    }

    override def hashCode: Int = 31 * (31 * (31 + i1) + i2) + i3 // compatible to Arrays.hashCode
}

case class IntArraySetN private[immutable] (
        private[immutable] val is: Array[Int]
) extends IntArraySet {

    assert(is.length > 3)

    def apply(index: Int): Int = is(index)
    def size: Int = is.length
    def isSingletonSet: Boolean = false
    def hasMultipleElements: Boolean = true
    def isEmpty: Boolean = false
    def max: Int = is(is.length - 1)
    def min: Int = is(0)
    def getAndRemove: (Int, IntArraySet) = {
        if (is.length > 4)
            (max, new IntArraySetN(is.init))
        else
            (max, new IntArraySet3(is(0), is(1), is(2)))
    }
    def foreach[U](f: Int ⇒ U): Unit = {
        val max = is.length
        var i = 0
        while (i < max) {
            f(is(i))
            i += 1
        }
    }
    def foreachPair[U](f: (Int, Int) ⇒ U): Unit = {
        val max = is.length
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

    def withFilter(p: (Int) ⇒ Boolean): IntArraySet = new FilteredIntArraySet(p, this)

    def map(f: Int ⇒ Int): IntArraySet = {
        // let's check if all values are mapped to their original values; if so return "this"
        val max = is.length
        var i = 0
        var f_is_i: Int = 0 // the initial value is never used!
        while (i < max && {
            val is_i = is(i)
            f_is_i = f(is_i)
            is_i == f_is_i
        }) { i += 1 }
        if (i == max)
            return this;

        val isb = new IntArraySetBuilder
        var l = 0
        while (l < i) { isb += is(l) /*the values were unchanged*/ ; l += 1 }
        while (i < max) {
            isb += f(is(i))
            i += 1
        }
        isb.result()
    }

    def -(i: Int): IntArraySet = {
        val index = Arrays.binarySearch(is, 0, size, i)
        if (index >= 0) {
            if (is.length == 4) {
                index match {
                    case 0 ⇒ new IntArraySet3(is(1), is(2), is(3))
                    case 1 ⇒ new IntArraySet3(is(0), is(2), is(3))
                    case 2 ⇒ new IntArraySet3(is(0), is(1), is(3))
                    case 3 ⇒ new IntArraySet3(is(0), is(1), is(2))
                }
            } else {
                // the element is found
                val targetIs = new Array[Int](is.length - 1)
                System.arraycopy(is, 0, targetIs, 0, index)
                System.arraycopy(is, index + 1, targetIs, index, is.length - 1 - index)
                new IntArraySetN(targetIs)
            }
        } else
            this
    }

    def +(i: Int): IntArraySet = {
        val index = Arrays.binarySearch(is, 0, size, i)
        if (index < 0) {
            val insertionPoint = -index - 1
            // the element is NOT already found
            val targetIs = new Array[Int](is.length + 1)
            System.arraycopy(is, 0, targetIs, 0, insertionPoint)
            targetIs(insertionPoint) = i
            System.arraycopy(is, insertionPoint, targetIs, insertionPoint + 1, is.length - insertionPoint)
            new IntArraySetN(targetIs)
        } else {
            this
        }
    }

    def contains(value: Int): Boolean = Arrays.binarySearch(is, 0, size, value) >= 0

    def exists(p: Int ⇒ Boolean): Boolean = {
        var i = 0
        val data = this.is
        val max = data.length
        while (i < max) { if (p(data(i))) return true; i += 1 }
        false
    }

    def foldLeft[B](z: B)(f: (B, Int) ⇒ B): B = {
        var i = 0
        val data = this.is
        val max = data.length
        var r = z
        while (i < max) { r = f(r, data(i)); i += 1 }
        r
    }

    def forall(p: Int ⇒ Boolean): Boolean = {
        var i = 0
        val data = this.is
        val max = data.length
        while (i < max) { if (!p(data(i))) return false; i += 1 }
        true
    }

    def iterator: Iterator[Int] = is.iterator

    def toChain: Chain[Int] = {
        val cb = new Chain.ChainBuilder[Int]()
        foreach(i ⇒ cb += i)
        cb.result()
    }

    override def equals(other: Any): Boolean = {
        other match {
            case that: IntArraySet if that.size == this.size ⇒ this.subsetOf(that)
            case _                                           ⇒ false
        }
    }

    override def hashCode: Int = Arrays.hashCode(is)
}

private[immutable] class FilteredIntArraySet(p: Int ⇒ Boolean, origS: IntArraySetN) extends IntArraySet {

    @volatile private[this] var filteredS: IntArraySet = _

    private def getFiltered: IntArraySet = {
        if (filteredS eq null) {
            this.synchronized {
                if (filteredS eq null) {
                    filteredS = {
                        val is = origS.is
                        val targetIs = new Array[Int](origS.size)
                        val max = is.length
                        var index = 0
                        var targetIsIndex = 0
                        while (index < max) {
                            if (p(is(index))) {
                                targetIs(targetIsIndex) = is(index)
                                targetIsIndex += 1
                            }
                            index += 1
                        }
                        targetIsIndex match {
                            case 0 ⇒ EmptyIntArraySet
                            case 1 ⇒ new IntArraySet1(targetIs(0))
                            case 2 ⇒ new IntArraySet2(targetIs(0), targetIs(1))
                            case _ ⇒
                                if (targetIsIndex == max) // no value was filtered...
                                    origS
                                else {
                                    new IntArraySetN(Arrays.copyOf(targetIs, targetIsIndex))
                                }

                        }
                    }
                }
            }
        }
        filteredS
    }

    def apply(index: Int): Int = getFiltered.apply(index)

    def withFilter(p: (Int) ⇒ Boolean): IntArraySet = {
        if (filteredS ne null) {
            filteredS.withFilter(p)
        } else {
            new FilteredIntArraySet((i: Int) ⇒ this.p(i) && p(i), origS)
        }
    }

    def iterator: Iterator[Int] = {
        if (filteredS ne null) {
            filteredS.iterator
        } else {
            origS.iterator.filter(p)
        }
    }

    def foreach[U](f: Int ⇒ U): Unit = {
        if (filteredS ne null) {
            filteredS.foreach(f)
        } else {
            val is = origS.is
            val max = is.length
            var j = 0
            while (j < max) {
                val i = is(j)
                if (p(i)) f(i)
                j += 1
            }
        }
    }
    def foreachPair[U](f: (Int, Int) ⇒ U): Unit = getFiltered.foreachPair(f)

    def contains(value: Int): Boolean = p(value) && origS.contains(value)
    def exists(p: Int ⇒ Boolean): Boolean = iterator.exists(p)
    def foldLeft[B](z: B)(f: (B, Int) ⇒ B): B = iterator.foldLeft(z)(f)
    def forall(p: Int ⇒ Boolean): Boolean = iterator.forall(p)

    def size: Int = getFiltered.size
    def isSingletonSet: Boolean = getFiltered.isSingletonSet
    def hasMultipleElements: Boolean = getFiltered.hasMultipleElements
    def isEmpty: Boolean = getFiltered.isEmpty
    def min: Int = getFiltered.min
    def max: Int = getFiltered.max
    def map(f: Int ⇒ Int): IntArraySet = getFiltered.map(f)
    def getAndRemove: (Int, IntArraySet) = getFiltered.getAndRemove
    def -(i: Int): IntArraySet = getFiltered - i
    def +(i: Int): IntArraySet = getFiltered + 1
    def toChain: Chain[Int] = getFiltered.toChain
    override def equals(other: Any): Boolean = getFiltered.equals(other)
    override def hashCode: Int = getFiltered.hashCode
}

class IntArraySetBuilder private[immutable] (
        private[this] var is:   Array[Int],
        private[this] var size: Int
) extends Builder[Int, IntArraySet] {

    require(size <= is.length)

    def this() {
        this(new Array[Int](4), 0)
    }

    def +=(elem: Int): this.type = {
        val index = Arrays.binarySearch(is, 0, size, elem)
        if (index < 0) {
            // the element is NOT already found
            size += 1
            val insertionPoint = -index - 1
            if ( /*new*/ size <= is.length) { // we have enough space
                System.arraycopy(is, insertionPoint, is, insertionPoint + 1, (size - 1) - insertionPoint)
                is(insertionPoint) = elem
            } else {
                val targetIs = new Array[Int](is.length * 2)
                System.arraycopy(is, 0, targetIs, 0, insertionPoint)
                targetIs(insertionPoint) = elem
                System.arraycopy(is, insertionPoint, targetIs, insertionPoint + 1, is.length - insertionPoint)
                is = targetIs
            }
        }

        this
    }

    def ++=(elems: IntArraySet): this.type = { elems.foreach(this.+=); this }

    def clear(): Unit = { is = new Array[Int](4); size = 0 }

    def result(): IntArraySet = {
        size match {
            case 0 ⇒ EmptyIntArraySet
            case 1 ⇒ new IntArraySet1(is(0))
            case 2 ⇒ new IntArraySet2(is(0), is(1))
            case 3 ⇒ new IntArraySet3(is(0), is(1), is(2))
            case _ ⇒
                if (size == is.length)
                    new IntArraySetN(is)
                else {
                    val targetIs = new Array[Int](size)
                    System.arraycopy(is, 0, targetIs, 0, size)
                    new IntArraySetN(targetIs)
                }

        }
    }

    override def toString: String = is.mkString(s"IntArraySetBuilder(size=$size;values={", ",", "})")
}

object IntArraySetBuilder {

    def apply(vs: Int*): IntArraySetBuilder = {
        val isb = new IntArraySetBuilder(new Array[Int](vs.size), 0)
        vs.foreach(isb.+=)
        isb
    }

    def apply(vs: Set[Int]): IntArraySetBuilder = {
        val isb = new IntArraySetBuilder(new Array[Int](Math.max(4, vs.size)), 0)
        vs.foreach(isb.+=)
        isb
    }

    def apply(c: Chain[Int]): IntArraySetBuilder = {
        val isb = new IntArraySetBuilder(new Array[Int](4), 0)
        c.foreach(isb.+=)
        isb
    }
}

object IntArraySet {

    def empty: IntArraySet = EmptyIntArraySet

    def apply(i: Int): IntArraySet = new IntArraySet1(i)

    def apply(i1: Int, i2: Int): IntArraySet = {
        if (i1 < i2) new IntArraySet2(i1, i2)
        else if (i1 == i2) new IntArraySet1(i1)
        else IntArraySet2(i2, i1)
    }

    def apply(i1: Int, i2: Int, i3: Int): IntArraySet = {
        if (i1 == i2)
            return apply(i2, i3);
        if (i1 == i3 || i2 == i3)
            return if (i1 < i2) new IntArraySet2(i1, i2) else new IntArraySet2(i2, i1);

        //... all three values are different
        var v0 = 0
        var v1 = 0
        if (i1 < i2) {
            v0 = i1
            v1 = i2
        } else {
            v0 = i2
            v1 = i1
        }
        if (i3 < v1) {
            if (i3 < v0) new IntArraySet3(i3, v0, v1)
            else new IntArraySet3(v0, i3, v1)
        } else {
            new IntArraySet3(v0, v1, i3)
        }
    }

}
