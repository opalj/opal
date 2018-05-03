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
import org.opalj.collection.immutable.IntTrieSet

/**
 * Tests the (Post)[[DominatorTree]] implementation.
 *
 * @author Stephan Neumann
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class PostDominatorTreeTest extends FlatSpec with Matchers {

    "a graph with just one node" should "result in a post dominator tree with a single node" in {
        val g = Graph.empty[Int] += 0
        val foreachSuccessorOf: IntFunction[Consumer[IntConsumer]] = (n: Int) ⇒ {
            f: IntConsumer ⇒ g.successors.getOrElse(n, Nil).foreach(e ⇒ f.accept(e))
        }
        val foreachPredecessorOf: IntFunction[Consumer[IntConsumer]] = (n: Int) ⇒ {
            f: IntConsumer ⇒ g.predecessors.getOrElse(n, Nil).foreach(e ⇒ f.accept(e))
        }
        val existNodes = Set(0)
        val dt = time {
            PostDominatorTree(
                Some(0),
                (i: Int) ⇒ i == 0,
                IntTrieSet.empty,
                (f: IntConsumer) ⇒ existNodes.foreach(e ⇒ f.accept(e)),
                foreachSuccessorOf, foreachPredecessorOf,
                0
            )
        } { t ⇒ info("post dominator tree computed in "+t.toSeconds) }
        var ns: List[Int] = null

        ns = Nil
        dt.foreachDom(0, reflexive = true) { n ⇒ ns = n :: ns }
        ns should be(List(0))

        ns = Nil
        dt.foreachDom(0, reflexive = false) { n ⇒ ns = n :: ns }
        ns should be(List())

        //io.writeAndOpen(dt.toDot, "PostDominatorTree", ".dot")
    }

    "a simple tree with multiple exits" should "result in a corresponding postdominator tree" in {
        val g = Graph.empty[Int] += (0 → 1) += (1 → 2) += (1 → 3) += (2 → 4)
        val foreachSuccessorOf: IntFunction[Consumer[IntConsumer]] = (n: Int) ⇒ {
            f: IntConsumer ⇒ g.successors.getOrElse(n, Nil).foreach(e ⇒ f.accept(e))
        }
        val foreachPredecessorOf: IntFunction[Consumer[IntConsumer]] = (n: Int) ⇒ {
            f: IntConsumer ⇒ g.predecessors.getOrElse(n, Nil).foreach(e ⇒ f.accept(e))
        }
        val existNodes = Set(3, 4)
        val dt = time {
            PostDominatorTree(
                None,
                existNodes.contains,
                IntTrieSet.empty,
                (f: IntConsumer) ⇒ existNodes.foreach(e ⇒ f.accept(e)),
                foreachSuccessorOf, foreachPredecessorOf,
                4
            )
        } { t ⇒ info("post dominator tree computed in "+t.toSeconds) }
        dt.dom(0) should be(1)
        dt.dom(1) should be(5)
        dt.dom(2) should be(4)
        dt.dom(3) should be(5)
        dt.dom(4) should be(5)

        var ns: List[Int] = null

        ns = Nil
        dt.foreachDom(0, reflexive = true) { n ⇒ ns = n :: ns }
        ns should be(List(5, 1, 0))

        ns = Nil
        dt.foreachDom(2, reflexive = false) { n ⇒ ns = n :: ns }
        ns should be(List(5, 4))

        //io.writeAndOpen(dt.toDot, "PostDominatorTree", ".dot")
    }

    "a graph with a cycle" should "yield the correct postdominators" in {
        val g = Graph.empty[Int] += (0 → 1) += (1 → 2) += (1 → 3) += (0 → 4) += (2 → 1)
        val foreachSuccessorOf: IntFunction[Consumer[IntConsumer]] = (n: Int) ⇒ {
            f: IntConsumer ⇒ g.successors.getOrElse(n, Nil).foreach(e ⇒ f.accept(e))
        }
        val foreachPredecessorOf: IntFunction[Consumer[IntConsumer]] = (n: Int) ⇒ {
            f: IntConsumer ⇒ g.predecessors.getOrElse(n, Nil).foreach(e ⇒ f.accept(e))
        }
        val existNodes = Set(3, 4)
        val dt = time {
            PostDominatorTree(
                None,
                existNodes.contains,
                IntTrieSet.empty,
                (f: IntConsumer) ⇒ existNodes.foreach(e ⇒ f.accept(e)),
                foreachSuccessorOf, foreachPredecessorOf,
                4
            )
        } { t ⇒ info("post dominator tree computed in "+t.toSeconds) }

        try {

            dt.dom(1) should be(3)
            dt.dom(2) should be(1)
            dt.dom(3) should be(5)
            dt.dom(4) should be(5)
            dt.dom(0) should be(5)

            var ns: List[Int] = null

            ns = Nil
            dt.foreachDom(0, reflexive = true) { n ⇒ ns = n :: ns }
            ns should be(List(5, 0))

            ns = Nil
            dt.foreachDom(1, reflexive = false) { n ⇒ ns = n :: ns }
            ns should be(List(5, 3))

        } catch {
            case t: Throwable ⇒
                io.writeAndOpen(g.toDot(), "CFG", ".dot")
                io.writeAndOpen(dt.toDot(), "PostDominatorTree", ".dot")
                throw t;
        }
    }

    "a path with multiple artificial exit points" should "yield the correct postdominators" in {
        val g = Graph.empty[Int] += (0 → 1) += (1 → 2)
        val foreachSuccessorOf: IntFunction[Consumer[IntConsumer]] = (n: Int) ⇒ {
            f: IntConsumer ⇒ g.successors.getOrElse(n, Nil).foreach(e ⇒ f.accept(e))
        }
        val foreachPredecessorOf: IntFunction[Consumer[IntConsumer]] = (n: Int) ⇒ {
            f: IntConsumer ⇒ g.predecessors.getOrElse(n, Nil).foreach(e ⇒ f.accept(e))
        }
        val existNodes = Set(1, 2)
        val dt = time {
            PostDominatorTree(
                None,
                existNodes.contains,
                IntTrieSet.empty,
                (f: IntConsumer) ⇒ existNodes.foreach(e ⇒ f.accept(e)),
                foreachSuccessorOf, foreachPredecessorOf,
                2
            )
        } { t ⇒ info("post dominator tree computed in "+t.toSeconds) }

        try {

            dt.dom(0) should be(1)
            dt.dom(1) should be(3)
            dt.dom(2) should be(3)

        } catch {
            case t: Throwable ⇒
                io.writeAndOpen(g.toDot(), "CFG", ".dot")
                io.writeAndOpen(dt.toDot(), "PostDominatorTree", ".dot")
                throw t;
        }
    }

}
