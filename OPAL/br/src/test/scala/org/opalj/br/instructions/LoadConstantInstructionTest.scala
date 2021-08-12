/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

/**
 * Tests for [[LoadConstantInstruction]].
 *
 * @author Arne Lottmann
 */
@RunWith(classOf[JUnitRunner])
class LoadConstantInstructionTest extends AnyFunSpec with Matchers {

    describe("putting constant integer values on the stack") {

        it("[-1..5] should be put on the stack using iconst instructions") {
            LoadConstantInstruction(-1) should be(ICONST_M1)
            LoadConstantInstruction(0) should be(ICONST_0)
            LoadConstantInstruction(1) should be(ICONST_1)
            LoadConstantInstruction(2) should be(ICONST_2)
            LoadConstantInstruction(3) should be(ICONST_3)
            LoadConstantInstruction(4) should be(ICONST_4)
            LoadConstantInstruction(5) should be(ICONST_5)
        }

        it("byte values should use bipush") {
            Seq(
                Byte.MinValue.toInt,
                6,
                Byte.MaxValue.toInt / 2,
                Byte.MaxValue.toInt
            ) foreach { i => LoadConstantInstruction(i) should be(BIPUSH(i)) }
        }

        it("short values that are larger/smaller than byte values shoud use sipush") {
            Seq(
                Short.MinValue.toInt,
                Byte.MinValue.toInt - 1,
                Byte.MaxValue.toInt + 1,
                Short.MaxValue.toInt / 2,
                Short.MaxValue.toInt
            ) foreach { i => LoadConstantInstruction(i) should be(SIPUSH(i)) }
        }

        it("integer values larger/smaller than short values should use ") {
            Seq(
                Short.MaxValue.toInt + 1,
                Int.MaxValue.toInt / 2,
                Int.MaxValue.toInt
            ) foreach { i => LoadConstantInstruction(i) should be(LoadInt(i)) }
        }
    }
}
