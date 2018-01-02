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
package instructions

/**
 * Branch if int comparison succeeds; succeeds if and only if value1 ≤ value2.
 *
 * @author Michael Eichberg
 */
trait IF_ICMPLELike extends IFICMPInstructionLike {

    final def opcode: Opcode = IF_ICMPLE.opcode

    final def mnemonic: String = "if_icmple"

    final def operator: String = "<="

    final def condition: RelationalOperator = RelationalOperators.LE

}

case class IF_ICMPLE(branchoffset: Int) extends IFICMPInstruction[IF_ICMPLE] with IF_ICMPLELike {

    def copy(branchoffset: Int): IF_ICMPLE = new IF_ICMPLE(branchoffset)

    def negate(newBranchoffset: Int = branchoffset): IF_ICMPGT = {
        IF_ICMPGT(newBranchoffset)
    }

    def toLabeledInstruction(currentPC: PC): LabeledInstruction = {
        LabeledIF_ICMPLE(InstructionLabel(currentPC + branchoffset))
    }
}

/**
 * Defines constants and factory methods.
 *
 * @author Malte Limmeroth
 */
object IF_ICMPLE {

    final val opcode = 164

    /**
     * Creates [[LabeledIF_ICMPLE]] instructions with a `Symbol` as the branch target.
     */
    def apply(branchTarget: InstructionLabel): LabeledIF_ICMPLE = LabeledIF_ICMPLE(branchTarget)

}

case class LabeledIF_ICMPLE(
        branchTarget: InstructionLabel
) extends LabeledSimpleConditionalBranchInstruction with IF_ICMPLELike {

    @throws[BranchoffsetException]("if the branchoffset is invalid")
    override def resolveJumpTargets(pc: PC, pcs: Map[InstructionLabel, PC]): IF_ICMPLE = {
        IF_ICMPLE(asShortBranchoffset(pcs(branchTarget) - pc))
    }
}
