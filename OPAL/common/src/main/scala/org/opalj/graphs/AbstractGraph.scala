/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package graphs

/**
 * Represents a(n im)mutable (multi-)graph with (un)ordered edges.
 *
 * @author Michael Eichberg
 */
trait AbstractGraph[@specialized(Int) N] extends (N ⇒ TraversableOnce[N]) {

    def vertices: Traversable[N]

    def nonEmpty: Boolean = vertices.nonEmpty

    /** Returns a given node's successor nodes. */
    def apply(s: N): TraversableOnce[N]

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
        var rootNodes = vertices.toSet
        for {
            v ← vertices
            t ← this(v)
            if ignoreSelfRecursiveDependencies || (t != v)
        } {
            rootNodes -= t
        }
        rootNodes
    }

    //
    // GENERATE VARIOUS KINDS OF DEBUG OUTPUTS
    //

    override def toString: String = {
        val vertices = this.vertices map { v ⇒ this(v).mkString(v.toString()+" => {", ",", "}") }
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
