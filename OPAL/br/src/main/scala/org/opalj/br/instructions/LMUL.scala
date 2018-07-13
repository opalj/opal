/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Multiply long.
 *
 * @author Michael Eichberg
 */
case object LMUL extends MultiplyInstruction {

    final val opcode = 105

    final val mnemonic = "lmul"

    final val computationalType = ComputationalTypeLong

    final def stackSlotsChange: Int = -computationalType.operandSize

}
