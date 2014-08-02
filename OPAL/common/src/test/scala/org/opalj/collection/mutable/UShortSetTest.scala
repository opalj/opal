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
 * Tests the IntegerRanges Domain.
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

        it("it should be possible to store the single value 0xFFFF-1 === 65535 === UShort.MaxValue in the set") {
            var set = UShortSet.empty
            set = UShort.MaxValue +≈: set

            set.contains(UShort.MaxValue) should be(true)
            for (i ← (0 until UShort.MaxValue))
                set.contains(i) should be(false)
        }

        it("it should be possible to store a larger number of different values in the set") {
            val seed = 72387238787323390l
            val ValuesCount = 1000
            val rnd = new java.util.Random
            var uShortSet = UShortSet.empty

            rnd.setSeed(seed)
            for (i ← (0 until ValuesCount))
                uShortSet = rnd.nextInt(0xFFFF) +≈: uShortSet

            var scalaSet = Set.empty[Int]
            rnd.setSeed(seed)
            for (i ← (0 until ValuesCount))
                scalaSet += rnd.nextInt(0xFFFF)

            uShortSet.size should equal(scalaSet.size) // we use a random number generator...

            scalaSet.forall(uShortSet.contains(_)) should be(true)

            rnd.setSeed(seed)
            for (i ← (0 until ValuesCount)) {
                val value = rnd.nextInt(0xFFFF)
                if (!uShortSet.contains(value)) fail(s"the $i. value ($value) was not stored in the set")
            }
        }

    }

}
