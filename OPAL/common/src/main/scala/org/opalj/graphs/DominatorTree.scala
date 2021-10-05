/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package graphs

import org.opalj.collection.mutable.IntArrayStack
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.collection.immutable.IntTrieSet1

/**
 * A (standard) dominator tree.
 *
 * @note `Int => ((Int => Unit) => Unit)` is basically an `IntFunction[Consumer[IntConsumer]]`.
 *
 * @param  startNode The unique start node of the (augmented) dominator tree.
 * @param  hasVirtualStartNode `true` if the underlying cfg's startNode has a predecessor.
 *         If the start nodes had predecessors, a virtual start node was created; in this case
 *         the startNode will have an id larger than any id used by the graph and is identified by
 *         `startNode`.
 */
final class DominatorTree private (
        val startNode:            Int,
        val hasVirtualStartNode:  Boolean,
        val foreachSuccessorOf:   Int => ((Int => Unit) => Unit),
        private[graphs] val idom: Array[Int]
) extends AbstractDominatorTree {

    def isAugmented: Boolean = hasVirtualStartNode

}

/**
 * Factory to compute [[DominatorTree]]s.
 *
 * @author Stephan Neumann
 * @author Michael Eichberg
 */
object DominatorTree {

    // def fornone(g: Int => Unit): Unit = { /*nothing to do*/ }
    final val fornone: (Int => Unit) => Unit = (_: Int => Unit) => {}

    /**
     * Convenience factory method for dominator trees; see
     * [[org.opalj.graphs.DominatorTree$.apply[D<:org\.opalj\.graphs\.AbstractDominatorTree]*]]
     * for details.
     */
    def apply(
        startNode:                Int,
        startNodeHasPredecessors: Boolean,
        foreachSuccessorOf:       Int => ((Int => Unit) => Unit),
        foreachPredecessorOf:     Int => ((Int => Unit) => Unit),
        maxNode:                  Int
    ): DominatorTree = {
        this(
            startNode,
            startNodeHasPredecessors,
            foreachSuccessorOf,
            foreachPredecessorOf,
            maxNode,
            (
                startNode: Int,
                hasVirtualStartNode: Boolean,
                foreachSuccessorOf: Int => ((Int => Unit) => Unit),
                idom: Array[Int]
            ) => {
                new DominatorTree(startNode, hasVirtualStartNode, foreachSuccessorOf, idom)
            }
        )
    }

    /**
     * Computes the immediate dominators for each node of a given graph. Each node of the graph
     * is identified using a unique int value (e.g. the pc of an instruction) in the range
     * [0..maxNode], although not all ids need to be used.
     *
     * @param   startNode The id of the root node of the graph. (Often pc="0" for the CFG
     *          computed for some method; sometimes the id of an artificial start node
     *          that was created when computing the dominator tree).
     * @param   startNodeHasPredecessors If `true` an artificial start node with the id `maxNode+1`
     *          will be created and added to the graph.
     * @param   foreachSuccessorOf A function that given a node subsequently executes the given
     *          function for each direct successor of the given node.
     * @param   foreachPredecessorOf A function that - given a node - executes the given function
     *          for each direct predecessor. The signature of a function that can directly be passed
     *          as a parameter is:
     *          {{{
     *          def foreachPredecessorOf(pc: PC)(f: PC => Unit): Unit
     *          }}}
     * @param   maxNode The largest unique int id that identifies a node. (E.g., in case of
     *          the analysis of some code it is typically equivalent to the length of the code-1.)
     *
     * @return  The computed dominator tree.
     *
     * @note    This is an implementation of the "fast dominators" algorithm presented by
     *          <pre>
     *          T. Lengauaer and R. Tarjan in
     *          A Fast Algorithm for Finding Dominators in a Flowgraph
     *          ACM Transactions on Programming Languages and Systems (TOPLAS) 1.1 (1979): 121-141
     *          </pre>
     *
     *          '''This implementation does not use non-tailrecursive methods and hence
     *          also handles very large degenerated graphs (e.g., a graph which consists of a
     *          a very, very long single path.).'''
     */
    def apply[D <: AbstractDominatorTree](
        startNode:                Int,
        startNodeHasPredecessors: Boolean,
        foreachSuccessorOf:       Int => ((Int => Unit) => Unit),
        foreachPredecessorOf:     Int => ((Int => Unit) => Unit),
        maxNode:                  Int,
        dominatorTreeFactory:     ( /*startNode*/ Int, /*hasVirtualStartNode*/ Boolean, /*foreachSuccessorOf*/ Int => ((Int => Unit) => Unit), Array[Int]) => D
    ): D = {

        if (startNodeHasPredecessors) {
            val newStartNode = maxNode + 1
            create(
                newStartNode,
                true,
                /* newForeachSuccessorOf */ (n: Int) => {
                    if (n == newStartNode)
                        (f: Int => Unit) => { f(startNode) }
                    else
                        foreachSuccessorOf(n)
                },
                /* newForeachPredecessorOf */ (n: Int) => {
                    if (n == newStartNode)
                        (f: Int => Unit) => {}
                    else if (n == startNode)
                        (f: Int => Unit) => { f(newStartNode) }
                    else
                        foreachPredecessorOf(n)
                },
                newStartNode,
                dominatorTreeFactory
            );
        } else {
            create(
                startNode,
                false,
                foreachSuccessorOf,
                foreachPredecessorOf,
                maxNode,
                dominatorTreeFactory
            )
        }
    }

