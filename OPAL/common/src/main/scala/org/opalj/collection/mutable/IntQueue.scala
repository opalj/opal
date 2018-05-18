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

/**
 * A lightweight, linked-list based implementation of a mutable queue of int values.
 *
 * @note   This data-structure is only intended to be used by OPAL.
 * @note   This data structure is not thread safe.
 *
 * @author Michael Eichberg
 */
protected[opalj] final class IntQueue private (
        private var first: QueueNode = null,
        private var last:  QueueNode = null
) extends Serializable { queue ⇒

    def this(value: Int) = {
        this(new QueueNode(value, null))
    }

    def isEmpty: Boolean = first eq null

    def nonEmpty: Boolean = first ne null

    def size: Int = {
        var size = 0
        var c = first
        while (c ne null) {
            size += 1
            c = c.next
        }
        size
    }

    def head: Int = first.value

    def tail: this.type = { first = first.next; this }

    def foreach[U](f: Int ⇒ U): Unit = {
        var c = first
        while (c ne null) {
            f(c.value)
            c = c.next
        }
    }

    def enqueue(value: Int): this.type = {
        if (last eq null) {
            last = new QueueNode(value, null)
            first = last
        } else {
            last.next = new QueueNode(value, null)
            last = last.next
        }
        this
    }

    def dequeue: Int = {
        val value = first.value
        first = first.next
        if (first eq null) {
            last = null
        }
        value
    }

    override def toString: String = {
        val sb = new StringBuilder("IntQueue(")
        if (first ne null) {
            sb.append(first.value)
            var c = first.next
            while (c ne null) {
                sb.append(',')
                sb.append(c.value)
                c = c.next
            }
        }
        sb.append(")")
        sb.toString
    }
}

private[mutable] class QueueNode(val value: Int, var next: QueueNode)

/**
 * Factory to create [[IntArrayStack]]s.
 */
object IntQueue {

    def empty: IntQueue = new IntQueue

    def apply(elems: Int*): IntQueue = elems.foldLeft(empty)(_.enqueue(_))

}
