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
 * Branch if int comparison succeeds; succeeds if and only if value1 ≠ value2.
 *
 * @author Michael Eichberg
 */
trait IF_ICMPNELike extends IFICMPInstructionLike {

    final def opcode: Opcode = IF_ICMPNE.opcode

    final def mnemonic: String = "if_icmpne"

    final def operator: String = "!="

    final def condition: RelationalOperator = RelationalOperators.NE

}

case class IF_ICMPNE(branchoffset: Int) extends IFICMPInstruction[IF_ICMPNE] with IF_ICMPNELike {

    def copy(branchoffset: Int): IF_ICMPNE = new IF_ICMPNE(branchoffset)

    def negate(newBranchoffset: Int = branchoffset): IF_ICMPEQ = {
        IF_ICMPEQ(newBranchoffset)
    }

    def toLabeledInstruction(currentPC: PC): LabeledInstruction = {
        LabeledIF_ICMPNE(InstructionLabel(currentPC + branchoffset))
    }
}

/**
 * Defines constants and factory methods.
 *
 * @author Malte Limmeroth
 */
object IF_ICMPNE {

    final val opcode = 160

    /**
     * Creates [[LabeledIF_ICMPNE]] instructions with a `Symbol` as the branch target.
     */
    def apply(branchTarget: InstructionLabel): LabeledIF_ICMPNE = LabeledIF_ICMPNE(branchTarget)

}

case class LabeledIF_ICMPNE(
        branchTarget: InstructionLabel
) extends LabeledSimpleConditionalBranchInstruction with IF_ICMPNELike {

    override def resolveJumpTargets(pc: PC, pcs: Map[InstructionLabel, PC]): IF_ICMPNE = {
        IF_ICMPNE(pcs(branchTarget) - pc)
    }
}
