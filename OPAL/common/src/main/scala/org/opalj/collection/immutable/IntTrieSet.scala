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
    with IntWorkSet[IntTrieSet] {

    /** Returns some value and removes it from this set. */
    def getAndRemove: (Int, IntTrieSet)

    final override def toString: String = mkString("IntTrieSet(", ",", ")")

    private[immutable] def +(i: Int, level: Int): IntTrieSet
    private[immutable] def -(i: Int, key: Int): IntTrieSet
    private[immutable] def constringe(): IntTrieSet
    private[immutable] def contains(value: Int, key: Int): Boolean = this.contains(value)
}

object IntTrieSet {

    def empty: IntTrieSet = EmptyIntTrieSet
}

/** The (potential) leaves of an IntTrie. */
private[immutable] abstract class IntTrieSetL extends IntTrieSet {

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
    override def getAndRemove: (Int, IntTrieSet) = throw new UnsupportedOperationException("empty")
    override def foreach[U](f: Int ⇒ U): Unit = {}
    override def withFilter(p: (Int) ⇒ Boolean): IntTrieSet = this
    override def map(f: Int ⇒ Int): IntTrieSet = this
    override def -(i: Int): this.type = this
    override def subsetOf(other: IntTrieSet): Boolean = true
    override def +(i: Int): IntTrieSet1 = new IntTrieSet1(i)
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
}

case class IntTrieSet1(i: Int) extends IntTrieSetL {
    override def isEmpty: Boolean = false
    override def isSingletonSet: Boolean = true
    override def hasMultipleElements: Boolean = false
    override def size: Int = 1
    override def foreach[U](f: Int ⇒ U): Unit = { f(i) }
    override def getAndRemove: (Int, IntTrieSet) = (i, EmptyIntTrieSet)
    override def withFilter(p: (Int) ⇒ Boolean): IntTrieSet = if (p(i)) this else EmptyIntTrieSet
    override def map(f: Int ⇒ Int): IntTrieSet = {
        val newI = f(i)
        if (newI != i)
            new IntTrieSet1(newI)
        else
            this
    }
    override def flatMap(f: Int ⇒ IntTrieSet): IntTrieSet = f(i)
    override def head: Int = i
    override def -(i: Int): IntTrieSet = if (this.i != i) this else EmptyIntTrieSet
    override def +(i: Int): IntTrieSet = if (this.i == i) this else IntTrieSet2.from(this.i, i)
    override def iterator: Iterator[Int] = Iterator.single(i)
    override def intIterator: IntIterator = IntIterator(i)
    override def subsetOf(other: IntTrieSet): Boolean = other.contains(i)
    override def contains(value: Int): Boolean = value == i
    override def exists(p: Int ⇒ Boolean): Boolean = p(i)
    override def foldLeft[B](z: B)(f: (B, Int) ⇒ B): B = f(z, i)
    override def forall(f: Int ⇒ Boolean): Boolean = f(i)

    override def toChain: Chain[Int] = new :&:[Int](i)

    override def equals(other: Any): Boolean = {
        other match {
            case that: IntTrieSet ⇒ that.size == 1 && this.i == that.head
            case _                ⇒ false
        }
    }

    override def hashCode: Int = 31 + i // compatible to Arrays.hashCode

    override private[immutable] def +(i: Int, level: Int): IntTrieSet = this.+(i)
}

/**
 * Represents an ordered set of two values where i1 has to be smaller than i2.
 */
