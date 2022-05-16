/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package graphs

import org.junit.runner.RunWith
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.junit.JUnitRunner

import org.opalj.io.writeAndOpen
import org.opalj.collection.immutable.EmptyIntArraySet
import org.opalj.collection.immutable.IntArraySet
import org.opalj.collection.immutable.IntArraySetBuilder

/**
 * Tests the [[DominanceFrontiers]] implementation.
 *
 * Dominance frontiers are defined as follows:
 *
 * The dominance frontier of node w:
 *     Node u is in the dominance frontier of node w
 *     if w dominates a CFG predecessor v of u,
 *     (hence, v can be w)
 *     but does not strictly dominate u.
 *
 * @author Michael Reif
 */
@RunWith(classOf[JUnitRunner])
class DominanceFrontiersTest extends AnyFlatSpec with Matchers {

    private def setUpDominanceFrontiers(
        startNode:                Int,
        g:                        Graph[Int],
        maxNode:                  Int,
        startNodeHasPredecessors: Boolean    = false
    ): DominanceFrontiers = {
        setUpDominanceFrontiers(
            startNode,
            g,
            maxNode,
            (n: Int) => n >= startNode && n <= maxNode,
            startNodeHasPredecessors
        )
    }

    private def setUpDominanceFrontiers(
        startNode:                Int,
        g:                        Graph[Int],
        maxNode:                  Int,
        isValidNode:              Int => Boolean,
        startNodeHasPredecessors: Boolean
    ): DominanceFrontiers = {
        val dominatorTree =
            DominatorTree(
                startNode, startNodeHasPredecessors,
                (n: Int) => { f: (Int => Unit) =>
                    g.successors.getOrElse(n, List.empty).foreach[Unit](e => f(e))
                },
                (n: Int) => { f: (Int => Unit) =>
                    g.predecessors.getOrElse(n, List.empty).foreach[Unit](e => f(e))
                },
                maxNode
            )
        try {
            DominanceFrontiers(dominatorTree, isValidNode)
        } catch {
            case t: Throwable =>
                writeAndOpen(dominatorTree.toDot(), "FailedComputingDominaceFrontierFor", ".dt.gv")
                throw t
        }
    }

    "a graph with a single node" should "result in no dominance frontiers" in {
        val graph = Graph.empty[Int] addVertice 0
        val df = setUpDominanceFrontiers(0, graph, 0)

        df.df(0) should be(EmptyIntArraySet)
    }

    "a graph a single cyclic node" should "result in a reflexive dominance frontier" in {
        val graph = Graph.empty[Int] addEdge (0 -> 0)
        val df = setUpDominanceFrontiers(0, graph, 0, startNodeHasPredecessors = true)

        //        org.opalj.io.writeAndOpen(df.toDot(), "graph", ".dt.gv")
        //        org.opalj.io.writeAndOpen(df.toDot(), "graph", ".df.gv")

        df.df(0) should be(IntArraySet(0))
    }

    "a graph with a single graph" should "result in no dominance frontiers" in {
        val graph = Graph.empty[Int] addEdge (0 -> 1) addEdge (1 -> 2) addEdge (2 -> 3) addEdge (3 -> 4)

        val df = setUpDominanceFrontiers(0, graph, 4)

        //        org.opalj.io.writeAndOpen(dt.toDot(), "graph", ".dt.gv")
        //        org.opalj.io.writeAndOpen(df.toDot(), "graph", ".df.gv")

        df.df(0) should be(EmptyIntArraySet)
        df.df(1) should be(EmptyIntArraySet)
        df.df(2) should be(EmptyIntArraySet)
        df.df(3) should be(EmptyIntArraySet)
        df.df(4) should be(EmptyIntArraySet)
    }

    "a diamond-shaped graph (e.g., from an if statement)" should "be handled properly" in {
        val graph = Graph.empty[Int] addEdge (0 -> 1) addEdge (1 -> 2) addEdge (1 -> 3) addEdge (2 -> 4) addEdge (3 -> 4)

        val df = setUpDominanceFrontiers(0, graph, 4)

        //        org.opalj.io.writeAndOpen(dt.toDot(), "graph", ".dt.gv")
        //        org.opalj.io.writeAndOpen(df.toDot(), "graph", ".df.gv")

        df.df(1) should be(EmptyIntArraySet)
        df.df(2) should be(IntArraySet(4))
        df.df(3) should be(IntArraySet(4))
        df.df(4) should be(EmptyIntArraySet)
    }

    "a graph modeling a simple if" should
        "result in a dominance frontier grpah which correctly represents the if-block" in {
            val graph = Graph.empty[Int] addEdge (0 -> 1) addEdge (1 -> 2) addEdge (2 -> 3) addEdge (1 -> 3)

            val df = setUpDominanceFrontiers(0, graph, 3)

            df.df(1) should be(EmptyIntArraySet)
            df.df(2) should be(IntArraySetBuilder(3).result())
            df.df(3) should be(EmptyIntArraySet)
        }

