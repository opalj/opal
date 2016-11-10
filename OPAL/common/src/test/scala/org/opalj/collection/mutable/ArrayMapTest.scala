/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2016
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

import org.scalatest.Matchers
import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

/**
 * Tests the ArrayMap.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class ArrayMapTest extends FlatSpec with Matchers {

    behavior of "an ArrayMap data structure"

    it should ("be empty if it is newly created") in {
        ArrayMap.empty.foreachValue { e ⇒ fail("non empty") }
        ArrayMap(100).foreachValue { e ⇒ fail("non empty") }
    }

    it should ("should only contain those elements that are added even if some keys are not used") in {
        val map = ArrayMap.empty[Integer]
        map(0) = 0
        map(2) = 2

        map(0) should be(0)
        map(1) should be(null)
        map(2) should be(2)

        // Never stored in the map:
        map(3) should be(null)
        map(1000) should be(null)
    }

    it should ("should only contain those elements that are added even if the first elements are empty") in {
        val map = ArrayMap.empty[Integer]
        map(1) = 102
        map(0) should be(null)
        map(1) should be(102)
        map(2) should be(null)
    }

    it should ("should only contain those elements that are not removed") in {
        val map = ArrayMap.empty[Integer]
        map(1) = 102
        map(0) should be(null)
        map(1) should be(102)
        map(2) should be(null)

        map(3) = 103
        map(3) should be(103)

        map.remove(0)
        map.remove(1)
        map(0) should be(null)
        map(1) should be(null)
        map(2) should be(null)
        map(3) should be(103)

        map.remove(3)
        map(3) should be(null)
    }

    it should ("should only contain the most recent value") in {
        val map = ArrayMap.empty[Integer]
        map(1) = 101
        map(0) should be(null)
        map(1) should be(101)
        map(2) should be(null)

        map(3) = 103
        map(3) should be(103)

        map(0) = 100
        map(1) = 201
        map(2) = 102
        map(3) = 203

        map(0) should be(100)
        map(1) should be(201)
        map(2) should be(102)
        map(3) should be(203)

    }

    it should ("correctly implement a deep equals for empty array with different size hints") in {
        val m1 = ArrayMap.empty.foreachValue { e ⇒ fail("non empty") }
        val m2 = ArrayMap(100).foreachValue { e ⇒ fail("non empty") }

        m1 should equal(m2)
        m2 should equal(m1)
    }

    it should ("correctly implement a deep equals for two identical arrays") in {
        val m1 = ArrayMap.empty[Integer]
        val m2 = ArrayMap.empty[Integer]
        m1(0) = 0
        m1(2) = 2
        m2(0) = 0
        m2(2) = 2

        m1 should equal(m2)
        m2 should equal(m1)
    }

    it should ("correctly implement a deep equals for two unequal arrays of same size") in {
        val m1 = ArrayMap.empty[Integer]
        val m2 = ArrayMap.empty[Integer]
        m1(0) = 0
        m1(2) = 2
        m2(0) = 0
        m2(2) = 3

        m1 should not equal (m2)
        m2 should not equal (m1)
    }

    it should ("correctly implement a deep equals for two unequal arrays of different size") in {
        val m1 = ArrayMap.empty[Integer]
        val m2 = ArrayMap.empty[Integer]
        m1(0) = 0
        m1(2) = 2
        m2(0) = 0
        m2(3) = 2

        m1 should not equal (m2)
        m2 should not equal (m1)

        m1(4) = 4

        m1 should not equal (m2)
        m2 should not equal (m1)
    }

    it should ("correctly implement the hashCode method ") in {
        val m1 = ArrayMap.empty.foreachValue { e ⇒ fail("non empty") }
        val m2 = ArrayMap(100).foreachValue { e ⇒ fail("non empty") }

        m1.hashCode() should be(m2.hashCode())
        m2.hashCode() should be(m1.hashCode())
    }

    it should ("have a correct values iterator if the map is not continuous") in {
        val map = ArrayMap.empty[Integer]
        map(0) = 0
        map(2) = 2
        map(4) = 4
        map.values.size should be(3)
        map.values.map(_.intValue()).sum should be(6)
    }

    it should ("have a correct values iterator if the map is continuous") in {
        val map = ArrayMap.empty[Integer]
        map(0) = 0
        map(2) = 2
        map(3) = 3
        map.values.size should be(3)
        map.values.map(_.intValue()).sum should be(5)
    }

    it should ("have a correct values iterator if the map is empty") in {
        val map = ArrayMap.empty[Integer]
        map.values.size should be(0)
        map.values.map(_.intValue()).sum should be(0)
    }

    it should ("have a useable map implementation") in {
        val map = ArrayMap.empty[Integer]
        map(0) = 0
        map(2) = 2
        map(4) = 4
        val rs = map.map { (i, e) ⇒ i * e }
        rs.size should be(3)
        rs.sum should be(20)
    }

    it should ("be able to correctly iterate over the elements of an empty map") in {
        val map = ArrayMap.empty[Integer]
        map.entries.size should be(0)
        map.entries should be(empty)
    }

    it should ("be able to correctly iterate over the elements of a continues map") in {
        val map = ArrayMap.empty[Integer]
        map(0) = 0
        map(1) = 1
        map(2) = 2
        map.entries.size should be(3)
        map.entries.forall { iv ⇒ val (i, v) = iv; i == v.intValue } should be(true)
    }

    it should ("be able to correctly iterate over the elements of a non-continues map") in {
        val map = ArrayMap.empty[Integer]
        map(0) = 0
        map(1) = 1
        map(4) = 4
        map(100) = 100
        map.entries.size should be(4)
        map.entries.forall { iv ⇒ val (i, v) = iv; i == v.intValue } should be(true)
    }

    it should ("create a toString representation that enables the creation of a map") in {
        val map = ArrayMap.empty[Integer]
        map(0) = 0
        map(2) = 102
        map(4) = 6
        map.toString should be("ArrayMap(0 -> 0, 2 -> 102, 4 -> 6)")
    }

}
