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
 * Branch if int comparison with zero succeeds; succeeds if and only if value = 0.
 *
 * @author Michael Eichberg
 */
trait IFEQLike extends IF0InstructionLike {

    final def opcode: Opcode = IFEQ.opcode

    final def mnemonic: String = "ifeq"

    final def operator: String = "== 0"

    final def condition: RelationalOperator = RelationalOperators.EQ

}

case class IFEQ(branchoffset: Int) extends IF0Instruction[IFEQ] with IFEQLike {

    def copy(branchoffset: Int): IFEQ = new IFEQ(branchoffset)

    def negate(newBranchoffset: Int = branchoffset): IFNE = {
        IFNE(newBranchoffset)
    }
}

/**
 * Defines constants and factory methods.
 *
 * @author Malte Limmeroth
 */
object IFEQ {

    final val opcode = 153

    /**
     * Creates [[LabeledIFEQ]] instructions with a `Symbol` as the branch target.
     */
    def apply(branchTarget: Symbol): LabeledIFEQ = LabeledIFEQ(branchTarget)

}

case class LabeledIFEQ(
        branchTarget: Symbol
) extends LabeledSimpleConditionalBranchInstruction with IFEQLike {

    override def resolveJumpTargets(pc: PC, pcs: Map[Symbol, PC]): IFEQ = {
        IFEQ(pcs(branchTarget) - pc)
    }
}
