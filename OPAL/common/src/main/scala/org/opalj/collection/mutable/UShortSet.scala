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
package mutable

import UShort.{ MinValue, MaxValue }

/**
 * A memory-efficient, mutable, sorted set of unsigned short values that
 * is tailored for small(er) sets.
 *
 * @author Michael Eichberg
 */
trait UShortSet extends collection.UShortSet {

    /**
     * Adds the given value to this set if it is not already contained in this set.
     * If this set has enough space to hold the additional value, a reference to this
     * set is returned. Otherwise, a new set is created and a reference to that set
     * is returned. Hence, the return value ''must not'' be ignored.
     *
     * @return The set with the given value.
     */
    def +≈:(value: UShort): UShortSet

    override def +(value: UShort): UShortSet = {
        value +≈: this.mutableCopy
    }

    // FOR DEBUGGING AND ANALYSIS PURPOSES ONLY:
    private[mutable] def nodeCount: Int
    private[mutable] def asGraph: org.opalj.graphs.Node
}

/**
 * This set uses a single `Int` value to store one or two unsigned short values.
 */
private class UShortSet2(private var value: Int) extends UShortSet {

    def this(value1: UShort, value2: UShort) {
        this(value1 | value2 << 16)
    }

    import UShortSet2._

    @inline protected final def value1 = (value & Value1Mask)
    @inline protected final def value2 = (value & Value2Mask) >>> 16
    @inline protected final def notFull = (value & Value2Mask) == 0

    def mutableCopy: mutable.UShortSet = {
        if (notFull)
            new UShortSet2(value)
        else
            // When this set is full it is never manipulated again;
            // there is NO remove method.
            this
    }

    def max: UShort = if (notFull) value1 else value2

    def min: UShort = value1

    def size: Int = if (notFull) 1 else 2

    def contains(uShortValue: UShort): Boolean = {
        val value1 = this.value1
        value1 == uShortValue || (uShortValue > value1 && value2 == uShortValue)
    }

    def foreach(f: /*ushortValue:*/ UShort ⇒ Unit): Unit =
        { f(value1); val value2 = this.value2; if (value2 > 0) f(value2) }

    def forall(f: /*ushortValue:*/ UShort ⇒ Boolean): Boolean = {
        val value2 = this.value2
        f(value1) && ((value2 == 0) || f(value2))
    }

    def +≈:(uShortValue: UShort): UShortSet = {
        if (uShortValue < MinValue || uShortValue > MaxValue)
            throw new IllegalArgumentException("value out of range: "+uShortValue)

        if (notFull) {
            val value = this.value
            // update this set's container, if necessary 
            if (uShortValue < value)
                this.value = (value << 16) | uShortValue
            else if (uShortValue > value)
                this.value = (uShortValue << 16) | value

            this // this set..
        } else {
            // this set is full...
            val value1 = this.value1
            if (uShortValue < value1) {
                // the new value is smaller than the first value
                new UShortSet4((i2lBitMask(value) << 16) | uShortValue)
            } else if (uShortValue == value1) {
                // the new value is already in the set
                this
            } else {
                // the new value is larger than the first value...
                val value2 = this.value2
                if (uShortValue < value2) {
                    // the new value is smaller than the second value
                    new UShortSet4(
                        value1.toLong | (uShortValue.toLong << 16) | (value2.toLong << 32)
                    )
                } else if (uShortValue == value2)
                    // the new value is equal to the second value
                    this
                else /*uShortValue > value2*/ {
                    new UShortSet4(
                        i2lBitMask(value) | (i2lBitMask(uShortValue) << 32)
                    )
                }
            }
        }
    }

    def isEmpty = false

    def iterator: Iterator[UShort] =
        if (notFull)
            Iterator.single(value1)
        else
            new Iterator[UShort] {
                private var i = 0
                def hasNext = i == 0 || (i == 1 && !notFull)
                def next = { i += 1; if (i == 1) value1 else value2 }
            }

    def iterable: Iterable[UShort] =
        if (notFull) Iterable(value1) else Iterable(value1, value2)

    override def hashCode = value

    override def equals(other: Any): Boolean = other match {
        case that: UShortSet2 ⇒ that.value == this.value
        case _                ⇒ false
    }

