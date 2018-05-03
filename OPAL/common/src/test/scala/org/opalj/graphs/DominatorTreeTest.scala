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

import java.util.function.IntConsumer
import java.util.function.Consumer
import java.util.function.IntFunction

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.Matchers

import org.opalj.util.PerformanceEvaluation.time

/**
 * Test the [[DominatorTree]] implementation.
 *
 * @author Stephan Neumann
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class DominatorTreeTest extends FlatSpec with Matchers {

    "a graph with just one node" should "result in a dominator tree with a single node" in {
        val g = Graph.empty[Int] += 0
        val foreachSuccessorOf: IntFunction[Consumer[IntConsumer]] = (n: Int) ⇒ {
            f: IntConsumer ⇒ g.successors.getOrElse(n, Nil).foreach(e ⇒ f.accept(e))
        }
        val foreachPredecessorOf: IntFunction[Consumer[IntConsumer]] = (n: Int) ⇒ {
            f: IntConsumer ⇒ g.predecessors.getOrElse(n, Nil).foreach(e ⇒ f.accept(e))
        }

        val dt = time {
            DominatorTree(0, false, foreachSuccessorOf, foreachPredecessorOf, 0)
        } { t ⇒ info("dominators computed in "+t.toSeconds) }
        var ns: List[Int] = null

        ns = Nil
        dt.foreachDom(0, reflexive = true) { n ⇒ ns = n :: ns }
        ns should be(List(0))

        ns = Nil
        dt.foreachDom(0, reflexive = false) { n ⇒ ns = n :: ns }
        ns should be(Nil)

        //io.writeAndOpen(dt.toDot, "DominatorTree", ".dot")
    }

    "a graph with one custom node" should "result in a dominator tree with a single node" in {
        val g = Graph.empty[Int] += 7
        val foreachSuccessorOf: IntFunction[Consumer[IntConsumer]] = (n: Int) ⇒ {
            f: IntConsumer ⇒ g.successors.getOrElse(n, Nil).foreach(e ⇒ f.accept(e))
        }
        val foreachPredecessorOf: IntFunction[Consumer[IntConsumer]] = (n: Int) ⇒ {
            f: IntConsumer ⇒ g.predecessors.getOrElse(n, Nil).foreach(e ⇒ f.accept(e))
        }

        val dt = time {
            DominatorTree(7, false, foreachSuccessorOf, foreachPredecessorOf, 7)
        } { t ⇒ info("dominators computed in "+t.toSeconds) }
        var ns: List[Int] = null

        ns = Nil
        dt.foreachDom(7, reflexive = true) { n ⇒ ns = n :: ns }
        ns should be(List(7))

        ns = Nil
        dt.foreachDom(7, reflexive = false) { n ⇒ ns = n :: ns }
        ns should be(Nil)

        //io.writeAndOpen(dt.toDot, "DominatorTree", ".dot")
    }

    "a graph with two connected nodes" should "yield one node dominating the other" in {
        val g = Graph.empty[Int] += (0 → 1)
        val foreachSuccessorOf: IntFunction[Consumer[IntConsumer]] = (n: Int) ⇒ {
            f: IntConsumer ⇒ g.successors.getOrElse(n, Nil).foreach(e ⇒ f.accept(e))
        }
        val foreachPredecessorOf: IntFunction[Consumer[IntConsumer]] = (n: Int) ⇒ {
            f: IntConsumer ⇒ g.predecessors.getOrElse(n, Nil).foreach(e ⇒ f.accept(e))
        }
        val dt = time {
            DominatorTree(0, false, foreachSuccessorOf, foreachPredecessorOf, 4)
        } { t ⇒ info("dominator tree computed in "+t.toSeconds) }

        dt.dom(1) should be(0)

        //io.writeAndOpen(dt.toDot, "DominatorTree", ".dot")
    }

    "a simple tree" should "result in a corresponding dominator tree" in {
        val g = Graph.empty[Int] += (0 → 1) += (1 → 2) += (1 → 3) += (1 → 4)
        val foreachSuccessorOf: IntFunction[Consumer[IntConsumer]] = (n: Int) ⇒ {
            f: IntConsumer ⇒ g.successors.getOrElse(n, Nil).foreach(e ⇒ f.accept(e))
        }
        val foreachPredecessorOf: IntFunction[Consumer[IntConsumer]] = (n: Int) ⇒ {
            f: IntConsumer ⇒ g.predecessors.getOrElse(n, Nil).foreach(e ⇒ f.accept(e))
        }
        val dt = time {
            DominatorTree(0, false, foreachSuccessorOf, foreachPredecessorOf, 4)
        } { t ⇒ info("dominator tree computed in "+t.toSeconds) }
        dt.dom(1) should be(0)
        dt.dom(2) should be(1)
        dt.dom(3) should be(1)
        dt.dom(4) should be(1)

        //io.writeAndOpen(dt.toDot, "DominatorTree", ".dot")
    }

    "a tree with a custom start node" should "result in a corresponding dominator tree" in {
        val g = Graph.empty[Int] += (5 → 0) += (0 → 1) += (1 → 2) += (1 → 3) += (2 → 4)
        val foreachSuccessorOf: IntFunction[Consumer[IntConsumer]] = (n: Int) ⇒ {
            f: IntConsumer ⇒ g.successors.getOrElse(n, Nil).foreach(e ⇒ f.accept(e))
        }
        val foreachPredecessorOf: IntFunction[Consumer[IntConsumer]] = (n: Int) ⇒ {
            f: IntConsumer ⇒ g.predecessors.getOrElse(n, Nil).foreach(e ⇒ f.accept(e))
        }
        val dt = time {
            DominatorTree(5, false, foreachSuccessorOf, foreachPredecessorOf, 5)
        } { t ⇒ info("dominator tree computed in "+t.toSeconds) }
        dt.dom(0) should be(5)
        dt.dom(1) should be(0)
        dt.dom(2) should be(1)
        dt.dom(3) should be(1)
        dt.dom(4) should be(2)

        var ns: List[Int] = Nil
        dt.foreachDom(4, reflexive = true) { n ⇒ ns = n :: ns }
        ns should be(List(5, 0, 1, 2, 4))

        //io.writeAndOpen(dt.toDot, "DominatorTree", ".dot")
    }

    "a graph with a cycle" should "correctly be resolved" in {
        val g = Graph.empty[Int] += (0 → 1) += (1 → 2) += (1 → 3) += (0 → 4) += (2 → 1)
        val foreachSuccessorOf: IntFunction[Consumer[IntConsumer]] = (n: Int) ⇒ {
            f: IntConsumer ⇒ g.successors.getOrElse(n, Nil).foreach(e ⇒ f.accept(e))
        }
        val foreachPredecessorOf: IntFunction[Consumer[IntConsumer]] = (n: Int) ⇒ {
            f: IntConsumer ⇒ g.predecessors.getOrElse(n, Nil).foreach(e ⇒ f.accept(e))
        }
        val dt = time {
            DominatorTree(0, false, foreachSuccessorOf, foreachPredecessorOf, 4)
        } { t ⇒ info("dominator tree computed in "+t.toSeconds) }

        dt.dom(1) should be(0)
        dt.dom(2) should be(1)
        dt.dom(3) should be(1)
        dt.dom(4) should be(0)

        //io.writeAndOpen(dt.toDot, "DominatorTree", ".dot")
    }

    "a graph with a cycle and custom start node" should "correctly be resolved" in {
        val g = Graph.empty[Int] += (5 → 1) += (1 → 2) += (1 → 3) += (5 → 4) += (2 → 1)
        val foreachSuccessorOf: IntFunction[Consumer[IntConsumer]] = (n: Int) ⇒ {
            f: IntConsumer ⇒ g.successors.getOrElse(n, Nil).foreach(e ⇒ f.accept(e))
        }
        val foreachPredecessorOf: IntFunction[Consumer[IntConsumer]] = (n: Int) ⇒ {
            f: IntConsumer ⇒ g.predecessors.getOrElse(n, Nil).foreach(e ⇒ f.accept(e))
        }
        val dt = time {
            DominatorTree(5, false, foreachSuccessorOf, foreachPredecessorOf, 5)
        } { t ⇒ info("dominator tree computed in "+t.toSeconds) }

        dt.dom(1) should be(5)
        dt.dom(2) should be(1)
        dt.dom(3) should be(1)
        dt.dom(4) should be(5)

        //io.writeAndOpen(dt.toDot, "DominatorTree", ".dot")
    }

    "a graph with a cycle related to the root node" should "correctly be resolved" in {
        val g = Graph.empty[Int] += (0 → 1) += (1 → 0)
        val foreachSuccessorOf: IntFunction[Consumer[IntConsumer]] = (n: Int) ⇒ {
            f: IntConsumer ⇒ g.successors.getOrElse(n, Nil).foreach(e ⇒ f.accept(e))
        }
        val foreachPredecessorOf: IntFunction[Consumer[IntConsumer]] = (n: Int) ⇒ {
            f: IntConsumer ⇒ g.predecessors.getOrElse(n, Nil).foreach(e ⇒ f.accept(e))
        }
        val dt = time {
            DominatorTree(0, true, foreachSuccessorOf, foreachPredecessorOf, 4)
        } { t ⇒ info("dominator tree computed in "+t.toSeconds) }

        dt.dom(1) should be(0)

        //io.writeAndOpen(dt.toDot, "DominatorTree", ".dot")
    }

    "a graph with a cycle related to a custom root node" should "correctly be resolved" in {
        val g = Graph.empty[Int] += (2 → 1) += (1 → 2)
        val foreachSuccessorOf: IntFunction[Consumer[IntConsumer]] = (n: Int) ⇒ {
            f: IntConsumer ⇒ g.successors.getOrElse(n, Nil).foreach(e ⇒ f.accept(e))
        }
        val foreachPredecessorOf: IntFunction[Consumer[IntConsumer]] = (n: Int) ⇒ {
            f: IntConsumer ⇒ g.predecessors.getOrElse(n, Nil).foreach(e ⇒ f.accept(e))
        }
        val dt = time {
            DominatorTree(2, true, foreachSuccessorOf, foreachPredecessorOf, 2)
        } { t ⇒ info("dominator tree computed in "+t.toSeconds) }

        dt.dom(1) should be(2)

        //io.writeAndOpen(dt.toDot, "DominatorTree", ".dot")
    }

    "a sparse cyclic graph" should "result in a compact dominator tree" in {
        val g = Graph.empty[Int] += (0 → 8) += (8 → 20) += (8 → 3) += (0 → 4) += (20 → 8)
        val foreachSuccessorOf: IntFunction[Consumer[IntConsumer]] = (n: Int) ⇒ {
            f: IntConsumer ⇒ g.successors.getOrElse(n, Nil).foreach(e ⇒ f.accept(e))
        }
        val foreachPredecessorOf: IntFunction[Consumer[IntConsumer]] = (n: Int) ⇒ {
            f: IntConsumer ⇒ g.predecessors.getOrElse(n, Nil).foreach(e ⇒ f.accept(e))
        }
        val dt = time {
            DominatorTree(0, false, foreachSuccessorOf, foreachPredecessorOf, 20)
        } { t ⇒ info("dominator tree computed in "+t.toSeconds) }

        dt.dom(3) should be(8)
        dt.dom(4) should be(0)
        dt.dom(8) should be(0)
        dt.dom(20) should be(8)

        var ns: List[Int] = Nil
        dt.foreachDom(20, reflexive = true) { n ⇒ ns = n :: ns }
        ns should be(List(0, 8, 20))

        //io.writeAndOpen(dt.toDot, "DominatorTree", ".dot")
    }

    "a graph with a long cycle" should "be handled gracefully" in {
        import scala.language.implicitConversions
        implicit def stringToInt(s: String): Int = s.charAt(0).toInt
        val g = Graph.empty[Int] += (0, "b") += ("b", "c") += ("b", "d") += (0, "e") += ("d", "f") += ("f", "b")
        val foreachSuccessorOf: IntFunction[Consumer[IntConsumer]] = (n: Int) ⇒ {
            f: IntConsumer ⇒ g.successors.getOrElse(n, Nil).foreach(e ⇒ f.accept(e))
        }
        val foreachPredecessorOf: IntFunction[Consumer[IntConsumer]] = (n: Int) ⇒ {
            f: IntConsumer ⇒ g.predecessors.getOrElse(n, Nil).foreach(e ⇒ f.accept(e))
        }
        val dt = time {
            DominatorTree(0, false, foreachSuccessorOf, foreachPredecessorOf, 128)
        } { t ⇒ info("dominator tree computed in "+t.toSeconds) }

        var ns: List[Int] = Nil
        dt.foreachDom("f", reflexive = true) { n ⇒ ns = n :: ns }
        ns should be(List[Int]("f", "d", "b", 0).reverse)

        //io.writeAndOpen(dt.toDot, "DominatorTree", ".dot")
    }

    "a very large, degenerated graph (path)" should "be computed in due time and should not raise an exception (e.g. StackOverflowError)" in {
        val g = Graph.empty[Int]
        val foreachSuccessorOf: IntFunction[Consumer[IntConsumer]] = (n: Int) ⇒ {
            f: IntConsumer ⇒ g.successors.getOrElse(n, Nil).foreach(e ⇒ f.accept(e))
        }
        val foreachPredecessorOf: IntFunction[Consumer[IntConsumer]] = (n: Int) ⇒ {
            f: IntConsumer ⇒ g.predecessors.getOrElse(n, Nil).foreach(e ⇒ f.accept(e))
        }
        var lastI = 0
        for (i ← 1 to 65000) {
            g += (lastI → i)
            lastI = i
        }
        val dt = time {
            DominatorTree(0, false, foreachSuccessorOf, foreachPredecessorOf, 65000)
        } { t ⇒ info("dominator tree computed in "+t.toSeconds) }
        var ns: List[Int] = null

        ns = Nil
        dt.foreachDom(0, reflexive = true) { n ⇒ ns = n :: ns }
        ns should be(List(0))

        ns = Nil
        dt.foreachDom(1, reflexive = true) { n ⇒ ns = n :: ns }
        ns should be(List(0, 1))

        ns = Nil
        dt.foreachDom(60000, reflexive = false) { n ⇒ ns = n :: ns }
        ns should be(Range(0, 60000, 1).toList)

        //io.writeAndOpen(dt.toDot, "DominatorTree", ".dot")
    }

}
