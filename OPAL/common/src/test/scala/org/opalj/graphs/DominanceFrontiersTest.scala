/*
 * BSD 2-Clause License:
 * Copyright (c) 2009 - 2016
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *   - Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *   - Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
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
import org.opalj.collection.immutable.EmptySmallValuesSet
import org.opalj.collection.mutable
import org.scalatest.FlatSpec
import org.scalatest.Matchers
import org.scalatest.junit.JUnitRunner

/**
 * Test the [[DominanceFrontiers]] implementation.
 *
 * Dominance frontiers are defined as follows:
 *
 * Dominance frontier of node w:
 * Node u is in dominance frontier of node w if w dominates a CFG predecessor v of u,
 * but does not strictly dominate u does not strictly dominate u.
 *
 * @author Michael Reif
 */
@RunWith(classOf[JUnitRunner])
class DominanceFrontiersTest extends FlatSpec with Matchers {

    "a dominance tree with a single node" should "result in no dominance frontiers" in {
        val graph = Graph.empty[Int] += 0
        val (_ /*dt*/ , df) = DominanceFrontiersTest.setUpDominanceFrontiers(0, graph, 0)

        df.df(0) should be(EmptySmallValuesSet)
    }

    "a dominance tree with a single cyclic node" should "result in a reflexive dominance frontier" in {
        val graph = Graph.empty[Int] += (0 → 0)
        val (_ /*dt*/ , df) = DominanceFrontiersTest.setUpDominanceFrontiers(0, graph, 0, true)

        //        org.opalj.io.writeAndOpen(dt.toDot(), "graph", ".dt.gv")
        //        org.opalj.io.writeAndOpen(df.toDot(), "graph", ".df.gv")

        df.df(0) should be(mutable.SmallValuesSet.create(1, 0))
    }

    "a degenerated dominance tree (path)" should "result in no dominance frontiers" in {
        val graph = Graph.empty[Int] += (0 → 1) += (1 → 2) += (2 → 3) += (3 → 4)

        val (_ /*dt*/ , df) = DominanceFrontiersTest.setUpDominanceFrontiers(0, graph, 4)

        //        org.opalj.io.writeAndOpen(dt.toDot(), "graph", ".dt.gv")
        //        org.opalj.io.writeAndOpen(df.toDot(), "graph", ".df.gv")

        df.df(0) should be(EmptySmallValuesSet)
        df.df(1) should be(EmptySmallValuesSet)
        df.df(2) should be(EmptySmallValuesSet)
        df.df(3) should be(EmptySmallValuesSet)
        df.df(4) should be(EmptySmallValuesSet)
    }

    "a dominance tree from an if-statement" should "be handled properly" in {
        val graph = Graph.empty[Int] += (0 → 1) += (1 → 2) += (1 → 3) += (2 → 4) += (3 → 4)

        val (_ /*dt*/ , df) = DominanceFrontiersTest.setUpDominanceFrontiers(0, graph, 4)

        //        org.opalj.io.writeAndOpen(dt.toDot(), "graph", ".dt.gv")
        //        org.opalj.io.writeAndOpen(df.toDot(), "graph", ".df.gv")

        df.df(1) should be(EmptySmallValuesSet)
        df.df(2) should be(mutable.SmallValuesSet.create(1, 4))
        df.df(3) should be(mutable.SmallValuesSet.create(1, 4))
        df.df(4) should be(EmptySmallValuesSet)
    }

    "a domiance tree that captures a guard" should "reflect the corresponding dominance frontiers" in {
        val graph = Graph.empty[Int] += (0 → 1) += (1 → 2) += (2 → 3) += (1 → 3)

        val (_ /*dt*/ , df) = DominanceFrontiersTest.setUpDominanceFrontiers(0, graph, 3)

        df.df(1) should be(EmptySmallValuesSet)
        df.df(2) should be(createSmallValueSet(Set(3)))
        df.df(3) should be(EmptySmallValuesSet)
    }

    "a dominance tree from an nested if-statement" should "be handled properly" in {
        val graph = Graph.empty[Int] += (0 → 1) += (1 → 2) += (1 → 6) += (2 → 3) += (2 → 4) += (3 → 5) += (4 → 5) += (5 → 7) += (6 → 7)

        val (_ /*dt*/ , df) = DominanceFrontiersTest.setUpDominanceFrontiers(0, graph, 7)

        df.df(0) should be(EmptySmallValuesSet)
        df.df(1) should be(EmptySmallValuesSet)
        df.df(2) should be(mutable.SmallValuesSet.create(1, 7))
        df.df(3) should be(mutable.SmallValuesSet.create(1, 5))
        df.df(4) should be(mutable.SmallValuesSet.create(1, 5))
        df.df(5) should be(mutable.SmallValuesSet.create(1, 7))
        df.df(6) should be(mutable.SmallValuesSet.create(1, 7))
        df.df(7) should be(EmptySmallValuesSet)
    }

    " a dominance tree that captures a cycle" should "be handled properly" in {
        val graph = Graph.empty[Int] += (0 → 1) += (1 → 2) += (2 → 0)

        val (_ /*dt*/ , df) = DominanceFrontiersTest.setUpDominanceFrontiers(0, graph, 2, true)

        df.df(2) should be(createSmallValueSet(Set(0)))
    }

