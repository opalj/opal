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
package br
package instructions

import org.scalatest.FunSpec
import org.scalatest.Matchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

/**
 * Tests for [[ConstantLengthInstruction]]s.
 *
 * @author Arne Lottmann
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class ConstantLengthInstructionTest extends FunSpec with Matchers {

    describe("putting constant integer values on the stack") {

        it("LoadConstantInstructions should report their correct length") {
            LoadConstantInstruction(0).length should be(1)
            LoadConstantInstruction(1).length should be(1)
            LoadConstantInstruction(2).length should be(1)
            LoadConstantInstruction(3).length should be(1)
            LoadConstantInstruction(4).length should be(1)
            LoadConstantInstruction(5).length should be(1)

            LoadConstantInstruction(6).length should be(2)
            LoadConstantInstruction(64).length should be(2)
            LoadConstantInstruction(Byte.MaxValue).length should be(2)

            LoadConstantInstruction(Byte.MaxValue + 1).length should be(3)
            LoadConstantInstruction(Short.MaxValue / 2).length should be(3)
            LoadConstantInstruction(Short.MaxValue).length should be(3)

            LoadConstantInstruction(Short.MaxValue + 1).length should be(2)
            LoadConstantInstruction(Int.MaxValue / 2).length should be(2)
            LoadConstantInstruction(Int.MaxValue).length should be(2)
        }

    }
}