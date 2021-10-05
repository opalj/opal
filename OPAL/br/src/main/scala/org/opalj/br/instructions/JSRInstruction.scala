/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Jump subroutine.
 *
 * @author Michael Eichberg
 */
trait JSRInstructionLike extends UnconditionalBranchInstructionLike {

    final def numberOfPushedOperands(ctg: Int => ComputationalTypeCategory): Int = 1

    final def stackSlotsChange: Int = 1
}

trait JSRInstruction extends UnconditionalBranchInstruction with JSRInstructionLike {

    final override def isIsomorphic(thisPC: PC, thatPC: PC)(implicit code: Code): Boolean = {
        val that = code.instructions(thatPC)
        (this eq that) || (
            that match {
                case that: JSRInstruction => thisPC + this.branchoffset == thatPC + that.branchoffset
                case _                    => false
            }
        )
    }
}

object JSRInstruction {

    def unapply(i: JSRInstruction): Some[Int] = Some(i.branchoffset)

}