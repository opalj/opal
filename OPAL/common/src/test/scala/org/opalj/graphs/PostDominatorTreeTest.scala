/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package graphs

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import org.opalj.util.PerformanceEvaluation.time
import org.opalj.collection.immutable.IntTrieSet

/**
 * Tests the (Post)[[DominatorTree]] implementation.
 *
 * @author Stephan Neumann
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class PostDominatorTreeTest extends AnyFlatSpec with Matchers {

    "a graph with just one node" should "result in a post dominator tree with a single node" in {
        val g = Graph.empty[Int] addVertice 0
        val foreachSuccessorOf: Int => ((Int => Unit) => Unit) = (n: Int) => {
            f: (Int => Unit) => g.successors.getOrElse(n, Nil).foreach(e => f(e))
        }
        val foreachPredecessorOf: Int => ((Int => Unit) => Unit) = (n: Int) => {
            f: (Int => Unit) => g.predecessors.getOrElse(n, Nil).foreach(e => f(e))
        }
        val existNodes = Set(0)
        val dt = time {
            PostDominatorTree(
                Some(0),
                (i: Int) => i == 0,
                IntTrieSet.empty,
                (f: (Int => Unit)) => existNodes.foreach(e => f(e)),
                foreachSuccessorOf, foreachPredecessorOf,
                0
            )
        } { t => info("post dominator tree computed in "+t.toSeconds) }
        var ns: List[Int] = null

        ns = Nil
        dt.foreachDom(0, reflexive = true) { n => ns = n :: ns }
        ns should be(List(0))

        ns = Nil
        dt.foreachDom(0, reflexive = false) { n => ns = n :: ns }
        ns should be(List())

        //io.writeAndOpen(dt.toDot, "PostDominatorTree", ".dot")
    }

    "a simple tree with multiple exits" should "result in a corresponding postdominator tree" in {
        val g = Graph.empty[Int] addEdge (0 -> 1) addEdge (1 -> 2) addEdge (1 -> 3) addEdge (2 -> 4)
        val foreachSuccessorOf: Int => ((Int => Unit) => Unit) = (n: Int) => {
            f: (Int => Unit) => g.successors.getOrElse(n, Nil).foreach(e => f(e))
        }
        val foreachPredecessorOf: Int => ((Int => Unit) => Unit) = (n: Int) => {
            f: (Int => Unit) => g.predecessors.getOrElse(n, Nil).foreach(e => f(e))
        }
        val existNodes = Set(3, 4)
        val dt = time {
            PostDominatorTree(
                None,
                existNodes.contains,
                IntTrieSet.empty,
                (f: (Int => Unit)) => existNodes.foreach(e => f(e)),
                foreachSuccessorOf, foreachPredecessorOf,
                4
            )
        } { t => info("post dominator tree computed in "+t.toSeconds) }
        dt.dom(0) should be(1)
        dt.dom(1) should be(5)
        dt.dom(2) should be(4)
        dt.dom(3) should be(5)
        dt.dom(4) should be(5)

        var ns: List[Int] = null

        ns = Nil
        dt.foreachDom(0, reflexive = true) { n => ns = n :: ns }
        ns should be(List(5, 1, 0))

        ns = Nil
        dt.foreachDom(2, reflexive = false) { n => ns = n :: ns }
        ns should be(List(5, 4))

        //io.writeAndOpen(dt.toDot, "PostDominatorTree", ".dot")
    }

    "a graph with a cycle" should "yield the correct postdominators" in {
        val g = Graph.empty[Int] addEdge (0 -> 1) addEdge (1 -> 2) addEdge (1 -> 3) addEdge (0 -> 4) addEdge (2 -> 1)
        val foreachSuccessorOf: Int => ((Int => Unit) => Unit) = (n: Int) => {
            f: (Int => Unit) => g.successors.getOrElse(n, Nil).foreach(e => f(e))
        }
        val foreachPredecessorOf: Int => ((Int => Unit) => Unit) = (n: Int) => {
            f: (Int => Unit) => g.predecessors.getOrElse(n, Nil).foreach(e => f(e))
        }
        val existNodes = Set(3, 4)
        val dt = time {
            PostDominatorTree(
                None,
                existNodes.contains,
                IntTrieSet.empty,
                (f: (Int => Unit)) => existNodes.foreach(e => f(e)),
                foreachSuccessorOf, foreachPredecessorOf,
                4
            )
        } { t => info("post dominator tree computed in "+t.toSeconds) }

        try {

            dt.dom(1) should be(3)
            dt.dom(2) should be(1)
            dt.dom(3) should be(5)
            dt.dom(4) should be(5)
            dt.dom(0) should be(5)

            var ns: List[Int] = null

            ns = Nil
            dt.foreachDom(0, reflexive = true) { n => ns = n :: ns }
            ns should be(List(5, 0))

            ns = Nil
            dt.foreachDom(1, reflexive = false) { n => ns = n :: ns }
            ns should be(List(5, 3))

        } catch {
            case t: Throwable =>
                io.writeAndOpen(g.toDot(), "CFG", ".dot")
                io.writeAndOpen(dt.toDot(), "PostDominatorTree", ".dot")
                throw t;
        }
    }

    "a path with multiple artificial exit points" should "yield the correct postdominators" in {
        val g = Graph.empty[Int] addEdge (0 -> 1) addEdge (1 -> 2)
        val foreachSuccessorOf: Int => ((Int => Unit) => Unit) = (n: Int) => {
            f: (Int => Unit) => g.successors.getOrElse(n, Nil).foreach(e => f(e))
        }
        val foreachPredecessorOf: Int => ((Int => Unit) => Unit) = (n: Int) => {
            f: (Int => Unit) => g.predecessors.getOrElse(n, Nil).foreach(e => f(e))
        }
        val existNodes = Set(1, 2)
        val dt = time {
            PostDominatorTree(
                None,
                existNodes.contains,
                IntTrieSet.empty,
                (f: Int => Unit) => existNodes.foreach(e => f(e)),
                foreachSuccessorOf, foreachPredecessorOf,
                2
            )
        } { t => info("post dominator tree computed in "+t.toSeconds) }

        try {

            dt.dom(0) should be(1)
            dt.dom(1) should be(3)
            dt.dom(2) should be(3)

        } catch {
            case t: Throwable =>
                io.writeAndOpen(g.toDot(), "CFG", ".dot")
                io.writeAndOpen(dt.toDot(), "PostDominatorTree", ".dot")
                throw t;
        }
    }

}
