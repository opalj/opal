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
 * Tests for [[LoadConstantInstruction]].
 *
 * @author Arne Lottmann
 */
@RunWith(classOf[JUnitRunner])
class LoadConstantInstructionTest extends FunSpec with Matchers {

    describe("putting constant integer values on the stack") {

        it("[0..5] should be put on the stack using iconst instructions") {
            LoadConstantInstruction(0) should be(ICONST_0)
            LoadConstantInstruction(1) should be(ICONST_1)
            LoadConstantInstruction(2) should be(ICONST_2)
            LoadConstantInstruction(3) should be(ICONST_3)
            LoadConstantInstruction(4) should be(ICONST_4)
            LoadConstantInstruction(5) should be(ICONST_5)
        }

        it("byte values should use bipush") {
            Seq(Byte.MinValue, -1, 6, Byte.MaxValue / 2, Byte.MaxValue) foreach { i ⇒
                LoadConstantInstruction(i) should be(BIPUSH(i))
            }
        }

        it("short values that are larger/smaller than byte values shoud use sipush") {
            Seq(Short.MinValue,
                Byte.MinValue - 1,
                Byte.MaxValue + 1,
                Short.MaxValue / 2,
                Short.MaxValue) foreach { i ⇒
                    LoadConstantInstruction(i) should be(SIPUSH(i))
                }
        }

        it("integer values larger/smaller than short values should use ") {
            Seq(Short.MaxValue + 1, Int.MaxValue / 2, Int.MaxValue) foreach { i ⇒
                LoadConstantInstruction(i) should be(LoadInt(i))
            }
        }
    }
}