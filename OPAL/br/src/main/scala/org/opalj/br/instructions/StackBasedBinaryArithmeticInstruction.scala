/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Implemented by all arithmetic instructions that have two (runtime-dependent) operands.
 *
 * @note   [[IINC]] is considered a special binary instruction since it does not operate on
 *         operand stack values!
 */
trait StackBasedBinaryArithmeticInstruction extends StackBasedArithmeticInstruction {

    final def numberOfPoppedOperands(ctg: Int => ComputationalTypeCategory): Int = 2

    final def numberOfPushedOperands(ctg: Int => ComputationalTypeCategory): Int = 1

}

object StackBasedBinaryArithmeticInstruction {

    def unapply(instruction: StackBasedBinaryArithmeticInstruction): Some[ComputationalType] = {
        Some(instruction.computationalType)
    }
}
