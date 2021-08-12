/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Return void from method.
 *
 * @author Michael Eichberg
 */
case object RETURN extends ReturnInstruction {

    final val opcode = 177

    final val mnemonic = "return"

    final def numberOfPoppedOperands(ctg: Int => ComputationalTypeCategory): Int = 0

    final def stackSlotsChange: Int = 0
}
