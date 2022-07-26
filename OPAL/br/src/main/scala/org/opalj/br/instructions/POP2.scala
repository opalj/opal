/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Pops the top ''computational type category 2'' value or the two top operand stack values
 * if both have ''computational type category 1''.
 *
 * @author Michael Eichberg
 */
case object POP2 extends PopInstruction {

    final val opcode = 88

    final val mnemonic = "pop2"

    final def numberOfPoppedOperands(ctg: Int => ComputationalTypeCategory): Int = {
        if (ctg(0).operandSize == 1)
            2
        else
            1
    }

    final def numberOfPushedOperands(ctg: Int => ComputationalTypeCategory): Int = 0

    final def stackSlotsChange: Int = -2
}
