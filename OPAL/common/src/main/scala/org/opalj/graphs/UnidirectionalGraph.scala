/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package graphs

import org.opalj.collection.IntIterator
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

    override def apply(s: Int): IterableOnce[Int] = theSuccessors(s).iterator

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

    def edges: Int => IntIterator = (n: Int) => { theSuccessors(n).iterator }

    /**
     * Adds a new edge between the given vertices.
     *
     * (If the vertices were not previously added, they will be added.)
     */
    def add(s: Int, t: Int): this.type = {
        successors(s) = theSuccessors(s) + t
        this
    }

    def sccs(filterSingletons: Boolean = false): List[List[Int]] = {
        org.opalj.graphs.sccs(verticesCount, edges, filterSingletons)
    }
}
