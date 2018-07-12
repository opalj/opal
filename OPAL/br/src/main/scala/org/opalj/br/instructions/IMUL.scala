/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Multiply int.
 *
 * @author Michael Eichberg
 */
case object IMUL extends MultiplyInstruction {

    final val opcode = 104

    final val mnemonic = "imul"

    final val computationalType = ComputationalTypeInt

    final def stackSlotsChange: Int = -1
}
