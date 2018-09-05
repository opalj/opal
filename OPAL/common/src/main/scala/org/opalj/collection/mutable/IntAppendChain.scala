/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.collection.mutable

/**
 * A mutable chain (basically a linked-list) that enables o(1) append and prepend operations.
 * However, only removing the head element is a constant operation!
 *
 * @author Michael Eichberg
 */
class IntAppendChain private (
        private var h: IntAppendChainNode,
        private var l: IntAppendChainNode
) {

    def this() { this(null, null) }

    def isEmpty: Boolean = h == null

    def nonEmpty: Boolean = h != null

    def take(): Int = {
        val v = h.v
        h = h.rest
        if (h == null) l = null
        v
    }

    def head: Int = h.v

    def last: Int = l.v

    def prepend(v: Int): this.type = {
        if (h == null) {
            h = new IntAppendChainNode(v, null)
            l = h
        } else {
            h = new IntAppendChainNode(v, h)
        }
        this
    }

    def append(v: Int): this.type = {
        if (l == null) {
            h = new IntAppendChainNode(v, null)
            l = h
        } else {
            val newL = new IntAppendChainNode(v, null)
            l.rest = newL
            l = newL
        }
        this
    }

}

private[mutable] class IntAppendChainNode(val v: Int, var rest: IntAppendChainNode)
