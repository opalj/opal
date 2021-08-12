/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Swap the top two operand stack values.
 *
 * @author Michael Eichberg
 */
case object SWAP extends StackManagementInstruction {

    final val opcode = 95

    final val mnemonic = "swap"

    final def numberOfPoppedOperands(ctg: Int => ComputationalTypeCategory): Int = 2

    final def numberOfPushedOperands(ctg: Int => ComputationalTypeCategory): Int = 2

    final def stackSlotsChange: Int = 0
}