    // FOR DEBUGGING AND ANALYSIS PURPOSES ONLY:
    private[mutable] def nodeCount: Int = 1
    private[mutable] def asGraph: org.opalj.graphs.Node =
        new org.opalj.graphs.SimpleNode[Int](
            System.identityHashCode(this), { i ⇒ this.toString })
}
private object UShortSet2 {
    final val Value1Mask /*: Int*/ = UShort.MaxValue
    final val Value2Mask /*: Int*/ = Value1Mask << 16
}

/**
 * This set uses a single long value to store three or four unsigned short values.
 */
private class UShortSet4(private var value: Long) extends UShortSet {

    def this(value1: Long, value2: Long, value3: Long, value4: Long) {
        this(value1 | (value2 << 16) | (value3 << 32) | (value4 << 48))
    }

    import UShortSet4._

    @inline protected final def value1: Long = (value & Value1Mask)
    @inline protected final def value2: Long = ((value & Value2Mask) >>> 16)
    @inline protected final def value3: Long = ((value & Value3Mask) >>> 32)
    @inline protected final def value4: Long = ((value & Value4Mask) >>> 48)
    @inline protected final def notFull: Boolean = (value & Value4Mask) == 0

    def max: UShort = (if (notFull) value3 else value4).toInt

    def min: UShort = value1.toInt

    def size: Int = if (notFull) 3 else 4

    def mutableCopy: mutable.UShortSet = {
        if (notFull)
            new UShortSet4(value)
        else
            // When this set is full it is never manipulated again;
            // there is NO remove method.
            this
    }

    def +≈:(uShortValue: UShort): UShortSet = {
        if (uShortValue < MinValue || uShortValue > MaxValue)
            throw new IllegalArgumentException("value out of range: "+uShortValue)

        val newValue: Long = uShortValue.toLong
        val value1 = this.value1
        val value3 = this.value3
        if (newValue < value3) {
            val value2 = this.value2
            if (newValue < value1) {
                if (notFull) {
                    value = value << 16 | newValue
                    this
                } else {
                    new UShortSetNode(
                        new UShortSet4(newValue, value1, value2, value3),
                        new UShortSet2(value4.toInt)
                    )
                }
            } else if (newValue == value1) {
                this
            } else if (newValue < value2) {
                if (notFull) {
                    value = (value1 | (newValue << 16)) | (value2 << 32) | (value3 << 48)
                    this
                } else {
                    new UShortSetNode(
                        new UShortSet4(value1, newValue, value2, value3),
                        new UShortSet2(value4.toInt)
                    )
                }
            } else if (newValue == value2) {
                this
            } else /*newValue > value2 && newValue < value3*/ {
                if (notFull) {
                    value = (value1 | (value2 << 16)) | (newValue << 32) | (value3 << 48)
                    this
                } else {
                    new UShortSetNode(
                        new UShortSet4(value1, value2, newValue, value3),
                        new UShortSet2(value4.toInt)
                    )
                }
            }
        } else /*newValue >= value3*/ {
            val value4 = this.value4
            if (newValue == value3) {
                this
            } else if (newValue < value4) {
                if (notFull) {
                    value = (value1 | (value2 << 16)) | (value3 << 32) | (newValue << 48)
                    this
                } else {
                    new UShortSetNode(
                        new UShortSet4(value1, value2, value3, newValue),
                        new UShortSet2(value4.toInt)
                    )
                }
            } else if (newValue == value4) {
                this
            } else /*newValue > value4 */ {
                new UShortSetNode(this, new UShortSet2(uShortValue))
            }
        }
    }

    def contains(uShortValue: UShort): Boolean = {
        val value3 = this.value3
        if (uShortValue < value3) // this test also handles the nonFull case!
            value1 == uShortValue || value2 == uShortValue
        else
            value3 == uShortValue || value4 == uShortValue
    }

    def foreach(f: /*ushortValue:*/ UShort ⇒ Unit): Unit = {
        f(value1.toInt)
        f(value2.toInt)
        f(value3.toInt)
        val value4 = this.value4
        if (value4 > 0l) f(value4.toInt)
    }

