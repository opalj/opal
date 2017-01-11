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
package br
package cp

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSpec
import org.scalatest.Matchers

/**
 * Basic tests of the TypesSet class.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class ConstantsBufferTest extends FunSpec with Matchers {

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
