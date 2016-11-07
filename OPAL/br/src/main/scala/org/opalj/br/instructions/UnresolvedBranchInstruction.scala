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
 * UnresolvedBranchInstructions are used to encode branch instructions with a
 * Symbol as the branch target in the BytecodeAssembler DSL
 * @author Malte Limmeroth
 */
trait UnresolvedBranchInstruction extends ControlTransferInstruction {
    /**
      * resolves the UnresolvedBranchInstruction to the actual ControlTransferInstruction with the
      * given branchoffset
      */
    def resolve(offset: Int): ControlTransferInstruction

    /**
      * the branching target as a Symbol
      */
    def label: Symbol
}

/**
 * for convenience to instantiate UnresolvedBranchInstruction easily
 */
trait UnresolvedBranchInstructionBuilder {
    val opcode: Int

    /**
     * creates an UnresolvedBranchInstruction with the branch target "label"
     */
    def apply(label: Symbol): UnresolvedBranchInstruction = {
        opcode match {
            case 153 ⇒ Unresolved_IFEQ(label)
            case 154 ⇒ Unresolved_IFNE(label)
            case 155 ⇒ Unresolved_IFLT(label)
            case 156 ⇒ Unresolved_IFGE(label)
            case 157 ⇒ Unresolved_IFGT(label)
            case 158 ⇒ Unresolved_IFLE(label)

            case 159 ⇒ Unresolved_IF_ICMPEQ(label)
            case 160 ⇒ Unresolved_IF_ICMPNE(label)
            case 161 ⇒ Unresolved_IF_ICMPLT(label)
            case 162 ⇒ Unresolved_IF_ICMPGE(label)
            case 163 ⇒ Unresolved_IF_ICMPGT(label)
            case 164 ⇒ Unresolved_IF_ICMPLE(label)
            case 165 ⇒ Unresolved_IF_ACMPEQ(label)
            case 166 ⇒ Unresolved_IF_ACMPNE(label)

            case 198 ⇒ Unresolved_IFNULL(label)
            case 199 ⇒ Unresolved_IFNONNULL(label)
            case _ ⇒ throw new IllegalArgumentException(opcode+
                " is not a valid SimpleConditionalBranchInstruction")
        }
    }
}
