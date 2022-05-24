/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package control

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.collection.immutable.ArraySeq

/**
 * Tests the implemented control abstractions.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class ControlAbstractionsTest extends AnyFlatSpec with Matchers {

    //
    // foreachNonNullValueOf
    //

    behavior of "the ControlAbstractions.foreachNonNullValueOf macro"

    it should "evaluate the expression that passes in the array exactly once" in {
        var initialized = false
        foreachNonNullValue[String] {
            if (initialized) fail(); initialized = true; new Array(0)
        } {
            (i, e) => /*nothing*/ ;
        }

        initialized = false
        foreachNonNullValue[String] {
            if (initialized) fail(); initialized = true; Array("a", "b", "c", null, null, "d")
        } {
            (i, e) => /*nothing*/ ;
        }
    }

    it should "evaluate the expression that processes the array elements exactly once per value" in {
        var result = List.empty[(Int, String)]
        foreachNonNullValue[String] {
            Array(null, "a", null, "b", "c", null, null, "d", null, null)
        } {
            (i, e) => result = (i, e) :: result;
        }
        result.reverse should be(List((1, "a"), (3, "b"), (4, "c"), (7, "d")))
    }

    //
    // REPEAT
    //

    behavior of "the ControlAbstractions.repeat macro"

    it should "should do nothing if times is 0" in {
        var success = true
        repeat(0) { success = false }
        assert(success)
    }

    it should "should execute the given function the given numeber of times" in {
        var index = 0
        repeat(5) { index += 1 }
        assert(index == 5)
    }

    //
    // fill(Int|AnyRef)Array
    //

    behavior of "the ControlAbstractions.fill(Int|AnyRef)Array macro"

    it should "return a empty ArraySeq when the number of times is 0" in {
        val result = fillIntArray(0) { 1 }
        result.isEmpty should be(true)
    }

    it should "return an ArraySeq with one entry when the number of times is 1" in {
        val result = fillIntArray(1) { 1 }
        result should be(ArraySeq(1))
        result(0) should be(1)
        result.length should be(1)
    }

    it should "return an ArraySeq with five entries that are dynamically calculated when the number of times is 5" in {
        var index = 0
        val result = fillIntArray(5) { index += 1; index }
        result.length should be(5)
        result.toList should be(List(1, 2, 3, 4, 5))
    }

    it should "work when the number of times is calculated at runtime" in {
        var index = 0
        val result = fillIntArray((System.nanoTime() % 3 + 1).toInt) { index += 1; index }
        result.length should not be (0)
    }

    it should "evaluate the expression that calculates the number of times just once" in {
        var times = 0
        var index = 0
        val result = fillIntArray({ times = times + 1; times }) { index += 1; index };
        times should be(1)
        result.toList should be(List(1))
    }

    //
    // ITERATE
    //

    behavior of "the ControlAbstractions.iterateTo macro"

    it should "iterate over all values in a range" in {
        var lastResult = -1
        iterateTo(0, 10) { i =>
            if (i != lastResult + 1)
                fail();

            lastResult = i
        }
        lastResult should be(10)
    }

    behavior of "the ControlAbstractions.iterateUntil macro"

    it should "iterate over all values in a range" in {
        var lastResult = -1
        iterateUntil(0, 10) { i =>
            if (i != lastResult + 1)
                fail();

            lastResult = i
        }
        lastResult should be(9)
    }

}
