/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Pop the top operand stack value.
 *
 * @author Michael Eichberg
 */
case object POP extends PopInstruction {

    final val opcode = 87

    final val mnemonic = "pop"

    final def numberOfPoppedOperands(ctg: Int => ComputationalTypeCategory): Int = 1

    final def numberOfPushedOperands(ctg: Int => ComputationalTypeCategory): Int = 0

    final def stackSlotsChange: Int = -1
}
