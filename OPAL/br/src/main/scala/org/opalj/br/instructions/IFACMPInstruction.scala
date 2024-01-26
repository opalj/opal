/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Common superclass of all instructions that perform a conditional jump based
 * on the comparison of reference values.
 *
 * @author Michael Eichberg
 */
trait IFACMPInstructionLike extends SimpleConditionalBranchInstructionLike {

    final def operandCount = 2

    final def stackSlotsChange: Int = -2

    def condition: RelationalOperator

}

trait IFACMPInstruction[T <: IFACMPInstruction[T]]
    extends SimpleConditionalBranchInstruction[T]
    with IFACMPInstructionLike {

    override final def asIFACMPInstruction: this.type = this

}

object IFACMPInstruction {

    def unapply(i: IFACMPInstruction[?]): Some[(RelationalOperator, Int)] = {
        Some((i.condition, i.branchoffset))
    }

}
