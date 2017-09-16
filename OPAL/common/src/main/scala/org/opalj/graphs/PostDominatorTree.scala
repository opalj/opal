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

/**
 * Factory to compute classical `Post[[DominatorTree]]`s.
 *
 * @author Stephan Neumann
 * @author Michael Eichberg
 */
object PostDominatorTree {

    /**
     * Computes the post dominator tree for the given graph. The artificial start node that
     * will be created by this algorithm to ensure that we have a unique start node for
     * the post dominator tree will have the `id = (maxNodeId+1)`.
     *
     * @example
     * {{{
     * scala>//Graph: 0 -> 1->E;  1 -> 2->E
     * scala>def isExitNode(i: Int) = i == 1 || i == 2
     * isExitNode: (i: Int)Boolean
     *
     * scala>def foreachExitNode(f: Int ⇒ Unit) = { f(1); f(2) }
     * foreachExitNode: (f: Int => Unit)Unit
     *
     * scala>def foreachPredecessorOf(i: Int)(f: Int ⇒ Unit) = i match {
     *      |    case 0 ⇒
     *      |    case 1 ⇒ f(0)
     *      |    case 2 ⇒ f(1)
     *      |}
     * foreachPredecessorOf: (i: Int)(f: Int => Unit)Unit
     * scala>def foreachSuccessorOf(i: Int)(f: Int ⇒ Unit) = i match {
     *      |    case 0 ⇒ f(1)
     *      |    case 1 ⇒ f(2)
     *      |    case 2 ⇒
     *      |}
     * foreachSuccessorOf: (i: Int)(f: Int => Unit)Unit
     * scala>val pdt = org.opalj.graphs.PostDominatorTree.apply(
     *      |    isExitNode,
     *      |    foreachExitNode,
     *      |    foreachSuccessorOf,
     *      |    foreachPredecessorOf,
     *      |    2
     *      |)
     * pdt: org.opalj.graphs.DominatorTree = org.opalj.graphs.DominatorTree@3a82ac80
     * scala>pdt.toDot()
     * }}}
     *
     * @note    The underlying graph '''MUST NOT''' contain any node which is not (indirectly)
     *          connected to an exit node! If the underlying CFG contains infinite loops then
     *          the caller has to handle this case by augmenting the CFG and later on
     *          post-processing the PDT.
     *
     * @param   isExitNode A function that returns `true` if the given node – in the underlying
     *          (control-flow) graph – is an exit node; that is the node has no successors.
     * @param   foreachExitNode A function f that takes a function g with an int parameter
     *          and which executes g for each exit node.
     *          '''Note that ALL NODES have to be reachable from the exit nodes; otherwise the
     *          PostDominatorTree will be useless.'''
     * @param   infiniteLoopHeaders The first instructions of infinite loops.
     *
     * @param   maxNode The largest id used by the underlying (control-flow) graph.
     *
     */
    def apply(
        isExitNode:           Int ⇒ Boolean,
        foreachExitNode:      (Int ⇒ Unit) ⇒ Unit,
        foreachSuccessorOf:   Int ⇒ ((Int ⇒ Unit) ⇒ Unit),
        foreachPredecessorOf: Int ⇒ ((Int ⇒ Unit) ⇒ Unit),
        maxNode:              Int
    ): DominatorTreeFactory = {
        // the artificial start node
        val startNode = maxNode + 1

        // reverse flowgraph

        val revFGForeachSuccessorOf: Int ⇒ ((Int ⇒ Unit) ⇒ Unit) = (n: Int) ⇒ {
            if (n == startNode)
                foreachExitNode
            else
                foreachPredecessorOf(n)
        }

        val revFGForeachPredecessorOf: Int ⇒ ((Int ⇒ Unit) ⇒ Unit) = (n: Int) ⇒ {
            if (n == startNode) {
                DominatorTree.fornone
            } else if (isExitNode(n)) {
                // a function that expects a function that will be called for all successors
                def f(f: Int ⇒ Unit): Unit = {
                    f(startNode)
                    foreachSuccessorOf(n)(f)
                }
                f
            } else
                foreachSuccessorOf(n)
        }

        DominatorTreeFactory(
            startNode, startNodeHasPredecessors = false,
            revFGForeachSuccessorOf, revFGForeachPredecessorOf,
            maxNode = startNode // we have an additional node
        )
    }
}
