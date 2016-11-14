/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2016
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
 * UnresolvedSimpleConditionalBranchInstructions are used to encode branch instructions with a
 * Symbol as the branch target in the BytecodeAssembler DSL
 *
 * @author Malte Limmeroth
 */
trait LabeledSimpleConditionalBranchInstruction
        extends SimpleConditionalBranchInstruction
        with LabeledBranchInstruction {

    override def resolveLabel(branchoffset: Int): SimpleConditionalBranchInstruction

    //used to mirror the original instructions properties
    private val instruction: SimpleConditionalBranchInstruction = resolveLabel(0)

    override def opcode: Int = instruction.opcode

    override def toString = mnemonic+"("+branchTargetLabel+")"

    override def operator: String = instruction.operator

    override def branchoffset: PC = throw new IllegalStateException("the branchoffset is not yet resolved")

    override def operandCount: PC = instruction.operandCount

    override def stackSlotsChange: PC = instruction.stackSlotsChange

    override def mnemonic: String = instruction.mnemonic
}

case class LabeledIF_ACMPEQ(branchTargetLabel: Symbol)
        extends LabeledSimpleConditionalBranchInstruction {
    override def resolveLabel(branchoffset: Int): IF_ACMPEQ = IF_ACMPEQ(branchoffset)
}

case class LabeledIF_ACMPNE(branchTargetLabel: Symbol)
        extends LabeledSimpleConditionalBranchInstruction {
    override def resolveLabel(branchoffset: Int): IF_ACMPNE = IF_ACMPNE(branchoffset)
}

case class LabeledIF_ICMPEQ(branchTargetLabel: Symbol)
        extends LabeledSimpleConditionalBranchInstruction {
    override def resolveLabel(branchoffset: Int): IF_ICMPEQ = IF_ICMPEQ(branchoffset)
}

case class LabeledIF_ICMPNE(branchTargetLabel: Symbol)
        extends LabeledSimpleConditionalBranchInstruction {
    override def resolveLabel(branchoffset: Int): IF_ICMPNE = IF_ICMPNE(branchoffset)
}

case class LabeledIF_ICMPLT(branchTargetLabel: Symbol)
        extends LabeledSimpleConditionalBranchInstruction {
    override def resolveLabel(branchoffset: Int): IF_ICMPLT = IF_ICMPLT(branchoffset)
}

case class LabeledIF_ICMPGE(branchTargetLabel: Symbol)
        extends LabeledSimpleConditionalBranchInstruction {
    override def resolveLabel(branchoffset: Int): IF_ICMPGE = IF_ICMPGE(branchoffset)
}

case class LabeledIF_ICMPGT(branchTargetLabel: Symbol)
        extends LabeledSimpleConditionalBranchInstruction {
    override def resolveLabel(branchoffset: Int): IF_ICMPGT = IF_ICMPGT(branchoffset)
}

case class LabeledIF_ICMPLE(branchTargetLabel: Symbol)
        extends LabeledSimpleConditionalBranchInstruction {
    override def resolveLabel(branchoffset: Int): IF_ICMPLE = IF_ICMPLE(branchoffset)
}

case class LabeledIFEQ(branchTargetLabel: Symbol) extends LabeledSimpleConditionalBranchInstruction {
    override def resolveLabel(branchoffset: Int): IFEQ = IFEQ(branchoffset)
}

case class LabeledIFNE(branchTargetLabel: Symbol) extends LabeledSimpleConditionalBranchInstruction {
    override def resolveLabel(branchoffset: Int): IFNE = IFNE(branchoffset)
}

case class LabeledIFLT(branchTargetLabel: Symbol) extends LabeledSimpleConditionalBranchInstruction {
    override def resolveLabel(branchoffset: Int): IFLT = IFLT(branchoffset)
}

case class LabeledIFGE(branchTargetLabel: Symbol) extends LabeledSimpleConditionalBranchInstruction {
    override def resolveLabel(branchoffset: Int): IFGE = IFGE(branchoffset)
}

case class LabeledIFGT(branchTargetLabel: Symbol) extends LabeledSimpleConditionalBranchInstruction {
    override def resolveLabel(branchoffset: Int): IFGT = IFGT(branchoffset)
}

case class LabeledIFLE(branchTargetLabel: Symbol) extends LabeledSimpleConditionalBranchInstruction {
    override def resolveLabel(branchoffset: Int): IFLE = IFLE(branchoffset)
}

case class LabeledIFNONNULL(branchTargetLabel: Symbol)
        extends LabeledSimpleConditionalBranchInstruction {
    override def resolveLabel(branchoffset: Int): IFNONNULL = IFNONNULL(branchoffset)
}

case class LabeledIFNULL(branchTargetLabel: Symbol) extends LabeledSimpleConditionalBranchInstruction {
    override def resolveLabel(branchoffset: Int): IFNULL = IFNULL(branchoffset)
}