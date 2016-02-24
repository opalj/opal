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

/**
 * Tests the dominator algorithm.
 *
 * @author Stephan Neumann
 */
@RunWith(classOf[JUnitRunner])
class DominatorTest extends FlatSpec with Matchers {

    "a graph with just one node" should "yield the node dominating itself" in {
        val g = Graph.empty[AnyRef] += "a"
        dominators(g) should be(Map("a" → Set("a")))
    }

    "a graph with two connected nodes" should "yield one node dominating the other" in {
        val g = Graph.empty[AnyRef] += ("a" → "b")
        dominators(g) should be(Map("a" → Set("a"), "b" → Set("a", "b")))
    }

    "a simple graph" should "yield the correct dominators" in {
        val g = Graph.empty[AnyRef] += ("a" → "b") += ("b" → "c") += ("b" → "d") += ("a" → "e")
        dominators(g) should be(Map("a" → Set("a"), "e" → Set("e", "a"), "b" → Set("b", "a"), "d" → Set("d", "b", "a"), "c" → Set("c", "b", "a")))
    }

    "a cyclic graph" should "not crash the algorithm" in {
        val g = Graph.empty[AnyRef] += ("a" → "b") += ("b" → "c") += ("b" → "d") += ("a" → "e") += ("c" → "b")
        dominators(g) should be(Map("a" → Set("a"), "e" → Set("e", "a"), "b" → Set("b", "a"), "d" → Set("d", "b", "a"), "c" → Set("c", "b", "a")))
    }

    "a graph with a big cycle" should "not crash the algorithm" in {
        val g = Graph.empty[AnyRef] += ("a" → "b") += ("b" → "c") += ("b" → "d") += ("a" → "e") += ("d" → "f") += ("f" → "b")
        dominators(g) should be(Map("a" → Set("a"), "e" → Set("e", "a"), "b" → Set("b", "a"), "d" → Set("d", "a", "b"), "c" → Set("c", "b", "a"), "f" → Set("d", "b", "a", "f")))
    }

    "a graph with multiple paths" should "yield only the real dominators" in {
        val g = Graph.empty[AnyRef] += ("a" → "b") += ("b" → "c") += ("b" → "d") += ("a" → "e") += ("e" → "d")
        dominators(g) should be(Map("a" → Set("a"), "e" → Set("e", "a"), "b" → Set("b", "a"), "d" → Set("d", "a"), "c" → Set("c", "b", "a")))
    }

}

