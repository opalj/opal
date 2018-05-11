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
import java.util.function.IntConsumer

import scala.collection.AbstractIterator
import scala.collection.mutable.Builder

/**
 * A sorted set of integer values backed by an ordered array to store the values; this
 * guarantees log2(n) lookup.
 *
 * @author Michael Eichberg
 */
sealed abstract class IntArraySet
    extends ((Int) ⇒ Int)
    with IntSet[IntArraySet]
    with IntCollectionWithStableOrdering[IntArraySet] {

    /**
     * Returns each pairing of two values. I.e., if the set contains 1, 4, 8, the pairings
     * (1,4),(1,8) and (4,8) will be returned; the pairings (4,1) etc. will not be returned.
     * The order between the two values is not defined.
     */
    def foreachPair[U](f: (Int, Int) ⇒ U): Unit

    def reverseIntIterator: IntIterator

    def min: Int
    def max: Int
    final def last: Int = max

    final override def head: Int = min

    final override def toString: String = mkString("IntArraySet(", ",", ")")
}

case object EmptyIntArraySet extends IntArraySet {
    override def apply(index: Int): Int = throw new IndexOutOfBoundsException("empty")
    override def isSingletonSet: Boolean = false
    override def hasMultipleElements: Boolean = false
    override def isEmpty: Boolean = true
    override def size: Int = 0
    override def max: Int = throw new UnsupportedOperationException("empty set")
    override def min: Int = throw new UnsupportedOperationException("empty set")
    override def foreach(f: IntConsumer): Unit = {}
    override def foreachPair[U](f: (Int, Int) ⇒ U): Unit = {}
    override def withFilter(p: (Int) ⇒ Boolean): IntArraySet = this
    override def map(f: Int ⇒ Int): IntArraySet = this
    override def map(map: Array[Int]): IntArraySet = this
    override def flatMap(f: Int ⇒ IntArraySet): IntArraySet = this
    override def -(i: Int): this.type = this
    override def subsetOf(other: IntArraySet): Boolean = true
    override def +(i: Int): IntArraySet1 = new IntArraySet1(i)
    override def iterator: Iterator[Int] = Iterator.empty
    override def intIterator: IntIterator = IntIterator.empty
    override def reverseIntIterator: IntIterator = IntIterator.empty
    override def contains(value: Int): Boolean = false
    override def exists(p: Int ⇒ Boolean): Boolean = false
    override def foldLeft[B](z: B)(f: (B, Int) ⇒ B): B = z
    override def forall(f: Int ⇒ Boolean): Boolean = true

    override def toChain: Chain[Int] = Naught

    override def equals(other: Any): Boolean = {
        other match {
            case is: IntArraySet ⇒ is.isEmpty
            case _               ⇒ false
        }
    }

    override def hashCode: Int = 1 // compatible to Arrays.hashCode
}

case class IntArraySet1(i: Int) extends IntArraySet {
    override def apply(index: Int): Int = {
        if (index == 0) i else throw new IndexOutOfBoundsException()
    }
    override def isEmpty: Boolean = false
    override def isSingletonSet: Boolean = true
    override def hasMultipleElements: Boolean = false
    override def foreach(f: IntConsumer): Unit = { f.accept(i) }
    override def foreachPair[U](f: (Int, Int) ⇒ U): Unit = {}
    override def max: Int = this.i
    override def min: Int = this.i
    override def withFilter(p: (Int) ⇒ Boolean): IntArraySet = if (p(i)) this else EmptyIntArraySet
    override def map(f: Int ⇒ Int): IntArraySet = {
        val i = this.i
        val newI = f(i)
        if (newI != i)
            new IntArraySet1(newI)
        else
            this
    }
    override def map(map: Array[Int]): IntArraySet = {
        val mappedI = map(i)
        if (mappedI == i)
            this
        else
            new IntArraySet1(mappedI)
    }
    override def flatMap(f: Int ⇒ IntArraySet): IntArraySet = f(i)
    override def -(i: Int): IntArraySet = if (this.i != i) this else EmptyIntArraySet
    override def +(i: Int): IntArraySet = {
        val thisI = this.i
        if (thisI == i)
            this
        else if (thisI < i)
            new IntArraySet2(thisI, i)
        else
            new IntArraySet2(i, thisI)
    }
    override def iterator: Iterator[Int] = Iterator.single(i)
    override def intIterator: IntIterator = IntIterator(i)
    override def reverseIntIterator: IntIterator = IntIterator(i)
    override def size: Int = 1

    override def contains(value: Int): Boolean = value == i
    override def exists(p: Int ⇒ Boolean): Boolean = p(i)
    override def foldLeft[B](z: B)(f: (B, Int) ⇒ B): B = f(z, i)
    override def forall(f: Int ⇒ Boolean): Boolean = f(i)

    override def toChain: Chain[Int] = new :&:[Int](i)

    override def equals(other: Any): Boolean = {
        other match {
            case is: IntArraySet ⇒ is.isSingletonSet && is.min == i
            case _               ⇒ false
        }
    }

    override def hashCode: Int = 31 + i // compatible to Arrays.hashCode
}

