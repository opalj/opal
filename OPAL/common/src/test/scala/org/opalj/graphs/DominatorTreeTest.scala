/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package graphs

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import org.opalj.util.PerformanceEvaluation.time

/**
 * Test the [[DominatorTree]] implementation.
 *
 * @author Stephan Neumann
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class DominatorTreeTest extends AnyFlatSpec with Matchers {

    "a graph with just one node" should "result in a dominator tree with a single node" in {
        val g = Graph.empty[Int] addVertice 0
        val foreachSuccessorOf: Int => ((Int => Unit) => Unit) = (n: Int) => {
            f: (Int => Unit) => g.successors.getOrElse(n, Nil).foreach(e => f(e))
        }
        val foreachPredecessorOf: Int => ((Int => Unit) => Unit) = (n: Int) => {
            f: (Int => Unit) => g.predecessors.getOrElse(n, Nil).foreach(e => f(e))
        }

        val dt = time {
            DominatorTree(0, false, foreachSuccessorOf, foreachPredecessorOf, 0)
        } { t => info("dominators computed in "+t.toSeconds) }
        var ns: List[Int] = null

        ns = Nil
        dt.foreachDom(0, reflexive = true) { n => ns = n :: ns }
        ns should be(List(0))

        ns = Nil
        dt.foreachDom(0, reflexive = false) { n => ns = n :: ns }
        ns should be(Nil)

        //io.writeAndOpen(dt.toDot, "DominatorTree", ".dot")
    }

    "a graph with one custom node" should "result in a dominator tree with a single node" in {
        val g = Graph.empty[Int] addVertice 7
        val foreachSuccessorOf: Int => ((Int => Unit) => Unit) = (n: Int) => {
            f: (Int => Unit) => g.successors.getOrElse(n, Nil).foreach(e => f(e))
        }
        val foreachPredecessorOf: Int => ((Int => Unit) => Unit) = (n: Int) => {
            f: (Int => Unit) => g.predecessors.getOrElse(n, Nil).foreach(e => f(e))
        }

        val dt = time {
            DominatorTree(7, false, foreachSuccessorOf, foreachPredecessorOf, 7)
        } { t => info("dominators computed in "+t.toSeconds) }
        var ns: List[Int] = null

        ns = Nil
        dt.foreachDom(7, reflexive = true) { n => ns = n :: ns }
        ns should be(List(7))

        ns = Nil
        dt.foreachDom(7, reflexive = false) { n => ns = n :: ns }
        ns should be(Nil)

        //io.writeAndOpen(dt.toDot, "DominatorTree", ".dot")
    }

    "a graph with two connected nodes" should "yield one node dominating the other" in {
        val g = Graph.empty[Int] addEdge (0 -> 1)
        val foreachSuccessorOf: Int => ((Int => Unit) => Unit) = (n: Int) => {
            f: (Int => Unit) => g.successors.getOrElse(n, Nil).foreach(e => f(e))
        }
        val foreachPredecessorOf: Int => ((Int => Unit) => Unit) = (n: Int) => {
            f: (Int => Unit) => g.predecessors.getOrElse(n, Nil).foreach(e => f(e))
        }
        val dt = time {
            DominatorTree(0, false, foreachSuccessorOf, foreachPredecessorOf, 4)
        } { t => info("dominator tree computed in "+t.toSeconds) }

        dt.dom(1) should be(0)

        //io.writeAndOpen(dt.toDot, "DominatorTree", ".dot")
    }

    "a simple tree" should "result in a corresponding dominator tree" in {
        val g = Graph.empty[Int] addEdge (0 -> 1) addEdge (1 -> 2) addEdge (1 -> 3) addEdge (1 -> 4)
        val foreachSuccessorOf: Int => ((Int => Unit) => Unit) = (n: Int) => {
            f: (Int => Unit) => g.successors.getOrElse(n, Nil).foreach(e => f(e))
        }
        val foreachPredecessorOf: Int => ((Int => Unit) => Unit) = (n: Int) => {
            f: (Int => Unit) => g.predecessors.getOrElse(n, Nil).foreach(e => f(e))
        }
        val dt = time {
            DominatorTree(0, false, foreachSuccessorOf, foreachPredecessorOf, 4)
        } { t => info("dominator tree computed in "+t.toSeconds) }
        dt.dom(1) should be(0)
        dt.dom(2) should be(1)
        dt.dom(3) should be(1)
        dt.dom(4) should be(1)

        //io.writeAndOpen(dt.toDot, "DominatorTree", ".dot")
    }

    "a tree with a custom start node" should "result in a corresponding dominator tree" in {
        val g = Graph.empty[Int] addEdge (5 -> 0) addEdge (0 -> 1) addEdge (1 -> 2) addEdge (1 -> 3) addEdge (2 -> 4)
        val foreachSuccessorOf: Int => ((Int => Unit) => Unit) = (n: Int) => {
            f: (Int => Unit) => g.successors.getOrElse(n, Nil).foreach(e => f(e))
        }
        val foreachPredecessorOf: Int => ((Int => Unit) => Unit) = (n: Int) => {
            f: (Int => Unit) => g.predecessors.getOrElse(n, Nil).foreach(e => f(e))
        }
        val dt = time {
            DominatorTree(5, false, foreachSuccessorOf, foreachPredecessorOf, 5)
        } { t => info("dominator tree computed in "+t.toSeconds) }
        dt.dom(0) should be(5)
        dt.dom(1) should be(0)
        dt.dom(2) should be(1)
        dt.dom(3) should be(1)
        dt.dom(4) should be(2)

        var ns: List[Int] = Nil
        dt.foreachDom(4, reflexive = true) { n => ns = n :: ns }
        ns should be(List(5, 0, 1, 2, 4))

        //io.writeAndOpen(dt.toDot, "DominatorTree", ".dot")
    }

    "a graph with a cycle" should "correctly be resolved" in {
        val g = Graph.empty[Int] addEdge (0 -> 1) addEdge (1 -> 2) addEdge (1 -> 3) addEdge (0 -> 4) addEdge (2 -> 1)
        val foreachSuccessorOf: Int => ((Int => Unit) => Unit) = (n: Int) => {
            f: (Int => Unit) => g.successors.getOrElse(n, Nil).foreach(e => f(e))
        }
        val foreachPredecessorOf: Int => ((Int => Unit) => Unit) = (n: Int) => {
            f: (Int => Unit) => g.predecessors.getOrElse(n, Nil).foreach(e => f(e))
        }
        val dt = time {
            DominatorTree(0, false, foreachSuccessorOf, foreachPredecessorOf, 4)
        } { t => info("dominator tree computed in "+t.toSeconds) }

        dt.dom(1) should be(0)
        dt.dom(2) should be(1)
        dt.dom(3) should be(1)
        dt.dom(4) should be(0)

        //io.writeAndOpen(dt.toDot, "DominatorTree", ".dot")
    }

    "a graph with a cycle and custom start node" should "correctly be resolved" in {
        val g = Graph.empty[Int] addEdge (5 -> 1) addEdge (1 -> 2) addEdge (1 -> 3) addEdge (5 -> 4) addEdge (2 -> 1)
        val foreachSuccessorOf: Int => ((Int => Unit) => Unit) = (n: Int) => {
            f: (Int => Unit) => g.successors.getOrElse(n, Nil).foreach(e => f(e))
        }
        val foreachPredecessorOf: Int => ((Int => Unit) => Unit) = (n: Int) => {
            f: (Int => Unit) => g.predecessors.getOrElse(n, Nil).foreach(e => f(e))
        }
        val dt = time {
            DominatorTree(5, false, foreachSuccessorOf, foreachPredecessorOf, 5)
        } { t => info("dominator tree computed in "+t.toSeconds) }

        dt.dom(1) should be(5)
        dt.dom(2) should be(1)
        dt.dom(3) should be(1)
        dt.dom(4) should be(5)

        //io.writeAndOpen(dt.toDot, "DominatorTree", ".dot")
    }

    "a graph with a cycle related to the root node" should "correctly be resolved" in {
        val g = Graph.empty[Int] addEdge (0 -> 1) addEdge (1 -> 0)
        val foreachSuccessorOf: Int => ((Int => Unit) => Unit) = (n: Int) => {
            f: (Int => Unit) => g.successors.getOrElse(n, Nil).foreach(e => f(e))
        }
        val foreachPredecessorOf: Int => ((Int => Unit) => Unit) = (n: Int) => {
            f: (Int => Unit) => g.predecessors.getOrElse(n, Nil).foreach(e => f(e))
        }
        val dt = time {
            DominatorTree(0, true, foreachSuccessorOf, foreachPredecessorOf, 4)
        } { t => info("dominator tree computed in "+t.toSeconds) }

        dt.dom(1) should be(0)

        //io.writeAndOpen(dt.toDot, "DominatorTree", ".dot")
    }

    "a graph with a cycle related to a custom root node" should "correctly be resolved" in {
        val g = Graph.empty[Int] addEdge (2 -> 1) addEdge (1 -> 2)
        val foreachSuccessorOf: Int => ((Int => Unit) => Unit) = (n: Int) => {
            f: (Int => Unit) => g.successors.getOrElse(n, Nil).foreach(e => f(e))
        }
        val foreachPredecessorOf: Int => ((Int => Unit) => Unit) = (n: Int) => {
            f: (Int => Unit) => g.predecessors.getOrElse(n, Nil).foreach(e => f(e))
        }
        val dt = time {
            DominatorTree(2, true, foreachSuccessorOf, foreachPredecessorOf, 2)
        } { t => info("dominator tree computed in "+t.toSeconds) }

        dt.dom(1) should be(2)

        //io.writeAndOpen(dt.toDot, "DominatorTree", ".dot")
    }

    "a sparse cyclic graph" should "result in a compact dominator tree" in {
        val g = Graph.empty[Int] addEdge (0 -> 8) addEdge (8 -> 20) addEdge (8 -> 3) addEdge (0 -> 4) addEdge (20 -> 8)
        val foreachSuccessorOf: Int => ((Int => Unit) => Unit) = (n: Int) => {
            f: (Int => Unit) => g.successors.getOrElse(n, Nil).foreach(e => f(e))
        }
        val foreachPredecessorOf: Int => ((Int => Unit) => Unit) = (n: Int) => {
            f: (Int => Unit) => g.predecessors.getOrElse(n, Nil).foreach(e => f(e))
        }
        val dt = time {
            DominatorTree(0, false, foreachSuccessorOf, foreachPredecessorOf, 20)
        } { t => info("dominator tree computed in "+t.toSeconds) }

        dt.dom(3) should be(8)
        dt.dom(4) should be(0)
        dt.dom(8) should be(0)
        dt.dom(20) should be(8)

        var ns: List[Int] = Nil
        dt.foreachDom(20, reflexive = true) { n => ns = n :: ns }
        ns should be(List(0, 8, 20))

        //io.writeAndOpen(dt.toDot, "DominatorTree", ".dot")
    }

    "a graph with a long cycle" should "be handled gracefully" in {
        import scala.language.implicitConversions
        implicit def stringToInt(s: String): Int = s.charAt(0).toInt
        val g = Graph.empty[Int] addEdge (0, "b") addEdge ("b", "c") addEdge ("b", "d") addEdge (0, "e") addEdge ("d", "f") addEdge ("f", "b")
        val foreachSuccessorOf: Int => ((Int => Unit) => Unit) = (n: Int) => {
            f: (Int => Unit) => g.successors.getOrElse(n, Nil).foreach(e => f(e))
        }
        val foreachPredecessorOf: Int => ((Int => Unit) => Unit) = (n: Int) => {
            f: (Int => Unit) => g.predecessors.getOrElse(n, Nil).foreach(e => f(e))
        }
        val dt = time {
            DominatorTree(0, false, foreachSuccessorOf, foreachPredecessorOf, 128)
        } { t => info("dominator tree computed in "+t.toSeconds) }

        var ns: List[Int] = Nil
        dt.foreachDom("f", reflexive = true) { n => ns = n :: ns }
        ns should be(List[Int]("f", "d", "b", 0).reverse)

        //io.writeAndOpen(dt.toDot, "DominatorTree", ".dot")
    }

    "a very large, degenerated graph (path)" should "be computed in due time and should not raise an exception (e.g. StackOverflowError)" in {
        val g = Graph.empty[Int]
        val foreachSuccessorOf: Int => ((Int => Unit) => Unit) = (n: Int) => {
            f: (Int => Unit) => g.successors.getOrElse(n, Nil).foreach(e => f(e))
        }
        val foreachPredecessorOf: Int => ((Int => Unit) => Unit) = (n: Int) => {
            f: (Int => Unit) => g.predecessors.getOrElse(n, Nil).foreach(e => f(e))
        }
        var lastI = 0
        for (i <- 1 to 65000) {
            g addEdge (lastI -> i)
            lastI = i
        }
        val dt = time {
            DominatorTree(0, false, foreachSuccessorOf, foreachPredecessorOf, 65000)
        } { t => info("dominator tree computed in "+t.toSeconds) }
        var ns: List[Int] = null

        ns = Nil
        dt.foreachDom(0, reflexive = true) { n => ns = n :: ns }
        ns should be(List(0))

        ns = Nil
        dt.foreachDom(1, reflexive = true) { n => ns = n :: ns }
        ns should be(List(0, 1))

        ns = Nil
        dt.foreachDom(60000, reflexive = false) { n => ns = n :: ns }
        ns should be(Range(0, 60000, 1).toList)

        //io.writeAndOpen(dt.toDot, "DominatorTree", ".dot")
    }

}
