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
