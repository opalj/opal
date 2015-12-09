/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
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

import scala.collection.mutable.LinkedHashMap
import scala.collection.mutable.Set

/**
 * A very simple representation of a graph that is generally useful, but was designed
 * to facilitate testing of graph based algorithms.
 *
 * @author Michael Eichberg
 */
class Graph[N >: Null <: AnyRef] private (
        val vertices: Set[N],
        val edges:    LinkedHashMap[N, List[N]]
) extends (N ⇒ Traversable[N]) {

    def apply(s: N): Traversable[N] = edges.getOrElse(s, List.empty)

    def +=(n: N): this.type = {
        vertices += n
        this
    }

    def +=(e: Tuple2[N, N]): this.type = {
        val (s, t) = e
        this += (s, t)
    }

    def +=(s: N, t: N): this.type = {
        vertices += s += t
        edges += ((s, t :: edges.getOrElse(s, List.empty)))

        this
    }

    override def toString: String = {
        "Graph{\n"+
            vertices.map { v ⇒
                v.toString() +
                    edges.getOrElse(v, List.empty).mkString(" => {", ",", "}")
            }.mkString("\n")+
            "\n}"
    }

    def toNodes: Iterable[Node] = {
        val nodesMap: Map[N, DefaultMutableNode[String]] = vertices.map(v ⇒ (v, new DefaultMutableNode(v.toString()))).toMap
        vertices.foreach { v ⇒
            val node = nodesMap(v)
            val successors = this(v).map(v ⇒ nodesMap(v)).toList
            node.addChildren(successors)
        }
        nodesMap.values
    }

    def toDot: String = {
        org.opalj.graphs.toDot(toNodes.toSet)
    }
}

object Graph {

    def empty[N >: Null <: AnyRef] = new Graph[N](Set.empty, LinkedHashMap.empty)
}
