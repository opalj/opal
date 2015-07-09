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
 * Tests UByteSet.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class UByteSetTest extends FunSpec with Matchers with ParallelTestExecution {

    describe("an UByteSet") {

        it("it should be possible to store the single value 0 in the set") {
            var set = UByteSet.empty
            set = 0 +≈: set

            set.size should be(1)

            set.contains(0) should be(true)
            for (i ← (1 to UByte.MaxValue))
                set.contains(i) should be(false)
        }

        it("it should be possible to store the single value 0xFF === 255 === UByte.MaxValue in the set") {
            var set = UByteSet.empty
            set = UByte.MaxValue +≈: set

            set.size should be(1)

            set.contains(UByte.MaxValue) should be(true)
            for (i ← (0 until UByte.MaxValue))
                set.contains(i) should be(false)
        }

        it("it should be possible to store all possible values in the set") {
            var set = UByteSet.empty

            for (i ← (0 to UByte.MaxValue)) {
                set = i +≈: set
            }
            set.size should be(256)

            var lastValue = -1
            set.foreach { v ⇒
                (v) should be(lastValue + 1)
                lastValue = v
            }
        }

        it("it should be possible – though the datastructure is not intended to be used for that – to store a larger number of different values in the set") {
            val seed = 21372380337323390l
            val ValuesCount = 100000
            val rnd = new java.util.Random
            rnd.setSeed(seed)
            var uByteSet = UByteSet.empty
            var scalaSet = Set.empty[Int]
            for (i ← (0 until ValuesCount)) {
                val value = rnd.nextInt(0xFF + 1)
                if (scalaSet.contains(value)) {
                    if ((value +≈: uByteSet) ne uByteSet) {
                        fail(s"adding the existing value $value to $uByteSet (Structure: ${uByteSet.structure}) resulted in a new instance")
                    }
                } else {
                    uByteSet = value +≈: uByteSet
                    scalaSet += value
                }
            }
            info(s"stored ${scalaSet.size} elemets - using 100000 insertions")

            scalaSet.forall(uByteSet.contains(_)) should be(true)
            uByteSet.size should equal(scalaSet.size) // we use a random number generator...

        }

        it("it should be possible to test the subtypeOf relation between constructed sets") {
            // HANDLED BY UByteSet4
            UByteSet.empty.isSubsetOf(UByteSet(92)) should be(true)
            UByteSet(92).isSubsetOf(UByteSet(92)) should be(true)
            UByteSet(92).isSubsetOf(UByteSet(92) ++ UByteSet(70)) should be(true)
            UByteSet(92).isSubsetOf(UByteSet(92).+≈:(70).+≈:(3)) should be(true)
            UByteSet(92).isSubsetOf(UByteSet(92).+≈:(70).+≈:(3).+≈:(100)) should be(true)

            UByteSet(92).+≈:(70).isSubsetOf(UByteSet(92).+≈:(70).+≈:(3).+≈:(100)) should be(true)
            UByteSet(92).+≈:(70).+≈:(3).isSubsetOf(UByteSet(92).+≈:(70).+≈:(3).+≈:(100)) should be(true)
            UByteSet(92).+≈:(70).+≈:(3).+≈:(100).isSubsetOf(UByteSet(92).+≈:(70).+≈:(3).+≈:(100)) should be(true)

            UByteSet(92).+≈:(3).+≈:(100).isSubsetOf(UByteSet(92).+≈:(70).+≈:(3).+≈:(100)) should be(true)
            UByteSet(92).+≈:(100).isSubsetOf(UByteSet(92).+≈:(70).+≈:(3).+≈:(100)) should be(true)
            UByteSet(92).+≈:(100).+≈:(70).+≈:(3).isSubsetOf(UByteSet(92).+≈:(70).+≈:(3).+≈:(100)) should be(true)

            // Handled by UBytesSetNode
            UByteSet.empty.isSubsetOf(UByteSet(92).+≈:(70).+≈:(3).+≈:(100).+≈:(34)) should be(true)
            UByteSet(92).+≈:(100).+≈:(70).+≈:(3).isSubsetOf(UByteSet(92).+≈:(70).+≈:(3).+≈:(100).+≈:(34)) should be(true)
            UByteSet(92).+≈:(34).+≈:(100).+≈:(70).+≈:(3).isSubsetOf(UByteSet(92).+≈:(70).+≈:(3).+≈:(100).+≈:(34)) should be(true)
        }
    }

}
