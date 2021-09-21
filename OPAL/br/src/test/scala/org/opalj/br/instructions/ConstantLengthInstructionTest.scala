/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

/**
 * Tests for [[ConstantLengthInstruction]]s.
 *
 * @author Arne Lottmann
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class ConstantLengthInstructionTest extends AnyFunSpec with Matchers {

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
            LoadConstantInstruction(Byte.MaxValue.toInt).length should be(2)

            LoadConstantInstruction(Byte.MaxValue.toInt + 1).length should be(3)
            LoadConstantInstruction(Short.MaxValue.toInt / 2).length should be(3)
            LoadConstantInstruction(Short.MaxValue.toInt).length should be(3)

            LoadConstantInstruction(Short.MaxValue.toInt + 1).length should be(2)
            LoadConstantInstruction(Int.MaxValue / 2).length should be(2)
            LoadConstantInstruction(Int.MaxValue).length should be(2)
        }

    }
}