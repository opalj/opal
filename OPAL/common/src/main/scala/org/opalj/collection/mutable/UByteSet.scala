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

import org.opalj.UByte.{ MinValue, MaxValue }

/**
 * A set that efficiently stores small sets of unsigned byte values.
 *
 * @author Michael Eichberg
 */
trait UByteSet extends SmallValuesSet {

    override def mutableCopy: UByteSet /* The return type is refined! */

    override def +≈:(value: Int): UByteSet /* The return type is refined! */

    /**
     * Converts the values to a string using the given separator and adding offset
     * to each value.
     *
     * Basically this is comparable to a {{{map(_+offset).mkString(sep)}}}.
     */
    def valuesToString(sep: String, offset: Int): String

    final protected[collection] def mkString(
        start: String, sep: String, end: String,
        offset: Int): String =
        start + valuesToString(sep, offset) + end

    def mkString(start: String, sep: String, end: String): String =
        mkString(start, sep, end, 0)

    private[mutable] def isLeafNode: Boolean
    private[mutable] def asTreeNode: UByteSetNode
    private[mutable] def asNonEmptyLeafNode: UByteSet4

    override def toString() = mkString("UByteSet(", ",", ")", 0)

    /*FOR DEBUGGING AND OPTIMIZATION PURPOSES*/
    /**
     * Returns a representation of the AST used to store the set.
     */
    private[mutable] def structure: String

}
/**
 * Factory to create [[UByteSet]]s.
 */
object UByteSet {

    def empty: UByteSet = EmptyUByteSet

    def apply(value: UByte): UByteSet = new UByteSet4(value)

}

/**
 * An effectively immutable, empty set of unsigned byte values.
 *
 * @author Michael Eichberg
 */
private[mutable] object EmptyUByteSet extends UByteSet {

    def isEmpty = true
    def isSingletonSet = false
    def size: Int = 0
    def min: UByte = throw new UnsupportedOperationException("this set is empty")
    def max: UByte = throw new UnsupportedOperationException("this set is empty")

    def mutableCopy: this.type = this
    def +≈:(value: UByte): UByteSet = new UByteSet4(value)

    def contains(value: UByte): Boolean = false
    def exists(f: Int ⇒ Boolean): Boolean = false
    def isSubsetOf(other: org.opalj.collection.SmallValuesSet): Boolean = true
    def foreach[U](f: UShort ⇒ U): Unit = {}
    def forall(f: Int ⇒ Boolean): Boolean = true

    def valuesToString(sep: String, offset: Int): String = ""

    private[mutable] def isLeafNode: Boolean = true
    private[mutable] def asTreeNode: UByteSetNode = throw new ClassCastException()
    private[mutable] def asNonEmptyLeafNode: UByteSet4 = throw new ClassCastException()

    /*FOR DEBUGGING AND OPTIMIZATION PURPOSES*/
    private[mutable] def structure: String = "EmptyUByteSet"
}

/**
 * This set always contains at least one value which may be "0".
 */
