/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package graphs

import org.opalj.collection.immutable.IntArraySet
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.collection.immutable.IntRefPair
import org.opalj.collection.mutable.FixedSizeBitSet
import org.opalj.collection.mutable.IntArrayStack

import scala.collection.immutable.ArraySeq

/**
 * Representation of the dominance frontiers.
 *
 * @author Michael Eichberg
 */
final class DominanceFrontiers private (
        private val dfs: Array[IntArraySet] // IMPROVE Consider using an IntTrieSet (if it doesn't work, document it!)
) extends ControlDependencies {

    def apply(n: Int): IntArraySet = df(n)

    def maxNode: Int = dfs.length - 1

    /**
     * Returns the nodes in the dominance frontier of the given node.
     */
    def df(n: Int): IntArraySet = {
        val df = dfs(n)
        if (df eq null)
            IntArraySet.empty
        else
            df
    }

    def transitiveDF(n: Int): IntTrieSet = {
        var transitiveDF = IntTrieSet.empty ++ this.df(n).iterator
        var nodesToVisit = transitiveDF - n
        while (nodesToVisit.nonEmpty) {
            val IntRefPair(nextN, newNodesToVisit) = nodesToVisit.headAndTail
            nodesToVisit = newNodesToVisit
            val nextDF = this.df(nextN)
            transitiveDF ++= nextDF.iterator
            nodesToVisit ++= nextDF.iterator.filter(_ != nextN)
        }
        transitiveDF
    }

    def dominanceFrontiers: IndexedSeq[IntArraySet] = ArraySeq.unsafeWrapArray(dfs)

    def xIsDirectlyControlDependentOn(x: Int): IntArraySet = df(x)

    def xIsControlDependentOn(x: Int)(f: Int => Unit): Unit = {
        val maxNodeId = maxNode

        // IMPROVE Evaluate if a typed chain or an Int(Array|Trie)Set is more efficient...
        val seen = FixedSizeBitSet.create(maxNodeId)
        val worklist = new IntArrayStack(Math.min(10, maxNodeId / 3))
        worklist.push(x)

        do {
            val x = worklist.pop()

            df(x) foreach { y =>
                if (!seen.contains(y)) {
                    seen += y
                    worklist.push(y)
                    f(y)
                }
            }
        } while (worklist.nonEmpty)
    }

    //
    //
    // DEBUGGING RELATED FUNCTIONALITY
    //
    //

    /**
     * Creates a dot graph which depicts the dominance frontiers.
     *
     * @param   isNodeValid A function that returns `true` if the given int value
     *          identifies a valid node. If the underlying graph is not a sparse
     *          graph; i.e., if every index in the range [0...maxNode] identifies
     *          a valid node, then the default function, which always returns `true`,
     *          can be used.
     */
    def toDot(isNodeValid: Int => Boolean = _ => true): String = {
        val g = Graph.empty[Int]
        dfs.zipWithIndex.foreach { e =>
            val (df, s /*index*/ ) = e
            if (isNodeValid(s)) {
                if (df == null) {
                    g addVertice s
                } else {
                    df.foreach { t => g.addEdge(s, t) }
                }
            }
        }
        g.toDot(rankdir = "BT", ranksep = "0.3")
    }
}

/**
 * Factory to compute [[DominanceFrontiers]].
 *
 * @author Michael Eichberg
 */
object DominanceFrontiers {

