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
package mutable

import java.util.function.IntConsumer

import scala.collection.mutable
import scala.collection.generic
import scala.collection.AbstractIterator

/**
 * An array based implementation of a mutable stack of `int` values which has a
 * given initial size. If the stack is non-empty, the index of the top value is `0` and the
 * index of the bottom value is (`length-1`).
 *
 * @param data The array containing the values.
 * @param size0 The number of stored values.
 * @author Michael Eichberg
 */
final class IntArrayStack private (
        private var data:  Array[Int],
        private var size0: Int
) extends mutable.IndexedSeq[Int]
    with mutable.IndexedSeqLike[Int, IntArrayStack]
    with mutable.Cloneable[IntArrayStack]
    with Serializable { stack ⇒

    def this(initialSize: Int = 4) { this(new Array[Int](initialSize), 0) }

    override def size: Int = size0
    override def length: Int = size0
    override def isEmpty: Boolean = size0 == 0
    override def nonEmpty: Boolean = size0 > 0

    override def apply(index: Int): Int = {
        val size0 = this.size0
        val valueIndex = size0 - 1 - index
        if (valueIndex < 0 || valueIndex >= size0)
            throw new IndexOutOfBoundsException(s"$index (size: $size0)");

        data(valueIndex)
    }

    override def update(index: Int, v: Int): Unit = data(size0 - 1 - index) = v

    override def newBuilder: mutable.Builder[Int, IntArrayStack] = IntArrayStack.newBuilder

    /** The same as push but additionally returns `this`. */
    def +=(i: Int): this.type = {
        push(i)
        this
    }

    def push(i: Int): Unit = {
        val size0 = this.size0
        var data = this.data
        if (data.length == size0) {
            val newData = new Array[Int]((size0 + 1) * 2)
            System.arraycopy(data, 0, newData, 0, size0)
            data = newData
            this.data = newData
        }

        data(size0) = i
        this.size0 = size0 + 1
    }

    /**
     * Pushes the value of the given stack on this stack while maintaining the order
     * in which the values were pushed on the given stack. I.e.,
     * if this contains the values `[1|2->` and the given one the values `[3,4->`
     * then the resulting stack will contain the values `[1|2|3|4...`.
     *
     * @note In case of `++` the order of the values is reversed.
     */
    def push(that: IntArrayStack): Unit = {
        val thatSize = that.size0

        if (thatSize == 0) {
            return ;
        }

        val thisSize = this.size0
        var thisData = this.data

        val newSize = thisSize + thatSize
        if (newSize > thisData.length) {
            val newData = new Array[Int](newSize + 10)
            System.arraycopy(thisData, 0, newData, 0, thisSize)
            thisData = newData
            this.data = thisData
        }

        System.arraycopy(that.data, 0, thisData, thisSize, thatSize)

        this.size0 = newSize
    }

    /**
     * Returns and removes the top most value from the stack.
     *
     * @note If the stack is empty a `NoSuchElementException` will be thrown.
     */
    def pop(): Int = {
        val index = this.size0 - 1
        if (index < 0)
            throw new NoSuchElementException("the stack is empty");

        val i = this.data(index)
        this.size0 = index
        i
    }

    /**
     * Returns the stack's top-most value.
     *
     * @note If the stack is empty a `NoSuchElementException` will be thrown.
     */
    def top(): Int = {
        val index = this.size0 - 1
        if (index < 0)
            throw new NoSuchElementException("the stack is empty");

        this.data(index)
    }

    /**
     * Same as `top()`, but potentially less efficient due to (un)boxing if the head method
     * of the supertype is called.
     */
    override /*TraversableLike*/ def head: Int = top()

    override /*TraversableLike*/ def last: Int = {
        if (this.size0 == 0)
            throw new NoSuchElementException("the stack is empty");

        this.data(0)
    }

    override def foreach[U](f: Int ⇒ U): Unit = { // TODO Use Java8 IntConsumer interface
        val data = this.data
        var i = this.size0 - 1
        while (i >= 0) {
            f(data(i))
            i -= 1
        }
    }

    def foreachReverse(f: IntConsumer): Unit = {
        val data = this.data
        val max = this.size0 - 1
        var i = 0
        while (i <= max) {
            f.accept(data(i))
            i += 1
        }
    }

    override def foldLeft[B](z: B)(f: (B, Int) ⇒ B): B = {
        val data = this.data
        var v = z
        var i = this.size0 - 1
        while (i >= 0) {
            v = f(v, data(i))
            i -= 1
        }
        v
    }

    /**
     * Returns an iterator which produces the values in LIFO order.
     *
     * @note    The `next` method will throw an `IndexOutOfBoundsException`
     *          when all elements are already returned.
     */
    override def iterator: Iterator[Int] = {
        new AbstractIterator[Int] {
            var currentIndex = stack.size0 - 1
            def hasNext: Boolean = currentIndex >= 0

            def next(): Int = {
                val currentIndex = this.currentIndex
                val r = stack.data(currentIndex)
                this.currentIndex = currentIndex - 1
                r
            }

        }
    }

    def intIterator: IntIterator = {
        new IntIterator {
            var currentIndex = stack.size0 - 1
            def hasNext: Boolean = currentIndex >= 0

            def next(): Int = {
                val currentIndex = this.currentIndex
                val r = stack.data(currentIndex)
                this.currentIndex = currentIndex - 1
                r
            }

        }
    }

    override def clone(): IntArrayStack = new IntArrayStack(data.clone(), size0)

    override def toString: String = {
        s"IntArrayStack(/*size=$size0;*/data=${data.take(size0).mkString("[", ",", "→")})"
    }
}

/**
 * Factory to create [[IntArrayStack]]s.
 */
object IntArrayStack {

    implicit def canBuildFrom: generic.CanBuildFrom[IntArrayStack, Int, IntArrayStack] = {
        new generic.CanBuildFrom[IntArrayStack, Int, IntArrayStack] {
            def apply(): mutable.Builder[Int, IntArrayStack] = newBuilder
            def apply(from: IntArrayStack): mutable.Builder[Int, IntArrayStack] = newBuilder
        }
    }

    def newBuilder: mutable.Builder[Int, IntArrayStack] = {
        new mutable.ArrayBuffer[Int] mapResult fromSeq
    }

    /**
     * Creates a new stack based on a given sequence. The last value of the sequence will
     * be the top value of the stack.
     */
    def fromSeq(seq: TraversableOnce[Int]): IntArrayStack = seq.foldLeft(new IntArrayStack(8))(_ += _)

    def apply(value: Int): IntArrayStack = {
        val initialArray = new Array[Int](10)
        initialArray(0) = value
        new IntArrayStack(initialArray, 1)
    }

    def empty: IntArrayStack = new IntArrayStack
}
