/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.collection
package mutable

/**
 * A mutable chain (basically a linked-list) that enables o(1) append and prepend operations.
 * However, only removing the head element is a constant operation!
 *
 * @author Michael Eichberg
 */
class RefAppendChain[N >: Null <: AnyRef] private (
        private var h: RefAppendChainNode[N],
        private var l: RefAppendChainNode[N]
) {

    def this() { this(null, null) }

    def isEmpty: Boolean = h == null

    def nonEmpty: Boolean = h != null

    def take(): N = {
        val v = h.v
        h = h.rest
        if (h == null) l = null
        v
    }

    def head: N = h.v

    def last: N = l.v

    def prepend(v: N): this.type = {
        if (h == null) {
            h = new RefAppendChainNode(v, null)
            l = h
        } else {
            h = new RefAppendChainNode(v, h)
        }
        this
    }

    def append(v: N): this.type = {
        if (l == null) {
            h = new RefAppendChainNode(v, null)
            l = h
        } else {
            val newL = new RefAppendChainNode(v, null)
            l.rest = newL
            l = newL
        }
        this
    }

}

private[mutable] class RefAppendChainNode[T](val v: T, var rest: RefAppendChainNode[T])
