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
class SmallValuesSetTest extends FunSpec with Matchers with ParallelTestExecution {

    describe("a SmallValuesSet") {

        it("it should create the potentially most efficient data structure") {
            {
                val set = SmallValuesSet.empty(300, 310)
                if (!set.isInstanceOf[SmallValuesSetBackedByOPALSet])
                    fail("expteced UByteSet base set found "+set.getClass)
            }
            {
                val set = SmallValuesSet.empty(0, 255)
                if (!set.isInstanceOf[EmptyUByteSet.type])
                    fail("expteced UByteSet base set found "+set.getClass)
            }
            {
                val set = SmallValuesSet.empty(-10, 245)
                if (!set.isInstanceOf[SmallValuesSetBackedByOPALSet])
                    fail("expteced UByteSet base set found "+set.getClass)
            }

            {
                val set = SmallValuesSet.empty(0, 65535)
                if (!set.isInstanceOf[EmptyUShortSet.type])
                    fail("expteced UShortSet base set found "+set.getClass)
            }
            {
                val set = SmallValuesSet.empty(-10, 65525)
                if (!set.isInstanceOf[SmallValuesSetBackedByOPALSet])
                    fail("expteced UShortSet based set found "+set.getClass)
            }

        }

        it("it should be possible to store the min and max value in the set") {
            { // very small set
                var set = SmallValuesSet.empty(300, 310)
                set = 300 +≈: set
                set = 310 +≈: set

                if (!set.contains(300)) fail(s"the set $set should contain the value 300")
                if (!set.contains(310)) fail(s"the set $set should contain the value 310")
                if (set.contains(305)) fail(s"the set $set should not contain the value 305")

                if (set.max != 310) fail(s"expected 310 got ${set.max} (min=${set.min})")
                set.min should be(300)
            }

            { // small set
                var set = SmallValuesSet.empty(-100, -90)
                set = -100 +≈: set
                set = -90 +≈: set

                if (!set.contains(-100)) fail(s"the set $set should contain the value -100")
                if (!set.contains(-90)) fail(s"the set $set should contain the value 90")
                if (set.contains(-95)) fail(s"the set $set should not contain the value -95")

                set.max should be(-90)
                set.min should be(-100)

            }

            { // a larger set
                var set = SmallValuesSet.empty(-100, +10000)
                set = -100 +≈: set
                set = 10000 +≈: set
                if (!set.contains(-100)) fail(s"the set $set should contain the value 300")
                if (!set.contains(10000)) fail(s"the set $set should contain the value 310")
                if (set.contains(-95)) fail(s"the set $set should not contain the value -95")
                if (set.contains(95)) fail(s"the set $set should not contain the value 95")

                set.max should be(10000)
                set.min should be(-100)
            }
        }

        it("a mutable copy should be independently mutable") {
            { // very small set
                var set = SmallValuesSet.empty(-100, 90)
                set = -100 +≈: set
                val newSet = (-95 +≈: set.mutableCopy)
                newSet should not be theSameInstanceAs(set)
                if (set.contains(-95)) fail(s"the set $set should not contain the value -95")
            }

            { // a larger set
                var set = SmallValuesSet.empty(-100, 9000)
                set = -100 +≈: set
                val newSet = (-90 +≈: set.mutableCopy)
                newSet should not be theSameInstanceAs(set)
                if (set.contains(-90)) fail(s"the set $set should not contain the value -90")
            }

            { // a very large set
                var set = SmallValuesSet.empty(-1000, 90000)
                set = -100 +≈: set
                val newSet = (-90 +≈: set.mutableCopy)
                newSet should not be theSameInstanceAs(set)
                if (set.contains(-90)) fail(s"the set $set should not contain the value -90")
            }
        }

        it("two empty sets are always a subtype of each other") {
            {
                val set1 = SmallValuesSet.empty(300, 310)
                val set2 = SmallValuesSet.empty(0, 5)

                set1.subsetOf(set2) should be(true)
                set2.subsetOf(set1) should be(true)
            }

            {
                val set1 = SmallValuesSet.empty(0, 1310)
                val set2 = SmallValuesSet.empty(0, 5)

                set1.subsetOf(set2) should be(true)
                set2.subsetOf(set1) should be(true)
            }
            {
                val set1 = SmallValuesSet.empty(-10, 1310)
                val set2 = SmallValuesSet.empty(-100, 5)

                set1.subsetOf(set2) should be(true)
                set2.subsetOf(set1) should be(true)
            }
            {
                val set1 = SmallValuesSet.empty(0, 10)
                val set2 = SmallValuesSet.empty(-100, 5)

                set1.subsetOf(set2) should be(true)
                set2.subsetOf(set1) should be(true)
            }
            {
                val set1 = SmallValuesSet.empty(0, 10000)
                val set2 = SmallValuesSet.empty(-100, 5)

                set1.subsetOf(set2) should be(true)
                set2.subsetOf(set1) should be(true)
            }
        }

        it("it should be possible to compare arbitrary small values sets") {
            {
                val set1 = SmallValuesSet.empty(300, 310)
                val set2 = SmallValuesSet.create(0, 310, 300)
                assert(set2.contains(300))

                set1.subsetOf(set2) should be(true)
                if (set2.subsetOf(set1)) fail(s"$set2 is a subset of $set1")
            }
            {
                val set1 = SmallValuesSet.empty(300, 310).+≈:(303).+≈:(304)
                val set2 = SmallValuesSet.create(0, 310, 300).+≈:(301).+≈:(303).+≈:(304)
                val set3 = set2.+≈:(304).+≈:(307).+≈:(305).+≈:(309)

                if (!set1.subsetOf(set2)) fail(s"$set1 is not a subset of $set2")
                set1.subsetOf(set3) should be(true)

                set2.subsetOf(set3) should be(true)
                set2.subsetOf(set1) should be(false)

                set3.subsetOf(set1) should be(false)
                set3.subsetOf(set2) should be(false)
            }
            {
                val set1 = SmallValuesSet.empty(-10, 310).+≈:(303).+≈:(304).+≈:(-5)
                val set2 = SmallValuesSet.create(-100, 10310, 300).+≈:(301).+≈:(-5).+≈:(303).+≈:(304)
                val set2Copy = set2.mutableCopy
                val set3 = set2.+≈:(304).+≈:(307).+≈:(305).+≈:(309)
                set2.size should be >= (set2Copy.size)
                set3.size should be >= (set2.size)
                set3.size should be > (set2Copy.size)
                set2Copy should equal(SmallValuesSet.create(-100, 10310, -5).+≈:(300).+≈:(301).+≈:(303).+≈:(304))

                if (!set1.subsetOf(set2)) fail(s"$set1 is not a subset of $set2")
                set1.subsetOf(set3) should be(true)

                set2.subsetOf(set3) should be(true)
                set2.subsetOf(set1) should be(false)

                set3.subsetOf(set1) should be(false)
                if (set3.subsetOf(set2Copy)) fail(s"$set3 is not a subset of $set2")
                if (!set2Copy.subsetOf(set3)) fail(s"$set2Copy should not be a subset of $set3")
                if (!set2Copy.subsetOf(set2)) fail(s"$set2Copy should not be a subset of $set2")
            }

            {
                val set1 = SmallValuesSet.empty(-10, 10).+≈:(3).+≈:(4).+≈:(-5)
                val set2 = SmallValuesSet.create(-100, 10310, 300).+≈:(301).+≈:(-5).+≈:(3).+≈:(4)

                if (!set1.subsetOf(set2)) fail(s"$set1 is not a subset of $set2")
                set2.subsetOf(set1) should be(false)

            }
        }

        it("an empty set should be a subset of an empty set") {
            val emptySet1 = SmallValuesSet.empty(-10, 23)
            val emptySet2 = SmallValuesSet.empty(-3, 23)
            val emptySet3 = SmallValuesSet.empty(-10, -2)
            val emptySet4 = SmallValuesSet.empty(10, 300)
            val emptySet5 = SmallValuesSet.empty(-100000, 30000)
            emptySet1.subsetOf(emptySet2) should be(true)
            emptySet1.subsetOf(emptySet2) should be(true)
            emptySet1.subsetOf(emptySet3) should be(true)
            emptySet1.subsetOf(emptySet4) should be(true)
            emptySet1.subsetOf(emptySet5) should be(true)
            emptySet2.subsetOf(emptySet1) should be(true)
            emptySet2.subsetOf(emptySet2) should be(true)
            emptySet2.subsetOf(emptySet3) should be(true)
            emptySet2.subsetOf(emptySet4) should be(true)
            emptySet2.subsetOf(emptySet5) should be(true)
            emptySet3.subsetOf(emptySet1) should be(true)
            emptySet3.subsetOf(emptySet2) should be(true)
            emptySet3.subsetOf(emptySet3) should be(true)
            emptySet3.subsetOf(emptySet4) should be(true)
            emptySet3.subsetOf(emptySet5) should be(true)
            emptySet4.subsetOf(emptySet1) should be(true)
            emptySet4.subsetOf(emptySet2) should be(true)
            emptySet4.subsetOf(emptySet3) should be(true)
            emptySet4.subsetOf(emptySet4) should be(true)
            emptySet4.subsetOf(emptySet5) should be(true)
            if (!emptySet5.subsetOf(emptySet1)) fail(s"$emptySet5 is not a subset of $emptySet1")
            emptySet5.subsetOf(emptySet2) should be(true)
            emptySet5.subsetOf(emptySet3) should be(true)
            emptySet5.subsetOf(emptySet4) should be(true)
            emptySet5.subsetOf(emptySet5) should be(true)

        }

        it("should be possible to add the elements of two singleton sets") {
            {
                val r = SmallValuesSet.create(-4, 10000, 47) ++ SmallValuesSet.create(-4, 10000, 51)
                r.size should be(2)
                r.contains(47) should be(true)
                r.contains(51) should be(true)
            }
            {
                val r = SmallValuesSet.create(-4, 10000, 51) ++ SmallValuesSet.create(-4, 10000, 47)
                r.size should be(2)
                r.contains(47) should be(true)
                r.contains(51) should be(true)
            }

        }

    }

}
