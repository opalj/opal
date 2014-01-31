/* License (BSD Style License):
 * Copyright (c) 2009 - 2013
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
 *  - Neither the name of the Software Technology Group or Technische
 *    Universität Darmstadt nor the names of its contributors may be used to
 *    endorse or promote products derived from this software without specific
 *    prior written permission.
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
package de.tud.cs.st
package bat
package resolved
package ai
package domain
package l1

import de.tud.cs.st.util.{ Answer, Yes, No, Unknown }

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.Matchers
import org.scalatest.concurrent.TimeLimitedTests
import org.scalatest.time._
import org.scalatest.BeforeAndAfterAll
import org.scalatest.ParallelTestExecution

/**
 * This test(suite) checks if PreciseLongValues is working fine
 *
 * @author Riadh Chtara
 */
@RunWith(classOf[JUnitRunner])
class PreciseLongValuesTest
        extends FlatSpec
        with Matchers
        with ParallelTestExecution {

    val domain = new DefaultConfigurableDomain("PreciseLongValuesTest")
    import domain._

    //
    // TESTS
    //

    behavior of "the precise long values domain"

    //
    // QUESTION'S ABOUT VALUES
    //

    it should ("be able to check if two long values are equal") in {
        val v1 = LongValue(-1, 7)
        val v2 = LongValue(-1, 7)
        val v3 = LongValue(-1, 8)
        v1.equals(v2) should be(true)
        v1.equals(v3) should be(false)
    }

// TODO [RIADH]   it should ("be able to check if a long value is in some range") in {
//        val v1 = LongValue(-1, 7)
//        isSomeValueInRange(v1, 6, 10) should be(Yes)
//    }
//
// TODO [RIADH]   it should ("be able to check if a long value is not in some range") in {
//        val v1 = LongValue(-1, 7)
//        isSomeValueNotInRange(v1, 8, 10) should be(No)
//    }

    it should ("be able to check if a long value is less than another value") in {
        val v1 = LongValue(-1, 7)
        val v2 = LongValue(-1, 8)
        isLessThan(v1, v2) should be(Answer(true))
        isLessThan(v1, v1) should be(Answer(false))
        isLessThan(v2, v1) should be(Answer(false))
    }

    it should ("be able to check if a long value is less than or equal another value") in {
        val v1 = LongValue(-1, 7)
        val v2 = LongValue(-1, 8)
        isLessThanOrEqualTo(v1, v2) should be(Answer(true))
        isLessThanOrEqualTo(v1, v1) should be(Answer(true))
        isLessThanOrEqualTo(v2, v1) should be(Answer(false))
    }

    //
    // RELATIONAL OPERATORS
    //

    it should ("be able to compare two long values") in {
        val v1 = LongValue(-1, 7)
        val v2 = LongValue(-1, 8)
        lcmp(-1, v1, v2) should be(IntegerValue(-1, -1))
        lcmp(-1, v1, v1) should be(IntegerValue(-1, 0))
        lcmp(-1, v2, v1) should be(IntegerValue(-1, 1))
    }

    //
    // UNARY EXPRESSIONS
    //

    it should ("be able to the calculate the neg of a long value") in {
        val v1 = LongValue(-1, 7)
        val v2 = lneg(-1, lneg(-1, v1))
        v1.equals(v2) should be(true)
    }

    //
    // BINARY EXPRESSIONS
    //

    it should ("be able to the calculate the result of the add of two long values") in {
        val v1 = LongValue(-1, 7)
        val v2 = LongValue(-1, 6)
        ladd(-1, v1, v2) should be(LongValue(-1, 7 + 6))
    }

    it should ("be able to the calculate the result of the and of two long values") in {
        val v1 = LongValue(-1, 7)
        val v2 = LongValue(-1, 6)
        land(-1, v1, v2) should be(LongValue(-1, 7 & 6))
    }

    it should ("be able to the calculate the result of the div of two long values") in {
        val v1 = LongValue(-1, 7)
        val v2 = LongValue(-1, 6)
        ldiv(-1, v1, v2) should be(ComputedValue(LongValue(-1, 7 / 6)))
    }

    it should ("be able to the calculate the result of the mul of two long values") in {
        val v1 = LongValue(-1, 7)
        val v2 = LongValue(-1, 6)
        lmul(-1, v1, v2) should be(LongValue(-1, 7 * 6))
    }

    it should ("be able to the calculate the result of the or of two long values") in {
        val v1 = LongValue(-1, 7)
        val v2 = LongValue(-1, 6)
        lor(-1, v1, v2) should be(LongValue(-1, 7 | 6))
    }

    it should ("be able to the calculate the result of the rem of two long values") in {
        val v1 = LongValue(-1, 7)
        val v2 = LongValue(-1, 6)
        lrem(-1, v1, v2) should be(ComputedValue(LongValue(-1, 7 % 6)))
    }

    it should ("be able to the calculate the result of the shl of two long values") in {
        val v1 = LongValue(-1, 7)
        val v2 = LongValue(-1, 6)
        lshl(-1, v1, v2) should be(LongValue(-1, 7 << 6))
    }

    it should ("be able to the calculate the result of the or of shr long values") in {
        val v1 = LongValue(-1, 7)
        val v2 = LongValue(-1, 6)
        lshr(-1, v1, v2) should be(LongValue(-1, 7 >> 6))
    }

    it should ("be able to the calculate the result of the sub of two long values") in {
        val v1 = LongValue(-1, 7)
        val v2 = LongValue(-1, 6)
        lsub(-1, v1, v2) should be(LongValue(-1, 7 - 6))
    }

    it should ("be able to the calculate the result of the ushr of two long values") in {
        val v1 = LongValue(-1, 7)
        val v2 = LongValue(-1, 6)
        lushr(-1, v1, v2) should be(LongValue(-1, 7 >>> 6))
    }

    it should ("be able to the calculate the result of the xor of two long values") in {
        val v1 = LongValue(-1, 7)
        val v2 = LongValue(-1, 6)
        lxor(-1, v1, v2) should be(LongValue(-1, 7 ^ 6))
    }
}

