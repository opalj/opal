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
package collection

import org.scalacheck.Properties
import org.scalacheck.Gen
import org.scalacheck.Arbitrary
import org.scalacheck.Prop.forAll
import org.scalacheck.Prop.classify
import org.scalacheck.Prop.BooleanOperators

import org.scalatest.FunSpec
import org.scalatest.Matchers

import java.util.Arrays

import org.opalj.collection.immutable.IntArraySet
import org.opalj.collection.immutable.Chain
import org.opalj.collection.immutable.Naught

/**
 * Tests IntIterator.
 *
 * @author Michael Eichberg
 */
object IntIteratorProperties extends Properties("IntIterator") {

    implicit val arbIntArraySet: Arbitrary[IntArraySet] = Arbitrary {
        val r = new java.util.Random()
        Gen.sized { l ⇒
            (0 until l).foldLeft(IntArraySet.empty) { (c, n) ⇒ c + r.nextInt(100) - 50 }
        }
    }

    property("exists") = forAll { (is: IntArraySet, v: Int) ⇒
        classify(is.contains(v), "v is in the set") {
            is.intIterator.exists(_ == v) == is.iterator.exists(_ == v)
        }
    }

    property("forall") = forAll { (is: IntArraySet, v: Int) ⇒
        classify(is.contains(v), "v is in the set") {
            is.intIterator.forall(_ != v) == is.iterator.forall(_ != v)
        }
    }

    property("contains") = forAll { (is: IntArraySet, values: IntArraySet) ⇒
        values.forall { v ⇒
            is.intIterator.contains(v) == is.iterator.contains(v)
        }
    }

    property("foldLeft") = forAll { (is: IntArraySet) ⇒
        is.intIterator.foldLeft(0)(_ + _) == is.iterator.foldLeft(0)(_ + _)
    }

    property("map") = forAll { (is: IntArraySet) ⇒
        is.intIterator.map(_ + 1).toChain == is.map(_ + 1).toChain
    }

    property("foreach") = forAll { (is: IntArraySet) ⇒
        var c: Chain[Int] = Naught
        is.intIterator.foreach { i ⇒ c = i :&: c }
        c.reverse == is.toChain
    }

    property("filter") = forAll { (is: IntArraySet, values: IntArraySet) ⇒
        (is.intIterator.filter(values.contains).forall(values.contains) :| "filter => forall") &&
            is.intIterator.filter(values.contains).toChain == is.withFilter(values.contains).toChain
    }

    property("withFilter") = forAll { (is: IntArraySet, values: IntArraySet) ⇒
        is.intIterator.withFilter(values.contains).toChain == is.withFilter(values.contains).toChain
    }

    property("toArray") = forAll { (is: IntArraySet) ⇒
        val itArray = is.intIterator.toArray
        val isArray = is.toChain.toArray
        Arrays.equals(itArray, isArray) :| isArray.mkString(",")+" vs. "+itArray.mkString(",")
    }

    property("toChain") = forAll { (is: IntArraySet) ⇒
        is.intIterator.toChain == is.toChain
    }

    property("mkString") = forAll { (is: IntArraySet) ⇒
        is.intIterator.mkString("-", ";", "-!") == is.iterator.mkString("-", ";", "-!")
    }

    property("iterator") = forAll { (is: IntArraySet) ⇒
        val it = is.iterator
        val intIt = is.intIterator.iterator
        intIt.forall(i ⇒ it.next == i) && !it.hasNext
    }
}

class IntIteratorTest extends FunSpec with Matchers {

    describe("an empty IntIterator") {

        it("hasNextValue should return false") {
            assert(!IntIterator.empty.hasNext)
        }

        it("should return an empty array") {
            assert(IntIterator.empty.toArray.length == 0)
        }

    }

    describe("a singleton IntIterator") {

        it("should iterate over the given value") {
            val it = IntIterator(1);

            assert(it.hasNext)
            assert(it.next == 1)
            assert(!it.hasNext)
        }

        it("should return an array with specified value") {
            assert(Arrays.equals(IntIterator(1).toArray, Array(1)))
        }
    }

    describe("an IntIterator for two values") {

        it("should iterate over both values") {
            val it = IntIterator(1, 2);

            assert(it.hasNext)
            assert(it.next == 1)
            assert(it.hasNext)
            assert(it.next == 2)
            assert(!it.hasNext)
        }

        it("should return an array with specified values") {
            assert(Arrays.equals(IntIterator(1, 2).toArray, Array(1, 2)))
        }

    }

    describe("an IntIterator for three values") {

        it("should iterate over the three values") {
            val it = IntIterator(1, 2, 3);

            assert(it.hasNext)
            assert(it.next == 1)
            assert(it.hasNext)
            assert(it.next == 2)
            assert(it.hasNext)
            assert(it.next == 3)
            assert(!it.hasNext)
        }

        it("should return an array with specified values") {
            assert(Arrays.equals(IntIterator(1, 2, 3).toArray, Array(1, 2, 3)))
        }

    }

}
