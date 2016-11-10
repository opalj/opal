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
package immutable

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSpec
import org.scalatest.Matchers

/**
 * Tests IdentityPair.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class IdentityPairTest extends FunSpec with Matchers {

    describe("an IdentityPair") {

        val a = new String("fooBar")
        val b = "foo"+"Bar"
        require(a ne b)
        require(a == b)
        val p1 = new IdentityPair(a, b) // #1
        val p2 = new IdentityPair(a, a) // #2
        val p3 = new IdentityPair(a, b) // #3

        it("should return the given values") {
            assert(p1._1 eq a)
            assert(p1._2 eq b)
        }

        it("should be equal to a pair containing the same values") {
            p1 should equal(p3)
            p1.hashCode should be(p3.hashCode)
        }

        it("should not be equal to a pair containing equal values") {
            p1 should not equal (p2)
        }

        it("should never be equal to another Product2 (Pair)") {
            val p = ((a, b))
            assert((p._1 eq p1._1) && (p._2 eq p1._2))

            p1 should not equal (p)
            (p) should not equal (p1)
        }
    }

}
