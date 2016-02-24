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

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.Matchers
import org.scalatest.ParallelTestExecution

import org.opalj.util.PerformanceEvaluation.time

/**
 * Tests the dominator algorithm.
 *
 * @author Stephan Neumann
 */
@RunWith(classOf[JUnitRunner])
class DominatorTest extends FlatSpec with Matchers {

    "a graph with just one node" should "yield the node dominating itself" in {
        val g = Graph.empty[AnyRef] += "a"
        time {
            dominators(g) should be(Map("a" → List("a")))
        } { t ⇒ info("dominators computed in "+t.toSeconds) }
    }

    "a graph with two connected nodes" should "yield one node dominating the other" in {
        val g = Graph.empty[AnyRef] += ("a" → "b")
        time {
            dominators(g) should be(Map("a" → List("a"), "b" → List("b", "a")))
        } { t ⇒ info("dominators computed in "+t.toSeconds) }
    }

    "a simple graph" should "yield the correct dominators" in {
        val g = Graph.empty[AnyRef] += ("a" → "b") += ("b" → "c") += ("b" → "d") += ("a" → "e")
        time {
            dominators(g) should be(Map("a" → List("a"), "e" → List("e", "a"), "b" → List("b", "a"), "d" → List("d", "b", "a"), "c" → List("c", "b", "a")))
        } { t ⇒ info("dominators computed in "+t.toSeconds) }
    }

    "a cyclic graph" should "not crash the algorithm" in {
        val g = Graph.empty[AnyRef] += ("a" → "b") += ("b" → "c") += ("b" → "d") += ("a" → "e") += ("c" → "b")
        time {
            dominators(g) should be(Map("a" → List("a"), "e" → List("e", "a"), "b" → List("b", "a"), "d" → List("d", "b", "a"), "c" → List("c", "b", "a")))
        } { t ⇒ info("dominators computed in "+t.toSeconds) }
    }

    "a graph with a big cycle" should "not crash the algorithm" in {
        val g = Graph.empty[AnyRef] += ("a" → "b") += ("b" → "c") += ("b" → "d") += ("a" → "e") += ("d" → "f") += ("f" → "b")
        time {
            dominators(g) should be(Map("a" → List("a"), "e" → List("e", "a"), "b" → List("b", "a"), "d" → List("d", "b", "a"), "c" → List("c", "b", "a"), "f" → List("f", "d", "b", "a")))
        } { t ⇒ info("dominators computed in "+t.toSeconds) }
    }

    "a graph with multiple paths" should "yield only the real dominators" in {
        val g = Graph.empty[AnyRef] += ("a" → "b") += ("b" → "c") += ("b" → "d") += ("a" → "e") += ("e" → "d")
        time {
            dominators(g) should be(Map("a" → List("a"), "e" → List("e", "a"), "b" → List("b", "a"), "d" → List("d", "a"), "c" → List("c", "b", "a")))
        } { t ⇒ info("dominators computed in "+t.toSeconds) }
    }

    "a very large, degenerated graph" should "be possible to process without a stackoverflow error" in {
        val g = Graph.empty[String]
        var lastI = 0
        for (i ← 1 to 65000) {
            g += (lastI.toString → i.toString)
            lastI = i
        }
        val immediateDoms = time {
            immediateDominators("0", g)
        } { t ⇒ info("immediate dominators computed in "+t.toSeconds) }
        val doms = time {
            dominators("0", immediateDoms)
        } { t ⇒ info("all dominators collected in "+t.toSeconds) }

        doms(0.toString) should be(List("0"))
        doms(1.toString).size should be(2)
        doms(59999.toString).size should be(60000)
    }

    //
    // TESTING THE SECOND IMPLEMENTATION WHICH OPERATES ON INTS...
    //

    "an int graph with just one node" should "yield the node dominating itself" in {
        val g = Graph.empty[Int] += 0
        time {
            dominators(0, g.vertices, immediateDominators(g, 0)) should be(Map(0 → List(0)))
        } { t ⇒ info("dominators computed in "+t.toSeconds) }
    }

    "an int graph with two connected nodes" should "yield one node dominating the other" in {
        val g = Graph.empty[Int] += (0 → 1)
        time {
            dominators(0, g.vertices, immediateDominators(g, 1)) should be(Map(0 → List(0), 1 → List(1, 0)))
        } { t ⇒ info("dominators computed in "+t.toSeconds) }
    }

    "a simple int graph" should "yield the correct dominators" in {
        val g = Graph.empty[Int] += (0 → 1) += (1 → 2) += (1 → 3) += (1 → 4)
        time {
            dominators(0, g.vertices, immediateDominators(g, 4)) should be(Map(0 → List(0), 1 → List(1, 0), 2 → List(2, 1, 0), 3 → List(3, 1, 0), 4 → List(4, 1, 0)))
        } { t ⇒ info("dominators computed in "+t.toSeconds) }
    }

    "a cyclic int graph" should "not crash the algorithm" in {
        val g = Graph.empty[Int] += (0 → 1) += (1 → 2) += (1 → 3) += (0 → 4) += (2 → 1)
        time {
            dominators(0, g.vertices, immediateDominators(g, 4)) should be(Map(0 → List(0), 4 → List(4, 0), 1 → List(1, 0), 3 → List(3, 1, 0), 2 → List(2, 1, 0)))
        } { t ⇒ info("dominators computed in "+t.toSeconds) }
    }

    "a very large, degenerated int graph" should "be possible to process without a stackoverflow error" in {
        val g = Graph.empty[Int]
        var lastI = 0
        for (i ← 1 to 65000) {
            g += (lastI → i)
            lastI = i
        }
        val immediateDoms = time {
            immediateDominators(g, 65000)
        } { t ⇒ info("immediate dominators computed in "+t.toSeconds) }.zipWithIndex.map(_.swap).toMap

        val doms = time {
            dominators(0, immediateDoms)
        } { t ⇒ info("all dominators collected in "+t.toSeconds) }

        doms(0) should be(List(0))
        doms(1).size should be(2)
        doms(59999).size should be(60000)
    }

}

