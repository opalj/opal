/* BSD 2-Clause License - see OPAL/LICENSE for details. */
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
            case that: PCAndInstruction => this.pc == that.pc && this.instruction == that.instruction
            case _                      => false
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