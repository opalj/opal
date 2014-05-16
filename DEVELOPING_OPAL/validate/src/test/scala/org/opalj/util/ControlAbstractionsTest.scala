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
package util

import org.junit.runner.RunWith

import org.scalatest.ParallelTestExecution
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.BeforeAndAfterAll
import org.scalatest.Matchers

/**
 * Tests the implemented control abstractions.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class ControlAbstractionsTest extends FlatSpec with Matchers with ParallelTestExecution {

    behavior of "The ControlAbstractions.foreachNonNullValueOf macro"

    it should "evaluate the expression that passes in the array exactly once" in {
        var initialized = false
        foreachNonNullValueOf[String] {
            if (initialized) fail(); initialized = true; new Array(0)
        } {
            (i, e) ⇒ /*nothing*/ ;
        }

        initialized = false
        foreachNonNullValueOf[String] {
            if (initialized) fail(); initialized = true; Array("a", "b", "c", null, null, "d")
        } {
            (i, e) ⇒ /*nothing*/ ;
        }
    }

    it should "evaluate the expression that processes the array elements exactly once per value" in {
        var result = List.empty[(Int, String)]
        foreachNonNullValueOf[String] {
            Array(null, "a", null, "b", "c", null, null, "d", null, null)
        } {
            (i, e) ⇒ result = (i, e) :: result;
        }
        result.reverse should be(List((1, "a"), (3, "b"), (4, "c"), (7, "d")))
    }

    behavior of "The ControlAbstractions.repeat macro"
  
    it should "return a empty seq when the number of times is 0" in {
        val result = repeat(0) { 1 }
        result.isEmpty should be(true)
    }

    it should "return a seq with one entry when the number of times is 1" in {
        val result = repeat(1) { "Hello" }
        result(0) should be("Hello")
    }

    it should "return a seq with five entries that are dynamically calculated when the number of times is 5" in {
        var index = 0
        val result = repeat(5) { index = (index + 1); index }
        result.length should be(5)
        result should be(Seq(1, 2, 3, 4, 5))
    }

    it should "work when the number of times is calculated at runtime" in {
        var index = 0
        val result = repeat((System.nanoTime() % 3 + 1).toInt) { index = (index + 1); index }
        result.length should not be (0)
    }

    it should "evaluate the expression that calculates the number of times just once" in {
        var times = 0
        var index = 0
        val result = repeat({ times = times + 1; times }) { index = (index + 1); index };
        times should be(1)
        result should be(Seq(1))
    }

}
