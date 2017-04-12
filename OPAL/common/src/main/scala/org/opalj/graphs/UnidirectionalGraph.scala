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

import org.opalj.collection.immutable.Chain

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
        val vertices: Int
)( // a graph which contains the nodes with the ids: [0...vertices-1]
        private val successors: Array[Set[Int]] = new Array[Set[Int]](vertices)
) extends (Int ⇒ Set[Int]) {

    def nonEmpty: Boolean = vertices > 0

    /**
     * Returns a node's successors.
     */
    def apply(s: Int): Set[Int] = {
        val sSuccessors = successors(s)
        if (sSuccessors eq null)
            Set.empty
        else
            sSuccessors
    }

    def edges: Int ⇒ Iterator[Int] = (n: Int) ⇒ { this(n).toIterator }

    /**
     * Adds a new edge between the given vertices.
     *
     * (If the vertices were not previously added, they will be added.)
     */
    def +=(s: Int, t: Int): this.type = {
        successors(s) = apply(s) + t
        this
    }

    def sccs(filterSingletons: Boolean = false): Chain[Chain[Int]] = {
        org.opalj.graphs.sccs(vertices, edges, filterSingletons)
    }

    //
    // GENERATE VARIOUS KINDS OF DEBUG OUTPUTS
    //

    override def toString: String = {
        val vertices = (0 until this.vertices).iterator filter (v ⇒ successors(v) ne null) map { v ⇒
            this(v).mkString(v.toString()+" => {", ",", "}")
        }
        vertices.mkString("Graph{\n\t", "\n\t", "\n}")
    }

    def toNodes: Iterable[Node] = {
        val allVertices = (0 until vertices)
        val nodesMap: Map[Int, DefaultMutableNode[String]] = {
            allVertices.map(v ⇒ (v, new DefaultMutableNode(v.toString()))).toMap
        }

        allVertices.foreach { v ⇒
            val node = nodesMap(v)
            val successors = this(v).map(v ⇒ nodesMap(v)).toList
            node.addChildren(successors)
        }

        nodesMap.values
    }

    def toDot(dir: String = "forward", ranksep: String = "1.0", rankdir: String = "TB"): String = {
        org.opalj.graphs.toDot(toNodes, dir, ranksep, rankdir = rankdir)
    }
}
