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
import scala.collection.mutable.Builder

/**
 * A sorted set of integer values. Conceptually, an ordered array is used to store the values; this
 * guarantees log(n) lookup.
 *
 * @author Michael Eichberg
 */
abstract class IntSet {

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

    def foreach[U](f: Int ⇒ U): Unit

    /**
     * Returns a lazily filtered set. However, all operations other operations except of
     * `withFilter` and `iterator` will force the evaluation.
     */
    def withFilter(p: (Int) ⇒ Boolean): IntSet

    def map(f: Int ⇒ Int): IntSet

    def transform[T, To](f: Int ⇒ T, b: Builder[T, To]): To = {
        foreach(i ⇒ b += f(i))
        b.result()
    }

    def -(i: Int): IntSet

    def +(i: Int): IntSet

    def subsetOf(other: IntSet): Boolean = {
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

    def contains(value: Int): Boolean = iterator.contains(value)

    def exists(p: Int ⇒ Boolean): Boolean = iterator.exists(p)

    def foldLeft[B](z: B)(f: (B, Int) ⇒ B): B = iterator.foldLeft(z)(f)

    def forall(f: Int ⇒ Boolean): Boolean = iterator.forall(f)

    def ++(that: IntSet): IntSet = {
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

    final override def toString = mkString("IntSet(", ",", ")")
}

case object EmptyIntSet extends IntSet {
    def isSingletonSet: Boolean = false
    def hasMultipleElements: Boolean = false
    def isEmpty: Boolean = true
    def size: Int = 0
    def max: Int = throw new UnsupportedOperationException("empty set")
    def min: Int = throw new UnsupportedOperationException("empty set")
    def foreach[U](f: Int ⇒ U): Unit = {}
    def withFilter(p: (Int) ⇒ Boolean): IntSet = this
    def map(f: Int ⇒ Int): IntSet = this
    def -(i: Int): this.type = this
    override def subsetOf(other: IntSet): Boolean = true
    def +(i: Int): IntSet1 = new IntSet1(i)
    def iterator: Iterator[Int] = Iterator.empty
    override def contains(value: Int): Boolean = false
    override def exists(p: Int ⇒ Boolean): Boolean = false
    override def foldLeft[B](z: B)(f: (B, Int) ⇒ B): B = z
    override def forall(f: Int ⇒ Boolean): Boolean = true

    def toChain: Chain[Int] = Naught

    override def equals(other: Any): Boolean = {
        other match {
            case is: IntSet ⇒ is.isEmpty
            case _          ⇒ false
        }
    }

    override def hashCode: Int = 0 // compatible to Arrays.hashCode
}

case class IntSet1(i: Int) extends IntSet {
    def isEmpty: Boolean = false
    def isSingletonSet: Boolean = true
    def hasMultipleElements: Boolean = false
    def foreach[U](f: Int ⇒ U): Unit = { f(i) }
    def max: Int = this.i
    def min: Int = this.i
    def withFilter(p: (Int) ⇒ Boolean): IntSet = if (p(i)) this else EmptyIntSet
    def map(f: Int ⇒ Int): IntSet = new IntSet1(f(i))
    def -(i: Int): IntSet = {
        if (this.i != i) this else EmptyIntSet
    }
    def +(i: Int): IntSet = {
        if (this.i == i) this else if (this.i < i) new IntSet2(this.i, i) else new IntSet2(i, this.i)
    }
    def iterator: Iterator[Int] = Iterator(i)
    def size: Int = 1

    override def contains(value: Int): Boolean = value == i
    override def exists(p: Int ⇒ Boolean): Boolean = p(i)
    override def foldLeft[B](z: B)(f: (B, Int) ⇒ B): B = f(z, i)
    override def forall(f: Int ⇒ Boolean): Boolean = f(i)

    def toChain: Chain[Int] = new :&:[Int](i)

    override def equals(other: Any): Boolean = {
        other match {
            case is: IntSet if is.isSingletonSet ⇒ is.min == i
            case _                               ⇒ false
        }
    }

    override def hashCode: Int = 31 + i // compatible to Arrays.hashCode
}

case class IntSet2(i1: Int, i2: Int) extends IntSet {

    require(i1 < i2)

    def isEmpty: Boolean = false
    def isSingletonSet: Boolean = false
    def hasMultipleElements: Boolean = true
    def size: Int = 2
    def min: Int = this.i1
    def max: Int = this.i2

    def iterator: Iterator[Int] = Iterator(i1, i2)
    def foreach[U](f: Int ⇒ U): Unit = { f(i1); f(i2) }

    def withFilter(p: (Int) ⇒ Boolean): IntSet = {
        if (p(i1)) {
            if (p(i2)) this
            else new IntSet1(i1)
        } else {
            if (p(i2)) new IntSet1(i2)
            else
                EmptyIntSet
        }
    }
    def map(f: Int ⇒ Int): IntSet = IntSet(f(i1), f(i2)) // ensures invariant
    def -(i: Int): IntSet = {
        if (i <= i1) {
            if (i == i1) new IntSet1(i2)
            else this
        } else if (i <= i2) {
            if (i == i2) new IntSet1(i1)
            else this
        } else {
            this
        }
    }
    def +(i: Int): IntSet = {
        if (i <= i1) {
            if (i == i1) this
            else new IntArraySet(Array[Int](i, i1, i2))
        } else if (i <= i2) {
            if (i == i2) this
            else new IntArraySet(Array[Int](i1, i, i2))
        } else {
            new IntArraySet(Array[Int](i1, i2, i))
        }
    }
    override def contains(value: Int): Boolean = value == i1 || value == i2
    override def exists(p: Int ⇒ Boolean): Boolean = p(i1) || p(i2)
    override def foldLeft[B](z: B)(f: (B, Int) ⇒ B): B = f(f(z, i1), i2)
    override def forall(f: Int ⇒ Boolean): Boolean = f(i1) && f(i2)

    def toChain: Chain[Int] = i1 :&: i2 :&: Naught

    override def equals(other: Any): Boolean = {
        other match {
            case is: IntSet if is.size == 2 ⇒ is.min == this.i1 && is.max == this.i2
            case _                          ⇒ false
        }
    }

    override def hashCode: Int = 31 * (31 + i1) + i2 // compatible to Arrays.hashCode
}

case class IntArraySet private[immutable] (private[immutable] val is: Array[Int]) extends IntSet {

    require(is.length > 2)
    def size: Int = is.length
    def isSingletonSet: Boolean = false
    def hasMultipleElements: Boolean = true
    def isEmpty: Boolean = false
    def max: Int = is(size - 1)
    def min: Int = is(0)
    def foreach[U](f: Int ⇒ U): Unit = {
        val max = is.length
        var i = 0
        while (i < max) {
            f(is(i))
            i += 1
        }
    }
    def withFilter(p: (Int) ⇒ Boolean): IntSet = new FilteredIntArraySet(p, this)

    def map(f: Int ⇒ Int): IntSet = {
        val isb = new IntSetBuilder
        val max = is.length
        var i = 0
        while (i < max) {
            isb += f(is(i))
            i += 1
        }
        isb.result
    }

    def -(i: Int): IntSet = {
        val index = Arrays.binarySearch(is, 0, size, i)
        if (index >= 0) {
            if (is.length == 3) {
                index match {
                    case 0 ⇒ new IntSet2(is(1), is(2))
                    case 1 ⇒ new IntSet2(is(0), is(2))
                    case 2 ⇒ new IntSet2(is(0), is(1))
                }
            } else {
                // the element is found
                val targetIs = new Array[Int](is.length - 1)
                System.arraycopy(is, 0, targetIs, 0, index)
                System.arraycopy(is, index + 1, targetIs, index, is.length - 1 - index)
                new IntArraySet(targetIs)
            }
        } else
            this
    }

    def +(i: Int): IntSet = {
        val index = Arrays.binarySearch(is, 0, size, i)
        if (index < 0) {
            val insertionPoint = -index - 1
            // the element is NOT already found
            val targetIs = new Array[Int](is.length + 1)
            System.arraycopy(is, 0, targetIs, 0, insertionPoint)
            targetIs(insertionPoint) = i
            System.arraycopy(is, insertionPoint, targetIs, insertionPoint + 1, is.length - insertionPoint)
            new IntArraySet(targetIs)
        } else {
            this
        }
    }

    def iterator: Iterator[Int] = is.iterator

    def toChain: Chain[Int] = {
        val cb = new Chain.ChainBuilder[Int]()
        foreach(i ⇒ cb += i)
        cb.result()
    }

    override def equals(other: Any): Boolean = {
        other match {
            case that: IntSet if that.size == this.size ⇒ this.subsetOf(that)
            case _                                      ⇒ false
        }
    }

    override def hashCode: Int = Arrays.hashCode(is)
}

private[immutable] class FilteredIntArraySet(
        p:     Int ⇒ Boolean,
        origS: IntArraySet
) extends IntSet {

    @volatile private[this] var filteredS: IntSet = null

    private def getFiltered: IntSet = {
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
                            case 0 ⇒ EmptyIntSet
                            case 1 ⇒ new IntSet1(targetIs(0))
                            case 2 ⇒ new IntSet2(targetIs(0), targetIs(1))
                            case _ ⇒
                                if (targetIsIndex == max) // no value was filtered...
                                    origS
                                else {
                                    new IntArraySet(Arrays.copyOf(targetIs, targetIsIndex))
                                }

                        }
                    }
                }
            }
        }
        filteredS
    }

    def withFilter(p: (Int) ⇒ Boolean): IntSet = {
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

    def size: Int = getFiltered.size
    def isSingletonSet: Boolean = getFiltered.isSingletonSet
    def hasMultipleElements: Boolean = getFiltered.hasMultipleElements
    def isEmpty: Boolean = getFiltered.isEmpty
    def min: Int = getFiltered.min
    def max: Int = getFiltered.max
    def map(f: Int ⇒ Int): IntSet = getFiltered.map(f)
    def -(i: Int): IntSet = getFiltered - i
    def +(i: Int): IntSet = getFiltered + 1
    def toChain: Chain[Int] = getFiltered.toChain
    override def equals(other: Any): Boolean = getFiltered.equals(other)
    override def hashCode: Int = getFiltered.hashCode
}