private[mutable] class UByteSet4(private var value: Int) extends UByteSet {

    def this(value1: UByte, value2: UByte) {
        this(value1 | value2 << 8)
    }

    def this(value1: UByte, value2: UByte, value3: UByte) {
        this(value1 | value2 << 8 | value2 << 16)
    }

    def this(value1: UByte, value2: UByte, value3: UByte, value4: UByte) {
        this(value1 | value2 << 8 | value2 << 16 | value2 << 24)
    }

    import UByteSet4._

    @inline private[mutable] final def value1: Int = (value & Value1Mask)
    @inline private[mutable] final def value2: Int = ((value & Value2Mask) >>> 8)
    @inline private[mutable] final def value3: Int = ((value & Value3Mask) >>> 16)
    @inline private[mutable] final def value4: Int = ((value & Value4Mask) >>> 24)
    @inline private[mutable] final def notFull: Boolean = (value & Value4Mask) == 0
    @inline private[mutable] final def atLeastTwoValues: Boolean = (value & Value2Mask) != 0

    def mutableCopy: UByteSet4 = {
        if (notFull)
            new UByteSet4(value)
        else
            // When this set is full it is never manipulated again;
            // there is NO remove method.
            this
    }

    def isEmpty = false

    def isSingletonSet = (value & Value2_3_4Mask) == 0

    def size: Int = {
        if (value3 == 0) {
            if (value2 == 0)
                1
            else
                2
        } else {
            if (value4 == 0)
                3
            else
                4
        }
    }

    def min = value1

    def max = {
        val value3 = this.value3
        if (value3 != 0) {
            val value4 = this.value4
            if (value4 == 0)
                value3
            else
                value4
        } else {
            val value2 = this.value2
            if (value2 == 0) {
                value1
            } else
                value2
        }
    }

    def +≈:(uByteValue: UByte): UByteSet = {

        @inline def updateValue(newValue: Int): UByteSet = {
            if (notFull) {
                this.value = newValue
                this
            } else {
                new UByteSetNode(new UByteSet4(newValue), new UByteSet4(value4))
            }
        }

        val value1 = this.value1
        if (uByteValue < value1) {
            updateValue(this.value << 8 | uByteValue)
        } else if (uByteValue == value1) {
            this
        } else if (value2 == 0 || uByteValue < value2) {
            updateValue(((this.value << 8) & Value3_4Mask) | uByteValue << 8 | value1)
        } else if (uByteValue == value2) {
            this
        } else if (value3 == 0 || uByteValue < value3) {
            updateValue(((this.value << 8) & Value4Mask) | uByteValue << 16 | (this.value & Value1_2Mask))
        } else if (uByteValue == value3) {
            this
        } else if (value4 == 0 || uByteValue < value4) {
            updateValue(uByteValue << 24 | (this.value & Value1_2_3Mask))
        } else if (uByteValue == value4) {
            this
        } else {
            new UByteSetNode(this, new UByteSet4(uByteValue))
        }
    }

    def contains(value: UByte): Boolean = {
        val value1 = this.value1
        if (value < value1) // this test also handles the nonFull case!
            return false;

        value1 == value || value2 == value || value3 == value || value4 == value
    }

    def exists(f: Int ⇒ Boolean): Boolean = {
        if (f(value1)) return true;

        val value2 = this.value2
        if (value2 == 0)
            return false;
        else if (f(value2))
            return true;

        val value3 = this.value3
        if (value3 == 0)
            return false;
        else if (f(value3))
            return true;

        val value4 = this.value4
        if (value4 == 0)
            return false;

        f(value4)
    }

    def isSubsetOf(other: org.opalj.collection.SmallValuesSet): Boolean = {
        if (other.isEmpty)
            return false;

        this.forall(other.contains)
    }

    def foreach[U](f: UByte ⇒ U): Unit = {
        f(value1)
        val value2 = this.value2
        if (value2 > 0) {
            f(value2)
            val value3 = this.value3
            if (value3 > 0) {
                f(value3)
                val value4 = this.value4
                if (value4 > 0) {
                    f(value4)
                }
            }
        }
    }

    def forall(f: UByte ⇒ Boolean): Boolean = {
        if (!f(value1))
            return false;

        val value2 = this.value2
        if (value2 > 0) {
            if (!f(value2))
                return false;

            val value3 = this.value3
            if (value3 > 0) {
                if (!f(value3))
                    return false;

                val value4 = this.value4
                if (value4 > 0)
                    return f(value4);
            }
        }

        true // RETURN
    }

    private[mutable] def isLeafNode: Boolean = true

    private[mutable] def asTreeNode: UByteSetNode = throw new ClassCastException()
    private[mutable] def asNonEmptyLeafNode: UByteSet4 = this

    def valuesToString(seperator: String, offset: Int): String = {
        var s = String.valueOf(value1 + offset)
        if (value2 > 0) {
            s += seperator + String.valueOf(value2 + offset)
            val value3 = this.value3
            if (value3 > 0) {
                s += seperator + String.valueOf(value3 + offset)
                val value4 = this.value4
                if (value4 > 0) {
                    s += seperator + String.valueOf(value4 + offset)
                }
            }
        }
        s
    }

    /*FOR DEBUGGING AND OPTIMIZATION PURPOSES*/
    def structure: String = toString
}

