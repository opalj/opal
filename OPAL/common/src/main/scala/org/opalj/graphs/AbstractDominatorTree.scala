/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package graphs

import scala.annotation.tailrec

import scala.collection.immutable.ArraySeq

/**
 * Representation of a (post) dominator tree of, for example, a control flow graph.
 * To construct a '''(post)dominator tree''' use the companion objects' factory methods (`apply`).
 *
 * @author Michael Eichberg
 */
abstract class AbstractDominatorTree {

    // PROPERTIES OF THE SOURCE GRAPHS
    //

    /**
     * The unique start-node of the dominator tree. If the graph is augmented, this maybe a
     * virtual node.
     */
    val startNode: Int

    /**
     * Executes the given function `f` for each successor instruction of `pc`.
     *
     * Defined w.r.t. the (implicitly) augmented CFG.
     */
    val foreachSuccessorOf: Int => ((Int => Unit) => Unit) // IntFunction[Consumer[IntConsumer]]

    // PROPERTIES OF THE TREE
    //

    /**
     * If `true` the underlying cfg was augmented. To determine in which way the dominator tree
     * is augmented, a pattern match should be used. E.g., the [[DominatorTree]] may an additional
     * node which replaces the underlying CFG's root node.
     */
    def isAugmented: Boolean

    /**
     * The array contains for each node its immediate dominator.
     * If not all unique ids are used, then the array is a sparse array and external
     * knowledge is necessary to determine which elements of the array contain useful
     * information.
     */
    private[graphs] val idom: Array[Int]

    final def maxNode: Int = idom.length - 1

    assert(startNode <= maxNode, s"start node ($startNode) out of range ([0,$maxNode])")

    /**
     * Returns the immediate dominator of the node with the given id.
     *
     * @note   The root node does not have a(n immediate) dominator; in case `n`
     *         is the root node an `IllegalArgumentException` is thrown.
     *
     * @param n The id of a valid node which is not the `startNode`.
     * @return The id of the node which immediately dominates the given node.
     */
    final def dom(n: Int): Int = {
        if (n == startNode) {
            val errorMessage = s"the root node $startNode(max=$maxNode) cannot be dominated"
            throw new IllegalArgumentException(errorMessage)
        }

        idom(n)
    }

    /**
     * @param n a valid node of the graph.
     * @param w a valid node of the graph.
     * @return `true` if `n` strictly dominates `w`.
     */
    @tailrec final def strictlyDominates(n: Int, w: Int): Boolean = {
        if (n == w)
            // a node never strictly dominates itself
            return false;

        val wIDom = idom(w)
        wIDom == n || (wIDom != startNode && strictlyDominates(n, wIDom))
    }

    /**
     * Iterates over all dominator nodes of the given node and calls the given function f for each
     * dominator node.
     * Iteration starts with the immediate dominator of the given node if reflexive is `false` and
     * starts with the node itself if reflexive is `true`.
     *
     * @param n The id of a valid node.
     */
    final def foreachDom[U](n: Int, reflexive: Boolean = false)(f: Int => U): Unit = {
        if (n != startNode || reflexive) {
            var c = if (reflexive) n else idom(n)
            while (c != startNode) {
                f(c)
                c = idom(c)
            }
            f(startNode)
        }
    }

    /**
     * The array which stores the immediate dominator for each node.
     */
    def immediateDominators: ArraySeq[Int] = ArraySeq.unsafeWrapArray(idom)

    /**
     * (Re-)computes the dominator tree's leaf nodes. Due to the way the graph is stored,
     * this method has a complexity of O(2n). Hence, if the leaves are required more than
     * once, storing/caching them should be considered.
     */
    def leaves(isIndexValid: Int => Boolean = _ => true): List[Int] = {
        // A leaf is a node which does not dominate another node.
        var i = 0
        val max = idom.length

        // first loop - identify nodes which dominate other nodes
        val dominates = new Array[Boolean](max)
        while (i < max) {
            if (i != startNode && isIndexValid(i)) {
                dominates(idom(i)) = true // negative values are not used to identify "normal" nodes
            }
            i += 1
        }
        // second loop - collect leaves
        var theLeaves = List.empty[Int]
        i = 0
        while (i < max) {
            if (isIndexValid(i) && !dominates(i)) {
                theLeaves ::= i
            }
            i += 1
        }
        theLeaves

    }

    /**
     * Returns a Graphviz based visualization of this dominator tree.
     *
     * @param   isIndexValid A function that returns `true` if an element in the iDom array with a
     *          specific index is actually identifying a node. This is particularly useful/
     *          required if the `idom` array given at initialization time is a sparse array.
     */
    def toDot(isIndexValid: Int => Boolean = _ => true): String = {
        val g = Graph.empty[Int]
        idom.zipWithIndex.foreach { e =>
            val (t, s /*index*/ ) = e
            if (isIndexValid(s) && s != startNode)
                g.addEdge(t, s)
        }
        g.toDot(rankdir = "BT", ranksep = "0.3")
    }
}
