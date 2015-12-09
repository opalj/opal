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

/**
 * Tests UByteSet.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class UByteSetTest extends FunSpec with Matchers {

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
            UByteSet.empty.subsetOf(UByteSet(92)) should be(true)
            UByteSet(92).subsetOf(UByteSet(92)) should be(true)
            UByteSet(92).subsetOf(UByteSet(92) ++ UByteSet(70)) should be(true)
            UByteSet(92).subsetOf(UByteSet(92).+≈:(70).+≈:(3)) should be(true)
            UByteSet(92).subsetOf(UByteSet(92).+≈:(70).+≈:(3).+≈:(100)) should be(true)

            UByteSet(92).+≈:(70).subsetOf(UByteSet(92).+≈:(70).+≈:(3).+≈:(100)) should be(true)
            UByteSet(92).+≈:(70).+≈:(3).subsetOf(UByteSet(92).+≈:(70).+≈:(3).+≈:(100)) should be(true)
            UByteSet(92).+≈:(70).+≈:(3).+≈:(100).subsetOf(UByteSet(92).+≈:(70).+≈:(3).+≈:(100)) should be(true)

            UByteSet(92).+≈:(3).+≈:(100).subsetOf(UByteSet(92).+≈:(70).+≈:(3).+≈:(100)) should be(true)
            UByteSet(92).+≈:(100).subsetOf(UByteSet(92).+≈:(70).+≈:(3).+≈:(100)) should be(true)
            UByteSet(92).+≈:(100).+≈:(70).+≈:(3).subsetOf(UByteSet(92).+≈:(70).+≈:(3).+≈:(100)) should be(true)

            // Handled by UBytesSetNode
            UByteSet.empty.subsetOf(UByteSet(92).+≈:(70).+≈:(3).+≈:(100).+≈:(34)) should be(true)
            UByteSet(92).+≈:(100).+≈:(70).+≈:(3).subsetOf(UByteSet(92).+≈:(70).+≈:(3).+≈:(100).+≈:(34)) should be(true)
            UByteSet(92).+≈:(34).+≈:(100).+≈:(70).+≈:(3).subsetOf(UByteSet(92).+≈:(70).+≈:(3).+≈:(100).+≈:(34)) should be(true)
        }

        it("the same set should be returned if the set does not contain the vaue") {
            // HANDLED BY UByteSet4
            val set = UByteSet(92).+≈:(70).+≈:(3).+≈:(100)

            (set - 1) should be theSameInstanceAs (set)
            (set - 5) should be theSameInstanceAs (set)
            (set - 200) should be theSameInstanceAs (set)
        }

        it("it should be possible to call remove on empty sets") {
            // HANDLED BY UByteSet4
            val set = UByteSet.empty
            val newSet = (set - 3)
            newSet.size should be(0)
            newSet should be('empty)
        }

        it("it should be possible to remove the 1st element of a set with one element") {
            // HANDLED BY UByteSet4
            val set = UByteSet(92)
            val newSet = (set - 92)
            if (newSet.size != 0) fail("should be empty: "+newSet)
            newSet.contains(92) should be(false)
        }

        it("it should be possible to remove the 1st element of a set with 4 elements") {
            // HANDLED BY UByteSet4
            val set = UByteSet(92).+≈:(70).+≈:(3).+≈:(100)
            val newSet = (set - 3)
            if (newSet.size != 3) fail("should contain only three elements: "+newSet)
            newSet.contains(3) should be(false)
        }

        it("it should be possible to remove the 2nd element of a set with 4 elements") {
            // HANDLED BY UByteSet4
            val set = UByteSet(92).+≈:(70).+≈:(3).+≈:(100)
            val newSet = (set - 70)
            newSet.size should be(3)
            newSet.contains(70) should be(false)
        }

        it("it should be possible to remove the 3rd element of a set with 4 elements") {
            // HANDLED BY UByteSet4
            val set = UByteSet(92).+≈:(70).+≈:(3).+≈:(100)
            val newSet = (set - 92)
            newSet.size should be(3)
            newSet.contains(92) should be(false)
        }

        it("it should be possible to remove the 4th element of a set with 4 elements") {
            // HANDLED BY UByteSet4
            val set = UByteSet(92).+≈:(70).+≈:(3).+≈:(100)
            val newSet = (set - 100)
            newSet.size should be(3)
            newSet.contains(100) should be(false)
        }

        it("it should be possible to remove the 4th element of a set with 5 elements") {
            // HANDLED BY UByteSet4
            val set = UByteSet(92).+≈:(70).+≈:(3).+≈:(100).+≈:(50)
            val newSet = (set - 92)
            newSet.size should be(4)
            newSet.contains(92) should be(false)
        }

        it("it should be possible to remove the 5th element of a set with 5 elements") {
            // HANDLED BY UByteSet4
            val set = UByteSet(92).+≈:(70).+≈:(3).+≈:(100).+≈:(50)
            val newSet = (set - 100)
            newSet.size should be(4)
            newSet.contains(100) should be(false)
        }

        it("it should be possible to remove one of the four largest values from a corresponding set") {
            val set = org.opalj.collection.mutable.UByteSet(253).+≈:(254).+≈:(255).+≈:(252)
            set.size should be(4)
            val newSet = set - 252
            if (newSet.size != 3)
                fail(s"could not remove 252 from $set (index: ${set.asInstanceOf[UByteSet4].indexOf(252)}) => $newSet")
        }

        it("it should be possible to remove all possible values in the set - starting with the smallest value") {
            var set = UByteSet.empty

            for (i ← (0 to UByte.MaxValue)) {
                set = i +≈: set
            }
            set.size should be(256)

            for (i ← (0 to UByte.MaxValue)) {
                set = set - i
                val expectedSize = (256 - (i + 1))
                if (set.size != expectedSize) fail(s"expected size is $expectedSize: "+set)
            }

        }

        it("it should be possible to remove all possible values in the set - starting with the largest value") {
            var set = UByteSet.empty

            for (i ← (0 to UByte.MaxValue)) {
                set = i +≈: set
            }
            set.size should be(256)

            for (i ← (0 to UByte.MaxValue).reverse) {
                set = set - i
                set.size should be(256 - (256 - i))
            }
        }
    }

}