    def forall(f: /*ushortValue:*/ UShort ⇒ Boolean): Boolean = {
        val value4 = this.value4
        f(value1.toInt) &&
            f(value2.toInt) &&
            f(value3.toInt) &&
            ((value4 == 0l) || f(value4.toInt))
    }

    def isEmpty = false

    def iterator: Iterator[UShort] = new Iterator[UShort] {
        private var i = 0;
        private final val maxI = if (notFull) 3 else 4
        def hasNext: Boolean = i < maxI
        def next: UShort = {
            val v = i match {
                case 0 ⇒ value1.toInt
                case 1 ⇒ value2.toInt
                case 2 ⇒ value3.toInt
                case 3 ⇒ value4.toInt
            }
            i += 1
            v
        }
    }

    def iterable: Iterable[UShort] =
        if (notFull)
            Iterable(value1.toInt, value2.toInt, value3.toInt)
        else
            Iterable(value1.toInt, value2.toInt, value3.toInt, value4.toInt)

    override def hashCode = ((value1 * 37 + value2) * 37 + value3).toInt * 37 + value4.toInt

    override def equals(other: Any): Boolean = other match {
        case that: UShortSet4 ⇒ that.value == this.value
        case _                ⇒ false
    }

    // FOR DEBUGGING AND ANALYSIS PURPOSES ONLY:
    private[mutable] def nodeCount: Int = 1
    private[mutable] def asGraph: org.opalj.graphs.Node =
        new org.opalj.graphs.SimpleNode[Int](
            System.identityHashCode(this), { i ⇒ this.toString })
}

private object UShortSet4 {
    final val Value1Mask /*: Long*/ = UShort.MaxValue.toLong
    final val Value2Mask /*: Long*/ = Value1Mask << 16
    final val Value3Mask /*: Long*/ = Value2Mask << 16
    final val Value4Mask /*: Long*/ = Value3Mask << 16
}

private class UShortSetNode(
        private val set1: UShortSet,
        private val set2: UShortSet) extends UShortSet {

    private[this] var currentMax = set2.max
    def max = currentMax

    def min = set1.min

    def mutableCopy: mutable.UShortSet = {
        val set1Copy = set1.mutableCopy
        if (set1Copy eq set1) {
            val set2Copy = set2.mutableCopy
            if (set2Copy eq set2)
                this
            else
                new UShortSetNode(set1Copy, set2Copy)
        } else {
            new UShortSetNode(set1Copy, set2.mutableCopy)
        }
    }

    def iterator: Iterator[UShort] =
        new Iterator[UShort] {
            private[this] var firstSet = true
            private[this] var iterator = set1.iterator
            def hasNext = iterator.hasNext
            def next = {
                if (iterator.hasNext) {
                    val v = iterator.next
                    if (firstSet && !iterator.hasNext) {
                        firstSet = false
                        iterator = set2.iterator
                    }
                    v
                } else {
                    throw new UnsupportedOperationException
                }
            }
        }

    def iterable = set1.iterable ++ set2.iterable

    def size: Int = set1.size + set2.size

    def contains(uShortValue: UShort): Boolean =
        if (set1.max > uShortValue)
            set1.contains(uShortValue)
        else if (set1.max == uShortValue)
            true
        else
            set2.contains(uShortValue)

    def foreach(f: /*ushortValue:*/ UShort ⇒ Unit): Unit =
        { set1.foreach(f); set2.foreach(f) }

    def forall(f: /*ushortValue:*/ UShort ⇒ Boolean): Boolean =
        set1.forall(f) && set2.forall(f)

    def +≈:(uShortValue: UShort): UShortSet = {
        if (uShortValue < MinValue || uShortValue > MaxValue)
            throw new IllegalArgumentException("value out of range: "+uShortValue)

        val set1Max = set1.max
        if (set1Max > uShortValue ||
            (uShortValue < set2.min &&
                uShortValue > set1Max &&
                set1.isInstanceOf[UShortSet2])) {
            val newSet1 = uShortValue +≈: set1
            if (newSet1 eq set1)
                this
            else if (set2.isInstanceOf[UShortSet2] && newSet1.isInstanceOf[UShortSetNode]) {
                val tempNode = newSet1.asInstanceOf[UShortSetNode]
                val v = tempNode.set2.asInstanceOf[UShortSet2].min
                new UShortSetNode(tempNode.set1, v +≈: set2)
            } else
                new UShortSetNode(newSet1, set2)
        } else if (set1Max == uShortValue)
            this
        else {
            val newSet2 = uShortValue +≈: set2
            if (newSet2 eq set2) {
                currentMax = newSet2.max
                this
            } else
                new UShortSetNode(set1, newSet2)
        }
    }

    def isEmpty = false

    override def hashCode = (set1.hashCode() * 37 + set2.hashCode()) * 37

    override def equals(other: Any): Boolean = other match {
        case that: UShortSetNode ⇒ this.set1 == that.set1 && this.set2 == that.set2
        case _                   ⇒ false
    }

    // FOR DEBUGGING AND ANALYSIS PURPOSES ONLY:
    private[mutable] def nodeCount: Int = set1.nodeCount + set2.nodeCount
    private[mutable] def asGraph: org.opalj.graphs.Node =
        new org.opalj.graphs.SimpleNode[Int](
            System.identityHashCode(this),
            { i ⇒ "UShortSetNode" },
            None,
            List(set1.asGraph, set2.asGraph))
}

