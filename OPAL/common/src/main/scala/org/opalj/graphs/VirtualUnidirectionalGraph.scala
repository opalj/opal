/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package graphs

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
        val successors:    Int => IntIterator
) extends AbstractGraph[Int] {

    def vertices: Range = 0 until this.verticesCount

    override def nonEmpty: Boolean = verticesCount > 0

    override def apply(s: Int): IterableOnce[Int] = theSuccessors(s)

    /**
     * Returns a node's successors.
     */
    def theSuccessors(s: Int): IntIterator = successors(s)

    def sccs(filterSingletons: Boolean = false): List[List[Int]] = {
        org.opalj.graphs.sccs(verticesCount, successors, filterSingletons)
    }
}
