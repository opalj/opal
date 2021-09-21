/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package cp

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

/**
 * Basic tests of the TypesSet class.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class ConstantsBufferTest extends AnyFunSpec with Matchers {

    //
    // Verify
    //

    describe("the ConstantsBuffer") {

        it("should be empty empty upon creation") {
            val (cp, _) = ConstantsBuffer(Set.empty).build
            cp.length should be(1)
            cp(0) should be(null)
        }

        it("should contain two entries when we add a constant string due to an LDC instruction") {
            val (cp, _) = ConstantsBuffer(Set(instructions.LoadString("hello"))).build
            cp.length should be(3)
            cp(0) should be(null)
            cp(1) should be(CONSTANT_String_info(2))
            cp(2) should be(CONSTANT_Utf8_info("hello"))
        }

        it("should still contain two entries when we add a constant string due to an LDC instruction twice") {
            val constantsBuffer = ConstantsBuffer(Set(instructions.LoadString("hello")))
            constantsBuffer.CPEString("hello", true)
            val (cp, _) = constantsBuffer.build
            cp.length should be(3)
            cp(0) should be(null)
            cp(1) should be(CONSTANT_String_info(2))
            cp(2) should be(CONSTANT_Utf8_info("hello"))
        }

        it("should still contain four entries when we add two constant strings") {
            val constantsBuffer = ConstantsBuffer(Set(instructions.LoadString("hello")))
            constantsBuffer.CPEString("world", true)
            val (cp, _) = constantsBuffer.build
            cp.length should be(5)
            cp(0) should be(null)
            cp(1) should be(CONSTANT_String_info(2))
            cp(2) should be(CONSTANT_Utf8_info("hello"))
            // added afterwards
            cp(3) should be(CONSTANT_Utf8_info("world"))
            cp(4) should be(CONSTANT_String_info(3))
        }
    }

}
