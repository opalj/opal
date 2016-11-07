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

/**
 * UnresolvedSimpleConditionalBranchInstructions are used to encode branch instructions with a
 * Symbol as the branch target in the BytecodeAssembler DSL
 *
 * @author Malte Limmeroth
 */
trait UnresolvedSimpleConditionalBranchInstruction
        extends SimpleConditionalBranchInstruction
        with UnresolvedBranchInstruction {

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

case class Unresolved_IF_ACMPEQ(label: Symbol)
  extends UnresolvedSimpleConditionalBranchInstruction {
    override def resolve(offset: PC): SimpleConditionalBranchInstruction = IF_ACMPEQ(offset)
}

case class Unresolved_IF_ACMPNE(label: Symbol)
  extends UnresolvedSimpleConditionalBranchInstruction {
    override def resolve(offset: PC): SimpleConditionalBranchInstruction = IF_ACMPNE(offset)
}

case class Unresolved_IF_ICMPEQ(label: Symbol)
  extends UnresolvedSimpleConditionalBranchInstruction {
    override def resolve(offset: PC): SimpleConditionalBranchInstruction = IF_ICMPEQ(offset)
}

case class Unresolved_IF_ICMPNE(label: Symbol)
  extends UnresolvedSimpleConditionalBranchInstruction {
    override def resolve(offset: PC): SimpleConditionalBranchInstruction = IF_ICMPNE(offset)
}

case class Unresolved_IF_ICMPLT(label: Symbol)
  extends UnresolvedSimpleConditionalBranchInstruction {
    override def resolve(offset: PC): SimpleConditionalBranchInstruction = IF_ICMPLT(offset)
}

case class Unresolved_IF_ICMPGE(label: Symbol)
  extends UnresolvedSimpleConditionalBranchInstruction {
    override def resolve(offset: PC): SimpleConditionalBranchInstruction = IF_ICMPGE(offset)
}

case class Unresolved_IF_ICMPGT(label: Symbol)
  extends UnresolvedSimpleConditionalBranchInstruction {
    override def resolve(offset: PC): SimpleConditionalBranchInstruction = IF_ICMPGT(offset)
}

case class Unresolved_IF_ICMPLE(label: Symbol)
  extends UnresolvedSimpleConditionalBranchInstruction {
    override def resolve(offset: PC): SimpleConditionalBranchInstruction = IF_ICMPLE(offset)
}

case class Unresolved_IFEQ(label: Symbol) extends UnresolvedSimpleConditionalBranchInstruction {
    override def resolve(offset: PC): SimpleConditionalBranchInstruction = IFEQ(offset)
}

case class Unresolved_IFNE(label: Symbol) extends UnresolvedSimpleConditionalBranchInstruction {
    override def resolve(offset: PC): SimpleConditionalBranchInstruction = IFNE(offset)
}

case class Unresolved_IFLT(label: Symbol) extends UnresolvedSimpleConditionalBranchInstruction {
    override def resolve(offset: PC): SimpleConditionalBranchInstruction = IFLT(offset)
}

case class Unresolved_IFGE(label: Symbol) extends UnresolvedSimpleConditionalBranchInstruction {
    override def resolve(offset: PC): SimpleConditionalBranchInstruction = IFGE(offset)
}

case class Unresolved_IFGT(label: Symbol) extends UnresolvedSimpleConditionalBranchInstruction {
    override def resolve(offset: PC): SimpleConditionalBranchInstruction = IFGT(offset)
}

case class Unresolved_IFLE(label: Symbol) extends UnresolvedSimpleConditionalBranchInstruction {
    override def resolve(offset: PC): SimpleConditionalBranchInstruction = IFLE(offset)
}

case class Unresolved_IFNONNULL(label: Symbol)
  extends UnresolvedSimpleConditionalBranchInstruction {
    override def resolve(offset: PC): SimpleConditionalBranchInstruction = IFNONNULL(offset)
}

case class Unresolved_IFNULL(label: Symbol) extends UnresolvedSimpleConditionalBranchInstruction {
    override def resolve(offset: PC): SimpleConditionalBranchInstruction = IFNULL(offset)
}