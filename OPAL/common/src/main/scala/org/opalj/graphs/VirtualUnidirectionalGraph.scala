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

import org.opalj.collection.immutable.Chain
import org.opalj.collection.IntIterator

/**
 * Efficient representation of a mutable graph where the nodes are identified using consecutive
 * int values (0,1,3,...).
 * This graph in particular supports the case where many nodes do not have successors.
 * Furthermore, computing the strongly connected components is particular efficient as no
 * transformations are required since we already use int values for the nodes.
 *
 * ==Thread Safety==
 * This class is not thread-safe!
 *
 * @example
 * Some nodes may have no successors:
 * {{{
 * val edges = Map((0 -> List(1)),(1 -> List(0)),(2 -> List(3))/*,(3 -> List())*/)
 * val successors : Int => Iterator[Int] = (i : Int) => {
 * edges.get(i) match {case Some(successors) => successors.toIterator; case _ => Iterator.empty }
 * }
 * val vg = new org.opalj.graphs.VirtualUnidirectionalGraph(4/*max id of a node +1 */,successors)
 * }}}
 *
 *
 * @author Michael Eichberg
 */
class VirtualUnidirectionalGraph(
        val verticesCount: Int,
        val successors:    IntFunction[IntIterator] // (Int ⇒ IntIterator)
) extends AbstractGraph[Int] {

    def vertices: Range = (0 until this.verticesCount)

    override def nonEmpty: Boolean = verticesCount > 0

    override def apply(s: Int): TraversableOnce[Int] = theSuccessors(s).iterator

    /**
     * Returns a node's successors.
     */
    def theSuccessors(s: Int): IntIterator = successors(s)

    def sccs(filterSingletons: Boolean = false): Chain[Chain[Int]] = {
        org.opalj.graphs.sccs(verticesCount, successors, filterSingletons)
    }
}
