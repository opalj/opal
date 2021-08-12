/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Duplicate the top operand stack value and insert two or three values
 * down.
 *
 * @author Michael Eichberg
 */
case object DUP_X2 extends StackManagementInstruction {

    final val opcode = 91

    final val mnemonic = "dup_x2"

    final def numberOfPoppedOperands(ctg: Int => ComputationalTypeCategory): Int = {
        if (ctg(1) == Category2ComputationalTypeCategory) 2 else 3
    }

    final def numberOfPushedOperands(ctg: Int => ComputationalTypeCategory): Int = {
        if (ctg(1) == Category2ComputationalTypeCategory) 3 else 4
    }

    final def stackSlotsChange: Int = 1
}
