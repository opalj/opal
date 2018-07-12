/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Shift left long.
 *
 * @author Michael Eichberg
 */
case object LSHL extends ShiftInstruction {

    final val opcode = 121

    final val mnemonic = "lshl"

    final def operator = "<<"

    final val computationalType = ComputationalTypeLong

}
