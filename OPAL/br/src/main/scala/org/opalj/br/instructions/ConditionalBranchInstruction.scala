/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Common super class of all conditional branch instructions.
 *
 * @author Michael Eichberg
 */
trait ConditionalBranchInstructionLike extends ControlTransferInstructionLike {

    final def numberOfPoppedOperands(ctg: Int => ComputationalTypeCategory): Int = operandCount

    final def numberOfPushedOperands(ctg: Int => ComputationalTypeCategory): Int = 0

    final def readsLocal: Boolean = false

    final def indexOfReadLocal: Int = throw new UnsupportedOperationException()

    final def writesLocal: Boolean = false

    final def indexOfWrittenLocal: Int = throw new UnsupportedOperationException()

    /**
     * The number of operand values popped from the operand stack.
     */
    def operandCount: Int

}

trait ConditionalBranchInstruction
    extends ControlTransferInstruction
    with ConditionalBranchInstructionLike
