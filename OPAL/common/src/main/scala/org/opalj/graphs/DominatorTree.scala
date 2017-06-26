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

import org.opalj.collection.mutable.IntArrayStack

/**
 * The (post) dominator tree of, for example, a control flow graph.
 * To construct a '''dominator tree''' use the companion object's factory method (`apply`).
 * To compute a '''post dominator tree''' use the factory
 * method defined in [[org.opalj.graphs.PostDominatorTree]].
 *
 * @param   idom An array that contains for each node its immediate dominator.
 *          If not all unique ids are used, then the array is a sparse array and external
 *          knowledge is necessary to determine which elements of the array contain useful
 *          information.
 *
 * @author Michael Eichberg
 */
final class DominatorTree private (
        private[graphs] final val idom: Array[Int],
        final val startNode:            Int
) {

    final def maxNode: Int = idom.length - 1

    assert(startNode <= maxNode, s"start node ($startNode) out of range ([0,$maxNode])")

    /**
     * Returns the immediate dominator of the node with the given id.
     *
     * @note The root node does not have an immediate dominator!
     *
     * @param n The id of a valid node which is not the `startNode`.
     * @return The id of the node which immediately dominates the given node.
     */
    final def dom(n: Int): Int = {
        if (n == startNode) {
            val errorMessage = s"the root node ($startNode) does not have an immediate dominator"
            throw new IllegalArgumentException(errorMessage)
        }

        idom(n)
    }

    /**
     * @param n a valid node of the graph.
     * @param w a valid node of the graph.
     * @return `true` if `n` strictly dominates `w`.
     */
    @scala.annotation.tailrec final def strictlyDominates(n: Int, w: Int): Boolean = {
        if (n == w)
            // a node never strictly dominates itself
            return false;

        val wIDom = idom(w)
        wIDom == n || (wIDom != startNode && strictlyDominates(n, wIDom))
    }

    /**
     * Iterates over all dominator nodes of the given node and calls the given function f each
     * dominator node.
     * Iteration starts with the immediate dominator of the given node if reflexive is `false` and
     * starts with the node itself if reflexive is `true`.
     *
     * @param n The id of a valid node.
     */
    final def foreachDom[U](n: Int, reflexive: Boolean = false)(f: Int ⇒ U): Unit = {
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
    def immediateDominators: IndexedSeq[Int] = idom

    /**
     * @param   isIndexValid A function that returns `true` if an element in the iDom array with a
     *          specific index is actually containing some valid data. This is particularly useful/
     *          required if the `idom` array given at initialization time is a sparse array.
     */
    def toDot(isIndexValid: (Int) ⇒ Boolean = (i) ⇒ true): String = {
        val g = Graph.empty[Int]
        idom.zipWithIndex.foreach { e ⇒
            val (t, s /*index*/ ) = e
            if (isIndexValid(s) && s != startNode)
                g += (t, s)
        }
        g.toDot(rankdir = "BT", dir = "forward", ranksep = "0.3")
    }

    // THE FOLLOWING FUNCTION IS REALLY EXPENSIVE (DUE TO (UN)BOXING)
    // AND THEREFORE NO LONGER SUPPORTED
    // def toMap(nodes: Traversable[Int]): mutable.Map[Int, List[Int]] = {
    //     val dominators = mutable.Map(0 → List(0))
    //
    //     for (n ← nodes if !dominators.contains(n)) {
    //         // Since we traverse the dom tree no "visited" checks are necessary.
    //
    //         // The method needs to be tail recursive to be able to handle "larger graphs"
    //         // which are,e.g., generated by large methods.
    //         @tailrec def traverseDomTree(path: List[Int]): List[Int] = {
    //             val node = path.head
    //             dominators.get(node) match {
    //                case Some(nodeDoms) ⇒
    //                     // we have found a node for which we already have the list of dominators
    //                     var accDoms = nodeDoms
    //                     path.tail foreach { n ⇒
    //                         accDoms ::= n
    //                         // let's also update the map to speed up overall processing
    //                         dominators(n) = accDoms
    //                     }
    //                     accDoms
    //
    //                 case None ⇒
    //                     traverseDomTree(dom(node) :: path)
    //             }
    //         }
    //         dominators(n) = traverseDomTree(List(n))
    //     }
    //
    //     dominators
    // }

}

/**
 * Factory to compute [[DominatorTree]]s.
 *
 * @author Stephan Neumann
 * @author Michael Eichberg
 */
object DominatorTree {

    /**
     * Computes the immediate dominators for each node of a given graph. Each node of the graph
     * is identified using a unique int value (e.g. the pc of an instruction) in the range
     * [0..maxNode], although not all ids need to be used.
     *
     * @param   startNode The id of the root node of the graph. (E.g., (pc=)"0" for the CFG
     *          computed for some method or the id of the artificial start node created when
     *          computing a reverse CFG.
     * @param   startNodeHasPredecessors If `true` an artificial start node with the id `maxNode+1`
     *          will be created and added to the graph.
     * @param   foreachSuccessorOf A function that given a node subsequently executes the given
     *          function for each direct successor of the given node.
     * @param   foreachPredecessorOf A function that given a node executes the given function for
     *          each direct predecessor. The signature of a function that can directly be passed
     *          as a parameter is:
     *          {{{
     *          def foreachPredecessorOf(pc: PC)(f: PC ⇒ Unit): Unit
     *          }}}
     * @param   maxNode The largest unique int id that identifies a node. (E.g., in case of
     *          the analysis of some code it is equivalent to the length of the code-1.)
     *
     * @return  The computed dominator tree.
     *
     * @note    This is an implementation of the "fast dominators" algorithm
     *          presented by T. Lengauaer and R. Tarjan in
     *          A Fast Algorithm for Finding Dominators in a Flowgraph
     *          ACM Transactions on Programming Languages and Systems (TOPLAS) 1.1 (1979): 121-141
     *
     *          '''This implementation does not use non-tailrecursive methods anymore and hence
     *          also handles very large degenerated graphs (e.g., a graph which consists of a
     *          a very long single path.).'''
     */
    def apply(
        startNode:                Int,
        startNodeHasPredecessors: Boolean,
        foreachSuccessorOf:       Int ⇒ ((Int ⇒ Unit) ⇒ Unit),
        foreachPredecessorOf:     Int ⇒ ((Int ⇒ Unit) ⇒ Unit),
        maxNode:                  Int
    ): DominatorTree = {

        if (startNodeHasPredecessors) {
            val newStartNode = maxNode + 1
            return this(
                newStartNode,
                false,
                /* newForeachSuccessorOf */ (n: Int) ⇒ {
                    if (n == newStartNode)
                        (f: Int ⇒ Unit) ⇒ { f(startNode) }
                    else
                        foreachSuccessorOf(n)
                },
                /* newForeachPredecessorOf */ (n: Int) ⇒ {
                    if (n == newStartNode)
                        (f: Int ⇒ Unit) ⇒ {}
                    else if (n == startNode)
                        (f: Int ⇒ Unit) ⇒ { f(newStartNode) }
                    else
                        foreachPredecessorOf(n)
                },
                newStartNode
            );
        }

        val max = maxNode + 1

        var n = 0;
        val dom = new Array[Int](max)

        val parent = new Array[Int](max)
        val ancestor = new Array[Int](max)
        val vertex = new Array[Int](max + 1)
        val label = new Array[Int](max)
        val semi = new Array[Int](max)
        val bucket = new Array[Set[Int]](max)

        // helper data-structure to resolve recursive methods
        val vertexStack = new IntArrayStack(initialSize = Math.max(2, (max / 4)))

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

                foreachSuccessorOf(v) { w ⇒
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
            foreachPredecessorOf(w) { (v: Int) ⇒
                val u = eval(v)
                val uSemi = semi(u)
                if (uSemi < semi(w)) {
                    semi(w) = uSemi
                }
            }

            val v = vertex(semi(w))
            val b = bucket(v)
            bucket(v) = if (b ne null) { b + w } else { Set(w) }

            ancestor(w) = parent(w)

            // Step 3
            val wParent = parent(w)
            val wParentBucket = bucket(wParent)
            if (wParentBucket != null) {
                for (v ← wParentBucket) {
                    val u = eval(v)
                    dom(v) = if (semi(u) < semi(v)) u else wParent;
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

        new DominatorTree(dom, startNode)
    }

}
