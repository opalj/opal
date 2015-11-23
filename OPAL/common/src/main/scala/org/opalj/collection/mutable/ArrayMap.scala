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

import scala.annotation.tailrec
import scala.reflect.ClassTag
import scala.collection.immutable.Vector
import scala.collection.generic.FilterMonadic

/**
 * Conceptually, a map where the keys are positive int values and the values are non-null.
 * The key values always have to
 * be larger than or equal to 0 and are ideally continues (0,1,2,3,...).
 * The values are stored in a plain array to enable true O(1) access.
 * Furthermore, the array is only as large as it has to be to keep a value associated
 * with the largest key.
 *
 * @author Michael Eichberg
 */
class ArrayMap[T >: Null <: AnyRef: ClassTag] private (
        private var data: Array[T]
) {

    /**
     * Returns the value stored at the given index or `null` instead.
     *
     * @note If the index is not valid the result is not defined.
     */
    @throws[IndexOutOfBoundsException]("if the index is negative")
    def apply(index: Int): T = {
        if (index < data.length)
            data(index)
        else
            null
    }

    /**
     * Sets the value at the given index to the given value. If the index is larger than
     * the currently used array, the underlying array is immediately resized to make
     * it possible to store the new value.
     */
    @throws[IndexOutOfBoundsException]("if the index is negative")
    final def update(index: Int, value: T): Unit = {
        assert(value ne null, "ArrayMap only supports non-null values")

        val max = data.length
        if (index < max)
            data(index) = value
        else if (index == max) {
            data = data :+ value
        } else {
            val newData = new Array[T](index + 1)
            System.arraycopy(data, 0, newData, 0, max)
            newData(index) = value
            data = newData
        }
    }

    def foreach(f: T ⇒ Unit): Unit = {
        var i = 0
        val max = data.length
        while (i < max) {
            val e = data(i)
            // Recall that all values have to be non-null...
            if (e != null) f(e)
            i += 1
        }
    }

    def values: Iterator[T] = data.iterator.filter(_ ne null)

    def entries: Iterator[(Int, T)] = {

        new Iterator[(Int, T)] {

            private[this] def getNextIndex(startIndex: Int): Int = {
                val max = data.length
                var i = startIndex
                while (i + 1 < max) {
                    i = i + 1
                    if (data(i) ne null)
                        return i;
                }
                return max;
            }

            private[this] var i = getNextIndex(-1)

            def hasNext: Boolean = i < data.length

            def next(): (Int, T) = {
                val r = (i, data(i))
                i = getNextIndex(i)
                r
            }
        }
    }

    def map[X](f: (Int, T) ⇒ X): List[X] = {
        var rs = List.empty[X]
        var i = 0
        val max = data.length
        while (i < max) {
            val e = data(i)
            if (e != null) rs = f(i, e) :: rs
            i += 1
        }
        rs
    }

    override def equals(other: Any): Boolean = {
        other match {
            case that: ArrayMap[_] ⇒
                val thisData = this.data.asInstanceOf[Array[Object]]
                val thisLength = thisData.length
                val thatData = that.data.asInstanceOf[Array[Object]]
                val thatLength = thatData.length
                if (thisLength == thatLength) {
                    java.util.Arrays.equals(thisData, thatData)
                } else if (thisLength < thatLength) {
                    thatData.startsWith(thisData) &&
                        (thatData.view(thisLength, thatLength).forall { _ eq null })
                } else {
                    thisData.startsWith(thatData) &&
                        (thisData.view(thatLength, thisLength).forall { _ eq null })
                }
            case _ ⇒ false
        }
    }

    override def hashCode: Int = {
        var hc = 1
        foreach { e ⇒
            hc = hc * 41 + { if (e ne null) e.hashCode else 0 /* === System.identityHashCode(null) */ }
        }
        hc
    }

    def mkString(start: String, sep: String, end: String): String = {
        var s = start
        var i = 0
        val max = data.length
        while (i < max) {
            val e = data(i)
            if (e ne null)
                s += s"$i -> $e"
            i += 1
            if ((e ne null) && i < max)
                s += sep
        }
        s + end
    }

    override def toString: String = mkString("ArrayMap(", ", ", ")")

}
object ArrayMap {

    def empty[T >: Null <: AnyRef: ClassTag] =
        new ArrayMap(new Array[T](2))

    def apply[T >: Null <: AnyRef: ClassTag](sizeHint: Int) =
        new ArrayMap(new Array[T](sizeHint))
}