    /**
     * Computes the dominance frontiers for each node of a graph G using the (post) dominator tree.
     *
     * @example
     * {{{
     * // A graph taken from the paper:
     * // Efficiently Computing Static Single Assignment Form and the Control Dependence Graph
     * val g = org.opalj.graphs.Graph.empty[Int] += (0 -> 1) += (1 -> 2) += (2 -> 3) += (2 -> 7) += (3 -> 4) += (3->5) += (5->6) += (4->6) += (6->8) += (7->8)  += (8->9) += (9->10) += (9->11) += (10->11) += (11->9) += (11 -> 12) += (12 -> 13) += (12 ->2) += (0 -> 13)
     * val foreachSuccessor = (n: Int) => g.successors.getOrElse(n, List.empty).foreach _
     * val foreachPredecessor = (n: Int) => g.predecessors.getOrElse(n, List.empty).foreach _
     * val dt = org.opalj.graphs.DominatorTree(0, false, foreachSuccessor, foreachPredecessor, 13)
     * val isValidNode = (n : Int) => n>= 0 && n <= 13
     * org.opalj.io.writeAndOpen(dt.toDot(),"g",".dt.gv")
     * val df = org.opalj.graphs.DominanceFrontiers(dt,isValidNode)
     * org.opalj.io.writeAndOpen(df.toDot(),"g",".df.gv")
     *
     *
     * // A degenerated graph which consists of a single node that has a self-reference.
     * val g = org.opalj.graphs.Graph.empty[Int] += (0 -> 0)
     * val foreachSuccessor = (n: Int) => g.successors.getOrElse(n, List.empty).foreach _
     * val foreachPredecessor = (n: Int) => g.predecessors.getOrElse(n, List.empty).foreach _
     * val dtf = org.opalj.graphs.DominatorTreeFactory(0, true, foreachSuccessor, foreachPredecessor, 0)
     * val isValidNode = (n : Int) => n == 0
     * org.opalj.io.writeAndOpen(dtf.dt.toDot(),"g",".dt.gv")
     * val df = org.opalj.graphs.DominanceFrontiers(dtf,isValidNode)
     * org.opalj.io.writeAndOpen(df.toDot(),"g",".df.gv")
     * }}}
     *
     * @param   dt The dominator tree of the specified (flow) graph. We provide basic support
     *          for augmented post dominator trees: [[PostDominatorTree]]; we in particular
     *          handle common cases related to additional exit nodes as created by the implented(!)
     *          post dominator tree computation algorithm.
     *          However, the following case:
     *          {{{
     *          while (true) {
     *               if (i < 0) {
     *                   i += 1000;
     *                   // Exit Piont 1
     *               } else {
     *                   i -= 100;
     *                   // Exit Point 2
     *               }
     *          }
     *          }}}
     *          is not yet supported; it would require a significant transformation of the
     *          computed PDT, which we currently do not perform.
     *          Basically, in the PDT we would need to make both bodies dependent on the
     *          artifical exit node of the loop to ensure that both bodies are control-dependent
     *          on the "if" node.
     *
     * @param   isValidNode A function that returns `true` if the given id represents a node of the
     *          underlying graph. If the underlying graph contains a single, new artificial start
     *          node then this node may or may not be reported as a valid node; this is not relevant
     *          for this algorithm.
     */
    def apply(dt: AbstractDominatorTree, isValidNode: Int => Boolean): DominanceFrontiers = {
        val startNode: Int = dt.startNode
        val foreachSuccessorOf = dt.foreachSuccessorOf
        val max = dt.maxNode + 1

        val potentialChildrenCount = 3
        val children = new Array[IntArrayStack](max)
        var i = 0
        while (i < max) {
            if (isValidNode(i) && i != startNode) {
                val d = dt.idom(i)
                val dChildren = children(d)
                if (dChildren eq null) {
                    val child = new IntArrayStack(potentialChildrenCount)
                    child.push(i)
                    children(d) = child
                } else {
                    dChildren.push(i)
                }
            }
            i += 1
        }

        var dfs /* dominanceFrontiers */ = new Array[IntArraySet](max)

        @inline def dfLocal(n: Int): IntArraySet = {
            var s = IntArraySet.empty
            try {
                foreachSuccessorOf(n) { y => if (dt.dom(y) != n) s = s + y }
            } catch {
                case t: Throwable =>
                    throw new Throwable(s"failed iterating over successors of node $n", t)
            }
            s
        }

        val inDFSOrder = new IntArrayStack(Math.max(max - 2, 2))
        var nodes: List[Int] = List(startNode)
        while (nodes.nonEmpty) {
            val n = nodes.head
            nodes = nodes.tail
            val nChildren = children(n)
            if (nChildren ne null) {
                inDFSOrder.push(n)
                nChildren.foreach { nodes ::= _ }
            } else {
                // we immediately compute the dfs_local information
                dfs(n) = dfLocal(n)
            }
        }

        inDFSOrder foreach { n =>
            val s = children(n).foldLeft(dfLocal(n)) { (s, c) =>
                dfs(c).foldLeft(s) { (s, w) =>
                    if (!dt.strictlyDominates(n, w)) {
                        s + w
                    } else
                        s
                }
            }
            dfs(n) = s
        }

        if (dt.isAugmented) {
            dt match {
                case pdt: PostDominatorTree =>
                    // let's filter the extra exit points; recall that we are
                    // non-termination _in_sensitive
                    dfs = dfs.map { e =>
                        if (e ne null)
                            e -- pdt.additionalExitNodes
                        else
                            e
                    }

                case _: DominatorTree =>
                //nothing special to do

                case dt =>
                    org.opalj.log.OPALLogger.warn(
                        "computing dominance frontier",
                        s"the augmentation of $dt is not understood and ignored"
                    )(org.opalj.log.GlobalLogContext)
            }
        }
        new DominanceFrontiers(dfs)
    }

}