private object UByteSet4 {
    final val Value1Mask /*: Int*/ = UByte.MaxValue
    final val Value2Mask /*: Int*/ = Value1Mask << 8
    final val Value1_2Mask = Value1Mask | Value2Mask
    final val Value3Mask /*: Int*/ = Value2Mask << 8
    final val Value1_2_3Mask = Value1_2Mask | Value3Mask
    final val Value4Mask /*: Int*/ = Value3Mask << 8
    final val Value3_4Mask /*: Int*/ = Value3Mask | Value4Mask
    final val Value2_3_4Mask = Value2Mask | Value3_4Mask
}

private class UByteSetNode(
        private val set1: UByteSet,
        private val set2: UByteSet) extends UByteSet {

    private[this] var currentMax = set2.max
    def max = currentMax

    def min = set1.min

    def size = set1.size + set2.size

    def isEmpty = false
    def isSingletonSet = false

    private[mutable] def isLeafNode: Boolean = false
    private[mutable] def asTreeNode: UByteSetNode = this
    private[mutable] def asNonEmptyLeafNode: UByteSet4 = throw new ClassCastException()

    def mutableCopy: UByteSet = {
        val set1Copy = set1.mutableCopy
        if (set1Copy eq set1) {
            val set2Copy = set2.mutableCopy
            if (set2Copy eq set2)
                this
            else
                new UByteSetNode(set1Copy, set2Copy)
        } else {
            new UByteSetNode(set1Copy, set2.mutableCopy)
        }
    }

    def contains(value: UByte): Boolean = {
        val set1Max = set1.max
        if (set1Max > value)
            set1.contains(value)
        else if (set1.max == value)
            true
        else
            set2.contains(value)
    }
    def exists(f: Int ⇒ Boolean): Boolean = {
        set1.exists(f) || set2.exists(f)
    }

    def isSubsetOf(that: org.opalj.collection.SmallValuesSet): Boolean = {
        if (that.isEmpty)
            false
        else
            this.forall { that.contains }

    }

    def foreach[U](f: UByte ⇒ U): Unit = { set1.foreach(f); set2.foreach(f) }
    def forall(f: UByte ⇒ Boolean): Boolean = set1.forall(f) && set2.forall(f)

    def +≈:(uByteValue: UByte): UByteSet = {
        assert(
            uByteValue >= MinValue && uByteValue <= MaxValue,
            s"no ubyte value: $uByteValue")

        val set1 = this.set1
        val set1Max = set1.max
        if (uByteValue < set1Max) {
            val newSet1 = uByteValue +≈: set1
            if (newSet1 eq set1) {
                // it was already contained in the set...
                this
            } else if (set1.isLeafNode && !newSet1.isLeafNode) {
                // add the largest value to the right set...
                // (which is necessarily smaller than all current values in the old right set)
                val newTreeNode = newSet1.asTreeNode
                val newSet2 = newTreeNode.set2.asNonEmptyLeafNode.value1 +≈: set2
                new UByteSetNode(newTreeNode.set1, newSet2)
            } else {
                val newSet2 = uByteValue +≈: set2
                if (newSet2 eq set2) {
                    currentMax = newSet2.max
                    this
                } else
                    new UByteSetNode(newSet1, set2)
            }
        } else if (uByteValue == set1Max) {
            this
        } else {
            val newSet2 = uByteValue +≈: set2
            if (newSet2 eq set2) {
                currentMax = newSet2.max
                this
            } else {
                new UByteSetNode(set1, newSet2)
            }
        }
    }

    def valuesToString(seperator: String, offset: Int): String =
        set1.valuesToString(seperator, offset) + seperator + set2.valuesToString(seperator, offset)

    /*FOR DEBUGGING AND OPTIMIZATION PURPOSES*/
    def structure: String = s"UByteSetNode(${set1.structure}, ${set2.structure})"
}

