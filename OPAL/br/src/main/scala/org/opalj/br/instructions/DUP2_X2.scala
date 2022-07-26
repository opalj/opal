/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Duplicate the top one or two operand stack values and insert two,
 * three, or four values down.
 *
 * @author Michael Eichberg
 */
case object DUP2_X2 extends StackManagementInstruction {

    final val opcode = 94

    final val mnemonic = "dup2_x2"

    final def numberOfPoppedOperands(ctg: Int => ComputationalTypeCategory): Int =
        if (ctg(0) == Category2ComputationalTypeCategory) {
            if (ctg(1) == Category2ComputationalTypeCategory)
                2 // Form 4
            else
                3 // Form 2

        } else {
            if (ctg(2) == Category2ComputationalTypeCategory)
                3 // Form 3
            else
                4 // Form 1
        }

    final def numberOfPushedOperands(ctg: Int => ComputationalTypeCategory): Int =
        if (ctg(0) == Category2ComputationalTypeCategory) {
            if (ctg(1) == Category2ComputationalTypeCategory)
                3 // Form 4
            else
                4 // Form 2
        } else {
            if (ctg(2) == Category2ComputationalTypeCategory)
                5 // Form 3
            else
                6 // Form 1
        }

    final def stackSlotsChange: Int = 2

}
