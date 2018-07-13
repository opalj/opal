/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.collection
package mutable

/**
 * A mutable chain (basically a linked-list) that enables o(1) append and prepend operations.
 * However, only removing the head element is a constant operation!
 *
 * @author Michael Eichberg
 */
class AnyRefAppendChain[N >: Null <: AnyRef] private (
        private var h: AnyRefAppendChainNode[N],
        private var l: AnyRefAppendChainNode[N]
) {

    def this() {
        this(null, null)
    }

    def isEmpty = h == null

    def nonEmpty = h != null

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
            h = new AnyRefAppendChainNode(v, null)
            l = h
        } else {
            h = new AnyRefAppendChainNode(v, h)
        }
        this
    }

    def append(v: N): this.type = {
        if (l == null) {
            h = new AnyRefAppendChainNode(v, null)
            l = h
        } else {
            val newL = new AnyRefAppendChainNode(v, null)
            l.rest = newL
            l = newL
        }
        this
    }

}

private[mutable] class AnyRefAppendChainNode[T](val v: T, var rest: AnyRefAppendChainNode[T])