private[immutable] class IntTrieSet2 private[immutable] (
        i1: Int, i2: Int
) extends IntTrieSetL {

    override def isEmpty: Boolean = false
    override def isSingletonSet: Boolean = false
    override def hasMultipleElements: Boolean = true
    override def size: Int = 2
    override def head: Int = i2
    override def getAndRemove: (Int, IntTrieSet) = (i2, new IntTrieSet1(i1))

    override def iterator: Iterator[Int] = new AbstractIterator[Int] {
        private[this] var i = 0
        def hasNext: Boolean = i < 2
        def next: Int = {
            val v = i
            i = v + 1
            v match {
                case 0 ⇒ i1
                case 1 ⇒ i2
                case _ ⇒ throw new IllegalStateException()
            }
        }
    }
    override def intIterator: IntIterator = IntIterator(i1, i2)

    override def foreach[U](f: Int ⇒ U): Unit = { f(i1); f(i2) }
    override def withFilter(p: (Int) ⇒ Boolean): IntTrieSet = {
        if (p(i1)) {
            if (p(i2))
                this
            else
                new IntTrieSet1(i1)
        } else {
            if (p(i2))
                new IntTrieSet1(i2)
            else
                EmptyIntTrieSet
        }
    }
    override def map(f: Int ⇒ Int): IntTrieSet = {
        val i1 = this.i1
        val newI1 = f(i1)
        val i2 = this.i2
        val newI2 = f(i2)
        if (newI1 != i1 || newI2 != i2)
            IntTrieSet2(newI1, newI2)
        else
            this
    }
    override def flatMap(f: Int ⇒ IntTrieSet): IntTrieSet = f(i1) ++ f(i2)

    override def -(i: Int): IntTrieSet = {
        if (i == i1) new IntTrieSet1(i2)
        else if (i == i2) new IntTrieSet1(i1)
        else this
    }
    override def +(i: Int): IntTrieSet = if (i1 == i | i2 == i) this else IntTrieSet3.from(i1, i2, i)
    override def contains(value: Int): Boolean = value == i1 || value == i2
    override def exists(p: Int ⇒ Boolean): Boolean = p(i1) || p(i2)
    override def foldLeft[B](z: B)(f: (B, Int) ⇒ B): B = f(f(z, i1), i2)
    override def forall(f: Int ⇒ Boolean): Boolean = f(i1) && f(i2)

    override def toChain: Chain[Int] = i1 :&: i2 :&: Naught

    override def equals(other: Any): Boolean = {
        other match {
            case that: IntTrieSet ⇒ that.size == 2 && that.contains(i1) && that.contains(i2)
            case _                ⇒ false
        }
    }

    override def hashCode: Int = 31 * (31 + i1) + i2 // compatible to Arrays.hashCode

    override private[immutable] def +(i: Int, level: Int): IntTrieSet = this.+(i)
}

object IntTrieSet2 {

    def apply(i1: Int, i2: Int): IntTrieSet = {
        if (i1 == i2)
            new IntTrieSet1(i1)
        else {
            from(i1, i2)
        }
    }

