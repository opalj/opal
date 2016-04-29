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

/**
 * Factory to compute post [[DominatorTree]]s.
 *
 * @author Stephan Neumann
 */
object PostDominatorTree {

    def fornone(g: Int ⇒ Unit): Unit = { (f: (Int ⇒ Unit)) ⇒ { /*nothing to to*/ } }

    /**
     * Computes the post dominator tree for the given graph. The artificial start node of
     * the post dominator tree will have the id = (maxNodeId+1).
     */
    def apply(
        isExitNode:           Int ⇒ Boolean,
        foreachExitNode:      (Int ⇒ Unit) ⇒ Unit,
        foreachSuccessorOf:   Int ⇒ ((Int ⇒ Unit) ⇒ Unit),
        foreachPredecessorOf: Int ⇒ ((Int ⇒ Unit) ⇒ Unit),
        maxNode:              Int
    ): DominatorTree = {
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
                fornone
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

        DominatorTree(
            startNode,
            revFGForeachSuccessorOf, revFGForeachPredecessorOf,
            maxNode = startNode // we have an additional node
        )
    }

}
