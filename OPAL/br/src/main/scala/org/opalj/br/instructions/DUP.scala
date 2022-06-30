/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Duplicate the top operand stack value.
 *
 * @author Michael Eichberg
 */
case object DUP extends StackManagementInstruction {

    final val opcode = 89

    final val mnemonic = "dup"

    final def numberOfPoppedOperands(ctg: Int => ComputationalTypeCategory): Int = 1

    final def numberOfPushedOperands(ctg: Int => ComputationalTypeCategory): Int = 2

    final def stackSlotsChange: Int = 1

}
