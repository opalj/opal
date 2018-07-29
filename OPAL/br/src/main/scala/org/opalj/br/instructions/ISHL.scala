/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Shift left int.
 *
 * @author Michael Eichberg
 */
case object ISHL extends ShiftInstruction {

    final val opcode = 120

    final val mnemonic = "ishl"

    final val operator = "<<"

    final val computationalType = ComputationalTypeInt
}
