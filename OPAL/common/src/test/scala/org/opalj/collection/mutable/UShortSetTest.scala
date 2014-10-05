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
package collection
package mutable

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSpec
import org.scalatest.Matchers
import org.scalatest.ParallelTestExecution

/**
 * Tests UShortSet.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class UShortSetTest extends FunSpec with Matchers with ParallelTestExecution {

    describe("an UShortSet") {

        it("it should be possible to store the single value 0 in the set") {
            var set = UShortSet.empty
            set = 0 +≈: set

            set.contains(0) should be(true)
            for (i ← (1 to UShort.MaxValue))
                set.contains(i) should be(false)
        }

        it("it should be possible to store the single value 0xFFFF === 65535 === UShort.MaxValue in the set") {
            var set = UShortSet.empty
            set = UShort.MaxValue +≈: set

            set.contains(UShort.MaxValue) should be(true)
            for (i ← (0 until UShort.MaxValue))
                set.contains(i) should be(false)
        }

        it("it should be possible to iterate over a set with just one value") {
            var set = UShortSet.empty
            set = 232 +≈: set

            set.iterator.toSet should be(Set(232))
        }

        it("it should be possible to iterate over a set with just two values") {
            var set = UShortSet.empty
            set = 232 +≈: set
            set = 23 +≈: set

            set.iterator.toSet should be(Set(23, 232))
        }

        it("it should be possible to iterate over a set with just three values") {
            var set = UShortSet.empty
            set = 232 +≈: set
            set = 3 +≈: set
            set = 23 +≈: set

            set.iterator.toSet should be(Set(3, 23, 232))
        }

        it("it should be possible to iterate over a set with just four values") {
            var set = UShortSet.empty
            set = 232 +≈: set
            set = 3 +≈: set
            set = 23 +≈: set
            set = 2 +≈: set

            set.iterator.toSet should be(Set(2, 3, 23, 232))
        }

        it("it should be possible – though the datastructure is not intended to be used for that – to store a larger number of different values in the set") {
            val seed = 72387238787323390l
            val ValuesCount = 100000
            val rnd = new java.util.Random
            var uShortSet = UShortSet.empty

            rnd.setSeed(seed)
            for (i ← (0 until ValuesCount))
                uShortSet = rnd.nextInt(0xFFFF + 1) +≈: uShortSet

            var scalaSet = Set.empty[Int]
            rnd.setSeed(seed)
            for (i ← (0 until ValuesCount))
                scalaSet += rnd.nextInt(0xFFFF + 1)

            uShortSet.size should equal(scalaSet.size) // we use a random number generator...
            info(s"stored ${scalaSet.size} elemets - using 100000 insertions and ${uShortSet.nodeCount} nodes - in the set")
            scalaSet.forall(uShortSet.contains(_)) should be(true)

            rnd.setSeed(seed)
            for (i ← (0 until ValuesCount)) {
                val value = rnd.nextInt(0xFFFF + 1)
                if (!uShortSet.contains(value)) fail(s"the $i. value ($value) was not stored in the set")
            }
        }

        it("the number of leaf nodes should be 3 if we add nine distinct value") {

            var uShortSet = UShortSet.empty

            val values = List(3, 40033, 23433, 11233, 2, 233, 23, 1233, 55555)

            values.foreach(v ⇒ uShortSet = v +≈: uShortSet)
            uShortSet.iterator.toSet should equal(values.toSet)

            if (uShortSet.nodeCount > 3) {
                val file =
                    org.opalj.util.writeAndOpen(
                        org.opalj.graphs.toDot((
                            for (i ← (4 to values.size)) yield {
                                var uShortSet = UShortSet.empty
                                values.take(i).foreach(v ⇒ uShortSet = v +≈: uShortSet)
                                uShortSet.asGraph
                            }
                        ).toSet),
                        "UShortSet-"+8,
                        ".dot")

                fail(s"two many nodes: ${uShortSet.nodeCount} (expected 3})(details: $file)")
            }
        }

        it("the number of leaf nodes should be close to 1/4 of the number of entries") {
            val seed = -983432872323390l
            val ValuesCount = 10000
            val rnd = new java.util.Random
            var uShortSet = UShortSet.empty

            rnd.setSeed(seed)
            var valueToBeAdded: UShort = 0
            try {
                for (i ← (0 until ValuesCount)) {
                    valueToBeAdded = rnd.nextInt(0xFFFF + 1)
                    uShortSet = valueToBeAdded +≈: uShortSet
                }
            } catch {
                case e: Exception ⇒
                    org.opalj.util.writeAndOpen(
                        org.opalj.graphs.toDot(Set(uShortSet.asGraph)),
                        "UShortSet-CREATION_FAILED_FOR_VALUE_"+valueToBeAdded+"-"+ValuesCount,
                        ".dot")
                    throw e
            }
            val nodeCount = uShortSet.nodeCount
            if (!(nodeCount < (ValuesCount / 3))) {
                val file = org.opalj.util.writeAndOpen(
                    org.opalj.graphs.toDot(Set(uShortSet.asGraph)),
                    "UShortSet-"+ValuesCount,
                    ".dot")
                fail(s"two many nodes: ${uShortSet.nodeCount} (expected << ${ValuesCount / 3})(details: $file)")
            } else {
                info(s"for storing $ValuesCount values $nodeCount nodes are required")
            }
        }
    }

}