    "a graph which models nested if-statements" should "be handled properly" in {
        val graph =
            Graph.empty[Int] addEdge
                (0 -> 1) addEdge (1 -> 2) addEdge (1 -> 6) addEdge (2 -> 3) addEdge (2 -> 4) addEdge (3 -> 5) addEdge
                (4 -> 5) addEdge (5 -> 7) addEdge (6 -> 7)

        val df = setUpDominanceFrontiers(0, graph, 7)

        df.df(0) should be(EmptyIntArraySet)
        df.df(1) should be(EmptyIntArraySet)
        df.df(2) should be(IntArraySet(7))
        df.df(3) should be(IntArraySet(5))
        df.df(4) should be(IntArraySet(5))
        df.df(5) should be(IntArraySet(7))
        df.df(6) should be(IntArraySet(7))
        df.df(7) should be(EmptyIntArraySet)
    }

    "a non-trivial, cyclic graph" should "be handled properly" in {
        val graph = Graph.empty[Int] addEdge (0 -> 1) addEdge (1 -> 2) addEdge (2 -> 0)

        val df = setUpDominanceFrontiers(0, graph, 2, startNodeHasPredecessors = true)

        df.df(2) should be(IntArraySetBuilder(0).result())
    }

    // Referenced paper:
    // Efficiently Computing Static Single Assignment Form and the Control Dependence Graph
    "analyzing the graph from Efficiently Computing SSA-Form and the CDG" should
        "result in the respective dominance frontiers" in {

            val graph =
                org.opalj.graphs.Graph.empty[Int] addEdge
                    (0 -> 1) addEdge (1 -> 2) addEdge (2 -> 3) addEdge (2 -> 7) addEdge (3 -> 4) addEdge (3 -> 5) addEdge (5 -> 6) addEdge
                    (4 -> 6) addEdge (6 -> 8) addEdge (7 -> 8) addEdge (8 -> 9) addEdge (9 -> 10) addEdge (9 -> 11) addEdge (10 -> 11) addEdge
                    (11 -> 9) addEdge (11 -> 12) addEdge (12 -> 13) addEdge (12 -> 2) addEdge (0 -> 13)

            val df = setUpDominanceFrontiers(0, graph, 13)

            //        org.opalj.io.writeAndOpen(dt.toDot(), "graph", ".dt.gv")
            //        org.opalj.io.writeAndOpen(df.toDot(), "graph", ".df.gv")

            df.df(0) should be(EmptyIntArraySet)
            df.df(1) should be(IntArraySetBuilder(13).result())
            df.df(2) should be(IntArraySetBuilder(2, 13).result())
            df.df(3) should be(IntArraySetBuilder(8).result())
            df.df(4) should be(IntArraySetBuilder(6).result())
            df.df(5) should be(IntArraySetBuilder(6).result())
            df.df(6) should be(IntArraySetBuilder(8).result())
            df.df(7) should be(IntArraySetBuilder(8).result())
            df.df(8) should be(IntArraySetBuilder(2, 13).result())
            df.df(9) should be(IntArraySetBuilder(2, 9, 13).result())
            df.df(10) should be(IntArraySetBuilder(11).result())
            df.df(11) should be(IntArraySetBuilder(2, 9, 13).result())
            df.df(12) should be(IntArraySetBuilder(2, 13).result())
            df.df(13) should be(EmptyIntArraySet)

        }

    "a graph with randomly named nodes" should "result in the correct dominance frontiers" in {

        val graph =
            org.opalj.graphs.Graph.empty[Int] addEdge
                (0 -> 1) addEdge (1 -> 2) addEdge (2 -> 77) addEdge (2 -> 7) addEdge (77 -> 4) addEdge (77 -> 55) addEdge (55 -> 6) addEdge
                (4 -> 6) addEdge (6 -> 8) addEdge (7 -> 8) addEdge (8 -> 9) addEdge (9 -> 10) addEdge (9 -> 11) addEdge (10 -> 11) addEdge
                (11 -> 9) addEdge (11 -> 12) addEdge (12 -> 22) addEdge (12 -> 2) addEdge (0 -> 22)

        val isValidNode = (n: Int) => Set(0, 1, 2, 77, 4, 55, 6, 7, 8, 9, 10, 11, 12, 22).contains(n)

        val df = setUpDominanceFrontiers(0, graph, 77, isValidNode, false)

        df.df(0) should be(EmptyIntArraySet)
        df.df(1) should be(IntArraySetBuilder(22).result())
        df.df(2) should be(IntArraySetBuilder(2, 22).result())
        df.df(77) should be(IntArraySetBuilder(8).result())
        df.df(4) should be(IntArraySetBuilder(6).result())
        df.df(55) should be(IntArraySetBuilder(6).result())
        df.df(6) should be(IntArraySetBuilder(8).result())
        df.df(7) should be(IntArraySetBuilder(8).result())
        df.df(8) should be(IntArraySetBuilder(2, 22).result())
        df.df(9) should be(IntArraySetBuilder(2, 9, 22).result())
        df.df(10) should be(IntArraySetBuilder(11).result())
        df.df(11) should be(IntArraySetBuilder(2, 9, 22).result())
        df.df(12) should be(IntArraySetBuilder(2, 22).result())
        df.df(22) should be(EmptyIntArraySet)
    }

}