    /* refered paper:  Efficiently Computing Static Single Assignment Form and the Control Dependence Graph */
    "a dominance tree derived from the paper's graph" should "result in the correct dominance frontiers" in {

        val graph = org.opalj.graphs.Graph.empty[Int] += (0 → 1) += (1 → 2) += (2 → 3) += (2 → 7) += (3 → 4) += (3 → 5) += (5 → 6) += (4 → 6) += (6 → 8) += (7 → 8) += (8 → 9) += (9 → 10) += (9 → 11) += (10 → 11) += (11 → 9) += (11 → 12) += (12 → 13) += (12 → 2) += (0 → 13)

        val (_ /*dt*/ , df) = DominanceFrontiersTest.setUpDominanceFrontiers(0, graph, 13)

        //        org.opalj.io.writeAndOpen(dt.toDot(), "graph", ".dt.gv")
        //        org.opalj.io.writeAndOpen(df.toDot(), "graph", ".df.gv")

        df.df(0) should be(EmptySmallValuesSet)
        df.df(1) should be(createSmallValueSet(Set(13)))
        df.df(2) should be(createSmallValueSet(Set(2, 13)))
        df.df(3) should be(createSmallValueSet(Set(8)))
        df.df(4) should be(createSmallValueSet(Set(6)))
        df.df(5) should be(createSmallValueSet(Set(6)))
        df.df(6) should be(createSmallValueSet(Set(8)))
        df.df(7) should be(createSmallValueSet(Set(8)))
        df.df(8) should be(createSmallValueSet(Set(2, 13)))
        df.df(9) should be(createSmallValueSet(Set(2, 9, 13)))
        df.df(10) should be(createSmallValueSet(Set(11)))
        df.df(11) should be(createSmallValueSet(Set(2, 9, 13)))
        df.df(12) should be(createSmallValueSet(Set(2, 13)))
        df.df(13) should be(EmptySmallValuesSet)

    }

  "a sparse dominance tree derived from the paper's graph" should "result in the correct dominance frontiers" in {

    val graph = org.opalj.graphs.Graph.empty[Int] += (0 → 1) += (1 → 2) += (2 → 3) += (2 → 7) += (3 → 4) += (3 → 5) += (5 → 6) += (4 → 6) += (6 → 8) += (7 → 8) += (8 → 9) += (9 → 10) += (9 → 11) += (10 → 11) += (11 → 9) += (11 → 12) += (12 → 13) += (12 → 2) += (0 → 13)

    val (_ /*dt*/ , df) = DominanceFrontiersTest.setUpDominanceFrontiers(0, graph, 13)

    //        org.opalj.io.writeAndOpen(dt.toDot(), "graph", ".dt.gv")
    //        org.opalj.io.writeAndOpen(df.toDot(), "graph", ".df.gv")

    df.df(0) should be(EmptySmallValuesSet)
    df.df(1) should be(createSmallValueSet(Set(13)))
    df.df(2) should be(createSmallValueSet(Set(2, 13)))
    df.df(3) should be(createSmallValueSet(Set(8)))
    df.df(4) should be(createSmallValueSet(Set(6)))
    df.df(5) should be(createSmallValueSet(Set(6)))
    df.df(6) should be(createSmallValueSet(Set(8)))
    df.df(7) should be(createSmallValueSet(Set(8)))
    df.df(8) should be(createSmallValueSet(Set(2, 13)))
    df.df(9) should be(createSmallValueSet(Set(2, 9, 13)))
    df.df(10) should be(createSmallValueSet(Set(11)))
    df.df(11) should be(createSmallValueSet(Set(2, 9, 13)))
    df.df(12) should be(createSmallValueSet(Set(2, 13)))
    df.df(13) should be(EmptySmallValuesSet)

  }

    def createSmallValueSet(set: Set[Int]): mutable.SmallValuesSet = {
        var svs = mutable.SmallValuesSet.empty(set.size)
        for (num ← set)
            svs = svs.+≈:(num)
        svs
    }
}

object DominanceFrontiersTest {

    def setUpDominanceFrontiers(
        startNode:                 Int,
        g:                         Graph[Int],
        maxNode:                   Int,
        startNodeHasPredecesssors: Boolean    = false
    ): (DominatorTree, DominanceFrontiers) = {
      setUpDominanceFrontiers(startNode,
        g,
        maxNode,
        (n: Int) => n>= startNode && n<= maxNode,
        startNodeHasPredecesssors
      )
    }

  def setUpDominanceFrontiers(
                               startNode:                 Int,
                               g:                         Graph[Int],
                               maxNode:                   Int,
                               isValidNode: Int => Boolean,
                               startNodeHasPredecesssors: Boolean    = false
                             ): (DominatorTree, DominanceFrontiers) = {
    val foreachSuccessor = (n: Int) ⇒ g.successors.getOrElse(n, List.empty).foreach _
    val foreachPredecessor = (n: Int) ⇒ g.predecessors.getOrElse(n, List.empty).foreach _
    val isValidNode = (n: Int) ⇒ n >= startNode && n <= maxNode
    val dominatorTreeFactory = DominatorTreeFactory(startNode, startNodeHasPredecesssors, foreachSuccessor, foreachPredecessor, maxNode)

    (dominatorTreeFactory.dt, DominanceFrontiers(
      dominatorTreeFactory,
      isValidNode
    ))
  }
}
