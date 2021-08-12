/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Duplicate the top operand stack value and insert two values down.
 *
 * @author Michael Eichberg
 */
case object DUP_X1 extends StackManagementInstruction {

    final val opcode = 90

    final val mnemonic = "dup_x1"

    final def numberOfPoppedOperands(ctg: Int => ComputationalTypeCategory): Int = 2

    final def numberOfPushedOperands(ctg: Int => ComputationalTypeCategory): Int = 3

    final def stackSlotsChange: Int = 1
}
