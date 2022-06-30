/* BSD 2-Clause License - see OPAL/LICENSE for details. */
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
) extends Serializable { queue =>

    def this(value: Int) = this(new QueueNode(value, null))

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

    // FIXME ... we should generate a new instance def tail: this.type = { first = first.next; this }

    def foreach[U](f: Int => U): Unit = {
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
        sb.append(')')
        sb.toString
    }
}

private[mutable] class QueueNode(val value: Int, var next: QueueNode)

/**
 * Factory to create [[IntQueue]]s.
 */
object IntQueue {

    def empty: IntQueue = new IntQueue

    def apply(elems: Int*): IntQueue = elems.foldLeft(empty)(_.enqueue(_))

}
