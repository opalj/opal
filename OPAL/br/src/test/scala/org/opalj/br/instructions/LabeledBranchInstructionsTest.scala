/* BSD 2-Clause License:
 * Copyright (c) 2016
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
package org.opalj.br.instructions

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}

/**
 * Tests instantiation and resolving of LabeledBranchInstructions
 *
 * @author Malte Limmeroth
 */
@RunWith(classOf[JUnitRunner])
class LabeledBranchInstructionsTest extends FlatSpec with Matchers {
    behavior of "LabeledBranchInstructions"

    val label = 'TestLabel
    val simpleBranchInstructionsMap = List(
        IFEQ(label) → LabeledIFEQ,
        IFNE(label) → LabeledIFNE,
        IFLT(label) → LabeledIFLT,
        IFGE(label) → LabeledIFGE,
        IFGT(label) → LabeledIFGT,
        IFLE(label) → LabeledIFLE,

        IF_ICMPEQ(label) → LabeledIF_ICMPEQ,
        IF_ICMPNE(label) → LabeledIF_ICMPNE,
        IF_ICMPLT(label) → LabeledIF_ICMPLT,
        IF_ICMPGE(label) → LabeledIF_ICMPGE,
        IF_ICMPGT(label) → LabeledIF_ICMPGT,
        IF_ICMPLE(label) → LabeledIF_ICMPLE,
        IF_ACMPEQ(label) → LabeledIF_ACMPEQ,
        IF_ACMPNE(label) → LabeledIF_ACMPNE,

        IFNULL(label) → LabeledIFNULL,
        IFNONNULL(label) → LabeledIFNONNULL
    )

    val offset = 42
    val simpleBranchInstructions = List(
        IFEQ(offset),
        IFNE(offset),
        IFLT(offset),
        IFGE(offset),
        IFGT(offset),
        IFLE(offset),

        IF_ICMPEQ(offset),
        IF_ICMPNE(offset),
        IF_ICMPLT(offset),
        IF_ICMPGE(offset),
        IF_ICMPGT(offset),
        IF_ICMPLE(offset),
        IF_ACMPEQ(offset),
        IF_ACMPNE(offset),

        IFNULL(offset),
        IFNONNULL(offset)
    )

    val resolvedSimpleBranchInstructions = for (i ← simpleBranchInstructionsMap.indices)
        yield simpleBranchInstructionsMap(i)._1
        .resolveLabel(i)
        .asInstanceOf[SimpleConditionalBranchInstruction]

    "the convenience factories of SimpleConditionalBranchInstructions" should
        "return the correct type of LabeledBranchInstruction" in {
            simpleBranchInstructionsMap.foreach { bi ⇒
                assert(bi._1 == bi._2(label))
            }
        }

    "LabeledBranchInstruction.resolve" should "resolve to the correct BranchOffset" in {
        for (i ← resolvedSimpleBranchInstructions.indices) {
            assert(resolvedSimpleBranchInstructions(i).branchoffset == i)
        }
    }

    "LabeledBranchInstructions" should "resolve to the correct Instruction" in {
        for (i ← simpleBranchInstructions.indices) {
            assert(resolvedSimpleBranchInstructions(i).getClass ==
                simpleBranchInstructions(i).getClass)
        }
    }

    "LabelSimpleConditionalBranchInstruction" should "mirror all properties of "+
        "SimpleConditionalBranchInstruction" in {
            for (i ← simpleBranchInstructionsMap.indices) {
                val testInst = simpleBranchInstructionsMap(i)._1
                val refInst = simpleBranchInstructions(i)

                assert(testInst.opcode == refInst.opcode)
                assert(testInst.toString() == s"${refInst.mnemonic}($label)")
                assert(testInst.operator == refInst.operator)
                assert(testInst.operandCount == refInst.operandCount)
                assert(testInst.stackSlotsChange == refInst.stackSlotsChange)
                assert(testInst.mnemonic == refInst.mnemonic)
                assertThrows[IllegalStateException](testInst.branchoffset)
            }
        }
}
