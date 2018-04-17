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
package graphs

import java.util.function.IntFunction

import org.opalj.collection.IntIterator
import org.opalj.collection.immutable.Chain
import org.opalj.collection.immutable.IntTrieSet

/**
 * Efficient representation of a mutable graph where the nodes are identified using consecutive
 * int values. This graph in particular supports the case where many nodes do not have successors.
 * Computing the strongly connected components is particular efficient as no transformations are
 * are required.
 *
 * ==Thread Safety==
 * This class is not thread-safe!
 *
 * @example
 * {{{
 * val g = new org.opalj.graphs.UnidirectionalGraph(10)() += (3,2) += (4,4) += (4,2) += (2, 4)
 * }}}
 *
 * @author Michael Eichberg
 */
class UnidirectionalGraph(
        val verticesCount: Int
)( // a graph which contains the nodes with the ids: [0...vertices-1]
        private val successors: Array[IntTrieSet] = new Array[IntTrieSet](verticesCount)
) extends AbstractGraph[Int] {

    def vertices: Range = (0 until this.verticesCount)

    override def nonEmpty: Boolean = verticesCount > 0

    override def apply(s: Int): TraversableOnce[Int] = theSuccessors(s).iterator

    /**
     * Returns a node's successors.
     */
    def theSuccessors(s: Int): IntTrieSet = {
        val sSuccessors = successors(s)
        if (sSuccessors eq null)
            IntTrieSet.empty
        else
            sSuccessors
    }

    def edges: IntFunction[IntIterator] = (n: Int) ⇒ { theSuccessors(n).intIterator }

    /**
     * Adds a new edge between the given vertices.
     *
     * (If the vertices were not previously added, they will be added.)
     */
    def +=(s: Int, t: Int): this.type = {
        successors(s) = theSuccessors(s) + t
        this
    }

    def sccs(filterSingletons: Boolean = false): Chain[Chain[Int]] = {
        org.opalj.graphs.sccs(verticesCount, edges, filterSingletons)
    }
}