class IntSetBuilder private[immutable] (
        private[this] var is:   Array[Int],
        private[this] var size: Int
) extends Builder[Int, IntSet] {

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

    def clear(): Unit = { is = new Array[Int](4); size = 0 }

    def result(): IntSet = {
        size match {
            case 0 ⇒ EmptyIntSet
            case 1 ⇒ new IntSet1(is(0))
            case 2 ⇒ new IntSet2(is(0), is(1))
            case _ ⇒
                if (size == is.length)
                    new IntArraySet(is)
                else {
                    val targetIs = new Array[Int](size)
                    System.arraycopy(is, 0, targetIs, 0, size)
                    new IntArraySet(targetIs)
                }

        }
    }

    override def toString: String = is.mkString(s"IntSetBuilder(size=$size;values={", ",", "})")
}

object IntSetBuilder {

    def apply(vs: Int*): IntSetBuilder = {
        val isb = new IntSetBuilder(new Array[Int](vs.size), 0)
        vs.foreach(isb.+=)
        isb
    }

    def apply(vs: Set[Int]): IntSetBuilder = {
        val isb = new IntSetBuilder(new Array[Int](Math.max(4, vs.size)), 0)
        vs.foreach(isb.+=)
        isb
    }

    def apply(c: Chain[Int]): IntSetBuilder = {
        val isb = new IntSetBuilder(new Array[Int](4), 0)
        c.foreach(isb.+=)
        isb
    }
}

object IntSet {

    def empty: IntSet = EmptyIntSet

    def apply(i: Int): IntSet = new IntSet1(i)

    def apply(i1: Int, i2: Int): IntSet = {
        if (i1 < i2) new IntSet2(i1, i2)
        else if (i1 == i2) new IntSet1(i1)
        else IntSet2(i2, i1)
    }

}
