/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2016
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

/**
 * A very lightweight array based implementation of a mutable stack of `int` values which has a
 * given initial height.
 *
 * @author Michael Eichberg
 */
final class IntArrayStack private (private var data: Array[Int]) {

    private var size = 0

    def this(initialSize: Int) { this(new Array[Int](Math.max(initialSize, 4))) }

    def push(i: Int): Unit = {
        val size = this.size
        var data = this.data
        if (data.length == size) {
            val newData = new Array[Int](size * 2)
            System.arraycopy(data, 0, newData, 0, size)
            data = newData
            this.data = newData
        }

        data(size) = i
        this.size = size + 1
    }

    def push(that: IntArrayStack): Unit = {
        val thatSize = that.size

        if (thatSize == 0)
            return ;

        val thisSize = this.size
        var data = this.data
        val combinedSize = thisSize + thatSize
        if (combinedSize > data.length) {
            val newData = new Array[Int](combinedSize + 10)
            System.arraycopy(data, 0, newData, 0, thisSize)
            data = newData
            this.data = newData
        }

        System.arraycopy(that.data, 0, data, thisSize, thatSize)

        this.size = combinedSize
    }

    def pop(): Int = {
        val index = this.size - 1
        val i = data(index)
        this.size = index
        i
    }

    def foreach[U](f: Int ⇒ U): Unit = {
        var i = size - 1
        while (i >= 0) {
            f(data(i))
            i -= 1
        }
    }

    def foldLeft[B](z: B)(f: (B, Int) ⇒ B): B = {
        var v = z
        var i = size - 1
        while (i >= 0) {
            v = f(v, data(i))
            i -= 1
        }
        v
    }

    def isEmpty: Boolean = size == 0

    def nonEmpty: Boolean = size > 0

    override def toString: String = s"IntArrayStack(size=$size;data=${data.take(size).mkString("(", ",", ")")})"
}