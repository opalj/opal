/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Common superclass of all instructions that perform a conditional jump based on
 * an explicit comparison with `null`.
 *
 * @author Michael Eichberg
 */
trait IFXNullInstructionLike extends SimpleConditionalBranchInstructionLike {

    final def operandCount = 1

    def condition: RelationalOperator

    final def stackSlotsChange: Int = -1

}

trait IFXNullInstruction[T <: IFXNullInstruction[T]]
    extends SimpleConditionalBranchInstruction[T]
    with IFXNullInstructionLike {

    final override def asIFXNullInstruction: this.type = this

}

object IFXNullInstruction {

    def unapply(i: IFXNullInstruction[_]): Some[(RelationalOperator, Int)] = {
        Some((i.condition, i.branchoffset))
    }

}
