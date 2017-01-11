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

import scala.language.implicitConversions

import scala.collection.mutable.LinkedHashMap
import scala.collection.mutable.Set
import scala.collection.{Map ⇒ AMap}
import org.opalj.collection.immutable.Chain
import org.opalj.collection.immutable.Naught

/**
 * Represents a mutable (multi-)graph with ordered edges.
 *
 * @author Michael Eichberg
 */
class Graph[@specialized(Int) N] private (
        val vertices:     Set[N],
        val successors:   LinkedHashMap[N, Chain[N]],
        val predecessors: LinkedHashMap[N, Chain[N]]
) extends (N ⇒ Chain[N]) {

    def nonEmpty: Boolean = vertices.nonEmpty

    def apply(s: N): Chain[N] = successors.getOrElse(s, Naught)

    def asTraversable: N ⇒ Traversable[N] = (n: N) ⇒ { this(n).toTraversable }

    /**
     * Adds a new vertice.
     */
    def +=(n: N): this.type = {
        vertices += n
        this
    }

    /**
     * Adds a new edge between the given vertices.
     *
     * (If the vertices were not previously added, they will be added.)
     */
    def +=(e: (N, N)): this.type = {
        val (s, t) = e
        this += (s, t)
    }

    /**
     * Adds a new edge between the given vertices.
     *
     * (If the vertices were not previously added, they will be added.)
     */
    def +=(s: N, t: N): this.type = {
        vertices += s += t
        successors += ((s, t :&: successors.getOrElse(s, Naught)))
        predecessors += ((t, s :&: predecessors.getOrElse(t, Naught)))
        this
    }

    /**
     * Removes the given vertice from this graph.
     */
    def -=(v: N): this.type = {
        vertices -= v
        val oldSuccessorsOpt = successors.get(v)
        oldSuccessorsOpt.foreach(_ foreach { s ⇒ predecessors(s) = predecessors(s) filter (_ != v) })
        val oldPredecessorsOpt = predecessors.get(v)
        oldPredecessorsOpt.foreach(_ foreach { s ⇒ successors(s) = successors(s) filter (_ != v) })
        successors -= v
        predecessors -= v
        this
    }

    def --=(vs: TraversableOnce[N]): this.type = { vs foreach { v ⇒ this -= v }; this }

    /**
     * Returns the set of nodes with no incoming dependencies; self-dependencies are optionally
     * ignored.
     *
     * @param   ignoreSelfRecursiveDependencies If true self-dependencies are ignored.
     *          This means that nodes that have a self dependency are considered as being root
     *          nodes if they have no further incoming dependencies.
     *
     * @return  The set of root nodes which can be freely mutated.
     * @example
     * {{{
     * scala> val g = org.opalj.graphs.Graph.empty[AnyRef] +=
     *          ("a" → "b") += ("b" → "c") += ("b" → "d") +=
     *          ("a" → "e") += ("f" -> "e") += ("y" -> "y") +=
     *          ("a" -> "f")
     * g: org.opalj.graphs.Graph[AnyRef] =
     * Graph{
     * d => {}
     * c => {}
     * a => {f,e,b}
     * b => {d,c}
     * e => {}
     * y => {y}
     * f => {e}
     * }
     *
     * scala> g.rootNodes(ignoreSelfRecursiveDependencies = true)
     * res1: scala.collection.mutable.Set[AnyRef] = Set(a)
     *
     * scala> g.rootNodes(ignoreSelfRecursiveDependencies = false)
     * res2: scala.collection.mutable.Set[AnyRef] = Set(y, a)
     * }}}
     */
    def rootNodes(ignoreSelfRecursiveDependencies: Boolean = true): Set[N] = {
        val rootNodes = vertices.clone()
        for {
            v ← vertices
            tsOpt ← successors.get(v)
            t ← tsOpt
            if ignoreSelfRecursiveDependencies || (t != v)
        } {
            rootNodes -= t
        }
        rootNodes
    }

    /**
     * All nodes which only have incoming dependencies.
     */
    def leafNodes: Set[N] = vertices.filter(v ⇒ !successors.contains(v) || successors(v).isEmpty)

    //
    // GENERATE VARIOUS KINDS OF DEBUG OUTPUTS
    //

    override def toString: String = {
        val vertices = this.vertices map { v ⇒
            successors.getOrElse(v, Nil).mkString(v.toString()+" => {", ",", "}")
        }
        vertices.mkString("Graph{\n\t", "\n\t", "\n}")
    }

    def toNodes: Iterable[Node] = {

        val nodesMap: Map[N, DefaultMutableNode[String]] = {
            vertices.map(v ⇒ (v, new DefaultMutableNode(v.toString()))).toMap
        }

        vertices.foreach { v ⇒
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

/**
 * Defines factory methods to create simple graphs.
 */
object Graph {

    implicit def intToInteger(i: Int): Integer = Integer.valueOf(i)

    implicit def AnyRefToAnyRef(o: AnyRef): AnyRef = o

    def empty[N]: Graph[N] = new Graph[N](Set.empty, LinkedHashMap.empty, LinkedHashMap.empty)

    def apply[N](edges: AMap[N, List[N]]): Graph[N] = {
        val g = Graph.empty[N]
        edges foreach { e ⇒
            val (s, ts) = e
            ts foreach { t ⇒
                g += (s → t)
            }
        }
        g
    }
}
