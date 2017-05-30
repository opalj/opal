/*
 * BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
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
package strings

import org.junit.runner.RunWith
import org.scalatest.FunSpec
import org.scalatest.Matchers
import org.scalatest.junit.JUnitRunner

/**
 *
 * Tests the StringExtensions comparison methods.
 *
 * @author Michael Reif
 */
@RunWith(classOf[JUnitRunner])
class StringExtensionsTest extends FunSpec with Matchers {

    describe("a ConcreteString") {

        it("should be equal to another ConcreteString holding the same value") {
            val c1: StringExtension = ConcreteString("OPAL rocks!")
            val c2: StringExtension = ConcreteString("OPAL rocks!")

            c1 should equal(c2)
            c2 should equal(c1)

        }

        it("should not be equal to another ConcreteString holding a different value") {
            val c1: StringExtension = ConcreteString("OPAL rocks!")
            val c2: StringExtension = ConcreteString("OPAL always improves!")

            c1 should not equal (c2)
            c2 should not equal (c1)
        }

        it("should not be equal to other subtypes of StringExtensions") {

            val concStr: StringExtension = ConcreteString("OPAL rocks!")
            //val fragStrSame: StringExtension = StringFragment("OPAL rocks!")
            //val fragStrDiff: StringExtension = StringFragment("Soot")
            val anyStr: StringExtension = AnyString

            //concStr should not equal (fragStrSame)
            // concStr should not equal (fragStrDiff)
            concStr should not equal (anyStr)
        }

        it("should leftInclude a another ConcreteString with the same value") {
            val c1: StringExtension = ConcreteString("OPAL rocks!")
            val c2: StringExtension = ConcreteString("OPAL rocks!")

            c1.leftIncludes(c2) shouldBe (Yes)
            c2.leftIncludes(c1) shouldBe (Yes)
        }

        it("should not leftInclude other StringExtensions") {
            val c1: StringExtension = ConcreteString("OPAL rocks!")
            val c2: StringExtension = ConcreteString("OPAL does rock!")
            val c3 = ConcreteString("")

            c1.leftIncludes(c2) shouldBe (No)
            c1.leftIncludes(c3) shouldBe (No)
        }

        it("should rightInclude a another ConcreteString with the same value") {
            val c1: StringExtension = ConcreteString("OPAL rocks!")
            val c2: StringExtension = ConcreteString("OPAL rocks!")

            c1.rightIncludes(c2) shouldBe (Yes)
            c2.rightIncludes(c1) shouldBe (Yes)
        }

        it("should not rightInclude other StringExtensions") {
            val c1: StringExtension = ConcreteString("OPAL does")
            val c2: StringExtension = ConcreteString("OPAL does rock!")
            val c3 = ConcreteString("")

            c1.rightIncludes(c2) shouldBe (No)
            c1.rightIncludes(c3) shouldBe (No)
        }
    }

    /* describe("a StringFragment") {

        it("should be equal to another StringFragment holding the same value") {
            val f1: StringExtension = StringFragment("OPAL++")
            val f2: StringExtension = StringFragment("OPAL++")

            f1 should equal(f2)
            f2 should equal(f1)
        }

        it("should not be equal to another StringFragment holding a different value") {
            val f1: StringExtension = StringFragment("OPAL++")
            val f2: StringExtension = StringFragment("++OPAL")

            f1 should not equal (f2)
            f2 should not equal (f1)
        }

        it("should not be equal to other subtypes of StringExtensions") {
            val fragStr: StringExtension = StringFragment("++OPAL")
            val concStrDiff: StringExtension = ConcreteString("OPAL rocks!")
            val concStrSame: StringExtension = ConcreteString("++OPAL")
            val anyStr: StringExtension = AnyString

            fragStr should not equal (concStrSame)
            fragStr should not equal (concStrDiff)
            fragStr should not equal (anyStr)
        }

        it("should leftInclude ConcreteStrings that can be build from the fragment") {
            val fragStr: StringExtension = StringFragment("OPAL")

            val prefix = ConcreteString("OPAL rocks!")
            val suffix = ConcreteString("PREOPAL")
            val exact = ConcreteString("OPAL")
            val substring = ConcreteString("the OPAL framework")

            fragStr.leftIncludes(prefix) shouldBe (Yes)
            fragStr.leftIncludes(suffix) shouldBe (Yes)
            fragStr.leftIncludes(exact) shouldBe (Yes)
            fragStr.leftIncludes(substring) shouldBe (Yes)
        }

        it("should not leftInclude a ConcreteString that can't be build from the fragment") {
            val fragStr = StringFragment("OPAL")

            val cs1 = ConcreteString("OPA")
            val cs2 = ConcreteString("OP AL")
            val cs3 = ConcreteString("PAL")

            fragStr.leftIncludes(cs1) shouldBe (No)
            fragStr.leftIncludes(cs2) shouldBe (No)
            fragStr.leftIncludes(cs3) shouldBe (No)
        }
    } */

    describe("the AnyString object ") {

        it("should not be equal to other subtypes of StringExtensions") {
            //val fragStr: StringExtension = StringFragment("++OPAL")
            val concStr: StringExtension = ConcreteString("++OPAL")
            val anyStr: StringExtension = AnyString

            //            anyStr should not equal (fragStr)
            anyStr should not equal (concStr)
        }

        it("should leftInclude every other possible StringExtension") {
            val concStr: StringExtension = ConcreteString("OPAL rocks!")
            //          val fragStrSame: StringExtension = StringFragment("OPAL rocks!")
            val anyStr: StringExtension = AnyString

            anyStr.leftIncludes(concStr) should be(Yes)
            //        anyStr.leftIncludes(fragStrSame) should be(Yes)
            anyStr.leftIncludes(anyStr) should be(Yes)
        }
    }
}
