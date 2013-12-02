/* License (BSD Style License):
 * Copyright (c) 2009 - 2013
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
 *  - Neither the name of the Software Technology Group or Technische
 *    Universität Darmstadt nor the names of its contributors may be used to
 *    endorse or promote products derived from this software without specific
 *    prior written permission.
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
package mutable

import UShort.{ MinValue, MaxValue }

/**
 * A mutable, sorted set of unsigned short values that is tailored for small(er) sets.
 *
 * @author Michael Eichberg
 */
trait UShortSet extends collection.UShortSet {

    /**
     * Adds the given value to this set if it is not already contained in this set.
     * If this set has enough space to hold the additional value a reference to this
     * set is returned. Otherwise a new set is created and a reference to that set
     * is returned.
     */
    def +(value: Int): UShortSet

}

/**
 * This set uses a single int value to store two unsigned short values.
 */
private class UShortSet2(private var value: Int) extends UShortSet {

    def this(value1: Int, value2: Int) {
        this(value1 | value2 << 16)
    }

    import UShortSet2._

    @inline protected final def value1 = (value & Value1Mask)
    @inline protected final def value2 = ((value & Value2Mask) >>> 16)
    @inline protected final def notFull = (value & Value2Mask) == 0

    def max: Int = if (notFull) value1 else value2

    def contains(uShortValue: Int): Boolean = {
        val value1 = this.value1
        value1 == uShortValue || (uShortValue > value1 && value2 == uShortValue)
    }

    def foreach[U](f: /*ushortValue:*/ Int ⇒ U): Unit =
        { f(value1); f(value2) }

    def +(uShortValue: Int): UShortSet = {
        if (uShortValue < MinValue || uShortValue > MaxValue)
            throw new IllegalArgumentException("value out of range: "+uShortValue)

        if (notFull) {
            val value = this.value
            // update this sets container, if necessary 
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
                new UShortSet4((value.toLong << 16) | uShortValue)
            } else if (uShortValue == value1) {
                // the new value is already in the set
                this
            } else {
                // the new value is larger than the first value...
                val value2 = this.value2
                if (uShortValue < value2) {
                    // the new value is smaller than the second value
                    new UShortSet4(value1 | (uShortValue << 16) | (value2.toLong << 32))
                } else if (uShortValue == value2)
                    // the new value is equal to the second value
                    this
                else /*uShortValue > value2*/ {
                    new UShortSet4(value | (uShortValue.toLong << 32))
                }
            }
        }
    }

    def iterable: Iterable[Int] = if (notFull) Iterable(value1) else Iterable(value1, value2)

    override def hashCode = value

    override def equals(other: Any): Boolean = other match {
        case that: UShortSet2 ⇒ that.value == this.value
        case _                ⇒ false
    }
}
private object UShortSet2 {
    final val Value1Mask: Int = UShort.MaxValue
    final val Value2Mask: Int = Value1Mask << 16
}

private class UShortSet4(private var value: Long) extends UShortSet {

    def this(value1: Long, value2: Long, value3: Long, value4: Long) {
        this(value1 | (value2 << 16) | (value3 << 32) | (value4 << 48))
    }

    import UShortSet4._

    @inline protected final def value1 = (value & Value1Mask)
    @inline protected final def value2 = ((value & Value2Mask) >>> 16)
    @inline protected final def value3 = ((value & Value3Mask) >>> 32)
    @inline protected final def value4 = ((value & Value4Mask) >>> 48)
    @inline protected final def notFull = (value & Value4Mask) == 0

    def max: Int = (if (notFull) value3 else value4).toInt

    def +(uShortValue: Int): UShortSet = {
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

    def contains(uShortValue: Int): Boolean = {
        val value3 = this.value3
        if (uShortValue < value3) // this test also handles the nonFull case!
            value1 == uShortValue || value2 == uShortValue
        else
            value3 == uShortValue || value4 == uShortValue
    }

    def foreach[U](f: /*ushortValue:*/ Int ⇒ U): Unit =
        { f(value1.toInt); f(value2.toInt); f(value3.toInt); f(value4.toInt) }

    def iterable: Iterable[Int] =
        if (notFull)
            Iterable(value1.toInt, value2.toInt, value3.toInt)
        else
            Iterable(value1.toInt, value2.toInt, value3.toInt, value4.toInt)

    override def hashCode = ((value1 * 37 + value2) * 37 + value3).toInt * 37 + value4.toInt

    override def equals(other: Any): Boolean = other match {
        case that: UShortSet4 ⇒ that.value == this.value
        case _                ⇒ false
    }
}

private object UShortSet4 {
    final val Value1Mask: Long = UShort.MaxValue.toLong
    final val Value2Mask: Long = Value1Mask << 16
    final val Value3Mask: Long = Value2Mask << 16
    final val Value4Mask: Long = Value3Mask << 16
}

private class UShortSetNode(set1: UShortSet, set2: UShortSet) extends UShortSet {

    private[this] var currentMax = set2.max
    def max = currentMax

    def iterable = set1.iterable ++ set2.iterable

    def contains(uShortValue: Int): Boolean =
        if (set1.max > uShortValue)
            set1.contains(uShortValue)
        else if (set1.max == uShortValue)
            true
        else
            set2.contains(uShortValue)

    def foreach[U](f: /*ushortValue:*/ Int ⇒ U): Unit = { set1.foreach(f); set2.foreach(f) }

    def +(uShortValue: Int): UShortSet = {
        if (uShortValue < MinValue || uShortValue > MaxValue)
            throw new IllegalArgumentException("value out of range: "+uShortValue)
        val set1Max = set1.max
        if (set1Max > uShortValue) {
            val newSet1 = set1 + uShortValue
            if (newSet1 eq set1)
                this
            else
                new UShortSetNode(newSet1, set2)
        } else if (set1Max == uShortValue)
            this
        else {
            val newSet2 = set2 + uShortValue
            if (newSet2 eq set2) {
                currentMax = newSet2.max
                this
            } else
                new UShortSetNode(set1, newSet2)
        }
    }
}

private object EmptyUShortSet extends UShortSet {
    def iterable = Iterable.empty
    def contains(uShortValue: Int): Boolean = false
    def foreach[U](f: /*ushortValue:*/ Int ⇒ U): Unit = { /*Nothing to do.*/ }
    def +(uShortValue: Int): UShortSet = UShortSet(uShortValue)
    def max = throw new UnsupportedOperationException("the set is empty")
}
/**
 * Factory object to create new sets of unsigned short values.
 *
 * @author Michael Eichberg
 */
object UShortSet {

    /**
     * The empty set.
     */
    val empty: UShortSet = EmptyUShortSet

    /**
     * Creates a new set of unsigned short values which contains the given value.
     */
    @inline def apply(uShortValue: Int): UShortSet = {
        if (uShortValue < MinValue || uShortValue > MaxValue)
            throw new IllegalArgumentException("value out of range: "+uShortValue)

        new UShortSet2(uShortValue)
    }
}

