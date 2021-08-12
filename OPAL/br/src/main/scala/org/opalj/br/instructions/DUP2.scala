/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Duplicate the top one or two operand stack values.
 *
 * @author Michael Eichberg
 */
case object DUP2 extends StackManagementInstruction {

    final val opcode = 92

    final val mnemonic = "dup2"

    final def numberOfPoppedOperands(ctg: Int => ComputationalTypeCategory): Int =
        if (ctg(0).operandSize == 1) 2 else 1

    final def numberOfPushedOperands(ctg: Int => ComputationalTypeCategory): Int =
        if (ctg(0).operandSize == 1) 4 else 2

    final def stackSlotsChange: Int = 2
}
