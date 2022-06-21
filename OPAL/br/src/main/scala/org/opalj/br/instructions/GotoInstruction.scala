/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

import scala.annotation.switch

/**
 * Super class of the Goto instructions.
 *
 * @author Michael Eichberg
 */
trait GotoInstructionLike extends UnconditionalBranchInstructionLike {

    final def numberOfPushedOperands(ctg: Int => ComputationalTypeCategory): Int = 0

    final def isIsomorphic(thisPC: PC, otherPC: PC)(implicit code: Code): Boolean = {
        val other = code.instructions(otherPC)
        (this eq other) || this == other
    }

}

object GotoInstruction {

    def unapply(instruction: Instruction): Option[Int] = {
        (instruction.opcode: @switch) match {
            case GOTO.opcode | GOTO_W.opcode =>
                Some(instruction.asInstanceOf[GotoInstruction].branchoffset)
            case _ => None
        }
    }
}

trait GotoInstruction extends UnconditionalBranchInstruction with GotoInstructionLike {

    final override def isGotoInstruction: Boolean = true

    final override def asGotoInstruction: this.type = this

}
