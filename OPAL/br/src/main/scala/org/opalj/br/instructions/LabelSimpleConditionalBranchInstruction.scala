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
package org.opalj
package br
package instructions

/**
 * UnresolvedSimpleConditionalBranchInstructions are used to encode branch instructions with a
 * Symbol as the branch target in the BytecodeAssembler DSL
 *
 * @author Malte Limmeroth
 */
trait LabelSimpleConditionalBranchInstruction
        extends SimpleConditionalBranchInstruction
        with LabelBranchInstruction {

    override def resolve(offset: Int): SimpleConditionalBranchInstruction

    //used to mirror the original instructions properties
    private val instruction: SimpleConditionalBranchInstruction = resolve(0)

    override def opcode: Int = instruction.opcode

    override def toString = mnemonic+"("+label+")"

    override def operator: String = instruction.operator

    override def branchoffset: PC =
        throw new IllegalStateException("The branchoffset has not been resolved yet")

    override def operandCount: PC = instruction.operandCount

    override def stackSlotsChange: PC = instruction.stackSlotsChange

    override def mnemonic: String = instruction.mnemonic
}

case class LabelIF_ACMPEQ(label: Symbol)
        extends LabelSimpleConditionalBranchInstruction {
    override def resolve(offset: PC): SimpleConditionalBranchInstruction = IF_ACMPEQ(offset)
}

case class LabelIF_ACMPNE(label: Symbol)
        extends LabelSimpleConditionalBranchInstruction {
    override def resolve(offset: PC): SimpleConditionalBranchInstruction = IF_ACMPNE(offset)
}

case class LabelIF_ICMPEQ(label: Symbol)
        extends LabelSimpleConditionalBranchInstruction {
    override def resolve(offset: PC): SimpleConditionalBranchInstruction = IF_ICMPEQ(offset)
}

case class LabelIF_ICMPNE(label: Symbol)
        extends LabelSimpleConditionalBranchInstruction {
    override def resolve(offset: PC): SimpleConditionalBranchInstruction = IF_ICMPNE(offset)
}

case class LabelIF_ICMPLT(label: Symbol)
        extends LabelSimpleConditionalBranchInstruction {
    override def resolve(offset: PC): SimpleConditionalBranchInstruction = IF_ICMPLT(offset)
}

case class LabelIF_ICMPGE(label: Symbol)
        extends LabelSimpleConditionalBranchInstruction {
    override def resolve(offset: PC): SimpleConditionalBranchInstruction = IF_ICMPGE(offset)
}

case class LabelIF_ICMPGT(label: Symbol)
        extends LabelSimpleConditionalBranchInstruction {
    override def resolve(offset: PC): SimpleConditionalBranchInstruction = IF_ICMPGT(offset)
}

case class LabelIF_ICMPLE(label: Symbol)
        extends LabelSimpleConditionalBranchInstruction {
    override def resolve(offset: PC): SimpleConditionalBranchInstruction = IF_ICMPLE(offset)
}

case class LabelIFEQ(label: Symbol) extends LabelSimpleConditionalBranchInstruction {
    override def resolve(offset: PC): SimpleConditionalBranchInstruction = IFEQ(offset)
}

case class LabelIFNE(label: Symbol) extends LabelSimpleConditionalBranchInstruction {
    override def resolve(offset: PC): SimpleConditionalBranchInstruction = IFNE(offset)
}

case class LabelIFLT(label: Symbol) extends LabelSimpleConditionalBranchInstruction {
    override def resolve(offset: PC): SimpleConditionalBranchInstruction = IFLT(offset)
}

case class LabelIFGE(label: Symbol) extends LabelSimpleConditionalBranchInstruction {
    override def resolve(offset: PC): SimpleConditionalBranchInstruction = IFGE(offset)
}

case class LabelIFGT(label: Symbol) extends LabelSimpleConditionalBranchInstruction {
    override def resolve(offset: PC): SimpleConditionalBranchInstruction = IFGT(offset)
}

case class LabelIFLE(label: Symbol) extends LabelSimpleConditionalBranchInstruction {
    override def resolve(offset: PC): SimpleConditionalBranchInstruction = IFLE(offset)
}

case class LabelIFNONNULL(label: Symbol)
        extends LabelSimpleConditionalBranchInstruction {
    override def resolve(offset: PC): SimpleConditionalBranchInstruction = IFNONNULL(offset)
}

case class LabelIFNULL(label: Symbol) extends LabelSimpleConditionalBranchInstruction {
    override def resolve(offset: PC): SimpleConditionalBranchInstruction = IFNULL(offset)
}