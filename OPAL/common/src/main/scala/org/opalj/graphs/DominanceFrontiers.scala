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

    def toDot(isIndexValid: (Int) ⇒ Boolean = (i) ⇒ true): String = {
        val g = Graph.empty[Int]
        dfs.zipWithIndex.foreach { e ⇒
            val (df, s /*index*/ ) = e
            if (isIndexValid(s)) {
                df.foreach { t ⇒ g += (t, s) }
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

    def apply(
        startNode:            Int,
        foreachSuccessorOf:   Int ⇒ ((Int ⇒ Unit) ⇒ Unit),
        foreachPredecessorOf: Int ⇒ ((Int ⇒ Unit) ⇒ Unit),
        maxNode:              Int,
        dt:                   DominatorTree
    ): DominanceFrontiers = {

        val max = maxNode + 1

        // collect child nodes (in the DT) for each node
        val children = new Array[mutable.SmallValuesSet](max)
        var i = 0
        while (i < max) {
            val d = dt.idom(i)
            val dChildren = children(d)
            children(d) =
                if (dChildren eq null) {
                    mutable.SmallValuesSet.create(max, i)
                } else {
                    i +≈: dChildren
                }
            i += 1
        }

        val dfs = new Array[SmallValuesSet](max)

        def computeDF(n: Int): Unit = {
            var s = mutable.SmallValuesSet.empty(max)
            foreachSuccessorOf(n) { y ⇒
                if (dt.dom(y) != n) s = y +≈: s
            }
            children(n).foreach { c ⇒
                computeDF(c)
                dfs(c).foreach { w ⇒
                    if (!dt.strictlyDominates(n, w))
                        s = w +≈: s
                }
            }
            dfs(n) = s
        }

        computeDF(startNode)

        new DominanceFrontiers(dfs)

    }

}