object UShortSetNode {

}

private object EmptyUShortSet extends UShortSet {
    def isEmpty = true
    def size: Int = 0
    def mutableCopy: mutable.UShortSet = this
    def iterator = Iterator.empty
    def iterable = Iterable.empty
    def contains(uShortValue: UShort): Boolean = false
    def foreach(f: /*ushortValue:*/ UShort ⇒ Unit): Unit = { /*Nothing to do.*/ }
    def forall(f: /*ushortValue:*/ UShort ⇒ Boolean): Boolean = true
    def +≈:(uShortValue: UShort): UShortSet = UShortSet(uShortValue)
    def max = throw new UnsupportedOperationException("the set is empty")
    def min = throw new UnsupportedOperationException("the set is empty")

    // FOR DEBUGGING AND ANALYSIS PURPOSES ONLY:
    private[mutable] def nodeCount: Int = 1
    private[mutable] def asGraph: org.opalj.graphs.Node =
        new org.opalj.graphs.SimpleNode[Int](
            System.identityHashCode(this), { i ⇒ "EmptyUShortSet" })
}
/**
 * Factory to create sets of unsigned short values.
 *
 * @author Michael Eichberg
 */
object UShortSet {

    /**
     * The empty (sorted) set of unsigned short values.
     */
    val empty: UShortSet = EmptyUShortSet

    /**
     * Creates a new set of unsigned short values which contains the given value.
     *
     * @param uShortValue An integer value in the range [0,0xFFFF).
     */
    @inline def apply(uShortValue: UShort): UShortSet = {
        if (uShortValue < MinValue || uShortValue > MaxValue)
            throw new IllegalArgumentException("value out of range: "+uShortValue)

        new UShortSet2(uShortValue)
    }

    /**
     * Creates a new sorted set of unsigned short values of the given values.
     *
     * @param uShortValue1 An integer value in the range [0,0xFFFF).
     * @param uShortValue2 An integer value in the range [0,0xFFFF).
     */
    @inline def apply(uShortValue1: UShort, uShortValue2: UShort): UShortSet = {
        if (uShortValue1 < MinValue || uShortValue1 > MaxValue)
            throw new IllegalArgumentException("value out of range: "+uShortValue1)
        if (uShortValue2 < MinValue || uShortValue2 > MaxValue)
            throw new IllegalArgumentException("value out of range: "+uShortValue2)

        if (uShortValue1 == uShortValue2)
            new UShortSet2(uShortValue1)
        else if (uShortValue1 < uShortValue2)
            new UShortSet2(uShortValue1, uShortValue2)
        else
            new UShortSet2(uShortValue2, uShortValue1)
    }

    /**
     * Creates a new sorted set of unsigned short values.
     */
    def create(uShortValues: UShort*): UShortSet = {
        uShortValues match {
            case Nil ⇒
                EmptyUShortSet
            case Seq(uShortValue) ⇒
                apply(uShortValue)
            case values ⇒
                (apply(values.head) /: values.tail)((s, v) ⇒ v +≈: s)
        }
    }
}

