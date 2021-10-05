/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Common superclass of all instructions that perform a conditional jump based
 * on the comparison of two integer values.
 *
 * @author Michael Eichberg
 */
trait IFICMPInstructionLike extends SimpleConditionalBranchInstructionLike {

    final def operandCount = 2

    final def stackSlotsChange: Int = -2

    def condition: RelationalOperator
}

trait IFICMPInstruction[T <: IFICMPInstruction[T]]
    extends SimpleConditionalBranchInstruction[T]
    with IFICMPInstructionLike {

    final override def asIFICMPInstruction: this.type = this

}

object IFICMPInstruction {

    def unapply(i: Instruction): Option[(RelationalOperator, Int /*Branchoffset*/ )] = {
        i match {
            case i: IFICMPInstruction[_] => Some((i.condition, i.branchoffset))
            case _                       => None
        }
    }

}
