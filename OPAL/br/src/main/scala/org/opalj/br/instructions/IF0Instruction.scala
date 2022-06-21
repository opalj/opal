/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Common superclass of all instructions that perform a comparison of an integer value
 * against the constant value `0`.
 *
 * @author Michael Eichberg
 */
trait IF0InstructionLike extends SimpleConditionalBranchInstructionLike {

    final def operandCount = 1

    final def stackSlotsChange: Int = -1

    def condition: RelationalOperator

}

trait IF0Instruction[T <: IF0Instruction[T]]
    extends SimpleConditionalBranchInstruction[T]
    with IF0InstructionLike {

    final override def asIF0Instruction: this.type = this

}

object IF0Instruction {

    def unapply(i: Instruction): Option[(RelationalOperator, Int /*Branchoffset*/ )] = {
        i match {
            case i: IF0Instruction[_] => Some((i.condition, i.branchoffset))
            case _                    => None
        }
    }

}