    /** Constructs a new IntTrie from the two distinct(!) values. */
    private[immutable] def from(i1: Int, i2: Int): IntTrieSet = {
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
}

/**
 * Represents an ordered set of three int values: i1 < i2 < i3.
 */
private[immutable] class IntTrieSet3 private[immutable] (
        i1: Int, i2: Int, i3: Int
) extends IntTrieSetL {

    override def isEmpty: Boolean = false
    override def isSingletonSet: Boolean = false
    override def hasMultipleElements: Boolean = true
    override def size: Int = 3
    override def getAndRemove: (Int, IntTrieSet) = (i3, new IntTrieSet2(i1, i2))
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

    override def foreach[U](f: Int ⇒ U): Unit = { f(i1); f(i2); f(i3) }
    override def withFilter(p: (Int) ⇒ Boolean): IntTrieSet = {
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
                    new IntTrieSet1(i1)
            }
        } else {
            if (p(i2)) {
                if (p(i3))
                    new IntTrieSet2(i2, i3)
                else
                    new IntTrieSet1(i2)
            } else {
                if (p(i3))
                    new IntTrieSet1(i3)
                else
                    EmptyIntTrieSet
            }
        }
    }
    override def map(f: Int ⇒ Int): IntTrieSet = {
        val i1 = this.i1
        val newI1 = f(i1)
        val i2 = this.i2
        val newI2 = f(i2)
        val i3 = this.i3
        val newI3 = f(i3)
        if (newI1 != i1 || newI2 != i2 || newI3 != i3)
            IntTrieSet3(newI1, newI2, newI3) // ensures invariant
        else
            this
    }
    def -(i: Int): IntTrieSet = {
        if (i1 == i) new IntTrieSet2(i2, i3)
        else if (i2 == i) new IntTrieSet2(i1, i3)
        else if (i3 == i) new IntTrieSet2(i1, i2)
        else this
    }
    def +(i: Int): IntTrieSet = this.+(i, 0)
    def contains(value: Int): Boolean = value == i1 || value == i2 || value == i3
    def exists(p: Int ⇒ Boolean): Boolean = p(i1) || p(i2) || p(i3)
    def foldLeft[B](z: B)(f: (B, Int) ⇒ B): B = f(f(f(z, i1), i2), i3)
    def forall(f: Int ⇒ Boolean): Boolean = f(i1) && f(i2) && f(i3)
    def toChain: Chain[Int] = i1 :&: i2 :&: i3 :&: Naught

    override def equals(other: Any): Boolean = {
        other match {
            case that: IntTrieSet ⇒
                that.size == 3 && that.contains(i1) && that.contains(i2) && that.contains(i3)
            case _ ⇒
                false
        }
    }

    override def hashCode: Int = 31 * (31 * (31 + i1) + i2) + i3 // compatible to Arrays.hashCode

    private[immutable] override def +(i: Int, level: Int): IntTrieSet = {
        if (i == i1 || i == i2 || i == i3)
            this
        else
            IntTrieSetN.from(i, i1, i2, i3, level)
    }
}

object IntTrieSet3 {

    def apply(i1: Int, i2: Int, i3: Int): IntTrieSet = {
        if (i1 == i2)
            IntTrieSet2(i1, i3) // this also handles the case i1 == i3
        else if (i1 == i3 || i2 == i3) { // we have i1 =!= i2
            IntTrieSet2.from(i1, i2)
        } else { // i1 =!= i2 && i2 =!= i3 && i1 =!= i3
            IntTrieSet3.from(i1, i2, i3)
        }
    }

    /** Constructs a new IntTrie from the two distinct(!) values! */
    private[immutable] def from(i1: Int, i2: Int, i3: Int): IntTrieSet = {
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
}

private[immutable] final class IntTrieSetN private[immutable] (
        private val left:  IntTrieSet, // can be empty, but never null!
        private val right: IntTrieSet, // can be empty, but never null!
        val size:          Int
) extends IntTrieSet {

    assert(left.size + right.size == size)
    assert(size > 0)

    override def isSingletonSet: Boolean = false
    override def hasMultipleElements: Boolean = true
    override def isEmpty: Boolean = false

    override def head: Int = if (left.nonEmpty) left.head else right.head

    override def exists(p: Int ⇒ Boolean): Boolean = left.exists(p) || right.exists(p)
    override def forall(p: Int ⇒ Boolean): Boolean = left.forall(p) && right.forall(p)

    override def foreach[U](f: Int ⇒ U): Unit = {
        left.foreach(f)
        right.foreach(f)
    }

    override def map(f: Int ⇒ Int): IntTrieSet = foldLeft(EmptyIntTrieSet: IntTrieSet)(_ + f(_))

    override def flatMap(f: Int ⇒ IntTrieSet): IntTrieSet = {
        foldLeft(EmptyIntTrieSet: IntTrieSet)(_ ++ f(_))
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
                new IntTrieSetN(newLeft, right, size + 1)
        } else {
            val right = this.right
            val newRight = right + (i, level + 1)
            if (newRight eq right)
                this
            else
                new IntTrieSetN(left, newRight, size + 1)
        }
    }

    override def +(i: Int): IntTrieSet = this.+(i, 0)

    override private[this] def contains(value: Int, key: Int): Boolean = {
        if ((key & 1) == 0)
            left.contains(value, key >>> 1)
        else
            right.contains(value, key >>> 1)
    }

    override def contains(value: Int): Boolean = this.contains(value, value)