    private[graphs] def create[D <: AbstractDominatorTree](
        startNode:            Int,
        hasVirtualStartNode:  Boolean,
        foreachSuccessorOf:   Int => ((Int => Unit) => Unit),
        foreachPredecessorOf: Int => ((Int => Unit) => Unit),
        maxNode:              Int,
        dominatorTreeFactory: ( /*startNode*/ Int, /*hasVirtualStartNode*/ Boolean, /*foreachSuccessorOf*/ Int => ((Int => Unit) => Unit), Array[Int]) => D
    ): D = {
        val max = maxNode + 1

        var n = 0;
        val dom = new Array[Int](max)

        val parent = new Array[Int](max)
        val ancestor = new Array[Int](max)
        val vertex = new Array[Int](max + 1)
        val label = new Array[Int](max)
        val semi = new Array[Int](max)
        val bucket = new Array[IntTrieSet](max)

        // helper data-structure to resolve recursive methods
        val vertexStack = new IntArrayStack(initialSize = Math.max(2, max / 4))

        // Step 1 (assign dfsnum)
        vertexStack.push(startNode)
        while (vertexStack.nonEmpty) {
            val v = vertexStack.pop()
            // The following "if" is necessary, because the recursive DFS impl. in the paper
            // performs an eager decent. This may already initialize a node that is also pushed
            // on the stack and, hence, must not be visited again.
            if (semi(v) == 0) {
                n = n + 1
                semi(v) = n
                label(v) = v
                vertex(n) = v
                dom(v) = v

                foreachSuccessorOf(v) { w =>
                    if (semi(w) == 0) {
                        parent(w) = v
                        vertexStack.push(w)
                    }
                }
            }
        }

        // Steps 2 & 3
        def eval(v: Int): Int = {
            if (ancestor(v) == 0) {
                v
            } else {
                compress(v)
                label(v)
            }
        }

        // // PAPER VERSION USING RECURSION
        // def compress(v: Int): Unit = {
        //     var theAncestor = ancestor(v)
        //     if (ancestor(theAncestor) != 0) {
        //         compress(theAncestor)
        //         theAncestor = ancestor(v)
        //         val ancestorLabel = label(theAncestor)
        //         if (semi(ancestorLabel) < semi(label(v))) {
        //             label(v) = ancestorLabel
        //         }
        //         ancestor(v) = ancestor(theAncestor)
        //     }
        // }

        def compress(v: Int): Unit = {
            // 1. walk the path
            {
                var w = v
                while (ancestor(ancestor(w)) != 0) {
                    vertexStack.push(w)
                    w = ancestor(w)
                }
            }

            // 2. compress
            while (vertexStack.nonEmpty) {
                val w = vertexStack.pop()
                val theAncestor = ancestor(w)
                val ancestorLabel = label(theAncestor)
                if (semi(ancestorLabel) < semi(label(w))) {
                    label(w) = ancestorLabel
                }
                ancestor(w) = ancestor(theAncestor)
            }
        }

        var i = n
        while (i >= 2) {
            val w = vertex(i)

            // Step 2
            foreachPredecessorOf(w) { v: Int =>
                val u = eval(v)
                val uSemi = semi(u)
                if (uSemi < semi(w)) {
                    semi(w) = uSemi
                }
            }

            val v = vertex(semi(w))
            val b = bucket(v)
            bucket(v) = if (b ne null) { b + w } else { IntTrieSet1(w) }

            ancestor(w) = parent(w)

            // Step 3
            val wParent = parent(w)
            val wParentBucket = bucket(wParent)
            if (wParentBucket != null) {
                for (v <- wParentBucket) {
                    val u = eval(v)
                    dom(v) = if (semi(u) < semi(v)) u else wParent
                }
                bucket(wParent) = null
            }
            i = i - 1
        }

        // Step 4
        var j = 2;
        while (j <= n) {
            val w = vertex(j)
            val domW = dom(w)
            if (domW != vertex(semi(w))) {
                dom(w) = dom(domW)
            }
            j = j + 1
        }

        dominatorTreeFactory(startNode, hasVirtualStartNode, foreachSuccessorOf, dom)
    }

}
