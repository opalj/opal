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

import org.opalj.br.instructions.Instruction

/**
 * An efficient (i.e., no (un)boxing...) representation of an instruction and its pc.
 *
 *
 * @param pc The program counter of an instruction.
 * @param i The instruction with the respective program counter.
 * @author Michael Eichberg
 */
/* no case class */ final class PCAndInstruction(val pc: Int /* PC */ , val instruction: Instruction) {

    override def hashCode(): Opcode = instruction.hashCode() * 117 + pc

    override def equals(other: Any): Boolean = {
        other match {
            case that: PCAndInstruction ⇒ this.pc == that.pc && this.instruction == that.instruction
            case _                      ⇒ false
        }
    }

    override def toString: String = s"PCAndInstruction(pc=$pc,$instruction)"
}

object PCAndInstruction {
    def apply(pc: Int /* PC */ , i: Instruction): PCAndInstruction = {
        new PCAndInstruction(pc, i)
    }

    // TODO Figure out how large the performance overhead is, when we use unapply.
    def unapply(p: PCAndInstruction): Option[(Int /* PC */ , Instruction)] = {
        Some((p.pc, p.instruction))
    }
}