/**
 * Represents an orderd set of two values where i1 has to be smaller than i2.
 */
private[immutable] case class IntArraySet2(i1: Int, i2: Int) extends IntArraySet {

    assert(i1 < i2)

    override def apply(index: Int): Int = {
        index match {
            case 0 ⇒ i1
            case 1 ⇒ i2
            case _ ⇒ throw new IndexOutOfBoundsException()
        }
    }
    override def isEmpty: Boolean = false
    override def isSingletonSet: Boolean = false
    override def hasMultipleElements: Boolean = true
    override def size: Int = 2
    override def min: Int = this.i1
    override def max: Int = this.i2
    override def iterator: Iterator[Int] = new AbstractIterator[Int] {
        private[this] var i = 0
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
    override def intIterator: IntIterator = {
        new IntIterator {
            private[this] var i = 0
            def hasNext: Boolean = i < 2
            def next: Int = {
                val v = if (i == 0) i1 else i2
                i += 1
                v
            }
        }
    }
    override def reverseIntIterator: IntIterator = IntIterator(i2, i1)
    override def foreach(f: IntConsumer): Unit = { f.accept(i1); f.accept(i2) }
    override def foreachPair[U](f: (Int, Int) ⇒ U): Unit = f(i1, i2)

    override def withFilter(p: (Int) ⇒ Boolean): IntArraySet = {
        if (p(i1)) {
            if (p(i2)) this
            else new IntArraySet1(i1)
        } else {
            if (p(i2)) new IntArraySet1(i2)
            else
                EmptyIntArraySet
        }
    }
    override def map(f: Int ⇒ Int): IntArraySet = {
        val i1 = this.i1
        val newI1 = f(i1)
        val i2 = this.i2
        val newI2 = f(i2)
        if (newI1 != i1 || newI2 != i2)
            IntArraySet(newI1, newI2) // ensures invariant
        else
            this
    }
    override def map(map: Array[Int]): IntArraySet = {
        IntArraySet2(map(i1), map(i2))
    }
    override def flatMap(f: Int ⇒ IntArraySet): IntArraySet = f(i1) ++ f(i2)
    override def -(i: Int): IntArraySet = {
        if (i == i1) new IntArraySet1(i2)
        else if (i == i2) new IntArraySet1(i1)
        else this
    }
    override def +(i: Int): IntArraySet = {
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
    override def contains(value: Int): Boolean = value == i1 || value == i2
    override def exists(p: Int ⇒ Boolean): Boolean = p(i1) || p(i2)
    override def foldLeft[B](z: B)(f: (B, Int) ⇒ B): B = f(f(z, i1), i2)
    override def forall(f: Int ⇒ Boolean): Boolean = f(i1) && f(i2)

    override def toChain: Chain[Int] = i1 :&: i2 :&: Naught

    override def equals(other: Any): Boolean = {
        other match {
            case is: IntArraySet ⇒ is.size == 2 && is.min == this.i1 && is.max == this.i2
            case _               ⇒ false
        }
    }

    override def hashCode: Int = 31 * (31 + i1) + i2 // compatible to Arrays.hashCode
}

/**
 * Represents an orderd set of three int values: i1 < i2 < i3.
 */
private[immutable] case class IntArraySet3(i1: Int, i2: Int, i3: Int) extends IntArraySet {

    assert(i1 < i2, s"i1 < i2: $i1 >= $i2")
    assert(i2 < i3, s"i2 < i3: $i2 >= $i3")

    override def apply(index: Int): Int = {
        index match {
            case 0 ⇒ i1
            case 1 ⇒ i2
            case 2 ⇒ i3
            case _ ⇒ throw new IndexOutOfBoundsException()
        }
    }
    override def isEmpty: Boolean = false
    override def isSingletonSet: Boolean = false
    override def hasMultipleElements: Boolean = true
    override def size: Int = 3
    override def min: Int = this.i1
    override def max: Int = this.i3

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
    override def intIterator: IntIterator = new IntIterator {
        private[this] var i = 0
        def hasNext: Boolean = i < 3
        def next: Int = {
            val v = if (i == 0) i1 else if (i == 1) i2 else i3
            i += 1
            v
        }
    }
    override def reverseIntIterator: IntIterator = IntIterator(i3, i2, i1)
    override def foreach(f: IntConsumer): Unit = { f.accept(i1); f.accept(i2); f.accept(i3) }
    override def foreachPair[U](f: (Int, Int) ⇒ U): Unit = { f(i1, i2); f(i1, i3); f(i2, i3) }

    override def withFilter(p: (Int) ⇒ Boolean): IntArraySet = {
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
    override def map(f: Int ⇒ Int): IntArraySet = {
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
    override def map(map: Array[Int]): IntArraySet = {
        IntArraySet3(map(i1), map(i2), map(i3))
    }
    override def flatMap(f: Int ⇒ IntArraySet): IntArraySet = f(i1) ++ f(i2) ++ f(i3)

    override def -(i: Int): IntArraySet = {
        if (i1 == i) new IntArraySet2(i2, i3)
        else if (i2 == i) new IntArraySet2(i1, i3)
        else if (i3 == i) new IntArraySet2(i1, i2)
        else this
    }
    override def +(i: Int): IntArraySet = {
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
    override def contains(value: Int): Boolean = value == i1 || value == i2 || value == i3
    override def exists(p: Int ⇒ Boolean): Boolean = p(i1) || p(i2) || p(i3)
    override def foldLeft[B](z: B)(f: (B, Int) ⇒ B): B = f(f(f(z, i1), i2), i3)
    override def forall(f: Int ⇒ Boolean): Boolean = f(i1) && f(i2) && f(i3)

    override def toChain: Chain[Int] = i1 :&: i2 :&: i3 :&: Naught

    override def equals(other: Any): Boolean = {
        other match {
            case that: IntArraySet ⇒
                that.size == 3 && this.i1 == that(0) && this.i2 == that(1) && this.i3 == that(2)
            case _ ⇒ false
        }
    }

    override def hashCode: Int = 31 * (31 * (31 + i1) + i2) + i3 // compatible to Arrays.hashCode
}

case class IntArraySetN private[immutable] (
        private[immutable] val is: Array[Int]
) extends IntArraySet {

    assert(is.length > 3)

    override def apply(index: Int): Int = is(index)
    override def size: Int = is.length
    override def isSingletonSet: Boolean = false
    override def hasMultipleElements: Boolean = true
    override def isEmpty: Boolean = false
    override def max: Int = is(is.length - 1)
    override def min: Int = is(0)

    override def foreach(f: IntConsumer): Unit = {
        val max = is.length
        var i = 0
        while (i < max) {
            f.accept(is(i))
            i += 1
        }
    }
    override def foreachPair[U](f: (Int, Int) ⇒ U): Unit = {
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

    override def withFilter(p: (Int) ⇒ Boolean): IntArraySet = {
        new FilteredIntArraySet(p, this)
    }

    override def map(f: Int ⇒ Int): IntArraySet = {
        // let's check if all values are mapped to their original values; if so return "this"
        val is = this.is
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

        val isb = new IntArraySetBuilder(max)
        var l = 0
        while (l < i) { isb += is(l) /*the values were unchanged*/ ; l += 1 }
        while (i < max) {
            isb += f(is(i))
            i += 1
        }
        isb.result()
    }
    override def map(map: Array[Int]): IntArraySet = {
        val is = this.is
        val max = is.length
        val isb = new IntArraySetBuilder(max)
        var i = 0
        while (i < max) {
            isb += map(is(i))
            i += 1
        }
        isb.result()
    }

    override def flatMap(f: Int ⇒ IntArraySet): IntArraySet = {
        foldLeft(EmptyIntArraySet: IntArraySet)(_ ++ f(_))
    }

    override def -(i: Int): IntArraySet = {
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
        } else {
            this
        }
    }

    override def +(i: Int): IntArraySet = {
        val index = Arrays.binarySearch(is, 0, size, i)
        if (index < 0) {
            val insertionPoint = -index - 1
            // the element is NOT already found
            val targetIs = new Array[Int](is.length + 1)
            System.arraycopy(is, 0, targetIs, 0, insertionPoint)
            targetIs(insertionPoint) = i
            val count = is.length - insertionPoint
            System.arraycopy(is, insertionPoint, targetIs, insertionPoint + 1, count)
            new IntArraySetN(targetIs)
        } else {
            this
        }
    }

    override def contains(value: Int): Boolean = {
        Arrays.binarySearch(is, 0, size, value) >= 0
    }

    override def exists(p: Int ⇒ Boolean): Boolean = {
        var i = 0
        val data = this.is
        val max = data.length
        while (i < max) { if (p(data(i))) return true; i += 1 }
        false
    }

    override def foldLeft[B](z: B)(f: (B, Int) ⇒ B): B = {
        var i = 0
        val data = this.is
        val max = data.length
        var r = z
        while (i < max) { r = f(r, data(i)); i += 1 }
        r
    }

    override def forall(p: Int ⇒ Boolean): Boolean = {
        var i = 0
        val data = this.is
        val max = data.length
        while (i < max) { if (!p(data(i))) return false; i += 1 }
        true
    }

    override def iterator: Iterator[Int] = is.iterator
    override def intIterator: IntIterator = new IntIterator {
        private[this] var i = 0
        def hasNext: Boolean = i < is.length
        def next(): Int = { val i = this.i; this.i = i + 1; is(i) }
    }
    override def reverseIntIterator: IntIterator = new IntIterator {
        private[this] var i = is.length - 1
        def hasNext: Boolean = i >= 0
        def next(): Int = { val i = this.i; this.i = i - 1; is(i) }
    }

    override def toChain: Chain[Int] = {
        val cb = new Chain.ChainBuilder[Int]()
        foreach((i: Int) ⇒ cb += i)
        cb.result()
    }

    override def equals(other: Any): Boolean = {
        other match {
            case that: IntArraySet ⇒ that.size == this.size && this.subsetOf(that)
            case _                 ⇒ false
        }
    }

    override def hashCode: Int = Arrays.hashCode(is)
}

private[immutable] class FilteredIntArraySet(
        p: Int ⇒ Boolean, origS: IntArraySetN
) extends IntArraySet {

    @volatile private[this] var filteredS: IntArraySet = _

    private[this] def getFiltered: IntArraySet = {
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
                            case 3 ⇒ new IntArraySet3(targetIs(0), targetIs(1), targetIs(2))
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

    override def apply(index: Int): Int = getFiltered.apply(index)

    override def withFilter(p: (Int) ⇒ Boolean): IntArraySet = {
        if (filteredS ne null) {
            filteredS.withFilter(p)
        } else {
            new FilteredIntArraySet((i: Int) ⇒ this.p(i) && p(i), origS)
        }
    }

    override def intIterator: IntIterator = {
        if (filteredS ne null) {
            filteredS.intIterator
        } else {
            origS.intIterator.filter(p)
        }
    }

    override def reverseIntIterator: IntIterator = {
        if (filteredS ne null) {
            filteredS.reverseIntIterator
        } else {
            origS.reverseIntIterator.filter(p)
        }
    }

    override def iterator: Iterator[Int] = {
        if (filteredS ne null) {
            filteredS.iterator
        } else {
            origS.iterator.filter(p)
        }
    }

    override def foreach(f: IntConsumer): Unit = {
        if (filteredS ne null) {
            filteredS.foreach(f)
        } else {
            val is = origS.is
            val max = is.length
            var j = 0
            while (j < max) {
                val i = is(j)
                if (p(i)) f.accept(i)
                j += 1
            }
        }
    }
    override def foreachPair[U](f: (Int, Int) ⇒ U): Unit = getFiltered.foreachPair(f)

    override def contains(value: Int): Boolean = p(value) && origS.contains(value)
    override def exists(p: Int ⇒ Boolean): Boolean = intIterator.exists(p)
    override def foldLeft[B](z: B)(f: (B, Int) ⇒ B): B = intIterator.foldLeft(z)(f)
    override def forall(p: Int ⇒ Boolean): Boolean = intIterator.forall(p)

    override def size: Int = getFiltered.size
    override def isSingletonSet: Boolean = getFiltered.isSingletonSet
    override def hasMultipleElements: Boolean = getFiltered.hasMultipleElements
    override def isEmpty: Boolean = getFiltered.isEmpty
    override def min: Int = getFiltered.min
    override def max: Int = getFiltered.max
    override def map(f: Int ⇒ Int): IntArraySet = getFiltered.map(f)
    override def map(map: Array[Int]): IntArraySet = getFiltered.map(map)
    override def flatMap(f: Int ⇒ IntArraySet): IntArraySet = getFiltered.flatMap(f)
    override def -(i: Int): IntArraySet = getFiltered - i
    override def +(i: Int): IntArraySet = getFiltered + 1
    override def toChain: Chain[Int] = intIterator.toChain
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

    def this(initialSize: Int) {
        this(new Array[Int](Math.max(initialSize, 4)), 0)
    }

    override def +=(elem: Int): this.type = {
        import System.arraycopy
        val index = Arrays.binarySearch(is, 0, size, elem)
        if (index < 0) {
            // the element is NOT already found
            size += 1
            val insertionPoint = -index - 1
            if ( /*new*/ size <= is.length) { // we have enough space
                arraycopy(is, insertionPoint, is, insertionPoint + 1, (size - 1) - insertionPoint)
                is(insertionPoint) = elem
            } else {
                val targetIs = new Array[Int](is.length * 2)
                arraycopy(is, 0, targetIs, 0, insertionPoint)
                targetIs(insertionPoint) = elem
                val count = is.length - insertionPoint
                arraycopy(is, insertionPoint, targetIs, insertionPoint + 1, count)
                is = targetIs
            }
        }

        this
    }

    def ++=(elems: IntArraySet): this.type = { elems.foreach(this.+=); this }

    override def clear(): Unit = { is = new Array[Int](4); size = 0 }

    override def result(): IntArraySet = {
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

    override def toString: String = {
        is.mkString(s"IntArraySetBuilder(size=$size;values={", ",", "})")
    }
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

    /** Constructs a sorted IntArraySet ''potentially using the given data-structure''. */
    def fromSortedArray(data: Array[Int]): IntArraySet = {
        data.length match {
            case 0     ⇒ EmptyIntArraySet
            case 1     ⇒ new IntArraySet1(data(0))
            case 2     ⇒ new IntArraySet2(data(0), data(1))
            case 3     ⇒ new IntArraySet3(data(0), data(1), data(2))
            case size0 ⇒ new IntArraySetN(data)
        }
    }

}
