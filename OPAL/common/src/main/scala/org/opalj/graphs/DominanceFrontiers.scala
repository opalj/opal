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

import org.opalj.collection.SmallValuesSet
import org.opalj.collection.immutable
import org.opalj.collection.mutable

/**
 * @author Michael Eichberg
 */
final class DominanceFrontiers private (private final val dfs: Array[SmallValuesSet]) {

    /**
     * Returns the of nodes in the dominance frontier of the given node.
     */
    final def df(n: Int): SmallValuesSet = {
        val df = dfs(n)
        if (df eq null)
            immutable.EmptySmallValuesSet
        else
            df
    }

    def dominanceFrontiers: IndexedSeq[SmallValuesSet] = dfs

    def toDot(isNodeValid: (Int) ⇒ Boolean = (i) ⇒ true): String = {
        val g = Graph.empty[Int]
        dfs.zipWithIndex.foreach { e ⇒
            val (df, s /*index*/ ) = e
            if (isNodeValid(s)) {
                df.foreach { t ⇒ g += (s, t) }
            }
        }
        g.toDot(rankdir = "BT", dir = "forward", ranksep = "0.3")
    }
}

/**
 * Factory to compute [[DominanceFrontiers]].
 *
 * @author Michael Eichberg
 */
object DominanceFrontiers {

    /**
     * Computes the dominance frontiers for each node of a given graph G.
     *
     * @example
     * {{{
     * // A graph taken from the paper:
     * // Efficiently Computing Static Single Assignment Form and the Control Dependence Graph
val g = org.opalj.graphs.Graph.empty[Int] += (0 → 1) += (1 → 2) += (2 → 3) += (2 → 7) += (3 → 4) += (3->5) += (5->6) += (4->6) += (6->8) += (7->8)  += (8->9) += (9->10) += (9->11) += (10->11) += (11->9) += (11 -> 12) += (12 -> 13) += (12 ->2) += (0 -> 13)
val foreachSuccessor = (n: Int) ⇒ g.successors.getOrElse(n, List.empty).foreach _
val foreachPredecessor = (n: Int) ⇒ g.predecessors.getOrElse(n, List.empty).foreach _
val dtf = org.opalj.graphs.DominatorTreeFactory(0, false, foreachSuccessor, foreachPredecessor, 13)
val isValidNode = (n : Int) => n>= 0 && n <= 13
org.opalj.io.writeAndOpen(dtf.dt.toDot(),"g",".dt.gv")
val df = org.opalj.graphs.DominanceFrontiers(dtf,isValidNode)
org.opalj.io.writeAndOpen(df.toDot(),"g",".df.gv")


// A degenerated graph which consists of a single node that has a self-references.
val g = org.opalj.graphs.Graph.empty[Int] += (0 → 0) 
val foreachSuccessor = (n: Int) ⇒ g.successors.getOrElse(n, List.empty).foreach _
val foreachPredecessor = (n: Int) ⇒ g.predecessors.getOrElse(n, List.empty).foreach _
val dtf = org.opalj.graphs.DominatorTreeFactory(0, true, foreachSuccessor, foreachPredecessor, 0)
val isValidNode = (n : Int) => n == 0
org.opalj.io.writeAndOpen(dtf.dt.toDot(),"g",".dt.gv")
val df = org.opalj.graphs.DominanceFrontiers(dtf,isValidNode)
org.opalj.io.writeAndOpen(df.toDot(),"g",".df.gv")
     * }}}
     * @param dt The dominator tree of the specified (flow) graph. In case of the reverse flow
     * 			graph you have to give the [[DominatorTree]] computed using [[PostDominatorTree$]].
     */
    def apply(
        dtf:         DominatorTreeFactory,
        isValidNode: (Int) ⇒ Boolean
    ): DominanceFrontiers = {

        import dtf.{startNode, maxNode, foreachSuccessorOf, dt}
        val max = maxNode + 1

        // pre-collect the child nodes (in the DT) for each node
        val children = new Array[mutable.SmallValuesSet](max)
        var i = 0
        while (i < max) {
            if (isValidNode(i) && i != startNode) {
                val d = dt.idom(i)
                val dChildren = children(d)
                children(d) =
                    if (dChildren eq null) {
                        mutable.SmallValuesSet.create(max, i)
                    } else {
                        i +≈: dChildren
                    }
            }
            i += 1
        }

        // println(children.map(c ⇒ if (c eq null) "null" else c.mkString("{", ",", "}")).zipWithIndex.map(_.swap).mkString(" - "))

        val dfs = new Array[SmallValuesSet](max)

        def computeDF(n: Int): Unit = {
            var s = mutable.SmallValuesSet.empty(max)
            foreachSuccessorOf(n) { y ⇒
                if (dt.dom(y) != n) {
                    s = y +≈: s
                    // println(s"local($n):"+s.mkString("(", ",", ")"))
                }
            }
            val nChildren = children(n)
            if (nChildren ne null) {
                children(n).foreach { c ⇒
                    computeDF(c)
                    dfs(c).foreach { w ⇒
                        if (!dt.strictlyDominates(n, w)) {
                            s = w +≈: s
                            // println(s"up($n):"+s.mkString("(", ",", ")"))
                        }
                    }
                }
            }
            // println(n+"=>"+s.mkString("(", ",", ")"))
            dfs(n) = s
        }

        computeDF(startNode)

        new DominanceFrontiers(dfs)

    }

}