    /** Ensures that the IntTrieSet does not contain paths to empty leaf nodes. */
    override private[immutable] def constringe(): IntTrieSet = {
        assert(size <= 1)
        if (left.isEmpty)
            right.constringe
        else
            left.constringe
    }

    private[immutable] def -(i: Int, key: Int): IntTrieSet = {
        if ((key & 1) == 0) {
            val left = this.left
            val newLeft = left.-(i, key >>> 1)
            if (newLeft eq left)
                this
            else {
                val newSize = size - 1
                if (newSize == 0)
                    EmptyIntTrieSet
                else if (newSize == 1) {
                    if (newLeft.isEmpty)
                        right.constringe
                    else
                        newLeft.constringe
                } else {
                    new IntTrieSetN(newLeft, right, newSize)
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
                        left.constringe
                    else
                        newRight.constringe
                } else {
                    new IntTrieSetN(left, newRight, newSize)
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

            checkIterator()

            def hasNext: Boolean = it.hasNext
            def next(): Int = { val v = it.next; checkIterator(); v }
        }
    }

    def iterator: Iterator[Int] = {
        new AbstractIterator[Int] {
            private[this] val it = intIterator
            override def hasNext = it.hasNext
            override def next() = it.next
        }
    }

    override def getAndRemove: (Int, IntTrieSet) = {
        // try to reduce the tree size by removing an element from the
        // bigger subtree
        val left = this.left
        val right = this.right
        val leftSize = left.size
        val rightSize = right.size
        if (leftSize > rightSize) {
            // => left has at least one element
            if (leftSize == 1) { // => right is empty!
                (left.head, EmptyIntTrieSet)
            } else {
                val (v, newLeft) = left.getAndRemove
                val theNewLeft = if (leftSize == 2) newLeft.constringe else newLeft
                (v, new IntTrieSetN(theNewLeft, right, leftSize - 1 + rightSize))
            }
        } else {
            // ...leftSize <= right.size
            assert(right.nonEmpty)
            if (right.isSingletonSet) {
                // left.size \in {0,1}
                (right.head, left.constringe)
            } else {
                val (v, newRight) = right.getAndRemove
                val theNewRight = if (rightSize == 2) newRight.constringe else newRight
                (v, new IntTrieSetN(left, theNewRight, size - 1))
            }
        }
    }

    override def withFilter(p: (Int) ⇒ Boolean): IntTrieSet = ???

    override def toChain: Chain[Int] = {
        val cb = new Chain.ChainBuilder[Int]()
        foreach(i ⇒ cb += i)
        cb.result()
    }

    override def equals(other: Any): Boolean = {
        other match {
            case that: IntTrieSet ⇒ that.size == this.size && this.subsetOf(that)
            case _                ⇒ false
        }
    }

    override def hashCode: Int = foldLeft(1)(31 * _ + _)

}

object IntTrieSetN {

    def apply(i1: Int, i2: Int, i3: Int, i4: Int): IntTrieSet = {
        if (i1 == i2)
            IntTrieSet3(i2, i3, i4)
        else if (i1 == i3 || i2 == i3 || i3 == i4) { // we have i1 =!= i2
            IntTrieSet3(i1, i2, i4)
        } else if (i1 == i4 || i2 == i4) {
            IntTrieSet3(i1, i2, i3)
        }
        IntTrieSetN.from(i1, i2, i3, i4, 0)
    }

    /**
     * Constructs a new IntTrie from the distinct values!
     * If level is > 0 then all values have to have the same level least significant bits!
     */
    private[immutable] def from(i1: Int, i2: Int, i3: Int, i4: Int, level: Int): IntTrieSet = {
        val root =
            if (((i1 >>> level) & 1) == 0)
                new IntTrieSetN(new IntTrieSet1(i1), EmptyIntTrieSet, 1)
            else
                new IntTrieSetN(EmptyIntTrieSet, new IntTrieSet1(i1), 1)

        root + (i2, level) + (i3, level) + (i4, level)
    }
}
