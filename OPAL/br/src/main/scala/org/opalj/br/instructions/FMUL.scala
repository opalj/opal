/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Multiply float.
 *
 * @author Michael Eichberg
 */
case object FMUL extends MultiplyInstruction {

    final val opcode = 106

    final val mnemonic = "fmul"

    final val computationalType = ComputationalTypeFloat

    final def stackSlotsChange: Int = -1
}
