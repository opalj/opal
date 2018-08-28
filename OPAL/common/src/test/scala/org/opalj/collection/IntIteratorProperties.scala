/* BSD 2-Clause License - see OPAL/LICENSE for details. */
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
            is.iterator.exists(_ == v) == is.iterator.exists(_ == v)
        }
    }

    property("forall") = forAll { (is: IntArraySet, v: Int) ⇒
        classify(is.contains(v), "v is in the set") {
            is.iterator.forall(_ != v) == is.iterator.forall(_ != v)
        }
    }

    property("contains") = forAll { (is: IntArraySet, values: IntArraySet) ⇒
        values.forall { v ⇒
            is.iterator.contains(v) == is.iterator.contains(v)
        }
    }

    property("foldLeft") = forAll { (is: IntArraySet) ⇒
        is.iterator.foldLeft(0)(_ + _) == is.iterator.foldLeft(0)(_ + _)
    }

    property("map") = forAll { (is: IntArraySet) ⇒
        is.iterator.map(_ + 1).toChain == is.map(_ + 1).toChain
    }

    property("foreach") = forAll { (is: IntArraySet) ⇒
        var c: Chain[Int] = Naught
        is.iterator.foreach { i ⇒ c = i :&: c }
        c.reverse == is.toChain
    }

    property("filter") = forAll { (is: IntArraySet, values: IntArraySet) ⇒
        (is.iterator.filter(values.contains).forall(values.contains) :| "filter => forall") &&
            is.iterator.filter(values.contains).toChain == is.withFilter(values.contains).toChain
    }

    property("withFilter") = forAll { (is: IntArraySet, values: IntArraySet) ⇒
        is.iterator.withFilter(values.contains).toChain == is.withFilter(values.contains).toChain
    }

    property("toArray") = forAll { (is: IntArraySet) ⇒
        val itArray = is.iterator.toArray
        val isArray = is.toChain.toArray
        Arrays.equals(itArray, isArray) :| isArray.mkString(",")+" vs. "+itArray.mkString(",")
    }

    property("toChain") = forAll { (is: IntArraySet) ⇒
        is.iterator.toChain == is.toChain
    }

    property("mkString") = forAll { (is: IntArraySet) ⇒
        is.iterator.mkString("-", ";", "-!") == is.iterator.mkString("-", ";", "-!")
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
