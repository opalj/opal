/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Duplicate the top one or two operand stack values and insert two or
 * three values down.
 *
 * @author Michael Eichberg
 */
case object DUP2_X1 extends StackManagementInstruction {

    final val opcode = 93

    final val mnemonic = "dup2_x1"

    final def numberOfPoppedOperands(ctg: Int => ComputationalTypeCategory): Int = {
        if (ctg(0) == Category2ComputationalTypeCategory) 2 else 3
    }

    final def numberOfPushedOperands(ctg: Int => ComputationalTypeCategory): Int = {
        if (ctg(0) == Category2ComputationalTypeCategory) 3 else 5
    }

    final def stackSlotsChange: Int = 2
}
