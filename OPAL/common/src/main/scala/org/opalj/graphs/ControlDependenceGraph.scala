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

import scala.collection.mutable.BitSet
import org.opalj.collection.immutable.IntArraySet
import org.opalj.collection.mutable.IntArrayStack

/**
 * Represents the control-dependence information.
 *
 * An instruction/statement is control dependent on a predicate (here: `if`, `switch` or any
 * instruction that may throw an exception) if the value of the predicate
 * controls the execution of the instruction.
 * Let G be a control flow graph; Let X and Y be nodes in G; Y is control dependent on X iff
 * there exists a directed path P from X to Y with any Z in P \ X is not post-dominated by Y.
 *
 * Note that in the context of static analysis an invocation instruction that may throw an
 * exception, which may result in a different control-flow, is also a `predicate` additionally to
 * all ifs and switches.
 *
 * @author Michael Eichberg
 */
final class ControlDependencies private[graphs] (val dominanceFrontiers: DominanceFrontiers) {

    /**
     * @return  The nodes/basic blocks on which the given node/basic block is '''directly'''
     *          control dependent on. That is, the set of nodes which directly control whether x is
     *          executed or not.
     *          '''Directly''' means that there is at least one path  between a node Y in
     *          `Control(X)/*the returned set*/` and X, whose selection is controlled by Y and
     *          which contains no nodes that may prevent the execution of X.
     */
    def xIsDirectlyControlDependentOn(x: Int): IntArraySet = {
        dominanceFrontiers(x)
    }

    /**
     * Calls the function `f` with those nodes on which the given node `x` is control
     * dependent on.
     */
    def xIsControlDependentOn(x: Int)(f: Int ⇒ Unit): Unit = {
        val maxNodeId = dominanceFrontiers.maxNode

        // TODO Evaluate if a typed chain or an IntArraySet is more efficient...
        val seen = new BitSet(dominanceFrontiers.maxNode)
        val worklist = new IntArrayStack(Math.min(10, maxNodeId / 3))
        worklist.push(x)

        //var worklist = List(x)
        while (worklist.nonEmpty) {
            //  val x = worklist.head
            //worklist = worklist.tail
            val x = worklist.pop()

            dominanceFrontiers(x).foreach { y ⇒
                if (!seen.contains(y)) {
                    //seen = y +≈: seen
                    seen += y
                    //worklist = y +≈: worklist
                    //worklist ::= y
                    worklist.push(y)
                    f(y)
                }
            }
        }
    }
}

/**
 * Factory to compute the control-dependence graph (based on the [[PostDominatorTree]]).
 *
 * The control dependence graph is effectively directly based on the dominance frontiers computed
 * using the dominator tree for the reverse control-flow graph (aka post dominator tree).
 *
 * @example The following example demonstrates this.
 * {{{
 * // A graph taken from the paper:
 * // Efficiently Computing Static Single Assignment Form and the Control Dependence Graph
 * val g = org.opalj.graphs.Graph.empty[Int] +=
 *          (0 → 1) += (1 → 2) += (2 → 3) += (2 → 7) += (3 → 4) += (3->5) += (5->6) += (4->6) +=
 *          (6->8) += (7->8)  += (8->9) += (9->10) += (9->11) += (10->11) += (11->9) +=
 *          (11 -> 12) += (12 -> 13) += (12 ->2) += (0 -> 13)
 * import org.opalj.graphs.DominatorTreeFactory
 * val foreachSuccessor = (n: Int) ⇒ g.successors.getOrElse(n, List.empty).foreach _
 * val foreachPredecessor = (n: Int) ⇒ g.predecessors.getOrElse(n, List.empty).foreach _
 * val dtf = DominatorTreeFactory(0, false, foreachSuccessor, foreachPredecessor, 13)
 * val isValidNode = (n : Int) => n>= 0 && n <= 13
 * org.opalj.io.writeAndOpen(dtf.dt.toDot(),"g",".dt.gv")
 * val df = org.opalj.graphs.DominanceFrontiers(dtf,isValidNode)
 * org.opalj.io.writeAndOpen(df.toDot(),"g",".df.gv")
 * val pdtf = org.opalj.graphs.PostDominatorTree(
 * (i : Int) => i == 13, (f : Int => Unit) => f(13),
 * foreachSuccessor,foreachPredecessor,
 * 13)
 * val rdf =  org.opalj.graphs.DominanceFrontiers(pdtf, isValidNode)
 * org.opalj.io.writeAndOpen(rdf.toDot(isValidNode),"g",".rdf.gv")
 * }}}
 *
 * @author Michael Eichberg
 */
object ControlDependenceGraph {

    /**
     * Computes the control-dependence graph. The artificial start node of
     * the internally used post dominator tree will have the id = (maxNodeId+1).
     *
     * A node (basic block) Y is control-dependent on another X iff X determines whether Y
     * executes, i.e.
     *  -   there exists a path from X to Y such that every node in the path other than X & Y is
     *      post-dominated by Y
     *  -   X is not post-dominated by Y
     *
     * @return The tuple `(`(Post)`[[DominatorTree]], [[ControlDependencies]])`
     */
    def apply(
        isExitNode:           Int ⇒ Boolean,
        foreachExitNode:      (Int ⇒ Unit) ⇒ Unit,
        foreachSuccessorOf:   Int ⇒ ((Int ⇒ Unit) ⇒ Unit),
        foreachPredecessorOf: Int ⇒ ((Int ⇒ Unit) ⇒ Unit),
        maxNode:              Int,
        isValidNode:          (Int) ⇒ Boolean
    ): (DominatorTree, ControlDependencies) = {

        val pdtf =
            PostDominatorTree(
                isExitNode, foreachExitNode, foreachSuccessorOf, foreachPredecessorOf, maxNode
            )

        this(pdtf, DominanceFrontiers(pdtf, isValidNode))
    }

    /**
     * @param   pdtf A (Post)DominatorTreeFactory that creates the post dominator tree.
     * @see     [[PostDominatorTree$]] and [[DominanceFrontiers]]
     */
    def apply(
        pdtf:        DominatorTreeFactory,
        isValidNode: (Int) ⇒ Boolean
    ): ControlDependencies = {
        new ControlDependencies(DominanceFrontiers(pdtf, isValidNode))
    }

    /**
     * @param   pdtf A (Post)DominatorTreeFactory that creates the post dominator tree.
     * @param   rdf The reverse dominance frontiers. I.e., the dominance frontiers computed using
     *          the post dominator tree computed by the `pdtf`.
     * @see     [[PostDominatorTree$]] and [[DominanceFrontiers]]
     */
    def apply(
        pdtf: DominatorTreeFactory,
        rdf:  DominanceFrontiers
    ): (DominatorTree, ControlDependencies) = {
        val pdt = pdtf.dt
        (pdt, new ControlDependencies(rdf))
    }

}
